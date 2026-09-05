package com.limelight.ui.compose

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView

/**
 * A porta pela qual o Java entra na casca inteira.
 *
 * <h2>Por que uma classe com ligadores, e não um `montar` de trinta e cinco parâmetros</h2>
 *
 * Porque era isso ou uma assinatura que ninguém consegue chamar sem contar vírgulas. As cinco
 * pontes que isto substitui somavam os mesmos trinta e cinco argumentos — o que muda é que
 * agora eles chegam agrupados por tela, e o compilador diz qual grupo faltou em vez de aceitar
 * um `null` na posição errada.
 *
 * O uso, do lado Java, é: construir, ligar cada tela uma vez, montar uma vez. Depois disso o
 * que muda são os campos de [EstadoDaCasca] e [EstadoDaHome], que são observáveis — nada
 * precisa remontar nada.
 */
class PonteDaCasca(
    private val view: ComposeView,
    private val estado: EstadoDaCasca,
    private val estadoDaHome: EstadoDaHome,
) {

    // ── home ────────────────────────────────────────────────────────────────
    private var aoTocarHost: (HostUi) -> Unit = {}
    private var aoSegurarHost: (HostUi) -> Unit = {}
    private var aoContinuar: (Historico.Entrada) -> Unit = {}
    private var aoAdicionar: () -> Unit = {}
    private var aoCenas: () -> Unit = {}
    private var aoAjustes: () -> Unit = {}
    private var aoRetomar: (HostUi) -> Unit = {}
    private var aoEncerrar: (HostUi) -> Unit = {}
    private var aoParear: (HostUi) -> Unit = {}
    private var aoAcordar: (HostUi) -> Unit = {}
    private var aoRemover: (HostUi) -> Unit = {}
    private var aoMais: (HostUi) -> Unit = {}

    // ── catálogo ────────────────────────────────────────────────────────────
    private var aoRecarregarCatalogo: () -> Unit = {}
    private var aoRetomarSessao: () -> Unit = {}
    private var aoAcordarDoCatalogo: () -> Unit = {}
    private var aoParearDoCatalogo: () -> Unit = {}
    private var aoTocarApp: (Int) -> Unit = {}
    private var aoSegurarApp: (Int) -> Unit = {}
    private var aoLigarCelula: (Int, View) -> Unit = { _, _ -> }

    private var aoSair: () -> Unit = {}

    fun ligarHome(
        aoTocar: (HostUi) -> Unit,
        aoSegurar: (HostUi) -> Unit,
        aoContinuar: (Historico.Entrada) -> Unit,
        aoAdicionar: () -> Unit,
        aoCenas: () -> Unit,
        aoAjustes: () -> Unit,
        aoRetomar: (HostUi) -> Unit,
        aoEncerrar: (HostUi) -> Unit,
        aoParear: (HostUi) -> Unit,
        aoAcordar: (HostUi) -> Unit,
        aoRemover: (HostUi) -> Unit,
        aoMais: (HostUi) -> Unit,
    ) {
        this.aoTocarHost = aoTocar
        this.aoSegurarHost = aoSegurar
        this.aoContinuar = aoContinuar
        this.aoAdicionar = aoAdicionar
        this.aoCenas = aoCenas
        this.aoAjustes = aoAjustes
        this.aoRetomar = aoRetomar
        this.aoEncerrar = aoEncerrar
        this.aoParear = aoParear
        this.aoAcordar = aoAcordar
        this.aoRemover = aoRemover
        this.aoMais = aoMais
    }

    fun ligarCatalogo(
        aoRecarregar: () -> Unit,
        aoRetomarSessao: () -> Unit,
        aoAcordar: () -> Unit,
        aoParear: () -> Unit,
        aoTocarApp: (Int) -> Unit,
        aoSegurarApp: (Int) -> Unit,
        aoLigarCelula: (Int, View) -> Unit,
    ) {
        this.aoRecarregarCatalogo = aoRecarregar
        this.aoRetomarSessao = aoRetomarSessao
        this.aoAcordarDoCatalogo = aoAcordar
        this.aoParearDoCatalogo = aoParear
        this.aoTocarApp = aoTocarApp
        this.aoSegurarApp = aoSegurarApp
        this.aoLigarCelula = aoLigarCelula
    }

    fun ligarSaida(aoSair: () -> Unit) {
        this.aoSair = aoSair
    }

    private var aoVoltarParaSessao: () -> Unit = {}

    fun ligarRetornoDaSessao(acao: () -> Unit) {
        this.aoVoltarParaSessao = acao
    }

    private var aoTestarRede: (HostUi) -> Unit = {}

    fun ligarTesteDeRede(acao: (HostUi) -> Unit) {
        this.aoTestarRede = acao
    }

    fun montar() {
        view.setContent {
            Casca(
                estado = estado,
                aoSairDaCasca = aoSair,
                aoVoltarParaSessao = { aoVoltarParaSessao() },
                home = {
                    TelaHome(
                        estado = estadoDaHome,
                        continuar = estado.continuar,
                        aoTocarHost = aoTocarHost,
                        aoSegurarHost = aoSegurarHost,
                        aoContinuar = aoContinuar,
                        aoAdicionar = aoAdicionar,
                        aoCenas = aoCenas,
                        aoAjustes = aoAjustes,
                        nomeDasCenas = estado.nomeDasCenas,
                        aoRetomar = aoRetomar,
                        aoEncerrar = aoEncerrar,
                        aoParear = aoParear,
                        aoAcordar = aoAcordar,
                        aoRemover = aoRemover,
                        aoMais = aoMais,
                        aoTestarRede = aoTestarRede,
                        motivoDoSilencio = estado.motivoDoSilencio,
                    )
                },
                catalogo = { dados ->
                    // A geração é lida aqui de propósito: o `BaseAdapter` muda por dentro sem
                    // trocar de instância, então nada nesta composição perceberia a lista nova
                    // sem um valor observável que ande junto.
                    val geracao = dados.geracao
                    CatalogoDoLugar(dados, geracao)
                },
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun CatalogoDoLugar(dados: EstadoDoCatalogo, geracao: Int) {
        val adapter = dados.adapter
        TelaCatalogo(
            nomeDoHost = dados.nomeDoHost,
            uuidDoHost = dados.uuid,
            quantos = dados.quantos,
            recarregando = dados.recarregando,
            sessaoAberta = dados.sessaoAberta,
            estadoDoHost = dados.estadoDoHost,
            aoVoltar = { estado.voltar() },
            aoRecarregar = aoRecarregarCatalogo,
            aoRetomar = aoRetomarSessao,
            aoAcordar = aoAcordarDoCatalogo,
            aoParear = aoParearDoCatalogo,
            itens = if (adapter == null) 0 else adapter.count,
            celula = { indice ->
                if (adapter != null) {
                    CelulaDoAdapter(adapter, indice, geracao, aoLigarCelula)
                }
            },
            aoTocarItem = aoTocarApp,
            aoSegurarItem = aoSegurarApp,
        )
    }
}
