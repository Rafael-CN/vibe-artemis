package com.limelight.ui;

import android.view.Choreographer;

/**
 * Uma mola — o movimento do lado View, onde não há Compose.
 *
 * <h2>Por que isto existe</h2>
 *
 * Até aqui todo movimento do app dizia a mesma frase: duração + curva + alpha. Uma varredura
 * do repositório inteiro não achava uma {@code SpringAnimation}, um {@code VelocityTracker},
 * um {@code FlingAnimation} nem um {@code Animatable}. O sintoma mais claro estava na própria
 * identidade contra si mesma: a tabela de movimento tinha a linha <i>"voltar por gesto ·
 * 220 ms · segue o dedo"</i>, ela virou {@code vidro_motion_back} em {@code motion.xml}, e
 * não tinha um único chamador. A única entrada interrompível da linguagem nunca foi ligada.
 *
 * <p>O problema não era quantidade de movimento. Era que <b>uma tabela de durações não
 * consegue expressar interrupção</b>. É por isso que aquela tabela tinha oito linhas e seis
 * delas eram a mesma ideia em velocidades diferentes: duração pressupõe que ninguém agarra no
 * meio. Uma mola tem dois números e cobre as oito.
 *
 * <p><b>E o que ela NÃO compra, medido em {@code MolaTest} e dito aqui para ninguém prometer
 * demais:</b> uma mola linear é invariante a escala, e uma criticamente amortecida absorve a
 * velocidade inicial sob a mesma envoltória que governa o resto. Nem a distância nem a força
 * do arremesso mudam o tempo de chegada de um {@code CORPO}. A afirmação de que "duas viagens
 * da mesma distância deixam de durar o mesmo tempo" só vale abaixo do amortecimento crítico —
 * e é por isso que {@code IMA} existe como perfil próprio, e não como ajuste do {@code CORPO}.
 *
 * <p>O que ela compra, então, são duas coisas concretas. A primeira é <b>interrupção</b>: um
 * destino novo no meio do voo continua da posição e da velocidade atuais, em vez de recomeçar.
 * A segunda é <b>coerência</b>: o mesmo par (rigidez, amortecimento) serve a qualquer
 * distância, então três perfis substituem oito linhas de tabela sem ninguém escolher um número
 * por trajeto.
 *
 * <h2>Por que escrita à mão, e não androidx.dynamicanimation</h2>
 *
 * Porque uma dependência nova é um ADR, e porque em sessão o orçamento de quadro é do vídeo:
 * este laço precisa estar sob nosso controle para poder ser interrompido, medido e desligado.
 * São quarenta linhas de integração e nenhum comportamento escondido.
 *
 * <h2>O laço</h2>
 *
 * Euler semi-implícito, que é o suficiente para os deslocamentos deste app e é estável nos
 * passos de tempo que um {@code Choreographer} entrega. O passo é limitado a 32 ms porque um
 * quadro perdido não pode virar um salto: sem o teto, voltar de uma pausa de 300 ms integraria
 * a mola por 300 ms de uma vez e o objeto atravessaria o destino.
 */
public final class Mola implements Choreographer.FrameCallback {

    /** O que a mola move. Chamado uma vez por quadro, na thread da interface. */
    public interface Alvo {
        void aplicar(float valor);
    }

    /** Chamado uma vez quando a mola chega e para. Nunca chamado quando ela é cancelada. */
    public interface AoChegar {
        void chegou();
    }

    /**
     * Massa do que se move — a única escolha que quem chama precisa fazer.
     *
     * <p>Três, e não oito, porque a massa é a coisa real: o que muda entre a mola de um chip e
     * a de uma gaveta não é o gosto de quem desenhou, é o tamanho do que está sendo movido.
     */
    public enum Perfil {
        /** Toque, chip, ponto de estado. Rápido e sem sobra. */
        LEVE(1500f, 1f),
        /** Gaveta, folha, realce, punho. O corpo da interface. */
        CORPO(600f, 1f),
        /** O que virou a tela inteira. Pesado, e por isso mais lento. */
        TELA(320f, 1f),
        /**
         * A exceção, e ela é uma só no app inteiro: o punho grudando na âncora.
         *
         * <p>O olho lê ímã pelo pequeno excesso na chegada. Em qualquer outro lugar
         * sobressalto é a versão brinquedo desta ideia — é assim que física vira decoração.
         */
        IMA(620f, 0.72f);

        final float rigidez;
        final float amortecimentoRelativo;

        Perfil(float rigidez, float amortecimentoRelativo) {
            this.rigidez = rigidez;
            this.amortecimentoRelativo = amortecimentoRelativo;
        }
    }

    /** Passo máximo integrado num quadro. Ver a nota do laço, acima. */
    private static final float PASSO_MAXIMO_S = 0.032f;

