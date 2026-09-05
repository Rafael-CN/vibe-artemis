package com.limelight.ui.compose

import androidx.compose.ui.platform.ComposeView

/** A porta pela qual o Java entra na tela de pareamento. */
object PonteDoPareamento {

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        passo: PassoDoPareamento,
        nomeDoHost: String,
        codigo: String,
        paginaDoHost: String?,
        aoCancelar: () -> Unit,
        aoPedirNovo: () -> Unit,
        aoAbrirPagina: () -> Unit,
        aoVerApps: () -> Unit,
    ) {
        view.setContent {
            TelaPareamento(
                passo = passo,
                nomeDoHost = nomeDoHost,
                codigo = codigo,
                paginaDoHost = paginaDoHost,
                aoCancelar = aoCancelar,
                aoPedirNovo = aoPedirNovo,
                aoAbrirPagina = aoAbrirPagina,
                aoVerApps = aoVerApps,
            )
        }
    }

    @JvmStatic
    fun passoProcurando() = PassoDoPareamento.PROCURANDO

    @JvmStatic
    fun passoCodigo() = PassoDoPareamento.CODIGO

    @JvmStatic
    fun passoRecusado() = PassoDoPareamento.RECUSADO

    @JvmStatic
    fun passoPronto() = PassoDoPareamento.PRONTO
}
