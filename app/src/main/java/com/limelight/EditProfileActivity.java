package com.limelight;

import com.limelight.ui.VidroToast;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.preferences.StreamSettings;
import com.limelight.profiles.ProfilesManager;
import androidx.compose.ui.platform.ComposeView;
import com.limelight.ui.compose.PonteDoEditorDeCena;
import com.limelight.profiles.SettingsProfile;
import com.limelight.utils.UiHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {
    private String profileUuid;
    private SettingsProfile currentProfile;
    private InMemorySharedPreferences inMemoryPrefs;
    private ProfilePreferenceFragment prefsFragment;
    private String pendingProfileName; // Holds new name for unsaved profiles
    private TextView titleView;

    /**
     * O editor de quatro escolhas é a porta da frente; a árvore de 111 chaves é a gaveta.
     *
     * O PE06 diz que uma cena é uma **intenção** — papel, tela, taxa e entrada — e não um
     * retrato do app num instante. O retrato continua sendo salvo (é o que `saveProfile`
     * grava, e é o que faz a gaveta continuar valendo); o que ele deixou de ser é a
     * interface. Quem chega aqui para trocar a resolução não esbarra mais em cento e sete
     * preferências no caminho até ela.
     */
    private boolean gavetaAvancada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        UiHelper.setLocale(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // A barra fica só com o voltar e o ✓; quem mostra o nome é o TextView abaixo dela,
        // na mesma coluna da lista. Ver o comentário em activity_edit_profile.xml.
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        titleView = findViewById(R.id.vidroProfileTitle);
        // O nome do perfil é o único campo desta tela e não tem moldura de campo nenhuma:
        // quem chega nela lê "Novo perfil" como título e sai sem descobrir que dá para
        // trocar. O lápis da barra continua valendo; o toque no próprio nome é o caminho
        // que a pessoa tenta primeiro.
        if (titleView != null) {
            titleView.setOnClickListener(v -> showRenameDialog());
        }

        // Get profile UUID from intent
        profileUuid = getIntent().getStringExtra("profileUuid");

        if (profileUuid != null) {
            // Editing existing profile
            List<SettingsProfile> profiles = ProfilesManager.getInstance().getProfiles();
            for (SettingsProfile profile : profiles) {
                if (profile.getUuid().toString().equals(profileUuid)) {
                    currentProfile = profile;
                    break;
                }
            }

            if (currentProfile != null) {
                applyTitle(getString(R.string.profile_manager_edit_profile_with, currentProfile.getName()));
                inMemoryPrefs = new InMemorySharedPreferences(currentProfile.getOptions());
            } else {
                VidroToast.show(this, R.string.profile_manager_profile_not_found, Toast.LENGTH_SHORT);
                finish();
                return;
            }
        } else {
            // Creating new profile
            applyTitle(getString(R.string.profile_manager_new_profile));
            inMemoryPrefs = new InMemorySharedPreferences(PreferenceManager.getDefaultSharedPreferences(this).getAll());
        }

        mostrarEditor();

        UiHelper.notifyNewRootView(this);
    }

    /**
     * As quatro escolhas, em Compose.
     *
     * A `ComposeView` entra no MESMO container do fragmento, e não no lugar da Activity
     * inteira: a barra com o ✓ e o cabeçalho com o nome continuam sendo os de sempre, porque
     * salvar e renomear não mudaram de dono. Só a lista embaixo trocou.
     */
    private void mostrarEditor() {
        gavetaAvancada = false;
        prefsFragment = null;

        // UM CABEÇALHO SÓ, e a razão está na gravação de 26/08.
        //
        // A tela tinha QUATRO elementos de navegação empilhados: a Toolbar do XML com a seta,
        // o lápis e o ✓; o TextView "Novo perfil" logo abaixo dela; o "Voltar" do Compose; e
        // o título "Nova cena". Dois cabeçalhos de duas eras, um em cima do outro, e nenhum
        // dos dois sabendo do outro.
        //
        // Quem fica é o do Compose, porque é ele que pertence à tela que está sendo
        // desenhada. O do XML volta inteiro na gaveta "Avançado", onde ele é o cabeçalho da
        // árvore de preferências e faz sentido de novo.
        mostrarCabecalhoDoXml(false);

        ComposeView vidro = new ComposeView(this);
        vidro.setId(View.generateViewId());
        PonteDoEditorDeCena.montar(
                vidro,
                inMemoryPrefs,
                nomeAtual(),
                currentProfile == null,
                () -> { finish(); return null; },
                nome -> { renomear(nome); return null; },
                () -> { saveProfile(); return null; },
                () -> { abrirGaveta(); return null; });

        ViewGroup container = findViewById(R.id.preferences_container);
        container.removeAllViews();
        container.addView(vidro);
    }

    /** A árvore inteira, atrás da linha "Avançado" no pé do editor. */
    private void abrirGaveta() {
        gavetaAvancada = true;

        // A gaveta é a árvore de preferências, e ela é do XML: o cabeçalho dele volta com
        // ela — é dali que sai o ✓ que salva e a seta que devolve as quatro escolhas.
        mostrarCabecalhoDoXml(true);

        ViewGroup container = findViewById(R.id.preferences_container);
        container.removeAllViews();

        prefsFragment = new ProfilePreferenceFragment(this, inMemoryPrefs);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.preferences_container, prefsFragment)
            .commit();
    }

    /**
     * Mostra ou esconde o cabeçalho de XML — a Toolbar e o título dela.
     *
     * Esconder a Toolbar de uma AppCompatActivity é mais que {@code setVisibility}: a
     * ActionBar continua reservando altura e continua respondendo ao {@code
     * setDisplayHomeAsUpEnabled}. Por isso ela é escondida pelas duas pontas.
     */
    private void mostrarCabecalhoDoXml(boolean mostrar) {
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
        if (titleView != null) {
            titleView.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
        if (getSupportActionBar() != null) {
            if (mostrar) {
                getSupportActionBar().show();
            } else {
                getSupportActionBar().hide();
            }
        }
        // O menu (lápis e ✓) é da Toolbar; sem ela ele não tem onde morar.
        invalidateOptionsMenu();
    }

    /**
     * O nome que o editor mostra.
     *
     * Uma cena nova chega com o nome já preenchido, e não em branco: um campo vazio pede
     * para ser preenchido antes de a pessoa saber para que serve a tela. O nome sugerido é o
     * mesmo que `saveProfile` usaria se ela não digitasse nada, então o que está escrito é o
     * que vai acontecer.
     */
    private String nomeAtual() {
        if (currentProfile != null) {
            return currentProfile.getName();
        }
        if (pendingProfileName != null && !pendingProfileName.trim().isEmpty()) {
            return pendingProfileName;
        }
        return getString(R.string.profile_manager_profile)
                + (ProfilesManager.getInstance().getProfiles().size() + 1);
    }

    /** O nome digitado no editor — a mesma porta que o diálogo de renomear usa. */
    private void renomear(String nome) {
        if (currentProfile != null) {
            currentProfile.setName(nome);
            applyTitle(getString(R.string.profile_manager_edit_profile_with, nome));
        } else {
            pendingProfileName = nome;
            applyTitle(nome);
        }
    }

    /**
     * O título continua indo para a janela — é dele que saem o rótulo de recentes e o
     * anúncio do leitor de tela — mas quem o desenha é o TextView do cabeçalho.
     */
    private void applyTitle(CharSequence title) {
        setTitle(title);
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!gavetaAvancada) {
            // Sem Toolbar não há menu: o lápis e o ✓ do XML são duplicatas do que o editor
            // em Compose já oferece — o nome se edita no próprio campo, e "Salvar" é um
            // botão da tela.
            return false;
        }
        getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // O editor usa o mesmo preferences.xml de Ajustes e por isso herdou o índice de
            // categorias: dentro de uma delas a seta sobe um nível, e só do índice ela fecha
            // o editor. Ver StreamSettings.SettingsFragment.backToIndex().
            if (prefsFragment != null && prefsFragment.backToIndex()) {
                return true;
            }
            if (gavetaAvancada) {
                // Do índice da gaveta a seta devolve as quatro escolhas, e não fecha: quem
                // entrou por "Avançado" veio de lá.
                mostrarEditor();
                return true;
            }
            finish();
            return true;
        } else if (id == R.id.action_save) {
            saveProfile();
            return true;
        } else if (id == R.id.action_rename) {
            showRenameDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void reloadSettings() {
        // Sem gaveta aberta não há fragmento para recriar. Isto chega por rotação, que
        // acontece nas duas telas.
        if (!gavetaAvancada || prefsFragment == null) {
            return;
        }

        // A categoria aberta atravessa a recriação do fragmento; sem isto o ajuste que
        // dispara o reload — resolução personalizada — devolvia a pessoa ao índice.
        String openScreen = prefsFragment.getOpenScreenKey();

        prefsFragment = new ProfilePreferenceFragment(this, prefsFragment.getPrefs());
        prefsFragment.setInitialScreenKey(openScreen);
        getSupportFragmentManager().beginTransaction().replace(
                R.id.preferences_container, prefsFragment
        ).commitAllowingStateLoss();
    }

    private void saveProfile() {
        // Get the profile options from our in-memory prefs
        Map<String, Object> profileOptions = new HashMap<>(inMemoryPrefs.getAll());

        String displayName;

        if (currentProfile != null) {
            // Update existing profile
            currentProfile.setOptions(profileOptions);
            currentProfile.setModifiedUtc(System.currentTimeMillis());
            displayName = currentProfile.getName();
            ProfilesManager.getInstance().update(currentProfile);
        } else {
            // Create new profile
            String profileName = pendingProfileName != null ? pendingProfileName.trim() : null;
            if (profileName == null || profileName.isEmpty()) {
                profileName = getString(R.string.profile_manager_profile) + (ProfilesManager.getInstance().getProfiles().size() + 1);
            }
            long now = System.currentTimeMillis();
            SettingsProfile newProfile = new SettingsProfile(
                UUID.randomUUID(),
                profileName,
                now,
                now,
                profileOptions
            );
            displayName = profileName;
            ProfilesManager.getInstance().add(newProfile);
        }

        if (ProfilesManager.getInstance().save(this)) {
            // Show confirmation Toast
            VidroToast.show(this,
                    getString(R.string.profile_manager_profile_saved, displayName),
                    Toast.LENGTH_SHORT);
        } else {
            VidroToast.show(this, R.string.profile_manager_failed_to_save, Toast.LENGTH_LONG);
        }

        finish();
    }

    private void showRenameDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        // Nome de perfil é uma linha. Sem isto o campo aceita enter e o título da tela
        // passa a ter uma quebra no meio. Antes do setText porque trocar o tipo de entrada
        // remexe na seleção.
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        String initial = currentProfile != null ? currentProfile.getName() : (pendingProfileName != null ? pendingProfileName : "");
        input.setText(initial);
        input.setSelection(initial.length());

        // setView() não põe recuo nenhum: o campo saía encostado nas duas bordas do cartão,
        // enquanto o título dele respeita a calha do diálogo. O quadro devolve a mesma calha.
        FrameLayout frame = new FrameLayout(this);
        int side = getResources().getDimensionPixelSize(R.dimen.vidro_space_5);
        int top = getResources().getDimensionPixelSize(R.dimen.vidro_space_3);
        frame.setPadding(side, top, side, 0);
        frame.addView(input);

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.VidroDialog)
                .setTitle(R.string.profile_manager_edit_profile_name)
                .setView(frame)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        // A validação do nome em branco existia e não valia nada: o AlertDialog fecha
        // depois de chamar o listener do builder, aconteça o que acontecer dentro dele.
        // O aviso aparecia, o diálogo fechava junto e o que a pessoa tinha digitado ia
        // embora — pior que não validar, porque parece que validou. Trocado depois do
        // show(), o clique é nosso e só fecha quando o nome vale.
        dialog.setOnShowListener(shown -> {
            // A mesma veste de todo diálogo do app — sem ela o "OK" saía no lilás do
            // Material. Ver UiHelper.vestirBotoesDoDialogo, que é o funil disso.
            com.limelight.utils.UiHelper.vestirBotoesDoDialogo(dialog, getResources());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        VidroToast.show(this, R.string.profile_manager_name_cannot_be_blank, Toast.LENGTH_SHORT);
                        return;
                    }

                    if (currentProfile != null) {
                        currentProfile.setName(newName);
                        currentProfile.setModifiedUtc(System.currentTimeMillis());
                        ProfilesManager.getInstance().update(currentProfile);
                        applyTitle(getString(R.string.profile_manager_edit_profile_with, newName));
                    } else {
                        pendingProfileName = newName;
                        applyTitle(getString(R.string.profile_manager_new_profile_with, newName));
                    }

                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    public SharedPreferences getInMemoryPrefs() {
        return inMemoryPrefs;
    }

    public static class ProfilePreferenceFragment extends StreamSettings.SettingsFragment {
        private static class InMemoryPreferenceDataStore extends androidx.preference.PreferenceDataStore {
            private final SharedPreferences prefs;
            InMemoryPreferenceDataStore(SharedPreferences prefs) {
                this.prefs = prefs;
            }
            @Override public void putString(String key, String value) { prefs.edit().putString(key, value).apply(); }
            @Override public void putStringSet(String key, java.util.Set<String> values) { prefs.edit().putStringSet(key, values).apply(); }
            @Override public void putInt(String key, int value) { prefs.edit().putInt(key, value).apply(); }
            @Override public void putBoolean(String key, boolean value) { prefs.edit().putBoolean(key, value).apply(); }
            @Override public void putFloat(String key, float value) { prefs.edit().putFloat(key, value).apply(); }
            @Override public void putLong(String key, long value) { prefs.edit().putLong(key, value).apply(); }

            @Override public String getString(String key, String defValue) { return prefs.getString(key, defValue); }
            @Override public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) { return prefs.getStringSet(key, defValues); }
            @Override public int getInt(String key, int defValue) {
                Object value = prefs.getAll().get(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return defValue;
            }
            @Override public boolean getBoolean(String key, boolean defValue) { return prefs.getBoolean(key, defValue); }
            @Override public float getFloat(String key, float defValue) { return prefs.getFloat(key, defValue); }
            @Override public long getLong(String key, long defValue) {
                Object value = prefs.getAll().get(key);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return defValue;
            }

            public SharedPreferences getPrefs() {
                return prefs;
            }
        }

        private static Map<String, Object> diff(Map<String, ?> target, Map<String, ?> newPrefs) {
            Map<String, Object> patch = new HashMap<>();
            for (Map.Entry<String, ?> entry : target.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (newPrefs.containsKey(k)) {
                    Object def = newPrefs.get(k);
                    if (v == null || !v.equals(def)) {
                        patch.put(k, v);
                    }
                } else {
                    // unknown key, include
                    patch.put(k, v);
                }
            }
            return patch;
        }

        @Override
        protected SharedPreferences getPrefs() {
            return ((InMemoryPreferenceDataStore)getPreferenceManager().getPreferenceDataStore()).getPrefs();
        }

        public ProfilePreferenceFragment(EditProfileActivity context, SharedPreferences prefs) {
            super(PreferenceConfiguration.readPreferences(context, prefs));
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState, true);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            EditProfileActivity act = (EditProfileActivity) requireActivity();
            SharedPreferences memPrefs = act.getInMemoryPrefs();
            // Route preference storage to in-memory prefs
            getPreferenceManager().setPreferenceDataStore(new InMemoryPreferenceDataStore(memPrefs));

            super.onCreatePreferences(savedInstanceState, rootKey);

            Preference prefScreen = getPreferenceScreen();

            // FIXME:
            // We can't separate keyboard files and special button files in profiles
            // Shitty code written by previous implementations, too much to fix
            // Hide the import options for now
            Preference _pref = findPreference("option_reset_osc_preference");
            if (_pref != null) {
                _pref.setVisible(false);
            }
            _pref = findPreference("import_keyboard_file");
            if (_pref != null) {
                _pref.setVisible(false);
            }
            _pref = findPreference("export_keyboard_file");
            if (_pref != null) {
                _pref.setVisible(false);
            }
            _pref = findPreference("import_special_button_file");
            if (_pref != null) {
                _pref.setVisible(false);
            }
            _pref = findPreference("option_help_custom_keys");
            if (_pref != null) {
                _pref.setVisible(false);
            }

            // Highlight changed preferences
            java.util.Map<String, ?> patch = diff(
                    PreferenceManager.getDefaultSharedPreferences(act).getAll(),
                    memPrefs.getAll()
            );
            highlightPreferences(prefScreen, patch.keySet());
        }

        @Override
        protected void reloadSettings() {
            ((EditProfileActivity)requireActivity()).reloadSettings();
        }

        /**
         * Marca com "*" o que este perfil muda em relação aos ajustes globais. Devolve se
         * achou alguma mudança abaixo de pref.
         *
         * A linha do índice também leva a marca: com cada categoria atrás de uma tela
         * própria, o asterisco do ajuste ficava invisível até entrar na categoria certa — e
         * ver de relance o que o perfil muda é para o que se abre esta tela.
         */
        private boolean highlightPreferences(Preference pref, java.util.Set<String> changedKeys) {
            if (pref == null) return false;

            if (pref instanceof PreferenceGroup) {
                androidx.preference.PreferenceGroup group = (androidx.preference.PreferenceGroup) pref;
                boolean changed = false;
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    changed |= highlightPreferences(group.getPreference(i), changedKeys);
                }
                if (changed && pref instanceof PreferenceScreen && pref != getPreferenceScreen()) {
                    pref.setTitle("*" + pref.getTitle());
                }
                return changed;
            }

            String key = pref.getKey();
            if (key != null && changedKeys.contains(key)) {
                pref.setTitle("*" + pref.getTitle());
                return true;
            }
            return false;
        }
    }

    /**
     * Simple in-memory SharedPreferences implementation for profile editing
     */
    private static class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values;

        public InMemorySharedPreferences(Map<String, ?> initialValues) {
            this.values = new HashMap<>(initialValues != null ? initialValues : new HashMap<>());
        }

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) {
            Object value = values.get(key);
            return value instanceof java.util.Set ? (java.util.Set<String>) value : defValues;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new InMemoryEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private class InMemoryEditor implements Editor {
            private final Map<String, Object> changes = new HashMap<>();

            @Override
            public Editor putString(String key, String value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, java.util.Set<String> values) {
                changes.put(key, values);
                return this;
            }

            @Override
            public Editor remove(String key) {
                changes.put(key, null);
                return this;
            }

            @Override
            public Editor clear() {
                values.clear();
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                for (Map.Entry<String, Object> entry : changes.entrySet()) {
                    if (entry.getValue() == null) {
                        values.remove(entry.getKey());
                    } else {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
                changes.clear();
            }
        }
    }
}