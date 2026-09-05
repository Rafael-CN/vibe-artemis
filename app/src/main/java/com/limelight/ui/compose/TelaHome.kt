package com.limelight.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.viewinterop.AndroidView
import com.limelight.R
import com.limelight.ui.VidroLightView

/**
 * A home inteira.
 *
 * Substitui `activity_pc_view.xml` e as suas DUAS cópias (`layout-sw600dp/` e
 * `layout-sw600dp-land/`) — 1.064 linhas de XML em três arquivos que precisavam ser
 * mantidos em sincronia id por id.
 *
 * A moldura de tablet deitado — coluna de 420 dp à esquerda, conteúdo à direita — deixou de
 * ser um arquivo separado e virou um `if` sobre a largura real. E o cabeçalho deixou de
 * precisar do registro "só escreve quem mudou": ele é derivado dos hosts, e derivar de novo
 * o mesmo valor não custa layout nenhum.
 */
@Composable
fun TelaHome(
    estado: EstadoDaHome,
    continuar: List<Historico.Entrada>,
    aoTocarHost: (HostUi) -> Unit,
    aoSegurarHost: (HostUi) -> Unit,
    aoContinuar: (Historico.Entrada) -> Unit,
    aoAdicionar: () -> Unit,
    aoCenas: () -> Unit,
    aoAjustes: () -> Unit,
    nomeDasCenas: String,
    aoRetomar: (HostUi) -> Unit = {},
    aoEncerrar: (HostUi) -> Unit = {},
    aoParear: (HostUi) -> Unit = {},
    aoAcordar: (HostUi) -> Unit = {},
    aoRemover: (HostUi) -> Unit = {},
    aoMais: (HostUi) -> Unit = {},
    aoTestarRede: (HostUi) -> Unit = {},
    /** Por que ninguém respondeu ainda — ou 0 quando não há nada de útil a dizer. */
    motivoDoSilencio: Int = 0,
) {
    val hosts = estado.hosts
    val resumo = resumoDosHosts(hosts)

    Box(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.vidro_void))
    ) {
        // A luz continua sendo a View que já existe: ela desenha um gradiente radial com
        // shader próprio, e reescrevê-la em Canvas seria refazer trabalho que está pronto e
        // certo. `AndroidView` custa uma View no meio de uma árvore Compose — aceitável para
        // uma peça que não recebe toque e não muda de tamanho.
        AndroidView(
            factory = { ctx -> VidroLightView(ctx) },
            update = {
                it.setTemperatura(resumo.temperatura)
                it.setIntensity(resumo.intensidade, true)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.TopCenter),
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // A mesma fronteira de `layout-sw600dp-land/`, agora sobre a largura de verdade.
            val deitadoLargo = maxWidth >= 900.dp

            if (deitadoLargo) {
                Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    Column(
                        Modifier
                            .width(420.dp)
                            .fillMaxHeight()
                            .padding(horizontal = dimensionResource(R.dimen.vidro_space_6)),
                    ) {
                        Cabecalho(resumo)
                        Continuar(continuar, aoContinuar)
                        // O rodape mora no PE da coluna. Solto logo abaixo do "Continuar"
                        // ele flutuava no meio de meia tela vazia -- que e exatamente o
                        // vazio que esta coluna veio preencher.
                        Spacer(Modifier.weight(1f))
                        Rodape(nomeDasCenas, aoCenas, aoAjustes)
                    }
                    // TETO DE LEITURA, e ele existe por um defeito medido.
                    //
                    // Sem ele a lista de lugares ocupa os 1300 dp que sobram da coluna, e o
                    // nome fica na margem esquerda enquanto a palavra de estado vai para a
                    // direita -- na captura do Tab S8 Ultra sao 1400 px de vazio entre uma e
                    // outra, e a linha deixa de ser lida como uma coisa so. Isso nao e
                    // respiro; respiro e vazio que da forma a alguma coisa.
                    //
                    // 640 dp e a largura em que o nome, o endereco e o estado convivem numa
                    // linha que o olho abraca. O vazio migra para a direita da coluna, onde
                    // ele nao esta separando nada.
                    //
                    // E 640 e nao 560 por um motivo concreto: GradeDeHosts decide o cartao
                    // LARGO em 600 dp, e e o largo que mostra o endereco. Com 560 o teto
                    // empurrava a linha para a forma estreita e o endereco sumia -- um
                    // numero apertando o outro, sem nada dizendo.
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .widthIn(max = 640.dp)
                            .padding(end = dimensionResource(R.dimen.vidro_inset_card)),
                    ) {
                        // O ROTULO SO EXISTE QUANDO HA LISTA.
                        //
                        // Sem lista ele ficava pairando sobre 600 px de nada: um cabecalho de
                        // coisa nenhuma, que era a primeira coisa que alguem via do app. Quem
                        // fala no estado vazio e a marca, e ela nao precisa de titulo.
                        if (hosts.isNotEmpty()) {
                            TituloDosLugares(aoAdicionar, mostrarAdicionar = true)
                        }
                        Corpo(estado, hosts, aoTocarHost, aoSegurarHost, motivoDoSilencio, aoAdicionar)
                    }
                }
            } else {
                // `systemBars`, e nao `statusBars`: as duas barras, nao so a de cima.
                //
                // Medido com uiautomator, no emulador em navegacao por GESTOS: o rodape
                // "Cenas . Ajustes" ficava em y=2270..2396 e a barra de navegacao em
                // y=2337..2400 -- os ultimos 59 px do alvo debaixo da barra de gestos do
                // sistema. Tocar na parte de baixo do botao disparava o home do Android em
                // vez do app. O dono do fork relatou exatamente isso: "puxo a bottom bar do
                // celular sem querer e fica saindo do app".
                //
                // As sete telas da casca tinham o mesmo erro, pela mesma linha copiada.
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    Column(Modifier.padding(horizontal = dimensionResource(R.dimen.vidro_inset_card))) {
                        Cabecalho(resumo)
                        Continuar(continuar, aoContinuar)
                        // O ROTULO SO EXISTE QUANDO HA LISTA.
                        //
                        // Sem lista ele ficava pairando sobre 600 px de nada: um cabecalho de
                        // coisa nenhuma, que era a primeira coisa que alguem via do app. Quem
                        // fala no estado vazio e a marca, e ela nao precisa de titulo.
                        if (hosts.isNotEmpty()) {
                            TituloDosLugares(aoAdicionar, mostrarAdicionar = true)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Corpo(estado, hosts, aoTocarHost, aoSegurarHost, motivoDoSilencio, aoAdicionar)
                    }
                    Rodape(nomeDasCenas, aoCenas, aoAjustes)
                }
            }
        }

        // O VOLTAR FECHA A FOLHA, e não o app.
        //
        // Não havia BackHandler em lugar nenhum da árvore Compose, e PcView é a Activity de
        // lançamento: com a folha aberta, o Voltar do sistema atravessava e encerrava a
        // Activity — saía do aplicativo. No Tab S8 Ultra com a capa acoplada esse é o
        // caminho provável, não o excepcional: com as mãos no teclado, o reflexo para
        // desfazer uma abertura acidental é Esc, e Esc chega como Voltar.
        //
        // O comentário do próprio scrim já dizia que sem uma saída a folha vira um beco. O
        // Voltar do dedo tinha sido implementado; o do teclado, não.
        BackHandler(enabled = estado.folhaDoHost != null) {
            estado.fecharFolha()
        }

        FolhaDoHost(
            host = estado.folhaDoHost,
            aoFechar = { estado.fecharFolha() },
            aoAbrirCatalogo = { estado.fecharFolha(); aoTocarHost(it) },
            aoRetomar = { estado.fecharFolha(); aoRetomar(it) },
            aoEncerrar = { estado.fecharFolha(); aoEncerrar(it) },
            aoParear = { estado.fecharFolha(); aoParear(it) },
            aoAcordar = { estado.fecharFolha(); aoAcordar(it) },
            aoRemover = { estado.fecharFolha(); aoRemover(it) },
            aoMais = { estado.fecharFolha(); aoMais(it) },
            aoTestarRede = { estado.fecharFolha(); aoTestarRede(it) },
        )
    }
}

