package com.limelight.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.limelight.R
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.PairingManager
import com.limelight.ui.VidroLightView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Os guardas da home, portados para Compose.
 *
 * Eles existiam em `PcViewEmptyStateTest` e `ProfilesNavigationTest` e falavam por
 * `findViewById` — que não alcança um composable. Os contratos, porém, continuam valendo
 * exatamente iguais, e cada um deles nasceu de um defeito real:
 *
 *  - o título de estado abria vazio para quem não tinha host nenhum, porque ele era escrito
 *    no primeiro polling e quem não tem host nunca recebe um;
 *  - a saída pelo endereço tinha virado uma segunda porta permanente, pendurada no vazio
 *    entre o último cartão e o rodapé;
 *  - o acesso a Cenas já derrubou a home com `ClassCastException` depois de um redesenho.
 *
 * Portar em vez de apagar é o ponto: uma migração que perde os guardas troca defeito
 * conhecido por defeito esquecido.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-port-night-xhdpi")
class TelaHomeTest {

    @get:Rule
    val compose = createComposeRule()

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun montar(
        estado: EstadoDaHome,
        aoCenas: () -> Unit = {},
        aoAdicionar: () -> Unit = {},
        motivo: Int = 0,
    ) {
        compose.setContent {
            TelaHome(
                estado = estado,
                continuar = emptyList(),
                aoTocarHost = {},
                aoSegurarHost = {},
                aoContinuar = {},
                aoAdicionar = aoAdicionar,
                aoCenas = aoCenas,
                aoAjustes = {},
                nomeDasCenas = ctx.getString(R.string.vidro_scenes),
                motivoDoSilencio = motivo,
            )
        }
    }

    private fun host(uuid: String, nome: String, estado: ComputerDetails.State) =
        ComputerDetails().apply {
            this.uuid = uuid
            this.name = nome
            this.state = estado
            this.pairState = PairingManager.PairState.PAIRED
        }

    @Test
    fun `sem host nenhum a home mostra o estado de procura e um titulo`() {
        montar(EstadoDaHome())

        compose.onNodeWithText(ctx.getString(R.string.vidro_searching_title)).assertIsDisplayed()

        // O título não pode abrir vazio: quem não tem host nenhum jamais receberia um
        // evento de polling para preenchê-lo.
        val resumo = resumoDosHosts(emptyList())
        assertEquals(R.string.vidro_host_none, tituloRes(resumo))
        assertEquals(VidroLightView.INTENSITY_NONE, resumo.intensidade, 0.001f)
        compose.onNodeWithText(ctx.getString(R.string.vidro_host_none)).assertIsDisplayed()
    }

    @Test
    fun `a saida pelo endereco vive dentro do estado de procura`() {
        var abriu = false
        montar(EstadoDaHome(), aoAdicionar = { abriu = true })

        compose.onNodeWithText(ctx.getString(R.string.vidro_search_by_address))
            .assertIsDisplayed()
            .performClick()
        assertTrue("a saída pelo endereço precisa levar a algum lugar", abriu)
    }

    @Test
    fun `com host na lista a saida pelo endereco sai da tela`() {
        // Ela era a segunda porta para a mesma AddComputerManually do "Adicionar" do
        // cabeçalho, visível junto com ele o tempo todo. Nunca as duas ao mesmo tempo.
        val estado = EstadoDaHome()
        estado.aoResponder(host("a", "PC-RAFAEL", ComputerDetails.State.ONLINE), 0L)
        montar(estado)

        compose.onNodeWithText(ctx.getString(R.string.vidro_search_by_address))
            .assertDoesNotExist()
        compose.onNodeWithText(ctx.getString(R.string.vidro_add)).assertIsDisplayed()
    }

    @Test
    fun `o rodape leva a cenas`() {
        var foi = false
        montar(EstadoDaHome(), aoCenas = { foi = true })

        compose.onNodeWithText(ctx.getString(R.string.vidro_scenes)).performClick()
        assertTrue("o rodapé precisa levar a Cenas", foi)
    }

    @Test
    fun `o motivo do silencio troca a frase de apoio`() {
        // P05: a frase genérica só vale quando não há nada mais útil a dizer.
        montar(EstadoDaHome(), motivo = R.string.vidro_why_vpn)
        compose.onNodeWithText(ctx.getString(R.string.vidro_why_vpn)).assertIsDisplayed()
    }

    @Test
    fun `um lugar aceso nomeia o lugar no titulo`() {
        // O singular indefinido dizia "o PC" mesmo quando a lista abaixo mostrava dois.
        val estado = EstadoDaHome()
        estado.aoResponder(host("a", "PC-RAFAEL", ComputerDetails.State.ONLINE), 0L)
        montar(estado)

        compose.onNodeWithText(
            ctx.getString(R.string.vidro_host_awake_one, "PC-RAFAEL")
        ).assertIsDisplayed()
    }

    /** Qual string o resumo escolheria, sem precisar de um Composable para perguntar. */
    private fun tituloRes(r: ResumoDosHosts): Int = when {
        r.acordados == 1 && !r.nomeDoAcordado.isNullOrEmpty() -> R.string.vidro_host_awake_one
        r.acordados > 0 -> R.plurals.vidro_host_awake_many
        r.aindaProcurando -> R.string.vidro_host_searching
        r.algumDormindo -> R.string.vidro_host_asleep
        else -> R.string.vidro_host_none
    }

}
