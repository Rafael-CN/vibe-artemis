package com.limelight.preferences;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.utils.UiHelper;

/**
 * Ajustes básica — Plano B / F4 (docs/planos/B-CONSOLE.md §F4). Casca nova sobre as
 * MESMAS chaves de {@link PreferenceConfiguration} e da árvore de sempre
 * ({@code res/xml/preferences.xml}); esta Activity nunca introduz um armazenamento
 * paralelo. O cartão "Avançado" abre {@link StreamSettings}, intocada — é assim que a
 * árvore inteira de 136 preferências continua alcançável ("esconder pode, apagar não").
 *
 * <p>Lê e grava direto em {@code PreferenceManager.getDefaultSharedPreferences}, o
 * mesmo arquivo que {@link StreamSettings.SettingsFragment#getPrefs()} usa — não o
 * overlay de perfil ({@code ProfilesManager#getOverlayingSharedPreferences}) — para
 * garantir que esta tela e a Avançado sempre concordem sobre o valor atual de cada
 * chave. É o comportamento que a árvore de hoje já tinha (a UI de settings nunca
 * respeitou o perfil ativo; só a edição de perfil, via {@code EditProfileActivity},
 * usa um SharedPreferences próprio).</p>
 */
public class StreamSettingsBasico extends AppCompatActivity {

    public static final String EXTRA_PC_NAME = "console_pc_name";
    public static final String EXTRA_PC_ADDRESS = "console_pc_address";

    // Chaves literais (não as constantes private de PreferenceConfiguration): a spec da
    // fatia (B-CONSOLE.md §F4) já dá essas chaves de próprio punho, e usá-las aqui
    // como literais evita alargar a visibilidade de uma classe com fan-in 33 só para
    // esta tela — a única exceção é CHECKBOX_COMPOSE_BAR, que esta fatia introduziu e
    // já nasceu package-private.
    private static final String KEY_RESOLUTION = "list_resolution";
    private static final String KEY_FPS = "list_fps";
    private static final String KEY_BITRATE = "seekbar_bitrate_kbps";
    private static final String KEY_VIDEO_FORMAT = "video_format";
    private static final String KEY_HDR = "checkbox_enable_hdr";
    private static final String KEY_AUDIO_CONFIG = "list_audio_config";
    private static final String KEY_MOUSE_MODE = "mouse_mode_list";
    private static final String KEY_ONSCREEN_CONTROLS = "checkbox_show_onscreen_controls";

    private static final int BITRATE_MIN_KBPS = 500;
    private static final int BITRATE_MAX_KBPS = 300000;
    private static final int BITRATE_STEP_KBPS = 500;

    private SharedPreferences prefs;
    private String nativeResolutionValue;

    private TextView pcAtualTexto;

    private TextView resolucaoValorMono;
    private LinearLayout resolucaoSegmentos;
    private TextView[] segsResolucao;
    private TextView taxaValorMono;
    private LinearLayout taxaSegmentos;
    private TextView[] segsTaxa;
    private TextView bitrateValor;
    private SeekBar bitrateSlider;
    private TextView codecValorMono;
    private LinearLayout codecSegmentos;
    private TextView[] segsCodec;
    private TextView hdrLegenda;
    private SwitchCompat hdrSwitch;
    private TextView audioValor;
    private TextView modoToqueValor;
    private LinearLayout modoToqueSegmentos;
    private TextView[] segsModoToque;
    private SwitchCompat barraTextoSwitch;
    private SwitchCompat controlesTelaSwitch;
    private TextView avancadoLegenda;

    private boolean hdrSuportado;

    // Guarda os três switches (HDR/Barra de texto/Controles na tela) contra o próprio
    // setChecked(...) de renderizarTudo(): CompoundButton dispara onCheckedChanged tanto
    // para clique do usuário (toque OU D-pad/Enter) quanto para setChecked() por código, e
    // View#isPressed() não é confiável para diferenciar os dois — um clique disparado por
    // KEYCODE_DPAD_CENTER troca o estado visual sem deixar isPressed()==true no momento em
    // que o listener roda, então a gravação nunca acontecia por D-pad (bug encontrado na
    // verificação desta fatia). Um flag explícito ligado só durante a atualização
    // programática resolve para os dois casos de entrada.
    private boolean atualizandoProgramaticamente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiHelper.setLocale(this);

