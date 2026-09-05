package com.limelight.ui.compose

import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.limelight.R
import com.limelight.preferences.PreferenceConfiguration
import kotlin.math.roundToInt

/**
 * A árvore de Ajustes, escrita à mão.
 *
 * `res/xml/preferences.xml` continua existindo enquanto a tela do `androidx.preference` não
 * for desligada, mas ele **não é lido aqui**. Um parser em runtime devolveria uma árvore que
 * ninguém consegue ler no editor, que o compilador não confere e que diverge em silêncio no
 * dia em que alguém mexer no XML e esquecer do outro lado — e o modo de falhar é o pior que
 * existe numa tela de Ajustes: a preferência some, o comportamento continua, e não há nada
 * na tela que explique. Transcrita, a árvore é código: o `R.string` errado não compila, a
 * chave duplicada aparece na busca, e o diff mostra o que mudou.
 *
 * O preço é este arquivo ser longo. É o preço certo.
 *
 * ## O que foi cortado, e por quem
 *
 * A poda é do catálogo de produto, não desta migração. Boa parte dela **já estava aplicada
 * ao XML** quando esta árvore foi escrita — os dois campos de texto duplicados (P15), os
 * quatro controles de estereoscopia e os experimentais mortos (P16), os seis modos de mouse
 * reduzidos a três (P17), a lista dos cinco "Profile 1" (P22), o "lembrar o modo do mouse"
 * (P23). O que sobrou para cortar aqui está marcado item por item, no lugar onde a linha
 * deixou de existir. Nada foi cortado por gosto de quem transcreveu.
 *
 * ## O que esta função NÃO faz, e alguém precisa fazer
 *
 * `StreamSettings.initializePreferences()` remove itens conforme o **aparelho**: sem
 * `FEATURE_TOUCHSCREEN` somem as duas telas de controles na tela; sem vibrador somem
 * `checkbox_vibrate_osc`, `checkbox_vibrate_keyboard`, `checkbox_vibrate_fallback`,
 * `checkbox_enable_device_rumble` e `seekbar_vibrate_fallback_strength`; sem
 * `FEATURE_USB_HOST` somem `checkbox_usb_driver` e `checkbox_usb_bind_all`; abaixo do
 * Android 12 somem os três de sensor de movimento; abaixo do Android 8 some
 * `checkbox_enable_pip`; sem HDR no decodificador some `checkbox_enable_hdr`.
 *
 * Nada disso está aqui. Esta função devolve a árvore inteira, e quem a monta é que precisa
 * peneirar — inclusive o P14, que peneira pelo **host** e não pelo aparelho, e que precisa
 * de uma resposta (`HostCapabilities`) que a assinatura `(Context)` não tem como receber.
 * Não inventar essa peneira aqui é deliberado: filtro escondido dentro de um leitor é
 * exatamente a divergência silenciosa que motivou transcrever a árvore.
 */
fun lerArvoreDeAjustes(contexto: Context): List<TelaDeAjustes> {
    // A MESMA fonte em que a tela escreve.
    //
    // `PreferenceConfiguration` lê pelo `getOverlayingSharedPreferences`, que sobrepõe as
    // opções da cena ativa por cima das preferências reais — leitura correta para quem vai
    // abrir uma sessão, e errada para quem vai EDITAR: a tela mostraria o valor da cena e
    // gravaria no arquivo de baixo, e a linha voltaria sozinha ao valor anterior no próximo
    // desenho. Aqui, ler e escrever têm de ser o mesmo lugar.
    val prefs = PreferenceManager.getDefaultSharedPreferences(contexto)

    // A ordem do índice é a do XML, e ela já está certa: Vídeo e Áudio primeiro, porque são
    // as duas telas que mudam o que a pessoa vê e ouve. O que este arquivo reordena é o
    // DENTRO de cada tela.
    return listOf(
        telaDeVideo(contexto, prefs),
        telaDeAudio(contexto, prefs),
        telaDeControle(contexto, prefs),
        telaDeEntrada(contexto, prefs),
        telaDeHost(prefs),
        telaGeral(prefs),
        telaDeInterface(contexto, prefs),
        telaDeControlesNaTela(contexto, prefs),
        telaDeTeclasEspeciais(contexto, prefs),
        telaAvancada(contexto, prefs),
        telaDeDiversos(),
    )
    // TELAS QUE NÃO ESTÃO NESTA LISTA, e por quê:
    //
    // `category_virtual_trackpad_settings` — P18. Sete itens, e três deles eram
    // "Sensibilidade do touchpad X", "Sensibilidade do trackpad X" e "Ajuste de sensibilidade
    // da tela X": três nomes quase iguais, em duas telas, para a mesma pergunta. A linha
    // inteira sai do índice.
    //
    // `category_perf_monitor_settings` — P19. Seis itens para governar oito linhas de inglês
    // cru derramadas sobre o vídeo. O resumo de uma delas era "Útil com Exibir no topo-centro
    // e Modo reduzido ligados" — uma preferência cuja ajuda é a combinação de outras duas. O
    // relatório continua vivo em Ajustes › Depuração, que é onde se lê parado.
}

// ─── Vídeo ──────────────────────────────────────────────────────────────────────────────
//
// A tela mais usada dos Ajustes, e a que mais ganha com a reordenação: as seis decisões que
// mudam o que a pessoa vê estavam espalhadas entre caixas de compatibilidade. HDR era a
// ÚLTIMA linha das catorze; agora é a quinta.

