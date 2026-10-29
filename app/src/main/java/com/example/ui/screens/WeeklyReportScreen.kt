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
import androidx.compose.ui.platform.testTag
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
    val report = uiState.weeklyReport
    val maxDayCount = report.days.maxOfOrNull { it.completedCount + it.snoozedCount }?.coerceAtLeast(5) ?: 5

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("weekly_report_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Weekly Overview Summary Card
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
                            value = "${report.totalCompletedWeek}",
                            badgeColor = palette.wellnessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "OK NA NOTIFICAÇÃO",
                            value = "${report.quickOkCountWeek}",
                            badgeColor = palette.waterCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "MELHOR PERÍODO",
                            value = report.bestPeriodOfDay.substringBefore(" "),
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
                        report.days.forEach { day ->
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

        // 3. Category Balance Breakdown
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EQUILÍBRIO POR ÁREA DE BEM-ESTAR",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        report.categoryBreakdown.forEach { item ->
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

        // 4. Recent Check-In History Log
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
                        text = "Assim que você der 'OK' nas notificações ou concluir tarefas no app, o histórico aparecerá aqui.",
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
    badgeColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surface)
            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = badgeColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}
