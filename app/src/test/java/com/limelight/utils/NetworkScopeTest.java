package com.limelight.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * O teste de portas do moonlight-common-c conversa com um servidor na internet, e ele
 * rodava em TODA falha de conexão — inclusive com o host em 192.168.x.x, e inclusive em
 * cada uma das tentativas internas. Resolução de DNS mais conexões TCP, repetidas,
 * justamente quando a rede está ruim, para produzir uma resposta que não vale para a rede
 * de casa.
 *
 * Estes testes prendem a regra que corta essa conta. O que está preso aqui é o critério de
 * "endereço que não sai da rede local", e as bordas dele são o que costuma escapar: o
 * 172.16/12 que não é o /16 inteiro, o CGNAT que o Tailscale usa, o IPv6 de enlace com
 * identificador de zona colado, e o nome que NÃO é local — porque pular o teste ali seria
 * apagar a única informação útil que a folha de falha tem no cenário remoto.
 */
public class NetworkScopeTest {

    @Test
    public void faixasPrivadasDeIpv4NaoSaemDaRede() {
        assertTrue(NetworkScope.isPrivateHost("192.168.1.20"));
        assertTrue(NetworkScope.isPrivateHost("10.0.0.1"));
        assertTrue(NetworkScope.isPrivateHost("10.255.255.254"));
        assertTrue(NetworkScope.isPrivateHost("127.0.0.1"));
        assertTrue(NetworkScope.isPrivateHost("169.254.13.7"));
    }

    @Test
    public void oBlocoDe172EhDozeBitsENaoDezesseis() {
        // 172.16.0.0/12 vai de 172.16 a 172.31. Tratar "172.x" inteiro como privado
        // engoliria endereços públicos de verdade e mataria o teste no cenário remoto.
        assertTrue(NetworkScope.isPrivateHost("172.16.0.1"));
        assertTrue(NetworkScope.isPrivateHost("172.31.255.255"));
        assertFalse(NetworkScope.isPrivateHost("172.15.0.1"));
        assertFalse(NetworkScope.isPrivateHost("172.32.0.1"));
    }

    @Test
    public void oCgnatEhAFaixaDoTailscaleETambemNaoPassaPeloProvedor() {
        assertTrue(NetworkScope.isPrivateHost("100.64.0.1"));
        assertTrue(NetworkScope.isPrivateHost("100.101.102.103"));
        assertTrue(NetworkScope.isPrivateHost("100.127.255.255"));
        assertFalse(NetworkScope.isPrivateHost("100.63.255.255"));
        assertFalse(NetworkScope.isPrivateHost("100.128.0.1"));
    }

    @Test
    public void enderecoPublicoDeIpv4MantemOTeste() {
        assertFalse(NetworkScope.isPrivateHost("8.8.8.8"));
        assertFalse(NetworkScope.isPrivateHost("203.0.113.9"));
        assertTrue(NetworkScope.shouldTestInternetPorts("203.0.113.9"));
    }

    @Test
    public void ipv6DeEnlaceEUniqueLocal() {
        assertTrue(NetworkScope.isPrivateHost("fe80::1"));
        assertTrue(NetworkScope.isPrivateHost("febf::1"));
        assertTrue(NetworkScope.isPrivateHost("fd12:3456:789a::1"));
        assertTrue(NetworkScope.isPrivateHost("fc00::1"));
        assertTrue(NetworkScope.isPrivateHost("::1"));

        // fec0::/10 é site-local depreciado e fe00::/9 é reservado: nenhum dos dois entra
        // na regra, e nenhum dos dois pode entrar por acidente de máscara.
        assertFalse(NetworkScope.isPrivateHost("fec0::1"));
        assertFalse(NetworkScope.isPrivateHost("fe00::1"));
        assertFalse(NetworkScope.isPrivateHost("2001:db8::1"));
    }

    @Test
    public void ipv6DeEnlaceChegaComColchetesEZona() {
        // É a forma em que o endereço aparece quando veio de descoberta em enlace local.
        assertTrue(NetworkScope.isPrivateHost("[fe80::1%wlan0]"));
        assertTrue(NetworkScope.isPrivateHost("fe80::1%wlan0"));
        assertTrue(NetworkScope.isPrivateHost("[fd00::5]"));
    }

    @Test
    public void ipv4VestidoDeIpv6SegueSendoOIpv4DeDentro() {
        assertTrue(NetworkScope.isPrivateHost("::ffff:192.168.1.20"));
        assertFalse(NetworkScope.isPrivateHost("::ffff:8.8.8.8"));
    }

    @Test
    public void nomesDeEnlaceSaoLocaisEOsDemaisNao() {
        assertTrue(NetworkScope.isPrivateHost("localhost"));
        assertTrue(NetworkScope.isPrivateHost("vibeshine.local"));
        assertTrue(NetworkScope.isPrivateHost("VIBESHINE.LOCAL."));
        assertTrue(NetworkScope.isPrivateHost("pc.home.arpa"));

        // Um nome comum pode apontar para qualquer lugar. Chutar "local" aqui apagaria o
        // aviso de porta bloqueada de quem realmente transmite pela internet.
        assertFalse(NetworkScope.isPrivateHost("meupc.duckdns.org"));
        assertFalse(NetworkScope.isPrivateHost("vibeshine"));
    }

    @Test
    public void semEnderecoOTesteContinua() {
        // Não saber não é motivo para pular: o teste só é dispensado quando dá para
        // afirmar que o endereço não sai da rede.
        assertFalse(NetworkScope.isPrivateHost(null));
        assertFalse(NetworkScope.isPrivateHost(""));
        assertFalse(NetworkScope.isPrivateHost("   "));
        assertTrue(NetworkScope.shouldTestInternetPorts(null));
    }

    @Test
    public void textoQueNaoEhEnderecoNaoViraEnderecoPrivado() {
        assertFalse(NetworkScope.isPrivateHost("192.168.1"));
        assertFalse(NetworkScope.isPrivateHost("192.168.1.20.5"));
        assertFalse(NetworkScope.isPrivateHost("192.168.1.256"));
        assertFalse(NetworkScope.isPrivateHost("10.0.0.-1"));
        assertFalse(NetworkScope.isPrivateHost("10.0..1"));
    }

    @Test
    public void espacoEmVoltaNaoMudaARespostaMasPreservaAExtremidade() {
        assertTrue(NetworkScope.isPrivateHost("  192.168.1.20  "));
        assertFalse(NetworkScope.isPrivateHost("  8.8.8.8 "));
    }
}
