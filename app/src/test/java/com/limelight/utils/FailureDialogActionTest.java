package com.limelight.utils;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;
import com.limelight.computers.ComputerManagerService;
import com.limelight.preferences.AddComputerManually;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * A folha de falha precisa de uma saída que não seja desistir.
 *
 * O único desfecho dela era "OK", e "OK" encerra a sessão e devolve ao catálogo — de onde
 * repetir a tentativa custa mais quatro toques e uma navegação. Com o host subindo um
 * display virtual por cliente, falhar na primeira tentativa é rotina, e repetir é o gesto
 * que a pessoa ia fazer de qualquer jeito.
 *
 * O que está preso aqui é o contrato do botão novo, e principalmente a metade dele que é
 * fácil de quebrar sem perceber: a ação SUBSTITUI a saída. Se ela passar a rodar junto com
 * o runOnDismiss, o "Tentar de novo" reconecta e encerra a Activity no mesmo toque — e o
 * sintoma no aparelho seria a sessão piscando e voltando para o catálogo, que ninguém liga
 * a este arquivo.
 */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class FailureDialogActionTest {

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @After
    public void limparFolhasAbertas() {
        // rundownDialogs é estático, e uma folha que sobra de um teste é candidata a ser
        // deduplicada no próximo.
        Dialog.closeDialogs();
    }

    private ActivityController<AddComputerManually> abrirTela() {
        Context ctx = ApplicationProvider.getApplicationContext();
        ComponentName cn = new ComponentName(ctx, ComputerManagerService.class);
        ComputerManagerService.ComputerManagerBinder binder =
                mock(ComputerManagerService.ComputerManagerBinder.class);
        Shadows.shadowOf((android.app.Application) ctx)
                .setComponentNameAndServiceForBindService(cn, binder);

        return Robolectric.buildActivity(AddComputerManually.class).setup();
    }

    private AlertDialog folhaNaTela() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        android.app.Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("a folha precisa aparecer", dialog);
        assertTrue("a folha de falha é um AlertDialog", dialog instanceof AlertDialog);
        return (AlertDialog) dialog;
    }

    @Test
    public void aAcaoRodaEDeixaATelaDePe() {
        ActivityController<AddComputerManually> controller = abrirTela();
        AddComputerManually tela = controller.get();

        final int[] tentativas = {0};
        Dialog.displayDialog(tela, "Erro de Conexão", "Falha ao iniciar Desktop", true,
                "Tentar de novo", new Runnable() {
                    @Override
                    public void run() {
                        tentativas[0]++;
                    }
                });

        AlertDialog folha = folhaNaTela();
        Button acao = folha.getButton(AlertDialog.BUTTON_NEGATIVE);
        assertNotNull("o slot negativo é onde a ação mora", acao);
        assertEquals("Tentar de novo", acao.getText().toString());

        acao.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals("tocar na ação tem de rodá-la uma vez", 1, tentativas[0]);
        assertFalse("tentar de novo não pode encerrar a sessão no mesmo toque",
                tela.isFinishing());

        controller.close();
    }

    @Test
    public void oOkContinuaEncerrandoESemRodarAAcao() {
        ActivityController<AddComputerManually> controller = abrirTela();
        AddComputerManually tela = controller.get();

        final int[] tentativas = {0};
        Dialog.displayDialog(tela, "Erro de Conexão", "Falha ao iniciar Jogo", true,
                "Tentar de novo", new Runnable() {
                    @Override
                    public void run() {
                        tentativas[0]++;
                    }
                });

        AlertDialog folha = folhaNaTela();
        Button ok = folha.getButton(AlertDialog.BUTTON_POSITIVE);
        assertNotNull(ok);

        ok.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals("quem desistiu não pediu retentativa", 0, tentativas[0]);
        assertTrue("com endAfterDismiss, OK continua encerrando", tela.isFinishing());

        controller.close();
    }

    @Test
    public void folhaSemAcaoNaoGanhaBotaoNovo() {
        // A sobrecarga antiga é a de longe mais usada: pareamento, lista de apps, detalhes
        // do PC. Nenhuma delas pode passar a mostrar um botão a mais.
        ActivityController<AddComputerManually> controller = abrirTela();
        AddComputerManually tela = controller.get();

        Dialog.displayDialog(tela, "Detalhes", "Só informação", false);

        AlertDialog folha = folhaNaTela();
        Button acao = folha.getButton(AlertDialog.BUTTON_NEGATIVE);
        assertTrue("sem ação, o slot negativo continua vazio",
                acao == null || acao.getVisibility() != View.VISIBLE);

        controller.close();
    }
}
