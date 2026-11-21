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
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AlertMode
import com.example.data.RoutineEntity
import com.example.ui.components.PixelSpriteType
import com.example.ui.components.createNotificationLargeBitmap
import com.example.ui.components.createNotificationSmallBitmap
import com.example.ui.components.defaultMainColorArgb

object NotificationHelper {

    const val CHANNEL_QUICK_OK = "pixel_rotina_quick_ok"
    const val CHANNEL_SCHEDULED = "pixel_rotina_scheduled"
    const val CHANNEL_PERSISTENT_ALARM = "pixel_rotina_persistent_alarm"
    const val CHANNEL_DAILY_CHECKIN = "pixel_rotina_daily_checkin"

    const val DAILY_CHECKIN_NOTIFICATION_ID = 49_995

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

        val dailyCheckInChannel = NotificationChannel(
            CHANNEL_DAILY_CHECKIN,
            context.getString(R.string.channel_daily_checkin_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_daily_checkin_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 220, 120, 220)
            setSound(notifSound, audioAttrs)
        }

        manager.createNotificationChannels(
            listOf(quickChannel, scheduledChannel, alarmChannel, dailyCheckInChannel)
        )
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

        val okAndMoodIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_OK_AND_MOOD
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routine.id)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ICON, routine.sprite.name)
            putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val okAndMoodPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1500,
            okAndMoodIntent,
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
            "Toque em 'OK! Feito' ou em 'OK + Humor' para já registrar como está agora."
        } else {
            "Momento da sua rotina de ${routine.category.label.lowercase()}."
        }

        val activitySprite = routine.sprite
        val smallIconCompat = IconCompat.createWithBitmap(activitySprite.createNotificationSmallBitmap(64))
        val largeBitmap = activitySprite.createNotificationLargeBitmap(128)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconCompat)
            .setLargeIcon(largeBitmap)
            .setColor(activitySprite.defaultMainColorArgb())
            .setContentTitle("${routine.title} • ${routine.category.label}")
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
                android.R.drawable.ic_menu_edit,
                context.getString(R.string.action_ok_with_mood),
                okAndMoodPendingIntent
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
        iconName: String = "",
        snoozeMinutes: Int = 10
    ): Notification {
        ensureChannelsCreated(context)
        val notificationId = notificationIdForRoutine(routineId)

        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_DESC, description)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ICON, iconName)
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

        val activitySprite = PixelSpriteType.entries.firstOrNull { it.name == iconName }
            ?: PixelSpriteType.ALARM_BELL
        val smallIconCompat = IconCompat.createWithBitmap(activitySprite.createNotificationSmallBitmap(64))
        val largeBitmap = activitySprite.createNotificationLargeBitmap(128)

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT_ALARM)
            .setSmallIcon(smallIconCompat)
            .setLargeIcon(largeBitmap)
            .setColor(activitySprite.defaultMainColorArgb())
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

    fun showPomodoroIntervalFinishedNotification(
        context: Context,
        title: String,
        message: String,
        spriteType: PixelSpriteType = PixelSpriteType.BOOK_FOCUS
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            49_990,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconCompat = IconCompat.createWithBitmap(spriteType.createNotificationSmallBitmap(64))
        val largeBitmap = spriteType.createNotificationLargeBitmap(128)

        val builder = NotificationCompat.Builder(context, CHANNEL_SCHEDULED)
            .setSmallIcon(smallIconCompat)
            .setLargeIcon(largeBitmap)
            .setColor(spriteType.defaultMainColorArgb())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(49_990, builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    /**
     * Step 1 of 2: Interactive notification asking for Mood right in the notification shade.
     * Used both for the configurable End-of-Day Daily Check-in and right after tapping "OK + Humor" on a routine.
     */
    fun showMoodStepNotification(
        context: Context,
        notificationId: Int = DAILY_CHECKIN_NOTIFICATION_ID,
        contextTitle: String = "",
        routineId: Long = -1L,
        spriteName: String = "",
        isDailyEndOfDay: Boolean = true,
        silentUpdate: Boolean = false
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val openJournalIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_OPEN_TAB, "JOURNAL")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 6000,
            openJournalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun moodActionPendingIntent(moodScore: Int, offset: Int): PendingIntent {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_NOTIF_PICK_MOOD
                putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, contextTitle)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ICON, spriteName)
                putExtra(AlarmScheduler.EXTRA_MOOD_SCORE, moodScore)
                putExtra(AlarmScheduler.EXTRA_IS_DAILY_CHECKIN, isDailyEndOfDay)
            }
            return PendingIntent.getBroadcast(
                context,
                notificationId + 6100 + offset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sprite = PixelSpriteType.entries.firstOrNull { it.name == spriteName }
            ?: if (isDailyEndOfDay) PixelSpriteType.MOON_REST else PixelSpriteType.SPARKLE_OK
        val smallIconCompat = IconCompat.createWithBitmap(sprite.createNotificationSmallBitmap(64))
        val largeBitmap = sprite.createNotificationLargeBitmap(128)

        val title = if (isDailyEndOfDay) {
            "🌙 Check-in do Fim do Dia"
        } else {
            "✓ $contextTitle concluída!"
        }
        val body = if (isDailyEndOfDay) {
            "Passo 1/2 — Como foi seu humor hoje? Responda aqui ou toque para abrir o Diário."
        } else {
            "Passo 1/2 — Como está seu humor agora? (Vai para sua média do dia)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_CHECKIN)
            .setSmallIcon(smallIconCompat)
            .setLargeIcon(largeBitmap)
            .setColor(sprite.defaultMainColorArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (silentUpdate) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(silentUpdate)
            .setOnlyAlertOnce(silentUpdate)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "😔 Baixo", moodActionPendingIntent(2, 1))
            .addAction(0, "😐 Neutro", moodActionPendingIntent(3, 2))
            .addAction(0, "😄 Alto", moodActionPendingIntent(5, 3))

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    /**
     * Step 2 of 2: Silently morphs the same notification in-place to ask for Energy after Mood was picked.
     */
    fun showEnergyStepNotification(
        context: Context,
        notificationId: Int,
        moodScore: Int,
        contextTitle: String = "",
        routineId: Long = -1L,
        spriteName: String = "",
        isDailyEndOfDay: Boolean = true
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val openJournalIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_OPEN_TAB, "JOURNAL")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 7000,
            openJournalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun energyActionPendingIntent(energyScore: Int, offset: Int): PendingIntent {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_NOTIF_PICK_ENERGY
                putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, contextTitle)
                putExtra(AlarmScheduler.EXTRA_MOOD_SCORE, moodScore)
                putExtra(AlarmScheduler.EXTRA_ENERGY_SCORE, energyScore)
                putExtra(AlarmScheduler.EXTRA_IS_DAILY_CHECKIN, isDailyEndOfDay)
            }
            return PendingIntent.getBroadcast(
                context,
                notificationId + 7100 + offset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sprite = PixelSpriteType.entries.firstOrNull { it.name == spriteName }
            ?: if (isDailyEndOfDay) PixelSpriteType.MOON_REST else PixelSpriteType.SPARKLE_OK
        val smallIconCompat = IconCompat.createWithBitmap(sprite.createNotificationSmallBitmap(64))
        val largeBitmap = sprite.createNotificationLargeBitmap(128)

        val moodBadge = when {
            moodScore <= 2 -> "😔 Baixo ($moodScore/5)"
            moodScore == 3 -> "😐 Neutro (3/5)"
            else -> "😄 Alto ($moodScore/5)"
        }

        val title = "Humor: $moodBadge • Passo 2/2"
        val body = if (isDailyEndOfDay) {
            "E como está sua energia hoje? Toque abaixo para salvar na sua média diária."
        } else {
            "E como está sua energia agora após '$contextTitle'?"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_CHECKIN)
            .setSmallIcon(smallIconCompat)
            .setLargeIcon(largeBitmap)
            .setColor(sprite.defaultMainColorArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "🪫 Baixa", energyActionPendingIntent(2, 1))
            .addAction(0, "🔋 Média", energyActionPendingIntent(3, 2))
            .addAction(0, "⚡ Alta", energyActionPendingIntent(5, 3))

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    fun cancelNotification(context: Context, routineId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationIdForRoutine(routineId))
    }

    fun cancelNotificationById(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun notificationIdForRoutine(routineId: Long): Int {
        return (50_000L + (routineId % 40_000L)).toInt()
    }
}
