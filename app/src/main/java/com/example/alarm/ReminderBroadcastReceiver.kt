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
import com.example.data.RoutineCategory
import com.example.data.RoutineEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
