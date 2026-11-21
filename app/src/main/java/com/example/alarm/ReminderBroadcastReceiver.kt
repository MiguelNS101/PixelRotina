package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AlertMode
import com.example.data.AppDatabase
import com.example.data.CheckInLogEntity
import com.example.data.CheckInSource
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.data.JournalEntryEntity
import com.example.data.JournalEntryType
import com.example.data.RoutineCategory
import com.example.data.RoutineEntity
import com.example.data.UserPreferencesRepository
import com.example.ui.RoutineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).routineDao()
                val routineId = intent.getLongExtra(AlarmScheduler.EXTRA_ROUTINE_ID, -1L)
                val fallbackTitle = intent.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE) ?: "Rotina"
                val fallbackDesc = intent.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_DESC) ?: ""
                val fallbackIcon = intent.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_ICON) ?: ""
                val fallbackMode = intent.getStringExtra(AlarmScheduler.EXTRA_ALERT_MODE)
                    ?: AlertMode.QUICK_OK_NOTIFICATION.name
                val isTest = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_TEST, false)

                val routine = if (routineId > 0L) {
                    dao.getRoutineById(routineId)
                } else {
                    null
                }

                when (action) {
                    AlarmScheduler.ACTION_TRIGGER_REMINDER -> {
                        val targetRoutine = routine ?: RoutineEntity(
                            id = if (routineId > 0L) routineId else 999L,
                            title = fallbackTitle,
                            description = fallbackDesc,
                            spriteName = fallbackIcon,
                            alertModeName = fallbackMode
                        )

                        if (targetRoutine.alertMode == AlertMode.PERSISTENT_ALARM) {
                            AlarmRingingService.startAlarm(
                                context = appContext,
                                routineId = targetRoutine.id,
                                title = targetRoutine.title,
                                description = targetRoutine.description,
                                iconName = targetRoutine.sprite.name
                            )
                        } else {
                            NotificationHelper.showRoutineNotification(
                                context = appContext,
                                routine = targetRoutine,
                                snoozeMinutes = 15
                            )
                        }

                        // Automatically schedule the next occurrence if this is a saved enabled routine
                        if (routine != null && routine.isEnabled && !isTest) {
                            val nextMillis = AlarmScheduler.scheduleRoutine(appContext, routine)
                            dao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
                        }
                    }

                    AlarmScheduler.ACTION_TRIGGER_DAILY_CHECKIN -> {
                        NotificationHelper.showMoodStepNotification(
                            context = appContext,
                            notificationId = NotificationHelper.DAILY_CHECKIN_NOTIFICATION_ID,
                            contextTitle = "Check-in do Fim do Dia",
                            routineId = -1L,
                            spriteName = "",
                            isDailyEndOfDay = true,
                            silentUpdate = false
                        )
                        if (!isTest) {
                            val settings = UserPreferencesRepository(appContext).settingsFlow.first()
                            if (settings.dailyCheckInNotifEnabled) {
                                AlarmScheduler.scheduleDailyCheckInNotification(
                                    context = appContext,
                                    enabled = true,
                                    hour = settings.dailyCheckInNotifHour,
                                    minute = settings.dailyCheckInNotifMinute
                                )
                            }
                        }
                    }

                    AlarmScheduler.ACTION_OK_DONE -> {
                        AlarmRingingService.stopAlarm(appContext)
                        if (routineId > 0L) {
                            NotificationHelper.cancelNotification(appContext, routineId)
                        }
                        val now = System.currentTimeMillis()
                        dao.insertCheckIn(
                            CheckInLogEntity(
                                routineId = if (routineId > 0L) routineId else 0L,
                                routineTitleSnapshot = routine?.title ?: fallbackTitle,
                                categorySnapshot = routine?.categoryName ?: RoutineCategory.HIDRATACAO.name,
                                dateKey = DateUtils.todayDateKey(now),
                                timestamp = now,
                                statusName = CheckInStatus.COMPLETED.name,
                                sourceName = CheckInSource.NOTIFICATION_OK.name,
                                note = "Confirmado via OK rápido na notificação"
                            )
                        )
                        if (routine != null && routine.isEnabled) {
                            val nextMillis = AlarmScheduler.scheduleRoutine(appContext, routine)
                            dao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
                        }
                    }

                    AlarmScheduler.ACTION_OK_AND_MOOD -> {
                        AlarmRingingService.stopAlarm(appContext)
                        val now = System.currentTimeMillis()
                        val resolvedTitle = routine?.title ?: fallbackTitle
                        val resolvedIcon = routine?.sprite?.name ?: fallbackIcon
                        val notifId = intent.getIntExtra(
                            AlarmScheduler.EXTRA_NOTIFICATION_ID,
                            if (routineId > 0L) NotificationHelper.notificationIdForRoutine(routineId)
                            else NotificationHelper.DAILY_CHECKIN_NOTIFICATION_ID
                        )

                        dao.insertCheckIn(
                            CheckInLogEntity(
                                routineId = if (routineId > 0L) routineId else 0L,
                                routineTitleSnapshot = resolvedTitle,
                                categorySnapshot = routine?.categoryName ?: RoutineCategory.HIDRATACAO.name,
                                dateKey = DateUtils.todayDateKey(now),
                                timestamp = now,
                                statusName = CheckInStatus.COMPLETED.name,
                                sourceName = CheckInSource.NOTIFICATION_OK.name,
                                note = "Confirmado via OK + Humor na notificação"
                            )
                        )
                        if (routine != null && routine.isEnabled) {
                            val nextMillis = AlarmScheduler.scheduleRoutine(appContext, routine)
                            dao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
                        }

                        // Silently morph the same notification into Step 1 (Mood selection)
                        NotificationHelper.showMoodStepNotification(
                            context = appContext,
                            notificationId = notifId,
                            contextTitle = resolvedTitle,
                            routineId = routineId,
                            spriteName = resolvedIcon,
                            isDailyEndOfDay = false,
                            silentUpdate = true
                        )
                    }

                    AlarmScheduler.ACTION_NOTIF_PICK_MOOD -> {
                        val notifId = intent.getIntExtra(
                            AlarmScheduler.EXTRA_NOTIFICATION_ID,
                            NotificationHelper.DAILY_CHECKIN_NOTIFICATION_ID
                        )
                        val moodScore = intent.getIntExtra(AlarmScheduler.EXTRA_MOOD_SCORE, 3).coerceIn(1, 5)
                        val isDaily = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_DAILY_CHECKIN, true)

                        // Silently morph the same notification into Step 2 (Energy selection)
                        NotificationHelper.showEnergyStepNotification(
                            context = appContext,
                            notificationId = notifId,
                            moodScore = moodScore,
                            contextTitle = fallbackTitle,
                            routineId = routineId,
                            spriteName = fallbackIcon,
                            isDailyEndOfDay = isDaily
                        )
                    }

                    AlarmScheduler.ACTION_NOTIF_PICK_ENERGY -> {
                        val notifId = intent.getIntExtra(
                            AlarmScheduler.EXTRA_NOTIFICATION_ID,
                            NotificationHelper.DAILY_CHECKIN_NOTIFICATION_ID
                        )
                        val moodScore = intent.getIntExtra(AlarmScheduler.EXTRA_MOOD_SCORE, 3).coerceIn(1, 5)
                        val energyScore = intent.getIntExtra(AlarmScheduler.EXTRA_ENERGY_SCORE, 3).coerceIn(1, 5)
                        val isDaily = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_DAILY_CHECKIN, true)

                        NotificationHelper.cancelNotificationById(appContext, notifId)

                        val now = System.currentTimeMillis()
                        val todayKey = DateUtils.todayDateKey(now)
                        val allRoutines = dao.getAllRoutinesOnce()
                        val allLogs = dao.getAllCheckInsOnce()
                        val streakSummary = RoutineViewModel.computeStreakMedalSummary(
                            routines = allRoutines,
                            logs = allLogs,
                            todayKey = todayKey
                        )

                        val noteText = if (isDaily) {
                            "Check-in do fim do dia via notificação"
                        } else {
                            "Check-in rápido após rotina: $fallbackTitle"
                        }

                        dao.insertJournalEntry(
                            JournalEntryEntity(
                                dateKey = todayKey,
                                timestamp = now,
                                entryTypeName = JournalEntryType.MOOD_CHECKIN.name,
                                moodScore = moodScore,
                                energyScore = energyScore,
                                contextTagsCsv = "",
                                content = noteText,
                                routineCompletionPercentSnapshot = streakSummary.todayCompletionPercent,
                                medalSnapshotName = streakSummary.todayMedal.name,
                                pomodoroCyclesSnapshot = 0
                            )
                        )
                    }

                    AlarmScheduler.ACTION_DISMISS_ALARM -> {
                        AlarmRingingService.stopAlarm(appContext)
                        if (routineId > 0L) {
                            NotificationHelper.cancelNotification(appContext, routineId)
                        }
                        val now = System.currentTimeMillis()
                        dao.insertCheckIn(
                            CheckInLogEntity(
                                routineId = if (routineId > 0L) routineId else 0L,
                                routineTitleSnapshot = routine?.title ?: fallbackTitle,
                                categorySnapshot = routine?.categoryName ?: RoutineCategory.DESCANSO.name,
                                dateKey = DateUtils.todayDateKey(now),
                                timestamp = now,
                                statusName = CheckInStatus.COMPLETED.name,
                                sourceName = CheckInSource.ALARM_DISMISSED.name,
                                note = "Alarme desligado e tarefa concluída"
                            )
                        )
                        if (routine != null && routine.isEnabled) {
                            val nextMillis = AlarmScheduler.scheduleRoutine(appContext, routine)
                            dao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
                        }
                    }

                    AlarmScheduler.ACTION_SNOOZE -> {
                        AlarmRingingService.stopAlarm(appContext)
                        if (routineId > 0L) {
                            NotificationHelper.cancelNotification(appContext, routineId)
                        }
                        val snoozeMin = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 15)
                        val now = System.currentTimeMillis()
                        dao.insertCheckIn(
                            CheckInLogEntity(
                                routineId = if (routineId > 0L) routineId else 0L,
                                routineTitleSnapshot = routine?.title ?: fallbackTitle,
                                categorySnapshot = routine?.categoryName ?: RoutineCategory.CORPO_MENTE.name,
                                dateKey = DateUtils.todayDateKey(now),
                                timestamp = now,
                                statusName = CheckInStatus.SNOOZED.name,
                                sourceName = CheckInSource.NOTIFICATION_OK.name,
                                note = "Soneca rápida de +${snoozeMin} min"
                            )
                        )
                        if (routine != null && routine.isEnabled) {
                            val snoozeMillis = AlarmScheduler.scheduleSnooze(appContext, routine, snoozeMin)
                            dao.updateRoutine(routine.copy(nextTriggerMillis = snoozeMillis))
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
