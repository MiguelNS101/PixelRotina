package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.RoutineEntity
import com.example.ui.theme.LocalPixelPalette
import java.util.Locale
import kotlin.math.sin

enum class PomodoroPhase(
    val title: String,
    val badgeLabel: String,
    val shortTabLabel: String,
    val defaultMinutes: Int
) {
    FOCUS("FOCO PROFUNDO", "🍅 FOCO", "🍅 FOCO", 25),
    SHORT_BREAK("PAUSA CURTA", "☕ PAUSA CURTA", "☕ CURTA", 5),
    LONG_BREAK("PAUSA LONGA", "🌿 PAUSA LONGA", "🌿 LONGA", 15)
}

data class PomodoroVisualAlert(
    val completedPhase: PomodoroPhase,
    val completedCyclesToday: Int,
    val linkedRoutineId: Long?,
    val linkedRoutineTitle: String?,
    val headline: String,
    val subtitle: String,
    val wellnessTip: String
)

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val totalSeconds: Int = PomodoroPhase.FOCUS.defaultMinutes * 60,
    val remainingSeconds: Int = PomodoroPhase.FOCUS.defaultMinutes * 60,
    val isRunning: Boolean = false,
    val isExpanded: Boolean = false,
    val completedFocusCyclesToday: Int = 0,
    val linkedRoutineId: Long? = null,
    val linkedRoutineTitle: String? = null,
    val visualAlert: PomodoroVisualAlert? = null
) {
    val progressFraction: Float
        get() {
            if (totalSeconds <= 0) return 0f
            val elapsed = (totalSeconds - remainingSeconds).coerceIn(0, totalSeconds)
            return (elapsed.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        }

    val formattedTime: String
        get() {
            val mins = (remainingSeconds / 60).coerceAtLeast(0)
            val secs = (remainingSeconds % 60).coerceAtLeast(0)
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
}

@Composable
fun PomodoroEmbeddedCard(
    pomodoroState: PomodoroState,
    todayRoutines: List<RoutineEntity>,
    onToggleExpanded: () -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSetCustomDurationSeconds: (Int) -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onCompleteIntervalNow: () -> Unit,
    onLinkRoutine: (RoutineEntity?) -> Unit,
    onDismissVisualAlert: () -> Unit,
    onCompleteLinkedRoutineFromAlert: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    var showTaskSelector by remember { mutableStateOf(false) }

    val phaseAccentColor = when (pomodoroState.phase) {
        PomodoroPhase.FOCUS -> palette.alarmCoral
        PomodoroPhase.SHORT_BREAK -> palette.waterCyan
        PomodoroPhase.LONG_BREAK -> palette.wellnessGreen
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Pixel Art Visual Reminder Modal/Banner when an interval finishes
        AnimatedVisibility(
            visible = pomodoroState.visualAlert != null,
            enter = fadeIn(tween(260)) + expandVertically(tween(280)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(220))
        ) {
            pomodoroState.visualAlert?.let { alert ->
                PomodoroIntervalVisualReminderBanner(
                    alert = alert,
                    onStartNextSuggestedPhase = {
                        val nextPhase = if (alert.completedPhase == PomodoroPhase.FOCUS) {
                            if (alert.completedCyclesToday > 0 && alert.completedCyclesToday % 4 == 0) {
                                PomodoroPhase.LONG_BREAK
                            } else {
                                PomodoroPhase.SHORT_BREAK
                            }
                        } else {
                            PomodoroPhase.FOCUS
                        }
                        onDismissVisualAlert()
                        onSelectPhase(nextPhase)
                        onToggleStartPause()
                    },
                    onCompleteLinkedTaskAndBreak = { routineId ->
                        onCompleteLinkedRoutineFromAlert(routineId)
                    },
                    onDismiss = onDismissVisualAlert
                )
            }
        }

        // 2. Main Embedded Pomodoro Card (Closed by default, clean single-line layout)
        PixelCard(
            containerColor = palette.surfaceElevated,
            borderColor = if (pomodoroState.isRunning) phaseAccentColor else palette.pixelBorder,
            modifier = Modifier.testTag("pomodoro_embedded_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top Row: Companion Icon + Single-line Title & Subtitle + Expand/Collapse Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                phaseAccentColor.copy(alpha = 0.18f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PomodoroPixelArtCompanion(
                            phase = pomodoroState.phase,
                            isRunning = pomodoroState.isRunning,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "POMODORO DE FOCO",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pomodoroState.linkedRoutineTitle?.let { "Tarefa: $it" }
                                ?: "Sessões de foco com avisos em pixel art",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    PomodoroTactileChip(
                        text = if (pomodoroState.isExpanded) "▲ FECHAR" else "▼ ABRIR",
                        selected = false,
                        accentColor = palette.secondary,
                        onClick = onToggleExpanded,
                        modifier = Modifier.testTag("pomodoro_toggle_expand_button")
                    )
                }

                // Second Compact Status Row: Timer Badge + 4 Pixel Tomatoes + Quick Action when Closed
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        PixelBadge(
                            text = "${pomodoroState.phase.shortTabLabel} • ${pomodoroState.formattedTime}",
                            containerColor = phaseAccentColor,
                            contentColor = palette.onPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val cycleInRound = pomodoroState.completedFocusCyclesToday % 4
                            val hasCompletedFullRound =
                                pomodoroState.completedFocusCyclesToday > 0 && cycleInRound == 0
                            for (i in 0 until 4) {
                                val filled = hasCompletedFullRound || i < cycleInRound
                                PixelMiniTomatoIndicator(
                                    isFilled = filled,
                                    activeColor = palette.alarmCoral,
                                    inactiveColor = palette.surface
                                )
                            }
                        }
                    }

                    if (!pomodoroState.isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PomodoroTactileChip(
                            text = if (pomodoroState.isRunning) "⏸ PAUSAR" else "▶ INICIAR",
                            selected = pomodoroState.isRunning,
                            accentColor = phaseAccentColor,
                            onClick = onToggleStartPause,
                            modifier = Modifier.testTag("pomodoro_compact_start_button")
                        )
                    }
                }

                // Expanded Controls & Retro Digital Timer Display
                AnimatedVisibility(
                    visible = pomodoroState.isExpanded,
                    enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Phase Selector Pills (Short labels that never wrap in 3 columns)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PomodoroPhase.entries.forEach { phase ->
                                val selected = pomodoroState.phase == phase
                                PomodoroTactileChip(
                                    text = phase.shortTabLabel,
                                    selected = selected,
                                    accentColor = when (phase) {
                                        PomodoroPhase.FOCUS -> palette.alarmCoral
                                        PomodoroPhase.SHORT_BREAK -> palette.waterCyan
                                        PomodoroPhase.LONG_BREAK -> palette.wellnessGreen
                                    },
                                    onClick = { onSelectPhase(phase) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pomodoro_phase_${phase.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Retro 8-bit Digital Clock Display Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.background)
                                .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${pomodoroState.phase.title} (${pomodoroState.completedFocusCyclesToday} CICLOS HOJE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = phaseAccentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pomodoroState.formattedTime,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.testTag("pomodoro_timer_text")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                PixelBlockProgressBar(
                                    progress = pomodoroState.progressFraction,
                                    totalBlocks = 12,
                                    activeColor = phaseAccentColor,
                                    inactiveColor = palette.surfaceElevated
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Duration Preset Chips (Horizontally scrollable with non-wrapping chips)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val presets = listOf(
                                        "15 MIN" to 15 * 60,
                                        "25 MIN" to 25 * 60,
                                        "45 MIN" to 45 * 60,
                                        "5 MIN" to 5 * 60,
                                        "⚡ 5S TESTE" to 5
                                    )
                                    presets.forEach { (label, seconds) ->
                                        val isCurrentTotal = pomodoroState.totalSeconds == seconds
                                        PomodoroTactileChip(
                                            text = label,
                                            selected = isCurrentTotal,
                                            accentColor = palette.secondary,
                                            onClick = { onSetCustomDurationSeconds(seconds) },
                                            modifier = Modifier.testTag("pomodoro_preset_$seconds")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Task Linking Bar (Concise single-line text with ellipsis)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.surface)
                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .clickable { showTaskSelector = !showTaskSelector }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("pomodoro_link_task_bar"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                PixelSpriteIcon(
                                    spriteType = PixelSpriteType.BOOK_FOCUS,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pomodoroState.linkedRoutineTitle?.let { "Foco em: $it" }
                                        ?: "Vincular a uma tarefa de hoje",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showTaskSelector) "▲ FECHAR" else "▼ TROCAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        AnimatedVisibility(visible = showTaskSelector) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.background)
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Escolha uma tarefa para vincular ao Pomodoro:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PomodoroTactileChip(
                                        text = "FOCO LIVRE",
                                        selected = pomodoroState.linkedRoutineId == null,
                                        accentColor = palette.secondary,
                                        onClick = {
                                            onLinkRoutine(null)
                                            showTaskSelector = false
                                        }
                                    )
                                    todayRoutines.forEach { routine ->
                                        PomodoroTactileChip(
                                            text = "🍅 ${routine.title}",
                                            selected = pomodoroState.linkedRoutineId == routine.id,
                                            accentColor = palette.primary,
                                            onClick = {
                                                onLinkRoutine(routine)
                                                showTaskSelector = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Main Physical Action Buttons (2-row layout so buttons never wrap or clip)
                        PixelButton(
                            text = if (pomodoroState.isRunning) "⏸ PAUSAR CONTAGEM" else "▶ INICIAR SESSÃO DE FOCO",
                            onClick = onToggleStartPause,
                            containerColor = if (pomodoroState.isRunning) palette.snoozeAmber else phaseAccentColor,
                            contentColor = if (pomodoroState.isRunning) palette.onSecondary else palette.onPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pomodoro_start_pause_button")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelButton(
                                text = "↺ REINICIAR",
                                onClick = onResetTimer,
                                containerColor = palette.surface,
                                contentColor = palette.textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pomodoro_reset_button")
                            )
                            PixelButton(
                                text = "⏭ FIM DO BLOCO",
                                onClick = onCompleteIntervalNow,
                                containerColor = palette.secondary,
                                contentColor = palette.onSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pomodoro_finish_interval_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-contrast Pixel Art Visual Reminder Card displayed at the end of every Pomodoro interval
 * (Focus, Short Break, or Long Break).
 */
@Composable
fun PomodoroIntervalVisualReminderBanner(
    alert: PomodoroVisualAlert,
    onStartNextSuggestedPhase: () -> Unit,
    onCompleteLinkedTaskAndBreak: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val isFocusCompleted = alert.completedPhase == PomodoroPhase.FOCUS
    val bannerColor = if (isFocusCompleted) palette.wellnessGreen else palette.primary

    val infiniteTransition = rememberInfiniteTransition(label = "pomodoro_alert_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_pulse_anim"
    )

    PixelCard(
        containerColor = palette.surfaceElevated,
        borderColor = bannerColor,
        shadowOffset = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("pomodoro_visual_reminder_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerColor.copy(alpha = 0.12f + pulse * 0.08f))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom 8-bit Pixel Art Celebration Illustration Box
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(palette.background, RoundedCornerShape(6.dp))
                        .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PomodoroReminderPixelIllustration(
                        isBreakTimeReminder = isFocusCompleted,
                        pulse = pulse,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    PixelBadge(
                        text = if (isFocusCompleted) {
                            "✨ FIM DO FOCO!"
                        } else {
                            "🍅 FIM DA PAUSA!"
                        },
                        containerColor = bannerColor,
                        contentColor = palette.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert.headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = alert.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pixel Art Wellness Tip Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.surface)
                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelSpriteIcon(
                    spriteType = if (isFocusCompleted) PixelSpriteType.WATER_DROP else PixelSpriteType.SPARKLE_OK,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = alert.wellnessTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // If a routine task was linked to this Focus session, let the user complete it in 1 tap!
            if (isFocusCompleted && alert.linkedRoutineId != null && !alert.linkedRoutineTitle.isNullOrBlank()) {
                PixelButton(
                    text = "✓ CONCLUIR TAREFA + PAUSA",
                    onClick = { onCompleteLinkedTaskAndBreak(alert.linkedRoutineId) },
                    containerColor = palette.wellnessGreen,
                    contentColor = palette.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pomodoro_alert_complete_linked_task")
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val nextButtonLabel = if (isFocusCompleted) {
                    if (alert.completedCyclesToday > 0 && alert.completedCyclesToday % 4 == 0) {
                        "🌿 PAUSA LONGA"
                    } else {
                        "☕ PAUSA CURTA"
                    }
                } else {
                    "🍅 NOVO FOCO"
                }

                PixelButton(
                    text = nextButtonLabel,
                    onClick = onStartNextSuggestedPhase,
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("pomodoro_alert_next_phase_button")
                )
                PixelButton(
                    text = "FECHAR",
                    onClick = onDismiss,
                    containerColor = palette.surface,
                    contentColor = palette.textPrimary,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("pomodoro_alert_dismiss_button")
                )
            }
        }
    }
}

/**
 * Animated 8-bit Pixel Art Companion inside the Pomodoro header:
 * - FOCUS: 8-bit Red Tomato with green leafy crown and orbiting concentration pixels when running.
 * - SHORT_BREAK: 8-bit Steaming Mug with rising steam pixels.
 * - LONG_BREAK: 8-bit Nature Leaf with sun sparkles.
 */
@Composable
fun PomodoroPixelArtCompanion(
    phase: PomodoroPhase,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val transition = rememberInfiniteTransition(label = "pomodoro_companion")
    val animPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "companion_tick"
    )

    val grid: List<String> = when (phase) {
        PomodoroPhase.FOCUS -> listOf(
            "..33.33..",
            ".3333333.",
            ".1113111.",
            "112211111",
            "112111111",
            "111111111",
            "111111111",
            ".1111111.",
            "..11111.."
        )
        PomodoroPhase.SHORT_BREAK -> listOf(
            "..4...4..",
            "...4...4.",
            ".111111..",
            ".12222111",
            ".111111.1",
            ".11111111",
            ".111111..",
            "..1111...",
            ".111111.."
        )
        PomodoroPhase.LONG_BREAK -> listOf(
            "....3333.",
            "..333223.",
            ".3322333.",
            ".323333..",
            ".33333...",
            "..333....",
            ".33......",
            "3........",
            "........."
        )
    }

    val bounceSteps = if (isRunning) {
        if (animPhase < 0.5f) 0f else -1f
    } else {
        0f
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Mascote Pixel Art do Pomodoro: ${phase.title}"
        }
    ) {
        val cols = 9
        val rows = 9
        val cellW = size.width / cols
        val cellH = size.height / rows

        grid.forEachIndexed { rIdx, rowStr ->
            rowStr.forEachIndexed { cIdx, ch ->
                val color = when (ch) {
                    '1' -> if (phase == PomodoroPhase.FOCUS) palette.alarmCoral else palette.waterCyan
                    '2' -> Color.White.copy(alpha = 0.9f)
                    '3' -> palette.wellnessGreen
                    '4' -> {
                        val steamShift = if (isRunning && animPhase > 0.5f) 0.45f else 0.85f
                        Color.White.copy(alpha = steamShift)
                    }
                    else -> null
                }
                if (color != null) {
                    val yOffset = if (ch == '4' && isRunning && animPhase > 0.5f) {
                        -cellH * 0.6f
                    } else {
                        bounceSteps * (cellH * 0.45f)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(cIdx * cellW, rIdx * cellH + yOffset),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }

        if (isRunning) {
            val sparkleColor = palette.snoozeAmber
            val topSparkleY = if (animPhase < 0.5f) 0f else cellH
            drawRect(
                color = sparkleColor,
                topLeft = Offset(0f, topSparkleY),
                size = Size(cellW * 0.9f, cellH * 0.9f)
            )
            drawRect(
                color = sparkleColor,
                topLeft = Offset(size.width - cellW, size.height - cellH - topSparkleY),
                size = Size(cellW * 0.9f, cellH * 0.9f)
            )
        }
    }
}

/**
 * 10x10 Pixel Art Celebration Scene drawn inside the end-of-interval visual reminder banner.
 */
@Composable
private fun PomodoroReminderPixelIllustration(
    isBreakTimeReminder: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val sceneGrid = if (isBreakTimeReminder) {
        listOf(
            ".4..2..4..",
            "..2...2...",
            ".111111...",
            ".12221111.",
            ".111111.1.",
            ".11111111.",
            "..1111....",
            ".333333...",
            "..........",
            ".........."
        )
    } else {
        listOf(
            "4..33.33.4",
            "..3333333.",
            "..1113111.",
            ".112211111",
            ".112111111",
            ".111111111",
            "..1111111.",
            "...11111..",
            "4........4",
            ".........."
        )
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Ilustração em pixel art de fim de intervalo Pomodoro"
        }
    ) {
        val cellW = size.width / 10f
        val cellH = size.height / 10f
        val verticalBob = (sin(pulse * Math.PI).toFloat() * -0.5f) * cellH

        sceneGrid.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                val color = when (ch) {
                    '1' -> if (isBreakTimeReminder) palette.waterCyan else palette.alarmCoral
                    '2' -> Color.White
                    '3' -> palette.wellnessGreen
                    '4' -> palette.snoozeAmber.copy(alpha = 0.5f + pulse * 0.5f)
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(c * cellW, r * cellH + verticalBob),
                        size = Size(cellW + 0.6f, cellH + 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelMiniTomatoIndicator(
    isFilled: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    val palette = LocalPixelPalette.current
    Box(
        modifier = Modifier
            .size(15.dp)
            .background(
                if (isFilled) activeColor else inactiveColor,
                RoundedCornerShape(3.dp)
            )
            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isFilled) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(palette.wellnessGreen, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun PomodoroTactileChip(
    text: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val shape = RoundedCornerShape(5.dp)
    val shadowOffset = 2.5.dp
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pomodoro_chip_press"
    )

    val bgColor = if (selected) accentColor else palette.surface
    val fgColor = if (selected) palette.onPrimary else palette.textPrimary

    Surface(
        color = bgColor,
        shape = shape,
        border = BorderStroke(1.5.dp, palette.pixelBorder),
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .padding(bottom = shadowOffset, end = shadowOffset)
            .drawBehind {
                val fullPx = shadowOffset.toPx()
                val remPx = fullPx * (1f - pressProgress * 0.85f)
                val cornerPx = 5.dp.toPx()
                drawRoundRect(
                    color = palette.pixelBorder,
                    topLeft = Offset(remPx, remPx),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            }
            .graphicsLayer {
                val travelPx = shadowOffset.toPx() * 0.85f * pressProgress
                translationX = travelPx
                translationY = travelPx
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
