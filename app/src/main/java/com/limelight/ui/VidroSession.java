package com.limelight.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.File;
import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * As superfícies da sessão que não são a ilha: a tela de conectar e a folha de ajustes.
 *
 * As duas vivem por cima do vídeo e nenhuma delas desmonta o stream — a folha é uma View,
 * não uma Activity, justamente para o aparelho não voltar para retrato e a sessão não
 * cair no caminho.
 */
public class VidroSession {

    /**
     * O que a folha precisa pedir à sessão.
     *
     * Interface, e não a Activity inteira: a folha mexe em sete coisas e não tem por que
     * enxergar as outras cento e quarenta de Game. Ela existe porque metade do que a folha
     * agora comanda NÃO mora numa preferência — o teclado na tela, o controle e o cursor
     * local são views vivas, e só quem as criou sabe dizer se estão de pé. Foi por não ter
     * como perguntar isso que essas coisas viviam numa lista de "Toggle X", que sabe
     * mandar alternar mas não sabe dizer em que estado está.
     */
    public interface Controls {
        boolean canChangeMouseMode();
        int getMouseMode();
        void setMouseMode(int mode);
        boolean isLocalCursorVisible();
        void toggleMouseLocalCursor();
        boolean isZoomModeEnabled();
        void toggleZoomMode();
        boolean isOnScreenControllerVisible();
        void toggleVirtualController();
        Boolean isKeyboardLayoutVisible();
        void toggleFullKeyboard();
        boolean isSpecialKeysVisible();
        void toggleKeyboardController();
        void toggleHUD();
    }

    /**
     * Os índices de R.array.mouse_mode_values, que é o que applyMouseMode() entende.
     *
     * A folha oferece três dos seis. Os que faltam são variações — trackpad de jogo,
     * botões trocados — e o "desativado", que é escolha de quem sempre joga com mouse
     * físico e por isso mora em Ajustes, não no meio de uma sessão.
     */
    private static final int MOUSE_MODE_MULTITOUCH = 0;
    private static final int MOUSE_MODE_ABSOLUTE = 1;
    private static final int MOUSE_MODE_ABSOLUTE_SWAPPED = 5;
    private static final int MOUSE_MODE_TRACKPAD = 2;
    private static final int MOUSE_MODE_TRACKPAD_GAMING = 3;

    /**
     * Os três passos da conexão, com o quanto da barra cada um preenche.
     *
     * Os percentuais são do protótipo; os TEMPOS não são. Lá eles rodavam em 800/1600/
     * 2500 ms porque o protótipo precisava andar sozinho. Aqui cada passo espera o evento
     * real da máquina de estados — uma barra que promete e não cumpre é pior que barra
     * nenhuma.
     */
    private static final int PROGRESS_TALKING = 18;
    private static final int PROGRESS_RESERVING = 62;
    private static final int PROGRESS_VIDEO = 100;

    private final Activity activity;
    private final PreferenceConfiguration prefConfig;
    private final Controls controls;

    private final View connecting;
    private final TextView connectingApp;
    private final TextView connectingStep;
    private final android.widget.ProgressBar connectingBar;
    private final VidroLightView connectingLight;

    private GavetaDaSessao gaveta;
    private final View sheet;

    private int stagesSeen;

    /**
     * Conta quantas vezes a tela de conectar trocou de estado.
     *
     * Mesma trava da folha, pelo mesmo motivo: cancelar um ViewPropertyAnimator dispara o
     * mesmo onAnimationEnd que terminar, então a ação de saída rodaria mesmo quando é uma
     * falha que interrompe a saída — e apagaria a tela que o cartão de erro acabou de
     * pedir de volta.
     */
    private int connectingGeneration;

    /** A barra completando o passo 3. Guardada para poder ser cortada numa falha. */
    private ObjectAnimator connectingFill;

    public VidroSession(Activity activity, PreferenceConfiguration prefConfig, Controls controls) {
        this.activity = activity;
        this.prefConfig = prefConfig;
        this.controls = controls;

        connecting = achar(R.id.vidroConnecting);
        connectingApp = achar(R.id.vidroConnectingApp);
        connectingStep = achar(R.id.vidroConnectingStep);
        connectingBar = achar(R.id.vidroConnectingBar);
        connectingLight = achar(R.id.vidroConnectingLight);

        sheet = activity.findViewById(R.id.vidroSheet);
        // Capturada AQUI, antes de a folha ganhar janela propria (Game.mostrarJanelaDaFolha
        // a remove da hierarquia da Activity). Depois disso, activity.findViewById devolve
        // null para tudo que mora nela -- e o achar() abaixo existe por causa disso.

        wireSheet();
    }

