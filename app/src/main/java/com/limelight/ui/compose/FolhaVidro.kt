package com.limelight.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R

/**
 * Uma ação oferecida numa folha.
 *
 * `destrutiva` pinta o rótulo diferente — encerrar sessão e remover lugar não podem parecer
 * iguais a abrir catálogo.
 */
/**
 * As curvas da folha, traduzidas dos interpoladores da identidade.
 *
 * `tween(240)` SEM easing usa o padrao do Compose, que e `FastOutSlowInEasing` --
 * cubic-bezier(.4, 0, .2, 1), literalmente a curva padrao do Material Design. O Material
 * foi deixado de fora das dependencias justamente para nao entrar por essa porta, e a folha
 * mais nova do app entrava e saia por ela.
 *
 * Os numeros sao os mesmos de `res/interpolator/vidro_sheet.xml` e `vidro_sheet_down.xml`.
 * A curva de SAIDA e a que mais importa: ela acelera ate sumir, e e o que faz "fechar"
 * significar sair da frente. Com a curva de entrada nos dois sentidos, o fechamento parecia
 * hesitar.
 */
private val SOBE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val DESCE = CubicBezierEasing(0.4f, 0f, 1f, 1f)

/** Folha sobe 240, desce 180 -- a tabela de movimento do handoff. */
private const val MS_SOBE = 240
private const val MS_DESCE = 180

data class AcaoVidro(
    val rotulo: String,
    val apoio: String? = null,
    val destrutiva: Boolean = false,
    /**
     * Não é ação: é uma frase que explica por que uma ação não está aqui.
     *
     * Nasceu de um defeito concreto. "Sem endereço guardado para acordar daqui" era montada
     * como `destrutiva = true` só para sair em outra cor — e saía no vermelho de alerta, ao
     * lado de "Remover este lugar", que é destrutiva de verdade. Dois vermelhos, dois
     * significados, e o mais grave dos dois perdendo peso por causa do outro.
     *
     * Informativa é cinza, não responde ao toque e não conta para o respiro que separa a
     * primeira destrutiva do resto.
     */
    val informativa: Boolean = false,
    val aoTocar: () -> Unit,
)

/**
 * A folha de ações — o PE03.
 *
 * Substitui o menu de contexto do sistema, que na home tinha **14 `menu.add`** e no catálogo
 * **12**, abertos por um toque longo que nada na tela anunciava, com a moldura, a tipografia
 * e o espaçamento do Android no meio de um app que passou meses construindo os próprios.
 *
 * Duas regras que vieram do catálogo de produto e não são estéticas:
 *
 *  1. **No máximo quatro linhas por vez**, escolhidas pelo estado real. Dormindo mostra
 *     acordar; não pareado mostra parear; rodando mostra retomar e encerrar.
 *  2. **Nada some sem ter para onde ir.** O que não cabe nas quatro desce para "Mais", que
 *     ainda abre o menu antigo. Podar é decidir item a item — e essa decisão é uma segunda
 *     rodada, não um efeito colateral desta.
 */
