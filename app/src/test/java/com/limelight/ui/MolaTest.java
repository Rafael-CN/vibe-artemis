package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * A física, exercitada sem Choreographer.
 *
 * O laço foi separado do quadro justamente para isto: as propriedades que fazem a mola valer
 * a pena — chegar, não passar onde não deve, passar onde deve, e obedecer à velocidade da mão
 * — são todas verificáveis integrando passos à mão. O que sobra no {@code doFrame} é relógio.
 *
 * Robolectric entra só porque {@code Choreographer.getInstance()} é chamado no construtor.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MolaTest {

    /** 60 Hz, que é o passo que o Choreographer entrega na esmagadora maioria dos quadros. */
    private static final float QUADRO = 1f / 60f;

    /** Guarda o ultimo valor aplicado -- e o que prova que `fixar` desenha na hora. */
    private static final class Registro implements Mola.Alvo {
        float ultimo = Float.NaN;

        @Override
        public void aplicar(float valor) {
            ultimo = valor;
        }
    }

    /** Roda até parar, com teto — uma mola que não converge é o defeito que isto pega. */
    private static int correr(Mola mola) {
        for (int i = 0; i < 600; i++) {
            if (mola.integrar(QUADRO)) {
                return i + 1;
            }
        }
        throw new AssertionError("a mola nao parou em 10 segundos de quadros");
    }

    @Test
    public void chegaExatamenteNoDestino() {
        Mola mola = new Mola(Mola.Perfil.CORPO, 0f, v -> { });
        mola.lancar(240f, 0f);
        correr(mola);

        // Exato, e não "perto": meio pixel de erro por transição acumula numa borda.
        assertEquals(240f, mola.valor(), 0f);
        assertEquals(0f, mola.velocidade(), 0f);
    }

    /** O ponto mais distante que a mola alcancou no caminho ate parar. */
    private static float maisLonge(Mola.Perfil perfil, float destino) {
        Mola mola = new Mola(perfil, 0f, v -> { });
        mola.lancar(destino, 0f);
        float pico = 0f;
        for (int i = 0; i < 600; i++) {
            if (mola.integrar(QUADRO)) {
                break;
            }
            pico = Math.max(pico, mola.valor());
        }
        return pico;
    }

    @Test
    public void criticamenteAmortecidasNaoPassamDoDestino() {
        // Sobressalto em tudo é a versão brinquedo desta ideia — é assim que física vira
        // decoração. As três massas normais chegam e encostam.
        for (Mola.Perfil perfil : new Mola.Perfil[] {
                Mola.Perfil.LEVE, Mola.Perfil.CORPO, Mola.Perfil.TELA }) {
            float pico = maisLonge(perfil, 240f);
            assertTrue(perfil + " passou do destino: " + pico, pico <= 240f);
        }
    }

    @Test
    public void oImaPassaDoDestino() {
        // O olho lê ímã pelo excesso na chegada. Sem excesso, o punho desliza em vez de grudar.
        float pico = maisLonge(Mola.Perfil.IMA, 240f);
        assertTrue("o IMA nao passou do destino: " + pico, pico > 240f);
    }

    /** Quantos quadros ate vencer 90% do caminho -- a chegada, e nao a cauda. */
    private static int quadrosAte90(Mola mola, float destino) {
        float limiar = destino * 0.9f;
        for (int i = 0; i < 600; i++) {
            if (mola.valor() >= limiar) {
                return i;
            }
            mola.integrar(QUADRO);
        }
        throw new AssertionError("a mola nao venceu 90 por cento do caminho");
    }

    @Test
    public void aMolaEhInvarianteAEscala() {
        Mola curta = new Mola(Mola.Perfil.CORPO, 0f, v -> { });
        curta.lancar(60f, 0f);

        Mola longa = new Mola(Mola.Perfil.CORPO, 0f, v -> { });
        longa.lancar(600f, 0f);

        // ISTO NÃO É UM DEFEITO, e o teste existe para que ninguém "conserte".
        //
        // Uma mola linear vence a mesma FRAÇÃO do caminho no mesmo tempo, seja o caminho de
        // 60 px ou de 600 px. Ela tem um tempo característico, não uma duração — e é por isso
        // que três perfis substituem oito linhas de tabela: o mesmo par (rigidez,
        // amortecimento) serve a qualquer distância sem ninguém escolher um número por
        // trajeto.
        //
        // Correção honesta a duas afirmações minhas e do briefing de design, ambas medidas
        // aqui e ambas largas demais: nem a distância nem a velocidade da mão mudam o tempo
        // de chegada de uma mola criticamente amortecida. O que a física compra de verdade
        // está nos outros testes deste arquivo — poder ser agarrada no meio sem descartar o
        // movimento, e o excesso do ÍMÃ crescendo com a força do arremesso.
        assertEquals(quadrosAte90(curta, 60f), quadrosAte90(longa, 600f));
    }

    @Test
    public void oArremessoMudaOIma() {
        // A velocidade da mão aparece no ÍMÃ, e é para isso que ele é sub-amortecido.
        //
        // Correção honesta a uma afirmação larga demais do briefing de design: numa mola
        // CRITICAMENTE amortecida a velocidade inicial quase não muda o tempo de chegada,
        // porque o termo dela decai sob a mesma envoltória e-^(wt) que governa o resto. Medido
        // aqui: empurrar e arremessar a 6000 px/s chegam no mesmo quadro. Com zeta abaixo de
        // 1 a energia extra não é absorvida na hora — ela vira excesso, e o excesso é
        // exatamente o que o olho lê como peso.
        //
        // Por isso o ÍMÃ existe como perfil próprio em vez de ser um ajuste do CORPO: ele é o
        // único lugar do app onde a força do arremesso tem de ser visível.
        float picoEmpurrado = 0f;
        Mola empurrado = new Mola(Mola.Perfil.IMA, 0f, v -> { });
        empurrado.lancar(240f, 0f);
        for (int i = 0; i < 600 && !empurrado.integrar(QUADRO); i++) {
            picoEmpurrado = Math.max(picoEmpurrado, empurrado.valor());
        }

        float picoArremessado = 0f;
        Mola arremessado = new Mola(Mola.Perfil.IMA, 0f, v -> { });
        arremessado.lancar(240f, 6000f);
        for (int i = 0; i < 600 && !arremessado.integrar(QUADRO); i++) {
            picoArremessado = Math.max(picoArremessado, arremessado.valor());
        }

        assertTrue("arremessar (pico " + picoArremessado + ") nao passou mais que empurrar "
                        + "(pico " + picoEmpurrado + ")",
                picoArremessado > picoEmpurrado);
    }

    @Test
    public void umDestinoNovoNoMeioNaoDescartaOMovimento() {
        Mola mola = new Mola(Mola.Perfil.CORPO, 0f, v -> { });
        mola.lancar(240f, 0f);
        for (int i = 0; i < 6; i++) {
            mola.integrar(QUADRO);
        }
        float velocidadeNoMeio = mola.velocidade();
        assertTrue("a mola nem chegou a se mover", velocidadeNoMeio > 0f);

        // Agarrar no meio e mandar para outro lugar: a mola continua de onde está, com a
        // velocidade que tinha. É o que separa uma resposta de uma gravação.
        mola.lancar(0f, Float.NaN);
        assertEquals(velocidadeNoMeio, mola.velocidade(), 0f);

        correr(mola);
        assertEquals(0f, mola.valor(), 0f);
    }

    @Test
    public void umQuadroPerdidoNaoViraUmSalto() {
        Mola comTeto = new Mola(Mola.Perfil.TELA, 0f, v -> { });
        comTeto.lancar(1000f, 0f);
        // Trezentos milissegundos de pausa — o app voltando do segundo plano.
        comTeto.integrar(0.300f);

        // Sem o teto de 32 ms, integrar 300 ms de uma vez joga o objeto para muito além do
        // destino e a mola volta cambaleando. Com o teto, o pior caso é um quadro gordo.
        assertTrue("saltou para " + comTeto.valor(), comTeto.valor() <= 1000f);
    }

    @Test
    public void fixarPoeNoLugarSemAnimar() {
        Registro registro = new Registro();
        Mola mola = new Mola(Mola.Perfil.CORPO, 0f, registro);
        mola.lancar(240f, 900f);
        mola.integrar(QUADRO);

        // Enquanto o dedo manda não há mola: há posição seguindo o dedo.
        mola.fixar(72f);

        assertEquals(72f, mola.valor(), 0f);
        assertEquals(0f, mola.velocidade(), 0f);
        assertEquals(72f, registro.ultimo, 0f);
    }
}
