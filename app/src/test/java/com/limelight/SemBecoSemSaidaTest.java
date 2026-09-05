package com.limelight;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Toda sobreposição que tapa a tela carrega a própria saída.
 *
 * Isto nasceu de um beco real: o teclado completo da sessão era ligado por um interruptor
 * na folha de ajustes e cobria justamente a faixa por onde a folha é chamada. Quem ligava
 * não conseguia desligar — a única saída era derrubar a sessão e reconectar.
 *
 * A regra não é sobre aquele teclado: é sobre a forma do erro. Um controle não pode
 * depender de alcançar outro controle que ele mesmo está cobrindo. Enquanto o teste existe,
 * quem remover a saída de uma dessas camadas descobre aqui, e não no aparelho.
 */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class SemBecoSemSaidaTest {

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    /**
     * A folha e a barra são <merge>, que não infla com pai nulo — ele existe justamente
     * para não criar um container e por isso precisa de um para se fundir. Um FrameLayout
     * descartável serve, e é o mesmo tipo de pai que a activity_game oferece a elas.
     */
    private View inflar(int layout) {
        FrameLayout pai = new FrameLayout(ApplicationProvider.getApplicationContext());
        LayoutInflater.from(pai.getContext()).inflate(layout, pai, true);
        return pai;
    }

    /** O teclado completo, que é o que motivou a regra. */
    @Test
    public void tecladoCompleto_temSaidaPropria() {
        View teclado = inflar(R.layout.layout_axixi_keyboard);
        View sair = teclado.findViewById(R.id.keyboardDismiss);

        assertNotNull("o teclado completo cobre a tela e precisa da própria saída", sair);
        assertTrue("a saída precisa ser visível para servir de saída",
                sair.getVisibility() == View.VISIBLE);
    }

    /** A barra de composição também cobre a base da tela quando o teclado sobe. */
    @Test
    public void barraDeComposicao_temSaidaPropria() {
        View barra = inflar(R.layout.vidro_compose_bar);
        assertNotNull("a barra de composição precisa de um fechar próprio",
                barra.findViewById(R.id.vidroComposeClose));
    }

    /** A folha de ajustes da sessão cobre a lateral inteira. */
    @Test
    public void folhaDeAjustes_temSaidaPropria() {
        View folha = inflar(R.layout.vidro_session_sheet);
        assertNotNull("a folha de ajustes precisa de um fechar desenhado",
                folha.findViewById(R.id.vidroSheetClose));
    }
}
