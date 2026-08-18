package com.limelight.ui;

import android.app.Activity;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * As superfícies da sessão que não são a ilha: a tela de conectar e a folha de ajustes.
 *
 * As duas vivem por cima do vídeo e nenhuma delas desmonta o stream — a folha é uma View,
 * não uma Activity, justamente para o aparelho não voltar para retrato e a sessão não
 * cair no caminho.
 */
public class VidroSession {

    /**
     * Os três passos da conexão, com o quanto da barra cada um preenche.
     *
     * Os percentuais são do protótipo; os TEMPOS não são. Lá eles rodavam em 800/1600/
     * 2500 ms porque o protótipo precisava andar sozinho. Aqui cada passo espera o evento
     * real da máquina de estados — uma barra que promete e não cumpre é pior que barra
     * nenhuma.
     */
    private static final int PROGRESS_TALKING = 18;
    private static final int PROGRESS_RESERVING = 62;
    private static final int PROGRESS_VIDEO = 100;

    private final Activity activity;
    private final PreferenceConfiguration prefConfig;

    private final View connecting;
    private final TextView connectingApp;
    private final TextView connectingStep;
    private final android.widget.ProgressBar connectingBar;
    private final VidroLightView connectingLight;

    private final View sheetScrim;
    private final View sheet;

    private int stagesSeen;

    public VidroSession(Activity activity, PreferenceConfiguration prefConfig) {
        this.activity = activity;
        this.prefConfig = prefConfig;

        connecting = activity.findViewById(R.id.vidroConnecting);
        connectingApp = activity.findViewById(R.id.vidroConnectingApp);
        connectingStep = activity.findViewById(R.id.vidroConnectingStep);
        connectingBar = activity.findViewById(R.id.vidroConnectingBar);
        connectingLight = activity.findViewById(R.id.vidroConnectingLight);

        sheetScrim = activity.findViewById(R.id.vidroSheetScrim);
        sheet = activity.findViewById(R.id.vidroSheet);

        wireSheet();
    }

    // ==================== conectando ====================

    public void showConnecting(String appName, String hostName) {
        if (connecting == null) {
            return;
        }

        stagesSeen = 0;

        if (connectingApp != null) {
            connectingApp.setText(appName != null ? appName : "");
        }
        if (connectingStep != null) {
            connectingStep.setText(activity.getString(R.string.vidro_step_talking,
                    hostName != null ? hostName : ""));
        }
        if (connectingBar != null) {
            connectingBar.setProgress(PROGRESS_TALKING);
        }
        if (connectingLight != null) {
            // A luz sobe junto com o progresso: ela é o sinal de que há algo vivo do
            // outro lado, e é por isso que não existe spinner aqui.
            connectingLight.setIntensity(VidroLightView.INTENSITY_ASLEEP, false);
        }

        connecting.setVisibility(View.VISIBLE);
    }

    /**
     * Cada estágio que a conexão anuncia empurra a barra um degrau. Não olhamos o nome do
     * estágio: ele vem do moonlight-common em inglês e muda entre versões, então contar
     * quantos já passaram é mais estável que casar string.
     */
    public void onStageStarting(String stage) {
        if (connecting == null || connecting.getVisibility() != View.VISIBLE) {
            return;
        }

        stagesSeen++;

        if (stagesSeen >= 2 && connectingStep != null && connectingBar != null) {
            connectingStep.setText(R.string.vidro_step_reserving);
            connectingBar.setProgress(PROGRESS_RESERVING);

            if (connectingLight != null) {
                connectingLight.setIntensity(VidroLightView.INTENSITY_AWAKE, true);
            }
        }
    }

    public void onConnected() {
        if (connecting == null) {
            return;
        }

        if (connectingStep != null) {
            connectingStep.setText(R.string.vidro_step_video);
        }
        if (connectingBar != null) {
            connectingBar.setProgress(PROGRESS_VIDEO);
        }

        connecting.setVisibility(View.GONE);
    }

    public View getConnectingCancel() {
        return activity.findViewById(R.id.vidroConnectingCancel);
    }

    // ==================== folha de ajustes ====================

    public boolean isSheetOpen() {
        return sheetScrim != null && sheetScrim.getVisibility() == View.VISIBLE;
    }

    public void openSheet() {
        if (sheetScrim == null) {
            return;
        }

        refreshSheet();
        sheetScrim.setVisibility(View.VISIBLE);

        if (sheet != null) {
            // Folha sobe · 240 ms. Só translate e alpha: animar largura pediria layout por
            // quadro, e cada quadro gasto aqui sai do vídeo.
            sheet.setTranslationX(sheet.getWidth() > 0 ? sheet.getWidth() : 352);
            sheet.animate().translationX(0).setDuration(240).start();
        }
    }

