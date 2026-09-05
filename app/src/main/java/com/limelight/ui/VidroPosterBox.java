package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Caixa da capa no catálogo: a altura sai da largura, na proporção da arte.
 *
 * A altura era cravada em 132dp enquanto a largura acompanhava a coluna da grade. Num
 * celular a conta fechava — 170dp de coluna por 132 de altura, que é o card do protótipo.
 * No tablet em paisagem a mesma coluna passa de 400dp e a caixa saía deitada em mais de
 * 3:1: com a capa desenhada por centerCrop dentro dela, a arte perdia topo e base, e o
 * rótulo que o host assa dentro da própria box art saía cortado ao meio pela borda de
 * baixo do card.
 *
 * Amarrar a altura à largura acabou com o corte, mas em proporção FIXA (4:3) trocou um
 * defeito por outro: a capa que não é 4:3 desenha por fitCenter e sobra o fundo listrado
 * do placeholder nas laterais ou em cima e embaixo. Por isso a proporção agora é ajustável
 * — o adapter mede a box art que o host mandou e passa a proporção dela para cá, e a capa
 * volta a preencher a caixa inteira: sem corte E sem sobra.
 *
 * A proporção é UMA para a grade inteira, não uma por card. Numa GridView a fileira toma a
 * altura do card mais alto, então card com altura própria só empurra a sobra para fora da
 * capa — vira calha morta ao lado dos cards mais baixos, e desalinha o nome de um card
 * para o do vizinho. Como o host publica toda a box art no mesmo tamanho, uma proporção só
 * zera a sobra dos dois jeitos. Quem decide qual é essa proporção é AppGridAdapter.
 *
 * 4:3 fica como palpite até a primeira capa carregar: é o card do protótipo (170x132)
 * arredondado, então antes de a arte chegar a grade está onde já estava.
 */
public class VidroPosterBox extends FrameLayout {
    private static final int DEFAULT_ART_WIDTH = 4;
    private static final int DEFAULT_ART_HEIGHT = 3;

    // Trava contra arte degenerada: um asset de 300x2000 faria um card mais alto que a
    // tela e a grade viraria uma coluna de tiras. Fora da faixa a capa volta a sobrar
    // fundo, que é o mal menor.
    private static final float MIN_ART_RATIO = 0.5f;
    private static final float MAX_ART_RATIO = 2.0f;

    private int artWidth = DEFAULT_ART_WIDTH;
    private int artHeight = DEFAULT_ART_HEIGHT;

    public VidroPosterBox(Context context) {
        super(context);
    }

    public VidroPosterBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VidroPosterBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Proporção em pixels da box art que o host publicou. Medida inválida é ignorada, e
     * enquanto ninguém chamar vale o palpite de 4:3.
     */
    public void setArtAspect(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float ratio = (float) width / height;
        if (ratio < MIN_ART_RATIO || ratio > MAX_ART_RATIO) {
            return;
        }

        // Comparação cruzada, não campo a campo: 600x800 e 300x400 são a mesma forma e não
        // valem uma remedida da grade inteira.
        if (width * artHeight == height * artWidth) {
            return;
        }

        artWidth = width;
        artHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);

        // Sem largura conhecida não há de onde tirar altura. Acontece em medida
        // UNSPECIFIED — pai que pergunta "de que tamanho você quer ser?" —, e aí vale o
        // comportamento normal do FrameLayout em vez de uma caixa de altura zero.
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED || width <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(width * artHeight / artWidth, MeasureSpec.EXACTLY));
    }
}
