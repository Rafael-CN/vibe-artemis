package com.limelight;

import com.limelight.ui.VidroToast;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.limelight.binding.PlatformBinding;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.ui.compose.EstadoDaHome;
import com.limelight.ui.compose.EstadoHost;
import com.limelight.ui.compose.Historico;
import com.limelight.ui.compose.Rota;
import com.limelight.ui.compose.PonteDaCasca;
import com.limelight.ui.compose.EstadoDaCasca;
import com.limelight.ui.compose.PassoDoPareamento;
import com.limelight.ui.compose.PonteDaHome;
import com.limelight.ui.compose.PonteDoPareamento;
import com.limelight.ui.compose.HostUi;
import com.limelight.grid.assets.DiskAssetLoader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.http.PairingManager.PairState;
import com.limelight.nvstream.wol.WakeOnLanSender;
import com.limelight.preferences.AddComputerManually;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.preferences.StreamSettings;
import com.limelight.profiles.ProfilesManager;
import com.limelight.ui.VidroLightView;
import com.limelight.utils.Dialog;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.ResumeDecision;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.UiHelper;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.xmlpull.v1.XmlPullParserException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * A casca do app — a lista de lugares e o catálogo de cada um.
 *
 * <h2>Por que ela deixou de ser uma Activity</h2>
 *
 * Porque ela precisa existir em DOIS lugares. No arranque frio ela é a tela inteira, hospedada
 * por {@link PcView}. Depois que há uma sessão, ela é uma camada por cima do vídeo, hospedada
 * pela {@code Game} — e é isso que faz sair de uma sessão deixar de matá-la.
 *
 * <p>Antes, sair da sessão era {@code finish()}, e {@code Game.onStop()} derruba a conexão sem
 * condição. A sessão era a folha da árvore: para escolher outra coisa era preciso matar o que
 * estava rodando. Com a casca podendo ser convocada por cima do vídeo, escolher deixou de ser
 * um lugar aonde se vai.
 *
 * <h2>O que mudou do que era PcView</h2>
 *
 * Nada de comportamento. Foi a mesma transformação mecânica de {@code AppView} →
 * {@link CatalogoDoLugar}: em vez de <b>ser</b> a Activity, ela <b>tem</b> uma. O ciclo de
 * vida é chamado por quem hospeda, e {@code finish()} virou {@link #aoSair}, que cada
 * hospedeiro implementa do jeito dele — a {@code PcView} fecha, a {@code Game} volta para o
 * vídeo.
 */
public class CascaDoApp {

    /** Quem hospeda: contexto, recursos, menus e ciclo de vida. */
    private final AppCompatActivity act;

    /** Onde a árvore Compose é montada. Vem de quem hospeda, porque o layout é dele. */
    private final androidx.compose.ui.platform.ComposeView hospedeira;

    /**
     * O que fazer quando o Voltar chega na raiz da casca.
     *
     * Na {@code PcView} é fechar o app. Na {@code Game} é voltar para o vídeo, que continua
     * rodando atrás. É a única diferença de comportamento entre os dois hospedeiros.
     */
    private final Runnable aoSair;

    /**
     * Se este hospedeiro pode trocar o próprio conteúdo para sondar a GL.
     *
     * A sonda substitui o content view por uma {@code GLSurfaceView} só para ler o nome do
     * renderizador, e depois devolve. Na {@code PcView} isso é inofensivo — não há nada na
     * tela ainda. Na {@code Game} seria catastrófico: ela trocaria a árvore que contém a
     * SurfaceView do vídeo.
     *
     * <p>Falso é seguro porque o resultado é cacheado por FINGERPRINT do aparelho e a
     * {@code PcView} continua sendo o arranque frio — a sonda já rodou antes de existir
     * qualquer sessão.
     */
    private final boolean podeSondarGl;

    public CascaDoApp(AppCompatActivity act,
                      androidx.compose.ui.platform.ComposeView hospedeira,
                      boolean podeSondarGl,
                      Runnable aoSair) {
        this.act = act;
        this.hospedeira = hospedeira;
        this.podeSondarGl = podeSondarGl;
        this.aoSair = aoSair;
    }
    // Só recebe setVisibility. Prender um tipo de container aqui obrigaria o layout
    // a manter um que o desenho não pede mais.
    /**
     * A lista de lugares, separada da Activity.
     *
     * Substitui PcGridAdapter e, com ele, PcCellSnapshot: a lista e de data class imutavel,
     * entao a comparacao que o snapshot fazia campo a campo virou igualdade estrutural, e
     * quem a usa e a recomposicao. Ver docs/guides/compose-piloto-home.md.
     */
    private final EstadoDaHome estadoDaHome = new EstadoDaHome();

    /** A View que hospeda a home inteira. Tambem e a ancora do menu de contexto. */
    private androidx.compose.ui.platform.ComposeView gradeCompose;

    /**
     * A pilha da casca — hoje, home e catálogo.
     *
     * Duas telas numa árvore de composição só, e é o que permite o nome de uma máquina viajar
     * da lista até o título do catálogo em vez de a janela inteira piscar. Cenas, endereço e
     * ajustes continuam Activities, e naquelas arestas o pop de janela continua existindo.
     */
    private final EstadoDaCasca estadoDaCasca = new EstadoDaCasca();

    /**
     * Diz a casca que ha video rodando atras dela, ou que nao ha mais.
     *
     * Quem sabe disso e a Game, que hospeda as duas coisas. A PcView nunca chama isto, e por
     * isso a faixa nunca aparece la -- o que e correto: la nao ha nada atras.
     */
    public void dizerQueHaSessao(String nome) {
        estadoDaCasca.setSessaoViva(nome);
    }
    private PonteDaCasca casca;

    /** O catálogo do lugar aberto, ou nulo quando não há nenhum. */
    private CatalogoDoLugar catalogo;

    /**
     * O host que o toque longo marcou.
     *
     * O menu de contexto do Android descobre o item pela posicao no adapter
     * (AdapterContextMenuInfo). Sem adapter nao ha posicao, entao quem diz de qual host o
     * menu fala e este campo, escrito antes de openContextMenu. Some junto com o menu,
     * quando o PE03 puser uma folha Vidro no lugar dele.
     */
    private ComputerDetails hostDoMenu;
    private ShortcutHelper shortcutHelper;
    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private boolean freezeUpdates, runningPolling, inForeground, completeOnCreateCalled;
    private ComputerDetails.AddressTuple pendingPairingAddress;
    private String pendingPairingPin, pendingPairingPassphrase;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder)binder);

            // Wait in a separate thread to avoid stalling the UI
            new Thread() {
                @Override
                public void run() {
                    // Wait for the binder to be ready
                    localBinder.waitForReady();

                    // Now make the binder visible
                    managerBinder = localBinder;

                    // O catálogo pode ter nascido antes do serviço — é o caso do atalho da
                    // tela inicial, que abre a Casca já apontando para um lugar. Ele espera
                    // o binder em vez de ligar-se ao serviço por conta própria.
                    CatalogoDoLugar aberto = catalogo;
                    if (aberto != null) {
                        aberto.ligarBinder(localBinder);
                    }

                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            abrirLugarPedidoPorIntent();
                        }
                    });

                    // Start updates
                    startComputerUpdates();

                    // Force a keypair to be generated early to avoid discovery delays
                    new AndroidCryptoProvider(act).getClientCertificate();
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    public void aoTrocarConfiguracao(Configuration newConfig) {

        // Only reinitialize views if completeOnCreate() was called
        // before this callback. If it was not, completeOnCreate() will
        // handle initializing views with the config change accounted for.
        // This is not prone to races because both callbacks are invoked
        // in the main thread.
        if (completeOnCreateCalled) {
            // Reinitialize views just in case orientation changed
            initializeViews();
        }

        refreshProfileButton();

        // O atalho traz o lugar no próprio Intent. Ele é lido aqui e atendido quando o
        // serviço responder — ver abrirLugarPedidoPorIntent.
        uuidPedidoPorIntent = act.getIntent().getStringExtra(CatalogoDoLugar.UUID_EXTRA);
        nomePedidoPorIntent = act.getIntent().getStringExtra(CatalogoDoLugar.NAME_EXTRA);
        abrirLugarPedidoPorIntent();
    }

    /**
     * O lugar que um Intent pediu, esperando o serviço responder.
     *
     * O atalho da tela inicial e o arquivo `.art` chegam apontando para uma máquina, mas o
     * catálogo dela só pode nascer com o binder na mão — é ele que resolve o uuid num
     * `ComputerDetails`. Guardar o pedido e atendê-lo quando o serviço responde é o que
     * substitui a pilha de dois Intents que existia quando eram duas Activities.
     */
    private String uuidPedidoPorIntent;
    private String nomePedidoPorIntent;

    /**
     * Atende o pedido guardado, se houver um.
     *
     * Chamado das duas pontas — do onCreate e da resposta do serviço — porque a ordem entre
     * elas não é garantida, e o `uuidPedidoPorIntent` só é consumido uma vez.
     */
    private void abrirLugarPedidoPorIntent() {
        if (uuidPedidoPorIntent == null || managerBinder == null || gradeCompose == null) {
            return;
        }

        ComputerDetails detalhes = managerBinder.getComputer(uuidPedidoPorIntent);
        uuidPedidoPorIntent = null;
        nomePedidoPorIntent = null;
        if (detalhes != null) {
            doAppList(detalhes, false, false);
        }
    }

    private final static int PAIR_ID = 2;
    private final static int UNPAIR_ID = 3;
    private final static int WOL_ID = 4;
    private final static int DELETE_ID = 5;
    private final static int RESUME_ID = 6;
    private final static int QUIT_ID = 7;
    private final static int VIEW_DETAILS_ID = 8;
    private final static int FULL_APP_LIST_ID = 9;
    private final static int TEST_NETWORK_ID = 10;
    private final static int GAMESTREAM_EOL_ID = 11;
    private final static int OPEN_MANAGEMENT_PAGE_ID = 20;
    private final static int PAIR_ID_OTP = 21;

    private void initializeViews() {
        // Sem setContentView: quem infla é o hospedeiro. Na PcView o layout é a tela
        // inteira; na Game ele é uma camada por cima do vídeo, e trocar o conteúdo lá
        // levaria a SurfaceView junto.
        UiHelper.notifyNewRootView(act);

        // Allow floating expanded PiP overlays while browsing PCs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            act.setShouldDockBigOverlays(false);
        }

        // Set default preferences if we've never been run
        PreferenceManager.setDefaultValues(act, R.xml.preferences, false);

        gradeCompose = act.findViewById(R.id.vidroHomeCompose);
        montarHome();

        // O menu de contexto continua sendo o do sistema, com os mesmos itens. Ele so
        // deixou de ser ancorado na lista e passou a ser ancorado na View que hospeda a
        // home -- quem diz de qual host ele fala e hostDoMenu, marcado no toque longo.
        act.registerForContextMenu(gradeCompose);
    }

    /**
     * Liga a home ao Compose.
     *
     * Um lugar so, chamado tambem quando o historico ou a cena ativa mudam. O recuo da
     * barra de status vem por aqui e nao por fitsSystemWindows: a luz e o primeiro
     * elemento e precisa nascer na borda fisica da tela, senao fica acima dela uma faixa
     * chapada do fundo -- na gravacao a home saltava de #010205 para #0B1B27 numa linha de
     * pixel, atravessando a tela toda.
     */
    private void montarHome() {
        if (gradeCompose == null) {
            return;
        }
        // O que muda a cada chamada são só os campos observáveis; a árvore é montada uma vez.
        estadoDaCasca.setContinuar(Historico.ler(act));
        estadoDaCasca.setNomeDasCenas(nomeDoRodape());
        estadoDaCasca.setMotivoDoSilencio(motivoDoSilencio());

        if (casca != null) {
            return;
        }

        casca = new PonteDaCasca(gradeCompose, estadoDaCasca, estadoDaHome);

        casca.ligarHome(
                this::aoTocarHost,
                this::aoSegurarHost,
                this::aoContinuar,
                () -> {
                    act.startActivity(new Intent(act, AddComputerManually.class));
                    return kotlin.Unit.INSTANCE;
                },
                () -> {
                    act.startActivity(new Intent(act, ProfilesActivity.class));
                    return kotlin.Unit.INSTANCE;
                },
                () -> {
                    // Ajustes continua sendo Activity, e por um motivo concreto: ela hospeda a
                    // árvore de `preferences.xml` num PreferenceFragmentCompat, que é View e
                    // precisa de FragmentManager. Configuração avançada tem de continuar
                    // alcançável — esconder pode, apagar não. Nesta aresta o pop de janela
                    // continua existindo, e isso está dito em vez de escondido.
                    act.startActivity(new Intent(act, StreamSettings.class));
                    return kotlin.Unit.INSTANCE;
                },
                this::aoRetomarDaFolha,
                this::aoEncerrarDaFolha,
                this::aoParearDaFolha,
                this::aoAcordarDaFolha,
                this::aoRemoverDaFolha,
                this::aoAbrirMenuAntigo);

        casca.ligarCatalogo(
                () -> { if (catalogo != null) catalogo.pedirRecarregar(); return kotlin.Unit.INSTANCE; },
                () -> { if (catalogo != null) catalogo.retomarSessao(); return kotlin.Unit.INSTANCE; },
                () -> { estadoDaCasca.voltarParaHome(); return kotlin.Unit.INSTANCE; },
                () -> { estadoDaCasca.voltarParaHome(); return kotlin.Unit.INSTANCE; },
                (indice) -> { if (catalogo != null) catalogo.aoTocarApp(indice); return kotlin.Unit.INSTANCE; },
                (indice) -> { if (catalogo != null) catalogo.aoSegurarApp(indice); return kotlin.Unit.INSTANCE; },
                (indice, view) -> { if (catalogo != null) catalogo.aoLigarCelula(indice, view); return kotlin.Unit.INSTANCE; });

        // A saida e de quem HOSPEDA, e essa e a unica diferenca entre os dois.
        //
        // Na PcView ela fecha o app. Na Game ela volta para o video, que continua rodando
        // atras da casca -- e e por isso que sair de uma sessao deixou de mata-la.
        casca.ligarSaida(() -> { aoSair.run(); return kotlin.Unit.INSTANCE; });

        // O retorno para a sessao e o mesmo destino da saida, e nao um segundo caminho.
        //
        // Na PcView isso nunca acontece: la nao ha video atras, entao a faixa nunca aparece
        // e este canal nunca e usado. Na Game ele e o toque na faixa do topo.
        casca.ligarRetornoDaSessao(() -> { aoSair.run(); return kotlin.Unit.INSTANCE; });

        // O mesmo teste que o menu de contexto oferece, agora tambem alcancavel da folha.
        // Um caminho novo para a MESMA acao, e nao uma segunda implementacao dela.
        casca.ligarTesteDeRede((host) -> {
            ServerHelper.doNetworkTest(act);
            return kotlin.Unit.INSTANCE;
        });

        casca.montar();
    }

    /**
     * O rodape diz "Cenas" quando nenhuma esta ativa, e o nome da cena quando ha uma.
     *
     * O par nao some nem encolhe: no prototipo o rodape e fixo, e um par que muda de
     * largura sozinho faz a tela inteira dancar.
     */
    private String nomeDoRodape() {
        String ativa = ProfilesManager.getInstance().getActiveName();
        return (ativa == null || ativa.isEmpty()) ? act.getString(R.string.vidro_scenes) : ativa;
    }

    /**
     * Por que ninguem respondeu ainda -- P05 do catalogo de produto.
     *
     * A frase de antes era uma so: "nenhum PC respondeu ainda, deixe o app aberto".
     * Verdadeira e inutil quando o motivo e que o aparelho esta nos dados moveis ou com VPN
     * ligada, e o mDNS nunca vai chegar. O comentario do layout antigo ja sabia disso, o
     * manifesto ja pedia ACCESS_NETWORK_STATE, e nada era feito com a resposta.
     *
     * Devolve 0 quando nao ha nada de util a dizer, e ai vale a frase generica.
     */
    private int motivoDoSilencio() {
        ConnectivityManager cm =
                (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return 0;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network rede = cm.getActiveNetwork();
            NetworkCapabilities caps = rede == null ? null : cm.getNetworkCapabilities(rede);
            if (caps == null) {
                return R.string.vidro_why_no_network;
            }
            // VPN primeiro: com ela ligada o multicast nao sai da interface virtual, e o
            // host da casa nunca aparece por mais que se espere.
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return R.string.vidro_why_vpn;
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return R.string.vidro_why_mobile;
            }
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    && !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return R.string.vidro_why_no_network;
            }
        }

        // Na rede certa e ninguem respondeu: sobra a unica coisa que quem instalou agora
        // nao tem como adivinhar, que e o lado do PC.
        return R.string.vidro_why_host_off;
    }

    /** O toque numa linha do "Continuar": lanca direto, sem passar pelo catalogo. */
    private kotlin.Unit aoContinuar(Historico.Entrada entrada) {
        ComputerDetails details = estadoDaHome.detalhes(entrada.getHostUuid());
        if (details == null || managerBinder == null) {
            // O host do historico pode nem estar na lista ainda -- a sondagem leva um
            // instante. Nao inventamos erro: o toque simplesmente ainda nao vale.
            return kotlin.Unit.INSTANCE;
        }

        if (details.state != ComputerDetails.State.ONLINE) {
            // Dormindo ou fora do ar: o caminho util e o do proprio host, nao um
            // lancamento que vai falhar.
            hostDoMenu = details;
            act.openContextMenu(gradeCompose);
            return kotlin.Unit.INSTANCE;
        }

        ServerHelper.doStart(act,
                new NvApp(entrada.getAppNome(), null, entrada.getAppId(), false),
                details, managerBinder,
                PreferenceConfiguration.readPreferences(act).useVirtualDisplay);
        return kotlin.Unit.INSTANCE;
    }

    public void criar() {

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;

        // Create a GLSurfaceView to fetch GLRenderer unless we have
        // a cached result already.
        final GlPreferences glPrefs = GlPreferences.readPreferences(act);
        if (podeSondarGl
                && (!glPrefs.savedFingerprint.equals(Build.FINGERPRINT) || glPrefs.glRenderer.isEmpty())) {
            GLSurfaceView surfaceView = new GLSurfaceView(act);
            surfaceView.setRenderer(new GLSurfaceView.Renderer() {
                @Override
                public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                    // Save the GLRenderer string so we don't need to do this next time
                    glPrefs.glRenderer = gl10.glGetString(GL10.GL_RENDERER);
                    glPrefs.savedFingerprint = Build.FINGERPRINT;
                    glPrefs.writePreferences();

                    LimeLog.info("Fetched GL Renderer: " + glPrefs.glRenderer);

                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            completeOnCreate();
                        }
                    });
                }

                @Override
                public void onSurfaceChanged(GL10 gl10, int i, int i1) {
                }

                @Override
                public void onDrawFrame(GL10 gl10) {
                }
            });
            act.setContentView(surfaceView);
        }
        else {
            LimeLog.info("Cached GL Renderer: " + glPrefs.glRenderer);
            completeOnCreate();
        }

        Intent intent = act.getIntent();

        String hostname = intent.getStringExtra("hostname");
        int port = intent.getIntExtra("port", NvHTTP.DEFAULT_HTTP_PORT);
        pendingPairingPin = intent.getStringExtra("pin");
        pendingPairingPassphrase = intent.getStringExtra("passphrase");

        if (hostname != null && pendingPairingPin != null && pendingPairingPassphrase != null) {
            pendingPairingAddress = new ComputerDetails.AddressTuple(hostname, port);
        } else {
            pendingPairingPin = null;
            pendingPairingPassphrase = null;
        }
    }

    private void completeOnCreate() {
        completeOnCreateCalled = true;

        shortcutHelper = new ShortcutHelper(act);

        UiHelper.setLocale(act);

        // Bind to the computer manager service
        act.bindService(new Intent(act, ComputerManagerService.class), serviceConnection,
                Service.BIND_AUTO_CREATE);

        initializeViews();
    }

    private void startComputerUpdates() {
        // Only allow polling to start if we're bound to CMS, polling is not already running,
        // and our activity is in the foreground.
        if (managerBinder != null && !runningPolling && inForeground) {
            freezeUpdates = false;
            managerBinder.startPolling(new ComputerManagerListener() {
                @Override
                public void notifyComputerUpdated(final ComputerDetails details) {
                    // O CATÁLOGO OUVE PELA CASCA.
                    //
                    // `startPolling` guarda um ouvinte só. Antes eram duas Activities e cada
                    // uma instalava o seu, o que funcionava porque só uma existia por vez.
                    // Numa casca só, instalar dois faria o segundo derrubar o primeiro em
                    // silêncio — e silêncio aqui é a home parando de atualizar sem sinal.
                    CatalogoDoLugar aberto = catalogo;
                    if (aberto != null) {
                        aberto.ouvinte().notifyComputerUpdated(details);
                    }

                    if (!freezeUpdates) {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateComputer(details);
                            }
                        });

                        // Add a launcher shortcut for this PC (off the main thread to prevent ANRs)
                        if (details.pairState == PairState.PAIRED) {
                            shortcutHelper.createAppViewShortcutForOnlineHost(details);
//                        } else
                        }
                            if (pendingPairingAddress != null) {
                            if (
                                details.state == ComputerDetails.State.ONLINE &&
                                details.activeAddress.equals(pendingPairingAddress)
                            ) {
                                act.runOnUiThread(() -> {
                                    doPair(details, pendingPairingPin, pendingPairingPassphrase);
                                    pendingPairingAddress = null;
                                    pendingPairingPin = null;
                                    pendingPairingPassphrase = null;
                                });
                            }
                        }
                    }
                }
            });
            runningPolling = true;
        }
    }

    private void stopComputerUpdates(boolean wait) {
        if (managerBinder != null) {
            if (!runningPolling) {
                return;
            }

            freezeUpdates = true;

            managerBinder.stopPolling();

            if (wait) {
                managerBinder.waitForPollingStopped();
            }

            runningPolling = false;
        }
    }

    /** A cena ativa mudou: o rodape precisa dizer o nome novo. */
    private void refreshProfileButton() {
        montarHome();
    }

    public void destruir() {

        if (managerBinder != null) {
            act.unbindService(serviceConnection);
        }
    }

    public void retomar() {

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(act);

        refreshProfileButton();

        inForeground = true;
        startComputerUpdates();

        if (catalogo != null) {
            catalogo.retomar();
        }
    }

    public void pausar() {

        inForeground = false;
        stopComputerUpdates(false);

        if (catalogo != null) {
            catalogo.pausar();
        }
    }

    public void parar() {

        Dialog.closeDialogs();
    }

    public void aoCriarMenuDeContexto(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        stopComputerUpdates(false);

        // O MENU É REPARTIDO POR ROTA.
        //
        // A âncora é a mesma View para as duas telas — a Casca inteira é uma ComposeView — e
        // por isso quem decide de quem é o menu não pode ser a View: é a rota que está em
        // cima. Sem isto, o toque longo num aplicativo abriria o menu de um host.
        if (catalogo != null && !estadoDaCasca.estaNaHome()) {
            catalogo.aoCriarMenuDeContexto(menu, v, menuInfo);
            return;
        }

        // Call superclass
        

        if (hostDoMenu == null) {
            return;
        }
        ComputerObject computer = new ComputerObject(hostDoMenu);

        // Add a header with PC status details
        menu.clearHeader();
        String headerTitle = computer.details.name + " - ";
        switch (computer.details.state)
        {
            case ONLINE:
                headerTitle += act.getResources().getString(R.string.pcview_menu_header_online);
                break;
            case OFFLINE:
                menu.setHeaderIcon(R.drawable.ic_pc_offline);
                headerTitle += act.getResources().getString(R.string.pcview_menu_header_offline);
                break;
            case UNKNOWN:
                headerTitle += act.getResources().getString(R.string.pcview_menu_header_unknown);
                break;
        }

        menu.setHeaderTitle(headerTitle);

        // Inflate the context menu
        if (computer.details.state == ComputerDetails.State.OFFLINE ||
            computer.details.state == ComputerDetails.State.UNKNOWN) {
            menu.add(Menu.NONE, WOL_ID, 1, act.getResources().getString(R.string.pcview_menu_send_wol));
            // O aviso de fim do GameStream só cabe para quem teve GameStream. nvidiaServer
            // vem do serverinfo e nunca é persistido, então host desligado que nunca
            // respondeu fica false — os outros dois ramos deste menu já guardam assim.
            if (computer.details.nvidiaServer) {
                menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 2, act.getResources().getString(R.string.pcview_menu_eol));
            }
        }
        else if (computer.details.pairState != PairState.PAIRED) {
            // P09: "OTP Pair" so existe no Apollo, e a propria string de ajuda do app
            // admite isso. Quem escolhia a primeira -- a de cima, a que parece mais moderna
            // -- caia num diálogo cru com dois campos sem estilo e falhava. O bit de
            // modelo de permissoes so existe em Apollo -- o Vibeshine nao manda nenhum, e
            // hasPermissionModel() e exatamente essa pergunta. Sobra um caminho so, que e o que o
            // toque simples no cartao ja dispara.
            if (computer.details.getCapabilities().hasPermissionModel()) {
                menu.add(Menu.NONE, PAIR_ID_OTP, 1, act.getResources().getString(R.string.pcview_menu_pair_pc_otp));
            }
            menu.add(Menu.NONE, PAIR_ID, 2, act.getResources().getString(R.string.pcview_menu_pair_pc));
            if (computer.details.nvidiaServer) {
                menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 3, act.getResources().getString(R.string.pcview_menu_eol));
            } else {
                menu.add(Menu.NONE, OPEN_MANAGEMENT_PAGE_ID, 3, act.getResources().getString(R.string.pcview_menu_open_management_page));
            }
        }
        else {
            if (computer.details.runningGameId != 0) {
                menu.add(Menu.NONE, RESUME_ID, 1, act.getResources().getString(R.string.applist_menu_resume));
                menu.add(Menu.NONE, QUIT_ID, 2, act.getResources().getString(R.string.applist_menu_quit));
            }

            if (computer.details.nvidiaServer) {
                menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 3, act.getResources().getString(R.string.pcview_menu_eol));
            } else {
                menu.add(Menu.NONE, OPEN_MANAGEMENT_PAGE_ID, 3, act.getResources().getString(R.string.pcview_menu_open_management_page));
            }

            menu.add(Menu.NONE, FULL_APP_LIST_ID, 4, act.getResources().getString(R.string.pcview_menu_app_list));
        }

        menu.add(Menu.NONE, TEST_NETWORK_ID, 5, act.getResources().getString(R.string.pcview_menu_test_network));
        menu.add(Menu.NONE, DELETE_ID, 6, act.getResources().getString(R.string.pcview_menu_delete_pc));
        menu.add(Menu.NONE, VIEW_DETAILS_ID, 7,  act.getResources().getString(R.string.pcview_menu_details));
    }

    public void aoFecharMenuDeContexto(Menu menu) {
        // For some reason, this gets called again _after_ onPause() is called on this activity.
        // startComputerUpdates() manages this and won't actual start polling until the activity
        // returns to the foreground.
        startComputerUpdates();
    }


    /**
     * O pareamento inteiro, em tres passos -- o P07.
     *
     * O prototipo desenha tres e o app entregava um: os quatro digitos, sem dizer que ha
     * tres nem em qual voce esta. Aqui o passo e estado, e trocar de passo e trocar o
     * estado -- a tela nao e derrubada e reconstruida a cada mudanca.
     *
     * Chamado tanto da thread de rede quanto da UI, entao tudo passa pelo runOnUiThread.
     */
    private PassoDoPareamento passoDoPareamento;
    private String codigoDoPareamento = "";
    private ComputerDetails hostDoPareamento;
    private Runnable cancelarPareamento;

    private void mostrarPareamento(final PassoDoPareamento passo, final String codigo) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                passoDoPareamento = passo;
                if (codigo != null) {
                    codigoDoPareamento = codigo;
                }
                montarPareamento();
            }
        });
    }

    private void esconderPareamento() {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                passoDoPareamento = null;
                montarPareamento();
            }
        });
    }

    private void montarPareamento() {
        androidx.compose.ui.platform.ComposeView tela = act.findViewById(R.id.vidroPareamentoCompose);
        if (tela == null) {
            return;
        }

        if (passoDoPareamento == null) {
            tela.setVisibility(View.GONE);
            return;
        }

        final ComputerDetails host = hostDoPareamento;
        final String nome = host != null && host.name != null ? host.name : "";

        // O P08: a pagina do host, quando ele tem uma.
        //
        // No Vibeshine o codigo NAO aparece na tela do PC -- ele vai na pagina web do host,
        // e a unica forma de chegar la era sair do pareamento e achar um item escondido num
        // menu de toque longo, que nem alcancavel era com esta tela aberta.
        String pagina = null;
        if (host != null && host.activeAddress != null) {
            pagina = "https://" + host.activeAddress.address + ":" + (host.guessExternalPort() + 1);
        }
        final String paginaFinal = pagina;

        tela.setVisibility(View.VISIBLE);
        PonteDoPareamento.montar(
                tela,
                passoDoPareamento,
                nome,
                codigoDoPareamento,
                paginaFinal,
                () -> {
                    if (cancelarPareamento != null) {
                        cancelarPareamento.run();
                    }
                    esconderPareamento();
                    return kotlin.Unit.INSTANCE;
                },
                () -> {
                    // O P02: pedir um codigo novo SEM sair da tela. Antes a recusa derrubava
                    // o painel, torrava por quatro segundos e devolvia a pessoa a home, de
                    // onde ela tinha de reencontrar o cartao do host para recomecar.
                    if (host != null) {
                        doPair(host, null, null);
                    }
                    return kotlin.Unit.INSTANCE;
                },
                () -> {
                    if (paginaFinal != null) {
                        abrirPaginaDoHost(paginaFinal);
                    }
                    return kotlin.Unit.INSTANCE;
                },
                () -> {
                    esconderPareamento();
                    if (host != null) {
                        doAppList(host, true, false);
                    }
                    return kotlin.Unit.INSTANCE;
                });
    }

    /**
     * Abre a pagina de gerencia do host no navegador.
     *
     * O certificado do Vibeshine e autoassinado e o navegador vai reclamar antes de abrir --
     * o botao avisa isso na propria tela, senao parece que o app mandou a pessoa para um
     * lugar quebrado.
     */
    private void abrirPaginaDoHost(String url) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (android.content.ActivityNotFoundException e) {
            VidroToast.show(act, act.getResources().getString(R.string.error_usb_prohibited),
                    Toast.LENGTH_SHORT);
        }
    }

    /**
     * Devolve ao host a requisição de pareamento que ficou pendurada.
     *
     * O primeiro passo do pareamento vai por uma conexão sem prazo de leitura
     * ({@code httpClientLongConnectNoReadTimeout}, readTimeout 0): enquanto o PC não
     * confirma o código, {@code pm.pair()} não retorna, o {@code stopComputerUpdates(true)}
     * do início segue valendo e a lista de PCs congela sem nada na tela dizendo por quê.
     * O protocolo tem como desfazer isso — o {@code /unpair} descarta o pedido pendente e
     * faz o host responder ao {@code /pair} —, e é o mesmo caminho que o PairingManager já
     * usa em todos os seus ramos de erro. Com o {@code /pair} respondido, {@code pair()}
     * volta, o fluxo desce pelo caminho normal e o {@code startComputerUpdates()} do fim
     * roda sozinho.
     *
     * Em thread e em instância PRÓPRIA de NvHTTP: a da thread de pareamento está presa na
     * conexão longa, e reusá-la só empilharia uma segunda espera atrás da primeira.
     *
     * Se o host não devolver o pedido, nada piora — a requisição expira sozinha junto com o
     * código, e destravar a sondagem já é o ganho. O Sunshine devolve, e o Vibeshine é fork
     * dele; confirmar no aparelho.
     */
    private void abortPairing(final ComputerDetails computer) {
        final ComputerManagerService.ComputerManagerBinder binder = managerBinder;
        if (binder == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NvHTTP unpairConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, binder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(act));
                    unpairConn.unpair();
                } catch (IOException e) {
                    // Falhar aqui não muda o que o usuário vê: a tela do código já saiu e o
                    // pedido expira sozinho no host.
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void doPair(final ComputerDetails computer, String otp, String passphrase) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            VidroToast.show(act, act.getResources().getString(R.string.pair_pc_offline), Toast.LENGTH_SHORT);
            return;
        }
        if (managerBinder == null) {
            VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
            return;
        }

        VidroToast.show(act, act.getResources().getString(R.string.pairing), Toast.LENGTH_SHORT);

        // Cancelar vale uma vez só: dois toques mandariam dois /unpair, e o segundo
        // chegaria depois de o host já ter devolvido o pedido — desfazendo um pareamento
        // que pode ter dado certo no meio tempo.
        final AtomicBoolean pairingCancelled = new AtomicBoolean(false);
        final Runnable cancelPairing = new Runnable() {
            @Override
            public void run() {
                if (pairingCancelled.compareAndSet(false, true)) {
                    abortPairing(computer);
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                boolean success = false;
                boolean recusado = false;
                boolean pareou = false;
                try {
                    // Stop updates and wait while pairing
                    stopComputerUpdates(true);

                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(act));
                    if (httpConn.getPairState() == PairState.PAIRED) {
                        // Don't display any toast, but open the app list
                        message = null;
                        success = true;
                    }
                    else {
                        String pinStr = otp;
                        if (pinStr == null) {
                            pinStr = PairingManager.generatePinString();
                        }

                        // A tela de pareamento no lugar do diálogo. O código é o assunto
                        // inteiro deste momento, e um AlertDialog o enterra numa linha de
                        // corpo de texto entre dois parágrafos.
                        if (passphrase == null) {
                            hostDoPareamento = computer;
                            cancelarPareamento = cancelPairing;
                            mostrarPareamento(PonteDoPareamento.passoCodigo(), pinStr);
                        } else {
                            hostDoPareamento = computer;
                            cancelarPareamento = cancelPairing;
                            mostrarPareamento(PonteDoPareamento.passoCodigo(), "");
                        }

                        PairingManager pm = httpConn.getPairingManager();

                        PairState pairState = pm.pair(httpConn.getServerInfo(true), pinStr, passphrase);
                        if (pairState == PairState.PIN_WRONG) {
                            // P02: a recusa NAO derruba a tela.
                            //
                            // Antes daqui saia uma mensagem que virava torrada de quatro
                            // segundos, com o painel do codigo ja derrubado -- e a pessoa
                            // acordava na home tendo de reencontrar o cartao do host para
                            // recomecar. O codigo expira a cada minuto, entao este e o
                            // caminho COMUM, nao o excepcional.
                            //
                            // Agora o mesmo painel troca de estado: os digitos apagam, entra
                            // o titulo da recusa com a causa mais provavel, e "Pedir um
                            // codigo novo" refaz o pedido sem sair dali.
                            recusado = true;
                            message = null;
                        }
                        else if (pairState == PairState.FAILED) {
                            if (computer.runningGameId != 0) {
                                message = act.getResources().getString(R.string.pair_pc_ingame);
                            }
                            else {
                                message = act.getResources().getString(R.string.pair_fail);
                            }
                        }
                        else if (pairState == PairState.ALREADY_IN_PROGRESS) {
                            message = act.getResources().getString(R.string.pair_already_in_progress);
                        }
                        else if (pairState == PairState.PAIRED) {
                            // O passo 3, que o prototipo desenha e o app pulava.
                            //
                            // Ir direto para o catalogo economiza um toque e cobra outra
                            // coisa: o pareamento e a unica vez que a pessoa faz aquilo, e
                            // ela termina sem saber se deu certo -- a tela some e outra
                            // aparece. O passo 3 diz que deu, e a saida dele leva ao mesmo
                            // lugar.
                            message = null;
                            success = true;
                            pareou = true;

                            // Pin this certificate for later HTTPS use
                            managerBinder.getComputer(computer.uuid).serverCert = pm.getPairedCert();

                            // Invalidate reachability information after pairing to force
                            // a refresh before reading pair state again
                            managerBinder.invalidateStateForComputer(computer.uuid);
                        }
                        else {
                            // Should be no other values
                            message = null;
                        }
                    }
                } catch (UnknownHostException e) {
                    message = act.getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = act.getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                    message = e.getMessage();
                }

                Dialog.closeDialogs();
                // A tela do codigo so sai quando NAO ha um passo para ela mostrar.
                //
                // Recusa e sucesso continuam nela, em estados proprios. O que a derruba e o
                // que ela nao sabe dizer: erro de rede, host que sumiu, cancelamento.
                if (!recusado && !pareou) {
                    esconderPareamento();
                }

                final String toastMessage = message;
                final boolean toastSuccess = success;
                final boolean foiRecusado = recusado;
                final boolean foiPareado = pareou;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (foiRecusado && !pairingCancelled.get()) {
                            mostrarPareamento(PonteDoPareamento.passoRecusado(), null);
                            startComputerUpdates();
                            return;
                        }

                        if (foiPareado) {
                            mostrarPareamento(PonteDoPareamento.passoPronto(), null);
                            startComputerUpdates();
                            return;
                        }

                        // Quem cancelou não precisa ser informado de que falhou: o /unpair
                        // faz o host devolver o pedido e pair() volta FAILED, que é o
                        // resultado esperado do cancelamento, não uma notícia ruim.
                        if (toastMessage != null && !pairingCancelled.get()) {
                            VidroToast.show(act, toastMessage, Toast.LENGTH_LONG);
                        }

                        if (toastSuccess) {
                            // Open the app list after a successful pairing attempt
                            doAppList(computer, true, false);
                        }
                        else {
                            // Start polling again if we're still in the foreground
                            startComputerUpdates();
                        }
                    }
                });
            }
        }).start();
    }

    private void doOTPPair(final ComputerDetails computer) {
        Context context = act;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final EditText otpInput = new EditText(context);
        otpInput.setHint("PIN");
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });

        final EditText passphraseInput = new EditText(context);
        passphraseInput.setHint(act.getString(R.string.pair_passphrase_hint));
        passphraseInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(otpInput);
        layout.addView(passphraseInput);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.VidroDialogPlatform);
        dialogBuilder.setTitle(R.string.pcview_menu_pair_pc_otp);
        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton(act.getString(R.string.proceed), null);

        dialogBuilder.setNegativeButton(act.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
        // Sem isto o "Prosseguir" saia no lilas do Material -- ver o funil.
        com.limelight.utils.UiHelper.vestirBotoesDoDialogo(dialog, act.getResources());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pin = otpInput.getText().toString();
            String passphrase = passphraseInput.getText().toString();
            if (pin.length() != 4) {
                VidroToast.show(context, act.getString(R.string.pair_pin_length_msg), Toast.LENGTH_SHORT);
                return;
            }
            if (passphrase.length() < 4 ) {
                VidroToast.show(context, act.getString(R.string.pair_passphrase_length_msg), Toast.LENGTH_SHORT);
                return;
            }
            doPair(computer, pin, passphrase);
            dialog.dismiss(); // Manually dismiss the dialog if the input is valid
        });
    }

    private void doWakeOnLan(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.ONLINE) {
            VidroToast.show(act, act.getResources().getString(R.string.wol_pc_online), Toast.LENGTH_SHORT);
            return;
        }

        if (computer.macAddress == null) {
            VidroToast.show(act, act.getResources().getString(R.string.wol_no_mac), Toast.LENGTH_SHORT);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    WakeOnLanSender.sendWolPacket(computer);
                    message = act.getResources().getString(R.string.wol_waking_msg);
                } catch (IOException e) {
                    message = act.getResources().getString(R.string.wol_fail);
                }

                final String toastMessage = message;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        VidroToast.show(act, toastMessage, Toast.LENGTH_LONG);
                    }
                });
            }
        }).start();
    }

    private void doUnpair(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            VidroToast.show(act, act.getResources().getString(R.string.error_pc_offline), Toast.LENGTH_SHORT);
            return;
        }
        if (managerBinder == null) {
            VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
            return;
        }

        VidroToast.show(act, act.getResources().getString(R.string.unpairing), Toast.LENGTH_SHORT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                try {
                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(act));
                    if (httpConn.getPairState() == PairState.PAIRED) {
                        httpConn.unpair();
                        if (httpConn.getPairState() == PairState.NOT_PAIRED) {
                            message = act.getResources().getString(R.string.unpair_success);
                        }
                        else {
                            message = act.getResources().getString(R.string.unpair_fail);
                        }
                    }
                    else {
                        message = act.getResources().getString(R.string.unpair_error);
                    }
                } catch (UnknownHostException e) {
                    message = act.getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = act.getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    message = e.getMessage();
                    e.printStackTrace();
                }

                final String toastMessage = message;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        VidroToast.show(act, toastMessage, Toast.LENGTH_LONG);
                    }
                });
            }
        }).start();
    }

    private void doAppList(ComputerDetails computer, boolean newlyPaired, boolean showHiddenGames) {
        if (computer.state == ComputerDetails.State.OFFLINE) {
            VidroToast.show(act, act.getResources().getString(R.string.error_pc_offline), Toast.LENGTH_SHORT);
            return;
        }
        if (managerBinder == null) {
            VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
            return;
        }

        // O LUGAR ABRE NO LUGAR.
        //
        // Antes daqui saía um Intent e o Android trocava a janela inteira com o pop de 0,94 a
        // partir do centro. Agora o catálogo é uma rota da mesma árvore, e o nome da máquina
        // que você acabou de tocar é o MESMO elemento que vira o título — ele cresce, não
        // pisca. Ver Compartilhamento.viaja.
        if (catalogo != null) {
            catalogo.destruir();
        }

        catalogo = new CatalogoDoLugar(act, estadoDaCasca.getCatalogo(),
                computer.uuid, computer.name, showHiddenGames,
                () -> estadoDaCasca.voltarParaHome());
        catalogo.marcarRecemPareado(newlyPaired);
        catalogo.ligarAncora(gradeCompose);
        catalogo.ligarBinder(managerBinder);
        catalogo.criar();

        estadoDaCasca.ir(new Rota.Catalogo(computer.uuid));
    }

    public boolean aoEscolherNoMenuDeContexto(MenuItem item) {
        if (catalogo != null && !estadoDaCasca.estaNaHome()
                && catalogo.aoEscolherNoMenuDeContexto(item)) {
            return true;
        }

        if (hostDoMenu == null) {
            return false;
        }
        final ComputerObject computer = new ComputerObject(hostDoMenu);
        switch (item.getItemId()) {
            case PAIR_ID:
                doPair(computer.details, null, null);
                return true;

            case PAIR_ID_OTP:
                doOTPPair(computer.details);
                return true;

            case UNPAIR_ID:
                doUnpair(computer.details);
                return true;

            case WOL_ID:
                doWakeOnLan(computer.details);
                return true;

            case DELETE_ID:
                if (ActivityManager.isUserAMonkey()) {
                    LimeLog.info("Ignoring delete PC request from monkey");
                    return true;
                }
                UiHelper.displayDeletePcConfirmationDialog(act, computer.details, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder == null) {
                            VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
                            return;
                        }
                        removeComputer(computer.details);
                    }
                }, null);
                return true;

            case FULL_APP_LIST_ID:
                doAppList(computer.details, false, true);
                return true;

            case RESUME_ID:
                if (managerBinder == null) {
                    VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
                    return true;
                }

                // Mesmo valor de display virtual que o toque no cartão e que o catálogo:
                // três botões que dizem "retomar" pediam coisas diferentes ao host — aqui
                // false cravado, na faixa do catálogo true cravado, no resto a preferência.
                ServerHelper.doStart(act, new NvApp("app", null, computer.details.runningGameId, false),
                        computer.details, managerBinder,
                        PreferenceConfiguration.readPreferences(act).useVirtualDisplay);
                return true;

            case QUIT_ID:
                if (managerBinder == null) {
                    VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
                    return true;
                }

                // Display a confirmation dialog first
                UiHelper.displayQuitConfirmationDialog(act, new Runnable() {
                    @Override
                    public void run() {
                        ServerHelper.doQuit(act, computer.details,
                                new NvApp("app", null, 0, false), managerBinder, null);
                    }
                }, null);
                return true;

            case VIEW_DETAILS_ID:
                Dialog.displayDialog(act, act.getResources().getString(R.string.title_details), computer.details.toString(), false);
                return true;

            case TEST_NETWORK_ID:
                ServerHelper.doNetworkTest(act);
                return true;

            case GAMESTREAM_EOL_ID:
                HelpLauncher.launchGameStreamEolFaq(act);
                return true;

            case OPEN_MANAGEMENT_PAGE_ID:
                String managementUrl = computer.guessManagementUrl();
                if (managementUrl == null) {
                    VidroToast.show(act, act.getResources().getString(R.string.pcview_error_no_management_url), Toast.LENGTH_LONG);
                } else {
                    HelpLauncher.launchUrl(act, managementUrl);
                }

            default:
                return false;
        }
    }

    private void removeComputer(ComputerDetails details) {
        managerBinder.removeComputer(details);

        new DiskAssetLoader(act).deleteAssetsForComputer(details.uuid);

        // Delete hidden games preference value
        act.getSharedPreferences(CatalogoDoLugar.HIDDEN_APPS_PREF_FILENAME, Context.MODE_PRIVATE)
                .edit()
                .remove(details.uuid)
                .apply();

        // Disable or delete shortcuts referencing this PC
        shortcutHelper.disableComputerShortcut(details,
                act.getResources().getString(R.string.scut_deleted_pc));

        estadoDaHome.remover(details.uuid);

        Historico.esquecerHost(act, details.uuid);
    }

    /**
     * Uma resposta da sondagem.
     *
     * Aqui estava o coracao do Q36: a sondagem responde a cada 1,5 s POR HOST e quase sempre
     * repete o resultado anterior, e como ela entrega SEMPRE a mesma instancia de
     * ComputerDetails mutada no lugar, comparar referencia nunca acusava mudanca. A home se
     * redesenhava em ciclo sem nada ter mudado, e foi preciso um PcCellSnapshot copiando
     * campo a campo, mais um HashMap paralelo, para descobrir se valia redesenhar.
     *
     * Nada disso existe mais. EstadoDaHome monta um HostUi imutavel, e se ele for igual ao
     * anterior a lista nova e estruturalmente igual a antiga -- a recomposicao nao toca em
     * nada, sem ninguem precisar decidir isso.
     */
    private void updateComputer(ComputerDetails details) {
        estadoDaHome.aoResponder(details);
    }

    /**
     * O toque num lugar.
     *
     * Mesma decisao de antes, na mesma funcao compartilhada: o que a linha DIZ e para onde o
     * toque LEVA nao podem discordar -- e antes discordavam, com o cartao anunciando a sessao
     * e o toque abrindo o catalogo.
     */
    private kotlin.Unit aoTocarHost(HostUi host) {
        ComputerDetails details = estadoDaHome.detalhes(host.getUuid());
        if (details == null) {
            return kotlin.Unit.INSTANCE;
        }

        switch (ResumeDecision.forHost(
                details.state == ComputerDetails.State.ONLINE,
                details.pairState == PairState.PAIRED,
                details.runningGameId)) {
            case MENU:
                // Host dormindo: o toque ACORDA, que e o que a linha esta dizendo. Antes
                // ele abria um menu de catorze itens onde acordar era o quarto.
                if (host.getEstado() == EstadoHost.DORMINDO && !host.getAcordando()) {
                    aoAcordarDaFolha(host);
                }
                else {
                    aoSegurarHost(host);
                }
                break;

            case PAIR:
                doPair(details, null, null);
                break;

            case RESUME:
                if (managerBinder == null) {
                    VidroToast.show(act, act.getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG);
                    break;
                }
                ServerHelper.doStart(act, new NvApp("app", null, details.runningGameId, false),
                        details, managerBinder,
                        PreferenceConfiguration.readPreferences(act).useVirtualDisplay);
                break;

            case CATALOG:
                doAppList(details, false, false);
                break;
        }
        return kotlin.Unit.INSTANCE;
    }

    /** O toque longo abre a folha Vidro -- o PE03. */
    private kotlin.Unit aoSegurarHost(HostUi host) {
        estadoDaHome.abrirFolha(host);
        return kotlin.Unit.INSTANCE;
    }

    /**
     * O menu antigo, com tudo que ainda nao tem lugar na folha.
     *
     * Ele continua existindo de proposito: podar catorze itens e uma decisao item a item, e
     * apagar o menu antes dessa decisao seria perder capacidade sem argumento escrito. A
     * folha oferece o que serve agora; o resto continua a um toque de distancia.
     */
    private kotlin.Unit aoAbrirMenuAntigo(HostUi host) {
        hostDoMenu = estadoDaHome.detalhes(host.getUuid());
        if (hostDoMenu != null && gradeCompose != null) {
            act.openContextMenu(gradeCompose);
        }
        return kotlin.Unit.INSTANCE;
    }

    private kotlin.Unit aoRetomarDaFolha(HostUi host) {
        ComputerDetails d = estadoDaHome.detalhes(host.getUuid());
        if (d != null && managerBinder != null) {
            ServerHelper.doStart(act, new NvApp("app", null, d.runningGameId, false), d,
                    managerBinder, PreferenceConfiguration.readPreferences(act).useVirtualDisplay);
        }
        return kotlin.Unit.INSTANCE;
    }

    private kotlin.Unit aoEncerrarDaFolha(HostUi host) {
        ComputerDetails d = estadoDaHome.detalhes(host.getUuid());
        if (d != null && managerBinder != null) {
            ServerHelper.doQuit(act, d, new NvApp("app", null, d.runningGameId, false),
                    managerBinder, null);
        }
        return kotlin.Unit.INSTANCE;
    }

    private kotlin.Unit aoParearDaFolha(HostUi host) {
        ComputerDetails d = estadoDaHome.detalhes(host.getUuid());
        if (d != null) {
            doPair(d, null, null);
        }
        return kotlin.Unit.INSTANCE;
    }

    /**
     * O P06: acordar pelo proprio cartao que diz que o host dorme.
     *
     * Antes isto era um item de menu de contexto que ninguem achava, e o desfecho era uma
     * torrada de dois segundos -- depois dela restava encarar o cartao por uns 20 s sem
     * saber se o pacote pegou. Agora a linha conta a espera, e ela some sozinha quando a
     * sondagem de 1,5 s devolver o host.
     */
    private kotlin.Unit aoAcordarDaFolha(HostUi host) {
        ComputerDetails d = estadoDaHome.detalhes(host.getUuid());
        if (d != null) {
            estadoDaHome.marcarAcordando(host.getUuid());
            doWakeOnLan(d);
        }
        return kotlin.Unit.INSTANCE;
    }

    private kotlin.Unit aoRemoverDaFolha(HostUi host) {
        ComputerDetails d = estadoDaHome.detalhes(host.getUuid());
        if (d != null) {
            removeComputer(d);
        }
        return kotlin.Unit.INSTANCE;
    }

    public static class ComputerObject {
        public ComputerDetails details;

        public ComputerObject(ComputerDetails details) {
            if (details == null) {
                throw new IllegalArgumentException("details must not be null");
            }
            this.details = details;
        }

        @Override
        public String toString() {
            return details.name;
        }
        public String guessManagementUrl() {
            if (details.activeAddress == null) return null;
            return "https://" + details.activeAddress.address + ":" + (details.guessExternalPort() + 1);
        }
    }
}
