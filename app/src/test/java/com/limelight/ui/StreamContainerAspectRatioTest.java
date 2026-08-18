package com.limelight.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * A proporção do vídeo tem que sobreviver ao teclado.
 *
 * O bug original: no modo RESIZE, Game.java põe padding embaixo do tamanho do IME para o
 * vídeo ceder espaço. A conta de letterbox usava o tamanho cheio, e o
 * {@code ViewGroup.measureChild} ainda descontava o padding do filho — largura inteira,
 * altura cortada, imagem comprimida na vertical. Com {@code setFixedSize} travando o
 * buffer na resolução do vídeo, o compositor estica para preencher o retângulo errado.
 *
 * <p><b>Estes testes medem o FILHO, não o container.</b> A primeira versão deles afirmava
 * sobre {@code getMeasuredWidth/Height} do próprio container e nunca criava filho — passava
 * verde enquanto a superfície era medida em 600x0 e o vídeo sumia da tela. Um teste que
 * não olha para onde o pixel vai não prova nada sobre o pixel.
 */
@Config(sdk = {33})
@RunWith(RobolectricTestRunner.class)
public class StreamContainerAspectRatioTest {

    /** 2:1 deixa a conta óbvia de conferir a olho. */
    private static final double ASPECT = 2.0;
    private static final double TOLERANCE = 0.02;

    private static final int SIZE = 800;

    /** O container com uma superfície dentro, como ele existe em sessão. */
    private static View measureWithChild(int aspectSource, int imeHeight) {
        Context context = ApplicationProvider.getApplicationContext();

        StreamContainer container = new StreamContainer(context, null);
        if (aspectSource != 0) {
            container.setDesiredAspectRatio(ASPECT);
        }

        View surface = new View(context);
        container.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        container.setPadding(0, 0, 0, imeHeight);
        container.measure(
                View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY));

        return surface;
    }

    @Test
    public void semTeclado_oVideoUsaAAlturaQueCabe() {
        View surface = measureWithChild(1, 0);

        // 800 de largura numa proporção 2:1 pede 400 de altura, e 400 cabe em 800.
        assertEquals(800, surface.getMeasuredWidth());
        assertEquals(400, surface.getMeasuredHeight());
    }

    @Test
    public void comTeclado_oVideoEncolheEMantemAProporcao() {
        // O IME come 500 de 800: sobram 300 de altura útil.
        View surface = measureWithChild(1, 500);

        // Numa proporção 2:1, caber em 300 de altura significa 600 de largura.
        assertEquals(300, surface.getMeasuredHeight());
        assertEquals(600, surface.getMeasuredWidth());
    }

    @Test
    public void comTeclado_oVideoNaoDesaparece() {
        // A regressão que o teste anterior não pegava: a superfície era medida com o
        // padding descontado duas vezes e zerava a altura.
        View surface = measureWithChild(1, 500);

        assertTrue("a superfície zerou — o vídeo sumiria da tela",
                surface.getMeasuredHeight() > 0 && surface.getMeasuredWidth() > 0);
    }

    @Test
    public void aProporcaoNaoMudaConformeOTecladoCresce() {
        for (int imeHeight = 0; imeHeight <= 600; imeHeight += 100) {
            View surface = measureWithChild(1, imeHeight);

            int w = surface.getMeasuredWidth();
            int h = surface.getMeasuredHeight();

            assertTrue("teclado de " + imeHeight + "px zerou a superfície", w > 0 && h > 0);
            assertTrue("teclado de " + imeHeight + "px estourou o espaço disponível",
                    h <= SIZE - imeHeight);
            assertEquals("teclado de " + imeHeight + "px distorceu o vídeo",
                    ASPECT, (double) w / h, TOLERANCE);
        }
    }

    @Test
    public void semProporcaoDefinida_oContainerNaoSeMete() {
        // Sem proporção negociada ainda, o comportamento é o do FrameLayout: o filho
        // recebe a caixa de conteúdo inteira.
        View surface = measureWithChild(0, 0);

        assertEquals(800, surface.getMeasuredWidth());
        assertEquals(800, surface.getMeasuredHeight());
    }
}
