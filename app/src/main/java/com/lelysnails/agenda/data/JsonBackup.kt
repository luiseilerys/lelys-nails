package com.lelysnails.agenda.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Respaldo JSON en tres sitios:
 * 1) App (Android/data/.../files/LelysNails) — siempre
 * 2) Interno filesDir/LelysNails — siempre
 * 3) Publico Descargas/LelysNails — sobrevive a desinstalar la app
 */
class JsonBackup(private val context: Context) {

    companion object {
        const val FOLDER_NAME = "LelysNails"
        const val FILE_LATEST = "agenda_latest.json"
    }

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

    /** Carpeta publica: Descargas/LelysNails (no se borra al desinstalar) */
    fun publicDir(): File? {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, FOLDER_NAME)
            if (!dir.exists()) dir.mkdirs()
            if (dir.exists() && dir.canWrite()) dir else null
        } catch (_: Exception) {
            null
        }
    }

    fun publicFile(): File? = publicDir()?.let { File(it, FILE_LATEST) }

    fun pathHint(): String =
        "Descargas/$FOLDER_NAME/$FILE_LATEST  (y copia en Android/data/.../files/$FOLDER_NAME/)"

    fun publicPathHint(): String = "Descargas/$FOLDER_NAME/$FILE_LATEST"

    fun exportNow(store: AppointmentStore): String {
        val json = store.exportFullJson()
        val pretty = try {
            json.toString(2)
        } catch (_: Exception) {
            json.toString()
        }

        // 1) Carpeta de la app
        val primary = backupFile()
        primary.writeText(pretty, Charsets.UTF_8)

        // 2) Espejo interno
        try {
            mirrorFile().writeText(pretty, Charsets.UTF_8)
        } catch (_: Exception) { }

        // 3) Publico (sobrevive desinstalacion)
        writePublic(pretty)

        // Historial en carpeta de la app
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            File(backupDir(), "agenda_$stamp.json").writeText(pretty, Charsets.UTF_8)
            backupDir().listFiles { f -> f.name.startsWith("agenda_") && f.name != FILE_LATEST }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(10)
                ?.forEach { it.delete() }
        } catch (_: Exception) { }

        return publicFile()?.absolutePath ?: primary.absolutePath
    }

    private fun writePublic(content: String) {
        // Intentar File directo (funciona en muchos equipos + legacy)
        try {
            val pub = publicFile()
            if (pub != null) {
                pub.writeText(content, Charsets.UTF_8)
                return
            }
        } catch (_: Exception) { }

        // MediaStore (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                writeViaMediaStore(content)
            } catch (_: Exception) { }
        }
    }

    private fun writeViaMediaStore(content: String) {
        val resolver = context.contentResolver
        // Buscar si ya existe para actualizar
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(FILE_LATEST, "%$FOLDER_NAME%")

        var uri: Uri? = null
        resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                uri = Uri.withAppendedPath(collection, id.toString())
            }
        }

        if (uri == null) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_LATEST)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/$FOLDER_NAME")
            }
            uri = resolver.insert(collection, values)
        }

        uri?.let { u ->
            resolver.openOutputStream(u, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun readPublicText(): String? {
        // 1) File publico
        try {
            val f = publicFile()
            if (f != null && f.exists()) return f.readText(Charsets.UTF_8)
        } catch (_: Exception) { }

        // 2) MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection =
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val args = arrayOf(FILE_LATEST, "%$FOLDER_NAME%")
                resolver.query(collection, projection, selection, args, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val uri = Uri.withAppendedPath(collection, id.toString())
                        resolver.openInputStream(uri)?.use { input ->
                            return input.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        return null
    }

    fun importFromFile(store: AppointmentStore, file: File? = null): Boolean {
        // Orden: archivo indicado → app → espejo → publico
        if (file != null && file.exists()) {
            return tryImportText(store, file.readText(Charsets.UTF_8))
        }
        if (backupFile().exists()) {
            return tryImportText(store, backupFile().readText(Charsets.UTF_8))
        }
        if (mirrorFile().exists()) {
            return tryImportText(store, mirrorFile().readText(Charsets.UTF_8))
        }
        val publicText = readPublicText()
        if (publicText != null) {
            return tryImportText(store, publicText)
        }
        return false
    }

    private fun tryImportText(store: AppointmentStore, text: String): Boolean {
        return try {
            store.importFullJson(JSONObject(text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exists(): Boolean {
        if (backupFile().exists() || mirrorFile().exists()) return true
        try {
            val f = publicFile()
            if (f != null && f.exists()) return true
        } catch (_: Exception) { }
        return readPublicText() != null
    }

    fun lastModifiedLabel(): String {
        val candidates = mutableListOf<File>()
        if (backupFile().exists()) candidates.add(backupFile())
        if (mirrorFile().exists()) candidates.add(mirrorFile())
        try {
            publicFile()?.takeIf { it.exists() }?.let { candidates.add(it) }
        } catch (_: Exception) { }
        val f = candidates.maxByOrNull { it.lastModified() } ?: return "Sin respaldo"
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return fmt.format(Date(f.lastModified()))
    }

    fun hasWritePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
