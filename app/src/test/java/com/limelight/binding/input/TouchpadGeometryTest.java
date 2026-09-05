package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * O trackpad da capa do S8 Ultra chegava ao aparelho pedindo configuração manual: trocar X
 * por Y e pôr o eixo Y em -100%. Estes testes prendem a dedução que tirou essa exigência.
 *
 * Aritmética pura, sem Android — é justamente por a lógica ter nascido enfiada no
 * tratamento de MotionEvent da Game que ela chegou quebrada ao aparelho sem ninguém notar.
 */
public class TouchpadGeometryTest {

    // Um pad deitado, como todo pad físico é: mais largo que alto.
    private static TouchpadGeometry deitado() {
        return TouchpadGeometry.resolve(2000, 1000, 1920, 1080, false);
    }

    // O mesmo pad, mas com o driver reportando os eixos girados em 90°.
    private static TouchpadGeometry girado() {
        return TouchpadGeometry.resolve(1000, 2000, 1920, 1080, false);
    }

    @Test
    public void padDeitado_naoEhTratadoComoGirado() {
        assertFalse(deitado().isRotated());
    }

    @Test
    public void padQueSeDizEmPe_ehTratadoComoGirado() {
        // Não existe trackpad mais alto que largo. Se o driver diz isso, ele girou.
        assertTrue(girado().isRotated());
    }

    @Test
    public void padDeitado_dedoParaDireitaMoveCursorParaDireita() {
        TouchpadGeometry g = deitado();
        assertTrue("dedo para a direita tem de mover o cursor para a direita",
                g.screenDeltaX(100, 0) > 0);
        assertEquals("andar só na horizontal não pode mexer na vertical",
                0, g.screenDeltaY(100, 0));
    }

    @Test
    public void padDeitado_dedoParaBaixoMoveCursorParaBaixo() {
        TouchpadGeometry g = deitado();
        assertTrue(g.screenDeltaY(0, 100) > 0);
        assertEquals(0, g.screenDeltaX(0, 100));
    }

    /**
     * O caso que motivou tudo. Com o pad girado, o dedo indo para a direita chega como
     * crescimento de Y no driver — e é isso que tem de virar movimento horizontal.
     */
    @Test
    public void padGirado_dedoParaDireitaAindaMoveCursorParaDireita() {
        TouchpadGeometry g = girado();
        assertTrue("é o Y do driver que acompanha o dedo indo para a direita",
                g.screenDeltaX(0, 100) > 0);
        assertEquals(0, g.screenDeltaY(0, 100));
    }

    /**
     * A metade que a troca de eixos sozinha não resolvia, e por isso pedia "-100% no Y":
     * girar sem trocar o sinal viraria espelho, e o cursor subiria quando o dedo desce.
     */
    @Test
    public void padGirado_dedoParaBaixoNaoFazOCursorSubir() {
        TouchpadGeometry g = girado();
        assertTrue("dedo descendo tem de descer o cursor, não subir",
                g.screenDeltaY(-100, 0) > 0);
        assertEquals(0, g.screenDeltaX(-100, 0));
    }

    @Test
    public void aPreferenciaDeTrocarEixosInverteADeducao() {
        // Escape para hardware que engane a regra: quem marca a preferência recebe o
        // oposto do que foi deduzido, em vez de ficar preso a um palpite errado.
        assertTrue(TouchpadGeometry.resolve(2000, 1000, 1920, 1080, true).isRotated());
        assertFalse(TouchpadGeometry.resolve(1000, 2000, 1920, 1080, true).isRotated());
    }

    @Test
    public void umaVarridaInteiraDoPadAtravessaAImagemInteira() {
        // É o que faz a velocidade parecer a mesma em hardware de resolução diferente.
        assertEquals(1920, deitado().screenDeltaX(2000, 0));
        assertEquals(1080, deitado().screenDeltaY(0, 1000));

        TouchpadGeometry g = girado();
        assertEquals(1920, g.screenDeltaX(0, 2000));
        assertEquals(1080, g.screenDeltaY(-1000, 0));
    }

    @Test
    public void geometriaDesconhecida_passaAsUnidadesDoPadSemInventar() {
        TouchpadGeometry g = TouchpadGeometry.resolve(0, 0, 1920, 1080, false);
        assertFalse(g.isRotated());
        assertEquals(37, g.screenDeltaX(37, 0));
        assertEquals(-9, g.screenDeltaY(0, -9));
    }

    @Test
    public void telaAindaSemMedida_naoDividePorZero() {
        TouchpadGeometry g = TouchpadGeometry.resolve(2000, 1000, 0, 0, false);
        assertEquals(TouchpadGeometry.UNKNOWN.screenDeltaX(5, 5), g.screenDeltaX(5, 5));
    }
}
