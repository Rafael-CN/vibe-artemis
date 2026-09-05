package com.limelight;

import android.app.GameManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * A home tem de dizer alguma coisa antes de qualquer host responder.
 *
 * Este teste nasceu de uma dúvida concreta: a captura viva da PcView saía toda preta, e
 * preto pode ser tanto "a tela espera o serviço" quanto "a tela quebrou". O
 * shootActivity engole Throwable e grava a imagem mesmo assim, então a cor não distingue
 * os dois casos — já houve uma vez em que um ClassCastException virou PNG preto que eu li
 * como tela vazia.
 *
 * Aqui a pergunta vira asserção: com zero hosts e nenhum serviço conectado, o estado
 * vazio precisa estar visível e o título precisa ter texto. Se algum dia a home voltar a
 * abrir em branco para quem ainda não tem PC nenhum, isto falha antes de chegar no
 * aparelho.
 */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class PcViewEmptyStateTest {

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    private void prepareEnvironment() {
        SharedPreferences glPrefs = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("GlPreferences", 0);
        glPrefs.edit()
                .putString("Renderer", "TestRenderer")
                .putString("Fingerprint", Build.FINGERPRINT)
                .commit();

        org.robolectric.shadows.ShadowApplication shadowApp = org.robolectric.Shadows.shadowOf(
                (android.app.Application) ApplicationProvider.getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            shadowApp.setSystemService(Context.GAME_SERVICE, mock(GameManager.class));
        }

        Context ctx = ApplicationProvider.getApplicationContext();
        android.content.ComponentName cn = new android.content.ComponentName(
                ctx, com.limelight.computers.ComputerManagerService.class);
        com.limelight.computers.ComputerManagerService.ComputerManagerBinder binder =
                mock(com.limelight.computers.ComputerManagerService.ComputerManagerBinder.class);
        org.robolectric.Shadows.shadowOf((android.app.Application) ctx)
                .setComponentNameAndServiceForBindService(cn, binder);
    }

    /**
     * A home sobe sem host nenhum.
     *
     * O que este teste guarda hoje e so isto: que a Activity monta. As assertivas sobre o
     * estado vazio, o titulo que nao pode abrir em branco e a saida pelo endereco dentro do
     * estado de procura foram PORTADAS para TelaHomeTest -- findViewById nao alcanca
     * composable, e enfraquece-las para ficar verde seria trocar defeito conhecido por
     * defeito esquecido.
     */
    @Test
    public void semNenhumHost_aHomeSobe() {
        prepareEnvironment();

        ActivityController<PcView> controller = Robolectric.buildActivity(PcView.class).setup();
        assertNotNull("a home precisa montar sem host nenhum", controller.get());
        controller.close();
    }
}
