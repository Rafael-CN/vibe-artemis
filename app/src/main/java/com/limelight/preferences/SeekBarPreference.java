package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.limelight.R;

import java.util.Locale;

// Based on a Stack Overflow example: http://stackoverflow.com/questions/1974193/slider-on-my-preferencescreen
public class SeekBarPreference extends Preference
{
    private static final String ANDROID_SCHEMA_URL = "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR_SCHEMA_URL = "http://schemas.moonlight-stream.com/apk/res/seekbar";

    private AlertDialog dialog;
    private SeekBar seekBar;
    private TextView valueText;
    private final Context context;

    private final String dialogMessage;
    private final String suffix;
    private final int defaultValue;
    private final int maxValue;
    private final int minValue;
    private final int stepSize;
    private final int keyStepSize;
    private final int divisor;
    private final boolean zeroIsAuto;
    private int currentValue;

    private final int seekbarMax;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Read the message from XML
        int dialogMessageId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "dialogMessage", 0);
        if (dialogMessageId == 0) {
            dialogMessage = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "dialogMessage");
        }
        else {
            dialogMessage = context.getString(dialogMessageId);
        }

        // Get the suffix for the number displayed in the dialog
        int suffixId = attrs.getAttributeResourceValue(ANDROID_SCHEMA_URL, "text", 0);
        if (suffixId == 0) {
            suffix = attrs.getAttributeValue(ANDROID_SCHEMA_URL, "text");
        }
        else {
            suffix = context.getString(suffixId);
        }

        // Get default, min, and max seekbar values
        defaultValue = attrs.getAttributeIntValue(ANDROID_SCHEMA_URL, "defaultValue", PreferenceConfiguration.getDefaultBitrate(context));
        maxValue = attrs.getAttributeIntValue(ANDROID_SCHEMA_URL, "max", 100);
        minValue = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "min", 1);
        stepSize = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "step", 1);
        divisor = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "divisor", 1);
        keyStepSize = attrs.getAttributeIntValue(SEEKBAR_SCHEMA_URL, "keyStep", 0);
        // Rotulo de ESTADO para o zero. A taxa em rede limitada mostrava "0.0 Mbps" na
        // coluna, que se le como "video desligado" — e o zero ali significa o contrario,
        // e a unica frase que explica isso e a ultima linha do resumo, abaixo da dobra.
        //
        // Sinalizador e nao referencia de string: este namespace nao passa pelo compilador
        // de atributos, entao um "@string/..." aqui nao teria id garantido. Quem escolhe a
        // palavra e o codigo.
        zeroIsAuto = attrs.getAttributeBooleanValue(SEEKBAR_SCHEMA_URL, "zeroAuto", false);
        seekbarMax = maxValue - minValue;

        // Declarado aqui e nao no XML: sao vinte SeekBarPreference em preferences.xml, e
        // repetir widgetLayout em cada uma so cria um lugar a mais para esquecer.
        setWidgetLayoutResource(R.layout.vidro_pref_value);
    }

    /**
     * A linha da lista e o dialogo saem da MESMA conta: dois formatadores separados
     * acabam divergindo no arredondamento ou no espaco antes do sufixo.
     */
    private String formatValue(int value) {
        String t;
        if (divisor != 1) {
            // Casa decimal so quando ela existe. O passo desta barra e de meio Mbps, entao
            // "59.5" e informacao; ja "60.0" e uma precisao que o numero nao tem, e a
            // coluna inteira ficava com um ".0" pendurado em todo valor inteiro.
            if (value % divisor == 0) {
                t = String.valueOf(value / divisor);
            }
            else {
                t = String.format((Locale)null, "%.1f", value / (float)divisor);
            }
        }
        else {
            t = String.valueOf(value);
        }
        return suffix == null ? t : t.concat(suffix.length() > 1 ? " "+suffix : suffix);
    }

    /**
     * O que a pessoa LE, que nem sempre e o numero.
     *
     * Quando a preferencia declara zeroAuto, o zero e um estado e nao uma medida: mostra-lo
     * como "0.0 Mbps" informa o oposto do que ele faz.
     */
    private String displayValue(int value) {
        if (zeroIsAuto && value == 0) {
            return context.getString(R.string.vidro_seekbar_auto);
        }
        return formatValue(value);
    }

    /**
     * Mono so para numero.
     *
     * JetBrains Mono e o papel numerico da identidade; palavra em mono e ruido tipografico,
     * entao o rotulo de estado volta para a Manrope do resto do app.
     */
    private void applyValueFace(TextView view, int value) {
        boolean isWord = zeroIsAuto && value == 0;
        Typeface face = ResourcesCompat.getFont(context,
                isWord ? R.font.manrope_semibold : R.font.jetbrains_mono_regular);
        if (face != null) {
            view.setTypeface(face);
        }
    }

    // A linha da lista mostrava so titulo e descricao: para ler a taxa de bits era preciso
    // abrir o dialogo. O valor persistido e relido a cada bind porque a view e reciclada.
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View view = holder.findViewById(R.id.vidro_pref_value);
        if (view instanceof TextView) {
            if (shouldPersist()) {
                currentValue = getPersistedInt(defaultValue);
            }
            ((TextView)view).setText(displayValue(currentValue));
            applyValueFace((TextView)view, currentValue);
        }
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Ponta da regua: o menor e o maior valor que a trilha aceita, escritos. */
    private TextView scaleLabel(String text, int gravity) {
        TextView label = new TextView(context);
        label.setTextAppearance(context, R.style.VidroText_Numeric);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTextColor(ContextCompat.getColor(context, R.color.vidro_text_3));
        label.setGravity(gravity);
        label.setText(text);
        return label;
    }

    /**
     * Um passo de cada vez, num alvo que o dedo alcanca.
     *
     * O sinal e desenhado em Manrope e nao em mono: e simbolo de acao, nao numero.
     */
    private TextView stepButton(String glyph, int descriptionId, final int delta) {
        TextView button = new TextView(context);
        button.setText(glyph);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        button.setTextColor(ContextCompat.getColor(context, R.color.vidro_text));
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(context.getString(descriptionId));
        button.setBackgroundResource(R.drawable.vidro_button_ghost_bg);
        // Mesmo cuidado do dialogo: um tint herdado por cima do fundo pintado.
        button.setBackgroundTintList(null);
        Typeface face = ResourcesCompat.getFont(context, R.font.manrope_semibold);
        if (face != null) {
            button.setTypeface(face);
        }
        button.setLayoutParams(new LinearLayout.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.vidro_touch_target),
                context.getResources().getDimensionPixelSize(R.dimen.vidro_touch_target)));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nudge(delta);
            }
        });
        return button;
    }

    /** Anda um passo e deixa o proprio ouvinte da barra arredondar e reescrever o valor. */
    private void nudge(int delta) {
        if (seekBar == null) {
            return;
        }
        int target = seekBar.getProgress() + minValue + delta;
        target = Math.max(minValue, Math.min(maxValue, target));
        seekBar.setProgress(target - minValue);
    }

    protected AlertDialog getDialog() {
        if (dialog != null) {
            return dialog;
        }

        // O layout deste dialogo era do Moonlight original, montado em pixels crus
        // (setPadding(6,6,6,6), setTextSize(32)) — o resultado escalava errado e nao
        // pertencia a identidade. Aqui ele vira dp/sp e adota a tipografia Vidro: o
        // numero em mono tabular, porque e um valor que muda enquanto se arrasta e
        // proporcional faria a linha inteira dancar a cada digito.
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        // 16 dp de lado, e nao 24: e o mesmo dialogPreferredPadding do tema, que veste o
        // titulo da folha logo acima. Com 24 aqui o corpo ficava numa guia propria e o
        // titulo sobrava sozinho a esquerda de tudo.
        layout.setPadding(dp(16), dp(20), dp(16), dp(8));

        TextView splashText = new TextView(context);
        splashText.setTextAppearance(context, R.style.VidroText_Support);
        splashText.setLineSpacing(0f, 1.35f);
        if (dialogMessage != null) {
            splashText.setText(dialogMessage);
        }
        layout.addView(splashText);

        valueText = new TextView(context);
        valueText.setGravity(Gravity.CENTER);
        valueText.setTextAppearance(context, R.style.VidroText_Numeric);
        valueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
        valueText.setTextColor(context.getResources().getColor(R.color.vidro_accent_text));
        // Default text for value; hides bug where OnSeekBarChangeListener isn't called when opacity is 0%
        valueText.setText("0%");

        // AJUSTE FINO AO LADO DO VALOR.
        //
        // O unico jeito de escolher um numero era arrastar um thumb de 13 px sobre 273 px
        // de trilha — cada pixel valia uns 1,1 Mbps, e nao havia como acertar um valor
        // exato. Os dois botoes andam um passo de cada vez e sao alvos de 48 dp, o piso
        // que o dedo pede.
        LinearLayout valueRow = new LinearLayout(context);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        valueRow.setPadding(0, dp(20), 0, dp(4));

        valueRow.addView(stepButton("−", R.string.vidro_seekbar_less, -stepSize));
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        valueRow.addView(valueText, valueParams);
        valueRow.addView(stepButton("+", R.string.vidro_seekbar_more, stepSize));

        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(valueRow, params);

        seekBar = new SeekBar(context);
        // A barra era a do sistema tingida de azul: trilha fina de canto vivo e uma pega de
        // 13 px para escolher um valor exato. Tingir nao resolve nenhuma das duas coisas —
        // o tinte pinta o desenho da plataforma, ele nao troca o desenho. Aqui trilha e
        // pega sao nossas; sem os drawables proprios o setProgressTintList precisaria
        // voltar, porque senao a barra cai no destaque do sistema, roxo sob Material3.
        seekBar.setProgressDrawable(
                ContextCompat.getDrawable(context, R.drawable.vidro_seek_track));
        seekBar.setThumb(ContextCompat.getDrawable(context, R.drawable.vidro_seek_thumb));
        // O recorte da trilha sob a pega e do Material e deixa uma falha na barra; com a
        // pega opaca ele so mostra o fundo do dialogo no meio do percorrido.
        seekBar.setSplitTrack(false);
        seekBar.setPadding(dp(4), dp(16), dp(4), dp(16));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                value += minValue;
                if (value < minValue) {
                    seekBar.setProgress(0);
                    return;
                }

                int roundedValue = Math.round((float)value / stepSize) * stepSize;
                if (roundedValue != value) {
                    seekBar.setProgress(roundedValue - minValue);
                    return;
                }

                valueText.setText(displayValue(roundedValue));
                applyValueFace(valueText, roundedValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(seekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // A ESCALA DA TRILHA.
        //
        // A barra nao dizia onde comecava nem onde terminava: com o thumb a 20% do curso
        // nada na tela informava que o topo era 300 Mbps, entao arrastar era adivinhar. As
        // duas pontas escritas transformam a trilha numa regua.
        LinearLayout scaleRow = new LinearLayout(context);
        scaleRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView minLabel = scaleLabel(displayValue(minValue), Gravity.START);
        TextView maxLabel = scaleLabel(formatValue(maxValue), Gravity.END);
        scaleRow.addView(minLabel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        scaleRow.addView(maxLabel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        layout.addView(scaleRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist()) {
            currentValue = getPersistedInt(defaultValue);
        }

        seekBar.setMax(seekbarMax);
        if (keyStepSize != 0) {
            seekBar.setKeyProgressIncrement(keyStepSize);
        }
        seekBar.setProgress(currentValue - minValue);
        syncValueText();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.VidroDialog);
        dialogBuilder.setTitle(getTitle());
        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton("OK", (dialog, which) -> {
            if (shouldPersist()) {
                currentValue = seekBar.getProgress() + minValue;
                persistInt(currentValue);
                callChangeListener(currentValue);
                // Sem isto a linha continua mostrando o valor de antes do dialogo.
                notifyChanged();
            }

            dialog.dismiss();
        });
        dialogBuilder.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        dialog = dialogBuilder.create();
        return dialog;
    }

    protected void updateSeekbar() {
        seekBar.setMax(seekbarMax);
        if (keyStepSize != 0) {
            seekBar.setKeyProgressIncrement(keyStepSize);
        }
        seekBar.setProgress(currentValue - minValue);
        syncValueText();
    }

    /**
     * Escreve o valor sem depender do ouvinte da barra.
     *
     * setProgress nao dispara onProgressChanged quando o progresso ja e o pedido, e o caso
     * em que isso acontece e justamente o zero: o dialogo abria mostrando o "0%" de reserva
     * em vez do estado real. Aqui isso doi mais do que antes, porque o zero e o estado
     * "Automatico" e e ele que a pessoa foi ler.
     */
    private void syncValueText() {
        if (valueText == null) {
            return;
        }
        valueText.setText(displayValue(currentValue));
        applyValueFace(valueText, currentValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            currentValue = shouldPersist() ? getPersistedInt(this.defaultValue) : 0;
        }
        else {
            currentValue = (Integer) defaultValue;
        }
    }

    public void setProgress(int progress) {
        this.currentValue = progress;
        if (seekBar != null) {
            seekBar.setProgress(progress - minValue);
        }
    }
    public int getProgress() {
        return currentValue + minValue;
    }

    public void showDialog() {
        AlertDialog dialog = getDialog();
        updateSeekbar();
        dialog.show();
    }

    @Override
    protected void onClick() {
        super.onClick();
        showDialog();
    }
}
