package com.limelight.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R

/**
 * A tela do endereço — e o P03 junto.
 *
 * Substitui `activity_add_computer_manually.xml`. Mas a mudança que importa não é a stack:
 * é **onde o erro aparece**.
 *
 * Antes, digitar um IP errado abria um `AlertDialog` com o título "Connection Error" — a
 * palavra que o handoff proíbe —, uma frase genérica sobre firewall, e um botão "Ajuda" que
 * jogava a pessoa na wiki do Moonlight, fora do app, no meio de adicionar o primeiro PC. E
 * a frase sobre firewall nem se aplica contra um endereço privado, porque `NetworkScope` já
 * sabe que é rede local e nem roda o teste de portas.
 *
 * Aqui a causa aparece **abaixo do campo**, com o texto preservado e o teclado de pé: corrigir
 * uma letra deixa de custar fechar um diálogo e reencontrar onde você estava.
 */
@Composable
fun TelaEndereco(
    texto: String,
    aoDigitar: (String) -> Unit,
    /** A causa da última tentativa, ou 0 quando não houve. */
    erro: Int,
    ocupado: Boolean,
    /** Endereços já tentados nesta instalação, para o segundo aparelho não redigitar. */
    tentados: List<String>,
    aoVoltar: () -> Unit,
    aoConectar: () -> Unit,
    aoEscolherTentado: (String) -> Unit,
) {
    val teclado = LocalSoftwareKeyboardController.current
    val foco = remember { FocusRequester() }

    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_void))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                // O teclado empurra, não cobre. É a mesma regra da sessão, e aqui ela é
                // ainda mais óbvia: a tela inteira é um campo de texto.
                .imePadding()
                .widthIn(max = 720.dp)
                .padding(horizontal = dimensionResource(R.dimen.vidro_inset_card)),
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

            BasicText(
                text = stringResource(R.string.vidro_return),
                modifier = Modifier.cartaoTocavel(aoTocar = aoVoltar),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_medium)),
                    fontSize = 15.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

            BasicText(
                text = stringResource(R.string.title_add_pc),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                    fontSize = 29.sp,
                    letterSpacing = (-0.035).emKt(),
                    color = colorResource(R.color.vidro_text),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))

            BasicText(
                text = stringResource(R.string.vidro_address_help),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_medium)),
                    fontSize = 13.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

            CampoDeEndereco(texto, aoDigitar, erro != 0, foco) {
                teclado?.hide()
                aoConectar()
            }

            // A CAUSA, DENTRO DA TELA.
            //
            // Ela chega DESLIZANDO e não CRESCENDO, e a diferença não é de gosto: crescer é
            // animar altura de layout, que é a primeira coisa que a regra de movimento
            // proíbe. Altura re-mede e re-posiciona a árvore inteira a cada quadro; deslocar
            // é transform, e transform não mede nada.
            //
            // O que se queria com o crescimento — a linha ser lida como consequência do que
            // acabou de acontecer, e não como algo que sempre esteve ali — sobrevive: o
            // espaço dela é reservado (a `Column` já o contém) e ela entra por cima, subindo
            // um terço da própria altura. O olho lê a chegada do mesmo jeito.
            AnimatedVisibility(
                visible = erro != 0,
                enter = slideInVertically(molaCorpo()) { -it / 3 } + fadeIn(molaCorpo()),
                exit = slideOutVertically(molaCorpo()) { -it / 3 } + fadeOut(molaCorpo()),
            ) {
                Column {
                    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
                    BasicText(
                        text = if (erro != 0) stringResource(erro) else "",
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.manrope_medium)),
                            fontSize = 13.sp,
                            color = colorResource(R.color.vidro_alerta),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

            BotaoDeCompromisso(
                texto = stringResource(
                    if (ocupado) R.string.vidro_connecting_short else R.string.vidro_connect
                ),
                aoTocar = {
                    teclado?.hide()
                    aoConectar()
                },
            )

            // Os endereços já tentados.
            //
            // O vazio enorme entre o campo e o rodapé foi marcado nas anotações. Ele ganha o
            // que estava faltando: o segundo aparelho não redigita o que o primeiro já
            // descobriu. Três, os últimos, e nada além do texto — não é histórico, é atalho.
            if (tentados.isNotEmpty()) {
                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))
                BasicText(
                    text = stringResource(R.string.vidro_address_tried).uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.manrope_bold)),
                        fontSize = 12.sp,
                        letterSpacing = 0.10.emKt(),
                        color = colorResource(R.color.vidro_text_3),
                    ),
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
                tentados.forEach { endereco ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_m)))
                            .cartaoTocavel(aoTocar = { aoEscolherTentado(endereco) })
                            .padding(
                                horizontal = dimensionResource(R.dimen.vidro_space_3),
                                vertical = dimensionResource(R.dimen.vidro_space_2),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = endereco,
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.jetbrains_mono_regular)),
                                fontSize = 14.sp,
                                color = colorResource(R.color.vidro_text_2),
                            ),
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_1)))
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CampoDeEndereco(
    texto: String,
    aoDigitar: (String) -> Unit,
    comErro: Boolean,
    foco: FocusRequester,
    aoConfirmar: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)))
            .background(colorResource(R.color.vidro_surface_overlay))
            .border(
                width = dimensionResource(R.dimen.vidro_stroke_width),
                // A borda muda de tom quando há erro: o campo é o lugar do problema, e
                // dizer isso na moldura dele custa zero pixel de texto.
                color = colorResource(
                    if (comErro) R.color.vidro_alerta else R.color.vidro_stroke_hairline
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)),
            )
            .padding(horizontal = 15.dp, vertical = 16.dp),
    ) {
        BasicTextField(
            value = texto,
            onValueChange = aoDigitar,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(foco),
            singleLine = true,
            // Monoespaçada: endereço é dado, não prosa, e o zero precisa se distinguir do O.
            textStyle = TextStyle(
                fontFamily = FontFamily(Font(R.font.jetbrains_mono_regular)),
                fontSize = 16.sp,
                color = colorResource(R.color.vidro_text),
            ),
            cursorBrush = SolidColor(colorResource(R.color.vidro_accent_text)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { aoConfirmar() }),
            decorationBox = { campo ->
                if (texto.isEmpty()) {
                    BasicText(
                        text = stringResource(R.string.vidro_address_hint),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.jetbrains_mono_regular)),
                            fontSize = 16.sp,
                            color = colorResource(R.color.vidro_text_3),
                        ),
                    )
                }
                campo()
            },
        )
    }
}
