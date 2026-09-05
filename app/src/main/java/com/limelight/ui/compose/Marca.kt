package com.limelight.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import com.limelight.R

/**
 * A marca — uma máquina e o brilho dela.
 *
 * <h2>Por que o app precisava de uma</h2>
 *
 * Ele vestia o ícone herdado do upstream. Reconhecível numa miniatura é a definição
 * operacional de identidade, e nenhum produto premiado é anônimo no launcher. A identidade
 * anterior tinha treze tokens de cor, sete degraus de raio, uma tabela tipográfica — e nenhum
 * objeto. É por isso que ela era um tema e não uma identidade.
 *
 * <h2>Por que ela é ESTA forma, e não uma inventada</h2>
 *
 * Marca desenhada por quem não é ilustrador vira logotipo genérico. A proteção aqui é a
 * geometria não ser invenção: ela é a luz que o app já desenha, congelada. `VidroLightView`
 * pinta um gradiente radial com o núcleo deslocado do centro — os mesmos `CENTER_Y_DEFAULT`,
 * `CORE_ALPHA` e `DIAMETER_OF_WIDTH`. Se a marca precisar de qualquer curva que não venha
 * daí, ela está errada.
 *
 * <p>E o deslocamento do núcleo é a única coisa que a separa de um ponto qualquer. Ele não é
 * estilo: é o que faz o círculo parecer iluminado por dentro em vez de preenchido.
 *
 * @param temperatura cor do núcleo, de `R.color` — a marca herda o eixo de estado como
 *   qualquer outra coisa no app.
 */
@Composable
fun Marca(
    tamanho: Dp,
    modifier: Modifier = Modifier,
    temperatura: Int = R.color.vidro_quente,
    /** Respira devagar. Usado no estado vazio, onde a espera é o assunto. */
    respirando: Boolean = false,
) {
    val nucleo = colorResource(temperatura)

    // O laço é um LAÇO, e por isso é `tween` e não mola: mola descreve uma coisa indo a um
    // lugar, e aqui não há lugar nenhum — há um pulso. Ver a doutrina em Molas.kt.
    val transicao = rememberInfiniteTransition(label = "marca")
    val pulso by transicao.animateFloat(
        initialValue = if (respirando) 0.55f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respiro",
    )

    Canvas(modifier.then(Modifier.size(tamanho))) {
        val raio = size.minDimension / 2f
        val centro = Offset(size.width / 2f, size.height / 2f)

        // O núcleo fica a 30% do raio acima e à esquerda do centro geométrico. É o mesmo
        // deslocamento da luz das telas, e é o que dá a impressão de fonte de luz.
        val fonte = Offset(centro.x - raio * 0.30f, centro.y - raio * 0.30f)

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to nucleo.copy(alpha = 0.95f * pulso),
                    0.22f to nucleo.copy(alpha = 0.78f * pulso),
                    0.62f to nucleo.copy(alpha = 0.20f * pulso),
                    1.00f to Color.Transparent,
                ),
                center = fonte,
                radius = raio * 1.35f,
            ),
            radius = raio,
            center = centro,
        )

        // O contorno é o que a mantém sendo um objeto quando o brilho está baixo — sem ele,
        // a marca apagada do estado vazio some no chão em vez de esperar nele.
        drawCircle(
            color = nucleo.copy(alpha = 0.16f + 0.10f * pulso),
            radius = raio,
            center = centro,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = raio * 0.045f),
        )
    }
}
