package com.limelight.ui.compose

import androidx.annotation.StringRes
import com.limelight.R

/**
 * O papel da cena — para que esta sessão serve.
 *
 * É a primeira das quatro coisas que a especificação diz que uma cena define: *"Cada uma
 * define papel, tela, taxa e entrada"*. No protótipo as três cenas de exemplo são
 * "Trabalho", "Jogo à noite" e "Só entrada", e a primeira palavra de cada uma é justamente
 * o papel.
 *
 * **Papel é a única das quatro que não tem uma chave de preferência só sua hoje.** Tela,
 * taxa e entrada são `list_resolution`, `list_fps` e `mouse_mode_list`; o papel decide o que
 * a cena abre — a área de trabalho ou o jogo — e, junto com isso, se faz sentido pedir ao
 * host que otimize as configurações do jogo (`checkbox_enable_sops`). Quem liga a tela ao
 * modelo resolve isso; a tela só conhece as duas palavras.
 */
enum class PapelDaCena(@StringRes val rotulo: Int) {
    DESKTOP(R.string.vidro_scene_role_desktop),
    JOGO(R.string.vidro_scene_role_game),
}

/**
 * O que o dedo é nesta cena.
 *
 * Os rótulos são os **da folha de sessão**, não sinônimos novos: `vidro_mouse_multitouch` e
 * `vidro_mouse_trackpad` são os mesmos chips que `vidro_session_sheet.xml` mostra em MODO DO
 * MOUSE, e `vidro_onscreen_controller` é a mesma linha que ela mostra em NA TELA. Quem
 * ajustou a folha no meio da sessão e vem salvar uma cena tem de reencontrar as palavras que
 * acabou de ler — inventar "Toque", "Mouse virtual" e "Gamepad" aqui faria a mesma escolha
 * ter dois nomes dependendo da tela.
 *
 * São três, e não os seis modos de mouse do app: os que faltam — absoluto, botões trocados,
 * desativado — são preferência de quem usa mouse físico, e mouse físico não muda de cena.
 *
 * No disco isto vira `mouse_mode_list` ("0" para multitoque, "2" para trackpad) e, no caso do
 * controle, `checkbox_show_onscreen_controls`. A conversão mora na ponte, porque `CONTROLE`
 * não é um valor de `mouse_mode_list` e um campo `valor: String` aqui teria de mentir para
 * um dos três.
 */
enum class ModoDeEntrada(@StringRes val rotulo: Int) {
    TOQUE(R.string.vidro_mouse_multitouch),
    TRACKPAD(R.string.vidro_mouse_trackpad),
    CONTROLE(R.string.vidro_onscreen_controller),
}

/**
 * Uma cena sendo editada — o PE06.
 *
 * O modelo antigo (`SettingsProfile.options`) é um `Map<String, Object>` com **as 126
 * preferências** dentro, porque `EditProfileActivity` grava `inMemoryPrefs.getAll()` inteiro.
 * Este aqui tem quatro campos e um nome, e a razão não é economia: é que uma cena é uma
 * **intenção**, não um retrato do app num instante. Retrato congela junto tudo o que a
 * pessoa não escolheu, e congela invisivelmente — foi por isso que ninguém salvou cena
 * nenhuma.
 *
 * `tela` e `taxa` são `String` de propósito, e são exatamente os valores que vão para a
 * preferência: `"1920x1080"` e `"60"`, os itens de `resolution_values` e `fps_values` em
 * `res/values/arrays.xml`. Um enum não serviria — `addNativeResolutionEntries` acrescenta em
 * runtime a resolução nativa do aparelho, que não está em nenhuma lista fixa, e é justamente
 * a que o dono do fork usa no Vibeshine.
 */
data class CenaEmEdicao(
    val nome: String,
    val papel: PapelDaCena,
    val tela: String,
    val taxa: String,
    val entrada: ModoDeEntrada,
)
