package com.limelight.utils;

/**
 * Para onde o toque leva, na home e no catálogo.
 *
 * Isto existe porque a mesma pergunta era respondida em três lugares que não se falavam:
 * o cartão da home escrevia "sessão aberta" com uma condição, o toque no cartão ignorava
 * a condição e sempre abria o catálogo, e o toque no jogo do catálogo dependia de uma
 * preferência que nascia desligada. Com a regra fora das Activities, o cartão e o toque
 * consultam a MESMA função — dizer uma coisa e fazer outra passa a ser impossível, não
 * apenas improvável.
 *
 * Sem estado e sem Android: dá para cobrir por teste, que é a outra metade da razão.
 */
public final class ResumeDecision {

    private ResumeDecision() {
    }

    /** O que um toque no cartão de host deve fazer. */
    public enum HostTap {
        /**
         * Host que não respondeu (dormindo, fora do ar ou ainda sem resposta). Não há
         * ação óbvia: quem decide é o menu, que tem Wake-on-LAN, detalhes e apagar.
         */
        MENU,
        /** Host visto agora e ainda não pareado — parear é a única coisa que falta. */
        PAIR,
        /** Há sessão aberta lá: o toque volta para ela. */
        RESUME,
        /** Host pronto e sem nada rodando: o toque abre o catálogo. */
        CATALOG
    }

    /** O que um toque num jogo do catálogo deve fazer. */
    public enum AppTap {
        /** Nada rodando no host: iniciar este jogo. */
        START,
        /** É justamente o jogo que já está rodando: voltar para ele. */
        RESUME,
        /**
         * Outro jogo está rodando. Aqui há decisão de verdade — a única saída é derrubar
         * a sessão aberta —, então o toque abre o menu em vez de agir.
         */
        MENU
    }

    /**
     * @param online       o host respondeu ao último sondeio (State.ONLINE)
     * @param paired       o pareamento está válido
     * @param runningGameId o que o host diz estar rodando; zero quando não há nada
     */
    public static HostTap forHost(boolean online, boolean paired, int runningGameId) {
        // A ordem importa: runningGameId é transiente e sobrevive na memória depois de o
        // host cair, então afirmar sessão aberta sem ONLINE mandaria o toque para uma
        // sessão que não existe mais — e o cartão escreveria a mesma mentira.
        if (!online) {
            return HostTap.MENU;
        }
        if (!paired) {
            return HostTap.PAIR;
        }
        return runningGameId != 0 ? HostTap.RESUME : HostTap.CATALOG;
    }

    /**
     * Verdadeiro quando o cartão pode escrever "sessão aberta". É a mesma condição do
     * desvio do toque, de propósito.
     */
    public static boolean hasOpenSession(boolean online, boolean paired, int runningGameId) {
        return forHost(online, paired, runningGameId) == HostTap.RESUME;
    }

    /**
     * @param runningAppId o app que o host reporta rodando; zero quando não há nada
     * @param tappedAppId  o app que a pessoa tocou
     */
    public static AppTap forApp(int runningAppId, int tappedAppId) {
        if (runningAppId == 0) {
            return AppTap.START;
        }
        return runningAppId == tappedAppId ? AppTap.RESUME : AppTap.MENU;
    }
}
