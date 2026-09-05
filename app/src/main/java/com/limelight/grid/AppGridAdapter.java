package com.limelight.grid;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.grid.assets.CachedAppAssetLoader;
import com.limelight.grid.assets.DiskAssetLoader;
import com.limelight.grid.assets.MemoryAssetLoader;
import com.limelight.grid.assets.NetworkAssetLoader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.VidroPosterBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class AppGridAdapter extends GenericGridAdapter<AppObject>
        implements CachedAppAssetLoader.ArtAspectListener {
    private static final int ART_WIDTH_PX = 300;
    // Estes dp não medem mais a capa — a largura dela vem da coluna da grade e a altura
    // sai dela na proporção da arte (VidroPosterBox). Eles governam só a resolução em que
    // a arte é decodificada: decodificar a 110 deixava a capa macia no modo pequeno, que é
    // justamente o modo que todo celular usa.
    private static final int SMALL_WIDTH_DP = 132;
    private static final int LARGE_WIDTH_DP = 170;

    /**
     * Recorte da capa no raio do card.
     *
     * O fundo do placeholder e a máscara de "app aberto" — esta pintada por
     * setBackgroundColor — cobrem o retângulo até os cantos. O contorno de
     * vidro_poster_frame arca com @dimen/vidro_radius_m por dentro deles, então sobrava um
     * ombro quadrado para fora do arco nos quatro cantos. Recortar a caixa inteira resolve
     * os dois de uma vez, e continua valendo para a box art que chegue na proporção da
     * caixa e a preencha toda.
     *
     * Provider único e sem estado: ele lê o raio dos recursos da própria view, então serve
     * qualquer item da grade sem alocar um objeto por bind.
     */
    private static final ViewOutlineProvider POSTER_OUTLINE = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            float radius = view.getResources().getDimension(R.dimen.vidro_radius_m);
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    };

    private final ComputerDetails computer;
    private final String uniqueId;
    private final boolean showHiddenApps;

    private CachedAppAssetLoader loader;
    private Set<Integer> hiddenAppIds = new HashSet<>();
    private ArrayList<AppObject> allApps = new ArrayList<>();

    // Proporção da box art do host, aprendida da primeira capa que carrega e válida para a
    // grade toda. Zero enquanto nenhuma carregou: aí o card fica no palpite de 4:3 da
    // VidroPosterBox. Ver onArtAspectMeasured.
    private int artAspectWidth;
    private int artAspectHeight;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AppGridAdapter(Context context, PreferenceConfiguration prefs, ComputerDetails computer, String uniqueId, boolean showHiddenApps) {
        super(context, getLayoutIdForPreferences(prefs));

        this.computer = computer;
        this.uniqueId = uniqueId;
        this.showHiddenApps = showHiddenApps;

        updateLayoutWithPreferences(context, prefs);
    }

    public void updateHiddenApps(Set<Integer> newHiddenAppIds, boolean hideImmediately) {
        this.hiddenAppIds.clear();
        this.hiddenAppIds.addAll(newHiddenAppIds);

        if (hideImmediately) {
            // Reconstruct the itemList with the new hidden app set
            itemList.clear();
            for (AppObject app : allApps) {
                app.isHidden = hiddenAppIds.contains(app.app.getAppId());

                if (!app.isHidden || showHiddenApps) {
                    itemList.add(app);
                }
            }
        }
        else {
            // Just update the isHidden state to show the correct UI indication
            for (AppObject app : allApps) {
                app.isHidden = hiddenAppIds.contains(app.app.getAppId());
            }
        }

        notifyDataSetChanged();
    }

    private static int getLayoutIdForPreferences(PreferenceConfiguration prefs) {
        if (prefs.smallIconMode) {
            return R.layout.app_grid_item;
        }
        else {
            return R.layout.app_grid_item;
        }
    }

    public void updateLayoutWithPreferences(Context context, PreferenceConfiguration prefs) {
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        int dp;

        if (prefs.smallIconMode) {
            dp = SMALL_WIDTH_DP;
        }
        else {
            dp = LARGE_WIDTH_DP;
        }

        double scalingDivisor = ART_WIDTH_PX / (dp * (dpi / 160.0));
        if (scalingDivisor < 1.0) {
            // We don't want to make them bigger before draw-time
            scalingDivisor = 1.0;
        }
        LimeLog.info("Art scaling divisor: " + scalingDivisor);

        if (loader != null) {
            // Cancel operations on the old loader
            cancelQueuedOperations();
        }

        this.loader = new CachedAppAssetLoader(computer, scalingDivisor,
                new NetworkAssetLoader(context, uniqueId),
                new MemoryAssetLoader(),
                new DiskAssetLoader(context),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.no_app_image));
        this.loader.setArtAspectListener(this);

        // This will trigger the view to reload with the new layout
        setLayoutId(getLayoutIdForPreferences(prefs));
    }

    public void cancelQueuedOperations() {
        loader.cancelForegroundLoads();
        loader.cancelBackgroundLoads();
        loader.freeCacheMemory();
    }

    private static void sortList(List<AppObject> list) {
        Collections.sort(list, new Comparator<AppObject>() {
            @Override
            public int compare(AppObject lhs, AppObject rhs) {
                int lIndex = lhs.app.getAppIndex();
                int rIndex = rhs.app.getAppIndex();
                if (lIndex == rIndex) {
                    return lhs.app.getAppName().toLowerCase().compareTo(rhs.app.getAppName().toLowerCase());
                } else {
                    return lIndex - rIndex;
                }
            }
        });
    }

    public void addApp(AppObject app) {
        // Update hidden state
        app.isHidden = hiddenAppIds.contains(app.app.getAppId());

        // Always add the app to the all apps list
        allApps.add(app);
        sortList(allApps);

        // Add the app to the adapter data if it's not hidden
        if (showHiddenApps || !app.isHidden) {
            // Queue a request to fetch this bitmap into cache
            loader.queueCacheLoad(app.app);

            // Add the app to our sorted list
            itemList.add(app);
            sortList(itemList);
        }
    }

    public void removeApp(AppObject app) {
        itemList.remove(app);
        allApps.remove(app);
    }

    @Override
    public void clear() {
        super.clear();
        allApps.clear();
    }

    /**
     * A primeira capa que carrega define a forma de TODOS os cards.
     *
     * O pedido é card na proporção da imagem, sem faixa de fundo sobrando. Altura por card
     * não entrega isso numa GridView: a fileira toma a altura do card mais alto, e a
     * diferença vira calha morta embaixo dos cards mais baixos, com os nomes desalinhados
     * de um card para o vizinho — a sobra sai de dentro da capa e reaparece fora dela. Uma
     * proporção só para a grade inteira zera as duas, porque o host publica toda a box art
     * no mesmo tamanho.
     *
     * A PRIMEIRA, e não a mais recente: a arte carrega em três executores e por cache, e
     * seguir a última faria a grade mudar de forma a cada rolagem. Num host que publique
     * capa em tamanhos diferentes, quem não bate com a primeira volta a sobrar fundo —
     * ainda assim menos gente que com os 4:3 cravados, que não batiam com ninguém.
     */
    @Override
    public void onArtAspectMeasured(int width, int height) {
        if (width <= 0 || height <= 0 || artAspectWidth != 0) {
            return;
        }

        artAspectWidth = width;
        artAspectHeight = height;

        // Os cards já montados precisam remedir. O aviso pode chegar de dentro do próprio
        // getView — o cache de memória responde na hora —, e mexer no adapter durante o
        // layout da GridView é receita de estado inconsistente; daí o post.
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * A arte cobre a caixa inteira quando tem a MESMA forma que a grade — e a forma da
     * grade é a da primeira capa que carregou, para todos os cards.
     *
     * Quem pergunta é o carregador, para decidir se pode largar o fundo listrado atrás da
     * capa. Batendo a proporção, a capa desenha por fitCenter e ocupa a caixa toda: o fundo
     * fica atrás de pixel opaco e só custa overdraw. Não batendo, sobra beirada e o fundo
     * precisa continuar lá.
     *
     * Comparação cruzada, como em VidroPosterBox.setArtAspect: 600x800 e 300x400 são a
     * mesma forma.
     *
     * Buraco conhecido: arte degenerada — mais de 2:1 em qualquer direção — é RECUSADA pela
     * caixa, que fica no palpite de 4:3. Aí toda arte bate com a proporção guardada aqui, o
     * fundo sai, e a beirada que sobra passa a mostrar o fundo da tela em vez do listrado.
     * Um host assim já entrega card com sobra por decisão da própria caixa; o que muda é a
     * cor da sobra.
     */
    @Override
    public boolean artFillsCard(int width, int height) {
        if (width <= 0 || height <= 0 || artAspectWidth <= 0 || artAspectHeight <= 0) {
            return false;
        }

        return width * artAspectHeight == height * artAspectWidth;
    }

    @Override
    public void populateView(View parentView, ImageView imgView, RelativeLayout gridMask, ProgressBar prgView, TextView txtView, ImageView overlayView, AppObject obj) {
        VidroPosterBox poster = parentView.findViewById(R.id.grid_poster);
        if (poster != null && poster.getOutlineProvider() != POSTER_OUTLINE) {
            poster.setOutlineProvider(POSTER_OUTLINE);
            poster.setClipToOutline(true);
        }

        // A caixa ignora medida inválida, então enquanto nenhuma capa carregou isto é um
        // no-op e o card fica no palpite de 4:3.
        if (poster != null) {
            poster.setArtAspect(artAspectWidth, artAspectHeight);
        }

        // Let the cached asset loader handle it
        loader.populateImageView(obj.app, imgView, txtView);

        // O disco de "play" do Moonlight caía centrado na capa, que é exatamente onde o
        // Vibeshine assa o nome do app dentro da própria box art — o glifo aterrissava em
        // cima da palavra. E era redundante: a faixa de retomada no topo da tela já diz
        // qual app está aberto, com o nome por extenso.
        overlayView.setVisibility(View.GONE);
        if (obj.isRunning) {
            gridMask.setBackgroundColor(0x66000000);
        }
        else {
            // null, e não transparente: um ColorDrawable de alpha zero continua sendo uma
            // camada de desenho por card, mantida em toda a grade só para não pintar nada.
            gridMask.setBackground(null);
        }

        // As pílulas de categoria e a de "aberto" saíram do card por decisão do dono do
        // fork. Numa grade de capas elas competiam com a arte, e quem está aberto já é dito
        // duas vezes: pela máscara escura acima e pela faixa de retomada no topo da tela.
        //
        // O discriminador de sessão sintética abaixo fica: ele foi medido contra o Vibeshine
        // de verdade e é a única coisa no app que separa Remote Monitor e Remote Input de um
        // jogo qualquer. Qualquer ideia futura sobre esses dois cards começa por ele.

        if (obj.isHidden) {
            parentView.setAlpha(0.40f);
        }
        else {
            parentView.setAlpha(1.0f);
        }
    }

    /**
     * Sessão remota: reconhecida pelo ID sintético, não pelo nome nem por UUID fixo.
     *
     * Os valores vieram do applist de um Vibeshine de verdade — Remote Monitor 2147483505
     * e Remote Input 2147483506, na faixa 2147483501–506 que o host reserva para as
     * sessões que ele próprio sintetiza. Antes disto o código comparava com "Remote
     * Display", nome que o Vibeshine não usa, e com NvApp.REMOTE_INPUT_UUID, que é o UUID
     * do Apollo e não bate com o que o Vibeshine publica: nenhuma das duas pílulas
     * aparecia, e nada no catálogo denunciava a falha — um card sem pílula é exatamente o
     * que um jogo deve parecer.
     *
     * O ID é o discriminador estável: sobrevive a renomeação e ao idioma do host.
     */
    private static final int REMOTE_SESSION_ID_FIRST = 2147483501;
    private static final int REMOTE_SESSION_ID_LAST = 2147483506;
    private static final int REMOTE_MONITOR_ID = 2147483505;
    private static final int REMOTE_INPUT_ID = 2147483506;

    private static boolean isRemoteSession(NvApp app) {
        int id = app.getAppId();
        return id >= REMOTE_SESSION_ID_FIRST && id <= REMOTE_SESSION_ID_LAST;
    }

    private static boolean isRemoteDisplay(NvApp app) {
        return isRemoteSession(app) && app.getAppId() == REMOTE_MONITOR_ID;
    }

    private static boolean isRemoteInput(NvApp app) {
        // O UUID do Apollo continua valendo para quem usa Apollo em vez de Vibeshine.
        return (isRemoteSession(app) && app.getAppId() == REMOTE_INPUT_ID)
                || NvApp.REMOTE_INPUT_UUID.equals(app.getAppUUID());
    }
}
