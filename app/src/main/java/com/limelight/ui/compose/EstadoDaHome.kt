package com.limelight.ui.compose

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.limelight.nvstream.http.ComputerDetails

/**
 * O estado da home, separado da Activity.
 *
 * É a camada que faltava — o app não tinha nenhuma, e as Activities ERAM a lógica. Aqui ela
 * aparece no menor tamanho que resolve a home: uma lista imutável, um mapa de relógios de
 * sonda, e três operações. `PcView` continua dono do serviço, do pareamento e da navegação;
 * o que saiu dela foi só a decisão de o que a lista mostra.
 *
 * Vale mesmo se o Compose for abandonado depois: `hosts` é uma lista de `data class` que
 * qualquer adapter também saberia consumir.
 */
class EstadoDaHome {

    /** A lista que a tela desenha. Trocar a lista é o que dispara o redesenho. */
    var hosts by mutableStateOf<List<HostUi>>(emptyList())
        private set

    /**
     * Quando cada host entrou em UNKNOWN.
     *
     * Um host que não resolve nunca sai de UNKNOWN — a sondagem só avisa a interface depois
     * de esgotar as tentativas — e sem prazo a linha giraria para sempre. Foi o que aconteceu
     * com um host remoto do dono do fork. O relógio é zerado quando o estado sai de UNKNOWN,
     * então a linha se corrige sozinha assim que alguém responde.
     */
    private val sondandoDesde = HashMap<String, Long>()

    /** Onde o `details` cru fica, para quem ainda precisa dele (pareamento, WoL, catálogo). */
    private val porUuid = HashMap<String, ComputerDetails>()

    /** Hosts para os quais o pacote de Wake-on-LAN já saiu. Ver HostUi.acordando. */
    private val acordando = HashSet<String>()

    /** O host cuja folha de ações está aberta, ou `null`. */
    var folhaDoHost by mutableStateOf<HostUi?>(null)
        private set

    fun abrirFolha(host: HostUi) { folhaDoHost = host }
    fun fecharFolha() { folhaDoHost = null }

    /** O pacote saiu: a linha passa a contar a espera em vez de repetir "dormindo". */
    fun marcarAcordando(uuid: String) {
        if (acordando.add(uuid)) {
            reavaliarPrazos()
        }
    }

    fun detalhes(uuid: String): ComputerDetails? = porUuid[uuid]

    /**
     * Uma resposta da sondagem.
     *
     * Não há `if (mudou)` aqui, e essa ausência é o ganho: se o `HostUi` resultante for igual
     * ao anterior, a lista nova é estruturalmente igual à antiga e a recomposição não toca
     * em nada. A comparação que `PcCellSnapshot` fazia à mão acontece sozinha.
     */
    @JvmOverloads
    fun aoResponder(details: ComputerDetails, agoraMs: Long = SystemClock.uptimeMillis()) {
        val uuid = details.uuid ?: return
        porUuid[uuid] = details

        if (details.state == ComputerDetails.State.UNKNOWN) {
            sondandoDesde.putIfAbsentCompat(uuid, agoraMs)
        } else {
            sondandoDesde.remove(uuid)
        }

        if (details.state == ComputerDetails.State.ONLINE) {
            acordando.remove(uuid)
        }

        val novo = paraUi(details, agoraMs, sondandoDesde[uuid], acordando.contains(uuid))
        val atual = hosts
        val i = atual.indexOfFirst { it.uuid == uuid }

        hosts = if (i >= 0) {
            if (atual[i] == novo) atual else atual.toMutableList().also { it[i] = novo }
        } else {
            (atual + novo).sortedBy { it.nome.lowercase() }
        }
    }

    fun remover(uuid: String) {
        porUuid.remove(uuid)
        acordando.remove(uuid)
        if (folhaDoHost?.uuid == uuid) {
            folhaDoHost = null
        }
        sondandoDesde.remove(uuid)
        hosts = hosts.filterNot { it.uuid == uuid }
    }

    /**
     * Reavalia os prazos de sonda sem que ninguém tenha respondido.
     *
     * O caso que estamos consertando é justamente o de nenhum evento chegar. Antes isto era
     * um `postDelayed` dentro do adapter chamando `notifyDataSetChanged()` — desenho pedindo
     * redesenho a si mesmo. Agora quem marca o tempo é a tela, e isto só recalcula.
     */
    @JvmOverloads
    fun reavaliarPrazos(agoraMs: Long = SystemClock.uptimeMillis()) {
        val novo = hosts.map { ui ->
            val d = porUuid[ui.uuid] ?: return@map ui
            paraUi(d, agoraMs, sondandoDesde[ui.uuid], acordando.contains(ui.uuid))
        }
        if (novo != hosts) {
            hosts = novo
        }
    }

    /** Há alguma linha girando? A tela só precisa acordar enquanto houver. */
    fun temSondaEmCurso(): Boolean = hosts.any { it.estado == EstadoHost.CHECANDO }

    // putIfAbsent é API 24. O minSdk é 21.
    private fun HashMap<String, Long>.putIfAbsentCompat(chave: String, valor: Long) {
        if (!containsKey(chave)) put(chave, valor)
    }
}
