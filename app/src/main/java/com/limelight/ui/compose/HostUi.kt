package com.limelight.ui.compose

import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.NvHTTP
import com.limelight.nvstream.http.PairingManager
import com.limelight.utils.ResumeDecision

/**
 * O que o cartão de host mostra, e nada além disso.
 *
 * Este é o ponto do piloto do Compose. `PcCellSnapshot` existia porque a sondagem entrega
 * SEMPRE a mesma instância de `ComputerDetails` mutada no lugar: comparar referência nunca
 * acusava mudança, então a home se redesenhava a cada 1,5 s por host, o painel nunca baixava
 * a taxa de atualização, e foi preciso copiar campo a campo à mão para descobrir se algo
 * havia mudado de verdade (Q36).
 *
 * `HostUi` é uma `data class` imutável. A comparação que o snapshot fazia à mão é a igualdade
 * estrutural que o Kotlin gera, e quem a usa é a recomposição — de graça, sem HashMap
 * paralelo e sem ninguém precisar lembrar de atualizá-lo. Trocar de stack aqui não
 * acrescentou uma capacidade; **removeu uma obrigação**.
 */
data class HostUi(
    val uuid: String,
    val nome: String,
    val estado: EstadoHost,
    val pareado: Boolean,
    val emSessao: Boolean,
    /** Só o cartão largo mostra. `null` quando não há nenhum endereço guardado. */
    val endereco: String?,
    /**
     * O P06: o pacote de Wake-on-LAN já saiu e estamos esperando.
     *
     * Antes o desfecho do "acordar" era uma torrada que sumia em dois segundos, e depois
     * dela restava encarar o cartão por ~20 s sem saber se pegou. Agora a espera tem lugar
     * e prazo, e ela some sozinha quando o host responde.
     */
    val acordando: Boolean = false,
)

/**
 * O que a linha diz sobre o host.
 *
 * Cinco estados, não três: "dormindo" e "fora do ar" são coisas diferentes para quem está
 * olhando — um pode ser acordado daqui, o outro não —, e "checando" e "sem resposta" também,
 * porque um tem prazo e o outro já esgotou o dele.
 */
enum class EstadoHost {
    /** Perguntamos e ainda estamos dentro do prazo. Gira, e não afirma nada. */
    CHECANDO,
    PRONTO,
    /** Perguntamos, o prazo acabou e ninguém respondeu. */
    SEM_RESPOSTA,
    /** Fora do ar, mas com MAC guardado: dá para acordar daqui. */
    DORMINDO,
    FORA_DO_AR,
}

/** Quanto tempo a linha gira antes de admitir que ninguém respondeu. */
const val PRAZO_DE_SONDA_MS = 10_000L

/**
 * Traduz o que a rede respondeu para o que a tela mostra.
 *
 * Função pura de propósito: recebe o tempo em vez de perguntar ao relógio, e por isso o
 * prazo de sonda pode ser testado sem esperar dez segundos. `sondandoDesdeMs` é `null`
 * quando o host nunca esteve em UNKNOWN.
 */
fun paraUi(
    details: ComputerDetails,
    agoraMs: Long,
    sondandoDesdeMs: Long?,
    acordando: Boolean = false,
): HostUi {
    val online = details.state == ComputerDetails.State.ONLINE
    val pareado = details.pairState == PairingManager.PairState.PAIRED

    val estado = when {
        online -> EstadoHost.PRONTO
        details.state == ComputerDetails.State.UNKNOWN -> {
            val dentroDoPrazo = sondandoDesdeMs != null &&
                    agoraMs - sondandoDesdeMs < PRAZO_DE_SONDA_MS
            if (dentroDoPrazo) EstadoHost.CHECANDO else EstadoHost.SEM_RESPOSTA
        }
        // O que separa dormindo de fora do ar é ter MAC guardado, que é do que o
        // Wake-on-LAN precisa. Sem ele, oferecer "acordar" seria um botão que não faz nada.
        !details.macAddress.isNullOrEmpty() -> EstadoHost.DORMINDO
        else -> EstadoHost.FORA_DO_AR
    }

    return HostUi(
        uuid = details.uuid ?: "",
        nome = details.name ?: "",
        estado = estado,
        pareado = pareado,
        // Mesma função que decide para onde o toque leva. O que a linha diz e o que o toque
        // faz não podem discordar — antes discordavam, e o cartão anunciava uma sessão que
        // o toque não abria.
        emSessao = ResumeDecision.hasOpenSession(online, pareado, details.runningGameId),
        endereco = enderecoVisivel(details),
        // Assim que ele responde acordado, a espera deixou de existir mesmo que ninguém
        // tenha limpado a marca.
        acordando = acordando && !online,
    )
}

/**
 * O endereço que o cartão largo mostra, ou `null` quando não há nenhum guardado.
 *
 * `activeAddress` é transiente e só existe depois de alguém responder, então host dormindo
 * cairia num traço — justamente quando saber onde ele fica é mais útil. Vale então o que
 * está persistido, na ordem em que a pessoa pensa no host: o que ela digitou, a casa, e por
 * fim o de fora.
 *
 * A porta padrão não é escrita: repeti-la em todo cartão não distingue nada e ainda estoura
 * a linha. Porta trocada, ao contrário, é exatamente o que precisa aparecer.
 */
private fun enderecoVisivel(details: ComputerDetails): String? {
    val tupla = details.activeAddress
        ?: details.manualAddress
        ?: details.localAddress
        ?: details.remoteAddress
        ?: return null

    return if (tupla.port == NvHTTP.DEFAULT_HTTP_PORT) tupla.address else tupla.toString()
}