private fun telaDeVideo(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_video_settings",
    titulo = R.string.category_video_settings,
    resumo = R.string.vidro_index_video,
    itens = listOf(
        // As cinco primeiras são a razão de a tela existir: resolução, quadros, taxa de
        // bits, ritmo e HDR. Nesta ordem, e antes de qualquer outra coisa.
        listaDeResolucoes(contexto, prefs),
        listaDeQuadros(contexto, prefs),
        prefs.deslizante(
            chave = "seekbar_bitrate_kbps",
            titulo = R.string.title_seekbar_bitrate,
            minimo = 500, maximo = 300_000, passo = 500,
            // Sem `defaultValue` no XML de propósito: o padrão da taxa de bits sai da
            // resolução e dos quadros escolhidos, não de um número fixo.
            padrao = PreferenceConfiguration.getDefaultBitrate(contexto),
            sufixo = SUFIXO_TAXA_DE_BITS,
            divisor = KBPS_POR_MBPS,
        ),
        contexto.escolha(
            prefs,
            chave = "frame_pacing",
            titulo = R.string.title_frame_pacing,
            resumo = R.string.summary_frame_pacing,
            rotulos = R.array.video_frame_pacing_names,
            valores = R.array.video_frame_pacing_values,
            padrao = "latency",
        ),
        prefs.interruptor(
            chave = "checkbox_enable_hdr",
            titulo = R.string.title_enable_hdr,
            resumo = R.string.summary_enable_hdr,
            padrao = false,
        ),

        // Ainda é o que ele vê — como a imagem preenche a tela e em que escala —, mas são
        // decisões de uma vez só, não de toda sessão.
        contexto.escolha(
            prefs,
            chave = "list_video_scale_mode",
            titulo = R.string.title_video_scale_mode,
            // SEM RESUMO, e nao por engano: `summary_video_scale_mode` e literalmente "%s".
            //
            // E a convencao do ListPreference do androidx, que substitui o %s pela entrada
            // escolhida na hora de desenhar. Fora daquele componente ninguem substitui nada, e
            // a linha mostra o marcador cru. Aqui os tres chips ja estao a vista com um deles
            // aceso, entao o resumo nao tinha o que acrescentar mesmo se funcionasse.
            //
            // Duas traducoes (fr, ru, vi) escreveram uma frase de verdade no lugar do %s. Elas
            // sao de contribuidores e a regra 5 do AGENTS.md as protege; a perda e uma linha de
            // ajuda em tres idiomas, contra um "%s" visivel em todos os outros.
            rotulos = R.array.video_scale_mode_names,
            valores = R.array.video_scale_mode_values,
            padrao = "fill",
        ),
        prefs.deslizante(
            chave = "seekbar_resolution_scale_factor",
            titulo = R.string.title_resolution_scale_factor,
            resumo = R.string.summary_resolution_scale_factor,
            minimo = 20, maximo = 200, passo = 1, padrao = 100,
            sufixo = "%",
        ),
        // A taxa em rede limitada é a mesma decisão da taxa de bits, mas só vale quando a
        // rede é limitada. Desce por isso, não por ser menos importante.
        prefs.deslizante(
            chave = "seekbar_metered_bitrate_kbps",
            titulo = R.string.title_seekbar_metered_bitrate,
            resumo = R.string.summary_seekbar_metered_bitrate,
            minimo = 0, maximo = 300_000, passo = 500, padrao = 0,
            sufixo = SUFIXO_TAXA_DE_BITS,
            divisor = KBPS_POR_MBPS,
            // Zero aqui é "deixa comigo", não "sem vídeo".
            zeroEhAuto = true,
        ),

        // Daqui para baixo, o que muda como o app negocia a tela com o host e com o
        // aparelho. Nada aqui é decisão de todo dia.
        prefs.interruptor(
            chave = "checkbox_use_virtual_display",
            titulo = R.string.title_checkbox_use_virtual_display,
            resumo = R.string.summary_checkbox_use_virtual_display,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_auto_orientation",
            titulo = R.string.title_auto_orientation,
            resumo = R.string.summary_auto_orientation,
            padrao = false,
        ),
        // Governado por `checkbox_auto_orientation` no XML. O modelo não tem como dizer
        // "depende de", então a única coisa que sobra é mantê-lo COLADO no dono, e depois
        // dele: uma caixa dependente longe da que a governa é uma caixa que não faz nada e
        // não explica por quê.
        prefs.interruptor(
            chave = "checkbox_auto_invert_video_resolution",
            titulo = R.string.title_checkbox_auto_invert_video_resolution,
            resumo = R.string.summary_checkbox_auto_invert_video_resolution,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_enforce_display_mode",
            titulo = R.string.title_checkbox_enforce_display_mode,
            resumo = R.string.summary_checkbox_enforce_display_mode,
            padrao = false,
        ),
        // Campo de texto livre, e o modelo não tem essa forma. Vira Porta: quem monta a tela
        // abre o editor que quiser e grava a chave. A alternativa — espremer um float numa
        // Escolha de valores fixos — tiraria a capacidade em vez de adiar a forma dela.
        // A taxa gravada aqui também aparece como uma entrada da lista de quadros acima.
        AjusteUi.Porta(
            chave = "custom_refresh_rate",
            titulo = R.string.title_custom_refresh_rate,
            resumo = R.string.summary_custom_refresh_rate,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_view_top_center",
            titulo = R.string.title_top_center_display,
            resumo = R.string.summary_top_center_display,
            padrao = false,
        ),
    ),
    // FORA daqui, e não por decisão desta migração:
    //
    // P15 — `edit_diy_bitrate` e `edit_diy_w_h`, os dois campos de texto que gravavam na
    // mesma chave dos controles vizinhos. Já haviam saído do XML.
    // P16 — `render_mode_list`, `parallax_depth`, `convergence_ratio` e `balance_shift` (os
    // quatro de estereoscopia), `checkbox_forceTightThresholds` (morto: lido por reflection
    // sobre um campo que não existe, B27), `ultra_low_latency` e o balanço de quadros. Já
    // haviam saído do XML. "Fully External Display Mode" mudou de tela e está em Avançado.
)

// ─── Áudio ──────────────────────────────────────────────────────────────────────────────

private fun telaDeAudio(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_audio_settings",
    titulo = R.string.category_audio_settings,
    resumo = R.string.vidro_index_audio,
    itens = listOf(
        // Canais de áudio é uma das seis decisões que mudam o que a pessoa percebe, e já
        // era a primeira linha. Nada a reordenar nesta tela.
        contexto.escolha(
            prefs,
            chave = "list_audio_config",
            titulo = R.string.title_audio_config_list,
            resumo = R.string.summary_audio_config_list,
            rotulos = R.array.audio_config_names,
            valores = R.array.audio_config_values,
            padrao = "2",
        ),
        prefs.interruptor(
            chave = "checkbox_enable_audiofx",
            titulo = R.string.title_checkbox_enable_audiofx,
            resumo = R.string.summary_checkbox_enable_audiofx,
            padrao = false,
        ),
    ),
)

// ─── Controle ───────────────────────────────────────────────────────────────────────────
//
// Nenhuma das seis decisões de imagem mora aqui, então o critério vira o equivalente do
// tato: o que se sente na mão primeiro. Zona morta e vibração sobem; o driver USB e o
// conserto de Joy-Con descem, porque são coisas que se resolve uma vez na vida do aparelho.

