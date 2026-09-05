package com.limelight.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * O gesto de sair da sessão, e sobretudo o que ele NÃO pode fazer.
 *
 * Sair é a única ação da sessão com consequência: ela devolve a escolha e arma o prazo que
 * derruba a conexão. Um gesto que a dispara sem querer é pior que não ter gesto nenhum — daí
 * este arquivo existir e daí o caso do encerramento acidental ser o primeiro dele.
 *
 * Sem Robolectric: a regra é aritmética pura, e mantê-la assim é metade do valor dela.
 */
public class SaidaPorGestoTest {

    /** Um quadro de tablet deitado, em pixels. */
    private static final float ALTURA = 2150f;

    /** O limiar de velocidade já convertido: 900 dp/s numa densidade 2. */
    private static final float LIMIAR_V = 1800f;

    @Test
    public void umEsbarraoNaBordaNaoEncerraAPartida() {
        // Trinta pixels para baixo, devagar. É o caso que este teste existe para impedir:
        // a mão encostando na borda enquanto joga.
        assertFalse("um esbarrao encerrou a sessao",
                GavetaDaSessao.decideSair(120f, LIMIAR_V, 30f, ALTURA));
    }

    @Test
    public void umArrastoLentoAteMetadeEncerra() {
        // Lento, mas foi até o meio do quadro: a intenção está na distância, e ela é clara.
        assertTrue(GavetaDaSessao.decideSair(80f, LIMIAR_V, ALTURA / 2f, ALTURA));
    }

    @Test
    public void umArremessoCurtoEncerra() {
        // Cem pixels só, mas jogado: é como se empurra uma coisa para longe. Com limiar só
        // de distância, este gesto — o mais natural dos dois — não faria nada.
        assertTrue("o arremesso foi ignorado",
                GavetaDaSessao.decideSair(3000f, LIMIAR_V, 100f, ALTURA));
    }

    @Test
    public void umArremessoParaCimaNuncaEncerra() {
        // Rápido, mas na direção contrária: é alguém devolvendo a imagem depois de mudar de
        // ideia no meio do caminho. A velocidade decide, e ela diz "de volta".
        assertFalse("subir encerrou a sessao",
                GavetaDaSessao.decideSair(-3000f, LIMIAR_V, ALTURA * 0.9f, ALTURA));
    }

    @Test
    public void abaixoDoTercoNaoEncerra() {
        // O limiar de distância é um terço. Logo abaixo dele, sem velocidade, não encerra —
        // e é isso que permite espiar o gesto e desistir.
        assertFalse(GavetaDaSessao.decideSair(0f, LIMIAR_V, ALTURA * 0.32f, ALTURA));
        assertTrue(GavetaDaSessao.decideSair(0f, LIMIAR_V, ALTURA * 0.34f, ALTURA));
    }

    @Test
    public void alturaZeroNaoDivideNemEncerra() {
        // Antes da primeira medida a altura é zero. Encerrar ali seria encerrar por um gesto
        // que ninguém chegou a fazer.
        assertFalse(GavetaDaSessao.decideSair(0f, LIMIAR_V, 0f, 0f));
    }
}
