package com.limelight.ui.compose

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.limelight.R
import com.limelight.ui.VidroLightView

/** O que o catálogo mostra sobre o host enquanto ele está aberto. */
enum class EstadoDoHostNoCatalogo { NORMAL, DORMINDO, SEM_PAREAMENTO }

/**
 * O catálogo de um host.
 *
 * Substitui `activity_app_view.xml` e as duas variantes de tablet escritas ontem. Era a tela
 * que o dono marcou como "toda zoada": no Tab deitado ela era o desenho de celular esticado
 * por 1722 dp, com o "voltar" num extremo e o "recarregar" no outro.
 *
 * **A grade continua em XML de propósito.** Cada célula é o `app_grid_item.xml` inflado pelo
 * adapter que já existe, dentro de um `AndroidView`. Não é preguiça: ali mora o
 * `CachedAppAssetLoader` com cache em disco, reamostragem por densidade, cancelamento de
 * carga e o `VidroPosterBox` que trava a proporção da arte — a peça que atende ao "NUNCA
 * deveria quebrar o aspect ratio das imagens". Reescrever isso durante uma migração de UI
 * seria trocar código medido contra um host de verdade por código novo sem medição nenhuma.
 * O Compose cuida da moldura e do arranjo; a célula fica onde está até ter motivo próprio
 * para mudar.
 */
