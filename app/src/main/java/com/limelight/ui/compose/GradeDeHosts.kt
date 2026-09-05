package com.limelight.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import com.limelight.R
import kotlinx.coroutines.delay

/**
 * A lista de lugares da home.
 *
 * O protótipo lista lugares, não os dispõe em grade: uma linha por host, com o estado em
 * palavra à direita. Grade de ladrilhos era herança do Moonlight.
 *
 * Esta função substitui `pc_grid_view.xml` + `pc_grid_item.xml` + a variante de tablet
 * `layout-sw600dp/pc_grid_item.xml`. **A variação por tamanho deixou de ser
 * arquivo duplicado e virou condição:** com o `BoxWithConstraints` abaixo, o cartão largo é
 * um ramo dentro da mesma função, e a armadilha documentada no piloto — id novo exige editar
 * três arquivos, senão `findViewById` volta `null` só no tablet — deixa de ser possível.
 */
@Composable
fun GradeDeHosts(
    hosts: List<HostUi>,
    aoTocar: (HostUi) -> Unit,
    aoSegurar: (HostUi) -> Unit,
    modifier: Modifier = Modifier,
    temSondaEmCurso: Boolean = false,
    aoVencerPrazo: () -> Unit = {},
) {
    // O prazo de sonda vence sem que ninguém responda, e é justamente esse o caso que
    // precisa de um empurrão. Só enquanto houver linha girando: sem sonda em curso não há
    // laço nenhum rodando atrás da tela.
    if (temSondaEmCurso) {
        LaunchedEffect(hosts) {
            delay(500)
            aoVencerPrazo()
        }
    }

    BoxWithConstraints(modifier) {
        // 600dp é a mesma fronteira que o app já usa em `layout-sw600dp/`. A diferença é
        // que aqui ela é a largura REAL disponível para esta lista, não a menor largura do
        // aparelho — então a lista se comporta certo mesmo quando divide a tela com outra
        // coluna, que é o caso da home deitada no Tab S8 Ultra.
        val largo = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = dimensionResource(R.dimen.vidro_space_4)
            ),
        ) {
            items(hosts, key = { it.uuid }) { host ->
                LinhaDeHost(
                    host = host,
                    largo = largo,
                    aoTocar = { aoTocar(host) },
                    aoSegurar = { aoSegurar(host) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaDeHost(
    host: HostUi,
    largo: Boolean,
    aoTocar: () -> Unit,
    aoSegurar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.vidro_inset_card),
                vertical = dimensionResource(R.dimen.vidro_space_1),
            )
            // O CARTÃO SAIU, e a regra que ficou é de uma frase: cartão só existe onde há
            // imagem.
            //
            // Aqui não há. Cada linha é um nome e um estado, e o cartão é o que se usa
            // quando não se consegue estabelecer hierarquia de outro jeito. Tirar as seis
            // caixas tira seis fundos, doze fios e seis raios — e o que sobra é tipo e luz,
            // que é o único par capaz de sustentar 1722 dp sem parecer esticado.
            //
            // O cartão veio por herança da grade de capas do Moonlight, e a capa não está
            // aqui. No catálogo ela está, e lá o cartão continua.
            //
            // O ALVO DE TOQUE É O QUE ISTO CUSTA, e por isso a altura mínima subiu de 52
            // para 60 dp: sem caixa, a área tocável fica invisível, e a única coisa que
            // garante que o dedo acerte é a linha ser alta o bastante para não precisar de
            // pontaria. A mola do toque continua sendo a resposta.
            // <b>O que falta aqui, dito em vez de escondido:</b> o estado da máquina
            // (acordada, dormindo, fora do ar) é desenhado como ponto de luz num `Canvas`, e
            // ponto não tem texto. O nome sai certo para quem usa leitor de tela — é uma
            // `TextView` filha, e o leitor lê o filho e ativa o pai —, mas o estado não sai.
            // Consertar é dar rótulo ao ponto, e é tarefa própria.
            .cartaoTocavel(aoTocar = aoTocar, aoSegurar = aoSegurar)
            .defaultMinSize(minHeight = if (largo) 96.dp else 60.dp)
            .padding(
                horizontal = if (largo) 8.dp else 2.dp,
                vertical = if (largo) 14.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // O PONTO SAIU JUNTO COM O CARTÃO.
        //
        // Ele dizia em 9 dp de cor exatamente o que o nome agora diz em 20 sp da mesma cor.
        // Duas marcas para um dado é o que sobra quando se acrescenta sem tirar.

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = host.nome,
                // A ORIGEM DA VIAGEM.
                //
                // Pela chave do uuid, e não pelo nome: dois lugares podem se chamar igual, e
                // duas chaves iguais na mesma composição fazem o Compose escolher uma
                // arbitrariamente. Fora da Casca isto é identidade e a linha não muda em nada.
                modifier = Modifier.viaja(chaveDeViagem(host.uuid)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    // Maior, porque agora ele é a linha inteira: o cartão que dava peso à
                    // linha saiu, e sem um degrau de tamanho o que sobra é uma lista fraca.
                    fontFamily = FontFamily(Font(R.font.manrope_bold)),
                    fontSize = if (largo) 24.sp else 20.sp,
                    letterSpacing = (-0.025).emKt(),
                    // O ESTADO É O PRÓPRIO NOME.
                    //
                    // Isto troca leitura por percepção: antes era preciso varrer seis linhas
                    // e ler seis palavras em inglês na margem direita para saber quais
                    // máquinas estavam de pé. Agora a lista se lê de longe.
                    color = colorResource(temperaturaDe(host.estado)),
                ),
            )

            // O endereço só no cartão largo: no estreito ele não cabe sem competir com o
            // nome, e uma linha vazia daria ao cartão uma altura que não corresponde a nada.
            if (largo && host.endereco != null) {
                // MONOESPAÇADO, e não por gosto: é a voz da MÁQUINA.
                //
                // A regra de leitura da identidade é de uma linha — tudo em mono é dado que
                // veio de fora e é literal (endereço, código, latência); tudo em Manrope é o
                // app falando. Aprende-se em um dia e nunca mais se pensa nela.
                BasicText(
                    text = host.endereco,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.jetbrains_mono_medium)),
                        fontSize = 12.sp,
                        color = colorResource(R.color.vidro_text_3),
                    ),
                )
            }

            if (host.emSessao) {
                // Em `vidro_text_2` e não no acento: o nome já está quente, e o que esta
                // linha acrescenta é o que está acontecendo, não que a máquina está viva.
                // Duas marcas quentes na mesma linha competiriam entre si.
                BasicText(
                    text = stringResource(R.string.vidro_place_in_session),
                    modifier = Modifier.padding(top = 3.dp),
                    style = estiloDeApoio(R.color.vidro_text_2),
                )
            }
        }

        // O cadeado só aparece quando há o que destrancar.
        if (host.estado == EstadoHost.PRONTO && !host.pareado) {
            Image(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = dimensionResource(R.dimen.vidro_space_2)),
            )
        }

        // Enquanto a sondagem não respondeu não afirmamos nada: some a palavra e fica só o
        // giro. Dizer "fora do ar" antes de saber é a mentira mais fácil desta tela.
        if (host.acordando) {
            // A espera do WoL: prazo escrito, no lugar da palavra de estado. Prometer 20 s
            // e não entregar é pior que não oferecer -- por isso ela some sozinha quando o
            // host responde, e não fica girando sem fim.
            BasicText(
                text = stringResource(R.string.vidro_waking),
                style = estiloDeApoio(R.color.vidro_accent_text),
            )
        } else if (host.estado == EstadoHost.CHECANDO) {
            Girando()
        } else if (host.estado != EstadoHost.PRONTO && host.estado != EstadoHost.DORMINDO) {
            // A PALAVRA SÓ FICA ONDE ELA ACRESCENTA.
            //
            // "pronto" e "dormindo" saíram: a temperatura do nome já diz as duas, e repetir
            // em inglês na margem direita é a leitura que esta mudança veio eliminar.
            //
            // "fora do ar" e "sem resposta" FICAM, e a razão é que o eixo não sabe
            // distingui-las — as duas são ausência sem croma. A diferença entre uma máquina
            // desligada e uma que ninguém alcança é acionável (uma se acorda, a outra se
            // investiga), então ela continua escrita.
            BasicText(
                text = stringResource(palavraDoEstado(host.estado, host.pareado)),
                style = estiloDeApoio(R.color.vidro_text_3),
            )
        }
    }
}

