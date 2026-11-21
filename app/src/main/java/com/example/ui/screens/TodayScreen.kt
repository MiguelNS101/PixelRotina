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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.JournalContextTag
import com.example.data.RoutineEntity
import com.example.ui.RoutineUiState
import com.example.ui.StreakMedalSummary
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelBlockProgressBar
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelEnergyIcon
import com.example.ui.components.PixelMedalIcon
import com.example.ui.components.PixelMedalTier
import com.example.ui.components.PixelMoodIcon
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.components.PixelStreakFlameIcon
import com.example.ui.components.PomodoroEmbeddedCard
import com.example.ui.components.PomodoroPhase
import com.example.ui.components.energyLabelPt
import com.example.ui.components.moodLabelPt
import com.example.ui.theme.LocalPixelPalette
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import java.util.Locale
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
    onSaveQuickMoodEnergy: (moodScore: Int, energyScore: Int, contextTags: List<String>) -> Unit = { _, _, _ -> },
    onSavePostRoutineMoodEnergy: (routine: RoutineEntity, moodScore: Int, energyScore: Int) -> Unit = { _, _, _ -> },
    onDismissPostRoutineMoodPrompt: () -> Unit = {},
    onOpenJournalTab: () -> Unit = {},
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
    val variationOffset = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
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
                            .height(228.dp)
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
                                .background(Color.Black.copy(alpha = 0.28f))
                        )

                        PixelBadge(
                            text = activeArt.title,
                            containerColor = Color.Black.copy(alpha = 0.62f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .testTag("hero_scene_badge")
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
                        Spacer(modifier = Modifier.height(12.dp))
                        DailyStreakMedalSection(
                            streakSummary = uiState.streakMedalSummary
                        )
                    }
                }
            }
        }

        // 1.25 Quick 2-Tap Mood & Energy Check-in Card (with live daily average)
        item {
            QuickMoodEnergyCheckInCard(
                todayLatestMood = uiState.todayLatestMood,
                todayLatestEnergy = uiState.todayLatestEnergy,
                todayAvgMood = uiState.todayAvgMood,
                todayAvgEnergy = uiState.todayAvgEnergy,
                todayCheckInCount = uiState.todayMoodCheckInCount,
                onSaveQuickCheckIn = onSaveQuickMoodEnergy,
                onOpenJournalTab = onOpenJournalTab
            )
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

    uiState.pendingMoodPromptRoutine?.let { completedRoutine ->
        PostRoutineMoodEnergyDialog(
            routine = completedRoutine,
            initialMood = uiState.todayLatestMood ?: 4,
            initialEnergy = uiState.todayLatestEnergy ?: 4,
            onSave = { mood, energy ->
                onSavePostRoutineMoodEnergy(completedRoutine, mood, energy)
            },
            onSkip = onDismissPostRoutineMoodPrompt
        )
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
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(palette.surface, RoundedCornerShape(4.dp))
                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelSpriteIcon(
                                spriteType = routine.sprite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

@Composable
private fun DailyStreakMedalSection(
    streakSummary: StreakMedalSummary,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val currentMedal = streakSummary.todayMedal
    val percent = streakSummary.todayCompletionPercent
    val hasActiveRoutines = streakSummary.hasActiveRoutinesToday

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surface)
            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
            .padding(12.dp)
            .testTag("daily_streak_medal_section")
    ) {
        // Top Row: Streak Flame + Streak Counter + Current Earned Medal Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (streakSummary.currentStreakDays > 0) {
                                Color(0xFFF97316).copy(alpha = 0.18f)
                            } else {
                                palette.surfaceElevated
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PixelStreakFlameIcon(
                        isActive = streakSummary.currentStreakDays > 0,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    val streakLabel = if (streakSummary.currentStreakDays == 1) {
                        "SEQUÊNCIA: 1 DIA"
                    } else {
                        "SEQUÊNCIA: ${streakSummary.currentStreakDays} DIAS"
                    }
                    Text(
                        text = streakLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("streak_counter_label")
                    )
                    Text(
                        text = if (streakSummary.goldStreakDays > 0) {
                            "Ouro 100%: ${streakSummary.goldStreakDays}d • Recorde: ${streakSummary.bestStreakDays}d"
                        } else {
                            "Recorde de consistência: ${streakSummary.bestStreakDays} dias"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            PixelBadge(
                text = currentMedal.shortLabelPt,
                containerColor = if (currentMedal != PixelMedalTier.NONE) {
                    currentMedal.discMainColor
                } else {
                    palette.surfaceElevated
                },
                contentColor = if (currentMedal != PixelMedalTier.NONE) {
                    Color(0xFF1A1B26)
                } else {
                    palette.textSecondary
                },
                modifier = Modifier.testTag("today_current_medal_badge")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 Pixel Art Medal Slots: Bronze (50%), Prata (75%), Ouro (100%)
        val medalTiers = listOf(
            PixelMedalTier.BRONZE,
            PixelMedalTier.SILVER,
            PixelMedalTier.GOLD
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            medalTiers.forEach { tier ->
                val isUnlocked = hasActiveRoutines && percent >= tier.thresholdPercent
                val isCurrentTopTier = currentMedal == tier
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isUnlocked) {
                                tier.discMainColor.copy(alpha = 0.16f)
                            } else {
                                palette.surfaceElevated.copy(alpha = 0.65f)
                            }
                        )
                        .border(
                            width = if (isCurrentTopTier) 2.5.dp else 1.5.dp,
                            color = if (isUnlocked) tier.discMainColor else palette.pixelBorder.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                        .testTag("medal_slot_${tier.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PixelMedalIcon(
                        tier = tier,
                        isUnlocked = isUnlocked,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (tier) {
                            PixelMedalTier.BRONZE -> "BRONZE"
                            PixelMedalTier.SILVER -> "PRATA"
                            PixelMedalTier.GOLD -> "OURO"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUnlocked) palette.textPrimary else palette.textSecondary
                    )
                    Text(
                        text = if (isUnlocked) "✓ ${tier.thresholdPercent}%" else "${tier.thresholdPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) palette.wellnessGreen else palette.textSecondary
                    )
                }
            }
        }

        // Celebratory banner when a medal is earned today (especially Gold 100% when all tasks are completed!)
        if (currentMedal != PixelMedalTier.NONE) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(currentMedal.discMainColor.copy(alpha = 0.2f))
                    .border(2.dp, currentMedal.discMainColor, RoundedCornerShape(6.dp))
                    .padding(10.dp)
                    .testTag("earned_medal_celebration_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixelMedalIcon(
                    tier = currentMedal,
                    isUnlocked = true,
                    modifier = Modifier.size(38.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentMedal.titlePt.uppercase()} CONQUISTADA!",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = currentMedal.encouragementPt,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        } else if (hasActiveRoutines) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = PixelMedalTier.NONE.encouragementPt,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickMoodEnergyCheckInCard(
    todayLatestMood: Int?,
    todayLatestEnergy: Int?,
    todayAvgMood: Float?,
    todayAvgEnergy: Float?,
    todayCheckInCount: Int,
    onSaveQuickCheckIn: (moodScore: Int, energyScore: Int, contextTags: List<String>) -> Unit,
    onOpenJournalTab: () -> Unit
) {
    val palette = LocalPixelPalette.current
    val hasRecordedToday = todayLatestMood != null && todayLatestEnergy != null
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var selectedMood by rememberSaveable(todayLatestMood) { mutableIntStateOf(todayLatestMood ?: 4) }
    var selectedEnergy by rememberSaveable(todayLatestEnergy) { mutableIntStateOf(todayLatestEnergy ?: 4) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }

    PixelCard(
        containerColor = palette.surfaceElevated,
        modifier = Modifier.testTag("today_quick_mood_energy_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHECK-IN DE ENERGIA & HUMOR",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.primary
                )
                PixelBadge(
                    text = if (hasRecordedToday) {
                        if (todayCheckInCount > 1) "${todayCheckInCount}x HOJE" else "✓ REGISTRADO"
                    } else {
                        "2 TOQUES"
                    },
                    containerColor = if (hasRecordedToday) palette.wellnessGreen else palette.secondary,
                    contentColor = if (hasRecordedToday) palette.onPrimary else palette.onSecondary
                )
            }

            if (hasRecordedToday && !isEditing) {
                val displayMoodAvg = todayAvgMood ?: todayLatestMood!!.toFloat()
                val displayEnergyAvg = todayAvgEnergy ?: todayLatestEnergy!!.toFloat()
                val avgMoodFormatted = String.format(Locale.US, "%.1f", displayMoodAvg)
                val avgEnergyFormatted = String.format(Locale.US, "%.1f", displayEnergyAvg)

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixelMoodIcon(
                            level = displayMoodAvg.roundToInt().coerceIn(1, 5),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (todayCheckInCount > 1) "MÉDIA HUMOR" else "HUMOR HOJE",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = "$avgMoodFormatted/5 • ${moodLabelPt(displayMoodAvg.roundToInt().coerceIn(1, 5))}",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixelEnergyIcon(
                            level = displayEnergyAvg.roundToInt().coerceIn(1, 5),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (todayCheckInCount > 1) "MÉDIA ENERGIA" else "ENERGIA HOJE",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = "$avgEnergyFormatted/5 • ${energyLabelPt(displayEnergyAvg.roundToInt().coerceIn(1, 5))}",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "+ REGISTRAR",
                        onClick = { isEditing = true },
                        containerColor = palette.surface,
                        contentColor = palette.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_mood_update_button")
                    )
                    PixelButton(
                        text = "ABRIR DIÁRIO",
                        onClick = onOpenJournalTab,
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_open_journal_button")
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HUMOR AGORA",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = moodLabelPt(selectedMood),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { level ->
                        val chosen = selectedMood == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (chosen) palette.primary.copy(alpha = 0.2f) else palette.surface
                                )
                                .border(
                                    width = if (chosen) 2.5.dp else 1.5.dp,
                                    color = if (chosen) palette.primary else palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedMood = level }
                                .testTag("today_quick_mood_$level"),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelMoodIcon(
                                level = level,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BATERIA / ENERGIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "${energyLabelPt(selectedEnergy)} ($selectedEnergy/5)",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.wellnessGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { level ->
                        val chosen = selectedEnergy == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (chosen) palette.wellnessGreen.copy(alpha = 0.2f) else palette.surface
                                )
                                .border(
                                    width = if (chosen) 2.5.dp else 1.5.dp,
                                    color = if (chosen) palette.wellnessGreen else palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedEnergy = level }
                                .testTag("today_quick_energy_$level"),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelEnergyIcon(
                                level = level,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JournalContextTag.entries.forEach { tag ->
                        val isChosen = tag.code in selectedTags
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isChosen) palette.secondary else palette.surface)
                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(5.dp))
                                .clickable {
                                    selectedTags = if (isChosen) {
                                        selectedTags - tag.code
                                    } else {
                                        selectedTags + tag.code
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("today_quick_tag_${tag.code.lowercase()}")
                        ) {
                            Text(
                                text = if (isChosen) "✓ ${tag.labelPt}" else tag.labelPt,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isChosen) palette.onSecondary else palette.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "SALVAR CHECK-IN",
                        onClick = {
                            onSaveQuickCheckIn(selectedMood, selectedEnergy, selectedTags.toList())
                            selectedTags = emptySet()
                            isEditing = false
                        },
                        containerColor = palette.wellnessGreen,
                        contentColor = palette.onPrimary,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("today_save_quick_checkin_button")
                    )
                    if (isEditing) {
                        PixelButton(
                            text = "FECHAR",
                            onClick = { isEditing = false },
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary,
                            modifier = Modifier.weight(0.7f)
                        )
                    } else {
                        PixelButton(
                            text = "DIÁRIO",
                            onClick = onOpenJournalTab,
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary,
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("today_open_journal_secondary_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostRoutineMoodEnergyDialog(
    routine: RoutineEntity,
    initialMood: Int,
    initialEnergy: Int,
    onSave: (moodScore: Int, energyScore: Int) -> Unit,
    onSkip: () -> Unit
) {
    val palette = LocalPixelPalette.current
    var selectedMood by remember(routine.id) { mutableIntStateOf(initialMood.coerceIn(1, 5)) }
    var selectedEnergy by remember(routine.id) { mutableIntStateOf(initialEnergy.coerceIn(1, 5)) }

    Dialog(onDismissRequest = onSkip) {
        PixelCard(
            containerColor = palette.surfaceElevated,
            borderColor = palette.primary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_routine_mood_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(palette.wellnessGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelSpriteIcon(
                            spriteType = routine.sprite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✓ ${routine.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Como você está agora? (Opcional — atualiza sua média diária)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HUMOR AGORA",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = moodLabelPt(selectedMood),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { level ->
                        val chosen = selectedMood == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (chosen) palette.primary.copy(alpha = 0.22f) else palette.surface)
                                .border(
                                    width = if (chosen) 2.5.dp else 1.5.dp,
                                    color = if (chosen) palette.primary else palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedMood = level }
                                .testTag("post_routine_mood_$level"),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelMoodIcon(level = level, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ENERGIA AGORA",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "${energyLabelPt(selectedEnergy)} ($selectedEnergy/5)",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.wellnessGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { level ->
                        val chosen = selectedEnergy == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (chosen) palette.wellnessGreen.copy(alpha = 0.22f) else palette.surface)
                                .border(
                                    width = if (chosen) 2.5.dp else 1.5.dp,
                                    color = if (chosen) palette.wellnessGreen else palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedEnergy = level }
                                .testTag("post_routine_energy_$level"),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelEnergyIcon(level = level, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "PULAR",
                        onClick = onSkip,
                        containerColor = palette.surface,
                        contentColor = palette.textPrimary,
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("post_routine_mood_skip")
                    )
                    PixelButton(
                        text = "✨ SALVAR NA MÉDIA",
                        onClick = { onSave(selectedMood, selectedEnergy) },
                        containerColor = palette.wellnessGreen,
                        contentColor = palette.onPrimary,
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("post_routine_mood_save")
                    )
                }
            }
        }
    }
}
