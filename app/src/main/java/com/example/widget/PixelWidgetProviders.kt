package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.AlarmScheduler
import com.example.data.AppDatabase
import com.example.data.CheckInLogEntity
import com.example.data.CheckInSource
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.data.JournalEntryEntity
import com.example.data.JournalEntryType
import com.example.data.RoutineCategory
import com.example.ui.RoutineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 2x2 Compact Widget: Pixel Art Medal, Streak Flame, Progress Bar & Daily Mood/Energy Pill.
 */
class PixelStreakMedalWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = PixelWidgetRenderer.loadSnapshot(appContext)
                val rv = PixelWidgetRenderer.buildStreakMedalRemoteViews(appContext, snapshot)
                appWidgetManager.updateAppWidget(appWidgetIds, rv)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 4x1 Wide Strip Widget: Interactive 5-Level Mood & Energy Quick Check-in right on the Home Screen.
 */
class PixelMoodQuickWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = PixelWidgetRenderer.loadSnapshot(appContext)
                val rv = PixelWidgetRenderer.buildMoodQuickRemoteViews(appContext, snapshot)
                appWidgetManager.updateAppWidget(appWidgetIds, rv)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 4x2 Medium Widget: Hero Art Card + Interactive Next-Routines Checklist with 1-tap OK.
 */
class PixelTodayCardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = PixelWidgetRenderer.loadSnapshot(appContext)
                val rv = PixelWidgetRenderer.buildTodayCardRemoteViews(appContext, snapshot)
                appWidgetManager.updateAppWidget(appWidgetIds, rv)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 4x4 Large Dashboard Widget: Hero Art + Streak/Medal + Interactive Routines + 5-Level Mood/Energy + Tab Shortcuts.
 */
class PixelDashboardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = PixelWidgetRenderer.loadSnapshot(appContext)
                val rv = PixelWidgetRenderer.buildDashboardLargeRemoteViews(appContext, snapshot)
                appWidgetManager.updateAppWidget(appWidgetIds, rv)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Handles interactive clicks directly from PixelRotina's Home Screen Widgets:
 * - 1-tap routine completion
 * - 2-step 5-level Mood -> Energy check-in saved straight to Room
 * - Manual refresh
 */
class PixelWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).routineDao()
                when (action) {
                    ACTION_WIDGET_COMPLETE_ROUTINE -> {
                        val routineId = intent.getLongExtra(AlarmScheduler.EXTRA_ROUTINE_ID, -1L)
                        val fallbackTitle = intent.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE) ?: "Rotina"
                        if (routineId > 0L) {
                            val routine = dao.getRoutineById(routineId)
                            val now = System.currentTimeMillis()
                            dao.insertCheckIn(
                                CheckInLogEntity(
                                    routineId = routineId,
                                    routineTitleSnapshot = routine?.title ?: fallbackTitle,
                                    categorySnapshot = routine?.categoryName ?: RoutineCategory.CORPO_MENTE.name,
                                    dateKey = DateUtils.todayDateKey(now),
                                    timestamp = now,
                                    statusName = CheckInStatus.COMPLETED.name,
                                    sourceName = CheckInSource.NOTIFICATION_OK.name,
                                    note = "Concluída via widget na tela inicial"
                                )
                            )
                            if (routine != null && routine.isEnabled) {
                                val nextMillis = AlarmScheduler.scheduleRoutine(appContext, routine)
                                dao.updateRoutine(routine.copy(nextTriggerMillis = nextMillis))
                            }
                        }
                        PixelWidgetUpdater.refreshAllSuspending(appContext)
                    }

                    ACTION_WIDGET_PICK_MOOD -> {
                        val moodScore = intent.getIntExtra(AlarmScheduler.EXTRA_MOOD_SCORE, 3).coerceIn(1, 5)
                        PixelWidgetStateStore.setPendingMood(appContext, moodScore)
                        PixelWidgetUpdater.refreshAllSuspending(appContext)
                    }

                    ACTION_WIDGET_PICK_ENERGY -> {
                        val moodScore = intent.getIntExtra(AlarmScheduler.EXTRA_MOOD_SCORE, 3).coerceIn(1, 5)
                        val energyScore = intent.getIntExtra(AlarmScheduler.EXTRA_ENERGY_SCORE, 3).coerceIn(1, 5)
                        PixelWidgetStateStore.setPendingMood(appContext, null)

                        val now = System.currentTimeMillis()
                        val todayKey = DateUtils.todayDateKey(now)
                        val streakSummary = RoutineViewModel.computeStreakMedalSummary(
                            routines = dao.getAllRoutinesOnce(),
                            logs = dao.getAllCheckInsOnce(),
                            todayKey = todayKey
                        )
                        dao.insertJournalEntry(
                            JournalEntryEntity(
                                dateKey = todayKey,
                                timestamp = now,
                                entryTypeName = JournalEntryType.MOOD_CHECKIN.name,
                                moodScore = moodScore,
                                energyScore = energyScore,
                                contextTagsCsv = "",
                                content = "Check-in rápido via widget na tela inicial",
                                routineCompletionPercentSnapshot = streakSummary.todayCompletionPercent,
                                medalSnapshotName = streakSummary.todayMedal.name,
                                pomodoroCyclesSnapshot = 0
                            )
                        )
                        PixelWidgetStateStore.setRecentSavedMessage(
                            appContext,
                            "✓ SALVO! HUMOR $moodScore/5 • ENERGIA $energyScore/5"
                        )
                        PixelWidgetUpdater.refreshAllSuspending(appContext)
                    }

                    ACTION_WIDGET_RESET_MOOD -> {
                        PixelWidgetStateStore.setPendingMood(appContext, null)
                        PixelWidgetUpdater.refreshAllSuspending(appContext)
                    }

                    ACTION_WIDGET_REFRESH -> {
                        PixelWidgetUpdater.refreshAllSuspending(appContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_COMPLETE_ROUTINE = "com.example.widget.ACTION_COMPLETE_ROUTINE"
        const val ACTION_WIDGET_PICK_MOOD = "com.example.widget.ACTION_PICK_MOOD"
        const val ACTION_WIDGET_PICK_ENERGY = "com.example.widget.ACTION_PICK_ENERGY"
        const val ACTION_WIDGET_RESET_MOOD = "com.example.widget.ACTION_RESET_MOOD"
        const val ACTION_WIDGET_REFRESH = "com.example.widget.ACTION_REFRESH"
    }
}
