package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.ui.RoutineUiState
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelBlockProgressBar
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.theme.LocalPixelPalette

@Composable
fun WeeklyReportScreen(
    uiState: RoutineUiState,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val weekly = uiState.weeklyReport
    val monthly = uiState.monthlyReport
    val maxDayCount = weekly.days.maxOfOrNull { it.completedCount + it.snoozedCount }?.coerceAtLeast(5) ?: 5
    val maxMonthWeekCount = monthly.weeksOfMonth.maxOfOrNull { it.completedCount + it.snoozedCount }?.coerceAtLeast(10) ?: 10

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("weekly_report_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // PARTE 1: RELATÓRIO SEMANAL (ÚLTIMOS 7 DIAS)
        // =====================================================================
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelSpriteIcon(
                            spriteType = PixelSpriteType.SPARKLE_OK,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RELATÓRIO SEMANAL DE BEM-ESTAR",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Text(
                                text = "Seu ritmo nos últimos 7 dias sem cobranças",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            label = "CONCLUÍDAS (7D)",
                            value = "${weekly.totalCompletedWeek}",
                            badgeColor = palette.wellnessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "OK NOTIFICAÇÃO",
                            value = "${weekly.quickOkCountWeek}",
                            badgeColor = palette.waterCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "MELHOR PERÍODO",
                            value = weekly.bestPeriodOfDay.substringBefore(" "),
                            badgeColor = palette.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. 7-Day Pixel Block Bar Chart
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONSTÂNCIA NOS ÚLTIMOS 7 DIAS",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Barras verdes indicam rotinas concluídas; âmbar indica reagendamentos suaves.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekly.days.forEach { day ->
                            val completedRatio = (day.completedCount.toFloat() / maxDayCount.toFloat()).coerceIn(0f, 1f)
                            val snoozedRatio = (day.snoozedCount.toFloat() / maxDayCount.toFloat()).coerceIn(0f, 1f - completedRatio)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${day.completedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(104.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(palette.surface)
                                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        if (snoozedRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height((100 * snoozedRatio).dp.coerceAtLeast(6.dp))
                                                    .background(palette.snoozeAmber)
                                            )
                                        }
                                        if (completedRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height((100 * completedRatio).dp.coerceAtLeast(8.dp))
                                                    .background(palette.wellnessGreen)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.dayShortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Weekly Category Balance Breakdown
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EQUILÍBRIO SEMANAL POR ÁREA",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        weekly.categoryBreakdown.forEach { item ->
                            val ratio = (item.completedCount.toFloat() / item.targetTotal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PixelSpriteIcon(
                                            spriteType = item.category.defaultSprite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.category.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "${item.completedCount} check-ins",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                PixelBlockProgressBar(
                                    progress = ratio,
                                    totalBlocks = 8,
                                    activeColor = palette.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // PARTE 2: RELATÓRIO MENSAL (VISÃO COMPLETA DO MÊS ATUAL)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            PixelCard(
                containerColor = palette.surfaceElevated,
                modifier = Modifier.testTag("monthly_report_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            PixelSpriteIcon(
                                spriteType = PixelSpriteType.CALENDAR_GRID,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "RELATÓRIO MENSAL",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.secondary
                                )
                                Text(
                                    text = monthly.monthTitle.ifBlank { "Mês Atual" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        val activeDaysPct = ((monthly.activeDaysInMonth.toFloat() / monthly.totalDaysInMonth.coerceAtLeast(1)) * 100).toInt()
                        PixelBadge(
                            text = "$activeDaysPct% DIAS ATIVOS",
                            containerColor = palette.secondary,
                            contentColor = palette.onSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            label = "NO MÊS",
                            value = "${monthly.totalCompletedMonth}",
                            badgeColor = palette.wellnessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "DIAS ATIVOS",
                            value = "${monthly.activeDaysInMonth}/${monthly.totalDaysInMonth}",
                            badgeColor = palette.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "ALARMES OK",
                            value = "${monthly.alarmDismissedCountMonth}",
                            badgeColor = palette.alarmCoral,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "EVOLUÇÃO POR SEMANA DO MÊS",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(136.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        monthly.weeksOfMonth.forEach { weekBar ->
                            val completedRatio = (weekBar.completedCount.toFloat() / maxMonthWeekCount.toFloat()).coerceIn(0f, 1f)
                            val snoozedRatio = (weekBar.snoozedCount.toFloat() / maxMonthWeekCount.toFloat()).coerceIn(0f, 1f - completedRatio)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${weekBar.completedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(88.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(palette.surface)
                                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        if (snoozedRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height((84 * snoozedRatio).dp.coerceAtLeast(6.dp))
                                                    .background(palette.snoozeAmber)
                                            )
                                        }
                                        if (completedRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height((84 * completedRatio).dp.coerceAtLeast(8.dp))
                                                    .background(palette.secondary)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = weekBar.weekLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = weekBar.rangeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "MAPA DE CONSTÂNCIA DO MÊS (HEATMAP PIXEL)",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cada bloco representa um dia do mês (1 a ${monthly.totalDaysInMonth}). Quanto mais vibrante, mais check-ins!",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val rowsOfDays = monthly.dailyCompletedCounts.chunked(7)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowsOfDays.forEach { weekChunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 0 until 7) {
                                    val entry = weekChunk.getOrNull(i)
                                    if (entry != null) {
                                        val (dayNum, count) = entry
                                        val cellColor = when {
                                            count >= 5 -> palette.wellnessGreen
                                            count >= 3 -> palette.wellnessGreen.copy(alpha = 0.72f)
                                            count >= 1 -> palette.wellnessGreen.copy(alpha = 0.42f)
                                            else -> palette.surface
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(cellColor)
                                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (count >= 3) palette.onPrimary else palette.textPrimary
                                            )
                                        }
                                    } else {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "ACUMULADO DO MÊS POR CATEGORIA",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        monthly.categoryBreakdownMonth.forEach { catItem ->
                            val ratio = (catItem.completedCount.toFloat() / catItem.targetTotal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PixelSpriteIcon(
                                            spriteType = catItem.category.defaultSprite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catItem.category.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "${catItem.completedCount} no mês",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                PixelBlockProgressBar(
                                    progress = ratio,
                                    totalBlocks = 10,
                                    activeColor = palette.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // PARTE 3: HISTÓRICO DE REGISTROS RECENTES
        // =====================================================================
        item {
            Text(
                text = "REGISTROS RECENTES DA ROTINA",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textSecondary
            )
        }

        val recentLogs = uiState.checkInLogs.take(15)
        if (recentLogs.isEmpty()) {
            item {
                PixelCard(containerColor = palette.surface) {
                    Text(
                        text = "Nenhum registro ainda. Assim que você criar suas rotinas e der 'OK' nas notificações ou concluir tarefas no app, o histórico aparecerá aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentLogs, key = { "log_${it.id}" }) { log ->
                PixelCard(containerColor = palette.surfaceElevated) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.routineTitleSnapshot,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${log.source.label} • ${DateUtils.formatDayShortPt(log.dateKey)} às ${DateUtils.formatTime(log.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        PixelBadge(
                            text = log.status.label.uppercase(),
                            containerColor = if (log.status == CheckInStatus.COMPLETED) palette.wellnessGreen else palette.snoozeAmber,
                            contentColor = palette.textPrimary
                        )
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
private fun StatMiniCard(
    label: String,
    value: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surface)
            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = badgeColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
