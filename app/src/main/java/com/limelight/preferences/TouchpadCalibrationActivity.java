package com.limelight.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.limelight.R;
import com.limelight.binding.input.TouchpadCalibration;
import com.limelight.ui.VidroToast;
import com.limelight.utils.UiHelper;

/**
 * Ensina ao app como o trackpad da capa está orientado, medindo dois gestos.
 *
 * Isto existe porque deduzir não funcionou. A regra "um trackpad é mais largo que alto,
 * então alcance de Y maior significa girado" morreu no aparelho do dono: a capa do S8 Ultra
 * reporta X [0..2959] e Y [0..1847] — a resolução da TELA —, ou seja ela é mapeada em
 * absoluto sobre o display e sempre se anuncia deitada, por mais girado que o movimento
 * chegue. Nenhuma propriedade que dá para consultar de fora distingue os casos.
 *
 * Dois gestos distinguem. Deslize para a direita, deslize para baixo, e a orientação está
 * resolvida para aquele hardware — sem ninguém precisar entender o que "trocar eixos mais
 * eixo Y a -100%" quer dizer.
 */
public class TouchpadCalibrationActivity extends AppCompatActivity {

    /** Chave da preferência onde a orientação medida fica guardada. */
    public static final String PREF_KEY = "touchpad_calibration";

    private static final int PASSO_DIREITA = 0;
    private static final int PASSO_BAIXO = 1;
    private static final int PASSO_PRONTO = 2;

    private TextView instruction;
    private TextView hint;

    private int passo = PASSO_DIREITA;
    private boolean medindo;
    private float startX, startY;
    private float rightDx, rightDy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad_calibration);
        UiHelper.notifyNewRootView(this);

        instruction = findViewById(R.id.calibrationInstruction);
        hint = findViewById(R.id.calibrationHint);

        findViewById(R.id.calibrationCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // O alvo do gesto precisa do foco, senão os eventos do pad vão para o cursor do
        // sistema e nunca chegam aqui.
        View alvo = findViewById(R.id.calibrationTarget);
        alvo.setFocusable(true);
        alvo.setFocusableInTouchMode(true);
        alvo.requestFocus();

        mostrarPasso();
    }

    private void mostrarPasso() {
        switch (passo) {
            case PASSO_DIREITA:
                instruction.setText(R.string.vidro_calibration_right);
                hint.setText(R.string.vidro_calibration_hint);
                break;
            case PASSO_BAIXO:
                instruction.setText(R.string.vidro_calibration_down);
                hint.setText(R.string.vidro_calibration_hint);
                break;
            default:
                instruction.setText(R.string.vidro_calibration_done);
                hint.setText("");
                break;
        }
    }

    /**
     * O pad entrega movimento sem toque na tela, então é por aqui que ele chega — não por
     * onTouchEvent.
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (passo == PASSO_PRONTO || !ehTrackpad(event)) {
            return super.onGenericMotionEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_DOWN:
                comecar(event);
                return true;

            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_MOVE:
                if (!medindo) {
                    comecar(event);
                }
                return true;

            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                terminar(event);
                return true;

            default:
                return super.onGenericMotionEvent(event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Alguns drivers entregam o pad como toque em vez de hover; o caminho é o mesmo.
        if (passo != PASSO_PRONTO && ehTrackpad(event)) {
            return onGenericMotionEvent(event);
        }
        return super.onTouchEvent(event);
    }

    private boolean ehTrackpad(MotionEvent event) {
        InputDevice dev = event.getDevice();
        if (dev == null) {
            return false;
        }
        int sources = dev.getSources();
        return (sources & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD;
    }

    private void comecar(MotionEvent event) {
        medindo = true;
        startX = event.getX();
        startY = event.getY();
    }

    private void terminar(MotionEvent event) {
        if (!medindo) {
            return;
        }
        medindo = false;

        float dx = event.getX() - startX;
        float dy = event.getY() - startY;

        if (passo == PASSO_DIREITA) {
            rightDx = dx;
            rightDy = dy;
            passo = PASSO_BAIXO;
            mostrarPasso();
            return;
        }

        TouchpadCalibration calibracao =
                TouchpadCalibration.fromGestures(rightDx, rightDy, dx, dy);
        if (calibracao == null) {
            // Gesto curto ou torto demais. Voltar ao começo é melhor que gravar uma
            // orientação ao acaso, que daria um cursor pior do que o de agora.
            passo = PASSO_DIREITA;
            mostrarPasso();
            VidroToast.show(this, R.string.vidro_calibration_retry);
            return;
        }

        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString(PREF_KEY, calibracao.serialize()).apply();

        passo = PASSO_PRONTO;
        mostrarPasso();
        instruction.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 900);
    }
}
