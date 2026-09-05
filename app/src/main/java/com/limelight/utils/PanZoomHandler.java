package com.limelight.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.limelight.Game;
import com.limelight.LimeLog;
import com.limelight.preferences.PreferenceConfiguration;

public class PanZoomHandler {
    static private final float MAX_SCALE = 10.0f;

    private final Game game;
    private final View streamView;
    private final PreferenceConfiguration prefConfig;
    private final boolean isTopMode;
    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;
    private View parent;
    private float scaleFactor = 1.0f;
    private float childX, childY = 0;
    private float parentWidth, parentHeight = 0;
    private float childWidth, childHeight = 0;

    public PanZoomHandler(Context context, Game game, View streamView, View parent, PreferenceConfiguration prefConfig) {
        this.game = game;
        this.streamView = streamView;
        this.parent = parent;
        this.prefConfig = prefConfig;
        this.isTopMode = prefConfig.alignDisplayTopCenter;
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        // Everything gets easier with 0,0 as the pivot point
        streamView.setPivotX(0);
        streamView.setPivotY(0);
    }

    public void handleTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        gestureDetector.onTouchEvent(motionEvent);
    }

    private void updateDimensions() {
        childHeight = streamView.getHeight() * scaleFactor;
        childWidth = streamView.getWidth() * scaleFactor;
        parentWidth = parent.getWidth();
        parentHeight = parent.getHeight();
    }

    private void constrainToBounds() {
        updateDimensions();

        if (parentWidth >= childWidth) {
            childX = (parentWidth - childWidth) / 2;
        } else {
            float boundaryX = parentWidth - childWidth;
            childX = Math.max(boundaryX, Math.min(childX, 0));
        }

        if (parentHeight >= childHeight) {
            if (isTopMode) {
                childY = 0;
            } else {
                childY = (parentHeight - childHeight) / 2;
            }
        } else {
            float boundaryY = parentHeight - childHeight;
            childY = Math.max(boundaryY, Math.min(childY, 0));
        }

        streamView.setX(childX);
        streamView.setY(childY);
    }

    public void handleSurfaceChange() {
        if (childWidth == 0 || parent == null) {
            // Retrieve parent, should handle both built-in display and external display
            parent = (View)streamView.getParent();
            return;
        }

        float prevChildWidth = childWidth;
        float prevChildHeight = childHeight;
        float prevParentWidth = parentWidth;
        float prevParentHeight = parentHeight;

        updateDimensions();

        float viewScaleX = childWidth / prevChildWidth;
        float viewScaleY = childHeight / prevChildHeight;

        float dPivotX1 = childX - prevParentWidth / 2;
        float dPivotY1 = childY - prevParentHeight / 2;

        float dPivotX2 = dPivotX1 * viewScaleX;
        float dPivotY2 = dPivotY1 * viewScaleY;

        childX = dPivotX2 + parentWidth / 2;
        childY = dPivotY2 + parentHeight / 2;

        streamView.setX(childX);
        streamView.setY(childY);

        constrainToBounds();
    }

    /**
     * Aplica um fator de escala em torno de um ponto.
     *
     * Estava dentro do onScale do ScaleGestureDetector, que só existe para o toque na
     * tela. A pinça do trackpad não passa por detector nenhum — ela chega como um número
     * pronto num eixo do MotionEvent —, então a conta precisa morar num lugar que os dois
     * caminhos possam chamar.
     */
    public void applyScale(float factor, float focusX, float focusY) {
        float newScaleFactor = scaleFactor * factor;
        newScaleFactor = Math.max(1, Math.min(newScaleFactor, MAX_SCALE)); // Apply minimum scale

        float dPivotX = (childX - focusX) / scaleFactor * newScaleFactor;
        float dPivotY = (childY - focusY) / scaleFactor * newScaleFactor;

        childX = focusX + dPivotX;
        childY = focusY + dPivotY;

        scaleFactor = newScaleFactor;

        streamView.setScaleX(scaleFactor);
        streamView.setScaleY(scaleFactor);

        streamView.setX(childX);
        streamView.setY(childY);

        constrainToBounds();
    }

    /**
     * Pinça vinda do trackpad. O gesto não acontece sobre a tela, então não há ponto focal
     * para respeitar: o zoom se ancora no centro do que está visível, que é o que qualquer
     * um espera de um pad separado da imagem.
     */
    public void applyTrackpadPinch(float factor) {
        if (factor <= 0 || Math.abs(factor - 1f) < 0.0005f) {
            return;
        }
        applyScale(factor, parent.getWidth() / 2f, parent.getHeight() / 2f);
    }

    /** Já está ampliado? O menu usa isto para saber se tem zoom a desfazer. */
    public boolean isZoomed() {
        return scaleFactor > 1.0f;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            applyScale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            game.updatePipAutoEnter();
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            childX = streamView.getX() - distanceX;
            childY = streamView.getY() - distanceY;

            streamView.setX(childX);
            streamView.setY(childY);

            constrainToBounds();
            return true;
        }
    }

    public void setInitialZoomAndPan(float scale, float offsetX, float offsetY) {
        this.scaleFactor = scale;
        // apply to view
        streamView.setScaleX(scaleFactor);
        streamView.setScaleY(scaleFactor);
        this.childX = offsetX;
        this.childY = offsetY;
        streamView.setX(childX);
        streamView.setY(childY);
    }

    public float getScaleFactor() { return scaleFactor; }
    public float getChildX() { return childX; }
    public float getChildY() { return childY; }
}