/**
 * As ações de um lugar, escolhidas pelo estado dele.
 *
 * A regra do PE03: **no máximo quatro linhas por vez**. O menu de contexto que isto
 * substitui oferecia catorze de uma vez, incluindo "Legacy Pair" ao lado de "OTP Pair" e
 * duas variantes de display virtual que o Vibeshine decide sozinho.
 *
 * O que não cabe nas quatro desce para "Mais…", que ainda abre o menu antigo — nada some
 * sem ter para onde ir. Podar item a item é a segunda rodada, não efeito colateral desta.
 */
@Composable
private fun FolhaDoHost(
    host: HostUi?,
    aoFechar: () -> Unit,
    aoAbrirCatalogo: (HostUi) -> Unit,
    aoRetomar: (HostUi) -> Unit,
    aoEncerrar: (HostUi) -> Unit,
    aoParear: (HostUi) -> Unit,
    aoAcordar: (HostUi) -> Unit,
    aoRemover: (HostUi) -> Unit,
    aoMais: (HostUi) -> Unit,
    aoTestarRede: (HostUi) -> Unit,
) {
    // `remember` do último host: sem ele a folha perderia o texto no meio da animação de
    // saída, porque o estado vira null antes de ela terminar de descer.
    val ultimo = remember { mutableStateOf<HostUi?>(null) }
    if (host != null) {
        ultimo.value = host
    }
    val alvo = ultimo.value ?: return

    val acoes = mutableListOf<AcaoVidro>()

    when (alvo.estado) {
        EstadoHost.PRONTO -> {
            if (!alvo.pareado) {
                acoes += AcaoVidro(stringResource(R.string.vidro_action_pair)) { aoParear(alvo) }
            } else {
                if (alvo.emSessao) {
                    acoes += AcaoVidro(stringResource(R.string.vidro_action_resume)) { aoRetomar(alvo) }
                }
                acoes += AcaoVidro(stringResource(R.string.vidro_action_open)) { aoAbrirCatalogo(alvo) }
                if (alvo.emSessao) {
                    acoes += AcaoVidro(
                        stringResource(R.string.vidro_action_quit),
                        destrutiva = true,
                    ) { aoEncerrar(alvo) }
                }
            }
        }
        EstadoHost.DORMINDO -> {
            acoes += AcaoVidro(
                stringResource(R.string.vidro_wake_pc),
                apoio = stringResource(R.string.vidro_wake_takes),
            ) { aoAcordar(alvo) }
        }
        EstadoHost.FORA_DO_AR, EstadoHost.SEM_RESPOSTA -> {
            // Sem MAC não há o que oferecer aqui: dizer isso é mais honesto que um botão
            // que não faz nada.
            //
            // E ela é INFORMATIVA, não destrutiva. Nasceu marcada como destrutiva só para
            // sair em outra cor, e saía no vermelho de alerta ao lado de "Remover este
            // lugar" — dois vermelhos, dois significados, e o grave perdendo peso.
            acoes += AcaoVidro(
                stringResource(R.string.vidro_wake_no_mac),
                informativa = true,
            ) { }

            // O TESTE DE REDE VEM PARA CÁ, e só neste ramo.
            //
            // Ele existia só dentro do menu de contexto do sistema, atrás de "Mais" — e era
            // a única coisa acionável que este estado tinha. Na gravação de 26/08 a máquina
            // aparece "Indisponível" e a folha não oferecia NADA: quem abriu a folha por
            // causa do problema saía dela sem ter tocado no problema.
            //
            // Só neste ramo porque em máquina que responde ele não diagnostica nada — e ação
            // que não muda nada é ruído em toda folha que a inclui.
            acoes += AcaoVidro(
                stringResource(R.string.vidro_action_test_network),
                apoio = stringResource(R.string.vidro_action_test_network_sub),
            ) { aoTestarRede(alvo) }
        }
        EstadoHost.CHECANDO -> Unit
    }

    acoes += AcaoVidro(stringResource(R.string.vidro_action_forget), destrutiva = true) { aoRemover(alvo) }
    acoes += AcaoVidro(stringResource(R.string.vidro_action_more)) { aoMais(alvo) }

    FolhaVidro(
        titulo = alvo.nome,
        subtitulo = alvo.endereco,
        acoes = acoes,
        aoFechar = aoFechar,
        visivel = host != null,
    )
}

