package com.limelight.ui;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import java.util.Collections;

/**
 * A gaveta da sessão — o controle sai de CIMA do vídeo e vai para TRÁS dele.
 *
 * <h2>O que isto substitui</h2>
 *
 * A sessão tinha seis superfícies de controle sobre o vídeo: punho, ilha, folha lateral com
 * véu de tela cheia, barra de composição, menu em lista e a faixa de aviso do topo. Todas
 * obedeciam à mesma regra — desenhar por cima e esconder por temporizador. Na captura real do
 * Tab S8 Ultra isso aparecia como um círculo aceso parado num canto de 1722 dp e uma barra
 * noutro canto: dois inquilinos permanentes do jogo, a meio metro um do outro.
 *
 * <p>Aqui o vídeo anda para o lado e descobre o que estava atrás dele. Em repouso a sessão
 * tem <b>zero</b> pixel de interface. E o ganho não é só de composição: se nada precisa ficar
 * <i>sobre</i> a {@code SurfaceView}, o vidro fosco, o véu e a briga com a GPU do decodificador
 * deixam de existir por eliminação.
 *
 * <h2>O puxador é o dono do gesto, e a borda é do Android</h2>
 *
 * <b>Esta seção descreve uma reversão, e a razão dela é uso real.</b> A primeira versão fazia
 * como o {@code DrawerLayout}: uma banda invisível de 24 dp na borda direita começava o
 * arrasto. Era elegante no papel e insustentável no aparelho — em navegação por gestos, a
 * borda direita é o <b>Voltar do Android</b>, e {@code setSystemGestureExclusionRects} devolve
 * no máximo 200 dp por borda (pedíamos a altura inteira, então quase tudo era ignorado). O
 * relato do dono do fork: puxava a aba de notificações e a barra de baixo sem querer, e saía
 * do app. E o arrasto vertical na mesma banda, que servia para sair da sessão, piorava isso.
 *
 * <p>Então a regra virou: <b>o gesto só começa dentro do puxador</b>, que é uma View de 48 dp
 * de alvo crescendo para DENTRO da tela — fora da faixa que o sistema reivindica. O resto da
 * tela volta a ser do jogo e do Android, inteiro, sem disputa. A exclusão de gesto agora pede
 * só a altura do puxador, que cabe folgada nos 200 dp e por isso é de fato honrada.
 *
 * <p>O que se perdeu: a promessa de "zero pixel de interface em repouso". O que se ganhou: um
 * controle que a pessoa acha e acerta. Não era um empate difícil.
 *
 * <h2>As duas restrições de plataforma, ditas na cara</h2>
 *
 * <ul>
 *   <li><b>Os cantos são do sistema, e nós paramos de disputá-los.</b> A exclusão de gesto
 *       cobre agora só o retângulo do puxador — poucas dezenas de dp, dentro do teto de 200 dp
 *       que o Android honra por borda. Em API &lt; 29 não há exclusão nenhuma e mesmo assim
 *       funciona, porque o alvo não está mais na faixa do sistema. O acorde do teclado e o
 *       resgate de cinco dedos continuam abrindo, como sempre.</li>
 *   <li><b>Transladar {@code SurfaceView} tem sincronia garantida a partir da API 24.</b> Por
 *       isso só {@code translationX}, e <b>nunca</b> mexer na largura da surface: mudar a
 *       largura realoca buffer por quadro, que é o oposto do que se quer com o decodificador
 *       escrevendo ali.</li>
 * </ul>
 */
public class GavetaDaSessao extends FrameLayout {

    /** O que reagir à gaveta. */
    public interface Ouvinte {
        /** Chamado quando a gaveta termina de abrir ou de fechar, não durante o arrasto. */
        void aoMudarGaveta(boolean aberta);

