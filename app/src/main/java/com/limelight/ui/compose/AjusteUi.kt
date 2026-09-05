package com.limelight.ui.compose

/** Um item da tela de Ajustes. `chave` é a chave de SharedPreferences. */
sealed interface AjusteUi {
    val chave: String
    val titulo: Int          // R.string
    val resumo: Int          // R.string, ou 0

    /**
     * A chave do interruptor que manda neste item, quando há um.
     *
     * Vinte e tantas preferências declaram `android:dependency` no XML, e sem isto a tela
     * desenha um deslizante que a pessoa pode arrastar sem que nada aconteça — o pior tipo
     * de defeito de interface, porque ele não parece um defeito.
     *
     * É lido de uma tabela e não de um campo de construtor de propósito: o XML já mantém
     * essa relação num lugar só, e trinta parâmetros novos espalhados pelos call sites da
     * árvore seriam trinta chances de esquecer um. A tabela abaixo é a mesma lista que
     * `grep android:dependency preferences.xml` devolve.
     */
    val dependeDe: String? get() = DONOS[chave]

    data class Interruptor(
        override val chave: String,
        override val titulo: Int,
        override val resumo: Int = 0,
        val ligado: Boolean,
    ) : AjusteUi

    data class Escolha(
        override val chave: String,
        override val titulo: Int,
        override val resumo: Int = 0,
        val opcoes: List<String>,     // rótulos já resolvidos
        val valores: List<String>,
        val escolhido: String,
    ) : AjusteUi

    data class Deslizante(
        override val chave: String,
        override val titulo: Int,
        override val resumo: Int = 0,
        val minimo: Int,
        val maximo: Int,
        val passo: Int,
        val valor: Int,
        val sufixo: String = "",
        /**
         * Quantas unidades guardadas cabem numa unidade mostrada.
         *
         * A taxa de bits é gravada em kbps porque é isso que `Game.java` lê, e é mostrada em
         * Mbps porque é assim que se fala dela. O `SeekBarPreference` do XML já fazia essa
         * divisão (`seekbar:divisor`); sem o campo, a linha mostraria `45000 kbps` onde o app
         * inteiro diz `45 Mbps`. A conta não pode subir para o modelo: o valor que volta em
         * `aoMudarDeslizante` vai para a mesma chave, e um fator de mil silencioso ali não
         * quebra nada — só faz o vídeo rodar na taxa errada, para sempre.
         */
        val divisor: Int = 1,
        /**
         * Zero quer dizer "automático", não "desligado".
         *
         * É o caso da taxa em rede limitada: `0` manda o app decidir sozinho. Desenhar
         * `0 Mbps` informa exatamente o oposto do que a preferência faz.
         */
        val zeroEhAuto: Boolean = false,
    ) : AjusteUi

    data class Porta(                  // leva a outra tela, ou a uma ação externa
        override val chave: String,
        override val titulo: Int,
        override val resumo: Int = 0,
    ) : AjusteUi
}

/**
 * Quem manda em quem — o espelho de `android:dependency` no `preferences.xml`.
 *
 * Dependente à esquerda, dono à direita. Há cadeia: `share_performance_logs` depende de
 * `checkbox_enable_perf_logging`, que depende de `checkbox_enable_perf_overlay` — quem
 * consulta resolve transitivamente.
 */
private val DONOS: Map<String, String> = mapOf(
    "checkbox_auto_invert_video_resolution" to "checkbox_auto_orientation",
    "seekbar_osc_free_analog_stick_opacity" to "checkbox_enable_analog_stick_new",
    "checkbox_enable_keyboard_square" to "checkbox_enable_keyboard",
    "keyboard_axi_list" to "checkbox_enable_keyboard",
    "option_reset_osc" to "checkbox_enable_keyboard",
    "share_performance_logs" to "checkbox_enable_perf_logging",
    "checkbox_enable_perf_logging" to "checkbox_enable_perf_overlay",
    "checkbox_enable_perf_overlay_bottom" to "checkbox_enable_perf_overlay",
    "checkbox_enable_perf_overlay_lite" to "checkbox_enable_perf_overlay",
    "checkbox_enable_perf_overlay_lite_dialog" to "checkbox_enable_perf_overlay_lite",
    "checkbox_vibrate_fallback" to "checkbox_enable_rumble",
    "checkbox_enable_global_touch_sensitivity" to "checkbox_enable_touch_sensitivity",
    "checkbox_enable_touch_sensitivity_rotation_auto" to "checkbox_enable_touch_sensitivity",
    "seekbar_touch_sensitivity_opacity_x" to "checkbox_enable_touch_sensitivity",
    "seekbar_touch_sensitivity_opacity_y" to "checkbox_enable_touch_sensitivity",
    "checkbox_force_device_motion" to "checkbox_gamepad_motion_fallback",
    "analog_scrolling" to "checkbox_mouse_emulation",
    "checkbox_enable_analog_stick_new" to "checkbox_show_onscreen_controls",
    "checkbox_hide_osc_when_has_gamepad" to "checkbox_show_onscreen_controls",
    "checkbox_only_show_L3R3" to "checkbox_show_onscreen_controls",
    "checkbox_onscreen_style_official" to "checkbox_show_onscreen_controls",
    "checkbox_show_guide_button" to "checkbox_show_onscreen_controls",
    "checkbox_vibrate_osc" to "checkbox_show_onscreen_controls",
    "seekbar_osc_opacity" to "checkbox_show_onscreen_controls",
    "mouse_mode_list" to "checkbox_touchscreen_as_mouse",
    "checkbox_usb_bind_all" to "checkbox_usb_driver",
    "checkbox_enable_device_rumble" to "checkbox_vibrate_fallback",
    "seekbar_vibrate_fallback_strength" to "checkbox_vibrate_fallback",
    "seekbar_onscreen_keyboard_height" to "onscreen_keyboard_autofit",
    "seekbar_onscreen_keyboard_width" to "onscreen_keyboard_autofit",
)

/** O dono de uma chave, para quem precisa subir a cadeia degrau a degrau. */
internal fun donoDe(chave: String): String? = DONOS[chave]

/** Uma tela de Ajustes: um título e os itens dela. */
data class TelaDeAjustes(
    val chave: String,
    val titulo: Int,
    val resumo: Int,
    val itens: List<AjusteUi>,
)
