package com.limelight.nvstream.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * O menu da sessão oferecia sincronização de área de transferência para um host que não
 * tem o endpoint, e só descobria isso ao falhar. Estes testes prendem a regra que passou a
 * decidir se a ação aparece — e, principalmente, prendem a armadilha que faria a regra
 * dizer exatamente o contrário do certo.
 */
public class HostCapabilitiesTest {

    /** Máscara de um Apollo com as duas direções liberadas, mais entradas e ações. */
    private static final int APOLLO_COMPLETO =
            HostCapabilities.PERM_INPUT_CONTROLLER
                    | HostCapabilities.PERM_INPUT_TOUCH
                    | HostCapabilities.PERM_INPUT_PEN
                    | HostCapabilities.PERM_INPUT_MOUSE
                    | HostCapabilities.PERM_INPUT_KEYBOARD
                    | HostCapabilities.PERM_CLIPBOARD_SET
                    | HostCapabilities.PERM_CLIPBOARD_READ
                    | HostCapabilities.PERM_SERVER_CMD
                    | HostCapabilities.PERM_LIST_APPS
                    | HostCapabilities.PERM_VIEW_STREAMS
                    | HostCapabilities.PERM_LAUNCH_APPS;

    @Test
    public void hostSemModeloDePermissoes_naoHabilitaNada() {
        // ESTA é a asserção que justifica a classe existir. PERMISSION_UNKNOWN é -1, que
        // em complemento de dois tem TODOS os bits ligados: quem testar o bit direto na
        // máscara recebe "sim" para todo host que não implementa permissões — Sunshine,
        // Vibeshine e GFE, ou seja, justamente os hosts em que a ação não funciona.
        HostCapabilities caps = HostCapabilities.fromPermissionMask(
                HostCapabilities.PERMISSION_UNKNOWN);

        assertFalse("host sem modelo de permissões não tem modelo de permissões",
                caps.hasPermissionModel());
        assertFalse("um -1 cru responderia 'sim' aqui", caps.supportsClipboardSend());
        assertFalse("um -1 cru responderia 'sim' aqui", caps.supportsClipboardFetch());
    }

    @Test
    public void qualquerMascaraNegativa_viraDesconhecida() {
        // O campo Permission vem do host como texto. Se um dia vier algo que o parse
        // interpreta como negativo, o resultado tem que ser "não sei", nunca "pode tudo".
        for (int mascara : new int[] {-1, -2, Integer.MIN_VALUE}) {
            HostCapabilities caps = HostCapabilities.fromPermissionMask(mascara);
            assertSame("máscara negativa devia colapsar em desconhecida",
                    HostCapabilities.unknown(), caps);
            assertEquals(HostCapabilities.PERMISSION_UNKNOWN, caps.getPermissionMask());
        }
    }

    @Test
    public void hostComPermissoesEsemClipboard_naoOferece() {
        // Apollo é reconhecido — mandou o campo — mas este cliente não recebeu as duas
        // permissões de área de transferência. Silêncio sobre um bit é "não".
        HostCapabilities caps = HostCapabilities.fromPermissionMask(
                HostCapabilities.PERM_INPUT_KEYBOARD | HostCapabilities.PERM_LAUNCH_APPS);

        assertTrue(caps.hasPermissionModel());
        assertFalse(caps.supportsClipboardSend());
        assertFalse(caps.supportsClipboardFetch());
    }

    @Test
    public void permissaoZerada_eDiferenteDeDesconhecida() {
        // "Nenhuma permissão" (_no = 0) é uma resposta do host, não a ausência dela. As
        // duas escondem as mesmas ações, mas só uma delas significa que o host respondeu.
        HostCapabilities caps = HostCapabilities.fromPermissionMask(0);

        assertTrue("zero é resposta, não ausência", caps.hasPermissionModel());
        assertFalse(caps.supportsClipboardSend());
        assertFalse(caps.supportsClipboardFetch());
    }

