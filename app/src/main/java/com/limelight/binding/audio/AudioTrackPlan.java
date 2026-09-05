package com.limelight.binding.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Como pedir a saída de áudio ao Android: qual máscara de canal, qual buffer, em que ordem
 * tentar — e quando uma falha de escrita significa "recrie a AudioTrack".
 *
 * Isto morava dentro de {@code AndroidAudioRenderer.setup()}, como um laço de quatro casos
 * com dois {@code switch} e dois {@code continue}, e por isso nunca teve teste. Duas coisas
 * ficaram erradas ali por anos sem ninguém notar: o caso de 4 canais mandava Centro e LFE
 * para as caixas traseiras, e a única saída quando o aparelho recusava a máscara pedida era
 * derrubar a conexão inteira — {@code setup()} devolve -2 e o {@code LiStartConnection}
 * falha, ou seja, o usuário não perde o surround, perde o stream.
 *
 * Tudo aqui é aritmética de inteiro. Nenhuma classe do Android é importada de propósito:
 * as constantes de máscara estão repetidas com o nome do original ao lado, porque é o valor
 * numérico que define o layout de canal na fronteira, e é ele que o teste precisa prender.
 */
public final class AudioTrackPlan {

    /**
     * Bits de canal do Android, na ordem de saída que o Limelight.h fixa: FL, FR, C, LFE,
     * BL, BR, SL, SR. O Android entrega os canais intercalados em ordem crescente de bit —
     * e essa ordem crescente é exatamente a mesma lista. Por isso a máscara de N canais é
     * o OU dos N primeiros bits, e não uma constante por contagem.
     */
    private static final int[] CHANNEL_BITS = {
            0x4,    // AudioFormat.CHANNEL_OUT_FRONT_LEFT
            0x8,    // AudioFormat.CHANNEL_OUT_FRONT_RIGHT
            0x10,   // AudioFormat.CHANNEL_OUT_FRONT_CENTER
            0x20,   // AudioFormat.CHANNEL_OUT_LOW_FREQUENCY
            0x40,   // AudioFormat.CHANNEL_OUT_BACK_LEFT
            0x80,   // AudioFormat.CHANNEL_OUT_BACK_RIGHT
            0x800,  // AudioFormat.CHANNEL_OUT_SIDE_LEFT
            0x1000, // AudioFormat.CHANNEL_OUT_SIDE_RIGHT
    };
    // Os bits 0x100, 0x200 e 0x400 (FRONT_LEFT_OF_CENTER, FRONT_RIGHT_OF_CENTER e
    // BACK_CENTER) ficam de fora de propósito: não existem na ordem do Limelight.h. Pular
    // um bit não quebra nada, porque o que define a intercalação é a ORDEM crescente dos
    // bits presentes, não a contiguidade deles.

    /** Devolvido por {@link #channelMaskFor(int)} quando a contagem não é representável. */
    public static final int NO_CHANNEL_MASK = 0;

    // Códigos de erro de AudioTrack.write(). Repetidos aqui em vez de importados para esta
    // classe continuar sem Android: são valores de ABI pública, congelados desde a API 21.
    private static final int ERROR_INVALID_OPERATION = -3;  // AudioTrack.ERROR_INVALID_OPERATION
    private static final int ERROR_DEAD_OBJECT = -6;        // AudioTrack.ERROR_DEAD_OBJECT

    /** Uma forma de pedir a AudioTrack. A cascata tenta uma por vez, na ordem da lista. */
    public static final class Attempt {
        /** Máscara posicional (CHANNEL_OUT_*) ou, se {@link #indexedMask}, máscara por índice. */
        public final int channelMask;
        /** true quando a máscara é de índice ({@code setChannelIndexMask}, API 24+). */
        public final boolean indexedMask;
        public final int bufferSizeInBytes;
        public final boolean lowLatency;

        Attempt(int channelMask, boolean indexedMask, int bufferSizeInBytes, boolean lowLatency) {
            this.channelMask = channelMask;
            this.indexedMask = indexedMask;
            this.bufferSizeInBytes = bufferSizeInBytes;
            this.lowLatency = lowLatency;
        }

        @Override
        public String toString() {
            return (indexedMask ? "index 0x" : "mask 0x") + Integer.toHexString(channelMask)
                    + " buffer " + bufferSizeInBytes + (lowLatency ? " low-latency" : "");
        }
    }

    private final int channelCount;
    private final int bytesPerFrame;
    private final List<Attempt> attempts;

    private AudioTrackPlan(int channelCount, int bytesPerFrame, List<Attempt> attempts) {
        this.channelCount = channelCount;
        this.bytesPerFrame = bytesPerFrame;
        this.attempts = Collections.unmodifiableList(attempts);
    }

