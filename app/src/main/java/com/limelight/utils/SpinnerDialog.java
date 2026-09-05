package com.limelight.utils;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.limelight.R;

/**
 * Carregando bloqueante — parear, buscar a lista de apps, encerrar a sessão.
 *
 * Era um ProgressDialog, que ignora tema e desenha a roda cinza do Android. Como ele
 * aparece logo no fluxo de adicionar PC, era a peça do sistema que mais gente via num
 * app que não deveria ter nenhuma. Agora é um AlertDialog com o tema Vidro e a mesma
 * barra fina do "Conectando".
 *
 * A API pública não mudou: displayDialog/dismiss/setMessage/closeDialogs continuam
 * iguais, e run() mantém a semântica de alternância que o resto do app já espera —
 * rodar de novo com o diálogo montado é o que o fecha.
 */
public class SpinnerDialog implements Runnable,OnCancelListener {
    private final String title;
    private String message;
    private final Activity activity;
    private AlertDialog progress;
    private TextView messageView;
    private final boolean finish;
    private final boolean cancelable;
    private final Runnable onCancel;

    private static final ArrayList<SpinnerDialog> rundownDialogs = new ArrayList<>();

    private SpinnerDialog(Activity activity, String title, String message, boolean finish,
                          boolean cancelable, Runnable onCancel)
    {
        this.activity = activity;
        this.title = title;
        this.message = message;
        this.progress = null;
        this.finish = finish;
        this.cancelable = cancelable;
        this.onCancel = onCancel;
    }

    public static SpinnerDialog displayDialog(Activity activity, String title, String message, boolean finish)
    {
        // Comportamento histórico: quem encerra a Activity no cancelamento é também quem
        // aceita ser cancelado. Preservado para as chamadas que não pediram outra coisa.
        SpinnerDialog spinner = new SpinnerDialog(activity, title, message, finish, finish, null);
        activity.runOnUiThread(spinner);
        return spinner;
    }

    /**
     * Roda de espera que o usuário pode interromper sem sair da tela.
     *
     * "Cancelável" e "encerra a Activity" eram a mesma flag, então quem não queria fechar
     * a tela no cancelamento acabava com um diálogo que engolia o Voltar — a espera virava
     * um beco. Aqui o cancelamento só chama {@code onCancel} (na thread da UI): quem
     * mostrou a roda é quem sabe o que fazer com a espera interrompida.
     *
     * O diálogo continua sem fechar por toque fora: ele cobre uma espera de rede e um
     * toque de raspão na área escura não é um pedido de desistir. O Voltar é.
     */
    public static SpinnerDialog displayCancellableDialog(Activity activity, String title, String message, Runnable onCancel)
    {
        SpinnerDialog spinner = new SpinnerDialog(activity, title, message, false, true, onCancel);
        activity.runOnUiThread(spinner);
        return spinner;
    }

    public static void closeDialogs(Activity activity)
    {
        synchronized (rundownDialogs) {
            Iterator<SpinnerDialog> i = rundownDialogs.iterator();
            while (i.hasNext()) {
                SpinnerDialog dialog = i.next();
                if (dialog.activity == activity) {
                    i.remove();
                    if (dialog.progress != null && dialog.progress.isShowing()) {
                        dialog.progress.dismiss();
                    }
                }
            }
        }
    }

    public void dismiss()
    {
        // Running again with progress != null will destroy it
        activity.runOnUiThread(this);
    }

    public void setMessage(final String message)
    {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpinnerDialog.this.message = message;
                if (messageView != null) {
                    messageView.setText(message);
                }
            }
        });
    }

    @Override
    public void run() {

        // If we're dying, don't bother doing anything
        if (activity.isFinishing()) {
            return;
        }

        if (progress == null)
        {
            View view = LayoutInflater.from(activity).inflate(R.layout.vidro_spinner, null);
            ((TextView) view.findViewById(R.id.vidroSpinnerTitle)).setText(title);
            messageView = view.findViewById(R.id.vidroSpinnerMessage);
            messageView.setText(message);

            // O título vai dentro da view, e não no setTitle: o título do AlertDialog
            // traz o padding e o divisor do sistema, e a folha Vidro não tem divisor.
            progress = new AlertDialog.Builder(activity, R.style.VidroDialog)
                    .setView(view)
                    .create();
            progress.setOnCancelListener(this);

            progress.setCancelable(cancelable);
            if (cancelable)
            {
                // Cancelar aqui é decisão, não escorregão: só pelo Voltar.
                progress.setCanceledOnTouchOutside(false);
            }

            synchronized (rundownDialogs) {
                rundownDialogs.add(this);
                progress.show();
            }
        }
        else
        {
            synchronized (rundownDialogs) {
                if (rundownDialogs.remove(this) && progress.isShowing()) {
                    progress.dismiss();
                }
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        synchronized (rundownDialogs) {
            rundownDialogs.remove(this);
        }

        // Cancelar deixou de implicar encerrar a Activity, então agora os dois efeitos
        // são checados separadamente.
        if (onCancel != null) {
            onCancel.run();
        }

        if (finish) {
            activity.finish();
        }
    }
}