/**
 * O eixo de estado, num lugar só.
 *
 * Três valores e nada mais. `CHECANDO` cai no ausente de propósito: enquanto a sondagem não
 * respondeu não afirmamos nada, e pintar de quente ou de frio antes de saber é a mentira mais
 * fácil desta tela — quem indica a espera é o giro ao lado.
 */
private fun temperaturaDe(estado: EstadoHost): Int = when (estado) {
    EstadoHost.PRONTO -> R.color.vidro_quente
    EstadoHost.DORMINDO -> R.color.vidro_frio
    EstadoHost.CHECANDO, EstadoHost.SEM_RESPOSTA, EstadoHost.FORA_DO_AR -> R.color.vidro_ausente
}

@Composable
private fun Ponto(estado: EstadoHost) {
    // Desenhado, nao carregado de drawable.
    //
    // Achado do piloto, e vale registrar: `painterResource` so aceita VectorDrawable e
    // bitmap. Os tres pontos eram <shape oval>, que nao atravessa para o Compose -- a
    // captura falhou com "Only VectorDrawables and rasterized asset types are supported".
    // Converter o shape em vetor seria manter o rodeio; desenhar e o caminho da stack, e
    // aqui ele custa seis linhas.
    //
    // As decisoes visuais sao as MESMAS que os shapes documentavam, e por isso ficam:
    //  - aceso e disco cheio no acento de PREENCHIMENTO, nao no de brilho: brilho e papel
    //    do halo atras da tela, e num disco de 9dp ele le mais escuro e mais saturado que
    //    o resto da identidade.
    //  - apagado e anel, e o que separa "dormindo" de "nao responde" e a COR do traco,
    //    nunca a transparencia: quando era alfa, o anel distante chegava a tela em 0,26 e
    //    media ~1,9:1 contra o cartao, ou seja, nao se enxergava.
    val cheio = estado == EstadoHost.PRONTO
    val cor = colorResource(
        when (estado) {
            EstadoHost.PRONTO -> R.color.vidro_accent_fill
            EstadoHost.CHECANDO, EstadoHost.DORMINDO -> R.color.vidro_text_2
            EstadoHost.SEM_RESPOSTA, EstadoHost.FORA_DO_AR -> R.color.vidro_text_3
        }
    )

    Canvas(Modifier.size(9.dp)) {
        if (cheio) {
            drawCircle(color = cor)
        } else {
            val traco = 1.5.dp.toPx()
            drawCircle(color = cor, radius = size.minDimension / 2f - traco / 2f, style = Stroke(traco))
        }
    }
}

