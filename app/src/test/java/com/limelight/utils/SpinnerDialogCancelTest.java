package com.limelight.utils;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;
import com.limelight.computers.ComputerManagerService;
import com.limelight.preferences.AddComputerManually;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * A roda de espera tem de aceitar desistência sem levar a tela junto.
 *
 * "Cancelável" e "encerra a Activity" eram a mesma flag no SpinnerDialog, e a tela de
 * adicionar PC — que não quer ser encerrada — acabava com uma roda que engolia o Voltar.
 * Endereço errado são uns dez segundos de sondagem; sondagem travada é para sempre, porque
 * a espera pelo endereço manual não tem prazo. O usuário ficava olhando a barra andar sem
 * nenhuma saída.
 *
 * Estes testes fixam os dois lados do contrato: a roda cancelável avisa quem espera e
 * deixa a tela de pé, e a roda que encerra a Activity continua encerrando.
 */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class SpinnerDialogCancelTest {

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    /**
     * A tela registra um ComputerManagerService de mentira porque, sem binder, ela guarda
     * um managerBinder nulo e o onDestroy deixa de recolher a thread de adição.
     */
    private ActivityController<AddComputerManually> abrirTelaDeAdicionar() {
        Context ctx = ApplicationProvider.getApplicationContext();
        ComponentName cn = new ComponentName(ctx, ComputerManagerService.class);
        ComputerManagerService.ComputerManagerBinder binder =
                mock(ComputerManagerService.ComputerManagerBinder.class);
        Shadows.shadowOf((android.app.Application) ctx)
                .setComponentNameAndServiceForBindService(cn, binder);

        return Robolectric.buildActivity(AddComputerManually.class).setup();
    }

    private Dialog rodaNaTela() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("a roda de espera precisa aparecer", dialog);
        return dialog;
    }

    @Test
    public void rodaCancelavelAvisaQuemEsperaESegueNaMesmaTela() {
        ActivityController<AddComputerManually> controller = abrirTelaDeAdicionar();
        AddComputerManually tela = controller.get();

        final boolean[] avisado = {false};
        SpinnerDialog.displayCancellableDialog(tela, "Adicionando PC", "Procurando…",
                new Runnable() {
                    @Override
                    public void run() {
                        avisado[0] = true;
                    }
                });

        Dialog roda = rodaNaTela();
        assertTrue("a roda cancelável tem de aceitar o Voltar",
                Shadows.shadowOf(roda).isCancelable());

        roda.cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue("cancelar tem de avisar quem está esperando", avisado[0]);
        assertFalse("desistir da espera não pode encerrar a tela de adicionar PC",
                tela.isFinishing());

        controller.close();
    }

    @Test
    public void rodaQueEncerraAActivityContinuaEncerrando() {
        ActivityController<AddComputerManually> controller = abrirTelaDeAdicionar();
        AddComputerManually tela = controller.get();

        SpinnerDialog.displayDialog(tela, "Conectando", "Aguarde…", true);

        Dialog roda = rodaNaTela();
        assertTrue("a roda que encerra a Activity sempre foi cancelável",
                Shadows.shadowOf(roda).isCancelable());

        roda.cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue("com finish=true, cancelar continua encerrando a tela", tela.isFinishing());

        controller.close();
    }
}
