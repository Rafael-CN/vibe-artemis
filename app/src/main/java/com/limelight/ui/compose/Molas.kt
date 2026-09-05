package com.limelight.ui.compose

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * As molas do lado Compose — as mesmas três do lado View, com os mesmos números.
 *
 * O par gêmeo disto é [com.limelight.ui.Mola], que roda no `Choreographer` porque a sessão é
 * Java/View. Os números são repetidos de propósito e **têm de ser mantidos iguais**: um chip
 * de Ajustes e um chip da folha de sessão são a mesma peça para quem usa, e se as duas metades
 * do app tiverem molas diferentes a costura entre elas fica audível.
 *
 * ## Por que perfil e não duração
 *
 * A tabela de `motion.xml` tinha oito linhas e seis eram a mesma ideia em velocidades
 * diferentes, porque duração pressupõe que ninguém agarra no meio. Uma mola tem dois números
 * — rigidez e amortecimento — e cobre qualquer distância sem ninguém escolher um trajeto.
 *
 * A escolha que sobra para quem chama é **a massa do que se move**, e ela é a coisa real: o
 * que separa a mola de um chip da mola de uma tela inteira não é gosto, é tamanho.
 *
 * ## O que continua sendo `tween`, e por quê
 *
 * Mola descreve uma coisa **indo a um lugar**. Onde não há lugar nenhum — uma pulsação que
 * respira, um brilho que atravessa a tela em laço — a duração é a descrição certa e trocá-la
 * por mola seria a mesma confusão ao contrário. Esses casos ficam em `tween` com `easing`
 * explícito, e cada um diz por quê no lugar onde está.
 */

/** Toque, chip, ponto de estado. Rápido e sem sobra. */
fun <T> molaLeve(): FiniteAnimationSpec<T> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 1500f,
)

/** Gaveta, folha, realce, linha de lista. O corpo da interface. */
fun <T> molaCorpo(): FiniteAnimationSpec<T> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 600f,
)

/** O que virou a tela inteira. Pesado, e por isso mais lento. */
fun <T> molaTela(): FiniteAnimationSpec<T> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 320f,
)
