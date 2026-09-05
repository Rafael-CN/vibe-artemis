package com.limelight.binding.video;

import com.limelight.preferences.PreferenceConfiguration;

/**
 * Qual caminho de saída a render thread do decoder usa em cada modo de ritmo de quadros.
 *
 * Isto era uma coleção de comparações soltas espalhadas pela MediaCodecDecoderRenderer, e uma
 * delas nem olhava o ritmo: o caminho "latest-only" ficava atrás da preferência de menor
 * atraso, que é outra coisa. O resultado é que Equilibrado, Suavidade máxima e Limitar FPS
 * tinham os quadros comidos por um caminho que só faz sentido em latência mínima — e como
 * nada disso era uma decisão nomeada, ninguém enxergava a contradição lendo o laço.
 *
 * São três perguntas, e a resposta de cada modo a elas é a tabela inteira. Sem Android
 * nenhum: dá para prender em teste que dois caminhos nunca valem para o mesmo modo.
 */
public final class FramePacingRoute {

    private FramePacingRoute() {}

    /**
     * Drenar tudo o que o decoder tem, jogar fora os atrasados e exibir só o mais novo, na
     * hora. É o comportamento de latência mínima e de mais nenhum: quem pediu suavidade não
     * quer que a render thread descarte quadros por conta própria.
     */
    public static boolean usesLatestOnly(int framePacing) {
        return framePacing == PreferenceConfiguration.FRAME_PACING_MIN_LATENCY;
    }

    /**
     * Enfileirar os quadros e deixar o Choreographer exibi-los alinhados ao vsync. Só o modo
     * Equilibrado faz isso, e é por isso que ele não pode passar pelo latest-only: um quadro
     * exibido fora da fila é um quadro que o Choreographer nunca vê.
     */
    public static boolean usesChoreographer(int framePacing) {
        return framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED;
    }

    /**
     * Exibir mesmo o quadro que chegou atrasado, em vez de soltá-lo sem renderizar. Suavidade
     * máxima e Limitar FPS existem exatamente para isso.
     */
    public static boolean neverDropsLateFrames(int framePacing) {
        return framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS
                || framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS;
    }
}
