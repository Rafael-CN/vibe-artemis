package com.limelight.profiles;

import android.content.Intent;
import android.view.View;
import android.content.SharedPreferences;
import android.os.Build;
import android.content.Context;
import android.app.GameManager;

import com.limelight.PcView;
import com.limelight.ProfilesActivity;
import com.limelight.R;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.junit.BeforeClass;
import com.limelight.TestLogSuppressor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class ProfilesNavigationTest {

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    private void prepareEnvironment() {
        // Ensure GL renderer prefs exist so PcView skips GLSurface initialization
        SharedPreferences glPrefs = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("GlPreferences", 0);
        glPrefs.edit()
                .putString("Renderer", "TestRenderer")
                .putString("Fingerprint", Build.FINGERPRINT)
                .commit();

        // Provide fake GameManager service to avoid ServiceNotFoundException
        org.robolectric.shadows.ShadowApplication shadowApp = org.robolectric.Shadows.shadowOf((android.app.Application) ApplicationProvider.getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            shadowApp.setSystemService(Context.GAME_SERVICE, mock(GameManager.class));
        } else {
            // On older APIs, the service doesn't exist, but our code might still try to access it.
            // The cast will fail if it's just `new Object()`, but it shouldn't be called anyway.
            shadowApp.setSystemService(Context.GAME_SERVICE, null);
        }

        // Provide stub ComputerManagerService binder for bindService()
        Context ctx = ApplicationProvider.getApplicationContext();
        android.content.ComponentName cn = new android.content.ComponentName(ctx, com.limelight.computers.ComputerManagerService.class);

        // We only need a mock binder, not the real service
        com.limelight.computers.ComputerManagerService.ComputerManagerBinder binder = mock(com.limelight.computers.ComputerManagerService.ComputerManagerBinder.class);
        org.robolectric.Shadows.shadowOf((android.app.Application) ctx)
                .setComponentNameAndServiceForBindService(cn, binder);

        // Apply an AppCompat theme required by ProfilesActivity
        // ApplicationProvider.getApplicationContext().setTheme(R.style.SettingsTheme);
    }

    @Test
    public void clickingProfileButton_launchesProfilesActivity() {
        prepareEnvironment();
        ActivityController<PcView> controller = Robolectric.buildActivity(PcView.class).setup();
        PcView pcView = controller.get();

        // View, e não ImageButton. Na identidade Vidro o acesso a Cenas é o botão de
        // texto do rodapé, e foi exatamente um findViewById tipado como ImageButton que
        // derrubou a PcView com ClassCastException depois do redesenho. O contrato que
        // importa aqui é "tem um alvo clicável que leva a Cenas", não a classe dele.
        // O alvo de Cenas virou o rodape em Compose, e o clique nele e guardado em
        // TelaHomeTest. Aqui fica o que so a Activity prova: que ela monta com o rodape
        // ligado -- foi um findViewById tipado como ImageButton que a derrubou com
        // ClassCastException depois de um redesenho.
        assertNotNull("a home precisa montar", pcView);
    }

    /**
     * O catálogo deixou de ser uma Activity, e este teste mudou de endereço com ele.
     *
     * Ele guardava que a `AppView` subia sem estourar recebendo um UUID que não existe — o
     * caso do atalho da tela inicial apontando para uma máquina que sumiu. Agora o catálogo é
     * uma rota da Casca, e quem recebe esse Intent é a `PcView`: o mesmo caso, a mesma
     * garantia, um destino diferente.
     *
     * Sem assertiva sobre `isFinishing`: no ambiente de teste não há PC nenhum e a rota nem
     * chega a ser empilhada. O que se guarda é que o Intent com um lugar inexistente não
     * derruba a casca — e se o layout quebrasse, o `setup()` já teria lançado.
     */
    @Test
    public void casca_abreComAtalhoParaLugarInexistente_semEstourar() {
        prepareEnvironment();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PcView.class);
        intent.putExtra("UUID", "test-uuid");
        intent.putExtra("Name", "Test PC");

        ActivityController<PcView> controller =
                Robolectric.buildActivity(PcView.class, intent).setup();

        assertNotNull("a casca precisa montar com o atalho", controller.get());
    }

    @Test
    public void profilesActivity_startsWithoutCrash() {
        prepareEnvironment();
        ProfilesActivity activity = Robolectric.buildActivity(ProfilesActivity.class).setup().get();
        assertNotNull(activity);
    }
}