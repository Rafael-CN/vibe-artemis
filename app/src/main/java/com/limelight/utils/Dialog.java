package com.limelight.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;

import com.limelight.R;

public class Dialog implements Runnable {
    private final String title;
    private final String message;
    private final Activity activity;
    // Não é mais final: quando o mesmo aviso chega duas vezes, o segundo não abre uma
    // folha nova — ele pendura a própria ação de saída na folha que já está na tela.
    private Runnable runOnDismiss;

    // A ação alternativa da folha, no slot que estava vago. Nula na maioria das folhas.
    private final CharSequence actionLabel;
    private final Runnable action;

    private AlertDialog alert;

    private static final ArrayList<Dialog> rundownDialogs = new ArrayList<>();

    private Dialog(Activity activity, String title, String message, Runnable runOnDismiss)
    {
        this(activity, title, message, runOnDismiss, null, null);
    }

    private Dialog(Activity activity, String title, String message, Runnable runOnDismiss,
                   CharSequence actionLabel, Runnable action)
    {
        this.activity = activity;
        this.title = title;
        this.message = message;
        this.runOnDismiss = runOnDismiss;
        this.actionLabel = actionLabel;
        this.action = action;
    }

    public static void closeDialogs()
    {
        synchronized (rundownDialogs) {
            for (Dialog d : rundownDialogs) {
                if (d.alert.isShowing()) {
                    d.alert.dismiss();
                }
            }

            rundownDialogs.clear();
        }
    }

    public static void displayDialog(final Activity activity, String title, String message, final boolean endAfterDismiss)
    {
        activity.runOnUiThread(new Dialog(activity, title, message, new Runnable() {
            @Override
            public void run() {
                if (endAfterDismiss) {
                    activity.finish();
                }
            }
        }));
    }

    public static void displayDialog(Activity activity, String title, String message, Runnable runOnDismiss)
    {
        activity.runOnUiThread(new Dialog(activity, title, message, runOnDismiss));
    }

    /**
     * A mesma folha, com uma segunda saída além de "OK".
     *
     * Nasceu da folha de falha de conexão, cujo único desfecho era desistir: "OK" fecha a
     * sessão e devolve ao catálogo, de onde repetir a tentativa custa mais quatro toques e
     * uma navegação — e repetir é justamente o que a pessoa ia fazer. Com o host subindo um
     * display virtual por cliente, falhar na primeira tentativa é rotina, não exceção.
     *
     * A ação SUBSTITUI a saída: quem toca nela não quer que a folha encerre nada no
     * caminho. Quem quiser encerrar toca em OK, que continua fazendo o de sempre.
     */
    public static void displayDialog(final Activity activity, String title, String message,
                                     final boolean endAfterDismiss,
                                     CharSequence actionLabel, Runnable action)
    {
        activity.runOnUiThread(new Dialog(activity, title, message, new Runnable() {
            @Override
            public void run() {
                if (endAfterDismiss) {
                    activity.finish();
                }
            }
        }, actionLabel, action));
    }

    /**
     * Encadeia mais uma ação de saída nesta folha, preservando a que já existia.
     *
     * Serve ao caso do aviso repetido: a segunda chamada não pode simplesmente sumir,
     * porque a ação dela pode ser a que encerra a Activity.
     */
    private void alsoRunOnDismiss(final Runnable extra) {
        if (extra == null) {
            return;
        }
        final Runnable previous = runOnDismiss;
        runOnDismiss = new Runnable() {
            @Override
            public void run() {
                if (previous != null) {
                    previous.run();
                }
                extra.run();
            }
        };
    }

