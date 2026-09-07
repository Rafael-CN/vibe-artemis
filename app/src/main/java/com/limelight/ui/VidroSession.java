package com.limelight.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.limelight.R;

/**
 * A tela de conectar da sessão.
 *
 * Vive por cima do vídeo e não desmonta o stream — é uma View, não uma Activity,
 * justamente para o aparelho não voltar para retrato e a sessão não cair no caminho.
 *
 * Até a F2 (Plano B) esta classe também hospedava a folha de ajustes em sessão
 * (vidro_session_sheet.xml — taxa de bits, ritmo de quadros, modo do mouse, números
 * na tela). A folha foi aposentada junto com a ilha: a guia da sessão
 * (console_guia.xml, GameMenu) cobre "modo do toque" e "estatísticas", e taxa de
 * bits/ritmo de quadros não têm bloco equivalente na guia — ver
 * docs/planos/console/relatorios/F2-implementacao.md.
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

    private final View connecting;
    private final TextView connectingApp;
    private final TextView connectingStep;
    private final android.widget.ProgressBar connectingBar;

    /**
     * Ponto de estado simples (B-CONSOLE.md §F5 — "acento = ativo"), não mais a luz
     * radial VidroLightView. Uma View comum de fundo trocável: vidro_dot_off (apagado)
     * até o segundo estágio da conexão, vidro_dot_on (acento) dali em diante.
     */
    private final View connectingLight;

    private int stagesSeen;

    public VidroSession(Activity activity) {
        this.activity = activity;

        connecting = activity.findViewById(R.id.vidroConnecting);
        connectingApp = activity.findViewById(R.id.vidroConnectingApp);
        connectingStep = activity.findViewById(R.id.vidroConnectingStep);
        connectingBar = activity.findViewById(R.id.vidroConnectingBar);
        connectingLight = activity.findViewById(R.id.vidroConnectingLight);
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
            // Apagado até o segundo estágio — o ponto acende junto com o progresso,
            // sinal de que há algo vivo do outro lado; é por isso que não existe
            // spinner aqui.
            connectingLight.setBackgroundResource(R.drawable.vidro_dot_off);
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
                connectingLight.setBackgroundResource(R.drawable.vidro_dot_on);
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
}
