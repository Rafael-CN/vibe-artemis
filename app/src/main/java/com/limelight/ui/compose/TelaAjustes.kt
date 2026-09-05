package com.limelight.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R
import kotlin.math.roundToInt

/**
 * A tela de Ajustes.
 *
 * Substitui `StreamSettings` sobre `PreferenceFragmentCompat` — o último lugar do app onde
 * a interface era montada pelo `androidx.preference`, que o ADR 0004 já apontava como sem
 * equivalente em Compose e por isso deixou para o fim.
 *
 * O que muda não é a stack, é o **respiro**. A tela antiga é o lugar mais apertado do app, e
 * a razão está documentada em `res/layout/vidro_preference_item.xml`: a altura da linha da
 * biblioteca era `max(texto, listPreferredItemHeightSmall, widget)`, então o espaço entre
 * duas linhas dependia de quantas linhas o resumo quebrou e de a linha ter ou não caixa de
 * seleção. Três medidas diferentes na mesma lista. Aqui o respiro é do item, igual para
 * todos, e o que separa um grupo do outro é **espaço, não régua** — a `DividerDecoration`
 * some junto com a biblioteca que a desenhava.
 *
 * Duas telas na mesma função, decididas por `abertaEm`: o índice de categorias, e os itens
 * de uma categoria. É a navegação que o `PreferenceScreen` aninhado dava de graça e que
 * agora é um parâmetro — o dono da tela decide o que está aberto, esta função só desenha.
 *
 * **Nada aqui é do Material.** Interruptor e deslizante são desenhados em `Canvas`; o
 * `Switch` e o `Slider` da biblioteca trariam a paleta, o ripple e a forma do Material
 * junto, que é exatamente o que o ADR 0004 manteve fora da dependência.
 */
@Composable
fun TelaAjustes(
    telas: List<TelaDeAjustes>,
    abertaEm: TelaDeAjustes?,
    aoVoltar: () -> Unit,
    aoAbrirTela: (TelaDeAjustes) -> Unit,
    aoMudarInterruptor: (String, Boolean) -> Unit,
    aoMudarEscolha: (String, String) -> Unit,
    aoMudarDeslizante: (String, Int) -> Unit,
    aoTocarPorta: (String) -> Unit,
) {
    // A folha de escolha é a única camada que abre POR CIMA desta tela, e por isso é a
    // única coisa que o estado local precisa guardar. O resto do estado é do dono.
    var escolhaAberta by remember { mutableStateOf<AjusteUi.Escolha?>(null) }

    // Trocar de categoria fecha a folha. Sem isto, uma folha aberta sobreviveria à
    // navegação e passaria a oferecer as opções de um item que não está mais na tela.
    LaunchedEffect(abertaEm?.chave) { escolhaAberta = null }

    // O VOLTAR FECHA A FOLHA ANTES DE SAIR.
    //
    // Um só, e não dois: dois `BackHandler` na mesma árvore dependem da ordem em que o
    // dispatcher os registrou para decidir quem ganha, e essa ordem é a da composição —
    // uma sutileza que não se vê lendo o arquivo. Com um if fica escrito na cara.
    //
    // Sempre habilitado porque `aoVoltar` é contratado para as duas metades: volta ao
    // índice, ou sai se já estiver nele. Quem implementa a ponte é que sabe qual é.
    BackHandler {
        if (escolhaAberta != null) {
            escolhaAberta = null
        } else {
            aoVoltar()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_void))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                // Teto de leitura, e CENTRADO no que sobra.
                //
                // As outras telas migradas deixam a moldura encostada à esquerda porque o
                // conteúdo delas é card, que ocupa a largura. Esta é uma coluna de leitura
                // pura: no Tab S8 Ultra deitado, 1722 dp de linha com a seta de "abre outra
                // tela" a meio metro do título não é lista, é tabela. A tela de hoje já
                // centraliza — ver app/build/vidro-screens/tab-00-ajustes-viva.png — e
                // migrar não é hora de perder isso.
                .widthIn(max = TETO_DE_LEITURA)
                .align(Alignment.TopCenter),
        ) {
            Cabecalho(abertaEm, aoVoltar)

            if (abertaEm == null) {
                Indice(telas, aoAbrirTela)
            } else {
                Categoria(
                    aberta = abertaEm,
                    aoMudarInterruptor = aoMudarInterruptor,
                    aoMudarEscolha = aoMudarEscolha,
                    aoMudarDeslizante = aoMudarDeslizante,
                    aoTocarPorta = aoTocarPorta,
                    aoAbrirEscolha = { escolhaAberta = it },
                )
            }
        }

        FolhaDeEscolha(
            escolha = escolhaAberta,
            aoEscolher = aoMudarEscolha,
            aoFechar = { escolhaAberta = null },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// A moldura
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Cabecalho(aberta: TelaDeAjustes?, aoVoltar: () -> Unit) {
    Column(Modifier.padding(horizontal = dimensionResource(R.dimen.vidro_inset_card))) {
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

        // Dentro de uma categoria o rótulo do voltar NOMEIA O DESTINO — "Ajustes" —, que é
        // o mesmo movimento que o catálogo faz com "Lugares". No índice não há nome mais
        // fundo para dar, e ele volta a ser o "Voltar" genérico.
        //
        // A palavra vive dentro de uma caixa de @dimen/vidro_touch_target: as telas irmãs
        // deixam o alvo do tamanho da palavra, que fica abaixo do piso do dedo. Aqui a
        // caixa dá a altura e o texto continua centrado nela.
        Box(
            Modifier
                .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))
                .cartaoTocavel(aoTocar = aoVoltar),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = stringResource(
                    if (aberta == null) R.string.vidro_return else R.string.vidro_settings
                ),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_medium)),
                    fontSize = 15.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )
        }

        BasicText(
            text = stringResource(aberta?.titulo ?: R.string.vidro_settings),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                fontSize = 29.sp,
                letterSpacing = (-0.035).emKt(),
                color = colorResource(R.color.vidro_text),
            ),
        )

        // O resumo da categoria vira o subtítulo DELA. No índice ele já é o que explica a
        // linha; aberta a categoria, a mesma frase explica a tela — não há por que
        // escrever a explicação duas vezes em dois lugares.
        if (aberta != null && aberta.resumo != 0) {
            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
            BasicText(
                text = stringResource(aberta.resumo),
                style = estiloDeResumo(),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))
    }
}

