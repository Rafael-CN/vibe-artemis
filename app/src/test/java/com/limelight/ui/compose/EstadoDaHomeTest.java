package com.limelight.ui.compose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;

import org.junit.Test;

import java.util.List;

/**
 * O contrato que substitui o de PcCellSnapshot.
 *
 * Aquela classe existia para responder "mudou alguma coisa que a tela mostra?", porque a
 * sondagem entrega SEMPRE a mesma instância de ComputerDetails mutada no lugar e comparar
 * referência nunca acusava nada. A pergunta continua valendo — o que mudou foi quem
 * responde: agora é a igualdade estrutural de um HostUi imutável.
 *
 * Estes testes prendem exatamente isso, e mais o prazo de sonda, que antes vivia dentro do
 * adapter com um postDelayed chamando notifyDataSetChanged().
 */
public class EstadoDaHomeTest {

    private static ComputerDetails host(String uuid, String nome, ComputerDetails.State estado) {
        ComputerDetails d = new ComputerDetails();
        d.uuid = uuid;
        d.name = nome;
        d.state = estado;
        d.pairState = PairingManager.PairState.PAIRED;
        return d;
    }

    @Test
    public void respostaRepetidaNaoTrocaALista() {
        // O caso do Q36: a mesma instância volta a cada 1,5 s, mutada no lugar, com o mesmo
        // conteúdo. Se a lista mudasse de identidade aqui, a home se redesenharia em ciclo.
        EstadoDaHome estado = new EstadoDaHome();
        ComputerDetails d = host("a", "PC-RAFAEL", ComputerDetails.State.ONLINE);

        estado.aoResponder(d, 0L);
        List<HostUi> primeira = estado.getHosts();

        estado.aoResponder(d, 1500L);

        assertSame("resposta idêntica não pode trocar a lista", primeira, estado.getHosts());
    }

    @Test
    public void mudancaDeVerdadeTrocaALista() {
        EstadoDaHome estado = new EstadoDaHome();
        ComputerDetails d = host("a", "PC-RAFAEL", ComputerDetails.State.ONLINE);

        estado.aoResponder(d, 0L);
        List<HostUi> primeira = estado.getHosts();

        // A MESMA instância, mutada — que é exatamente o que a sondagem faz.
        d.state = ComputerDetails.State.OFFLINE;
        estado.aoResponder(d, 1500L);

        assertNotSame(primeira, estado.getHosts());
        assertEquals(EstadoHost.FORA_DO_AR, estado.getHosts().get(0).getEstado());
    }

    @Test
    public void semRespostaSoDepoisDoPrazo() {
        EstadoDaHome estado = new EstadoDaHome();
        ComputerDetails d = host("a", "REMOTO", ComputerDetails.State.UNKNOWN);

        estado.aoResponder(d, 0L);
        assertEquals("dentro do prazo a linha gira e não afirma nada",
                EstadoHost.CHECANDO, estado.getHosts().get(0).getEstado());
        assertTrue(estado.temSondaEmCurso());

        estado.reavaliarPrazos(PRAZO_DE_SONDA_MS + 1);
        assertEquals("passado o prazo ela precisa admitir que ninguém respondeu",
                EstadoHost.SEM_RESPOSTA, estado.getHosts().get(0).getEstado());
        assertFalse(estado.temSondaEmCurso());
    }

    @Test
    public void hostQueRespondeZeraORelogioDaSonda() {
        EstadoDaHome estado = new EstadoDaHome();
        ComputerDetails d = host("a", "SALA", ComputerDetails.State.UNKNOWN);

        estado.aoResponder(d, 0L);
        d.state = ComputerDetails.State.ONLINE;
        estado.aoResponder(d, 5000L);
        d.state = ComputerDetails.State.UNKNOWN;
        estado.aoResponder(d, 6000L);

        // O relógio recomeça em 6000, então em 12000 ainda faltam 4 s para o prazo vencer.
        estado.reavaliarPrazos(12_000L);
        assertEquals(EstadoHost.CHECANDO, estado.getHosts().get(0).getEstado());
    }

    @Test
    public void dormindoExigeMacGuardado() {
        EstadoDaHome estado = new EstadoDaHome();

        ComputerDetails semMac = host("a", "SEM MAC", ComputerDetails.State.OFFLINE);
        ComputerDetails comMac = host("b", "COM MAC", ComputerDetails.State.OFFLINE);
        comMac.macAddress = "aa:bb:cc:dd:ee:ff";

        estado.aoResponder(semMac, 0L);
        estado.aoResponder(comMac, 0L);

        // Oferecer "acordar" sem MAC é um botão que não faz nada.
        assertEquals(EstadoHost.FORA_DO_AR, porNome(estado, "SEM MAC").getEstado());
        assertEquals(EstadoHost.DORMINDO, porNome(estado, "COM MAC").getEstado());
    }

    @Test
    public void aListaFicaEmOrdemDeNome() {
        EstadoDaHome estado = new EstadoDaHome();
        estado.aoResponder(host("a", "zebra", ComputerDetails.State.ONLINE), 0L);
        estado.aoResponder(host("b", "Alfa", ComputerDetails.State.ONLINE), 0L);

        assertEquals("Alfa", estado.getHosts().get(0).getNome());
        assertEquals("zebra", estado.getHosts().get(1).getNome());
    }

    @Test
    public void aPortaPadraoNaoEntraNoEndereco() {
        // Repetir ":47989" em todo cartão não distingue nada e ainda estoura a linha.
        // Porta trocada, ao contrário, é exatamente o que precisa aparecer.
        EstadoDaHome estado = new EstadoDaHome();
        ComputerDetails d = host("a", "PC", ComputerDetails.State.ONLINE);
        d.manualAddress = new ComputerDetails.AddressTuple("192.168.0.9", NvHTTP.DEFAULT_HTTP_PORT);
        estado.aoResponder(d, 0L);
        assertEquals("192.168.0.9", estado.getHosts().get(0).getEndereco());

        ComputerDetails outra = host("b", "PC2", ComputerDetails.State.ONLINE);
        outra.manualAddress = new ComputerDetails.AddressTuple("192.168.0.9", 47990);
        estado.aoResponder(outra, 0L);
        assertTrue(porNome(estado, "PC2").getEndereco().contains("47990"));
    }

    @Test
    public void removerTiraDaLista() {
        EstadoDaHome estado = new EstadoDaHome();
        estado.aoResponder(host("a", "PC", ComputerDetails.State.ONLINE), 0L);
        estado.remover("a");
        assertTrue(estado.getHosts().isEmpty());
    }

    private static HostUi porNome(EstadoDaHome estado, String nome) {
        for (HostUi h : estado.getHosts()) {
            if (nome.equals(h.getNome())) {
                return h;
            }
        }
        throw new AssertionError("não achei " + nome);
    }

    private static final long PRAZO_DE_SONDA_MS = 10_000L;
}
