package com.limelight;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.limelight.binding.PlatformBinding;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.grid.AppCarouselAdapter;
import com.limelight.grid.AppGridAdapter;
import com.limelight.grid.PcGridAdapter;
import com.limelight.grid.assets.CachedAppAssetLoader;
import com.limelight.grid.assets.DiskAssetLoader;
import com.limelight.grid.assets.MemoryAssetLoader;
import com.limelight.grid.assets.ScaledBitmap;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.http.PairingManager.PairState;
import com.limelight.nvstream.wol.WakeOnLanSender;
import com.limelight.preferences.AddComputerManually;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.preferences.StreamSettingsBasico;
import com.limelight.profiles.ProfilesManager;
import com.limelight.ui.AdapterFragment;
import com.limelight.ui.MoqueDeApps;
import com.limelight.ui.VidroLightView;
import com.limelight.ui.AdapterFragmentCallbacks;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.UiHelper;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PcView extends AppCompatActivity implements AdapterFragmentCallbacks {
    // Só recebe setVisibility. Prender um tipo de container aqui obrigaria o layout
    // a manter um que o desenho não pede mais.
    private View noPcFoundLayout;
    private PcGridAdapter pcGridAdapter;
    private ShortcutHelper shortcutHelper;
    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private boolean freezeUpdates, runningPolling, inForeground, completeOnCreateCalled;
    private ComputerDetails.AddressTuple pendingPairingAddress;
    private String pendingPairingPin, pendingPairingPassphrase;

    // ================= Início (tablet) — Plano B / F3, B-CONSOLE.md =================
    // Só usados quando isConsoleInicio() é verdadeiro (layout-sw600dp/activity_pc_view
    // tem o cabeçalho de chip que o layout de celular não tem). O PC "atual" desta
    // seção é independente da lista de PcGridAdapter que a Fragment mostraria — aqui a
    // navegação é o chip + diálogo, não uma lista visível.
    private AppGridAdapter tabletAppGridAdapter;
    private AppCarouselAdapter tabletCarouselAdapter;
    private ComputerManagerService.ApplistPoller tabletAppPoller;
    private ComputerDetails tabletCurrentComputerDetails;
    private String tabletCurrentUuid;
    private String tabletLastRawApplist;
    private AppView.AppObject tabletFocusedApp;
    private AppView.AppObject tabletRunningApp;
    private AlertDialog pcDialogAtual;

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

                    // Start updates
                    startComputerUpdates();

                    // A Início do tablet pode ter tentado montar a seção de apps do PC
                    // atual antes do binder ficar pronto (caminho de host de verdade,
                    // não o moque) — agora que managerBinder existe, tenta de novo.
                    if (isConsoleInicio()) {
                        runOnUiThread(PcView.this::refreshConsoleInicio);
                    }

                    // Force a keypair to be generated early to avoid discovery delays
                    new AndroidCryptoProvider(PcView.this).getClientCertificate();
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

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

    /**
     * Dois PCs sem rede nenhuma, para {@code tools/fotos.mjs} fotografar esta tela sem
     * um Vibeshine de verdade no ar. Só em compilação de depuração — ver
     * {@link #emModoDeCasca()} — e sem o extra a PcView não muda em nada.
     *
     * <pre>adb shell am start -n com.limelight.noirdebug/com.limelight.PcView --ez MoqueDeCasca true</pre>
     */
    private static final String EXTRA_MOQUE_CASCA = "MoqueDeCasca";

    private boolean emModoDeCasca() {
        return BuildConfig.DEBUG && getIntent().getBooleanExtra(EXTRA_MOQUE_CASCA, false);
    }

    private void initializeViews() {
        setContentView(R.layout.activity_pc_view);

        UiHelper.notifyNewRootView(this);

        // Allow floating expanded PiP overlays while browsing PCs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }

        // Set default preferences if we've never been run
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Set the correct layout for the PC grid
        pcGridAdapter.updateLayoutWithPreferences(this, PreferenceConfiguration.readPreferences(this));

        // Setup the list view.
        //
        // settingsButton é View, não ImageButton: ele saiu do canto superior e virou
        // metade do par do rodapé, "Cenas · Ajustes", que no protótipo é texto e não
        // ícone. Os outros dois continuam sendo ícone no topo.
        View settingsButton = findViewById(R.id.settingsButton);
        TextView profilesButton = findViewById(R.id.profilesButton);

        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Plano B / F4: a porta de entrada de Ajustes agora é a tela básica —
                // ela é quem sabe abrir a árvore inteira atrás do card "Avançado".
                Intent settingsIntent = new Intent(PcView.this, StreamSettingsBasico.class);
                // Só a Início do tablet tem noção de "PC atual" (isConsoleInicio());
                // no celular a tela de Computadores não elege um PC, então o cabeçalho
                // de Ajustes fica sem o trecho do PC — "o PC atual, se houver" (B-CONSOLE.md §F4).
                if (isConsoleInicio() && tabletCurrentComputerDetails != null
                        && tabletCurrentComputerDetails.activeAddress != null) {
                    settingsIntent.putExtra(StreamSettingsBasico.EXTRA_PC_NAME, tabletCurrentComputerDetails.name);
                    settingsIntent.putExtra(StreamSettingsBasico.EXTRA_PC_ADDRESS, tabletCurrentComputerDetails.activeAddress.address);
                }
                startActivity(settingsIntent);
            }
        });
        View addPlace = findViewById(R.id.vidroAddPlace);
        if (addPlace != null) {
            // console_borda_tracejada.xml (dashPathEffect) só desenha tracejado de
            // verdade em LAYER_TYPE_SOFTWARE — em hardware accel ele normalmente cai
            // pra traço sólido. O comentário do drawable já prometia isto; faltava
            // a chamada em si.
            addPlace.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            addPlace.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PcView.this, AddComputerManually.class));
                }
            });
        }
        profilesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PcView.this, ProfilesActivity.class));
            }
        });

        // Início (tablet) — Plano B / F3. O cabeçalho de chip só existe no
        // layout-sw600dp; nada aqui roda no celular.
        if (isConsoleInicio()) {
            findViewById(R.id.consolePcChipAtual).setOnClickListener(v -> mostrarDialogoDePcs());

            findViewById(R.id.consoleAppsOpenButton).setOnClickListener(v -> abrirAppFocado());
            findViewById(R.id.consoleContinueResume).setOnClickListener(v -> retomarContinuar());
            findViewById(R.id.consoleContinueQuit).setOnClickListener(v -> encerrarContinuar());

            RecyclerView carousel = findViewById(R.id.consoleAppsCarousel);
            carousel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }

        // Amazon review didn't like the help button because the wiki was not entirely
        // navigable via the Fire TV remote (though the relevant parts were). Let's hide
        // it on Fire TV.

        getFragmentManager().beginTransaction()
            .replace(R.id.pcFragmentContainer, new AdapterFragment())
            .commitAllowingStateLoss();

        noPcFoundLayout = findViewById(R.id.no_pc_found_layout);
        if (pcGridAdapter.getCount() == 0) {
            noPcFoundLayout.setVisibility(View.VISIBLE);
        }
        else {
            noPcFoundLayout.setVisibility(View.INVISIBLE);
        }
        pcGridAdapter.notifyDataSetChanged();

        refreshConsoleInicio();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;

        // Create a GLSurfaceView to fetch GLRenderer unless we have
        // a cached result already.
        final GlPreferences glPrefs = GlPreferences.readPreferences(this);
        if (!glPrefs.savedFingerprint.equals(Build.FINGERPRINT) || glPrefs.glRenderer.isEmpty()) {
            GLSurfaceView surfaceView = new GLSurfaceView(this);
            surfaceView.setRenderer(new GLSurfaceView.Renderer() {
                @Override
                public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                    // Save the GLRenderer string so we don't need to do this next time
                    glPrefs.glRenderer = gl10.glGetString(GL10.GL_RENDERER);
                    glPrefs.savedFingerprint = Build.FINGERPRINT;
                    glPrefs.writePreferences();

                    LimeLog.info("Fetched GL Renderer: " + glPrefs.glRenderer);

                    runOnUiThread(new Runnable() {
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
            setContentView(surfaceView);
        }
        else {
            LimeLog.info("Cached GL Renderer: " + glPrefs.glRenderer);
            completeOnCreate();
        }

        Intent intent = getIntent();

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

        shortcutHelper = new ShortcutHelper(this);

        UiHelper.setLocale(this);

        // Bind to the computer manager service
        bindService(new Intent(PcView.this, ComputerManagerService.class), serviceConnection,
                Service.BIND_AUTO_CREATE);

        pcGridAdapter = new PcGridAdapter(this, PreferenceConfiguration.readPreferences(this));

        if (emModoDeCasca()) {
            popularComputadoresDeMentira();
        }

        initializeViews();
    }

    /**
     * ARTEMIS on-line e emparelhada, NOTEBOOK fora do ar — os dois PCs que o extra
     * {@code MoqueDeCasca} promete. Preenche o adapter direto, sem passar pelo
     * {@link ComputerManagerService}: não há host de verdade para sondar, e o serviço
     * real continua ligado (o bind acima) pelo que mais nesta tela depende dele.
     */
    private void popularComputadoresDeMentira() {
        ComputerDetails artemis = new ComputerDetails();
        artemis.uuid = "moque-artemis";
        artemis.name = "ARTEMIS";
        artemis.state = ComputerDetails.State.ONLINE;
        artemis.pairState = PairState.PAIRED;
        artemis.activeAddress = new ComputerDetails.AddressTuple("192.168.0.10", NvHTTP.DEFAULT_HTTP_PORT);

        ComputerDetails notebook = new ComputerDetails();
        notebook.uuid = "moque-notebook";
        notebook.name = "NOTEBOOK";
        notebook.state = ComputerDetails.State.OFFLINE;
        notebook.pairState = PairState.PAIRED;
        // Um MAC presente é o sinal de "dá pra acordar" (Wake-on-LAN) que um PC fora
        // do ar de verdade também carregaria.
        notebook.macAddress = "02:00:00:00:00:01";

        pcGridAdapter.addComputer(new ComputerObject(artemis));
        pcGridAdapter.addComputer(new ComputerObject(notebook));
    }

    private void startComputerUpdates() {
        if (emModoDeCasca()) {
            // Os dois PCs de mentira já estão no adapter; não há o que sondar.
            return;
        }

        // Only allow polling to start if we're bound to CMS, polling is not already running,
        // and our activity is in the foreground.
        if (managerBinder != null && !runningPolling && inForeground) {
            freezeUpdates = false;
            managerBinder.startPolling(new ComputerManagerListener() {
                @Override
                public void notifyComputerUpdated(final ComputerDetails details) {
                    if (!freezeUpdates) {
                        PcView.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateComputer(details);
                                atualizarAppsDoTabletSeForOAtual(details);
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
                                PcView.this.runOnUiThread(() -> {
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

    private void refreshProfileButton() {
        TextView profilesButton = findViewById(R.id.profilesButton);
        // User report Samsung and Xiaomi devices have this problem
        // Why just these two brands have the most problems?
        if (profilesButton == null) {
            return;
        }
        // O rodapé diz "Cenas" quando nenhuma está ativa e o nome da cena quando há uma.
        // O botão não some nem encolhe: no protótipo o rodapé é um par fixo, e um par que
        // muda de largura sozinho faz a tela inteira dançar.
        String activeProfileName = ProfilesManager.getInstance().getActiveName();
        profilesButton.setText(activeProfileName.isEmpty()
                ? getString(R.string.vidro_scenes) : activeProfileName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        pararAppsDoTablet();

        if (managerBinder != null) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(this);

        refreshProfileButton();

        inForeground = true;
        startComputerUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        inForeground = false;
        stopComputerUpdates(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Dialog.closeDialogs();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        stopComputerUpdates(false);

        // Call superclass
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(info.position);

        // Add a header with PC status details
        menu.clearHeader();
        String headerTitle = computer.details.name + " - ";
        switch (computer.details.state)
        {
            case ONLINE:
                headerTitle += getResources().getString(R.string.pcview_menu_header_online);
                break;
            case OFFLINE:
                menu.setHeaderIcon(R.drawable.ic_pc_offline);
                headerTitle += getResources().getString(R.string.pcview_menu_header_offline);
                break;
            case UNKNOWN:
                headerTitle += getResources().getString(R.string.pcview_menu_header_unknown);
                break;
        }

        menu.setHeaderTitle(headerTitle);

        // Inflate the context menu
        if (computer.details.state == ComputerDetails.State.OFFLINE ||
            computer.details.state == ComputerDetails.State.UNKNOWN) {
            menu.add(Menu.NONE, WOL_ID, 1, getResources().getString(R.string.pcview_menu_send_wol));
            menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 2, getResources().getString(R.string.pcview_menu_eol));
        }
        else if (computer.details.pairState != PairState.PAIRED) {
            menu.add(Menu.NONE, PAIR_ID_OTP, 1, getResources().getString(R.string.pcview_menu_pair_pc_otp));
            menu.add(Menu.NONE, PAIR_ID, 2, getResources().getString(R.string.pcview_menu_pair_pc));
            if (computer.details.nvidiaServer) {
                menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 3, getResources().getString(R.string.pcview_menu_eol));
            } else {
                menu.add(Menu.NONE, OPEN_MANAGEMENT_PAGE_ID, 3, getResources().getString(R.string.pcview_menu_open_management_page));
            }
        }
        else {
            if (computer.details.runningGameId != 0) {
                menu.add(Menu.NONE, RESUME_ID, 1, getResources().getString(R.string.applist_menu_resume));
                menu.add(Menu.NONE, QUIT_ID, 2, getResources().getString(R.string.applist_menu_quit));
            }

            if (computer.details.nvidiaServer) {
                menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 3, getResources().getString(R.string.pcview_menu_eol));
            } else {
                menu.add(Menu.NONE, OPEN_MANAGEMENT_PAGE_ID, 3, getResources().getString(R.string.pcview_menu_open_management_page));
            }

            menu.add(Menu.NONE, FULL_APP_LIST_ID, 4, getResources().getString(R.string.pcview_menu_app_list));
        }

        menu.add(Menu.NONE, TEST_NETWORK_ID, 5, getResources().getString(R.string.pcview_menu_test_network));
        menu.add(Menu.NONE, DELETE_ID, 6, getResources().getString(R.string.pcview_menu_delete_pc));
        menu.add(Menu.NONE, VIEW_DETAILS_ID, 7,  getResources().getString(R.string.pcview_menu_details));
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        // For some reason, this gets called again _after_ onPause() is called on this activity.
        // startComputerUpdates() manages this and won't actual start polling until the activity
        // returns to the foreground.
        startComputerUpdates();
    }


    /**
     * Mostra os quatro dígitos que o host vai pedir na tela do PC.
     *
     * Chamada de uma thread de rede — o {@code pm.pair()} logo abaixo bloqueia até o PC
     * confirmar ou o código expirar —, então tudo aqui passa pelo runOnUiThread.
     *
     * Sem {@code pinStr} é o fluxo de senha do Apollo, que não mostra código nenhum: aí a
     * tela existe só para dizer que está esperando.
     */
    private void showVidroPairing(final String pinStr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View pairing = findViewById(R.id.vidroPairing);
                if (pairing == null) {
                    return;
                }

                int[] digitIds = {
                        R.id.vidroPairDigit0, R.id.vidroPairDigit1,
                        R.id.vidroPairDigit2, R.id.vidroPairDigit3
                };

                boolean hasCode = pinStr != null && pinStr.length() >= digitIds.length;
                for (int i = 0; i < digitIds.length; i++) {
                    TextView digit = findViewById(digitIds[i]);
                    if (digit != null) {
                        digit.setText(hasCode ? String.valueOf(pinStr.charAt(i)) : "");
                        digit.setVisibility(hasCode ? View.VISIBLE : View.GONE);
                    }
                }

                TextView step = findViewById(R.id.vidroPairingStep);
                if (step != null) {
                    step.setVisibility(hasCode ? View.VISIBLE : View.GONE);
                }

                VidroLightView light = findViewById(R.id.vidroPairingLight);
                if (light != null) {
                    light.setIntensity(VidroLightView.INTENSITY_AWAKE, true);
                }

                View cancel = findViewById(R.id.vidroPairCancel);
                if (cancel != null) {
                    cancel.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Só tira a tela da frente. O pedido continua no ar até o host
                            // responder ou o código virar — não há caminho para abortar um
                            // pareamento já enviado, e fingir que há seria pior.
                            hideVidroPairing();
                        }
                    });
                }

                pairing.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideVidroPairing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View pairing = findViewById(R.id.vidroPairing);
                if (pairing != null) {
                    pairing.setVisibility(View.GONE);
                }
            }
        });
    }

    private void doPair(final ComputerDetails computer, String otp, String passphrase) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.pair_pc_offline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(PcView.this, getResources().getString(R.string.pairing), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                boolean success = false;
                try {
                    // Stop updates and wait while pairing
                    stopComputerUpdates(true);

                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(PcView.this));
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
                            showVidroPairing(pinStr);
                        } else {
                            showVidroPairing(null);
                        }

                        PairingManager pm = httpConn.getPairingManager();

                        PairState pairState = pm.pair(httpConn.getServerInfo(true), pinStr, passphrase);
                        if (pairState == PairState.PIN_WRONG) {
                            message = getResources().getString(R.string.pair_incorrect_pin);
                        }
                        else if (pairState == PairState.FAILED) {
                            if (computer.runningGameId != 0) {
                                message = getResources().getString(R.string.pair_pc_ingame);
                            }
                            else {
                                message = getResources().getString(R.string.pair_fail);
                            }
                        }
                        else if (pairState == PairState.ALREADY_IN_PROGRESS) {
                            message = getResources().getString(R.string.pair_already_in_progress);
                        }
                        else if (pairState == PairState.PAIRED) {
                            // Just navigate to the app view without displaying a toast
                            message = null;
                            success = true;

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
                    message = getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                    message = e.getMessage();
                }

                Dialog.closeDialogs();
                // Terminou de um jeito ou de outro: a tela do código sai. Ela some
                // aqui e não em cada ramo acima porque todo caminho passa por este
                // ponto, inclusive as exceções de rede.
                hideVidroPairing();

                final String toastMessage = message;
                final boolean toastSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toastMessage != null) {
                            Toast.makeText(PcView.this, toastMessage, Toast.LENGTH_LONG).show();
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
        Context context = PcView.this;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final EditText otpInput = new EditText(context);
        otpInput.setHint("PIN");
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });

        final EditText passphraseInput = new EditText(context);
        passphraseInput.setHint(getString(R.string.pair_passphrase_hint));
        passphraseInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(otpInput);
        layout.addView(passphraseInput);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(R.string.pcview_menu_pair_pc_otp);
        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton(getString(R.string.proceed), null);

        dialogBuilder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pin = otpInput.getText().toString();
            String passphrase = passphraseInput.getText().toString();
            if (pin.length() != 4) {
                Toast.makeText(context, getString(R.string.pair_pin_length_msg), Toast.LENGTH_SHORT).show();
                return;
            }
            if (passphrase.length() < 4 ) {
                Toast.makeText(context, getString(R.string.pair_passphrase_length_msg), Toast.LENGTH_SHORT).show();
                return;
            }
            doPair(computer, pin, passphrase);
            dialog.dismiss(); // Manually dismiss the dialog if the input is valid
        });
    }

    private void doWakeOnLan(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.ONLINE) {
            Toast.makeText(PcView.this, getResources().getString(R.string.wol_pc_online), Toast.LENGTH_SHORT).show();
            return;
        }

        if (computer.macAddress == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.wol_no_mac), Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    WakeOnLanSender.sendWolPacket(computer);
                    message = getResources().getString(R.string.wol_waking_msg);
                } catch (IOException e) {
                    message = getResources().getString(R.string.wol_fail);
                }

                final String toastMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PcView.this, toastMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void doUnpair(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(PcView.this, getResources().getString(R.string.unpairing), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                try {
                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(PcView.this));
                    if (httpConn.getPairState() == PairState.PAIRED) {
                        httpConn.unpair();
                        if (httpConn.getPairState() == PairState.NOT_PAIRED) {
                            message = getResources().getString(R.string.unpair_success);
                        }
                        else {
                            message = getResources().getString(R.string.unpair_fail);
                        }
                    }
                    else {
                        message = getResources().getString(R.string.unpair_error);
                    }
                } catch (UnknownHostException e) {
                    message = getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    message = e.getMessage();
                    e.printStackTrace();
                }

                final String toastMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PcView.this, toastMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void doAppList(ComputerDetails computer, boolean newlyPaired, boolean showHiddenGames) {
        if (computer.state == ComputerDetails.State.OFFLINE) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(this, AppView.class);
        i.putExtra(AppView.NAME_EXTRA, computer.name);
        i.putExtra(AppView.UUID_EXTRA, computer.uuid);
        i.putExtra(AppView.NEW_PAIR_EXTRA, newlyPaired);
        i.putExtra(AppView.SHOW_HIDDEN_APPS_EXTRA, showHiddenGames);
        startActivity(i);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(info.position);
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
                UiHelper.displayDeletePcConfirmationDialog(this, computer.details, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder == null) {
                            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
                    return true;
                }

                ServerHelper.doStart(this, new NvApp("app", null, computer.details.runningGameId, false), computer.details, managerBinder, false);
                return true;

            case QUIT_ID:
                if (managerBinder == null) {
                    Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
                    return true;
                }

                // Display a confirmation dialog first
                UiHelper.displayQuitConfirmationDialog(this, new Runnable() {
                    @Override
                    public void run() {
                        ServerHelper.doQuit(PcView.this, computer.details,
                                new NvApp("app", null, 0, false), managerBinder, null);
                    }
                }, null);
                return true;

            case VIEW_DETAILS_ID:
                Dialog.displayDialog(PcView.this, getResources().getString(R.string.title_details), computer.details.toString(), false);
                return true;

            case TEST_NETWORK_ID:
                ServerHelper.doNetworkTest(PcView.this);
                return true;

            case GAMESTREAM_EOL_ID:
                HelpLauncher.launchGameStreamEolFaq(PcView.this);
                return true;

            case OPEN_MANAGEMENT_PAGE_ID:
                String managementUrl = computer.guessManagementUrl();
                if (managementUrl == null) {
                    Toast.makeText(PcView.this, getResources().getString(R.string.pcview_error_no_management_url), Toast.LENGTH_LONG).show();
                } else {
                    HelpLauncher.launchUrl(PcView.this, managementUrl);
                }

            default:
                return super.onContextItemSelected(item);
        }
    }

    private void removeComputer(ComputerDetails details) {
        managerBinder.removeComputer(details);

        new DiskAssetLoader(this).deleteAssetsForComputer(details.uuid);

        // Delete hidden games preference value
        getSharedPreferences(AppView.HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                .edit()
                .remove(details.uuid)
                .apply();

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(i);

            if (details.equals(computer.details)) {
                // Disable or delete shortcuts referencing this PC
                shortcutHelper.disableComputerShortcut(details,
                        getResources().getString(R.string.scut_deleted_pc));

                pcGridAdapter.removeComputer(computer);
                pcGridAdapter.notifyDataSetChanged();

                if (pcGridAdapter.getCount() == 0) {
                    // Show the "Discovery in progress" view
                    noPcFoundLayout.setVisibility(View.VISIBLE);
                }

                break;
            }
        }

        refreshConsoleInicio();
    }

    private void updateComputer(ComputerDetails details) {
        ComputerObject existingEntry = null;

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(i);

            // Check if this is the same computer
            if (details.uuid.equals(computer.details.uuid)) {
                existingEntry = computer;
                break;
            }
        }

        if (existingEntry != null) {
            // Replace the information in the existing entry
            existingEntry.details = details;
        }
        else {
            // Add a new entry
            pcGridAdapter.addComputer(new ComputerObject(details));

            // Remove the "Discovery in progress" view
            noPcFoundLayout.setVisibility(View.INVISIBLE);
        }

        // Notify the view that the data has changed
        pcGridAdapter.notifyDataSetChanged();

        refreshConsoleInicio();
    }

    // ================= Início (tablet) — Plano B / F3, B-CONSOLE.md =================
    //
    // A PcView do tablet (layout-sw600dp/activity_pc_view.xml) é a tela "Início"
    // inteira: cabeçalho com o chip do PC atual, "Continuar" (app em execução) e
    // "Apps" (fileira de capas). O que segue só roda quando esse layout está no ar —
    // isConsoleInicio() detecta isso pela presença do id consoleHeader, que o layout
    // de celular não tem.

    private boolean isConsoleInicio() {
        return findViewById(R.id.consoleHeader) != null;
    }

    /**
     * Mesmo extra que {@link AppView#MOQUE_APPS_EXTRA} — quando os dois moques
     * (casca + apps) estão ligados juntos, a Início do tablet monta a seção "Apps" e
     * "Continuar" com {@link MoqueDeApps}, sem tentar rede nenhuma.
     */
    private boolean emModoDeApps() {
        return BuildConfig.DEBUG && getIntent().getBooleanExtra(AppView.MOQUE_APPS_EXTRA, false);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    /**
     * Recalcula tudo que a Início mostra a partir do estado atual de
     * {@link #pcGridAdapter}: cabeçalho vazio, chip do PC atual + chips esmaecidos, e
     * (re)carrega a seção de apps só quando o PC atual muda de verdade — trocar o
     * app em execução no MESMO PC não deveria reconstruir o adapter inteiro.
     */
    private void refreshConsoleInicio() {
        if (!isConsoleInicio()) {
            return;
        }

        View header = findViewById(R.id.consoleHeader);
        View empty = findViewById(R.id.no_pc_found_layout);
        View continueSection = findViewById(R.id.consoleContinueSection);
        View appsSection = findViewById(R.id.consoleAppsSection);

        if (pcGridAdapter.getCount() == 0) {
            header.setVisibility(View.GONE);
            continueSection.setVisibility(View.GONE);
            appsSection.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            tabletCurrentUuid = null;
            return;
        }

        header.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);

        ComputerObject atual = escolherComputadorAtual();
        boolean trocouComputador = atual != null
                && !atual.details.uuid.equals(tabletCurrentUuid);

        if (atual != null) {
            atualizarCabecalhoDoChip(atual);
            if (trocouComputador) {
                tabletCurrentUuid = atual.details.uuid;
                carregarAppsDoComputadorAtual(atual);
            }
        }
    }

    /**
     * Mantém o PC selecionado se ele ainda for on-line + emparelhado; senão, prefere
     * o primeiro on-line + emparelhado; senão, qualquer emparelhado; senão, o
     * primeiro da lista (mesmo fora do ar — a Início ainda mostra o chip dele).
     */
    private ComputerObject escolherComputadorAtual() {
        ComputerObject mantido = null;
        ComputerObject primeiroOnlinePareado = null;
        ComputerObject primeiroPareado = null;

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject c = (ComputerObject) pcGridAdapter.getItem(i);
            boolean online = c.details.state == ComputerDetails.State.ONLINE;
            boolean paired = c.details.pairState == PairState.PAIRED;

            if (c.details.uuid.equals(tabletCurrentUuid)) {
                mantido = c;
            }
            if (paired && primeiroPareado == null) {
                primeiroPareado = c;
            }
            if (paired && online && primeiroOnlinePareado == null) {
                primeiroOnlinePareado = c;
            }
        }

        if (mantido != null && mantido.details.pairState == PairState.PAIRED
                && mantido.details.state == ComputerDetails.State.ONLINE) {
            return mantido;
        }
        if (primeiroOnlinePareado != null) {
            return primeiroOnlinePareado;
        }
        if (mantido != null) {
            return mantido;
        }
        if (primeiroPareado != null) {
            return primeiroPareado;
        }
        return (ComputerObject) pcGridAdapter.getItem(0);
    }

    private void atualizarCabecalhoDoChip(ComputerObject atual) {
        ImageView dot = findViewById(R.id.consolePcChipDot);
        TextView texto = findViewById(R.id.consolePcChipTexto);

        boolean online = atual.details.state == ComputerDetails.State.ONLINE;
        boolean paired = atual.details.pairState == PairState.PAIRED;

        String estado;
        if (online && paired) {
            dot.setImageResource(R.drawable.vidro_dot_on);
            estado = getString(R.string.console_estado_online);
        } else if (online) {
            dot.setImageResource(R.drawable.console_dot_neutro);
            estado = getString(R.string.console_estado_emparelhar);
        } else {
            dot.setImageResource(R.drawable.console_dot_neutro);
            estado = getString(R.string.console_estado_offline);
        }
        texto.setText(getString(R.string.console_nome_e_estado, atual.details.name, estado));

        LinearLayout chipsOffline = findViewById(R.id.consolePcChipsOffline);
        chipsOffline.removeAllViews();
        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject c = (ComputerObject) pcGridAdapter.getItem(i);
            if (c.details.uuid.equals(atual.details.uuid)) {
                continue;
            }
            chipsOffline.addView(criarChipEsmaecido(c));
        }
    }

    /**
     * Chip discreto dos PCs que não são o atual (NOTA.md §4: sem artboard de
     * seletor no tablet, o chip aberto já é o diálogo — estes só avisam que existe
     * mais gente na lista). Construído em código porque são poucos e variam de
     * quantidade; um layout próprio por chip seria mais máquina que o problema pede.
     */
    private View criarChipEsmaecido(final ComputerObject c) {
        TextView chip = new TextView(this);
        androidx.core.widget.TextViewCompat.setTextAppearance(chip, R.style.ConsoleText_Rotulo2);
        chip.setText(c.details.name);
        chip.setBackgroundResource(R.drawable.console_chip_bg);
        chip.setAlpha(0.55f);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setClickable(true);
        chip.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
        lp.setMarginStart(dp(8));
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> mostrarDialogoDePcs());
        return chip;
    }

    /**
     * O "diálogo simples" que a F3 pede: lista os PCs conhecidos com estado, mais
     * "Adicionar PC". Toque num PC on-line+emparelhado troca o atual da Início;
     * num PC on-line não emparelhado, abre o pareamento de sempre (doPair); num PC
     * fora do ar, só fecha o diálogo — a ação de acordar/remover continua no menu de
     * contexto da lista de celular, não duplicada aqui.
     */
    private void mostrarDialogoDePcs() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            final ComputerObject c = (ComputerObject) pcGridAdapter.getItem(i);
            boolean online = c.details.state == ComputerDetails.State.ONLINE;
            boolean paired = c.details.pairState == PairState.PAIRED;

            View row = inflater.inflate(R.layout.console_pc_dialog_row, container, false);
            ((ImageView) row.findViewById(R.id.consoleDialogDot)).setImageResource(
                    online && paired ? R.drawable.vidro_dot_on : R.drawable.console_dot_neutro);
            ((TextView) row.findViewById(R.id.consoleDialogNome)).setText(c.details.name);
            ((TextView) row.findViewById(R.id.consoleDialogEstado)).setText(
                    online ? (paired ? R.string.console_estado_online : R.string.console_estado_emparelhar)
                            : R.string.console_estado_offline);

            row.setOnClickListener(v -> {
                if (online && paired) {
                    selecionarComputadorAtual(c);
                } else if (online) {
                    doPair(c.details, null, null);
                }
                if (pcDialogAtual != null) {
                    pcDialogAtual.dismiss();
                }
            });
            container.addView(row);
        }

        View addRow = inflater.inflate(R.layout.console_pc_dialog_row, container, false);
        addRow.findViewById(R.id.consoleDialogDot).setVisibility(View.GONE);
        addRow.findViewById(R.id.consoleDialogEstado).setVisibility(View.GONE);
        ((TextView) addRow.findViewById(R.id.consoleDialogNome)).setText(
                "+ " + getString(R.string.console_add_pc));
        addRow.setOnClickListener(v -> {
            if (pcDialogAtual != null) {
                pcDialogAtual.dismiss();
            }
            startActivity(new Intent(PcView.this, AddComputerManually.class));
        });
        container.addView(addRow);

        pcDialogAtual = new AlertDialog.Builder(this)
                .setTitle(R.string.console_computadores_titulo)
                .setView(container)
                .create();
        pcDialogAtual.show();
    }

    private void selecionarComputadorAtual(ComputerObject c) {
        tabletCurrentUuid = c.details.uuid;
        atualizarCabecalhoDoChip(c);
        carregarAppsDoComputadorAtual(c);
    }

    private void pararAppsDoTablet() {
        if (tabletAppPoller != null) {
            tabletAppPoller.stop();
            tabletAppPoller = null;
        }
        if (tabletAppGridAdapter != null) {
            tabletAppGridAdapter.cancelQueuedOperations();
        }
    }

    /**
     * Monta (ou remonta) a seção "Apps" para o PC dado. Reusa {@link AppGridAdapter}
     * e {@link CachedAppAssetLoader} de verdade — moque ou host real, o caminho de
     * dados é o mesmo que {@link AppView} usa, nunca uma cópia.
     */
    private void carregarAppsDoComputadorAtual(ComputerObject atual) {
        pararAppsDoTablet();

        tabletCurrentComputerDetails = atual.details;
        tabletLastRawApplist = null;
        tabletFocusedApp = null;
        tabletRunningApp = null;
        tabletAppGridAdapter = null;

        boolean online = atual.details.state == ComputerDetails.State.ONLINE;
        boolean paired = atual.details.pairState == PairState.PAIRED;

        if (!online || !paired) {
            // PC fora do ar ou não emparelhado: nada para listar.
            montarCarrossel(null);
            atualizarSecaoContinuar();
            return;
        }

        if (emModoDeCasca()) {
            if (emModoDeApps()) {
                tabletAppGridAdapter = MoqueDeApps.popularAppGridDeMentira(
                        this, PreferenceConfiguration.readPreferences(this), atual.details, false);
                for (int i = 0; i < tabletAppGridAdapter.getCount(); i++) {
                    AppView.AppObject obj = (AppView.AppObject) tabletAppGridAdapter.getItem(i);
                    if (obj.isRunning) {
                        tabletRunningApp = obj;
                        break;
                    }
                }
            }
            // Casca de mentira sem o extra de apps: a seção "Apps" fica vazia — não
            // há de onde vir uma lista sem inventar uma.
            montarCarrossel(tabletAppGridAdapter);
            atualizarSecaoContinuar();
            return;
        }

        if (managerBinder == null) {
            // completeOnCreate() ainda não terminou de conectar ao serviço; o bind
            // chama refreshConsoleInicio() de novo assim que managerBinder existir.
            return;
        }

        tabletAppGridAdapter = new AppGridAdapter(this, PreferenceConfiguration.readPreferences(this),
                atual.details, managerBinder.getUniqueId(), false);
        montarCarrossel(tabletAppGridAdapter);

        // Cache em disco primeiro — mesmo caminho de AppView#populateAppGridWithCache.
        try {
            tabletLastRawApplist = CacheHelper.readInputStreamToString(
                    CacheHelper.openCacheFileForInput(getCacheDir(), "applist", atual.details.uuid));
            atualizarListaDeAppsDoTablet(
                    NvHTTP.getAppListByReader(new StringReader(tabletLastRawApplist)), atual.details);
        } catch (IOException | XmlPullParserException e) {
            tabletLastRawApplist = null;
        }

        tabletAppPoller = managerBinder.createAppListPoller(atual.details);
        tabletAppPoller.start();
    }

    private void montarCarrossel(AppGridAdapter adapter) {
        RecyclerView carousel = findViewById(R.id.consoleAppsCarousel);
        View appsSection = findViewById(R.id.consoleAppsSection);

        if (adapter == null || adapter.getCount() == 0) {
            appsSection.setVisibility(View.GONE);
            carousel.setAdapter(null);
            tabletCarouselAdapter = null;
            return;
        }

        appsSection.setVisibility(View.VISIBLE);
        tabletCarouselAdapter = new AppCarouselAdapter(adapter, new AppCarouselAdapter.Listener() {
            @Override
            public void onAppFocused(AppView.AppObject app) {
                tabletFocusedApp = app;
                TextView name = findViewById(R.id.consoleAppsFocusedName);
                if (name != null) {
                    name.setText(app.app.getAppName());
                }
            }

            @Override
            public void onAppActivated(AppView.AppObject app) {
                abrirApp(app);
            }
        });
        carousel.setAdapter(tabletCarouselAdapter);
        carousel.post(carousel::requestFocus);

        // Nome/estado inicial: o primeiro item, antes de qualquer foco explícito do
        // usuário (teclado, D-pad ou toque).
        AppView.AppObject primeiro = (AppView.AppObject) adapter.getItem(0);
        tabletFocusedApp = primeiro;
        TextView name = findViewById(R.id.consoleAppsFocusedName);
        if (name != null) {
            name.setText(primeiro.app.getAppName());
        }
    }

    /**
     * "Continuar" só existe com um app rodando de verdade no PC atual (NOTA.md §4).
     * A miniatura vem do mesmo cache em memória que {@link CachedAppAssetLoader} usa
     * — se a capa já foi carregada para a fileira de apps, aparece aqui de graça,
     * sem pedir de novo.
     */
    private void atualizarSecaoContinuar() {
        View section = findViewById(R.id.consoleContinueSection);
        if (tabletRunningApp == null || tabletCurrentComputerDetails == null) {
            section.setVisibility(View.GONE);
            return;
        }

        section.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.consoleContinueAppName)).setText(tabletRunningApp.app.getAppName());
        ((TextView) findViewById(R.id.consoleContinueState)).setText(getString(
                R.string.console_nome_e_estado, tabletCurrentComputerDetails.name,
                getString(R.string.vidro_pill_open)));

        ImageView thumb = findViewById(R.id.consoleContinueThumb);
        if (thumb != null) {
            ScaledBitmap cache = new MemoryAssetLoader().loadBitmapFromCache(
                    new CachedAppAssetLoader.LoaderTuple(tabletCurrentComputerDetails, tabletRunningApp.app));
            if (cache != null) {
                thumb.setImageBitmap(cache.bitmap);
            }
        }
    }

    private void retomarContinuar() {
        if (tabletRunningApp == null || tabletCurrentComputerDetails == null) {
            return;
        }
        ServerHelper.doStart(this, tabletRunningApp.app, tabletCurrentComputerDetails, managerBinder, false);
    }

    private void encerrarContinuar() {
        if (tabletRunningApp == null || tabletCurrentComputerDetails == null || managerBinder == null) {
            return;
        }
        UiHelper.displayQuitConfirmationDialog(this, () ->
                ServerHelper.doQuit(PcView.this, tabletCurrentComputerDetails, tabletRunningApp.app,
                        managerBinder, null), null);
    }

    private void abrirApp(AppView.AppObject app) {
        if (tabletCurrentComputerDetails == null) {
            return;
        }
        ServerHelper.doStart(this, app.app, tabletCurrentComputerDetails, managerBinder, false);
    }

    private void abrirAppFocado() {
        if (tabletFocusedApp != null) {
            abrirApp(tabletFocusedApp);
        }
    }

    /** Espelha {@code AppView#updateUiWithAppList}, mas para o adapter da Início. */
    private void atualizarListaDeAppsDoTablet(List<NvApp> appList, ComputerDetails details) {
        if (tabletAppGridAdapter == null) {
            return;
        }

        for (NvApp app : appList) {
            boolean found = false;
            for (int i = 0; i < tabletAppGridAdapter.getCount(); i++) {
                if (((AppView.AppObject) tabletAppGridAdapter.getItem(i)).app.getAppId() == app.getAppId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                tabletAppGridAdapter.addApp(new AppView.AppObject(app));
            }
        }

        int i = 0;
        while (i < tabletAppGridAdapter.getCount()) {
            AppView.AppObject existing = (AppView.AppObject) tabletAppGridAdapter.getItem(i);
            boolean stillThere = false;
            for (NvApp app : appList) {
                if (existing.app.getAppId() == app.getAppId()) {
                    stillThere = true;
                    break;
                }
            }
            if (!stillThere) {
                tabletAppGridAdapter.removeApp(existing);
                continue;
            }
            i++;
        }

        tabletAppGridAdapter.notifyDataSetChanged();
        if (tabletCarouselAdapter != null) {
            tabletCarouselAdapter.notifyDataSetChanged();
        }
        atualizarRunningGameIdDoTablet(details);
    }

    /** Espelha {@code AppView#updateUiWithServerinfo}, mas para o adapter da Início. */
    private void atualizarRunningGameIdDoTablet(ComputerDetails details) {
        if (tabletAppGridAdapter == null) {
            return;
        }

        boolean updated = false;
        tabletRunningApp = null;
        for (int i = 0; i < tabletAppGridAdapter.getCount(); i++) {
            AppView.AppObject obj = (AppView.AppObject) tabletAppGridAdapter.getItem(i);
            boolean deveriaEstarRodando = obj.app.getAppId() == details.runningGameId;
            if (obj.isRunning != deveriaEstarRodando) {
                obj.isRunning = deveriaEstarRodando;
                updated = true;
            }
            if (deveriaEstarRodando) {
                tabletRunningApp = obj;
            }
        }

        if (updated) {
            tabletAppGridAdapter.notifyDataSetChanged();
            if (tabletCarouselAdapter != null) {
                tabletCarouselAdapter.notifyDataSetChanged();
            }
        }
        atualizarSecaoContinuar();
    }

    /** Chamado a cada notifyComputerUpdated real (não-moque) — ver startComputerUpdates(). */
    private void atualizarAppsDoTabletSeForOAtual(ComputerDetails details) {
        if (!isConsoleInicio() || tabletCurrentUuid == null || !tabletCurrentUuid.equals(details.uuid)) {
            return;
        }

        tabletCurrentComputerDetails = details;

        if (tabletAppGridAdapter == null) {
            return;
        }

        if (details.rawAppList != null && !details.rawAppList.equals(tabletLastRawApplist)) {
            tabletLastRawApplist = details.rawAppList;
            try {
                atualizarListaDeAppsDoTablet(
                        NvHTTP.getAppListByReader(new StringReader(details.rawAppList)), details);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
        } else {
            atualizarRunningGameIdDoTablet(details);
        }
    }

    @Override
    public int getAdapterFragmentLayoutId() {
        return R.layout.pc_grid_view;
    }

    @Override
    public void receiveAbsListView(AbsListView listView) {
        listView.setAdapter(pcGridAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long id) {
                ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(pos);
                if (computer.details.state == ComputerDetails.State.UNKNOWN ||
                    computer.details.state == ComputerDetails.State.OFFLINE) {
                    // Open the context menu if a PC is offline or refreshing
                    openContextMenu(arg1);
                } else if (computer.details.pairState != PairState.PAIRED) {
                    // Pair an unpaired machine by default
                    doPair(computer.details, null, null);
                } else {
                    doAppList(computer.details, false, false);
                }
            }
        });
        UiHelper.applyStatusBarPadding(listView);
        registerForContextMenu(listView);
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
