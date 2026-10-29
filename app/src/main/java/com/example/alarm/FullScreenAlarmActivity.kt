package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.theme.LocalPixelPalette
import com.example.ui.theme.PixelRotinaTheme

class FullScreenAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndUnlock()
        enableEdgeToEdge()

        val routineId = intent?.getLongExtra(AlarmScheduler.EXTRA_ROUTINE_ID, 1L) ?: 1L
        val title = intent?.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE) ?: "Hora da sua Rotina!"
        val description = intent?.getStringExtra(AlarmScheduler.EXTRA_ROUTINE_DESC) ?: ""

        setContent {
            val prefsRepo = remember { UserPreferencesRepository(applicationContext) }
            val settings by prefsRepo.settingsFlow.collectAsState(initial = UserSettings())

            PixelRotinaTheme(themeType = settings.themeType) {
                FullScreenAlarmContent(
                    title = title,
                    description = description,
                    onDismissAndComplete = {
                        val broadcast = Intent(this, ReminderBroadcastReceiver::class.java).apply {
                            action = AlarmScheduler.ACTION_DISMISS_ALARM
                            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
                            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
                        }
                        sendBroadcast(broadcast)
                        finish()
                    },
                    onSnooze = {
                        val broadcast = Intent(this, ReminderBroadcastReceiver::class.java).apply {
                            action = AlarmScheduler.ACTION_SNOOZE
                            putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routineId)
                            putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, title)
                            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, settings.alarmSnoozeMinutes)
                        }
                        sendBroadcast(broadcast)
                        finish()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
private fun FullScreenAlarmContent(
    title: String,
    description: String,
    onDismissAndComplete: () -> Unit,
    onSnooze: () -> Unit
) {
    val palette = LocalPixelPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(
            containerColor = palette.surfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PixelBadge(
                    text = "ALARME SONORO & VIBRAÇÃO",
                    containerColor = palette.alarmCoral,
                    contentColor = palette.onPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.scale(pulseScale)) {
                    PixelSpriteIcon(
                        spriteType = PixelSpriteType.ALARM_BELL,
                        modifier = Modifier.size(72.dp),
                        tintOverride = palette.alarmCoral,
                        description = "Ícone de alarme tocando"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = DateUtils.formatTime(System.currentTimeMillis()),
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.primary,
                    textAlign = TextAlign.Center
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                PixelButton(
                    text = "✓ DESLIGAR E CONCLUIR",
                    onClick = onDismissAndComplete,
                    containerColor = palette.wellnessGreen,
                    contentColor = palette.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_dismiss_button")
                )
                Spacer(modifier = Modifier.height(14.dp))
                PixelButton(
                    text = "⏰ SONECA +10 MIN",
                    onClick = onSnooze,
                    containerColor = palette.snoozeAmber,
                    contentColor = palette.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_snooze_button")
                )
            }
        }
    }
}