private fun telaDeControle(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_gamepad_settings",
    titulo = R.string.category_gamepad_settings,
    resumo = R.string.vidro_index_gamepad,
    itens = listOf(
        prefs.deslizante(
            chave = "seekbar_deadzone",
            titulo = R.string.title_seekbar_deadzone,
            resumo = R.string.summary_seekbar_deadzone,
            // O mínimo NEGATIVO é o do XML, não engano de transcrição: a barra aceita
            // reduzir abaixo do que o driver informa.
            minimo = -20, maximo = 20, passo = 1, padrao = 5,
            sufixo = contexto.getString(R.string.suffix_seekbar_deadzone),
        ),
        prefs.interruptor(
            chave = "checkbox_multi_controller",
            titulo = R.string.title_checkbox_multi_controller,
            resumo = R.string.summary_checkbox_multi_controller,
            padrao = true,
        ),

        // A família da vibração, inteira e em ordem de dependência: cada uma governa a
        // seguinte no XML.
        prefs.interruptor(
            chave = "checkbox_enable_rumble",
            titulo = R.string.title_enable_rumble,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_vibrate_fallback",
            titulo = R.string.title_checkbox_vibrate_fallback,
            resumo = R.string.summary_checkbox_vibrate_fallback,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_device_rumble",
            titulo = R.string.title_device_rumble,
            resumo = R.string.summary_device_rumble,
            padrao = false,
        ),
        prefs.deslizante(
            chave = "seekbar_vibrate_fallback_strength",
            titulo = R.string.title_seekbar_vibrate_fallback_strength,
            // Sem `seekbar:min` no XML — o mínimo do `SeekBarPreference` é 1, não 0.
            minimo = 1, maximo = 200, passo = 1, padrao = 100,
            sufixo = contexto.getString(R.string.suffix_seekbar_vibrate_fallback_strength),
        ),

        // O controle virando mouse, e a rolagem que vem junto dele.
        prefs.interruptor(
            chave = "checkbox_mouse_emulation",
            titulo = R.string.title_checkbox_mouse_emulation,
            resumo = R.string.summary_checkbox_mouse_emulation,
            padrao = true,
        ),
        contexto.escolha(
            prefs,
            chave = "analog_scrolling",
            titulo = R.string.title_analog_scrolling,
            rotulos = R.array.analog_scrolling_names,
            valores = R.array.analog_scrolling_values,
            padrao = "right",
        ),
        prefs.interruptor(
            chave = "checkbox_flip_face_buttons",
            titulo = R.string.title_checkbox_flip_face_buttons,
            resumo = R.string.summary_checkbox_flip_face_buttons,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_gamepad_touchpad_as_mouse",
            titulo = R.string.title_checkbox_gamepad_touchpad_as_mouse,
            resumo = R.string.summary_checkbox_gamepad_touchpad_as_mouse,
            padrao = false,
        ),

        // Sensores e bateria: o aparelho ou tem, ou não tem. Quem monta a tela peneira.
        prefs.interruptor(
            chave = "checkbox_gamepad_motion_sensors",
            titulo = R.string.title_checkbox_gamepad_motion_sensors,
            resumo = R.string.summary_checkbox_gamepad_motion_sensors,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_gamepad_motion_fallback",
            titulo = R.string.title_checkbox_gamepad_motion_fallback,
            resumo = R.string.summary_checkbox_gamepad_motion_fallback,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_force_device_motion",
            titulo = R.string.title_checkbox_force_device_motion,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_gamepad_enable_battery_report",
            titulo = R.string.title_checkbox_gamepad_enable_battery_report,
            resumo = R.string.summary_checkbox_gamepad_enable_battery_report,
            padrao = true,
        ),

        // O fundo da tela: coisa que se decide uma vez, quando o controle novo chega.
        prefs.interruptor(
            chave = "checkbox_usb_driver",
            titulo = R.string.title_checkbox_xb1_driver,
            resumo = R.string.summary_checkbox_xb1_driver,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_usb_bind_all",
            titulo = R.string.title_checkbox_usb_bind_all,
            resumo = R.string.summary_checkbox_usb_bind_all,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_joyconfix",
            titulo = R.string.title_joyconfix,
            resumo = R.string.summary_joyconfix,
            padrao = false,
        ),
    ),
)

// ─── Entrada ────────────────────────────────────────────────────────────────────────────
//
// O modo do mouse abre a tela porque é a escolha que muda o que o dedo faz. Logo abaixo vem
// o teclado — o E01, a dor principal deste fork: como o IME trata a tela (empurrar em vez de
// cobrir) e o envio de texto de uma vez. No XML os dois estavam no fim, depois de doze
// linhas de trackpad.

