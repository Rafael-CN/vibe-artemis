package com.limelight.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.limelight.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale

/**
 * Um alvo de toque da identidade Vidro: mola, sem ripple, com toque longo.
 *
 * Mola e clique vêm juntos de propósito. Separados, cada chamador teria de lembrar de passar
 * a MESMA fonte de interação para os dois — e quem esquecesse teria um card que compila,
 * aceita o toque e nunca anima. É o defeito mais silencioso possível.
 *
 * Os números são os do handoff: 120 ms, escala 0,97 e volta. E o `indication = null` é a
 * primeira linha da lista PROIBIDO — é também o motivo de o Material não ter entrado como
 * dependência.
 */
@OptIn(ExperimentalFoundationApi::class)
private const val ESCALA_DO_TOQUE = 0.97f

fun Modifier.cartaoTocavel(
    aoTocar: () -> Unit,
    aoSegurar: (() -> Unit)? = null,
    habilitado: Boolean = true,
): Modifier = composed {
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    // A MOLA DO TOQUE, e agora ela é uma mola de verdade.
    //
    // Era `tween(120)` sem `easing`, o que significa `FastOutSlowInEasing` — a curva padrão
    // do Material, justamente o que o ADR 0004 manteve fora da dependência. E cravava 0,97 em
    // código embora `@dimen/vidro_press_scale` exista para isso desde sempre.
    //
    // Com mola, soltar no meio de um toque rápido não espera os 120 ms acabarem: a escala
    // volta de onde está. Num toque repetido — que é o caso de uma lista — a diferença entre
    // as duas é a diferença entre responder e reproduzir.
    //
    // A escala vive como constante e não como `@dimen`: `vidro_press_scale` é um float
    // disfarçado de dimensão, e lê-lo em Compose exigiria TypedValue a cada composição
    // (`Resources.getFloat` é API 29 e o minSdk é 21). O recurso continua servindo ao
    // animator de XML; este número é o mesmo, com o nome do que ele significa.
    val escala by animateFloatAsState(
        targetValue = if (pressionado) ESCALA_DO_TOQUE else 1f,
        animationSpec = molaLeve(),
        label = "molaDoToque",
    )

    this
        .scale(escala)
        .combinedClickable(
            interactionSource = fonte,
            indication = null,
            enabled = habilitado,
            onClick = aoTocar,
            onLongClick = aoSegurar,
        )
}

/**
 * Esmaece o que está inerte.
 *
 * Usado quando o host some enquanto o catálogo está aberto (P04): a grade continua na tela,
 * recuada, e o toque nela não dispara conexão contra um host que não está lá.
 */
fun Modifier.alphaSeMorto(vivo: Boolean): Modifier = if (vivo) this else this.alpha(0.35f)
