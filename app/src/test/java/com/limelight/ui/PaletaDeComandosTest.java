package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.limelight.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * O contrato da paleta, que é a razão de ela existir do jeito que existe.
 *
 * <b>Enter é SEMPRE texto para o PC.</b> Digitar "sair" não pode encerrar a sessão. Este
 * arquivo existe para que essa frase não vire uma boa intenção no comentário de alguém: ela é
 * a diferença entre um campo em que se pode confiar depressa e um que precisa ser lido antes
 * de cada tecla — e depressa é a única velocidade que serve com as mãos no teclado.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PaletaDeComandosTest {

    private Activity activity;
    private EditText campo;
    private PaletaDeComandos paleta;
    private final List<String> executados = new ArrayList<>();

    @Before
    public void montar() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();

        campo = new EditText(activity);
        ScrollView rolagem = new ScrollView(activity);
        LinearLayout lista = new LinearLayout(activity);
        lista.setOrientation(LinearLayout.VERTICAL);
        rolagem.addView(lista);
        View realce = new View(activity);

        LinearLayout raiz = new LinearLayout(activity);
        raiz.addView(campo);
        raiz.addView(rolagem);
        raiz.addView(realce);
        activity.setContentView(raiz);

        paleta = new PaletaDeComandos(activity, campo, rolagem, lista, realce);

        executados.clear();
        List<PaletaDeComandos.Comando> comandos = new ArrayList<>();
        comandos.add(comando(R.string.vidro_mouse_multitouch));
        comandos.add(comando(R.string.vidro_mouse_trackpad));
        comandos.add(comando(R.string.vidro_leave_screen));
        paleta.definirComandos(comandos);
    }

    private PaletaDeComandos.Comando comando(final int rotulo) {
        return new PaletaDeComandos.Comando(rotulo, new Runnable() {
            @Override
            public void run() {
                executados.add(activity.getString(rotulo));
            }
        });
    }

    private boolean teclar(int keyCode) {
        return paleta.aoTeclar(keyCode, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    }

    @Test
    public void enterNoCampoNaoExecutaNada() {
        campo.setText("leave");

        // Mesmo com o filtro reduzido a UMA linha, e mesmo com o texto batendo exatamente
        // com o rótulo dela. É aqui que uma barra de busca comum executaria o resultado.
        assertFalse("a paleta consumiu o Enter do campo", teclar(KeyEvent.KEYCODE_ENTER));
        assertTrue("um comando rodou sem ninguem escolher: " + executados, executados.isEmpty());
    }

    @Test
    public void aSetaDesceEOEnterPassaAValerParaALinha() {
        assertTrue("a seta para baixo nao entrou na lista", teclar(KeyEvent.KEYCODE_DPAD_DOWN));
        assertTrue(paleta.estaNaLista());

        assertTrue("o Enter na lista nao foi consumido", teclar(KeyEvent.KEYCODE_ENTER));
        assertEquals(1, executados.size());
        assertEquals(activity.getString(R.string.vidro_mouse_multitouch), executados.get(0));

        // Executar devolve o Enter ao campo: a paleta recolhe e o contrato volta a valer.
        assertFalse(paleta.estaNaLista());
    }

    @Test
    public void subirAcimaDaPrimeiraDevolveOEnterAoCampo() {
        teclar(KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(paleta.estaNaLista());

        assertTrue(teclar(KeyEvent.KEYCODE_DPAD_UP));
        assertFalse("quem desceu por engano ficou preso na lista", paleta.estaNaLista());

        // E o Enter volta a ser do campo, que é a prova de que a saída é de verdade.
        assertFalse(teclar(KeyEvent.KEYCODE_ENTER));
        assertTrue(executados.isEmpty());
    }

    @Test
    public void oFiltroReiniciaASelecao() {
        teclar(KeyEvent.KEYCODE_DPAD_DOWN);
        teclar(KeyEvent.KEYCODE_DPAD_DOWN);

        // Com a lista rolada para a segunda linha, digitar muda o conjunto: a linha de
        // índice 1 de antes não é a mesma de agora. Manter o índice realçaria uma coisa e
        // executaria outra.
        campo.setText("leave");
        assertTrue(paleta.estaNaLista());

        teclar(KeyEvent.KEYCODE_ENTER);
        assertEquals(1, executados.size());
        assertEquals(activity.getString(R.string.vidro_leave_screen), executados.get(0));
    }

    @Test
    public void semResultadoNaoSobraSelecaoPendurada() {
        teclar(KeyEvent.KEYCODE_DPAD_DOWN);
        campo.setText("zzzzz nao existe");

        assertFalse("sobrou selecao apontando para uma lista vazia", paleta.estaNaLista());
        assertFalse("o Enter ficou sequestrado", teclar(KeyEvent.KEYCODE_ENTER));
    }

    @Test
    public void asLetrasNuncaSaoDaPaleta() {
        // Nada além das setas, do Enter na lista e do Esc na lista. Uma paleta que
        // reivindicasse teclas de texto seria um modo, e modo é o que ela não é.
        for (int tecla : new int[] {KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_Z,
                KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_TAB}) {
            assertFalse("a paleta engoliu " + tecla, teclar(tecla));
        }
    }
}
