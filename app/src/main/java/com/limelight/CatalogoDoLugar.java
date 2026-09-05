package com.limelight;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.grid.AppGridAdapter;
import com.limelight.grid.AppObject;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.profiles.ProfilesManager;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.ui.compose.EstadoDoHostNoCatalogo;
import com.limelight.ui.compose.EstadoDoCatalogo;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParserException;

/**
 * O catálogo de um lugar — os aplicativos que uma máquina publica.
 *
 * <h2>O que isto era, e por que deixou de ser</h2>
 *
 * Era a Activity {@code AppView}. Ela deixou de ser uma Activity para que o nome da máquina
 * possa <b>viajar</b> da lista até o título daqui: continuidade é impossível atravessando
 * fronteira de janela — o sistema não sabe que os dois são a mesma coisa, então ele só sabe
 * apagar uma página e acender outra.
 *
 * <p>Toda a lógica veio inteira: o polling, o cache do applist, os apps escondidos, o menu de
 * contexto, os atalhos. O que mudou foi de quem ela pede as coisas — em vez de ser a Activity,
 * ela <b>tem</b> uma. É por isso que a transformação foi mecânica e não uma reescrita: o
 * comportamento não é assunto desta mudança.
 *
 * <h2>O que ela NÃO faz mais</h2>
 *
 * Não liga no {@code ComputerManagerService}: a Casca já está ligada nele, e dois clientes do
 * mesmo binder significam dois {@code startPolling} concorrentes — o segundo substitui o
 * ouvinte do primeiro em silêncio. O binder chega pronto, por {@link #ligarBinder}.
 */
public class CatalogoDoLugar {

    /** Quem hospeda: contexto, recursos, menus e ciclo de vida. */
    private final AppCompatActivity act;

    /** Onde o desenho é publicado. A Casca observa este objeto. */
    private final EstadoDoCatalogo estado;

    /** Chamado quando o catálogo pede para sair — a Casca desempilha. */
    private final Runnable aoFechar;

    public CatalogoDoLugar(AppCompatActivity act, EstadoDoCatalogo estado,
                           String uuid, String nome, boolean mostrarEscondidos,
                           Runnable aoFechar) {
        this.act = act;
        this.estado = estado;
        this.aoFechar = aoFechar;
        this.uuidString = uuid;
        this.nomeDoHost = nome;
        this.showHiddenApps = mostrarEscondidos;
    }

    /**
     * O binder vem de fora, já ligado.
     *
     * Chamado pela Casca assim que o serviço responde — que pode ser antes ou depois de
     * {@link #criar()}, e por isso os dois caminhos disparam o polling.
     */
    public void ligarBinder(ComputerManagerService.ComputerManagerBinder binder) {
        this.managerBinder = binder;
        if (criado) {
            aoLigarServico();
        }
    }

    /** A View que ancora o menu de contexto do sistema. */
    private View ancora;

    public void ligarAncora(View ancora) {
        this.ancora = ancora;
    }

    private boolean criado;

    /** A View que hospeda o catalogo inteiro. Tambem e a ancora do menu de contexto. */
    private androidx.compose.ui.platform.ComposeView catalogoCompose;
    private String nomeDoHost;

    /**
     * Recarregar precisa dizer que ouviu.
     *
     * O toque chamava invalidateStateForComputer e a tela nao mudava em nada -- sem rotulo,
     * sem bloqueio, e sem resposta nenhuma quando o servico ainda nem tinha ligado. Quem nao
     * ve retorno toca de novo, e cada toque enfileira outra consulta ao host. Quem devolve o
     * rotulo e a resposta do host, em notifyComputerUpdated.
     */
    private boolean recarregando;
    private static final long ESPERA_DO_RECARREGAR_MS = 6000;
    private final Runnable soltarRecarregar = new Runnable() {
        @Override
        public void run() {
            // O rotulo de espera nao pode ficar preso para sempre se o host nunca responder.
            marcarRecarregando(false);
        }
    };

    /**
     * O P04: o que a faixa do topo diz quando o host some enquanto a tela esta aberta.
     *
     * Antes, quando o host dormia no meio da escolha, o AppView chamava finish() e jogava a
     * pessoa de volta na home com uma torrada de dois segundos: perdia-se o rolamento e nao
     * dava para saber se ela tinha feito algo errado.
     */
    private EstadoDoHostNoCatalogo estadoDoHost = EstadoDoHostNoCatalogo.NORMAL;

