package com.limelight.binding.video;

import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Prende a tabela de roteamento do ritmo de quadros.
 *
 * O defeito que motivou isto: o caminho "latest-only" da render thread ficava atrás da
 * preferência de menor atraso, e não do modo de ritmo. Como o padrão dessa preferência é
 * desligado, o caminho rodava para quem NÃO pediu latência mínima — comia os quadros de
 * Equilibrado, Suavidade máxima e Limitar FPS, e o Choreographer só via a sobra. O erro
 * atravessou revisão porque não havia lugar nenhum onde a tabela estivesse escrita inteira.
 *
 * Aritmética de inteiro, sem Android: os quatro modos são constantes de compilação.
 */
public class FramePacingRouteTest {

    private static final int[] TODOS_OS_MODOS = {
            PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            PreferenceConfiguration.FRAME_PACING_BALANCED,
            PreferenceConfiguration.FRAME_PACING_CAP_FPS,
            PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS,
    };

    @Test
    public void latenciaMinima_usaLatestOnly() {
        assertTrue(FramePacingRoute.usesLatestOnly(PreferenceConfiguration.FRAME_PACING_MIN_LATENCY));
    }

    @Test
    public void nenhumOutroModo_usaLatestOnly() {
        // Este é o bug B20 inteiro: qualquer um destes voltar a true significa que o
        // Choreographer e o controle de descarte perderam os quadros de novo.
        assertFalse("Equilibrado depende do Choreographer, não do latest-only",
                FramePacingRoute.usesLatestOnly(PreferenceConfiguration.FRAME_PACING_BALANCED));
        assertFalse("Suavidade máxima não pode descartar quadro",
                FramePacingRoute.usesLatestOnly(PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS));
        assertFalse("Limitar FPS não pode descartar quadro",
                FramePacingRoute.usesLatestOnly(PreferenceConfiguration.FRAME_PACING_CAP_FPS));
    }

    @Test
    public void somenteEquilibrado_usaChoreographer() {
        assertTrue(FramePacingRoute.usesChoreographer(PreferenceConfiguration.FRAME_PACING_BALANCED));
        assertFalse(FramePacingRoute.usesChoreographer(PreferenceConfiguration.FRAME_PACING_MIN_LATENCY));
        assertFalse(FramePacingRoute.usesChoreographer(PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS));
        assertFalse(FramePacingRoute.usesChoreographer(PreferenceConfiguration.FRAME_PACING_CAP_FPS));
    }

    @Test
    public void suavidadeELimiteDeFps_naoDescartamQuadroAtrasado() {
        assertTrue(FramePacingRoute.neverDropsLateFrames(PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS));
        assertTrue(FramePacingRoute.neverDropsLateFrames(PreferenceConfiguration.FRAME_PACING_CAP_FPS));
        assertFalse(FramePacingRoute.neverDropsLateFrames(PreferenceConfiguration.FRAME_PACING_MIN_LATENCY));
        assertFalse(FramePacingRoute.neverDropsLateFrames(PreferenceConfiguration.FRAME_PACING_BALANCED));
    }

    @Test
    public void nenhumModo_cabeEmDoisCaminhosDeSaida() {
        // "Um caminho de saída por modo de ritmo" é a propriedade que o conserto comprou.
        // Dois caminhos ativos no mesmo modo é o que produzia o micro-stutter: os dois
        // disputavam o mesmo dequeueOutputBuffer e o vencedor variava a cada volta do laço.
        for (int modo : TODOS_OS_MODOS) {
            int caminhos = 0;
            if (FramePacingRoute.usesLatestOnly(modo)) caminhos++;
            if (FramePacingRoute.usesChoreographer(modo)) caminhos++;
            if (FramePacingRoute.neverDropsLateFrames(modo)) caminhos++;

            assertTrue("modo " + modo + " respondeu a " + caminhos + " caminhos de saída",
                    caminhos == 1);
        }
    }
}