/** O índice: uma linha por categoria, e nada mais. */
@Composable
private fun ColumnScope.Indice(telas: List<TelaDeAjustes>, aoAbrir: (TelaDeAjustes) -> Unit) {
    LazyColumn(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.vidro_space_6)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vidro_space_2)),
    ) {
        items(telas, key = { it.chave }) { tela ->
            // Categoria e Porta são a MESMA peça, e é assim de propósito: as duas são uma
            // linha que leva a outro lugar. Desenhá-las diferente diria que uma delas faz
            // algo que a outra não faz.
            LinhaDeAjuste(
                titulo = tela.titulo,
                resumo = tela.resumo,
                aoTocar = { aoAbrir(tela) },
                direita = { Seta() },
            )
        }
    }
}

/** Uma categoria aberta: os itens dela. */
@Composable
private fun ColumnScope.Categoria(
    aberta: TelaDeAjustes,
    aoMudarInterruptor: (String, Boolean) -> Unit,
    aoMudarEscolha: (String, String) -> Unit,
    aoMudarDeslizante: (String, Int) -> Unit,
    aoTocarPorta: (String) -> Unit,
    aoAbrirEscolha: (AjusteUi.Escolha) -> Unit,
) {
    // A CATEGORIA CHEGA, não aparece.
    //
    // Isto não é `AnimatedVisibility(visible = true)` — aquilo tem alvo constante, nunca
    // transiciona, e a camada que ele insere já comeu o toque de um card de cena neste
    // repositório. Aqui quem muda é o estado: `chegou` nasce falso a cada categoria nova
    // (a chave reinicia o `remember`) e o efeito o vira verdadeiro no primeiro quadro.
    //
    // Só deslocamento, sem alpha, e por um motivo prático além do gosto: `graphicsLayer`
    // não intercepta ponteiro, e uma captura do Robolectric que pare no meio da animação
    // mostra a lista 12 dp mais baixa — diferença que ninguém confunde com defeito. Com
    // alpha ela sairia lavada e pareceria bug de desenho.
    var chegou by remember(aberta.chave) { mutableStateOf(false) }
    LaunchedEffect(aberta.chave) { chegou = true }
    val percurso = dimensionResource(R.dimen.vidro_space_3)
    val subida by animateFloatAsState(
        targetValue = if (chegou) 0f else 1f,
        animationSpec = molaCorpo(),
        label = "chegadaDaCategoria",
    )

    // QUEM ESTA LIGADO, para quem depende de alguem.
    //
    // O mapa e montado uma vez por composicao da categoria e nao por item: sem isto cada
    // dependente varreria a lista inteira atras do dono, e a tela de Entrada tem oito deles.
    val ligados = remember(aberta.itens) {
        aberta.itens.filterIsInstance<AjusteUi.Interruptor>().associate { it.chave to it.ligado }
    }

    LazyColumn(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .navigationBarsPadding()
            .graphicsLayer { translationY = subida * percurso.toPx() },
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.vidro_space_6)),
    ) {
        itemsIndexed(aberta.itens, key = { _, item -> item.chave }) { i, item ->
            // O respiro entra DENTRO do item, e não como `verticalArrangement`, porque ele
            // não é constante: depende de quem veio antes.
            Column {
                if (i > 0) {
                    Spacer(Modifier.height(espacoEntre(aberta.itens[i - 1], item)))
                }
                ItemDeAjuste(
                    item = item,
                    habilitado = vivo(item, ligados),
                    aoMudarInterruptor = aoMudarInterruptor,
                    aoMudarEscolha = aoMudarEscolha,
                    aoMudarDeslizante = aoMudarDeslizante,
                    aoTocarPorta = aoTocarPorta,
                    aoAbrirEscolha = aoAbrirEscolha,
                )
            }
        }
    }
}

