package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * O prazo da sessão viva, exercitado com o relógio na mão.
 *
 * Sair de uma sessão deixou de matá-la, e este é o contrapeso: sem ele, uma sessão esquecida
 * segura o display virtual do host indefinidamente. As quatro regras que importam estão aqui
 * porque as quatro interagem, e porque nenhuma delas precisa de aparelho para ser verdade.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PrazoDaSessaoTest {

    private static final long PRAZO = 2 * 60 * 1000;

    private Handler relogio;
    private ShadowLooper looper;
    private int derrubadas;
    private PrazoDaSessao prazo;

    @Before
    public void montar() {
        relogio = new Handler(Looper.getMainLooper());
        looper = Shadows.shadowOf(Looper.getMainLooper());
        derrubadas = 0;
        prazo = new PrazoDaSessao(relogio, PRAZO, new Runnable() {
            @Override
            public void run() {
                derrubadas++;
            }
        });
    }

    @Test
    public void armaAoSairParaACascaEDerrubaNoFim() {
        prazo.armar();
        assertTrue(prazo.armado());

        // Um segundo antes: ainda não.
        looper.idleFor(PRAZO - 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals("derrubou antes da hora", 0, derrubadas);

        looper.idleFor(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals("nao derrubou no prazo", 1, derrubadas);
    }

    @Test
    public void voltarParaOVideoDesarma() {
        prazo.armar();
        looper.idleFor(PRAZO / 2, java.util.concurrent.TimeUnit.MILLISECONDS);

        prazo.desarmar();
        assertFalse(prazo.armado());

        // O tempo continua correndo e nada acontece — é a garantia inteira desta regra:
        // quem voltou para a sessão não pode perdê-la porque o cronômetro seguiu sozinho.
        looper.idleFor(PRAZO * 2, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(0, derrubadas);
    }

    @Test
    public void armarDuasVezesNaoReiniciaAContagem() {
        prazo.armar();
        looper.idleFor(PRAZO - 1000, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Um segundo antes do fim, alguém arma de novo — o caso de a casca ser reaberta sem
        // ter sido fechada. Se isto reiniciasse, a sessão duraria para sempre desde que
        // alguém mexesse na casca de vez em quando, e o prazo deixaria de ser prazo.
        prazo.armar();

        looper.idleFor(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals("armar de novo reiniciou a contagem", 1, derrubadas);
    }

    @Test
    public void emPiPNaoArma() {
        prazo.aoMudarPiP(true);
        prazo.armar();

        assertFalse("armou em PiP", prazo.armado());
        looper.idleFor(PRAZO * 2, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals("derrubou uma sessao que estava a vista", 0, derrubadas);
    }

    @Test
    public void entrarEmPiPDesarmaOQueJaEstavaCorrendo() {
        prazo.armar();
        looper.idleFor(PRAZO / 2, java.util.concurrent.TimeUnit.MILLISECONDS);

        prazo.aoMudarPiP(true);
        assertFalse(prazo.armado());

        looper.idleFor(PRAZO * 2, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(0, derrubadas);
    }

    @Test
    public void sairDoPiPNaoRearmaSozinho() {
        prazo.aoMudarPiP(true);
        prazo.aoMudarPiP(false);

        // Rearmar aqui seria adivinhar: sair do PiP devolve a sessão ao primeiro plano, e
        // quem sabe se a casca está à mostra é a casca.
        assertFalse(prazo.armado());
        looper.idleFor(PRAZO * 2, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(0, derrubadas);
    }

    @Test
    public void desarmarSemTerArmadoNaoExplode() {
        // Caso normal, não defensivo: esconderCasca() é chamado em caminhos onde a casca
        // nunca chegou a aparecer.
        prazo.desarmar();
        assertFalse(prazo.armado());
    }
}
