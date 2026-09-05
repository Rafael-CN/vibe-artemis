package com.limelight.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R

/** Uma cena, do jeito que a tela precisa dela. */
data class CenaUi(
    val id: String,
    val nome: String,
    val quando: String,
    val ativa: Boolean,
)

/**
 * A tela de Cenas.
 *
 * Substitui `activity_profiles.xml` e as suas duas variantes de tablet, mais o
 * `ProfilesAdapter` sobre `RecyclerView` com `GridLayoutManager`. A contagem de colunas era
 * um `R.integer` por pasta de recurso; agora ela sai da largura real, que é o que ela sempre
 * quis dizer.
 *
 * O movimento aqui não é enfeite: **as cenas entram deslizando de baixo, uma de cada vez.**
 * Uma cena que aparece do nada parece que sempre esteve lá; entrando, ela se anuncia como
 * coisa nova — e é exatamente isso que acontece quando alguém acaba de salvar uma.
 */
@Composable
fun TelaCenas(
    cenas: List<CenaUi>,
    aoVoltar: () -> Unit,
    aoAbrir: (CenaUi) -> Unit,
    aoEditar: (CenaUi) -> Unit,
    aoApagar: (CenaUi) -> Unit,
    aoCriar: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_void))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            val largo = maxWidth >= 700.dp

            Column(Modifier.fillMaxSize()) {
                // A moldura tem teto de leitura; a lista não. Mesma regra do catálogo, pelo
                // mesmo motivo: sem teto, num tablet deitado o título fica num extremo da
                // tela e o texto de apoio atravessa 1722 dp.
                Column(
                    Modifier
                        .widthIn(max = 720.dp)
                        .padding(
                            horizontal = if (largo) dimensionResource(R.dimen.vidro_space_6)
                                         else dimensionResource(R.dimen.vidro_inset_card),
                        )
                        .padding(top = dimensionResource(R.dimen.vidro_space_4)),
                ) {
                    BasicText(
                        text = stringResource(R.string.vidro_return),
                        // 48 dp de ALVO. Medido a 48,4x38,5 dp com uiautomator: o mesmo
                        // "Voltar" das outras telas sai a 48 e só este saía curto, porque
                        // aqui ele não herda o mesmo respiro vertical. Texto de 14 sp não
                        // preenche 48 dp sozinho — quem garante é o mínimo.
                        modifier = Modifier
                            .cartaoTocavel(aoTocar = { aoVoltar() })
                            .defaultMinSize(
                                minHeight = dimensionResource(R.dimen.vidro_touch_target))
                            .wrapContentHeight(Alignment.CenterVertically),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.manrope_medium)),
                            fontSize = 15.sp,
                            color = colorResource(R.color.vidro_text_3),
                        ),
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_1)))
                    BasicText(
                        text = stringResource(R.string.vidro_scenes),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                            fontSize = 29.sp,
                            letterSpacing = (-0.035).emKt(),
                            color = colorResource(R.color.vidro_text),
                        ),
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
                    BasicText(
                        text = stringResource(R.string.vidro_scenes_sub),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.manrope_medium)),
                            fontSize = 13.sp,
                            color = colorResource(R.color.vidro_text_3),
                        ),
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))
                }

                Box(Modifier.weight(1f)) {
                    if (cenas.isEmpty()) {
                        CenasVazio()
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(320.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = dimensionResource(R.dimen.vidro_inset_card),
                                end = dimensionResource(R.dimen.vidro_inset_card),
                                bottom = dimensionResource(R.dimen.vidro_space_5),
                            ),
                            horizontalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.vidro_space_2)),
                            verticalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.vidro_space_2)),
                        ) {
                            items(cenas, key = { it.id }) { cena ->
                                // Anima aparecer, sumir e MUDAR DE LUGAR. O terceiro e o
                                // que importa: a lista reordena quando uma cena vira ativa,
                                // e sem isto os cards saltam de posicao sem explicacao.
                                CartaoDeCena(
                                    cena, aoAbrir, aoEditar, aoApagar,
                                    Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                BotaoDeCompromisso(
                    texto = stringResource(R.string.vidro_scene_save),
                    aoTocar = aoCriar,
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(R.dimen.vidro_inset_card),
                            vertical = dimensionResource(R.dimen.vidro_space_3),
                        )
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun CartaoDeCena(
    cena: CenaUi,
    aoAbrir: (CenaUi) -> Unit,
    aoEditar: (CenaUi) -> Unit,
    aoApagar: (CenaUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    // NADA de AnimatedVisibility aqui.
    //
    // Havia um `AnimatedVisibility(visible = true)` envolvendo este card. Com o alvo
    // constante ele nunca transiciona — era animação morta —, e a camada que ele insere
    // engolia o toque: o teste "tocar numa cena troca a ativa" falhava porque o clique não
    // chegava ao card.
    //
    // A entrada de item de lista tem API própria: `Modifier.animateItem()`, aplicada no
    // escopo do LazyGrid, que anima aparecer, sumir e MUDAR DE LUGAR — o que importa aqui,
    // porque a lista reordena quando uma cena vira ativa.
    Row(
            Modifier
                .fillMaxWidth()
                .cartaoTocavel(aoTocar = { aoAbrir(cena) })
                .clip(RoundedCornerShape(19.dp))
                .background(colorResource(R.color.vidro_surface_overlay))
                .border(
                    width = dimensionResource(R.dimen.vidro_stroke_width),
                    // A cena ativa se distingue pela BORDA, não por um badge extra: o card
                    // já é pequeno e um selo a mais competiria com o nome.
                    color = colorResource(
                        // A cena ATIVA e a que esta em efeito agora: quente, o mesmo
                        // que a identidade ja usa para "presente". Azul saiu porque na
                        // lista de lugares ele quer dizer "dormindo".
                        if (cena.ativa) R.color.vidro_quente
                        else R.color.vidro_stroke_hairline
                    ),
                    shape = RoundedCornerShape(19.dp),
                )
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = cena.nome,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                        fontSize = 16.sp,
                        letterSpacing = (-0.02).emKt(),
                        color = colorResource(R.color.vidro_text),
                    ),
                )
                BasicText(
                    text = if (cena.ativa) stringResource(R.string.vidro_scene_active) else cena.quando,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.manrope_medium)),
                        fontSize = 12.sp,
                        color = colorResource(
                            if (cena.ativa) R.color.vidro_quente else R.color.vidro_text_3
                        ),
                    ),
                )
            }

            // Editar e apagar como texto, não como ícone.
            //
            // Eram dois ícones de 24dp no card antigo, abaixo do alvo mínimo de 48dp e sem
            // rótulo. Aqui são alvos de texto com área de toque de verdade, e a palavra diz
            // o que acontece — "apagar" não pode depender de reconhecer uma lixeira.
            BasicText(
                text = stringResource(R.string.vidro_scene_edit),
                modifier = Modifier
                    .cartaoTocavel(aoTocar = { aoEditar(cena) })
                    .padding(dimensionResource(R.dimen.vidro_space_3)),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                    fontSize = 13.sp,
                    color = colorResource(R.color.vidro_text_2),
                ),
            )
            BasicText(
                text = stringResource(R.string.vidro_scene_delete),
                modifier = Modifier
                    .cartaoTocavel(aoTocar = { aoApagar(cena) })
                    .padding(dimensionResource(R.dimen.vidro_space_3)),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                    fontSize = 13.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )
    }
}

@Composable
private fun CenasVazio() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.vidro_space_6)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = stringResource(R.string.vidro_scenes_empty_title),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 18.sp,
                color = colorResource(R.color.vidro_text),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
        BasicText(
            text = stringResource(R.string.vidro_scenes_empty_sub),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_medium)),
                fontSize = 14.sp,
                color = colorResource(R.color.vidro_text_3),
            ),
        )
    }
}

/**
 * O botão cheio de compromisso, no rodapé.
 *
 * Era um FAB circular flutuando por cima dos cards — gramática do Material, que esta
 * identidade não usa. A ação de compromisso mora no rodapé, com largura inteira, onde o
 * polegar alcança e onde ela não cobre conteúdo.
 */
@Composable
fun BotaoDeCompromisso(texto: String, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .cartaoTocavel(aoTocar = aoTocar)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)))
            // O botao primario e um DEGRAU DE CHAO acima do resto, nao uma mancha de cor.
            // Ver os tokens vidro_botao em colors.xml.
            .background(colorResource(R.color.vidro_botao))
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_bold)),
                fontSize = 15.sp,
                letterSpacing = (-0.01).emKt(),
                color = colorResource(R.color.vidro_void),
            ),
        )
    }
}
