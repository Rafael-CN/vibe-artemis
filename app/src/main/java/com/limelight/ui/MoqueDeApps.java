package com.limelight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.limelight.AppView;
import com.limelight.grid.AppGridAdapter;
import com.limelight.grid.assets.CachedAppAssetLoader;
import com.limelight.grid.assets.MemoryAssetLoader;
import com.limelight.grid.assets.ScaledBitmap;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Seis apps sem host nenhum, para {@code tools/fotos.mjs} fotografar um catálogo sem
 * um Vibeshine de verdade no ar — debug-only, ver {@code AppView#MOQUE_APPS_EXTRA} e
 * {@code PcView#EXTRA_MOQUE_CASCA}.
 *
 * Extraído de {@code AppView} (F0) para a F3 (B-CONSOLE.md), que precisa da mesma
 * lista de apps de mentira na Início do tablet (seção "APPS" + "Continuar") sem
 * duplicar a lógica de gerar capa nem a de semear o {@link AppGridAdapter}. As duas
 * telas que usam este helper (AppView e PcView) continuam sendo as únicas donas do
 * caminho de dados de verdade — isto aqui é só a fixture.
 */
public final class MoqueDeApps {

    /** A primeira da lista é sempre a "em execução" que os dois extras prometem. */
    public static final String[] NOMES = {
            "Área de trabalho", "Steam", "Corrida Noturna",
            "Fortaleza", "Vale Silencioso", "Estrada Sem Fim",
    };

    private MoqueDeApps() {}

    /**
     * Um {@link ComputerDetails} on-line e emparelhado com o nome dado — o "PC atual"
     * que tanto {@code AppView} quanto a Início do tablet precisam para construir o
     * {@link AppGridAdapter} de mentira.
     */
    public static ComputerDetails computadorDeMentira(String uuid, String nome) {
        ComputerDetails computer = new ComputerDetails();
        computer.uuid = uuid;
        computer.name = nome;
        computer.state = ComputerDetails.State.ONLINE;
        computer.pairState = PairingManager.PairState.PAIRED;
        computer.activeAddress = new ComputerDetails.AddressTuple(
                "192.168.0.10", com.limelight.nvstream.http.NvHTTP.DEFAULT_HTTP_PORT);
        return computer;
    }

    /**
     * Constrói um {@link AppGridAdapter} de verdade e semeia os seis apps de mentira,
     * capa incluída, direto no cache em memória do {@link CachedAppAssetLoader}
     * (estático — {@link MemoryAssetLoader}) antes do {@code addApp}, para que
     * {@code populateImageView} nunca tente rede nenhuma. Zero duplicação da lógica
     * de carregamento: quem chama isto continua usando o {@link AppGridAdapter}
     * retornado exatamente como usaria um de verdade.
     */
    public static AppGridAdapter popularAppGridDeMentira(Context context, PreferenceConfiguration prefConfig,
                                                          ComputerDetails computer, boolean showHiddenApps) {
        AppGridAdapter appGridAdapter = new AppGridAdapter(context, prefConfig, computer, "moque", showHiddenApps);

        MemoryAssetLoader cacheDeCapas = new MemoryAssetLoader();
        for (int i = 0; i < NOMES.length; i++) {
            NvApp app = new NvApp(NOMES[i], "", i + 1, false);
            app.setAppIndex(i);

            Bitmap capa = criarCapaDeMentira(NOMES[i]);
            cacheDeCapas.populateCache(
                    new CachedAppAssetLoader.LoaderTuple(computer, app),
                    new ScaledBitmap(capa.getWidth(), capa.getHeight(), capa));

            AppView.AppObject obj = new AppView.AppObject(app);
            // A primeira da lista é a única "em execução" que os extras prometem.
            obj.isRunning = (i == 0);
            appGridAdapter.addApp(obj);
        }

        appGridAdapter.notifyDataSetChanged();
        return appGridAdapter;
    }

    /**
     * Uma capa 3:4 com o nome desenhado nela. A cor vem do hash do nome só pra cada
     * jogo ter a sua sem precisar manter uma paleta à parte; não é identidade visual
     * de nada.
     */
    public static Bitmap criarCapaDeMentira(String nome) {
        int w = 300, h = 400;
        Bitmap capa = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas tela = new Canvas(capa);

        float matiz = Math.abs(nome.hashCode()) % 360;
        tela.drawColor(Color.HSVToColor(new float[]{matiz, 0.42f, 0.30f}));

        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setColor(Color.WHITE);
        texto.setTextAlign(Paint.Align.CENTER);
        texto.setTextSize(34f);
        texto.setFakeBoldText(true);

        // Quebra ingênua no espaço mais próximo do meio: os seis nomes de teste têm no
        // máximo três palavras curtas, então não precisa de StaticLayout pra isso.
        int meio = nome.indexOf(' ', Math.max(0, nome.length() / 2 - 2));
        if (meio < 0) {
            meio = nome.indexOf(' ');
        }
        if (meio < 0) {
            tela.drawText(nome, w / 2f, h / 2f + 12, texto);
        } else {
            tela.drawText(nome.substring(0, meio), w / 2f, h / 2f - 10, texto);
            tela.drawText(nome.substring(meio + 1), w / 2f, h / 2f + 36, texto);
        }

        return capa;
    }
}