private fun telaDeEntrada(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_input_settings",
    titulo = R.string.category_input_settings,
    resumo = R.string.vidro_index_input,
    itens = listOf(
        // Uma VISTA da chave do modo, não um estado próprio.
        //
        // `checkbox_touchscreen_as_mouse` não tem valor guardado que se possa acreditar: ele
        // é derivado de `mouse_mode_list`, onde o valor 4 significa "o toque não move o
        // mouse". Ler o booleano cru daria dois donos do mesmo estado — e a folha da sessão,
        // que grava a chave do modo direto ao tocar um chip, dessincronizaria o booleano sem
        // ninguém perceber. É o mesmo cuidado que `StreamSettings` toma ao abrir a tela.
        AjusteUi.Interruptor(
            chave = "checkbox_touchscreen_as_mouse",
            titulo = R.string.title_touchscreen_as_mouse,
            resumo = R.string.summary_touchscreen_as_mouse,
            ligado = toqueMoveOMouse(prefs),
        ),
        // Três modos, os mesmos três da folha de sessão (P17, já aplicado ao array).
        // O escolhido é normalizado: ver `modoDeMouseVigente`.
        contexto.escolha(
            prefs,
            chave = "mouse_mode_list",
            titulo = R.string.title_touchscreen_mode,
            resumo = R.string.summary_touchscreen_mode,
            rotulos = R.array.mouse_mode_names,
            valores = R.array.mouse_mode_values,
            padrao = MODO_MOUSE_MULTITOQUE,
            escolhido = modoDeMouseVigente(prefs),
        ),

        // O teclado. Sobe por ser a dor principal do dono do fork, e porque o modo de
        // exibição do IME muda literalmente o que aparece na tela durante a sessão.
        contexto.escolha(
            prefs,
            chave = "list_ime_display_mode",
            titulo = R.string.title_ime_display_mode,
            rotulos = R.array.ime_display_mode_names,
            valores = R.array.ime_display_mode_values,
            padrao = "resize",
        ),
        prefs.interruptor(
            chave = "checkbox_enable_commit_text",
            titulo = R.string.title_enable_commit_text,
            resumo = R.string.summary_enable_commit_text,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_force_qwerty",
            titulo = R.string.title_checkbox_force_qwerty,
            resumo = R.string.summary_checkbox_force_qwerty,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_back_as_meta",
            titulo = R.string.title_checkbox_back_as_meta,
            resumo = R.string.summary_back_as_meta,
            padrao = false,
        ),

        // A sensibilidade do toque, que se ajusta uma vez e se esquece.
        prefs.deslizante(
            chave = "seekbar_trackpad_sensitivity_x",
            titulo = R.string.title_trackpad_sensitivity_x,
            resumo = R.string.summary_trackpad_sensitivity_x,
            minimo = -200, maximo = 200, passo = 10, padrao = 100,
            sufixo = "%",
        ),
        prefs.deslizante(
            chave = "seekbar_trackpad_sensitivity_y",
            titulo = R.string.title_trackpad_sensitivity_y,
            resumo = R.string.summary_trackpad_sensitivity_y,
            minimo = -200, maximo = 200, passo = 10, padrao = 100,
            sufixo = "%",
        ),

        prefs.interruptor(
            chave = "checkbox_multi_touch_gestures",
            titulo = R.string.title_checkbox_multi_touch_gestures,
            resumo = R.string.summary_checkbox_multi_touch_gestures,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_mouse_local_cursor",
            titulo = R.string.title_checkbox_mouse_local_cursor,
            resumo = R.string.summary_checkbox_mouse_local_cursor,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_mouse_nav_buttons",
            titulo = R.string.title_checkbox_mouse_nav_buttons,
            resumo = R.string.summary_checkbox_mouse_nav_buttons,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_allow_portrait_session",
            titulo = R.string.title_allow_portrait_session,
            resumo = R.string.summary_allow_portrait_session,
            padrao = true,
        ),

        // O trackpad FÍSICO — a capa. Os três andam juntos: o interruptor da correção
        // (B65), a troca de eixos e a calibração por gesto. O catálogo os protege
        // explicitamente da tesoura: preferência que existe porque o hardware mente é
        // preferência legítima, e a capa do S8 Ultra se anuncia sempre deitada.
        prefs.interruptor(
            chave = "checkbox_physical_trackpad_native",
            titulo = R.string.title_physical_trackpad_native,
            resumo = R.string.summary_physical_trackpad_native,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_trackpad_swap_axis",
            titulo = R.string.title_trackpad_swap_axis,
            resumo = R.string.summary_trackpad_swap_axis,
            padrao = false,
        ),
        // A única linha desta tela que abre outra tela do app.
        AjusteUi.Porta(
            chave = "touchpad_calibration_launch",
            titulo = R.string.title_touchpad_calibration,
            resumo = R.string.summary_touchpad_calibration,
        ),

        prefs.interruptor(
            chave = "checkbox_trackpad_drag_drop_vibration",
            titulo = R.string.title_trackpad_drag_drop_vibration,
            resumo = R.string.summary_trackpad_drag_drop_vibration,
            padrao = false,
        ),
        prefs.deslizante(
            chave = "seekbar_trackpad_drag_drop_threshold",
            titulo = R.string.title_trackpad_drag_drop_threshold,
            minimo = 0, maximo = 2000, passo = 1, padrao = 250,
            sufixo = "ms",
        ),

        // O fundo: duas trocas de tecla que se decide uma vez e nunca mais.
        prefs.interruptor(
            chave = "checkbox_back_as_guide",
            titulo = R.string.title_back_as_guide,
            resumo = R.string.summary_back_as_guide,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_ignore_synth_events",
            titulo = R.string.title_checkbox_ignore_synth_events,
            resumo = R.string.summary_ignore_synth_events,
            padrao = false,
        ),
    ),
    // FORA daqui: `checkbox_absolute_mouse_mode` e o "modo de mouse para área de trabalho
    // remota" (P17) e `checkbox_remember_mouse_mode` (P23) — os três eram caminhos paralelos
    // para estados que a lista acima e a folha da sessão já governam. Já haviam saído do XML.
)

// ─── Host ───────────────────────────────────────────────────────────────────────────────

private fun telaDeHost(prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_host_settings",
    titulo = R.string.category_host_settings,
    resumo = R.string.vidro_index_host,
    itens = listOf(
        // O primeiro muda a resolução que o JOGO usa no host — é o que ele vê. Já era a
        // primeira linha.
        prefs.interruptor(
            chave = "checkbox_enable_sops",
            titulo = R.string.title_checkbox_enable_sops,
            resumo = R.string.summary_checkbox_enable_sops,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_host_audio",
            titulo = R.string.title_checkbox_host_audio,
            resumo = R.string.summary_checkbox_host_audio,
            padrao = false,
        ),
    ),
)

// ─── Geral ──────────────────────────────────────────────────────────────────────────────

private fun telaGeral(prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_general_settings",
    titulo = R.string.category_general_settings,
    resumo = R.string.vidro_index_general,
    itens = listOf(
        // A ordem do XML já é a corrente da causa: sincronizar, avisar que sincronizou,
        // esconder o que o aviso mostra. Trocar qualquer uma de lugar quebraria a leitura.
        //
        // Esta é a tela que o P14 faz sumir inteira contra um host sem sincronia de área de
        // transferência — o que o Vibeshine não faz. Sumir é decisão de quem monta a tela,
        // com a resposta do host na mão; aqui ela continua completa.
        prefs.interruptor(
            chave = "checkbox_smart_clipboard_sync",
            titulo = R.string.title_smart_clipboard_sync,
            resumo = R.string.summary_smart_clipboard_sync,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_smart_clipboard_sync_toast",
            titulo = R.string.title_smart_clipboard_sync_toast,
            resumo = R.string.summary_smart_clipboard_sync_toast,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_hide_clipboard_content",
            titulo = R.string.title_hide_clipboard_content,
            resumo = R.string.summary_hide_clipboard_content,
            padrao = true,
        ),
    ),
)

