package com.lelysnails.agenda.data

import android.content.Context
import org.json.JSONArray
import java.util.Calendar

enum class AppStyle(val id: String, val label: String, val emoji: String) {
    ROSA("rosa", "Clasico rosa", "\uD83D\uDC85"),
    PRO("pro", "Profesional", "\uD83D\uDCBC"),
    MINIMAL("minimal", "Minimalista", "\u25FB"),
    LAVANDA("lavanda", "Lavanda", "\uD83C\uDF3A"),
    OCEANO("oceano", "Oceano", "\uD83C\uDF0A"),
    DORADO("dorado", "Dorado elegante", "\u2728");

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: ROSA
    }
}

enum class NightMode(val id: String, val label: String) {
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro"),
    SYSTEM("system", "Segun el sistema");

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: LIGHT
    }
}

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var style: AppStyle
        get() = AppStyle.fromId(prefs.getString(KEY_STYLE, AppStyle.ROSA.id))
        set(v) { prefs.edit().putString(KEY_STYLE, v.id).apply() }

    var nightMode: NightMode
        get() = NightMode.fromId(prefs.getString(KEY_NIGHT, NightMode.LIGHT.id))
        set(v) { prefs.edit().putString(KEY_NIGHT, v.id).apply() }

    var weekStartsOn: Int
        get() = prefs.getInt(KEY_WEEK_START, Calendar.MONDAY)
        set(v) { prefs.edit().putInt(KEY_WEEK_START, v).apply() }

    var maxSlots: Int
        get() = prefs.getInt(KEY_MAX_SLOTS, 2).coerceIn(1, 4)
        set(v) { prefs.edit().putInt(KEY_MAX_SLOTS, v.coerceIn(1, 4)).apply() }

    var defaultOffDays: Set<Int>
        get() {
            val raw = prefs.getString(KEY_OFF_WEEKDAYS, null)
            if (raw.isNullOrBlank()) return setOf(Calendar.SUNDAY, Calendar.WEDNESDAY)
            return raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
        set(v) { prefs.edit().putString(KEY_OFF_WEEKDAYS, v.joinToString(",")).apply() }

    var salonName: String
        get() = prefs.getString(KEY_SALON, "Lely's Nails") ?: "Lely's Nails"
        set(v) { prefs.edit().putString(KEY_SALON, v.ifBlank { "Lely's Nails" }).apply() }

    fun isWeekdayOff(calendarDayOfWeek: Int): Boolean =
        calendarDayOfWeek in defaultOffDays

    /** Servicios personalizados (vac\u00edo = lista por defecto) */
    fun getServices(): List<String> {
        val raw = prefs.getString(KEY_SERVICES, null)
        if (raw.isNullOrBlank()) return DEFAULT_SERVICES.toList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
                .ifEmpty { DEFAULT_SERVICES.toList() }
        } catch (_: Exception) {
            DEFAULT_SERVICES.toList()
        }
    }

    fun setServices(list: List<String>) {
        val clean = list.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val arr = JSONArray()
        clean.forEach { arr.put(it) }
        prefs.edit().putString(KEY_SERVICES, arr.toString()).apply()
    }

    fun addService(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        val list = getServices().toMutableList()
        if (list.any { it.equals(n, ignoreCase = true) }) return false
        list.add(n)
        setServices(list)
        return true
    }

    fun removeService(name: String) {
        setServices(getServices().filterNot { it.equals(name, ignoreCase = true) })
    }

    companion object {
        private const val PREFS = "lelys_settings_v1"
        private const val KEY_STYLE = "style"
        private const val KEY_NIGHT = "night"
        private const val KEY_WEEK_START = "week_start"
        private const val KEY_MAX_SLOTS = "max_slots"
        private const val KEY_OFF_WEEKDAYS = "off_weekdays"
        private const val KEY_SALON = "salon_name"
        private const val KEY_SERVICES = "services"

        val DEFAULT_SERVICES = listOf(
            "Manicura clasica",
            "Unas acrilicas",
            "Gelish / Semipermanente",
            "Kapping",
            "Pedicura",
            "Diseno de unas",
            "Retiro de unas",
            "Otro"
        )
    }
}
