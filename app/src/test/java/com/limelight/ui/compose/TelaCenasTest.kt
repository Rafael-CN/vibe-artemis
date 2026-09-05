package com.limelight.ui.compose

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.limelight.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Os guardas de Cenas, portados para Compose.
 *
 * Vinham de `ProfilesActivityUiTest` e falavam por `findViewById` sobre um `RecyclerView`
 * com `ViewHolder`. Os contratos continuam valendo, e cada um nasceu de um defeito:
 *
 *  - o botão de criar cena já foi um FAB circular e já mudou de forma duas vezes; o que o
 *    teste sempre guardou é que ele **leva ao editor**, não como ele se parece;
 *  - a seta de voltar era o que faltava em Cenas — a tela só saía pela tecla do sistema;
 *  - tocar numa cena tem de trocar a cena ATIVA, e apagar tem de tirar a linha da lista.
 *
 * O que este arquivo NÃO cobre, e o antigo cobria: a persistência real em `ProfilesManager`.
 * Aquilo é comportamento da Activity, não da tela, e continua em `ProfilesActivityUiTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-port-night-xhdpi")
class TelaCenasTest {

    @get:Rule
    val compose = createComposeRule()

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun cena(id: String, nome: String, ativa: Boolean = false) =
        CenaUi(id, nome, "hoje", ativa)

    @Test
    fun `sem cena nenhuma a tela explica em vez de ficar em branco`() {
        compose.setContent {
            TelaCenas(emptyList(), {}, {}, {}, {}, {})
        }
        compose.onNodeWithText(ctx.getString(R.string.vidro_scenes_empty_title)).assertIsDisplayed()
    }

    @Test
    fun `o botao do rodape leva ao editor`() {
        var criou = false
        compose.setContent {
            TelaCenas(emptyList(), {}, {}, {}, {}, { criou = true })
        }
        compose.onNodeWithText(ctx.getString(R.string.vidro_scene_save)).performClick()
        assertTrue("criar cena precisa levar a algum lugar", criou)
    }

    @Test
    fun `a saida desenhada encerra a tela`() {
        var voltou = false
        compose.setContent {
            TelaCenas(emptyList(), { voltou = true }, {}, {}, {}, {})
        }
        compose.onNodeWithText(ctx.getString(R.string.vidro_return)).performClick()
        assertTrue("Cenas precisa de saída desenhada", voltou)
    }

    @Test
    fun `tocar numa cena troca a ativa`() {
        var escolhida: String? = null
        compose.setContent {
            TelaCenas(
                listOf(cena("1", "Uma", ativa = true), cena("2", "Outra")),
                {}, { escolhida = it.id }, {}, {}, {},
            )
        }
        // useUnmergedTree: mira o NOME, que e onde o dedo vai. No mergido, o alvo e o card
        // inteiro e o clique cai no centro dele -- que numa lista com "Editar" e "Remover" a
        // direita pode nao ser o lugar que a pessoa tocaria.
        compose.onNodeWithText("Outra", useUnmergedTree = true).performClick()
        assertEquals("2", escolhida)
    }

    @Test
    fun `apagar aponta a cena certa`() {
        // Antes isto era o ImageButton de lixeira dentro do ViewHolder — 24dp, sem rótulo,
        // abaixo do alvo mínimo. Agora é texto, e o teste pode achá-lo pelo que ele diz.
        var apagada: String? = null
        compose.setContent {
            TelaCenas(listOf(cena("42", "Some")), {}, {}, {}, { apagada = it.id }, {})
        }
        compose.onNodeWithText(ctx.getString(R.string.vidro_scene_delete)).performClick()
        assertEquals("42", apagada)
    }

    @Test
    fun `a cena ativa se anuncia`() {
        compose.setContent {
            TelaCenas(listOf(cena("1", "Noite", ativa = true)), {}, {}, {}, {}, {})
        }
        compose.onNodeWithText(ctx.getString(R.string.vidro_scene_active)).assertIsDisplayed()
    }
}