// ─── Interface ──────────────────────────────────────────────────────────────────────────
//
// Sobem as quatro que mudam o que aparece na tela — tela cheia, tamanho das capas, PiP e o
// reenquadramento lembrado. As sete linhas de teclado desenhado descem juntas, em bloco:
// quem não usa o teclado do app não precisa passar por elas para chegar em nada.

private fun telaDeInterface(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_ui_settings",
    titulo = R.string.category_ui_settings,
    resumo = R.string.vidro_index_ui,
    itens = listOf(
        prefs.interruptor(
            chave = "checkbox_full_screen",
            titulo = R.string.title_checkbox_full_screen,
            padrao = true,
        ),
        // Muda o tamanho das capas na home inteira. Estava na penúltima linha.
        AjusteUi.Interruptor(
            chave = "checkbox_small_icon_mode",
            titulo = R.string.title_checkbox_small_icon_mode,
            ligado = prefs.getBoolean(
                "checkbox_small_icon_mode",
                // Sem `defaultValue` no XML: `SmallIconCheckboxPreference` decide pelo
                // tamanho da tela e por ser ou não uma TV.
                PreferenceConfiguration.getDefaultSmallMode(contexto),
            ),
        ),
        // Nasce ligado, e não é sobre assistir: sem PiP, `onStop()` derruba a conexão — uma
        // notificação tocada e a sessão acaba.
        prefs.interruptor(
            chave = "checkbox_enable_pip",
            titulo = R.string.title_checkbox_enable_pip,
            resumo = R.string.summary_checkbox_enable_pip,
            padrao = true,
        ),
        // P23 olhou para esta e decidiu que ela FICA, nascendo ligada: reenquadrar a tela é
        // trabalho manual, e perdê-lo a cada sessão é o mesmo desperdício de reescolher o
        // modo do mouse. Continua sendo pergunta porque zoom herdado sem aviso surpreende.
        prefs.interruptor(
            chave = "checkbox_remember_zoom_pan",
            titulo = R.string.title_checkbox_remember_zoom_pan,
            resumo = R.string.summary_checkbox_remember_zoom_pan,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_quit_dialog",
            titulo = R.string.title_quit_dialog,
            resumo = R.string.summary_quit_dialog,
            padrao = true,
        ),

        // O teclado que o app desenha, em bloco.
        prefs.interruptor(
            chave = "onscreen_keyboard_autofit",
            titulo = R.string.title_onscreen_keyboard_autofit,
            resumo = R.string.summary_onscreen_keyboard_autofit,
            padrao = false,
        ),
        prefs.deslizante(
            chave = "seekbar_onscreen_keyboard_height",
            titulo = R.string.title_onscreen_keyboard_height,
            minimo = 150, maximo = 400, passo = 10, padrao = 200,
            sufixo = "dp",
        ),
        prefs.deslizante(
            chave = "seekbar_onscreen_keyboard_width",
            titulo = R.string.title_onscreen_keyboard_width,
            minimo = 150, maximo = 1000, passo = 10, padrao = 1000,
            sufixo = "dp",
        ),
        contexto.escolha(
            prefs,
            chave = "list_onscreen_keyboard_align_mode",
            titulo = R.string.title_onscreen_keyboard_align_mode,
            rotulos = R.array.onscreen_keyboard_align_modes,
            valores = R.array.onscreen_keyboard_align_values,
            padrao = "center",
        ),
        prefs.deslizante(
            chave = "seekbar_keyboard_axi_opacity",
            titulo = R.string.title_keyboard_opacity,
            minimo = 0, maximo = 100, passo = 1, padrao = 90,
            sufixo = "%",
        ),
        prefs.interruptor(
            chave = "checkbox_enable_sticky_modifier_key_virtual_keyboard",
            titulo = R.string.title_checkbox_enable_sticky_modifier_key_virtual_keyboard,
            resumo = R.string.summary_checkbox_enable_sticky_modifier_key_virtual_keyboard,
            padrao = true,
        ),

        // Os dois avisos, no fim.
        prefs.interruptor(
            chave = "checkbox_disable_warnings",
            titulo = R.string.title_checkbox_disable_warnings,
            resumo = R.string.summary_checkbox_disable_warnings,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_post_stream_toast",
            titulo = R.string.title_enable_post_stream_toast,
            padrao = false,
        ),
    ),
    // FORA daqui, e já fora do XML: "Retomar rápido" (virou o comportamento do app), o
    // "Botão flutuante" e o botão de zoom sobre o vídeo — os dois discos que a ilha e o
    // punho substituíram (P24).
)

// ─── Controles na tela ──────────────────────────────────────────────────────────────────

private fun telaDeControlesNaTela(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_onscreen_controls",
    titulo = R.string.category_on_screen_controls_settings,
    resumo = R.string.vidro_index_osc,
    itens = listOf(
        prefs.interruptor(
            chave = "checkbox_show_onscreen_controls",
            titulo = R.string.title_checkbox_show_onscreen_controls,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_hide_osc_when_has_gamepad",
            titulo = R.string.title_hide_osc_when_has_gamepad,
            padrao = true,
        ),
        // A opacidade sobe: é o que decide se o controle desenhado atrapalha ou não ver o
        // jogo. Estava em sétimo, depois de cinco caixas de composição.
        prefs.deslizante(
            chave = "seekbar_osc_opacity",
            titulo = R.string.dialog_title_osc_opacity,
            minimo = 0, maximo = 100, passo = 1, padrao = 90,
            sufixo = contexto.getString(R.string.suffix_osc_opacity),
        ),
        prefs.interruptor(
            chave = "checkbox_onscreen_style_official",
            titulo = R.string.title_checkbox_onscreen_style_official,
            resumo = R.string.summary_checkbox_onscreen_style_official,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_only_show_L3R3",
            titulo = R.string.title_only_l3r3,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_show_guide_button",
            titulo = R.string.title_show_guide_button,
            padrao = true,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_analog_stick_new",
            titulo = R.string.title_free_analog_stick,
            resumo = R.string.summary_free_analog_stick,
            padrao = false,
        ),
        prefs.deslizante(
            chave = "seekbar_osc_free_analog_stick_opacity",
            titulo = R.string.title_free_analog_stick_opacity,
            minimo = 0, maximo = 100, passo = 1, padrao = 20,
            sufixo = contexto.getString(R.string.suffix_osc_opacity),
        ),
        prefs.interruptor(
            chave = "checkbox_vibrate_osc",
            titulo = R.string.title_checkbox_vibrate_osc,
            padrao = true,
        ),
        // Destrutiva, e por isso é a última linha: apagar o layout inteiro não pode ficar no
        // caminho de quem só queria mexer na opacidade.
        AjusteUi.Porta(
            chave = "option_reset_osc_preference",
            titulo = R.string.title_reset_osc,
        ),
    ),
)

