package com.limelight.ui.compose

import androidx.compose.ui.platform.ComposeView

/** A porta pela qual o Java entra na tela do endereco. Ver PonteDaHome para o porque. */
object PonteDoEndereco {

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        texto: String,
        erro: Int,
        ocupado: Boolean,
        tentados: List<String>,
        aoDigitar: (String) -> Unit,
        aoVoltar: () -> Unit,
        aoConectar: () -> Unit,
        aoEscolherTentado: (String) -> Unit,
    ) {
        view.setContent {
            TelaEndereco(
                texto = texto,
                aoDigitar = aoDigitar,
                erro = erro,
                ocupado = ocupado,
                tentados = tentados,
                aoVoltar = aoVoltar,
                aoConectar = aoConectar,
                aoEscolherTentado = aoEscolherTentado,
            )
        }
    }
}