@Composable
private fun Corpo(
    estado: EstadoDaHome,
    hosts: List<HostUi>,
    aoTocarHost: (HostUi) -> Unit,
    aoSegurarHost: (HostUi) -> Unit,
    motivoDoSilencio: Int,
    aoAdicionar: () -> Unit,
) {
    if (hosts.isEmpty()) {
        Procurando(motivoDoSilencio, aoAdicionar)
    } else {
        GradeDeHosts(
            hosts = hosts,
            aoTocar = aoTocarHost,
            aoSegurar = aoSegurarHost,
            temSondaEmCurso = estado.temSondaEmCurso(),
            aoVencerPrazo = { estado.reavaliarPrazos() },
        )
    }
}

// ─────────────────────────────────────────────────────────────── cabeçalho

@Composable
private fun Cabecalho(resumo: ResumoDosHosts) {
    Column(Modifier.padding(top = dimensionResource(R.dimen.vidro_space_4))) {
        BasicText(
            text = stringResource(resumo.saudacao),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_medium)),
                fontSize = 15.sp,
                color = colorResource(R.color.vidro_text_3),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_1)))
        BasicText(
            // A única frase da home. Ela muda com o que está lá fora, e a luz atrás dela
            // carrega o mesmo estado em intensidade — as duas coisas precisam concordar
            // sempre, senão a tela diz duas coisas ao mesmo tempo.
            text = resumo.titulo(),
            // O DEGRAU DE DISPLAY, e ele aparece UMA VEZ por tela.
            //
            // O maior tipo do app era 34sp. Num painel de 1722 dp isso le como timido, e
            // contraste de escala e o sinal de sofisticacao mais barato que existe --
            // principalmente numa tela que acabou de perder os cartoes e passou a se
            // sustentar em tipografia.
            //
            // Uma vez por tela e a regra, nao o gosto: duas aparicoes de display nao sao
            // enfase, sao a ausencia dela.
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_extrabold)),
                fontSize = 44.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.04).emKt(),
                color = colorResource(R.color.vidro_text),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))
    }
}

