package com.limelight.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.limelight.R
import com.limelight.ui.VidroLightView
import java.util.Calendar

/**
 * O que o cabeçalho da home diz, derivado dos hosts.
 *
 * Antes isto vivia em `PcView.updateVidroHeader`, junto de um registro do que já estava
 * escrito em cada TextView (`saudacaoEscrita`, `tituloEscrito`) — porque `setText` pede
 * layout mesmo com texto idêntico, e a sondagem chamava o método a cada 1,5 s por host.
 *
 * Esse registro era a mesma classe de trabalho que o `PcCellSnapshot` fazia na lista: cuidar
 * à mão de não redesenhar o que não mudou. Aqui ele some pelo mesmo motivo — o resumo é
 * derivado, e derivar de novo o mesmo valor não escreve em View nenhuma.
 *
 * Função pura, com a hora recebida em vez de perguntada, para poder ser testada.
 */
data class ResumoDosHosts(
    val saudacao: Int,
    val acordados: Int,
    val nomeDoAcordado: String?,
    val algumDormindo: Boolean,
    val aindaProcurando: Boolean,
    val intensidade: Float,
    /** Cor do núcleo da luz, de `R.color`. Ver VidroLightView.setTemperatura. */
    val temperatura: Int,
) {
    /**
     * A única frase da home.
     *
     * Com um lugar aceso ela nomeia esse lugar: o singular indefinido dizia "o PC" mesmo
     * quando a lista logo abaixo mostrava dois.
     */
    @Composable
    fun titulo(): String = when {
        acordados == 1 && !nomeDoAcordado.isNullOrEmpty() ->
            stringResource(R.string.vidro_host_awake_one, nomeDoAcordado)
        acordados > 0 ->
            pluralStringResource(R.plurals.vidro_host_awake_many, acordados, acordados)
        aindaProcurando -> stringResource(R.string.vidro_host_searching)
        algumDormindo -> stringResource(R.string.vidro_host_asleep)
        else -> stringResource(R.string.vidro_host_none)
    }
}

fun resumoDosHosts(hosts: List<HostUi>, hora: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): ResumoDosHosts {
    var acordados = 0
    var nome: String? = null
    var dormindo = false
    var procurando = false

    for (h in hosts) {
        when (h.estado) {
            EstadoHost.PRONTO -> {
                acordados++
                nome = h.nome
            }
            // Só conta como "ainda procurando" quem está dentro do prazo. Depois que o prazo
            // vence o host virou SEM_RESPOSTA, e dizer que a busca continua seria a mesma
            // espera sem fim que o prazo veio acabar.
            EstadoHost.CHECANDO -> procurando = true
            EstadoHost.DORMINDO -> dormindo = true
            else -> Unit
        }
    }

    val intensidade = when {
        acordados > 0 -> VidroLightView.INTENSITY_AWAKE
        procurando || dormindo -> VidroLightView.INTENSITY_ASLEEP
        else -> VidroLightView.INTENSITY_NONE
    }

    // A LUZ TEM A TEMPERATURA DO QUE ESTÁ LÁ FORA.
    //
    // Mesmo eixo da lista, mesma leitura: quente quando há máquina acordada, frio quando só
    // há máquina dormindo, sem croma quando não há nenhuma. Ela é a primeira coisa que se vê
    // ao abrir o app, e agora ela já respondeu à pergunta antes de qualquer palavra.
    val temperatura = when {
        acordados > 0 -> R.color.vidro_quente_halo
        dormindo -> R.color.vidro_frio_halo
        else -> R.color.vidro_ausente
    }

    return ResumoDosHosts(
        saudacao = saudacaoPara(hora),
        acordados = acordados,
        nomeDoAcordado = nome,
        algumDormindo = dormindo,
        aindaProcurando = procurando,
        intensidade = intensidade,
        temperatura = temperatura,
    )
}

/** Manhã até as 12, tarde até as 18, noite depois. */
fun saudacaoPara(hora: Int): Int = when {
    hora < 12 -> R.string.vidro_greeting_morning
    hora < 18 -> R.string.vidro_greeting_afternoon
    else -> R.string.vidro_greeting_evening
}
