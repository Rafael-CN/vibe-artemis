package com.limelight.grid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * FrameLayout que força a altura a 4/3 da largura medida — a capa 3:4 em pé do card de
 * app no celular (app_grid_item.xml).
 *
 * G1: a capa tinha altura FIXA de 132dp com largura match_parent. Numa grade de 2
 * colunas (app_grid_view.xml), a coluna mede o que sobra da tela — ~190dp num celular de
 * 412dp — então a capa saía deitada (~190x132), quando o desenho pede 3:4 em pé (é a
 * mesma proporção de console_capa_w/h = 160x213dp, usada no carrossel do tablet). Uma
 * dimen fixa não serve porque a largura da coluna varia por aparelho (stretchMode
 * columnWidth); esta view mede a altura a partir da própria largura, então a proporção
 * se mantém em qualquer tela sem esticar nem cortar a imagem (o ImageView interno
 * continua com centerCrop).
 */
public class CapaAspectFrameLayout extends FrameLayout {
    private static final float RATIO_W = 3f;
    private static final float RATIO_H = 4f;

    public CapaAspectFrameLayout(Context context) {
        super(context);
    }

    public CapaAspectFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CapaAspectFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width * RATIO_H / RATIO_W);
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }
}