// ─── Teclas especiais ───────────────────────────────────────────────────────────────────

private fun telaDeTeclasEspeciais(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_special_key_layout",
    titulo = R.string.title_special_key_layout,
    resumo = R.string.vidro_index_special_keys,
    itens = listOf(
        prefs.interruptor(
            chave = "checkbox_enable_keyboard",
            titulo = R.string.title_show_virtual_key,
            resumo = R.string.summary_show_virtual_key,
            padrao = false,
        ),
        // P22: as cinco opções diziam todas "Profile 1" porque o array apontava cinco vezes
        // para a mesma string. O array já foi corrigido; aqui a lista só é lida.
        contexto.escolha(
            prefs,
            chave = "keyboard_axi_list",
            titulo = R.string.title_default_layout_scheme,
            resumo = R.string.summary_default_layout_scheme,
            rotulos = R.array.keyboard_axi_names,
            valores = R.array.keyboard_axi_values,
            padrao = "OSC_Keyboard",
        ),
        prefs.interruptor(
            chave = "checkbox_enable_keyboard_square",
            titulo = R.string.title_square_buttons,
            resumo = R.string.summary_square_buttons,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_vibrate_keyboard",
            titulo = R.string.title_checkbox_vibrate_osc,
            resumo = R.string.summary_vibrate_keyboard,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_enable_clear_default_special_button",
            titulo = R.string.title_clear_default_special_button,
            resumo = R.string.summary_clear_default_special_button,
            padrao = false,
        ),
        AjusteUi.Porta(
            chave = "import_keyboard_file",
            titulo = R.string.title_import_keyboard_layout,
            resumo = R.string.summary_import_keyboard_layout,
        ),
        AjusteUi.Porta(
            chave = "export_keyboard_file",
            titulo = R.string.title_export_keyboard_layout,
            resumo = R.string.summary_export_keyboard_layout,
        ),
        // Destrutiva, por último — mesma regra da tela de controles na tela.
        AjusteUi.Porta(
            chave = "option_reset_osc",
            titulo = R.string.title_reset_keyboard_layout,
            resumo = R.string.summary_reset_keyboard_layout,
        ),
    ),
)

// ─── Avançado ───────────────────────────────────────────────────────────────────────────

private fun telaAvancada(contexto: Context, prefs: SharedPreferences) = TelaDeAjustes(
    chave = "category_advanced_settings",
    titulo = R.string.category_advanced_settings,
    resumo = R.string.vidro_index_advanced,
    itens = listOf(
        // Codec, quadros destravados e faixa de cor mudam o que ele vê — mesmo aqui, onde
        // tudo é avançado, essas três vêm primeiro.
        contexto.escolha(
            prefs,
            chave = "video_format",
            titulo = R.string.title_video_format,
            rotulos = R.array.video_format_names,
            valores = R.array.video_format_values,
            padrao = "auto",
        ),
        prefs.interruptor(
            chave = "checkbox_unlock_fps",
            titulo = R.string.title_unlock_fps,
            resumo = R.string.summary_unlock_fps,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_full_range",
            titulo = R.string.title_full_range,
            resumo = R.string.summary_full_range,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_reduce_refresh_rate",
            titulo = R.string.title_checkbox_reduce_refresh_rate,
            resumo = R.string.summary_checkbox_reduce_refresh_rate,
            padrao = false,
        ),
        prefs.interruptor(
            chave = "checkbox_prevent_packet_loss",
            titulo = R.string.title_prevent_packet_loss,
            resumo = R.string.summary_prevent_packet_loss,
            padrao = false,
        ),
        // Veio de Vídeo (P16) e é a ÚLTIMA linha de propósito: o resumo dele diz que combina
        // tudo automaticamente e ignora os ajustes manuais. Um interruptor que anula os
        // vizinhos não pode ficar acima deles.
        prefs.interruptor(
            chave = "checkbox_enable_fullexdisplay",
            titulo = R.string.title_fullexdisplay_mode,
            resumo = R.string.summary_fullexdisplay_mode,
            padrao = false,
        ),
    ),
)

// ─── Diversos ───────────────────────────────────────────────────────────────────────────

private fun telaDeDiversos() = TelaDeAjustes(
    chave = "category_settings_misc",
    titulo = R.string.title_settings_misc,
    resumo = R.string.vidro_index_misc,
    itens = listOf(
        // A depuração primeiro: é a única linha desta tela que alguém procura com pressa,
        // porque é a que se abre quando algo quebrou e é preciso pedir ajuda (P21).
        AjusteUi.Porta(
            chave = "pref_debug_info",
            titulo = R.string.title_debug_info,
            resumo = R.string.summary_debug_info,
        ),
        AjusteUi.Porta(
            chave = "import_special_button_file",
            titulo = R.string.title_import_special_button,
            resumo = R.string.summary_import_special_button,
        ),
        AjusteUi.Porta(
            chave = "option_software_release",
            titulo = R.string.title_software_update,
            resumo = R.string.summary_software_update,
        ),
    ),
    // FORA daqui:
    //
    // P20 — `list_languages`. O Android já dá esta tela: o manifesto declara
    // `android:localeConfig` e `Settings.ACTION_APP_LOCALE_SETTINGS` leva à página de idioma
    // do sistema, que é onde a pessoa espera trocar o idioma de um app. Manter uma lista de
    // vinte e três idiomas dentro do app é manter uma segunda resposta para a mesma pergunta
    // — e a de dentro não sabe do que o sistema já escolheu. Abaixo do Android 13 o caminho
    // do sistema não existe; quem montar a tela decide se acrescenta a Porta nesse caso, e
    // essa é uma decisão de tela, não de árvore.
    //
    // Já fora do XML, também por P20: o segundo link de atualização e os dois atalhos para o
    // wiki do projeto de onde este veio.
)

