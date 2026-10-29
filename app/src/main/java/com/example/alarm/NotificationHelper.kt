package com.example.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AlertMode
import com.example.data.RoutineEntity

object NotificationHelper {

    const val CHANNEL_QUICK_OK = "pixel_rotina_quick_ok"
    const val CHANNEL_SCHEDULED = "pixel_rotina_scheduled"
    const val CHANNEL_PERSISTENT_ALARM = "pixel_rotina_persistent_alarm"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val quickChannel = NotificationChannel(
            CHANNEL_QUICK_OK,
            context.getString(R.string.channel_quick_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_quick_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 120, 200)
            setSound(notifSound, audioAttrs)
        }

        val scheduledChannel = NotificationChannel(
            CHANNEL_SCHEDULED,
            context.getString(R.string.channel_scheduled_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_scheduled_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
            setSound(notifSound, audioAttrs)
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_PERSISTENT_ALARM,
            context.getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }

        manager.createNotificationChannels(listOf(quickChannel, scheduledChannel, alarmChannel))
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showRoutineNotification(
        context: Context,
        routine: RoutineEntity,
        snoozeMinutes: Int = 15
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notificationId = notificationIdForRoutine(routine.id)
        val isQuickOk = routine.alertMode == AlertMode.QUICK_OK_NOTIFICATION
        val channelId = if (isQuickOk) CHANNEL_QUICK_OK else CHANNEL_SCHEDULED

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routine.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val okIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_OK_DONE
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routine.id)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, routine.title)
        }
        val okPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            okIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routine.id)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = if (routine.description.isNotBlank()) {
            routine.description
        } else if (isQuickOk) {
            "Toque em 'OK! Feito' para registrar na sua rotina agora mesmo."
        } else {
            "Momento da sua rotina de ${routine.category.label.lowercase()}."
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("✨ ${routine.title}")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.action_ok_done),
                okPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Adiar +${snoozeMinutes}m",
                snoozePendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    fun buildPersistentAlarmForegroundNotification(
        context: Context,
        routineId: Long,
        title: String,
        description: String,
        snoozeMinutes: Int = 10
    ): Notification {
        ensureChannelsCreated(context)
        val notificationId = notificationIdForRoutine(routineId)

        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_DESC, description)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 3000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_DISMISS_ALARM
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 4000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 5000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = description.ifBlank {
            "Alarme de rotina ativo! Toque para desligar e concluir."
        }

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 ALARME: $title")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.action_dismiss_alarm),
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                context.getString(R.string.action_snooze_alarm),
                snoozePendingIntent
            )
            .build()
    }

    fun cancelNotification(context: Context, routineId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationIdForRoutine(routineId))
    }

    fun notificationIdForRoutine(routineId: Long): Int {
        return (50_000L + (routineId % 40_000L)).toInt()
    }
}