    public void closeSheet() {
        if (sheetScrim == null) {
            return;
        }

        if (sheet == null) {
            sheetScrim.setVisibility(View.GONE);
            return;
        }

        // Folha desce · 180 ms. Sair é mais rápido que entrar: quem fecha já decidiu.
        sheet.animate()
                .translationX(sheet.getWidth())
                .setDuration(180)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        sheetScrim.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void wireSheet() {
        if (sheetScrim == null) {
            return;
        }

        View close = activity.findViewById(R.id.vidroSheetClose);
        if (close != null) {
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeSheet();
                }
            });
        }

        // Tocar fora fecha; tocar na folha não atravessa.
        sheetScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSheet();
            }
        });
        if (sheet != null) {
            sheet.setClickable(true);
        }

        bindBitrate(R.id.vidroBitrate20, 20000);
        bindBitrate(R.id.vidroBitrate40, 40000);
        bindBitrate(R.id.vidroBitrate60, 60000);

        // Ritmo de quadros e modo do mouse valem AGORA, ao contrário da taxa de bits:
        // os dois são lidos a cada quadro e a cada evento, não na hora de conectar.
        bindPacing(R.id.vidroPacingSmooth, PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS);
        bindPacing(R.id.vidroPacingBalanced, PreferenceConfiguration.FRAME_PACING_BALANCED);
        bindPacing(R.id.vidroPacingLatency, PreferenceConfiguration.FRAME_PACING_MIN_LATENCY);

        bindMouseMode(R.id.vidroMouseMultitouch, false);
        bindMouseMode(R.id.vidroMouseTrackpad, true);

        View numbers = activity.findViewById(R.id.vidroNumbersRow);
        final Switch numbersSwitch = activity.findViewById(R.id.vidroNumbersSwitch);
        if (numbers != null && numbersSwitch != null) {
            numbers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    numbersSwitch.setChecked(!numbersSwitch.isChecked());
                    setPerformanceOverlayVisible(numbersSwitch.isChecked());
                }
            });
        }
    }

    private void bindPacing(final int viewId, final int pacing) {
        View chip = activity.findViewById(viewId);
        if (chip == null) {
            return;
        }

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefConfig.framePacing = pacing;
                refreshSheet();
            }
        });
    }

    private void bindMouseMode(final int viewId, final boolean trackpad) {
        View chip = activity.findViewById(viewId);
        if (chip == null) {
            return;
        }

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefConfig.touchscreenTrackpad = trackpad;
                refreshSheet();
            }
        });
    }

    private void bindBitrate(final int viewId, final int kbps) {
        View chip = activity.findViewById(viewId);
        if (chip == null) {
            return;
        }

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefConfig.bitrate = kbps;
                PreferenceConfiguration.writeBitrate(activity, kbps);
                refreshSheet();
            }
        });
    }

    /**
     * O estado dos chips vem da preferência, não de memória própria — a folha pode abrir
     * depois de alguém ter mudado a mesma coisa pelo menu antigo.
     */
    private void refreshSheet() {
        select(R.id.vidroBitrate20, prefConfig.bitrate <= 25000);
        select(R.id.vidroBitrate40, prefConfig.bitrate > 25000 && prefConfig.bitrate <= 50000);
        select(R.id.vidroBitrate60, prefConfig.bitrate > 50000);

        select(R.id.vidroPacingSmooth,
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS);
        select(R.id.vidroPacingBalanced,
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED);
        select(R.id.vidroPacingLatency,
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MIN_LATENCY);

        select(R.id.vidroMouseMultitouch, !prefConfig.touchscreenTrackpad);
        select(R.id.vidroMouseTrackpad, prefConfig.touchscreenTrackpad);

        Switch numbersSwitch = activity.findViewById(R.id.vidroNumbersSwitch);
        if (numbersSwitch != null) {
            numbersSwitch.setChecked(isPerformanceOverlayVisible());
        }
    }

    private void select(int viewId, boolean selected) {
        View chip = activity.findViewById(viewId);
        if (chip != null) {
            chip.setSelected(selected);
        }
    }

    // "Números na tela" é o mesmo Detalhes do Moonlight, só com nome que diz o que faz.
    private boolean isPerformanceOverlayVisible() {
        View overlay = activity.findViewById(R.id.performanceOverlay);
        View lite = activity.findViewById(R.id.performanceOverlayLite);
        return (overlay != null && overlay.getVisibility() == View.VISIBLE)
                || (lite != null && lite.getVisibility() == View.VISIBLE);
    }

    private void setPerformanceOverlayVisible(boolean visible) {
        View lite = activity.findViewById(R.id.performanceOverlayLite);
        if (lite != null) {
            lite.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