// ─── Leitura ────────────────────────────────────────────────────────────────────────────

/**
 * A taxa de bits é guardada em kbps, e o modelo não tem divisor.
 *
 * O XML mostra Mbps dividindo o valor por 1000 só na hora de desenhar — `seekbar:divisor`. O
 * `AjusteUi.Deslizante` não tem esse campo, e a conta NÃO pode ser feita aqui: o que a tela
 * devolver em `aoMudarDeslizante` é o que vai para a mesma chave que `Game.java` lê como
 * kbps, e um fator de mil silencioso ali seria a pior espécie de erro — o vídeo continua
 * funcionando, só que na taxa errada. Então o número fica cru e o sufixo diz a verdade sobre
 * ele. Ver o relato: o contrato precisa de `divisor` para esta linha voltar a dizer "45 Mbps".
 */
// A taxa de bits mora em kbps e se fala em Mbps. O espaço vive dentro do sufixo porque
// quem concatena não tem como saber qual unidade quer colar no número e qual não quer.
private const val SUFIXO_TAXA_DE_BITS = " Mbps"
private const val KBPS_POR_MBPS = 1000

private const val CHAVE_RESOLUCAO = "list_resolution"
private const val CHAVE_QUADROS = "list_fps"
private const val CHAVE_TAXA_PERSONALIZADA = "custom_refresh_rate"
private const val PADRAO_RESOLUCAO = "1280x720"
private const val PADRAO_QUADROS = "60"

private const val CHAVE_MODO_MOUSE = "mouse_mode_list"
private const val CHAVE_MODO_MOUSE_ANTERIOR = "mouse_mode_list_before_touch_off"
private const val MODO_MOUSE_MULTITOQUE = "0"
private const val MODO_MOUSE_ABSOLUTO = "1"
private const val MODO_MOUSE_TRACKPAD = "2"
private const val MODO_MOUSE_TOQUE_DESLIGADO = "4"

private fun SharedPreferences.interruptor(
    chave: String,
    titulo: Int,
    resumo: Int = 0,
    padrao: Boolean,
) = AjusteUi.Interruptor(chave, titulo, resumo, getBoolean(chave, padrao))

private fun SharedPreferences.deslizante(
    chave: String,
    titulo: Int,
    resumo: Int = 0,
    minimo: Int,
    maximo: Int,
    passo: Int,
    padrao: Int,
    sufixo: String = "",
    divisor: Int = 1,
    zeroEhAuto: Boolean = false,
) = AjusteUi.Deslizante(
    chave, titulo, resumo, minimo, maximo, passo, getInt(chave, padrao), sufixo,
    divisor, zeroEhAuto,
)

private fun Context.escolha(
    prefs: SharedPreferences,
    chave: String,
    titulo: Int,
    resumo: Int = 0,
    rotulos: Int,
    valores: Int,
    padrao: String,
    /** Só para quem precisa normalizar o valor guardado antes de mostrá-lo. */
    escolhido: String? = null,
): AjusteUi.Escolha {
    val listaDeRotulos = resources.getStringArray(rotulos).toMutableList()
    val listaDeValores = resources.getStringArray(valores).toMutableList()
    val atual = escolhido ?: prefs.getString(chave, padrao) ?: padrao
    garantirEscolhido(listaDeRotulos, listaDeValores, atual)
    return AjusteUi.Escolha(chave, titulo, resumo, listaDeRotulos, listaDeValores, atual)
}

/**
 * O valor guardado SEMPRE aparece na lista.
 *
 * Uma lista que não contém o próprio valor atual não tem nada marcado, e aí a primeira
 * escolha da pessoa não é uma troca: é a descoberta de que a preferência dela já tinha sido
 * jogada fora sem aviso. Não é hipótese — `list_resolution` guarda a resolução nativa do
 * aparelho, que não está no array; `list_fps` guarda a taxa personalizada; e um perfil
 * importado de outro aparelho traz o que quiser.
 */
private fun garantirEscolhido(
    rotulos: MutableList<String>,
    valores: MutableList<String>,
    escolhido: String,
    rotuloExtra: (String) -> String = { it },
) {
    if (valores.contains(escolhido)) return
    rotulos.add(rotuloExtra(escolhido))
    valores.add(escolhido)
}

/**
 * A lista de resoluções, com a do aparelho dentro dela.
 *
 * A entrada nativa não vem do XML: hoje quem a injeta é `StreamSettings`. Ela precisa existir
 * aqui porque o P15 apagou `edit_diy_w_h` — o campo de texto que era a outra forma de pedir
 * uma resolução fora da lista — justamente com o argumento de que a nativa já é injetada. Sem
 * isto, o corte deixaria de ser "duas portas para o mesmo valor" e passaria a ser uma porta a
 * menos.
 *
 * O que continua faltando em relação ao Java é a peneira pelo decodificador (`MediaCodecInfo`
 * e os modos suportados do `Display`), que RETIRA da lista o que o aparelho não decodifica.
 * Isso pede um `Activity` e a inicialização do `MediaCodecHelper`, e está relatado.
 */
private fun listaDeResolucoes(contexto: Context, prefs: SharedPreferences): AjusteUi.Escolha {
    val rotulos = contexto.resources.getStringArray(R.array.resolution_names).toMutableList()
    val valores = contexto.resources.getStringArray(R.array.resolution_values).toMutableList()

    val (largura, altura) = tamanhoRealDaTela(contexto)
    if (largura > 0 && altura > 0) {
        if (PreferenceConfiguration.isSquarishScreen(largura, altura)) {
            // Tela quase quadrada entra duas vezes, de pé e deitada: no Tab S8 Ultra as duas
            // formas são resoluções diferentes de verdade, e pedir a errada ao host custa
            // imagem cortada.
            contexto.acrescentarResolucao(rotulos, valores, altura, largura,
                R.string.resolution_prefix_native_portrait)
            contexto.acrescentarResolucao(rotulos, valores, largura, altura,
                R.string.resolution_prefix_native_landscape)
        } else {
            contexto.acrescentarResolucao(rotulos, valores, largura, altura, 0)
        }
    }

    val escolhido = prefs.getString(CHAVE_RESOLUCAO, PADRAO_RESOLUCAO) ?: PADRAO_RESOLUCAO
    garantirEscolhido(rotulos, valores, escolhido) {
        contexto.getString(R.string.resolution_prefix_custom) + " (" + it + ")"
    }
    return AjusteUi.Escolha(
        chave = CHAVE_RESOLUCAO,
        titulo = R.string.title_resolution_list,
        opcoes = rotulos,
        valores = valores,
        escolhido = escolhido,
    )
}