        // Garante que toda chave já tenha o defaultValue do XML gravado em disco antes
        // da primeira leitura — mesmo cuidado que PcView#initializeViews toma para a
        // árvore de hoje.
        androidx.preference.PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_stream_settings_basico);
        UiHelper.notifyNewRootView(this);

        encontrarViews();
        configurarCliques();

        avancadoLegenda.setText(getResources().getBoolean(R.bool.console_avancado_legenda_curta)
                ? R.string.console_ajuste_avancado_legenda_curta
                : R.string.console_ajuste_avancado_legenda);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Volta do card Avançado (StreamSettings) ou do diálogo de Áudio pode ter
        // mudado qualquer chave — reler tudo é mais simples e barato do que rastrear
        // qual controle mexeu em qual outro.
        hdrSuportado = calcularSuporteHdr();
        renderizarTudo();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        renderizarTudo();
    }

    private void encontrarViews() {
        findViewById(R.id.consoleAjustesVoltar).setOnClickListener(v -> finish());

        pcAtualTexto = findViewById(R.id.consoleAjustesPcAtual);

        resolucaoValorMono = findViewById(R.id.consoleResolucaoValorMono);
        resolucaoSegmentos = findViewById(R.id.consoleResolucaoSegmentos);
        segsResolucao = new TextView[]{
                findViewById(R.id.consoleSeg1080p),
                findViewById(R.id.consoleSeg1440p),
                findViewById(R.id.consoleSegNativa)
        };
        taxaValorMono = findViewById(R.id.consoleTaxaValorMono);
        taxaSegmentos = findViewById(R.id.consoleTaxaSegmentos);
        segsTaxa = new TextView[]{
                findViewById(R.id.consoleSeg60),
                findViewById(R.id.consoleSeg90),
                findViewById(R.id.consoleSeg120)
        };
        bitrateValor = findViewById(R.id.consoleBitrateValor);
        bitrateSlider = findViewById(R.id.consoleBitrateSlider);
        codecValorMono = findViewById(R.id.consoleCodecValorMono);
        codecSegmentos = findViewById(R.id.consoleCodecSegmentos);
        segsCodec = new TextView[]{
                findViewById(R.id.consoleSegCodecAuto),
                findViewById(R.id.consoleSegCodecHevc),
                findViewById(R.id.consoleSegCodecAv1)
        };
        hdrLegenda = findViewById(R.id.consoleHdrLegenda);
        hdrSwitch = findViewById(R.id.consoleHdrSwitch);
        audioValor = findViewById(R.id.consoleAudioValor);
        modoToqueValor = findViewById(R.id.consoleModoToqueValor);
        modoToqueSegmentos = findViewById(R.id.consoleModoToqueSegmentos);
        segsModoToque = new TextView[]{
                findViewById(R.id.consoleSegToqueDireto),
                findViewById(R.id.consoleSegTouchpad)
        };
        barraTextoSwitch = findViewById(R.id.consoleBarraTextoSwitch);
        controlesTelaSwitch = findViewById(R.id.consoleControlesTelaSwitch);
        avancadoLegenda = findViewById(R.id.consoleAvancadoLegenda);

        String pcName = getIntent().getStringExtra(EXTRA_PC_NAME);
        String pcAddress = getIntent().getStringExtra(EXTRA_PC_ADDRESS);
        if (pcName != null && pcAddress != null) {
            pcAtualTexto.setText(getString(R.string.console_nome_e_estado, pcName, pcAddress));
            pcAtualTexto.setVisibility(android.view.View.VISIBLE);
        } else {
            pcAtualTexto.setVisibility(android.view.View.GONE);
        }
    }

    private void configurarCliques() {
        bitrateSlider.setMax((BITRATE_MAX_KBPS - BITRATE_MIN_KBPS) / BITRATE_STEP_KBPS);
        bitrateSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int kbps = BITRATE_MIN_KBPS + progress * BITRATE_STEP_KBPS;
                bitrateValor.setText(getString(R.string.console_mbps_valor, Math.round(kbps / 1000f)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int kbps = BITRATE_MIN_KBPS + seekBar.getProgress() * BITRATE_STEP_KBPS;
                prefs.edit().putInt(KEY_BITRATE, kbps).apply();
            }
        });

        hdrSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!atualizandoProgramaticamente) {
                prefs.edit().putBoolean(KEY_HDR, isChecked).apply();
            }
        });

        barraTextoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!atualizandoProgramaticamente) {
                prefs.edit().putBoolean(PreferenceConfiguration.CHECKBOX_COMPOSE_BAR, isChecked).apply();
            }
        });

        controlesTelaSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!atualizandoProgramaticamente) {
                prefs.edit().putBoolean(KEY_ONSCREEN_CONTROLS, isChecked).apply();
            }
        });

        findViewById(R.id.consoleAudioLinha).setOnClickListener(v -> mostrarDialogoDeAudio());

        findViewById(R.id.consoleAvancadoCard).setOnClickListener(v ->
                startActivity(new Intent(StreamSettingsBasico.this, StreamSettings.class)));
    }

    private void renderizarTudo() {
        PreferenceConfiguration cfg = PreferenceConfiguration.readPreferences(this, prefs);
        nativeResolutionValue = calcularResolucaoNativa();

        renderizarResolucao(cfg);
        renderizarTaxa(cfg);
        renderizarBitrate(cfg);
        renderizarCodec(cfg);
        atualizandoProgramaticamente = true;
        try {
            renderizarHdr(cfg);
            renderizarBarraTexto(cfg);
            renderizarControlesTela(cfg);
        } finally {
            atualizandoProgramaticamente = false;
        }
        renderizarAudio(cfg);
        renderizarModoToque();
    }

    // ================= Resolução =================

    private void renderizarResolucao(PreferenceConfiguration cfg) {
        String atual = prefs.getString(KEY_RESOLUTION, PreferenceConfiguration.DEFAULT_RESOLUTION);
        String[] valores = {"1920x1080", "2560x1440", nativeResolutionValue};
        configurarSegmentado(resolucaoSegmentos, resolucaoValorMono, segsResolucao, valores, atual, valor -> {
            prefs.edit().putString(KEY_RESOLUTION, valor).apply();
            renderizarTudo();
        }, atual /* valor cru exibido em mono quando não bate com nenhum segmento */);
    }

    // ================= Taxa =================

    private void renderizarTaxa(PreferenceConfiguration cfg) {
        String atual = prefs.getString(KEY_FPS, PreferenceConfiguration.DEFAULT_FPS);
        String[] valores = {"60", "90", "120"};
        configurarSegmentado(taxaSegmentos, taxaValorMono, segsTaxa, valores, atual, valor -> {
            prefs.edit().putString(KEY_FPS, valor).apply();
            renderizarTudo();
        }, getString(R.string.console_hz_valor, atual));
    }

    // ================= Taxa de bits =================

    private void renderizarBitrate(PreferenceConfiguration cfg) {
        int kbps = cfg.bitrate;
        int progress = Math.round((kbps - BITRATE_MIN_KBPS) / (float) BITRATE_STEP_KBPS);
        progress = Math.max(0, Math.min(bitrateSlider.getMax(), progress));
        bitrateSlider.setProgress(progress);
        bitrateValor.setText(getString(R.string.console_mbps_valor, Math.round(kbps / 1000f)));
    }

    // ================= Codec =================

    private void renderizarCodec(PreferenceConfiguration cfg) {
        String atual = prefs.getString(KEY_VIDEO_FORMAT, "auto");
        String[] valores = {"auto", "forceh265", "forceav1"};
        // Rótulo humano (não mono) quando o valor é "neverh265" (Forçar H264) — não é um
        // dado técnico como IP/Hz, é uma opção nomeada, então usa o mesmo estilo
        // secundário do resto da linha em vez de ConsoleText.Mono.
        String rotuloDoValorAtual = indexOf(new String[]{"auto", "forceh265", "forceav1", "neverh265"}, atual) == 3
                ? getString(R.string.videoformat_h264always)
                : atual;
        configurarSegmentado(codecSegmentos, codecValorMono, segsCodec, valores, atual, valor -> {
            prefs.edit().putString(KEY_VIDEO_FORMAT, valor).apply();
            renderizarTudo();
        }, rotuloDoValorAtual);
    }

    // ================= HDR =================

    private boolean calcularSuporteHdr() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Display display = getWindowManager().getDefaultDisplay();
        Display.HdrCapabilities hdrCaps = display.getHdrCapabilities();
        boolean temHdr10 = false;
        if (hdrCaps != null) {
            for (int tipo : hdrCaps.getSupportedHdrTypes()) {
                if (tipo == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                    temHdr10 = true;
                    break;
                }
            }
        }
        if (!temHdr10 || PreferenceConfiguration.isShieldAtvFirmwareWithBrokenHdr()) {
            return false;
        }

        try {
            MediaCodecHelper.initialize(this, GlPreferences.readPreferences(this).glRenderer);
            MediaCodecInfo hevcDecoder = MediaCodecHelper.findProbableSafeDecoder("video/hevc", -1);
            return hevcDecoder != null;
        } catch (Exception e) {
            // Defensivo: um decoder relatando informação inconsistente não pode derrubar
            // a tela de Ajustes — só perde a distinção "suportado vs. não".
            LimeLog.warning("Falha ao consultar decoder HEVC para gate de HDR: " + e);
            return false;
        }
    }

    private void renderizarHdr(PreferenceConfiguration cfg) {
        hdrSwitch.setEnabled(hdrSuportado);
        hdrSwitch.setChecked(hdrSuportado && cfg.enableHdr);
        hdrLegenda.setVisibility(hdrSuportado ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    // ================= Áudio =================

    private void renderizarAudio(PreferenceConfiguration cfg) {
        String[] nomes = getResources().getStringArray(R.array.audio_config_names);
        String[] valores = getResources().getStringArray(R.array.audio_config_values);
        String atual = prefs.getString(KEY_AUDIO_CONFIG, "2");
        int i = indexOf(valores, atual);
        audioValor.setText(i >= 0 ? nomes[i] : atual);
    }

    private void mostrarDialogoDeAudio() {
        String[] nomes = getResources().getStringArray(R.array.audio_config_names);
        String[] valores = getResources().getStringArray(R.array.audio_config_values);
        String atual = prefs.getString(KEY_AUDIO_CONFIG, "2");
        int selecionado = indexOf(valores, atual);

        new AlertDialog.Builder(this, R.style.ConsoleDialog)
                .setTitle(R.string.console_ajuste_audio)
                .setSingleChoiceItems(nomes, selecionado, (dialog, which) -> {
                    prefs.edit().putString(KEY_AUDIO_CONFIG, valores[which]).apply();
                    dialog.dismiss();
                    renderizarTudo();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ================= Modo do toque =================

    private void renderizarModoToque() {
        String atual = prefs.getString(KEY_MOUSE_MODE, "0");
        String[] valores = {"0", "2"};
        String[] nomesCompletos = getResources().getStringArray(R.array.mouse_mode_names);
        String[] valoresCompletos = getResources().getStringArray(R.array.mouse_mode_values);
        int i = indexOf(valoresCompletos, atual);
        String rotuloAtual = i >= 0 ? nomesCompletos[i] : atual;

        configurarSegmentado(modoToqueSegmentos, modoToqueValor, segsModoToque, valores, atual, valor -> {
            prefs.edit().putString(KEY_MOUSE_MODE, valor).apply();
            renderizarTudo();
        }, rotuloAtual);
    }

    // ================= Barra de texto / Controles na tela =================

    private void renderizarBarraTexto(PreferenceConfiguration cfg) {
        barraTextoSwitch.setChecked(cfg.composeBarEnabled);
    }

    private void renderizarControlesTela(PreferenceConfiguration cfg) {
        controlesTelaSwitch.setChecked(cfg.onscreenController);
    }

    // ================= utilidades =================

    private interface OnSegmentoEscolhido {
        void onEscolhido(String valor);
    }

    /**
     * Liga um controle segmentado JÁ DECLARADO no XML (uma linha, um número fixo e
     * conhecido de opções — não um adapter). Quando {@code valorAtual} não bate com
     * nenhum item de {@code valores}, nenhum segmento fica marcado e {@code container}
     * desaparece em favor de {@code campoMono}, que mostra {@code valorCruParaMono} —
     * a regra de "não force o valor para o segmento mais próximo" (B-CONSOLE.md §F4).
     *
     * <p>Os próprios {@code TextView} de cada opção são estáticos (ids fixos no layout,
     * texto e estilo já no XML); esta função só liga estado (activated) e clique — nunca
     * infla nem remove View nenhuma. Uma versão anterior inflava um item por opção
     * dinamicamente dentro de um LinearLayout wrap_content aninhado em outro
     * wrap_content com peso — o Robolectric em modo de gráficos nativo media só o
     * PRIMEIRO item inflado após a remontagem (os demais ficavam com largura zero,
     * mesmo com childCount correto), então a troca para Views fixas no XML não é só
     * estilo, é o que corrigiu esse defeito.</p>
     */
    private void configurarSegmentado(LinearLayout container, TextView campoMono, TextView[] segmentos,
                                       String[] valores, String valorAtual,
                                       OnSegmentoEscolhido onEscolhido, String valorCruParaMono) {
        int selecionado = indexOf(valores, valorAtual);

        if (selecionado < 0) {
            container.setVisibility(android.view.View.GONE);
            campoMono.setVisibility(android.view.View.VISIBLE);
            campoMono.setText(valorCruParaMono);
            return;
        }

        campoMono.setVisibility(android.view.View.GONE);
        container.setVisibility(android.view.View.VISIBLE);

        for (int i = 0; i < segmentos.length; i++) {
            final String valor = valores[i];
            segmentos[i].setActivated(i == selecionado);
            segmentos[i].setOnClickListener(v -> onEscolhido.onEscolhido(valor));
        }
    }

    private static int indexOf(String[] array, String valor) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(valor)) {
                return i;
            }
        }
        return -1;
    }

    private String calcularResolucaoNativa() {
        Display display = getWindowManager().getDefaultDisplay();
        int width, height;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = display.getMode();
            width = mode.getPhysicalWidth();
            height = mode.getPhysicalHeight();
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        }
        int w = Math.max(width, height);
        int h = Math.min(width, height);
        return w + "x" + h;
    }
}