@Composable
fun FolhaVidro(
    titulo: String,
    subtitulo: String?,
    acoes: List<AcaoVidro>,
    aoFechar: () -> Unit,
    visivel: Boolean,
) {
    AnimatedVisibility(
        visible = visivel,
        // No MESMO tempo e na MESMA curva da folha. Antes o véu entrava e saía em 180 ms
        // enquanto a folha viajava 240: o escurecimento terminava 60 ms antes de ela
        // chegar, então o fundo já tinha apagado com a folha ainda a caminho.
        enter = fadeIn(molaCorpo()),
        exit = fadeOut(molaCorpo()),
    ) {
        val rotuloDeFechar = stringResource(R.string.vidro_close_sheet)
        Box(
            Modifier
                .fillMaxSize()
                // Tocar fora fecha. É o Voltar do dedo, e sem ele a folha vira um beco.
                .clickable(indication = null, interactionSource = fonteDeInteracao()) { aoFechar() }
                // E ele precisa DIZER isso. Medido com uiautomator: o véu saía como um
                // `View` clicável de tela cheia, sem texto e sem descrição, e sem filho
                // nenhum de onde tirar um rótulo. Quem usa leitor de tela encontrava um
                // botão anônimo cobrindo a tela inteira e não tinha como saber que ele é a
                // saída da folha.
                .semantics { contentDescription = rotuloDeFechar }
                // O véu da identidade, não preto puro: `vidro_scrim` é #8C020305, 55% do
                // azul-preto do void. Preto puro cravado no código era um segundo véu para
                // o mesmo trabalho, e o que não vinha da paleta.
                .background(colorResource(R.color.vidro_scrim_cor)),
        )
    }

    AnimatedVisibility(
        visible = visivel,
        // Sobe em 240 ms e desce em 180 — os tempos de folha da tabela de movimento do
        // handoff, não números novos.
        // A ASSIMETRIA SUBIR/DESCER SAIU, e a mola é o motivo.
        //
        // Ela existia porque `tween` não pode ser interrompido: sem 180 ms na saída contra
        // 240 na entrada, dispensar a folha parecia lento. Uma mola sai da posição em que
        // está — quem fecha logo depois de abrir vê a folha voltar do meio do caminho, e não
        // recomeçar do fim. A pressa deixou de precisar de um número próprio.
        enter = slideInVertically(molaCorpo()) { it } + fadeIn(molaCorpo()),
        exit = slideOutVertically(molaCorpo()) { it } + fadeOut(molaCorpo()),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val largo = maxWidth >= 600.dp

            Column(
                Modifier
                    .align(if (largo) Alignment.BottomEnd else Alignment.BottomCenter)
                    .widthIn(max = if (largo) 420.dp else 10_000.dp)
                    .padding(dimensionResource(R.dimen.vidro_inset_card))
                    .navigationBarsPadding()
                    // TETO DE ALTURA, e ele existe por um caso concreto.
                    //
                    // A folha e ancorada no RODAPE. Uma lista que passa da altura da tela
                    // nao transborda para baixo -- ela cresce para CIMA, e as primeiras
                    // opcoes saem pelo topo, sem rolagem e sem jeito de alcanca-las. A
                    // escolha de resolucao chega a nove opcoes (seis do array, mais nativa
                    // retrato, nativa paisagem e personalizada): no tablet deitado, as tres
                    // de cima simplesmente desapareciam.
                    //
                    // O teto e 80% e nao 100% de proposito: a folha tem de continuar
                    // parecendo uma folha, com o fundo aparecendo por tras dela. Uma folha
                    // que toma a tela inteira e uma tela, e ai o toque fora -- que e como
                    // ela se dispensa -- deixa de ter onde acontecer.
                    .heightIn(max = maxHeight * 0.8f)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_xl)))
                    .background(colorResource(R.color.vidro_glass_fill_session))
                    .border(
                        width = dimensionResource(R.dimen.vidro_stroke_width),
                        color = colorResource(R.color.vidro_stroke_glass),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_xl)),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.vidro_space_4)),
            ) {
                BasicText(
                    text = titulo,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.manrope_bold)),
                        fontSize = 18.sp,
                        letterSpacing = (-0.02).emKt(),
                        color = colorResource(R.color.vidro_text),
                    ),
                )
                if (!subtitulo.isNullOrEmpty()) {
                    BasicText(
                        text = subtitulo,
                        modifier = Modifier.padding(top = 2.dp),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.manrope_medium)),
                            fontSize = 13.sp,
                            color = colorResource(R.color.vidro_text_3),
                        ),
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

                acoes.forEachIndexed { i, acao ->
                    // Respiro ENTRE as linhas, e um respiro maior antes da primeira
                    // destrutiva.
                    //
                    // Elas eram empilhadas sem espaço nenhum: viravam uma laje contínua
                    // onde a fronteira só existia no realce de toque. E "Remover este
                    // lugar" ficava encostada em "Ver os aplicativos" — errar três dp para
                    // baixo trocava abrir o catálogo por apagar o host. O agrupamento tem
                    // de dizer o que a cor sozinha não diz.
                    if (i > 0) {
                        // A informativa não conta para a fronteira: ela não é ação, então
                        // "a primeira destrutiva depois de uma não-destrutiva" tem de olhar
                        // para a última AÇÃO, não para a última linha.
                        val anterior = acoes[i - 1]
                        val primeiraDestrutiva = acao.destrutiva
                                && !anterior.destrutiva && !anterior.informativa
                        Spacer(Modifier.height(dimensionResource(
                            if (primeiraDestrutiva) R.dimen.vidro_space_4
                            else R.dimen.vidro_space_2)))
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            // O alvo mínimo vem do recurso, então o tablet pega os 52dp
                            // sozinho — a linha tinha ~45dp, abaixo dos dois pisos.
                            .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_m)))
                            // A informativa não responde ao toque, e não deve: ela é uma
                            // frase que explica por que uma ação não está aqui. Uma linha
                            // que afunda ao ser tocada e não faz nada é pior que uma linha
                            // que não afunda.
                            .then(
                                if (acao.informativa) Modifier
                                else Modifier.cartaoTocavel(aoTocar = acao.aoTocar)
                            )
                            .padding(
                                horizontal = dimensionResource(R.dimen.vidro_space_3),
                                vertical = dimensionResource(R.dimen.vidro_space_2),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            BasicText(
                                text = acao.rotulo,
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.02).emKt(),
                                    // Destrutiva pinta o tom de ALERTA, não um branco mais
                                    // apagado. #D5D7DD contra #F4F5F8 sobre o vidro escuro
                                    // é 88% de luminosidade contra 97%: o olho não separa,
                                    // e o pouco que separa lê como DESABILITADO, que é o
                                    // oposto do aviso pretendido.
                                    color = colorResource(
                                        when {
                                            // Cinza, e nem alerta nem tinta principal: ela
                                            // não é ação nenhuma. Pintá-la de vermelho —
                                            // que é como ela nasceu — punha dois vermelhos
                                            // de significados diferentes na mesma folha, e
                                            // o de verdade perdia peso por causa do outro.
                                            acao.informativa -> R.color.vidro_text_3
                                            acao.destrutiva -> R.color.vidro_alerta
                                            else -> R.color.vidro_text
                                        }
                                    ),
                                ),
                            )
                            if (!acao.apoio.isNullOrEmpty()) {
                                BasicText(
                                    text = acao.apoio,
                                    style = TextStyle(
                                        fontFamily = FontFamily(Font(R.font.manrope_medium)),
                                        fontSize = 12.sp,
                                        color = colorResource(R.color.vidro_text_3),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** O scrim não desenha realce de toque; ele só precisa de uma fonte para aceitar o clique. */
@Composable
private fun fonteDeInteracao() =
    remember { MutableInteractionSource() }
