package com.limelight.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R

/**
 * Onde o pareamento está.
 *
 * O protótipo desenha três passos e o app entregava **um**: os quatro dígitos, sem "PASSO 2
 * DE 3" em cima, sem o passo de procura antes, e sem o passo que confirma depois. E quando o
 * código não era aceito — o caso comum, porque ele expira — a tela do código era derrubada,
 * uma torrada de quatro segundos aparecia, e a pessoa acordava na home tendo de reencontrar
 * o cartão do host para recomeçar.
 */
enum class PassoDoPareamento { PROCURANDO, CODIGO, RECUSADO, PRONTO }

/**
 * A tela de pareamento — o P07 e o P02 juntos.
 *
 * Substitui `vidro_pairing.xml` e as suas variantes. As oito strings que o protótipo já
 * previa e ninguém usava passam a existir: `vidro_step_of`, `vidro_code_refused`,
 * `vidro_code_expired`, `vidro_where_to_look`, `vidro_ask_new_code`, `vidro_pair_done`,
 * `vidro_pair_done_sub` e `vidro_pair_see_apps`.
 *
 * **A recusa não derruba a tela** (P02). O mesmo painel troca de estado: os dígitos apagam,
 * entra o título da recusa, a causa mais provável primeiro — expirou —, e "Pedir um código
 * novo" refaz o pedido sem sair dali.
 */
@Composable
fun TelaPareamento(
    passo: PassoDoPareamento,
    nomeDoHost: String,
    /** Os quatro dígitos, ou vazio no fluxo de senha, que não mostra código. */
    codigo: String,
    /**
     * O endereço da página do host, ou `null`.
     *
     * O P08: no Vibeshine o código NÃO aparece na tela do PC — ele vai na página web do
     * host. A tela dizia "The host will ask for this code on the PC screen", que é verdade
     * no Apollo e mentira aqui, e a única forma de chegar à página era sair do pareamento e
     * achar um item escondido num menu de toque longo.
     */
    paginaDoHost: String?,
    aoCancelar: () -> Unit,
    aoPedirNovo: () -> Unit,
    aoAbrirPagina: () -> Unit,
    aoVerApps: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_scrim_cor)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .padding(dimensionResource(R.dimen.vidro_inset_card))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_xl)))
                .background(colorResource(R.color.vidro_glass_fill_session))
                .border(
                    width = dimensionResource(R.dimen.vidro_stroke_width),
                    color = colorResource(R.color.vidro_stroke_glass),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_xl)),
                )
                .padding(dimensionResource(R.dimen.vidro_space_5)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // O passo, que é a única coisa fixa desta tela.
            //
            // Saber que existem três e em qual você está é o que transforma espera em
            // progresso. A recusa não é um passo: ela é o passo 2 dando errado.
            BasicText(
                text = stringResource(
                    R.string.vidro_step_of,
                    when (passo) {
                        PassoDoPareamento.PROCURANDO -> 1
                        PassoDoPareamento.CODIGO, PassoDoPareamento.RECUSADO -> 2
                        PassoDoPareamento.PRONTO -> 3
                    },
                ).uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_bold)),
                    fontSize = 12.sp,
                    letterSpacing = 0.10.emKt(),
                    color = colorResource(R.color.vidro_text_3),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))

            // Os passos trocam deslizando, não piscando: o movimento é o que diz que se
            // avançou, e é o mesmo tempo de folha da tabela do handoff.
            AnimatedContent(
                targetState = passo,
                transitionSpec = {
                    (slideInVertically(molaCorpo()) { it / 3 }
                        + fadeIn(molaCorpo())) togetherWith fadeOut(molaCorpo())
                },
                label = "passoDoPareamento",
            ) { atual ->
                when (atual) {
                    PassoDoPareamento.PROCURANDO -> Procurando(nomeDoHost)
                    PassoDoPareamento.CODIGO -> Codigo(codigo, nomeDoHost, paginaDoHost != null)
                    PassoDoPareamento.RECUSADO -> Recusado(nomeDoHost)
                    PassoDoPareamento.PRONTO -> Pronto(nomeDoHost)
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

            when (passo) {
                PassoDoPareamento.PRONTO -> {
                    BotaoDeCompromisso(stringResource(R.string.vidro_pair_see_apps), aoVerApps)
                }
                PassoDoPareamento.RECUSADO -> {
                    BotaoDeCompromisso(stringResource(R.string.vidro_ask_new_code), aoPedirNovo)
                    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
                    Fantasma(stringResource(R.string.vidro_leave), aoCancelar)
                }
                PassoDoPareamento.CODIGO -> {
                    if (paginaDoHost != null) {
                        BotaoDeCompromisso(stringResource(R.string.vidro_open_host_page), aoAbrirPagina)
                        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
                    }
                    Fantasma(stringResource(R.string.vidro_leave), aoCancelar)
                }
                PassoDoPareamento.PROCURANDO -> {
                    Fantasma(stringResource(R.string.vidro_leave), aoCancelar)
                }
            }
        }
    }
}

