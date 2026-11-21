package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.DayJournalSummary
import com.example.data.DeviceCalendarEvent
import com.example.data.JournalCorrelationCalculator
import com.example.data.MonthCalendarCell
import com.example.data.RoutineEntity
import com.example.ui.RoutineUiState
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelEnergyIcon
import com.example.ui.components.PixelMoodIcon
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.theme.LocalPixelPalette
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeeklyCalendarScreen(
    uiState: RoutineUiState,
    hasCalendarPermission: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onSelectDateKey: (String) -> Unit,
    onChangeMonth: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onToggleRoutineEnabled: (RoutineEntity, Boolean) -> Unit,
    onSyncRoutineToCalendar: (RoutineEntity) -> Unit,
    onEditRoutine: (RoutineEntity) -> Unit,
    onRefreshCalendar: () -> Unit,
    onOpenJournalForDate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val calState = uiState.calendarMonthState
    val monthCells = remember(calState.year, calState.monthZeroBased) {
        DateUtils.buildMonthCalendarCells(calState.year, calState.monthZeroBased)
    }
    val weekHeaders = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    val completedByDate = uiState.completedCountByDateKey
    val eventsByDate = remember(uiState.calendarEvents) {
        uiState.calendarEvents.groupBy { it.dateKey }
    }
    val journalSummaryByDate = remember(uiState.journalEntries) {
        JournalCorrelationCalculator.computeDaySummaryByDate(uiState.journalEntries)
    }

    val selectedDateKey = calState.selectedDateKey
    val selectedIsoDay = uiState.selectedIsoDay
    val selectedDayRoutines = uiState.routines.filter { selectedIsoDay in it.activeDays }
    val selectedDayEvents = eventsByDate[selectedDateKey].orEmpty()
    val selectedDayCompletedCount = completedByDate[selectedDateKey] ?: 0
    val selectedDayJournal = journalSummaryByDate[selectedDateKey]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("weekly_calendar_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Monthly Calendar Card (Calendário dos Meses)
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month navigation header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelSpriteIcon(
                                spriteType = PixelSpriteType.CALENDAR_GRID,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DateUtils.formatMonthYearPt(calState.year, calState.monthZeroBased).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface)
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onJumpToToday)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("calendar_today_button")
                            ) {
                                Text(
                                    text = "HOJE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                            }
                            IconButton(
                                onClick = { onChangeMonth(-1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("calendar_prev_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Mês anterior",
                                    tint = palette.textPrimary
                                )
                            }
                            IconButton(
                                onClick = { onChangeMonth(1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("calendar_next_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Próximo mês",
                                    tint = palette.textPrimary
                                )
                            }
                            IconButton(
                                onClick = onRefreshCalendar,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("refresh_calendar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Atualizar calendário",
                                    tint = palette.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weekday headers (Seg..Dom)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekHeaders.forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6 rows x 7 columns Monthly Grid
                    val weeks = monthCells.chunked(7)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        weeks.forEach { weekRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekRow.forEach { cell ->
                                    val isSelected = cell.dateKey == selectedDateKey
                                    val doneCount = completedByDate[cell.dateKey] ?: 0
                                    val hasEvents = !eventsByDate[cell.dateKey].isNullOrEmpty()
                                    val activeRoutinesCount = uiState.routines.count {
                                        it.isEnabled && cell.isoDayOfWeek in it.activeDays
                                    }
                                    val cellJournal = journalSummaryByDate[cell.dateKey]

                                    MonthDayCellView(
                                        cell = cell,
                                        isSelected = isSelected,
                                        doneCount = doneCount,
                                        hasCalendarEvents = hasEvents,
                                        hasActiveRoutines = activeRoutinesCount > 0,
                                        dayJournalSummary = cellJournal,
                                        onClick = { onSelectDateKey(cell.dateKey) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDotItem(color = palette.wellnessGreen, label = "Check-in")
                        LegendDotItem(color = palette.accent, label = "Agenda")
                        LegendDotItem(color = palette.snoozeAmber, label = "Humor", isSquare = true)
                    }
                }
            }
        }

        // 2. Selected Day Summary Banner
        item {
            PixelCard(containerColor = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateUtils.formatFullDatePt(selectedDateKey).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primary
                        )
                        Text(
                            text = "${selectedDayRoutines.size} rotinas • $selectedDayCompletedCount concluídas neste dia",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    if (selectedDateKey == uiState.todayDateKey) {
                        PixelBadge(
                            text = "HOJE",
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        )
                    }
                }
            }
        }

        // 2.5 Selected Day Mood, Energy & Journal Summary Card
        item {
            SelectedDayJournalSummaryCard(
                dateKey = selectedDateKey,
                summary = selectedDayJournal,
                onOpenJournal = { onOpenJournalForDate(selectedDateKey) }
            )
        }

        // 3. Native Android Calendar Integration Section for Selected Day
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AGENDA DO ANDROID NESTE DIA",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        PixelBadge(
                            text = if (hasCalendarPermission) "${selectedDayEvents.size} EVENTOS" else "DESCONECTADO",
                            containerColor = if (hasCalendarPermission) palette.wellnessGreen else palette.snoozeAmber,
                            contentColor = palette.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (!hasCalendarPermission) {
                        Text(
                            text = "Conecte ao Calendário do Android para ver seus compromissos do mês na grade acima e exportar suas rotinas direto para o aparelho.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PixelButton(
                            text = "📅 CONECTAR CALENDÁRIO DO ANDROID",
                            onClick = onRequestCalendarPermission,
                            containerColor = palette.secondary,
                            contentColor = palette.onSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_android_calendar_button")
                        )
                    } else if (selectedDayEvents.isEmpty()) {
                        Text(
                            text = "Nenhum evento agendado no Calendário do Android para ${DateUtils.formatFullDatePt(selectedDateKey)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedDayEvents.forEach { ev ->
                                CalendarEventRow(event = ev)
                            }
                        }
                    }
                }
            }
        }

        // 4. Routines for the Selected Date's Day of Week
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROTINAS PROGRAMADAS (${selectedDayRoutines.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
            }
        }

        items(selectedDayRoutines, key = { "month_routine_${it.id}" }) { routine ->
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelSpriteIcon(
                            spriteType = routine.sprite,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${routine.formattedScheduleSummary()} • ${routine.formattedDaysSummary()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = routine.isEnabled,
                            onCheckedChange = { onToggleRoutineEnabled(routine, it) },
                            modifier = Modifier.testTag("toggle_routine_${routine.id}")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelButton(
                            text = if (routine.syncToCalendar) "📅 ATUALIZAR NA AGENDA" else "📅 ENVIAR P/ CALENDÁRIO",
                            onClick = { onSyncRoutineToCalendar(routine) },
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_calendar_routine_${routine.id}")
                        )
                        IconButton(
                            onClick = { onEditRoutine(routine) },
                            modifier = Modifier
                                .size(48.dp)
                                .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .background(palette.surface, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar rotina",
                                tint = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun SelectedDayJournalSummaryCard(
    dateKey: String,
    summary: DayJournalSummary?,
    onOpenJournal: () -> Unit
) {
    val palette = LocalPixelPalette.current
    PixelCard(
        containerColor = palette.surfaceElevated,
        modifier = Modifier.testTag("calendar_selected_day_journal_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIÁRIO & HUMOR DESTE DIA",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.primary
                )
                PixelBadge(
                    text = "${summary?.entryCount ?: 0} REGISTROS",
                    containerColor = palette.secondary,
                    contentColor = palette.onSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary != null && (summary.avgMood != null || summary.avgEnergy != null || summary.entryCount > 0)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val moodVal = summary.avgMood
                        if (moodVal != null) {
                            PixelMoodIcon(
                                level = moodVal.roundToInt().coerceIn(1, 5),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "HUMOR MÉDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = moodVal?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "Sem nota",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val energyVal = summary.avgEnergy
                        if (energyVal != null) {
                            PixelEnergyIcon(
                                level = energyVal.roundToInt().coerceIn(1, 5),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ENERGIA MÉDIA",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = energyVal?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "Sem nota",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Nenhum check-in de humor ou nota salva em ${DateUtils.formatFullDatePt(dateKey)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            PixelButton(
                text = "📖 VER NO DIÁRIO",
                onClick = onOpenJournal,
                containerColor = palette.primary,
                contentColor = palette.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_open_journal_button")
            )
        }
    }
}

@Composable
private fun MonthDayCellView(
    cell: MonthCalendarCell,
    isSelected: Boolean,
    doneCount: Int,
    hasCalendarEvents: Boolean,
    hasActiveRoutines: Boolean,
    dayJournalSummary: DayJournalSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val bgColor = when {
        isSelected -> palette.primary
        cell.isToday -> palette.secondary.copy(alpha = 0.25f)
        cell.isCurrentMonth -> palette.surface
        else -> palette.surface.copy(alpha = 0.4f)
    }
    val borderColor = when {
        isSelected -> palette.primary
        cell.isToday -> palette.accent
        else -> palette.pixelBorder.copy(alpha = if (cell.isCurrentMonth) 0.75f else 0.3f)
    }
    val textColor = when {
        isSelected -> palette.onPrimary
        cell.isCurrentMonth -> palette.textPrimary
        else -> palette.textSecondary.copy(alpha = 0.45f)
    }

    val moodOrEnergy = dayJournalSummary?.avgMood ?: dayJournalSummary?.avgEnergy
    val moodCornerColor = moodOrEnergy?.let { score ->
        when {
            score < 2.5f -> palette.alarmCoral
            score < 3.8f -> palette.snoozeAmber
            else -> palette.wellnessGreen
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .border(
                width = if (isSelected || cell.isToday) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onClick)
            .testTag("month_cell_${cell.dateKey}")
    ) {
        // Discrete pixel art corner indicator for daily mood/energy without overlapping routine/event dots
        if (moodCornerColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 3.dp)
                    .size(5.dp)
                    .background(moodCornerColor, RoundedCornerShape(1.dp))
                    .border(0.75.dp, palette.pixelBorder, RoundedCornerShape(1.dp))
                    .testTag("month_cell_mood_dot_${cell.dateKey}")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${cell.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (doneCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) palette.onPrimary else palette.wellnessGreen)
                    )
                }
                if (hasCalendarEvents) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) palette.onPrimary else palette.accent)
                    )
                }
                if (doneCount == 0 && !hasCalendarEvents && hasActiveRoutines && cell.isCurrentMonth) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) palette.onPrimary.copy(alpha = 0.7f)
                                else palette.secondary.copy(alpha = 0.7f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    isSquare: Boolean = false
) {
    val palette = LocalPixelPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(if (isSquare) RoundedCornerShape(1.5.dp) else CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun CalendarEventRow(event: DeviceCalendarEvent) {
    val palette = LocalPixelPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surface)
            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            val timeRange = if (event.isAllDay) {
                "Dia inteiro • ${event.calendarName}"
            } else {
                "${DateUtils.formatTime(event.startMillis)} – ${DateUtils.formatTime(event.endMillis)} • ${event.calendarName}"
            }
            Text(
                text = timeRange,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}