@Composable
fun TelaCatalogo(
    nomeDoHost: String,
    /**
     * O uuid do lugar, para o nome poder viajar da lista até aqui.
     *
     * Nulo quando a tela é desenhada sozinha — pelas capturas e pelos testes —, e aí ela é a
     * mesma tela de sempre, sem viagem nenhuma.
     */
    uuidDoHost: String? = null,
    quantos: Int,
    recarregando: Boolean,
    /** O app aberto no host, ou `null`. */
    sessaoAberta: String?,
    estadoDoHost: EstadoDoHostNoCatalogo,
    aoVoltar: () -> Unit,
    aoRecarregar: () -> Unit,
    aoRetomar: () -> Unit,
    aoAcordar: () -> Unit,
    aoParear: () -> Unit,
    /** Quantos itens a grade tem, e como desenhar cada um. */
    itens: Int,
    celula: @Composable (Int) -> Unit,
    aoTocarItem: (Int) -> Unit,
    aoSegurarItem: (Int) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_void))
    ) {
        AndroidView(
            factory = { ctx -> VidroLightView(ctx) },
            update = {
                it.setIntensity(
                    if (estadoDoHost == EstadoDoHostNoCatalogo.NORMAL) VidroLightView.INTENSITY_AWAKE
                    else VidroLightView.INTENSITY_ASLEEP,
                    true,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.TopCenter),
        )

        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            val deitadoLargo = maxWidth >= 900.dp
            // A largura de UMA capa. Em vez de fixar a contagem de colunas por pasta de
            // recurso — quatro em sw600dp, seis em sw720dp — a grade divide o espaço real
            // pelo tamanho que a capa precisa ter. O tablet de 8" deitado e o Tab Ultra
            // deixam de precisar de números diferentes escritos em arquivos diferentes.
            // A capa CRESCE com a tela.
            //
            // 200dp num tablet de 1722dp de largura da oito colunas de miniatura, e a
            // captura mostrava as capas parecendo icones perdidos. A arte e o conteudo desta
            // tela; ela merece o tamanho. Em telas estreitas 160dp continua sendo o que cabe
            // sem virar uma coluna so.
            val larguraDaCapa = when {
                maxWidth >= 1200.dp -> 260.dp
                maxWidth >= 900.dp -> 220.dp
                else -> 160.dp
            }

            // A MOLDURA FICA NO TOPO NOS DOIS TAMANHOS.
            //
            // Houve uma versao em duas colunas aqui, copiada da home. Na home a coluna de
            // 420dp se justifica: ela carrega saudacao, titulo, "Continuar" e o rodape. O
            // catalogo tem SO um cabecalho -- a mesma coluna deixava metade da tela vazia a
            // esquerda e espremia a grade na outra metade, com as capas encolhidas num canto.
            //
            // Aqui o conteudo e a grade, e conteudo fica com a largura inteira. O que muda
            // com o tamanho e o TAMANHO DA CAPA, nao o numero de colunas nem a moldura.
            Column(Modifier.fillMaxSize()) {
                // A MOLDURA TEM TETO DE LARGURA. A grade nao.
                //
                // Sem teto, o Row do cabecalho espalha ate as bordas: num Tab de 1722dp a
                // contagem ficava num extremo e "Recarregar" no outro, e a faixa de sessao
                // atravessava a tela com o rotulo de um lado e a acao do outro. Era o
                // defeito que esta tela veio consertar, reaparecendo em outra forma.
                //
                // 720dp e a largura de leitura que a home e as cenas ja usam. A GRADE fica
                // de fora do teto de proposito: ela e conteudo, e conteudo legitimamente
                // ocupa a tela.
                Column(
                    Modifier
                        .widthIn(max = 720.dp)
                        .padding(
                            horizontal = if (deitadoLargo) dimensionResource(R.dimen.vidro_space_6)
                                         else dimensionResource(R.dimen.vidro_inset_card)
                        )
                ) {
                    Moldura(nomeDoHost, uuidDoHost, quantos, recarregando, aoVoltar, aoRecarregar)
                    Faixa(sessaoAberta, estadoDoHost, aoRetomar, aoAcordar, aoParear)
                }
                Grade(itens, celula, aoTocarItem, aoSegurarItem, larguraDaCapa, estadoDoHost,
                    Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Moldura(
    nomeDoHost: String,
    uuidDoHost: String?,
    quantos: Int,
    recarregando: Boolean,
    aoVoltar: () -> Unit,
    aoRecarregar: () -> Unit,
) {
    Column(Modifier.padding(top = dimensionResource(R.dimen.vidro_space_4))) {
        BasicText(
            text = stringResource(R.string.vidro_back_places),
            modifier = Modifier.cartaoTocavel(aoTocar = { aoVoltar() }),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_medium)),
                fontSize = 15.sp,
                color = colorResource(R.color.vidro_text_3),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_1)))
        BasicText(
            text = nomeDoHost,
            // O DESTINO DA VIAGEM. O mesmo elemento, maior.
            modifier = if (uuidDoHost == null) Modifier
            else Modifier.viaja(chaveDeViagem(uuidDoHost)),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                fontSize = 29.sp,
                letterSpacing = (-0.035).emKt(),
                color = colorResource(R.color.vidro_text),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // A contagem existia no layout e ninguém escrevia nela: a tela abria com um
            // espaço em branco onde deveria estar "6 aplicativos publicados pelo host".
            BasicText(
                text = if (quantos == 1) stringResource(R.string.vidro_published_app_one)
                       else stringResource(R.string.vidro_published_apps, quantos),
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_medium)),
                    fontSize = 13.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )
            BasicText(
                text = stringResource(
                    if (recarregando) R.string.vidro_reloading else R.string.vidro_reload
                ),
                modifier = Modifier
                    .cartaoTocavel(aoTocar = aoRecarregar, habilitado = !recarregando)
                    // 48 dp de ALVO, medido a 57,5x45,7 dp — curto na altura. É o mesmo
                    // caso do "Voltar" em Cenas: texto de 14 sp não preenche 48 dp sozinho.
                    .defaultMinSize(minHeight = dimensionResource(R.dimen.vidro_touch_target))
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(start = dimensionResource(R.dimen.vidro_space_3)),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                    fontSize = 14.sp,
                    color = colorResource(
                        // Ação inline em tinta, não em acento: azul quer dizer "dormindo"
                        // na lista de lugares, e a mesma cor não pode dizer "toque aqui" duas
                        // telas adiante. Quem responde ao dedo é a mola de `cartaoTocavel`.
                        if (recarregando) R.color.vidro_text_3 else R.color.vidro_text
                    ),
                ),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_3)))
    }
}

/**
 * A faixa do topo: sessão aberta, ou o estado do host — o P04.
 *
 * Antes, quando o host dormia enquanto você escolhia o jogo, o `AppView` chamava `finish()`
 * e jogava você de volta na home com uma torrada de dois segundos: perdia-se o rolamento e
 * não dava para saber se você tinha feito algo errado. Agora a tela fica, a grade esmaece, e
 * a faixa carrega o estado — com a saída ao lado.
 */
