package com.limelight.ui.compose

import android.widget.BaseAdapter
import android.view.ViewGroup
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A porta pela qual o `AppView` entra no Compose.
 *
 * Aqui mora a decisão mais importante desta tela: **a célula da grade continua sendo a View
 * que já existe.** O `AndroidView` abaixo pede ao adapter o item de sempre, com todo o
 * `CachedAppAssetLoader` atrás dele — cache em disco, reamostragem por densidade,
 * cancelamento de carga ao rolar, e o `VidroPosterBox` que trava a proporção da arte.
 *
 * Reescrever essa cadeia em Compose durante uma migração de UI seria trocar código medido
 * contra um Vibeshine de verdade por código novo sem medição nenhuma — e o defeito que ela
 * conserta foi levantado pelo dono em anotação: *"NUNCA deveria quebrar o aspect ratio das
 * imagens, nem deformar nem cortar"*.
 */
object PonteDoCatalogo {

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        nomeDoHost: String,
        quantos: Int,
        recarregando: Boolean,
        sessaoAberta: String?,
        estadoDoHost: EstadoDoHostNoCatalogo,
        aoVoltar: () -> Unit,
        aoRecarregar: () -> Unit,
        aoRetomar: () -> Unit,
        aoAcordar: () -> Unit,
        aoParear: () -> Unit,
        adapter: BaseAdapter?,
        aoTocarItem: (Int) -> Unit,
        aoSegurarItem: (Int) -> Unit,
        /**
         * Avisa qual View ficou ligada a cada indice.
         *
         * O menu de contexto de uma AbsListView entregava `info.targetView` de graca, e o
         * catalogo usava essa View para tirar a capa ja carregada e virar icone de atalho.
         * Com a grade em Compose nao ha targetView -- entao a celula se anuncia.
         */
        aoLigarCelula: (Int, android.view.View) -> Unit,
    ) {
        view.setContent {
            TelaCatalogo(
                nomeDoHost = nomeDoHost,
                quantos = quantos,
                recarregando = recarregando,
                sessaoAberta = sessaoAberta,
                estadoDoHost = estadoDoHost,
                aoVoltar = aoVoltar,
                aoRecarregar = aoRecarregar,
                aoRetomar = aoRetomar,
                aoAcordar = aoAcordar,
                aoParear = aoParear,
                itens = quantos,
                celula = { i -> CelulaDoAdapter(adapter, i, 0, aoLigarCelula) },
                aoTocarItem = aoTocarItem,
                aoSegurarItem = aoSegurarItem,
            )
        }
    }
}

/**
 * Uma célula da grade, vinda do adapter.
 *
 * O `FrameLayout` intermediário existe para a reciclagem funcionar: `getView` devolve a
 * mesma View quando recebe uma reaproveitável, e sem um recipiente estável não haveria onde
 * guardá-la entre recomposições.
 */
@Composable
internal fun CelulaDoAdapter(
    adapter: BaseAdapter?,
    indice: Int,
    /**
     * Muda quando a lista muda por dentro sem trocar de instancia de adapter.
     *
     * O `update` de um `AndroidView` so roda de novo quando algo que ele LE muda. O adapter
     * e a mesma instancia do primeiro ao ultimo `notifyDataSetChanged`, entao sem um valor
     * que ande junto a celula ficaria desenhando a lista de quando a tela abriu.
     */
    geracao: Int,
    aoLigar: (Int, android.view.View) -> Unit,
) {
    if (adapter == null || indice >= adapter.count) {
        return
    }
    AndroidView(
        // A CAIXA VEM DE FORA, e a View do adaptador se acomoda nela.
        //
        // Sem `fillMaxSize` o `AndroidView` media o que a View media, e a celula herdava a
        // altura da arte -- artes de proporcoes diferentes davam fileiras desalinhadas. Quem
        // decide o tamanho agora e a grade, que sabe que capa e 3:4.
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> FrameLayout(ctx) },
        update = { recipiente ->
            // Lido, e nao usado: e o que faz o Compose reexecutar este bloco.
            @Suppress("UNUSED_EXPRESSION") geracao
            if (indice < adapter.count) {
                val reaproveitavel = if (recipiente.childCount > 0) recipiente.getChildAt(0) else null
                val nova = adapter.getView(indice, reaproveitavel, recipiente)
                if (nova !== reaproveitavel) {
                    recipiente.removeAllViews()
                    recipiente.addView(
                        nova,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }

                // A CÉLULA PRECISA DIZER O NOME DO APLICATIVO.
                //
                // Medido com uiautomator no catálogo: cada célula sai como um `View` clicável
                // de 182x242 dp sem texto e sem descrição — nenhum descendente tem rótulo,
                // porque o nome do jogo está DESENHADO DENTRO da imagem de capa que o host
                // gera. Não há texto nenhum para um leitor de tela encontrar: o catálogo
                // inteiro é uma parede de botões anônimos.
                //
                // O nome vem do próprio item do adaptador, que é onde ele sempre esteve.
                recipiente.contentDescription = adapter.getItem(indice)?.toString()

                // A arte estica para preencher, e a celula ja recorta nos cantos. Sem isto a
                // capa do DESKTOP saia com tarja preta dos dois lados dentro do proprio
                // cartao -- a arte dele nao tem a proporcao das outras.
                esticarCapa(nova)
                aoLigar(indice, nova)
            }
        },
    )
}

/**
 * Faz a arte da célula preencher a caixa que a grade deu.
 *
 * O adaptador entrega a View do Moonlight, e dentro dela a `ImageView` da capa vem com o
 * `scaleType` do layout antigo — que encaixava, não preenchia. Encaixar dentro de um cartão
 * com fundo próprio deixa tarja, e tarja dentro de cartão lê como imagem quebrada.
 *
 * Procura em profundidade porque a hierarquia da célula é do upstream e pode mudar: o que
 * importa é que exista uma ImageView, não onde ela está.
 */
private fun esticarCapa(raiz: android.view.View) {
    if (raiz is android.widget.ImageView) {
        raiz.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        raiz.adjustViewBounds = false
        return
    }
    if (raiz is ViewGroup) {
        for (i in 0 until raiz.childCount) {
            esticarCapa(raiz.getChildAt(i))
        }
    }
}
