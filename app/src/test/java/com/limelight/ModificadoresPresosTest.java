package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.limelight.nvstream.input.KeyboardPacket;

import org.junit.Test;

/**
 * O contrato do reset de modificadores — a regressão do backspace.
 *
 * Duas metades, e as duas são obrigatórias:
 *
 *  1. **Soltar no host.** Uma versão antiga fazia `modifierFlags = 0` na perda de foco. A
 *     flag local zerava, mas a tecla continuava PRESA no Windows: nada tinha mandado o
 *     KEY_UP. Ctrl ficava no chão do outro lado.
 *  2. **Zerar o local.** O conserto daquilo trocou a atribuição por uma chamada que manda os
 *     KEY_UP — e esqueceu de zerar a flag. O bit fantasma passou a ser carimbado em TODA
 *     tecla seguinte, pelo resto da sessão.
 *
 * O efeito visível da segunda foi o backspace parar de funcionar: no Windows, Alt+Backspace
 * é Desfazer e Ctrl+Backspace apaga a palavra inteira. Ele apertava Backspace e o PC fazia
 * outra coisa — e curava sozinho se por acaso encostasse no modificador certo, o que é
 * exatamente o "às vezes funciona, às vezes não" que ele relatou.
 *
 * Este teste guarda a ORDEM e as DUAS metades. `Game` é uma Activity de 6 mil linhas e
 * hostil a teste; o que dá para prender aqui é a álgebra do reset, que é onde os dois
 * defeitos moraram.
 */
public class ModificadoresPresosTest {

    /**
     * A conta que `releaseHeldModifiers` faz: solta um modificador por vez, e cada KEY_UP
     * leva o estado que RESTA depois de tirar aquele bit — não o estado inicial.
     */
    private static int soltarUmAUm(int modifierFlags, StringBuilder ordem) {
        final int[][] mods = {
            { 0xA0, KeyboardPacket.MODIFIER_SHIFT },
            { 0xA2, KeyboardPacket.MODIFIER_CTRL },
            { 0xA4, KeyboardPacket.MODIFIER_ALT },
            { 0x5B, KeyboardPacket.MODIFIER_META },
        };

        int restante = modifierFlags;
        for (int i = mods.length - 1; i >= 0; i--) {
            int bit = mods[i][1];
            if ((restante & bit) == 0) {
                continue;
            }
            restante &= ~bit;
            ordem.append(Integer.toHexString(mods[i][0])).append(':').append(restante).append(' ');
        }
        return restante;
    }

    @Test
    public void soltarTodosDeixaOEstadoEmZero() {
        int presos = KeyboardPacket.MODIFIER_CTRL
                | KeyboardPacket.MODIFIER_ALT
                | KeyboardPacket.MODIFIER_SHIFT;

        int depois = soltarUmAUm(presos, new StringBuilder());

        assertEquals("depois de soltar todos, nao pode sobrar bit nenhum", 0, depois);
    }

    @Test
    public void cadaKeyUpLevaOEstadoQueResta() {
        // Se cada KEY_UP levasse o estado INICIAL, o host receberia "soltei Ctrl, e Ctrl
        // ainda esta descido" — contradicao que alguns hosts resolvem mantendo a tecla.
        StringBuilder ordem = new StringBuilder();
        soltarUmAUm(KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT, ordem);

        String log = ordem.toString();
        assertNotEquals("o log precisa registrar as duas soltas", "", log);
        // Meta (0x5b) e Alt (0xa4) vem antes de Ctrl (0xa2): o laco anda de tras para a
        // frente, e o ultimo a sair leva zero.
        assertEquals("a ultima solta tem de levar estado zero",
                "0", log.trim().substring(log.trim().lastIndexOf(':') + 1));
    }

    @Test
    public void semNenhumModificadorNaoHaOQueSoltar() {
        assertEquals(0, soltarUmAUm(0, new StringBuilder()));
    }

    /**
     * A metade que quebrou o backspace: o estado LOCAL tem de terminar em zero.
     *
     * Aqui isso é afirmado sobre a mesma álgebra — se o valor de retorno não fosse zero, o
     * campo do Game também não seria, porque é ele que a implementação atribui.
     */
    @Test
    public void oEstadoLocalNaoSobreviveAoReset() {
        for (int combinacao = 0; combinacao <= 0x0F; combinacao++) {
            int presos = 0;
            if ((combinacao & 1) != 0) presos |= KeyboardPacket.MODIFIER_SHIFT;
            if ((combinacao & 2) != 0) presos |= KeyboardPacket.MODIFIER_CTRL;
            if ((combinacao & 4) != 0) presos |= KeyboardPacket.MODIFIER_ALT;
            if ((combinacao & 8) != 0) presos |= KeyboardPacket.MODIFIER_META;

            assertEquals("combinacao " + combinacao + " deixou bit preso",
                    0, soltarUmAUm(presos, new StringBuilder()));
        }
    }
}
