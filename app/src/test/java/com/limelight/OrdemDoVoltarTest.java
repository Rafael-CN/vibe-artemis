package com.limelight;

import static com.limelight.Game.Voltar.ABRIR_CONTROLES;
import static com.limelight.Game.Voltar.CASCA;
import static com.limelight.Game.Voltar.CONVOCAR_CASCA;
import static com.limelight.Game.Voltar.FECHAR_GAVETA;
import static com.limelight.Game.Voltar.SISTEMA;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A ordem do Voltar dentro da sessão.
 *
 * Existe por um defeito real, não por completude: quando o menu legado foi absorvido pela
 * gaveta, `showGameMenu` passou a ABRIR a gaveta — e o ramo do `enableBackMenu`, que vem
 * antes de qualquer verificação de gaveta aberta, passou a reabri-la em cima de si mesma.
 * Dois Voltares no emulador e o painel continuava idêntico.
 *
 * O que estes testes travam é a REGRA, não os ramos: quem está na frente consome primeiro.
 */
public class OrdemDoVoltarTest {

    @Test
    public void cascaNaFrenteDecideSozinha() {
        // Mesmo com tudo mais ligado: a casca tem pilha própria e o Voltar é dela.
        assertEquals(CASCA, Game.decideVoltar(true, true, true, true));
    }

    @Test
    public void gavetaAbertaFechaAntesDeQualquerCoisaAbrir() {
        // ESTE é o defeito. Com menuNoVoltar ligado, a ordem errada devolveria
        // ABRIR_CONTROLES e a gaveta se reabriria.
        assertEquals(FECHAR_GAVETA, Game.decideVoltar(false, true, true, true));
    }

    @Test
    public void gavetaFechadaComAtalhoLigadoAbreOsControles() {
        assertEquals(ABRIR_CONTROLES, Game.decideVoltar(false, false, true, true));
    }

    @Test
    public void semAtalhoOVoltarConvocaACascaEmVezDeEncerrar() {
        // A mudança semântica central da ideia 1: sair da sessão deixou de matá-la.
        assertEquals(CONVOCAR_CASCA, Game.decideVoltar(false, false, false, true));
    }

    @Test
    public void semVideoNenhumOSistemaDecide() {
        assertEquals(SISTEMA, Game.decideVoltar(false, false, false, false));
    }

    @Test
    public void aGavetaVenceOAtalhoMesmoSemVideo() {
        // A prioridade da gaveta não depende de ter havido vídeo: ela é uma superfície
        // aberta na frente, e isso basta.
        assertEquals(FECHAR_GAVETA, Game.decideVoltar(false, true, true, false));
    }
}
