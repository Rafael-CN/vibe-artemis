package com.limelight.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.limelight.R;

/**
 * A luz da identidade Vidro.
 *
 * Uma fonte só por tela, sempre no tom do acento, carregando três informações ao mesmo
 * tempo — e nenhuma delas por matiz, porque duas cores num mesmo elemento deixariam de
 * ser luz e virariam semáforo:
 *
 * <ul>
 *   <li><b>intensidade</b> = estado do host. Aceso 0,72 · dormindo 0,20 · nenhum host 0,07.</li>
 *   <li><b>alcance</b> = qualidade da rede. Ótima 100% · instável 78% · ruim 58%, com o
 *       croma caindo junto — a luz não só encolhe, ela esfria.</li>
 *   <li><b>posição</b> = origem, definida pelo layout que a hospeda: atrás do host na
 *       home, atrás do app aberto no catálogo, nas bordas do vídeo em sessão.</li>
 * </ul>
 *
 * Nunca muda instantaneamente. Uma luz que salta de valor lê como glitch; uma que leva
 * 900 ms lê como ambiente reagindo.
 */
public class VidroLightView extends View {

    /** Estado do host, na intensidade. */
    public static final float INTENSITY_AWAKE = 0.72f;
    public static final float INTENSITY_ASLEEP = 0.20f;
    public static final float INTENSITY_NONE = 0.07f;
    // Não existe INTENSITY_IN_SESSION. O ambilight de 0,10 que o handoff previa foi
    // retirado: em painel OLED ele acende a tela inteira e mata o preto, e por cima do
    // stream é overdraw permanente. Se alguém for reintroduzir, meça num OLED antes.

    /** Qualidade da rede, no alcance e no croma. */
    public static final int QUALITY_GOOD = 0;
    public static final int QUALITY_UNSTABLE = 1;
    public static final int QUALITY_POOR = 2;

    private static final float[] REACH = { 1.00f, 0.78f, 0.58f };
    private static final float[] CHROMA = { 1.00f, 0.70f, 0.45f };

    /** Alpha do núcleo no protótipo. A intensidade escala este valor, não o substitui. */
    private static final float CORE_ALPHA = 0.46f;

    /**
     * Diâmetro da luz em múltiplos da LARGURA da view.
     *
     * O protótipo desenha um círculo de 560 num quadro de 393 — 1,42 vez a largura. Já
     * tentamos duas outras origens: max(largura, altura) inflava a luz em tela alta, e a
     * aresta CURTA da tela consertava isso em pé mas quebrava deitado, porque num tablet
     * em paisagem a aresta curta é a altura. Num Tab S8 Ultra (1480 x 924 dp) a luz saía
     * com 866 dp de alcance, 58% da largura da tela, contra 94% no celular: o borrão
     * pequeno e concentrado que o dono anotou. A largura da view é a única medida que dá
     * a MESMA proporção nos dois aparelhos.
     *
     * O que fez a versão anterior recuar da largura — a queda passar da altura fixa em dp
     * da view e a luz ser cortada ali com uma aresta horizontal dura, ainda acesa — é o
     * que o teto vertical abaixo resolve.
     */
    private static final float DIAMETER_OF_WIDTH = 1.42f;

    /**
     * Teto do raio VERTICAL, em múltiplos da altura da view.
     *
     * Quando o raio da largura não caberia na altura, a luz deixa de ser círculo e vira
     * elipse — que é o caso de qualquer tela deitada. Com 1,4 a queda, que termina em 66%
     * do raio, chega a zero a 0,9 da altura: apaga dentro da view, e nenhuma borda de
     * baixo corta luz ainda acesa. Em pé o teto quase não morde (num celular de 393 dp a
     * largura pede 279 dp de raio e a view mais baixa que temos, de 200 dp, permite 280),
     * então o desenho do celular continua o mesmo círculo de antes.
     */
    private static final float VERTICAL_RADIUS_CAP_OF_HEIGHT = 1.4f;

    /** Do handoff: 900 ms, ease-out. Vale para intensidade e para alcance. */
    private static final long TRANSITION_MS = 900;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Reaproveitada: ela só muda quando o shader é reconstruído, e alocar Matrix em
    // onDraw é lixo por quadro.
    private final Matrix lightMatrix = new Matrix();
    private final float[] hsv = new float[3];

    private int coreColor;
    /**
     * Onde fica o CENTRO da luz, em fração da altura da view.
     *
     * O protótipo posiciona o círculo com top:-2% e translate(-50%,-50%): o centro cai
     * ACIMA da borda de cima, e o que se vê na tela é só a metade de baixo do radial —
     * um sangramento a partir do topo. O padrão aqui era 0,5, e isso desenhava a luz
     * inteira boiando no meio da tela, com uma faixa preta entre ela e o topo. Era o
     * defeito que mais aparecia, porque toda tela com fundo herda esta view.
     */
    private static final float CENTER_Y_DEFAULT = -0.02f;

