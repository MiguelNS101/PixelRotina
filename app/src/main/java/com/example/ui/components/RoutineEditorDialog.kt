package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AlertMode
import com.example.data.FrequencyType
import com.example.data.RoutineCategory
import com.example.data.RoutineEntity
import com.example.ui.theme.LocalPixelPalette
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineEditorDialog(
    initialRoutine: RoutineEntity,
    onDismiss: () -> Unit,
    onSave: (RoutineEntity) -> Unit,
    onDelete: ((RoutineEntity) -> Unit)? = null
) {
    val palette = LocalPixelPalette.current
    var title by remember(initialRoutine) { mutableStateOf(initialRoutine.title) }
    var description by remember(initialRoutine) { mutableStateOf(initialRoutine.description) }
    var category by remember(initialRoutine) { mutableStateOf(initialRoutine.category) }
    var sprite by remember(initialRoutine) { mutableStateOf(initialRoutine.sprite) }
    var alertMode by remember(initialRoutine) { mutableStateOf(initialRoutine.alertMode) }
    var frequencyType by remember(initialRoutine) { mutableStateOf(initialRoutine.frequencyType) }
    var hour by remember(initialRoutine) { mutableIntStateOf(initialRoutine.hour) }
    var minute by remember(initialRoutine) { mutableIntStateOf(initialRoutine.minute) }
    var intervalMinutes by remember(initialRoutine) { mutableIntStateOf(initialRoutine.intervalMinutes) }
    var windowStartHour by remember(initialRoutine) { mutableIntStateOf(initialRoutine.windowStartHour) }
    var windowEndHour by remember(initialRoutine) { mutableIntStateOf(initialRoutine.windowEndHour) }
    var targetCountPerDay by remember(initialRoutine) { mutableIntStateOf(initialRoutine.targetCountPerDay) }
    var selectedDays by remember(initialRoutine) { mutableStateOf(initialRoutine.activeDays) }
    var syncToCalendar by remember(initialRoutine) { mutableStateOf(initialRoutine.syncToCalendar) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(3.dp, palette.pixelBorder, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = palette.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRoutine.id == 0L) "NOVA ROTINA PIXEL" else "EDITAR ROTINA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.primary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_routine_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar editor",
                            tint = palette.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da tarefa ou hábito") },
                    placeholder = { Text("Ex: Beber água, Alongar, Tomar vitamina...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Nota ou lembrete gentil (opcional)") },
                    placeholder = { Text("Ex: Respire fundo e beba um copo cheio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_description_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "CATEGORIA & ÍCONE PIXEL",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoutineCategory.entries.forEach { item ->
                        val isSelected = category == item
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) palette.primary else palette.surfaceElevated
                                )
                                .border(
                                    width = 2.dp,
                                    color = palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    category = item
                                    sprite = item.defaultSprite
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PixelSpriteIcon(
                                    spriteType = item.defaultSprite,
                                    modifier = Modifier.size(18.dp),
                                    tintOverride = if (isSelected) palette.onPrimary else null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) palette.onPrimary else palette.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "COMO QUER SER LEMBRADO?",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertMode.entries.forEach { mode ->
                        val isSelected = alertMode == mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) palette.secondary.copy(alpha = 0.22f)
                                    else palette.surfaceElevated
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) palette.primary else palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { alertMode = mode }
                                .padding(12.dp)
                                .testTag("alert_mode_${mode.name.lowercase()}")
                        ) {
                            Column {
                                Text(
                                    text = if (isSelected) "▶ ${mode.title}" else mode.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FREQUÊNCIA DO LEMBRETE",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrequencyType.entries.forEach { freq ->
                        val isSelected = frequencyType == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) palette.primary else palette.surfaceElevated
                                )
                                .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    frequencyType = freq
                                    if (freq == FrequencyType.INTERVAL && targetCountPerDay < 2) {
                                        targetCountPerDay = 6
                                    }
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freq.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) palette.onPrimary else palette.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                if (frequencyType == FrequencyType.SPECIFIC_TIME) {
                    PixelCard(containerColor = palette.surface) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "HORÁRIO EXATO: ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hora (${hour}h)",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Slider(
                                value = hour.toFloat(),
                                onValueChange = { hour = it.toInt().coerceIn(0, 23) },
                                valueRange = 0f..23f,
                                steps = 22
                            )
                            Text(
                                text = "Minuto (${String.format(Locale.getDefault(), "%02d", minute)}m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0, 10, 15, 20, 30, 40, 45, 50).forEach { m ->
                                    val sel = minute == m
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (sel) palette.primary else palette.surfaceElevated)
                                            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
                                            .clickable { minute = m }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), ":%02d", m),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (sel) palette.onPrimary else palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    PixelCard(containerColor = palette.surface) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "REPETIR A CADA $intervalMinutes MINUTOS",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(30, 45, 60, 90, 120, 180).forEach { mins ->
                                    val sel = intervalMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (sel) palette.primary else palette.surfaceElevated)
                                            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
                                            .clickable { intervalMinutes = mins }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        val label = if (mins >= 60 && mins % 60 == 0) "${mins / 60}h" else "${mins}m"
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (sel) palette.onPrimary else palette.textPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Janela ativa: das ${String.format(Locale.getDefault(), "%02d:00", windowStartHour)} às ${String.format(Locale.getDefault(), "%02d:00", windowEndHour)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Slider(
                                value = windowStartHour.toFloat(),
                                onValueChange = {
                                    windowStartHour = it.toInt().coerceIn(5, 20)
                                    if (windowEndHour <= windowStartHour) {
                                        windowEndHour = (windowStartHour + 2).coerceAtMost(23)
                                    }
                                },
                                valueRange = 5f..20f
                            )
                            Text(
                                text = "Meta diária de check-ins: $targetCountPerDay vezes/dia",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Slider(
                                value = targetCountPerDay.toFloat(),
                                onValueChange = { targetCountPerDay = it.toInt().coerceIn(1, 15) },
                                valueRange = 1f..15f,
                                steps = 13
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "DIAS DA SEMANA",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val dayLabels = listOf(
                    1 to "Seg",
                    2 to "Ter",
                    3 to "Qua",
                    4 to "Qui",
                    5 to "Sex",
                    6 to "Sáb",
                    7 to "Dom"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayLabels.forEach { (iso, label) ->
                        val isSelected = iso in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) palette.secondary else palette.surfaceElevated)
                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
                                .clickable {
                                    selectedDays = if (isSelected && selectedDays.size > 1) {
                                        selectedDays - iso
                                    } else {
                                        selectedDays + iso
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) palette.onSecondary else palette.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .clickable { syncToCalendar = !syncToCalendar }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = syncToCalendar,
                        onCheckedChange = { syncToCalendar = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Sincronizar com Calendário do Android",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Adiciona os horários desta rotina à agenda do aparelho",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialRoutine.id != 0L && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(initialRoutine) },
                            modifier = Modifier
                                .size(48.dp)
                                .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .background(palette.alarmCoral.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .testTag("delete_routine_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir rotina",
                                tint = palette.alarmCoral
                            )
                        }
                    }
                    PixelButton(
                        text = "SALVAR ROTINA",
                        onClick = {
                            onSave(
                                initialRoutine.copy(
                                    title = title.ifBlank { category.label },
                                    description = description,
                                    categoryName = category.name,
                                    spriteName = sprite.name,
                                    alertModeName = alertMode.name,
                                    frequencyTypeName = frequencyType.name,
                                    hour = hour,
                                    minute = minute,
                                    intervalMinutes = intervalMinutes,
                                    windowStartHour = windowStartHour,
                                    windowEndHour = windowEndHour,
                                    targetCountPerDay = if (frequencyType == FrequencyType.SPECIFIC_TIME) 1 else targetCountPerDay,
                                    daysOfWeekCsv = selectedDays.sorted().joinToString(","),
                                    syncToCalendar = syncToCalendar
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_routine_button")
                    )
                }
            }
        }
    }
}
