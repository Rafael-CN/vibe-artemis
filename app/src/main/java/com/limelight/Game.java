package com.limelight;


import com.limelight.ui.VidroToast;
import static com.limelight.StartExternalDisplayControlReceiver.requestFocusToExternalDisplayControl;
import static com.limelight.binding.input.KeyboardTranslator.getModifier;
import static com.limelight.utils.ExternalDisplayControlActivity.SECONDARY_SCREEN_NOTIFICATION_ID;
import static com.limelight.utils.ExternalDisplayControlActivity.closeExternalDisplayControl;
import static com.limelight.utils.ServerHelper.getActiveDisplay;
import static com.limelight.utils.ServerHelper.getSecondaryDisplay;

import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.input.TouchpadCalibration;
import com.limelight.preferences.TouchpadCalibrationActivity;
import com.limelight.binding.input.TouchpadGeometry;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.GameInputDevice;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.TextInputPump;
import com.limelight.binding.input.capture.InputCaptureManager;
import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.touch.AbsoluteTouchContext;
import com.limelight.binding.input.touch.RelativeTouchContext;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.binding.input.touch.TouchContext;
import com.limelight.binding.input.touch.TrackpadContext;
import com.limelight.binding.input.virtual_controller.VirtualController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardLayoutController;
import com.limelight.binding.video.CrashListener;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.HostCapabilities;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.profiles.ProfilesManager;
import com.limelight.ui.GavetaDaSessao;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.PixelCopy;
import com.limelight.ui.Mola;
import android.graphics.Bitmap;
import android.widget.ImageView;
import com.limelight.ui.MoqueDaStream;
import com.limelight.ui.MolduraDeEstado;
import com.limelight.ui.PrazoDaSessao;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.limelight.ui.PaletaDeComandos;
import com.limelight.ui.VidroLightView;
import com.limelight.ui.VidroSession;
import com.limelight.ui.ExternalControllerView;
import com.limelight.ui.GameGestures;
import com.limelight.ui.StreamContainer;
import com.limelight.utils.Dialog;
import com.limelight.utils.ExternalDisplayControlActivity;
import com.limelight.utils.MouseModeOption;
import com.limelight.utils.NetworkScope;
import com.limelight.utils.PanZoomHandler;
import com.limelight.utils.PerformanceDataTracker;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Rational;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import android.os.Looper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.ArrayDeque;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import android.view.SurfaceView;
import android.view.ViewGroup;


public class Game extends AppCompatActivity implements SurfaceHolder.Callback,
        OnGenericMotionListener, OnTouchListener, NvConnectionListener, EvdevListener,
        OnSystemUiVisibilityChangeListener, GameGestures, StreamContainer.InputCallbacks,
        ExternalControllerView.InputCallbacks, VidroSession.Controls,
        PerfOverlayListener, UsbDriverService.UsbDriverStateListener, View.OnKeyListener {
    public static Game instance;

    private int lastButtonState = 0;

    // Only 2 touches are supported
    private final TouchContext[] touchContextMap = new TouchContext[2];
    private final TouchContext[] trackpadContextMap = new TouchContext[2];
    private PanZoomHandler panZoomHandler;
    private long threeFingerDownTime = 0;
    private long fourFingerDownTime = 0;
    private long fiveFingerDownTime = 0;

    private static final int REFERENCE_HORIZ_RES = 1280;
    private static final int REFERENCE_VERT_RES = 720;

    private static final int STYLUS_DOWN_DEAD_ZONE_DELAY = 100;
    private static final int STYLUS_DOWN_DEAD_ZONE_RADIUS = 20;

    private static final int STYLUS_UP_DEAD_ZONE_DELAY = 150;
    private static final int STYLUS_UP_DEAD_ZONE_RADIUS = 50;

    private static final int THREE_FINGER_TAP_THRESHOLD = 300;
    private static final int FOUR_FINGER_TAP_THRESHOLD = 300;
    private static final int FIVE_FINGER_TAP_THRESHOLD = 300;

    private Handler timerHandler;

    private ControllerHandler controllerHandler;
    private KeyboardTranslator keyboardTranslator;
    private VirtualController virtualController;

    private KeyBoardController keyBoardController;

    private KeyBoardLayoutController keyBoardLayoutController;

    private PreferenceConfiguration prefConfig;
    private SharedPreferences tombstonePrefs;

    private int displayWidth;
    private int displayHeight;
    private int currentOrientation;

    public NvConnection conn;
    private SpinnerDialog spinner;
    private boolean displayedFailureDialog = false;
    // O último motivo que a conexão contou. Ver displayMessage(): ele passava como aviso
    // de dois segundos ATRÁS da folha modal, e a folha só sabia dizer "(error 0)".
    private volatile String lastDisplayMessage;
    private boolean connecting = false;
    public boolean connected = false;

    /**
     * Se o primeiro quadro chegou a aparecer nesta sessão.
     *
     * É o que separa as duas telas de falha: quem cai antes disso ainda está na tela de
     * conectar, e a folha de falha do M99 é a superfície certa; quem cai depois estava
     * jogando, e merece a tela 09 sobre o último quadro.
     */
    private boolean jaTinhaVideo = false;
    private boolean autoEnterPip = false;
    private boolean surfaceCreated = false;
    private boolean attemptedConnection = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;
    private NvApp app;
    private float desiredRefreshRate;

    private InputCaptureProvider inputCaptureProvider;
    private int modifierFlags = 0;
    private boolean grabbedInput = true;
    private boolean cursorVisible = false;
    private boolean isPanZoomMode = false;

    /** Quando o acorde comecou a esperar os modificadores subirem. Ver o teto. */
    private long inicioDaEsperaDeAcorde;
    private static final long ESPERA_MAXIMA_DE_ACORDE_MS = 3000;

    /** Cadencia da repeticao de tecla. Ver o comentario em handleKeyDown. */
    private static final long INTERVALO_DE_REPETICAO_MS = 40;
    private long ultimaRepeticaoMs;

    /** Teclas que sao ESTADO, e nao sequencia: segurar nao repete. */
    private static boolean ehModificador(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
            case KeyEvent.KEYCODE_META_LEFT:
            case KeyEvent.KEYCODE_META_RIGHT:
            case KeyEvent.KEYCODE_CAPS_LOCK:
                return true;
            default:
                return false;
        }
    }

    /**
     * Onde o ultimo toque caiu na tela, em pixels da janela.
     *
     * Serve para aproximar ao abrir o teclado. Nao da para saber onde o host tem um campo
     * de texto -- o que volta de la e video, sem nenhuma informacao de layout. O que da
     * para saber e onde ELE tocou, e um toque num campo e exatamente como um campo ganha
     * foco. Entao a aproximacao mira o ultimo toque, que na pratica e o campo.
     */
    private float ultimoToqueX = -1f;
    private float ultimoToqueY = -1f;

    /** O zoom que valia antes de aproximar para digitar, para devolver ao fechar. */
    private float zoomAntesDeDigitar = 0f;
    private float panAntesDeDigitarX = 0f;
    private float panAntesDeDigitarY = 0f;

    /** O zoom e o pan guardados enquanto a sessao esta em PiP. Ver onPictureInPictureModeChanged. */
    private float zoomAntesDoPiP = 1f;
    private float panAntesDoPiPX = 0f;
    private float panAntesDoPiPY = 0f;
    private boolean synthClickPending = false;
    private boolean pointerSwiping = false;
    private boolean waitingForAllModifiersUp = false;
    private int specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private StreamContainer streamContainer;

    private int physicalTouchpadPointerId = -1;
    private TouchpadGeometry physicalTouchpadGeometry;
    private TouchpadCalibration physicalTouchpadCalibration = TouchpadCalibration.IDENTITY;
    private int physicalTouchpadLastX;
    private int physicalTouchpadLastY;
    private float physicalTouchpadScale;
    private long synthTouchDownTime = 0;

    private boolean pendingDrag = false;
    private boolean isDragging = false;
    private float lastTouchDownX, lastTouchDownY;

    private long lastAbsTouchUpTime = 0;
    private long lastAbsTouchDownTime = 0;
    private float lastAbsTouchUpX, lastAbsTouchUpY;
    private float lastAbsTouchDownX, lastAbsTouchDownY;

    private boolean quitOnStop = false;
    private boolean isHidingOverlays;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private View performanceOverlayView;

    private TextView performanceOverlayLite;

    private TextView performanceOverlayBig;

    private MediaCodecDecoderRenderer decoderRenderer;
    private boolean reportedCrash;

    private WifiManager.WifiLock highPerfWifiLock;
    private WifiManager.WifiLock lowLatencyWifiLock;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
            binder.setListener(controllerHandler);
            binder.setStateListener(Game.this);
            binder.start();
            connectedToUsbDriverService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectedToUsbDriverService = false;
        }
    };

    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_PORT = "Port";
    public static final String EXTRA_HTTPS_PORT = "HttpsPort";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_UUID = "AppUUID";
    public static final String EXTRA_APP_ID = "AppId";

    /**
     * Sobe a sessão com um vídeo FALSO, sem falar com host nenhum.
     *
     * <p>Existe porque a sessão é a única parte deste app que nenhuma ferramenta daqui
     * conseguia ver: a captura em Robolectric não desenha {@code SurfaceView}, e um emulador
     * sem host não tem o que decodificar. Sem isto, a gaveta, o menu, a paleta e a moldura só
     * eram conferidos quando o dono gravava a tela do aparelho — que foi exatamente como o
     * "o menu flutuante sumiu" apareceu.
     *
     * <pre>adb shell am start -n com.limelight.noirdebug/com.limelight.Game --ez MoqueDeStream true</pre>
     *
     * Lido só quando {@code BuildConfig.DEBUG}. Ver {@link MoqueDaStream} para o que ele
     * prova e, principalmente, para o que ele não prova.
     */
    public static final String EXTRA_MOQUE = "MoqueDeStream";
    public static final String EXTRA_UNIQUEID = "UniqueId";
    public static final String EXTRA_PC_UUID = "UUID";
    public static final String EXTRA_PC_NAME = "PcName";
    public static final String EXTRA_APP_HDR = "HDR";
    public static final String EXTRA_SERVER_CERT = "ServerCert";
    public static final String EXTRA_VDISPLAY = "VirtualDisplay";
    public static final String EXTRA_SERVER_COMMANDS = "ServerCommands";
    public static final String EXTRA_DISPLAY_ID = "DisplayID";
    // O que o host sabe fazer. Hoje é a máscara de permissões do Apollo, montada por
    // ServerHelper.createStartIntent — ver HostCapabilities para o porquê de ela ser a
    // única fonte e para a armadilha do -1.
    public static final String EXTRA_HOST_CAPABILITIES = "HostCapabilities";

    public static final String CLIPBOARD_IDENTIFIER = "ArtemisStreaming";

    private String appUUID;
    private String host;
    private int port;
    private int httpsPort;
    private int appId;
    private String uniqueId;
    private X509Certificate serverCert;
    private boolean vDisplay;
    private ArrayList<String> serverCommands;
    private HostCapabilities hostCapabilities = HostCapabilities.unknown();

    private ViewParent rootView;
    private ClipboardManager clipboardManager;
    private boolean clipboardSyncRunning = false;

    private NvHTTP httpConn;

    public interface GameMenuCallbacks {
        void showMenu(GameInputDevice devic);
        void hideMenu();
        boolean isMenuOpen();

        /**
         * A regua de teclas especiais, sem controle especifico.
         *
         * Ela e a UNICA parte do menu legado que a gaveta nao absorveu: sao dezoito
         * combinacoes, e elas nao cabem la sem virar rolagem dentro de rolagem.
         */
        void showSpecialKeysMenu();
    }

    public GameMenuCallbacks gameMenuCallbacks;

    public boolean isInputOnly = true;
    public boolean allowChangeMouseMode = true;
    private boolean onExternelDisplay = false;
    private VidroSession vidroSession;
    private boolean composeSawKeyboard;
    private static final float CLICK_ACTION_THRESHOLD = 5;

    // Paces text and backspaces on their way to the host. The logic lives in
    // TextInputPump so it can be covered by unit tests — see TextInputPumpTest.
    private final Handler commitTextHandler = new Handler(Looper.getMainLooper());

    private final TextInputPump textInputPump = new TextInputPump(
            new TextInputPump.Sink() {
                @Override
                public void sendText(String utf8Chunk) {
                    if (conn != null) {
                        conn.sendUtf8Text(utf8Chunk);
                    }
                }

                @Override
                public void sendBackspaces(int count) {
                    if (conn == null || keyboardTranslator == null) {
                        return;
                    }
                    short backspaceCode = keyboardTranslator.translate(KeyEvent.KEYCODE_DEL, 0, -1);
                    for (int i = 0; i < count; i++) {
                        conn.sendKeyboardInput(backspaceCode, KeyboardPacket.KEY_DOWN, (byte) 0, (byte) 0);
                        conn.sendKeyboardInput(backspaceCode, KeyboardPacket.KEY_UP, (byte) 0, (byte) 0);
                    }
                }
            },
            commitTextHandler::postDelayed);

    private final Runnable backgroundPing = () -> {
        if (connected) {
            timerHandler.postDelayed(Game.this.backgroundPing, 20);
            MoonBridge.sendEmptyPayload();
        }
    };

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        timerHandler = new Handler(Looper.getMainLooper());

        UiHelper.setLocale(this);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Read the stream preferences
        prefConfig = PreferenceConfiguration.readPreferences(this);
        tombstonePrefs = Game.this.getSharedPreferences("DecoderTombstone", 0);

        if (prefConfig.fullScreen) {
            // Full-screen
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // If we're going to use immersive mode, we want to have
            // the entire screen
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Listen for UI visibility events
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        // Change volume button behavior
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Inflate the content
        setContentView(R.layout.activity_game);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // A tela de conectar do Vidro, não o SpinnerDialog do Moonlight.
        //
        // Isto estava furado desde o começo e só apareceu conectando num host de verdade:
        // eu tinha ligado stageStarting e connectionStarted ao VidroSession, mas nunca
        // chamava showConnecting(), então o que aparecia na tela era o diálogo antigo
        // "Establishing Connection" com o spinner do sistema. O layout já está inflado
        // aqui (setContentView acima), então dá para mostrar a tela direto.
        //
        // spinner fica nulo de propósito; todo uso dele abaixo é protegido.
        spinner = null;
        showVidroConnecting();


        Display currentDisplay = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int displayId = getIntent().getIntExtra(EXTRA_DISPLAY_ID, Display.DEFAULT_DISPLAY);
            currentDisplay = getSystemService(DisplayManager.class).getDisplay(displayId);
        }

        if (currentDisplay == null) {
            currentDisplay = getWindowManager().getDefaultDisplay();
        }

        onExternelDisplay = currentDisplay.getDisplayId() != Display.DEFAULT_DISPLAY;

        boolean shouldInvertDecoderResolution = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && onExternelDisplay
                && prefConfig.renderMode == 0 // For 3D we want to maintain configured resolution
        ) {
            Display.Mode currentMode = currentDisplay.getMode();
            displayWidth = currentMode.getPhysicalWidth();
            displayHeight = currentMode.getPhysicalHeight();
            prefConfig.width = displayWidth;
            prefConfig.height = displayHeight;
            prefConfig.fps = currentMode.getRefreshRate();
            prefConfig.videoScaleMode = PreferenceConfiguration.ScaleMode.STRETCH;
            prefConfig.enablePip = false;
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        } else {
            if (prefConfig.renderMode != 0) {
                prefConfig.videoScaleMode = PreferenceConfiguration.ScaleMode.STRETCH;
            }

            if (prefConfig.autoOrientation) {
                currentOrientation = getResources().getConfiguration().orientation;
            } else {
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            }

            boolean portraitMode = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
            shouldInvertDecoderResolution = portraitMode && prefConfig.autoInvertVideoResolution;

            displayWidth = shouldInvertDecoderResolution ? prefConfig.height : prefConfig.width;
            displayHeight = shouldInvertDecoderResolution ? prefConfig.width : prefConfig.height;

            // Enter landscape unless we're on a square screen
            setPreferredOrientationForActivity();
        }


        if (
                prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH ||
                        shouldIgnoreInsetsForResolution(displayWidth, displayHeight)
        ) {
            // Allow the activity to layout under notches if the fill-screen option
            // was turned on by the user or it's a full-screen native resolution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        }

        //光标是否显示
        cursorVisible = prefConfig.enableMouseLocalCursor;

        // Listen for non-touch events on the game surface
        streamContainer = findViewById(R.id.streamContainer);
        streamContainer.init(this, prefConfig);
        streamContainer.setOnGenericMotionListener(this);
        streamContainer.setOnKeyListener(this);
        streamContainer.setInputCallbacks(this);
        streamContainer.setCommitTextEnabled(prefConfig.enableCommitText);
        setupImeInsetsHandling();

        rootView = streamContainer.getParent();

        //串流画面 顶部居中显示
        if(prefConfig.alignDisplayTopCenter){
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) streamContainer.getLayoutParams();
            params.gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;
        }
        // Listen for touch events on the background touch view to enable trackpad mode
        // to work on areas outside of the StreamView itself. We use a separate View
        // for this rather than just handling it at the Activity level, because that
        // allows proper touch splitting, which the OSC relies upon.
        View backgroundTouchView = findViewById(R.id.backgroundTouchView);
        backgroundTouchView.setOnTouchListener(this);


        panZoomHandler = new PanZoomHandler(
                getApplicationContext(),
                this,
                streamContainer.getSurfaceView(),
                streamContainer,
                prefConfig
        );

        // Restore previous zoom & pan if enabled and saved
        if (prefConfig.rememberZoomPan) {
            streamContainer.post(() -> panZoomHandler.setInitialZoomAndPan(
                    prefConfig.zoomScale,
                    prefConfig.panOffsetX,
                    prefConfig.panOffsetY
            ));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Request unbuffered input event dispatching for all input classes we handle here.
            // Without this, input events are buffered to be delivered in lock-step with VBlank,
            // artificially increasing input latency while streaming.
            streamContainer.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                            InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                            InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                            InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                            InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
            backgroundTouchView.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                            InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                            InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                            InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                            InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
        }

        notificationOverlayView = findViewById(R.id.notificationOverlay);

        performanceOverlayView = findViewById(R.id.performanceOverlay);

        performanceOverlayLite = findViewById(R.id.performanceOverlayLite);

        performanceOverlayBig = findViewById(R.id.performanceOverlayBig);

        inputCaptureProvider = InputCaptureManager.getInputCaptureProvider(this, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            streamContainer.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
//                    LimeLog.info("onCapturedPointer="+motionEvent.toString());
//                    LimeLog.info("onCapturedPointer-Device="+motionEvent.getDevice().toString());
                    return handleMotionEvent(view, motionEvent);
                }
            });
        }

        // Warn the user if they're on a metered connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isMetered = connMgr.isActiveNetworkMetered();
        if (isMetered) {
            displayTransientMessage(getResources().getString(R.string.conn_metered));
        }

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            highPerfWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Moonlight High Perf Lock");
            highPerfWifiLock.setReferenceCounted(false);
            highPerfWifiLock.acquire();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lowLatencyWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Moonlight Low Latency Lock");
                lowLatencyWifiLock.setReferenceCounted(false);
                lowLatencyWifiLock.acquire();
            }
        } catch (SecurityException e) {
            // Some Samsung Galaxy S10+/S10e devices throw a SecurityException from
            // WifiLock.acquire() even though we have android.permission.WAKE_LOCK in our manifest.
            e.printStackTrace();
        }

        appName = Game.this.getIntent().getStringExtra(EXTRA_APP_NAME);
        pcName = Game.this.getIntent().getStringExtra(EXTRA_PC_NAME);

        host = Game.this.getIntent().getStringExtra(EXTRA_HOST);
        port = Game.this.getIntent().getIntExtra(EXTRA_PORT, NvHTTP.DEFAULT_HTTP_PORT);
        httpsPort = Game.this.getIntent().getIntExtra(EXTRA_HTTPS_PORT, 0); // 0 is treated as unknown
        appUUID = Game.this.getIntent().getStringExtra(EXTRA_APP_UUID);
        appId = Game.this.getIntent().getIntExtra(EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);
        uniqueId = Game.this.getIntent().getStringExtra(EXTRA_UNIQUEID);
        vDisplay = Game.this.getIntent().getBooleanExtra(EXTRA_VDISPLAY, false);
        serverCommands = Game.this.getIntent().getStringArrayListExtra(EXTRA_SERVER_COMMANDS);
        // Sem o extra — atalho antigo na tela de início, Intent de terceiro — o host fica
        // sem capacidade nenhuma conhecida, e nada que dependa dele aparece. Errar para o
        // lado de não oferecer é o oposto exato do bug que isto conserta.
        hostCapabilities = HostCapabilities.fromPermissionMask(Game.this.getIntent()
                .getIntExtra(EXTRA_HOST_CAPABILITIES, HostCapabilities.PERMISSION_UNKNOWN));
        boolean appSupportsHdr = Game.this.getIntent().getBooleanExtra(EXTRA_APP_HDR, false);
        byte[] derCertData = Game.this.getIntent().getByteArrayExtra(EXTRA_SERVER_CERT);

        app = new NvApp(appName != null ? appName : "app", appUUID, appId, appSupportsHdr);

        try {
            if (derCertData != null) {
                serverCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(derCertData));

                httpConn = new NvHTTP(new ComputerDetails.AddressTuple(host, port), httpsPort, uniqueId, serverCert, PlatformBinding.getCryptoProvider(this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (appId == StreamConfiguration.INVALID_APP_ID) {
            finish();
            return;
        }

        // Initialize the MediaCodec helper before creating the decoder
        GlPreferences glPrefs = GlPreferences.readPreferences(this);
        MediaCodecHelper.initialize(this, glPrefs.glRenderer);

        // Check if the user has enabled HDR
        boolean willStreamHdr = false;
        if (prefConfig.enableHdr) {
            if (onExternelDisplay) {
                // Enforce HDR on unsupported hardware can still enable 10bit streaming for better quality
                willStreamHdr = true;
            } else {
                // Start our HDR checklist
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Display.HdrCapabilities hdrCaps = currentDisplay.getHdrCapabilities();

                    // We must now ensure our display is compatible with HDR10
                    if (hdrCaps != null) {
                        // getHdrCapabilities() returns null on Lenovo Lenovo Mirage Solo (vega), Android 8.0
                        for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                            if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                                willStreamHdr = true;
                                break;
                            }
                        }
                    }

                    if (!willStreamHdr) {
                        // Nope, no HDR for us :(
                        VidroToast.show(this, "Display does not support HDR10", Toast.LENGTH_LONG);
                    }
                }
                else {
                    VidroToast.show(this, "HDR requires Android 7.0 or later", Toast.LENGTH_LONG);
                }
            }
        }

        // Check if the user has enabled performance stats overlay
        if (prefConfig.enablePerfOverlay) {
            performanceOverlayView.setVisibility(View.VISIBLE);
            if (prefConfig.enablePerfOverlayLite) {
                performanceOverlayLite.setVisibility(View.VISIBLE);
                if(prefConfig.enablePerfOverlayLiteDialog){
                    performanceOverlayLite.setOnClickListener(v -> showGameMenu(null));
                }
            } else {
                performanceOverlayBig.setVisibility(View.VISIBLE);
            }
            if (prefConfig.enablePerfOverlayBottom) {
                //performanceOverlayView.getLayoutParams().layout_gravity = Gravity.BOTTOM;
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) performanceOverlayView.getLayoutParams();
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                performanceOverlayView.setLayoutParams(params);
            }
        }

        decoderRenderer = new MediaCodecDecoderRenderer(
                this,
                prefConfig,
                new CrashListener() {
                    @Override
                    public void notifyCrash(Exception e) {
                        // The MediaCodec instance is going down due to a crash
                        // let's tell the user something when they open the app again

                        // We must use commit because the app will crash when we return from this function
                        tombstonePrefs.edit().putInt("CrashCount", tombstonePrefs.getInt("CrashCount", 0) + 1).commit();
                        reportedCrash = true;
                    }
                },
                tombstonePrefs.getInt("CrashCount", 0),
                connMgr.isActiveNetworkMetered(),
                willStreamHdr,
                shouldInvertDecoderResolution,
                glPrefs.glRenderer,
                this);

// --- Force tight thresholds (prefConfig.forceTightThresholds) ---
        try {
            boolean forceTight = false;
            if (prefConfig != null) {
                try {
                    java.lang.reflect.Field f = prefConfig.getClass().getDeclaredField("forceTightThresholds");
                    f.setAccessible(true);
                    Object v = f.get(prefConfig);
                    if (v instanceof Boolean) forceTight = (Boolean) v;
                } catch (Throwable ignored) {}
            }
            try { decoderRenderer.setForceTightThresholds(forceTight); } catch (Throwable ignored) {}
            if (forceTight) {
                LimeLog.info("ForceTightThresholds enabled: using vsync-based thresholds on all devices");
            }
        } catch (Throwable ignored) {}

// --- latency profile selection ---
        try {
            if (prefConfig != null && prefConfig.preferLowerDelays) {
                // Intermediate: more responsive than Balanced but not 0 µs
                decoderRenderer.setPreferLowerDelays(true);
                decoderRenderer.setPreferLowerDelaysTimeoutUs(500);  // 0.5 ms
                LimeLog.info("PreferLowerDelays: preferLowerDelays=true, timeout=500us");
            } else {
                // Balanced default
                decoderRenderer.setPreferLowerDelays(false);
                decoderRenderer.setPreferLowerDelaysTimeoutUs(2000); // 2 ms
                LimeLog.info("PreferLowerDelays: preferLowerDelays=false, timeout=2000us");
            }
            // Note: this block used to overwrite prefConfig.framePacing with BALANCED on
            // *both* branches, which silently discarded the user's frame pacing choice —
            // the list preference offers four modes and only the side effects of this
            // block were reaching the decoder. The delay preference and the pacing
            // preference are independent; leave the latter alone.
        } catch (Throwable ignored) {}

