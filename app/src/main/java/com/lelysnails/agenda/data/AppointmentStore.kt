package com.lelysnails.agenda.data

import android.content.Context
import com.lelysnails.agenda.model.Appointment
import com.lelysnails.agenda.model.DayKind
import org.json.JSONObject
import java.util.Calendar

class AppointmentStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settings = SettingsStore(context)

    companion object {
        private const val PREFS_NAME = "lelys_nails_v1"
        private const val KEY_DATA = "appointments"
        private const val KEY_OFF = "off_days"
        /** Dias concretos que anulan el no-laborable fijo (ej. un domingo puntual) */
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
            // Si se marca off, quitar excepcion de trabajo
            setWorkException(dateKey, false)
        } else {
            obj.remove(dateKey)
        }
        prefs.edit().putString(KEY_OFF, obj.toString()).apply()
    }

    /** Excepcion: este dia concreto SI se trabaja aunque sea domingo/miercoles */
    fun isWorkException(dateKey: String): Boolean {
        return readWorkExceptions().optBoolean(dateKey, false)
    }

    fun setWorkException(dateKey: String, enabled: Boolean) {
        val obj = readWorkExceptions()
        if (enabled) {
            obj.put(dateKey, true)
            // Quitar custom off si existia
            val off = readOff()
            if (off.has(dateKey)) {
                off.remove(dateKey)
                prefs.edit().putString(KEY_OFF, off.toString()).apply()
            }
        } else {
            obj.remove(dateKey)
        }
        prefs.edit().putString(KEY_WORK_EXCEPTION, obj.toString()).apply()
    }

    fun isDefaultOff(year: Int, month0: Int, day: Int): Boolean {
        val cal = Calendar.getInstance().apply { set(year, month0, day) }
        return settings.isWeekdayOff(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun isDefaultOffKey(dateKey: String): Boolean {
        val parsed = parseKey(dateKey) ?: return false
        return isDefaultOff(parsed.first, parsed.second, parsed.third)
    }

    /**
     * No laborable si:
     * - es custom off, o
     * - es dia fijo off (dom/mie) Y no tiene excepcion de trabajo
     */
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

    private fun readData(): JSONObject {
        val raw = prefs.getString(KEY_DATA, "{}") ?: "{}"
        return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    }

    private fun writeData(obj: JSONObject) {
        prefs.edit().putString(KEY_DATA, obj.toString()).apply()
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