/**
 * O giro da sonda, no lugar onde a palavra de estado ficaria.
 *
 * Arco de 270 graus dando uma volta por segundo. Nada de indicador do Material: ele traria
 * a paleta e a espessura do Material junto, que e o motivo de a dependencia nao ter entrado.
 */
@Composable
private fun Girando() {
    val transicao = rememberInfiniteTransition(label = "sonda")
    val angulo by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "anguloDaSonda",
    )
    val cor = colorResource(R.color.vidro_text_3)

    Canvas(Modifier.size(14.dp)) {
        val traco = 1.5.dp.toPx()
        drawArc(
            color = cor,
            startAngle = angulo,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(traco, cap = StrokeCap.Round),
            topLeft = androidx.compose.ui.geometry.Offset(traco / 2f, traco / 2f),
            size = androidx.compose.ui.geometry.Size(size.width - traco, size.height - traco),
        )
    }
}

private fun palavraDoEstado(estado: EstadoHost, pareado: Boolean): Int = when (estado) {
    EstadoHost.PRONTO -> if (pareado) R.string.vidro_state_ready else R.string.vidro_state_unpaired
    EstadoHost.SEM_RESPOSTA -> R.string.vidro_state_no_answer
    EstadoHost.DORMINDO -> R.string.vidro_state_asleep
    EstadoHost.FORA_DO_AR -> R.string.vidro_state_offline
    EstadoHost.CHECANDO -> R.string.vidro_state_no_answer // não é exibido; ver Girando()
}

@Composable
private fun estiloDeApoio(cor: Int) = TextStyle(
    // VidroText.Support — 13sp, medium
    fontFamily = FontFamily(Font(R.font.manrope_medium)),
    fontSize = 13.sp,
    color = colorResource(cor),
)

@Preview(widthDp = 393, backgroundColor = 0xFF05070A, showBackground = true)
@Composable
private fun PreviaEstreita() {
    GradeDeHosts(hosts = hostsDeExemplo(), aoTocar = {}, aoSegurar = {})
}

@Preview(widthDp = 720, backgroundColor = 0xFF05070A, showBackground = true)
@Composable
private fun PreviaLarga() {
    GradeDeHosts(hosts = hostsDeExemplo(), aoTocar = {}, aoSegurar = {})
}

internal fun hostsDeExemplo() = listOf(
    HostUi("1", "PC-RAFAEL", EstadoHost.PRONTO, pareado = true, emSessao = true, endereco = "192.168.0.12"),
    HostUi("2", "SALA", EstadoHost.DORMINDO, pareado = true, emSessao = false, endereco = "192.168.0.31"),
    HostUi("3", "NOTEBOOK", EstadoHost.PRONTO, pareado = false, emSessao = false, endereco = null),
    HostUi("4", "REMOTO", EstadoHost.SEM_RESPOSTA, pareado = true, emSessao = false, endereco = "meu.host:47990"),
    HostUi("5", "CHECANDO", EstadoHost.CHECANDO, pareado = true, emSessao = false, endereco = null),
)