    @Test
    public void asDuasDirecoesSaoIndependentes() {
        // O menu tem uma linha por direção, e o host libera uma sem a outra. Se as duas
        // consultas se contaminassem, uma das linhas voltaria a aparecer sem funcionar.
        HostCapabilities soEnvia = HostCapabilities.fromPermissionMask(
                HostCapabilities.PERM_CLIPBOARD_SET);
        assertTrue(soEnvia.supportsClipboardSend());
        assertFalse(soEnvia.supportsClipboardFetch());

        HostCapabilities soBusca = HostCapabilities.fromPermissionMask(
                HostCapabilities.PERM_CLIPBOARD_READ);
        assertFalse(soBusca.supportsClipboardSend());
        assertTrue(soBusca.supportsClipboardFetch());
    }

    @Test
    public void apolloCompleto_liberaAsDuas() {
        HostCapabilities caps = HostCapabilities.fromPermissionMask(APOLLO_COMPLETO);

        assertTrue(caps.hasPermissionModel());
        assertTrue(caps.supportsClipboardSend());
        assertTrue(caps.supportsClipboardFetch());
    }

    @Test
    public void osBitsSaoOsDoEnumDoApollo() {
        // Os valores viraram deslocamentos legíveis em vez de hexadecimais soltos, e um
        // deslocamento errado não quebra compilação nenhuma — quebraria só no host, em
        // silêncio. Os números ficam presos aqui, com os valores do enum PERM.
        assertEquals(0x00000100, HostCapabilities.PERM_INPUT_CONTROLLER);
        assertEquals(0x00000200, HostCapabilities.PERM_INPUT_TOUCH);
        assertEquals(0x00000400, HostCapabilities.PERM_INPUT_PEN);
        assertEquals(0x00000800, HostCapabilities.PERM_INPUT_MOUSE);
        assertEquals(0x00001000, HostCapabilities.PERM_INPUT_KEYBOARD);

        assertEquals(0x00010000, HostCapabilities.PERM_CLIPBOARD_SET);
        assertEquals(0x00020000, HostCapabilities.PERM_CLIPBOARD_READ);
        assertEquals(0x00040000, HostCapabilities.PERM_FILE_UPLOAD);
        assertEquals(0x00080000, HostCapabilities.PERM_FILE_DOWNLOAD);
        assertEquals(0x00100000, HostCapabilities.PERM_SERVER_CMD);

        assertEquals(0x01000000, HostCapabilities.PERM_LIST_APPS);
        assertEquals(0x02000000, HostCapabilities.PERM_VIEW_STREAMS);
        assertEquals(0x04000000, HostCapabilities.PERM_LAUNCH_APPS);
    }

    @Test
    public void aMascaraAtravessaIntacta() {
        // O Intent da sessão carrega o inteiro, não o objeto: o que sai de um lado tem que
        // ser o que entra no outro, senão a sessão decide por uma capacidade diferente da
        // que o host anunciou.
        HostCapabilities origem = HostCapabilities.fromPermissionMask(APOLLO_COMPLETO);
        HostCapabilities destino = HostCapabilities.fromPermissionMask(origem.getPermissionMask());

        assertEquals(APOLLO_COMPLETO, destino.getPermissionMask());
        assertEquals(origem.supportsClipboardSend(), destino.supportsClipboardSend());
        assertEquals(origem.supportsClipboardFetch(), destino.supportsClipboardFetch());
    }

    @Test
    public void hostRecemDescoberto_naoOferece() {
        // ComputerDetails nasce com permission = -1 e só é preenchido pela sondagem. Entre
        // achar o PC e falar com ele, a resposta certa é "não sei".
        ComputerDetails aindaNaoSondado = new ComputerDetails();

        assertFalse(aindaNaoSondado.getCapabilities().hasPermissionModel());
        assertFalse(aindaNaoSondado.getCapabilities().supportsClipboardSend());
    }

    @Test
    public void capacidadeSaiDoComputerDetails() {
        ComputerDetails details = new ComputerDetails();
        details.permission = APOLLO_COMPLETO;

        assertTrue(details.getCapabilities().supportsClipboardFetch());
        assertEquals(APOLLO_COMPLETO, details.getCapabilities().getPermissionMask());
    }
}
