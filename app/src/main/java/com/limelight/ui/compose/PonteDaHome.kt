package com.limelight.ui.compose

import androidx.compose.ui.platform.ComposeView

/**
 * A porta pela qual o Java entra no Compose.
 *
 * `setContent` recebe uma lambda `@Composable`, e uma lambda dessas não existe em Java —
 * chamá-la de lá exigiria montar à mão o que o compilador do Kotlin gera. Então a fronteira
 * fica aqui, num arquivo só: `PcView` passa estado e callbacks comuns, e nada mais do app
 * precisa saber que existe Compose do outro lado.
 *
 * É também a fronteira que torna a migração reversível: apagar estas funções e voltar o XML
 * é um commit.
 */
object PonteDaHome {

    /** A home inteira. */
    @JvmStatic
    @JvmName("montarHome")
    fun montarHome(
        view: ComposeView,
        estado: EstadoDaHome,
        continuar: List<Historico.Entrada>,
        aoTocar: (HostUi) -> Unit,
        aoSegurar: (HostUi) -> Unit,
        aoContinuar: (Historico.Entrada) -> Unit,
        aoAdicionar: () -> Unit,
        aoCenas: () -> Unit,
        aoAjustes: () -> Unit,
        nomeDasCenas: String,
        motivoDoSilencio: Int,
        aoRetomar: (HostUi) -> Unit,
        aoEncerrar: (HostUi) -> Unit,
        aoParear: (HostUi) -> Unit,
        aoAcordar: (HostUi) -> Unit,
        aoRemover: (HostUi) -> Unit,
        aoMais: (HostUi) -> Unit,
    ) {
        view.setContent {
            TelaHome(
                estado = estado,
                continuar = continuar,
                aoTocarHost = aoTocar,
                aoSegurarHost = aoSegurar,
                aoContinuar = aoContinuar,
                aoAdicionar = aoAdicionar,
                aoCenas = aoCenas,
                aoAjustes = aoAjustes,
                nomeDasCenas = nomeDasCenas,
                aoRetomar = aoRetomar,
                aoEncerrar = aoEncerrar,
                aoParear = aoParear,
                aoAcordar = aoAcordar,
                aoRemover = aoRemover,
                aoMais = aoMais,
                motivoDoSilencio = motivoDoSilencio,
            )
        }
    }

    /** Só a lista, para a captura de tela e para quem quiser a grade sozinha. */
    @JvmStatic
    @JvmName("montar")
    fun montarGradeDeHosts(
        view: ComposeView,
        estado: EstadoDaHome,
        aoTocar: (HostUi) -> Unit,
        aoSegurar: (HostUi) -> Unit,
    ) {
        view.setContent {
            GradeDeHosts(
                hosts = estado.hosts,
                aoTocar = aoTocar,
                aoSegurar = aoSegurar,
                temSondaEmCurso = estado.temSondaEmCurso(),
                aoVencerPrazo = { estado.reavaliarPrazos() },
            )
        }
    }
}
