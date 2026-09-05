package com.limelight.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.limelight.R

/**
 * A tela de depuração — e o P21 junto.
 *
 * Substitui `activity_axitest.xml`. Mas o que muda não é a stack: é **para que a tela
 * serve**.
 *
 * Antes eram quatro botões de teste — "Test Device Vibration", "Refresh gamepad list",
 * "Test gamepad rumble", "Vibration Amplitude (220)" — aparecendo com o texto cortado na
 * captura, mais um despejo em inglês cru. Testar vibração é papel do slider de vibração,
 * não de um painel separado.
 *
 * Aqui ela é o que uma tela de depuração precisa ser: **leitura, e um botão Copiar.** Quando
 * ele precisar pedir ajuda, o que importa é conseguir colar isto em algum lugar.
 *
 * O bloco de dispositivos de entrada fica, recolhido: foi ele que permitiu descobrir que a
 * capa do S8 Ultra se anuncia deitada, e vai junto no Copiar.
 */
@Composable
fun TelaDepuracao(
    resumo: String,
    dispositivos: String,
    aoVoltar: () -> Unit,
    aoCopiar: () -> Unit,
) {
    var copiado by remember { mutableStateOf(false) }
    var mostrarDispositivos by remember { mutableStateOf(false) }

    // O "copiado" some sozinho. Um recibo que fica na tela vira rótulo, e rótulo permanente
    // deixa de ser recibo.
    LaunchedEffect(copiado) {
        if (copiado) {
            kotlinx.coroutines.delay(1800)
            copiado = false
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
                .widthIn(max = 720.dp)
                .padding(horizontal = dimensionResource(R.dimen.vidro_inset_card))
                .verticalScroll(rememberScrollState()),
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
                text = stringResource(R.string.vidro_debug_title),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                    fontSize = 29.sp,
                    letterSpacing = (-0.035).emKt(),
                    color = colorResource(R.color.vidro_text),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

            Bloco(resumo)

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_3)))

            // Os dispositivos de entrada, recolhidos.
            //
            // É a parte longa e a que quase nunca se lê — mas é a que resolveu o mistério do
            // trackpad da capa. Recolhida por padrão, e sempre presente no Copiar.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_m)))
                    .cartaoTocavel(aoTocar = { mostrarDispositivos = !mostrarDispositivos })
                    .padding(dimensionResource(R.dimen.vidro_space_3)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(
                        if (mostrarDispositivos) R.string.vidro_debug_hide_devices
                        else R.string.vidro_debug_show_devices
                    ),
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                        fontSize = 14.sp,
                        color = colorResource(R.color.vidro_text),
                    ),
                )
            }

            AnimatedVisibility(
                visible = mostrarDispositivos,
                enter = fadeIn(molaCorpo()),
                exit = fadeOut(molaCorpo()),
            ) {
                Bloco(dispositivos)
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

            BotaoDeCompromisso(
                texto = stringResource(
                    if (copiado) R.string.vidro_debug_copied else R.string.vidro_debug_copy
                ),
                aoTocar = {
                    aoCopiar()
                    copiado = true
                },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))
        }
    }
}

/** Um bloco de leitura, em monoespaçada — isto é dado, não prosa. */
@Composable
private fun Bloco(texto: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)))
            .background(colorResource(R.color.vidro_surface_overlay))
            .border(
                width = dimensionResource(R.dimen.vidro_stroke_width),
                color = colorResource(R.color.vidro_stroke_hairline),
                shape = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)),
            )
            .padding(15.dp),
    ) {
        BasicText(
            text = texto,
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.jetbrains_mono_regular)),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colorResource(R.color.vidro_text_2),
            ),
        )
    }
}
