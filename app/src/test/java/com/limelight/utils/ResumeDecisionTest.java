package com.limelight.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Voltar para a sessão aberta é o gesto mais frequente do dia, e era o que mais custava
 * toques: o cartão da home escrevia "sessão aberta" e o toque nele abria o catálogo, de
 * onde a pessoa tocava de novo no mesmo jogo — três toques e duas telas para voltar ao
 * que já estava rodando.
 *
 * Estes testes prendem as duas regras que tiraram esses toques, e principalmente prendem
 * a que amarra o cartão ao toque: o que a linha diz e para onde o toque leva vêm da mesma
 * função, então não podem discordar sem isto falhar.
 */
public class ResumeDecisionTest {

    private static final int ONLINE = 42;
    private static final int OUTRO = 7;

    @Test
    public void hostComSessaoAberta_oToqueRetoma() {
        assertEquals(ResumeDecision.HostTap.RESUME,
                ResumeDecision.forHost(true, true, ONLINE));
    }

    @Test
    public void hostProntoESemNadaRodando_oToqueAbreOCatalogo() {
        assertEquals(ResumeDecision.HostTap.CATALOG,
                ResumeDecision.forHost(true, true, 0));
    }

    @Test
    public void hostVistoENaoPareado_oToquePareia() {
        // Mesmo com runningGameId sujo na memória: sem pareamento não há sessão nossa
        // para retomar, e parear é a única coisa que falta.
        assertEquals(ResumeDecision.HostTap.PAIR,
                ResumeDecision.forHost(true, false, ONLINE));
    }

    @Test
    public void hostQueNaoRespondeu_caiNoMenu() {
        // runningGameId é transiente e sobrevive na memória depois de o host cair. Mandar
        // o toque para uma sessão que não existe mais é pior que não fazer nada.
        assertEquals("host fora do ar não retoma", ResumeDecision.HostTap.MENU,
                ResumeDecision.forHost(false, true, ONLINE));
        assertEquals("host sem resposta também não", ResumeDecision.HostTap.MENU,
                ResumeDecision.forHost(false, true, 0));
    }

    @Test
    public void oCartaoSoAnunciaSessaoQuandoOToqueRealmenteRetoma() {
        // Esta é a asserção que impede o bug original de voltar: o aviso escrito na linha
        // e o desvio do toque são a MESMA condição.
        boolean[] valores = {false, true};
        for (boolean online : valores) {
            for (boolean pareado : valores) {
                for (int rodando : new int[] {0, ONLINE}) {
                    boolean anuncia = ResumeDecision.hasOpenSession(online, pareado, rodando);
                    boolean retoma = ResumeDecision.forHost(online, pareado, rodando)
                            == ResumeDecision.HostTap.RESUME;
                    assertEquals("cartão e toque discordaram em online=" + online
                                    + " pareado=" + pareado + " rodando=" + rodando,
                            retoma, anuncia);
                }
            }
        }

        // E o caso que motivou tudo continua sendo o único que anuncia.
        assertTrue(ResumeDecision.hasOpenSession(true, true, ONLINE));
        assertFalse(ResumeDecision.hasOpenSession(true, true, 0));
    }

    @Test
    public void tocarNoJogoQueJaRoda_retomaSemMenu() {
        assertEquals(ResumeDecision.AppTap.RESUME,
                ResumeDecision.forApp(ONLINE, ONLINE));
    }

    @Test
    public void tocarEmOutroJogoComSessaoAberta_abreOMenu() {
        // Aqui há decisão de verdade — a única saída é derrubar o que está rodando —, e
        // isso se lê antes de tocar.
        assertEquals(ResumeDecision.AppTap.MENU,
                ResumeDecision.forApp(ONLINE, OUTRO));
    }

    @Test
    public void semNadaRodando_oToqueInicia() {
        assertEquals(ResumeDecision.AppTap.START,
                ResumeDecision.forApp(0, OUTRO));
    }

    @Test
    public void semNadaRodando_appDeIdZeroNaoEhConfundidoComSessao() {
        // getAppId() pode ser 0 num item mal formado vindo do host. Com nada rodando isso
        // não pode virar "retomar" — seria pedir resume de uma sessão inexistente.
        assertEquals(ResumeDecision.AppTap.START,
                ResumeDecision.forApp(0, 0));
    }
}
