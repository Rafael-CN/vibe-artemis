package com.limelight;

import com.limelight.ui.VidroToast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.limelight.utils.DeviceUtils;

import com.limelight.ui.compose.PonteDaDepuracao;
import com.limelight.utils.UiHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * A tela de depuracao, agora leitura e um botao Copiar.
 *
 * Ela implementava OnClickListener por causa dos quatro botoes de teste -- vibrar o
 * aparelho, atualizar a lista de gamepads, vibrar o gamepad, e um contador de amplitude --
 * que apareciam com o texto cortado. Testar vibracao e papel do slider de vibracao, nao de
 * um painel a parte. Sem botoes, sem listener.
 */
public class DebugInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_axitest);

        UiHelper.notifyNewRootView(this);

        androidx.compose.ui.platform.ComposeView tela = findViewById(R.id.vidroDepuracaoCompose);
        final String resumo = montarResumo();
        final String dispositivos = montarDispositivos();

        PonteDaDepuracao.montar(
                tela,
                resumo,
                dispositivos,
                () -> { finish(); return kotlin.Unit.INSTANCE; },
                () -> {
                    copiar(resumo + "\n\n" + dispositivos);
                    return kotlin.Unit.INSTANCE;
                });
    }

    /** O aparelho, o Android e a versao do app. O que se cola quando se pede ajuda. */
    private String montarResumo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.debug_info_android_version)).append(DeviceUtils.getSDKVersionName());
        sb.append("\t").append(getString(R.string.debug_info_api_version)).append(Build.VERSION.SDK_INT);
        sb.append("\n").append(getString(R.string.debug_info_kernel_version)).append(android.os.Build.VERSION.INCREMENTAL);
        sb.append("\n").append(getString(R.string.debug_info_brand_model))
          .append(DeviceUtils.getManufacturer()).append("\t-\t").append(DeviceUtils.getModel());
        sb.append("\n").append("VibeArtemis ").append(com.limelight.BuildConfig.VERSION_NAME);
        return sb.toString();
    }

    /**
     * Fontes e faixas de eixo de cada dispositivo de entrada.
     *
     * Foi este bloco que permitiu descobrir que a capa do S8 Ultra se anuncia deitada, com
     * faixas de eixo do tamanho da TELA. E diagnostico de verdade, e por isso ele fica --
     * recolhido, e sempre dentro do que o Copiar leva.
     */
    private String montarDispositivos() {
        StringBuilder sb = new StringBuilder();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null) {
                continue;
            }
            sb.append(d.getName()).append("  [id ").append(id).append("]\n");
            sb.append("  sources 0x").append(Integer.toHexString(d.getSources())).append("\n");
            for (InputDevice.MotionRange r : d.getMotionRanges()) {
                sb.append("  axis ").append(MotionEvent.axisToString(r.getAxis()))
                  .append("  ").append(r.getMin()).append(" .. ").append(r.getMax())
                  .append("\n");
            }
            sb.append("\n");
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    private void copiar(String tudo) {
        android.content.ClipboardManager cm =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("VibeArtemis", tudo));
        }
    }
}
