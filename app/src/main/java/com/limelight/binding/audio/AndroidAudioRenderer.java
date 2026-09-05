package com.limelight.binding.audio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.jni.MoonBridge;

/**
 * Saída de PCM do stream, sobre AudioTrack.
 *
 * O que este fork mudou em relação ao upstream: a saída deixou de ser criada uma vez e
 * usada até o fim da sessão. Uma AudioTrack morre por conta própria em duas situações
 * corriqueiras num tablet — o servidor de áudio do sistema reinicia, ou a track é
 * invalidada quando a rota muda (fone entrando, fone saindo, Bluetooth pareando). Antes,
 * o retorno de {@code write()} era ignorado: o áudio sumia para o resto da sessão, sem
 * nada no log, e só voltava reconectando o stream inteiro. Agora o erro de escrita e a
 * troca de rota levam a uma recriação da saída, com a cascata de configuração refeita —
 * o que também conserta a latência, já que a escolha de modo de baixa latência depende da
 * taxa nativa da saída ATIVA, e essa muda quando o fone Bluetooth entra.
 *
 * Regra de thread que sustenta tudo isto: <b>só a thread que escreve o PCM mexe na
 * AudioTrack</b>. O retorno de rota vem numa thread do sistema e apenas levanta uma
 * bandeira; quem abre e fecha a track é sempre {@link #playDecodedAudio}, que é chamada
 * pela thread nativa "AudioDec". {@link #start} e {@link #stop} só cuidam da sessão de
 * efeitos e do observador de rota, e por isso não disputam a track com um write() em
 * andamento — o que importa porque o AudioStream.c chama {@code stop()} ANTES de encerrar
 * a thread de decode, e só chama {@code cleanup()} depois de dar join nela.
 */
public class AndroidAudioRenderer implements AudioRenderer {

    // Teto de backlog na fila nativa antes de descartar o pacote decodificado.
    private static final int MAX_PENDING_MS = 40;

    // Uma saída que se recusa a abrir é tentada no máximo uma vez por segundo. Sem esse
    // freio seria uma tentativa por pacote — 200 por segundo a 5 ms de pacote.
    private static final long REBUILD_RETRY_INTERVAL_NS = 1000000000L;

    // O log de backlog roda na thread de decode justamente quando o aparelho já está
    // apertado. Um por segundo diz a mesma coisa sem competir pelo tempo que falta.
    private static final long OVERFLOW_LOG_INTERVAL_NS = 1000000000L;

    private final Context context;
    private final boolean enableAudioFx;
    private final Object trackLock = new Object();

    // Volátil porque start()/stop()/cleanup() chegam de outra thread que não a de escrita.
    private volatile AudioTrack track;

    private AudioTrackPlan plan;
    private int sampleRate;
    private int samplesPerFrame;
    private int channelCount;
    private int channelMask;

    private volatile boolean rebuildPending;
    private volatile long lastRebuildAttemptNs;
    private long lastOverflowLogNs;

    private int audioFxSessionId;
    private boolean audioFxSessionOpen;

    // AudioDeviceCallback só existe na API 23. Guardado como Object para que a classe
    // continue carregável na API 21, que é o mínimo deste app.
    private Object routeWatcher;
    private volatile boolean deviceSnapshotSeen;

    public AndroidAudioRenderer(Context context, boolean enableAudioFx) {
        this.context = context;
        this.enableAudioFx = enableAudioFx;
    }

