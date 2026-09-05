package com.limelight.ui.compose

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Vibrator
import android.view.Display
import com.limelight.preferences.PreferenceConfiguration
import com.limelight.utils.ServerHelper

/**
 * A peneira do aparelho — o que este telefone ou tablet simplesmente não sabe fazer.
 *
 * `lerArvoreDeAjustes` devolve a árvore INTEIRA de propósito, e o comentário dela diz por
 * quê: filtro escondido dentro de um leitor é divergência silenciosa esperando acontecer.
 * A peneira mora aqui, à vista, e é a transcrição literal dos dezessete `removePreference`
 * de `StreamSettings.initializePreferences()`.
 *
 * **Não** é a poda de produto. Aquela é do catálogo, já está aplicada, e é sobre o que não
 * vale a pena existir. Esta é sobre o que não pode funcionar: um interruptor de vibração num
 * aparelho sem vibrador não é uma escolha ruim, é uma mentira.
 *
 * Chaves que já não existem — porque a poda de produto as levou — são ignoradas em silêncio,
 * e isso é intencional: as duas listas são mantidas por motivos diferentes e vão divergir.
 */
internal fun peneirarPeloAparelho(
    contexto: Context,
    telas: List<TelaDeAjustes>,
): List<TelaDeAjustes> {
    val fora = chavesForaDoAlcance(contexto)
    val telasFora = telasForaDoAlcance(contexto)
    if (fora.isEmpty() && telasFora.isEmpty()) return telas

    return telas
        .filterNot { it.chave in telasFora }
        .map { tela -> tela.copy(itens = tela.itens.filterNot { it.chave in fora }) }
        // Uma categoria que perdeu o último item deixa de ser uma categoria: sem isto, o
        // índice ofereceria uma linha que abre numa tela vazia.
        .filter { it.itens.isNotEmpty() }
}

private fun telasForaDoAlcance(contexto: Context): Set<String> {
    val pm = contexto.packageManager
    // Sem tela de toque não há controle desenhado na tela nem régua de teclas especiais —
    // as duas categorias inteiras deixam de ter assunto.
    return if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) emptySet()
    else setOf("category_onscreen_controls", "category_special_key_layout")
}

private fun chavesForaDoAlcance(contexto: Context): Set<String> {
    val pm = contexto.packageManager
    val fora = mutableSetOf<String>()

    // Sensor de movimento no controle: extensões de movimento, LED e bateria só existem a
    // partir do Android 12.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        fora += "checkbox_gamepad_motion_sensors"
    }

    // O aparelho emprestando os próprios sensores ao controle só faz sentido se ele tiver
    // algum sensor para emprestar.
    if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) &&
        !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)
    ) {
        fora += "checkbox_force_device_motion"
        fora += "checkbox_gamepad_motion_fallback"
    }

    if (!pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
        fora += "checkbox_usb_bind_all"
        fora += "checkbox_usb_driver"
    }

    // Picture-in-picture: API 26, e a Fire OS anuncia a feature sem entregá-la.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        !pm.hasSystemFeature("android.software.picture_in_picture") ||
        pm.hasSystemFeature("com.amazon.software.fireos")
    ) {
        fora += "checkbox_enable_pip"
    }

    val vibrador = contexto.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (vibrador?.hasVibrator() != true) {
        fora += "checkbox_vibrate_fallback"
        fora += "seekbar_vibrate_fallback_strength"
        fora += "checkbox_vibrate_osc"
        fora += "checkbox_vibrate_keyboard"
        fora += "checkbox_enable_device_rumble"
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !vibrador.hasAmplitudeControl()) {
        // Vibra, mas só num nível: um deslizante de intensidade não teria o que deslizar.
        fora += "seekbar_vibrate_fallback_strength"
    }

    if (!temHdr10(contexto)) {
        fora += "checkbox_enable_hdr"
    }

    return fora
}

/**
 * HDR só aparece quando a TELA sabe HDR10 — não basta o decodificador.
 *
 * A ordem das perguntas é a do original e cada uma delas foi comprada com um relato de
 * campo: abaixo do Android 7 não há API; `getHdrCapabilities()` devolve nulo em alguns
 * aparelhos (o Lenovo Mirage Solo é o caso citado no upstream); e o firmware antigo do
 * SHIELD TV anuncia HDR10 e entrega imagem quebrada.
 */
private fun temHdr10(contexto: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    if (PreferenceConfiguration.isShieldAtvFirmwareWithBrokenHdr()) return false

    // A MESMA tela que a sessao vai usar, e nao a padrao: com display externo ligado e
    // `enableFullExDisplay`, quem manda no HDR e o monitor de fora.
    val prefs = PreferenceConfiguration.readPreferences(contexto)
    val tela = ServerHelper.getActiveDisplay(contexto, prefs) ?: return false
    val caps = tela.hdrCapabilities ?: return false
    return caps.supportedHdrTypes.any { it == Display.HdrCapabilities.HDR_TYPE_HDR10 }
}
