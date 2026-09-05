package com.limelight.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R

/**
 * A curva padrão da identidade — `res/interpolator/vidro_standard.xml`.
 *
 * Ela existe escrita aqui, e não como `tween(N)` pelado, porque `tween` sem `easing` usa o
 * padrão do Compose, que é `FastOutSlowInEasing`. **Não apague o `easing` achando que é
 * redundante**: os quatro números coincidirem com a curva padrão do Material é coincidência
 * de números, não de origem — eles vieram de `vidro_standard.xml`, e o dia em que a
 * identidade mudar a curva dela, quem apagou isto não vai perceber que a tela ficou para
 * trás. É a mesma armadilha que `FolhaVidro.kt` documenta.
 */
private val PADRAO = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * Três chips por linha, no máximo — é o que a folha de sessão já faz nos dois grupos dela.
 * Com quatro, cada chip fica estreito demais para "Controle na tela" numa tela de 393 dp.
 */
private const val CHIPS_POR_LINHA = 3

/** O raio do chip, de `res/drawable/vidro_chip_on.xml`. Não há token de 14 dp na escala. */
private val RAIO_DO_CHIP = 14.dp

/**
 * O editor de cena — o PE06.
 *
 * Substitui `EditProfileActivity`, que estende o fragmento de Ajustes e infla o **mesmo**
 * `R.xml.preferences`: tocar em "Salvar a sessão atual como cena" abria as 126 preferências
 * pré-preenchidas, com "Video Settings", "Audio Settings", "Gamepad Settings" e o resto do
 * índice. Era a tela de Ajustes com outro título. Por isso ninguém salvou cena nenhuma.
 *
 * A especificação já dizia o que uma cena é — *"Cada uma define papel, tela, taxa e
 * entrada"* —, e `vidro_scenes_sub`, que a tela de Cenas mostra logo antes desta, promete
 * exatamente isso em quatro palavras. **Aqui estão as quatro**, com os mesmos rótulos da
 * folha de sessão, e um nome. Tudo o mais herda do global e não aparece.
 *
 * Isso apaga junto o marcador `*` de `highlightPreferences`: ele só existia porque a tela
 * mostrava 126 linhas das quais quatro interessavam, e era preciso marcar quais.
 *
 * O que esta tela **não** faz é gravar. Ela devolve a cena inteira em `aoMudar` a cada
 * toque, e quem escreve as quatro chaves — e só as quatro — é a ponte. Ver o relato sobre a
 * migração das cenas antigas: o modelo em disco ainda guarda snapshot, não delta.
 */
