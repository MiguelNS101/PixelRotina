package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationHelper.ensureChannelsCreated(appContext)
                val dao = AppDatabase.getInstance(appContext).routineDao()
                val enabledRoutines = dao.getEnabledRoutinesOnce()
                for (routine in enabledRoutines) {
                    val nextMillis = AlarmScheduler.scheduleRoutine(appContext, routine)
                    dao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
                }
                val settings = UserPreferencesRepository(appContext).settingsFlow.first()
                AlarmScheduler.scheduleDailyCheckInNotification(
                    context = appContext,
                    enabled = settings.dailyCheckInNotifEnabled,
                    hour = settings.dailyCheckInNotifHour,
                    minute = settings.dailyCheckInNotifMinute
                )
                com.example.widget.PixelWidgetUpdater.refreshAllSuspending(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