/**
 * Um item está vivo quando todos os donos acima dele estão ligados.
 *
 * A cadeia é real e tem três degraus no pior caso: `share_performance_logs` depende de
 * `checkbox_enable_perf_logging`, que depende de `checkbox_enable_perf_overlay`. Parar no
 * primeiro degrau deixaria um item aceso embaixo de um pai apagado.
 *
 * Um dono que não está nesta tela conta como ligado: a tabela de donos é global e algumas
 * categorias foram podadas, então a alternativa seria apagar em silêncio um item cujo dono
 * ninguém consegue mais alcançar.
 */
private fun vivo(item: AjusteUi, ligados: Map<String, Boolean>): Boolean {
    var dono = item.dependeDe
    // Teto de segurança: a tabela é escrita à mão e um ciclo travaria a composição.
    var degraus = 0
    while (dono != null && degraus < 8) {
        if (ligados[dono] == false) return false
        dono = donoDe(dono)
        degraus++
    }
    return true
}

/**
 * O espaço entre dois itens — e ele é o que a régua era.
 *
 * A regra é uma só, e a mesma da folha: **a fronteira que importa é a de classe**. Uma
 * Porta não é um ajuste, é uma saída — ela não guarda valor nenhum, ela leva embora. Onde a
 * lista passa de ajuste para porta (ou de porta para ajuste) o vão dobra, e é isso que
 * agrupa. Entre dois itens do mesmo tipo basta o respiro comum, porque cada um já traz a
 * própria altura.
 */
@Composable
private fun espacoEntre(anterior: AjusteUi, atual: AjusteUi): Dp =
    dimensionResource(
        if ((anterior is AjusteUi.Porta) != (atual is AjusteUi.Porta)) R.dimen.vidro_space_5
        else R.dimen.vidro_space_2
    )

@Composable
private fun ItemDeAjuste(
    item: AjusteUi,
    /**
     * Falso quando o dono deste item está desligado.
     *
     * Esmaece E come o toque, nas duas coisas de propósito. Só esmaecer produz o defeito que
     * não parece defeito: a pessoa arrasta o deslizante, o número anda, e nada acontece do
     * outro lado. Só bloquear produz o oposto — uma linha que parece normal e ignora o dedo.
     */
    habilitado: Boolean = true,
    aoMudarInterruptor: (String, Boolean) -> Unit,
    aoMudarEscolha: (String, String) -> Unit,
    aoMudarDeslizante: (String, Int) -> Unit,
    aoTocarPorta: (String) -> Unit,
    aoAbrirEscolha: (AjusteUi.Escolha) -> Unit,
) {
    Box(
        Modifier
            .alphaSeMorto(habilitado)
            .then(
                if (habilitado) Modifier else Modifier.pointerInput(Unit) {
                    // Consome na passada INICIAL, antes de os filhos verem o evento: o
                    // deslizante escuta arrasto por pointerInput próprio e um clickable por
                    // cima não o alcançaria.
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                                .changes.forEach { it.consume() }
                        }
                    }
                }
            )
    ) {
    when (item) {
        is AjusteUi.Interruptor -> LinhaDeAjuste(
            titulo = item.titulo,
            resumo = item.resumo,
            // A LINHA INTEIRA liga e desliga, não só o desenho de 52 dp na ponta. Quem
            // acerta o título acertou o interruptor.
            aoTocar = { aoMudarInterruptor(item.chave, !item.ligado) },
            direita = { Interruptor(item.ligado) },
        )

        is AjusteUi.Escolha ->
            if (item.valores.size <= CHIPS_ATE) {
                // Poucas opções ficam à mostra: com duas ou três, esconder atrás de uma
                // folha custa dois toques para ver o que caberia na própria linha.
                LinhaDeAjuste(
                    titulo = item.titulo,
                    resumo = item.resumo,
                    abaixo = { FileiraDeChips(item, aoMudarEscolha) },
                )
            } else {
                // Muitas opções descem para a folha, e a linha mostra a escolhida — o
                // valor na linha é o que a tela antiga só entregava abrindo o diálogo.
                LinhaDeAjuste(
                    titulo = item.titulo,
                    resumo = item.resumo,
                    aoTocar = { aoAbrirEscolha(item) },
                    direita = { ValorDaLinha(rotuloDe(item)) },
                )
            }

        is AjusteUi.Deslizante -> LinhaDeslizante(item, aoMudarDeslizante)

        is AjusteUi.Porta -> LinhaDeAjuste(
            titulo = item.titulo,
            resumo = item.resumo,
            aoTocar = { aoTocarPorta(item.chave) },
            direita = { Seta() },
        )
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// A linha, que é a mesma para todos os tipos
// ─────────────────────────────────────────────────────────────────────────────

/**
 * O esqueleto de uma linha de Ajustes.
 *
 * Duas decisões vêm inteiras do XML que isto substitui, porque foram compradas caro lá:
 *
 *  1. **Título e controle dividem UMA linha; o resumo desce inteiro por baixo dos dois.**
 *     Quando o resumo dividia a coluna com o valor, cada parágrafo terminava numa guia
 *     diferente — a linha com "60.0 Mbps" perdia a largura do número, a vizinha sem valor
 *     não perdia nada, e a borda direita da lista virava uma serra.
 *  2. **O controle alinha pelo TOPO.** Num título de duas linhas o valor pertence à
 *     PRIMEIRA delas, que é a que o nomeia.
 *
 * A altura mínima existe como piso do dedo e nada mais: o respiro de `vidro_space_4` em
 * cima e embaixo já passa dos 48 dp com uma linha de título, e o conteúdo é alinhado ao
 * topo — então ela nunca reintroduz a sobra distribuída que centralizava o texto na linha
 * curta da tela antiga.
 */
@Composable
private fun LinhaDeAjuste(
    titulo: Int,
    resumo: Int,
    aoTocar: (() -> Unit)? = null,
    direita: (@Composable () -> Unit)? = null,
    abaixo: (@Composable () -> Unit)? = null,
) {
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))

    Column(
        (if (aoTocar != null) base.cartaoTocavel(aoTocar = aoTocar) else base)
            .padding(
                horizontal = dimensionResource(R.dimen.vidro_inset_card),
                vertical = dimensionResource(R.dimen.vidro_space_4),
            ),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            BasicText(
                text = stringResource(titulo),
                modifier = Modifier.weight(1f),
                style = estiloDeTitulo(),
            )
            if (direita != null) {
                Spacer(Modifier.width(dimensionResource(R.dimen.vidro_space_3)))
                direita()
            }
        }

        if (resumo != 0) {
            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
            BasicText(text = stringResource(resumo), style = estiloDeResumo())
        }

        if (abaixo != null) {
            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_3)))
            abaixo()
        }
    }
}