private val SOBE_PAREAMENTO = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@Composable
private fun Procurando(nomeDoHost: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Titulo(stringResource(R.string.vidro_pairing_reaching, nomeDoHost))
    }
}

@Composable
private fun Codigo(codigo: String, nomeDoHost: String, temPagina: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (codigo.length >= 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vidro_space_2))) {
                codigo.take(4).forEach { d -> Digito(d) }
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))
        }

        // A frase muda com o host.
        //
        // "O host vai pedir este código na tela do PC" é verdade no Apollo. No Vibeshine
        // ninguém pergunta nada na tela: o código vai na página web do host. Dizer a coisa
        // errada aqui faz a pessoa ficar olhando para um diálogo que nunca vai aparecer.
        // `vidro_where_to_look` NAO serve aqui: ela e "Onde procurar", um rotulo de secao,
        // e sozinha no lugar do texto de apoio ela nao diz nada. O que precisa ser dito e
        // ONDE o codigo aparece -- e isso muda com o host.
        Apoio(
            if (temPagina) stringResource(R.string.vidro_code_on_host_page)
            else stringResource(R.string.vidro_pair_code_help)
        )
    }
}

@Composable
private fun Recusado(nomeDoHost: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Titulo(stringResource(R.string.vidro_code_refused))
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
        // A causa MAIS PROVÁVEL primeiro, e sem código de status. O código expira a cada
        // minuto, e é isso que acontece na esmagadora maioria das vezes.
        Apoio(stringResource(R.string.vidro_code_expired, nomeDoHost))
    }
}

@Composable
private fun Pronto(nomeDoHost: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Titulo(stringResource(R.string.vidro_pair_done, nomeDoHost))
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
        Apoio(stringResource(R.string.vidro_pair_done_sub))
    }
}

@Composable
private fun Digito(d: Char) {
    Box(
        Modifier
            .size(58.dp, 72.dp)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)))
            .background(colorResource(R.color.vidro_surface_overlay))
            .border(
                width = dimensionResource(R.dimen.vidro_stroke_width),
                color = colorResource(R.color.vidro_stroke_hairline),
                shape = RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = d.toString(),
            // Monoespaçada e tabular: os quatro dígitos têm de ter a mesma largura, senão
            // um "1" faz a fileira dançar enquanto o código é lido em voz alta.
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.jetbrains_mono_bold)),
                fontSize = 32.sp,
                color = colorResource(R.color.vidro_text),
            ),
        )
    }
}

@Composable
private fun Titulo(texto: String) {
    BasicText(
        text = texto,
        style = TextStyle(
            fontFamily = FontFamily(Font(R.font.manrope_bold)),
            fontSize = 20.sp,
            letterSpacing = (-0.02).emKt(),
            color = colorResource(R.color.vidro_text),
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun Apoio(texto: String) {
    BasicText(
        text = texto,
        style = TextStyle(
            fontFamily = FontFamily(Font(R.font.manrope_medium)),
            fontSize = 13.sp,
            color = colorResource(R.color.vidro_text_3),
            textAlign = TextAlign.Center,
        ),
    )
}

/** Botão fantasma: a saída secundária, que não compete com a ação principal. */
@Composable
fun Fantasma(texto: String, aoTocar: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .cartaoTocavel(aoTocar = aoTocar)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_l)))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 15.sp,
                color = colorResource(R.color.vidro_text_3),
            ),
        )
    }
}
