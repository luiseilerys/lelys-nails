package com.lelysnails.agenda.data

import android.content.Context
import com.lelysnails.agenda.model.Appointment
import com.lelysnails.agenda.model.DayKind
import org.json.JSONObject
import java.util.Calendar

class AppointmentStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settings = SettingsStore(appContext)
    private val backup = JsonBackup(appContext)

    companion object {
        private const val PREFS_NAME = "lelys_nails_v1"
        private const val KEY_DATA = "appointments"
        private const val KEY_OFF = "off_days"
        private const val KEY_WORK_EXCEPTION = "work_exceptions"

        fun dateKey(year: Int, month0: Int, day: Int): String =
            "%04d-%02d-%02d".format(year, month0 + 1, day)

        fun parseKey(key: String): Triple<Int, Int, Int>? {
            val p = key.split("-")
            if (p.size != 3) return null
            return Triple(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
        }
    }

    fun maxSlots(): Int = settings.maxSlots

    fun getAppointment(dateKey: String, slot: Int): Appointment? {
        val root = readData()
        val day = root.optJSONObject(dateKey) ?: return null
        val obj = day.optJSONObject(slot.toString()) ?: return null
        return Appointment(
            name = obj.optString("name"),
            phone = obj.optString("phone"),
            service = obj.optString("service"),
            time = obj.optString("time"),
            notes = obj.optString("notes")
        ).takeIf { it.isFilled }
    }

    fun saveAppointment(dateKey: String, slot: Int, appt: Appointment) {
        val root = readData()
        val day = root.optJSONObject(dateKey) ?: JSONObject().also { root.put(dateKey, it) }
        day.put(slot.toString(), JSONObject().apply {
            put("name", appt.name)
            put("phone", appt.phone)
            put("service", appt.service)
            put("time", appt.time)
            put("notes", appt.notes)
        })
        writeData(root)
    }

    fun deleteAppointment(dateKey: String, slot: Int) {
        val root = readData()
        val day = root.optJSONObject(dateKey) ?: return
        day.remove(slot.toString())
        if (day.length() == 0) root.remove(dateKey)
        writeData(root)
    }

    fun clearDay(dateKey: String) {
        val root = readData()
        root.remove(dateKey)
        writeData(root)
    }

    fun bookedCount(dateKey: String): Int {
        var n = 0
        for (s in 1..maxSlots()) {
            if (getAppointment(dateKey, s) != null) n++
        }
        return n
    }

    fun findFreeSlot(dateKey: String): Int? {
        for (s in 1..maxSlots()) {
            if (getAppointment(dateKey, s) == null) return s
        }
        return null
    }

    fun moveAppointment(fromKey: String, fromSlot: Int, toKey: String): String {
        if (fromKey == toKey) return "Elige un dia distinto"
        if (isOff(toKey)) return "Ese dia no es laborable"
        val appt = getAppointment(fromKey, fromSlot) ?: return "No hay turno para trasladar"
        val free = findFreeSlot(toKey) ?: return "Ese dia no tiene espacios libres"
        saveAppointment(toKey, free, appt)
        deleteAppointment(fromKey, fromSlot)
        return "Turno trasladado al turno $free"
    }

    fun isCustomOff(dateKey: String): Boolean {
        return readOff().optBoolean(dateKey, false)
    }

    fun setCustomOff(dateKey: String, off: Boolean) {
        val obj = readOff()
        if (off) {
            obj.put(dateKey, true)
            setWorkException(dateKey, false)
        } else {
            obj.remove(dateKey)
        }
        prefs.edit().putString(KEY_OFF, obj.toString()).apply()
        autoBackup()
    }

    fun isWorkException(dateKey: String): Boolean {
        return readWorkExceptions().optBoolean(dateKey, false)
    }

    fun setWorkException(dateKey: String, enabled: Boolean) {
        val obj = readWorkExceptions()
        if (enabled) {
            obj.put(dateKey, true)
            val off = readOff()
            if (off.has(dateKey)) {
                off.remove(dateKey)
                prefs.edit().putString(KEY_OFF, off.toString()).apply()
            }
        } else {
            obj.remove(dateKey)
        }
        prefs.edit().putString(KEY_WORK_EXCEPTION, obj.toString()).apply()
        autoBackup()
    }

    fun isDefaultOff(year: Int, month0: Int, day: Int): Boolean {
        val cal = Calendar.getInstance().apply { set(year, month0, day) }
        return settings.isWeekdayOff(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun isDefaultOffKey(dateKey: String): Boolean {
        val parsed = parseKey(dateKey) ?: return false
        return isDefaultOff(parsed.first, parsed.second, parsed.third)
    }

    fun isOff(dateKey: String): Boolean {
        if (isCustomOff(dateKey)) return true
        if (isDefaultOffKey(dateKey) && !isWorkException(dateKey)) return true
        return false
    }

    fun dayKind(dateKey: String): DayKind {
        if (isOff(dateKey)) return DayKind.OFF
        val booked = bookedCount(dateKey)
        val max = maxSlots()
        return when {
            booked <= 0 -> DayKind.FREE
            booked >= max -> DayKind.FULL
            else -> DayKind.PARTIAL
        }
    }

    fun monthStats(year: Int, month0: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { set(year, month0, 1) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        var booked = 0
        var workDays = 0
        val max = maxSlots()
        for (d in 1..daysInMonth) {
            val key = dateKey(year, month0, d)
            if (isOff(key)) continue
            workDays++
            booked += bookedCount(key)
        }
        return booked to (workDays * max - booked)
    }

    fun hasAnyData(): Boolean {
        return readData().length() > 0 || readOff().length() > 0 || readWorkExceptions().length() > 0
    }

    /** JSON completo para respaldo */
    fun exportFullJson(): JSONObject {
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("appointments", readData())
            put("off_days", readOff())
            put("work_exceptions", readWorkExceptions())
            put("settings", JSONObject().apply {
                put("salonName", settings.salonName)
                put("maxSlots", settings.maxSlots)
                put("weekStartsOn", settings.weekStartsOn)
                put("style", settings.style.id)
                put("nightMode", settings.nightMode.id)
                put("offWeekdays", settings.defaultOffDays.joinToString(","))
                put("services", org.json.JSONArray(settings.getServices()))
            })
        }
    }

    fun importFullJson(json: JSONObject) {
        val apps = json.optJSONObject("appointments") ?: JSONObject()
        val off = json.optJSONObject("off_days") ?: JSONObject()
        val work = json.optJSONObject("work_exceptions") ?: JSONObject()
        prefs.edit()
            .putString(KEY_DATA, apps.toString())
            .putString(KEY_OFF, off.toString())
            .putString(KEY_WORK_EXCEPTION, work.toString())
            .apply()

        val s = json.optJSONObject("settings")
        if (s != null) {
            if (s.has("salonName")) settings.salonName = s.optString("salonName", settings.salonName)
            if (s.has("maxSlots")) settings.maxSlots = s.optInt("maxSlots", settings.maxSlots)
            if (s.has("weekStartsOn")) settings.weekStartsOn = s.optInt("weekStartsOn", settings.weekStartsOn)
            if (s.has("style")) settings.style = AppStyle.fromId(s.optString("style"))
            if (s.has("nightMode")) settings.nightMode = NightMode.fromId(s.optString("nightMode"))
            if (s.has("offWeekdays")) {
                val raw = s.optString("offWeekdays", "")
                if (raw.isNotBlank()) {
                    settings.defaultOffDays = raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
                }
            }
            if (s.has("services")) {
                val arr = s.optJSONArray("services")
                if (arr != null) {
                    val list = (0 until arr.length()).map { arr.getString(it) }
                    settings.setServices(list)
                }
            }
        }
        // Re-exportar para sincronizar archivo
        autoBackup()
    }

    fun autoBackup() {
        try {
            backup.exportNow(this)
        } catch (_: Exception) { }
    }

    fun manualExport(): String = backup.exportNow(this)

    fun manualImport(): Boolean = backup.importFromFile(this)

    fun backupPathHint(): String = backup.pathHint()

    fun backupLastModified(): String = backup.lastModifiedLabel()

    fun backupExists(): Boolean = backup.exists()

    /**
     * Al iniciar: si no hay datos locales pero si hay respaldo, restaurar.
     * Si hay datos locales, solo asegura que el JSON este al dia.
     */
    fun restoreOnStartupIfNeeded(): Boolean {
        return try {
            if (!hasAnyData() && backup.exists()) {
                backup.importFromFile(this)
            } else if (hasAnyData()) {
                autoBackup()
                false
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readData(): JSONObject {
        val raw = prefs.getString(KEY_DATA, "{}") ?: "{}"
        return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    }

    private fun writeData(obj: JSONObject) {
        prefs.edit().putString(KEY_DATA, obj.toString()).apply()
        autoBackup()
    }

    private fun readOff(): JSONObject {
        val raw = prefs.getString(KEY_OFF, "{}") ?: "{}"
        return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    }

    private fun readWorkExceptions(): JSONObject {
        val raw = prefs.getString(KEY_WORK_EXCEPTION, "{}") ?: "{}"
        return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    }
}
