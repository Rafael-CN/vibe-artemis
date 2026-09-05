package com.limelight.binding.input.virtual_controller.keyboard;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fechar o teclado de tela solta o que ficou fixado.
 *
 * O teclado tem tecla fixa por toque longo, e enquanto ela está fixada o {@code onRelease}
 * dos ouvintes volta cedo — é assim que a modificadora fica descida no host enquanto o
 * usuário digita a combinação. O {@code hide()} só virava tudo GONE: quem fechava pelo
 * gesto de quatro dedos com o Ctrl fixado ficava com o Ctrl descido no PC e sem nada na
 * tela para explicar por quê, porque a causa acabou de sair de cena.
 *
 * O que o teste fixa não é a chamada, é o mecanismo: o release precisa acontecer com a
 * fixação JÁ desfeita, senão o ouvinte real volta cedo de novo e o KEY_UP continua não
 * saindo. E o ocultamento temporário — o do PiP, que restaura o overlay depois — não pode
 * soltar nada.
 */
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class StickyKeyReleaseTest {

    private Context context;
    private KeyBoardController controller;
    private KeyBoardDigitalButton ctrl;

    /** Quantas vezes o caminho de soltar rodou, e se a fixação já tinha caído quando rodou. */
    private int releases;
    private boolean aindaFixadoNoRelease;

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(),
                androidx.appcompat.R.style.Theme_AppCompat);

        // conn nulo: os ouvintes só o usam ao enviar tecla, e o envio já é barrado antes
        // disso porque não há sessão de stream neste teste.
        controller = new KeyBoardController(null, new FrameLayout(context), context);

        // O botão de verdade, com o ouvinte de verdade — é ele que volta cedo enquanto
        // isSticky(), e é justamente esse retorno que o conserto tem de desarmar.
        ctrl = KeyBoardControllerConfigurationLoader.createDigitalButton(
                "key_" + KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_LEFT, 0, 1,
                "Ctrl", -1, true, controller, context);

        releases = 0;
        aindaFixadoNoRelease = false;
        ctrl.addDigitalButtonListener(new KeyBoardDigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onRelease() {
                releases++;
                aindaFixadoNoRelease = ctrl.isSticky();
            }
        });

        controller.addElement(ctrl, 0, 0, 40, 40);
    }

    @Test
    public void fecharOTecladoSoltaATeclaFixada() {
        ctrl.setSticky(true);

        controller.hide();

        assertFalse("a tecla não pode continuar fixada depois que o teclado sai",
                ctrl.isSticky());
        assertEquals("o caminho de soltar tem de rodar uma vez", 1, releases);
        assertFalse("soltar com a fixação ainda de pé faz o ouvinte voltar cedo e o KEY_UP não sai",
                aindaFixadoNoRelease);
    }

    @Test
    public void fecharOTecladoNaoMexeEmTeclaQueNaoEstavaFixada() {
        controller.hide();

        assertEquals("tecla solta não tem o que soltar", 0, releases);
    }

    @Test
    public void ocultamentoTemporarioPreservaAFixacao() {
        ctrl.setSticky(true);

        controller.hide(true);

        assertTrue("o PiP esconde para restaurar depois; soltar aqui perderia o estado",
                ctrl.isSticky());
        assertEquals("nada pode ser solto no ocultamento temporário", 0, releases);
    }
}
