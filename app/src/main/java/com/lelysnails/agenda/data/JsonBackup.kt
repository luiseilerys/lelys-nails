package com.lelysnails.agenda.data

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copia de seguridad en JSON.
 * Carpeta: almacenamiento interno de la app / LelysNails /
 * (visible en: Android/data/com.lelysnails.agenda/files/LelysNails/)
 * Tambien en filesDir interno como espejo.
 */
class JsonBackup(private val context: Context) {

    companion object {
        const val FOLDER_NAME = "LelysNails"
        const val FILE_NAME = "agenda_backup.json"
        const val FILE_LATEST = "agenda_latest.json"
    }

    /** Directorio principal de respaldos (almacenamiento externo de la app) */
    fun backupDir(): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun backupFile(): File = File(backupDir(), FILE_LATEST)

    fun mirrorFile(): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_LATEST)
    }

    fun pathHint(): String =
        "Android/data/${context.packageName}/files/$FOLDER_NAME/$FILE_LATEST"

    /** Exporta todo el estado de turnos a JSON y lo escribe en disco */
    fun exportNow(store: AppointmentStore): String {
        val json = store.exportFullJson()
        val pretty = try {
            json.toString(2)
        } catch (_: Exception) {
            json.toString()
        }

        val primary = backupFile()
        primary.writeText(pretty, Charsets.UTF_8)

        // Espejo en almacenamiento interno de la app
        try {
            mirrorFile().writeText(pretty, Charsets.UTF_8)
        } catch (_: Exception) { }

        // Copia con fecha (historial corto)
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            File(backupDir(), "agenda_$stamp.json").writeText(pretty, Charsets.UTF_8)
            // Conservar solo las 10 mas recientes fechadas
            backupDir().listFiles { f -> f.name.startsWith("agenda_") && f.name != FILE_LATEST }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(10)
                ?.forEach { it.delete() }
        } catch (_: Exception) { }

        return primary.absolutePath
    }

    /** Importa desde el archivo latest (o el path indicado) */
    fun importFromFile(store: AppointmentStore, file: File? = null): Boolean {
        val target = file ?: backupFile()
        if (!target.exists()) {
            val mirror = mirrorFile()
            if (!mirror.exists()) return false
            return importFromFile(store, mirror)
        }
        return try {
            val text = target.readText(Charsets.UTF_8)
            val json = JSONObject(text)
            store.importFullJson(json)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exists(): Boolean = backupFile().exists() || mirrorFile().exists()

    fun lastModifiedLabel(): String {
        val f = when {
            backupFile().exists() -> backupFile()
            mirrorFile().exists() -> mirrorFile()
            else -> return "Sin respaldo"
        }
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return fmt.format(Date(f.lastModified()))
    }
}