        /**
         * A gaveta vai começar a aparecer, por qualquer caminho.
         *
         * <p>Chamado uma vez, no INÍCIO — no primeiro pixel de um arrasto e antes da mola de
         * uma abertura programática. Quem recebe usa isto para pôr o painel em dia antes de
         * ele ser visto.
         *
         * <p>Existe porque a sincronia estava pendurada num caminho só: o toque no puxador
         * passava por {@code VidroSession.openSheet()}, que sincroniza, e o arrasto, o Voltar
         * e o acorde do teclado chamavam {@code abrir()} direto. Medido no emulador em
         * paisagem: arrastando, o painel subia com nenhum chip aceso.
         */
        void aoComecarAAbrir();

        /**
         * A gaveta assentou FECHADA, viesse de onde viesse o fechamento.
         *
         * <p>Diferente de {@code aoMudarGaveta(false)}, que só dispara quando ela estava
         * ABERTA de fato: um arrasto que começa e volta atrás nunca chegou a "aberta", então
         * aquele aviso não sai — e quem mostrou uma janela no início da abertura ficaria com
         * ela pendurada para sempre. Este sai em todo assentamento em zero, repetido e tudo;
         * quem recebe trata a idempotência.
         */
        void aoAssentarFechada();

        /**
         * O dedo empurrou o vídeo para baixo o bastante: a pessoa quer sair da sessão.
         *
         * <p>Chamado uma vez, no fim do gesto, e só quando ele cruzou o limiar. Quem recebe
         * decide o que "sair" significa — hoje é convocar a casca por cima do vídeo, que
         * continua rodando atrás.
         */
        void aoPedirSaida();
    }

    /**
     * A largura da banda da borda direita que começa um arrasto, em dp.
     *
     * <p>Estreita de propósito: ela não é uma área tocável, é o berço de um gesto. Um toque
     * ou um arrasto vertical que nasça aqui continua indo para o jogo — só o movimento
     * horizontal é reivindicado.
     */
    private static final int BANDA_DP = 24;

    /** Acima disto, o arremesso decide sozinho, mesmo contra a posição. */
    private static final float VELOCIDADE_DE_DECISAO_DP_S = 400f;

    private View painel;
    private View video;
    /** O alvo do gesto. Nulo só em teste de unidade, onde não há layout. */
    private View puxador;
    private Mola mola;
    private Ouvinte ouvinte;

    private final int slop;
    private final float bandaPx;
    private final float velocidadeDeDecisaoPx;

    /**
     * Fração da altura que o vídeo precisa descer para a saída valer.
     *
     * Um terço. Menos que isso e um esbarrão na borda encerra a partida; mais e o gesto vira
     * um exercício de braço. E ele nunca decide sozinho — a velocidade decide primeiro.
     */
    private static final float LIMIAR_DE_SAIDA = 0.33f;

    /**
     * Acima disto o arremesso decide, mesmo contra a posição.
     *
     * Mais alto que o da gaveta (400) de propósito: sair é a ação com consequência, e ela não
     * pode ser fácil de disparar sem querer. Um puxão distraído na borda não chega aqui.
     */
    private static final float VELOCIDADE_DE_SAIDA_DP_S = 900f;

    private VelocityTracker velocimetro;
    private boolean arrastando;
    /** O gesto em curso é o vertical (sair) e não o horizontal (gaveta). */
    private boolean saindo;
    private Mola molaDaSaida;
    private boolean gestoRecusado;
    private float baixouX;
    private float baixouY;
    /** Quando o DOWN aconteceu — separa o toque de dispensa do início de um arrasto. */
    private long tempoDoBaixou;
    private float deslocamentoNoBaixou;
    private boolean aberta;

    public GavetaDaSessao(Context context, AttributeSet attrs) {
        super(context, attrs);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        bandaPx = dp(BANDA_DP);
        velocidadeDeDecisaoPx = dp(VELOCIDADE_DE_DECISAO_DP_S);
    }

