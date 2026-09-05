package com.limelight.ui.compose

import android.content.Context
import android.content.SharedPreferences

/**
 * O que ele abriu por último, para a home poder oferecer de volta em um toque.
 *
 * É o dado que faltava para o P10. A seção "Continuar" estava DESENHADA em três arquivos de
 * layout e nenhuma linha de Java procurava os ids — porque não havia o que pôr nela.
 *
 * **Fato, não palpite.** O bloco vizinho, `vidroAnticipation` ("PROVÁVEL AGORA · você faz
 * isso quase toda noite a esta hora"), foi apagado junto: com um host só ele diria
 * exatamente a mesma frase que a primeira linha daqui, mas vestida de adivinhação — e
 * palpite que acerta sempre é só um atalho com fantasia. O que esta classe guarda é o que
 * aconteceu.
 */
object Historico {

    private const val ARQUIVO = "vidro_historico"
    private const val CHAVE = "ultimos"
    private val SEPARADOR = "\u001F"        // unit separator
    private val SEPARADOR_LINHA = "\u001E"  // record separator

    /** Quantas linhas o "Continuar" mostra. Três é o que cabe na dobra sem virar lista. */
    const val QUANTAS = 3

    /** Um lançamento que aconteceu. */
    data class Entrada(
        val hostUuid: String,
        val appId: Int,
        val appNome: String,
        val hostNome: String,
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    /**
     * Registra um lançamento.
     *
     * Chamado de `ServerHelper.doStart`, que é o funil único: catálogo, retomar, atalho do
     * lançador e o próprio "Continuar" passam todos por lá. Um funil só é o que impede o
     * histórico de discordar do que de fato abriu.
     */
    @JvmStatic
    fun registrar(ctx: Context, hostUuid: String?, hostNome: String?, appId: Int, appNome: String?) {
        if (hostUuid.isNullOrEmpty() || appId == 0) {
            return
        }

        val nova = Entrada(hostUuid, appId, appNome ?: "", hostNome ?: "")
        // A entrada nova vai para o topo e a repetição sai de baixo: abrir o mesmo jogo
        // cinco vezes não pode encher as três linhas com ele.
        val lista = (listOf(nova) + ler(ctx).filterNot { it.hostUuid == hostUuid && it.appId == appId })
            .take(QUANTAS)

        prefs(ctx).edit().putString(CHAVE, serializar(lista)).apply()
    }

    @JvmStatic
    fun ler(ctx: Context): List<Entrada> {
        val cru = prefs(ctx).getString(CHAVE, null) ?: return emptyList()
        return cru.split(SEPARADOR_LINHA)
            .mapNotNull { linha ->
                val p = linha.split(SEPARADOR)
                if (p.size != 4) null
                else p[1].toIntOrNull()?.let { Entrada(p[0], it, p[2], p[3]) }
            }
            .take(QUANTAS)
    }

    /** Some com o host quando ele é apagado — senão o "Continuar" oferece um lugar que não existe. */
    @JvmStatic
    fun esquecerHost(ctx: Context, hostUuid: String) {
        val restante = ler(ctx).filterNot { it.hostUuid == hostUuid }
        prefs(ctx).edit().putString(CHAVE, serializar(restante)).apply()
    }

    private fun serializar(lista: List<Entrada>) = lista.joinToString(SEPARADOR_LINHA) {
        // Nome de app e de host vêm do host e podem conter qualquer coisa. Os separadores
        // são caracteres de controle justamente por isso: não há nome que os contenha.
        listOf(it.hostUuid, it.appId.toString(), it.appNome, it.hostNome).joinToString(SEPARADOR)
    }
}
