package com.lelysnails.agenda.widget

import android.content.Context
import android.content.res.Configuration
import com.lelysnails.agenda.data.NightMode
import com.lelysnails.agenda.data.SettingsStore
import com.lelysnails.agenda.ui.StylePalette

object WidgetTheme {
    fun palette(context: Context): StylePalette {
        val settings = SettingsStore(context)
        val dark = when (settings.nightMode) {
            NightMode.DARK -> true
            NightMode.LIGHT -> false
            NightMode.SYSTEM -> {
                val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                night == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return StylePalette.resolve(settings.style, dark)
    }

    /** Fondo del widget con transparencia del usuario sobre el color de fondo del estilo */
    fun rootBg(palette: StylePalette, alphaPercent: Int): Int {
        val a = (alphaPercent * 255 / 100).coerceIn(40, 245)
        val c = palette.bg
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun panelBg(palette: StylePalette): Int {
        val c = palette.surface
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return (0xCC shl 24) or (r shl 16) or (g shl 8) or b
    }
}
