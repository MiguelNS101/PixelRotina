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

    suspend fun ensureSeedRoutinesIfFirstRun(hasSeeded: Boolean) {
        if (hasSeeded) return
        val existing = routineDao.getAllRoutinesOnce()
        if (existing.isEmpty()) {
            val defaults = listOf(
                RoutineEntity(
                    title = "Beber 1 Copo de Água",
                    description = "Hidratação contínua! Dê apenas OK na notificação quando beber.",
                    categoryName = RoutineCategory.HIDRATACAO.name,
                    spriteName = PixelSpriteType.WATER_DROP.name,
                    alertModeName = AlertMode.QUICK_OK_NOTIFICATION.name,
                    frequencyTypeName = FrequencyType.INTERVAL.name,
                    intervalMinutes = 60,
                    windowStartHour = 8,
                    windowEndHour = 21,
                    targetCountPerDay = 8,
                    daysOfWeekCsv = "1,2,3,4,5,6,7",
                    isEnabled = true
                ),
                RoutineEntity(
                    title = "Alongar Ombros & Pescoço",
                    description = "Pausa rápida de 2 minutos para soltar a tensão e respirar fundo.",
                    categoryName = RoutineCategory.CORPO_MENTE.name,
                    spriteName = PixelSpriteType.LEAF_WELLNESS.name,
                    alertModeName = AlertMode.QUICK_OK_NOTIFICATION.name,
                    frequencyTypeName = FrequencyType.INTERVAL.name,
                    intervalMinutes = 120,
                    windowStartHour = 9,
                    windowEndHour = 19,
                    targetCountPerDay = 4,
                    daysOfWeekCsv = "1,2,3,4,5",
                    isEnabled = true
                ),
                RoutineEntity(
                    title = "Sol da Manhã & Café Tranquilo",
                    description = "Começar o dia sem pressa antes de olhar mensagens.",
                    categoryName = RoutineCategory.ALIMENTACAO.name,
                    spriteName = PixelSpriteType.SUN_MORNING.name,
                    alertModeName = AlertMode.SCHEDULED_NOTIFICATION.name,
                    frequencyTypeName = FrequencyType.SPECIFIC_TIME.name,
                    hour = 8,
                    minute = 0,
                    targetCountPerDay = 1,
                    daysOfWeekCsv = "1,2,3,4,5,6,7",
                    isEnabled = true
                ),
                RoutineEntity(
                    title = "Bloco de Leitura ou Estudo Leve",
                    description = "30 minutos dedicados a algo que você gosta de aprender.",
                    categoryName = RoutineCategory.FOCO.name,
                    spriteName = PixelSpriteType.BOOK_FOCUS.name,
                    alertModeName = AlertMode.SCHEDULED_NOTIFICATION.name,
                    frequencyTypeName = FrequencyType.SPECIFIC_TIME.name,
                    hour = 19,
                    minute = 30,
                    targetCountPerDay = 1,
                    daysOfWeekCsv = "1,2,3,4,5",
                    isEnabled = true
                ),
                RoutineEntity(
                    title = "Desconecte das Telas & Preparar Sono",
                    description = "Alarme gentil para encerrar o dia e garantir uma noite revigorante.",
                    categoryName = RoutineCategory.DESCANSO.name,
                    spriteName = PixelSpriteType.MOON_REST.name,
                    alertModeName = AlertMode.PERSISTENT_ALARM.name,
                    frequencyTypeName = FrequencyType.SPECIFIC_TIME.name,
                    hour = 22,
                    minute = 30,
                    targetCountPerDay = 1,
                    daysOfWeekCsv = "1,2,3,4,5,6,7",
                    isEnabled = true
                )
            )
            for (item in defaults) {
                val id = routineDao.insertRoutine(item)
                val saved = item.copy(id = id)
                val nextTrigger = AlarmScheduler.scheduleRoutine(context, saved)
                routineDao.updateRoutine(saved.copy(nextTriggerMillis = nextTrigger))
            }
        }
        preferencesRepository.markSeededDefaults()
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