    /**
     * Máscara posicional para uma contagem de canais, ou {@link #NO_CHANNEL_MASK} se o
     * protocolo não pode produzi-la (o Limelight limita a 8 canais).
     */
    public static int channelMaskFor(int channelCount) {
        if (channelCount < 2 || channelCount > CHANNEL_BITS.length) {
            return NO_CHANNEL_MASK;
        }

        int mask = 0;
        for (int i = 0; i < channelCount; i++) {
            mask |= CHANNEL_BITS[i];
        }
        return mask;
    }

    /**
     * Monta a cascata de tentativas.
     *
     * @param channelCount           canais negociados com o host
     * @param samplesPerFrame        samples POR CANAL em cada pacote decodificado
     * @param sampleRate             taxa do stream (o Moonlight fixa 48 kHz)
     * @param nativeOutputSampleRate taxa da saída ativa do aparelho, agora
     * @param minBufferSizeInBytes   o que o AudioTrack.getMinBufferSize recomenda (pode vir
     *                               negativo quando ele recusa a máscara — é tratado)
     * @param audioFxEnabled         a sessão de efeitos do sistema está ligada
     * @param indexedMaskSupported   API 24+, onde existe setChannelIndexMask
     * @return o plano, ou null se a contagem de canais não é representável
     */
    public static AudioTrackPlan resolve(int channelCount, int samplesPerFrame, int sampleRate,
                                         int nativeOutputSampleRate, int minBufferSizeInBytes,
                                         boolean audioFxEnabled, boolean indexedMaskSupported) {
        int channelMask = channelMaskFor(channelCount);
        if (channelMask == NO_CHANNEL_MASK || samplesPerFrame <= 0) {
            return null;
        }

        int bytesPerFrame = channelCount * samplesPerFrame * 2;

        // O buffer minúsculo é dois frames. Está abaixo do mínimo recomendado de propósito:
        // muitos aparelhos aceitam e a latência cai. Quando não aceitam, a cascata desce.
        int smallBuffer = bytesPerFrame * 2;

        // getMinBufferSize devolve -1/-2 quando recusa a combinação; o max() já cobre isso.
        int largeBuffer = Math.max(minBufferSizeInBytes, smallBuffer);
        largeBuffer = ((largeBuffer + (bytesPerFrame - 1)) / bytesPerFrame) * bytesPerFrame;

        // Baixa latência exige que o hardware já esteja rodando na taxa do stream (senão o
        // resampler do sistema anula o ganho) e é incompatível com o pipeline de efeitos.
        boolean lowLatencyUsable = !audioFxEnabled && nativeOutputSampleRate == sampleRate;

        List<Attempt> attempts = new ArrayList<>(5);
        if (lowLatencyUsable) {
            attempts.add(new Attempt(channelMask, false, smallBuffer, true));
            attempts.add(new Attempt(channelMask, false, largeBuffer, true));
        }
        attempts.add(new Attempt(channelMask, false, smallBuffer, false));
        attempts.add(new Attempt(channelMask, false, largeBuffer, false));

        // Última saída para surround: máscara por índice. O aparelho que recusa
        // CHANNEL_OUT_5POINT1 costuma aceitar "seis canais quaisquer" e faz o downmix ele
        // mesmo. O PCM é o mesmo, intercalado na mesma ordem — só o rótulo muda. Sem isto,
        // um aparelho que não expressa o layout pedido derruba a conexão inteira em vez de
        // simplesmente tocar o áudio.
        if (indexedMaskSupported && channelCount > 2) {
            attempts.add(new Attempt((1 << channelCount) - 1, true, largeBuffer, false));
        }

        return new AudioTrackPlan(channelCount, bytesPerFrame, attempts);
    }

    public List<Attempt> attempts() {
        return attempts;
    }

    public int channelCount() {
        return channelCount;
    }

    public int bytesPerFrame() {
        return bytesPerFrame;
    }

    /**
     * O retorno de {@code AudioTrack.write()} pede que a track seja recriada?
     *
     * ERROR_DEAD_OBJECT é o que o servidor de áudio devolve quando reinicia ou quando
     * invalida a track numa troca de rota — a documentação manda recriar, e é o caminho
     * pelo qual o áudio hoje some para o resto da sessão sem nada no log. ERROR_INVALID_
     * OPERATION significa track em estado inválido, que também só se resolve recriando.
     * ERROR_BAD_VALUE fica de fora: esse é argumento errado nosso, e recriar não conserta.
     */
    public static boolean requiresRebuild(int writeResult) {
        return writeResult == ERROR_DEAD_OBJECT || writeResult == ERROR_INVALID_OPERATION;
    }
}