// ─────────────────────────────────────────────────────────────── continuar

/**
 * O P10: o que ele abriu por último, na primeira dobra.
 *
 * A seção estava desenhada em três arquivos e morta em todos. No tablet deitado é
 * exatamente a coluna esquerda que ficava vazia — o "espaço vazio no tablet" das anotações
 * é este buraco.
 */
@Composable
private fun Continuar(entradas: List<Historico.Entrada>, aoContinuar: (Historico.Entrada) -> Unit) {
    if (entradas.isEmpty()) {
        // Sem histórico a seção não existe. Seção vazia é pior que seção nenhuma.
        return
    }

    Rotulo(stringResource(R.string.vidro_continue))
    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))

    entradas.forEach { e ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(R.dimen.vidro_space_2))
                .clip(RoundedCornerShape(19.dp))
                .background(colorResource(R.color.vidro_surface_overlay))
                .border(
                    width = dimensionResource(R.dimen.vidro_stroke_width),
                    color = colorResource(R.color.vidro_stroke_hairline),
                    shape = RoundedCornerShape(19.dp),
                )
                .cartaoTocavel(aoTocar = { aoContinuar(e) })
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = e.appNome,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                        fontSize = 16.sp,
                        letterSpacing = (-0.02).emKt(),
                        color = colorResource(R.color.vidro_text),
                    ),
                )
                if (e.hostNome.isNotEmpty()) {
                    BasicText(
                        text = e.hostNome,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.manrope_medium)),
                            fontSize = 13.sp,
                            color = colorResource(R.color.vidro_text_3),
                        ),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_4)))
}

// ─────────────────────────────────────────────────────────────── lugares

