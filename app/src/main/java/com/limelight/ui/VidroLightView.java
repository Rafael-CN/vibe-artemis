package com.limelight.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

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

    /** Do handoff: 900 ms, ease-out. Vale para intensidade e para alcance. */
    private static final long TRANSITION_MS = 900;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] hsv = new float[3];

    private int coreColor;
    private float intensity = INTENSITY_NONE;
    private float reach = REACH[QUALITY_GOOD];
    private float chroma = CHROMA[QUALITY_GOOD];

    private ValueAnimator intensityAnimator;
    private ValueAnimator reachAnimator;
    private ValueAnimator chromaAnimator;

    /** Só reconstruímos o shader quando algo que o define mudou. */
    private RadialGradient shader;
    private float shaderIntensity = -1;
    private float shaderReach = -1;
    private float shaderChroma = -1;
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

        coreColor = getResources().getColor(R.color.vidro_accent_glow);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VidroLightView);
            try {
                intensity = a.getFloat(R.styleable.VidroLightView_vidro_intensity, INTENSITY_NONE);
                int quality = a.getInt(R.styleable.VidroLightView_vidro_quality, QUALITY_GOOD);
                reach = REACH[clampQuality(quality)];
                chroma = CHROMA[clampQuality(quality)];
                coreColor = a.getColor(R.styleable.VidroLightView_vidro_core_color, coreColor);
            }
            finally {
                a.recycle();
            }
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
    public void setIntensity(float target, boolean animate) {
        if (intensityAnimator != null) {
            intensityAnimator.cancel();
            intensityAnimator = null;
        }

        if (!animate) {
            intensity = target;
            invalidate();
            return;
        }

        if (Math.abs(target - intensity) < 0.001f) {
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
        animator.setInterpolator(new DecelerateInterpolator());
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
                || Math.abs(shaderIntensity - intensity) > 0.001f
                || Math.abs(shaderReach - reach) > 0.001f
                || Math.abs(shaderChroma - chroma) > 0.001f;

        if (stale) {
            float radius = Math.max(width, height) * 0.5f * reach;
            int tinted = coolTowardNeutral(coreColor, chroma);

            // O protótipo desenha a luz como `radial-gradient(circle, acento/.46 0%,
            // transparent 66%)`. Usar a intensidade crua como alpha do núcleo fazia dela
            // um holofote: no emulador ela dominava a tela em vez de acompanhá-la. A
            // intensidade escala esse 0,46 — ela diz o estado, não o brilho absoluto.
            int core = (tinted & 0x00FFFFFF) | (Math.round(255 * intensity * CORE_ALPHA) << 24);
            int mid = (tinted & 0x00FFFFFF) | (Math.round(255 * intensity * CORE_ALPHA * 0.30f) << 24);
            int edge = tinted & 0x00FFFFFF;

            shader = new RadialGradient(
                    width * 0.5f, height * 0.5f, radius,
                    new int[] { core, mid, edge },
                    // A queda precisa ser rápida no meio e longa na borda, senão a luz
                    // vira um disco com contorno visível em vez de derramar.
                    // Corta em 66% do raio, como no protótipo: além disso a mancha
                    // ganha borda visível e deixa de parecer luz.
                    new float[] { 0f, 0.33f, 0.66f },
                    Shader.TileMode.CLAMP);

            paint.setShader(shader);

            shaderWidth = width;
            shaderHeight = height;
            shaderIntensity = intensity;
            shaderReach = reach;
            shaderChroma = chroma;
        }

        canvas.drawRect(0, 0, width, height, paint);
    }
}
