package com.limelight.ui.compose

import android.widget.BaseAdapter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.remember
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.limelight.R

/**
 * As telas da casca. Uma pilha, não duas janelas.
 */
sealed interface Rota {
    data object Home : Rota
    data class Catalogo(val uuid: String) : Rota
}

/** O que o catálogo precisa saber, alimentado pelo lado Java. */
class EstadoDoCatalogo {
    var nomeDoHost by mutableStateOf("")
    var uuid by mutableStateOf("")
    var quantos by mutableStateOf(0)
    var recarregando by mutableStateOf(false)
    var sessaoAberta by mutableStateOf<String?>(null)
    var estadoDoHost by mutableStateOf(EstadoDoHostNoCatalogo.NORMAL)

    /**
     * O adaptador continua sendo de View, e isso é decisão e não dívida.
     *
     * A capa de um aplicativo vem do `CachedAppAssetLoader`, que carrega em disco, em memória
     * e pela rede com cancelamento por posição de célula. Reescrever aquilo em Compose seria
     * refazer um cache que funciona para ganhar nada visível — a célula é uma imagem num
     * retângulo. `PonteDoCatalogo` já sabe hospedar cada célula por `AndroidView`.
     */
    var adapter by mutableStateOf<BaseAdapter?>(null)

    /** Força o recomeço da composição quando o adaptador muda por dentro sem trocar de instância. */
    var geracao by mutableStateOf(0)
}

/**
 * A pilha da casca, e o estado de cada tela dela.
 *
 * <h2>Por que uma lista em estado, e não navigation-compose</h2>
 *
 * Porque a pilha deste app tem dois destinos e nenhuma rota com argumento de URL, e uma
 * dependência nova é um ADR. `mutableStateListOf` faz o que precisa ser feito e cabe num
 * arquivo que dá para ler inteiro.
 */
class EstadoDaCasca {

    private val pilha = mutableStateListOf<Rota>(Rota.Home)

    val atual: Rota get() = pilha.last()

    val catalogo = EstadoDoCatalogo()

    /** Preenchido pelo lado Java a cada mudança do rodapé da home. */
    var nomeDasCenas by mutableStateOf("")
    var motivoDoSilencio by mutableStateOf(0)
    var continuar by mutableStateOf<List<Historico.Entrada>>(emptyList())

    /**
     * O nome do que está rodando ATRÁS desta casca, ou nulo quando não há nada rodando.
     *
     * Isto não é o mesmo que o `sessaoAberta` do catálogo, e a diferença importa: aquele vem
     * do `runningGameId` que o host informa pela rede, e só aparece depois de uma sondagem.
     * Este vem da própria `Game`, que hospeda esta casca e portanto SABE, sem perguntar a
     * ninguém, que existe vídeo decodificando atrás.
     *
     * Sem ele a saída segura não é segura: medido no emulador, sair da sessão por "Escolher
     * outra coisa" levava a uma home idêntica à de quem nunca abriu nada — o vídeo continuava
     * vivo e o único caminho de volta era o Voltar, que ninguém tem como adivinhar.
     */
    var sessaoViva by mutableStateOf<String?>(null)

    fun ir(rota: Rota) {
        // Ir para onde já se está não empilha. Sem isto, dois toques rápidos no mesmo lugar
        // exigiriam dois Voltares para desfazer um só.
        if (pilha.last() != rota) {
            pilha.add(rota)
        }
    }

    fun voltar(): Boolean {
        if (pilha.size <= 1) {
            return false
        }
        pilha.removeAt(pilha.size - 1)
        return true
    }

    fun voltarParaHome() {
        while (pilha.size > 1) {
            pilha.removeAt(pilha.size - 1)
        }
    }

    fun estaNaHome(): Boolean = pilha.size == 1
}

