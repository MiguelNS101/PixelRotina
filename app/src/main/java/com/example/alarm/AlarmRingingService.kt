package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

class AlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val routineId = intent?.getLongExtra(AlarmScheduler.EXTRA_ROUTINE_ID, 1L) ?: 1L
        val title = intent?.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE) ?: "Rotina de Bem-Estar"
        val description = intent?.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_DESC) ?: ""
        val iconName = intent?.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_ICON) ?: ""

        val notification = NotificationHelper.buildPersistentAlarmForegroundNotification(
            context = this,
            routineId = routineId,
            title = title,
            description = description,
            iconName = iconName
        )
        val notifId = NotificationHelper.notificationIdForRoutine(routineId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notifId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(notifId, notification)
        }

        startContinuousAlarmSound()
        startContinuousVibration()

        // Also launch FullScreenAlarmActivity directly if possible
        try {
            val activityIntent = Intent(this, FullScreenAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_DESC, description)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ICON, iconName)
            }
            startActivity(activityIntent)
        } catch (_: Exception) {
            // FullScreenIntent on the notification handles lock screen presentation
        }

        return START_STICKY
    }

    private fun startContinuousAlarmSound() {
        stopSound()
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (alarmUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (_: Exception) {
            // Ignore audio hardware issues on headless test environments
        }
    }

    private fun startContinuousVibration() {
        stopVibration()
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 600, 350, 600, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
            // Ignore if vibrator is unavailable
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        } finally {
            vibrator = null
        }
    }

    override fun onDestroy() {
        stopSound()
        stopVibration()
        super.onDestroy()
    }

    companion object {
        fun startAlarm(
            context: Context,
            routineId: Long,
            title: String,
            description: String,
            iconName: String = ""
        ) {
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_DESC, description)
                putExtra(AlarmScheduler.EXTRA_ROUTINE_ICON, iconName)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        fun stopAlarm(context: Context) {
            val serviceIntent = Intent(context, AlarmRingingService::class.java)
            context.stopService(serviceIntent)
        }
    }
}
