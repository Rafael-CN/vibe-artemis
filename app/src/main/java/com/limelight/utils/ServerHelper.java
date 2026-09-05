package com.limelight.utils;

import com.limelight.ui.VidroToast;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.limelight.CatalogoDoLugar;
import com.limelight.Game;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.ShortcutTrampoline;
import com.limelight.binding.PlatformBinding;
import com.limelight.computers.ComputerManagerService;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.HostHttpResponseException;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;

public class ServerHelper {
    public static final String CONNECTION_TEST_SERVER = "android.conntest.moonlight-stream.org";

    public static ComputerDetails.AddressTuple getCurrentAddressFromComputer(ComputerDetails computer) throws IOException {
        if (computer.activeAddress == null) {
            throw new IOException("No active address for "+computer.name);
        }
        return computer.activeAddress;
    }

    public static Intent createPcShortcutIntent(Activity parent, ComputerDetails computer) {
        Intent i = new Intent(parent, ShortcutTrampoline.class);
        i.putExtra(CatalogoDoLugar.NAME_EXTRA, computer.name);
        i.putExtra(CatalogoDoLugar.UUID_EXTRA, computer.uuid);
        i.setAction(Intent.ACTION_DEFAULT);
        return i;
    }

    public static Intent createAppShortcutIntent(Activity parent, ComputerDetails computer, NvApp app) {
        Intent i = new Intent(parent, ShortcutTrampoline.class);
        i.putExtra(CatalogoDoLugar.NAME_EXTRA, computer.name);
        i.putExtra(CatalogoDoLugar.UUID_EXTRA, computer.uuid);
        i.putExtra(Game.EXTRA_APP_NAME, app.getAppName());
        i.putExtra(Game.EXTRA_APP_UUID, app.getAppUUID());
        i.putExtra(Game.EXTRA_APP_ID, ""+app.getAppId());
        i.putExtra(Game.EXTRA_APP_HDR, app.isHdrSupported());
        i.setAction(Intent.ACTION_DEFAULT);
        return i;
    }
    public static Display getActiveDisplay(Context context, PreferenceConfiguration prefs) {
        Display secondary = getSecondaryDisplay(context);
        if (secondary != null && (prefs.enableFullExDisplay)) {
            return secondary;
        } else {
            return ((DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(Display.DEFAULT_DISPLAY);
        }
    }

    public static Display getSecondaryDisplay(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display display = null;
        Display[] displays = displayManager.getDisplays();
        int mainDisplayId = Display.DEFAULT_DISPLAY;
        int secondaryDisplayId = -1;
        for (Display displayVariant : displays) {
            LimeLog.info(displayVariant.toString());
            if (displayVariant.getDisplayId() != mainDisplayId) {
                secondaryDisplayId = displayVariant.getDisplayId();
                break;
            }
        }

        if (secondaryDisplayId != -1) {
            display = displayManager.getDisplay(secondaryDisplayId);
        }
        return display;
    }

    public static Intent createStartIntent(Activity parent, NvApp app, ComputerDetails computer,
                                           ComputerManagerService.ComputerManagerBinder managerBinder,
                                           boolean withVDisplay) {
        Intent gameIntent = null;
        PreferenceConfiguration prefConfig = PreferenceConfiguration.readPreferences(parent);
        // Try to add secondary DisplayContext if supported and connected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && prefConfig.enableFullExDisplay && getSecondaryDisplay(parent) != null) {
            Context displayContext = parent.createDisplayContext(getSecondaryDisplay(parent)); // use secondary display
            gameIntent = new Intent(displayContext, Game.class);
            gameIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if(gameIntent == null) gameIntent = new Intent(parent, Game.class);
        gameIntent.putExtra(Game.EXTRA_HOST, computer.activeAddress.address);
        gameIntent.putExtra(Game.EXTRA_PORT, computer.activeAddress.port);
        gameIntent.putExtra(Game.EXTRA_HTTPS_PORT, computer.httpsPort);
        gameIntent.putExtra(Game.EXTRA_APP_NAME, app.getAppName());
        gameIntent.putExtra(Game.EXTRA_APP_UUID, app.getAppUUID());
        gameIntent.putExtra(Game.EXTRA_APP_ID, app.getAppId());
        gameIntent.putExtra(Game.EXTRA_APP_HDR, app.isHdrSupported());
        gameIntent.putExtra(Game.EXTRA_UNIQUEID, managerBinder.getUniqueId());
        gameIntent.putExtra(Game.EXTRA_PC_UUID, computer.uuid);
        gameIntent.putExtra(Game.EXTRA_PC_NAME, computer.name);
        gameIntent.putExtra(Game.EXTRA_VDISPLAY, withVDisplay);
        gameIntent.putExtra(Game.EXTRA_SERVER_COMMANDS, (ArrayList<String>) computer.serverCommands);

        // O que o host sabe fazer viaja junto com onde ele está.
        //
        // O Intent já levava endereço, porta, certificado e a lista de comandos do
        // servidor, mas nada sobre capacidade: dentro da sessão o app oferecia ações que
        // este host não tem e descobria isso ao falhar. Quem sondou o host foi o
        // ComputerManagerService, e a sessão não fala com ele — este extra é o único
        // caminho que existe entre os dois.
        //
        // Hoje o canal é um inteiro, porque a máscara de permissões é o único sinal de
        // capacidade que o serverinfo dá. Quando houver um segundo sinal a atravessar,
        // HostCapabilities vira Parcelable e passa a viajar inteiro — o nome do extra já é
        // o do conceito para que essa troca não apareça para quem chama.
        gameIntent.putExtra(Game.EXTRA_HOST_CAPABILITIES,
                computer.getCapabilities().getPermissionMask());

        try {
            if (computer.serverCert != null) {
                gameIntent.putExtra(Game.EXTRA_SERVER_CERT, computer.serverCert.getEncoded());
            }
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }

        if (prefConfig.enableFullExDisplay) {
            Display secondaryDisplay = getSecondaryDisplay(parent);
            if (secondaryDisplay != null) {
                int secondaryDisplayId = secondaryDisplay.getDisplayId();
                gameIntent.putExtra(Game.EXTRA_DISPLAY_ID, secondaryDisplayId);
                Intent touchpadIntent = new Intent(parent, ExternalDisplayControlActivity.class);
                touchpadIntent.putExtra(ExternalDisplayControlActivity.EXTRA_LAUNCH_INTENT, gameIntent);
                return touchpadIntent;
            }
        }

        return gameIntent;
    }

    // No @RequiresApi here on purpose. The only API 26+ call in this path lives in
    // createStartIntent() and is already guarded by an SDK_INT check, so requiring O of
    // every caller was wrong — it just pushed eight NewApi errors onto call sites that
    // are perfectly safe on API 21, which is the minSdk.
    public static void doStart(
            Activity parent,
            NvApp app,
            ComputerDetails computer,
            ComputerManagerService.ComputerManagerBinder managerBinder,
            boolean withVDisplay
    ) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            VidroToast.show(parent, parent.getString(R.string.pair_pc_offline), Toast.LENGTH_SHORT);
            return;
        }

        // O funil unico de lancamento. Catalogo, retomar, atalho do lancador e o proprio
        // "Continuar" da home passam todos por aqui -- por isso o historico e gravado neste
        // ponto e nao em cada chamador: um funil so e o que impede o "Continuar" de
        // discordar do que de fato abriu.
        // O NOME QUE VAI PARA O HISTÓRICO NÃO PODE SER UM MARCADOR.
        //
        // Retomar uma sessão constrói um `NvApp("app", …)`: "app" é um marcador interno, e
        // ele vazou para a tela — na gravação de 26/08 a home mostrava uma entrada chamada
        // literalmente "app" no "Continuar". Quando o nome não diz nada, o que se guarda é o
        // nome da MÁQUINA, que sempre diz.
        String nomeParaOHistorico = app.getAppName();
        if (nomeParaOHistorico == null || nomeParaOHistorico.isEmpty()
                || "app".equals(nomeParaOHistorico)) {
            nomeParaOHistorico = parent.getString(R.string.vidro_session_generic);
        }

        com.limelight.ui.compose.Historico.registrar(
                parent, computer.uuid, computer.name, app.getAppId(), nomeParaOHistorico);

        Intent intent = createStartIntent(parent, app, computer, managerBinder, withVDisplay);
        parent.startActivity(intent);
    }

