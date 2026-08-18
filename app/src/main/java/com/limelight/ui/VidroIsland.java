package com.limelight.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.limelight.R;

/**
 * A ilha de controle da sessão, e a pílula em que ela vira.
 *
 * A tese diz que em sessão o vídeo manda: a interface aparece, deixa a pessoa fazer o
 * que precisa e some sozinha em 6,2 s. Quem some sozinho não precisa de botão de fechar,
 * e quem volta ao toque não precisa de botão de abrir — por isso não existe nenhum dos
 * dois aqui.
 *
 * Qualidade é dita em PALAVRA. Número só quando a pessoa liga "Números na tela": no meio
 * de um jogo, "34 ms" não informa nada que "oscilando um pouco" não informe melhor.
 */
public class VidroIsland {

    /** Do handoff: 6,2 s. */
    private static final long COLLAPSE_DELAY_MS = 6200;
    /** Ilha recolhe · 260 ms · cubic-bezier(.4,0,.2,1). */
    private static final long COLLAPSE_MS = 260;

    public static final int QUALITY_GOOD = 0;
    public static final int QUALITY_UNSTABLE = 1;
    public static final int QUALITY_POOR = 2;

    private static final int[] QUALITY_PHRASE = {
            R.string.vidro_quality_good,
            R.string.vidro_quality_holding,
            R.string.vidro_quality_poor
    };

    private final View island;
    private final View pill;
    private final View pillDot;
    private final TextView appName;
    private final TextView quality;
    private final VidroLightView light;
    private final Interpolator collapseCurve;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable collapse = new Runnable() {
        @Override
        public void run() {
            setOpen(false, true);
        }
    };

    private boolean open;

    public VidroIsland(Activity activity) {
        island = activity.findViewById(R.id.vidroIsland);
        pill = activity.findViewById(R.id.vidroPill);
        pillDot = activity.findViewById(R.id.vidroPillDot);
        appName = activity.findViewById(R.id.vidroIslandApp);
        quality = activity.findViewById(R.id.vidroIslandQuality);
        light = activity.findViewById(R.id.vidroSessionLight);
        collapseCurve = AnimationUtils.loadInterpolator(activity, R.interpolator.vidro_standard);

        if (light != null) {
            // Ambilight: presença, não brilho. Fica no piso e não se anuncia.
            light.setIntensity(VidroLightView.INTENSITY_IN_SESSION, false);
            light.setVisibility(View.VISIBLE);
        }
    }

    public boolean isAvailable() {
        return island != null && pill != null;
    }

    public View getIsland() {
        return island;
    }

    public View getPill() {
        return pill;
    }

    /** Nome do app corrente, que é o título da sessão. */
    public void setAppName(String name) {
        if (appName != null) {
            appName.setText(name);
        }
    }

    /**
     * Qualidade da rede: uma palavra na ilha, o ponto da pílula e o alcance da luz.
     * As três precisam concordar sempre — são a mesma informação em três lugares.
     */
    public void setQuality(int level) {
        int q = level < 0 ? 0 : (level >= QUALITY_PHRASE.length ? QUALITY_PHRASE.length - 1 : level);

        if (quality != null) {
            quality.setText(QUALITY_PHRASE[q]);
        }
        if (pillDot != null) {
            pillDot.setBackgroundResource(q == QUALITY_POOR
                    ? R.drawable.vidro_dot_far : R.drawable.vidro_dot_on);
        }
        if (light != null) {
            light.setQuality(q, true);
        }
    }

    /** Abre a ilha e rearma o temporizador. É o que o toque na tela chama. */
    public void reopen() {
        setOpen(true, true);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean shouldOpen, boolean animate) {
        if (!isAvailable()) {
            return;
        }

        handler.removeCallbacks(collapse);
        open = shouldOpen;

        View entering = shouldOpen ? island : pill;
        View leaving = shouldOpen ? pill : island;

        if (!animate) {
            entering.setAlpha(1f);
            entering.setVisibility(View.VISIBLE);
            leaving.setVisibility(View.GONE);
        }
        else {
            crossFade(entering, leaving);
        }

        if (shouldOpen) {
            handler.postDelayed(collapse, COLLAPSE_DELAY_MS);
        }
    }

    private void crossFade(final View entering, final View leaving) {
        if (entering.getVisibility() != View.VISIBLE) {
            entering.setAlpha(0f);
            entering.setVisibility(View.VISIBLE);
        }

        entering.animate().cancel();
        leaving.animate().cancel();

        // Só alpha e transform. Animar largura pediria layout por quadro, e durante a
        // sessão cada quadro gasto na interface é um quadro tirado do vídeo.
        entering.animate()
                .alpha(1f)
                .setDuration(COLLAPSE_MS)
                .setInterpolator(collapseCurve)
                .start();

        leaving.animate()
                .alpha(0f)
                .setDuration(COLLAPSE_MS)
                .setInterpolator(collapseCurve)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        leaving.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    /** Estado de um bloco: rótulo fixo, valor em palavra. */
    public void setTile(int tileId, int labelRes, String value) {
        if (island == null) {
            return;
        }

        View tile = island.findViewById(tileId);
        if (tile == null) {
            return;
        }

        TextView label = tile.findViewById(R.id.tile_label);
        TextView valueView = tile.findViewById(R.id.tile_value);
        if (label != null) {
            label.setText(labelRes);
        }
        if (valueView != null) {
            valueView.setText(value);
        }
    }

    public void setTileClickListener(int tileId, View.OnClickListener listener) {
        if (island == null) {
            return;
        }

        View tile = island.findViewById(tileId);
        if (tile != null) {
            tile.setOnClickListener(listener);
        }
    }

    /** Some por completo — usado quando outra coisa toma a tela. */
    public void hide() {
        handler.removeCallbacks(collapse);
        if (island != null) {
            island.setVisibility(View.GONE);
        }
        if (pill != null) {
            pill.setVisibility(View.GONE);
        }
    }

    public void destroy() {
        handler.removeCallbacks(collapse);
    }
}
