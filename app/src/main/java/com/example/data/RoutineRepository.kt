package com.example.data

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.ui.components.PixelSpriteType
import kotlinx.coroutines.flow.Flow

class RoutineRepository(
    private val context: Context,
    private val routineDao: RoutineDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    val allRoutines: Flow<List<RoutineEntity>> = routineDao.observeAllRoutines()
    val allCheckIns: Flow<List<CheckInLogEntity>> = routineDao.observeAllCheckIns()
    val userSettings: Flow<UserSettings> = preferencesRepository.settingsFlow

    suspend fun clearAllUserData() {
        val currentRoutines = routineDao.getAllRoutinesOnce()
        for (r in currentRoutines) {
            AlarmScheduler.cancelRoutine(context, r.id)
        }
        routineDao.clearAllRoutines()
        routineDao.clearAllCheckIns()
    }

    suspend fun saveRoutine(routine: RoutineEntity): RoutineEntity {
        val id = if (routine.id == 0L) {
            routineDao.insertRoutine(routine)
        } else {
            routineDao.updateRoutine(routine)
            routine.id
        }
        var updated = routine.copy(id = id)
        if (updated.syncToCalendar) {
            val calEventId = CalendarSyncHelper.upsertRoutineInDeviceCalendar(context, updated)
            if (calEventId != null) {
                updated = updated.copy(calendarEventId = calEventId)
            }
        }
        val nextMillis = if (updated.isEnabled) {
            AlarmScheduler.scheduleRoutine(context, updated)
        } else {
            AlarmScheduler.cancelRoutine(context, updated.id)
            0L
        }
        val finalRoutine = updated.copy(nextTriggerMillis = nextMillis)
        routineDao.updateRoutine(finalRoutine)
        return finalRoutine
    }

    suspend fun toggleRoutineEnabled(routine: RoutineEntity, enabled: Boolean) {
        val nextMillis = if (enabled) {
            AlarmScheduler.scheduleRoutine(context, routine.copy(isEnabled = true))
        } else {
            AlarmScheduler.cancelRoutine(context, routine.id)
            0L
        }
        routineDao.updateRoutine(
            routine.copy(
                isEnabled = enabled,
                nextTriggerMillis = nextMillis
            )
        )
    }

    suspend fun deleteRoutine(routine: RoutineEntity) {
        AlarmScheduler.cancelRoutine(context, routine.id)
        routineDao.deleteRoutineById(routine.id)
    }

    suspend fun recordCheckIn(
        routine: RoutineEntity,
        status: CheckInStatus = CheckInStatus.COMPLETED,
        source: CheckInSource = CheckInSource.IN_APP,
        note: String = ""
    ) {
        val now = System.currentTimeMillis()
        routineDao.insertCheckIn(
            CheckInLogEntity(
                routineId = routine.id,
                routineTitleSnapshot = routine.title,
                categorySnapshot = routine.categoryName,
                dateKey = DateUtils.todayDateKey(now),
                timestamp = now,
                statusName = status.name,
                sourceName = source.name,
                note = note
            )
        )
        if (routine.isEnabled) {
            val nextMillis = AlarmScheduler.scheduleRoutine(context, routine, overrideTriggerMillis = null)
            routineDao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
        }
    }

    suspend fun gentleRescheduleRoutine(routine: RoutineEntity, delayMinutes: Int = 30): Long {
        val snoozedMillis = AlarmScheduler.scheduleSnooze(context, routine, delayMinutes)
        routineDao.updateRoutine(routine.copy(nextTriggerMillis = snoozedMillis))
        val now = System.currentTimeMillis()
        routineDao.insertCheckIn(
            CheckInLogEntity(
                routineId = routine.id,
                routineTitleSnapshot = routine.title,
                categorySnapshot = routine.categoryName,
                dateKey = DateUtils.todayDateKey(now),
                timestamp = now,
                statusName = CheckInStatus.SNOOZED.name,
                sourceName = CheckInSource.IN_APP.name,
                note = "Reagendado suavemente para +${delayMinutes} min"
            )
        )
        return snoozedMillis
    }

    suspend fun exportFullBackupJson(currentTheme: com.example.ui.theme.PixelThemeType): String {
        val routines = routineDao.getAllRoutinesOnce()
        val logs = routineDao.getAllCheckInsOnce()
        val json = BackupManager.serializeToJson(currentTheme, routines, logs)
        BackupManager.saveInternalQuickCopy(context, json)
        preferencesRepository.recordBackupTimestamp()
        return json
    }

    suspend fun importFullBackupJson(jsonString: String, replaceExisting: Boolean): BackupRestoreResult {
        val (theme, importedRoutines, importedLogs) = BackupManager.parseFromJson(jsonString)
        if (replaceExisting) {
            val currentRoutines = routineDao.getAllRoutinesOnce()
            for (r in currentRoutines) {
                AlarmScheduler.cancelRoutine(context, r.id)
            }
            routineDao.clearAllRoutines()
            routineDao.clearAllCheckIns()
        }

        for (routine in importedRoutines) {
            val toInsert = if (replaceExisting) routine else routine.copy(id = 0L)
            val newId = routineDao.insertRoutine(toInsert)
            val saved = toInsert.copy(id = newId)
            val nextMillis = if (saved.isEnabled) {
                AlarmScheduler.scheduleRoutine(context, saved)
            } else {
                0L
            }
            routineDao.updateRoutine(saved.copy(nextTriggerMillis = nextMillis))
        }

        for (log in importedLogs) {
            val toInsert = if (replaceExisting) log else log.copy(id = 0L)
            routineDao.insertCheckIn(toInsert)
        }

        preferencesRepository.setTheme(theme)
        preferencesRepository.recordBackupTimestamp()

        return BackupRestoreResult(
            routinesImported = importedRoutines.size,
            logsImported = importedLogs.size,
            themeRestored = theme
        )
    }
}