    public static void doNetworkTest(final Activity parent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SpinnerDialog spinnerDialog = SpinnerDialog.displayDialog(parent,
                        parent.getResources().getString(R.string.nettest_title_waiting),
                        parent.getResources().getString(R.string.nettest_text_waiting),
                        false);

                int ret = MoonBridge.testClientConnectivity(CONNECTION_TEST_SERVER, 443, MoonBridge.ML_PORT_FLAG_ALL);
                spinnerDialog.dismiss();

                String dialogSummary;
                if (ret == MoonBridge.ML_TEST_RESULT_INCONCLUSIVE) {
                    dialogSummary = parent.getResources().getString(R.string.nettest_text_inconclusive);
                }
                else if (ret == 0) {
                    dialogSummary = parent.getResources().getString(R.string.nettest_text_success);
                }
                else {
                    dialogSummary = parent.getResources().getString(R.string.nettest_text_failure);
                    dialogSummary += MoonBridge.stringifyPortFlags(ret, "\n");
                }

                Dialog.displayDialog(parent,
                        parent.getResources().getString(R.string.nettest_title_done),
                        dialogSummary,
                        false);
            }
        }).start();
    }

    /**
     * O texto do desfecho de um pedido de encerrar sessão.
     *
     * Existe para os dois caminhos de encerrar não discordarem. O do catálogo olhava o
     * retorno de quitApp(); o da própria sessão descartava o retorno e anunciava sucesso
     * sempre — a pessoa saía confiante e o jogo continuava rodando no PC, segurando o
     * display virtual. A pergunta é a mesma, então a resposta passa a sair do mesmo lugar.
     */
    public static String quitResultMessage(Context context, boolean quit, String appName) {
        return context.getString(quit ? R.string.applist_quit_success_named
                : R.string.applist_quit_fail_named, appName);
    }

    /**
     * O texto de uma falha ao encerrar, com o detalhe quando ele existe.
     *
     * IOException sem mensagem existe, e nesse caso o aviso mostrava a palavra "null" para
     * a pessoa. Sem texto, vale o nome da falha.
     */
    public static String quitFailureMessage(Context context, String detail, String appName) {
        return (detail == null || detail.isEmpty())
                ? context.getString(R.string.applist_quit_fail_named, appName)
                : detail;
    }

    public static void doQuit(final Activity parent,
                              final NvHTTP httpConn,
                              final String appName,
                              final Runnable onComplete,
                              final Runnable onFail
    ) {
        parent.runOnUiThread(() -> VidroToast.show(parent, parent.getString(R.string.applist_quit_app_named, appName), Toast.LENGTH_SHORT));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                boolean failed = false;
                try {
                    message = quitResultMessage(parent, httpConn.quitApp(), appName);
                } catch (HostHttpResponseException e) {
                    failed = true;
                    if (e.getErrorCode() == 599) {
                        message = parent.getString(R.string.applist_quit_not_owner, e.getErrorCode());
                    }
                    else {
                        // e.getMessage() monta a frase em inglês, cravada na classe de
                        // exceção. Só o corpo vem do host; a moldura passa a ser nossa e
                        // segue o idioma do resto da tela.
                        message = parent.getString(R.string.host_error_body,
                                e.getErrorMessage(), e.getErrorCode());
                    }
                } catch (UnknownHostException e) {
                    failed = true;
                    message = parent.getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    failed = true;
                    message = parent.getResources().getString(R.string.error_404);
                } catch (IOException | XmlPullParserException e) {
                    failed = true;
                    message = e.getMessage();
                    e.printStackTrace();
                } finally {
                    if (failed) {
                        if (onFail != null) {
                            onFail.run();
                        }
                    } else {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                }

                // Só o caminho de IOException chega aqui com texto possivelmente nulo; nos
                // outros a mensagem já veio pronta e a guarda não faz nada.
                final String finalMessage = quitFailureMessage(parent, message, appName);
                final boolean finalFailed = failed;
                parent.runOnUiThread(() -> {
                    if (finalFailed) {
                        // Falha não cabe em aviso passageiro. O aviso é uma pílula de duas
                        // linhas com ellipsize, e a resposta do host é longa: ela chegava
                        // cortada em "…" no meio da frase, sem como expandir, rolar ou
                        // copiar — justamente na única informação de diagnóstico da tela.
                        Dialog.displayDialog(parent,
                                parent.getString(R.string.applist_quit_fail_named, appName),
                                finalMessage, false);
                    }
                    else {
                        VidroToast.show(parent, finalMessage, Toast.LENGTH_LONG);
                    }
                });
            }
        }).start();

    }

    public static void doQuit(final Activity parent,
                              final ComputerDetails computer,
                              final NvApp app,
                              final ComputerManagerService.ComputerManagerBinder managerBinder,
                              final Runnable onComplete
    ) {
        try {
            NvHTTP httpConn = new NvHTTP(
                    ServerHelper.getCurrentAddressFromComputer(computer),
                    computer.httpsPort,
                    managerBinder.getUniqueId(),
                    computer.serverCert,
                    PlatformBinding.getCryptoProvider(parent)
            );
            doQuit(
                    parent,
                    httpConn,
                    app.getAppName(),
                    onComplete,
                    null
            );
        } catch (Exception e) {
            e.printStackTrace();

            // Exceção sem mensagem chegava aqui como null, e o aviso simplesmente não
            // aparecia: a pessoa tocava em "Sair do Jogo" e nada acontecia na tela.
            final String toastMessage = quitFailureMessage(parent, e.getMessage(), app.getAppName());
            parent.runOnUiThread(() -> VidroToast.show(parent, toastMessage, Toast.LENGTH_LONG));
        }
    }
}
