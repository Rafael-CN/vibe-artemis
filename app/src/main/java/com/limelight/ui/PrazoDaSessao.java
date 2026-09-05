package com.limelight.ui;

import android.os.Handler;

/**
 * O prazo de uma sessão que ficou viva atrás da casca.
 *
 * <h2>Por que ele existe</h2>
 *
 * Sair de uma sessão deixou de matá-la: a casca é convocada por cima do vídeo e a conexão
 * continua de pé, para que escolher outra coisa não custe derrubar o que está rodando. Essa é
 * a metade boa.
 *
 * <p>A outra metade é que uma sessão esquecida segura o display virtual do host, queima
 * bateria e queima banda — e quem descobre isso descobre pelo host, não pelo app. O prazo é o
 * que impede a continuidade de virar uma sessão fantasma.
 *
 * <h2>Por que é uma classe e não três linhas dentro de Game</h2>
 *
 * Porque as três linhas seriam três linhas num arquivo de seis mil, e a regra que elas
 * implementam tem quatro casos que interagem: armar, desarmar, não armar duas vezes, e o
 * PiP. Aqui elas são testáveis sem aparelho — e são, em {@code PrazoDaSessaoTest}.
 */
public class PrazoDaSessao {

    /**
     * Dois minutos.
     *
     * <p>Mais que o suficiente para escolher outra coisa, e curto o bastante para não virar
     * uma sessão que ninguém lembra que existe. O número é da decisão do dono do fork, não de
     * uma medição — e está aqui num lugar só para poder mudar num lugar só.
     */
    public static final long PADRAO_MS = 2 * 60 * 1000;

    private final Handler relogio;
    private final Runnable aoVencer;
    private final long prazoMs;

    private boolean armado;

    /**
     * Verdadeiro enquanto a janela está em picture-in-picture.
     *
     * <p>Em PiP a imagem está À VISTA — pequena, mas à vista. Derrubar ali seria matar
     * exatamente o que a pessoa escolheu continuar vendo. O prazo existe para o caso em que o
     * vídeo está escondido atrás da casca, e só para ele.
     */
    private boolean emPiP;

    public PrazoDaSessao(Handler relogio, long prazoMs, Runnable aoVencer) {
        this.relogio = relogio;
        this.prazoMs = prazoMs;
        this.aoVencer = aoVencer;
    }

    public boolean armado() {
        return armado;
    }

    /** Começa a contar. Chamar duas vezes não reinicia — quem já está esperando, espera. */
    public void armar() {
        if (armado || emPiP) {
            return;
        }
        armado = true;
        relogio.postDelayed(aoVencer, prazoMs);
    }

    /** Para de contar. Idempotente: desarmar o que não está armado é caso normal. */
    public void desarmar() {
        if (!armado) {
            return;
        }
        armado = false;
        relogio.removeCallbacks(aoVencer);
    }

    /**
     * Entrar em PiP desarma; sair NÃO rearma sozinho.
     *
     * <p>Não rearmar é deliberado: sair do PiP devolve a sessão ao primeiro plano, e quem
     * decide se a casca está à mostra é a própria casca. Rearmar aqui seria adivinhar.
     */
    public void aoMudarPiP(boolean entrou) {
        emPiP = entrou;
        if (entrou) {
            desarmar();
        }
    }
}