private fun Context.acrescentarResolucao(
    rotulos: MutableList<String>,
    valores: MutableList<String>,
    largura: Int,
    altura: Int,
    /** `R.string` da orientação, ou 0 quando a tela não é quase quadrada. */
    orientacao: Int,
) {
    val valor = largura.toString() + "x" + altura.toString()
    if (valores.contains(valor)) return
    var rotulo = getString(R.string.resolution_prefix_native)
    if (orientacao != 0) {
        rotulo += " " + getString(orientacao)
    }
    rotulos.add(rotulo + " (" + valor + ")")
    valores.add(valor)
}

/**
 * A lista de quadros, com a taxa do aparelho e a personalizada dentro dela.
 *
 * `custom_refresh_rate` não governa nada sozinha: ela injeta UMA entrada nesta lista, e
 * escolher essa entrada é que grava a taxa em `list_fps`. Por isso ela aparece como Porta em
 * Vídeo e como opção aqui — é uma preferência só, vista de dois lugares.
 */
private fun listaDeQuadros(contexto: Context, prefs: SharedPreferences): AjusteUi.Escolha {
    val rotulos = contexto.resources.getStringArray(R.array.fps_names).toMutableList()
    val valores = contexto.resources.getStringArray(R.array.fps_values).toMutableList()

    val nativa = taxaDeAtualizacao(contexto)
    if (nativa > 0) {
        contexto.acrescentarQuadros(rotulos, valores, nativa.toString(),
            R.string.resolution_prefix_native)
    }
    val personalizada = prefs.getString(CHAVE_TAXA_PERSONALIZADA, null)?.toFloatOrNull()
    if (personalizada != null && personalizada > 0f) {
        contexto.acrescentarQuadros(rotulos, valores, personalizada.toString(),
            R.string.resolution_prefix_custom)
    }

    val escolhido = prefs.getString(CHAVE_QUADROS, PADRAO_QUADROS) ?: PADRAO_QUADROS
    garantirEscolhido(rotulos, valores, escolhido) {
        contexto.getString(R.string.resolution_prefix_custom) + " (" + it + " " +
            contexto.getString(R.string.fps_suffix_fps) + ")"
    }
    return AjusteUi.Escolha(
        chave = CHAVE_QUADROS,
        titulo = R.string.title_fps_list,
        opcoes = rotulos,
        valores = valores,
        escolhido = escolhido,
    )
}

private fun Context.acrescentarQuadros(
    rotulos: MutableList<String>,
    valores: MutableList<String>,
    valor: String,
    prefixo: Int,
) {
    if (valores.contains(valor)) return
    rotulos.add(getString(prefixo) + " (" + valor + " " + getString(R.string.fps_suffix_fps) + ")")
    valores.add(valor)
}

/**
 * O modo que a lista deve mostrar, que nem sempre é o que está gravado.
 *
 * Dois valores guardados não são modos:
 *
 * - **4** é o estado "o toque não move o mouse" — o que `checkbox_touchscreen_as_mouse`
 *   desligado grava. O modo a mostrar é o que estava em vigor quando o toque foi desligado,
 *   guardado em `mouse_mode_list_before_touch_off`. Sem essa memória, religar o toque
 *   devolveria multitoque a quem usava trackpad.
 * - **3** e **5** são os modos que o P17 tirou da lista. `PreferenceConfiguration` migra os
 *   dois ao ler; um sobrevivente no disco viraria um `escolhido` que não está entre os
 *   valores, e a lista abriria sem nada marcado.
 */
private fun modoDeMouseVigente(prefs: SharedPreferences): String {
    var modo = prefs.getString(CHAVE_MODO_MOUSE, MODO_MOUSE_MULTITOQUE) ?: MODO_MOUSE_MULTITOQUE
    if (modo == MODO_MOUSE_TOQUE_DESLIGADO) {
        modo = prefs.getString(CHAVE_MODO_MOUSE_ANTERIOR, MODO_MOUSE_MULTITOQUE)
            ?: MODO_MOUSE_MULTITOQUE
    }
    return when (modo) {
        MODO_MOUSE_MULTITOQUE, MODO_MOUSE_ABSOLUTO, MODO_MOUSE_TRACKPAD -> modo
        "3" -> MODO_MOUSE_TRACKPAD           // trackpad de jogo caía na família do trackpad
        "5" -> MODO_MOUSE_ABSOLUTO           // absoluto com botões trocados, na do absoluto
        else -> MODO_MOUSE_MULTITOQUE        // perfil importado ou preferência adulterada
    }
}

/** O toque move o mouse sempre que o modo guardado não é o 4. Ver `modoDeMouseVigente`. */
private fun toqueMoveOMouse(prefs: SharedPreferences): Boolean =
    prefs.getString(CHAVE_MODO_MOUSE, MODO_MOUSE_MULTITOQUE) != MODO_MOUSE_TOQUE_DESLIGADO

/**
 * A tela de verdade, não a janela do app.
 *
 * `resources.displayMetrics` devolve o tamanho da JANELA, que em tela dividida é menor que a
 * tela — e a resolução que se pede ao host é a da tela. `getRealMetrics` é o que responde
 * isso, e é o que `StreamSettings` usa. Depreciado a partir do Android 30 e sem substituto
 * que sirva com `minSdk` 21.
 */
@Suppress("DEPRECATION")
private fun tamanhoRealDaTela(contexto: Context): Pair<Int, Int> {
    val tela = telaPadrao(contexto) ?: return 0 to 0
    val metrica = DisplayMetrics()
    tela.getRealMetrics(metrica)
    // Normalizado para deitado, como o Java faz: há aparelho que reporta em pé.
    return maxOf(metrica.widthPixels, metrica.heightPixels) to
        minOf(metrica.widthPixels, metrica.heightPixels)
}

@Suppress("DEPRECATION")
private fun taxaDeAtualizacao(contexto: Context): Int =
    telaPadrao(contexto)?.refreshRate?.roundToInt() ?: 0

@Suppress("DEPRECATION")
private fun telaPadrao(contexto: Context): Display? =
    (contexto.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
