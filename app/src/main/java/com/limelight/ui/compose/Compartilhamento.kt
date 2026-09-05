package com.limelight.ui.compose

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * O que faz uma coisa VIAJAR de uma tela para a outra.
 *
 * <h2>O problema</h2>
 *
 * Até aqui a troca de tela era o pop de janela do Android — escala 0,94 a partir do centro,
 * aplicado como `windowAnimationStyle`. É literalmente o som de uma janela sendo trocada, e é
 * o motivo de este ser o único app que o dono usa que parece um app Android comum no momento
 * mais repetido do dia: tocar numa máquina.
 *
 * Continuidade é impossível atravessando fronteira de janela. O sistema não sabe que o nome
 * na lista e o título do catálogo são a mesma coisa, então ele só sabe apagar uma página e
 * acender outra. Para eles serem o mesmo elemento, as duas telas precisam estar na mesma
 * árvore de composição — que é o que a Casca faz.
 *
 * <h2>Por que um CompositionLocal e não um parâmetro</h2>
 *
 * O caminho normal em Compose é o `SharedTransitionScope` chegar como receptor até quem
 * desenha o elemento. Aqui isso significaria atravessar `TelaHome` → `GradeDeHosts` →
 * `LinhaDeHost` e `TelaCatalogo` → cabeçalho, com um parâmetro nullable em cada degrau, em
 * telas que também são montadas sozinhas — pelas capturas de tela e pelos testes.
 *
 * Com um local, quem desenha pede o que precisa e **funciona igual quando não há nada**:
 * [viaja] vira identidade fora da Casca. Nenhuma tela passa a depender de estar dentro dela.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class Compartilhamento(
    val escopo: SharedTransitionScope,
    val visivel: AnimatedVisibilityScope,
)

val LocalCompartilhamento = compositionLocalOf<Compartilhamento?> { null }

/**
 * Marca este elemento como o MESMO nas duas telas.
 *
 * A chave é o que os liga, e ela tem de ser estável e única: o uuid do host, não o nome —
 * dois lugares podem se chamar igual, e duas chaves iguais na mesma composição fazem o
 * Compose escolher uma arbitrariamente.
 *
 * Fora da Casca isto não faz nada, de propósito. Uma tela desenhada sozinha continua sendo a
 * mesma tela.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.viaja(chave: String): Modifier = composed {
    val ponte = LocalCompartilhamento.current ?: return@composed this
    with(ponte.escopo) {
        // `sharedBounds` com `ScaleToBounds`, e nao `sharedElement`.
        //
        // O que viaja aqui e TEXTO, e ele muda de tamanho no caminho: e um item de lista na
        // home e um titulo grande no catalogo. `sharedElement` mantem um so composable e o
        // remede a cada quadro -- a linha e refeita em larguras intermediarias e o Compose
        // recorta o que nao cabe. Na gravacao de 26/08 da para ler "ARTF" no meio do voo.
        //
        // `ScaleToBounds` escala os glifos em vez de remedir a linha: o texto sai inteiro nas
        // duas pontas e inteiro no meio, so maior ou menor. E o modo que a documentacao do
        // proprio Compose indica exatamente para este caso.
        this@composed.sharedBounds(
            rememberSharedContentState(key = chave),
            animatedVisibilityScope = ponte.visivel,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
        )
    }
}

/**
 * A chave que liga o nome na lista ao título do catálogo.
 *
 * Uma função e não duas constantes montadas à mão nos dois lados: chave que se escreve duas
 * vezes é chave que diverge uma vez, e quando ela diverge nada quebra — só deixa de viajar,
 * em silêncio, que é o pior desfecho possível para uma coisa assim.
 */
fun chaveDeViagem(uuid: String): String = "lugar:$uuid"