/**
 * A casca inteira — uma tela só, e o que você toca viaja.
 *
 * <h2>O que isto apaga</h2>
 *
 * O pop de janela do Android (`vidro_enter.xml`, escala 0,94 a partir do centro, aplicado
 * como `windowAnimationStyle`) na aresta home → catálogo, e a Activity `AppView` inteira:
 * a lógica dela virou `CatalogoDoLugar`, que é controlador e não tela.
 *
 * <h2>O que fica de fora, e por quê</h2>
 *
 * <b>Isto mudou:</b> `Game` deixou de ser uma Activity separada no caminho normal — ela
 * hospeda esta casca como mais uma camada sobre a `SurfaceView`, e `noHistory` saiu do
 * manifesto. Quem abre esta árvore é `CascaDoApp`, que é controlador, não tela. O que sobrou
 * de janela separada é o atalho da tela inicial, que cai direto numa sessão.
 *
 * <b>E ficam de fora, por ora, três telas que poderiam entrar:</b> cenas, endereço e
 * depuração. Elas ainda são Activities, então naquelas arestas o pop de janela continua
 * existindo — está dito aqui em vez de escondido. A aresta que esta mudança atacou é a que se
 * atravessa todo dia: da lista de máquinas para o catálogo de uma delas.
 *
 * `StreamSettings` e o editor de cena são um caso diferente e permanente: os dois hospedam a
 * árvore de `preferences.xml` num `PreferenceFragmentCompat`, que é View e precisa de
 * FragmentManager. A árvore fica porque configuração avançada tem de continuar alcançável —
 * esconder pode, apagar não.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Casca(
    estado: EstadoDaCasca,
    home: @Composable () -> Unit,
    catalogo: @Composable (EstadoDoCatalogo) -> Unit,
    /** Chamado quando o Voltar chega na home — a Activity decide o que fazer. */
    aoSairDaCasca: () -> Unit,
    /** Chamado pela faixa do topo: devolve o vídeo que ficou rodando atrás. */
    aoVoltarParaSessao: () -> Unit = {},
) {
    BackHandler {
        if (!estado.voltar()) {
            aoSairDaCasca()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // EM COLUNA, e não sobreposta.
        //
        // Uma faixa flutuante teria de saber a altura do rodapé da home ("Cenas · Ajustes")
        // para não cobri-lo, e passaria a errar toda vez que aquele rodapé mudasse. Empurrando
        // o conteúdo ela não pode cobrir nada, e serve as duas rotas sem saber nada delas.
        // ENTRA DESLIZANDO, e o conteúdo desce junto — a mesma mola do corpo da
        // interface. Aparecer seca fazia a home inteira PULAR 48 dp num quadro, sem
        // explicação; a expansão diz de onde a faixa veio e devolve o espaço ao sair.
        //
        // O nome fica retido durante a saída (o mesmo truque da FolhaVidro): o estado vira
        // null antes de a animação terminar, e uma faixa esvaziando no meio do caminho
        // leria como defeito.
        val viva = estado.sessaoViva
        val ultimoNome = remember { mutableStateOf<String?>(null) }
        if (viva != null) {
            ultimoNome.value = viva
        }
        AnimatedVisibility(
            visible = viva != null,
            enter = expandVertically(molaCorpo()) + fadeIn(molaCorpo()),
            exit = shrinkVertically(molaCorpo()) + fadeOut(molaCorpo()),
        ) {
            FaixaDaSessaoViva(ultimoNome.value ?: "", aoVoltarParaSessao)
        }

        SharedTransitionLayout(Modifier.weight(1f)) {
        AnimatedContent(
            targetState = estado.atual,
            transitionSpec = {
                // A QUE SAI VAI PRIMEIRO. A que entra espera.
                //
                // Com as duas molas iguais, no meio do caminho as duas telas estavam a ~50% e
                // as duas apareciam INTEIRAS ao mesmo tempo — na gravação de 26/08 dá para
                // ler a home e o catálogo empilhados por vários quadros, com o nome do host
                // por cima do subtítulo. Um cruzamento simétrico não é transição, é
                // sobreposição.
                //
                // Aqui é `tween` e não mola, e a razão é honesta: o que este par precisa é de
                // ORDEM — uma sai, depois a outra entra —, e mola não tem como expressar
                // atraso. Mola descreve travessia; isto é sequência. Ver a doutrina em
                // Molas.kt, que já abre essa exceção para o que não vai a lugar nenhum.
                fadeIn(tween(160, delayMillis = 110)) togetherWith fadeOut(tween(110))
            },
            label = "casca",
        ) { rota ->
            CompositionLocalProvider(
                LocalCompartilhamento provides Compartilhamento(
                    this@SharedTransitionLayout,
                    this@AnimatedContent,
                )
            ) {
                when (rota) {
                    is Rota.Home -> home()
                    is Rota.Catalogo -> catalogo(estado.catalogo)
                }
            }
        }
        }
    }
}

/**
 * A faixa que diz que há vídeo rodando atrás, e leva de volta a ele.
 *
 * É o idioma que a plataforma já ensinou — a tarja de chamada em curso —, e ele é o certo
 * aqui pelo mesmo motivo: alguma coisa continua acontecendo fora da vista, e o caminho de
 * volta precisa estar sempre no mesmo lugar.
 *
 * Quente, e a regra de cor do app já dizia isso antes desta faixa existir: quente é o que
 * está EM VIGOR — a máquina ligada, a cena ativa. Uma sessão rodando é exatamente isso. Azul
 * aqui seria errado: azul é a opção escolhida nesta superfície, e isto não é uma escolha.
 */
@Composable
private fun FaixaDaSessaoViva(nome: String, aoVoltar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Tinta quente rebaixada, e não o halo cheio: o halo (#B18B4F) atrás do texto
            // quente (#E9C48B) dá dois tons vizinhos e o texto some. A 14% ele vira um véu
            // sobre o fundo escuro, e o texto quente fica com contraste de leitura.
            .background(colorResource(R.color.vidro_quente).copy(alpha = 0.14f))
            .clickable(onClick = aoVoltar)
            .height(dimensionResource(R.dimen.vidro_touch_target))
            .padding(horizontal = dimensionResource(R.dimen.vidro_space_4)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(R.string.vidro_back_to_session, nome),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 14.sp,
                color = colorResource(R.color.vidro_quente),
            ),
        )
    }
}