// Don't stream HDR if the decoder can't support it
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported() && !decoderRenderer.isAv1Main10Supported()) {
            willStreamHdr = false;
            VidroToast.show(this, "Decoder does not support HDR10 profile", Toast.LENGTH_LONG);
        }
        // Display a message to the user if HEVC was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_HEVC && !decoderRenderer.isHevcSupported()) {
            VidroToast.show(this, "No HEVC decoder found", Toast.LENGTH_LONG);
        }

        // Display a message to the user if AV1 was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_AV1 && !decoderRenderer.isAv1Supported()) {
            VidroToast.show(this, "No AV1 decoder found", Toast.LENGTH_LONG);
        }

        // H.264 is always supported
        int supportedVideoFormats = MoonBridge.VIDEO_FORMAT_H264;
        if (decoderRenderer.isHevcSupported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265;
            if (willStreamHdr && decoderRenderer.isHevcMain10Hdr10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265_MAIN10;
            }
        }
        if (decoderRenderer.isAv1Supported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;
            if (willStreamHdr && decoderRenderer.isAv1Main10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN10;
            }
        }

        int gamepadMask = ControllerHandler.getAttachedControllerMask(this);
        if (!prefConfig.multiController) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            gamepadMask = 1;
        }
        if (prefConfig.onscreenController) {
            // If we're using OSC, always set at least gamepad 1.
            gamepadMask |= 1;
        }

        // Set to the optimal mode for streaming
        float displayRefreshRate = prepareDisplayForRendering(currentDisplay);
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        int roundedRefreshRate = Math.round(displayRefreshRate);
        float chosenFrameRate = prefConfig.fps;
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    // Use frame drops when rendering above the screen frame rate
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Using drop mode for FPS > Hz");
                } else if (roundedRefreshRate <= 49) {
                    // Let's avoid clearly bogus refresh rates and fall back to legacy rendering
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Bogus refresh rate: " + roundedRefreshRate);
                }
                else {
                    chosenFrameRate = roundedRefreshRate - 1;
                    LimeLog.info("Adjusting FPS target for screen to " + chosenFrameRate);
                }
            }
        }

        if (prefConfig.framePacingWarpFactor > 0) {
            chosenFrameRate *= prefConfig.framePacingWarpFactor;
        }

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(
                        displayWidth,
                        displayHeight
                )
                .setLaunchRefreshRate(prefConfig.fps)
                .setRefreshRate(chosenFrameRate)
                .setVirtualDisplay(vDisplay)
                .setResolutionScaleFactor(prefConfig.resolutionScaleFactor)
                .setApp(app)
                .setEnableUltraLowLatency(prefConfig.enableUltraLowLatency)
                .setBitrate(isMetered ? prefConfig.meteredBitrate: prefConfig.bitrate)
                .setEnableSops(prefConfig.enableSops)
                .enableLocalAudioPlayback(prefConfig.playHostAudio)
                .setMaxPacketSize(1392)
                .setRemoteConfiguration(StreamConfiguration.STREAM_CFG_AUTO) // NvConnection will perform LAN and VPN detection
                .setSupportedVideoFormats(supportedVideoFormats)
                .setAttachedGamepadMask(gamepadMask)
                .setClientRefreshRateX100((int)(displayRefreshRate * 100))
                .setAudioConfiguration(prefConfig.audioConfiguration)
                .setColorSpace(decoderRenderer.getPreferredColorSpace())
                .setColorRange(decoderRenderer.getPreferredColorRange())
                .setPersistGamepadsAfterDisconnect(!prefConfig.multiController)
                .build();

        // Initialize the connection
        conn = new NvConnection(getApplicationContext(),
                new ComputerDetails.AddressTuple(host, port),
                httpsPort, uniqueId, config,
                PlatformBinding.getCryptoProvider(this), serverCert);
        controllerHandler = new ControllerHandler(this, conn, this, prefConfig);
        keyboardTranslator = new KeyboardTranslator(prefConfig);

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(keyboardTranslator, null);

        // Initialize trackpad contexts
        for (int i = 0; i < trackpadContextMap.length; i++) {
            trackpadContextMap[i] = new TrackpadContext(conn, i, prefConfig.trackpadSwapAxis, prefConfig.trackpadSensitivityX, prefConfig.trackpadSensitivityY);
        }

        if (Objects.equals(appUUID, NvApp.REMOTE_INPUT_UUID)) {
            // Force trackpad mode since we won't see anything on the screen
            isInputOnly = true;
            allowChangeMouseMode = false;
            applyMouseMode(2);
        } else {
            if (prefConfig.enableFullExDisplay && onExternelDisplay) {
                requestFocusToExternalDisplayControl(this);
                listenForExternalDisplayRemoval();
            }

            // Initialize touch contexts based on preferences
            // The mouse mode preference is also read in PreferenceConfiguration to set the boolean flags
            initMouseMode();
        }

        if (prefConfig.onscreenController) {
            // create virtual onscreen controller
            if (prefConfig.hideOSCWhenHasGamepad) {
                if (!controllerHandler.hasController()) {
                    initVirtualController();
                }
            } else {
                initVirtualController();
            }
        }

        //特殊按键屏幕布局
        if(prefConfig.enableKeyboard){
            initKeyboardController();
        }

        // A FORMA, logo depois de os controles de dedo nascerem.
        //
        // Aqui e nao antes: aplicar a forma acoplada com os controles ainda nao criados nao
        // teria o que esconder, e eles apareceriam um quadro depois -- que e exatamente o
        // piscar que a forma existe para evitar.
        aplicarFormaDaSessao();

        if (!decoderRenderer.isAvcSupported()) {
            if (spinner != null) {
                spinner.dismiss();
                spinner = null;
            }

            // If we can't find an AVC decoder, we can't proceed
            Dialog.displayDialog(this, getResources().getString(R.string.conn_error_title),
                    getResources().getString(R.string.conn_no_h264_decoder), true);
            return;
        }

        // The connection will be started when the surface gets created
        //streamContainer.getHolder().addCallback(this);

        streamContainer.setOnSurfaceAvailable(() -> {
            // O MOQUE ENTRA AQUI, e este é o único ponto de corte.
            //
            // `conn` continua existindo e continua sendo o mesmo objeto — são 56 usos sem
            // guarda de nulo espalhados por este arquivo, e trocá-lo por nulo transformaria
            // um modo de teste numa chuva de NPE. O que não acontece é o `start()`: sem ele
            // nada é negociado, nada é decodificado, e a superfície fica livre para receber
            // o nosso desenho.
            if (emModoDeMoque()) {
                if (moque == null) {
                    moque = new MoqueDaStream();
                    moque.comecar(streamContainer.getSurface());

                    // O mesmo caminho que a conexão de verdade percorre ao subir: sem ele a
                    // tela de conectar fica de pé para sempre e nada do que se quer ver
                    // aparece.
                    connectionStarted();
                }
                return;
            }

            if (!attemptedConnection) {
                LimeLog.info("Surface is available, starting connection...");
                attemptedConnection = true;

                // Der Decoder erhält die jeweils aktive Oberfläche vom Container
                decoderRenderer.setRenderTarget(streamContainer.getSurface());

                // Starten Sie die NvConnection
                // enableAudioFx, not playHostAudio: the renderer's flag controls client-side
                // audio effects, while playHostAudio asks the *host* to keep playing sound
                // through its own speakers. Passing one for the other tied the client's audio
                // processing to an unrelated host setting.
                conn.start(new AndroidAudioRenderer(Game.this, prefConfig.enableAudioFx),
                        decoderRenderer, Game.this);
            }
        });

        gameMenuCallbacks = new GameMenu(this);

        setupPainelDaSessao();
        setupComposeBar();
        // A Activity é quem sabe responder o que a folha pergunta: metade do que ela
        // comanda agora — teclado na tela, controle, cursor local — são views vivas, não
        // preferências.
        vidroSession = new VidroSession(this, prefConfig, this);

        // A GAVETA: o painel de sessão sai de cima do vídeo e vai para trás dele.
        //
        // Ela é ligada aqui e não no XML porque só a Activity sabe qual dos filhos é o vídeo.
        // O ouvinte existe por uma razão de entrada, não de desenho: com a gaveta aberta o
        // teclado do host não deve receber tecla nenhuma — quem está com o painel à mostra
        // está falando com o app, não com o PC.
        vidroGaveta = findViewById(R.id.vidroGaveta);
        if (vidroGaveta != null) {
            folhaDaSessao = findViewById(R.id.vidroSheet);
            vidroGaveta.ligar(
                    folhaDaSessao,
                    streamContainer,
                    new GavetaDaSessao.Ouvinte() {
                        @Override
                        public void aoComecarAAbrir() {
                            // O ÚNICO ponto em que a folha se põe em dia, e é de propósito.
                            //
                            // Antes isso morava em VidroSession.openSheet(), que só o toque
                            // no puxador chamava. Arrastar, apertar Voltar ou usar o acorde
                            // chamavam abrir() direto e o painel subia desatualizado — medido
                            // no emulador em paisagem: nenhum chip aceso.
                            vidroSession.sincronizarFolha();
                            mostrarJanelaDaFolha();
                        }

                        @Override
                        public void aoMudarGaveta(boolean aberta) {
                            if (aberta) {
                                // Soltar o que estiver preso ANTES de o painel ganhar o foco:
                                // um Alt segurado atravessaria a abertura e continuaria valendo
                                // do outro lado. É o mesmo cuidado da perda de foco de janela.
                                soltarEstadoDeTeclado();
                            }
                        }

                        @Override
                        public void aoAssentarFechada() {
                            esconderJanelaDaFolha();
                        }

                        @Override
                        public void aoPedirSaida() {
                            // SAIR É EMPURRAR A IMAGEM, e não apertar um botão.
                            //
                            // Uma ação com consequência passa a ser reversível por ser
                            // contínua: você vê a consequência SE FORMANDO e desfaz no meio do
                            // caminho, sem diálogo nenhum. Mais seguro que qualquer botão com
                            // confirmação e mais rápido que qualquer botão sem.
                            //
                            // E o gesto significa a saída SEGURA: a casca vem por cima e o
                            // vídeo continua rodando atrás, com prazo. Encerrar de verdade
                            // continua sendo escolha explícita, na gaveta.
                            mostrarCasca();
                        }
                    });
            vidroSession.setGaveta(vidroGaveta);

            // O PUXADOR TAMBEM ABRE POR TOQUE.
            //
            // Um gesto que so existe como gesto precisa ser ensinado, e este app nao tem
            // onde ensinar nada durante uma partida. Quem ve a barrinha e toca nela chega ao
            // mesmo lugar de quem sabe arrastar.
            View puxador = findViewById(R.id.vidroPuxadorDaGaveta);
            if (puxador != null) {
                // O SOCORRO: toque longo abre o menu legado, em DIÁLOGO.
                //
                // Existe por causa do aparelho do dono, onde a gaveta ficou invisível em três
                // arranjos de camada diferentes enquanto os DIÁLOGOS — janela separada do
                // sistema — renderizavam sobre o vídeo em todas as gravações. Diálogo é a
                // única superfície de controle comprovadamente visível naquele compositor.
                //
                // Não substitui a gaveta: é a saída de emergência para nunca mais ficar
                // "preso na stream". Se a gaveta desenha, ninguém precisa disto; se não
                // desenha, isto é o que resta — e resta sempre.
                puxador.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (vidroGaveta != null) {
                            vidroGaveta.fecharSeco();
                        }
                        if (gameMenuCallbacks != null) {
                            gameMenuCallbacks.showMenu(null);
                            return true;
                        }
                        return false;
                    }
                });
                puxador.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Pelo vidroSession, e nao pela gaveta direto: e ele que faz
                        // refreshSheet(). Abrindo pela gaveta o painel subia com NENHUM chip
                        // aceso -- o modo do mouse e o ritmo de quadros apareciam como tres
                        // opcoes iguais, sem dizer qual esta valendo.
                        if (vidroGaveta.estaAberta()) {
                            vidroGaveta.fechar();
                        } else if (vidroSession != null) {
                            vidroSession.openSheet();
                        } else {
                            vidroGaveta.abrir();
                        }
                    }
                });
            }
        }

        //fixed size + pacing without back-pressure on MTK
        try {
            View root = findViewById(android.R.id.content);
            // Niente getIdentifier: troviamo la prima SurfaceView nel layout
            SurfaceView streamSurfaceView = findFirstSurfaceViewFrom(root);

            if (streamSurfaceView != null) {
                // Avoid resizes/glitches that break the compositor
                int vw = (prefConfig != null && prefConfig.width > 0) ? prefConfig.width : displayWidth;
                int vh = (prefConfig != null && prefConfig.height > 0) ? prefConfig.height : displayHeight;
                try { streamSurfaceView.getHolder().setFixedSize(vw, vh); } catch (Throwable ignored) {}
                try { streamSurfaceView.setZOrderOnTop(false); } catch (Throwable ignored) {}
                try { streamSurfaceView.setZOrderMediaOverlay(false); } catch (Throwable ignored) {}

                // 2) setFrameRate via reflection (compat < 30)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    float displayHz = 60f;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            displayHz = currentDisplay.getMode().getRefreshRate();
                        } else {
                            displayHz = currentDisplay.getRefreshRate();
                        }
                    } catch (Throwable ignored) {}

                    float targetFps = (prefConfig != null && prefConfig.fps > 0) ? prefConfig.fps : displayHz;

                    boolean isMTKDevice;
                    try {
                        String sum = (android.os.Build.MANUFACTURER + " " + android.os.Build.HARDWARE + " " + android.os.Build.BOARD)
                                .toLowerCase(java.util.Locale.US);
                        isMTKDevice = sum.contains("mtk") || sum.contains("mediatek");
                    } catch (Throwable t) { isMTKDevice = false; }

                    int compat = isMTKDevice
                            ? Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                            : Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE;

                    try {
                        java.lang.reflect.Method m = SurfaceView.class.getMethod("setFrameRate", float.class, int.class);
                        m.invoke(streamSurfaceView, Math.min(targetFps, displayHz), compat);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private void listenForExternalDisplayRemoval() {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                if (getSecondaryDisplay(getBaseContext()) == null) {
                    handleDisplayRemoved();
                    finish();
                }
            }

            @Override
            public void onDisplayChanged(int displayId) {
            }
        }, null);
    }

    private void handleDisplayRemoved() {
        NotificationManagerCompat.from(getBaseContext()).cancel(SECONDARY_SCREEN_NOTIFICATION_ID);
        closeExternalDisplayControl();
    }

    private void initKeyboardController(){
        keyBoardController = new KeyBoardController(conn,(FrameLayout)rootView, this);
        keyBoardController.refreshLayout();
        keyBoardController.show();
    }

    @Override
    public Boolean isKeyboardLayoutVisible() {
        return keyBoardLayoutController != null && keyBoardLayoutController.shown;
    }

    private void initVirtualController(){
        virtualController = new VirtualController(controllerHandler, (FrameLayout)rootView, this);
        virtualController.refreshLayout();
        virtualController.show();
    }

    private void initkeyBoardLayoutController(){
        keyBoardLayoutController = new KeyBoardLayoutController((FrameLayout)rootView, this, prefConfig);
        keyBoardLayoutController.refreshLayout();
        keyBoardLayoutController.show();
    }

    //显示隐藏虚拟特殊按键

    /**
     * Mostra a tela de conectar com o nome do app e do host.
     *
     * Os três passos avançam por evento real da conexão — ver onStageStarting em
     * VidroSession. Não há temporizador aqui.
     */
    private void showVidroConnecting() {
        View connecting = findViewById(R.id.vidroConnecting);
        if (connecting == null) {
            return;
        }

        // Lê do Intent, não do campo: appName só é preenchido bem depois, em
        // completeOnCreate, e nesta altura ainda está nulo — a tela abria sem título.
        TextView appLabel = findViewById(R.id.vidroConnectingApp);
        if (appLabel != null) {
            String app = getIntent().getStringExtra(EXTRA_APP_NAME);
            appLabel.setText(app != null ? app : "");
        }

        TextView stepLabel = findViewById(R.id.vidroConnectingStep);
        if (stepLabel != null) {
            String host = getIntent().getStringExtra(EXTRA_PC_NAME);
            stepLabel.setText(getString(R.string.vidro_step_talking, host != null ? host : ""));
        }

        android.widget.ProgressBar bar = findViewById(R.id.vidroConnectingBar);
        if (bar != null) {
            bar.setProgress(18);
        }

        VidroLightView light = findViewById(R.id.vidroConnectingLight);
        if (light != null) {
            light.setIntensity(VidroLightView.INTENSITY_ASLEEP, false);
        }

        View cancel = findViewById(R.id.vidroConnectingCancel);
        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // A CAPA QUE VOCÊ TOCOU, e ela chega aqui de graça.
        //
        // `EXTRA_PC_UUID` e `EXTRA_APP_ID` já viajavam no Intent desde sempre — são os dois
        // que apontam para o arquivo que a grade acabou de desenhar. Nada de bitmap no
        // Intent: aquilo é TransactionTooLargeException com hora marcada.
        //
        // Estático, e não pelo vidroSession, porque este método roda ANTES dele existir:
        // é o caminho do primeiro toque, e é justamente ele que a espera precisa cobrir.
        VidroSession.carregarCapa(this,
                getIntent().getIntExtra(EXTRA_APP_ID, 0),
                getIntent().getStringExtra(EXTRA_PC_UUID));

        connecting.setVisibility(View.VISIBLE);
    }

    /**
     * A barra está de pé?
     *
     * Amarrado à VISIBILIDADE REAL da view, e não a um booleano paralelo. Este teste
     * decide se o teclado inteiro para de chegar no PC, então ele tem de olhar o único
     * estado que não dessincroniza — se a barra some e o booleano fica true, a sessão
     * inteira perde o teclado sem que nada na tela explique por quê.
     */
    public boolean isComposeBarVisible() {
        View bar = findViewById(R.id.vidroComposeBar);
        return bar != null && bar.getVisibility() == View.VISIBLE;
    }

    /**
     * Com a barra aberta, o teclado é do campo — não do jogo.
     *
     * O {@code EditText} consome o que é texto e devolve o resto, e esse resto descia para
     * {@code onKeyDown} e viajava: Esc pausava o jogo enquanto a barra continuava aberta,
     * F1 abria a ajuda do host, Tab passava o foco lá do outro lado. Quem tem teclado de
     * verdade usa Esc e Tab por reflexo, e o modelo pedido para este fork é o do AnyDesk:
     * enquanto se digita num campo, o campo é uma ilha.
     *
     * Entrega só à hierarquia de views e para aqui. O que sobrar morre nesta linha em vez
     * de descer para {@code handleKeyDown}.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!isComposeBarVisible()) {
            return super.dispatchKeyEvent(event);
        }

        // Controle não digita, então a ilha não é dele.
        //
        // Fora isto os botões do gamepad morreriam aqui junto com o teclado — e o KEY_UP
        // de um botão que já estava descido morreria junto, deixando-o preso no host, que
        // é o pior desfecho possível de abrir um campo de texto.
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            return super.dispatchKeyEvent(event);
        }

        if (getWindow().superDispatchKeyEvent(event)) {
            return true;
        }

        // Esc e Back fecham a barra mesmo sem o campo em foco — sem isto, um toque fora do
        // campo deixaria a única saída no botão "Fechar".
        if (event.getAction() == KeyEvent.ACTION_UP
                && (event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE
                    || event.getKeyCode() == KeyEvent.KEYCODE_BACK)) {
            closeComposeBar();
        }

        return true;
    }

    /**
     * Abre a barra de composição e o teclado junto.
     *
     * Digitar direto no stream é digitar às cegas: cada tecla viaja e o que volta é
     * vídeo. Aqui o texto se monta localmente — com cursor, seleção e correção do próprio
     * Android — e só atravessa quando a pessoa manda.
     */
    // Package-private: o menu de socorro (GameMenu) tambem abre a barra.
    /**
     * O que estava escrito quando a barra fechou sem enviar.
     *
     * Medido no emulador: digitar, tocar em "Fechar" e reabrir devolvia o campo VAZIO — e
     * um comentário no caminho de PiP jurava o contrário ("o rascunho não se perde"). O
     * fechamento cancela o envio em voo, e isso fica: texto atravessando para uma janela
     * que ninguém está olhando é o defeito que aquele cancelamento mata. Mas o que ainda
     * não foi enviado não é envio em voo — é trabalho da pessoa, e um toque acidental no
     * Fechar não pode destruí-lo. Enviar é o que limpa.
     */
    private String rascunhoDaBarra = "";

    void openComposeBar() {
        aproximarParaDigitar();

        final View bar = findViewById(R.id.vidroComposeBar);
        final EditText input = findViewById(R.id.vidroComposeInput);
        if (bar == null || input == null) {
            return;
        }

        // Solta no host o que estiver descido ANTES de fechar a porta. Daqui em diante os
        // KEY_UP ficam com o campo, então um Ctrl segurado na hora de abrir a barra nunca
        // subiria do outro lado — e tecla presa no PC estraga tudo o que vem depois.
        soltarEstadoDeTeclado();

        if (paleta != null) {
            // Abre sempre no campo, nunca na lista: reabrir com uma linha realçada de uma
            // sessão anterior faria o primeiro Enter executar algo que ninguém escolheu.
            paleta.recolher();
        }

        composeSawKeyboard = false;
        bar.setVisibility(View.VISIBLE);

        // O rascunho volta para onde estava, com o cursor no fim.
        if (input.getText().length() == 0 && !rascunhoDaBarra.isEmpty()) {
            input.setText(rascunhoDaBarra);
            input.setSelection(input.getText().length());
        }

        input.requestFocus();
        updateComposeSendEnabled();

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        }

        if (vidroGaveta != null) {
            // A gaveta fecha: o assunto agora é o texto. E ela não pode ficar aberta com a
            // barra em pé por um motivo de geometria, não de gosto — o tratamento de IME
            // desloca o vídeo, e o vídeo é justamente o que carrega a gaveta no ombro.
            vidroGaveta.fechar();
        }
    }

    /**
     * Fecha a barra, devolve o enquadramento e CANCELA o que ainda nao foi enviado.
     *
     * Fechar parecia cancelar e nao cancelava: o texto continuava atravessando caractere a
     * caractere numa janela que ele nao estava mais olhando, e no fim um Enter era apertado
     * la, confirmando o comando errado. Quem fecha esta desistindo.
     */
    private void closeComposeBar() {
        if (textInputPump != null) {
            textInputPump.clear();
        }
        devolverZoomDeDigitar();

        View bar = findViewById(R.id.vidroComposeBar);
        EditText input = findViewById(R.id.vidroComposeInput);
        if (bar == null) {
            return;
        }

        bar.setVisibility(View.GONE);
        composeSawKeyboard = false;

        // A barra saiu: o vídeo recupera na hora a altura que ela ocupava. Sem isto ele só
        // voltaria no próximo evento de inset, e num aparelho que já fechou o teclado esse
        // evento pode nunca chegar.
        applyImeOffset(lastImeOverlap);

        if (input != null) {
            // Guarda ANTES de limpar — ver rascunhoDaBarra. O campo esvazia porque a
            // barra some e um campo cheio invisível confundiria o updateComposeSendEnabled;
            // o texto em si continua guardado para a próxima abertura.
            rascunhoDaBarra = input.getText().toString();
            input.setText("");
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        }

        // Nada volta ao sair da barra, e isso é a regra e não um esquecimento: em repouso
        // a sessão tem zero pixel de interface. Quem quiser o painel de volta puxa a borda
        // ou usa o acorde — os dois custam menos que ler um controle que ficou na tela.
    }

    /**
     * A barra acompanha o topo do teclado.
     *
     * Ela só fecha quando o IME REALMENTE se foi — antes disso ela morria sozinha. Ao
     * abrir, o Android entrega um ou dois callbacks de inset com overlap 0 enquanto o
     * teclado ainda está animando para dentro, e eu tratava esse zero como "o teclado
     * sumiu". Daí o relato de que às vezes abria e às vezes fechava na hora: era uma
     * corrida com a animação do IME.
     *
     * Agora só fecha depois de ter visto o teclado de pé pelo menos uma vez.
     *
     * @param imeBottom inset do IME medido do fundo da JANELA. Tem de ser o número cheio,
     *                  não o que o vídeo usa (esse desconta a barra de navegação): a barra
     *                  se ancora no fundo da janela, então descontar deixava ela flutuando
     *                  uma barra de navegação acima do teclado.
     */
    private void updateComposeBar(int imeBottom) {
        View bar = findViewById(R.id.vidroComposeBar);
        if (bar == null || bar.getVisibility() != View.VISIBLE) {
            return;
        }

        if (imeBottom > 0) {
            composeSawKeyboard = true;
            bar.setTranslationY(-imeBottom);
            return;
        }

        if (composeSawKeyboard) {
            closeComposeBar();
        }
    }


