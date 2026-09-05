package com.limelight.utils;

import java.util.Locale;

/**
 * De que lado da rede mora o endereço do host.
 *
 * A pergunta existe por causa de um teste que só sabe responder sobre a internet. O
 * moonlight-common-c abre conexões contra um servidor público para descobrir se o provedor
 * está bloqueando as portas do streaming — resposta útil quando o host está do outro lado
 * do mundo, e nenhuma quando ele está em 192.168.x.x, porque as portas que importam nesse
 * caso nunca chegam a passar pelo provedor.
 *
 * Decidir isso sem resolver DNS é de propósito. Este teste roda justamente no caminho de
 * falha, quando a rede já está ruim, e em cada uma das tentativas: uma resolução a mais é
 * exatamente o que não se pode pagar ali. Por isso só o texto do endereço é lido, e um
 * nome que não seja literal nem de enlace continua valendo o teste — é onde ele responde
 * algo.
 */
public final class NetworkScope {

    private NetworkScope() {
    }

    /**
     * O teste de portas contra o servidor na internet tem o que dizer sobre este host?
     */
    public static boolean shouldTestInternetPorts(String host) {
        return !isPrivateHost(host);
    }

    /**
     * O endereço só existe dentro da rede local (ou do próprio aparelho)?
     *
     * Endereço nulo ou vazio devolve false: sem saber, o certo é manter o teste.
     */
    public static boolean isPrivateHost(String host) {
        if (host == null) {
            return false;
        }

        String addr = host.trim();
        if (addr.isEmpty()) {
            return false;
        }

        // [fe80::1%wlan0] — os colchetes e o identificador de zona não fazem parte do
        // endereço, mas chegam colados nele em enlace local.
        if (addr.length() > 2 && addr.charAt(0) == '[' && addr.charAt(addr.length() - 1) == ']') {
            addr = addr.substring(1, addr.length() - 1);
        }
        int zone = addr.indexOf('%');
        if (zone >= 0) {
            addr = addr.substring(0, zone);
        }
        if (addr.isEmpty()) {
            return false;
        }

        String lower = addr.toLowerCase(Locale.US);

        if (lower.equals("localhost") || lower.equals("localhost.")) {
            return true;
        }

        // ".local" é mDNS e ".home.arpa" é o domínio reservado para rede doméstica: os
        // dois só resolvem dentro do enlace, então o provedor não tem o que responder.
        if (lower.endsWith(".local") || lower.endsWith(".local.")
                || lower.endsWith(".home.arpa") || lower.endsWith(".home.arpa.")) {
            return true;
        }

        if (lower.indexOf(':') >= 0) {
            return isPrivateIpv6(lower);
        }

        int[] octets = parseIpv4(lower);
        return octets != null && isPrivateIpv4(octets);
    }

    private static boolean isPrivateIpv4(int[] o) {
        if (o[0] == 10) {
            return true;
        }
        if (o[0] == 127) {
            return true;
        }
        if (o[0] == 172 && o[1] >= 16 && o[1] <= 31) {
            return true;
        }
        if (o[0] == 192 && o[1] == 168) {
            return true;
        }
        // 169.254.0.0/16 — enlace local, o endereço de quem ficou sem DHCP.
        if (o[0] == 169 && o[1] == 254) {
            return true;
        }
        // 100.64.0.0/10 — o CGNAT do provedor, e a faixa que o Tailscale usa. Também não
        // é roteável pela internet, então o teste de portas não diz nada sobre ela.
        return o[0] == 100 && o[1] >= 64 && o[1] <= 127;
    }

    private static boolean isPrivateIpv6(String addr) {
        if (addr.equals("::1")) {
            return true;
        }

        // ::ffff:192.168.0.10 — endereço v4 vestido de v6. Quem decide é o v4 de dentro.
        int lastColon = addr.lastIndexOf(':');
        String tail = addr.substring(lastColon + 1);
        if (tail.indexOf('.') >= 0) {
            int[] octets = parseIpv4(tail);
            return octets != null && isPrivateIpv4(octets);
        }

        int firstColon = addr.indexOf(':');
        String head = firstColon < 0 ? addr : addr.substring(0, firstColon);
        if (head.isEmpty() || head.length() > 4) {
            // Começa em "::" (só ::1 interessa, já tratado) ou não é um hexteto válido.
            return false;
        }
        for (int i = 0; i < head.length(); i++) {
            char c = head.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) {
                return false;
            }
        }

        int hextet = Integer.parseInt(head, 16);

        // fc00::/7 é o endereço único local (o "192.168" do IPv6) e fe80::/10 é o enlace.
        return (hextet & 0xfe00) == 0xfc00 || (hextet & 0xffc0) == 0xfe80;
    }

    /**
     * Devolve os quatro octetos, ou null se o texto não for um IPv4 literal.
     *
     * Nada de InetAddress aqui: getByName() resolveria o nome na rede, que é a conta que
     * este arquivo inteiro existe para não pagar.
     */
    private static int[] parseIpv4(String addr) {
        int[] octets = new int[4];
        int part = 0;
        int value = 0;
        int digits = 0;

        for (int i = 0; i < addr.length(); i++) {
            char c = addr.charAt(i);
            if (c == '.') {
                if (digits == 0 || part == 3) {
                    return null;
                }
                octets[part++] = value;
                value = 0;
                digits = 0;
            }
            else if (c >= '0' && c <= '9') {
                if (++digits > 3) {
                    return null;
                }
                value = value * 10 + (c - '0');
                if (value > 255) {
                    return null;
                }
            }
            else {
                return null;
            }
        }

        if (part != 3 || digits == 0) {
            return null;
        }
        octets[3] = value;
        return octets;
    }
}