    private float dp(float valor) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics());
    }

    /**
     * Liga a gaveta ao painel e ao vídeo.
     *
     * <p>Chamado depois do inflate porque os dois vêm do layout, e porque a Activity é quem
     * sabe qual dos filhos é o vídeo — este grupo não deve saber o que é uma sessão.
     */
    public void ligar(View painel, View video, Ouvinte ouvinte) {
        this.painel = painel;
        this.video = video;
        this.ouvinte = ouvinte;
        this.mola = new Mola(Mola.Perfil.CORPO, 0f, new Mola.Alvo() {
            @Override
            public void aplicar(float valor) {
                aplicarDeslocamento(valor);
            }
        });

        painel.setVisibility(INVISIBLE);
        // Fora da tela desde o começo. `post` porque a largura só existe depois da medida —
        // antes dela, `larguraDoPainel()` é zero e o painel repousaria em cima do vídeo.
        painel.post(new Runnable() {
            @Override
            public void run() {
                painel.setTranslationX(larguraDoPainel());
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            reivindicarBanda();
            // Uma medida nova muda a largura do painel; o deslocamento aberto é uma diferença
            // contra ela e ficaria apontando para o lugar antigo. Reaplicar custa uma conta.
            if (aberta && video != null) {
                aplicarDeslocamento(-larguraDoPainel());
            }
        }
    }

    /**
     * Pede ao sistema para não usar a banda da borda como gesto de Voltar.
     *
     * <p>Só vale a pena com a sessão em pé: fora dela, o Voltar da borda é o gesto certo. Como
     * este grupo só existe dentro da sessão, pedir sempre é o comportamento correto.
     */
    private void reivindicarBanda() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        // SÓ O RETÂNGULO DO PUXADOR, e isso é o que faz a exclusão valer.
        //
        // Pedir a borda direita inteira era pedir ~800 dp de altura, e o Android honra no
        // máximo 200 dp por borda — o efeito prático era quase nenhuma exclusão, e o Voltar
        // do sistema vencia em quase toda a lateral. Um retângulo de ~96 dp cabe folgado no
        // teto e é de fato respeitado.
        if (puxador == null) {
            setSystemGestureExclusionRects(Collections.<Rect>emptyList());
            return;
        }
        setSystemGestureExclusionRects(Collections.singletonList(
                new Rect(puxador.getLeft(), puxador.getTop(),
                        puxador.getRight(), puxador.getBottom())));
    }

    /**
     * Diz se o toque começou dentro do alvo do puxador.
     *
     * Em coordenadas DESTE grupo, que é o pai do puxador — por isso os getters de posição
     * bastam e não há conversão de janela. Sem puxador no layout nada começa, que é o
     * desfecho seguro: o gesto simplesmente não existe e o jogo recebe o toque inteiro.
     */
    private boolean dentroDoPuxador(MotionEvent ev) {
        if (puxador == null || puxador.getVisibility() != VISIBLE) {
            return false;
        }
        float x = ev.getX();
        float y = ev.getY();
        return x >= puxador.getLeft() && x <= puxador.getRight()
                && y >= puxador.getTop() && y <= puxador.getBottom();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        puxador = findViewById(com.limelight.R.id.vidroPuxadorDaGaveta);
    }

    /**
     * Avisa o ouvinte, no máximo uma vez por abertura.
     *
     * O arrasto pode cruzar o limiar e voltar; sem a trava, um vaivém sincronizaria a folha
     * a cada travessia, e sincronizar lê preferência e reconstrói chips.
     */
    private void avisarQueVaiAbrir() {
        if (jaAvisouQueAbre || ouvinte == null) {
            return;
        }
        jaAvisouQueAbre = true;
        ouvinte.aoComecarAAbrir();
    }

    /** Rearmado quando a gaveta termina de fechar. */
    private boolean jaAvisouQueAbre;

    private float larguraDoPainel() {
        return painel == null ? 0f : painel.getWidth();
    }

    /**
     * Quanto o VÍDEO precisa andar para caber ao lado do painel — que é menos que a largura
     * dele.
     *
     * <p>O vídeo é 16:9 e a tela em paisagem não é: o {@code StreamContainer} faz letterbox,
     * então já existe uma tarja preta à direita da imagem. Deslizar o vídeo pela largura
     * inteira do painel arrasta essa tarja para dentro da tela, e ela aparece como uma costura
     * preta entre a imagem e o painel — medido em 2316x1080: 236 px de preto entre as duas
     * coisas, que lê como componente quebrado.
     *
     * <p>A tarja é espaço livre. Descontá-la encosta a borda da imagem na borda do painel, sem
     * costura, e ainda esconde MENOS imagem do que antes.
     *
     * <p>Devolve a largura do painel quando não há tarja ou quando não dá para medir.
     */
    private float deslocamentoDoVideo() {
        float largura = larguraDoPainel();
        View superficie = primeiraSuperficie(video);
        if (superficie == null) {
            return largura;
        }
        float tarja = getWidth() - superficie.getRight();
        if (tarja <= 0f) {
            return largura;
        }
        float necessario = largura - tarja;
        return necessario < 0f ? 0f : necessario;
    }

    /** A primeira SurfaceView na subárvore, que é onde a imagem realmente está. */
    private static View primeiraSuperficie(View raiz) {
        if (raiz instanceof android.view.SurfaceView) {
            return raiz;
        }
        if (raiz instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) raiz;
            for (int i = 0; i < g.getChildCount(); i++) {
                View achado = primeiraSuperficie(g.getChildAt(i));
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }

    private void aplicarDeslocamento(float x) {
        if (video == null || painel == null) {
            return;
        }
        float largura = larguraDoPainel();
        if (x < -largura) {
            x = -largura;
        }
        if (x > 0f) {
            x = 0f;
        }
        // O GESTO É MEDIDO EM PIXELS DO PAINEL, e o vídeo recebe uma fração disso.
        //
        // Os dois andam distâncias diferentes: o painel precisa percorrer a própria largura
        // inteira para entrar, e o vídeo precisa andar só até encostar nele — a diferença é a
        // tarja do letterbox. Manter o gesto na medida do painel deixa toda a matemática de
        // limiar, velocidade e mola exatamente como estava.
        float larguraDoPainelAgora = larguraDoPainel();
        float fracaoDoVideo = larguraDoPainelAgora > 0f
                ? deslocamentoDoVideo() / larguraDoPainelAgora
                : 1f;
        video.setTranslationX(x * fracaoDoVideo);

        // O PAINEL ANDA SOZINHO, porque ele deixou de estar atrás do vídeo.
        //
        // Ele repousa fora da tela, à direita da própria largura, e entra à medida que o vídeo
        // sai: em x = 0 ele está inteiro fora; em x = -largura ele está inteiro dentro. Antes
        // ele ficava parado e era DESCOBERTO pelo vídeo — o que dependia de a SurfaceView
        // mover o próprio furo de transparência junto com a translação, e isso não vale em
        // todo compositor. Ver o comentário no activity_game.xml.
        painel.setTranslationX(larguraDoPainel() + x);

        // O painel só existe enquanto há o que ver dele. Fora disso ele sai da composição —
        // não basta estar fora da tela, porque fora da tela ainda desenha.
        painel.setVisibility(x < -0.5f ? VISIBLE : INVISIBLE);
        if (x > -0.5f) {
            jaAvisouQueAbre = false;
        }

        // O PUXADOR SOME COM A GAVETA ABERTA.
        //
        // Medido com uiautomator em paisagem: o alvo do puxador (48 dp na borda direita)
        // cruzava as caixas de "Least delay" e "Trackpad", os chips da coluna da direita. Meia
        // dúzia de pixels do chip abriam a gaveta que já estava aberta em vez de trocar o
        // modo. Com ela aberta o puxador não tem trabalho: quem fecha é o toque no vídeo.
        if (puxador != null) {
            puxador.setVisibility(x < -0.5f ? INVISIBLE : VISIBLE);
        }
    }

    /**
     * Onde o gesto está, em pixels do PAINEL: 0 fechada, -largura aberta.
     *
     * Lido do painel e não do vídeo porque os dois deixaram de andar a mesma distância — ver
     * {@link #deslocamentoDoVideo()}. Ler do vídeo aqui faria o arrasto pular ao ser agarrado
     * no meio, e o limiar de saída decidir com o número errado.
     */
    private float deslocamentoAtual() {
        if (painel == null) {
            return 0f;
        }
        return painel.getTranslationX() - larguraDoPainel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // O gesto
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (video == null || painel == null) {
            return false;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                baixouX = ev.getX();
                baixouY = ev.getY();
                deslocamentoNoBaixou = deslocamentoAtual();
                arrastando = false;
                // Com a gaveta fechada, só a banda da borda direita começa um arrasto. Com ela
                // aberta, qualquer lugar do vídeo serve — o vídeo virou a superfície de
                // dispensa, que é o papel que o véu tinha.
                // FECHADA, SÓ O PUXADOR COMEÇA. Aberta, o vídeo inteiro dispensa.
                //
                // Era `baixouX < getWidth() - bandaPx`, uma banda cega de 24 dp na borda: o
                // mesmo lugar do Voltar por gesto do Android. Ver a seção "O puxador é o dono
                // do gesto" no topo desta classe.
                gestoRecusado = !aberta && !dentroDoPuxador(ev);
                if (!gestoRecusado) {
                    velocimetro = VelocityTracker.obtain();
                    velocimetro.addMovement(ev);
                    // A mola em curso morre no toque. Agarrar no meio é a razão de ela existir.
                    mola.cancelar();
                }

                // ABERTA, O VÍDEO É SÓ DISPENSA. O DOWN é consumido aqui e o jogo nem o vê:
                // com o painel de pé, quem toca a imagem está falando com o app — e um toque
                // que também virasse clique dentro da stream seria o pior dos dois mundos.
                // O doc desta classe prometia esse fechamento desde a primeira versão; a
                // implementação só tratava arrasto, e um toque parado atravessava para o
                // jogo. Toque curto fecha (ver ACTION_UP); dedo que anda vira arrasto normal.
                if (aberta && !gestoRecusado) {
                    arrastando = true;
                    tempoDoBaixou = ev.getEventTime();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE: {
                if (gestoRecusado) {
                    return false;
                }
                if (velocimetro != null) {
                    velocimetro.addMovement(ev);
                }
                float dx = ev.getX() - baixouX;
                float dy = ev.getY() - baixouY;
                if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
                    // Só agora o gesto é nosso. O filho recebe ACTION_CANCEL do framework.
                    arrastando = true;
                    if (!aberta) {
                        avisarQueVaiAbrir();
                    }
                    return true;
                }
                if (dy > slop && Math.abs(dy) > Math.abs(dx) && !aberta) {
                    // PARA BAIXO, NA MESMA BANDA: sair da sessão.
                    //
                    // A banda da borda direita já era nossa para o arrasto horizontal, e o
                    // vertical nela estava sendo devolvido ao jogo — espaço livre, sem
                    // disputar com nada. E a borda de CIMA, que seria a escolha óbvia, é do
                    // sistema: lá mora a aba de notificações.
                    //
                    // Só com a gaveta fechada. Com ela aberta, o vertical continua sendo do
                    // painel, que rola.
                    arrastando = true;
                    saindo = true;
                    return true;
                }
                if (Math.abs(dy) > slop) {
                    // Vertical fora do caso de saída: devolve o gesto ao jogo de uma vez,
                    // para não ficar espiando um arrasto que nunca vai ser nosso.
                    gestoRecusado = true;
                    soltarVelocimetro();
                }
                return false;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Um toque simples sobre o vídeo com a gaveta aberta fecha. É o que o véu fazia,
                // sem o véu.
                if (!gestoRecusado && aberta && !arrastando) {
                    fechar();
                }
                soltarVelocimetro();
                return false;

            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!arrastando || video == null) {
            return false;
        }
        if (velocimetro != null) {
            velocimetro.addMovement(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                // Enquanto o dedo está na tela não há mola: há posição seguindo o dedo.
                if (saindo) {
                    molaDaSaida().fixar(Math.max(0f, ev.getY() - baixouY));
                } else {
                    mola.fixar(deslocamentoNoBaixou + (ev.getX() - baixouX));
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float velocidade = 0f;
                if (velocimetro != null) {
                    velocimetro.computeCurrentVelocity(1000);
                    velocidade = velocimetro.getXVelocity();
                }
                float velocidadeY = 0f;
                if (velocimetro != null) {
                    velocidadeY = velocimetro.getYVelocity();
                }
                soltarVelocimetro();
                arrastando = false;

                if (saindo) {
                    saindo = false;
                    decidirSaida(velocidadeY);
                } else if (aberta
                        && Math.abs(ev.getX() - baixouX) < slop
                        && Math.abs(ev.getY() - baixouY) < slop
                        && ev.getEventTime() - tempoDoBaixou < 300) {
                    // O toque de dispensa: parado e curto. Os 300 ms separam o toque de um
                    // aperto pensativo que ia virar arrasto e desistiu — esse não fecha.
                    fechar();
                } else {
                    decidir(velocidade);
                }
                return true;
            }

            default:
                return true;
        }
    }

    private void soltarVelocimetro() {
        if (velocimetro != null) {
            velocimetro.recycle();
            velocimetro = null;
        }
    }

    /**
     * Para onde a gaveta vai quando o dedo sai.
     *
     * <p>O limiar é a <b>direção em que ela estava indo</b>, e só quando não há direção nenhuma
     * é que a posição decide. É a diferença entre um gesto e um formulário: com limiar só de
     * distância, um puxão curto e rápido — que é como se abre uma gaveta de verdade — não
     * abriria nada, porque não chegou à metade.
     */
    private void decidir(float velocidade) {
        float largura = larguraDoPainel();
        boolean vaiAbrir;
        if (Math.abs(velocidade) > velocidadeDeDecisaoPx) {
            vaiAbrir = velocidade < 0f;
        } else {
            vaiAbrir = deslocamentoAtual() < -largura / 2f;
        }
        deslizarPara(vaiAbrir ? -largura : 0f, velocidade, vaiAbrir);
    }

    private Mola molaDaSaida() {
        if (molaDaSaida == null) {
            // TELA, e não CORPO: o que se move aqui é a imagem inteira, que é a coisa mais
            // pesada que este app tem.
            molaDaSaida = new Mola(Mola.Perfil.TELA, 0f, new Mola.Alvo() {
                @Override
                public void aplicar(float valor) {
                    if (video == null) {
                        return;
                    }
                    video.setTranslationY(valor);

                    // Encolhe junto, e proporcional ao percurso: a imagem indo embora é uma
                    // coisa só se afastando, não duas animações concordando por acaso.
                    float fracao = getHeight() <= 0 ? 0f : valor / getHeight();
                    float escala = 1f - Math.min(fracao, 1f) * 0.22f;
                    video.setScaleX(escala);
                    video.setScaleY(escala);
                }
            });
        }
        return molaDaSaida;
    }

    /**
     * A regra da saída, sem View e sem relógio — para poder ser provada.
     *
     * <p>Estática de propósito: o caso que ela precisa impedir é o <b>encerramento
     * acidental</b>, e um caso que não se consegue exercitar é um caso que se acredita. Ver
     * {@code SaidaPorGestoTest}.
     *
     * @param velocidade em px/s no eixo Y; positiva é para baixo
     * @param limiarDeVelocidade acima disto o arremesso decide sozinho
     * @param percorrido quanto o vídeo já desceu, em px
     * @param altura a altura do quadro, em px
     */
    static boolean decideSair(float velocidade, float limiarDeVelocidade,
                              float percorrido, float altura) {
        // A DIREÇÃO DECIDE PRIMEIRO, e a posição só quando não há direção nenhuma.
        //
        // Com limiar só de distância, um puxão curto e rápido — que é como se empurra uma
        // coisa para longe — não faria nada. Com limiar só de velocidade, um arrasto lento e
        // longo também não. Os dois juntos cobrem as duas intenções.
        if (Math.abs(velocidade) > limiarDeVelocidade) {
            return velocidade > 0f;
        }
        return percorrido > altura * LIMIAR_DE_SAIDA;
    }

    /**
     * Para onde o vídeo vai quando o dedo sai — e se a sessão sai com ele.
     *
     * <p>A velocidade decide primeiro, e a posição só quando não há direção nenhuma. É a
     * mesma regra da gaveta e pelo mesmo motivo: com limiar só de distância, um puxão curto e
     * rápido — que é como se empurra uma coisa para longe — não faria nada.
     *
     * <p>Os dois limiares existem juntos porque só um não basta. Só distância deixa um
     * arremesso curto sem efeito; só velocidade deixa um arrasto lento e longo sem efeito. E
     * os dois são mais altos que os da gaveta, porque sair tem consequência e abrir não.
     */
    private void decidirSaida(float velocidade) {
        boolean vaiSair = decideSair(velocidade, dp(VELOCIDADE_DE_SAIDA_DP_S),
                molaDaSaida().valor(), getHeight());

        if (vaiSair) {
            // Devolve a imagem ao lugar ANTES de avisar: a casca vem por cima, e o vídeo que
            // ficar embaixo dela tem de estar inteiro para quando ela sair.
            molaDaSaida().fixar(0f);
            if (ouvinte != null) {
                ouvinte.aoPedirSaida();
            }
            return;
        }

        // Mudou de ideia no meio: a imagem volta com a velocidade que o dedo deixou.
        molaDaSaida().lancar(0f, velocidade);
    }

    private void deslizarPara(float destino, float velocidade, final boolean ficaAberta) {
        if (video == null) {
            return;
        }
        if (painel != null && destino < 0f) {
            painel.setVisibility(VISIBLE);
        }
        mola.lancar(destino, velocidade, new Mola.AoChegar() {
            @Override
            public void chegou() {
                anunciar(ficaAberta);
                if (!ficaAberta && ouvinte != null) {
                    ouvinte.aoAssentarFechada();
                }
            }
        });
    }

    private void anunciar(boolean nova) {
        if (aberta == nova) {
            return;
        }
        aberta = nova;
        if (ouvinte != null) {
            ouvinte.aoMudarGaveta(nova);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // O caminho sem dedo — teclado, acorde, resgate
    // ─────────────────────────────────────────────────────────────────────────

    public boolean estaAberta() {
        return aberta || deslocamentoAtual() < -0.5f;
    }

    public void abrir() {
        if (painel == null) {
            return;
        }
        avisarQueVaiAbrir();
        painel.setVisibility(VISIBLE);

        // A GAVETA ABRE NO TOPO, sempre.
        //
        // Sem isto ela abria ROLADA: o painel e mais alto que a tela, e quando ele aparece
        // algum filho pede foco -- um chip, um interruptor -- e o ScrollView rola ate ele.
        // No emulador o resultado foi abrir com o cabecalho, o nome do app e os dois
        // primeiros grupos acima da area visivel, o que le como painel quebrado.
        //
        // scrollTo e nao smoothScrollTo: a gaveta esta entrando, e uma rolagem animada
        // competindo com a entrada sao dois movimentos para uma coisa so.
        if (painel instanceof android.widget.ScrollView) {
            ((android.widget.ScrollView) painel).scrollTo(0, 0);
        }
        // O painel pode nunca ter sido medido: abrir antes da primeira medida deslizaria para
        // zero e a gaveta ficaria "aberta" sem nada à mostra.
        if (painel.getWidth() == 0) {
            painel.post(new Runnable() {
                @Override
                public void run() {
                    deslizarPara(-larguraDoPainel(), 0f, true);
                }
            });
            return;
        }
        deslizarPara(-larguraDoPainel(), 0f, true);
    }

    public void fechar() {
        deslizarPara(0f, 0f, false);
    }

    public void alternar() {
        if (estaAberta()) {
            fechar();
        } else {
            abrir();
        }
    }

    /** Fecha na hora, sem mola — para quando a sessão está indo embora. */
    public void fecharSeco() {
        mola.fixar(0f);
        anunciar(false);
        if (ouvinte != null) {
            ouvinte.aoAssentarFechada();
        }
    }
}