/**
     * Os oito lugares do desenho que o dono mandou, como GRAVIDADE.
     *
     *     . . .
     *     .   .
     *     . . .
     *
     * O centro fica de fora: é onde a imagem está, e é o único lugar em que um controle não
     * pode ficar durante a stream.
     *
     * Gravidade, e não coordenada, porque coordenada exige saber contra QUAL quadro medir —
     * e foi exatamente isso que eu errei duas vezes, deixando o punho cortado na borda. Aqui
     * quem faz a conta é o ViewGroup, contra os limites reais do pai.
     */
    private static final int[] VIDRO_HANDLE_ANCHORS = {
        Gravity.TOP | Gravity.START,
        Gravity.TOP | Gravity.CENTER_HORIZONTAL,
        Gravity.TOP | Gravity.END,
        Gravity.CENTER_VERTICAL | Gravity.START,
        Gravity.CENTER_VERTICAL | Gravity.END,
        Gravity.BOTTOM | Gravity.START,
        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
        Gravity.BOTTOM | Gravity.END,
    };

    /** Âncora padrão: canto de baixo à esquerda, que é onde a ilha sempre morou. */
    private static final int VIDRO_HANDLE_ANCHOR_DEFAULT = 5;

    private static final String VIDRO_HANDLE_ANCHOR = "vidro_handle_anchor";

    /** A gaveta que carrega o painel da sessão atrás do vídeo. */
    private GavetaDaSessao vidroGaveta;

    /**
     * A paleta de comandos, que vive dentro da barra de composição.
     *
     * Ela não é um modo: em repouso a barra continua sendo um campo de texto e mais nada. A
     * lista só existe depois que a seta desce, e some quando ela sobe demais.
     */
    private PaletaDeComandos paleta;

    /** A moldura que diz como a rede está, sem texto. */
    private MolduraDeEstado moldura;

    /**
     * A área que o sistema reivindica — recorte de câmera e barras.
     *
     * Guardada porque quem desenha por cima do vídeo precisa dela por outro caminho que não
     * a margem de layout. Alimentada em applyVidroSafeArea.
     */
    private final Rect vidroSafeInsets = new Rect();

    /** Altura do teclado do sistema quando ele está de pé, em pixels. */
    private int vidroImeInset;

    /**
     * Monta o painel da sessão — o que a ilha fazia, agora dentro da gaveta.
     *
     * A ilha tinha três blocos e um botão. "Ajustar" abria esta mesma folha e por isso
     * deixou de existir: era a porta para o cômodo em que já se está. Os outros três são
     * ações de verdade e viraram linhas no pé do painel, na mesma gramática do resto dele.
     */
    private void setupPainelDaSessao() {
        View escrever = findViewById(R.id.vidroSheetCompose);
        if (escrever != null) {
            escrever.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // A barra de composição já fecha a gaveta ao abrir; chamar as duas coisas
                    // aqui duplicaria a decisão em dois lugares que podem divergir depois.
                    openComposeBar();
                }
            });
        }

        View girar = findViewById(R.id.vidroSheetGirar);
        if (girar != null) {
            girar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (vidroGaveta != null) {
                        vidroGaveta.fechar();
                    }
                    rotateScreen();
                }
            });
        }

        View especiais = findViewById(R.id.vidroSheetTeclasEspeciais);
        if (especiais != null) {
            especiais.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (vidroGaveta != null) {
                        vidroGaveta.fechar();
                    }
                    // As teclas especiais continuam sendo a lista do menu legado: são
                    // dezoito combinações e elas não cabem na gaveta sem virar rolagem
                    // dentro de rolagem. É a exceção, e é uma só.
                    if (gameMenuCallbacks != null) {
                        gameMenuCallbacks.showSpecialKeysMenu();
                    }
                }
            });
        }

        View trocar = findViewById(R.id.vidroSheetTrocar);
        if (trocar != null) {
            trocar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // A SAIDA SEGURA. A casca vem por cima e o video continua rodando atras,
                    // com prazo. E o mesmo desfecho do gesto de empurrar a imagem para baixo,
                    // e o oposto da linha logo abaixo, que encerra.
                    mostrarCasca();
                }
            });
        }

        View sair = findViewById(R.id.vidroSheetLeave);
        if (sair != null) {
            sair.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // disconnect(), e não finish() direto: é a MESMA saída que o menu
                    // oferece, e sair por aqui pulava a sincronia da área de transferência
                    // que sair pelo menu faz. Mesmo rótulo, mesmo caminho, mesmo efeito.
                    disconnect();
                }
            });
        }

        TextView nome = findViewById(R.id.vidroSheetApp);
        if (nome != null && appName != null && !appName.isEmpty()) {
            nome.setText(appName);
        }

        moldura = findViewById(R.id.vidroMoldura);

        applyVidroSafeArea();
    }

    /**
     * Os índices dos modos do mouse, os mesmos que a folha e Game.applyMouseMode já falam.
     *
     * Repetidos aqui e não importados de VidroSession porque lá eles são privados, e abrir
     * três constantes de uma classe para um único chamador troca um número com nome por um
     * acoplamento. Os três são os mesmos valores de `mouse_mode_list` no XML.
     */
    private static final int MODO_MULTITOQUE = 0;
    private static final int MODO_ABSOLUTO = 1;
    private static final int MODO_TRACKPAD = 2;

    /**
     * Os comandos da paleta — os mesmos da gaveta, alcançáveis sem tirar as mãos do teclado.
     *
     * A lista é a MESMA coisa que o painel oferece, e isso é de propósito: duas listas de
     * comandos que divergem é como um app passa a mentir sobre si mesmo. O que muda entre as
     * duas é só o meio — lá o dedo, aqui a seta.
     *
     * Fora ficaram os comandos cujo efeito é sobre o dedo (controle na tela, teclado
     * desenhado, teclas especiais): quem está com a capa acoplada não os quer, e oferecê-los
     * aqui seria oferecer a saída da situação em que a pessoa está.
     */
    private void montarPaleta(EditText input) {
        ScrollView rolagem = findViewById(R.id.vidroPaletaRolagem);
        LinearLayout lista = findViewById(R.id.vidroPaletaLista);
        View realce = findViewById(R.id.vidroPaletaRealce);
        if (rolagem == null || lista == null || realce == null) {
            return;
        }

        paleta = new PaletaDeComandos(this, input, rolagem, lista, realce);

        java.util.List<PaletaDeComandos.Comando> comandos = new java.util.ArrayList<>();
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_mouse_multitouch,
                new Runnable() {
                    @Override
                    public void run() {
                        setMouseMode(MODO_MULTITOQUE);
                    }
                }));
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_mouse_trackpad,
                new Runnable() {
                    @Override
                    public void run() {
                        setMouseMode(MODO_TRACKPAD);
                    }
                }));
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_mouse_absolute,
                new Runnable() {
                    @Override
                    public void run() {
                        setMouseMode(MODO_ABSOLUTO);
                    }
                }));
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_zoom_mode,
                new Runnable() {
                    @Override
                    public void run() {
                        toggleZoomMode();
                    }
                }));
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_local_cursor,
                new Runnable() {
                    @Override
                    public void run() {
                        toggleMouseLocalCursor();
                    }
                }));
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_numbers_on_screen,
                new Runnable() {
                    @Override
                    public void run() {
                        toggleHUD();
                    }
                }));
        comandos.add(new PaletaDeComandos.Comando(R.string.vidro_leave_screen,
                new Runnable() {
                    @Override
                    public void run() {
                        disconnect();
                    }
                }));

        paleta.definirComandos(comandos);
    }

    private void setupComposeBar() {
        final EditText input = findViewById(R.id.vidroComposeInput);
        final View send = findViewById(R.id.vidroComposeSend);
        View bar = findViewById(R.id.vidroComposeBar);
        View close = findViewById(R.id.vidroComposeClose);
        if (input == null || send == null) {
            return;
        }

        // Sair da barra era assunto do teclado do sistema: ela só fechava quando o IME
        // fechava. Quem abrisse sem querer, ou desistisse do texto, dependia do chevron do
        // Android — e em navegação por gestos ele nem sempre está desenhado.
        if (close != null) {
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeComposeBar();
                }
            });
        }

        // A barra cresce para cima a cada linha que o texto quebra, e o vídeo já estava
        // ajustado à altura anterior — cada linha digitada comia mais um pedaço da imagem.
        // Refazer a conta a cada mudança de altura devolve o pedaço na mesma hora.
        if (bar != null) {
            bar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bottom - top != oldBottom - oldTop) {
                        applyImeOffset(lastImeOverlap);
                    }
                }
            });
        }

        // O botão só existe quando há o que mandar.
        //
        // Ele era idêntico com o campo vazio e com texto — mesmo azul, mesma opacidade — e
        // tocá-lo em branco não fazia nada, sem dizer por quê. Espaço em branco também não
        // é conteúdo: quem toca depois de apagar tudo e deixar um enter no campo não quer
        // mandar um enter para o PC.
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateComposeSendEnabled();
            }
        });
        updateComposeSendEnabled();

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchComposeText(false);
            }
        });

        montarPaleta(input);

        // Esc, Back e Enter moram aqui, e não no OnEditorActionListener.
        //
        // O campo é textMultiLine, e num campo multilinha o Android NÃO chama
        // onEditorAction para o Enter de teclado físico — ele insere quebra de linha e
        // pronto. O teste de KEYCODE_ENTER estava no listener errado desde sempre: quem
        // digitava com a capa acoplada apertava Enter e via o texto ganhar uma linha.
        //
        // O OnKeyListener roda antes do tratamento próprio da view, então consumir aqui é
        // o que impede a quebra de linha de acontecer mesmo assim.
        input.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // A PALETA PERGUNTA PRIMEIRO, e ela recusa quase tudo.
                //
                // Com nada realçado ela só reivindica a seta para baixo; o Enter, o Esc e
                // todas as letras seguem para o tratamento abaixo, que é o de sempre. É o
                // contrato inteiro: nada executa por digitar.
                if (paleta != null && paleta.aoTeclar(keyCode, event)) {
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
                    // No ACTION_UP: fechar no down deixaria o up órfão viajando para o PC.
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        closeComposeBar();
                    }
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    // Shift+Enter continua sendo quebra de linha: um comando de várias
                    // linhas ainda precisa de um jeito de ganhar a segunda.
                    if (event.isShiftPressed()) {
                        return false;
                    }
                    if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                        dispatchComposeText(true);
                    }
                    return true;
                }

                return false;
            }
        });

        // A ação de envio do próprio teclado de tela faz a mesma coisa que o botão: manda
        // o texto, sem Enter. Ela nem sempre existe num campo multilinha, e é por isso que
        // ela não pode ser o único caminho.
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    dispatchComposeText(false);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * O "Enviar" só acende quando há o que mandar E quando dá para mandar.
     *
     * Espaço em branco não é conteúdo. E com o pump ainda drenando o bloco anterior, o
     * botão apagado é a informação exata de que o texto continua saindo — antes ele ficava
     * aceso e um segundo toque empilhava um bloco atrás do outro.
     */
    private void updateComposeSendEnabled() {
        View send = findViewById(R.id.vidroComposeSend);
        EditText input = findViewById(R.id.vidroComposeInput);
        if (send == null || input == null) {
            return;
        }
        send.setEnabled(textInputPump.isIdle()
                && input.getText().toString().trim().length() > 0);
    }

    /**
     * Manda o que está no campo e só depois diz que mandou.
     *
     * O aviso mentia: {@code sendTextBlock} apenas enfileira, e a torrada disparava na
     * linha seguinte, com o pump ainda drenando um pedaço por tique. Quem confiava nela
     * fechava a barra no meio do próprio comando; quem não confiava mandava de novo e
     * chegava duplicado. Agora o aviso — e o Enter — esperam a fila esvaziar.
     *
     * @param withReturn true para o Enter físico: além do texto, um VK_RETURN no PC. Ele
     *                   sai DEPOIS do último pedaço, senão confirma um comando pela metade.
     */
    private void dispatchComposeText(final boolean withReturn) {
        final EditText input = findViewById(R.id.vidroComposeInput);
        if (input == null) {
            return;
        }

        String text = input.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }

        // SEM CONEXÃO, A VERDADE — e o texto fica onde está.
        //
        // Medido no emulador com a conexão que nunca subiu: o envio "drenava" para o nada,
        // o campo era limpo e a torrada dizia "enviado ao PC". Duas mentiras no pior
        // momento possível — a conexão que caiu no meio da digitação. Quem digitou uma
        // senha e leu "enviado" acredita que ela está no host. Agora o aviso diz que não
        // há conexão, e o que foi digitado continua no campo para sair quando ela voltar.
        if (!connected) {
            VidroToast.show(this, R.string.vidro_compose_no_connection, Toast.LENGTH_LONG);
            return;
        }

        sendTextBlock(text);
        input.setText("");
        rascunhoDaBarra = "";
        updateComposeSendEnabled();

        textInputPump.runWhenIdle(new Runnable() {
            @Override
            public void run() {
                if (withReturn && conn != null) {
                    sendKeys(new short[]{KeyboardTranslator.VK_RETURN});
                }

                // O envio não deixava rastro: a barra some, o texto some junto e nada diz
                // se ele chegou. Sem eco do host para conferir, o aviso é o único retorno
                // possível. O texto do aviso diz QUAL dos dois envios foi — é a única
                // pista de que o Enter também atravessou.
                VidroToast.show(Game.this, withReturn
                        ? R.string.vidro_compose_sent_enter
                        : R.string.vidro_compose_sent);
                updateComposeSendEnabled();
            }
        });
    }

    /**
     * A área segura da sessão — o que ainda tem dono depois que a ilha e o punho saíram.
     *
     * A sessão pede a janela inteira por baixo do recorte de câmera
     * (LAYOUT_IN_DISPLAY_CUTOUT_MODE, quando a resolução casa ou o vídeo estica), mas o
     * vídeo não entra ali: o letterbox do StreamContainer o mantém na área útil. Quem
     * desenha por cima é que precisa descontar o recorte à mão.
     *
     * Sobraram dois: a barra de composição e o painel da gaveta. O laço de margens que
     * existia aqui — com os arrays de base e o cálculo por overlay — servia a UM elemento, a
     * ilha, e foi embora com ela. Vale dizer o que ele fazia para ninguém reinventá-lo: ele
     * SOMAVA o inset à margem do layout, nunca substituía, porque substituir colava o
     * elemento no recorte.
     */
    private void applyVidroSafeArea() {
        final View content = findViewById(android.R.id.content);
        if (content == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            // WindowInsetsCompat porque displayCutout() nativo é API 28+ e minSdk é 21.
            int mask = WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.systemBars();
            int safeLeft = insets.getInsets(mask).left;
            int safeRight = insets.getInsets(mask).right;
            int safeBottom = insets.getInsets(mask).bottom;

            // O teclado é área do sistema como qualquer outra: enquanto ele está de pé, a
            // faixa que ele ocupa não pertence ao app.
            vidroImeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            vidroSafeInsets.set(safeLeft, insets.getInsets(mask).top, safeRight, safeBottom);

            applyComposeBarSafeArea(safeLeft, safeRight);
            applySheetSafeArea(safeRight, safeBottom);

            // Devolvidos intactos: o StreamContainer tem o próprio ouvinte e precisa dos
            // mesmos insets para descontar o teclado.
            return insets;
        });

        // Pedir os insets é obrigatório, não zelo. Registrar o ouvinte não dispara nada: se
        // a janela já tinha distribuído os insets antes desta chamada — e tinha, o
        // setContentView vem muito antes —, o ouvinte só acordaria na próxima mudança, e
        // até lá a área segura ficava zerada.
        ViewCompat.requestApplyInsets(content);
    }

    /**
     * A barra de composição também mora na área segura.
     *
     * Em paisagem a barra de navegação fica na LATERAL, e a barra ia de borda a borda: o
     * teclado do sistema parava no inset e ela não, então o "Enviar" terminava por baixo
     * dos controles do sistema — medido na gravação, 40% da largura do botão dentro da
     * faixa de 49 px em que o sistema desenha os próprios glifos.
     *
     * Padding e não margem: o vidro continua sangrando até a borda, que é o desenho; o que
     * precisa sair da faixa do sistema é o conteúdo, não o fundo.
     */
    private void applyComposeBarSafeArea(int safeLeft, int safeRight) {
        View bar = findViewById(R.id.vidroComposeBar);
        if (bar == null) {
            return;
        }

        // left/right e não start/end de propósito: os insets vêm em coordenada física.
        int base = getResources().getDimensionPixelSize(R.dimen.vidro_space_3);
        bar.setPadding(base + safeLeft, base, base + safeRight, base);
    }

    /**
     * A folha de ajustes encosta na borda direita e no rodapé — as duas faixas em que o
     * sistema desenha em paisagem.
     *
     * A última seção da folha já aparecia cortada, e o inset somado embaixo é o que impede
     * que a barra de navegação coma mais um pedaço dela. Padding, de novo: o vidro sangra
     * até a borda, o conteúdo não.
     */
    // ─────────────────────────────────────────────────────────────────────────
    // A janela da folha
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A folha, guardada em campo porque ela SAI da hierarquia desta Activity.
     *
     * Ela nasce no activity_game.xml — é lá que o inflate acontece e que todos os
     * findViewById do onCreate a encontram — e na primeira abertura é transplantada para uma
     * janela própria. Depois disso, findViewById(R.id.vidroSheet) devolve null; quem precisar
     * dela usa este campo.
     */
    private View folhaDaSessao;
    private android.app.Dialog janelaDaFolha;

    /**
     * Mostra a folha NUMA JANELA PRÓPRIA, por cima da janela do vídeo.
     *
     * <h3>Por que uma janela, e não uma View — a história de quatro tentativas</h3>
     *
     * O painel dentro da janela da Activity foi invisível no aparelho do dono em TRÊS
     * arranjos diferentes: atrás da SurfaceView (a ideia original), na frente dela dentro do
     * mesmo grupo, e na raiz do layout. Em todos, no emulador funcionava e no aparelho não —
     * preto absoluto, medido em gravação: RGB (0,0,0), uma cor só na região inteira.
     *
     * A prova de que o painel ESTAVA desenhado veio do PiP: ao entrar em
     * picture-in-picture ele aparece por meio segundo na miniatura, que é exatamente o
     * momento em que o sistema rearranja a camada da superfície de vídeo. Alguma coisa
     * naquele compositor compõe a superfície por cima do conteúdo da janela — e não é este
     * código: as únicas chamadas de z-order do app desligam o on-top.
     *
     * Contra isso não há ordem de filho que resolva. Uma janela separada resolve por
     * construção: janelas são compostas em cima de janelas, e o vídeo mora na outra.
     *
     * <h3>As escolhas desta janela</h3>
     *
     * <b>Não-focável:</b> o teclado e o gamepad continuam com a sessão; o painel é território
     * de toque, como sempre foi (o ScrollView já bloqueava foco de descendentes).
     * <b>Sem animação de janela:</b> quem anima é a translação do conteúdo, tocada pela
     * GavetaDaSessao — o recorte na borda da janela dá o mesmo desenho de sempre, o painel
     * entrando pela direita enquanto o vídeo desliza.
     * <b>Criada na primeira abertura:</b> antes disso a folha fica no layout da Activity,
     * medida e invisível, que é o que mantém todos os findViewById do onCreate funcionando.
     */
    private void mostrarJanelaDaFolha() {
        if (folhaDaSessao == null || isFinishing()) {
            return;
        }
        if (janelaDaFolha == null) {
            android.view.ViewParent pai = folhaDaSessao.getParent();
            if (pai instanceof ViewGroup) {
                ((ViewGroup) pai).removeView(folhaDaSessao);
            }

            // Dialog, e nao PopupWindow, e a diferenca e de CLASSE de janela.
            //
            // O popup e uma janela-filha (TYPE_APPLICATION_PANEL): a superficie dele e
            // camada-filha da janela do app, IRMA da camada da SurfaceView. No aparelho do
            // dono o sintoma e exatamente ordenacao de camadas-filhas -- conteudo da mesma
            // janela sobre os limites da superficie nao compoe. O dialogo e uma janela
            // NAO-anexada (TYPE_APPLICATION), com camada propria acima da janela base
            // INTEIRA -- e e a unica superficie que comprovadamente renderiza sobre a
            // stream nas gravacoes daquele aparelho: o erro de conexao, a lista de teclas,
            // o menu legado.
            janelaDaFolha = new android.app.Dialog(this);
            janelaDaFolha.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            janelaDaFolha.setContentView(folhaDaSessao);
            janelaDaFolha.setCancelable(false);

            android.view.Window w = janelaDaFolha.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                lp.width = getResources().getDimensionPixelSize(R.dimen.vidro_sheet_width);
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                lp.windowAnimations = 0;
                // Quem anima e a translacao do conteudo, tocada pela GavetaDaSessao; o
                // recorte na borda da janela da o mesmo desenho de sempre.
                //
                // NOT_FOCUSABLE: teclado e gamepad continuam com a sessao, e o Voltar
                // continua caindo em decideVoltar. NOT_TOUCH_MODAL: toque fora da janela
                // atravessa para a Activity, onde a GavetaDaSessao ja consome o DOWN com a
                // gaveta aberta -- toque curto fecha, arrasto move. Sem DIM: o video ao
                // lado continua aceso, que e a razao de o painel ser estreito.
                lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                lp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                w.setAttributes(lp);
            }
        }
        if (!janelaDaFolha.isShowing()) {
            janelaDaFolha.show();
            anunciarSentinelaDoMenu();
        }
    }

    /** A sentinela só fala uma vez por processo. */
    private static boolean sentinelaJaFalou;

    /**
     * Em depuração, a primeira abertura do menu solta uma torrada dizendo que ele abriu.
     *
     * A torrada é janela do SISTEMA e renderiza em qualquer compositor — aparece em todas
     * as gravações do aparelho onde o menu ficou invisível. É o instrumento que separa os
     * três desfechos possíveis do próximo teste: menu visível com torrada (resolvido),
     * torrada sem menu (a gaveta abriu e até a janela de diálogo foi comida — o defeito
     * não é de classe de janela), e nada (a abertura nem aconteceu — o defeito é no
     * gesto). Sem ela, os dois últimos são indistinguíveis num vídeo.
     */
    private void anunciarSentinelaDoMenu() {
        if (sentinelaJaFalou || !BuildConfig.DEBUG) {
            return;
        }
        sentinelaJaFalou = true;
        VidroToast.show(this, getString(R.string.vidro_diag_menu_aberto), Toast.LENGTH_LONG);
    }

    /** Idempotente de propósito: aoAssentarFechada dispara em todo assentamento em zero. */
    private void esconderJanelaDaFolha() {
        if (janelaDaFolha != null && janelaDaFolha.isShowing()) {
            janelaDaFolha.dismiss();
        }
    }

    private void applySheetSafeArea(int safeRight, int safeBottom) {
        View sheet = folhaDaSessao != null ? folhaDaSessao : findViewById(R.id.vidroSheet);
        if (sheet == null) {
            return;
        }

        int base = getResources().getDimensionPixelSize(R.dimen.vidro_space_4);
        sheet.setPadding(base, base, base + safeRight, base + safeBottom);
    }

    private static ViewGroup.MarginLayoutParams vidroMarginParams(View view) {
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return null;
        }
        return (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    }

    public void toggleKeyboardController(){
        if (keyBoardController==null) {
            initKeyboardController();
            return;
        }
        keyBoardController.toggleVisibility();
    }

    public void toggleFullKeyboard() {
        if (isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleFullKeyboard();
            return;
        }
        if (keyBoardLayoutController == null) {
            initkeyBoardLayoutController();
            return;
        }
        keyBoardLayoutController.toggleVisibility();
    }

    //显示隐藏虚拟手柄控制器
    public void toggleVirtualController(){
        if (virtualController==null) {
            initVirtualController();
            prefConfig.onscreenController=true;
            return;
        }
        prefConfig.onscreenController= virtualController.switchShowHide() != 0;
    }

    /*
     * O que a folha de ajustes pergunta antes de desenhar os interruptores.
     *
     * Cada uma destas camadas é criada sob demanda e some por caminhos que não passam pela
     * folha — gesto de cinco dedos, atalho do teclado da capa, o botão da tela externa.
     * Enquanto elas só sabiam alternar, a única interface possível era uma lista de
     * "Toggle X", que manda alternar sem dizer o que é X agora. Perguntar é o que permite
     * o interruptor.
     */

    @Override
    public boolean isSpecialKeysVisible() {
        return keyBoardController != null && keyBoardController.shown;
    }

    @Override
    public boolean isOnScreenControllerVisible() {
        return virtualController != null && prefConfig.onscreenController;
    }

    /**
     * A forma corrente: acoplado ou solto. Nulo enquanto ninguém perguntou.
     *
     * Terceiro estado de propósito, e não um booleano com valor inicial: a primeira
     * aplicação precisa acontecer mesmo quando a resposta coincide com o padrão, ou a sessão
     * nasceria com os controles de dedo à mostra num tablet acoplado.
     */
    private Boolean formaAcoplada;

    /** O que a pessoa escolheu, guardado enquanto a capa está acoplada. */
    private boolean querControleNaTela;
    private boolean querTeclasEspeciais;
    private boolean querTecladoDesenhado;

    /**
     * O app tem DUAS FORMAS, não uma forma com ajustes.
     *
     * <p><b>Acoplado</b> — o tablet é um laptop, as mãos estão no teclado, e nada é desenhado
     * sobre o vídeo. O jogo ocupa a tela inteira o tempo todo, e tudo o que se precisa fazer
     * começa digitando: Ctrl+Alt+Shift+T levanta o campo, a seta desce para a paleta.
     *
     * <p><b>Solto</b> — as formas de dedo voltam exatamente como estavam.
     *
     * <p>Esta é a única decisão automática deste app que o aparelho acerta em 100% dos casos,
     * porque a pergunta "há um teclado físico no colo dele?" é binária e verificável. O resto
     * é consequência: quem tem teclado não precisa de teclado desenhado, quem tem trackpad não
     * precisa de controle na tela.
     *
     * <p><b>O que ela NÃO faz, e é o cuidado principal:</b> ela não decide por ninguém. O que
     * a pessoa escolheu é guardado ao acoplar e devolvido ao soltar — acoplar e desacoplar não
     * pode virar um jeito de o app religar o controle na tela que ela tinha desligado.
     *
     * <p><b>Ressalva de campo:</b> a detecção da capa por {@code Configuration} erra em alguns
     * aparelhos. Precisa ser medida no S8 Ultra acoplando e soltando no meio da sessão antes
     * de se confiar nela. Enquanto isso, a saída de emergência é a de sempre: a paleta e a
     * gaveta continuam alcançáveis nas duas formas.
     */
    private void aplicarFormaDaSessao() {
        boolean acoplada = hasHardwareKeyboard();
        if (formaAcoplada != null && formaAcoplada == acoplada) {
            return;
        }

        boolean primeira = formaAcoplada == null;
        formaAcoplada = acoplada;

        if (acoplada) {
            if (!primeira) {
                // Só guarda numa TRANSIÇÃO de solto para acoplado. Na primeira aplicação o
                // que está na tela ainda é o padrão de layout, não uma escolha de ninguém —
                // guardá-lo faria a primeira soltada da capa acender coisas que a pessoa
                // nunca ligou.
                querControleNaTela = virtualController != null && prefConfig.onscreenController;
                querTeclasEspeciais = keyBoardController != null && keyBoardController.shown;
                querTecladoDesenhado = keyBoardLayoutController != null
                        && keyBoardLayoutController.shown;
            }

            if (virtualController != null) {
                virtualController.hide();
            }
            if (keyBoardController != null) {
                keyBoardController.hide();
            }
            if (keyBoardLayoutController != null) {
                keyBoardLayoutController.hide();
            }
            return;
        }

        if (primeira) {
            // Solto desde o começo: nada a restaurar, o layout já nasceu como devia.
            return;
        }

        if (querControleNaTela && virtualController != null) {
            virtualController.show();
        }
        if (querTeclasEspeciais && keyBoardController != null) {
            keyBoardController.show();
        }
        if (querTecladoDesenhado && keyBoardLayoutController != null) {
            keyBoardLayoutController.show();
        }
    }

    /**
     * A orientação que a pessoa PEDIU, por cima da política.
     *
     * UNSPECIFIED enquanto ninguém pediu nada. "Girar a tela" era um no-op medido no
     * emulador: com allowPortraitSession a política escolhe FULL_USER (giro livre pelo
     * sensor), o pedido de paisagem disparava onConfigurationChanged, e a política era
     * reaplicada na linha seguinte — devolvendo a orientação ao sensor, que a punha de
     * volta como estava. O botão girava a tela por um quadro e o sensor desgirava.
     *
     * Um pedido explícito não é política: ele vale até a sessão acabar ou até o próximo
     * toque no mesmo botão, que alterna de volta.
     */
    private int orientacaoForcada = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    private void setPreferredOrientationForActivity() {
        // O pedido explícito vence a política — ver orientacaoForcada.
        if (orientacaoForcada != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            setRequestedOrientation(orientacaoForcada);
            return;
        }

        Display display = getActiveDisplay(Game.this, prefConfig);

        // For semi-square displays, we use more complex logic to determine which orientation to use (if any)
        if (PreferenceConfiguration.isSquarishScreen(display)) {
            int desiredOrientation = Configuration.ORIENTATION_UNDEFINED;

            // OSC doesn't properly support portrait displays, so don't use it in portrait mode by default
            if (prefConfig.onscreenController) {
                desiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
            }

            // For native resolution, we will lock the orientation to the one that matches the specified resolution
            if (PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height)) {
                if (displayWidth > displayHeight) {
                    desiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
                }
                else {
                    desiredOrientation = Configuration.ORIENTATION_PORTRAIT;
                }
            }

            if (desiredOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            }
            else if (desiredOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            }
            else {
                // If we don't have a reason to lock to portrait or landscape, allow any orientation
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            }
        }
        else if (prefConfig.allowPortraitSession && !prefConfig.onscreenController) {
            // Sessão livre para girar.
            //
            // Dá para fazer isto sem derrubar o stream porque a .Game declara
            // orientation|screenSize em configChanges: girar não recria a Activity, só
            // remede. O vídeo continua na proporção que o host negociou e passa a caber
            // em letterbox no retrato — que é o que torna útil digitar com o teclado
            // aberto embaixo dele.
            //
            // FULL_USER, e não SENSOR: respeita a trava de rotação do sistema. Quem
            // deixou o aparelho travado não é surpreendido.
            //
            // O controle virtual na tela fica de fora: os botões dele são posicionados
            // em coordenadas de paisagem e saem do lugar em retrato.
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
        else {
            // Lock to current orientation
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Set requested orientation for possible new screen size
        setPreferredOrientationForActivity();

        // Acoplar ou soltar a capa chega POR AQUI: `keyboard|keyboardHidden` está em
        // configChanges no manifesto justamente para isto. A forma acompanha o aparelho sem
        // ninguém pedir.
        aplicarFormaDaSessao();

        if (virtualController != null) {
            // Refresh layout of OSC for possible new screen size
            virtualController.refreshLayout();
        }

        if(keyBoardController != null){
            keyBoardController.refreshLayout();
        }

        if(keyBoardLayoutController != null){
            keyBoardLayoutController.refreshLayout();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                isHidingOverlays = true;

                if (virtualController != null) {
                    virtualController.hide();
                }

                if (keyBoardController != null && keyBoardController.shown) {
                    keyBoardController.hide(true);
                }

                if (keyBoardLayoutController!=null && keyBoardLayoutController.shown) {
                    keyBoardLayoutController.hide(true);
                }

                hideGameMenu();

                // A folha e a barra de composicao tambem fecham.
                //
                // Este bloco era uma lista escrita a mao, e listas escritas a mao esquecem:
                // minimizar com a folha aberta punha um painel de 352 dp sobre uma janela do
                // tamanho de um cartao. Fechar de verdade, e nao so esconder -- a folha tem
                // um veu que continua comendo os toques se ela ficar invisivel em vez de
                // fechada.
                if (vidroSession != null) {
                    vidroSession.closeSheet();
                }
                if (isComposeBarVisible()) {
                    // O rascunho não se perde: closeComposeBar guarda o texto em
                    // rascunhoDaBarra e a próxima abertura o devolve. (Este comentário já
                    // mentiu — o fechamento apagava tudo — e foi ele que denunciou o
                    // defeito quando o teste de fechar-e-reabrir voltou vazio.)
                    closeComposeBar();
                }

                // A gaveta fecha SECO, sem mola.
                //
                // A janela de PiP tem o tamanho de um cartao e nao ha dedo nela: um painel
                // de 352 dp deslizando para dentro de uma miniatura nao e transicao, e
                // deslocamento e a unica coisa que a gaveta guarda. Fechar sem animar
                // devolve a imagem inteira no mesmo quadro em que a janela encolhe.
                if (vidroGaveta != null) {
                    vidroGaveta.fecharSeco();
                }

                // O zoom volta a 1:1 enquanto durar o PiP.
                //
                // A janela de PiP tem outra geometria, e handleSurfaceChange reescala o pan
                // proporcionalmente a cada troca de superficie: entrar e sair acumulava
                // erro, e o que voltava era uma imagem ampliada e deslocada. Pior que o
                // erro: um recorte ampliado dentro de uma miniatura nao mostra nada util.
                // Guardamos o estado e devolvemos exatamente ele na volta.
                if (panZoomHandler != null && panZoomHandler.isZoomed()) {
                    zoomAntesDoPiP = panZoomHandler.getScaleFactor();
                    panAntesDoPiPX = panZoomHandler.getChildX();
                    panAntesDoPiPY = panZoomHandler.getChildY();
                    panZoomHandler.setInitialZoomAndPan(1f, 0f, 0f);
                    isPanZoomMode = false;
                }

                performanceOverlayView.setVisibility(View.GONE);
                notificationOverlayView.setVisibility(View.GONE);

                // O prazo nao corre em PiP: ali a imagem esta A VISTA, pequena mas a
                // vista, e derrubar seria matar exatamente o que a pessoa escolheu ver.
                prazoDaCasca().aoMudarPiP(true);

                // Disable sensors while in PiP mode
                controllerHandler.disableSensors();

                // Update GameManager state to indicate we're in PiP (still gaming, but interruptible)
                UiHelper.notifyStreamEnteringPiP(this);
            }
            else {
                isHidingOverlays = false;

                // Sair do PiP nao rearma sozinho: quem sabe se a casca esta a mostra e a
                // casca. Ver PrazoDaSessao.aoMudarPiP.
                prazoDaCasca().aoMudarPiP(false);

                // Restore overlays to previous state when leaving PiP

                if (virtualController != null) {
                    virtualController.show();
                }

                if (keyBoardController != null && keyBoardController.shown) {
                    keyBoardController.show();
                }

                if(keyBoardLayoutController!=null && keyBoardLayoutController.shown){
                    keyBoardLayoutController.show();
                }

                // A gaveta NAO volta, e nao ha o que restaurar: ela ja estava fechada
                // antes do PiP tambem. Fechada e o estado de repouso da sessao.

                // E o zoom volta ao que era antes de entrar, sem passar pela conta de
                // reescala que a troca de superficie faria.
                if (panZoomHandler != null && zoomAntesDoPiP > 1f) {
                    panZoomHandler.setInitialZoomAndPan(zoomAntesDoPiP, panAntesDoPiPX, panAntesDoPiPY);
                    isPanZoomMode = true;
                    zoomAntesDoPiP = 1f;
                }

                if (prefConfig.enablePerfOverlay) {
                    performanceOverlayView.setVisibility(View.VISIBLE);
                }

                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);

                // Enable sensors again after exiting PiP
                controllerHandler.enableSensors();

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private PictureInPictureParams getPictureInPictureParams(boolean autoEnter) {
        View view;
        Rect hint;
        if (prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FIT && streamContainer.getScaleX() == 1) {
            view = streamContainer;
        } else {
            view = (View)rootView;
        }

        int[] viewLocation = new int[2];

        view.getLocationOnScreen(viewLocation);

        int left = viewLocation[0];
        int top = viewLocation[1];
        int width = view.getWidth();
        int height = view.getHeight();
        Rational aspectRatio = new Rational(width, height);
        hint = new Rect(left, top, left + width, top + height);

        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .setSourceRectHint(hint);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter);
            builder.setSeamlessResizeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName);
                if (pcName != null) {
                    builder.setSubtitle(pcName);
                }
            }
            else if (pcName != null) {
                builder.setTitle(pcName);
            }
        }

        return builder.build();
    }

    public void updatePipAutoEnter() {
        if (!prefConfig.enablePip || isOnExternalDisplay()) {
            return;
        }

        boolean autoEnter = connected && suppressPipRefCount == 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(getPictureInPictureParams(autoEnter));
        }
        else {
            autoEnterPip = autoEnter;
        }
    }

    public void setMetaKeyCaptureState(boolean enabled) {
        // This uses custom APIs present on some Samsung devices to allow capture of
        // meta key events while streaming.
        try {
            Class<?> semWindowManager = Class.forName("com.samsung.android.view.SemWindowManager");
            Method getInstanceMethod = semWindowManager.getMethod("getInstance");
            Object manager = getInstanceMethod.invoke(null);

            if (manager != null) {
                Class<?>[] parameterTypes = new Class<?>[2];
                parameterTypes[0] = ComponentName.class;
                parameterTypes[1] = boolean.class;
                Method requestMetaKeyEventMethod = semWindowManager.getDeclaredMethod("requestMetaKeyEvent", parameterTypes);
                requestMetaKeyEventMethod.invoke(manager, this.getComponentName(), enabled);
            }
            else {
                LimeLog.warning("SemWindowManager.getInstance() returned null");
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // PiP is only supported on Oreo and later, and we don't need to manually enter PiP on
        // Android S and later. On Android R, we will use onPictureInPictureRequested() instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (autoEnterPip && !isOnExternalDisplay()) {
                try {
                    // This has thrown all sorts of weird exceptions on Samsung devices
                    // running Oreo. Just eat them and close gracefully on leave, rather
                    // than crashing.
                    enterPictureInPictureMode(getPictureInPictureParams(false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        // Enter PiP when requested unless we're on Android 12 which supports auto-enter.
        if (autoEnterPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureMode(getPictureInPictureParams(false));
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // We can't guarantee the state of modifiers keys which may have
        // lifted while focus was not on us. Clear the modifier state.
        soltarEstadoDeTeclado();

        // With Android native pointer capture, capture is lost when focus is lost,
        // so it must be requested again when focus is regained.
        inputCaptureProvider.onWindowFocusChanged(hasFocus);
    }

    private boolean isRefreshRateEqualMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                refreshRate <= prefConfig.fps + 3;
    }

    private boolean isRefreshRateGoodMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                Math.round(refreshRate) % prefConfig.fps <= 3;
    }

    private boolean shouldIgnoreInsetsForResolution(int width, int height) {
        // Never ignore insets for non-native resolutions
        if (!PreferenceConfiguration.isNativeResolution(width, height)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display display = getActiveDisplay(Game.this, prefConfig);
            for (Display.Mode candidate : display.getSupportedModes()) {
                // Ignore insets if this is an exact match for the display resolution
                if ((width == candidate.getPhysicalWidth() && height == candidate.getPhysicalHeight()) ||
                        (height == candidate.getPhysicalWidth() && width == candidate.getPhysicalHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean mayReduceRefreshRate() {
        return prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS ||
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS ||
                (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED && prefConfig.reduceRefreshRate);
    }

    public boolean isOnExternalDisplay() {
        return onExternelDisplay;
    }

    private float prepareDisplayForRendering(Display currentDisplay) {
        WindowManager.LayoutParams windowLayoutParams = getWindow().getAttributes();
        float displayRefreshRate;

        // On M, we can explicitly set the optimal display mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode bestMode = currentDisplay.getMode();
            boolean isNativeResolutionStream = PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height);
            boolean refreshRateIsGood = isRefreshRateGoodMatch(bestMode.getRefreshRate());
            boolean refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.getRefreshRate());

            LimeLog.info("Current display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            for (Display.Mode candidate : currentDisplay.getSupportedModes()) {
                boolean refreshRateReduced = candidate.getRefreshRate() < bestMode.getRefreshRate();
                boolean resolutionReduced = candidate.getPhysicalWidth() < bestMode.getPhysicalWidth() ||
                        candidate.getPhysicalHeight() < bestMode.getPhysicalHeight();
                boolean resolutionFitsStream = candidate.getPhysicalWidth() >= prefConfig.width &&
                        candidate.getPhysicalHeight() >= prefConfig.height;

                LimeLog.info("Examining display mode: "+candidate.getPhysicalWidth()+"x"+
                        candidate.getPhysicalHeight()+"x"+candidate.getRefreshRate());

                if (candidate.getPhysicalWidth() > 4096 && prefConfig.width <= 4096) {
                    // Avoid resolutions options above 4K to be safe
                    continue;
                }

                // On non-4K streams, we force the resolution to never change unless it's above
                // 60 FPS, which may require a resolution reduction due to HDMI bandwidth limitations,
                // or it's a native resolution stream.
                if (prefConfig.width < 3840 && prefConfig.fps <= 60 && !isNativeResolutionStream) {
                    if (currentDisplay.getMode().getPhysicalWidth() != candidate.getPhysicalWidth() ||
                            currentDisplay.getMode().getPhysicalHeight() != candidate.getPhysicalHeight()) {
                        continue;
                    }
                }

                // Make sure the resolution doesn't regress unless if it's over 60 FPS
                // where we may need to reduce resolution to achieve the desired refresh rate.
                if (resolutionReduced && !(prefConfig.fps > 60 && resolutionFitsStream)) {
                    continue;
                }

                if (mayReduceRefreshRate() && refreshRateIsEqual && !isRefreshRateEqualMatch(candidate.getRefreshRate())) {
                    // If we had an equal refresh rate and this one is not, skip it. In min latency
                    // mode, we want to always prefer the highest frame rate even though it may cause
                    // microstuttering.
                    continue;
                }
                else if (refreshRateIsGood) {
                    // We've already got a good match, so if this one isn't also good, it's not
                    // worth considering at all.
                    if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                        continue;
                    }

                    if (mayReduceRefreshRate()) {
                        // User asked for the lowest possible refresh rate, so don't raise it if we
                        // have a good match already
                        if (candidate.getRefreshRate() > bestMode.getRefreshRate()) {
                            continue;
                        }
                    }
                    else {
                        // User asked for the highest possible refresh rate, so don't reduce it if we
                        // have a good match already
                        if (refreshRateReduced) {
                            continue;
                        }
                    }
                }
                else if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                    // We didn't have a good match and this match isn't good either, so just don't
                    // reduce the refresh rate.
                    if (refreshRateReduced) {
                        continue;
                    }
                } else {
                    // We didn't have a good match and this match is good. Prefer this refresh rate
                    // even if it reduces the refresh rate. Lowering the refresh rate can be beneficial
                    // when streaming a 60 FPS stream on a 90 Hz device. We want to select 60 Hz to
                    // match the frame rate even if the active display mode is 90 Hz.
                }

                bestMode = candidate;
                refreshRateIsGood = isRefreshRateGoodMatch(candidate.getRefreshRate());
                refreshRateIsEqual = isRefreshRateEqualMatch(candidate.getRefreshRate());
            }

            LimeLog.info("Best display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            // Only apply new window layout parameters if we've actually changed the display mode
            if (currentDisplay.getMode().getModeId() != bestMode.getModeId()) {
                // If we only changed refresh rate and we're on an OS that supports Surface.setFrameRate()
                // use that instead of using preferredDisplayModeId to avoid the possibility of triggering
                // bugs that can cause the system to switch from 4K60 to 4K24 on Chromecast 4K.
                if (prefConfig.enforceDisplayMode ||
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        currentDisplay.getMode().getPhysicalWidth() != bestMode.getPhysicalWidth() ||
                        currentDisplay.getMode().getPhysicalHeight() != bestMode.getPhysicalHeight()) {
                    // Apply the display mode change
                    windowLayoutParams.preferredDisplayModeId = bestMode.getModeId();
                    getWindow().setAttributes(windowLayoutParams);
                }
                else {
                    LimeLog.info("Using setFrameRate() instead of preferredDisplayModeId due to matching resolution");
                }
            }
            else {
                LimeLog.info("Current display mode is already the best display mode");
            }

            displayRefreshRate = bestMode.getRefreshRate();
        }
        // On L, we can at least tell the OS that we want a refresh rate
        else {
            float bestRefreshRate = currentDisplay.getRefreshRate();
            for (float candidate : currentDisplay.getSupportedRefreshRates()) {
                LimeLog.info("Examining refresh rate: "+candidate);

                if (candidate > bestRefreshRate) {
                    // Ensure the frame rate stays around 60 Hz for <= 60 FPS streams
                    if (prefConfig.fps <= 60) {
                        if (candidate >= 63) {
                            continue;
                        }
                    }

                    bestRefreshRate = candidate;
                }
            }

            LimeLog.info("Selected refresh rate: "+bestRefreshRate);
            windowLayoutParams.preferredRefreshRate = bestRefreshRate;
            displayRefreshRate = bestRefreshRate;

            // Apply the refresh rate change
            getWindow().setAttributes(windowLayoutParams);
        }

        // Until Marshmallow, we can't ask for a 4K display mode, so we'll
        // need to hint the OS to provide one.
        boolean aspectRatioMatch = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // We'll calculate whether we need to scale by aspect ratio. If not, we'll use
            // setFixedSize so we can handle 4K properly. The only known devices that have
            // >= 4K screens have exactly 4K screens, so we'll be able to hit this good path
            // on these devices. On Marshmallow, we can start changing to 4K manually but no
            // 4K devices run 6.0 at the moment.
            Point screenSize = new Point(0, 0);
            currentDisplay.getSize(screenSize);

            double screenAspectRatio = ((double)screenSize.y) / screenSize.x;
            double streamAspectRatio = ((double)displayHeight) / displayWidth;
            if (Math.abs(screenAspectRatio - streamAspectRatio) < 0.001|| isOnExternalDisplay()) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        // A proporção é informada SEMPRE, mesmo quando ela bate com a da tela.
        //
        // O atalho `|| aspectRatioMatch` parecia inofensivo: se a tela e o stream têm a
        // mesma forma, fit e fill dão o mesmo resultado, então para que calcular? Só que
        // ele não desliga a conta — ele deixa `desiredAspectRatio` em zero, e com zero o
        // onMeasure devolve ao super, que dá ao SurfaceView a caixa inteira.
        //
        // Enquanto a caixa é a tela cheia isso coincide com o certo. No instante em que
        // alguma coisa encolhe a caixa — o teclado abrindo em RESIZE, que põe padding do
        // tamanho do IME — a coincidência acaba: a superfície recebe uma caixa baixa e
        // larga e o vídeo ESTICA, porque não há mais proporção nenhuma guardada para
        // defendê-lo. Era o caso do relato: em pé, com o teclado acionado.
        //
        // Informar sempre não custa nada quando as formas batem, e é o que segura a forma
        // quando elas deixam de bater.
        if (prefConfig.videoScaleMode != PreferenceConfiguration.ScaleMode.STRETCH) {
            streamContainer.setDesiredAspectRatio((double)displayWidth / (double)displayHeight);
            streamContainer.setFillDisplay(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FILL);
        }

        // Set the desired refresh rate that will get passed into setFrameRate() later
        desiredRefreshRate = displayRefreshRate;

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || isOnExternalDisplay()) {// TVs may take a few moments to switch refresh rates, and we can probably assume
            // it will be eventually activated.
            // external displays cant be compared with displaymanager currents display refreshrate
            // TODO: Improve this
            return displayRefreshRate;
        }
        else {
            // Use the lower of the current refresh rate and the selected refresh rate.
            // The preferred refresh rate may not actually be applied (ex: Battery Saver mode).
            return Math.min(currentDisplay.getRefreshRate(), displayRefreshRate);
        }
    }

    @SuppressLint("InlinedApi")
    private final Runnable hideSystemUi = new Runnable() {
        @Override
        public void run() {
            // TODO: Do we want to use WindowInsetsController here on R+ instead of
            // SYSTEM_UI_FLAG_IMMERSIVE_STICKY? They seem to do the same thing as of S...

            // In multi-window mode on N+, we need to drop our layout flags or we'll
            // be drawing underneath the system UI.
            if (!prefConfig.fullScreen || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())) {
                Game.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            else {
                // Use immersive mode
                Game.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    };

    private void hideSystemUi(int delay) {
        // While the on-screen keyboard is up, re-asserting immersive mode fights the IME:
        // the flags come back every couple of seconds and undo the layout we just made room
        // with. Leave the system UI alone until the keyboard goes away.
        if (imeVisible && prefConfig != null
                && prefConfig.imeDisplayMode != PreferenceConfiguration.ImeDisplayMode.OVERLAY) {
            return;
        }

        Handler h = getWindow().getDecorView().getHandler();
        if (h != null) {
            h.removeCallbacks(hideSystemUi);
            h.postDelayed(hideSystemUi, delay);
        }
    }

    /**
     * Sends a block of text composed locally, rather than key by key.
     *
     * Note that "in one go" applies to the user experience, not to the wire: moonlight-common-c
     * deliberately splits UTF-8 payloads into one packet per code point so that none of them
     * straddles a packet boundary. Long text therefore takes a moment to arrive.
     *
     * No toast. A system toast reading "Sending 4 characters" used to pop over the video on
     * every send — English regardless of locale, in the system's own shape, telling the user
     * something they had just done themselves. The compose bar emptying is the confirmation,
     * and the thesis is explicit that the app stays quiet while it works.
     */
    public void sendTextBlock(String text) {
        if (text == null || text.isEmpty() || conn == null) {
            return;
        }

        textInputPump.offerText(text);
    }

    // ---------------------------------------------------------------- IME-aware layout

    /** True while the on-screen keyboard is covering part of the window. */
    private boolean imeVisible = false;

    /**
     * Makes the stream yield space to the on-screen keyboard.
     *
     * The activity declares adjustResize, but that alone does nothing here: a window with
     * FLAG_FULLSCREEN and immersive flags already spans the display, so the system has
     * nothing to resize and the IME simply draws on top. Opting out of the automatic fitting
     * and handling insets ourselves is what actually frees up the space.
     *
     * StreamContainer.onMeasure() already honours the desired aspect ratio, so shrinking the
     * available area is enough — the video rescales itself.
     */
    private void setupImeInsetsHandling() {
        // Sem porta aqui, de propósito.
        //
        // Havia um `if (imeDisplayMode == OVERLAY) return;` no topo. Ele existia para o
        // modo "cobrir" não empurrar o vídeo — mas não pulava só o reflow: pulava a
        // INSTALAÇÃO do ouvinte. E a barra de composição depende do mesmo ouvinte para
        // subir junto com o teclado, e `imeVisible` depende dele para suspender o modo
        // imersivo.
        //
        // Resultado: escolher "cobrir" em Ajustes matava a barra de composição em silêncio
        // — e no S22 Ultra ela é a única forma de digitar. A preferência se chama "modo de
        // exibição do teclado" e não sugere nenhuma relação com o campo de texto.
        //
        // Quem decide o que fazer com o vídeo é applyImeOffset, onde o caso OVERLAY já é um
        // no-op honesto. Aqui só se escuta.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(streamContainer, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // The IME inset already includes the navigation bar area it sits on top of.
            // Subtracting it keeps us from shrinking the stream twice over.
            int streamOverlap = Math.max(0, imeBottom - navBottom);
            lastNavBottom = navBottom;

            boolean nowVisible = imeBottom > 0;
            if (nowVisible != imeVisible) {
                imeVisible = nowVisible;
                if (!nowVisible) {
                    // Restore immersive mode once the keyboard is gone.
                    hideSystemUi(500);
                }
            }

            // Os dois consumidores medem a partir de origens diferentes, e usar um número
            // só deixava a barra flutuando uma barra de navegação acima do teclado. O vídeo
            // desconta a navegação porque ela já não era área útil dele; a barra de
            // composição tem layout_gravity=bottom numa janela com decorFitsSystemWindows
            // desligado, então o zero dela é o fundo da JANELA e o topo do IME está a
            // imeBottom dali — sem desconto nenhum.
            applyImeOffset(streamOverlap);
            updateComposeBar(imeBottom);
            return insets;
        });
    }

    /** Último recuo pedido pelo IME, para poder refazer a conta quando a barra cresce. */
    private int lastImeOverlap = 0;
    /** Última barra de navegação medida, pela mesma razão. */
    private int lastNavBottom = 0;

    /**
     * Quanto a barra de composição tira do vídeo.
     *
     * O recuo do teclado sozinho não bastava: o vídeo era ajustado à faixa acima do IME e
     * a barra era desenhada POR CIMA dessa faixa, tapando os 40% de baixo da imagem — e
     * subindo mais a cada linha que o texto quebrava.
     *
     * Vem somada a barra de navegação porque as duas contas partem de origens diferentes.
     * O recuo do vídeo já desconta a navegação, de propósito: aquela faixa nunca foi área
     * útil dele. A barra de composição, não — ela se ancora no fundo cru da janela e sobe
     * imeBottom a partir dali. Sem devolver a navegação à conta, o topo da barra ficava
     * uma barra de navegação abaixo da base do vídeo, e voltava a comer essa fatia.
     */
    private int composeBarOverlap() {
        View bar = findViewById(R.id.vidroComposeBar);
        if (bar == null || bar.getVisibility() != View.VISIBLE) {
            return 0;
        }
        return bar.getHeight() + lastNavBottom;
    }

    private void applyImeOffset(int overlap) {
        if (streamContainer == null) {
            return;
        }

        lastImeOverlap = overlap;
        int total = overlap + composeBarOverlap();

        switch (prefConfig.imeDisplayMode) {
            case RESIZE:
                // O padding tira altura da conta e o StreamContainer refaz o letterbox
                // dentro do que sobrou — o vídeo encolhe mantendo a proporção. A gravidade
                // TOP encosta ele no alto em vez de centrar no espaço restante, que é o
                // comportamento do AnyDesk: o teclado empurra a imagem para cima.
                streamContainer.setTranslationY(0);
                // E VOLTA AO CENTRO quando nada mais é tomado. O comentário acima sempre
                // prometeu isso, mas a gravidade era fixada em TOP na primeira vez que o
                // teclado subia e não voltava nunca — a superfície ficava ancorada no alto
                // pelo resto da sessão, e qualquer sobra de altura caía toda embaixo.
                setStreamGravity(total > 0
                        ? (Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                        : Gravity.CENTER);
                streamContainer.setPadding(0, 0, 0, total);
                break;

            case PAN:
                // Keep the video at its original size and centre it in what is left visible,
                // which means moving it up by half of what the keyboard took.
                setStreamGravity(Gravity.CENTER);
                streamContainer.setPadding(0, 0, 0, 0);
                streamContainer.setTranslationY(-total / 2f);
                break;

            case OVERLAY:
            default:
                break;
        }
    }

    /**
     * Onde o vídeo encosta dentro do container.
     *
     * Com o teclado fechado ele fica centrado; com o teclado aberto sobe para o topo, para
     * que o espaço que o IME não ocupou fique todo acima dele em vez de metade em cima e
     * metade embaixo.
     */
    private void setStreamGravity(int gravity) {
        if (streamContainer == null) {
            return;
        }

        // A superfície é filha do container e não tem campo próprio aqui; pegamos pela
        // árvore, que é como o resto de Game.java já a encontra.
        View surface = null;
        for (int i = 0; i < streamContainer.getChildCount(); i++) {
            if (streamContainer.getChildAt(i) instanceof SurfaceView) {
                surface = streamContainer.getChildAt(i);
                break;
            }
        }

        if (surface == null) {
            return;
        }

        ViewGroup.LayoutParams params = surface.getLayoutParams();
        if (!(params instanceof FrameLayout.LayoutParams)) {
            return;
        }

        FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams)params;
        if (frameParams.gravity != gravity) {
            frameParams.gravity = gravity;
            surface.setLayoutParams(frameParams);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);

        // In multi-window, we don't want to use the full-screen layout
        // flag. It will cause us to collide with the system UI.
        // This function will also be called for PiP so we can cover
        // that case here too.
        if (isInMultiWindowMode) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decoderRenderer.notifyVideoBackground();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decoderRenderer.notifyVideoForeground();
        }

        // Correct the system UI visibility flags
        hideSystemUi(50);
    }

    @Override
    protected void onDestroy() {
        // A janela da folha é presa ao token desta Activity: morrer sem dispensá-la é
        // WindowLeaked no log e janela órfã na tela.
        esconderJanelaDaFolha();
        if (moque != null) {
            moque.parar();
            moque = null;
        }

        super.onDestroy();

        instance = null;
        timerHandler.removeCallbacksAndMessages(null);

        // O pump some junto com a sessão. Ele guarda texto ainda por sair e ações de fim
        // de dreno — um aviso de "enviado", um Enter para o host —, e nada disso pode
        // acordar depois que a conexão já foi embora.
        textInputPump.clear();
        commitTextHandler.removeCallbacksAndMessages(null);

        if (prefConfig.enableFullExDisplay) handleDisplayRemoved();

        if (controllerHandler != null) {
            controllerHandler.destroy();
        }
        if (keyboardTranslator != null) {
            InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            inputManager.unregisterInputDeviceListener(keyboardTranslator);
        }

        if (lowLatencyWifiLock != null) {
            lowLatencyWifiLock.release();
        }
        if (highPerfWifiLock != null) {
            highPerfWifiLock.release();
        }

        // Save zoom/pan before other cleanup
        if (prefConfig != null && prefConfig.rememberZoomPan && panZoomHandler != null) {
            SharedPreferences basePrefs = PreferenceManager.getDefaultSharedPreferences(this);
            basePrefs.edit()
                    .putFloat("number_zoom_scale", panZoomHandler.getScaleFactor())
                    .putFloat("number_pan_offset_x", panZoomHandler.getChildX())
                    .putFloat("number_pan_offset_y", panZoomHandler.getChildY())
                    .apply();
        }

        if (connectedToUsbDriverService) {
            // Unbind from the discovery service
            unbindService(usbDriverServiceConnection);
        }

        // Destroy the capture provider
        inputCaptureProvider.destroy();
        streamContainer.onDestroy();
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            // Stop any further input device notifications before we lose focus (and pointer capture)
            if (controllerHandler != null) {
                controllerHandler.stop();
            }

            // Ungrab input to prevent further input device notifications
            setInputGrabState(false);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (virtualController != null) {
            virtualController.hide();
        }
        if (keyBoardController != null) {
            keyBoardController.hide();
        }

        if(keyBoardLayoutController!=null){
            keyBoardLayoutController.hide();
        }

        if (conn != null) {
            int videoFormat = decoderRenderer.getActiveVideoFormat();

            displayedFailureDialog = true;
            stopConnection();
            String message = null;
            String selectedVideoFormat = "";

            int averageEndToEndLat = decoderRenderer.getAverageEndToEndLatency();
            int averageDecoderLat = decoderRenderer.getAverageDecoderLatency();

            if (averageEndToEndLat > 0) {
                message = getResources().getString(R.string.conn_client_latency) + " " + averageEndToEndLat + " ms";
                if (averageDecoderLat > 0) {
                    message += " (" + getResources().getString(R.string.conn_client_latency_hw) + " " + averageDecoderLat + " ms)";
                }
            } else if (averageDecoderLat > 0) {
                message = getResources().getString(R.string.conn_hardware_latency) + " " + averageDecoderLat + " ms";
            }

            // Add the video codec to the post-stream toast
            selectedVideoFormat += " [";

            if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H264) != 0) {
                selectedVideoFormat += "H.264";
            } else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H265) != 0) {
                selectedVideoFormat += "HEVC";
            } else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_AV1) != 0) {
                selectedVideoFormat += "AV1";
            }
            else {
                selectedVideoFormat += "UNKNOWN";
            }

            if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_10BIT) != 0) {
                selectedVideoFormat += " HDR";
            }

            selectedVideoFormat += "]";

            if (message != null) {
                message += selectedVideoFormat;
            }

            if (message != null) {
                if (prefConfig.enableLatencyToast) {
                    VidroToast.show(this, message, Toast.LENGTH_LONG);
                }
            }

            // Clear the tombstone count if we terminated normally
            if (!reportedCrash && tombstonePrefs.getInt("CrashCount", 0) != 0) {
                tombstonePrefs.edit()
                        .putInt("CrashCount", 0)
                        .putInt("LastNotifiedCrashCount", 0)
                        .apply();
            }
            if(prefConfig.enablePerfLogging && decoderRenderer.performanceWasTracked()) {
                new PerformanceDataTracker().savePerformanceStatistics(
                        getBaseContext(),
                        Build.MODEL,
                        Build.VERSION.SDK_INT + "",
                        BuildConfig.VERSION_NAME,
                        selectedVideoFormat,
                        decoderRenderer.getMinDecoderLatency(),
                        decoderRenderer.getMinDecoderLatencyFullLog(),
                        String.valueOf((prefConfig.bitrate / 1000)),
                        displayWidth + "x" + displayHeight,
                        prefConfig.fps + " hz",
                        decoderRenderer.getAverageDecoderLatency() + " ms",
                        PreferenceConfiguration.getSelectedFramePacingName(getBaseContext()),
                        formatCurrentTime(System.currentTimeMillis())
                );
            }

        }

        finish();
    }

    public static String formatCurrentTime(long currentTimeMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date(currentTimeMillis);
        return dateFormat.format(date);
    }

    private void setInputGrabState(boolean grab) {
        // Grab/ungrab the mouse cursor
        if (grab) {
            inputCaptureProvider.enableCapture();

            // Enabling capture may hide the cursor again, so
            // we will need to show it again.
            if (cursorVisible) {
                inputCaptureProvider.showCursor();
            }
        }
        else {
            inputCaptureProvider.disableCapture();
        }

        // Grab/ungrab system keyboard shortcuts
        setMetaKeyCaptureState(grab);

        grabbedInput = grab;

        // Entrada solta precisa DIZER que está solta.
        //
        // Ctrl+Alt+Shift+Z desliga teclado, trackpad e toque — tudo para de chegar ao PC —
        // e o vídeo continua rodando normalmente. Nada na tela mudava. Quem não sabe do
        // acorde (ou o apertou sem querer, que é fácil com a capa) conclui "o teclado bugou
        // de novo" e derruba a sessão para consertar. É uma das causas do "às vezes
        // funciona, às vezes não".
        //
        // A faixa do topo já existe e já é a superfície de aviso da sessão. Ela nomeia o
        // acorde, porque quem está nesse estado precisa da saída, não do diagnóstico.
        if (notificationOverlayView != null) {
            if (!grab) {
                notificationOverlayView.setText(getString(R.string.vidro_entrada_solta));
                requestedNotificationOverlayVisibility = View.VISIBLE;
            }
            else {
                requestedNotificationOverlayVisibility = View.GONE;
            }
            if (!isHidingOverlays) {
                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);
            }
        }
    }

    private final Runnable toggleGrab = new Runnable() {
        @Override
        public void run() {
            setInputGrabState(!grabbedInput);
        }
    };

    // Returns true if the key stroke was consumed
    private boolean handleSpecialKeys(int androidKeyCode, boolean down) {
        int modifierMask = 0;
        int nonModifierKeyCode = KeyEvent.KEYCODE_UNKNOWN;

        if (androidKeyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
                androidKeyCode == KeyEvent.KEYCODE_CTRL_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_CTRL;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_SHIFT_LEFT ||
                androidKeyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_SHIFT;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_ALT_LEFT ||
                androidKeyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_ALT;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_META_LEFT ||
                androidKeyCode == KeyEvent.KEYCODE_META_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_META;
        }
        else {
            nonModifierKeyCode = androidKeyCode;
        }

        if (down) {
            this.modifierFlags |= modifierMask;
        }
        else {
            this.modifierFlags &= ~modifierMask;
        }

        // Handle the special combos on the key up
        if (waitingForAllModifiersUp || specialKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            if (specialKeyCode == androidKeyCode) {
                // If this is a key up for the special key itself, eat that because the host never saw the original key down
                return true;
            }
            else if (modifierFlags != 0) {
                // Teto de tempo na espera.
                //
                // Este ramo engole todo key down enquanto espera os modificadores subirem. Se
                // um unico KEY_UP se perder -- e ele se perde quando o acorde muda o foco da
                // janela -- a condicao de saida nunca chega e NENHUMA tecla mais atravessa
                // para o PC. O teclado fica morto pelo resto da sessao, sem nada na tela
                // explicando, e a unica saida e derrubar e reconectar.
                //
                // Passados tres segundos, desistimos do acorde: e muito mais tempo do que
                // qualquer pessoa leva para soltar tres teclas, e muito menos do que uma
                // sessao inteira sem teclado.
                if (SystemClock.uptimeMillis() - inicioDaEsperaDeAcorde > ESPERA_MAXIMA_DE_ACORDE_MS) {
                    soltarEstadoDeTeclado();
                    return false;
                }
                return down;
            }
            else {
                // When all modifiers are up, perform the special action
                switch (specialKeyCode) {
                    // Toggle input grab
                    case KeyEvent.KEYCODE_Z:
                        Handler h = getWindow().getDecorView().getHandler();
                        if (h != null) {
                            h.postDelayed(toggleGrab, 250);
                        }
                        break;

                    // Quit
                    case KeyEvent.KEYCODE_Q:
                        finish();
                        break;

                    // Toggle cursor visibility
                    case KeyEvent.KEYCODE_C:
                        if (!grabbedInput) {
                            inputCaptureProvider.enableCapture();
                            grabbedInput = true;
                        }
                        cursorVisible = !cursorVisible;
                        if (cursorVisible) {
                            inputCaptureProvider.showCursor();
                        } else {
                            inputCaptureProvider.hideCursor();
                        }
                        break;

                    // Abre a barra de composição sem tirar as mãos da capa. O caminho até
                    // ela era achar o punho na tela, abrir a ilha e tocar num bloco — três
                    // gestos de dedo para chegar a um campo de texto que só existe para
                    // quem está digitando.
                    case KeyEvent.KEYCODE_T:
                        openComposeBar();
                        break;

                    // O painel da sessao, sem tirar as maos da capa.
                    //
                    // Ele concentra modo do mouse, zoom, cursor, as camadas de tela e as
                    // acoes -- um acorde so resolve tudo. E este e o caminho que NAO depende
                    // da borda direita: com a capa acoplada nao ha dedo na tela, e mesmo com
                    // dedo o gesto de sistema pode disputar a borda abaixo da API 29.
                    case KeyEvent.KEYCODE_A:
                        if (vidroGaveta != null) {
                            vidroGaveta.alternar();
                        }
                        break;

                    // O menu de comandos da sessao.
                    case KeyEvent.KEYCODE_M:
                        showGameMenu(null);
                        break;

                    default:
                        break;
                }

                // Reset special key state
                specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
                waitingForAllModifiersUp = false;
            }
        }
        // Check if Ctrl+Alt+Shift is down when a non-modifier key is pressed
        else if ((modifierFlags & (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT | KeyboardPacket.MODIFIER_SHIFT)) ==
                (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT | KeyboardPacket.MODIFIER_SHIFT) &&
                (down && nonModifierKeyCode != KeyEvent.KEYCODE_UNKNOWN)) {
            switch (androidKeyCode) {
                case KeyEvent.KEYCODE_Z:
                // Ctrl+Alt+Shift+Q NAO entra mais aqui.
                //
                // Ele encerrava a sessao do cliente na hora, sem confirmacao -- e uma string
                // do proprio app anuncia esse mesmo acorde como acao do PC ("encerrar
                // transmissao no PC"). Duas coisas diferentes com o mesmo nome, e a que
                // acontecia era a destrutiva. Quem quer sair tem duas saidas visiveis, na
                // ilha e no menu; o acorde volta a atravessar para o host, como esta escrito.
                case KeyEvent.KEYCODE_C:
                case KeyEvent.KEYCODE_T:
                case KeyEvent.KEYCODE_H:
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_M:
                    // Remember that a special key combo was activated, so we can consume all key
                    // events until the modifiers come up
                    specialKeyCode = androidKeyCode;
                    waitingForAllModifiersUp = true;
                    inicioDaEsperaDeAcorde = SystemClock.uptimeMillis();
                    return true;

                default:
                    // This isn't a special combo that we consume on the client side
                    return false;
            }
        }

        // Not a special combo
        return false;
    }

    // We cannot simply use modifierFlags for all key event processing, because
    // some IMEs will not generate real key events for pressing Shift. Instead
    // they will simply send key events with isShiftPressed() returning true,
    // and we will need to send the modifier flag ourselves.
    private byte getModifierState(KeyEvent event) {
        // Start with the global modifier state to ensure we cover the case
        // detailed in https://github.com/moonlight-stream/moonlight-android/issues/840
        byte modifier = getModifierState();
        if (event.isShiftPressed()) {
            modifier |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if (event.isCtrlPressed()) {
            modifier |= KeyboardPacket.MODIFIER_CTRL;
        }
        if (event.isAltPressed()) {
            modifier |= KeyboardPacket.MODIFIER_ALT;
        }
        if (event.isMetaPressed()) {
            modifier |= KeyboardPacket.MODIFIER_META;
        }
        return modifier;
    }

    private byte getModifierState() {
        return (byte) modifierFlags;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return handleKeyDown(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean handleKeyDown(KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        int deviceId = event.getDeviceId();
        if (prefConfig.ignoreSynthEvents && deviceId <= 0) {
            return false;
        }

        // Handle a synthetic back button event that some Android OS versions
        // create as a result of a right-click. This event WILL repeat if
        // the right mouse button is held down, so we ignore those.
        int eventSource = event.getSource();
        if ((eventSource == InputDevice.SOURCE_MOUSE ||
                eventSource == InputDevice.SOURCE_MOUSE_RELATIVE) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            // Send the right mouse button event if mouse back and forward
            // are disabled. If they are enabled, handleMotionEvent() will take
            // care of this.
            if (!prefConfig.mouseNavButtons) {
                conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
            }

            // Always return true, otherwise the back press will be propagated
            // up to the parent and finish the activity.
            return true;
        }

        boolean handled = false;

        if (ControllerHandler.isGameControllerDevice(event.getDevice())) {
            // Always try the controller handler first, unless it's an alphanumeric keyboard device.
            // Otherwise, controller handler will eat keyboard d-pad events.
            handled = controllerHandler.handleButtonDown(event);
        }

        // Try the keyboard handler if it wasn't handled as a game controller
        if (!handled) {
            // Let this method take duplicate key down events
            if (handleSpecialKeys(event.getKeyCode(), true)) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            // We'll send it as a raw key event if we have a key mapping, otherwise we'll send it
            // as UTF-8 text (if it's a printable character).
            short translated = keyboardTranslator.translate(event.getKeyCode(), event.getScanCode(), deviceId);
            if (translated == 0) {
                if (prefConfig.backAsMeta && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    translated = 0x5b; // Meta key
                } else {
                    // Make sure it has a valid Unicode representation and it's not a dead character
                    // (which we don't support). If those are true, we can send it as UTF-8 text.
                    //
                    // NB: We need to be sure this happens before the getRepeatCount() check because
                    // UTF-8 events don't auto-repeat on the host side.
                    int unicodeChar = event.getUnicodeChar();
                    if ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0 && (unicodeChar & KeyCharacterMap.COMBINING_ACCENT_MASK) != 0) {
                        conn.sendUtf8Text(""+(char)unicodeChar);
                        return true;
                    }

                    return false;
                }
            }

            // A repeticao ATRAVESSA -- com ressalvas.
            //
            // O upstream come os repeat downs, e a razao e boa: para jogar, segurar W tem de
            // manter a tecla descida, e o host repete sozinho se precisar. So que quem repete
            // no Windows e o driver de teclado, para teclas FISICAS -- entrada injetada por
            // SendInput, que e o que o Sunshine e o Vibeshine usam, nao ganha auto-repeat do
            // sistema. Num jogo isso nao aparece; num desktop remoto e o dia inteiro:
            // segurar Backspace apaga um caractere, segurar a seta anda uma posicao.
            //
            // Duas ressalvas que fazem isto ser seguro:
            //
            //  - MODIFICADOR NAO REPETE. Ctrl segurado e um estado, nao uma sequencia de
            //    pressionamentos; repeti-lo inundaria o host com make codes de uma tecla que
            //    ja esta descida, e e o tipo de coisa que trava um jogo.
            //  - A CADENCIA E LIMITADA. O Android repete rapido, e mais rapido que o host
            //    consegue consumir vira fila, e fila em entrada vira atraso acumulado. 40 ms
            //    entre repeticoes e ~25 por segundo, acima do que o Windows faz por padrao e
            //    abaixo do que enche o fio.
            //
            // Se em aparelho aparecer repeticao DOBRADA -- o host repetindo tambem -- a
            // correcao e voltar a comer, e o comentario acima e o registro do porque.
            if (event.getRepeatCount() > 0) {
                if (ehModificador(event.getKeyCode())) {
                    return true;
                }

                // COMBO NÃO REPETE.
                //
                // Segurar Backspace é uma sequência: apaga, apaga, apaga. Segurar Alt+Tab
                // não é — é um comando com um modificador em pé, e o host o interpreta como
                // um pressionamento novo a cada repetição. Com Alt segurado e Tab repetindo
                // a 25/s, o Windows fica alternando entre as duas primeiras janelas sem
                // parar, que é exatamente o que aconteceu quando eu liguei a repetição sem
                // esta porta. Ctrl+Z repetido desfaz a sessão inteira; Ctrl+C repetido
                // enche a área de transferência de eventos.
                //
                // Shift não conta: Shift+seta é seleção, e seleção é sequência — segurar
                // Shift e a seta é como se seleciona um parágrafo.
                int seguraComando = modifierFlags
                        & (KeyboardPacket.MODIFIER_CTRL
                           | KeyboardPacket.MODIFIER_ALT
                           | KeyboardPacket.MODIFIER_META);
                if (seguraComando != 0) {
                    return true;
                }
                long agora = SystemClock.uptimeMillis();
                if (agora - ultimaRepeticaoMs < INTERVALO_DE_REPETICAO_MS) {
                    return true;
                }
                ultimaRepeticaoMs = agora;
            }

            conn.sendKeyboardInput(translated, KeyboardPacket.KEY_DOWN, getModifierState(event),
                    keyboardTranslator.hasNormalizedMapping(event.getKeyCode(), deviceId) ? 0 : MoonBridge.SS_KBE_FLAG_NON_NORMALIZED);
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyUp(event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean handleKeyUp(KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        int deviceId = event.getDeviceId();
        if (prefConfig.ignoreSynthEvents && deviceId <= 0) {
            return false;
        }

        // Handle a synthetic back button event that some Android OS versions
        // create as a result of a right-click.
        int eventSource = event.getSource();
        if ((eventSource == InputDevice.SOURCE_MOUSE ||
                eventSource == InputDevice.SOURCE_MOUSE_RELATIVE) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            // Send the right mouse button event if mouse back and forward
            // are disabled. If they are enabled, handleMotionEvent() will take
            // care of this.
            if (!prefConfig.mouseNavButtons) {
                conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
            }

            // Always return true, otherwise the back press will be propagated
            // up to the parent and finish the activity.
            return true;
        }

        boolean handled = false;
        if (ControllerHandler.isGameControllerDevice(event.getDevice())) {
            // Always try the controller handler first, unless it's an alphanumeric keyboard device.
            // Otherwise, controller handler will eat keyboard d-pad events.
            handled = controllerHandler.handleButtonUp(event);
        }

        // Try the keyboard handler if it wasn't handled as a game controller
        if (!handled) {
            if (handleSpecialKeys(event.getKeyCode(), false)) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            short translated = keyboardTranslator.translate(event.getKeyCode(), event.getScanCode(), deviceId);
            if (translated == 0) {
                if (prefConfig.backAsMeta && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    translated = 0x5b; // Meta key
                } else {
                    // If we sent this event as UTF-8 on key down, also report that it was handled
                    // when we get the key up event for it.
                    int unicodeChar = event.getUnicodeChar();
                    return (unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0 && (unicodeChar & KeyCharacterMap.COMBINING_ACCENT_MASK) != 0;
                }
            }

            conn.sendKeyboardInput(translated, KeyboardPacket.KEY_UP, getModifierState(event),
                    keyboardTranslator.hasNormalizedMapping(event.getKeyCode(), deviceId) ? 0 : MoonBridge.SS_KBE_FLAG_NON_NORMALIZED);
        }

        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return handleKeyMultiple(event) || super.onKeyMultiple(keyCode, repeatCount, event);
    }

    public boolean handleKeyMultiple(KeyEvent event) {
        // We can receive keys from a software keyboard that don't correspond to any existing
        // KEYCODE value. Android will give those to us as an ACTION_MULTIPLE KeyEvent.
        //
        // Despite the fact that the Android docs say this is unused since API level 29, these
        // events are still sent as of Android 13 for the above case.
        //
        // For other cases of ACTION_MULTIPLE, we will not report those as handled so hopefully
        // they will be passed to us again as regular singular key events.
        if (event.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN || event.getCharacters() == null) {
            return false;
        }

        conn.sendUtf8Text(event.getCharacters());
        return true;
    }

    public void sendKeys(short[] keys) {
        final byte[] modifier = {(byte) 0};

        for (short key : keys) {
            conn.sendKeyboardInput(key, KeyboardPacket.KEY_DOWN, modifier[0], (byte) 0);

            // Apply the modifier of the pressed key, e.g. CTRL first issues a CTRL event (without
            // modifier) and then sends the following keys with the CTRL modifier applied
            modifier[0] |= getModifier(key);
        }

        new Handler().postDelayed((() -> {
            for (int pos = keys.length - 1; pos >= 0; pos--) {
                short key = keys[pos];

                // Remove the keys modifier before releasing the key
                modifier[0] &= (byte) ~getModifier(key);

                conn.sendKeyboardInput(key, KeyboardPacket.KEY_UP, modifier[0], (byte) 0);
            }
        }), GameMenu.KEY_UP_DELAY);
    }

    /**
     * A sincronização de área de transferência pergunta ao host antes de agir.
     *
     * Ela disparava em TODA perda de foco de janela — e isso inclui abrir um diálogo, o IME
     * subir e entrar em PiP, ou seja, várias vezes por sessão. Sem checar se o host tem o
     * recurso, `getClipboard` gravava no clipboard do tablet o corpo cru da resposta: no
     * Sunshine e no Vibeshine, que não implementam a rota, isso é uma página de erro
     * devolvida com status 200. O que ele tinha copiado sumia, substituído por HTML, e uma
     * torrada anunciava sucesso.
     *
     * O menu já fazia a pergunta certa — as duas linhas de área de transferência só existem
     * lá quando os bits de permissão do host as autorizam. O caminho automático usa agora a
     * mesma resposta.
     */
    public boolean handleFocusChange(boolean hasFocus) {
        if (!connected || !prefConfig.smartClipboardSync) {
            return false;
        }

        if (hasFocus) {
            return hostCapabilities.supportsClipboardSend() && sendClipboard(false);
        }
        return hostCapabilities.supportsClipboardFetch() && getClipboard(0);
    }

    // Method to get clipboard content
    private String getClipboardContent(boolean force) {
        // Check if there is any clipboard data
        if (clipboardManager.hasPrimaryClip()) {
            ClipDescription clipDescription = clipboardManager.getPrimaryClipDescription();
            if (!force && clipDescription != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    PersistableBundle extras = clipDescription.getExtras();
                    if (extras != null && extras.getBoolean(CLIPBOARD_IDENTIFIER)) {
                        // We're getting the clipboard data we just set/read a while ago
                        return null;
                    }
                } else {
                    CharSequence clipLabel = clipDescription.getLabel();
                    if (clipLabel != null && clipLabel.equals(CLIPBOARD_IDENTIFIER)) {
                        // We're getting the clipboard data we set a while ago
                        return null;
                    }
                }
            }

            ClipData clipData = clipboardManager.getPrimaryClip();

            if (clipData != null && clipData.getItemCount() > 0) {
                // Get the first item from the clipboard data
                ClipData.Item item = clipData.getItemAt(0);

                // Mark the clip as visited
                if (clipDescription != null) {
                    ClipData clonedClip = cloneClipData(clipDescription, item);
                    clipboardManager.setPrimaryClip(clonedClip);
                }

                // Get the text data from the clipboard item
                CharSequence clipText = item.getText();
                if (clipText == null) {
                    return  null;
                }
                return clipText.toString();
            }
        }

        return null;
    }

    private static @NonNull ClipData cloneClipData(ClipDescription clipDescription, ClipData.Item item) {
        ClipDescription clonedDescription = new ClipDescription(clipDescription);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PersistableBundle extras = clipDescription.getExtras();
            if (extras == null) {
                extras = new PersistableBundle();
            }
            extras.putBoolean(CLIPBOARD_IDENTIFIER, true);
            clonedDescription.setExtras(extras);
        }

        return new ClipData(clonedDescription, item);
    }

    public boolean sendClipboard(boolean force) {
        if (httpConn == null) {
            LimeLog.warning("httpConn not ready, cannot send clipboard!");
            return false;
        }

        String clipboardText = getClipboardContent(force);
        if (clipboardText != null) {
            new Thread() {
                public void run() {
                    try {
                        if (!httpConn.sendClipboard(clipboardText)) {
                            if (prefConfig.smartClipboardSyncToast) {
                                Game.this.runOnUiThread(() -> VidroToast.show(Game.this, getString(R.string.clipboard_sync_unsupported), Toast.LENGTH_SHORT));
                            }
                        } else {
                            if (prefConfig.smartClipboardSyncToast) {
                                Game.this.runOnUiThread(() -> VidroToast.show(Game.this, getString(R.string.send_clipboard_success), Toast.LENGTH_SHORT));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (prefConfig.smartClipboardSyncToast) {
                            Game.this.runOnUiThread(() -> VidroToast.show(Game.this, getString(R.string.send_clipboard_failed) + e.getMessage(), Toast.LENGTH_SHORT));
                        }
                    }
                }
            }.start();

            return true;
        }

        return false;
    }

    public boolean getClipboard(int delay) {
        if (httpConn == null) {
            LimeLog.warning("httpConn not ready, cannot get clipboard!");
            return false;
        }

        if (delay == 0 && gameMenuCallbacks != null && gameMenuCallbacks.isMenuOpen()) {
            return false;
        }

        new Thread() {
            public void run() {
                if (clipboardSyncRunning) {
                    return;
                }

                clipboardSyncRunning = true;
                try {
                    if (delay > 0) {
                        sleep(delay);
                    }
                    String clipboardContent = httpConn.getClipboard();
                    ClipData clipData = ClipData.newPlainText(CLIPBOARD_IDENTIFIER, clipboardContent);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ClipDescription clipDescription = clipData.getDescription();
                        PersistableBundle newExtras = new PersistableBundle();
                        newExtras.putBoolean(CLIPBOARD_IDENTIFIER, true);
                        if (prefConfig.hideClipboardContent) {
                            // We don't know if the message is sensitive or not, to be safe mark them all as sensitive.
                            newExtras.putBoolean("android.content.extra.IS_SENSITIVE", true);
                        }
                        clipDescription.setExtras(newExtras);
                    }

                    clipboardManager.setPrimaryClip(clipData);
                    if (prefConfig.smartClipboardSyncToast) {
                        Game.this.runOnUiThread(() -> VidroToast.show(Game.this, getString(R.string.get_clipboard_success), Toast.LENGTH_SHORT));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (prefConfig.smartClipboardSyncToast) {
                        Game.this.runOnUiThread(() -> VidroToast.show(Game.this, getString(R.string.get_clipboard_failed) + e.getMessage(), Toast.LENGTH_SHORT));
                    }
                }
                clipboardSyncRunning = false;
            }
        }.start();

        return true;
    }

    private TouchContext getTouchContext(int actionIndex, TouchContext[] inputContextMap)
    {
        if (actionIndex < inputContextMap.length) {
            return inputContextMap[actionIndex];
        }
        else {
            return null;
        }
    }

    /**
     * A alternância do teclado cru, contada por nós.
     *
     * toggleSoftInput não devolve se abriu ou se fechou, e o inset do IME só chega quando
     * o modo de exibição do teclado é um dos que tratam inset — no modo sobreposto não
     * chega nunca. Guardar o estado aqui é o que permite falar só na abertura.
     */
    private boolean rawKeyboardShown = false;

    /**
     * Há teclado físico acoplado?
     *
     * Os dois testes juntos, e não um só: {@code keyboard == KEYBOARD_QWERTY} continua
     * valendo com a capa do S8 Ultra dobrada para trás, e é {@code hardKeyboardHidden} que
     * diz se as teclas estão ao alcance da mão agora.
     *
     * Pela Configuration e não por {@code InputDevice}: varrer dispositivos exigiria
     * excluir {@code isVirtual()} à mão, senão o próprio IME conta como teclado físico —
     * exatamente o oposto do que se está perguntando.
     *
     * {@code keyboard|keyboardHidden} estão em configChanges (AndroidManifest.xml), então
     * acoplar ou soltar a capa chega como onConfigurationChanged e a resposta acompanha.
     */
    public boolean hasHardwareKeyboard() {
        Configuration config = getResources().getConfiguration();
        return config.keyboard == Configuration.KEYBOARD_QWERTY
                && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
    }

    @Override
    public void toggleKeyboard() {
        if (isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleKeyboard();
        } else if (hasHardwareKeyboard()) {
            // Com a capa acoplada este gesto era morto: o Android não abre teclado de tela
            // quando há teclado de verdade conectado, então toggleSoftInput não mudava
            // nada — e ainda avisava que as teclas passariam a ir direto para o PC, que é
            // o que já acontecia. Três dedos passam a abrir o que faz sentido aqui: o
            // campo que monta o texto no aparelho e manda de uma vez.
            //
            // Sem torrada: o estado está desenhado na tela. E alterna de verdade — o gesto
            // que abre é o mesmo que fecha.
            if (isComposeBarVisible()) {
                closeComposeBar();
            } else {
                openComposeBar();
            }
        } else {
            LimeLog.info("Toggling keyboard overlay");
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.toggleSoftInput(0, 0);

            rawKeyboardShown = !rawKeyboardShown;
            if (rawKeyboardShown) {
                // Aqui cada tecla viaja para o PC na hora, e a tela não mostra campo nenhum
                // que diga isso — o estado é visualmente igual ao da barra de composição,
                // onde o texto é montado no aparelho e só sai quando se manda.
                VidroToast.show(this, R.string.vidro_keyboard_direct, Toast.LENGTH_SHORT);
            }
        }
    }

    private byte getLiTouchTypeFromEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                return MoonBridge.LI_TOUCH_EVENT_DOWN;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if ((event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                    return MoonBridge.LI_TOUCH_EVENT_CANCEL;
                }
                else {
                    return MoonBridge.LI_TOUCH_EVENT_UP;
                }

            case MotionEvent.ACTION_MOVE:
                return MoonBridge.LI_TOUCH_EVENT_MOVE;

            case MotionEvent.ACTION_CANCEL:
                // ACTION_CANCEL applies to *all* pointers in the gesture, so it maps to CANCEL_ALL
                // rather than CANCEL. For a single pointer cancellation, that's indicated via
                // FLAG_CANCELED on a ACTION_POINTER_UP.
                // https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi
                return MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL;

            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
                return MoonBridge.LI_TOUCH_EVENT_HOVER;

            case MotionEvent.ACTION_HOVER_EXIT:
                return MoonBridge.LI_TOUCH_EVENT_HOVER_LEAVE;

            case MotionEvent.ACTION_BUTTON_PRESS:
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY;

            default:
                return -1;
        }
    }

    //灵敏度保存到集合 适配多个手指
    private Map<String,SensitivityBean> sensitivityMap=new HashMap<>();

    //修改移动的触控灵敏度（通过修改移动的距离实现） 默认使用右半边屏幕的时候开启
    private float[] getStreamViewRelativeSensitivityXY(MotionEvent event,float normalizedX,float normalizedY,int pointerIndex){
        float[] normalized=new float[2];
        normalized[0]=normalizedX;
        normalized[1]=normalizedY;

        //如果不是全局模式 并且 坐标 不在右边 则返回
        if(!prefConfig.touchSensitivityGlobal&&normalizedX<getResources().getDisplayMetrics().widthPixels/2){
            return normalized;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            SensitivityBean bean=sensitivityMap.get(String.valueOf(event.getPointerId(pointerIndex)));
            if(bean==null){
                bean=new SensitivityBean();
            }
            if(bean.getLastAbsoluteX() !=-1){
                float dx=normalizedX- bean.getLastAbsoluteX();
                float dy=normalizedY- bean.getLastAbsoluteY();
                dx*=0.01f*prefConfig.touchSensitivityX;//灵敏度
                dy*=0.01f*prefConfig.touchSensitivityY;
                normalizedX= bean.getLastRelativelyX() +dx;
                normalizedY= bean.getLastRelativelyY() +dy;
            }
            if(prefConfig.touchSensitivityRotationAuto){
                if(normalizedX>= streamContainer.getWidth()){
                    normalizedX= streamContainer.getWidth()/2.0f;
                }
                if(normalizedY>= streamContainer.getHeight()){
                    normalizedY= streamContainer.getHeight()/2.0f;
                }
            }
            bean.setLastAbsoluteX(event.getX(pointerIndex));
            bean.setLastAbsoluteY(event.getY(pointerIndex));
            bean.setLastRelativelyX(normalizedX);
            bean.setLastRelativelyY(normalizedY);
            sensitivityMap.put(String.valueOf(event.getPointerId(pointerIndex)),bean);
        }
        //抬起的时候，恢复初始化状态
        if (event.getActionMasked() == MotionEvent.ACTION_UP||event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            sensitivityMap.remove(String.valueOf(event.getPointerId(pointerIndex)));
        }
        normalized[0]=normalizedX;
        normalized[1]=normalizedY;
        return normalized;
    }


    private float[] getStreamViewRelativeNormalizedXY(View view, MotionEvent event, int pointerIndex) {
        float normalizedX = event.getX(pointerIndex);
        float normalizedY = event.getY(pointerIndex);
        //开启自定义修改触控灵敏度 并且 数值不为100
        if(prefConfig.enableTouchSensitivity&&(prefConfig.touchSensitivityX !=100||prefConfig.touchSensitivityY!=100)){
            float[] normalized=getStreamViewRelativeSensitivityXY(event,normalizedX,normalizedY,pointerIndex);
            normalizedX=normalized[0];
            normalizedY=normalized[1];
        }
        // For the containing background view, we must subtract the origin
        // of the StreamView to get video-relative coordinates.
        if (view != streamContainer) {
            float[] normalized = getNormalizedCoordinates(streamContainer, normalizedX, normalizedY);
            normalizedX = normalized[0];
            normalizedY = normalized[1];
        }

        normalizedX = Math.max(normalizedX, 0.0f);
        normalizedY = Math.max(normalizedY, 0.0f);

        normalizedX = Math.min(normalizedX, streamContainer.getWidth());
        normalizedY = Math.min(normalizedY, streamContainer.getHeight());

        normalizedX /= streamContainer.getWidth();
        normalizedY /= streamContainer.getHeight();

        return new float[] { normalizedX, normalizedY };
    }

    private float[] getNormalizedCoordinates(View streamView, float rawX, float rawY) {
        float scaleX = streamView.getScaleX();
        float scaleY = streamView.getScaleY();

        float normalizedX = (rawX - streamView.getX()) / scaleX;
        float normalizedY = (rawY - streamView.getY()) / scaleY;

        return new float[] { normalizedX, normalizedY };
    }

    private static float normalizeValueInRange(float value, InputDevice.MotionRange range) {
        return (value - range.getMin()) / range.getRange();
    }

    private static float getPressureOrDistance(MotionEvent event, int pointerIndex) {
        InputDevice dev = event.getDevice();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_HOVER_EXIT:
                // Hover events report distance
                if (dev != null) {
                    InputDevice.MotionRange distanceRange = dev.getMotionRange(MotionEvent.AXIS_DISTANCE, event.getSource());
                    if (distanceRange != null) {
                        return normalizeValueInRange(event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex), distanceRange);
                    }
                }
                return 0.0f;

            default:
                // Other events report pressure
                return event.getPressure(pointerIndex);
        }
    }

    private static short getRotationDegrees(MotionEvent event, int pointerIndex) {
        InputDevice dev = event.getDevice();
        if (dev != null) {
            if (dev.getMotionRange(MotionEvent.AXIS_ORIENTATION, event.getSource()) != null) {
                short rotationDegrees = (short) Math.toDegrees(event.getOrientation(pointerIndex));
                if (rotationDegrees < 0) {
                    rotationDegrees += 360;
                }
                return rotationDegrees;
            }
        }
        return MoonBridge.LI_ROT_UNKNOWN;
    }

    private static float[] polarToCartesian(float r, float theta) {
        return new float[] { (float)(r * Math.cos(theta)), (float)(r * Math.sin(theta)) };
    }

    private static float cartesianToR(float[] point) {
        return (float)Math.sqrt(Math.pow(point[0], 2) + Math.pow(point[1], 2));
    }

    private float[] getStreamViewNormalizedContactArea(MotionEvent event, int pointerIndex) {
        float orientation;

        // If the orientation is unknown, we'll just assume it's at a 45 degree angle and scale it by
        // X and Y scaling factors evenly.
        if (event.getDevice() == null || event.getDevice().getMotionRange(MotionEvent.AXIS_ORIENTATION, event.getSource()) == null) {
            orientation = (float)(Math.PI / 4);
        }
        else {
            orientation = event.getOrientation(pointerIndex);
        }

        float contactAreaMajor, contactAreaMinor;
        switch (event.getActionMasked()) {
            // Hover events report the tool size
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_HOVER_EXIT:
                contactAreaMajor = event.getToolMajor(pointerIndex);
                contactAreaMinor = event.getToolMinor(pointerIndex);
                break;

            // Other events report contact area
            default:
                contactAreaMajor = event.getTouchMajor(pointerIndex);
                contactAreaMinor = event.getTouchMinor(pointerIndex);
                break;
        }

        // The contact area major axis is parallel to the orientation, so we simply convert
        // polar to cartesian coordinates using the orientation as theta.
        float[] contactAreaMajorCartesian = polarToCartesian(contactAreaMajor, orientation);

        // The contact area minor axis is perpendicular to the contact area major axis (and thus
        // the orientation), so rotate the orientation angle by 90 degrees.
        float[] contactAreaMinorCartesian = polarToCartesian(contactAreaMinor, (float)(orientation + (Math.PI / 2)));

        // Normalize the contact area to the stream view size
        contactAreaMajorCartesian[0] = Math.min(Math.abs(contactAreaMajorCartesian[0]), streamContainer.getWidth()) / streamContainer.getWidth();
        contactAreaMinorCartesian[0] = Math.min(Math.abs(contactAreaMinorCartesian[0]), streamContainer.getWidth()) / streamContainer.getWidth();
        contactAreaMajorCartesian[1] = Math.min(Math.abs(contactAreaMajorCartesian[1]), streamContainer.getHeight()) / streamContainer.getHeight();
        contactAreaMinorCartesian[1] = Math.min(Math.abs(contactAreaMinorCartesian[1]), streamContainer.getHeight()) / streamContainer.getHeight();

        // Convert the normalized values back into polar coordinates
        return new float[] { cartesianToR(contactAreaMajorCartesian), cartesianToR(contactAreaMinorCartesian) };
    }

    private boolean sendPenEventForPointer(View view, MotionEvent event, byte eventType, byte toolType, int pointerIndex) {
        byte penButtons = 0;
        if ((event.getButtonState() & MotionEvent.BUTTON_STYLUS_PRIMARY) != 0) {
            penButtons |= MoonBridge.LI_PEN_BUTTON_PRIMARY;
        }
        if ((event.getButtonState() & MotionEvent.BUTTON_STYLUS_SECONDARY) != 0) {
            penButtons |= MoonBridge.LI_PEN_BUTTON_SECONDARY;
        }

        byte tiltDegrees = MoonBridge.LI_TILT_UNKNOWN;
        InputDevice dev = event.getDevice();
        if (dev != null) {
            if (dev.getMotionRange(MotionEvent.AXIS_TILT, event.getSource()) != null) {
                tiltDegrees = (byte)Math.toDegrees(event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex));
            }
        }

        float[] normalizedCoords = getStreamViewRelativeNormalizedXY(view, event, pointerIndex);
        float[] normalizedContactArea = getStreamViewNormalizedContactArea(event, pointerIndex);
        return conn.sendPenEvent(eventType, toolType, penButtons,
                normalizedCoords[0], normalizedCoords[1],
                getPressureOrDistance(event, pointerIndex),
                normalizedContactArea[0], normalizedContactArea[1],
                getRotationDegrees(event, pointerIndex), tiltDegrees) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    private static byte convertToolTypeToStylusToolType(MotionEvent event, int pointerIndex) {
        switch (event.getToolType(pointerIndex)) {
            case MotionEvent.TOOL_TYPE_ERASER:
                return MoonBridge.LI_TOOL_TYPE_ERASER;
            case MotionEvent.TOOL_TYPE_STYLUS:
                return MoonBridge.LI_TOOL_TYPE_PEN;
            default:
                return MoonBridge.LI_TOOL_TYPE_UNKNOWN;
        }
    }

    private boolean trySendPenEvent(View view, MotionEvent event) {
        byte eventType = getLiTouchTypeFromEvent(event);
        if (eventType < 0) {
            return false;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            // Move events may impact all active pointers
            boolean handledStylusEvent = false;
            for (int i = 0; i < event.getPointerCount(); i++) {
                byte toolType = convertToolTypeToStylusToolType(event, i);
                if (toolType == MoonBridge.LI_TOOL_TYPE_UNKNOWN) {
                    // Not a stylus pointer, so skip it
                    continue;
                }
                else {
                    // This pointer is a stylus, so we'll report that we handled this event
                    handledStylusEvent = true;
                }

                if (!sendPenEventForPointer(view, event, eventType, toolType, i)) {
                    // Pen events aren't supported by the host
                    return false;
                }
            }
            return handledStylusEvent;
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            // Cancel impacts all active pointers
            return conn.sendPenEvent(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL, MoonBridge.LI_TOOL_TYPE_UNKNOWN, (byte)0,
                    0, 0, 0, 0, 0,
                    MoonBridge.LI_ROT_UNKNOWN, MoonBridge.LI_TILT_UNKNOWN) != MoonBridge.LI_ERR_UNSUPPORTED;
        }
        else {
            // Up, Down, and Hover events are specific to the action index
            byte toolType = convertToolTypeToStylusToolType(event, event.getActionIndex());
            if (toolType == MoonBridge.LI_TOOL_TYPE_UNKNOWN) {
                // Not a stylus event
                return false;
            }
            return sendPenEventForPointer(view, event, eventType, toolType, event.getActionIndex());
        }
    }

    private boolean sendTouchEventForPointer(View view, MotionEvent event, byte eventType, int pointerIndex) {
        float[] normalizedCoords = getStreamViewRelativeNormalizedXY(view, event, pointerIndex);
        float[] normalizedContactArea = getStreamViewNormalizedContactArea(event, pointerIndex);
        return conn.sendTouchEvent(eventType, event.getPointerId(pointerIndex),
                normalizedCoords[0], normalizedCoords[1],
                getPressureOrDistance(event, pointerIndex),
                normalizedContactArea[0], normalizedContactArea[1],
                getRotationDegrees(event, pointerIndex)) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    private boolean trySendTouchEvent(View view, MotionEvent event) {
        byte eventType = getLiTouchTypeFromEvent(event);
        if (eventType < 0) {
            return false;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            // Move events may impact all active pointers
            for (int i = 0; i < event.getPointerCount(); i++) {
                if (!sendTouchEventForPointer(view, event, eventType, i)) {
                    return false;
                }
            }
            return true;
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            // Cancel impacts all active pointers
            return conn.sendTouchEvent(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL, 0,
                    0, 0, 0, 0, 0,
                    MoonBridge.LI_ROT_UNKNOWN) != MoonBridge.LI_ERR_UNSUPPORTED;
        }
        else {
            // Up, Down, and Hover events are specific to the action index
            return sendTouchEventForPointer(view, event, eventType, event.getActionIndex());
        }
    }

    // Returns true if the event was consumed
    // NB: View is only present if called from a view callback
    public boolean handleMotionEvent(View view, MotionEvent event) {
        // Pass through mouse/touch/joystick input if we're not grabbing
        if (!grabbedInput) {
            return false;
        }

        int deviceId = event.getDeviceId();
        if (prefConfig.ignoreSynthEvents && deviceId <= 0) {
            return false;
        }

        int eventSource = event.getSource();
        int deviceSources = event.getDevice() != null ? event.getDevice().getSources() : 0;
        if ((eventSource & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            if (controllerHandler.handleMotionEvent(event)) {
                return true;
            }
        }
        else if ((deviceSources & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && controllerHandler.tryHandleTouchpadEvent(event)) {
            return true;
        }
        else if ((eventSource & InputDevice.SOURCE_CLASS_POINTER) != 0 ||
                (eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0 ||
                eventSource == InputDevice.SOURCE_MOUSE_RELATIVE)
        {
            boolean hasActionButton = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (event.getActionButton() != 0);
            // This case is for mice and non-finger touch devices
            if (
                    eventSource == InputDevice.SOURCE_MOUSE ||
                            ((eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0 && (hasActionButton || isPhysicalTouchpad(event))) || // SOURCE_TOUCHPAD
                            (eventSource == InputDevice.SOURCE_MOUSE_RELATIVE ||
                                    (event.getPointerCount() >= 1 &&
                                            (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE ||
                                                    event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                                                    event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)) ||
                                    eventSource == 12290) // 12290 = Samsung DeX mode desktop mouse
            ) {
                int buttonState = event.getButtonState();
                int changedButtons = buttonState ^ lastButtonState;

                // Two finger click
                if ((eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0 &&
                        event.getPointerCount() == 2 &&
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && event.getActionButton() == MotionEvent.BUTTON_PRIMARY)) {
                    if (event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS) {
                        buttonState |= MotionEvent.BUTTON_SECONDARY;
                    }
                    else if (event.getActionMasked() == MotionEvent.ACTION_BUTTON_RELEASE) {
                        buttonState &= ~MotionEvent.BUTTON_SECONDARY;
                    }
                    // We may not pressing the primary button down from a previous event,
                    // so be sure to clear that bit out the button state.
                    buttonState &= ~MotionEvent.BUTTON_PRIMARY;
                    buttonState |= (lastButtonState & MotionEvent.BUTTON_PRIMARY);

                    changedButtons = buttonState ^ lastButtonState;
                }

                // Ignore mouse input if we're not capturing from our input source
                if (!inputCaptureProvider.isCapturingActive()) {
                    // We return true here because otherwise the events may end up causing
                    // Android to synthesize d-pad events.
                    return true;
                }

                // Always update the position before sending any button events. If we're
                // dealing with a stylus without hover support, our position might be
                // significantly different than before.
                if (inputCaptureProvider.eventHasRelativeMouseAxes(event)) {
                    // Send the deltas straight from the motion event
                    short deltaX = (short)inputCaptureProvider.getRelativeAxisX(event);
                    short deltaY = (short)inputCaptureProvider.getRelativeAxisY(event);

                    if (deltaX != 0 || deltaY != 0) {
                        if (prefConfig.absoluteMouseMode) {
                            // NB: view may be null, but we can unconditionally use streamView because we don't need to adjust
                            // relative axis deltas for the position of the streamView within the parent's coordinate system.
                            conn.sendMouseMoveAsMousePosition(deltaX, deltaY, (short) streamContainer.getWidth(), (short) streamContainer.getHeight());
                        }
                        else {
                            conn.sendMouseMove(deltaX, deltaY);
                        }
                    }
                }
                else if (isPhysicalTouchpad(event)) {
                    // A trackpad in a keyboard cover reports absolute positions on its own
                    // surface. The host wants deltas, so we differentiate here instead of
                    // falling into sendMousePosition below, which would teleport the cursor to
                    // wherever the finger happened to land.
                    handlePhysicalTouchpadMotion(event);
                }
                else if ((eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0) {
                    // If this input device is not associated with the view itself (like a trackpad),
                    // we'll convert the device-specific coordinates to use to send the cursor position.
                    // This really isn't ideal but it's probably better than nothing.
                    //
                    // Trackpad on newer versions of Android (Oreo and later) should be caught by the
                    // relative axes case above. If we get here, we're on an older version that doesn't
                    // support pointer capture.
                    InputDevice device = event.getDevice();
                    if (device != null) {
                        InputDevice.MotionRange xRange = device.getMotionRange(MotionEvent.AXIS_X, eventSource);
                        InputDevice.MotionRange yRange = device.getMotionRange(MotionEvent.AXIS_Y, eventSource);

                        // All touchpads coordinate planes should start at (0, 0)
                        if (xRange != null && yRange != null && xRange.getMin() == 0 && yRange.getMin() == 0) {
                            int xMax = (int)xRange.getMax();
                            int yMax = (int)yRange.getMax();

                            // Touchpads must be smaller than (65535, 65535)
                            if (xMax <= Short.MAX_VALUE && yMax <= Short.MAX_VALUE) {
                                conn.sendMousePosition((short)event.getX(), (short)event.getY(),
                                        (short)xMax, (short)yMax);
                            }
                        }
                    }
                }
                else if (view != null && trySendPenEvent(view, event)) {
                    // If our host supports pen events, send it directly
                    return true;
                }
                else if (view != null) {
                    if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                        // Handle trackpad two finger swipes when pointer is not captured by synthesizing a trackpad movement
                        // Android emulates trackpad  two finger swipes as one finger swipe on the screen
                        int eventAction = event.getActionMasked();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.getClassification() == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE) {
                            if (!pointerSwiping) {
                                pointerSwiping = true;
                                handleTouchInput(event, trackpadContextMap, false, prefConfig.trackpadSwapAxis, MotionEvent.ACTION_POINTER_DOWN, 1, 2);
                            }
                            return handleTouchInput(event, trackpadContextMap, false, prefConfig.trackpadSwapAxis, MotionEvent.ACTION_MOVE, 1, 2);
                        } else if (pointerSwiping && eventAction == MotionEvent.ACTION_UP) {
                            pointerSwiping = false;
                            synthClickPending = false;
                            handleTouchInput(event, trackpadContextMap, false, prefConfig.trackpadSwapAxis, MotionEvent.ACTION_POINTER_UP, 1, 2);
                            return true;
                        }

                        // Press & Hold / Double-Tap & Hold for Selection or Drag & Drop
                        double positionDelta = Math.sqrt(
                                Math.pow(event.getX() - lastTouchDownX, 2) +
                                        Math.pow(event.getY() - lastTouchDownY, 2)
                        );

                        if (synthClickPending &&
                                event.getEventTime() - synthTouchDownTime >= prefConfig.trackpadDragDropThreshold) {
                            if (positionDelta > 50) {
                                pendingDrag = false;
                            } else if (pendingDrag) {
                                pendingDrag = false;
                                isDragging = true;
                                if (prefConfig.trackpadDragDropVibration) {
                                    Vibrator vibrator = ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE));
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(20, 127));
                                    } else {
                                        vibrator.vibrate(20);
                                    }
                                }
                                conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                                return true;
                            }
                        }

                        switch (eventAction) {
                            case MotionEvent.ACTION_HOVER_MOVE:
                            case MotionEvent.ACTION_MOVE:
                                updateMousePosition(view, event);
                                return true;
                            case MotionEvent.ACTION_HOVER_EXIT:
                            case MotionEvent.ACTION_DOWN:
                                pendingDrag = true;
                                synthClickPending = true;
                                lastTouchDownX = event.getX();
                                lastTouchDownY = event.getY();
                                synthTouchDownTime = event.getEventTime();
                                return true;
                            case MotionEvent.ACTION_HOVER_ENTER:
                            case MotionEvent.ACTION_UP:
                                if (synthClickPending) {
                                    long timeDiff = event.getEventTime() - synthTouchDownTime;

                                    if (eventSource == 12290) {
                                        // Special handle for DeX
                                        // DeX reports button secondary when tapping with two fingers
                                        // So there's no need to distinguish left/right click by time difference
                                        if (timeDiff < 120) {
                                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                                        }
                                    } else {
                                        if (timeDiff < 20) {
                                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                                        } else if (timeDiff < 120) {
                                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                                        }
                                    }
                                    if (isDragging) {
                                        isDragging = false;
                                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                                    }
                                    pendingDrag = false;
                                    synthClickPending = false;
                                }
                                return true;
                            case MotionEvent.ACTION_BUTTON_PRESS:
                            case MotionEvent.ACTION_BUTTON_RELEASE:
                                synthClickPending = false;
                            default:
                                break;
                        }
                    } else {
                        updateMousePosition(view, event);
                    }
                }

                if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
                    // Send the vertical scroll packet
                    conn.sendMouseHighResScroll((short)(event.getAxisValue(MotionEvent.AXIS_VSCROLL) * 120));
                    conn.sendMouseHighResHScroll((short)(event.getAxisValue(MotionEvent.AXIS_HSCROLL) * 120));
                }

                if ((changedButtons & MotionEvent.BUTTON_PRIMARY) != 0) {
                    if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                    }
                }

                // Mouse secondary or stylus primary is right click (stylus down is left click)
                if ((changedButtons & (MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_STYLUS_PRIMARY)) != 0) {
                    if ((buttonState & (MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_STYLUS_PRIMARY)) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                    }
                }

                // Mouse tertiary or stylus secondary is middle click
                if ((changedButtons & (MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_STYLUS_SECONDARY)) != 0) {
                    if ((buttonState & (MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_STYLUS_SECONDARY)) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_MIDDLE);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_MIDDLE);
                    }
                }

                if (prefConfig.mouseNavButtons) {
                    if ((changedButtons & MotionEvent.BUTTON_BACK) != 0) {
                        if ((buttonState & MotionEvent.BUTTON_BACK) != 0) {
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_X1);
                        }
                        else {
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_X1);
                        }
                    }

                    if ((changedButtons & MotionEvent.BUTTON_FORWARD) != 0) {
                        if ((buttonState & MotionEvent.BUTTON_FORWARD) != 0) {
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_X2);
                        }
                        else {
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_X2);
                        }
                    }
                }

                // Handle stylus presses
                if (event.getPointerCount() == 1 && event.getActionIndex() == 0) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                            lastAbsTouchDownTime = event.getEventTime();
                            lastAbsTouchDownX = event.getX(0);
                            lastAbsTouchDownY = event.getY(0);

                            // Stylus is left click
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                        } else if (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
                            lastAbsTouchDownTime = event.getEventTime();
                            lastAbsTouchDownX = event.getX(0);
                            lastAbsTouchDownY = event.getY(0);

                            // Eraser is right click
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                        }
                    }
                    else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                            lastAbsTouchUpTime = event.getEventTime();
                            lastAbsTouchUpX = event.getX(0);
                            lastAbsTouchUpY = event.getY(0);

                            // Stylus is left click
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                        } else if (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
                            lastAbsTouchUpTime = event.getEventTime();
                            lastAbsTouchUpX = event.getX(0);
                            lastAbsTouchUpY = event.getY(0);

                            // Eraser is right click
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                        }
                    }
                }

                lastButtonState = buttonState;
            }
            // This case is for fingers
            else {
                if (eventSource == InputDevice.SOURCE_TOUCHPAD) {
                    return handleTouchInput(event, trackpadContextMap, false);
                } else {
                    if (virtualController != null &&
                            (virtualController.getControllerMode() == VirtualController.ControllerMode.MoveButtons ||
                                    virtualController.getControllerMode() == VirtualController.ControllerMode.ResizeButtons)) {
                        // Ignore presses when the virtual controller is being configured
                        return true;
                    }

                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        ultimoToqueX = event.getX();
                        ultimoToqueY = event.getY();
                    }

                    // Um toque na imagem fecha a ilha aberta, e nao vai para o host.
                    //
                    // Com a ilha aberta o dedo ja estava "na interface", nao no jogo: quem
                    // toca fora dela esta pedindo para sair, do mesmo jeito que se toca fora
                    // de um menu. Antes o toque atravessava para o PC E deixava a ilha
                    // aberta -- clicava no jogo por engano e o bloco continuava la.
                    //
                    // So o primeiro dedo, e so o toque que DESCE: consumir o resto do gesto
                    // engoliria um arrasto que comecou legitimamente depois do fechamento.
                    // Fechar tocando fora deixou de ser assunto daqui: a GavetaDaSessao
                    // intercepta os proprios toques, e um toque sobre o video com ela aberta
                    // fecha antes de chegar a este ponto.
                    //
                    // E a saida de emergencia de cinco dedos saiu junto com o punho: nao ha
                    // mais nada que possa ser escondido a ponto de precisar de resgate. A
                    // gaveta esta sempre a uma borda ou a um acorde de distancia.

                    if (isPanZoomMode) {
                        // panning the streamView
                        panZoomHandler.handleTouchEvent(event);
                        return true;
                    }

                    // If touch is disabled or not initialized, we'll try panning the streamView
                    if (touchContextMap[0] == null) {
                        return true;
                    }

                    if (prefConfig.enableMultiTouchGestures || !prefConfig.enableMultiTouchScreen) {
                        int pointerCount = event.getPointerCount();
                        if (pointerCount > 2) {
                            int eventAction = event.getActionMasked();
                            if (
                                    (
                                            eventAction == MotionEvent.ACTION_POINTER_DOWN
                                                    || eventAction == MotionEvent.ACTION_POINTER_UP
                                                    || eventAction == MotionEvent.ACTION_UP
                                    )
                                            && handleMultiTouchGesture(event, eventAction, pointerCount, view)
                            ) {
                                return true;
                            }
                        }
                    }

                    if (prefConfig.enableMultiTouchScreen && !prefConfig.touchscreenTrackpad && trySendTouchEvent(view, event)) {
                        // If this host supports touch events and absolute touch is enabled,
                        // send it directly as a touch event.
                        return true;
                    }

                    return handleTouchInput(event, touchContextMap, true);
                }
            }

            // Handled a known source
            return true;
        }

        // Unknown class
        return false;
    }

    // A trackpad built into a keyboard cover advertises SOURCE_TOUCHPAD without
    // SOURCE_TOUCHSCREEN; the tablet's own panel advertises both. Only the former should
    // bypass TrackpadContext, which exists to emulate a trackpad from a finger on glass and
    // brings its own scroll physics that fight the pad's real scroll events.
    private boolean isPhysicalTouchpad(MotionEvent event) {
        if (!prefConfig.physicalTrackpadNative) {
            return false;
        }

        InputDevice device = event.getDevice();
        if (device == null) {
            return false;
        }

        int sources = device.getSources();
        if ((sources & InputDevice.SOURCE_TOUCHPAD) != InputDevice.SOURCE_TOUCHPAD) {
            return false;
        }

        // O que precisa ser excluído aqui é a TELA do aparelho, que em alguns drivers
        // também se anuncia como touchpad. Recusar tudo que reporte SOURCE_TOUCHSCREEN
        // junto era largo demais: a capa do S8 Ultra reporta as duas fontes no mesmo
        // dispositivo, e com isso o caminho nativo nunca era escolhido — foi por isso que
        // a rotação automática não teve efeito nenhum no teste em tablet, e a troca de
        // eixos continuou tendo de ser feita à mão.
        //
        // Ser EXTERNO separa os dois casos sem ambiguidade: a tela é do aparelho, a capa
        // é acessório. Onde a plataforma não sabe responder isso (API 21 a 22), volta a
        // valer o critério antigo, que ao menos não confunde a tela com um pad.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return device.isExternal()
                    || (sources & InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN;
        }
        return (sources & InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN;
    }

    /**
     * Descobre a geometria do pad uma vez.
     *
     * A conta em si mora em {@link TouchpadGeometry}, fora da Activity, porque é
     * aritmetica pura e precisava de teste: foi por ela ter nascido enfiada no tratamento
     * de MotionEvent que o app chegou ao aparelho exigindo que a pessoa trocasse os eixos
     * a mao e ainda pusesse o eixo Y em -100%.
     */
    private void resolvePhysicalTouchpadGeometry(MotionEvent event) {
        if (physicalTouchpadGeometry != null) {
            return;
        }

        InputDevice device = event.getDevice();
        if (device == null) {
            physicalTouchpadGeometry = TouchpadGeometry.UNKNOWN;
            return;
        }

        InputDevice.MotionRange xRange = device.getMotionRange(MotionEvent.AXIS_X, event.getSource());
        InputDevice.MotionRange yRange = device.getMotionRange(MotionEvent.AXIS_Y, event.getSource());

        physicalTouchpadCalibration = TouchpadCalibration.parse(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(TouchpadCalibrationActivity.PREF_KEY, null));

        physicalTouchpadGeometry = TouchpadGeometry.resolve(
                xRange != null ? xRange.getRange() : 0,
                yRange != null ? yRange.getRange() : 0,
                streamContainer != null ? streamContainer.getWidth() : 0,
                streamContainer != null ? streamContainer.getHeight() : 0,
                prefConfig.trackpadSwapAxis);

        LimeLog.info("Physical touchpad geometry: " +
                (physicalTouchpadGeometry.isRotated() ? "rotated" : "upright"));
    }

    /**
     * Pinça de dois dedos no trackpad, para ampliar a imagem.
     *
     * Desde a API 33 o Android classifica o gesto e entrega o fator pronto em
     * AXIS_GESTURE_PINCH_SCALE_FACTOR — não há dois ponteiros para um ScaleGestureDetector
     * acompanhar, porque o pad não é a tela. Abaixo disso a informação simplesmente não
     * existe no MotionEvent, e não há como sintetizá-la sem inventar.
     *
     * @return true se o evento era uma pinça e foi consumido aqui.
     */
    private boolean handlePhysicalTouchpadPinch(MotionEvent event) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || panZoomHandler == null) {
            return false;
        }

        if (event.getClassification() != MotionEvent.CLASSIFICATION_PINCH) {
            return false;
        }

        float factor = event.getAxisValue(MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR);
        if (factor <= 0) {
            return true;
        }

        // Ampliar sem estar no modo mover/zoom deixaria a imagem grande e presa, porque é
        // esse modo que liga o arrasto. A pinça entra nele sozinha, que é o que alguém que
        // acabou de apertar dois dedos para ampliar quer que aconteça.
        if (!isPanZoomMode && factor > 1f) {
            toggleZoomMode();
        }

        panZoomHandler.applyTrackpadPinch(factor);

        // E sai sozinha quando a pinça volta ao 1x — o inverso exato da linha acima.
        //
        // applyScale trava a escala em Math.max(1, ...), então pinçar de volta chega a 1.0
        // e isZoomed() vira false, mas isPanZoomMode continuava true para sempre. E
        // enquanto ele é true o ramo de dedos de handleMotionEvent devolve sem mandar
        // nada: o dedo na tela deixava de mexer no PC pelo resto da sessão, sem aviso, por
        // causa de uma pinça sem querer no pad da capa. A 1x não há o que arrastar de
        // qualquer forma — constrainToBounds recentra tudo.
        if (isPanZoomMode && !panZoomHandler.isZoomed()) {
            toggleZoomMode();
        }
        return true;
    }

    private void handlePhysicalTouchpadMotion(MotionEvent event) {
        if (handlePhysicalTouchpadPinch(event)) {
            return;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                break;
            default:
                // Any finger going down or up re-anchors. Carrying an anchor across a lift is
                // what makes the cursor jump when a second finger touches down.
                physicalTouchpadPointerId = -1;
                return;
        }

        // Two fingers on the pad is a scroll gesture, which arrives separately as ACTION_SCROLL.
        // Moving the cursor from it as well is what dragged the pointer around while scrolling.
        if (event.getPointerCount() != 1) {
            physicalTouchpadPointerId = -1;
            return;
        }

        int x = (int)event.getX();
        int y = (int)event.getY();

        if (physicalTouchpadPointerId != event.getPointerId(0)) {
            physicalTouchpadPointerId = event.getPointerId(0);
            physicalTouchpadLastX = x;
            physicalTouchpadLastY = y;
            return;
        }

        resolvePhysicalTouchpadGeometry(event);

        int rawDeltaX = x - physicalTouchpadLastX;
        int rawDeltaY = y - physicalTouchpadLastY;

        physicalTouchpadLastX = x;
        physicalTouchpadLastY = y;

        // A calibração vem primeiro: ela é MEDIDA no aparelho e vence qualquer dedução.
        // A geometria continua valendo para a escala, e para quem nunca calibrou ela é a
        // única coisa que há — mas onde as duas discordam sobre orientação, quem viu o
        // dedo se mover ganha de quem inferiu de um alcance de eixo.
        float padDx = physicalTouchpadCalibration.screenDeltaX(rawDeltaX, rawDeltaY);
        float padDy = physicalTouchpadCalibration.screenDeltaY(rawDeltaX, rawDeltaY);

        int deltaX = physicalTouchpadCalibration.isIdentity()
                ? physicalTouchpadGeometry.screenDeltaX(rawDeltaX, rawDeltaY)
                : Math.round(padDx * physicalTouchpadGeometry.scaleAlongX());
        int deltaY = physicalTouchpadCalibration.isIdentity()
                ? physicalTouchpadGeometry.screenDeltaY(rawDeltaX, rawDeltaY)
                : Math.round(padDy * physicalTouchpadGeometry.scaleAlongY());

        if (deltaX == 0 && deltaY == 0) {
            return;
        }

        conn.sendMouseMove(clampToShort(deltaX), clampToShort(deltaY));
    }

    /**
     * Solta no PC os modificadores que ficaram presos.
     *
     * O caso é o Alt+Tab na capa do S8 Ultra: o Android troca de app com o Alt ainda
     * descido, o KEY_UP nunca acontece, e o Windows continua achando que o Alt está
     * pressionado — clicar arrasta janela, teclas viram atalho. Zerar modifierFlags
     * limpava só o nosso lado; o outro lado continuava com a tecla no chão.
     *
     * Solta na ordem inversa da que sendKeys usa para descer, com o bit já removido do
     * byte de modificadores. Mandar KEY_UP de tecla que o host nunca viu descer é
     * inofensivo — bem menos que deixar uma pendurada.
     */
    /**
     * Solta no host os modificadores que ficaram no chao, e zera o estado local.
     *
     * As duas metades sao obrigatorias, e eu ja errei aqui: uma versao anterior deste
     * arquivo fazia `modifierFlags = 0` no gancho de foco, o que soltava a flag mas deixava
     * a tecla PRESA no Windows. Troquei por esta chamada e consertei aquilo -- e criei o
     * inverso, porque este metodo nunca zerava a flag. O bit fantasma passava a ser
     * carimbado em toda tecla seguinte, pelo resto da sessao.
     *
     * O efeito visivel era o backspace: Alt+Backspace no Windows e Desfazer, Ctrl+Backspace
     * apaga a palavra inteira. Ele apertava Backspace e o PC fazia outra coisa -- e curava
     * sozinho se por acaso encostasse no modificador certo, que e exatamente o "as vezes
     * funciona, as vezes nao" do relato.
     *
     * Preso em ModificadoresPresosTest.
     */
    private void releaseHeldModifiers() {
        if (conn == null || modifierFlags == 0) {
            // Zerar mesmo sem conexao: o estado local nao pode sobreviver ao motivo.
            modifierFlags = 0;
            return;
        }

        final short[][] mods = {
            { (short) 0xA0, KeyboardPacket.MODIFIER_SHIFT },
            { (short) 0xA2, KeyboardPacket.MODIFIER_CTRL },
            { (short) 0xA4, KeyboardPacket.MODIFIER_ALT },
            { (short) 0x5B, KeyboardPacket.MODIFIER_META },
        };

        // modifierFlags é int; o protocolo manda um byte. A conta é feita em int e só o
        // que vai no fio é estreitado.
        int restante = modifierFlags;
        for (int i = mods.length - 1; i >= 0; i--) {
            int bit = mods[i][1];
            if ((restante & bit) == 0) {
                continue;
            }
            restante &= ~bit;
            conn.sendKeyboardInput(mods[i][0], KeyboardPacket.KEY_UP, (byte) restante, (byte) 0);
        }

        // Soltou no host: agora o local pode ir a zero. Nesta ordem, sempre.
        modifierFlags = 0;
    }

    /**
     * Todo estado de teclado que nao pode sobreviver a uma perda de controle.
     *
     * O acorde Ctrl+Alt+Shift engole todo key down enquanto espera os modificadores
     * subirem. Se um unico KEY_UP se perder -- e ele se perde quando o acorde muda o foco
     * da janela -- a condicao de saida nunca chega e NENHUMA tecla mais atravessa para o
     * PC. O teclado fica morto pelo resto da sessao, sem nada na tela explicando.
     *
     * Quanto mais acordes existirem, mais provavel fica. Por isso o reset e um lugar so.
     */
    /**
     * Aproxima o ponto onde ele tocou por ultimo, ao abrir o teclado.
     *
     * Digitar num desktop remoto e digitar as cegas duas vezes: o campo e pequeno na
     * imagem reduzida, e o cursor que pisca la tem um pixel. Aproximar em volta do ultimo
     * toque poe o campo em tamanho de leitura sem que ele precise fazer pinca com o teclado
     * aberto -- que e o gesto mais desconfortavel que existe com a capa acoplada.
     *
     * Nao promete "achar o input": o app nao ve o layout do host. Promete mirar onde o dedo
     * esteve, que e onde o campo ganhou foco.
     */
    private void aproximarParaDigitar() {
        if (panZoomHandler == null || streamContainer == null) {
            return;
        }
        if (ultimoToqueX < 0 || ultimoToqueY < 0) {
            // Sem toque registrado nao ha o que mirar -- e mirar no centro seria aproximar
            // um lugar qualquer.
            return;
        }
        if (panZoomHandler.isZoomed()) {
            // Ja esta ampliado por escolha dele; nao mexemos no que ele montou.
            return;
        }

        zoomAntesDeDigitar = panZoomHandler.getScaleFactor();
        panAntesDeDigitarX = panZoomHandler.getChildX();
        panAntesDeDigitarY = panZoomHandler.getChildY();

        panZoomHandler.applyScale(ZOOM_PARA_DIGITAR, ultimoToqueX, ultimoToqueY);
        isPanZoomMode = true;
    }

    /** Devolve o enquadramento que valia antes de aproximar. */
    private void devolverZoomDeDigitar() {
        if (panZoomHandler == null || zoomAntesDeDigitar <= 0f) {
            return;
        }
        panZoomHandler.setInitialZoomAndPan(zoomAntesDeDigitar, panAntesDeDigitarX, panAntesDeDigitarY);
        isPanZoomMode = panZoomHandler.isZoomed();
        zoomAntesDeDigitar = 0f;
    }

    /**
     * Quanto aproximar ao digitar.
     *
     * 1,6x poe um campo de formulario em tamanho confortavel sem tirar o contexto em volta
     * -- e o contexto importa, porque e por ele que ele sabe em qual campo esta. Mais que
     * isso e olhar por um canudo.
     */
    private static final float ZOOM_PARA_DIGITAR = 1.6f;

    private void soltarEstadoDeTeclado() {
        releaseHeldModifiers();
        specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
        waitingForAllModifiersUp = false;
    }

    private static short clampToShort(int value) {
        return (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    private boolean handleTouchInput(MotionEvent event, TouchContext[] inputContextMap, boolean isTouchScreen) {
        // Actual invert logic is handled within the touch context
        return handleTouchInput(event, inputContextMap, isTouchScreen, false, event.getActionMasked(), event.getActionIndex(), event.getPointerCount());
    }

    private boolean handleTouchInput(MotionEvent event, TouchContext[] inputContextMap, boolean isTouchScreen, boolean invertAxis, int eventAction, int actionIndex, int pointerCount) {
        TouchContext context = getTouchContext(actionIndex, inputContextMap);
        if (context == null) {
            return false;
        }

        int actualActionIndex = event.getActionIndex();
        int actualPointerCount = event.getPointerCount();

        boolean shouldDuplicateMovement = actualPointerCount < pointerCount;

        if (eventAction == MotionEvent.ACTION_MOVE) {
            // ACTION_MOVE is special because it always has actionIndex == 0
            // We'll call the move handlers for all indexes manually

            // First process the historical events
            for (int i = 0; i < event.getHistorySize(); i++) {
                for (TouchContext aTouchContextMap : inputContextMap) {
                    if (aTouchContextMap.getActionIndex() < pointerCount)
                    {
                        int aActionIndex = shouldDuplicateMovement ? 0 : aTouchContextMap.getActionIndex();
                        int historicalX = (int)event.getHistoricalX(aActionIndex, i);
                        int historicalY = (int)event.getHistoricalY(aActionIndex, i);
                        if (isTouchScreen) {
                            float[] normalizedCoords = getNormalizedCoordinates(streamContainer, historicalX, historicalY);
                            historicalX = (int)normalizedCoords[0];
                            historicalY = (int)normalizedCoords[1];
                        }

                        // Invert axis again since synthetic events are not inverted
                        // Invert twice could correct the direction
                        // Blame Android for this problem
                        // some devices report inverted axis when trackpad pointer is captured
                        // but not when they're simulated as swipes on the screen
                        if (invertAxis) {
                            aTouchContextMap.touchMoveEvent(
                                    historicalY,
                                    historicalX,
                                    event.getHistoricalEventTime(i)
                            );
                        } else {
                            aTouchContextMap.touchMoveEvent(
                                    historicalX,
                                    historicalY,
                                    event.getHistoricalEventTime(i)
                            );
                        }
                    }
                }
            }

            // Now process the current values
            for (TouchContext aTouchContextMap : inputContextMap) {
                if (aTouchContextMap.getActionIndex() < pointerCount)
                {
                    int aActionIndex = shouldDuplicateMovement ? 0 : aTouchContextMap.getActionIndex();
                    int currentX = (int)event.getX(aActionIndex);
                    int currentY = (int)event.getY(aActionIndex);
                    if (isTouchScreen) {
                        float[] normalizedCoords = getNormalizedCoordinates(streamContainer, currentX, currentY);
                        currentX = (int)normalizedCoords[0];
                        currentY = (int)normalizedCoords[1];
                    }

                    // Invert axis again since synthetic events are not inverted
                    if (invertAxis) {
                        aTouchContextMap.touchMoveEvent(
                                currentY,
                                currentX,
                                event.getEventTime()
                        );
                    } else {
                        aTouchContextMap.touchMoveEvent(
                                currentX,
                                currentY,
                                event.getEventTime());
                    }
                }
            }

            return true;
        }

        int eventX = (int)event.getX(actualActionIndex);
        int eventY = (int)event.getY(actualActionIndex);

        // Handle view scaling
        if (isTouchScreen) {
            float[] normalizedCoords = getNormalizedCoordinates(streamContainer, eventX, eventY);
            eventX = (int)normalizedCoords[0];
            eventY = (int)normalizedCoords[1];
        }

        switch (eventAction)
        {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                for (TouchContext touchContext : inputContextMap) {
                    touchContext.setPointerCount(pointerCount);
                }
                context.touchDownEvent(eventX, eventY, event.getEventTime(), true);
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                if (prefConfig.touchscreenTrackpad) {
                    if (pointerCount == 1 &&
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || (event.getFlags() & MotionEvent.FLAG_CANCELED) == 0)) {
                        // All fingers up
                        long currentEventTime = event.getEventTime();
                        if (currentEventTime - threeFingerDownTime < THREE_FINGER_TAP_THRESHOLD) {
                            // This is a 3 finger tap to bring up the keyboard
                            toggleKeyboard();
                            return true;
                        } else if (currentEventTime - fourFingerDownTime < FOUR_FINGER_TAP_THRESHOLD) {
                            toggleFullKeyboard();
                            return true;
                        } else if (currentEventTime - fiveFingerDownTime < FIVE_FINGER_TAP_THRESHOLD) {
                            if (prefConfig.enableBackMenu) {
                                showGameMenu(null);
                            }
                            return true;
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                    context.cancelTouch();
                }
                else {
                    context.touchUpEvent(eventX, eventY, event.getEventTime());
                }

                for (TouchContext touchContext : inputContextMap) {
                    touchContext.setPointerCount(pointerCount - 1);
                }
                if (actionIndex == 0 && pointerCount > 1 && !context.isCancelled()) {
                    // The original secondary touch now becomes primary
                    int pointer1X = (int)event.getX(1);
                    int pointer1Y = (int)event.getY(1);
                    if (isTouchScreen) {
                        float[] normalizedCoords = getNormalizedCoordinates(streamContainer, pointer1X, pointer1Y);
                        pointer1X = (int)normalizedCoords[0];
                        pointer1Y = (int)normalizedCoords[1];
                    }
                    context.touchDownEvent(
                            pointer1X,
                            pointer1Y,
                            event.getEventTime(), false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                for (TouchContext aTouchContext : inputContextMap) {
                    aTouchContext.cancelTouch();
                    aTouchContext.setPointerCount(0);
                }
                break;
            default:
                return false;
        }

        return true;
    }

    private boolean handleMultiTouchGesture(MotionEvent event, int eventAction, int pointerCount, View view) {

        if (eventAction == MotionEvent.ACTION_POINTER_DOWN) {
            if (pointerCount == 3) {
                threeFingerDownTime = event.getEventTime();
            } else if (pointerCount == 4) {
                threeFingerDownTime = 0;
                fourFingerDownTime = event.getEventTime();
            } else if (pointerCount == 5) {
                threeFingerDownTime = 0;
                fourFingerDownTime = 0;
                fiveFingerDownTime = event.getEventTime();
            }
        }

        switch (eventAction) {
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                long currentEventTime = event.getEventTime();
                if (pointerCount >= 5 && fiveFingerDownTime > 0 && currentEventTime - fiveFingerDownTime < FIVE_FINGER_TAP_THRESHOLD) {
                    if (prefConfig.enableBackMenu) {
                        showGameMenu(null);
                    }
                    fiveFingerDownTime = 0;
                    break;
                } else if (pointerCount == 4 && fourFingerDownTime > 0 && currentEventTime - fourFingerDownTime < FOUR_FINGER_TAP_THRESHOLD) {
                    toggleFullKeyboard();
                    fourFingerDownTime = 0;
                    break;
                } else if (pointerCount == 3 && threeFingerDownTime > 0 && currentEventTime - threeFingerDownTime < THREE_FINGER_TAP_THRESHOLD) {
                    toggleKeyboard();
                    threeFingerDownTime = 0;
                    break;
                }
                threeFingerDownTime = 0;
                fourFingerDownTime = 0;
                fiveFingerDownTime = 0;

                cancelStaleTouchState(event, view);
                return false;
            default:
                return false;
        }

        cancelStaleTouchState(event, view);
        return true;
    }

    private void cancelStaleTouchState(MotionEvent event, View view) {
        MotionEvent cancelEvent = MotionEvent.obtain(event);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
        view.dispatchTouchEvent(cancelEvent);
        cancelEvent.recycle();
        for (TouchContext aTouchContext : touchContextMap) {
            aTouchContext.cancelTouch();
            aTouchContext.setPointerCount(0);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return handleMotionEvent(null, event) || super.onGenericMotionEvent(event);

    }

    private void updateMousePosition(View touchedView, MotionEvent event) {
        // X and Y are already relative to the provided view object
        float eventX, eventY;
        // For our StreamView itself, we can use the coordinates unmodified.

        if (touchedView == streamContainer) {
            eventX = event.getX(0);
            eventY = event.getY(0);
        } else {
            // For the containing background view, we must subtract the origin
            // of the StreamView to get video-relative coordinates.
            eventX = event.getX(0) - streamContainer.getX();
            eventY = event.getY(0) - streamContainer.getY();
        }

        if (event.getPointerCount() == 1 && event.getActionIndex() == 0 &&
                (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER ||
                        event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS))
        {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_EXIT:
                case MotionEvent.ACTION_HOVER_MOVE:
                    if (event.getEventTime() - lastAbsTouchUpTime <= STYLUS_UP_DEAD_ZONE_DELAY &&
                            Math.sqrt(Math.pow(eventX - lastAbsTouchUpX, 2) + Math.pow(eventY - lastAbsTouchUpY, 2)) <= STYLUS_UP_DEAD_ZONE_RADIUS) {
                        // Enforce a small deadzone between touch up and hover or touch down to allow more precise double-clicking
                        return;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    if (event.getEventTime() - lastAbsTouchDownTime <= STYLUS_DOWN_DEAD_ZONE_DELAY &&
                            Math.sqrt(Math.pow(eventX - lastAbsTouchDownX, 2) + Math.pow(eventY - lastAbsTouchDownY, 2)) <= STYLUS_DOWN_DEAD_ZONE_RADIUS) {
                        // Enforce a small deadzone between touch down and move or touch up to allow more precise double-clicking
                        return;
                    }
                    break;
            }
        }

        // We may get values slightly outside our view region on ACTION_HOVER_ENTER and ACTION_HOVER_EXIT.
        // Normalize these to the view size. We can't just drop them because we won't always get an event
        // right at the boundary of the view, so dropping them would result in our cursor never really
        // reaching the sides of the screen.
        eventX = Math.min(Math.max(eventX, 0), streamContainer.getWidth());
        eventY = Math.min(Math.max(eventY, 0), streamContainer.getHeight());

        conn.sendMousePosition((short)eventX, (short)eventY, (short) streamContainer.getWidth(), (short) streamContainer.getHeight());
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event) {
        return handleMotionEvent(view, event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Tell the OS not to buffer input events for us
            //
            // NB: This is still needed even when we call the newer requestUnbufferedDispatch()!
            view.requestUnbufferedDispatch(event);
        }

        return handleMotionEvent(view, event);
    }

    @Override
    public void stageStarting(final String stage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    if (spinner != null) {
                        spinner.setMessage(getResources().getString(R.string.conn_starting) + " " + stage);
                    }
                }

                // Os três passos do protótipo esperam evento real, não temporizador: os
                // tempos de lá (800/1600/2500 ms) eram encenação para o protótipo andar
                // sozinho. Aqui quem manda é a máquina de estados da conexão.
                if (vidroSession != null) {
                    vidroSession.onStageStarting(stage);
                }
            }
        });
    }

    @Override
    public void stageComplete(String stage) {
    }

    /**
     * Repete a tentativa sem sair da sessão.
     *
     * Com o host subindo um display virtual por cliente, falhar na primeira tentativa
     * enquanto o display ainda sobe é rotina — e o único desfecho que a folha oferecia era
     * "OK", que encerra a Activity e devolve ao catálogo, de onde repetir custa mais quatro
     * toques e uma navegação.
     *
     * A retentativa acontece AQUI DENTRO, sem relançar nada. Relançar seria o caminho
     * óbvio e é o errado: a Activity é singleTask com noHistory, então o relançamento pode
     * chegar como onNewIntent em vez de um onCreate limpo, e enquanto a instância velha
     * não termina de morrer as duas disputam estado ESTÁTICO — o listener do MoonBridge e
     * o semáforo de conexão do NvConnection. Chamar start() de novo não tem nenhum desses
     * problemas: é literalmente a mesma chamada que a superfície faz quando fica pronta.
     *
     * Só é oferecida enquanto o lado nativo não entrou em cena (ver
     * NvConnection.hasStartedNativeConnection()). Depois disso o decodificador já foi
     * liberado no cleanup e repetir exigiria um novo — outro problema, outra correção.
     */
    private void retryConnection() {
        // A folha desta tentativa já foi. A próxima falha precisa poder mostrar a sua.
        displayedFailureDialog = false;
        lastDisplayMessage = null;

        if (vidroSession != null) {
            // A capa é recarregada porque a tela pode ter sido escondida no meio: quem
            // tenta de novo tem de ver a mesma coisa que viu na primeira vez.
            VidroSession.carregarCapa(this,
                    getIntent().getIntExtra(EXTRA_APP_ID, 0),
                    getIntent().getStringExtra(EXTRA_PC_UUID));
            vidroSession.showConnecting(appName, pcName);
        }
        else {
            showVidroConnecting();
        }

        conn.start(new AndroidAudioRenderer(Game.this, prefConfig.enableAudioFx),
                decoderRenderer, Game.this);
    }

    private void stopConnection() {
        if (connecting || connected) {
            connecting = connected = false;
            updatePipAutoEnter();

            controllerHandler.stop();

            // Update GameManager state to indicate we're no longer in game
            UiHelper.notifyStreamEnded(this);

            // Stop may take a few hundred ms to do some network I/O to tell
            // the server we're going away and clean up. Let it run in a separate
            // thread to keep things smooth for the UI. Inside moonlight-common,
            // we prevent another thread from starting a connection before and
            // during the process of stopping this one.
            new Thread() {
                public void run() {
                    conn.stop();
                    if (httpConn != null && quitOnStop) {
                        // O aviso vai preso ao contexto da aplicação, não à Activity.
                        //
                        // Este bloco roda em onStop, com a sessão já encerrando: um aviso
                        // preso a uma Activity que está indo embora não chega a aparecer.
                        // Quem volta ao primeiro plano em seguida é o catálogo, e é lá que
                        // a pessoa precisa ler isto.
                        final Context toastContext = getApplicationContext();
                        String message;
                        try {
                            sleep(1000);
                            // quitApp() devolve false quando o host recusa encerrar. O
                            // retorno era descartado e a torrada anunciava sucesso sempre:
                            // a pessoa saía confiante e o jogo continuava rodando no PC,
                            // segurando o display virtual, e ela só descobria depois.
                            // ServerHelper.doQuit sempre ramificou aqui — os dois caminhos
                            // de encerrar não podiam discordar sobre a mesma pergunta.
                            message = ServerHelper.quitResultMessage(toastContext,
                                    httpConn.quitApp(), appName);
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = ServerHelper.quitFailureMessage(toastContext,
                                    e.getMessage(), appName);
                        }
                        final String finalMessage = message;
                        Game.this.runOnUiThread(() -> VidroToast.show(toastContext, finalMessage, Toast.LENGTH_LONG));
                    }
                }
            }.start();
        }
    }

    @Override
    public boolean stageFailed(final String stage, final int portFlags, final int errorCode) {
        // Perform a connection test if the failure could be due to a blocked port
        // This does network I/O, so don't do it on the main thread.
        //
        // Só que esse teste conversa com um servidor na internet, e para um host em
        // 192.168.x.x ele não tem o que responder: as portas que importam nesse caso nunca
        // passam pelo provedor. Rodava assim mesmo — resolução de DNS mais conexões TCP,
        // em CADA uma das tentativas internas, justamente quando a rede está ruim. Eram
        // segundos de tela parada antes de a folha aparecer, em troca de nada.
        //
        // Sem teste o resultado é "inconclusivo", que é a verdade e é o que o código
        // abaixo já sabia tratar.
        final int portTestResult = NetworkScope.shouldTestInternetPorts(host)
                ? MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443, portFlags)
                : MoonBridge.ML_TEST_RESULT_INCONCLUSIVE;

        // Repetir a tentativa só é oferecido enquanto o lado nativo não entrou em cena —
        // ver NvConnection.hasStartedNativeConnection(). Perguntado aqui, na thread da
        // conexão, e não lá dentro do runOnUiThread: o encerramento roda em paralelo e a
        // resposta mudaria embaixo da folha.
        //
        // Fora também o caminho de tela externa: lá o finishSecondScreen() logo abaixo
        // encerra a Activity dois segundos depois, e não há como oferecer repetir uma
        // sessão que já está com hora marcada para morrer.
        final boolean canRetry = conn != null && !conn.hasStartedNativeConnection()
                && !prefConfig.enableFullExDisplay;

        if (errorCode == 0 && portFlags != 0 && (portTestResult == MoonBridge.ML_TEST_RESULT_INCONCLUSIVE || portTestResult == 0)) {
            if (spinner != null) {
                spinner.setMessage(getResources().getString(R.string.unlocking_or_starting));
            }
            return true;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.dismiss();
                    spinner = null;
                }

                // A tela de conectar sai ANTES do cartão de erro. Sem isto as duas ficavam
                // na tela ao mesmo tempo, e a de baixo continuava mostrando barra de
                // progresso e "Cancelar" para uma conexão que já falhou.
                if (vidroSession != null) {
                    vidroSession.dismissConnecting();
                }

                if (!displayedFailureDialog) {
                    displayedFailureDialog = true;
                    LimeLog.severe(stage + " failed: " + errorCode);

                    // If video initialization failed and the surface is still valid, display extra information for the user
                    Surface currentSurface = streamContainer.getSurface();
                    if (stage.contains("video") && currentSurface != null && currentSurface.isValid()) {
                        VidroToast.show(Game.this, getResources().getText(R.string.video_decoder_init_failed), Toast.LENGTH_LONG);
                    }

                    String dialogText = getResources().getString(R.string.conn_error_msg) + " " + stage +" (error "+errorCode+")";

                    // O motivo real entra na folha, logo abaixo do cabeçalho.
                    //
                    // "(error 0)" não é motivo. O motivo estava em displayMessage e passou
                    // como aviso atrás desta mesma folha, sem tempo de ser lido.
                    String reason = limparEndereco(lastDisplayMessage);
                    if (reason != null && !reason.isEmpty()) {
                        dialogText += "\n\n" + reason;
                    }

                    switch (errorCode) {
                        case 403: {
                            dialogText += "\n\n" + getResources().getString(R.string.error_msg_permission_denied) + " (" + getResources().getString(R.string.permission_launch_app) + ")";
                            break;
                        }
                        case -408: {
                            dialogText += "\n\n" + getResources().getString(R.string.error_msg_timeout);
                            break;
                        }
                        default: {
                            // do nothing
                        }
                    }

                    if (portFlags != 0) {
                        dialogText += "\n\n" + getResources().getString(R.string.check_ports_msg) + "\n" +
                                MoonBridge.stringifyPortFlags(portFlags, "\n");
                    }

                    if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0)  {
                        dialogText += "\n\n" + getResources().getString(R.string.nettest_text_blocked);
                    }

                    if (canRetry) {
                        Dialog.displayDialog(Game.this, getResources().getString(R.string.conn_error_title),
                                dialogText, true,
                                getResources().getString(R.string.conn_retry), new Runnable() {
                                    @Override
                                    public void run() {
                                        retryConnection();
                                    }
                                });
                    }
                    else {
                        Dialog.displayDialog(Game.this, getResources().getString(R.string.conn_error_title), dialogText, true);
                    }
                    finishSecondScreen();
                }
            }
        });

        return false;
    }

    private void finishSecondScreen() {
        // Otherwise screen stays connected but not working with no way of quitting it
        if (prefConfig.enableFullExDisplay) {
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 2000);
        }
    }

    @Override
    public void connectionTerminated(final int errorCode) {
        // O PRIMEIRO SINAL, e ele tem de ser aqui em cima.
        //
        // Logo abaixo vem o teste de portas, que faz I/O de rede. Congelar depois dele seria
        // congelar um buffer que já não existe — um retângulo preto, que é pior que o corte
        // que isto veio substituir.
        congelarUltimoQuadro();

        // Perform a connection test if the failure could be due to a blocked port
        // This does network I/O, so don't do it on the main thread.
        final int portFlags = MoonBridge.getPortFlagsFromTerminationErrorCode(errorCode);
        // Mesmo motivo do stageFailed: contra um host da rede local o teste não responde
        // nada e ainda cobra DNS mais conexões TCP com a rede já ruim.
        final int portTestResult = NetworkScope.shouldTestInternetPorts(host)
                ? MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER,443, portFlags)
                : MoonBridge.ML_TEST_RESULT_INCONCLUSIVE;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Let the display go to sleep now
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Stop processing controller input
                controllerHandler.stop();
                timerHandler.removeCallbacksAndMessages(null);

                // Ungrab input
                setInputGrabState(false);

                // Mesmo motivo do stageFailed: quem cai antes do primeiro quadro ainda
                // está com a tela de conectar de pé, e o aviso empilharia por cima dela.
                if (vidroSession != null) {
                    vidroSession.dismissConnecting();
                }

                if (!displayedFailureDialog) {
                    displayedFailureDialog = true;
                    LimeLog.severe("Connection terminated: " + errorCode);
                    stopConnection();

                    // Display the error dialog if it was an unexpected termination.
                    // Otherwise, just finish the activity immediately.
                    if (errorCode != MoonBridge.ML_ERROR_GRACEFUL_TERMINATION) {
                        String message;

                        if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
                            // If we got a blocked result, that supersedes any other error message
                            message = getResources().getString(R.string.nettest_text_blocked);
                        }
                        else {
                            switch (errorCode) {
                                case MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC:
                                    message = getResources().getString(R.string.no_video_received_error);
                                    break;

                                case MoonBridge.ML_ERROR_NO_VIDEO_FRAME:
                                    message = getResources().getString(R.string.no_frame_received_error);
                                    break;

                                case MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION:
                                case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
                                    message = getResources().getString(R.string.early_termination_error);
                                    break;

                                case MoonBridge.ML_ERROR_FRAME_CONVERSION:
                                    message = getResources().getString(R.string.frame_conversion_error);
                                    break;

                                default:
                                    String errorCodeString;
                                    // We'll assume large errors are hex values
                                    if (Math.abs(errorCode) > 1000) {
                                        errorCodeString = Integer.toHexString(errorCode);
                                    }
                                    else {
                                        errorCodeString = Integer.toString(errorCode);
                                    }
                                    message = getResources().getString(R.string.conn_terminated_msg) + "\n\n" +
                                            getResources().getString(R.string.error_code_prefix) + " " + errorCodeString;
                                    break;
                            }
                        }

                        // O que a conexão contou antes de cair, quando contou algo.
                        //
                        // Fica nulo assim que o primeiro quadro chega (ver
                        // connectionStarted), então só sobrevive aqui a queda que aconteceu
                        // ANTES de a sessão abrir — que é justamente aquela em que o host
                        // tem algo a dizer.
                        String reason = limparEndereco(lastDisplayMessage);
                        if (reason != null && !reason.isEmpty() && !message.contains(reason)) {
                            message += "\n\n" + reason;
                        }

                        if (portFlags != 0) {
                            message += "\n\n" + getResources().getString(R.string.check_ports_msg) + "\n" +
                                    MoonBridge.stringifyPortFlags(portFlags, "\n");
                        }

                        // A queda DEPOIS que o vídeo já estava rodando ganha a tela do
                        // protótipo, não o diálogo do sistema.
                        //
                        // `vidro_lost.xml` está incluído em toda sessão desde sempre e nunca
                        // apareceu — nenhuma linha procurava os ids. Enquanto isso, quando a
                        // rede caía no meio da partida subia um AlertDialog com código
                        // hexadecimal, lista de portas e um botão "Ajuda" que jogava a pessoa
                        // na wiki de outro projeto, fora do app. É o momento em que ela mais
                        // precisa do app e ele trocava de personalidade.
                        //
                        // Quem cai ANTES do primeiro quadro continua no caminho de M99: lá a
                        // tela de conectar ainda está de pé e a folha de falha dela é a
                        // superfície certa.
                        if (jaTinhaVideo && vidroSession != null) {
                            vidroSession.showLost(
                                    pcName,
                                    message,
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            // Reabre a mesma sessão pelo caminho que ela usou
                                            // para nascer. Sem retentativa automática nesta
                                            // fatia: ver o comentário de showLost.
                                            recreate();
                                        }
                                    },
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            finish();
                                        }
                                    });
                        }
                        else {
                            Dialog.displayDialog(Game.this, getResources().getString(R.string.conn_terminated_title),
                                    message, true);
                        }
                    }
                    else {
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public void connectionStatusUpdate(final int connectionStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // A qualidade e dita pela MOLDURA da imagem, e ela continua sendo dita
                // mesmo com os avisos desligados.
                //
                // "Desabilitar mensagens de alerta" silencia a FAIXA DE TEXTO -- e o que o
                // titulo dela diz. A guarda de disableWarnings fica ABAIXO daqui de
                // proposito: quem desligou o texto nao pediu para deixar de ver que a rede
                // esta ruim, e a moldura nao interrompe ninguem.
                if (moldura != null) {
                    if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
                        moldura.setEstado(MolduraDeEstado.RUIM);
                    }
                    else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
                        moldura.setEstado(MolduraDeEstado.BOM);
                    }
                }

                if (prefConfig.disableWarnings) {
                    return;
                }

                // A FAIXA DE TEXTO SAIU DA QUALIDADE DE REDE, e a moldura acima ficou com
                // o assunto inteiro.
                //
                // "Segurando o sinal · a imagem pode travar" era uma frase para LER, no topo
                // da tela, no meio de uma partida -- exatamente onde a atencao nao pode ir.
                // E ela nao acrescentava nada acionavel: nao ha o que fazer a respeito da
                // rede a partir dali. A moldura diz a mesma coisa pela periferia da visao,
                // sem tirar ninguem do jogo, e diz tambem com os avisos desligados.
            }
        });
    }

    @Override
    public void connectionStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.dismiss();
                    spinner = null;
                }

                connected = true;
                connecting = false;
                updatePipAutoEnter();

                // A partir daqui o que foi dito na negociação já não explica nada: uma
                // queda daqui a duas horas não tem a ver com o que o host respondeu ao
                // lançar o app. Sem isto a folha de "conexão encerrada" viria com um
                // motivo velho colado nela.
                lastDisplayMessage = null;

                if (vidroSession != null) {
                    vidroSession.onConnected();
                }

                jaTinhaVideo = true;

                // Ha video, e por isso NADA aparece.
                //
                // A ilha abria inteira por cima da imagem em toda conexao nova -- a primeira
                // coisa que se via do jogo era um painel cobrindo o rodape dele. O repouso
                // da sessao e a tela vazia; o painel e convocado, nao oferecido.

                // Hide the mouse cursor now after a short delay.
                // Doing it before dismissing the spinner seems to be undone
                // when the spinner gets displayed. On Android Q, even now
                // is too early to capture. We will delay a second to allow
                // the spinner to dismiss before capturing.
                timerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setInputGrabState(true);
                    }
                }, 500);

                // Keep the display on
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Update GameManager state to indicate we're in game
                UiHelper.notifyStreamConnected(Game.this);

                // Sync local clipboard to host
                handleFocusChange(true);

                hideSystemUi(1000);

                if (prefConfig.preventPacketLoss) {
                    timerHandler.postDelayed(backgroundPing, 1000);
                }
            }
        });

        if (prefConfig.usbDriver) {
            // Start the USB driver
            bindService(new Intent(this, UsbDriverService.class),
                    usbDriverServiceConnection, Service.BIND_AUTO_CREATE);
        }

        // Report this shortcut being used (off the main thread to prevent ANRs)
        ComputerDetails computer = new ComputerDetails();
        computer.name = pcName;
        computer.uuid = Game.this.getIntent().getStringExtra(EXTRA_PC_UUID);
        ShortcutHelper shortcutHelper = new ShortcutHelper(this);
        shortcutHelper.reportComputerShortcutUsed(computer);
        if (appName != null) {
            // This may be null if launched from the "Resume Session" PC context menu item
            shortcutHelper.reportGameLaunched(computer, app);
        }
    }

    /**
     * Tira a barra que o {@code InetAddress.toString()} põe na frente do endereço.
     *
     * "Failed to connect to /127.0.0.1:47989" — a barra é o formato interno do Java
     * (hostname/literal, com o hostname vazio), e para quem lê parece erro de digitação do
     * app. Só a barra imediatamente antes de um dígito sai; um caminho de arquivo num texto
     * de erro qualquer não é tocado.
     */
    private static String limparEndereco(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replaceAll("(?<=\\s)/(?=\\d)", "");
    }

    @Override
    public void displayMessage(final String message) {
        // Guardado para a folha de falha.
        //
        // Este é o único lugar de onde sai o motivo de verdade — "o dispositivo não está
        // pareado", "o host não tem esse app na lista" — e ele saía daqui como aviso
        // passageiro, atrás de um diálogo modal que não podia ser descartado. Aparecia e
        // sumia sem que desse para ler, e o que ficava na tela era "(error 0)".
        lastDisplayMessage = message;

        // ANTES DO PRIMEIRO QUADRO, SÓ A FOLHA DE ERRO FALA.
        //
        // Toda falha de arranque termina na folha de stageFailed/connectionTerminated, que
        // já incorpora este texto como o motivo. Soltar a torrada também era dizer a mesma
        // frase duas vezes na mesma tela — medido contra 127.0.0.1: "Failed to connect…" no
        // topo e idêntico dentro do cartão logo abaixo. Em sessão a torrada volta a ser o
        // canal certo: aviso passageiro sobre um vídeo que continua rodando.
        if (!jaTinhaVideo) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VidroToast.show(Game.this, message, Toast.LENGTH_LONG);
            }
        });
    }

    @Override
    public void displayTransientMessage(final String message) {
        if (!prefConfig.disableWarnings) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VidroToast.show(Game.this, message, Toast.LENGTH_LONG);
                }
            });
        }
    }

    @Override
    public void rumble(short controllerNumber, short lowFreqMotor, short highFreqMotor) {
        if (prefConfig.enableRumble) {
            LimeLog.info(String.format((Locale)null, "Rumble on gamepad %d: %04x %04x", controllerNumber, lowFreqMotor, highFreqMotor));
            controllerHandler.handleRumble(controllerNumber, lowFreqMotor, highFreqMotor);
        }
    }

    @Override
    public void rumbleTriggers(short controllerNumber, short leftTrigger, short rightTrigger) {
        LimeLog.info(String.format((Locale)null, "Rumble on gamepad triggers %d: %04x %04x", controllerNumber, leftTrigger, rightTrigger));

        controllerHandler.handleRumbleTriggers(controllerNumber, leftTrigger, rightTrigger);
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        LimeLog.info("Display HDR mode: " + (enabled ? "enabled" : "disabled"));
        decoderRenderer.setHdrMode(enabled, hdrMetadata);
    }

    @Override
    public void setMotionEventState(short controllerNumber, byte motionType, short reportRateHz) {
        controllerHandler.handleSetMotionEventState(controllerNumber, motionType, reportRateHz);
    }

    @Override
    public void setControllerLED(short controllerNumber, byte r, byte g, byte b) {
        controllerHandler.handleSetControllerLED(controllerNumber, r, g, b);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!surfaceCreated) {
            throw new IllegalStateException("Surface changed before creation!");
        }

        LimeLog.info("surfaceChanged-->"+width+" x "+height + "----"+displayWidth+" x "+displayHeight);

        panZoomHandler.handleSurfaceChange();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isInPictureInPictureMode()) {
                updatePipAutoEnter();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        float desiredFrameRate;

        surfaceCreated = true;

        // Android will pick the lowest matching refresh rate for a given frame rate value, so we want
        // to report the true FPS value if refresh rate reduction is enabled. We also report the true
        // FPS value if there's no suitable matching refresh rate. In that case, Android could try to
        // select a lower refresh rate that avoids uneven pull-down (ex: 30 Hz for a 60 FPS stream on
        // a display that maxes out at 50 Hz).
        if (mayReduceRefreshRate() || desiredRefreshRate < prefConfig.fps) {
            desiredFrameRate = prefConfig.fps;
        }
        else {
            // Otherwise, we will pretend that our frame rate matches the refresh rate we picked in
            // prepareDisplayForRendering(). This will usually be the highest refresh rate that our
            // frame rate evenly divides into, which ensures the lowest possible display latency.
            desiredFrameRate = desiredRefreshRate;
        }

        // Tell the OS about our frame rate to allow it to adapt the display refresh rate appropriately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // We want to change frame rate even if it's not seamless, since prepareDisplayForRendering()
            // will not set the display mode on S+ if it only differs by the refresh rate. It depends
            // on us to trigger the frame rate switch here.
            holder.getSurface().setFrameRate(desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            holder.getSurface().setFrameRate(desiredFrameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!surfaceCreated) {
            throw new IllegalStateException("Surface destroyed before creation!");
        }

        if (attemptedConnection) {
            // Let the decoder know immediately that the surface is gone
            decoderRenderer.prepareForStop();

            if (connected) {
                stopConnection();
            }
        }
    }

    @Override
    public void mouseMove(int deltaX, int deltaY) {
        conn.sendMouseMove((short) deltaX, (short) deltaY);
    }

    @Override
    public void mouseButtonEvent(int buttonId, boolean down) {
        byte buttonIndex;

        switch (buttonId)
        {
            case EvdevListener.BUTTON_LEFT:
                buttonIndex = MouseButtonPacket.BUTTON_LEFT;
                break;
            case EvdevListener.BUTTON_MIDDLE:
                buttonIndex = MouseButtonPacket.BUTTON_MIDDLE;
                break;
            case EvdevListener.BUTTON_RIGHT:
                buttonIndex = MouseButtonPacket.BUTTON_RIGHT;
                break;
            case EvdevListener.BUTTON_X1:
                buttonIndex = MouseButtonPacket.BUTTON_X1;
                break;
            case EvdevListener.BUTTON_X2:
                buttonIndex = MouseButtonPacket.BUTTON_X2;
                break;
            default:
                LimeLog.warning("Unhandled button: "+buttonId);
                return;
        }

        if (down) {
            conn.sendMouseButtonDown(buttonIndex);
        }
        else {
            conn.sendMouseButtonUp(buttonIndex);
        }
    }

    @Override
    public void mouseVScroll(byte amount) {
        conn.sendMouseScroll(amount);
    }

    @Override
    public void mouseHScroll(byte amount) {
        conn.sendMouseHScroll(amount);
    }

    @Override
    public void keyboardEvent(boolean buttonDown, short keyCode) {
        short keyMap = keyboardTranslator.translate(keyCode, 0, -1);
        if (keyMap != 0) {
            // handleSpecialKeys() takes the Android keycode
            if (handleSpecialKeys(keyCode, buttonDown)) {
                return;
            }

            if (buttonDown) {
                conn.sendKeyboardInput(keyMap, KeyboardPacket.KEY_DOWN, getModifierState(), (byte)0);
            }
            else {
                conn.sendKeyboardInput(keyMap, KeyboardPacket.KEY_UP, getModifierState(), (byte)0);
            }
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Don't do anything if we're not connected
        if (!connected) {
            return;
        }

        // This flag is set for all devices
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            hideSystemUi(2000);
        }
        else if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            hideSystemUi(2000);
        }
    }

    /** Quando os Números foram atualizados pela última vez. Ver vigiaDosNumeros. */
    private long ultimoPerfUpdate;

    /**
     * Um relógio que denuncia quando os Números param de chegar.
     *
     * A virada da janela de estatística mora DENTRO do caminho que recebe um quadro da
     * rede. Sem quadro chegando não há virada, não há atualização, e o overlay continua
     * mostrando o último segundo saudável — 60 fps, 0% de perda — exatamente enquanto a
     * imagem está congelada. Ele olha para o canto justamente no engasgo, lê boa notícia, e
     * conclui que os números não servem para nada. É a definição de aviso aprendido a
     * ignorar.
     *
     * Isto não recalcula nada: só percebe o silêncio e diz o que ele significa.
     */
    private final Runnable vigiaDosNumeros = new Runnable() {
        @Override
        public void run() {
            if (!prefConfig.enablePerfOverlay || isFinishing()) {
                return;
            }

            long parado = SystemClock.uptimeMillis() - ultimoPerfUpdate;
            if (ultimoPerfUpdate > 0 && parado > SILENCIO_DE_VIDEO_MS) {
                String aviso = getString(R.string.vidro_perf_sem_video, parado / 1000);
                if (prefConfig.enablePerfOverlayLite) {
                    performanceOverlayLite.setText(aviso);
                } else {
                    performanceOverlayBig.setText(aviso);
                }
            }

            performanceOverlayView.postDelayed(this, 500);
        }
    };

    /** A partir de quanto tempo sem quadro os Números passam a dizer isso em vez de mentir. */
    private static final long SILENCIO_DE_VIDEO_MS = 1500;

    @Override
    public void onPerfUpdate(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ultimoPerfUpdate = SystemClock.uptimeMillis();
                if(prefConfig.enablePerfOverlayLite){
                    performanceOverlayLite.setText(text);
                }else{
                    performanceOverlayBig.setText(text);
                }
            }
        });
    }

    @Override
    public void onUsbPermissionPromptStarting() {
        // Disable PiP auto-enter while the USB permission prompt is on-screen. This prevents
        // us from entering PiP while the user is interacting with the OS permission dialog.
        suppressPipRefCount++;
        updatePipAutoEnter();
    }

    @Override
    public void onUsbPermissionPromptCompleted() {
        suppressPipRefCount--;
        updatePipAutoEnter();
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        switch (keyEvent.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return handleKeyDown(keyEvent);
            case KeyEvent.ACTION_UP:
                return handleKeyUp(keyEvent);
            case KeyEvent.ACTION_MULTIPLE:
                return handleKeyMultiple(keyEvent);
            default:
                return false;
        }
    }

    /**
     * Escolher outra coisa com a casca aberta — a sessão troca na MESMA janela.
     *
     * <h2>Por que isto passou a existir</h2>
     *
     * A casca dentro da sessão continua abrindo um app do jeito de sempre: um Intent para
     * {@code Game}. Como ela é {@code singleTask}, esse Intent não cria Activity nova — ele
     * cai aqui. Sem este método o Intent era descartado em silêncio e tocar num app não
     * fazia nada, que é o pior desfecho: nada acontece e nada explica.
     *
     * <h2>Por que `recreate()` e não uma troca por dentro</h2>
     *
     * Porque {@code onCreate} tem 644 linhas de ordenação comprada com relato de campo — a
     * conexão, o decodificador, os controles e a janela nascem numa ordem que já quebrou
     * antes. Reproduzir essa ordem num segundo caminho é criar um segundo lugar onde ela
     * pode divergir.
     *
     * <p>{@code recreate()} roda o MESMO {@code onCreate}, com o Intent novo, na mesma task e
     * na mesma janela — não há troca de Activity para o Android animar, então não há o pop de
     * 0,94 nem o preto entre uma sessão e outra. E o {@code onStop()} que ele dispara derruba
     * a conexão antiga, que é exatamente o que se quer ao trocar de sessão.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Sem os extras da sessão não há o que abrir: é a reentrada por atalho apontando
        // para a própria tela, e o certo é continuar onde se estava.
        if (intent == null || intent.getStringExtra(EXTRA_HOST) == null) {
            return;
        }

        setIntent(intent);
        recreate();
    }

    @Override
    public void onBackPressed() {
        // A CASCA PRIMEIRO, e ela decide sozinha.
        //
        // `super.onBackPressed()` roteia para o OnBackPressedDispatcher, e é lá que vive o
        // BackHandler do Compose da Casca. Ele desempilha (catálogo → home) e, quando a
        // pilha acaba, chama a saída dela — que aqui é esconderCasca(), devolvendo o vídeo.
        //
        // Tratar direto com esconderCasca() era o erro óbvio: o Voltar dentro do catálogo
        // pularia a home inteira e cairia no vídeo, engolindo um nível de navegação.
        switch (decideVoltar(cascaVisivel(),
                vidroGaveta != null && vidroGaveta.estaAberta(),
                prefConfig.enableBackMenu,
                jaTinhaVideo)) {
            case CASCA:
                super.onBackPressed();
                return;
            case FECHAR_GAVETA:
                vidroGaveta.fechar();
                return;
            case ABRIR_CONTROLES:
                showGameMenu(null);
                return;
            case CONVOCAR_CASCA:
                mostrarCasca();
                return;
            default:
                super.onBackPressed();
        }
    }

    /** Os desfechos possíveis do Voltar dentro da sessão, em ordem de prioridade. */
    enum Voltar {
        /** A casca está na frente e decide sozinha — ela tem pilha própria. */
        CASCA,
        /** A gaveta está aberta: ela é a superfície de controle e consome o Voltar. */
        FECHAR_GAVETA,
        /** `enableBackMenu` ligado: o Voltar é o atalho para os controles. */
        ABRIR_CONTROLES,
        /** Havia vídeo: sair da sessão traz a casca por cima, sem encerrar nada. */
        CONVOCAR_CASCA,
        /** Nada a fazer aqui — o comportamento do sistema. */
        SISTEMA,
    }

    /**
     * A ORDEM DO VOLTAR, e ela é a única coisa que este método decide.
     *
     * Separada do `onBackPressed` porque a ordem já errou uma vez e errou em silêncio: com a
     * gaveta aberta, o Voltar caía no ramo do `enableBackMenu` — que passou a ABRIR a gaveta
     * quando o menu legado foi absorvido — e a reabria em cima de si mesma. Medido no
     * emulador: dois Voltares seguidos e o painel continuava idêntico na tela.
     *
     * A regra que sai disso: **quem está na frente consome primeiro**. A casca está na frente
     * da gaveta, a gaveta está na frente do vídeo, e só depois de nenhuma delas estar aberta é
     * que o Voltar pode significar "abrir alguma coisa".
     *
     * <p>Estática e sem argumento de Android de propósito: é a parte testável, e testar
     * ordenação de ramos não precisa de aparelho.
     */
    static Voltar decideVoltar(boolean cascaVisivel, boolean gavetaAberta,
                               boolean menuNoVoltar, boolean jaTinhaVideo) {
        if (cascaVisivel) {
            return Voltar.CASCA;
        }
        if (gavetaAberta) {
            return Voltar.FECHAR_GAVETA;
        }
        if (menuNoVoltar) {
            return Voltar.ABRIR_CONTROLES;
        }
        if (jaTinhaVideo) {
            return Voltar.CONVOCAR_CASCA;
        }
        return Voltar.SISTEMA;
    }

    public void sendExecServerCmd(int cmdId) {
        conn.sendExecServerCmd(cmdId);
    }

    public ArrayList<String> getServerCmds() {
        return serverCommands;
    }

    /**
     * O que o host desta sessão sabe fazer. Nunca nulo: host desconhecido devolve
     * {@link HostCapabilities#unknown()}, que não habilita nada.
     */
    public HostCapabilities getHostCapabilities() {
        return hostCapabilities;
    }

    public boolean isZoomModeEnabled() {
        return isPanZoomMode;
    }
    public void toggleZoomMode() {
        this.isPanZoomMode = !this.isPanZoomMode;
        if (this.isPanZoomMode) {
            VidroToast.show(this, getString(R.string.pan_zoom_mode_enabled), Toast.LENGTH_SHORT);
        } else {
            VidroToast.show(this, getString(R.string.pan_zoom_mode_disabled), Toast.LENGTH_SHORT);
        }

        if (ExternalDisplayControlActivity.instance != null) {
            ExternalDisplayControlActivity.instance.toggleZoomMode(false);
        }
    }

    public void rotateScreen() {
        // Pelo estado REAL da configuração, não por um campo que pode ter envelhecido:
        // girar duas vezes tem de voltar ao começo, aconteça o que acontecer no meio.
        boolean estaEmPaisagem = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        orientacaoForcada = estaEmPaisagem
                ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
        currentOrientation = estaEmPaisagem
                ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;
        setRequestedOrientation(orientacaoForcada);
    }

    /**
     * Initializes and applies the appropriate mouse mode based on user preferences
     * and whether the app is running in secondary display mode (such as Samsung DeX).
     *
     * Behavior:
     * - If the app is in secondary display mode:
     *   - Applies the user's saved mouse mode if it's one of the supported modes:
     *     "Trackpad Natural", "Trackpad Gaming", or "Disabled".
     *   - Otherwise, defaults to applying the "Trackpad Natural" mode.
     *
     * - If the app is not in secondary display mode:
     *   - Applies the user's saved mouse mode as is.
     *
     * This ensures the correct input mode is applied depending on the environment,
     * improving compatibility with desktop-like multi-display modes.
     */
    private void initMouseMode() {
        String savedMouseModeIndexStr = ProfilesManager.getInstance()
                .getOverlayingSharedPreferences(this)
                .getString("mouse_mode_list", "0");

        int savedMouseModeIndex;
        try {
            savedMouseModeIndex = Integer.parseInt(savedMouseModeIndexStr);
        } catch (NumberFormatException e) {
            savedMouseModeIndex = MOUSE_MODE_MULTITOUCH;
        }

        // Em tela externa o modo é sobrescrito só para esta sessão, nunca gravado: o que ele
        // escolheu continua valendo quando a tela externa sair.
        if (isOnExternalDisplay() && !worksOnExternalDisplay(savedMouseModeIndex)) {
            applyMouseMode(MOUSE_MODE_TRACKPAD);
        } else {
            applyMouseMode(savedMouseModeIndex);
        }
    }

    /**
     * Displays a dialog allowing the user to select a mouse input mode.
     *
     * Behavior:
     * - On regular displays, all available mouse modes are shown.
     * - On secondary displays (e.g. Samsung DeX), only a limited set of modes are allowed:
     *   "Trackpad Natural", "Trackpad Gaming", and "Disabled".
     * - An additional option to toggle the local mouse cursor is always included.
     *
     * When the user selects a mode:
     * - If it's the toggle option, the local mouse cursor mode is toggled.
     * - Otherwise, the selected mode is applied and saved to shared preferences.
     *
     * @param context The context to use to decide where to show the dialog.
     */
    public void selectMouseMode(Context context){
        String[] allModes = getResources().getStringArray(R.array.mouse_mode_names);

        List<MouseModeOption> options = new ArrayList<>();

        for (int i = 0; i < allModes.length; i++) {
            if (!isOnExternalDisplay() || worksOnExternalDisplay(i)) {
                options.add(new MouseModeOption(i, allModes[i]));
            }
        }

        options.add(new MouseModeOption(-1, getString(R.string.toggle_local_mouse_cursor)));

        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
        }
        final MouseModeOption[] optionArray = options.toArray(new MouseModeOption[0]);

        new AlertDialog.Builder(context, R.style.VidroDialogPlatform)
                .setTitle(getString(R.string.game_menu_select_mouse_mode))
                .setItems(labels, (dialog, which) -> {
                    dialog.dismiss();
                    MouseModeOption selected = optionArray[which];
                    if (selected.index == -1) {
                        toggleMouseLocalCursor();
                    } else {
                        // Mesmo caminho que os chips da folha usam: aplicar e, se for para
                        // lembrar, gravar. Duas cópias da mesma gravação sairiam do lugar
                        // na primeira vez que uma delas mudasse.
                        setMouseMode(selected.index);
                    }
                })
                .create()
                .show();
    }

    //本地鼠标光标切换
    @Override
    public void toggleMouseLocalCursor(){
        if (!grabbedInput) {
            inputCaptureProvider.enableCapture();
            grabbedInput = true;
        }
        cursorVisible = !cursorVisible;
        if (cursorVisible) {
            inputCaptureProvider.showCursor();
        } else {
            inputCaptureProvider.hideCursor();
        }
    }

    @Override
    public boolean isLocalCursorVisible() {
        return cursorVisible;
    }

    /**
     * O modo do mouse em vigor NESTA sessão.
     *
     * Não vem da preferência salva: "lembrar o modo do mouse" é opcional, e com ela
     * desligada o que está gravado é o modo de outra sessão qualquer. A folha precisa
     * saber o que vale agora, senão acende o chip errado.
     */
    private int currentMouseMode = 0;

    @Override
    public boolean canChangeMouseMode() {
        return allowChangeMouseMode;
    }

    @Override
    public int getMouseMode() {
        return currentMouseMode;
    }

    /**
     * Troca o modo do mouse com a sessão de pé — o que "Selecionar modo do mouse" fazia,
     * sem o diálogo em cima do diálogo.
     */
    @Override
    public void setMouseMode(int mode) {
        applyMouseMode(mode);

        if (prefConfig.rememberMouseMode) {
            ProfilesManager.getInstance().getOverlayingSharedPreferences(this)
                    .edit()
                    .putString("mouse_mode_list", String.valueOf(mode))
                    .apply();
        }
    }

    /**
     * Os modos do mouse, pelos números que applyMouseMode sempre usou.
     *
     * A lista dos Ajustes hoje oferece três (multitoque, absoluto, trackpad), mas os outros
     * continuam sendo estados válidos em runtime: 3 e 5 sobrevivem em preferência antiga até
     * a migração reescrevê-la, e 4 é o que o interruptor "o toque na tela move o mouse"
     * grava quando está desligado. Ramificar por número mantém isso verdadeiro mesmo quando
     * a lista visível encolhe de novo.
     */
    private static final int MOUSE_MODE_MULTITOUCH = 0;
    private static final int MOUSE_MODE_ABSOLUTE = 1;
    private static final int MOUSE_MODE_TRACKPAD = 2;
    private static final int MOUSE_MODE_TRACKPAD_GAMING = 3;
    private static final int MOUSE_MODE_TOUCH_OFF = 4;

    /**
     * Modos que fazem sentido numa tela externa.
     *
     * Numa segunda tela não há dedo sobre a imagem, então apontar em coordenada absoluta
     * não tem como funcionar: o que sobra é mover um cursor por deslocamento, ou não mexer
     * no mouse. Era isto que a comparação por rótulo tentava dizer.
     */
    private static boolean worksOnExternalDisplay(int mode) {
        return mode == MOUSE_MODE_TRACKPAD
                || mode == MOUSE_MODE_TRACKPAD_GAMING
                || mode == MOUSE_MODE_TOUCH_OFF;
    }

    private void applyMouseMode(int mode) {
        currentMouseMode = mode;

        switch (mode) {
            case 0: // Multi-touch
                prefConfig.enableMultiTouchScreen = true;
                prefConfig.touchscreenTrackpad = false;
                break;
            case 1: // Normal mouse
            case 5: // Normal mouse with swapped buttons
                prefConfig.enableMultiTouchScreen = false;
                prefConfig.touchscreenTrackpad = false;
                break;
            case 2: // Trackpad (natural)
            case 3: // Trackpad (gaming)
                prefConfig.enableMultiTouchScreen = false;
                prefConfig.touchscreenTrackpad = true;
                break;
            case 4: // Touch mouse disabled
                break;
            default:
                break;
        }

        //Initialize touch contexts
        for (int i = 0; i < touchContextMap.length; i++) {
            if (touchContextMap[i] != null) touchContextMap[i].cancelTouch();
            if (mode == 4) {
                // Touch mouse disabled
                touchContextMap[i] = null;
            } else if (!prefConfig.touchscreenTrackpad) {
                touchContextMap[i] = new AbsoluteTouchContext(conn, i, streamContainer, mode == 5);
            } else if (mode == 3) {
                touchContextMap[i] = new RelativeTouchContext(conn, i, REFERENCE_HORIZ_RES, REFERENCE_VERT_RES, streamContainer, prefConfig);
            } else {
                touchContextMap[i] = new TrackpadContext(conn, i);
            }
        }

        // Always exit zoom mode if mouse mode has changed
        isPanZoomMode = false;
    }

    public void toggleHUD() {
        prefConfig.enablePerfOverlay = !prefConfig.enablePerfOverlay;
        if (prefConfig.enablePerfOverlay) {
            performanceOverlayView.setVisibility(View.VISIBLE);
            if(prefConfig.enablePerfOverlayLite){
                performanceOverlayLite.setVisibility(View.VISIBLE);
            }else{
                performanceOverlayBig.setVisibility(View.VISIBLE);
            }
            // O vigia só existe enquanto alguém está olhando os números.
            ultimoPerfUpdate = SystemClock.uptimeMillis();
            performanceOverlayView.removeCallbacks(vigiaDosNumeros);
            performanceOverlayView.postDelayed(vigiaDosNumeros, 500);
        } else {
            performanceOverlayView.setVisibility(View.GONE);
            performanceOverlayView.removeCallbacks(vigiaDosNumeros);
        }

        // A escolha atravessa a sessão: ele liga os números uma vez e eles continuam
        // ligados na próxima. Antes o interruptor só mexia no objeto em memória e a
        // preferência voltava ao que estava em Ajustes.
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("checkbox_enable_perf_overlay", prefConfig.enablePerfOverlay)
                .apply();
    }

    //切换触控灵敏度开关
    public void switchTouchSensitivity(){
        prefConfig.enableTouchSensitivity = !prefConfig.enableTouchSensitivity;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // A CASCA POR CIMA DO VÍDEO
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * A casca, criada só quando alguém a convoca pela primeira vez.
     *
     * <p><b>Preguiçosa de propósito.</b> Criá-la no {@code onCreate} ligaria o
     * {@code ComputerManagerService} e a descoberta por mDNS no exato momento em que o
     * decodificador está subindo — trabalho de rede e de thread roubado do único instante da
     * sessão em que ele não sobra. Quem nunca sai da sessão nunca paga por ela.
     */
    private CascaDoApp casca;

    /** O container da casca no layout, ou nulo se ele não existe nesta variante. */
    private View cascaCamada;

    /**
     * O contrapeso da sessão que fica viva atrás da casca.
     *
     * <p>Esta é a metade que "a sessão é o chão" não conta sozinha: sem prazo, uma sessão
     * esquecida segura o display virtual do Vibeshine e queima bateria e banda, e quem
     * descobre isso descobre pelo host. As quatro regras dele — armar, desarmar, não
     * reiniciar, e o PiP — moram em {@link PrazoDaSessao}, com teste.
     */
    private PrazoDaSessao prazoDaCasca;

    /** O vídeo falso, quando a sessão foi aberta em modo de moque. Nulo no uso normal. */
    private MoqueDaStream moque;

    private boolean emModoDeMoque() {
        return BuildConfig.DEBUG && getIntent().getBooleanExtra(EXTRA_MOQUE, false);
    }

    private PrazoDaSessao prazoDaCasca() {
        if (prazoDaCasca == null) {
            prazoDaCasca = new PrazoDaSessao(timerHandler, PrazoDaSessao.PADRAO_MS,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Só derruba se a casca AINDA estiver à mostra. A corrida entre
                            // o desarme e o disparo é possível e este é o guarda dela.
                            if (cascaVisivel()) {
                                disconnect();
                            }
                        }
                    });
        }
        return prazoDaCasca;
    }

    public boolean cascaVisivel() {
        return cascaCamada != null && cascaCamada.getVisibility() == View.VISIBLE;
    }

    /**
     * Convoca a casca por cima do vídeo — o que antes era {@code finish()}.
     *
     * <p>É a mudança semântica central desta rodada: escolher outra coisa deixou de exigir
     * matar o que está rodando. {@code Game.onStop()} continua derrubando a conexão, mas ele
     * não é chamado aqui — a Activity não para, ela só ganha uma camada.
     */
    public void mostrarCasca() {
        if (cascaCamada == null) {
            cascaCamada = findViewById(R.id.vidroCascaCamada);
        }
        if (cascaCamada == null) {
            // Sem a camada no layout não há para onde ir: o comportamento antigo, que é
            // sair, continua sendo o desfecho correto.
            disconnect();
            return;
        }

        if (casca == null) {
            casca = new CascaDoApp(
                    this,
                    findViewById(R.id.vidroHomeCompose),
                    // A sonda de GL troca o conteúdo da janela. Aqui isso levaria a
                    // SurfaceView do vídeo junto — ver `podeSondarGl`.
                    false,
                    new Runnable() {
                        @Override
                        public void run() {
                            esconderCasca();
                        }
                    });
            casca.criar();
            casca.retomar();
        }

        // A FAIXA DO TOPO, e ela e a metade que faltava da saida segura.
        //
        // Sem ela, sair da sessao por "Escolher outra coisa" dava numa home identica a de
        // quem nunca abriu nada: o video continuava decodificando atras e o unico caminho de
        // volta era o Voltar, que nao se ve. Medido no emulador antes de existir.
        casca.dizerQueHaSessao(appName != null && !appName.isEmpty()
                ? appName
                : getString(R.string.vidro_session_generic));

        // A entrada do teclado e a gaveta são da sessão, não da casca.
        soltarEstadoDeTeclado();
        if (vidroGaveta != null) {
            vidroGaveta.fecharSeco();
        }
        setInputGrabState(false);

        // ESMAECE EM VEZ DE CORTAR. A casca sobre o vídeo é a transição mais nova do
        // app e era a única sem voz de movimento: um VISIBLE seco, batido toda vez que a
        // saída segura ou a faixa de retorno é usada. A duração e a curva são as mesmas da
        // tela de conectar — as duas são "uma camada entra sobre o vídeo", e duas durações
        // diferentes para o mesmo gesto leriam como dois apps.
        //
        // `withLayer` pelo mesmo motivo de lá: sem ele a árvore inteira da casca redesenha
        // num buffer novo a cada quadro do esmaecimento, em cima de vídeo decodificando.
        cascaCamada.animate().cancel();
        cascaCamada.setAlpha(0f);
        cascaCamada.setVisibility(View.VISIBLE);
        cascaCamada.animate()
                .alpha(1f)
                .setDuration(getResources().getInteger(R.integer.vidro_motion_exit))
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                        this, R.interpolator.vidro_exit))
                .withLayer()
                .start();

        // O `stateAlwaysHidden` do manifesto é da SESSÃO: lá, teclado que sobe sozinho come
        // a imagem. Na casca há campos de texto — o código de pareamento, entre eles — e um
        // campo que não chama o teclado é um campo quebrado.
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);

        // O prazo começa a correr agora.
        prazoDaCasca().armar();
    }

    /** Devolve o vídeo, que continuou rodando o tempo todo. */
    public void esconderCasca() {
        if (cascaCamada == null) {
            return;
        }
        prazoDaCasca().desarmar();

        // A volta é mais curta que a ida, de propósito: quem toca na faixa quer o vídeo, e
        // o vídeo já está pronto atrás — não há o que esperar. O GONE só cai no fim, e o
        // alpha volta a 1 para a próxima entrada não herdar uma casca meio apagada.
        cascaCamada.animate().cancel();
        cascaCamada.animate()
                .alpha(0f)
                .setDuration(getResources().getInteger(R.integer.vidro_motion_press))
                .withLayer()
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        cascaCamada.setVisibility(View.GONE);
                        cascaCamada.setAlpha(1f);
                    }
                })
                .start();

        // De volta à sessão, de volta ao modo dela.
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setInputGrabState(true);
    }

    public void disconnect() {
        if (prefConfig.smartClipboardSync) {
            getClipboard(-1);
        }
        finish();
    }

    public void quit() {
        Context context;
        if (isOnExternalDisplay() && ExternalDisplayControlActivity.instance != null) {
            context = ExternalDisplayControlActivity.instance;
        } else {
            context = this;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.VidroDialogPlatform);
        builder.setTitle(R.string.game_dialog_title_quit_confirm);
        builder.setMessage(R.string.game_dialog_message_quit_confirm);

        builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> {
            quitOnStop = true;
            dialog.dismiss();
            finish();
        });

        builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        // A MESMA PINTURA DE PAPEL do Dialog.displayDialog, e pelo mesmo motivo: o tema
        // aponta o acento certo, mas sob Material o botão resolve a cor contra o tema da
        // Activity e sai no lilás do sistema — era o "Sim" roxo da gravação de 26/08, que a
        // troca de tema sozinha não matou. Confirmar veste a pílula de acento (com o tint
        // limpo, senão o Material escurece por cima); recusar fica na tinta comum.
        dialog.setOnShowListener(d -> {
            android.widget.Button sim = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (sim != null) {
                sim.setBackgroundResource(R.drawable.vidro_button_accent_bg);
                sim.setBackgroundTintList(null);
                sim.setTextColor(getResources().getColor(R.color.vidro_on_accent));
                sim.setAllCaps(false);
            }
            android.widget.Button nao = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (nao != null) {
                nao.setTextColor(getResources().getColor(R.color.vidro_text));
                nao.setAllCaps(false);
            }
        });
        dialog.show();
    }

    @Override
    public void showGameMenu(GameInputDevice device) {
        if(isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleGameMenu();
        } else {
            // UMA SUPERFÍCIE DE CONTROLE, E ELA É A GAVETA.
            //
            // "Ações desta sessão" era um AlertDialog em lista, e a gravação de 26/08 mostrou
            // que ele era o único controle que o dono conseguia alcançar — a gaveta não tinha
            // afordância e ele nunca a encontrou. Com o puxador na borda ela é alcançável, e
            // manter as duas seria manter duas linguagens para o mesmo trabalho, cada uma com
            // metade das ações.
            //
            // A gaveta absorveu as duas que só o menu tinha: girar a tela e as teclas
            // especiais. `device` era usado para montar o menu de um controle específico;
            // esse caminho continua no menu legado, que é o que a tela externa usa.
            if (vidroGaveta != null && device == null) {
                // Pelo vidroSession: e ele que atualiza o que os chips e os interruptores
                // mostram antes de o painel aparecer.
                if (vidroSession != null) {
                    vidroSession.openSheet();
                } else {
                    vidroGaveta.abrir();
                }
                return;
            }

            if (gameMenuCallbacks != null) {
                gameMenuCallbacks.showMenu(device);
            }
        }
    }

    public void hideGameMenu() {
        if (gameMenuCallbacks != null) {
            gameMenuCallbacks.hideMenu();
        }
    }


    // 设置surfaceView的圆角 setSurfaceviewCorner(UiHelper.dpToPx(this,24));
    private void setSurfaceviewCorner(final float radius) {

        streamContainer.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                int leftMargin = 0;
                int topMargin = 0;
                Rect selfRect = new Rect(leftMargin, topMargin, rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
                outline.setRoundRect(selfRect, radius);
            }
        });
        streamContainer.setClipToOutline(true);
    }

    @Override
    public boolean handleCommitText(CharSequence text) {
        if (!prefConfig.enableCommitText || conn == null) {
            return false;
        }
        textInputPump.offerText(text.toString());
        return true;
    }

    @Override
    public boolean handleDeleteSurroundingText(int beforeLength, int afterLength) {
        if (!prefConfig.enableCommitText || conn == null) {
            return false;
        }
        textInputPump.offerBackspaces(beforeLength);
        return true;
    }


    /** Helper ricorsivo per trovare la prima SurfaceView nel layout corrente */
    /**
     * Congela o último quadro do vídeo para que ele possa continuar existindo depois da queda.
     *
     * <h2>O que isto substitui</h2>
     *
     * O corte seco para preto. Quando a conexão caía, a imagem sumia e uma tela nova aparecia
     * por cima do nada — a pessoa perdia de vista o que estava olhando no exato momento em
     * que precisava entender o que aconteceu com aquilo.
     *
     * <p>Aqui a imagem não some: ela para, recua um pouco, esfria, e a mensagem chega na
     * frente dela.
     *
     * <h2>O destravamento embutido, e ele é bom</h2>
     *
     * A partir daqui o quadro é uma View comum, não uma surface. Ele pode dessaturar, pode
     * recuar, e na API 31+ pode até ser BORRADO de verdade — que é exatamente o que a
     * identidade jurou ser impossível sobre o vídeo. E continua sendo: isto não é blur sobre
     * vídeo, é blur sobre uma foto. A regra não afrouxou; o efeito que ela proibia passou a
     * existir no único momento em que ele faz sentido.
     *
     * <h2>O tempo, que é o assunto todo</h2>
     *
     * Tem de acontecer no PRIMEIRO sinal de que a sessão vai parar, e não depois do teardown:
     * depois, o buffer já não está lá e o que se congela é um retângulo preto — pior que o
     * corte que isto veio substituir. Por isso a chamada fica no topo de
     * {@code connectionTerminated}, ANTES do teste de portas, que faz I/O de rede.
     *
     * <h2>Quando ele não acontece</h2>
     *
     * Abaixo da API 24 não existe {@code PixelCopy}, e em surface marcada como segura ele
     * falha por projeto. Nos dois casos o desfecho é o de hoje — a tela de queda sobre o
     * preto — e nada quebra.
     */
    private void congelarUltimoQuadro() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        final ImageView alvo = findViewById(R.id.vidroCongelado);
        if (alvo == null || alvo.getVisibility() == View.VISIBLE) {
            return;
        }

        SurfaceView surface = findFirstSurfaceViewFrom(findViewById(android.R.id.content));
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) {
            return;
        }

        final Bitmap quadro;
        try {
            quadro = Bitmap.createBitmap(surface.getWidth(), surface.getHeight(),
                    Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            // Um quadro em tela cheia pode não caber num aparelho apertado. Falhar aqui é
            // aceitável: o pior caso é o comportamento de sempre.
            return;
        }

        try {
            PixelCopy.request(surface, quadro, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int resultado) {
                    if (resultado != PixelCopy.SUCCESS) {
                        quadro.recycle();
                        return;
                    }
                    mostrarQuadroCongelado(alvo, quadro);
                }
            }, new Handler(Looper.getMainLooper()));
        } catch (IllegalArgumentException e) {
            // A surface pode já ter sido destruída entre a checagem e a chamada.
            quadro.recycle();
        }
    }

    /**
     * Põe o quadro na tela: parado, recuado e frio.
     *
     * <p>Os três juntos, e nenhum sozinho. Só dessaturar deixa a imagem viva e cinza, o que
     * lê como defeito de vídeo. Só recuar deixa a imagem viva e menor, o que lê como zoom. Os
     * três dizem "isto é uma lembrança do que estava aqui" sem uma palavra.
     */
    private void mostrarQuadroCongelado(ImageView alvo, Bitmap quadro) {
        ColorMatrix esfriar = new ColorMatrix();
        esfriar.setSaturation(0.28f);

        alvo.setColorFilter(new ColorMatrixColorFilter(esfriar));
        alvo.setImageBitmap(quadro);
        alvo.setAlpha(1f);
        alvo.setScaleX(1f);
        alvo.setScaleY(1f);
        alvo.setVisibility(View.VISIBLE);

        // O recuo é uma mola TELA: é a imagem inteira que se move, e ela é a coisa mais
        // pesada da tela. Ver Mola.Perfil.
        final Mola recuo = new Mola(Mola.Perfil.TELA, 1f, new Mola.Alvo() {
            @Override
            public void aplicar(float valor) {
                alvo.setScaleX(valor);
                alvo.setScaleY(valor);
            }
        });
        recuo.lancar(0.94f, 0f);
    }

    private SurfaceView findFirstSurfaceViewFrom(View v) {
        if (v instanceof SurfaceView) return (SurfaceView) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                SurfaceView found = findFirstSurfaceViewFrom(g.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

}