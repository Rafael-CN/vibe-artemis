package com.limelight.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * A proporção do vídeo tem que sobreviver ao teclado.
 *
 * O bug: no modo RESIZE, Game.java põe padding embaixo do tamanho do IME para o vídeo
 * ceder espaço. O onMeasure do container ignorava esse padding, media a superfície com a
 * altura CHEIA e ainda reportava a altura cheia — o FrameLayout então encaixava uma
 * superfície grande demais numa área menor e a imagem esticava.
 *
 * O que estes testes fixam é a regra, não a implementação: com ou sem padding, a razão
 * entre largura e altura do vídeo é a mesma, e o padding sai da conta antes do letterbox.
 */
@Config(sdk = {33})
@RunWith(RobolectricTestRunner.class)
public class StreamContainerAspectRatioTest {

    /** 2:1 deixa a conta óbvia de conferir a olho. */
    private static final double ASPECT = 2.0;
    private static final double TOLERANCE = 0.02;

    private StreamContainer container() {
        Context context = ApplicationProvider.getApplicationContext();
        StreamContainer container = new StreamContainer(context, null);
        container.setDesiredAspectRatio(ASPECT);
        return container;
    }

    private static void measure(StreamContainer container, int width, int height) {
        container.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    @Test
    public void semTeclado_oVideoUsaAAlturaQueCabe() {
        StreamContainer container = container();
        measure(container, 800, 800);

        // 800 de largura numa proporção 2:1 pede 400 de altura, e 400 cabe em 800.
        assertEquals(800, container.getMeasuredWidth());
        assertEquals(400, container.getMeasuredHeight());
    }

    @Test
    public void comTeclado_oVideoEncolheEMantemAProporcao() {
        StreamContainer container = container();

        // O IME come 500 de 800: sobram 300 de altura útil.
        container.setPadding(0, 0, 0, 500);
        measure(container, 800, 800);

        // A medida reportada inclui o padding — é o espaço que o container ocupa.
        assertEquals(800, container.getMeasuredHeight());

        // O vídeo em si tem que caber nos 300 que sobraram, o que numa proporção 2:1
        // significa 600 de largura. Antes da correção ele saía 800x400: mais alto que o
        // espaço disponível, e por isso esticado.
        int videoHeight = container.getMeasuredHeight() - container.getPaddingBottom();
        int videoWidth = container.getMeasuredWidth();

        assertEquals(300, videoHeight);
        assertEquals(600, videoWidth);
        assertEquals(ASPECT, (double) videoWidth / videoHeight, TOLERANCE);
    }

    @Test
    public void aProporcaoNaoMudaConformeOTecladoCresce() {
        // O ponto do teste anterior, varrido: em qualquer altura de teclado a razão fica
        // a mesma. Uma regressão que estique o vídeo quebra aqui em algum dos passos.
        for (int imeHeight = 0; imeHeight <= 600; imeHeight += 100) {
            StreamContainer container = container();
            container.setPadding(0, 0, 0, imeHeight);
            measure(container, 800, 800);

            int videoHeight = container.getMeasuredHeight() - imeHeight;
            int videoWidth = container.getMeasuredWidth();

            if (videoHeight <= 0) {
                continue;
            }

            assertEquals("teclado de " + imeHeight + "px distorceu o vídeo",
                    ASPECT, (double) videoWidth / videoHeight, TOLERANCE);
        }
    }

    @Test
    public void semProporcaoDefinida_oContainerNaoSeMete() {
        Context context = ApplicationProvider.getApplicationContext();
        StreamContainer container = new StreamContainer(context, null);

        // Sem proporção negociada ainda, o comportamento é o do FrameLayout.
        measure(container, 800, 800);

        assertEquals(800, container.getMeasuredWidth());
        assertEquals(800, container.getMeasuredHeight());
    }
}
