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
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AlertMode
import com.example.data.HeroArtPreference
import com.example.data.RoutineEntity
import com.example.ui.components.PixelSpriteType
import com.example.ui.components.createNotificationLargeBitmap
import com.example.ui.components.createNotificationSmallBitmap
import com.example.ui.components.createPixelEnergyBitmap
import com.example.ui.components.createPixelMoodBitmap
import com.example.ui.components.defaultMainColorArgb
import com.example.ui.components.moodLabelPt
import com.example.ui.theme.PixelThemeType
import com.example.ui.theme.getPixelPalette

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

    @DrawableRes
    private fun resolveHeroArtDrawable(heroArtPreference: HeroArtPreference): Int {
        return heroArtPreference.resolveForHour().drawableRes ?: R.drawable.img_pixel_hero_banner
    }

    private fun resolveHeroSceneTag(heroArtPreference: HeroArtPreference): String {
        return when (heroArtPreference.resolveForHour()) {
            HeroArtPreference.MORNING -> "☀ MANHÃ"
            HeroArtPreference.MORNING_RAIN -> "🌧 CHUVA"
            HeroArtPreference.MORNING_SUNRISE -> "🍵 MATCHA"
            HeroArtPreference.AFTERNOON -> "🌤 TARDE"
            HeroArtPreference.AFTERNOON_GARDEN -> "🌿 JARDIM"
            HeroArtPreference.AFTERNOON_LIBRARY -> "📚 ESTUDOS"
            HeroArtPreference.EVENING -> "🌇 CREPÚSCULO"
            HeroArtPreference.EVENING_ROOFTOP -> "🌆 TERRAÇO"
            HeroArtPreference.NIGHT -> "🌙 NOITE"
            HeroArtPreference.NIGHT_CATPPUCCIN -> "🐾 GATINHO"
            HeroArtPreference.NIGHT_CAMPFIRE -> "🔥 LAGO"
            HeroArtPreference.AUTO -> "✦ PIXEL"
        }
    }

    fun buildRoutineNotification(
        context: Context,
        routine: RoutineEntity,
        snoozeMinutes: Int = 15,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ): Notification {
        ensureChannelsCreated(context)

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
            "Conclua com OK ou registre seu humor e foco agora."
        } else {
            "Momento da sua rotina de ${routine.category.label.lowercase()}."
        }

        val activitySprite = routine.sprite
        val smallIconCompat = IconCompat.createWithBitmap(activitySprite.createNotificationSmallBitmap(64))
        val spriteBitmap = activitySprite.createNotificationLargeBitmap(96)
        val palette = getPixelPalette(themeType)
        val accentColorArgb = palette.primary.toArgb()
        val heroRes = resolveHeroArtDrawable(heroArtPreference)
        val sceneTag = resolveHeroSceneTag(heroArtPreference)

        // Full-art collapsed custom view
        val collapsedViews = RemoteViews(context.packageName, R.layout.notif_routine_collapsed).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_title, routine.title.uppercase())
            setTextViewText(R.id.notif_subtitle, subtitle)
            setOnClickPendingIntent(R.id.notif_btn_ok_collapsed, okPendingIntent)
        }

        // Full-art expanded custom view with hero scene art + 3 pixel buttons
        val expandedViews = RemoteViews(context.packageName, R.layout.notif_routine_expanded).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_category_badge, "★ ${routine.category.label.uppercase()}")
            setTextViewText(R.id.notif_scene_badge, sceneTag)
            setTextViewText(R.id.notif_title, routine.title)
            setTextViewText(R.id.notif_subtitle, subtitle)
            setTextViewText(R.id.notif_btn_snooze, "⏰ +${snoozeMinutes}M")
            setOnClickPendingIntent(R.id.notif_btn_ok, okPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_ok_mood, okAndMoodPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_snooze, snoozePendingIntent)
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconCompat)
            .setColor(activitySprite.defaultMainColorArgb())
            .setContentTitle("${routine.title} • ${routine.category.label}")
            .setContentText(subtitle)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(collapsedViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showRoutineNotification(
        context: Context,
        routine: RoutineEntity,
        snoozeMinutes: Int = 15,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notification = buildRoutineNotification(
            context = context,
            routine = routine,
            snoozeMinutes = snoozeMinutes,
            themeType = themeType,
            heroArtPreference = heroArtPreference
        )
        try {
            NotificationManagerCompat.from(context).notify(notificationIdForRoutine(routine.id), notification)
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
        snoozeMinutes: Int = 10,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
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

        val okAndMoodIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_OK_AND_MOOD
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ROUTINE_ICON, iconName)
            putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmScheduler.EXTRA_IS_FROM_ALARM, true)
        }
        val okAndMoodPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 4500,
            okAndMoodIntent,
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
            "Alarme ativo! Desligue ou toque em OK+Humor para registrar como está."
        }

        val activitySprite = PixelSpriteType.entries.firstOrNull { it.name == iconName }
            ?: PixelSpriteType.ALARM_BELL
        val smallIconCompat = IconCompat.createWithBitmap(activitySprite.createNotificationSmallBitmap(64))
        val spriteBitmap = activitySprite.createNotificationLargeBitmap(96)
        val palette = getPixelPalette(themeType)
        val accentColorArgb = palette.alarmCoral.toArgb()
        val heroRes = resolveHeroArtDrawable(heroArtPreference)
        val sceneTag = resolveHeroSceneTag(heroArtPreference)

        val collapsedViews = RemoteViews(context.packageName, R.layout.notif_alarm_collapsed).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_title, "🔔 ${title.uppercase()}")
            setTextViewText(R.id.notif_subtitle, bodyText)
            setOnClickPendingIntent(R.id.notif_btn_dismiss_collapsed, dismissPendingIntent)
        }

        val expandedViews = RemoteViews(context.packageName, R.layout.notif_alarm_expanded).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_scene_badge, sceneTag)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, bodyText)
            setTextViewText(R.id.notif_btn_snooze, "⏰ +${snoozeMinutes}M")
            setOnClickPendingIntent(R.id.notif_btn_dismiss, dismissPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_ok_mood, okAndMoodPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_snooze, snoozePendingIntent)
        }

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT_ALARM)
            .setSmallIcon(smallIconCompat)
            .setColor(accentColorArgb)
            .setContentTitle("🔔 ALARME: $title")
            .setContentText(bodyText)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(expandedViews)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
    }

    fun buildPomodoroIntervalFinishedNotification(
        context: Context,
        title: String,
        message: String,
        spriteType: PixelSpriteType = PixelSpriteType.BOOK_FOCUS,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ): Notification {
        ensureChannelsCreated(context)

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
        val spriteBitmap = spriteType.createNotificationLargeBitmap(96)
        val palette = getPixelPalette(themeType)
        val accentColorArgb = palette.accent.toArgb()
        val heroRes = resolveHeroArtDrawable(heroArtPreference)
        val sceneTag = resolveHeroSceneTag(heroArtPreference)

        val collapsedViews = RemoteViews(context.packageName, R.layout.notif_mood_energy_collapsed).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, message)
            setTextViewText(R.id.notif_step_badge, "🍅 FIM")
        }

        val expandedViews = RemoteViews(context.packageName, R.layout.notif_pomodoro_expanded).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_scene_badge, sceneTag)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, message)
        }

        return NotificationCompat.Builder(context, CHANNEL_SCHEDULED)
            .setSmallIcon(smallIconCompat)
            .setColor(spriteType.defaultMainColorArgb())
            .setContentTitle(title)
            .setContentText(message)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(collapsedViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showPomodoroIntervalFinishedNotification(
        context: Context,
        title: String,
        message: String,
        spriteType: PixelSpriteType = PixelSpriteType.BOOK_FOCUS,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notification = buildPomodoroIntervalFinishedNotification(
            context = context,
            title = title,
            message = message,
            spriteType = spriteType,
            themeType = themeType,
            heroArtPreference = heroArtPreference
        )
        try {
            NotificationManagerCompat.from(context).notify(49_990, notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    /**
     * Step 1 of 2: Full-art pixel notification with all 5 Mood levels (1..5) rendered as real 8-bit sprites.
     */
    fun buildMoodStepNotification(
        context: Context,
        notificationId: Int = DAILY_CHECKIN_NOTIFICATION_ID,
        contextTitle: String = "",
        routineId: Long = -1L,
        spriteName: String = "",
        isDailyEndOfDay: Boolean = true,
        silentUpdate: Boolean = false,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ): Notification {
        ensureChannelsCreated(context)

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

        fun moodActionPendingIntent(moodScore: Int): PendingIntent {
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
                notificationId + 6100 + moodScore,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sprite = PixelSpriteType.entries.firstOrNull { it.name == spriteName }
            ?: if (isDailyEndOfDay) PixelSpriteType.MOON_REST else PixelSpriteType.SPARKLE_OK
        val smallIconCompat = IconCompat.createWithBitmap(sprite.createNotificationSmallBitmap(64))
        val spriteBitmap = sprite.createNotificationLargeBitmap(96)
        val palette = getPixelPalette(themeType)
        val accentColorArgb = palette.primary.toArgb()
        val heroRes = resolveHeroArtDrawable(heroArtPreference)
        val sceneTag = resolveHeroSceneTag(heroArtPreference)

        val title = if (isDailyEndOfDay) {
            "CHECK-IN DO FIM DO DIA"
        } else {
            "✓ ${contextTitle.uppercase()}"
        }
        val body = if (isDailyEndOfDay) {
            "Como foi seu humor hoje? Escolha de 1 a 5:"
        } else {
            "Como está seu humor agora? (Vai p/ sua média do dia)"
        }

        val collapsedViews = RemoteViews(context.packageName, R.layout.notif_mood_energy_collapsed).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, "Expanda p/ 5 níveis de humor (1–5) ▼")
            setTextViewText(R.id.notif_step_badge, "1/2")
        }

        val expandedViews = RemoteViews(context.packageName, R.layout.notif_mood_energy_expanded).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, spriteBitmap)
            setTextViewText(R.id.notif_step_badge, "PASSO 1/2 • HUMOR")
            setTextViewText(R.id.notif_scene_badge, sceneTag)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, body)

            val shortLabels = listOf("Esgot.", "Baixo", "Estáv.", "Bem", "Ótimo")
            val btnIds = intArrayOf(
                R.id.notif_btn_level_1,
                R.id.notif_btn_level_2,
                R.id.notif_btn_level_3,
                R.id.notif_btn_level_4,
                R.id.notif_btn_level_5
            )
            val iconIds = intArrayOf(
                R.id.notif_icon_level_1,
                R.id.notif_icon_level_2,
                R.id.notif_icon_level_3,
                R.id.notif_icon_level_4,
                R.id.notif_icon_level_5
            )
            val labelIds = intArrayOf(
                R.id.notif_label_level_1,
                R.id.notif_label_level_2,
                R.id.notif_label_level_3,
                R.id.notif_label_level_4,
                R.id.notif_label_level_5
            )

            for (i in 0 until 5) {
                val level = i + 1
                setImageViewBitmap(iconIds[i], createPixelMoodBitmap(level, 64))
                setTextViewText(labelIds[i], shortLabels[i])
                setOnClickPendingIntent(btnIds[i], moodActionPendingIntent(level))
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_DAILY_CHECKIN)
            .setSmallIcon(smallIconCompat)
            .setColor(sprite.defaultMainColorArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(expandedViews)
            .setPriority(if (silentUpdate) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(silentUpdate)
            .setOnlyAlertOnce(silentUpdate)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showMoodStepNotification(
        context: Context,
        notificationId: Int = DAILY_CHECKIN_NOTIFICATION_ID,
        contextTitle: String = "",
        routineId: Long = -1L,
        spriteName: String = "",
        isDailyEndOfDay: Boolean = true,
        silentUpdate: Boolean = false,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notification = buildMoodStepNotification(
            context = context,
            notificationId = notificationId,
            contextTitle = contextTitle,
            routineId = routineId,
            spriteName = spriteName,
            isDailyEndOfDay = isDailyEndOfDay,
            silentUpdate = silentUpdate,
            themeType = themeType,
            heroArtPreference = heroArtPreference
        )
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    /**
     * Step 2 of 2: Silently morphs the same full-art pixel notification in-place to show all 5 Energy/Focus
     * battery levels (1..5) rendered as real 8-bit sprites.
     */
    fun buildEnergyStepNotification(
        context: Context,
        notificationId: Int,
        moodScore: Int,
        contextTitle: String = "",
        routineId: Long = -1L,
        spriteName: String = "",
        isDailyEndOfDay: Boolean = true,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ): Notification {
        ensureChannelsCreated(context)

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

        fun energyActionPendingIntent(energyScore: Int): PendingIntent {
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
                notificationId + 7100 + energyScore,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sprite = PixelSpriteType.entries.firstOrNull { it.name == spriteName }
            ?: if (isDailyEndOfDay) PixelSpriteType.MOON_REST else PixelSpriteType.SPARKLE_OK
        val smallIconCompat = IconCompat.createWithBitmap(sprite.createNotificationSmallBitmap(64))
        val moodIconBitmap = createPixelMoodBitmap(moodScore, 64)
        val palette = getPixelPalette(themeType)
        val accentColorArgb = palette.wellnessGreen.toArgb()
        val heroRes = resolveHeroArtDrawable(heroArtPreference)
        val sceneTag = resolveHeroSceneTag(heroArtPreference)

        val clampedMood = moodScore.coerceIn(1, 5)
        val title = "HUMOR $clampedMood/5 (${moodLabelPt(clampedMood).uppercase()})"
        val body = if (isDailyEndOfDay) {
            "Agora escolha sua energia / foco de hoje (1 a 5):"
        } else {
            "E sua energia / foco após '$contextTitle'?"
        }

        val collapsedViews = RemoteViews(context.packageName, R.layout.notif_mood_energy_collapsed).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, moodIconBitmap)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, "Expanda p/ 5 níveis de energia/foco (1–5) ▼")
            setTextViewText(R.id.notif_step_badge, "2/2")
        }

        val expandedViews = RemoteViews(context.packageName, R.layout.notif_mood_energy_expanded).apply {
            setImageViewResource(R.id.notif_bg_art, heroRes)
            setInt(R.id.notif_accent_bar, "setBackgroundColor", accentColorArgb)
            setImageViewBitmap(R.id.notif_sprite_icon, moodIconBitmap)
            setTextViewText(R.id.notif_step_badge, "PASSO 2/2 • FOCO/ENERGIA")
            setTextViewText(R.id.notif_scene_badge, sceneTag)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, body)

            val shortLabels = listOf("Crít.", "Baixa", "Média", "Alta", "Máx.")
            val btnIds = intArrayOf(
                R.id.notif_btn_level_1,
                R.id.notif_btn_level_2,
                R.id.notif_btn_level_3,
                R.id.notif_btn_level_4,
                R.id.notif_btn_level_5
            )
            val iconIds = intArrayOf(
                R.id.notif_icon_level_1,
                R.id.notif_icon_level_2,
                R.id.notif_icon_level_3,
                R.id.notif_icon_level_4,
                R.id.notif_icon_level_5
            )
            val labelIds = intArrayOf(
                R.id.notif_label_level_1,
                R.id.notif_label_level_2,
                R.id.notif_label_level_3,
                R.id.notif_label_level_4,
                R.id.notif_label_level_5
            )

            for (i in 0 until 5) {
                val level = i + 1
                setImageViewBitmap(iconIds[i], createPixelEnergyBitmap(level, 64))
                setTextViewText(labelIds[i], shortLabels[i])
                setOnClickPendingIntent(btnIds[i], energyActionPendingIntent(level))
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_DAILY_CHECKIN)
            .setSmallIcon(smallIconCompat)
            .setColor(accentColorArgb)
            .setContentTitle(title)
            .setContentText(body)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(expandedViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showEnergyStepNotification(
        context: Context,
        notificationId: Int,
        moodScore: Int,
        contextTitle: String = "",
        routineId: Long = -1L,
        spriteName: String = "",
        isDailyEndOfDay: Boolean = true,
        themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
        heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notification = buildEnergyStepNotification(
            context = context,
            notificationId = notificationId,
            moodScore = moodScore,
            contextTitle = contextTitle,
            routineId = routineId,
            spriteName = spriteName,
            isDailyEndOfDay = isDailyEndOfDay,
            themeType = themeType,
            heroArtPreference = heroArtPreference
        )
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
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