@Composable
fun TelaEditorDeCena(
    cena: CenaEmEdicao,
    /** Muda só o título. Uma cena nova chega com nome já preenchido, não em branco. */
    nova: Boolean,
    /** As resoluções oferecidas, em valor de preferência: `"1920x1080"`. */
    telas: List<String>,
    /** As taxas oferecidas, em valor de preferência: `"60"`. */
    taxas: List<String>,
    aoVoltar: () -> Unit,
    /**
     * A cena inteira, a cada toque.
     *
     * Um callback por dimensão — como faz a tela de Ajustes, que lida com chaves soltas —
     * seria quatro chances de o chamador atualizar três campos e esquecer o quarto. Uma cena
     * é **um** objeto; `copy()` faz o resto.
     */
    aoMudar: (CenaEmEdicao) -> Unit,
    aoSalvar: () -> Unit,
    /**
     * A porta "Avançado": a árvore completa continua existindo, atrás de uma linha no pé.
     *
     * Ela é saída, não entrada — quem chega aqui para trocar a resolução não pode esbarrar
     * em 126 preferências no caminho.
     */
    aoAbrirAvancado: () -> Unit,
) {
    val teclado = LocalSoftwareKeyboardController.current

    // A escolha que a cena guarda entra na lista mesmo quando o aparelho não a oferece.
    //
    // Uma cena feita no Tab e aberta no S22 Ultra pode guardar uma resolução que este
    // aparelho não lista — é o caso do P36, e é o normal, não a exceção. Sem esta linha
    // nenhum chip acenderia, e a tela diria que a cena não escolheu tela nenhuma; pior, o
    // primeiro toque em qualquer outro chip apagaria a escolha antiga sem ninguém ver.
    val telasOferecidas = telas.comOEscolhido(cena.tela)
    val taxasOferecidas = taxas.comOEscolhido(cena.taxa)

    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_void))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                // O teclado empurra, não cobre — a mesma regra da sessão (E01) e da tela do
                // endereço. Aqui ele nasce no campo do nome, que é o topo da lista.
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
                text = stringResource(
                    if (nova) R.string.vidro_scene_new else R.string.vidro_scene_edit
                ),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                    fontSize = 29.sp,
                    letterSpacing = (-0.035).emKt(),
                    color = colorResource(R.color.vidro_text),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))

            // A promessa da tela, dita antes de a pessoa rolar: são quatro, e o resto segue
            // os ajustes. Sem esta linha, uma lista curta parece uma lista incompleta.
            BasicText(
                text = stringResource(R.string.vidro_scene_sub),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_medium)),
                    fontSize = 13.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

            // O miolo rola; o botão de compromisso não. Com o teclado aberto num aparelho
            // deitado sobram poucos dp de altura, e é o campo do nome que precisa deles.
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                CampoDoNome(
                    nome = cena.nome,
                    aoDigitar = { aoMudar(cena.copy(nome = it)) },
                    aoConfirmar = { teclado?.hide() },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

                // As quatro escolhas, na ordem em que `vidro_scenes_sub` as promete:
                // papel, tela, taxa e entrada. Trocar a ordem aqui quebraria a frase que a
                // pessoa acabou de ler na tela de Cenas.
                GrupoDeChips(
                    rotulo = stringResource(R.string.vidro_scene_role),
                    opcoes = PapelDaCena.entries.map { stringResource(it.rotulo) },
                    escolhido = cena.papel.ordinal,
                    aoEscolher = { aoMudar(cena.copy(papel = PapelDaCena.entries[it])) },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

                GrupoDeChips(
                    rotulo = stringResource(R.string.vidro_scene_screen),
                    opcoes = telasOferecidas.map { rotuloDaTela(it) },
                    escolhido = telasOferecidas.indexOf(cena.tela),
                    aoEscolher = { aoMudar(cena.copy(tela = telasOferecidas[it])) },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

                GrupoDeChips(
                    rotulo = stringResource(R.string.vidro_scene_rate),
                    opcoes = taxasOferecidas.map { rotuloDaTaxa(it) },
                    escolhido = taxasOferecidas.indexOf(cena.taxa),
                    aoEscolher = { aoMudar(cena.copy(taxa = taxasOferecidas[it])) },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))

                GrupoDeChips(
                    rotulo = stringResource(R.string.vidro_scene_input),
                    opcoes = ModoDeEntrada.entries.map { stringResource(it.rotulo) },
                    escolhido = cena.entrada.ordinal,
                    aoEscolher = { aoMudar(cena.copy(entrada = ModoDeEntrada.entries[it])) },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_6)))

                PortaDoAvancado(aoTocar = aoAbrirAvancado)

                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))
            }

            BotaoDeCompromisso(
                texto = stringResource(R.string.save),
                aoTocar = {
                    teclado?.hide()
                    aoSalvar()
                },
                modifier = Modifier
                    .padding(vertical = dimensionResource(R.dimen.vidro_space_3))
                    .navigationBarsPadding(),
            )
        }
    }
}

/**
 * O nome da cena.
 *
 * No editor antigo o nome não era campo: era o título da tela, e trocá-lo exigia descobrir
 * que o título aceitava toque, ou achar o lápis na barra — e depois um `AlertDialog`. Aqui
 * ele é a primeira coisa da lista, com moldura de campo, porque nomear é o que transforma
 * "mais uma configuração" em "a minha cena da noite".
 *
 * O campo chega **preenchido**: quem salva a sessão atual como cena recebe um nome pronto e
 * só troca se quiser. Por isso não há validação de nome em branco aqui — em branco é um
 * estado que a ponte não produz.
 */
@Composable
private fun CampoDoNome(nome: String, aoDigitar: (String) -> Unit, aoConfirmar: () -> Unit) {
    Column {
        Rotulo(stringResource(R.string.vidro_scene_name))

        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))

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
                .padding(horizontal = 15.dp, vertical = 16.dp),
        ) {
            BasicTextField(
                value = nome,
                onValueChange = aoDigitar,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // Manrope, e não a monoespaçada do campo de endereço: um nome é prosa, não
                // dado — ninguém precisa distinguir o zero da letra O em "Jogo à noite".
                textStyle = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                    fontSize = 16.sp,
                    letterSpacing = (-0.02).emKt(),
                    color = colorResource(R.color.vidro_text),
                ),
                cursorBrush = SolidColor(colorResource(R.color.vidro_accent_text)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { aoConfirmar() }),
                decorationBox = { campo ->
                    if (nome.isEmpty()) {
                        BasicText(
                            text = stringResource(R.string.vidro_scene_name_hint),
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                                fontSize = 16.sp,
                                letterSpacing = (-0.02).emKt(),
                                color = colorResource(R.color.vidro_text_3),
                            ),
                        )
                    }
                    campo()
                },
            )
        }
    }
}