/**
 * Título de item. `VidroText.Item` — 16 sp, semibold, −0.02em.
 *
 * A entrelinha é apertada de propósito, e o motivo está no XML: em "Taxa de bits do vídeo
 * em redes limitadas" o vão entre as DUAS LINHAS DO MESMO nome media quase o mesmo que o
 * vão até o resumo, e "limitadas" passava a ler como item próprio. Um título que quebra
 * tem de se ler como um bloco.
 */
@Composable
private fun estiloDeTitulo() = TextStyle(
    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
    fontSize = 16.sp,
    lineHeight = 21.sp,
    letterSpacing = (-0.02).emKt(),
    color = colorResource(R.color.vidro_text),
)

/** Resumo. Tom terciário: em `vidro_text_2` ele competiria com o título que explica. */
@Composable
private fun estiloDeResumo() = TextStyle(
    fontFamily = FontFamily(Font(R.font.manrope_medium)),
    fontSize = 13.sp,
    lineHeight = 18.sp,
    color = colorResource(R.color.vidro_text_3),
)

/**
 * A marca de "esta linha leva a outro lugar".
 *
 * O vetor já nasce na tinta certa (`vidro_text_3`) e sem acessibilidade: o título da linha
 * é o rótulo, e a seta repetida em treze linhas só faria o leitor de tela anunciar o mesmo
 * ruído treze vezes.
 */
@Composable
private fun Seta() {
    Image(
        painter = painterResource(R.drawable.ic_vidro_forward),
        contentDescription = null,
        modifier = Modifier.size(SETA),
    )
}

/**
 * O valor de uma escolha, na própria linha.
 *
 * Manrope e não mono, e tinta CHEIA — as duas coisas vêm de `res/layout/vidro_pref_value.xml`.
 * Mono é o papel do número que muda enquanto se olha, e ele continua sendo o do deslizante;
 * aqui a coluna é um resumo parado que divide lugar com valores que não são número nenhum
 * ("Automático", "Preferir menor latência"), e duas tipografias na mesma coluna leem como
 * dois campos diferentes.
 *
 * O teto de largura é `vidro_pref_value_max`, que existe exatamente para isto: sem ele,
 * "Nativa Tela Cheia (3088x1440)" tomaria a linha e empurraria o título para fora. Com
 * teto o valor quebra em duas linhas — a caixa cresce, nada é cortado.
 */