    /**
     * Quando parar.
     *
     * <p>Em pixels e em pixels por segundo, e não em fração do percurso: um percurso de 4 px e
     * um de 900 px param no mesmo lugar visual, que é "não dá mais para ver a diferença".
     */
    private static final float REPOUSO_DISTANCIA_PX = 0.5f;
    private static final float REPOUSO_VELOCIDADE_PX_S = 8f;

    private final Choreographer choreographer = Choreographer.getInstance();
    private final Alvo alvo;
    private final float rigidez;
    private final float amortecimento;

    private float valor;
    private float velocidade;
    private float destino;
    private boolean correndo;
    private long ultimoQuadroNanos;
    private AoChegar aoChegar;

    public Mola(Perfil perfil, float valorInicial, Alvo alvo) {
        this.alvo = alvo;
        this.rigidez = perfil.rigidez;
        // O amortecimento crítico de uma mola de rigidez k é 2·√k. O perfil diz a fração
        // dele: 1 chega sem passar, abaixo de 1 passa e volta.
        this.amortecimento = 2f * (float) Math.sqrt(perfil.rigidez) * perfil.amortecimentoRelativo;
        this.valor = valorInicial;
        this.destino = valorInicial;
    }

    /**
     * Manda a mola para um destino, herdando a velocidade que vier.
     *
     * <p>Pode ser chamado a qualquer momento, inclusive com a mola em movimento — é
     * exatamente para isso que ela existe. O estado atual (posição e velocidade) não é
     * descartado: a mola continua de onde está, na direção nova. É isso que faz uma
     * transição poder ser agarrada no meio.
     *
     * @param velocidadeInicial em unidades por segundo; {@code Float.NaN} mantém a atual.
     */
    public void lancar(float destino, float velocidadeInicial, AoChegar aoChegar) {
        this.destino = destino;
        this.aoChegar = aoChegar;
        if (!Float.isNaN(velocidadeInicial)) {
            this.velocidade = velocidadeInicial;
        }
        if (!correndo) {
            correndo = true;
            ultimoQuadroNanos = 0;
            choreographer.postFrameCallback(this);
        }
    }

    public void lancar(float destino, float velocidadeInicial) {
        lancar(destino, velocidadeInicial, null);
    }

    /**
     * Põe a mola num valor sem animar — o caso do dedo mandando.
     *
     * <p>Enquanto o dedo está na tela não há mola: há posição seguindo o dedo. A mola só
     * começa quando o dedo sai, e é aí que ela recebe a velocidade que ele deixou.
     */
    public void fixar(float valor) {
        cancelar();
        this.valor = valor;
        this.destino = valor;
        this.velocidade = 0f;
        alvo.aplicar(valor);
    }

    public void cancelar() {
        if (correndo) {
            correndo = false;
            aoChegar = null;
            choreographer.removeFrameCallback(this);
        }
    }

    public float valor() {
        return valor;
    }

    public float velocidade() {
        return velocidade;
    }

    public boolean correndo() {
        return correndo;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!correndo) {
            return;
        }

        if (ultimoQuadroNanos == 0) {
            // O primeiro quadro não integra nada: ele só marca o relógio. Integrar contra um
            // zero daria um passo do tamanho do uptime do aparelho.
            ultimoQuadroNanos = frameTimeNanos;
            choreographer.postFrameCallback(this);
            return;
        }

        float passo = (frameTimeNanos - ultimoQuadroNanos) / 1_000_000_000f;
        ultimoQuadroNanos = frameTimeNanos;

        if (integrar(passo)) {
            correndo = false;
            alvo.aplicar(valor);

            AoChegar fim = aoChegar;
            aoChegar = null;
            if (fim != null) {
                fim.chegou();
            }
            return;
        }

        alvo.aplicar(valor);
        choreographer.postFrameCallback(this);
    }

    /**
     * Um passo do laço. Separado do quadro para poder ser exercitado sem Choreographer.
     *
     * @param passo em segundos, já como o relógio o entregou
     * @return true quando a mola chegou e parou
     */
    boolean integrar(float passo) {
        if (passo > PASSO_MAXIMO_S) {
            passo = PASSO_MAXIMO_S;
        }

        float deslocamento = valor - destino;
        float aceleracao = -rigidez * deslocamento - amortecimento * velocidade;
        velocidade += aceleracao * passo;
        valor += velocidade * passo;

        if (Math.abs(valor - destino) < REPOUSO_DISTANCIA_PX
                && Math.abs(velocidade) < REPOUSO_VELOCIDADE_PX_S) {
            // Encosta no destino EXATO ao parar. Sem isto o objeto descansa a meio pixel do
            // lugar, e meio pixel de erro acumulado por transição é visível numa borda.
            valor = destino;
            velocidade = 0f;
            return true;
        }
        return false;
    }
}