/**
 * Um grupo de chips: o versalete e as opções.
 *
 * As opções entram por índice, e não por um tipo próprio de cada grupo, porque os quatro
 * grupos têm origens diferentes — dois enums e duas listas que o aparelho monta em runtime —
 * e o que eles têm em comum é exatamente isto: uma lista de rótulos e um escolhido.
 *
 * A quebra em linhas é calculada, não fixa. Três chips por linha é o teto (o mesmo da folha
 * de sessão), mas quatro opções viram **2 + 2**, e não 3 + 1: uma linha com um chip sozinho
 * esticado na largura toda lê como botão, não como opção entre pares. As sobras viram
 * espaçadores de mesmo peso para que a largura do chip não mude de uma linha para a outra.
 */
@Composable
private fun GrupoDeChips(
    rotulo: String,
    opcoes: List<String>,
    escolhido: Int,
    aoEscolher: (Int) -> Unit,
) {
    if (opcoes.isEmpty()) return

    // A REPARTIÇÃO É EQUILIBRADA, e não em blocos de tamanho fixo.
    //
    // `chunked(porLinha)` corta em pedaços iguais e joga a sobra na última linha: com sete
    // opções — que é quanto o grupo TELA tem — dava 3 + 3 + 1, e o "1704x786" ficava sozinho
    // no fim, do tamanho de um terço da largura, lendo como sobra e não como opção. A regra
    // que este arquivo já escrevia para quatro ("2 + 2, e não 3 + 1") vale para todos, então
    // aqui ela virou cálculo em vez de comentário: as `resto` primeiras linhas levam um chip
    // a mais que as outras, e nenhuma fica com um só quando havia como evitar.
    //
    // Sete em três linhas: 3 + 2 + 2. Quatro em duas: 2 + 2. Cinco em duas: 3 + 2.
    val linhas = (opcoes.size + CHIPS_POR_LINHA - 1) / CHIPS_POR_LINHA
    val base = opcoes.size / linhas
    val resto = opcoes.size % linhas
    val tamanhos = List(linhas) { if (it < resto) base + 1 else base }
    val porLinha = tamanhos.first()

    Column {
        Rotulo(rotulo)

        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))

        var cursor = 0
        val fatias = tamanhos.map { quantos ->
            val fatia = opcoes.indices.toList().subList(cursor, cursor + quantos)
            cursor += quantos
            fatia
        }

        fatias.forEachIndexed { linha, daLinha ->
            if (linha > 0) {
                Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
            }
            Row(
                // A LINHA TEM UMA ALTURA SÓ, e ela é a do chip mais alto.
                //
                // Os chips têm `heightIn(min = …)`, então um rótulo que quebra em duas linhas
                // cresce e os vizinhos não: na gravação de 26/08 "Controle na tela" ficava
                // visivelmente mais alto que os dois chips ao lado dele, na mesma linha. Com
                // `IntrinsicSize.Min` a `Row` mede o mais alto primeiro e `fillMaxHeight`
                // estica os outros até ele — sem fixar altura nenhuma no código, que voltaria
                // a errar no primeiro idioma de palavra mais longa.
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.vidro_space_2)
                ),
            ) {
                daLinha.forEach { i ->
                    Chip(
                        texto = opcoes[i],
                        ligado = i == escolhido,
                        aoTocar = { aoEscolher(i) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                repeat(porLinha - daLinha.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * O chip da identidade, em Compose.
 *
 * As cores vêm dos tokens que `vidro_chip_on.xml` e `vidro_chip_off.xml` desenham: borda e
 * texto no acento de TEXTO — nunca no de brilho, que reprova no contraste —, e o fundo num
 * véu do acento. O drawable crava `#291E8FEE`, 16% do acento; `vidro_accent_fill_idle` é o
 * token nomeado com essa mesma opacidade e esse mesmo papel, e é dele que a cor sai aqui.
 *
 * A altura sobe de 44 para `vidro_touch_target`. O chip é o alvo mais estreito do app e o
 * estilo XML o deixava quatro dp abaixo do piso — num alvo estreito é onde o piso mais vale.
 *
 * A troca de estado é animada em cor, no tempo da mola do toque (`vidro_motion_press`, de
 * `res/values/motion.xml`). O chip é o único controle que muda de estado sem trocar de tela:
 * sem a transição, saber qual acendeu depende de o olho conferir a lista inteira.
 */
@Composable
private fun Chip(texto: String, ligado: Boolean, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val ms = integerResource(R.integer.vidro_motion_press)

    val fundo by animateColorAsState(
        targetValue = colorResource(
            if (ligado) R.color.vidro_accent_fill_idle else R.color.vidro_surface_overlay
        ),
        animationSpec = molaLeve(),
        label = "fundoDoChip",
    )
    val borda by animateColorAsState(
        targetValue = colorResource(
            if (ligado) R.color.vidro_accent_text else R.color.vidro_stroke_hairline
        ),
        animationSpec = molaLeve(),
        label = "bordaDoChip",
    )
    val tinta by animateColorAsState(
        targetValue = colorResource(
            if (ligado) R.color.vidro_accent_text else R.color.vidro_text_3
        ),
        animationSpec = molaLeve(),
        label = "tintaDoChip",
    )

    Box(
        modifier
            .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))
            .cartaoTocavel(aoTocar = aoTocar)
            .clip(RoundedCornerShape(RAIO_DO_CHIP))
            .background(fundo)
            .border(
                width = dimensionResource(R.dimen.vidro_stroke_width),
                color = borda,
                shape = RoundedCornerShape(RAIO_DO_CHIP),
            )
            .padding(dimensionResource(R.dimen.vidro_space_2)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 13.sp,
                letterSpacing = (-0.01).emKt(),
                color = tinta,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * A porta "Avançado", no pé da lista.
 *
 * Discreta de propósito, e **sem o acento**: acento é a cor do que se veio fazer aqui, e
 * ninguém abre o editor de cena para visitar 126 preferências. Mas ela existe, e com a linha
 * de apoio dizendo o que há do outro lado — porta sem placa vira porta que ninguém abre, e
 * aí a árvore completa some de verdade, que não é a decisão tomada.
 */
@Composable
private fun PortaDoAvancado(aoTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.vidro_touch_target))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.vidro_radius_m)))
            .cartaoTocavel(aoTocar = aoTocar)
            .padding(
                horizontal = dimensionResource(R.dimen.vidro_space_3),
                vertical = dimensionResource(R.dimen.vidro_space_2),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            BasicText(
                text = stringResource(R.string.vidro_scene_advanced),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                    fontSize = 13.sp,
                    color = colorResource(R.color.vidro_text_2),
                ),
            )
            BasicText(
                text = stringResource(R.string.vidro_scene_advanced_sub),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_medium)),
                    fontSize = 12.sp,
                    color = colorResource(R.color.vidro_text_3),
                ),
            )
        }
    }
}

