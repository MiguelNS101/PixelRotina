package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AlertMode
import com.example.data.AmbientSceneEffect
import com.example.data.DateUtils
import com.example.data.FrequencyType
import com.example.data.HeroArtPreference
import com.example.data.RoutineEntity
import com.example.ui.RoutineUiState
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelBlockProgressBar
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.components.PomodoroEmbeddedCard
import com.example.ui.components.PomodoroPhase
import com.example.ui.theme.LocalPixelPalette
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TodayScreen(
    uiState: RoutineUiState,
    onCheckInRoutine: (RoutineEntity) -> Unit,
    onGentleReschedule: (RoutineEntity, Int) -> Unit,
    onEditRoutine: (RoutineEntity) -> Unit,
    onTestAlert: (RoutineEntity) -> Unit,
    onCreateNewRoutine: (FrequencyType) -> Unit,
    onTogglePomodoroExpanded: () -> Unit = {},
    onSelectPomodoroPhase: (PomodoroPhase) -> Unit = {},
    onSetPomodoroCustomDuration: (Int) -> Unit = {},
    onTogglePomodoroStartPause: () -> Unit = {},
    onResetPomodoroTimer: () -> Unit = {},
    onCompletePomodoroIntervalNow: () -> Unit = {},
    onLinkRoutineToPomodoro: (RoutineEntity?, Boolean) -> Unit = { _, _ -> },
    onDismissPomodoroAlert: () -> Unit = {},
    onCompleteLinkedRoutineFromPomodoroAlert: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val completedMap = uiState.todayCompletedCountByRoutine
    val totalDailyTarget = uiState.todayActiveRoutines.sumOf { it.targetCountPerDay }.coerceAtLeast(1)
    val totalDailyDone = uiState.todayActiveRoutines.sumOf { r ->
        (completedMap[r.id] ?: 0).coerceAtMost(r.targetCountPerDay)
    }
    val dailyProgress = (totalDailyDone.toFloat() / totalDailyTarget.toFloat()).coerceIn(0f, 1f)

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    var variationOffset by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
    }
    val activeArt = remember(uiState.settings.heroArtPreference, currentHour, variationOffset) {
        uiState.settings.heroArtPreference.resolveForHour(currentHour, variationOffset)
    }
    val drawableRes = activeArt.drawableRes ?: R.drawable.img_pixel_hero_banner

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dynamic Time-of-Day Pixel Art Hero Card with Variations & Ambient Pixel Particles
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(166.dp)
                    ) {
                        Crossfade(
                            targetState = drawableRes,
                            animationSpec = tween(durationMillis = 550),
                            label = "hero_art_crossfade"
                        ) { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = activeArt.greeting,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Subtle animated 8-bit ambient overlay (Raindrops, Sun Motes, or Twinkling Stars)
                        HeroAmbientPixelEffect(
                            effect = activeArt.ambientEffect,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.34f))
                        )

                        PixelBadge(
                            text = if (uiState.settings.heroArtPreference == HeroArtPreference.AUTO) {
                                "${activeArt.title} • 🎲 Variar"
                            } else {
                                activeArt.title
                            },
                            containerColor = Color.Black.copy(alpha = 0.62f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clickable {
                                    // Allows cycling through the current time-of-day's variations in Auto mode
                                    variationOffset++
                                }
                                .testTag("hero_variation_badge")
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                        ) {
                            PixelBadge(
                                text = DateUtils.formatFullDatePt(uiState.todayDateKey).uppercase(),
                                containerColor = palette.surfaceElevated.copy(alpha = 0.92f),
                                contentColor = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activeArt.greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = activeArt.cardSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RITMO DO DIA (${totalDailyDone}/${totalDailyTarget} CHECK-INS)",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${(dailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PixelBlockProgressBar(
                            progress = dailyProgress,
                            activeColor = palette.wellnessGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelButton(
                                text = "+ INTERVALO",
                                onClick = { onCreateNewRoutine(FrequencyType.INTERVAL) },
                                containerColor = palette.secondary,
                                contentColor = palette.onSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_interval_routine_button")
                            )
                            PixelButton(
                                text = "+ HORÁRIO / ALARME",
                                onClick = { onCreateNewRoutine(FrequencyType.SPECIFIC_TIME) },
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_scheduled_routine_button")
                            )
                        }
                    }
                }
            }
        }

        // 1.5 Embedded Pixel Art Pomodoro Focus Timer & End-of-Interval Visual Reminder
        item {
            PomodoroEmbeddedCard(
                pomodoroState = uiState.pomodoroState,
                todayRoutines = uiState.todayActiveRoutines,
                onToggleExpanded = onTogglePomodoroExpanded,
                onSelectPhase = onSelectPomodoroPhase,
                onSetCustomDurationSeconds = onSetPomodoroCustomDuration,
                onToggleStartPause = onTogglePomodoroStartPause,
                onResetTimer = onResetPomodoroTimer,
                onCompleteIntervalNow = onCompletePomodoroIntervalNow,
                onLinkRoutine = { routine -> onLinkRoutineToPomodoro(routine, false) },
                onDismissVisualAlert = onDismissPomodoroAlert,
                onCompleteLinkedRoutineFromAlert = onCompleteLinkedRoutineFromPomodoroAlert
            )
        }

        // 2. Smart Gentle Rescheduling Banner (if any scheduled routine is past its time today)
        if (uiState.overdueTodayRoutines.isNotEmpty()) {
            item {
                PixelCard(
                    containerColor = palette.snoozeAmber.copy(alpha = 0.2f),
                    borderColor = palette.pixelBorder
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelSpriteIcon(
                                spriteType = PixelSpriteType.SUN_MORNING,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REAGENDAMENTO SUAVE",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Passou do horário? Tudo bem! Reagende com 1 toque ou marque como feito sem pressão:",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        uiState.overdueTodayRoutines.forEach { overdue ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.surfaceElevated)
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${overdue.formattedScheduleSummary()} • ${overdue.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PixelButton(
                                        text = "✓ FEITO",
                                        onClick = { onCheckInRoutine(overdue) },
                                        containerColor = palette.wellnessGreen,
                                        contentColor = palette.onPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PixelButton(
                                        text = "⏰ +15M",
                                        onClick = { onGentleReschedule(overdue, 15) },
                                        containerColor = palette.snoozeAmber,
                                        contentColor = palette.onSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PixelButton(
                                        text = "⏰ +1H",
                                        onClick = { onGentleReschedule(overdue, 60) },
                                        containerColor = palette.surface,
                                        contentColor = palette.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Interval Quick-OK Habits (Water, Stretch, Eye Rest, etc.)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AVISOS DE INTERVALO (OK NA NOTIFICAÇÃO)",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                PixelBadge(
                    text = "${uiState.todayIntervalRoutines.size} ATIVOS",
                    containerColor = palette.waterCyan,
                    contentColor = palette.onPrimary
                )
            }
        }

        if (uiState.todayIntervalRoutines.isEmpty()) {
            item {
                PixelCard(containerColor = palette.surface) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum aviso recorrente de intervalo configurado para hoje.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.todayIntervalRoutines, key = { "interval_${it.id}" }) { routine ->
                val doneCount = completedMap[routine.id] ?: 0
                val isFocusedInPomodoro = uiState.pomodoroState.linkedRoutineId == routine.id
                IntervalRoutineCard(
                    routine = routine,
                    doneCount = doneCount,
                    isFocusedInPomodoro = isFocusedInPomodoro,
                    onCheckIn = { onCheckInRoutine(routine) },
                    onSnooze15 = { onGentleReschedule(routine, uiState.settings.defaultSnoozeMinutes) },
                    onFocusPomodoro = { onLinkRoutineToPomodoro(routine, true) },
                    onTestNotification = { onTestAlert(routine) },
                    onEdit = { onEditRoutine(routine) }
                )
            }
        }

        // 4. Specific-Time Reminders & Persistent Alarms
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HORÁRIOS ESPECÍFICOS & ALARMES DO DIA",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                PixelBadge(
                    text = "${uiState.todayScheduledRoutines.size} HOJE",
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            }
        }

        if (uiState.todayScheduledRoutines.isEmpty()) {
            item {
                PixelCard(containerColor = palette.surface) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nenhum lembrete de horário fixo para hoje. Toque em '+ Horário / Alarme' acima!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.todayScheduledRoutines, key = { "sched_${it.id}" }) { routine ->
                val isDone = (completedMap[routine.id] ?: 0) >= routine.targetCountPerDay
                val isFocusedInPomodoro = uiState.pomodoroState.linkedRoutineId == routine.id
                ScheduledRoutineCard(
                    routine = routine,
                    isDoneToday = isDone,
                    isFocusedInPomodoro = isFocusedInPomodoro,
                    snoozeMinutes = uiState.settings.defaultSnoozeMinutes,
                    onCheckIn = { onCheckInRoutine(routine) },
                    onSnooze = { onGentleReschedule(routine, uiState.settings.defaultSnoozeMinutes) },
                    onFocusPomodoro = { onLinkRoutineToPomodoro(routine, true) },
                    onTestAlert = { onTestAlert(routine) },
                    onEdit = { onEditRoutine(routine) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun HeroAmbientPixelEffect(
    effect: AmbientSceneEffect,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "hero_ambient")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )

    Canvas(modifier = modifier) {
        val snap = 3.dp.toPx().coerceAtLeast(2f)
        when (effect) {
            AmbientSceneEffect.RAIN_DROPS -> {
                for (i in 0 until 18) {
                    val x = (((i * 0.058f) + phase * 0.18f) % 1f) * size.width
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

@Composable
private fun IntervalRoutineCard(
    routine: RoutineEntity,
    doneCount: Int,
    isFocusedInPomodoro: Boolean,
    onCheckIn: () -> Unit,
    onSnooze15: () -> Unit,
    onFocusPomodoro: () -> Unit,
    onTestNotification: () -> Unit,
    onEdit: () -> Unit
) {
    val palette = LocalPixelPalette.current
    val progress = (doneCount.toFloat() / routine.targetCountPerDay.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    PixelCard(
        containerColor = palette.surfaceElevated,
        borderColor = if (isFocusedInPomodoro) palette.alarmCoral else palette.pixelBorder,
        modifier = Modifier.testTag("interval_card_${routine.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(palette.waterCyan.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PixelSpriteIcon(
                        spriteType = routine.sprite,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PixelBadge(
                            text = routine.formattedScheduleSummary(),
                            containerColor = palette.waterCyan,
                            contentColor = palette.onPrimary
                        )
                        if (isFocusedInPomodoro) {
                            PixelBadge(
                                text = "🍅 EM FOCO",
                                containerColor = palette.alarmCoral,
                                contentColor = palette.onPrimary
                            )
                        } else if (routine.nextTriggerMillis > System.currentTimeMillis()) {
                            PixelBadge(
                                text = "PRÓX: ${DateUtils.formatTime(routine.nextTriggerMillis)}",
                                containerColor = palette.surface,
                                contentColor = palette.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 2
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_routine_${routine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ${routine.title}",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOJE: $doneCount / ${routine.targetCountPerDay} CONFIRMAÇÕES",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Text(
                    text = if (doneCount >= routine.targetCountPerDay) "META COMPLETA! ✨" else "TOQUE EM OK QUANDO FIZER",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (doneCount >= routine.targetCountPerDay) palette.wellnessGreen else palette.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            PixelBlockProgressBar(
                progress = progress,
                totalBlocks = routine.targetCountPerDay.coerceIn(4, 12),
                activeColor = palette.waterCyan
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelButton(
                    text = "✓ OK (+1)",
                    onClick = onCheckIn,
                    containerColor = palette.wellnessGreen,
                    contentColor = palette.onPrimary,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("quick_ok_button_${routine.id}")
                )
                PixelButton(
                    text = "🍅 FOCAR",
                    onClick = onFocusPomodoro,
                    containerColor = if (isFocusedInPomodoro) palette.alarmCoral else palette.surface,
                    contentColor = if (isFocusedInPomodoro) palette.onPrimary else palette.textPrimary,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("focus_pomodoro_interval_${routine.id}")
                )
                PixelButton(
                    text = "ADIAR",
                    onClick = onSnooze15,
                    containerColor = palette.surface,
                    contentColor = palette.textPrimary,
                    modifier = Modifier.weight(0.75f)
                )
                IconButton(
                    onClick = onTestNotification,
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .background(palette.secondary.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                        .testTag("test_notif_button_${routine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Testar notificação externa com OK",
                        tint = palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledRoutineCard(
    routine: RoutineEntity,
    isDoneToday: Boolean,
    isFocusedInPomodoro: Boolean,
    snoozeMinutes: Int,
    onCheckIn: () -> Unit,
    onSnooze: () -> Unit,
    onFocusPomodoro: () -> Unit,
    onTestAlert: () -> Unit,
    onEdit: () -> Unit
) {
    val palette = LocalPixelPalette.current
    val isAlarm = routine.alertMode == AlertMode.PERSISTENT_ALARM

    PixelCard(
        containerColor = if (isDoneToday) {
            palette.wellnessGreen.copy(alpha = 0.14f)
        } else {
            palette.surfaceElevated
        },
        borderColor = if (isFocusedInPomodoro) palette.alarmCoral else palette.pixelBorder,
        modifier = Modifier.testTag("scheduled_card_${routine.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                        .background(
                            if (isAlarm) palette.alarmCoral else palette.primary,
                            RoundedCornerShape(6.dp)
                        )
                        .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = routine.formattedScheduleSummary(),
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelBadge(
                            text = if (isAlarm) "🔔 ALARME VIBRA & TOCA" else "📩 NOTIFICAÇÃO",
                            containerColor = if (isAlarm) palette.alarmCoral.copy(alpha = 0.2f) else palette.secondary.copy(alpha = 0.25f),
                            contentColor = palette.textPrimary
                        )
                        if (isFocusedInPomodoro) {
                            PixelBadge(
                                text = "🍅 EM FOCO",
                                containerColor = palette.alarmCoral,
                                contentColor = palette.onPrimary
                            )
                        } else if (routine.syncToCalendar) {
                            PixelBadge(
                                text = "📅 AGENDA",
                                containerColor = palette.surface,
                                contentColor = palette.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 2
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ${routine.title}",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelButton(
                    text = if (isDoneToday) "✓ FEITA (+1)" else "✓ CONCLUIR",
                    onClick = onCheckIn,
                    containerColor = if (isDoneToday) palette.surface else palette.wellnessGreen,
                    contentColor = if (isDoneToday) palette.textPrimary else palette.onPrimary,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("complete_scheduled_button_${routine.id}")
                )
                PixelButton(
                    text = "🍅 FOCAR",
                    onClick = onFocusPomodoro,
                    containerColor = if (isFocusedInPomodoro) palette.alarmCoral else palette.surface,
                    contentColor = if (isFocusedInPomodoro) palette.onPrimary else palette.textPrimary,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("focus_pomodoro_scheduled_${routine.id}")
                )
                PixelButton(
                    text = "+${snoozeMinutes}M",
                    onClick = onSnooze,
                    containerColor = palette.snoozeAmber.copy(alpha = 0.35f),
                    contentColor = palette.textPrimary,
                    modifier = Modifier.weight(0.65f)
                )
                IconButton(
                    onClick = onTestAlert,
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .background(
                            if (isAlarm) palette.alarmCoral.copy(alpha = 0.25f) else palette.secondary.copy(alpha = 0.25f),
                            RoundedCornerShape(6.dp)
                        )
                        .testTag("test_scheduled_alert_${routine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = if (isAlarm) "Testar alarme sonoro agora" else "Testar notificação agora",
                        tint = palette.textPrimary
                    )
                }
            }
        }
    }
}
