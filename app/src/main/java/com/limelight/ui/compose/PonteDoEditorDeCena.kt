package com.limelight.ui.compose

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.limelight.preferences.PreferenceConfiguration

/**
 * A porta pela qual o Java entra no editor de cena — o PE06.
 *
 * A tela conhece cinco campos; o disco conhece cento e poucas chaves. A tradução entre os
 * dois mora aqui, e ela é a parte interessante desta ponte: **papel** e **entrada** não são
 * preferências, são leituras de duas ou três preferências juntas.
 *
 * O modelo antigo gravava `inMemoryPrefs.getAll()` inteiro — as 111 chaves — porque uma cena
 * era um retrato do app num instante. Isto não muda: o editor continua salvando o mapa
 * inteiro, e é isso que faz "Avançado" continuar funcionando. O que muda é que as quatro
 * coisas que a pessoa realmente escolhe têm uma tela onde escolher, em vez de estarem
 * dissolvidas no meio das outras cento e sete.
 */
object PonteDoEditorDeCena {

    private const val CONTROLE_NA_TELA = "checkbox_show_onscreen_controls"
    private const val OTIMIZAR_JOGO = "checkbox_enable_sops"

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        /** As preferências EM MEMÓRIA da cena, não as do aparelho. */
        prefs: SharedPreferences,
        nomeInicial: String,
        nova: Boolean,
        aoVoltar: () -> Unit,
        /** O nome digitado, que não é preferência: ele é da cena. */
        aoMudarNome: (String) -> Unit,
        aoSalvar: () -> Unit,
        aoAbrirAvancado: () -> Unit,
    ) {
        val contexto = view.context

        view.setContent {
            // As listas oferecidas saem da MESMA árvore que a tela de Ajustes usa, e não de
            // uma leitura própria dos arrays. É o que faz a resolução nativa do aparelho —
            // injetada em runtime, ausente de qualquer array fixo, e justamente a que o dono
            // do fork usa — aparecer aqui sem uma segunda cópia da regra para desatualizar.
            val arvore = remember { lerArvoreDeAjustes(contexto) }
            val telas = remember(arvore) { valoresDe(arvore, "list_resolution") }
            val taxas = remember(arvore) { valoresDe(arvore, "list_fps") }

            var cena by remember { mutableStateOf(lerCena(prefs, nomeInicial)) }

            TelaEditorDeCena(
                cena = cena,
                nova = nova,
                telas = telas,
                taxas = taxas,
                aoVoltar = aoVoltar,
                aoMudar = { nova ->
                    cena = nova
                    gravarCena(prefs, nova)
                    aoMudarNome(nova.nome)
                },
                aoSalvar = aoSalvar,
                aoAbrirAvancado = aoAbrirAvancado,
            )
        }
    }

    private fun valoresDe(arvore: List<TelaDeAjustes>, chave: String): List<String> =
        arvore.asSequence()
            .flatMap { it.itens.asSequence() }
            .filterIsInstance<AjusteUi.Escolha>()
            .firstOrNull { it.chave == chave }
            ?.valores
            .orEmpty()

    /**
     * As cinco coisas, lidas de onde elas realmente moram.
     *
     * `papel` e `entrada` não têm chave própria e por isso são derivados:
     *
     *  - **Papel** sai de `checkbox_enable_sops`. "Otimizar as configurações do jogo" é uma
     *    pergunta que só existe quando há um jogo; ligada, a cena é de jogo. Não é uma chave
     *    inventada para isto — é a única do arquivo que já significava exatamente isso.
     *  - **Entrada** é lida em duas etapas porque o controle na tela e o modo do mouse são
     *    chaves diferentes que a pessoa vive como uma escolha só. Controle desenhado ligado
     *    vence: quem o escolheu não está usando o dedo como mouse.
     */
    private fun lerCena(prefs: SharedPreferences, nome: String) = CenaEmEdicao(
        nome = nome,
        papel = if (prefs.getBoolean(OTIMIZAR_JOGO, true)) PapelDaCena.JOGO
        else PapelDaCena.DESKTOP,
        tela = prefs.getString(
            PreferenceConfiguration.RESOLUTION_PREF_STRING,
            PreferenceConfiguration.DEFAULT_RESOLUTION,
        ).orEmpty(),
        taxa = prefs.getString(
            PreferenceConfiguration.FPS_PREF_STRING,
            PreferenceConfiguration.DEFAULT_FPS,
        ).orEmpty(),
        entrada = when {
            prefs.getBoolean(CONTROLE_NA_TELA, false) -> ModoDeEntrada.CONTROLE
            prefs.getString(
                PreferenceConfiguration.MOUSE_MODE_PREF_STRING,
                PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH,
            ) == MODO_TRACKPAD -> ModoDeEntrada.TRACKPAD
            else -> ModoDeEntrada.TOQUE
        },
    )

    /**
     * E de volta ao disco, a cada toque.
     *
     * Grava sempre as quatro, e não só a que mudou. A tela devolve a cena inteira justamente
     * para tornar isto possível: quatro gravações idênticas custam nada num arquivo em
     * memória, e a alternativa — descobrir qual campo mudou — é onde nasce o bug de gravar
     * três e esquecer a quarta.
     *
     * O caso do controle desenhado é o único com duas chaves: escolhê-lo liga o controle e
     * deixa o modo do mouse como estava, porque ele volta a valer assim que o controle sair.
     * Escolher toque ou trackpad desliga o controle — senão a cena diria uma coisa e a sessão
     * mostraria outra por cima.
     */
    private fun gravarCena(prefs: SharedPreferences, cena: CenaEmEdicao) {
        val e = prefs.edit()
        e.putString(PreferenceConfiguration.RESOLUTION_PREF_STRING, cena.tela)
        e.putString(PreferenceConfiguration.FPS_PREF_STRING, cena.taxa)
        e.putBoolean(OTIMIZAR_JOGO, cena.papel == PapelDaCena.JOGO)

        when (cena.entrada) {
            ModoDeEntrada.CONTROLE -> e.putBoolean(CONTROLE_NA_TELA, true)
            ModoDeEntrada.TOQUE -> {
                e.putBoolean(CONTROLE_NA_TELA, false)
                e.putString(
                    PreferenceConfiguration.MOUSE_MODE_PREF_STRING,
                    PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH,
                )
            }
            ModoDeEntrada.TRACKPAD -> {
                e.putBoolean(CONTROLE_NA_TELA, false)
                e.putString(PreferenceConfiguration.MOUSE_MODE_PREF_STRING, MODO_TRACKPAD)
            }
        }
        e.apply()
    }

    /**
     * "2" — o trackpad.
     *
     * A constante de `PreferenceConfiguration` é do pacote e este arquivo não é. O número
     * está aqui em vez de ser exportado de lá porque abrir mais um campo daquela classe para
     * um único uso é pior que um literal com o nome do que ele significa; a folha da sessão,
     * `Game.applyMouseMode` e a lista de Ajustes já falam por este mesmo índice.
     */
    private const val MODO_TRACKPAD = "2"
}