@Composable
private fun TituloDosLugares(aoAdicionar: () -> Unit, mostrarAdicionar: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.vidro_inset_card),
                vertical = dimensionResource(R.dimen.vidro_space_2),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rotulo(stringResource(R.string.vidro_your_places), Modifier.weight(1f))
        // Dois botões para a mesma porta, nunca os dois na tela: "Adicionar" vale enquanto
        // houver lista; sem lista, quem convida é o estado de procura.
        if (mostrarAdicionar) {
            BasicText(
                text = stringResource(R.string.vidro_add),
                // 48 dp de ALVO, mesmo com 14 sp de texto: medido a 48x41,5 dp, abaixo do
                // minimo tocavel. `wrapContentHeight` mantem o texto centrado no alvo maior
                // em vez de esticar a linha de base.
                modifier = Modifier
                    .cartaoTocavel(aoTocar = { aoAdicionar() })
                    .defaultMinSize(minHeight = dimensionResource(R.dimen.vidro_touch_target))
                    .wrapContentHeight(Alignment.CenterVertically),
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                    fontSize = 14.sp,
                    // A AÇÃO PERDEU A COR, e isso é a regra e não um esquecimento.
                    //
                    // Enquanto azul quis dizer "clicável", ele não podia querer dizer
                    // "dormindo" — e dormindo é o que a lista logo abaixo precisa dizer.
                    // Interatividade passa a ser dita por elevação e pela mola do toque, que
                    // é onde ela sempre devia ter morado: `cartaoTocavel` já está aqui.
                    color = colorResource(R.color.vidro_text_2),
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────── estado de procura

/**
 * O P05: dizer por que ninguém respondeu.
 *
 * A frase de antes era uma só — "nenhum PC respondeu ainda, deixe o app aberto". Verdadeira
 * e inútil quando o motivo é que o aparelho está nos dados móveis ou com VPN ligada, e o
 * mDNS nunca vai chegar. O comentário do layout antigo já sabia disso e nada era feito.
 */
@Composable
private fun Procurando(motivo: Int, aoAdicionar: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.vidro_space_6)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // A MARCA, e este era o pior momento do app.
        //
        // O primeiro contato com o VibeArtemis era um rotulo YOUR PLACES em caixa alta
        // pairando sobre 600 px de preto sem nada embaixo -- cabecalho de coisa nenhuma. A
        // marca apagada, respirando enquanto o mDNS procura, transforma a tela quebrada na
        // tela mais calma do app.
        //
        // Cinza e nao quente: nada foi encontrado ainda, e pintar de quente antes de saber e
        // a mesma mentira que a lista evita quando esta CHECANDO.
        Marca(
            tamanho = 96.dp,
            temperatura = R.color.vidro_ausente,
            respirando = true,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_6)))

        BasicText(
            text = stringResource(R.string.vidro_searching_title),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 18.sp,
                color = colorResource(R.color.vidro_text),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_2)))
        BasicText(
            text = stringResource(if (motivo != 0) motivo else R.string.vidro_searching_sub),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_medium)),
                fontSize = 14.sp,
                color = colorResource(R.color.vidro_text_3),
            ),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.vidro_space_5)))
        BasicText(
            text = stringResource(R.string.vidro_search_by_address),
            modifier = Modifier.cartaoTocavel(aoTocar = { aoAdicionar() }),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_semibold)),
                fontSize = 15.sp,
                color = colorResource(R.color.vidro_text_2),
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────── rodapé

@Composable
private fun Rodape(nomeDasCenas: String, aoCenas: () -> Unit, aoAjustes: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.vidro_inset_card),
                vertical = dimensionResource(R.dimen.vidro_space_4),
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "Cenas · Ajustes" — no protótipo o rodapé é texto, não ícone.
        BasicText(
            text = nomeDasCenas,
            modifier = Modifier.cartaoTocavel(aoTocar = { aoCenas() }),
            style = estiloDeRodape(),
        )
        BasicText(
            text = "  ·  ",
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.manrope_medium)),
                fontSize = 14.sp,
                color = colorResource(R.color.vidro_text_3),
            ),
        )
        BasicText(
            text = stringResource(R.string.vidro_settings),
            modifier = Modifier.cartaoTocavel(aoTocar = { aoAjustes() }),
            style = estiloDeRodape(),
        )
    }
}

@Composable
private fun estiloDeRodape() = TextStyle(
    fontFamily = FontFamily(Font(R.font.manrope_semibold)),
    fontSize = 14.sp,
    color = colorResource(R.color.vidro_text_2),
)

@Composable
private fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    BasicText(
        text = texto.uppercase(),
        modifier = modifier,
        style = TextStyle(
            // VidroText.Overline — 12sp, 700-800, +0.10em, caixa alta
            fontFamily = FontFamily(Font(R.font.manrope_bold)),
            fontSize = 12.sp,
            letterSpacing = 0.10.emKt(),
            color = colorResource(R.color.vidro_text_3),
        ),
    )
}

internal fun Double.emKt(): androidx.compose.ui.unit.TextUnit =
    androidx.compose.ui.unit.TextUnit(
        this.toFloat(),
        androidx.compose.ui.unit.TextUnitType.Em,
    )
