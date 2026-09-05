package com.limelight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageButton;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.limelight.profiles.ProfilesAdapter;
import com.limelight.profiles.ProfilesManager;
import com.limelight.profiles.SettingsProfile;
import com.limelight.ui.compose.CenaUi;
import com.limelight.ui.compose.PonteDeCenas;
import com.limelight.utils.UiHelper;

public class ProfilesActivity extends AppCompatActivity implements ProfilesManager.ProfileChangeListener {
    private ProfilesAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;

    private androidx.compose.ui.platform.ComposeView cenasCompose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        cenasCompose = findViewById(R.id.vidroCenasCompose);
        montarCenas();

        ProfilesManager.getInstance().addListener(this);
        UiHelper.notifyNewRootView(this);
    }

    /**
     * Liga a tela ao Compose.
     *
     * Um lugar so, chamado tambem quando a lista de cenas muda. Nao ha adapter e nao ha
     * notifyDataSetChanged: a lista e imutavel, e trocar a lista e o que redesenha.
     */
    private void montarCenas() {
        if (cenasCompose == null) {
            return;
        }

        java.util.List<SettingsProfile> perfis = ProfilesManager.getInstance().getProfiles();
        SettingsProfile ativa = ProfilesManager.getInstance().getActive();
        java.util.List<CenaUi> cenas = new java.util.ArrayList<>(perfis.size());

        java.text.DateFormat quando = android.text.format.DateFormat.getDateFormat(this);
        for (SettingsProfile perfil : perfis) {
            cenas.add(new CenaUi(
                    perfil.getUuid().toString(),
                    perfil.getName() == null ? "" : perfil.getName(),
                    quando.format(new java.util.Date(perfil.getModifiedUtc())),
                    ativa != null && ativa.getUuid().equals(perfil.getUuid())));
        }

        PonteDeCenas.montar(
                cenasCompose,
                cenas,
                () -> { finish(); return kotlin.Unit.INSTANCE; },
                this::aoAbrirCena,
                this::aoEditarCena,
                this::aoApagarCena,
                () -> {
                    startActivity(new Intent(this, EditProfileActivity.class));
                    return kotlin.Unit.INSTANCE;
                });
    }

    private kotlin.Unit aoAbrirCena(CenaUi cena) {
        ProfilesManager.getInstance().setActive(java.util.UUID.fromString(cena.getId()));
        montarCenas();
        return kotlin.Unit.INSTANCE;
    }

    private kotlin.Unit aoEditarCena(CenaUi cena) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        // A chave e literal do outro lado; um dia ela merece uma constante publica.
        intent.putExtra("profileUuid", cena.getId());
        startActivity(intent);
        return kotlin.Unit.INSTANCE;
    }

    private kotlin.Unit aoApagarCena(CenaUi cena) {
        // PERGUNTA ANTES, porque apagar não tem volta.
        //
        // Medido no emulador: "Apagar cena" apagava NO TOQUE, sem confirmação nenhuma — a
        // única ação destrutiva do app a um toque de distância. Apagar um PC pergunta,
        // encerrar a sessão pergunta; uma cena configurada à mão vale o mesmo cuidado. O
        // texto reaproveita a confirmação que o gerenciador antigo de perfis já tinha.
        android.app.AlertDialog dialogo = new android.app.AlertDialog.Builder(
                this, R.style.VidroDialogPlatform)
                .setTitle(R.string.profile_manager_delete_profile)
                .setMessage(getString(
                        R.string.profile_manager_confirm_profile_deleteion, cena.getNome()))
                .setPositiveButton(R.string.profile_manager_delete, (d, which) -> {
                    ProfilesManager.getInstance().delete(java.util.UUID.fromString(cena.getId()));
                    ProfilesManager.getInstance().save(this);
                    montarCenas();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        com.limelight.utils.UiHelper.vestirBotoesDoDialogo(dialogo, getResources());
        return kotlin.Unit.INSTANCE;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProfilesManager.getInstance().removeListener(this);
    }

    @Override
    public void onProfilesChanged() {
        runOnUiThread(this::montarCenas);
    }

}