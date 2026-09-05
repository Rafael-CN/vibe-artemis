package com.limelight.preferences;

import com.limelight.ui.VidroToast;
import static com.limelight.utils.ServerHelper.getActiveDisplay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaCodecInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import androidx.compose.ui.platform.ComposeView;
import com.limelight.DebugInfoActivity;
import com.limelight.ui.compose.PonteDeAjustes;
import com.limelight.BuildConfig;
import com.limelight.GameMenu;
import com.limelight.LimeLog;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.FileUriUtils;
import com.limelight.utils.PerformanceDataTracker;
import com.limelight.utils.UiHelper;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StreamSettings extends AppCompatActivity {
    /** As releases do upstream — a URL que vivia dentro do WebLauncherPreference. */
    private static final String RELEASES_URL =
            "https://github.com/ClassicOldSong/moonlight-android/releases";

    /**
     * Onde a gaveta abre, por Porta.
     *
     * O mapa é o espelho de quem contém quem em `preferences.xml`; uma chave ausente abre a
     * gaveta no índice dela, que é a degradação certa — nunca uma tela em branco.
     */
    private static final Map<String, String> TELA_DA_PORTA = new HashMap<>();
    static {
        TELA_DA_PORTA.put("custom_refresh_rate", "category_video_settings");
        TELA_DA_PORTA.put("option_reset_osc_preference", "category_onscreen_controls");
        TELA_DA_PORTA.put("option_reset_osc", "category_special_key_layout");
        TELA_DA_PORTA.put("import_keyboard_file", "category_special_key_layout");
        TELA_DA_PORTA.put("export_keyboard_file", "category_special_key_layout");
        TELA_DA_PORTA.put("import_special_button_file", "category_settings_misc");
    }

    private PreferenceConfiguration previousPrefs;
    private int previousDisplayPixelCount;

    private SettingsFragment prefsFragment;

    /**
     * A tela em Compose é a porta da frente; a árvore de XML virou a gaveta do fundo.
     *
     * Enquanto isto é falso a Activity mostra `TelaAjustes`, que é onde as onze categorias
     * podadas vivem. Fica verdadeiro quando alguém toca uma das seis Portas cuja máquina
     * ainda mora no `PreferenceFragmentCompat` — seletor de arquivo, campo de texto livre e
     * as duas confirmações destrutivas — e volta a ser falso no Voltar.
     *
     * É uma costura, e ela está aqui à vista de propósito. A alternativa era levantar o
     * `onActivityResult` de importação, a escrita do JSON de exportação e dois diálogos de
     * confirmação para dentro da Activity só para que seis linhas de mil e trezentas não
     * precisassem do fragmento — e ficar com metade disso pela metade é pior que uma gaveta
     * honesta. O item 10 do catálogo de redesenho é justamente terminar de fechá-la.
     */
    private boolean gavetaAvancada;

    /** A categoria que a gaveta deve abrir, quando ela é aberta por uma Porta. */
    private String telaDaGaveta;

    private ComposeView vidroAjustes;

    // HACK for Android 9
    static DisplayCutout displayCutoutP;

    void reloadSettings() {
        // Com a porta da frente à mostra não há fragmento para recriar, e o container
        // `R.id.stream_settings` sequer está inflado. Isto chega por rotação e por troca de
        // resolução personalizada, que acontecem nas duas telas.
        if (!gavetaAvancada) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getActiveDisplay(StreamSettings.this, previousPrefs).getMode();
            previousDisplayPixelCount = mode.getPhysicalWidth() * mode.getPhysicalHeight();
        }

        // Trocar a resolução personalizada recria o fragmento inteiro, e sem isto quem
        // estava dentro de "Ajustes de vídeo" era devolvido ao índice pelo próprio ajuste
        // que acabou de fazer. A categoria aberta atravessa a recriação pela chave.
        String openScreen = prefsFragment != null ? prefsFragment.getOpenScreenKey() : null;
        if (openScreen == null) {
            // Primeira montagem depois de uma Porta: a gaveta abre EM CIMA do que foi
            // tocado. Consumido de uma vez só — daqui em diante quem manda é o fragmento.
            openScreen = telaDaGaveta;
            telaDaGaveta = null;
        }

        prefsFragment = new SettingsFragment(PreferenceConfiguration.readPreferences(
                this,
                PreferenceManager.getDefaultSharedPreferences(this)
        ));
        prefsFragment.setInitialScreenKey(openScreen);
        getSupportFragmentManager().beginTransaction().replace(
                R.id.stream_settings, prefsFragment
        ).commitAllowingStateLoss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        previousPrefs = PreferenceConfiguration.readPreferences(this);

        UiHelper.setLocale(this);

        mostrarCompose();
    }

    /**
     * A tela de Ajustes em Compose, que é o que a Activity mostra por padrão.
     *
     * O cabeçalho, a luz e a saída são desenhados por `TelaAjustes` — por isso aqui não há
     * `activity_stream_settings.xml`. Aquele layout continua existindo e é o que a gaveta
     * infla, porque o fragmento do androidx precisa de um container de View para viver.
     */
    private void mostrarCompose() {
        gavetaAvancada = false;
        telaDaGaveta = null;
        prefsFragment = null;

        vidroAjustes = new ComposeView(this);
        PonteDeAjustes.montar(
                vidroAjustes,
                null,
                () -> {
                    // O contrato da ponte: `aoSair` só chega quando o Voltar aconteceu no
                    // índice. Sair daqui tem de passar pelo mesmo caminho da tecla do
                    // sistema, senão a troca de idioma deixa de ser aplicada.
                    onBackPressed();
                    return null;
                },
                chave -> {
                    abrirPorta(chave);
                    return null;
                });
        setContentView(vidroAjustes);
    }

    /**
     * As nove Portas — e são nove destinos diferentes, não nove telas.
     *
     * Três são um `Intent` e nada mais. As outras seis dependem de máquina que ainda vive no
     * fragmento, e para essas a gaveta abre na categoria que contém a linha: a pessoa cai
     * exatamente em cima do que tocou, e não num índice de novo.
     */
    private void abrirPorta(String chave) {
        switch (chave) {
            case "pref_debug_info":
                startActivity(new Intent(this, DebugInfoActivity.class));
                return;

            case "touchpad_calibration_launch":
                // Intent EXPLÍCITO e nunca <intent> em XML: `${applicationId}` não é
                // substituído em `res/xml`, e os buildTypes acrescentam .noir/.noirdebug.
                startActivity(new Intent(this, TouchpadCalibrationActivity.class));
                return;

            case "option_software_release":
                // A URL vivia dentro do XML, no WebLauncherPreference. O modelo não tem onde
                // guardá-la, então ela passou a morar aqui, que é quem a usa.
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)));
                return;

            default:
                abrirGaveta(TELA_DA_PORTA.get(chave));
        }
    }

    /** Troca a Activity para a árvore de XML, opcionalmente já dentro de uma categoria. */
    private void abrirGaveta(String telaInicial) {
        gavetaAvancada = true;
        telaDaGaveta = telaInicial;
        vidroAjustes = null;

        setContentView(R.layout.activity_stream_settings);

        // A seta faz o mesmo que a tecla Voltar; ela existe para que a saída esteja
        // desenhada. Ver o comentário em activity_stream_settings.xml.
        //
        // "O mesmo que Voltar" passou a incluir subir um nível: de dentro de uma categoria
        // a seta devolve o índice, e só do índice ela fecha Ajustes. Uma seta que saísse
        // direto enquanto a tecla do sistema volta ao índice seriam dois significados para
        // o mesmo gesto na mesma tela.
        View back = findViewById(R.id.vidroSettingsBack);
        if (back != null) {
            back.setOnClickListener(v -> {
                if (prefsFragment == null || !prefsFragment.backToIndex()) {
                    // Da gaveta a seta devolve a porta da frente, não fecha Ajustes: quem
                    // entrou por uma Porta veio de uma categoria em Compose e é para lá que
                    // ele espera voltar.
                    mostrarCompose();
                }
            });
        }

        reloadSettings();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // We have to use this hack on Android 9 because we don't have Display.getCutout()
        // which was added in Android 10.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Insets can be null when the activity is recreated on screen rotation
            // https://stackoverflow.com/questions/61241255/windowinsets-getdisplaycutout-is-null-everywhere-except-within-onattachedtowindo
            WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                displayCutoutP = insets.getDisplayCutout();
            }
        }

        reloadSettings();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getActiveDisplay(StreamSettings.this, previousPrefs).getMode();

            // If the display's physical pixel count has changed, we consider that it's a new display
            // and we should reload our settings (which include display-dependent values).
            //
            // NB: We aren't using displayId here because that stays the same (DEFAULT_DISPLAY) when
            // switching between screens on a foldable device.
            if (mode.getPhysicalWidth() * mode.getPhysicalHeight() != previousDisplayPixelCount) {
                reloadSettings();
            }
        }
    }

    @Override
    // NOTE: This will NOT be called on Android 13+ with android:enableOnBackInvokedCallback="true"
    public void onBackPressed() {
        if (gavetaAvancada) {
            // A gaveta primeiro. Dentro dela o fragmento já tratou o próprio nível; se o
            // Voltar chegou até aqui é porque ele estava no índice da árvore de XML.
            mostrarCompose();
            return;
        }

        finish();

        // Language changes are handled via configuration changes in Android 13+,
        // so manual activity relaunching is no longer required.
        PreferenceConfiguration newPrefs = PreferenceConfiguration.readPreferences(this);
        if (!newPrefs.language.equals(previousPrefs.language)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Restart the PC view to apply UI changes
                Intent intent = new Intent(this, PcView.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent, null);
            } else {
                if (newPrefs.language == PreferenceConfiguration.DEFAULT_LANGUAGE) {
                    VidroToast.show(this, "Language has been reset to default, please restart the app!", Toast.LENGTH_LONG);
                    System.exit(0);
                }
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final String TOUCHSCREEN_AS_MOUSE_PREF_STRING = "checkbox_touchscreen_as_mouse";
        /** Modo em vigor quando o toque foi desligado, para religar no mesmo lugar. */
        private static final String PREVIOUS_MOUSE_MODE_PREF_STRING = "mouse_mode_list_before_touch_off";

        private int nativeResolutionStartIndex = Integer.MAX_VALUE;
        private boolean nativeFramerateShown = false;

        private PreferenceConfiguration prevPrefConfig;

        /**
         * Índice de categorias.
         *
         * A raiz de preferences.xml é uma lista de PreferenceScreen aninhados, e a navegação
         * troca só o ADAPTADOR da lista: a árvore inteira continua montada no
         * PreferenceManager. setPreferenceScreen(), o caminho óbvio, trocaria também a raiz
         * do manager, e aí findPreferenceInHierarchy() passaria a enxergar uma categoria de
         * cada vez. Basta uma preferência declarar android:dependency de outra que mora em
         * OUTRA categoria para o androidx lançar IllegalStateException ao registrar a
         * dependência que não está na árvore, e a tela quebrar ao abrir. Já aconteceu com
         * "checkbox_remember_mouse_mode", que dependia de "checkbox_mouse_emulation" — a
         * preferência saiu, a armadilha não.
         *
         * Mantendo a árvore inteira, tudo o que initializePreferences() acha por chave
         * continua achando, e o editor de cena — que herda deste fragmento e usa o mesmo
         * preferences.xml — ganha a navegação sem uma linha própria.
         */
        private PreferenceScreen rootScreen;
        private PreferenceScreen shownScreen;
        private String initialScreenKey;
        private OnBackPressedCallback backCallback;

        // A rolagem é guardada por tela: voltar ao índice e entrar de novo na mesma
        // categoria devolve a linha onde a pessoa parou, não o topo.
        private final Map<String, Parcelable> listPosition = new HashMap<>();

        public SettingsFragment(PreferenceConfiguration prefCfg) {
            prevPrefConfig = prefCfg;
        }

        /** Chave da categoria aberta, ou null no índice. */
        public String getOpenScreenKey() {
            return shownScreen == null || shownScreen == rootScreen ? null : shownScreen.getKey();
        }

        /** Categoria a abrir assim que a lista montar. Ver StreamSettings.reloadSettings(). */
        public void setInitialScreenKey(String key) {
            initialScreenKey = key;
        }

        /** Volta do detalhe para o índice. Devolve false quando já estava no índice. */
        public boolean backToIndex() {
            if (shownScreen == null || shownScreen == rootScreen) {
                return false;
            }
            showScreen(rootScreen);
            return true;
        }

        @Override
        public void onNavigateToScreen(@NonNull PreferenceScreen preferenceScreen) {
            showScreen(preferenceScreen);
        }

        private void showScreen(PreferenceScreen screen) {
            RecyclerView list = getListView();
            if (list == null || screen == null) {
                return;
            }

            RecyclerView.LayoutManager layout = list.getLayoutManager();
            if (layout != null && shownScreen != null) {
                listPosition.put(screenId(shownScreen), layout.onSaveInstanceState());
            }

            shownScreen = screen;

            // setAdapter descarta as views e a lista renasce no topo; o estado guardado é
            // reaplicado logo depois, e onRestoreInstanceState ignora nulo — a primeira
            // visita a uma categoria começa no topo, que é o que se espera dela.
            list.setAdapter(onCreateAdapter(screen));
            if (layout != null) {
                layout.onRestoreInstanceState(listPosition.get(screenId(screen)));
            }

            applyScreenTitle(screen);
            if (backCallback != null) {
                backCallback.setEnabled(screen != rootScreen);
            }
        }

        private String screenId(PreferenceScreen screen) {
            return screen == rootScreen || screen.getKey() == null ? "" : screen.getKey();
        }

        /**
         * O cabeçalho de Ajustes passa a dizer em que categoria a pessoa está: sem isso a
         * tela de detalhe é uma lista sem nome, e a seta ao lado não diz para onde volta.
         * O editor de cena não tem este título — o dele é o nome do perfil, na barra — e aí
         * o método não acha nada e não faz nada.
         */
        private void applyScreenTitle(PreferenceScreen screen) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            View title = activity.findViewById(R.id.vidroSettingsTitle);
            if (title instanceof TextView) {
                ((TextView) title).setText(screen == rootScreen
                        ? getString(R.string.vidro_settings)
                        : screen.getTitle());
            }
        }

        protected SharedPreferences getPrefs() {
            return getPreferenceManager().getSharedPreferences();
        }

        private void setValue(String preferenceKey, String value) {
            ListPreference pref = (ListPreference) findPreference(preferenceKey);

            pref.setValue(value);
        }

        private void appendPreferenceEntry(ListPreference pref, String newEntryName, String newEntryValue) {
            CharSequence[] newEntries = Arrays.copyOf(pref.getEntries(), pref.getEntries().length + 1);
            CharSequence[] newValues = Arrays.copyOf(pref.getEntryValues(), pref.getEntryValues().length + 1);

            // Add the new option
            newEntries[newEntries.length - 1] = newEntryName;
            newValues[newValues.length - 1] = newEntryValue;

            pref.setEntries(newEntries);
            pref.setEntryValues(newValues);
        }

        private void addNativeResolutionEntry(int nativeWidth, int nativeHeight, boolean insetsRemoved, boolean portrait, boolean is_custom) {
            ListPreference pref = (ListPreference) findPreference(PreferenceConfiguration.RESOLUTION_PREF_STRING);

            String newName;

            if (insetsRemoved) {
                newName = getResources().getString(R.string.resolution_prefix_native_fullscreen);
            }
            else {
                newName = is_custom ? getResources().getString(R.string.resolution_prefix_custom) : getResources().getString(R.string.resolution_prefix_native);
            }

            if (PreferenceConfiguration.isSquarishScreen(nativeWidth, nativeHeight)) {
                if (portrait) {
                    newName += " " + getResources().getString(R.string.resolution_prefix_native_portrait);
                }
                else {
                    newName += " " + getResources().getString(R.string.resolution_prefix_native_landscape);
                }
            }

            newName += " ("+nativeWidth+"x"+nativeHeight+")";

            String newValue = nativeWidth+"x"+nativeHeight;

            // Check if the native resolution is already present
            for (CharSequence value : pref.getEntryValues()) {
                if (newValue.equals(value.toString())) {
                    // It is present in the default list, so don't add it again
                    return;
                }
            }

            if (pref.getEntryValues().length < nativeResolutionStartIndex) {
                nativeResolutionStartIndex = pref.getEntryValues().length;
            }
            appendPreferenceEntry(pref, newName, newValue);
        }

        private void addNativeResolutionEntries(int nativeWidth, int nativeHeight, boolean insetsRemoved, boolean is_custom) {
            if (PreferenceConfiguration.isSquarishScreen(nativeWidth, nativeHeight)) {
                addNativeResolutionEntry(nativeHeight, nativeWidth, insetsRemoved, true, is_custom);
            }
            addNativeResolutionEntry(nativeWidth, nativeHeight, insetsRemoved, false, is_custom);
        }

        private void addNativeFrameRateEntry(float framerate, boolean is_custom) {
            if (!is_custom) {
                framerate = Math.round(framerate);
                if (framerate == 0) {
                    return;
                }
            }

            ListPreference pref = (ListPreference) findPreference(PreferenceConfiguration.FPS_PREF_STRING);
            String fpsValue = is_custom ? Float.toString(framerate) : Integer.toString(Math.round(framerate));
            String fpsName = (is_custom ? getResources().getString(R.string.resolution_prefix_custom) : getResources().getString(R.string.resolution_prefix_native)) +
                    " (" + fpsValue + " " + getResources().getString(R.string.fps_suffix_fps) + ")";

            // Check if the native frame rate is already present
            for (CharSequence value : pref.getEntryValues()) {
                if (fpsValue.equals(value.toString())) {
                    // It is present in the default list, so don't add it again
                    nativeFramerateShown = false;
                    return;
                }
            }

            appendPreferenceEntry(pref, fpsName, fpsValue);
            nativeFramerateShown = true;
        }

        private void removeValue(String preferenceKey, String value, Runnable onMatched) {
            int matchingCount = 0;

            ListPreference pref = (ListPreference) findPreference(preferenceKey);

            // Count the number of matching entries we'll be removing
            for (CharSequence seq : pref.getEntryValues()) {
                if (seq.toString().equalsIgnoreCase(value)) {
                    matchingCount++;
                }
            }

            // Create the new arrays
            CharSequence[] entries = new CharSequence[pref.getEntries().length-matchingCount];
            CharSequence[] entryValues = new CharSequence[pref.getEntryValues().length-matchingCount];
            int outIndex = 0;
            for (int i = 0; i < pref.getEntryValues().length; i++) {
                if (pref.getEntryValues()[i].toString().equalsIgnoreCase(value)) {
                    // Skip matching values
                    continue;
                }

                entries[outIndex] = pref.getEntries()[i];
                entryValues[outIndex] = pref.getEntryValues()[i];
                outIndex++;
            }

            if (pref.getValue().equalsIgnoreCase(value)) {
                onMatched.run();
            }

            // Update the preference with the new list
            pref.setEntries(entries);
            pref.setEntryValues(entryValues);
        }

        private void resetBitrateToDefault(SharedPreferences prefs, String res, String fps) {
            if (res == null) {
                res = prefs.getString(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.DEFAULT_RESOLUTION);
            }
            if (fps == null) {
                fps = prefs.getString(PreferenceConfiguration.FPS_PREF_STRING, PreferenceConfiguration.DEFAULT_FPS);
            }

            prefs.edit()
                    .putInt(PreferenceConfiguration.BITRATE_PREF_STRING,
                            PreferenceConfiguration.getDefaultBitrate(res, fps))
                    .apply();
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            UiHelper.applyStatusBarPadding(view);

            // A borda de cima da lista coincide com a base do título, e o RecyclerView do
            // androidx vem sem máscara, sem degradê e sem elevação: a linha que passa ali
            // era cortada no meio da altura das letras, de uma vez. A borda que dissolve
            // devolve a transição sem mexer na estrutura da tela — o título continua sendo
            // o limite, mas o conteúdo some nele em vez de bater nele.
            //
            // O comprimento é vidro_space_2 e não vidro_space_6: com 32 dp a borda era mais
            // alta que uma linha de título, então o título INTEIRO do primeiro ajuste cabia
            // dentro dela e saía a 31% do brilho, com o valor dele a 12% — sete linhas de
            // parágrafo legíveis sob um nome que não dava para ler. Com 8 dp a dissolução
            // pega uma tira mais fina que qualquer linha de texto: nenhuma frase fica
            // inteira dentro da borda.
            View list = getListView();
            if (list != null) {
                list.setVerticalFadingEdgeEnabled(true);
                list.setFadingEdgeLength(getResources().getDimensionPixelSize(R.dimen.vidro_space_2));

                // E a barra de rolagem do sistema sai. Ela é um retângulo cinza neutro
                // encostado na borda direita, e aparecia bem em cima da borda que acabou
                // de ser feita para dissolver — a única peça de cinza que sobrava numa
                // tela inteira já traduzida para a identidade. A posição na lista já é
                // dita pela borda que some no topo.
                list.setVerticalScrollBarEnabled(false);
            }

            return view;
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, boolean unused) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Voltar de dentro de uma categoria devolve o índice. Vai pelo dispatcher e não
            // por onBackPressed() da Activity porque as duas telas que hospedam este
            // fragmento declaram enableOnBackInvokedCallback="true": a partir do Android 13
            // o gesto não passa mais por onBackPressed().
            backCallback = new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    backToIndex();
                }
            };
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

            // Aqui e não num post: o super acima já montou o adaptador da raiz — é dentro
            // de onViewCreated que a biblioteca chama bindPreferences() —, então a troca
            // acontece antes do primeiro desenho e o índice não pisca antes da categoria.
            if (initialScreenKey != null) {
                Preference target = findPreference(initialScreenKey);
                initialScreenKey = null;
                if (target instanceof PreferenceScreen) {
                    showScreen((PreferenceScreen) target);
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            initializePreferences();
        }

        public void initializePreferences() {
            // A calibração é a única preferência que abre outra tela do app. O Intent
            // vive aqui, e não no XML, porque só em código dá para dizer "esta classe,
            // neste pacote" sem depender do applicationId da variante — que muda com
            // os sufixos .noir e .noirdebug.
            Preference calibrar = findPreference("touchpad_calibration_launch");
            if (calibrar != null) {
                calibrar.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        startActivity(new Intent(getActivity(), TouchpadCalibrationActivity.class));
                        return true;
                    }
                });
            }

            addPreferencesFromResource(R.xml.preferences);
            PreferenceScreen screen = getPreferenceScreen();

            // A raiz é o índice e nunca sai do PreferenceManager; a navegação troca só o
            // adaptador. Ver o comentário nos campos rootScreen/shownScreen.
            rootScreen = screen;
            shownScreen = screen;

            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            PackageManager pm = activity.getPackageManager();

            // hide on-screen controls category on non touch screen devices
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
                PreferenceGroup category = findPreference("category_onscreen_controls");
                if (category != null) {
                    screen.removePreference(category);
                }
                category = findPreference("category_special_key_layout");
                if (category != null) {
                    screen.removePreference(category);
                }
            }

            // "O toque na tela move o mouse" é uma VISTA da mesma chave que a lista de modos,
            // não um estado paralelo: desligado grava o valor 4, que é como Game.applyMouseMode
            // reconhece "não monte contexto de toque". Um booleano próprio criaria dois donos do
            // mesmo estado, e a folha de sessão — que grava a chave direto ao tocar um chip —
            // desincronizaria o booleano sem ninguém perceber. Por isso a caixa é ressincronizada
            // a partir da chave toda vez que os Ajustes abrem.
            CheckBoxPreference touchAsMousePref = findPreference(TOUCHSCREEN_AS_MOUSE_PREF_STRING);
            if (touchAsMousePref != null) {
                boolean touchMovesMouse = !PreferenceConfiguration.MOUSE_MODE_TOUCH_DISABLED.equals(
                        getPrefs().getString(PreferenceConfiguration.MOUSE_MODE_PREF_STRING,
                                PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH));
                // Só grava quando de fato divergiu: setChecked persiste, e no editor de cena
                // uma gravação sem mudança marcaria a linha com "*" como se o perfil a tivesse
                // alterado.
                if (touchAsMousePref.isChecked() != touchMovesMouse) {
                    touchAsMousePref.setChecked(touchMovesMouse);
                }

                touchAsMousePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    SharedPreferences prefs = getPrefs();
                    if (Boolean.TRUE.equals(newValue)) {
                        // Volta ao modo que estava em vigor quando o toque foi desligado. Sem
                        // essa memória, religar devolveria multitoque a quem usava trackpad.
                        String previous = prefs.getString(PREVIOUS_MOUSE_MODE_PREF_STRING,
                                PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH);
                        editAndReload(PreferenceConfiguration.MOUSE_MODE_PREF_STRING, previous);
                    }
                    else {
                        prefs.edit()
                                .putString(PREVIOUS_MOUSE_MODE_PREF_STRING,
                                        prefs.getString(PreferenceConfiguration.MOUSE_MODE_PREF_STRING,
                                                PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH))
                                .apply();
                        editAndReload(PreferenceConfiguration.MOUSE_MODE_PREF_STRING,
                                PreferenceConfiguration.MOUSE_MODE_TOUCH_DISABLED);
                    }
                    return true;
                });
            }

            // Hide gamepad motion sensor option when running on OSes before Android 12.
            // Support for motion, LED, battery, and other extensions were introduced in S.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PreferenceGroup category =
                        (PreferenceGroup) findPreference("category_gamepad_settings");
                category.removePreference(findPreference("checkbox_gamepad_motion_sensors"));
            }

            // Hide gamepad motion sensor fallback option if the device has no gyro or accelerometer
            if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) &&
                    !activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
                PreferenceGroup category =
                        (PreferenceGroup) findPreference("category_gamepad_settings");
                category.removePreference(findPreference("checkbox_force_device_motion"));
                category.removePreference(findPreference("checkbox_gamepad_motion_fallback"));
            }

            // Hide USB driver options on devices without USB host support
            if (!pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
                PreferenceGroup category =
                        (PreferenceGroup) findPreference("category_gamepad_settings");
                category.removePreference(findPreference("checkbox_usb_bind_all"));
                category.removePreference(findPreference("checkbox_usb_driver"));
            }

            // Remove PiP mode on devices pre-Oreo, where the feature is not available (some low RAM devices),
            // and on Fire OS where it violates the Amazon App Store guidelines for some reason.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    !pm.hasSystemFeature("android.software.picture_in_picture") ||
                    pm.hasSystemFeature("com.amazon.software.fireos")) {
                PreferenceGroup category =
                        (PreferenceGroup) findPreference("category_ui_settings");
                category.removePreference(findPreference("checkbox_enable_pip"));
            }

            // Fire TV apps are not allowed to use WebViews or browsers, so hide the Help category
            /*if (getActivity().getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
                PreferenceGroup category =
                        (PreferenceGroup) findPreference("category_help");
                screen.removePreference(category);
            }*/
            PreferenceGroup category_gamepad_settings =
                    (PreferenceGroup) findPreference("category_gamepad_settings");
            // Remove the vibration options if the device can't vibrate
            if (!((Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
                category_gamepad_settings.removePreference(findPreference("checkbox_vibrate_fallback"));
                category_gamepad_settings.removePreference(findPreference("seekbar_vibrate_fallback_strength"));
                // The entire OSC category may have already been removed by the touchscreen check above
                PreferenceGroup category = findPreference("category_onscreen_controls");
                if (category != null) {
                    category.removePreference(findPreference("checkbox_vibrate_osc"));
                }
                category = findPreference("category_special_key_layout");
                if (category != null) {
                    category.removePreference(findPreference("checkbox_vibrate_keyboard"));
                }
                category = findPreference("category_gamepad_settings");
                if (category != null) {
                    category.removePreference(findPreference("checkbox_enable_device_rumble"));
                }
            }
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    !((Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE)).hasAmplitudeControl()) {
                // Remove the vibration strength selector of the device doesn't have amplitude control
                category_gamepad_settings.removePreference(findPreference("seekbar_vibrate_fallback_strength"));
            }

            // Check custom resolution
            String customResStr = prevPrefConfig.customResolution;
            if(customResStr != null && !customResStr.isEmpty()){
                String[] resolutionSegments = customResStr.split("x");
                if(resolutionSegments.length == 2){
                    try {
                        addNativeResolutionEntries(Integer.parseInt(resolutionSegments[0]), Integer.parseInt(resolutionSegments[1]), false, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // Check custom refresh rate
            String customRefreshRateStr = prevPrefConfig.customRefreshRate;
            if (customRefreshRateStr != null && !customRefreshRateStr.isEmpty()) {
                try {
                    float customRefreshRateValue = Float.parseFloat(customRefreshRateStr);
                    if (customRefreshRateValue > 0) {
                        addNativeFrameRateEntry(customRefreshRateValue, true);
                    }
                } catch (NumberFormatException e) {
                    getPrefs().edit().remove(PreferenceConfiguration.CUSTOM_REFRESH_RATE_PREF_STRING).apply();
                }
            }

            Display display = activity.getWindowManager().getDefaultDisplay();
            float maxSupportedFps = display.getRefreshRate();

            // Hide non-supported resolution/FPS combinations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int maxSupportedResW = 0;

                // Add a native resolution with any insets included for users that don't want content
                // behind the notch of their display
                boolean hasInsets = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DisplayCutout cutout;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Use the much nicer Display.getCutout() API on Android 10+
                        cutout = display.getCutout();
                    }
                    else {
                        // Android 9 only
                        cutout = displayCutoutP;
                    }

                    if (cutout != null) {
                        int widthInsets = cutout.getSafeInsetLeft() + cutout.getSafeInsetRight();
                        int heightInsets = cutout.getSafeInsetBottom() + cutout.getSafeInsetTop();

                        if (widthInsets != 0 || heightInsets != 0) {
                            DisplayMetrics metrics = new DisplayMetrics();
                            display.getRealMetrics(metrics);

                            int width = Math.max(metrics.widthPixels - widthInsets, metrics.heightPixels - heightInsets);
                            int height = Math.min(metrics.widthPixels - widthInsets, metrics.heightPixels - heightInsets);

                            addNativeResolutionEntries(width, height, false, false);
                            hasInsets = true;
                        }
                    }
                }

                // Always allow resolutions that are smaller or equal to the active
                // display resolution because decoders can report total non-sense to us.
                // For example, a p201 device reports:
                // AVC Decoder: OMX.amlogic.avc.decoder.awesome
                // HEVC Decoder: OMX.amlogic.hevc.decoder.awesome
                // AVC supported width range: 64 - 384
                // HEVC supported width range: 64 - 544
                for (Display.Mode candidate : display.getSupportedModes()) {
                    // Some devices report their dimensions in the portrait orientation
                    // where height > width. Normalize these to the conventional width > height
                    // arrangement before we process them.

                    int width = Math.max(candidate.getPhysicalWidth(), candidate.getPhysicalHeight());
                    int height = Math.min(candidate.getPhysicalWidth(), candidate.getPhysicalHeight());

                    // Some TVs report strange values here, so let's avoid native resolutions on a TV
                    // unless they report greater than 4K resolutions.
                    if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                            (width > 3840 || height > 2160)) {
                        addNativeResolutionEntries(width, height, hasInsets, false);
                    }

                    if ((width >= 3840 || height >= 2160) && maxSupportedResW < 3840) {
                        maxSupportedResW = 3840;
                    }
                    else if ((width >= 2560 || height >= 1440) && maxSupportedResW < 2560) {
                        maxSupportedResW = 2560;
                    }
                    else if ((width >= 1920 || height >= 1080) && maxSupportedResW < 1920) {
                        maxSupportedResW = 1920;
                    }

                    if (candidate.getRefreshRate() > maxSupportedFps) {
                        maxSupportedFps = candidate.getRefreshRate();
                    }
                }

                // This must be called to do runtime initialization before calling functions that evaluate
                // decoder lists.
                MediaCodecHelper.initialize(getContext(), GlPreferences.readPreferences(requireContext()).glRenderer);

                MediaCodecInfo avcDecoder = MediaCodecHelper.findProbableSafeDecoder("video/avc", -1);
                MediaCodecInfo hevcDecoder = MediaCodecHelper.findProbableSafeDecoder("video/hevc", -1);

                if (avcDecoder != null) {
                    Range<Integer> avcWidthRange = avcDecoder.getCapabilitiesForType("video/avc").getVideoCapabilities().getSupportedWidths();

                    LimeLog.info("AVC supported width range: "+avcWidthRange.getLower()+" - "+avcWidthRange.getUpper());

                    // If 720p is not reported as supported, ignore all results from this API
                    if (avcWidthRange.contains(1280)) {
                        if (avcWidthRange.contains(3840) && maxSupportedResW < 3840) {
                            maxSupportedResW = 3840;
                        }
                        else if (avcWidthRange.contains(1920) && maxSupportedResW < 1920) {
                            maxSupportedResW = 1920;
                        }
                        else if (maxSupportedResW < 1280) {
                            maxSupportedResW = 1280;
                        }
                    }
                }

                if (hevcDecoder != null) {
                    Range<Integer> hevcWidthRange = hevcDecoder.getCapabilitiesForType("video/hevc").getVideoCapabilities().getSupportedWidths();

                    LimeLog.info("HEVC supported width range: "+hevcWidthRange.getLower()+" - "+hevcWidthRange.getUpper());

                    // If 720p is not reported as supported, ignore all results from this API
                    if (hevcWidthRange.contains(1280)) {
                        if (hevcWidthRange.contains(3840) && maxSupportedResW < 3840) {
                            maxSupportedResW = 3840;
                        }
                        else if (hevcWidthRange.contains(1920) && maxSupportedResW < 1920) {
                            maxSupportedResW = 1920;
                        }
                        else if (maxSupportedResW < 1280) {
                            maxSupportedResW = 1280;
                        }
                    }
                }

                LimeLog.info("Maximum resolution slot: "+maxSupportedResW);

                if (maxSupportedResW != 0) {
                    if (maxSupportedResW < 3840) {
                        // 4K is unsupported
                        removeEntryFromListAndSetValue(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.RES_4K, PreferenceConfiguration.RES_1440P);
                    }
                    if (maxSupportedResW < 2560) {
                        // 1440p is unsupported
                        removeEntryFromListAndSetValue(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.RES_1440P, PreferenceConfiguration.RES_1080P);
                    }
                    if (maxSupportedResW < 1920) {
                        // 1080p is unsupported
                        removeEntryFromListAndSetValue(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.RES_1080P, PreferenceConfiguration.RES_720P);
                    }
                    // Never remove 720p
                }
            }
            else {
                // We can get the true metrics via the getRealMetrics() function (unlike the lies
                // that getWidth() and getHeight() tell to us).
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealMetrics(metrics);
                int width = Math.max(metrics.widthPixels, metrics.heightPixels);
                int height = Math.min(metrics.widthPixels, metrics.heightPixels);
                addNativeResolutionEntries(width, height, false, false);
            }

            if (!PreferenceConfiguration.readPreferences(this.getActivity()).unlockFps) {
                // We give some extra room in case the FPS is rounded down
                if (maxSupportedFps < 118) {
                    removeEntryFromListAndSetValue(PreferenceConfiguration.FPS_PREF_STRING, "120", "90");
                }
                if (maxSupportedFps < 88) {
                    // 1080p is unsupported
                    removeEntryFromListAndSetValue(PreferenceConfiguration.FPS_PREF_STRING, "90", "60");
                }
                // Never remove 30 FPS or 60 FPS
            }
            addNativeFrameRateEntry(maxSupportedFps, false);

            // Android L introduces the drop duplicate behavior of releaseOutputBuffer()
            // that the unlock FPS option relies on to not massively increase latency.
            findPreference(PreferenceConfiguration.UNLOCK_FPS_STRING).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // HACK: We need to let the preference change succeed before reinitializing to ensure
                    // it's reflected in the new layout.
                    reloadSettings();

                    // Allow the original preference change to take place
                    return true;
                }
            });

            // Remove HDR preference for devices below Nougat
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                LimeLog.info("Excluding HDR toggle based on OS");
                PreferenceGroup category =
                        (PreferenceGroup) findPreference("category_video_settings");
                category.removePreference(findPreference("checkbox_enable_hdr"));
            }
            else {
                Display.HdrCapabilities hdrCaps = display.getHdrCapabilities();
                Log.d("HDR CAP", display + "");
                // We must now ensure our display is compatible with HDR10
                boolean foundHdr10 = false;
                if (hdrCaps != null) {
                    // getHdrCapabilities() returns null on Lenovo Lenovo Mirage Solo (vega), Android 8.0
                    for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                        if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                            foundHdr10 = true;
                            break;
                        }
                    }
                }

                if (!foundHdr10) {
                    LimeLog.info("Excluding HDR toggle based on display capabilities");
                    PreferenceGroup category =
                            (PreferenceGroup) findPreference("category_video_settings");
                    category.removePreference(findPreference("checkbox_enable_hdr"));
                }
                else if (PreferenceConfiguration.isShieldAtvFirmwareWithBrokenHdr()) {
                    LimeLog.info("Disabling HDR toggle on old broken SHIELD TV firmware");
                    PreferenceGroup category =
                            (PreferenceGroup) findPreference("category_video_settings");
                    CheckBoxPreference hdrPref = (CheckBoxPreference) category.findPreference("checkbox_enable_hdr");
                    hdrPref.setEnabled(false);
                    hdrPref.setChecked(false);
                    hdrPref.setSummary("Update the firmware on your NVIDIA SHIELD Android TV to enable HDR");
                }
            }

            // Add a listener to the FPS and resolution preference
            // so the bitrate can be auto-adjusted
            findPreference(PreferenceConfiguration.RESOLUTION_PREF_STRING).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences prefs = getPrefs();
                    String valueStr = (String) newValue;

                    // Detect if this value is the native resolution option
                    CharSequence[] values = ((ListPreference)preference).getEntryValues();
                    boolean isNativeRes = true;
                    for (int i = 0; i < values.length; i++) {
                        // Look for a match prior to the start of the native resolution entries
                        if (valueStr.equals(values[i].toString()) && i < nativeResolutionStartIndex) {
                            isNativeRes = false;
                            break;
                        }
                    }

                    // If this is native resolution, show the warning dialog
                    if (isNativeRes) {
                        Dialog.displayDialog(getActivity(),
                                getResources().getString(R.string.title_native_res_dialog),
                                getResources().getString(R.string.text_native_res_dialog),
                                false);
                    }

                    // Write the new bitrate value
                    resetBitrateToDefault(prefs, valueStr, null);

                    // Allow the original preference change to take place
                    return true;
                }
            });
            findPreference(PreferenceConfiguration.FPS_PREF_STRING).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences prefs = getPrefs();
                    String valueStr = (String) newValue;

                    // If this is native frame rate, show the warning dialog
                    CharSequence[] values = ((ListPreference)preference).getEntryValues();
                    if (nativeFramerateShown && values[values.length - 1].toString().equals(newValue.toString())) {
                        Dialog.displayDialog(getActivity(),
                                getResources().getString(R.string.title_native_fps_dialog),
                                getResources().getString(R.string.text_native_res_dialog),
                                false);
                    }

                    // Write the new bitrate value
                    resetBitrateToDefault(prefs, null, valueStr);

                    // Allow the original preference change to take place
                    return true;
                }
            });

            findPreference("checkbox_enable_perf_logging").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Boolean loggingEnabled = (Boolean) newValue;

                    if(!loggingEnabled) {
                        new PerformanceDataTracker().clearLogs(preference.getContext());
                    }

                    // Allow the original preference change to take place
                    return true;
                }
            });

            Preference _pref;
            _pref = findPreference("import_keyboard_file");
            if (_pref != null) {
                _pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/json");
                        startActivityForResult(intent, READ_REQUEST_CODE);
                        return false;
                    }
                });
            }

            _pref = findPreference("import_special_button_file");
            if (_pref != null) {
                _pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/json");
                        startActivityForResult(intent, READ_REQUEST_SPECIAL_CODE);
                        return false;
                    }
                });
            }

            _pref = findPreference("share_performance_logs");
            if (_pref != null) {
                _pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Context context = preference.getContext();
                        PerformanceDataTracker tracker = new PerformanceDataTracker();
                        String logs = tracker.getLog(context);

                        if (logs == null || logs.trim().isEmpty()) {
                            VidroToast.show(context, context.getString(R.string.toast_no_logs), Toast.LENGTH_SHORT);
                            return false;
                        }

                        String prefixMessage = context.getString(R.string.email_prefix_message);
                        String emailRecipient = context.getString(R.string.email_recipient);
                        String emailSubject = context.getString(R.string.email_subject);
                        String chooserTitle = context.getString(R.string.email_chooser_title);
                        String noEmailClientsMsg = context.getString(R.string.toast_no_email_clients);

                        try {
                            File cacheDir = context.getCacheDir();
                            File logFile = new File(cacheDir, "artemistics_logs.txt");
                            try (FileOutputStream fos = new FileOutputStream(logFile)) {
                                fos.write(logs.getBytes(StandardCharsets.UTF_8));
                            }

                            Uri logFileUri = FileProvider.getUriForFile(context,
                                    context.getPackageName() + ".fileprovider",
                                    logFile);

                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("text/plain");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailRecipient});
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
                            emailIntent.putExtra(Intent.EXTRA_TEXT, prefixMessage);
                            emailIntent.putExtra(Intent.EXTRA_STREAM, logFileUri);

                            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            context.startActivity(Intent.createChooser(emailIntent, chooserTitle));
                        } catch (IOException e) {
                            Log.d("PerformanceDataTracker", "Error creating log file");
                        } catch (android.content.ActivityNotFoundException ex) {
                            VidroToast.show(context, noEmailClientsMsg, Toast.LENGTH_SHORT);
                        }
                        return false;
                    }
                });
            }

            _pref = findPreference("export_keyboard_file");
            if (_pref != null) {
                _pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        File file = new File(requireActivity().getExternalCacheDir(),"export_settings");
                        if(!file.exists()){
                            file.mkdir();
                        }
                        File file1= getJsonContent(requireActivity(),file);
                        if(file1==null){
                            VidroToast.show(requireActivity(),getString(R.string.pref_error_occurred),Toast.LENGTH_SHORT);
                            return false;
                        }
                        Uri uri;
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        String authority= BuildConfig.APPLICATION_ID+".fileprovider";
                        uri = FileProvider.getUriForFile(requireActivity(),authority,file1);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setType("application/json");
                        startActivity(Intent.createChooser(intent,getString(R.string.pref_save_keyboard_profile)));
                        return false;
                    }
                });
            }

            _pref = findPreference("pref_debug_info");
            if (_pref != null) {
                _pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        Intent intent=new Intent(requireActivity(), DebugInfoActivity.class);
                        requireActivity().startActivity(intent);
                        return false;
                    }
                });
            }

            // Os dois campos de texto que gravavam taxa de bits e resolução saíram da tela: um
            // escrevia na MESMA chave do slider sete linhas acima e ainda dava um toast de
            // "sucesso" que o slider não dá; o outro pedia por extenso uma resolução que
            // addNativeResolutionEntries já injeta na lista. O que sobrou deles é a leitura de
            // customResolution lá em cima, que transforma um valor já gravado numa entrada
            // extra da lista — ninguém perde a resolução que escolheu, só o campo de digitar.

            EditTextPreference customRefreshRatePref = findPreference(PreferenceConfiguration.CUSTOM_REFRESH_RATE_PREF_STRING);
            if (customRefreshRatePref != null) {
                customRefreshRatePref.setOnBindEditTextListener((EditText editText) -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
                });

                customRefreshRatePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String value = (String) newValue;
                    if (TextUtils.isEmpty(value)) {
                        VidroToast.show(getActivity(), getString(R.string.pref_enter_value_0_9999), Toast.LENGTH_SHORT);
                        return false;
                    }
                    
                    try {
                        float refreshRate = Float.parseFloat(value);
                        if (refreshRate <= 0) {
                            VidroToast.show(getActivity(), getString(R.string.pref_enter_value_0_9999), Toast.LENGTH_SHORT);
                            return false;
                        }
                        
                        // Format to max 3 decimal places
                        String formattedValue = String.format("%.3f", refreshRate);
                        // Remove trailing zeros
                        formattedValue = formattedValue.replaceAll("0+$", "").replaceAll("\\.$", "");

                        editAndReload(PreferenceConfiguration.CUSTOM_REFRESH_RATE_PREF_STRING, formattedValue);

                        return true;
                    } catch (NumberFormatException e) {
                        VidroToast.show(getActivity(), getString(R.string.pref_error_occurred), Toast.LENGTH_SHORT);
                        return false;
                    }
                });
            }

            // A identidade não tem caixa de seleção, então quem desenhava era o
            // androidx.preference: indicador do Material, com o azul do sistema e a margem
            // transparente que desalinhava o quadrado do título. Trocar o widget aqui pega
            // as 78 linhas de uma vez — e o editor de cena junto, que herda deste fragmento.
            // Pelo tema seria preciso estender Preference.CheckBoxPreference.Material, que
            // a AAR declara privado; e o próprio tema é descartado em API 29+ enquanto
            // values-v29/styles.xml redefinir SettingsTheme.
            applyVidroWidgets(screen);
        }

        private static void applyVidroWidgets(PreferenceGroup group) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference pref = group.getPreference(i);
                if (pref instanceof PreferenceGroup) {
                    if (pref instanceof PreferenceScreen) {
                        // Linha do índice. O androidx não desenha nada à direita de um
                        // PreferenceScreen aninhado, e sem marca nenhuma ela lê como um
                        // ajuste que não reage ao toque, no meio de uma lista em que toda
                        // linha ou abre diálogo ou marca caixa.
                        pref.setWidgetLayoutResource(R.layout.vidro_pref_forward);
                    }
                    applyVidroWidgets((PreferenceGroup) pref);
                }
                else if (pref instanceof CheckBoxPreference) {
                    pref.setWidgetLayoutResource(R.layout.vidro_pref_checkbox);
                }
                else if (pref instanceof ListPreference) {
                    // A escolha atual sobe do resumo para a coluna da direita, onde a taxa
                    // de bits já mostra a dela. Eram duas convenções na mesma lista — em
                    // metade das linhas o valor estava enterrado nas primeiras palavras do
                    // parágrafo, com a cor e o tamanho da explicação — e não dá para
                    // aprender onde procurar o estado de um ajuste se ele muda de lugar de
                    // linha para linha. Quem escreve o texto é o adaptador, em
                    // onBindViewHolder: a view é reciclada e o valor muda sem recarregar a
                    // tela.
                    pref.setWidgetLayoutResource(R.layout.vidro_pref_choice);
                }
                else if (pref instanceof EditTextPreference) {
                    // Mesma coluna, pelo mesmo motivo: "Resolução personalizada" e "Taxa de
                    // bits personalizada" não mostravam valor NENHUM — nem à direita nem no
                    // resumo, que só repete as opções. Não havia como saber em quanto o
                    // ajuste estava sem abrir o diálogo dele.
                    pref.setWidgetLayoutResource(R.layout.vidro_pref_choice);
                }
            }
        }

        @Override
        protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen screen) {
            return new PreferenceGroupAdapter(screen) {
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    bindChoiceValue(holder, getItem(position));
                }
            };
        }

        /**
         * Escreve o valor atual na coluna de valor da linha.
         *
         * getEntry() volta nulo quando o valor persistido não está mais entre as opções —
         * acontece de verdade aqui, porque a tela remove resoluções e taxas que o aparelho
         * não suporta —, e getText() volta nulo enquanto o campo nunca foi preenchido.
         * Nos dois casos a coluna fica vazia em vez de mostrar a chave crua ou "null".
         */
        private static void bindChoiceValue(PreferenceViewHolder holder, Preference pref) {
            View slot = holder.findViewById(R.id.vidro_pref_choice);
            if (!(slot instanceof TextView)) {
                return;
            }

            CharSequence value = null;
            if (pref instanceof ListPreference) {
                value = ((ListPreference) pref).getEntry();
            }
            else if (pref instanceof EditTextPreference) {
                value = ((EditTextPreference) pref).getText();
            }

            ((TextView) slot).setText(value != null ? value : "");
        }

        private void removeEntryFromListAndSetValue(String resolutionPrefString, String entryToRemove, String nextDefault) {
            removeValue(resolutionPrefString, entryToRemove, new Runnable() {
                @Override
                public void run() {
                    SharedPreferences prefs = getPrefs();
                    setValue(resolutionPrefString, nextDefault);
                    resetBitrateToDefault(prefs, null, null);
                }
            });
        }

        private void editAndReload(String prefKey, String newVal) {
            SharedPreferences prefs = getPrefs();
            prefs.edit().putString(prefKey, newVal).apply();

            reloadSettings();
        }

        protected void reloadSettings() {
            // HACK: We need to let the preference change succeed before reinitializing to ensure
            // it's reflected in the new layout.
            final Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Ensure the activity is still open when this timeout expires
                    StreamSettings settingsActivity = (StreamSettings) SettingsFragment.this.getActivity();
                    if (settingsActivity != null) {
                        settingsActivity.reloadSettings();
                    }
                }
            }, 500);
        }

        int READ_REQUEST_CODE = 1001;
        int READ_REQUEST_SPECIAL_CODE = 1002;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
                try {
                    Uri uri = data.getData();
                    String json = FileUriUtils.openUriForRead(getActivity(), uri);
                    if (TextUtils.isEmpty(json)) {
                        VidroToast.show(getActivity(), getString(R.string.pref_empty_file), Toast.LENGTH_SHORT);
                        return;
                    }
                    String name = getPrefs().getString(KeyBoardControllerConfigurationLoader.OSC_PREFERENCE, KeyBoardControllerConfigurationLoader.OSC_PREFERENCE_VALUE);
                    SharedPreferences.Editor prefEditor = requireActivity().getSharedPreferences(name, Activity.MODE_PRIVATE).edit();
                    JSONObject object = new JSONObject(json);
                    Iterator it = object.keys();
                    prefEditor.clear();
                    while (it.hasNext()) {
                        String key = (String) it.next();// 获得key
                        String value = object.getString(key);// 获得value
                        prefEditor.putString(key, value);
                    }
                    prefEditor.apply();
                    VidroToast.show(getActivity(), getString(R.string.pref_import_success), Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    e.printStackTrace();
                    VidroToast.show(getActivity(), getString(R.string.pref_error_occurred) + e.getMessage(), Toast.LENGTH_SHORT);
                }
                return;
            }

            if (requestCode == READ_REQUEST_SPECIAL_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
                try {
                    Uri uri = data.getData();
                    String json = FileUriUtils.openUriForRead(getActivity(), uri);
                    if (TextUtils.isEmpty(json)) {
                        VidroToast.show(getActivity(), getString(R.string.pref_empty_file), Toast.LENGTH_SHORT);
                        return;
                    }
                    SharedPreferences.Editor prefEditor = getActivity().getSharedPreferences(GameMenu.PREF_NAME, Activity.MODE_PRIVATE).edit();
                    prefEditor.putString(GameMenu.KEY_NAME, json);
                    prefEditor.apply();
                    VidroToast.show(getActivity(), getString(R.string.pref_import_success), Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    e.printStackTrace();
                    VidroToast.show(getActivity(), getString(R.string.pref_error_occurred) + e.getMessage(), Toast.LENGTH_SHORT);
                }
            }
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ConfirmDeleteOscPreference) {
                DialogFragment dialogFragment = ConfirmDeleteOscPreference.DialogFragmentCompat.newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), null);
            } else if (preference instanceof ConfirmDeleteKeyboardPreference) {
                DialogFragment dialogFragment = ConfirmDeleteKeyboardPreference.DialogFragmentCompat.newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), null);
            } else super.onDisplayPreferenceDialog(preference);
        }

        private File getJsonContent(Context context,File file){
            String name = getPrefs().getString(KeyBoardControllerConfigurationLoader.OSC_PREFERENCE, KeyBoardControllerConfigurationLoader.OSC_PREFERENCE_VALUE);
            SharedPreferences pref = context.getSharedPreferences(name, Activity.MODE_PRIVATE);
            Map<String,?> map = pref.getAll();
            File file1= new File(file,name+".json");
            String jsonStr=new Gson().toJson(map);
            if(!FileUriUtils.writerFileString(file1,jsonStr)){
                return null;
            }
            return file1;
        }

        //获取所有设置项配置文件
        private File getAllJsonData(File file){
            SharedPreferences pref = getPrefs();
            Map<String,?> map = pref.getAll();
            //获取适配电脑的数据库信息
//            List<ComputerDetails> map= new ComputerDatabaseManager(context).getAllComputers();
            File file1= new File(file,"allJSON.json");
            String jsonStr=new Gson().toJson(map);
            if(!FileUriUtils.writerFileString(file1,jsonStr)){
                return null;
            }
            return file1;
        }
    }
}