@Composable
private fun ValorDaLinha(rotulo: String) {
    BasicText(
        text = rotulo,
        modifier = Modifier.widthIn(max = dimensionResource(R.dimen.vidro_pref_value_max)),
        style = TextStyle(
            fontFamily = FontFamily(Font(R.font.manrope_medium)),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.End,
            color = colorResource(R.color.vidro_text),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Interruptor — desenhado, não emprestado
// ─────────────────────────────────────────────────────────────────────────────

/**
 * O interruptor.
 *
 * Desenhado em `Canvas` porque o `Switch` do Material traria a paleta, a elevação e o
 * ripple dele junto — e porque um trilho com um botão que desliza são quatro chamadas de
 * desenho, não uma dependência.
 *
 * **A cor do ligado é a mesma da seleção do app**, a de `res/drawable/vidro_chip_on.xml`:
 * borda no acento de texto, fundo com um véu do acento, e o botão no acento de
 * preenchimento. É o único padrão de seleção que esta identidade tem, e um interruptor que
 * inventasse o próprio faria o chip e ele parecerem coisas de famílias diferentes.
 *
 * O botão anda, e o movimento é o que diz o estado antes da cor: quem enxerga mal a
 * diferença entre o véu e o realce ainda vê a peça do lado esquerdo ou do direito.
 */
@Composable
private fun Interruptor(ligado: Boolean) {
    val larguraDaBorda = dimensionResource(R.dimen.vidro_stroke_width)

    // 180 ms com a curva de chegada de `res/interpolator/vidro_enter.xml`. A tabela de
    // movimento não tem linha para "controle mudando de estado" — este é o deslocamento
    // mais curto que ela nomeia, e a curva é a de quem CHEGA a um lugar, que é o que o
    // botão faz. A mola do toque não é responsabilidade daqui: ela vem do `cartaoTocavel`
    // da linha inteira.
    //
    // O percurso e as três cores andam no MESMO tempo. Com a cor trocando de uma vez
    // enquanto o botão viaja, o interruptor já estaria pintado de ligado com a peça ainda
    // no meio do caminho — o estado chegaria antes do gesto que o produziu.
    //
    // Nenhum deles usa `by`: lidos dentro do desenho, cada quadro invalida só a fase de
    // desenho, e não a linha inteira.
    val movimento = tween<Float>(
        durationMillis = integerResource(R.integer.vidro_motion_sheet_down),
        easing = CURVA_CHEGA,
    )
    val transicaoDeCor = tween<Color>(
        durationMillis = integerResource(R.integer.vidro_motion_sheet_down),
        easing = CURVA_CHEGA,
    )
    val avanco = animateFloatAsState(
        targetValue = if (ligado) 1f else 0f,
        animationSpec = movimento,
        label = "interruptor",
    )
    val corDoTrilho = animateColorAsState(
        targetValue = colorResource(
            if (ligado) R.color.vidro_accent_fill_idle else R.color.vidro_surface_overlay
        ),
        animationSpec = transicaoDeCor,
        label = "trilhoDoInterruptor",
    )
    val corDaBorda = animateColorAsState(
        targetValue = colorResource(
            if (ligado) R.color.vidro_accent_text else R.color.vidro_stroke_hairline
        ),
        animationSpec = transicaoDeCor,
        label = "bordaDoInterruptor",
    )
    val corDoBotao = animateColorAsState(
        targetValue = colorResource(
            if (ligado) R.color.vidro_accent_fill else R.color.vidro_text_3
        ),
        animationSpec = transicaoDeCor,
        label = "botaoDoInterruptor",
    )

    Canvas(Modifier.size(TRILHO_LARGURA, TRILHO_ALTURA)) {
        val raio = size.height / 2f
        val borda = larguraDaBorda.toPx()

        drawRoundRect(
            color = corDoTrilho.value,
            cornerRadius = CornerRadius(raio, raio),
        )
        // A borda recua meio traço para dentro: desenhada sobre o limite, metade dela
        // cairia fora do nó e sumiria.
        drawRoundRect(
            color = corDaBorda.value,
            topLeft = Offset(borda / 2f, borda / 2f),
            size = Size(size.width - borda, size.height - borda),
            cornerRadius = CornerRadius(raio, raio),
            style = Stroke(width = borda),
        )

        val raioDoBotao = BOTAO_DIAMETRO.toPx() / 2f
        val folga = BOTAO_FOLGA.toPx()
        val esquerda = folga + raioDoBotao
        val direita = size.width - folga - raioDoBotao
        drawCircle(
            color = corDoBotao.value,
            radius = raioDoBotao,
            center = Offset(esquerda + (direita - esquerda) * avanco.value, size.height / 2f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Escolha — chips quando são poucas, folha quando são muitas
// ─────────────────────────────────────────────────────────────────────────────

/** O rótulo da opção escolhida. Valor fora da lista cai no valor cru, que é melhor que nada. */
private fun rotuloDe(escolha: AjusteUi.Escolha): String {
    val indice = escolha.valores.indexOf(escolha.escolhido)
    return escolha.opcoes.getOrNull(indice) ?: escolha.escolhido
}

/**
 * Até três opções, numa fileira.
 *
 * Larguras iguais por `weight`, e não conteúdo: com "Ligado"/"Desligado" o chip menor
 * pareceria menos escolhível que o maior, e a fileira é um seletor, não dois botões de
 * tamanhos diferentes. O rótulo que não couber quebra dentro do chip — a caixa cresce, o
 * texto não é cortado.
 */
@Composable
private fun FileiraDeChips(escolha: AjusteUi.Escolha, aoEscolher: (String, String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vidro_space_2)),
    ) {
        escolha.valores.forEachIndexed { i, valor ->
            Chip(
                rotulo = escolha.opcoes.getOrNull(i) ?: valor,
                marcado = valor == escolha.escolhido,
                aoTocar = { aoEscolher(escolha.chave, valor) },
            )
        }
    }
}

@Composable
private fun RowScope.Chip(rotulo: String, marcado: Boolean, aoTocar: () -> Unit) {
    val canto = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_s))
    Box(
        Modifier
            .weight(1f)
            .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))
            .clip(canto)
            .cartaoTocavel(aoTocar = aoTocar)
            .background(
                colorResource(
                    if (marcado) R.color.vidro_accent_fill_idle else R.color.vidro_surface_overlay
                )
            )
            .border(
                width = dimensionResource(R.dimen.vidro_stroke_width),
                color = colorResource(
                    if (marcado) R.color.vidro_accent_text else R.color.vidro_stroke_hairline
                ),
                shape = canto,
            )
            .padding(
                horizontal = dimensionResource(R.dimen.vidro_space_2),
                vertical = dimensionResource(R.dimen.vidro_space_2),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = rotulo,
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                color = colorResource(
                    if (marcado) R.color.vidro_accent_text else R.color.vidro_text_2
                ),
            ),
        )
    }
}

