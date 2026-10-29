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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AmbientSceneEffect
import com.example.data.DateUtils
import com.example.data.HeroArtPreference
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.theme.LocalPixelPalette
import com.example.ui.theme.PixelRotinaTheme
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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
                    heroArtPreference = settings.heroArtPreference,
                    snoozeMinutes = settings.alarmSnoozeMinutes,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun FullScreenAlarmContent(
    title: String,
    description: String,
    heroArtPreference: HeroArtPreference,
    snoozeMinutes: Int,
    onDismissAndComplete: () -> Unit,
    onSnooze: () -> Unit
) {
    val palette = LocalPixelPalette.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val activeArt = remember(heroArtPreference, currentHour) {
        heroArtPreference.resolveForHour(currentHour)
    }
    val drawableRes = activeArt.drawableRes ?: R.drawable.img_pixel_hero_banner

    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_scale"
    )
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alarm_ambient_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("full_screen_alarm_root")
    ) {
        // 1. Full-Screen Atmospheric Pixel Art Backdrop of the Current Moment
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle atmospheric gradient overlay so the foreground card and photo stand out clearly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.background.copy(alpha = 0.65f),
                            palette.background.copy(alpha = 0.85f),
                            palette.background.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Badge: Ringing Alarm Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelBadge(
                    text = "🔔 ALARME DE ROTINA",
                    containerColor = palette.alarmCoral,
                    contentColor = palette.onPrimary
                )
                PixelBadge(
                    text = DateUtils.formatFullDatePt(DateUtils.todayDateKey()).uppercase(),
                    containerColor = palette.surfaceElevated,
                    contentColor = palette.textPrimary
                )
            }

            // Center Card: Framed Photo of the Moment + Digital Clock + Task Details
            PixelCard(
                containerColor = palette.surfaceElevated,
                borderColor = palette.alarmCoral,
                shadowOffset = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Photo of the Moment Banner inside the Alarm Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = activeArt.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("alarm_moment_photo")
                        )

                        // Animated 8-bit ambient particles on the moment photo
                        AlarmMomentParticles(
                            effect = activeArt.ambientEffect,
                            phase = ambientPhase,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.15f),
                                            Color.Black.copy(alpha = 0.62f)
                                        )
                                    )
                                )
                        )

                        // Top-left Moment of the Day Badge
                        PixelBadge(
                            text = activeArt.title,
                            containerColor = Color.Black.copy(alpha = 0.68f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )

                        // Top-right Pulsing Pixel Alarm Bell
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(44.dp)
                                .scale(pulseScale)
                                .background(palette.alarmCoral, RoundedCornerShape(6.dp))
                                .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelSpriteIcon(
                                spriteType = PixelSpriteType.ALARM_BELL,
                                modifier = Modifier.size(26.dp),
                                tintOverride = palette.onPrimary,
                                description = "Alarme tocando"
                            )
                        }

                        // Large Digital Time overlaid at bottom of the Moment Photo
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = DateUtils.formatTime(System.currentTimeMillis()),
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White
                            )
                            Text(
                                text = activeArt.greeting,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Routine Title & Description
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description.ifBlank {
                                activeArt.cardSubtitle
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Action Area: Pixel Art Slide to Complete + Snooze Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelSlideToComplete(
                    onSlideCompleted = onDismissAndComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_slide_to_complete")
                )

                Spacer(modifier = Modifier.height(14.dp))

                PixelButton(
                    text = "⏰ ADIAR SONECA (+${snoozeMinutes} MIN)",
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

/**
 * Interactive 8-bit Pixel Art "Slide to Complete" control.
 * The user drags the tactile pixel handle from left to right to complete the routine and silence the alarm.
 */
@Composable
fun PixelSlideToComplete(
    onSlideCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSizeDp = 56.dp
    val trackPaddingDp = 4.dp
    val thumbWidthPx = with(density) { thumbSizeDp.toPx() }
    val paddingPx = with(density) { (trackPaddingDp * 2).toPx() }
    val maxDragPx = (trackWidthPx - thumbWidthPx - paddingPx).coerceAtLeast(1f)

    var rawDragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    val animatedOffsetPx by animateFloatAsState(
        targetValue = when {
            isCompleted -> maxDragPx
            isDragging -> rawDragOffsetPx
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slide_thumb_offset"
    )

    val progressFraction = (animatedOffsetPx / maxDragPx).coerceIn(0f, 1f)

    val shimmerTransition = rememberInfiniteTransition(label = "slide_shimmer")
    val arrowPulse by shimmerTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_alpha"
    )

    Box(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfaceElevated)
            .border(3.dp, palette.pixelBorder, RoundedCornerShape(8.dp))
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            }
            .padding(trackPaddingDp)
            .semantics {
                contentDescription = "Deslize para concluir a rotina e desligar o alarme"
                role = Role.Button
                onClick(label = "Concluir e desligar alarme") {
                    if (!isCompleted) {
                        isCompleted = true
                        onSlideCompleted()
                    }
                    true
                }
            }
    ) {
        // Progressive green fill following the dragged thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = (progressFraction * 0.92f + 0.08f).coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(palette.wellnessGreen.copy(alpha = 0.25f + progressFraction * 0.65f))
        )

        // Center guide text
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 64.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isCompleted) {
                    "✓ CONCLUÍDA!"
                } else {
                    "DESLIZE P/ CONCLUIR >>>"
                },
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary.copy(alpha = arrowPulse),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Draggable 3D Pixel Art Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .size(thumbSizeDp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.wellnessGreen)
                .border(2.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!isCompleted) {
                            rawDragOffsetPx = (rawDragOffsetPx + delta).coerceIn(0f, maxDragPx)
                        }
                    },
                    onDragStarted = {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragStopped = {
                        isDragging = false
                        if (rawDragOffsetPx >= maxDragPx * 0.72f) {
                            isCompleted = true
                            rawDragOffsetPx = maxDragPx
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSlideCompleted()
                        } else {
                            rawDragOffsetPx = 0f
                        }
                    }
                )
                .testTag("alarm_slide_thumb"),
            contentAlignment = Alignment.Center
        ) {
            PixelSpriteIcon(
                spriteType = PixelSpriteType.SPARKLE_OK,
                modifier = Modifier.size(26.dp),
                tintOverride = palette.onPrimary,
                description = "Alça de deslizar para concluir"
            )
        }
    }
}

