package com.limelight.ui.compose

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.preference.PreferenceManager
import com.limelight.preferences.PreferenceConfiguration

/**
 * A porta pela qual o Java entra na tela de Ajustes. Ver PonteDaHome para o porquê.
 *
 * Esta é a maior das pontes, e a razão é que ela carrega três coisas que a tela não tem como
 * saber: **onde escrever**, **o que uma escrita significa** e **o que uma Porta abre**.
 *
 * A tela é uma função de `List<TelaDeAjustes>` — ela não conhece `SharedPreferences`, não
 * conhece `Intent` e não conhece `PreferenceConfiguration`. Todo o resto mora aqui.
 */
object PonteDeAjustes {

    /** Modo em vigor quando o toque foi desligado, para religar no mesmo lugar. */
    private const val MODO_ANTERIOR = "mouse_mode_list_before_touch_off"

    /** É uma vista de `mouse_mode_list`, não um booleano com chave própria. */
    private const val TOQUE_COMO_MOUSE = "checkbox_touchscreen_as_mouse"

    @JvmStatic
    @JvmName("montar")
    fun montar(
        view: ComposeView,
        /**
         * A categoria em que a tela abre, ou nulo para abrir no índice.
         *
         * O caminho normal é nulo: a pessoa sempre chega pelo índice. Existe para quem chega
         * de fora apontando para um lugar — hoje a captura de tela, amanhã um atalho.
         */
        telaInicial: String?,
        /** Chamado quando o Voltar chega no índice — a Activity fecha aqui. */
        aoSair: () -> Unit,
        /**
         * As nove Portas, cada uma com um destino diferente.
         *
         * Fica no Java porque todas as nove são coisas que a casca não faz: um `Intent`
         * explícito, um seletor de arquivo, uma confirmação destrutiva, um navegador. Passar
         * a chave e deixar o dono decidir é a fronteira mais fina possível aqui.
         */
        aoTocarPorta: (String) -> Unit,
    ) {
        val contexto = view.context

        view.setContent {
            // A ÁRVORE É RELIDA A CADA ESCRITA, e isso não é desperdício.
            //
            // Ela é ~97 objetos de dados montados a partir de um mapa em memória; o custo é
            // irrelevante ao lado do que ela compra: um dependente esmaece no MESMO quadro em
            // que o dono desliga, e uma escolha normalizada (o modo do mouse) volta pela mesma
            // função que a leu. A alternativa — mutar o item no lugar — teria dois donos do
            // mesmo valor e é exatamente o defeito que a tela antiga tinha.
            var geracao by remember { mutableStateOf(0) }
            val telas = remember(geracao) {
                peneirarPeloAparelho(contexto, lerArvoreDeAjustes(contexto))
            }

            var abertaEm by remember {
                mutableStateOf(telas.firstOrNull { it.chave == telaInicial })
            }

            // A categoria aberta precisa ser reencontrada por chave depois de uma escrita: o
            // objeto anterior ficou obsoleto no instante em que a árvore foi relida, e manter
            // a referência velha congelaria a tela no estado de antes do toque.
            val aberta = abertaEm?.let { alvo -> telas.firstOrNull { it.chave == alvo.chave } }

            TelaAjustes(
                telas = telas,
                abertaEm = aberta,
                aoVoltar = {
                    if (aberta == null) aoSair() else abertaEm = null
                },
                aoAbrirTela = { abertaEm = it },
                aoMudarInterruptor = { chave, ligado ->
                    escreverInterruptor(contexto, chave, ligado)
                    geracao++
                },
                aoMudarEscolha = { chave, valor ->
                    escreverEscolha(contexto, chave, valor)
                    geracao++
                },
                aoMudarDeslizante = { chave, valor ->
                    prefs(contexto).edit().putInt(chave, valor).apply()
                    geracao++
                },
                aoTocarPorta = aoTocarPorta,
            )
        }
    }

    /**
     * Onde a tela de Ajustes escreve.
     *
     * É o arquivo padrão, e **não** o `getOverlayingSharedPreferences` do gerenciador de
     * cenas. A sobreposição serve a quem vai abrir uma sessão: ela mostra o valor da cena
     * ativa por cima do valor real. Quem está editando precisa ver e mexer no de baixo — ler
     * pela sobreposição e gravar no arquivo faria a linha voltar sozinha ao valor anterior no
     * quadro seguinte, sem ninguém entender por quê.
     */
    private fun prefs(contexto: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(contexto)

    /**
     * Um interruptor, e a exceção que existe.
     *
     * `checkbox_touchscreen_as_mouse` não tem chave própria: ele é uma leitura de
     * `mouse_mode_list`, onde `4` quer dizer "o toque não move o mouse". Gravar um booleano
     * cru na chave dele criaria dois donos do mesmo estado — e a folha da sessão, que grava o
     * modo direto ao tocar um chip, dessincronizaria um deles em silêncio.
     *
     * Desligar guarda o modo que estava valendo; religar devolve esse mesmo modo, e não o
     * multitoque — sem essa memória, quem usa trackpad perderia a escolha a cada volta.
     */
    private fun escreverInterruptor(contexto: Context, chave: String, ligado: Boolean) {
        val p = prefs(contexto)
        if (chave != TOQUE_COMO_MOUSE) {
            p.edit().putBoolean(chave, ligado).apply()
            return
        }

        val modo = PreferenceConfiguration.MOUSE_MODE_PREF_STRING
        if (ligado) {
            val anterior = p.getString(MODO_ANTERIOR, PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH)
            p.edit().putString(modo, anterior).apply()
        } else {
            val agora = p.getString(modo, PreferenceConfiguration.MOUSE_MODE_MULTI_TOUCH)
            p.edit()
                .putString(MODO_ANTERIOR, agora)
                .putString(modo, PreferenceConfiguration.MOUSE_MODE_TOUCH_DISABLED)
                .apply()
        }
    }

    /**
     * Uma escolha, e a consequência que trocar de tela ou de quadros tem.
     *
     * A taxa de bits padrão sai da resolução e dos quadros — não é um número fixo. Mudar a
     * resolução sem refazer a conta deixa a pessoa com a taxa da resolução ANTERIOR, que é
     * baixa demais para subir ou alta demais para caber, e nos dois casos o defeito aparece
     * na imagem e não aqui. A tela antiga fazia isso num listener; ele viria junto ou o corte
     * seria uma regressão.
     */
    private fun escreverEscolha(contexto: Context, chave: String, valor: String) {
        val p = prefs(contexto)
        p.edit().putString(chave, valor).apply()

        val resolucao = PreferenceConfiguration.RESOLUTION_PREF_STRING
        val quadros = PreferenceConfiguration.FPS_PREF_STRING
        if (chave != resolucao && chave != quadros) return

        val res = if (chave == resolucao) valor
        else p.getString(resolucao, PreferenceConfiguration.DEFAULT_RESOLUTION)
        val fps = if (chave == quadros) valor
        else p.getString(quadros, PreferenceConfiguration.DEFAULT_FPS)

        p.edit()
            .putInt(
                PreferenceConfiguration.BITRATE_PREF_STRING,
                PreferenceConfiguration.getDefaultBitrate(res, fps),
            )
            .apply()
    }
}
