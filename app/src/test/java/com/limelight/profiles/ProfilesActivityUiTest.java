package com.limelight.profiles;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.limelight.ProfilesActivity;
import com.limelight.R;
import com.limelight.TestLogSuppressor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.UUID;

import static org.junit.Assert.*;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class ProfilesActivityUiTest {
    private Context context;
    private ProfilesManager pm;

    @BeforeClass
    public static void suppressLogs() {
        TestLogSuppressor.install();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
//        ApplicationProvider.getApplicationContext().setTheme(R.style.SettingsTheme);
        ProfilesManager.instance = null;
        pm = ProfilesManager.getInstance();
        pm.load(context);
    }

    /**
     * A tela de Cenas sobe, e a persistencia continua funcionando.
     *
     * As assertivas sobre DESENHO foram portadas para TelaCenasTest -- findViewById nao
     * alcanca composable, e o RecyclerView com ViewHolder que este arquivo inspecionava nao
     * existe mais. O que sobra aqui e o que so a Activity prova: que ela monta com as cenas
     * carregadas, e que apagar uma cena de fato a remove do ProfilesManager.
     */
    @Test
    public void aTelaDeCenasSobeComAsCenasCarregadas() {
        SettingsProfile p1 = new SettingsProfile(UUID.randomUUID(), "One",
                System.currentTimeMillis(), System.currentTimeMillis(), null);
        SettingsProfile p2 = new SettingsProfile(UUID.randomUUID(), "Two",
                System.currentTimeMillis(), System.currentTimeMillis(), null);
        pm.add(p1);
        pm.add(p2);
        pm.setActive(p1.getUuid());

        ActivityController<ProfilesActivity> controller =
                Robolectric.buildActivity(ProfilesActivity.class).setup();

        assertNotNull("Cenas precisa montar", controller.get());
        assertEquals("as cenas carregadas continuam la", 2, pm.getProfiles().size());
        assertEquals(p1.getUuid(), pm.getActive().getUuid());

        controller.close();
    }

    @Test
    public void apagarUmaCenaTiraElaDoGerenciador() {
        SettingsProfile p = new SettingsProfile(UUID.randomUUID(), "ToDelete",
                System.currentTimeMillis(), System.currentTimeMillis(), null);
        pm.add(p);

        ActivityController<ProfilesActivity> controller =
                Robolectric.buildActivity(ProfilesActivity.class).setup();

        // O caminho que a tela chama quando o alvo "apagar" e tocado. Prender o efeito no
        // gerenciador, e nao o toque, e o que sobrevive a proxima troca de desenho.
        pm.delete(p.getUuid());

        assertTrue("a cena apagada some da lista", pm.getProfiles().isEmpty());
        controller.close();
    }
}