/**
 * Mais de três opções, numa folha.
 *
 * `FolhaVidro` já existe, já traz as curvas certas e já trata o alvo mínimo — reescrever
 * isso aqui seria uma segunda folha para o mesmo trabalho.
 *
 * A opção escolhida vai no SUBTÍTULO da folha, e não marcada na lista: `AcaoVidro` não tem
 * estado de seleção, e o único outro campo que pintaria diferente é `destrutiva`, que
 * significa outra coisa. Antes de escolher, o que se precisa saber é o que está escolhido
 * agora — e a linha que abriu a folha ficou atrás do véu.
 */
@Composable
private fun FolhaDeEscolha(
    escolha: AjusteUi.Escolha?,
    aoEscolher: (String, String) -> Unit,
    aoFechar: () -> Unit,
) {
    // O último não-nulo, lembrado: sem isto a folha perderia o texto no meio da animação de
    // saída, porque o estado vira null antes de ela terminar de descer.
    val ultima = remember { mutableStateOf<AjusteUi.Escolha?>(null) }
    if (escolha != null) {
        ultima.value = escolha
    }
    val alvo = ultima.value ?: return

    FolhaVidro(
        titulo = stringResource(alvo.titulo),
        subtitulo = rotuloDe(alvo),
        acoes = alvo.valores.mapIndexed { i, valor ->
            AcaoVidro(rotulo = alvo.opcoes.getOrNull(i) ?: valor) {
                aoEscolher(alvo.chave, valor)
                aoFechar()
            }
        },
        aoFechar = aoFechar,
        visivel = escolha != null,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Deslizante — também desenhado
// ─────────────────────────────────────────────────────────────────────────────

/**
 * O deslizante, com o número ao lado do título.
 *
 * O valor em curso vive AQUI e não no dono da tela: enquanto o dedo arrasta, a pega segue o
 * dedo mesmo que a gravação da preferência demore um quadro a voltar. Solto o dedo, o
 * estado local morre e o valor do dono volta a mandar — é ele quem decide se aceitou.
 */
@Composable
private fun LinhaDeslizante(item: AjusteUi.Deslizante, aoMudar: (String, Int) -> Unit) {
    var arrastado by remember(item.chave) { mutableStateOf<Int?>(null) }
    val mostrado = arrastado ?: item.valor

    LinhaDeAjuste(
        titulo = item.titulo,
        resumo = item.resumo,
        direita = {
            val auto = stringResource(R.string.vidro_seekbar_auto)
            NumeroDoDeslizante(
                rotulo = rotuloDoValor(item, mostrado, auto),
                // A largura é reservada pelo rótulo mais LONGO dos extremos, não pelo
                // atual: com mínimo negativo, "-40" é mais largo que "150". "Automático"
                // entra na conta porque, em português, ele é mais largo que qualquer número.
                maisLargo = listOfNotNull(
                    rotuloDoValor(item, item.minimo, auto),
                    rotuloDoValor(item, item.maximo, auto),
                    if (item.zeroEhAuto) auto else null,
                ).maxByOrNull { it.length } ?: "",
            )
        },
        abaixo = {
            Trilho(
                minimo = item.minimo,
                maximo = item.maximo,
                passo = item.passo,
                valor = mostrado,
                arrastando = arrastado != null,
                aoMover = { novo ->
                    arrastado = novo
                    // Só escreve quando o passo REALMENTE virou. Sem isto um arraste manda
                    // uma gravação de preferência por quadro para reafirmar o mesmo valor.
                    if (novo != item.valor) {
                        aoMudar(item.chave, novo)
                    }
                },
                aoSoltar = { arrastado = null },
            )
        },
    )
}

/**
 * O que a linha escreve para um valor.
 *
 * Duas conversões que o `SeekBarPreference` do XML já fazia e que não podem subir para o
 * modelo, porque lá o número tem de continuar sendo o que vai para o disco: a divisão para a
 * unidade de leitura (kbps guardado, Mbps mostrado) e o zero que quer dizer "automático".
 *
 * A divisão é inteira de propósito. Com passo de 500 kbps, metade dos valores cai em 0,5
 * Mbps e a linha ficaria dançando entre uma e duas casas decimais enquanto o dedo anda —
 * pior de ler do que a casa perdida. Quem precisa do valor exato tem o passo, não o rótulo.
 */
private fun rotuloDoValor(item: AjusteUi.Deslizante, valor: Int, auto: String): String =
    if (item.zeroEhAuto && valor == 0) auto
    else "${valor / item.divisor}${item.sufixo}"

/**
 * O número que corre com o dedo.
 *
 * JetBrains Mono, que é onde o mono ganha o papel dele: dígito tabular impede a linha de
 * dançar enquanto o valor muda. Mas mono só resolve o dígito — a largura ainda salta quando
 * o número passa de 9 para 10. Por isso o rótulo mais longo é desenhado por baixo, invisível
 * e fora da acessibilidade, só para reservar a caixa. Medir texto para isso exigiria um
 * `TextMeasurer`; um fantasma custa uma linha e mede com a mesma fonte, no mesmo tamanho.
 */
@Composable
private fun NumeroDoDeslizante(rotulo: String, maisLargo: String) {
    val estilo = TextStyle(
        fontFamily = FontFamily(Font(R.font.jetbrains_mono_medium)),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.End,
        color = colorResource(R.color.vidro_text),
    )
    Box(contentAlignment = Alignment.CenterEnd) {
        BasicText(
            text = maisLargo,
            modifier = Modifier
                .alpha(0f)
                .clearAndSetSemantics { },
            style = estilo,
        )
        BasicText(text = rotulo, style = estilo)
    }
}

/**
 * O trilho.
 *
 * Desenhado com os mesmos números de `res/drawable/vidro_seek_track.xml` e
 * `vidro_seek_thumb.xml`: trilho de 4 dp com ponta arredondada, percorrido em cor PRÓPRIA
 * (a `SeekBar` tingia o drawable inteiro e dependia do alfa do tema para diferenciar os
 * dois pedaços), e pega de 22 dp com o anel escuro em volta — sem o anel ela sumiria dentro
 * do percorrido, que é da mesma cor.
 *
 * O alvo de toque é a caixa inteira, `vidro_touch_target` de altura: a trilha tem 4 dp, e
 * ninguém acerta 4 dp com o polegar.
 */
@Composable
private fun Trilho(
    minimo: Int,
    maximo: Int,
    passo: Int,
    valor: Int,
    arrastando: Boolean,
    aoMover: (Int) -> Unit,
    aoSoltar: () -> Unit,
) {
    val corDoResto = colorResource(R.color.vidro_stroke)
    val corPercorrida = colorResource(R.color.vidro_accent_fill)
    val corDoAnel = colorResource(R.color.vidro_surface_alt)

    val fracaoAlvo = fracaoDe(valor, minimo, maximo)
    // Sem `by`, e o valor é lido lá dentro do desenho de propósito: assim cada quadro da
    // animação invalida só a fase de DESENHO. Lido aqui fora, ele mandaria a linha inteira
    // recompor a cada quadro para mover uma bolinha.
    // LEVE e não CORPO: o que se move é uma pega de 22 dp.
    //
    // E a mola importa mais aqui do que em qualquer outro lugar desta tela, porque este é o
    // único controle que recebe uma sequência rápida de destinos — um por degrau do passo,
    // enquanto o dedo anda. Com duração, cada degrau reiniciava a interpolação do zero e a
    // pega andava aos trancos; a mola só troca o destino e segue com a velocidade que tem.
    val fracaoSuave = animateFloatAsState(
        targetValue = fracaoAlvo,
        animationSpec = molaLeve(),
        label = "deslizante",
    )

    // O bloco de `pointerInput` sobrevive a muitas recomposições: o que ele fechar por
    // valor fica congelado no que era quando a corotina começou. Só a chave restarta o
    // bloco, e restartar a cada valor novo mataria o arraste em curso — daí o
    // `rememberUpdatedState` para o que muda.
    val valorAgora by rememberUpdatedState(valor)
    val mover by rememberUpdatedState(aoMover)
    val soltar by rememberUpdatedState(aoSoltar)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.vidro_touch_target))
            .pointerInput(minimo, maximo, passo) {
                val raio = PEGA_DIAMETRO.toPx() / 2f
                detectTapGestures { ponto ->
                    val novo = valorEm(ponto.x, size.width.toFloat(), raio, minimo, maximo, passo)
                    if (novo != valorAgora) {
                        mover(novo)
                    }
                    soltar()
                }
            }
            .pointerInput(minimo, maximo, passo) {
                val raio = PEGA_DIAMETRO.toPx() / 2f
                detectHorizontalDragGestures(
                    onDragStart = { ponto ->
                        mover(valorEm(ponto.x, size.width.toFloat(), raio, minimo, maximo, passo))
                    },
                    onDragEnd = { soltar() },
                    onDragCancel = { soltar() },
                ) { mudanca, _ ->
                    // Consumido para a lista vertical não reivindicar o gesto no meio do
                    // arraste horizontal.
                    mudanca.consume()
                    val novo = valorEm(
                        mudanca.position.x, size.width.toFloat(), raio, minimo, maximo, passo
                    )
                    if (novo != valorAgora) {
                        mover(novo)
                    }
                }
            },
    ) {
        val raio = PEGA_DIAMETRO.toPx() / 2f
        val anel = ANEL_DA_PEGA.toPx()
        val espessura = TRILHO_ESPESSURA.toPx()
        val meio = size.height / 2f
        val comeco = raio
        val fim = size.width - raio
        // Arrastando, a pega é o dedo — animar aqui seria colocar 120 ms de atraso entre o
        // dedo e o desenho. Um toque no trilho, ao contrário, é um salto, e saltar sem
        // percurso não deixa ver de onde para onde.
        val fracao = if (arrastando) fracaoAlvo else fracaoSuave.value
        val pega = comeco + (fim - comeco) * fracao

        drawLine(
            color = corDoResto,
            start = Offset(comeco, meio),
            end = Offset(fim, meio),
            strokeWidth = espessura,
            cap = StrokeCap.Round,
        )
        if (pega > comeco) {
            drawLine(
                color = corPercorrida,
                start = Offset(comeco, meio),
                end = Offset(pega, meio),
                strokeWidth = espessura,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = corDoAnel, radius = raio, center = Offset(pega, meio))
        drawCircle(color = corPercorrida, radius = raio - anel, center = Offset(pega, meio))
    }
}