    /** O app que o toque longo marcou. Mesmo padrao de PcView.hostDoMenu. */
    private AppObject appDoMenu;
    /** A View da celula desse app, de onde sai a capa que vira icone de atalho. */
    private View celulaDoMenu;
    /** As celulas ligadas agora, por posicao. So as visiveis interessam. */
    private final android.util.SparseArray<View> celulasLigadas = new android.util.SparseArray<>();

    /**
     * A capa ja carregada da celula marcada, ou null.
     *
     * Antes isto vinha de `info.targetView`, que o menu de contexto de uma AbsListView
     * entregava de graca. Com a grade em Compose nao ha targetView: quem guarda a celula e o
     * proprio toque longo.
     */
    private Bitmap capaDaCelulaMarcada() {
        if (celulaDoMenu == null) {
            return null;
        }
        ImageView imagem = celulaDoMenu.findViewById(R.id.grid_image);
        if (imagem == null || !(imagem.getDrawable() instanceof BitmapDrawable)) {
            return null;
        }
        return ((BitmapDrawable) imagem.getDrawable()).getBitmap();
    }
    private AppGridAdapter appGridAdapter;
    private String uuidString;
    private ShortcutHelper shortcutHelper;

    private ComputerDetails computer;
    private ComputerManagerService.ApplistPoller poller;
    private SpinnerDialog blockingLoadSpinner;
    private String lastRawApplist;
    private int lastRunningAppId;
    private boolean suspendGridUpdates;
    private boolean inForeground;
    private boolean showHiddenApps;
    private HashSet<Integer> hiddenAppIds = new HashSet<>();

    private PreferenceConfiguration prefConfig;

    private final static int START_OR_RESUME_ID = 1;
    private final static int QUIT_ID = 2;
    private final static int START_WITH_QUIT = 4;
    private final static int VIEW_DETAILS_ID = 5;
    private final static int CREATE_SHORTCUT_ID = 6;
    private final static int EXPORT_LAUNCHER_FILE_ID = 7;
    private final static int HIDE_APP_ID = 8;
    private final static int START_WITH_VDISPLAY = 20;
    private final static int START_WITH_QUIT_VDISPLAY = 21;

    public final static String HIDDEN_APPS_PREF_FILENAME = "HiddenApps";

    /**
     * Os extras que trazem alguém direto para um catálogo.
     *
     * Ficam aqui porque são do catálogo, e continuam existindo porque os pontos de entrada de
     * fora — o atalho da tela inicial, o arquivo `.art`, o link `art://` — chegam por Intent e
     * não por toque. O que mudou é o destino: eles apontam para a Casca, que empilha a rota do
     * catálogo em vez de abrir uma janela nova.
     */
    public final static String NAME_EXTRA = "Name";
    public final static String UUID_EXTRA = "UUID";
    public final static String NEW_PAIR_EXTRA = "NewPair";
    public final static String SHOW_HIDDEN_APPS_EXTRA = "ShowHiddenApps";

    private ComputerManagerService.ComputerManagerBinder managerBinder;

