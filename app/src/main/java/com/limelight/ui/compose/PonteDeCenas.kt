package com.limelight.ui.compose

import androidx.compose.ui.platform.ComposeView

/** A porta pela qual o Java entra na tela de Cenas. Ver PonteDaHome para o porque. */
object PonteDeCenas {

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        cenas: List<CenaUi>,
        aoVoltar: () -> Unit,
        aoAbrir: (CenaUi) -> Unit,
        aoEditar: (CenaUi) -> Unit,
        aoApagar: (CenaUi) -> Unit,
        aoCriar: () -> Unit,
    ) {
        view.setContent {
            TelaCenas(
                cenas = cenas,
                aoVoltar = aoVoltar,
                aoAbrir = aoAbrir,
                aoEditar = aoEditar,
                aoApagar = aoApagar,
                aoCriar = aoCriar,
            )
        }
    }
}