    private AudioTrack createAudioTrack(AudioTrackPlan.Attempt attempt) {
        AudioAttributes.Builder attributesBuilder = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME);
        AudioFormat.Builder formatBuilder = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate);

        if (!attempt.indexedMask) {
            formatBuilder.setChannelMask(attempt.channelMask);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            formatBuilder.setChannelIndexMask(attempt.channelMask);
        }
        else {
            // A máscara por índice é a última carta da cascata e não existe antes da API 24.
            // Se o plano emitir uma aqui embaixo é defeito do plano — melhor estourar e cair
            // na tentativa seguinte do que registrar um índice como se fosse posição.
            throw new IllegalStateException("channel index mask requires API 24");
        }

        AudioFormat format = formatBuilder.build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Use FLAG_LOW_LATENCY on L through N
            if (attempt.lowLatency) {
                attributesBuilder.setFlags(AudioAttributes.FLAG_LOW_LATENCY);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioTrack.Builder trackBuilder = new AudioTrack.Builder()
                    .setAudioFormat(format)
                    .setAudioAttributes(attributesBuilder.build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(attempt.bufferSizeInBytes);

            // Use PERFORMANCE_MODE_LOW_LATENCY on O and later
            if (attempt.lowLatency) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
            }

            return trackBuilder.build();
        }
        else {
            return new AudioTrack(attributesBuilder.build(),
                    format,
                    attempt.bufferSizeInBytes,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
        }
    }

    @Override
    public int setup(MoonBridge.AudioConfiguration audioConfiguration, int sampleRate, int samplesPerFrame) {
        this.sampleRate = sampleRate;
        this.samplesPerFrame = samplesPerFrame;
        this.channelCount = audioConfiguration.channelCount;
        this.channelMask = AudioTrackPlan.channelMaskFor(audioConfiguration.channelCount);

        if (channelMask == AudioTrackPlan.NO_CHANNEL_MASK) {
            LimeLog.severe("Decoder returned unhandled channel count");
            return -1;
        }

        LimeLog.info("Audio channel config: "+String.format("0x%X", channelMask));

        plan = resolvePlanForCurrentRoute();
        if (plan == null) {
            LimeLog.severe("Decoder returned unusable audio parameters");
            return -1;
        }

        long now = System.nanoTime();
        lastRebuildAttemptNs = now - REBUILD_RETRY_INTERVAL_NS;
        lastOverflowLogNs = now - OVERFLOW_LOG_INTERVAL_NS;

        synchronized (trackLock) {
            if (!openTrackLocked()) {
                // Couldn't create any audio track for playback
                return -2;
            }
        }

        return 0;
    }

    /**
     * Monta o plano para a rota de saída que está ativa AGORA.
     *
     * Os dois valores que o sistema fornece — buffer mínimo e taxa nativa — descrevem a
     * saída atual, e é exatamente por isso que a troca de rota exige refazer a conta: num
     * fone Bluetooth a taxa nativa é outra e o modo de baixa latência deixa de fazer
     * sentido (buffer de dois frames sobre A2DP é underrun contínuo, o áudio picota).
     */
    private AudioTrackPlan resolvePlanForCurrentRoute() {
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask,
                AudioFormat.ENCODING_PCM_16BIT);
        int nativeOutputSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

        return AudioTrackPlan.resolve(channelCount, samplesPerFrame, sampleRate,
                nativeOutputSampleRate, minBufferSize, enableAudioFx,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
    }

    /** Percorre a cascata do plano e deixa aberta a primeira saída que o aparelho aceitar. */
    private boolean openTrackLocked() {
        for (AudioTrackPlan.Attempt attempt : plan.attempts()) {
            AudioTrack candidate = null;
            try {
                candidate = createAudioTrack(attempt);

                // Alguns aparelhos não estouram no construtor: devolvem uma track morta e só
                // reclamam no play(). Conferir o estado evita gastar a tentativa boa à toa.
                if (candidate.getState() != AudioTrack.STATE_INITIALIZED) {
                    throw new IllegalStateException("AudioTrack is uninitialized");
                }

                candidate.play();

                // Successfully created working AudioTrack. We're done here.
                track = candidate;
                LimeLog.info("Audio track configuration: "+attempt);
                return true;
            } catch (Exception e) {
                // Try to release the AudioTrack if we got far enough
                e.printStackTrace();
                try {
                    if (candidate != null) {
                        candidate.release();
                    }
                } catch (Exception ignored) {}
            }
        }

        track = null;
        return false;
    }

    private void releaseTrackLocked() {
        AudioTrack doomed = track;
        track = null;
        if (doomed == null) {
            return;
        }

        try {
            // Immediately drop all pending data
            doomed.pause();
            doomed.flush();
        } catch (IllegalStateException e) {
            // Uma track já invalidada recusa pause/flush. Não impede o release.
            LimeLog.warning("Audio track refused pause/flush before release: "+e.getMessage());
        }

        doomed.release();
    }

    /**
     * Fecha a saída e abre outra com a cascata refeita. Só pode ser chamada pela thread que
     * escreve o PCM: é a garantia de que nenhum {@code write()} está em curso na track velha.
     */
    private void rebuildTrack(String reason) {
        long now = System.nanoTime();
        if (now - lastRebuildAttemptNs < REBUILD_RETRY_INTERVAL_NS) {
            return;
        }
        lastRebuildAttemptNs = now;

        synchronized (trackLock) {
            closeAudioFxSessionLocked();
            releaseTrackLocked();

            // Refazer o plano é metade do conserto: a taxa nativa e o buffer mínimo da rota
            // nova são outros, e reaproveitar os antigos manteria a latência errada.
            AudioTrackPlan refreshed = resolvePlanForCurrentRoute();
            if (refreshed != null) {
                plan = refreshed;
            }

            if (openTrackLocked()) {
                rebuildPending = false;
                openAudioFxSessionLocked();
                LimeLog.info("Audio output rebuilt ("+reason+")");
            }
            else {
                // Sem saída não há o que escrever, mas o pacote continua sendo consumido da
                // fila nativa — o backlog não cresce e a próxima tentativa vem em 1 s.
                LimeLog.warning("Audio output could not be rebuilt ("+reason+"), will retry");
            }
        }
    }

    @Override
    public void playDecodedAudio(short[] audioData) {
        if (rebuildPending) {
            rebuildTrack("output route changed");
        }

        AudioTrack currentTrack = track;
        if (currentTrack == null) {
            // Sem saída aberta. Descartar aqui é o certo: o pacote sai da fila do mesmo
            // jeito e o backlog não cresce enquanto a recriação não vinga.
            return;
        }

        // Only queue up to 40 ms of pending audio data in addition to what AudioTrack is buffering for us.
        int pendingDuration = MoonBridge.getPendingAudioDuration();
        if (pendingDuration >= MAX_PENDING_MS) {
            logOverflow(pendingDuration);
            return;
        }

        int written;
        try {
            // This will block until the write is completed. That can cause a backlog
            // of pending audio data, so we do the above check to be able to bound
            // latency at 40 ms in that situation.
            written = currentTrack.write(audioData, 0, audioData.length);
        } catch (IllegalStateException e) {
            // Uma track já liberada estoura em vez de devolver código de erro. Deixar a
            // exceção subir seria fatal: quem chamou é a thread nativa de decode, e o
            // callbacks.c reage a exceção pendente desanexando a thread — "We will crash
            // here", nas palavras dele. Tratar como track morta é a leitura correta.
            LimeLog.warning("AudioTrack refused the write: "+e.getMessage());
            rebuildTrack("write threw "+e.getClass().getSimpleName());
            return;
        }

        if (written < 0 && AudioTrackPlan.requiresRebuild(written)) {
            // Este é o caminho pelo qual o áudio sumia até o fim da sessão: a track morre
            // (servidor de áudio reiniciado, rota invalidada) e todo write seguinte falha
            // em silêncio. O pacote atual está perdido de qualquer jeito; o que interessa
            // é a saída voltar antes do próximo.
            rebuildTrack("write failed with "+written);
        }
    }

    private void logOverflow(int pendingDuration) {
        long now = System.nanoTime();
        if (now - lastOverflowLogNs < OVERFLOW_LOG_INTERVAL_NS) {
            return;
        }
        lastOverflowLogNs = now;

        LimeLog.info("Too much pending audio data: " + pendingDuration + " ms");
    }

    @Override
    public void start() {
        synchronized (trackLock) {
            openAudioFxSessionLocked();
        }

        registerRouteWatcher();
    }

    @Override
    public void stop() {
        unregisterRouteWatcher();

        synchronized (trackLock) {
            closeAudioFxSessionLocked();
        }
    }

    @Override
    public void cleanup() {
        synchronized (trackLock) {
            // Defesa: se stop() não veio, não deixar uma sessão de efeitos pendurada no
            // sistema apontando para um id de sessão que acabou de ser liberado.
            closeAudioFxSessionLocked();
            releaseTrackLocked();
        }
    }

    private void openAudioFxSessionLocked() {
        if (!enableAudioFx || audioFxSessionOpen) {
            return;
        }

        AudioTrack currentTrack = track;
        if (currentTrack == null) {
            return;
        }

        // Open an audio effect control session to allow equalizers to apply audio effects
        audioFxSessionId = currentTrack.getAudioSessionId();
        Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioFxSessionId);
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        i.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_GAME);
        context.sendBroadcast(i);
        audioFxSessionOpen = true;
    }

    private void closeAudioFxSessionLocked() {
        if (!audioFxSessionOpen) {
            return;
        }

        // Close our audio effect control session when we're stopping. O id é guardado em
        // campo porque a recriação da saída troca a sessão, e fechar a nova no lugar da
        // velha deixaria um equalizador preso a uma sessão que não existe mais.
        Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioFxSessionId);
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(i);
        audioFxSessionOpen = false;
    }

    private AudioManager getAudioManager() {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private void registerRouteWatcher() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Sem AudioDeviceCallback resta o caminho reativo: a recriação continua
            // acontecendo quando o write() falha, só não se antecipa à troca de rota.
            return;
        }

        AudioManager audioManager = getAudioManager();
        if (audioManager == null) {
            return;
        }

        deviceSnapshotSeen = false;
        RouteWatcher watcher = new RouteWatcher();
        routeWatcher = watcher;

        Handler handler = new Handler(Looper.getMainLooper());
        audioManager.registerAudioDeviceCallback(watcher, handler);

        // O registro dispara um onAudioDevicesAdded com o inventário de quem já estava
        // conectado — isso não é uma troca e não pode custar um corte de áudio no começo de
        // toda sessão. Esta mensagem entra na fila DEPOIS daquele retorno; se ele não vier
        // (aparelho sem saída nenhuma listada), ela destrava o observador do mesmo jeito.
        handler.post(() -> deviceSnapshotSeen = true);
    }

    private void unregisterRouteWatcher() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        Object watcher = routeWatcher;
        routeWatcher = null;
        if (watcher == null) {
            return;
        }

        AudioManager audioManager = getAudioManager();
        if (audioManager != null) {
            audioManager.unregisterAudioDeviceCallback((AudioDeviceCallback) watcher);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void requestRebuildForSinks(AudioDeviceInfo[] devices) {
        for (AudioDeviceInfo device : devices) {
            if (device.isSink()) {
                // A recriação em si acontece na thread que escreve o PCM. Aqui só se levanta
                // a bandeira: mexer na AudioTrack a partir da thread do sistema correria com
                // um write() em andamento, que é bloqueante.
                rebuildPending = true;
                return;
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private final class RouteWatcher extends AudioDeviceCallback {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            if (!deviceSnapshotSeen) {
                deviceSnapshotSeen = true;
                return;
            }

            requestRebuildForSinks(addedDevices);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            requestRebuildForSinks(removedDevices);
        }
    }
}
