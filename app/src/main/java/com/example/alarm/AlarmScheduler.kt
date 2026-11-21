package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.AlertMode
import com.example.data.FrequencyType
import com.example.data.RoutineEntity
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
    const val ACTION_TRIGGER_DAILY_CHECKIN = "com.example.action.TRIGGER_DAILY_CHECKIN"
    const val ACTION_OK_DONE = "com.example.action.ACTION_OK_DONE"
    const val ACTION_OK_AND_MOOD = "com.example.action.ACTION_OK_AND_MOOD"
    const val ACTION_NOTIF_PICK_MOOD = "com.example.action.ACTION_NOTIF_PICK_MOOD"
    const val ACTION_NOTIF_PICK_ENERGY = "com.example.action.ACTION_NOTIF_PICK_ENERGY"
    const val ACTION_SNOOZE = "com.example.action.ACTION_SNOOZE"
    const val ACTION_DISMISS_ALARM = "com.example.action.ACTION_DISMISS_ALARM"

    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_ROUTINE_TITLE = "extra_routine_title"
    const val EXTRA_ROUTINE_DESC = "extra_routine_desc"
    const val EXTRA_ROUTINE_ICON = "extra_routine_icon"
    const val EXTRA_ALERT_MODE = "extra_alert_mode"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val EXTRA_IS_TEST = "extra_is_test"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_MOOD_SCORE = "extra_mood_score"
    const val EXTRA_ENERGY_SCORE = "extra_energy_score"
    const val EXTRA_IS_DAILY_CHECKIN = "extra_is_daily_checkin"
    const val EXTRA_OPEN_TAB = "extra_open_tab"

    const val DAILY_CHECKIN_REQUEST_CODE = 98_701

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Calculates the next trigger timestamp (in millis) for a RoutineEntity after [nowMillis].
     */
    fun computeNextTriggerMillis(
        routine: RoutineEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        if (!routine.isEnabled) return 0L
        val activeDays = routine.activeDays.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }

        return if (routine.frequencyType == FrequencyType.SPECIFIC_TIME) {
            computeNextSpecificTime(routine.hour, routine.minute, activeDays, nowMillis)
        } else {
            computeNextIntervalTime(
                intervalMinutes = routine.intervalMinutes.coerceAtLeast(10),
                windowStartHour = routine.windowStartHour.coerceIn(0, 23),
                windowEndHour = routine.windowEndHour.coerceIn(0, 23),
                activeDays = activeDays,
                nowMillis = nowMillis
            )
        }
    }

    private fun computeNextSpecificTime(
        hour: Int,
        minute: Int,
        activeDays: Set<Int>,
        nowMillis: Long
    ): Long {
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isoDay = calendarDayToIso(candidate.get(Calendar.DAY_OF_WEEK))
            if (isoDay in activeDays && candidate.timeInMillis > nowMillis + 2_000L) {
                return candidate.timeInMillis
            }
        }
        return nowMillis + 24 * 60 * 60 * 1000L
    }

    private fun computeNextIntervalTime(
        intervalMinutes: Int,
        windowStartHour: Int,
        windowEndHour: Int,
        activeDays: Set<Int>,
        nowMillis: Long
    ): Long {
        val safeEndHour = if (windowEndHour <= windowStartHour) {
            (windowStartHour + 1).coerceAtMost(23)
        } else {
            windowEndHour
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayIso = calendarDayToIso(nowCal.get(Calendar.DAY_OF_WEEK))
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)

        // If today is an active day and we are inside the active window, schedule now + intervalMinutes
        if (todayIso in activeDays && currentHour in windowStartHour until safeEndHour) {
            val nextInWindow = nowMillis + intervalMinutes * 60_000L
            val nextCal = Calendar.getInstance().apply { timeInMillis = nextInWindow }
            val nextHour = nextCal.get(Calendar.HOUR_OF_DAY)
            val nextMinute = nextCal.get(Calendar.MINUTE)
            if (nextHour < safeEndHour || (nextHour == safeEndHour && nextMinute == 0)) {
                return nextInWindow
            }
        }

        // If today is active and we are before windowStartHour, start today at windowStartHour:00
        if (todayIso in activeDays && currentHour < windowStartHour) {
            val startToday = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, windowStartHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (startToday.timeInMillis > nowMillis) {
                return startToday.timeInMillis
            }
        }

        // Otherwise find the next active day at windowStartHour:00
        return computeNextSpecificTime(windowStartHour, 0, activeDays, nowMillis)
    }

    private fun calendarDayToIso(calDay: Int): Int {
        return when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    /**
     * Schedules the routine in Android's AlarmManager and returns the scheduled trigger timestamp.
     */
    fun scheduleRoutine(
        context: Context,
        routine: RoutineEntity,
        overrideTriggerMillis: Long? = null
    ): Long {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0L
        if (!routine.isEnabled || routine.id <= 0L) {
            cancelRoutine(context, routine.id)
            return 0L
        }

        val triggerAtMillis = overrideTriggerMillis ?: computeNextTriggerMillis(routine)
        if (triggerAtMillis <= 0L) return 0L

        val pendingIntent = buildTriggerPendingIntent(context, routine)

        try {
            if (routine.alertMode == AlertMode.PERSISTENT_ALARM && canScheduleExactAlarms(context)) {
                val showIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_ROUTINE_ID, routine.id)
                    putExtra(EXTRA_ROUTINE_TITLE, routine.title)
                    putExtra(EXTRA_ROUTINE_DESC, routine.description)
                    putExtra(EXTRA_ROUTINE_ICON, routine.sprite.name)
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    routine.id.toInt(),
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            } else if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        return triggerAtMillis
    }

    fun scheduleSnooze(
        context: Context,
        routine: RoutineEntity,
        snoozeMinutes: Int
    ): Long {
        val snoozeTrigger = System.currentTimeMillis() + snoozeMinutes.coerceAtLeast(1) * 60_000L
        return scheduleRoutine(context, routine, overrideTriggerMillis = snoozeTrigger)
    }

    fun cancelRoutine(context: Context, routineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForRoutine(routineId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun triggerImmediateTest(context: Context, routine: RoutineEntity) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_ROUTINE_ID, routine.id)
            putExtra(EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(EXTRA_ROUTINE_DESC, routine.description)
            putExtra(EXTRA_ROUTINE_ICON, routine.sprite.name)
            putExtra(EXTRA_ALERT_MODE, routine.alertModeName)
            putExtra(EXTRA_IS_TEST, true)
        }
        context.sendBroadcast(intent)

        if (routine.alertMode == AlertMode.PERSISTENT_ALARM) {
            try {
                val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_ROUTINE_ID, routine.id)
                    putExtra(EXTRA_ROUTINE_TITLE, routine.title)
                    putExtra(EXTRA_ROUTINE_DESC, routine.description)
                    putExtra(EXTRA_ROUTINE_ICON, routine.sprite.name)
                }
                context.startActivity(fullScreenIntent)
            } catch (_: Exception) {
            }
        }
    }

    fun computeNextDailyCheckInTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        return computeNextSpecificTime(
            hour = safeHour,
            minute = safeMinute,
            activeDays = setOf(1, 2, 3, 4, 5, 6, 7),
            nowMillis = nowMillis
        )
    }

    fun scheduleDailyCheckInNotification(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ): Long {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0L
        if (!enabled) {
            cancelDailyCheckInNotification(context)
            return 0L
        }
        val triggerAtMillis = computeNextDailyCheckInTriggerMillis(hour, minute)
        val pendingIntent = buildDailyCheckInPendingIntent(context)
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        return triggerAtMillis
    }

    fun cancelDailyCheckInNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(buildDailyCheckInPendingIntent(context))
    }

    fun triggerImmediateDailyCheckInTest(context: Context) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_DAILY_CHECKIN
            putExtra(EXTRA_IS_TEST, true)
        }
        context.sendBroadcast(intent)
    }

    private fun buildDailyCheckInPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_DAILY_CHECKIN
        }
        return PendingIntent.getBroadcast(
            context,
            DAILY_CHECKIN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildTriggerPendingIntent(context: Context, routine: RoutineEntity): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_ROUTINE_ID, routine.id)
            putExtra(EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(EXTRA_ROUTINE_DESC, routine.description)
            putExtra(EXTRA_ROUTINE_ICON, routine.sprite.name)
            putExtra(EXTRA_ALERT_MODE, routine.alertModeName)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForRoutine(routine.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeForRoutine(routineId: Long): Int {
        return (10_000L + (routineId % 80_000L)).toInt()
    }
}