/** O versalete de seção — `VidroText.Overline`: 12 sp, 700, +0.10em, caixa alta. */
@Composable
private fun Rotulo(texto: String) {
    BasicText(
        text = texto.uppercase(),
        style = TextStyle(
            fontFamily = FontFamily(Font(R.font.manrope_bold)),
            fontSize = 12.sp,
            letterSpacing = 0.10.emKt(),
            color = colorResource(R.color.vidro_text_3),
        ),
    )
}

/**
 * `"1920x1080"` vira `"1080p"`.
 *
 * O par valor-rótulo já existe em `res/values/arrays.xml`, em `resolution_values` e
 * `resolution_names` — pedir ao chamador que passe os rótulos junto seria manter uma segunda
 * cópia dessa tabela, que é como duas listas paralelas saem de sincronia. O que não estiver
 * na lista aparece como veio: a resolução nativa que `addNativeResolutionEntries` acrescenta
 * em runtime não tem nome fixo, e mostrar `"2400x1080"` é melhor do que esconder a opção.
 */
@Composable
private fun rotuloDaTela(valor: String): String {
    val valores = stringArrayResource(R.array.resolution_values)
    val nomes = stringArrayResource(R.array.resolution_names)
    val i = valores.indexOf(valor)
    return if (i >= 0 && i < nomes.size) nomes[i] else valor
}

/** A lista oferecida, com o valor que a cena já guarda dentro dela. Ver o uso, no topo. */
private fun List<String>.comOEscolhido(escolhido: String): List<String> =
    if (escolhido.isEmpty() || contains(escolhido)) this else this + escolhido

/** `"60"` vira `"60 FPS"`. Mesma tabela, mesmo motivo de `rotuloDaTela`. */
@Composable
private fun rotuloDaTaxa(valor: String): String {
    val valores = stringArrayResource(R.array.fps_values)
    val nomes = stringArrayResource(R.array.fps_names)
    val i = valores.indexOf(valor)
    return if (i >= 0 && i < nomes.size) nomes[i] else valor
}
