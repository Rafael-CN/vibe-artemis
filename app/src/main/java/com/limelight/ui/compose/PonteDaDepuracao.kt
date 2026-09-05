package com.limelight.ui.compose

import androidx.compose.ui.platform.ComposeView

/** A porta pela qual o Java entra na tela de depuracao. */
object PonteDaDepuracao {

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        resumo: String,
        dispositivos: String,
        aoVoltar: () -> Unit,
        aoCopiar: () -> Unit,
    ) {
        view.setContent {
            TelaDepuracao(
                resumo = resumo,
                dispositivos = dispositivos,
                aoVoltar = aoVoltar,
                aoCopiar = aoCopiar,
            )
        }
    }
}