/** Onde o dedo tocou, já reduzido ao passo e preso aos extremos. */
private fun valorEm(
    x: Float,
    largura: Float,
    raio: Float,
    minimo: Int,
    maximo: Int,
    passo: Int,
): Int {
    if (maximo <= minimo) return minimo
    // Os extremos ficam a um raio de pega da borda: a pega desenhada no zero tem o centro
    // aí, e não encostado no canto.
    val comeco = raio
    val fim = largura - raio
    val fracao = if (fim <= comeco) 0f else ((x - comeco) / (fim - comeco)).coerceIn(0f, 1f)
    val degrau = if (passo > 0) passo else 1
    val bruto = minimo + fracao * (maximo - minimo)
    return (minimo + ((bruto - minimo) / degrau).roundToInt() * degrau).coerceIn(minimo, maximo)
}

private fun fracaoDe(valor: Int, minimo: Int, maximo: Int): Float =
    if (maximo <= minimo) 0f
    else ((valor - minimo).toFloat() / (maximo - minimo)).coerceIn(0f, 1f)

// ─────────────────────────────────────────────────────────────────────────────
// Números da tela
// ─────────────────────────────────────────────────────────────────────────────

/**
 * As curvas, traduzidas dos interpoladores da identidade.
 *
 * `tween(N)` SEM `easing` usa o padrão do Compose, que é `FastOutSlowInEasing` —
 * cubic-bezier(.4, 0, .2, 1), literalmente a curva padrão do Material Design. O Material
 * ficou fora das dependências justamente para não entrar por essa porta.
 */