    // ==================== perdemos o PC ====================

    /**
     * A tela 09 do protótipo, que existia e nunca aparecia.
     *
     * `vidro_lost.xml` está incluído em `activity_game.xml` desde sempre — título, apoio,
     * "Tentar de novo" cheio, "Sair" fantasma e um contador — e nasce `gone` para todo
     * sempre: nenhuma linha de Java procurava os ids. Quando a rede caía no meio da partida
     * o que subia era um `AlertDialog` do sistema, com código hexadecimal e um botão que
     * jogava a pessoa na wiki de outro projeto.
     *
     * Esta é a queda DEPOIS que o vídeo já estava rodando. A folha de falha do M99 continua
     * cuidando do que falha ANTES do lado nativo subir, que é o escopo que ela declarou.
     *
     * Sem contador nesta fatia: ele diz "3ª de 5" e só faz sentido quando houver política de
     * retentativa. Prometer a contagem sem ela seria mentir de novo, na mesma tela que
     * existe para parar de mentir.
     */
    public void showLost(String hostName, String motivo, final Runnable aoTentar, final Runnable aoSair) {
        final View lost = achar(R.id.vidroLost);
        if (lost == null) {
            return;
        }

        TextView titulo = achar(R.id.vidroLostTitle);
        if (titulo != null && hostName != null && !hostName.isEmpty()) {
            titulo.setText(activity.getString(R.string.vidro_lost_host, hostName));
        }

        // O contador é o lugar do motivo real enquanto não há retentativa: ele já está
        // desenhado como linha de apoio pequena, que é exatamente o peso que um motivo
        // técnico merece nesta tela.
        TextView apoio = achar(R.id.vidroLostRetryCount);
        if (apoio != null) {
            if (motivo != null && !motivo.isEmpty()) {
                apoio.setText(motivo);
                apoio.setVisibility(View.VISIBLE);
            }
            else {
                apoio.setVisibility(View.GONE);
            }
        }

        View tentar = achar(R.id.vidroLostRetry);
        if (tentar != null) {
            tentar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (aoTentar != null) {
                        aoTentar.run();
                    }
                }
            });
        }

        View sair = achar(R.id.vidroLostLeave);
        if (sair != null) {
            sair.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (aoSair != null) {
                        aoSair.run();
                    }
                }
            });
        }

        lost.setAlpha(0f);
        lost.setVisibility(View.VISIBLE);
        lost.animate().alpha(1f)
                .setDuration(activity.getResources().getInteger(R.integer.vidro_motion_enter))
                .start();
    }

    public boolean isLostShowing() {
        View lost = achar(R.id.vidroLost);
        return lost != null && lost.getVisibility() == View.VISIBLE;
    }

    // ==================== conectando ====================

    /**
     * A capa do aplicativo que foi tocado, vinda do mesmo cache que a grade usou.
     *
     * <p><b>Do disco e nunca da rede.</b> Este é o instante mais caro da sessão — o
     * decodificador está subindo — e uma leitura de rede aqui atrasaria o primeiro quadro
     * para ganhar uma imagem que já existe a três diretórios de distância. Se o arquivo não
     * estiver lá, não há capa: a tela cai no fundo liso de sempre e ninguém perde nada.
     *
     * <p>O caminho é o mesmo que {@code DiskAssetLoader} escreve, e ele está repetido aqui de
     * propósito em vez de instanciar o loader inteiro: aquilo carrega uma cadeia de cache em
     * memória, cancelamento por posição de célula e um executor — máquina para uma grade que
     * rola, não para uma imagem só que já se sabe qual é.
     *
     * @param appId o mesmo de {@code Game.EXTRA_APP_ID}
     * @param uuidDoHost o mesmo de {@code Game.EXTRA_PC_UUID}
     */
    public static void carregarCapa(Activity activity, int appId, String uuidDoHost) {
        // Da Activity mesmo: a capa da espera mora no layout dela, não na folha.
        View capa = activity.findViewById(R.id.vidroConnectingCapa);
        View veu = activity.findViewById(R.id.vidroConnectingVeu);
        if (capa == null || veu == null) {
            return;
        }

        Bitmap arte = null;
        if (uuidDoHost != null && appId > 0) {
            File arquivo = new File(new File(new File(activity.getCacheDir(), "boxart"),
                    uuidDoHost), appId + ".png");
            if (arquivo.exists() && arquivo.length() <= CAPA_MAXIMA_BYTES) {
                // Sem `inJustDecodeBounds` e sem reamostrar: a arte padrão é 300x400, que
                // cabe em qualquer tela deste app sem ampliar nada e custa ~480 KB.
                arte = BitmapFactory.decodeFile(arquivo.getAbsolutePath());
            }
        }

        if (arte == null) {
            capa.setVisibility(View.GONE);
            veu.setVisibility(View.GONE);
            return;
        }

        ((ImageView) capa).setImageBitmap(arte);
        capa.setVisibility(View.VISIBLE);
        veu.setVisibility(View.VISIBLE);
    }

    /** O mesmo teto do DiskAssetLoader: acima disso a arte é lixo, não capa. */
    private static final long CAPA_MAXIMA_BYTES = 5 * 1024 * 1024;

    public void showConnecting(String appName, String hostName) {
        if (connecting == null) {
            return;
        }

        stagesSeen = 0;

        // Uma saída pela metade não pode contaminar a entrada: sem isto a tela podia
        // renascer com o alpha em que a saída anterior parou, ou seja, invisível.
        connectingGeneration++;
        cancelConnectingExit();
        connecting.setAlpha(1f);
        setConnectingCancelEnabled(true);

        if (connectingApp != null) {
            connectingApp.setText(appName != null ? appName : "");
        }
        if (connectingStep != null) {
            connectingStep.setText(activity.getString(R.string.vidro_step_talking,
                    hostName != null ? hostName : ""));
        }
        if (connectingBar != null) {
            connectingBar.setProgress(PROGRESS_TALKING);
        }
        if (connectingLight != null) {
            // A luz sobe junto com o progresso: ela é o sinal de que há algo vivo do
            // outro lado, e é por isso que não existe spinner aqui.
            connectingLight.setIntensity(VidroLightView.INTENSITY_ASLEEP, false);
        }

        connecting.setVisibility(View.VISIBLE);
    }

    /**
     * Cada estágio que a conexão anuncia empurra a barra um degrau. Não olhamos o nome do
     * estágio: ele vem do moonlight-common em inglês e muda entre versões, então contar
     * quantos já passaram é mais estável que casar string.
     */
    public void onStageStarting(String stage) {
        if (connecting == null || connecting.getVisibility() != View.VISIBLE) {
            return;
        }

        stagesSeen++;

        if (stagesSeen >= 2 && connectingStep != null && connectingBar != null) {
            connectingStep.setText(R.string.vidro_step_reserving);
            connectingBar.setProgress(PROGRESS_RESERVING);

            if (connectingLight != null) {
                connectingLight.setIntensity(VidroLightView.INTENSITY_AWAKE, true);
            }
        }
    }

    /**
     * O terceiro passo acontece de verdade, em vez de ser escrito e escondido no mesmo
     * quadro.
     *
     * Antes, setText(passo 3), setProgress(100) e setVisibility(GONE) rodavam na mesma
     * passada: a barra sumia parada nos 62% do passo 2 e o vídeo entrava com corte seco. É
     * o único momento do app em que a espera tem forma, e ela terminava no meio.
     *
     * Aqui já HÁ vídeo — connectionStarted() só chega com o stream de pé —, então a barra
     * completa e a tela sai por alpha POR CIMA da imagem que já roda. Nada disto atrasa o
     * primeiro quadro: o que se atrasa é a saída de uma tela que agora sobrepõe o jogo.
     *
     * Os passos continuam presos aos estágios reais do NvConnection; o que ganha tempo é a
     * saída, não a promessa.
     */
    public void onConnected() {
        if (connecting == null) {
            return;
        }

        final int generation = ++connectingGeneration;

        if (connectingStep != null) {
            connectingStep.setText(R.string.vidro_step_video);
        }

        // O mesmo tempo e a mesma curva de saída do resto do app: a barra completando e a
        // tela esmaecendo são as duas metades de um movimento só, e metades de tamanhos
        // diferentes leriam como dois eventos.
        final int exit = activity.getResources().getInteger(R.integer.vidro_motion_exit);
        final Interpolator curve = AnimationUtils.loadInterpolator(activity, R.interpolator.vidro_exit);

        cancelConnectingExit();

        if (connectingBar != null) {
            // ObjectAnimator, e não setProgress(int, boolean): a animação embutida do
            // ProgressBar é API 24 e o piso do app é 21.
            connectingFill = ObjectAnimator.ofInt(connectingBar, "progress", PROGRESS_VIDEO);
            connectingFill.setDuration(exit);
            connectingFill.setInterpolator(curve);
            connectingFill.start();
        }

        // "Cancelar" sai de alcance assim que a saída começa. A tela continua recebendo
        // toque enquanto esmaece, e derrubar por engano uma sessão que acabou de subir é o
        // pior desfecho possível para um toque perdido.
        setConnectingCancelEnabled(false);

        connecting.setAlpha(1f);
        connecting.animate()
                .alpha(0f)
                .setStartDelay(exit)
                .setDuration(exit)
                .setInterpolator(curve)
                // Camada de hardware pela duração da saída: sem ela a árvore inteira da
                // tela de conectar é redesenhada num buffer novo a cada quadro, e esses
                // quadros são justamente os primeiros do vídeo.
                .withLayer()
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        // Só some se ninguém pediu a tela de volta nesse meio-tempo.
                        if (generation == connectingGeneration) {
                            hideConnecting();
                        }
                    }
                })
                .start();
    }

    /**
     * Tira a tela de conectar quando a conexão NÃO deu certo.
     *
     * Só onConnected() a removia, então numa falha o cartão de erro empilhava por cima de
     * uma tela que continuava prometendo progresso — duas superfícies modais ao mesmo
     * tempo, cada uma com a própria saída ("OK" no cartão, "Cancelar" na de baixo) e dois
     * textos diferentes sobre a mesma falha. Quem perde não pode continuar vendo a barra
     * andar.
     */
    public void dismissConnecting() {
        if (connecting == null) {
            return;
        }

        // Falha não tem saída bonita: corta a barra e o esmaecimento pelo meio. A geração
        // sobe ANTES do cancelamento porque cancelar dispara a ação de fim da saída, e ela
        // tem de virar no-op — quem manda aqui é este caminho.
        connectingGeneration++;
        cancelConnectingExit();
        hideConnecting();
    }

    public View getConnectingCancel() {
        return achar(R.id.vidroConnectingCancel);
    }

    /** Some com a tela de conectar e devolve o estado com que ela precisa renascer. */
    private void hideConnecting() {
        connecting.setVisibility(View.GONE);
        connecting.setAlpha(1f);
        setConnectingCancelEnabled(true);
    }

    private void cancelConnectingExit() {
        if (connectingFill != null) {
            connectingFill.cancel();
            connectingFill = null;
        }
        connecting.animate().cancel();
    }

    private void setConnectingCancelEnabled(boolean enabled) {
        View cancel = getConnectingCancel();
        if (cancel != null) {
            cancel.setEnabled(enabled);
        }
    }

    // ==================== folha de ajustes ====================

    /**
     * Quem move o painel agora é a gaveta, e a folha virou o conteúdo dela.
     *
     * A Activity liga isto depois do inflate porque é ela quem conhece o vídeo — este objeto
     * não deve saber que existe uma SurfaceView.
     */
    public void setGaveta(GavetaDaSessao gaveta) {
        this.gaveta = gaveta;
    }

    public boolean isSheetOpen() {
        return gaveta != null && gaveta.estaAberta();
    }

    /**
     * Abrir deixou de ser uma animação e passou a ser um destino.
     *
     * <p>Foi daqui que sumiram trinta linhas, e vale dizer o que elas eram. Havia um contador
     * de gerações — {@code sheetGeneration} — que existia só porque
     * {@code ViewPropertyAnimator.withEndAction} dispara igual ao cancelar e ao terminar:
     * fechar e reabrir depressa fazia a ação de saída apagar a folha que o toque tinha acabado
     * de abrir, e a pessoa tocava de novo achando que não tinha funcionado.
     *
     * <p>Contar gerações à mão é o preço de animação que não pode ser interrompida. Movimento
     * interrompível não tem ação de fim para disparar errado — a mola simplesmente troca de
     * destino e continua da posição em que está. A classe inteira de defeito foi embora com o
     * {@code tween}, e não porque alguém a consertou.
     */
    public void openSheet() {
        if (gaveta == null) {
            return;
        }
        refreshSheet();
        gaveta.abrir();
    }

    public void closeSheet() {
        if (gaveta != null) {
            gaveta.fechar();
        }
    }

    private void wireSheet() {
        if (sheet == null) {
            return;
        }

        View close = achar(R.id.vidroSheetClose);
        if (close != null) {
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeSheet();
                }
            });
        }

        // Tocar fora fecha — e "fora" agora é o vídeo, tratado pela GavetaDaSessao. O painel
        // continua clicável para o toque não atravessar para o jogo que está ao lado dele.
        if (sheet != null) {
            sheet.setClickable(true);
        }

        // Tudo o que a folha comanda é lido a cada quadro ou a cada evento — nada aqui
        // espera a próxima conexão. A taxa de bits era a exceção e por isso saiu: ela
        // entra na StreamConfiguration ao conectar, e MoonBridge não expõe nada que a
        // troque com a sessão de pé.
        bindPacing(R.id.vidroPacingSmooth, PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS);
        bindPacing(R.id.vidroPacingBalanced, PreferenceConfiguration.FRAME_PACING_BALANCED);
        bindPacing(R.id.vidroPacingLatency, PreferenceConfiguration.FRAME_PACING_MIN_LATENCY);

        bindMouseMode(R.id.vidroMouseMultitouch, MOUSE_MODE_MULTITOUCH);
        bindMouseMode(R.id.vidroMouseAbsolute, MOUSE_MODE_ABSOLUTE);
        bindMouseMode(R.id.vidroMouseTrackpad, MOUSE_MODE_TRACKPAD);

        // O que era "Toggle X" no menu modal, um por linha, com o estado à mostra.
        bindSwitch(R.id.vidroZoomRow, new Runnable() {
            @Override
            public void run() {
                controls.toggleZoomMode();
            }
        });

        bindSwitch(R.id.vidroLocalCursorRow, new Runnable() {
            @Override
            public void run() {
                controls.toggleMouseLocalCursor();
            }
        });

        // Números passa a chamar toggleHUD(), e não a acender o pill à mão.
        //
        // A folha ligava a visibilidade de performanceOverlayLite direto e não tocava em
        // enablePerfOverlay. Só que quem escreve o texto é onPerfUpdate(), que escolhe
        // entre o pill e o bloco grande pela preferência: com "Detalhes reduzidos"
        // desligado o texto ia para o outro view e o pill acendia VAZIO. Interruptor
        // ligado, nada na tela.
        bindSwitch(R.id.vidroNumbersRow, new Runnable() {
            @Override
            public void run() {
                controls.toggleHUD();
            }
        });

        bindSwitch(R.id.vidroControllerRow, new Runnable() {
            @Override
            public void run() {
                controls.toggleVirtualController();
            }
        });

        bindSwitch(R.id.vidroFullKeyboardRow, new Runnable() {
            @Override
            public void run() {
                controls.toggleFullKeyboard();
            }
        });

        bindSwitch(R.id.vidroSpecialKeysRow, new Runnable() {
            @Override
            public void run() {
                controls.toggleKeyboardController();
            }
        });
    }

    /**
     * A linha inteira é o alvo; o interruptor só mostra o resultado.
     *
     * Nenhuma destas ações devolve o estado novo — todas são "alterne isso" —, então o
     * interruptor não pode ser desenhado pelo que a pessoa tocou: ele é redesenhado por
     * refreshSheet(), que vai perguntar à sessão o que de fato aconteceu. É a mesma regra
     * dos chips, e é o que impede a folha de mostrar "ligado" quando a camada não subiu.
     */
    private void bindSwitch(final int rowId, final Runnable action) {
        View row = achar(rowId);
        if (row == null) {
            return;
        }

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
                refreshSheet();
            }
        });
    }

    private void bindPacing(final int viewId, final int pacing) {
        View chip = achar(viewId);
        if (chip == null) {
            return;
        }

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefConfig.framePacing = pacing;
                refreshSheet();
            }
        });
    }

    /**
     * O chip do mouse manda no modo de verdade, não só na preferência.
     *
     * Ele escrevia touchscreenTrackpad e parava aí. Mas quem trata o toque é o
     * TouchContext montado uma vez em applyMouseMode(): trocar a preferência com a sessão
     * de pé não reconstruía nada, o chip acendia e o dedo continuava fazendo o de antes.
     * O caminho certo já existia — era o que "Selecionar modo do mouse" chamava lá do
     * fundo do menu avançado.
     */
    private void bindMouseMode(final int viewId, final int mode) {
        View chip = achar(viewId);
        if (chip == null) {
            return;
        }

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controls.setMouseMode(mode);
                refreshSheet();
            }
        });
    }

    /**
     * O estado vem sempre de fora, nunca de memória própria: da preferência quando é
     * preferência, da sessão quando é camada viva. A folha abre e fecha várias vezes por
     * partida, e um gesto de cinco dedos ou um atalho do teclado da capa mexem nas mesmas
     * coisas sem passar por aqui — desenhar pelo que a folha se lembra seria mostrar o
     * estado de alguns minutos atrás.
     */
    /**
     * Põe o painel em dia sem abri-lo.
     *
     * Chamado pela gaveta no início de qualquer abertura — arrasto, toque, Voltar ou acorde.
     * `openSheet()` continua existindo para quem quer as duas coisas de uma vez.
     */
    public void sincronizarFolha() {
        refreshSheet();
    }

    private void refreshSheet() {
        select(R.id.vidroPacingSmooth,
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS);
        select(R.id.vidroPacingBalanced,
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED);
        select(R.id.vidroPacingLatency,
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MIN_LATENCY);

        // A sessão de Remote Input trava o modo do mouse — a tela não mostra nada e o
        // trackpad é o único que faz sentido. O menu escondia a linha inteira nesse caso;
        // a folha esconde a seção, em vez de oferecer três chips que não obedecem.
        View mouseModeGroup = achar(R.id.vidroMouseModeGroup);
        if (mouseModeGroup != null) {
            mouseModeGroup.setVisibility(controls.canChangeMouseMode() ? View.VISIBLE : View.GONE);
        }

        // Os três chips cobrem seis modos: as variações caem no chip da família a que
        // pertencem — trackpad de jogo continua sendo trackpad, botões trocados continua
        // sendo toque absoluto. Sem isso, quem tivesse escolhido uma variação em Ajustes
        // abria a folha e via a fileira inteira apagada, como se não houvesse modo algum.
        int mouseMode = controls.getMouseMode();
        select(R.id.vidroMouseMultitouch, mouseMode == MOUSE_MODE_MULTITOUCH);
        select(R.id.vidroMouseAbsolute,
                mouseMode == MOUSE_MODE_ABSOLUTE || mouseMode == MOUSE_MODE_ABSOLUTE_SWAPPED);
        select(R.id.vidroMouseTrackpad,
                mouseMode == MOUSE_MODE_TRACKPAD || mouseMode == MOUSE_MODE_TRACKPAD_GAMING);

        check(R.id.vidroZoomSwitch, controls.isZoomModeEnabled());
        check(R.id.vidroLocalCursorSwitch, controls.isLocalCursorVisible());
        check(R.id.vidroNumbersSwitch, prefConfig.enablePerfOverlay);
        check(R.id.vidroControllerSwitch, controls.isOnScreenControllerVisible());
        check(R.id.vidroFullKeyboardSwitch, Boolean.TRUE.equals(controls.isKeyboardLayoutVisible()));
        check(R.id.vidroSpecialKeysSwitch, controls.isSpecialKeysVisible());
    }

    /**
     * Procura primeiro NA FOLHA, depois na Activity.
     *
     * A folha vive numa janela própria desde que o compositor do aparelho do dono provou
     * compor a superfície de vídeo por cima do conteúdo da janela (ver
     * Game.mostrarJanelaDaFolha). Fora da hierarquia da Activity, activity.findViewById
     * devolve null para tudo que mora nela — e foi assim que tocar um chip passou a gravar a
     * preferência sem acender o chip: o clique funcionava pela referência guardada, o
     * repintar procurava pelo caminho morto e pulava em silêncio.
     *
     * O fallback pela Activity continua valendo para o que NÃO mora na folha — a moldura, a
     * gaveta, a barra de composição.
     */
    private <T extends View> T achar(int viewId) {
        if (sheet != null) {
            T naFolha = sheet.findViewById(viewId);
            if (naFolha != null) {
                return naFolha;
            }
        }
        return activity.findViewById(viewId);
    }

    private void select(int viewId, boolean selected) {
        View chip = achar(viewId);
        if (chip != null) {
            chip.setSelected(selected);
        }
    }

    private void check(int viewId, boolean checked) {
        Switch toggle = achar(viewId);
        if (toggle != null) {
            toggle.setChecked(checked);
        }
    }
}
