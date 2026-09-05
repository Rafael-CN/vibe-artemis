package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A calibração existe porque a dedução falhou duas vezes no aparelho do dono. Estes testes
 * prendem o comportamento das oito orientações possíveis para que a terceira tentativa não
 * dependa de mais uma rodada de teste em tablet.
 *
 * Aritmética pura, sem Android — que é o ponto: o que quebrou antes estava enfiado no
 * tratamento de MotionEvent e não tinha como ser exercitado sem hardware.
 */
public class TouchpadCalibrationTest {

    /** O caso fácil: o pad já está na orientação da tela. */
    @Test
    public void padAlinhado_saiIdentidade() {
        TouchpadCalibration c = TouchpadCalibration.fromGestures(100, 0, 0, 100);
        assertTrue(c.isIdentity());
        assertEquals(50f, c.screenDeltaX(50, 0), 0.001f);
        assertEquals(50f, c.screenDeltaY(0, 50), 0.001f);
    }

    /**
     * O caso do dono: ele precisava trocar X por Y E pôr o Y em -100%, que juntos são uma
     * rotação de 90°. Nessa orientação o dedo indo para a direita cresce o Y do pad, e o
     * dedo indo para baixo DIMINUI o X.
     */
    @Test
    public void padGirado90_dedoParaDireitaVemComoY() {
        TouchpadCalibration c = TouchpadCalibration.fromGestures(0, 100, -100, 0);

        assertFalse(c.isIdentity());
        assertTrue("dedo para a direita tem de mover o cursor para a direita",
                c.screenDeltaX(0, 100) > 0);
        assertEquals("andar só nesse eixo não pode mexer na vertical",
                0f, c.screenDeltaY(0, 100), 0.001f);
        assertTrue("dedo para baixo tem de descer o cursor",
                c.screenDeltaY(-100, 0) > 0);
    }

    @Test
    public void padGirado180_ambosOsSinaisInvertem() {
        TouchpadCalibration c = TouchpadCalibration.fromGestures(-100, 0, 0, -100);
        assertTrue(c.screenDeltaX(-40, 0) > 0);
        assertTrue(c.screenDeltaY(0, -40) > 0);
    }

    @Test
    public void padEspelhado_soUmEixoInverte() {
        // Espelho horizontal: a direita vem como X negativo, mas baixo continua Y positivo.
        TouchpadCalibration c = TouchpadCalibration.fromGestures(-100, 0, 0, 100);
        assertTrue(c.screenDeltaX(-30, 0) > 0);
        assertTrue(c.screenDeltaY(0, 30) > 0);
    }

    @Test
    public void gestoCurtoDemais_naoCalibra() {
        // Dedo parado não é gesto, e aceitar isso fixaria uma orientação ao acaso.
        assertNull(TouchpadCalibration.fromGestures(2, 1, 0, 100));
        assertNull(TouchpadCalibration.fromGestures(100, 0, 1, 3));
    }

    @Test
    public void gestoDiagonalDemais_naoCalibra() {
        // 100x80 não diz qual eixo a pessoa quis mover.
        assertNull(TouchpadCalibration.fromGestures(100, 80, 0, 100));
    }

    @Test
    public void doisGestosNoMesmoEixo_naoCalibra() {
        // Se "direita" e "baixo" caíram no mesmo eixo do pad, uma das medições foi torta —
        // aceitar daria um cursor preso numa linha só.
        assertNull(TouchpadCalibration.fromGestures(100, 0, 100, 0));
        assertNull(TouchpadCalibration.fromGestures(100, 0, -100, 0));
    }

    @Test
    public void sobreviveAoIdaEVolta() {
        TouchpadCalibration c = TouchpadCalibration.fromGestures(0, 100, -100, 0);
        TouchpadCalibration lida = TouchpadCalibration.parse(c.serialize());
        assertEquals(c.screenDeltaX(13, 37), lida.screenDeltaX(13, 37), 0.001f);
        assertEquals(c.screenDeltaY(13, 37), lida.screenDeltaY(13, 37), 0.001f);
    }

    @Test
    public void textoEstragado_voltaParaIdentidade() {
        // Preferência corrompida não pode virar cursor maluco: identidade é o pior caso
        // aceitável, e é o comportamento de quem nunca calibrou.
        assertTrue(TouchpadCalibration.parse(null).isIdentity());
        assertTrue(TouchpadCalibration.parse("").isIdentity());
        assertTrue(TouchpadCalibration.parse("1,0,0").isIdentity());
        assertTrue(TouchpadCalibration.parse("a,b,c,d").isIdentity());
        assertTrue("matriz com dois valores na mesma linha não é orientação",
                TouchpadCalibration.parse("1,1,0,1").isIdentity());
        assertTrue("matriz com o mesmo eixo nas duas linhas prenderia o cursor",
                TouchpadCalibration.parse("1,0,1,0").isIdentity());
    }
}