private val CURVA_SOBE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)   // res/interpolator/vidro_sheet.xml
private val CURVA_CHEGA = CubicBezierEasing(0.22f, 0.72f, 0f, 1f)  // res/interpolator/vidro_enter.xml

/** Teto da coluna de leitura, o mesmo das outras telas migradas. */
private val TETO_DE_LEITURA = 720.dp

/**
 * Quantas opções ainda cabem à mostra.
 *
 * Acima disto a fileira vira uma tira de chips estreitos demais para o rótulo, e a folha
 * passa a ser o lugar honesto — ela empilha e dá a cada opção a linha inteira.
 */
private const val CHIPS_ATE = 3

/**
 * O interruptor.
 *
 * A altura do trilho é escolhida contra a ENTRELINHA do título, não contra o gosto: a
 * linha do título mede 21 sp, e todo controle desta tela alinha pelo topo. Um trilho de
 * 30 dp ficaria com o centro 4,5 dp abaixo do centro da palavra que o nomeia — perto o
 * bastante para não parecer intencional e longe o bastante para se ver. Com 26 dp a
 * diferença cai para 2,5 dp e some.
 */
private val TRILHO_LARGURA = 52.dp
private val TRILHO_ALTURA = 26.dp
private val BOTAO_DIAMETRO = 20.dp
private val BOTAO_FOLGA = 3.dp

/** Os números de `vidro_seek_thumb.xml` e `vidro_seek_track.xml`. */
private val PEGA_DIAMETRO = 22.dp
private val ANEL_DA_PEGA = 3.dp
private val TRILHO_ESPESSURA = 4.dp

/** O vetor de avançar é desenhado a 24 dp; redimensioná-lo só borraria o traço. */
private val SETA = 24.dp