@Composable
private fun Faixa(
    sessaoAberta: String?,
    estadoDoHost: EstadoDoHostNoCatalogo,
    aoRetomar: () -> Unit,
    aoAcordar: () -> Unit,
    aoParear: () -> Unit,
) {
    val texto: String
    val acao: String
    val aoTocar: () -> Unit

    when {
        estadoDoHost == EstadoDoHostNoCatalogo.DORMINDO -> {
            texto = stringResource(R.string.vidro_catalog_host_asleep)
            acao = stringResource(R.string.vidro_wake_pc)
            aoTocar = aoAcordar
        }
        estadoDoHost == EstadoDoHostNoCatalogo.SEM_PAREAMENTO -> {
            texto = stringResource(R.string.vidro_catalog_unpaired)
            acao = stringResource(R.string.vidro_action_pair)
            aoTocar = aoParear
        }
        sessaoAberta != null -> {
            texto = stringResource(R.string.vidro_session_open, sessaoAberta)
            acao = stringResource(R.string.vidro_session_return)
            aoTocar = aoRetomar
        }
        else -> return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.vidro_space_3))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)))
            .background(colorResource(R.color.vidro_glass_fill_session))
            .border(
                width = dimensionResource(R.dimen.vidro_stroke_width),
                color = colorResource(R.color.vidro_stroke_glass),
                shape = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)),
            )
            .cartaoTocavel(aoTocar = { aoTocar() })
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = texto,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 14.sp,
                letterSpacing = (-0.02).emKt(),
                color = colorResource(R.color.vidro_text),
            ),
        )
        BasicText(
            text = acao,
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 14.sp,
                // A faixa de sessão aberta é um ESTADO da máquina — algo está rodando
                // nela agora — e estado quente é o que a identidade já diz.
                color = colorResource(R.color.vidro_quente),
            ),
        )
    }
}

@Composable
private fun Grade(
    itens: Int,
    celula: @Composable (Int) -> Unit,
    aoTocarItem: (Int) -> Unit,
    aoSegurarItem: (Int) -> Unit,
    larguraDaCapa: androidx.compose.ui.unit.Dp,
    estadoDoHost: EstadoDoHostNoCatalogo,
    modifier: Modifier = Modifier,
) {
    // Com o host fora do ar a grade esmaece e o toque fica INERTE: senão a pessoa dispara
    // uma conexão contra um host que não está lá.
    val vivo = estadoDoHost == EstadoDoHostNoCatalogo.NORMAL

    LazyVerticalGrid(
        columns = GridCells.Adaptive(larguraDaCapa),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.vidro_inset_card),
            end = dimensionResource(R.dimen.vidro_inset_card),
            // Respiro no topo: sem ele a primeira fileira de capas encostava na moldura, e
            // no tablet ela encostava na propria borda da tela.
            top = dimensionResource(R.dimen.vidro_space_3),
            bottom = dimensionResource(R.dimen.vidro_space_5),
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vidro_space_3)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vidro_space_4)),
    ) {
        items(itens) { i ->
            Box(
                Modifier
                    // A CELULA TEM TAMANHO PROPRIO, e nao o tamanho da arte dela.
                    //
                    // Sem isto cada celula media o que a View do adaptador media, e artes de
                    // proporcoes diferentes davam alturas diferentes na MESMA fileira -- na
                    // gravacao, DESKTOP saiu com ~120 px ao lado de REMOTE INPUT com ~220.
                    // Uma grade cujas linhas nao alinham nao le como grade.
                    //
                    // 3:4 e a proporcao da capa padrao do protocolo (300x400), entao esta e a
                    // caixa que a arte esperava desde sempre.
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_m)))
                    .background(colorResource(R.color.vidro_surface))
                    .cartaoTocavel(
                        aoTocar = { aoTocarItem(i) },
                        aoSegurar = { aoSegurarItem(i) },
                        habilitado = vivo,
                    )
                    .alphaSeMorto(vivo)
            ) {
                celula(i)
            }
        }
    }
}
