package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ui.theme.PixelThemeType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupRestoreResult(
    val routinesImported: Int,
    val logsImported: Int,
    val themeRestored: PixelThemeType
)

object BackupManager {

    private const val BACKUP_VERSION = 1

    fun serializeToJson(
        themeType: PixelThemeType,
        routines: List<RoutineEntity>,
        checkInLogs: List<CheckInLogEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "PixelRotina")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("themeId", themeType.id)

        val routinesArr = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description)
                put("categoryName", r.categoryName)
                put("spriteName", r.spriteName)
                put("alertModeName", r.alertModeName)
                put("frequencyTypeName", r.frequencyTypeName)
                put("hour", r.hour)
                put("minute", r.minute)
                put("intervalMinutes", r.intervalMinutes)
                put("windowStartHour", r.windowStartHour)
                put("windowEndHour", r.windowEndHour)
                put("targetCountPerDay", r.targetCountPerDay)
                put("daysOfWeekCsv", r.daysOfWeekCsv)
                put("isEnabled", r.isEnabled)
                put("syncToCalendar", r.syncToCalendar)
                put("createdAt", r.createdAt)
            }
            routinesArr.put(obj)
        }
        root.put("routines", routinesArr)

        val logsArr = JSONArray()
        for (log in checkInLogs) {
            val obj = JSONObject().apply {
                put("id", log.id)
                put("routineId", log.routineId)
                put("routineTitleSnapshot", log.routineTitleSnapshot)
                put("categorySnapshot", log.categorySnapshot)
                put("dateKey", log.dateKey)
                put("timestamp", log.timestamp)
                put("statusName", log.statusName)
                put("sourceName", log.sourceName)
                put("note", log.note)
            }
            logsArr.put(obj)
        }
        root.put("checkInLogs", logsArr)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): Triple<PixelThemeType, List<RoutineEntity>, List<CheckInLogEntity>> {
        val root = JSONObject(jsonString)
        val theme = PixelThemeType.fromId(root.optString("themeId", PixelThemeType.PASTEL_RETRO.id))

        val routines = mutableListOf<RoutineEntity>()
        val routinesArr = root.optJSONArray("routines") ?: JSONArray()
        for (i in 0 until routinesArr.length()) {
            val obj = routinesArr.getJSONObject(i)
            routines.add(
                RoutineEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.optString("title", "Rotina"),
                    description = obj.optString("description", ""),
                    categoryName = obj.optString("categoryName", RoutineCategory.CORPO_MENTE.name),
                    spriteName = obj.optString("spriteName", RoutineCategory.CORPO_MENTE.defaultSprite.name),
                    alertModeName = obj.optString("alertModeName", AlertMode.QUICK_OK_NOTIFICATION.name),
                    frequencyTypeName = obj.optString("frequencyTypeName", FrequencyType.SPECIFIC_TIME.name),
                    hour = obj.optInt("hour", 9),
                    minute = obj.optInt("minute", 0),
                    intervalMinutes = obj.optInt("intervalMinutes", 60),
                    windowStartHour = obj.optInt("windowStartHour", 8),
                    windowEndHour = obj.optInt("windowEndHour", 21),
                    targetCountPerDay = obj.optInt("targetCountPerDay", 1),
                    daysOfWeekCsv = obj.optString("daysOfWeekCsv", "1,2,3,4,5,6,7"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    syncToCalendar = obj.optBoolean("syncToCalendar", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val logs = mutableListOf<CheckInLogEntity>()
        val logsArr = root.optJSONArray("checkInLogs") ?: JSONArray()
        for (i in 0 until logsArr.length()) {
            val obj = logsArr.getJSONObject(i)
            logs.add(
                CheckInLogEntity(
                    id = obj.optLong("id", 0L),
                    routineId = obj.optLong("routineId", 0L),
                    routineTitleSnapshot = obj.optString("routineTitleSnapshot", "Rotina"),
                    categorySnapshot = obj.optString("categorySnapshot", RoutineCategory.CORPO_MENTE.name),
                    dateKey = obj.optString("dateKey", DateUtils.todayDateKey()),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    statusName = obj.optString("statusName", CheckInStatus.COMPLETED.name),
                    sourceName = obj.optString("sourceName", CheckInSource.IN_APP.name),
                    note = obj.optString("note", "")
                )
            )
        }

        return Triple(theme, routines, logs)
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes the save file locally into internal backups directory and opens the share chooser
     * pre-configured for sending to Google Drive or any cloud storage app.
     */
    fun shareSaveFileToGoogleDrive(context: Context, jsonContent: String): File? {
        return try {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val fileName = "pixelrotina_save_${DateUtils.todayDateKey()}.json"
            val file = File(dir, fileName)
            file.writeText(jsonContent, Charsets.UTF_8)

            // Also keep a copy in filesDir/backups for local offline safety
            val internalDir = File(context.filesDir, "backups").apply { mkdirs() }
            File(internalDir, "pixelrotina_latest_save.json").writeText(jsonContent, Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Save de Rotina — PixelRotina (${DateUtils.todayDateKey()})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Backup completo da minha rotina e histórico de bem-estar no PixelRotina."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Salvar Backup no Google Drive ou Nuvem").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            file
        } catch (_: Exception) {
            null
        }
    }

    fun saveInternalQuickCopy(context: Context, jsonContent: String): File? {
        return try {
            val internalDir = File(context.filesDir, "backups").apply { mkdirs() }
            val file = File(internalDir, "pixelrotina_latest_save.json")
            file.writeText(jsonContent, Charsets.UTF_8)
            file
        } catch (_: Exception) {
            null
        }
    }

    fun loadInternalQuickCopy(context: Context): String? {
        return try {
            val file = File(File(context.filesDir, "backups"), "pixelrotina_latest_save.json")
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        } catch (_: Exception) {
            null
        }
    }
}