    private float centerY = CENTER_Y_DEFAULT;
    private float intensity = INTENSITY_NONE;
    private float intensityTarget = INTENSITY_NONE;
    private float reach = REACH[QUALITY_GOOD];
    private float chroma = CHROMA[QUALITY_GOOD];

    private ValueAnimator intensityAnimator;
    private ValueAnimator reachAnimator;
    private ValueAnimator chromaAnimator;

    /**
     * Só reconstruímos o shader quando algo que o define mudou.
     *
     * A intensidade NÃO está nesta lista: ela é multiplicador linear do alpha e entra pelo
     * paint, na hora de desenhar. Enquanto estava aqui, cada quadro da transição de 900 ms
     * reconstruía o RadialGradient inteiro — com conversão HSV e Matrix junto — para
     * desenhar a mesma luz um pouco mais clara.
     */
    private RadialGradient shader;
    private float shaderReach = -1;
    private float shaderChroma = -1;
    private float shaderCenterY = -1;
    private int shaderWidth = -1;
    private int shaderHeight = -1;

    public VidroLightView(Context context) {
        this(context, null);
    }

    public VidroLightView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VidroLightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        coreColor = getResources().getColor(R.color.vidro_quente_halo);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VidroLightView);
            try {
                intensity = a.getFloat(R.styleable.VidroLightView_vidro_intensity, INTENSITY_NONE);
                int quality = a.getInt(R.styleable.VidroLightView_vidro_quality, QUALITY_GOOD);
                reach = REACH[clampQuality(quality)];
                chroma = CHROMA[clampQuality(quality)];
                coreColor = a.getColor(R.styleable.VidroLightView_vidro_core_color, coreColor);
                centerY = a.getFloat(R.styleable.VidroLightView_vidro_center_y, CENTER_Y_DEFAULT);
            }
            finally {
                a.recycle();
            }

            intensityTarget = intensity;
        }

        // A luz é decoração pura: não recebe toque e não entra na árvore de acessibilidade.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private static int clampQuality(int quality) {
        return quality < 0 ? 0 : (quality >= REACH.length ? REACH.length - 1 : quality);
    }

    /**
     * Estado do host. Use as constantes {@code INTENSITY_*} — os valores são medidos,
     * não escolhidos.
     */
    /**
     * A COR DA LUZ É O ESTADO DO LUGAR, e antes disto ela não era nada.
     *
     * A luz era um azul fixo no topo de toda tela — sempre ligada, sempre igual, com um eixo
     * só de variação. Um sinal que está sempre no mesmo lugar e nunca muda deixa de ser lido:
     * vira papel de parede. E a verificação que provou isso é dura: {@code setQuality()} não
     * tinha um único chamador no app inteiro, ou seja, dois terços do sistema de luz que a
     * especificação descreve nunca aconteceram.
     *
     * <p>Agora ela é quente quando há máquina acordada, fria quando só há máquina dormindo, e
     * neutra quando não há nenhuma. Ela diz a mesma coisa que a lista abaixo dela — e é a
     * primeira coisa que se vê, antes de ler qualquer palavra.
     *
     * @param corRes cor do núcleo, de {@code R.color}
     */
    public void setTemperatura(int corRes) {
        int nova = getResources().getColor(corRes);
        if (nova == coreColor) {
            return;
        }
        coreColor = nova;
        invalidate();
    }

    public void setIntensity(float target, boolean animate) {
        // O epsilon é contra o ALVO da transição em curso, e ANTES de cancelar.
        //
        // Cancelar primeiro e comparar com o valor instantâneo fazia uma chamada repetida
        // com o mesmo alvo cortar a curva no meio e recomeçar um ease-out de 900 ms a
        // partir de onde ela estava. Como a home reavalia o cabeçalho a cada resposta de
        // sondagem, com mais de um host isso se repetia antes de a curva terminar: a luz
        // rastejava em direção ao alvo em vez de pousar nele.
        //
        // Sem animador em curso o alvo guardado pode estar velho — onDetachedFromWindow
        // cancela sem desfazê-lo —, e aí quem vale é o valor que está na tela.
        float alvoAtual = intensityAnimator != null ? intensityTarget : intensity;
        if (animate && Math.abs(target - alvoAtual) < 0.001f) {
            return;
        }

        if (intensityAnimator != null) {
            intensityAnimator.cancel();
            intensityAnimator = null;
        }

        intensityTarget = target;

        if (!animate) {
            intensity = target;
            invalidate();
            return;
        }

        intensityAnimator = animateTo(intensity, target, new FloatSink() {
            @Override
            public void set(float value) {
                intensity = value;
            }
        });
    }

    /** Qualidade da rede. Encolhe o alcance e esfria o croma no mesmo gesto. */
    public void setQuality(int quality, boolean animate) {
        int q = clampQuality(quality);
        float targetReach = REACH[q];
        float targetChroma = CHROMA[q];

        if (reachAnimator != null) {
            reachAnimator.cancel();
            reachAnimator = null;
        }
        if (chromaAnimator != null) {
            chromaAnimator.cancel();
            chromaAnimator = null;
        }

        if (!animate) {
            reach = targetReach;
            chroma = targetChroma;
            invalidate();
            return;
        }

        reachAnimator = animateTo(reach, targetReach, new FloatSink() {
            @Override
            public void set(float value) {
                reach = value;
            }
        });
        chromaAnimator = animateTo(chroma, targetChroma, new FloatSink() {
            @Override
            public void set(float value) {
                chroma = value;
            }
        });
    }

    private interface FloatSink {
        void set(float value);
    }

    private ValueAnimator animateTo(float from, float to, final FloatSink sink) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(TRANSITION_MS);
        // ease-out do handoff, o mesmo da mola do toque — uma curva só para os dois.
        animator.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.vidro_out));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sink.set((Float)animation.getAnimatedValue());
                invalidate();
            }
        });
        animator.start();
        return animator;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Um animador de 900 ms sobrevive à view se ninguém o parar, e volta a chamar
        // invalidate() numa view que já saiu da árvore.
        if (intensityAnimator != null) {
            intensityAnimator.cancel();
            intensityAnimator = null;
        }
        if (reachAnimator != null) {
            reachAnimator.cancel();
            reachAnimator = null;
        }
        if (chromaAnimator != null) {
            chromaAnimator.cancel();
            chromaAnimator = null;
        }
    }

    /** Desaturação em HSV: a luz esfria em direção ao neutro sem trocar de matiz. */
    private int coolTowardNeutral(int color, float amount) {
        if (amount >= 0.999f) {
            return color;
        }

        Color.colorToHSV(color, hsv);
        hsv[1] *= amount;
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0 || intensity <= 0.001f || reach <= 0.001f) {
            return;
        }

        boolean stale = shader == null
                || shaderWidth != width
                || shaderHeight != height
                || Math.abs(shaderReach - reach) > 0.001f
                || Math.abs(shaderChroma - chroma) > 0.001f
                || Math.abs(shaderCenterY - centerY) > 0.001f;

        if (stale) {
            float radiusX = width * DIAMETER_OF_WIDTH * 0.5f * reach;
            float radiusY = Math.min(radiusX, height * VERTICAL_RADIUS_CAP_OF_HEIGHT * reach);
            int tinted = coolTowardNeutral(coreColor, chroma);

            // O protótipo desenha a luz como `radial-gradient(circle, acento/.46 0%,
            // transparent 66%)`. Usar a intensidade crua como alpha do núcleo fazia dela
            // um holofote: no emulador ela dominava a tela em vez de acompanhá-la. A
            // intensidade escala esse 0,46 — ela diz o estado, não o brilho absoluto. A
            // multiplicação está no paint, logo abaixo; aqui o gradiente nasce em cheio.
            int core = (tinted & 0x00FFFFFF) | (Math.round(255 * CORE_ALPHA) << 24);
            int mid = (tinted & 0x00FFFFFF) | (Math.round(255 * CORE_ALPHA * 0.30f) << 24);
            int edge = tinted & 0x00FFFFFF;

            // Nasce na origem e vai para o lugar pela matriz abaixo. RadialGradient só
            // sabe desenhar círculo; a elipse existe pela escala em Y aplicada por fora.
            shader = new RadialGradient(
                    0f, 0f, radiusX,
                    new int[] { core, mid, edge },
                    // A queda precisa ser rápida no meio e longa na borda, senão a luz
                    // vira um disco com contorno visível em vez de derramar.
                    // Corta em 66% do raio, como no protótipo: além disso a mancha
                    // ganha borda visível e deixa de parecer luz.
                    new float[] { 0f, 0.33f, 0.66f },
                    Shader.TileMode.CLAMP);

            // Escala antes da translação: na ordem inversa o centro também seria achatado
            // e a luz subiria para fora da view.
            lightMatrix.setScale(1f, radiusY / radiusX);
            lightMatrix.postTranslate(width * 0.5f, height * centerY);
            shader.setLocalMatrix(lightMatrix);

            paint.setShader(shader);

            shaderWidth = width;
            shaderHeight = height;
            shaderReach = reach;
            shaderChroma = chroma;
            shaderCenterY = centerY;
        }

        // O alpha do paint multiplica a saída do shader, então a intensidade produz aqui
        // exatamente a mesma cor que produzia dentro do gradiente — e sem reconstruir
        // nada, que é o que faz a transição de 900 ms sair de graça.
        paint.setAlpha(Math.min(255, Math.round(255 * intensity)));

        canvas.drawRect(0, 0, width, height, paint);
    }
}
