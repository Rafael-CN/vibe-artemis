package com.limelight.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.limelight.R;

/**
 * A moldura da imagem — o estado da sessão, sem um caractere de texto.
 *
 * <h2>O que isto substitui</h2>
 *
 * A sessão dizia como estava a rede de cinco jeitos ao mesmo tempo: uma faixa de aviso no
 * topo, dois sobrepostos de números (o grande e o lite), e dois pontos coloridos — um na ilha
 * e um na pílula, mostrando a mesma cor em dois cantos. Cinco peças para um dado, e todas
 * elas <b>para ler</b>: durante o jogo, ler é a única coisa que a pessoa não pode fazer.
 *
 * <p>Aqui é uma peça só, e ela não é lida — é percebida. Quando a rede piora, a borda da
 * imagem esfria e engrossa; a atenção continua no jogo e a informação chega pelo canto do
 * olho. É o que se faz quando o dado importa mas a atenção não deve sair do conteúdo.
 *
 * <h2>Por que ela mora dentro do StreamContainer</h2>
 *
 * Porque o {@code StreamContainer} já faz letterbox de todos os filhos para a proporção do
 * vídeo. Sendo filha dele, a moldura recebe de graça exatamente o retângulo da imagem — e não
 * o da janela. Numa sessão 16:9 num tablet 4:3 a diferença é uma faixa preta de cada lado: uma
 * moldura na borda da janela estaria emoldurando o vazio.
 *
 * <h2>O estado do meio, que nunca existiu</h2>
 *
 * {@code VidroIsland.setQuality()} só era chamado com BOM e RUIM. O aviso prévio — o momento
 * em que dá para fazer alguma coisa a respeito — não existia na tela. Aqui ele é
 * {@link #SEGURANDO}, e é o estado que justifica a peça: uma moldura que só acende quando já
 * está ruim chega tarde.
 *
 * <p><b>Ressalva honesta:</b> o limiar do estado do meio ainda não foi medido contra o
 * Vibeshine. Enquanto ninguém o alimentar, ele simplesmente não acontece — e o que fica é o
 * mesmo par BOM/RUIM de antes, numa peça só em vez de cinco. Moldura que pisca na periferia é
 * pior que a faixa que ela substituiu, então o estado do meio só deve ser ligado com medida na
 * mão.
 */
public class MolduraDeEstado extends View {

    /** Tudo fluindo. A moldura não existe. */
    public static final int BOM = 0;
    /** A rede está segurando — o aviso prévio. Ver a ressalva na documentação da classe. */
    public static final int SEGURANDO = 1;
    /** Ruim de verdade. */
    public static final int RUIM = 2;

    /**
     * Novecentos milissegundos, e a lentidão é o ponto.
     *
     * <p>Esta é a exceção declarada à regra de que movimento é mola: uma mola descreve uma
     * coisa <i>indo a um lugar</i>, e aqui nada vai a lugar nenhum — é uma cor virando outra.
     * Rápido, a troca vira um evento e puxa o olho para fora do jogo, que é exatamente o que a
     * peça existe para evitar. Devagar, ela é notada sem interromper.
     *
     * <p>É o mesmo número que a tabela de movimento dava para "a luz muda de estado", e pelo
     * mesmo motivo.
     */
    private static final int TRAVESSIA_MS = 900;

    private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF quadro = new RectF();
    private final ArgbEvaluator mistura = new ArgbEvaluator();

    private final float raio;
    private final float[] espessuras = new float[3];
    private final int[] cores = new int[3];

    private int estado = BOM;
    private float espessuraAtual;
    private int corAtual;
    private ValueAnimator travessia;

    public MolduraDeEstado(Context context, AttributeSet attrs) {
        super(context, attrs);

        tinta.setStyle(Paint.Style.STROKE);
        raio = dp(18f);

        // BOM tem espessura zero: a moldura não fica fina, ela deixa de existir. Um fio
        // permanente na borda seria mais um inquilino do jogo, que é o que se está tirando.
        espessuras[BOM] = 0f;
        espessuras[SEGURANDO] = dp(3f);
        espessuras[RUIM] = dp(6f);

        int frio = ContextCompat.getColor(context, R.color.vidro_accent_fill);
        cores[BOM] = frio & 0x00FFFFFF;
        cores[SEGURANDO] = (0x66 << 24) | (frio & 0x00FFFFFF);
        cores[RUIM] = (0xA0 << 24) | (frio & 0x00FFFFFF);

        espessuraAtual = espessuras[BOM];
        corAtual = cores[BOM];

        // Ela não participa de entrada nenhuma. Um toque na moldura é um toque no jogo.
        setClickable(false);
        setFocusable(false);
    }

    private float dp(float valor) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics());
    }

    public void setEstado(int novo) {
        if (novo < BOM || novo > RUIM || novo == estado) {
            return;
        }
        estado = novo;

        final float deEspessura = espessuraAtual;
        final int deCor = corAtual;
        final float paraEspessura = espessuras[novo];
        final int paraCor = cores[novo];

        if (travessia != null) {
            travessia.cancel();
        }
        travessia = ValueAnimator.ofFloat(0f, 1f);
        travessia.setDuration(TRAVESSIA_MS);
        travessia.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                float t = (Float) animator.getAnimatedValue();
                espessuraAtual = deEspessura + (paraEspessura - deEspessura) * t;
                corAtual = (Integer) mistura.evaluate(t, deCor, paraCor);
                // Só invalidate: nada aqui mede nem posiciona. A moldura é desenho puro, e é
                // por isso que ela pode existir sobre o vídeo sem entrar no orçamento dele.
                invalidate();
            }
        });
        travessia.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (travessia != null) {
            travessia.cancel();
            travessia = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (espessuraAtual <= 0.5f) {
            return;
        }
        float meio = espessuraAtual / 2f;
        quadro.set(meio, meio, getWidth() - meio, getHeight() - meio);
        tinta.setStrokeWidth(espessuraAtual);
        tinta.setColor(corAtual);
        canvas.drawRoundRect(quadro, raio, raio, tinta);
    }
}
