package com.limelight.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Button;
import android.app.GameManager;
import android.app.GameState;
import android.app.LocaleManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.Build;
import android.os.LocaleList;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.Game;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.Locale;

public class UiHelper {

    private static final int TV_VERTICAL_PADDING_DP = 15;
    private static final int TV_HORIZONTAL_PADDING_DP = 15;

    private static void setGameModeStatus(Context context, boolean streaming, boolean interruptible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GameManager gameManager = context.getSystemService(GameManager.class);

            if (gameManager == null) {
                LimeLog.warning("GameManager is null, maybe your system does not support it?");
                return;
            }

            if (streaming) {
                gameManager.setGameState(new GameState(false, interruptible ? GameState.MODE_GAMEPLAY_INTERRUPTIBLE : GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE));
            }
            else {
                gameManager.setGameState(new GameState(false, GameState.MODE_NONE));
            }
        }
    }

    public static void notifyStreamConnecting(Context context) {
        setGameModeStatus(context, true, true);
    }

    public static void notifyStreamConnected(Context context) {
        setGameModeStatus(context, true, false);
    }

    public static void notifyStreamEnteringPiP(Context context) {
        setGameModeStatus(context, true, true);
    }

    public static void notifyStreamExitingPiP(Context context) {
        setGameModeStatus(context, true, false);
    }

    public static void notifyStreamEnded(Context context) {
        setGameModeStatus(context, false, false);
    }

    public static void setLocale(Activity activity)
    {
        String locale = PreferenceConfiguration.readPreferences(activity).language;
        Configuration config = new Configuration(activity.getResources().getConfiguration());
        if (locale.equals(PreferenceConfiguration.DEFAULT_LANGUAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // On Android 13, migrate this non-default language setting into the OS native API
                LocaleManager localeManager = activity.getSystemService(LocaleManager.class);
                LocaleList systemLocales = localeManager.getSystemLocales();
                if (!systemLocales.isEmpty()) {
                    config.locale = systemLocales.get(0);
                }
            }
        } else {
            // We're handling some nasty non-standard devices which cannot set locale using system config correctly
            // Some locales include both language and country which must be separated
            // before calling the Locale constructor.
            if (locale.contains("-"))
            {
                config.locale = new Locale(locale.substring(0, locale.indexOf('-')),
                        locale.substring(locale.indexOf('-') + 1));
            }
            else
            {
                config.locale = new Locale(locale);
            }
        }

        activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
    }

    public static void applyStatusBarPadding(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This applies the padding that we omitted in notifyNewRootView() on Q
            view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.setPadding(view.getPaddingLeft(),
                            view.getPaddingTop(),
                            view.getPaddingRight(),
                            windowInsets.getTappableElementInsets().bottom);
                    return windowInsets;
                }
            });
            view.requestApplyInsets();
        }
    }

    public static void notifyNewRootView(final Activity activity)
    {
        View rootView = activity.findViewById(android.R.id.content);
        UiModeManager modeMgr = (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);

        // Set GameState.MODE_NONE initially for all activities
        setGameModeStatus(activity, false, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Allow this non-streaming activity to layout under notches.
            //
            // We should NOT do this for the Game activity unless
            // the user specifically opts in, because it can obscure
            // parts of the streaming surface.
            activity.getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        if (modeMgr.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            // Increase view padding on TVs
            float scale = activity.getResources().getDisplayMetrics().density;
            int verticalPaddingPixels = (int) (TV_VERTICAL_PADDING_DP*scale + 0.5f);
            int horizontalPaddingPixels = (int) (TV_HORIZONTAL_PADDING_DP*scale + 0.5f);

            rootView.setPadding(horizontalPaddingPixels, verticalPaddingPixels,
                    horizontalPaddingPixels, verticalPaddingPixels);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Draw under the status bar on Android Q devices

            // Using getDecorView() here breaks the translucent status/navigation bar when gestures are disabled
            activity.findViewById(android.R.id.content).setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    // Use the tappable insets so we can draw under the status bar in gesture mode
                    Insets tappableInsets = windowInsets.getTappableElementInsets();
                    view.setPadding(tappableInsets.left,
                            tappableInsets.top,
                            tappableInsets.right,
                            tappableInsets.bottom);

                    // Show a translucent navigation bar if we can't tap there
                    if (tappableInsets.bottom != 0) {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    }
                    else {
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    }

                    return windowInsets;
                }
            });

            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /**
     * Zera o recuo de topo da janela e o devolve a uma view só — o cabeçalho.
     *
     * A luz do Vidro é o primeiro filho da raiz e precisa encostar na borda física; o
     * recuo da barra de status aplicado por notifyNewRootView() no android.R.id.content
     * empurra a luz junto com o resto e deixa acima dela a faixa chapada que a identidade
     * não admite. Aqui o conteúdo sobe até a borda e quem recua é o texto, que é o único
     * que não pode ficar embaixo do relógio.
     *
     * Vale também para API 21-28, onde notifyNewRootView() não instala listener nenhum:
     * a barra é opaca, mas a janela desenha por baixo dela (windowDrawsSystemBarBackgrounds
     * do tema Material), então sem este recuo o cabeçalho ficaria escondido.
     *
     * Chamar DEPOIS de notifyNewRootView(): substitui o listener dela, e por isso repete
     * aqui o recuo lateral/inferior e a barra de navegação translúcida.
     */
    public static void applyTopInsetToHeader(final Activity activity, final View header)
    {
        UiModeManager modeMgr = (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);
        if (modeMgr.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            // Na TV quem manda é o padding de overscan de notifyNewRootView, e não há barras.
            return;
        }

        final View contentView = activity.findViewById(android.R.id.content);
        final int headerPaddingTop = header.getPaddingTop();

        contentView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Tappable, como em notifyNewRootView: no modo por gestos a barra de
                    // navegação não recebe toque e não precisa de recuo.
                    Insets tappableInsets = windowInsets.getTappableElementInsets();
                    view.setPadding(tappableInsets.left, 0, tappableInsets.right, tappableInsets.bottom);

                    // Show a translucent navigation bar if we can't tap there
                    if (tappableInsets.bottom != 0) {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    }
                    else {
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    }
                }
                else {
                    view.setPadding(windowInsets.getSystemWindowInsetLeft(), 0,
                            windowInsets.getSystemWindowInsetRight(),
                            windowInsets.getSystemWindowInsetBottom());
                }

                header.setPadding(header.getPaddingLeft(),
                        headerPaddingTop + windowInsets.getSystemWindowInsetTop(),
                        header.getPaddingRight(),
                        header.getPaddingBottom());

                return windowInsets;
            }
        });

        contentView.requestApplyInsets();
    }

    public static void showDecoderCrashDialog(Activity activity) {
        final SharedPreferences prefs = activity.getSharedPreferences("DecoderTombstone", 0);
        final int crashCount = prefs.getInt("CrashCount", 0);
        int lastNotifiedCrashCount = prefs.getInt("LastNotifiedCrashCount", 0);

        // Remember the last crash count we notified at, so we don't
        // display the crash dialog every time the app is started until
        // they stream again
        if (crashCount != 0 && crashCount != lastNotifiedCrashCount) {
            if (crashCount % 3 == 0) {
                // At 3 consecutive crashes, we'll forcefully reset their settings
                PreferenceConfiguration.resetStreamingSettings(activity);
                Dialog.displayDialog(activity,
                        activity.getResources().getString(R.string.title_decoding_reset),
                        activity.getResources().getString(R.string.message_decoding_reset),
                        new Runnable() {
                            @Override
                            public void run() {
                                // Mark notification as acknowledged on dismissal
                                prefs.edit().putInt("LastNotifiedCrashCount", crashCount).apply();
                            }
                        });
            }
            else {
                Dialog.displayDialog(activity,
                        activity.getResources().getString(R.string.title_decoding_error),
                        activity.getResources().getString(R.string.message_decoding_error),
                        new Runnable() {
                            @Override
                            public void run() {
                                // Mark notification as acknowledged on dismissal
                                prefs.edit().putInt("LastNotifiedCrashCount", crashCount).apply();
                            }
                        });
            }
        }
    }

    public static <T> void displayConfirmationDialog(Activity parent, String title, String message, String btnYesText, String btnNoText, final Runnable onYes, final Runnable onNo) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    if (onYes != null) {
                        onYes.run();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    if (onNo != null) {
                        onNo.run();
                    }
                    break;
            }
        };

        // Tema Vidro, e o par de plataforma: este é o android.app.AlertDialog, não o do
        // AppCompat. Alimenta sair da sessao, apagar PC e display virtual — as tres
        // confirmacoes que qualquer pessoa encontra no primeiro dia de uso.
        AlertDialog.Builder builder = new AlertDialog.Builder(parent, R.style.VidroDialogPlatform);
        builder.setMessage(Html.fromHtml(message));
        if (title != null) {
            builder.setTitle(title);
        }
        if (btnYesText != null) {
            builder.setPositiveButton(btnYesText, dialogClickListener);
        }
        if (btnNoText != null) {
            builder.setNegativeButton(btnNoText, dialogClickListener);
        }
        AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

        // Pinta os botoes a mao, pelo mesmo motivo do utils/Dialog: o AlertDialog resolve
        // a cor do botao contra o tema da Activity, que sob Material3 devolve o roxo do
        // sistema — e roxo e exatamente o que esta identidade nao tem.
        //
        // Cada um com a tinta do seu papel. O botao de confirmar veste a pilula de acento
        // preenchida, e vidro_accent_text sobre vidro_accent_fill da 1,1:1 — o rotulo
        // sumia dentro do proprio botao. Sobre preenchimento a tinta e vidro_on_accent.
        vestirBotoesDoDialogo(dialog, parent.getResources());
    }

    /**
     * Veste os botões de um AlertDialog nos papéis da identidade: confirmar na pílula de
     * acento, os outros na tinta comum.
     *
     * É um FUNIL de propósito. O mesmo defeito foi morto três vezes em cópias separadas —
     * o "OK" apagado do Dialog.java, o "Sim" lilás do quit, o confirmar daqui — e cada
     * cópia esquecia um pedaço: sob Material o botão resolve a cor contra o tema da
     * Activity (sai lilás), e setBackgroundResource sem limpar o backgroundTint sai
     * escurecido por cima. As duas metades têm de andar sempre juntas, então moram aqui.
     *
     * Chame DEPOIS do show() — antes dele os botões ainda não existem.
     */
    /** A mesma veste para o AlertDialog do AppCompat, que não compartilha ancestral útil. */
    public static void vestirBotoesDoDialogo(androidx.appcompat.app.AlertDialog dialog,
                                             android.content.res.Resources res) {
        Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (confirm != null) {
            confirm.setBackgroundResource(R.drawable.vidro_button_accent_bg);
            confirm.setBackgroundTintList(null);
            confirm.setTextColor(res.getColor(R.color.vidro_on_accent));
            confirm.setAllCaps(false);
        }
        for (int qual : new int[] { AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL }) {
            Button b = dialog.getButton(qual);
            if (b != null) {
                b.setTextColor(res.getColor(R.color.vidro_text));
                b.setAllCaps(false);
            }
        }
    }

    public static void vestirBotoesDoDialogo(android.app.AlertDialog dialog,
                                             android.content.res.Resources res) {
        Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (confirm != null) {
            confirm.setBackgroundResource(R.drawable.vidro_button_accent_bg);
            confirm.setBackgroundTintList(null);
            confirm.setTextColor(res.getColor(R.color.vidro_on_accent));
            confirm.setAllCaps(false);
        }
        for (int qual : new int[] { AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL }) {
            Button b = dialog.getButton(qual);
            if (b != null) {
                b.setTextColor(res.getColor(R.color.vidro_text));
                b.setAllCaps(false);
            }
        }
    }

    public static void displayVdisplayConfirmationDialog(Activity parent, ComputerDetails computer, final Runnable onYes, final Runnable onNo) {
        String message = computer.vDisplaySupported ?
                parent.getResources().getString(R.string.vdisplay_not_ready) :
                parent.getResources().getString(R.string.vdisplay_not_supported);
        UiHelper.displayConfirmationDialog(
                parent,
                null,
                message,
                parent.getResources().getString(R.string.proceed),
                parent.getResources().getString(R.string.cancel),
                onYes,
                onNo
        );
    }

    public static void displayQuitConfirmationDialog(Activity parent, final Runnable onYes, final Runnable onNo) {
        displayConfirmationDialog(
                parent,
                null,
                parent.getResources().getString(R.string.applist_quit_confirmation),
                parent.getResources().getString(R.string.yes),
                parent.getResources().getString(R.string.no),
                onYes,
                onNo
        );
    }

    public static void displayDeletePcConfirmationDialog(Activity parent, ComputerDetails computer, final Runnable onYes, final Runnable onNo) {
        displayConfirmationDialog(
                parent,
                computer.name,
                parent.getResources().getString(R.string.delete_pc_msg),
                parent.getResources().getString(R.string.yes),
                parent.getResources().getString(R.string.no),
                onYes,
                onNo
        );
    }

    public static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