@Composable
private fun AlarmMomentParticles(
    effect: AmbientSceneEffect,
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val snap = 3.dp.toPx().coerceAtLeast(2f)
        when (effect) {
            AmbientSceneEffect.RAIN_DROPS -> {
                for (i in 0 until 16) {
                    val x = (((i * 0.062f) + phase * 0.18f) % 1f) * size.width
                    val y = (((i * 0.13f) + phase * 1.4f) % 1f) * size.height
                    val sx = (x / snap).roundToInt() * snap
                    val sy = (y / snap).roundToInt() * snap
                    drawRect(
                        color = Color(0xFFB8ECFF).copy(alpha = 0.55f),
                        topLeft = Offset(sx, sy),
                        size = Size(snap, snap * 3f)
                    )
                }
            }
            AmbientSceneEffect.SUN_MOTES -> {
                for (i in 0 until 12) {
                    val x = ((i * 0.085f + sin((phase + i * 0.1f) * Math.PI * 2).toFloat() * 0.02f) % 1f) * size.width
                    val y = ((1f - ((phase * 0.5f + i * 0.09f) % 1f))) * size.height
                    val sx = (abs(x) / snap).roundToInt() * snap
                    val sy = (abs(y) / snap).roundToInt() * snap
                    drawRect(
                        color = Color(0xFFFFF3B0).copy(alpha = 0.5f),
                        topLeft = Offset(sx, sy),
                        size = Size(snap * 1.3f, snap * 1.3f)
                    )
                }
            }
            AmbientSceneEffect.TWINKLING_STARS -> {
                for (i in 0 until 14) {
                    val x = ((i * 0.073f + 0.04f) % 0.95f) * size.width
                    val y = ((i * 0.19f + 0.08f) % 0.65f) * size.height
                    val twinkle = (sin((phase + i * 0.14f) * Math.PI * 2).toFloat() + 1f) * 0.5f
                    val sx = (x / snap).roundToInt() * snap
                    val sy = (y / snap).roundToInt() * snap
                    drawRect(
                        color = Color.White.copy(alpha = 0.25f + twinkle * 0.55f),
                        topLeft = Offset(sx, sy),
                        size = Size(snap * 1.2f, snap * 1.2f)
                    )
                }
            }
        }
    }
}