    /**
     * O que era `onServiceConnected`, agora disparado por quem já está ligado.
     *
     * A ordem interna não mudou e ela importa: o adaptador nasce ANTES de o binder ficar
     * visível a este objeto, porque `updateUiWithServerinfo` pode chegar de outra thread e
     * tocaria num adaptador nulo; e o cache é carregado ANTES do polling, porque a primeira
     * resposta de serverinfo é o que acende o ícone do app que já está rodando.
     *
     * Continua numa thread própria pelo mesmo motivo de sempre: `waitForReady` bloqueia.
     */
    private void aoLigarServico() {
        final ComputerManagerService.ComputerManagerBinder localBinder = managerBinder;
        if (localBinder == null || appGridAdapter != null) {
            return;
        }

        // Enquanto o adaptador não existe, o binder não pode ser visto por quem lê o estado.
        managerBinder = null;

        new Thread() {
            @Override
            public void run() {
                localBinder.waitForReady();

                computer = localBinder.getComputer(uuidString);
                if (computer == null) {
                    act.runOnUiThread(aoFechar);
                    return;
                }

                shortcutHelper.createAppViewShortcut(computer, true, recemPareado);
                shortcutHelper.reportComputerShortcutUsed(computer);

                try {
                    appGridAdapter = new AppGridAdapter(act,
                            PreferenceConfiguration.readPreferences(act),
                            computer, localBinder.getUniqueId(),
                            showHiddenApps);
                } catch (Exception e) {
                    e.printStackTrace();
                    act.runOnUiThread(aoFechar);
                    return;
                }

                appGridAdapter.updateHiddenApps(hiddenAppIds, true);

                managerBinder = localBinder;

                populateAppGridWithCache();
                startComputerUpdates();

                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        montarCatalogo();
                    }
                });
            }
        }.start();
    }

    /** Veio de um pareamento que acabou de dar certo — o atalho é criado à força. */
    private boolean recemPareado;

    public void marcarRecemPareado(boolean sim) {
        this.recemPareado = sim;
    }

    public void aoTrocarConfiguracao(Configuration newConfig) {

        this.prefConfig = PreferenceConfiguration.readPreferences(act);

        // If appGridAdapter is initialized, let it know about the configuration change.
        // If not, it will pick it up when it initializes.
        if (appGridAdapter != null) {
            // Update the app grid adapter to create grid items with the correct layout
            appGridAdapter.updateLayoutWithPreferences(act, this.prefConfig);

            // Antes daqui saía uma transação de fragmento vazia, cujo único efeito era
            // forçar a grade de Views a reinflar e pegar o layout novo. Com a grade em
            // Compose, mudar o estado é o que redesenha — e a geração é o que faz o
            // adaptador ser relido, porque ele é a mesma instância de sempre.
            montarCatalogo();
        }
    }

    /**
     * O ouvinte deste catálogo — emprestado, e não instalado.
     *
     * `ComputerManagerBinder.startPolling` guarda UM ouvinte. Instalar o daqui derrubaria o da
     * home em silêncio, e o silêncio é o problema: a home continuaria desenhada e parada, sem
     * ninguém entender por quê. Quem instala é a Casca; ela repassa cada atualização para cá
     * enquanto este catálogo estiver em cena.
     */
    public ComputerManagerListener ouvinte() {
        return ouvinte;
    }

    private final ComputerManagerListener ouvinte = new ComputerManagerListener() {
            @Override
            public void notifyComputerUpdated(final ComputerDetails details) {
                // Do nothing if updates are suspended
                if (suspendGridUpdates) {
                    return;
                }

                // Don't care about other computers
                if (!details.uuid.equalsIgnoreCase(uuidString)) {
                    return;
                }

                if (details.state == ComputerDetails.State.OFFLINE) {
                    // The PC is unreachable now
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Display a toast to the user and quit the activity
                            estadoDoHost = EstadoDoHostNoCatalogo.DORMINDO;
                            montarCatalogo();
                        }
                    });

                    return;
                }

                // Close immediately if the PC is no longer paired
                // Voltou a responder: a faixa sai, a grade acende e o rotulo de espera cai.
                if (details.state == ComputerDetails.State.ONLINE
                        && details.pairState == PairingManager.PairState.PAIRED) {
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            marcarRecarregando(false);
                            if (estadoDoHost != EstadoDoHostNoCatalogo.NORMAL) {
                                estadoDoHost = EstadoDoHostNoCatalogo.NORMAL;
                                montarCatalogo();
                            }
                        }
                    });
                }

                if (details.state == ComputerDetails.State.ONLINE && details.pairState != PairingManager.PairState.PAIRED) {
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Disable shortcuts referencing this PC for now
                            shortcutHelper.disableComputerShortcut(details,
                                    act.getResources().getString(R.string.scut_not_paired));

                            // Display a toast to the user and quit the activity
                            estadoDoHost = EstadoDoHostNoCatalogo.SEM_PAREAMENTO;
                            montarCatalogo();
                        }
                    });

                    return;
                }

                // App list is the same or empty
                if (details.rawAppList == null || details.rawAppList.equals(lastRawApplist)) {

                    // Let's check if the running app ID changed
                    if (details.runningGameId != lastRunningAppId) {
                        // Update the currently running game using the app ID
                        lastRunningAppId = details.runningGameId;
                        updateUiWithServerinfo(details);
                    }

                    return;
                }

                lastRunningAppId = details.runningGameId;
                lastRawApplist = details.rawAppList;

                try {
                    updateUiWithAppList(NvHTTP.getAppListByReader(new StringReader(details.rawAppList)));
                    updateUiWithServerinfo(details);

                    if (blockingLoadSpinner != null) {
                        blockingLoadSpinner.dismiss();
                        blockingLoadSpinner = null;
                    }
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }
            }
    };

    /**
     * Começa a perguntar ao host o que ele publica.
     *
     * Só o applist poller: o de computador é da Casca. Os dois eram um só quando isto era uma
     * Activity, e separá-los é o que permite a home continuar viva com o catálogo aberto.
     */
    private void startComputerUpdates() {
        if (managerBinder == null || !inForeground) {
            return;
        }

        if (poller == null) {
            poller = managerBinder.createAppListPoller(computer);
        }
        poller.start();
    }

    private void stopComputerUpdates() {
        if (poller != null) {
            poller.stop();
        }

        // Sem stopPolling: quem manda no polling é a Casca, e a home continua precisando
        // dele depois que este catálogo sai de cena.

        if (appGridAdapter != null) {
            appGridAdapter.cancelQueuedOperations();
        }
    }

    /**
     * Entra em cena.
     *
     * Sem `setContentView` e sem `bindService`: quem desenha é a Casca, e quem está ligado no
     * serviço é ela. O que sobrou é o que só este objeto sabe fazer — ler os apps escondidos
     * deste lugar e publicar o primeiro desenho.
     */
    public void criar() {
        criado = true;
        inForeground = true;

        shortcutHelper = new ShortcutHelper(act);

        SharedPreferences hiddenAppsPrefs =
                act.getSharedPreferences(HIDDEN_APPS_PREF_FILENAME, Context.MODE_PRIVATE);
        for (String hiddenAppIdStr : hiddenAppsPrefs.getStringSet(uuidString, new HashSet<String>())) {
            hiddenAppIds.add(Integer.parseInt(hiddenAppIdStr));
        }

        this.prefConfig = PreferenceConfiguration.readPreferences(act);

        montarCatalogo();
        aoLigarServico();
    }

    private void updateHiddenApps(boolean hideImmediately) {
        HashSet<String> hiddenAppIdStringSet = new HashSet<>();

        for (Integer hiddenAppId : hiddenAppIds) {
            hiddenAppIdStringSet.add(hiddenAppId.toString());
        }

        act.getSharedPreferences(HIDDEN_APPS_PREF_FILENAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(uuidString, hiddenAppIdStringSet)
                .apply();

        appGridAdapter.updateHiddenApps(hiddenAppIds, hideImmediately);
    }

    private void populateAppGridWithCache() {
        try {
            // Try to load from cache
            lastRawApplist = CacheHelper.readInputStreamToString(CacheHelper.openCacheFileForInput(act.getCacheDir(), "applist", uuidString));
            List<NvApp> applist = NvHTTP.getAppListByReader(new StringReader(lastRawApplist));
            updateUiWithAppList(applist);
            LimeLog.info("Loaded applist from cache");
        } catch (IOException | XmlPullParserException e) {
            if (lastRawApplist != null) {
                LimeLog.warning("Saved applist corrupted: "+lastRawApplist);
                e.printStackTrace();
            }
            LimeLog.info("Loading applist from the network");
            // We'll need to load from the network
            loadAppsBlocking();
        }
    }

    private void loadAppsBlocking() {
        blockingLoadSpinner = SpinnerDialog.displayDialog(act, act.getResources().getString(R.string.applist_refresh_title),
                act.getResources().getString(R.string.applist_refresh_msg), true);
    }

    public void destruir() {

        SpinnerDialog.closeDialogs(act);
        Dialog.closeDialogs();

        // Sem unbindService: quem ligou no serviço foi a Casca, e ela continua ligada
        // depois que este catálogo sai de cena. Desligar aqui derrubaria o polling da home.
        managerBinder = null;
    }

    public void retomar() {

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(act);

        inForeground = true;
        startComputerUpdates();

    }

    public void pausar() {

        inForeground = false;
        stopComputerUpdates();
    }

    public void aoVoltarDeOutraTela(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == ShortcutHelper.REQUEST_CODE_EXPORT_ART_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                ShortcutHelper.writeArtFileToUri(act, uri);
            } else {
                // Clear the content if the user cancelled or if there was an error before this point
                ShortcutHelper.artFileContentToExport = null;
                // Show "File export cancelled." toast only if the user explicitly cancelled.
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(act, R.string.file_export_cancelled, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * O menu de contexto do sistema, agora encaminhado pela Casca.
     *
     * Ele continua sendo o do sistema porque a folha Vidro do catálogo é outra tarefa. O que
     * mudou é quem recebe o callback: a Activity da Casca, que repassa para cá quando a
     * âncora é a deste catálogo.
     */
    public void aoCriarMenuDeContexto(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        // Sem adapter na arvore de Views nao ha posicao no menuInfo: quem diz de qual app
        // o menu fala e appDoMenu, marcado no toque longo. Mesmo padrao de PcView.
        if (appDoMenu == null) {
            return;
        }
        AppObject selectedApp = appDoMenu;

        menu.setHeaderTitle(selectedApp.app.getAppName());

        if (lastRunningAppId == 0) {
            if (prefConfig.useVirtualDisplay) {
                menu.add(Menu.NONE, START_OR_RESUME_ID, 1, act.getResources().getString(R.string.applist_menu_start_primarydisplay));
            } else {
                menu.add(Menu.NONE, START_WITH_VDISPLAY, 1, act.getResources().getString(R.string.applist_menu_start_vdisplay));
            }
        } else {
            if (lastRunningAppId == selectedApp.app.getAppId()) {
                menu.add(Menu.NONE, START_OR_RESUME_ID, 1, act.getResources().getString(R.string.applist_menu_resume));
                menu.add(Menu.NONE, QUIT_ID, 2, act.getResources().getString(R.string.applist_menu_quit));
            }
            else {
                if (prefConfig.useVirtualDisplay) {
                    menu.add(Menu.NONE, START_WITH_QUIT_VDISPLAY, 1, act.getResources().getString(R.string.applist_menu_quit_and_start));
                    menu.add(Menu.NONE, START_WITH_QUIT, 2, act.getResources().getString(R.string.applist_menu_quit_and_start_primarydisplay));
                } else{
                    menu.add(Menu.NONE, START_WITH_QUIT, 1, act.getResources().getString(R.string.applist_menu_quit_and_start));
                    menu.add(Menu.NONE, START_WITH_QUIT_VDISPLAY, 2, act.getResources().getString(R.string.applist_menu_quit_and_start_vdisplay));
                }
            }
        }

        // Only show the hide checkbox if this is not the currently running app or it's already hidden
        if (lastRunningAppId != selectedApp.app.getAppId() || selectedApp.isHidden) {
            MenuItem hideAppItem = menu.add(Menu.NONE, HIDE_APP_ID, 3, act.getResources().getString(R.string.applist_menu_hide_app));
            hideAppItem.setCheckable(true);
            hideAppItem.setChecked(selectedApp.isHidden);
        }

        menu.add(Menu.NONE, VIEW_DETAILS_ID, 4, act.getResources().getString(R.string.applist_menu_details));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Only add an option to create shortcut if box art is loaded
            // and when we're in grid-mode (not list-mode).
            // O atalho leva a capa como icone, entao ele so faz sentido depois que a arte
            // carregou. A View da celula vem do toque longo, e nao mais do menuInfo do
            // adapter -- a grade nao e mais uma AbsListView.
            if (capaDaCelulaMarcada() != null) {
                menu.add(Menu.NONE, CREATE_SHORTCUT_ID, 5, act.getResources().getString(R.string.applist_menu_scut));
            }
        }

        menu.add(Menu.NONE, EXPORT_LAUNCHER_FILE_ID, 6, act.getResources().getString(R.string.applist_menu_export_launcher));
    }

    public void aoFecharMenuDeContexto(Menu menu) {
    }

    /** @return true quando o item era deste catálogo. */
    public boolean aoEscolherNoMenuDeContexto(MenuItem item) {
        if (appDoMenu == null) {
            return false;
        }
        final AppObject app = appDoMenu;
        int itemId = item.getItemId();
        switch (itemId) {
            case START_WITH_QUIT:
            case START_WITH_QUIT_VDISPLAY: {
                boolean withVDiaplay = itemId == START_WITH_QUIT_VDISPLAY;
                if (withVDiaplay && !(computer.vDisplaySupported && computer.vDisplayDriverReady)) {
                    UiHelper.displayVdisplayConfirmationDialog(
                        act,
                        computer,
                        () -> UiHelper.displayQuitConfirmationDialog(act, new Runnable() {
                            @Override
                            public void run() {
                                ServerHelper.doStart(act, app.app, computer, managerBinder, true);
                            }
                        }, null),
                        null
                    );
                } else {
                    // Display a confirmation dialog first
                    UiHelper.displayQuitConfirmationDialog(act, new Runnable() {
                        @Override
                        public void run() {
                            ServerHelper.doStart(act, app.app, computer, managerBinder, withVDiaplay);
                        }
                    }, null);
                }
                return true;
            }

            case START_OR_RESUME_ID:
            case START_WITH_VDISPLAY: {
                boolean withVDiaplay = itemId == START_WITH_VDISPLAY;
                if (withVDiaplay && !(computer.vDisplaySupported && computer.vDisplayDriverReady)) {
                    UiHelper.displayVdisplayConfirmationDialog(
                            act,
                            computer,
                            () -> ServerHelper.doStart(act, app.app, computer, managerBinder, true),
                            null
                    );
                } else {
                    // Resume is the same as start for us
                    ServerHelper.doStart(act, app.app, computer, managerBinder, withVDiaplay);
                }
                return true;
            }

            case QUIT_ID: {
                // Display a confirmation dialog first
                UiHelper.displayQuitConfirmationDialog(act, new Runnable() {
                    @Override
                    public void run() {
                        suspendGridUpdates = true;
                        ServerHelper.doQuit(act, computer,
                                app.app, managerBinder, new Runnable() {
                                    @Override
                                    public void run() {
                                        // Trigger a poll immediately
                                        suspendGridUpdates = false;
                                        if (poller != null) {
                                            poller.pollNow();
                                        }
                                    }
                                });
                    }
                }, null);
                return true;
            }

            case VIEW_DETAILS_ID: {
                Dialog.displayDialog(act, act.getResources().getString(R.string.title_details), app.app.toString(), false);
                return true;
            }

            case HIDE_APP_ID: {
                if (item.isChecked()) {
                    // Transitioning hidden to shown
                    hiddenAppIds.remove(app.app.getAppId());
                } else {
                    // Transitioning shown to hidden
                    hiddenAppIds.add(app.app.getAppId());
                }
                updateHiddenApps(false);
                return true;
            }

            case CREATE_SHORTCUT_ID: {
                Bitmap appBits = capaDaCelulaMarcada();
                if (appBits == null || !shortcutHelper.createPinnedGameShortcut(computer, app.app, appBits)) {
                    Toast.makeText(act, act.getResources().getString(R.string.unable_to_pin_shortcut), Toast.LENGTH_LONG).show();
                }
                return true;
            }

            case EXPORT_LAUNCHER_FILE_ID: {
                if (app.app.getAppUUID() == null || (app.app.getAppUUID() != null && app.app.getAppUUID().isEmpty())) {
                    UiHelper.displayConfirmationDialog(
                            act,
                            act.getResources().getString(R.string.title_export_sunshine_launcher_file),
                            act.getResources().getString(R.string.message_export_sunshine_launcher_file),
                            act.getResources().getString(R.string.proceed),
                            act.getResources().getString(R.string.cancel),
                            () -> shortcutHelper.exportLauncherFile(computer, app.app),
                            null
                    );
                } else {
                    shortcutHelper.exportLauncherFile(computer, app.app);
                }
                return true;
            }

            default: {
                // Não é nosso: quem chamou que decida. Antes isto subia para a Activity;
                // agora quem repassa é a Casca, e `false` é como ela sabe disso.
                return false;
            }
        }
    }

    /**
     * Liga o catalogo ao Compose.
     *
     * Um lugar so, chamado quando a lista muda, quando o recarregar entra ou sai de espera, e
     * quando o host aparece ou some. Nao ha "so escreve o que mudou" aqui -- o que se passa e
     * uma leitura do estado, e repetir a mesma leitura nao redesenha nada.
     */
    /**
     * Publica o desenho — e "publicar" agora quer dizer escrever num estado observável.
     *
     * Antes isto chamava `PonteDoCatalogo.montar`, que refazia o `setContent` inteiro a cada
     * mudança. Dentro da Casca a árvore de composição é uma só e vive enquanto a rota vive:
     * escrever nos campos é o que a faz redesenhar, e só o que mudou.
     *
     * A `geracao` existe por causa do adaptador. Ele é a MESMA instância do primeiro ao último
     * `notifyDataSetChanged`, então nada que só o observe perceberia a lista nova — o número
     * que anda junto é o que avisa.
     */
    private void montarCatalogo() {
        int quantos = appGridAdapter == null ? 0 : appGridAdapter.getCount();

        // So existe faixa de sessao quando ha app rodando de verdade no host.
        String sessao = null;
        if (appGridAdapter != null) {
            for (int i = 0; i < appGridAdapter.getCount(); i++) {
                AppObject candidato = (AppObject) appGridAdapter.getItem(i);
                if (candidato.isRunning) {
                    sessao = candidato.app.getAppName();
                    break;
                }
            }
        }

        estado.setNomeDoHost(nomeDoHost == null ? "" : nomeDoHost);
        estado.setUuid(uuidString == null ? "" : uuidString);
        estado.setQuantos(quantos);
        estado.setRecarregando(recarregando);
        estado.setSessaoAberta(sessao);
        estado.setEstadoDoHost(estadoDoHost);
        estado.setAdapter(appGridAdapter);
        estado.setGeracao(estado.getGeracao() + 1);
    }

    /** O toque em "Recarregar". Ver o comentario de `recarregando`. */
    public void pedirRecarregar() {
        if (managerBinder != null && computer != null) {
            managerBinder.invalidateStateForComputer(computer.uuid);
            marcarRecarregando(true);
        }
    }

    private void marcarRecarregando(boolean emEspera) {
        if (recarregando == emEspera) {
            return;
        }
        recarregando = emEspera;
        montarCatalogo();

        if (catalogoCompose != null) {
            if (emEspera) {
                catalogoCompose.postDelayed(soltarRecarregar, ESPERA_DO_RECARREGAR_MS);
            }
        }
    }

    public void retomarSessao() {
        if (computer == null || managerBinder == null) {
            return;
        }
        ServerHelper.doStart(act, new NvApp("app", null, computer.runningGameId, false),
                computer, managerBinder, prefConfig.useVirtualDisplay);
    }

    /**
     * O toque num app.
     *
     * Mesma decisao que vivia no OnItemClickListener da lista: com algo rodando no host, o
     * toque abre as opcoes em vez de lancar por cima -- salvo quando e o PROPRIO app aberto
     * e a pessoa pediu para retomar sem confirmar.
     */
    public kotlin.Unit aoTocarApp(Integer posicao) {
        if (appGridAdapter == null || posicao < 0 || posicao >= appGridAdapter.getCount()) {
            return kotlin.Unit.INSTANCE;
        }
        final AppObject app = (AppObject) appGridAdapter.getItem(posicao);

        if (lastRunningAppId != 0) {
            // Tocar no app que JA esta aberto retoma, sem perguntar -- e o Q33, "um toque
            // retoma o que ja esta rodando". A preferencia resumeWithoutConfirm que antes
            // governava isto foi podada: perguntar se pode retomar o que a propria linha
            // anuncia como aberto era a pergunta que nunca tinha resposta util.
            if (lastRunningAppId == app.app.getAppId()) {
                ServerHelper.doStart(act, app.app, computer, managerBinder, prefConfig.useVirtualDisplay);
            } else {
                // As opcoes do app: por ora o menu do sistema. A folha Vidro do catalogo e
                // a segunda metade do PE03.
                aoSegurarApp(posicao);
            }
        } else if (prefConfig.useVirtualDisplay
                && !(computer.vDisplaySupported && computer.vDisplayDriverReady)) {
            UiHelper.displayVdisplayConfirmationDialog(
                    act,
                    computer,
                    () -> ServerHelper.doStart(act, app.app, computer, managerBinder, true),
                    null);
        } else {
            ServerHelper.doStart(act, app.app, computer, managerBinder, prefConfig.useVirtualDisplay);
        }
        return kotlin.Unit.INSTANCE;
    }

    public kotlin.Unit aoLigarCelula(Integer posicao, View celula) {
        celulasLigadas.put(posicao, celula);
        return kotlin.Unit.INSTANCE;
    }

    public kotlin.Unit aoSegurarApp(Integer posicao) {
        if (appGridAdapter != null && posicao >= 0 && posicao < appGridAdapter.getCount()) {
            appDoMenu = (AppObject) appGridAdapter.getItem(posicao);
            celulaDoMenu = celulasLigadas.get(posicao);
            act.openContextMenu(catalogoCompose);
        }
        return kotlin.Unit.INSTANCE;
    }

    private void updateUiWithServerinfo(final ComputerDetails details) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean updated = false;

                    // Look through our current app list to tag the running app
                for (int i = 0; i < appGridAdapter.getCount(); i++) {
                    AppObject existingApp = (AppObject) appGridAdapter.getItem(i);

                    // There can only be one or zero apps running.
                    if (existingApp.isRunning &&
                            existingApp.app.getAppId() == details.runningGameId) {
                        // This app was running and still is, so we're done now
                        return;
                    }
                    else if (existingApp.app.getAppId() == details.runningGameId) {
                        // This app wasn't running but now is
                        existingApp.isRunning = true;
                        updated = true;
                    }
                    else if (existingApp.isRunning) {
                        // This app was running but now isn't
                        existingApp.isRunning = false;
                        updated = true;
                    }
                    else {
                        // This app wasn't running and still isn't
                    }
                }

                if (updated) {
                    appGridAdapter.notifyDataSetChanged();
                    montarCatalogo();
                }
            }
        });
    }

    private void updateUiWithAppList(final List<NvApp> appList) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean updated = false;

                // First handle app updates and additions
                for (NvApp app : appList) {
                    boolean foundExistingApp = false;

                    // Try to update an existing app in the list first
                    for (int i = 0; i < appGridAdapter.getCount(); i++) {
                        AppObject existingApp = (AppObject) appGridAdapter.getItem(i);
                        if (existingApp.app.getAppId() == app.getAppId()) {
                            // Found the app; update its properties
                            if (!existingApp.app.getAppName().equals(app.getAppName())) {
                                existingApp.app.setAppName(app.getAppName());
                                updated = true;
                            }

                            foundExistingApp = true;
                            break;
                        }
                    }

                    if (!foundExistingApp) {
                        // This app must be new
                        appGridAdapter.addApp(new AppObject(app));

                        // We could have a leftover shortcut from last time this PC was paired
                        // or if this app was removed then added again. Enable those shortcuts
                        // again if present.
                        shortcutHelper.enableAppShortcut(computer, app);

                        updated = true;
                    }
                }

                // Next handle app removals
                int i = 0;
                while (i < appGridAdapter.getCount()) {
                    boolean foundExistingApp = false;
                    AppObject existingApp = (AppObject) appGridAdapter.getItem(i);

                    // Check if this app is in the latest list
                    for (NvApp app : appList) {
                        if (existingApp.app.getAppId() == app.getAppId()) {
                            foundExistingApp = true;
                            break;
                        }
                    }

                    // This app was removed in the latest app list
                    if (!foundExistingApp) {
                        shortcutHelper.disableAppShortcut(computer, existingApp.app, act.getString(R.string.app_removed_from_pc));
                        appGridAdapter.removeApp(existingApp);
                        updated = true;

                        // Check this same index again because the item at i+1 is now at i after
                        // the removal
                        continue;
                    }

                    // Move on to the next item
                    i++;
                }

                if (updated) {
                    appGridAdapter.notifyDataSetChanged();
                    montarCatalogo();
                }
            }
        });
    }

}