    private static boolean sameText(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public void run() {
        // If we're dying, don't bother creating a dialog
        if (activity.isFinishing())
            return;

        // Um erro que acontece duas vezes é UM erro, não dois avisos.
        //
        // Cada falha empilhava a própria folha, e a pessoa ficava descartando a mesma
        // frase em fila — três toques em "OK" para o mesmo texto, medidos em 3,6 s de
        // gravação. Quando o aviso idêntico já está na tela, só a ação de saída dele é
        // acrescentada à folha aberta.
        synchronized (rundownDialogs) {
            for (Dialog showing : rundownDialogs) {
                if (showing.activity == activity
                        && showing.alert != null && showing.alert.isShowing()
                        && sameText(showing.title, title)
                        && sameText(showing.message, message)) {
                    showing.alsoRunOnDismiss(runOnDismiss);
                    return;
                }
            }
        }

        // O tema Vidro em vez do do sistema. Este diálogo aparece em toda falha de
        // rede, então era a peça do Moonlight que mais gente via.
        alert = new AlertDialog.Builder(activity, R.style.VidroDialog).create();

        alert.setTitle(title);
        alert.setMessage(message);
        alert.setCancelable(false);
        alert.setCanceledOnTouchOutside(false);
 
        alert.setButton(AlertDialog.BUTTON_POSITIVE, activity.getResources().getText(android.R.string.ok), new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                  synchronized (rundownDialogs) {
                      rundownDialogs.remove(Dialog.this);
                      alert.dismiss();
                  }

                  runOnDismiss.run();
              }
        });
        // O slot negativo estava vago desde sempre. É onde entra a segunda saída da folha
        // — hoje só o "Tentar de novo" da falha de conexão.
        if (action != null) {
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, actionLabel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (rundownDialogs) {
                        rundownDialogs.remove(Dialog.this);
                        alert.dismiss();
                    }

                    // De propósito sem runOnDismiss: a saída desta folha é encerrar a
                    // Activity, e quem pediu para tentar de novo não quer isso.
                    action.run();
                }
            });
        }
        alert.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getResources().getText(R.string.help), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                synchronized (rundownDialogs) {
                    rundownDialogs.remove(Dialog.this);
                    alert.dismiss();
                }

                runOnDismiss.run();

                HelpLauncher.launchTroubleshooting(activity);
            }
        });
        alert.setOnShowListener(new DialogInterface.OnShowListener(){

            @Override
            public void onShow(DialogInterface dialog) {
                // Tinge os botões à mão, cada um pelo PAPEL que tem.
                //
                // O tema do diálogo já aponta colorAccent para o acento Vidro, mas o
                // AlertDialog resolve a cor do botão contra o tema da Activity, que sob
                // Material3 devolve o roxo padrão do sistema — e roxo é justamente o que
                // esta identidade não tem. Pintar aqui não depende dessa resolução.
                //
                // Uma cor só para os três não serve mais: desde que "OK" ganhou a pílula
                // de acento preenchida, pintá-lo de vidro_accent_text põe #80C3FF sobre
                // #69BDFF — 1,1:1, o rótulo some dentro do próprio botão. Sobre
                // preenchimento a tinta é sempre vidro_on_accent; nos de contorno, a
                // tinta de texto comum. O fundo vai junto para o par não depender de o
                // estilo do tema ter sido resolvido: cor de tinta e preenchimento têm de
                // chegar sempre pelo mesmo caminho.
                Button positive = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positive != null) {
                    positive.setBackgroundResource(R.drawable.vidro_button_accent_bg);
                    // O tint TAMBÉM tem de sair: sob Material o botão do diálogo é um
                    // MaterialButton, e setBackgroundResource não limpa o backgroundTint
                    // dele — o tint escuro do tema era aplicado POR CIMA da pílula, e o
                    // "OK" saía como um círculo apagado, lendo como desabilitado. Medido
                    // com uiautomator: enabled=true, e mesmo assim escuro.
                    positive.setBackgroundTintList(null);
                    positive.setTextColor(activity.getResources().getColor(R.color.vidro_on_accent));
                    positive.setAllCaps(false);
                }
                int ink = activity.getResources().getColor(R.color.vidro_text);
                for (int which : new int[] { AlertDialog.BUTTON_NEGATIVE,
                        AlertDialog.BUTTON_NEUTRAL }) {
                    Button b = alert.getButton(which);
                    if (b != null) {
                        b.setTextColor(ink);
                        b.setAllCaps(false);
                    }
                }

                // O foco vai para a ação quando ela existe, e só então para o OK.
                //
                // Com a capa acoplada o Enter chega no botão focado, e nesta folha o botão
                // certo é o que repete a tentativa: quem falhou em conectar quer conectar,
                // não voltar ao catálogo. Sem ação alternativa, nada muda — o foco segue no
                // OK, como sempre foi.
                Button button = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (action == null || button == null) {
                    button = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                }
                if (button != null) {
                    button.setFocusable(true);
                    button.setFocusableInTouchMode(true);
                    button.requestFocus();
                }
            }
        });

        synchronized (rundownDialogs) {
            rundownDialogs.add(this);
            alert.show();
        }
    }

}
