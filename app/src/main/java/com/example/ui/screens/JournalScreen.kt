package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AiReactionMode
import com.example.data.DateUtils
import com.example.data.JournalContextTag
import com.example.data.JournalEntryEntity
import com.example.data.JournalEntryType
import com.example.ui.RoutineUiState
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelDiceIcon
import com.example.ui.components.PixelEnergyIcon
import com.example.ui.components.PixelLockIcon
import com.example.ui.components.PixelMedalIcon
import com.example.ui.components.PixelMoodIcon
import com.example.ui.components.energyLabelPt
import com.example.ui.components.moodLabelPt
import com.example.ui.theme.LocalPixelPalette
import kotlin.random.Random

private val REFLECTION_PROMPTS_PT = listOf(
    "Qual pequena vitória de hoje merece ser celebrada?",
    "O que drenou ou recarregou mais a sua energia nas últimas horas?",
    "Se você pudesse dar um conselho gentil para si mesmo hoje, qual seria?",
    "Qual rotina funcionou bem hoje e o que facilitou cumpri-la?",
    "O que está ocupando mais espaço na sua mente neste momento?",
    "Pelo que você sente gratidão sincera no dia de hoje?",
    "Qual ajuste simples tornaria o seu amanhã mais leve?",
    "Que momento hoje trouxe calma, foco ou um sorriso espontâneo?",
    "Se sua energia fosse uma barra de jogo hoje, o que seria a poção de cura?",
    "Qual pensamento você gostaria de deixar registrado para reler daqui a 30 dias?",
    "Que limite saudável você respeitou (ou gostaria de respeitar) hoje?",
    "O que você aprendeu sobre o seu ritmo pessoal esta semana?"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    uiState: RoutineUiState,
    onSelectDate: (String) -> Unit,
    onShiftDate: (Int) -> Unit,
    onSaveEntry: (
        entryType: JournalEntryType,
        moodScore: Int?,
        energyScore: Int?,
        contextTags: List<String>,
        content: String,
        unlockAtMillis: Long?
    ) -> Unit,
    onDeleteEntry: (JournalEntryEntity) -> Unit,
    onToggleStealthMode: () -> Unit,
    onSetStealthMode: (Boolean) -> Unit,
    onRequestAiReaction: (JournalEntryEntity, AiReactionMode) -> Unit = { _, _ -> },
    onRequestWeeklyPatchNotes: () -> Unit = {},
    onOpenSuggestedRoutine: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val isStealth = uiState.settings.stealthModeEnabled
    val configuredPin = uiState.settings.journalPin
    val hasGeminiKey = uiState.settings.geminiApiKey.isNotBlank()
    val isSelectedToday = uiState.selectedJournalDateKey == uiState.todayDateKey

    var selectedMode by rememberSaveable { mutableStateOf(JournalEntryType.MICRO_LOG) }
    var selectedMood by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedEnergy by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var contentText by rememberSaveable { mutableStateOf("") }
    var capsuleDays by rememberSaveable { mutableIntStateOf(30) }
    var activePrompt by rememberSaveable { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    val dayEntries = uiState.selectedDateJournalEntries

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("journal_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Date Selector & Stealth Mode Toggle
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DIÁRIO & ENERGIA",
                                style = MaterialTheme.typography.headlineSmall,
                                color = palette.primary
                            )
                            Text(
                                text = "Check-ins, micro-logs, reflexões e cápsulas",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isStealth) palette.snoozeAmber.copy(alpha = 0.22f)
                                    else palette.surface
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isStealth) palette.snoozeAmber else palette.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (isStealth && configuredPin.isNotEmpty()) {
                                        showPinDialog = true
                                    } else {
                                        onToggleStealthMode()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("journal_stealth_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PixelLockIcon(
                                isLocked = isStealth,
                                modifier = Modifier.size(20.dp),
                                description = if (isStealth) "Desativar modo furtivo" else "Ativar modo furtivo"
                            )
                            Text(
                                text = if (isStealth) "FURTIVO" else "VISÍVEL",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.surface)
                            .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onShiftDate(-1) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("journal_prev_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Dia anterior",
                                tint = palette.textPrimary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isSelectedToday) {
                                    "HOJE • ${DateUtils.formatFullDatePt(uiState.selectedJournalDateKey)}"
                                } else {
                                    DateUtils.formatFullDatePt(uiState.selectedJournalDateKey)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isSelectedToday) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Voltar para Hoje",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary,
                                    modifier = Modifier
                                        .clickable { onSelectDate(uiState.todayDateKey) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("journal_jump_today_button")
                                )
                            }
                        }

                        IconButton(
                            onClick = { onShiftDate(1) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("journal_next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Próximo dia",
                                tint = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // 1.5 Weekly Patch Notes Card (shown when Gemini API key is configured)
        if (hasGeminiKey) {
            item {
                WeeklyPatchNotesCard(
                    cachedNotes = uiState.settings.cachedWeeklyPatchNotes,
                    cachedDateKey = uiState.settings.cachedPatchNotesDateKey,
                    isLoading = uiState.isAiLoading && uiState.aiLoadingEntryId == null,
                    isStealth = isStealth,
                    onGeneratePatchNotes = onRequestWeeklyPatchNotes
                )
            }
        }

        // 2. "Há 30 dias" Memory Card (when an unlocked entry from 30 days ago exists)
        uiState.memory30DaysAgoEntry?.let { memoryEntry ->
            item {
                Memory30DaysAgoCard(
                    entry = memoryEntry,
                    isStealth = isStealth
                )
            }
        }

        // 3. Dual Recording Area (Micro-log / Reflection / Time Capsule)
        item {
            PixelCard(
                containerColor = palette.surfaceElevated,
                modifier = Modifier.testTag("journal_composer_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOVO REGISTRO",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primary
                        )
                        PixelBadge(
                            text = "ROTINA ${uiState.streakMedalSummary.todayCompletionPercent}%",
                            containerColor = palette.surface,
                            contentColor = palette.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Selector Chips
                    val modes = listOf(
                        JournalEntryType.MICRO_LOG,
                        JournalEntryType.REFLECTION,
                        JournalEntryType.TIME_CAPSULE
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { mode ->
                            val isSelected = selectedMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) palette.primary else palette.surface)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.5.dp,
                                        color = palette.pixelBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedMode = mode }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .testTag("journal_mode_${mode.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.badgeLabelPt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) palette.onPrimary else palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Time Capsule Unlock Horizon Picker
                    AnimatedVisibility(visible = selectedMode == JournalEntryType.TIME_CAPSULE) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            val unlockMillisPreview = System.currentTimeMillis() + capsuleDays * 86_400_000L
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DESTRANCAR EM:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                                Text(
                                    text = "Abre em ${DateUtils.formatDateNumericPt(unlockMillisPreview)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.snoozeAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(7, 30, 90).forEach { days ->
                                    val chosen = capsuleDays == days
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (chosen) palette.snoozeAmber.copy(alpha = 0.28f)
                                                else palette.surface
                                            )
                                            .border(
                                                width = if (chosen) 2.dp else 1.5.dp,
                                                color = if (chosen) palette.snoozeAmber else palette.pixelBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { capsuleDays = days }
                                            .padding(vertical = 8.dp)
                                            .testTag("capsule_days_$days"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+$days DIAS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Mood Selector (1-5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HUMOR (OPCIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = selectedMood?.let { moodLabelPt(it) } ?: "Toque para marcar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedMood != null) palette.primary else palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { level ->
                            val isChosen = selectedMood == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isChosen) palette.primary.copy(alpha = 0.2f)
                                        else palette.surface
                                    )
                                    .border(
                                        width = if (isChosen) 2.5.dp else 1.5.dp,
                                        color = if (isChosen) palette.primary else palette.pixelBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedMood = if (isChosen) null else level
                                    }
                                    .testTag("journal_mood_level_$level"),
                                contentAlignment = Alignment.Center
                            ) {
                                PixelMoodIcon(
                                    level = level,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Energy Selector (1-5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ENERGIA (OPCIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = selectedEnergy?.let { energyLabelPt(it) } ?: "Toque para marcar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedEnergy != null) palette.wellnessGreen else palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { level ->
                            val isChosen = selectedEnergy == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isChosen) palette.wellnessGreen.copy(alpha = 0.2f)
                                        else palette.surface
                                    )
                                    .border(
                                        width = if (isChosen) 2.5.dp else 1.5.dp,
                                        color = if (isChosen) palette.wellnessGreen else palette.pixelBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedEnergy = if (isChosen) null else level
                                    }
                                    .testTag("journal_energy_level_$level"),
                                contentAlignment = Alignment.Center
                            ) {
                                PixelEnergyIcon(
                                    level = level,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Context Tags Chips
                    Text(
                        text = "CONTEXTO DO MOMENTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("journal_tag_${tag.code.lowercase()}")
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

                    // Prompt Dice Button ("Me dê um gatilho")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedMode) {
                                JournalEntryType.MICRO_LOG -> "NOTA RÁPIDA"
                                JournalEntryType.REFLECTION -> "REFLEXÃO DO DIA"
                                JournalEntryType.TIME_CAPSULE -> "MENSAGEM PARA O FUTURO"
                                JournalEntryType.MOOD_CHECKIN -> "OBSERVAÇÃO"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.surface)
                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    val candidates = REFLECTION_PROMPTS_PT.filter { it != activePrompt }
                                    activePrompt = candidates[Random.nextInt(candidates.size)]
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("journal_prompt_dice_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PixelDiceIcon(modifier = Modifier.size(18.dp))
                            Text(
                                text = "Me dê um gatilho",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = activePrompt != null) {
                        activePrompt?.let { prompt ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.primary.copy(alpha = 0.12f))
                                    .border(1.5.dp, palette.primary, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                                    .testTag("journal_active_prompt_box")
                            ) {
                                Text(
                                    text = "🎲 $prompt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ USAR NO TEXTO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary,
                                        modifier = Modifier
                                            .clickable {
                                                contentText = if (contentText.isBlank()) {
                                                    "$prompt\n"
                                                } else {
                                                    "$contentText\n$prompt\n"
                                                }
                                            }
                                            .testTag("journal_insert_prompt_text")
                                    )
                                    Text(
                                        text = "FECHAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        modifier = Modifier.clickable { activePrompt = null }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        placeholder = {
                            Text(
                                text = when (selectedMode) {
                                    JournalEntryType.MICRO_LOG -> "Anote um pensamento rápido, vitória ou ajuste do momento..."
                                    JournalEntryType.REFLECTION -> "Escreva sua reflexão do dia com calma e clareza..."
                                    JournalEntryType.TIME_CAPSULE -> "Escreva uma carta para abrir no futuro..."
                                    JournalEntryType.MOOD_CHECKIN -> "Como você está se sentindo agora?"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary.copy(alpha = 0.7f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                        minLines = if (selectedMode == JournalEntryType.MICRO_LOG) 2 else 4,
                        maxLines = if (selectedMode == JournalEntryType.MICRO_LOG) 4 else 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.pixelBorder,
                            focusedContainerColor = palette.surface,
                            unfocusedContainerColor = palette.surface,
                            cursorColor = palette.primary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_content_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val canSave = when (selectedMode) {
                        JournalEntryType.TIME_CAPSULE -> contentText.isNotBlank()
                        else -> contentText.isNotBlank() || selectedMood != null || selectedEnergy != null
                    }

                    PixelButton(
                        text = when (selectedMode) {
                            JournalEntryType.TIME_CAPSULE -> "🔒 SELAR CÁPSULA (+${capsuleDays}D)"
                            JournalEntryType.REFLECTION -> "📖 SALVAR REFLEXÃO"
                            else -> "✨ SALVAR NO DIÁRIO"
                        },
                        onClick = {
                            if (canSave) {
                                val unlockAt = if (selectedMode == JournalEntryType.TIME_CAPSULE) {
                                    System.currentTimeMillis() + capsuleDays * 86_400_000L
                                } else {
                                    null
                                }
                                onSaveEntry(
                                    selectedMode,
                                    selectedMood,
                                    selectedEnergy,
                                    selectedTags.toList(),
                                    contentText,
                                    unlockAt
                                )
                                contentText = ""
                                selectedMood = null
                                selectedEnergy = null
                                selectedTags = emptySet()
                                activePrompt = null
                            }
                        },
                        containerColor = if (canSave) {
                            if (selectedMode == JournalEntryType.TIME_CAPSULE) palette.snoozeAmber else palette.primary
                        } else {
                            palette.surface
                        },
                        contentColor = if (canSave) {
                            if (selectedMode == JournalEntryType.TIME_CAPSULE) palette.onSecondary else palette.onPrimary
                        } else {
                            palette.textSecondary
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_save_entry_button")
                    )
                }
            }
        }

        // 4. Chronological Day Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEED CRONOLÓGICO DO DIA",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                PixelBadge(
                    text = "${dayEntries.size} REGISTROS",
                    containerColor = palette.secondary,
                    contentColor = palette.onSecondary
                )
            }
        }

        if (dayEntries.isEmpty()) {
            item {
                PixelCard(containerColor = palette.surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PixelMoodIcon(
                            level = 4,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum registro salvo nesta data.",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use o painel acima para registrar seu humor, nível de energia ou um micro-log rápido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            items(dayEntries, key = { "journal_entry_${it.id}" }) { entry ->
                JournalEntryCard(
                    entry = entry,
                    isStealth = isStealth,
                    hasGeminiKey = hasGeminiKey,
                    isAiLoadingThisEntry = uiState.isAiLoading && uiState.aiLoadingEntryId == entry.id,
                    isAnyAiLoading = uiState.isAiLoading,
                    onDelete = { onDeleteEntry(entry) },
                    onRequestAiReaction = { mode -> onRequestAiReaction(entry, mode) },
                    onOpenSuggestedRoutine = onOpenSuggestedRoutine
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showPinDialog) {
        JournalPinUnlockDialog(
            expectedPin = configuredPin,
            onDismiss = { showPinDialog = false },
            onUnlocked = {
                showPinDialog = false
                onSetStealthMode(false)
            }
        )
    }
}

@Composable
private fun Memory30DaysAgoCard(
    entry: JournalEntryEntity,
    isStealth: Boolean
) {
    val palette = LocalPixelPalette.current
    PixelCard(
        containerColor = palette.secondary.copy(alpha = 0.16f),
        borderColor = palette.secondary,
        modifier = Modifier.testTag("journal_memory_30_days_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelBadge(
                    text = "✨ CÁPSULA • HÁ 30 DIAS",
                    containerColor = palette.secondary,
                    contentColor = palette.onSecondary
                )
                Text(
                    text = DateUtils.formatFullDatePt(entry.dateKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isStealth) {
                StealthHiddenContentBox()
            } else {
                Text(
                    text = entry.content.ifBlank {
                        "Check-in registrado há 30 dias (${entry.routineCompletionPercentSnapshot}% da rotina concluída)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalEntryCard(
    entry: JournalEntryEntity,
    isStealth: Boolean,
    hasGeminiKey: Boolean,
    isAiLoadingThisEntry: Boolean,
    isAnyAiLoading: Boolean,
    onDelete: () -> Unit,
    onRequestAiReaction: (AiReactionMode) -> Unit,
    onOpenSuggestedRoutine: (String) -> Unit
) {
    val palette = LocalPixelPalette.current
    val isLockedCapsule = entry.isTimeCapsuleLocked()
    val supportsAiActions = hasGeminiKey &&
        !isLockedCapsule &&
        (entry.entryType == JournalEntryType.REFLECTION || entry.entryType == JournalEntryType.MICRO_LOG)

    PixelCard(
        containerColor = palette.surfaceElevated,
        borderColor = if (isLockedCapsule) palette.snoozeAmber else palette.pixelBorder,
        modifier = Modifier.testTag("journal_entry_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Time + Entry Type + Mood/Energy Sprites + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelBadge(
                        text = DateUtils.formatTime(entry.timestamp),
                        containerColor = palette.surface,
                        contentColor = palette.textPrimary
                    )
                    PixelBadge(
                        text = entry.entryType.badgeLabelPt,
                        containerColor = when (entry.entryType) {
                            JournalEntryType.MOOD_CHECKIN -> palette.wellnessGreen
                            JournalEntryType.MICRO_LOG -> palette.waterCyan
                            JournalEntryType.REFLECTION -> palette.primary
                            JournalEntryType.TIME_CAPSULE -> palette.snoozeAmber
                        },
                        contentColor = if (entry.entryType == JournalEntryType.TIME_CAPSULE) {
                            palette.onSecondary
                        } else {
                            palette.onPrimary
                        }
                    )
                    entry.moodScore?.let { mood ->
                        PixelMoodIcon(
                            level = mood,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    entry.energyScore?.let { energy ->
                        PixelEnergyIcon(
                            level = energy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_journal_entry_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remover registro do diário",
                        tint = palette.textSecondary
                    )
                }
            }

            // Automatic Routine & Pomodoro Snapshot Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.pixelBorder.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixelMedalIcon(
                    tier = entry.medalSnapshot,
                    isUnlocked = entry.medalSnapshot != com.example.ui.components.PixelMedalTier.NONE,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Rotina: ${entry.routineCompletionPercentSnapshot}% • ${entry.medalSnapshot.titlePt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (entry.pomodoroCyclesSnapshot > 0) {
                    PixelBadge(
                        text = "🍅 ${entry.pomodoroCyclesSnapshot}",
                        containerColor = palette.alarmCoral.copy(alpha = 0.22f),
                        contentColor = palette.textPrimary
                    )
                }
            }

            // Context Tags
            if (entry.contextTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.contextTags.forEach { tagCode ->
                        PixelBadge(
                            text = "#${JournalContextTag.labelForCode(tagCode)}",
                            containerColor = palette.surface,
                            contentColor = palette.textSecondary
                        )
                    }
                }
            }

            // Body Content (Locked Time Capsule / Stealth Redacted / Normal)
            if (isLockedCapsule) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.snoozeAmber.copy(alpha = 0.16f))
                        .border(1.5.dp, palette.snoozeAmber, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                        .testTag("locked_capsule_box_${entry.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PixelLockIcon(
                        isLocked = true,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CÁPSULA DO TEMPO TRANCADA",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary
                        )
                        val dateText = entry.unlockAtMillis?.let { DateUtils.formatDateNumericPt(it) } ?: "--/--/----"
                        Text(
                            text = "Conteúdo lacrado até $dateText.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            } else if (entry.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                if (isStealth) {
                    StealthHiddenContentBox()
                } else {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                }
            } else if (entry.moodScore != null || entry.energyScore != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val moodStr = entry.moodScore?.let { "Humor: ${moodLabelPt(it)}" }
                val energyStr = entry.energyScore?.let { "Energia: ${energyLabelPt(it)}" }
                val summary = listOfNotNull(moodStr, energyStr).joinToString(" • ")
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }

            // Optional On-Demand Gemini AI Action Bar (only for REFLECTION / MICRO_LOG when API key is set)
            if (supportsAiActions) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AiReactionMode.VALIDATE_SUMMARY,
                        AiReactionMode.SOCRATIC_QUESTIONS,
                        AiReactionMode.ACTIONABLE_STEPS
                    ).forEach { mode ->
                        val isActiveMode = entry.aiReactionMode == mode && entry.aiResponseText.isNotBlank()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (isActiveMode) palette.primary.copy(alpha = 0.18f)
                                    else palette.surface
                                )
                                .border(
                                    width = if (isActiveMode) 1.5.dp else 1.dp,
                                    color = if (isActiveMode) palette.primary else palette.pixelBorder.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .clickable(enabled = !isAnyAiLoading) { onRequestAiReaction(mode) }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("journal_ai_action_${mode.name.lowercase()}_${entry.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.buttonLabelPt,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAnyAiLoading) palette.textSecondary.copy(alpha = 0.5f) else palette.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isAiLoadingThisEntry) {
                Spacer(modifier = Modifier.height(10.dp))
                PixelAiLoadingIndicator(label = "GEMINI ANALISANDO...")
            }

            // Highlighted AI Response Block (persisted in Room, visible offline once generated)
            if (entry.aiResponseText.isNotBlank() && !isLockedCapsule) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.primary.copy(alpha = 0.12f))
                        .border(1.5.dp, palette.primary, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                        .testTag("journal_ai_response_box_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelBadge(
                            text = "✨ IA • ${entry.aiReactionMode.labelPt.uppercase()}",
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isStealth) {
                        StealthHiddenContentBox()
                    } else {
                        Text(
                            text = entry.aiResponseText,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        if (entry.aiSuggestedRoutineJson.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            PixelButton(
                                text = "✨ TRANSFORMAR EM ROTINA",
                                onClick = { onOpenSuggestedRoutine(entry.aiSuggestedRoutineJson) },
                                containerColor = palette.wellnessGreen,
                                contentColor = palette.onPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("journal_transform_routine_${entry.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyPatchNotesCard(
    cachedNotes: String,
    cachedDateKey: String,
    isLoading: Boolean,
    isStealth: Boolean,
    onGeneratePatchNotes: () -> Unit
) {
    val palette = LocalPixelPalette.current
    PixelCard(
        containerColor = palette.surfaceElevated,
        borderColor = palette.primary,
        modifier = Modifier.testTag("journal_weekly_patch_notes_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PATCH NOTES DA SEMANA",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.primary
                    )
                    Text(
                        text = if (cachedDateKey.isNotBlank()) {
                            "Último resumo: ${DateUtils.formatFullDatePt(cachedDateKey)}"
                        } else {
                            "Buffs, Nerfs e Próxima Quest dos últimos 7 dias"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                PixelBadge(
                    text = "✨ GEMINI",
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(10.dp))
                PixelAiLoadingIndicator(label = "GERANDO PATCH NOTES (7D)...")
            } else if (cachedNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    if (isStealth) {
                        StealthHiddenContentBox()
                    } else {
                        Text(
                            text = cachedNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            PixelButton(
                text = if (isLoading) "⏳ PROCESSANDO..." else "🎮 GERAR PATCH NOTES (7D)",
                onClick = { if (!isLoading) onGeneratePatchNotes() },
                containerColor = if (isLoading) palette.surface else palette.secondary,
                contentColor = if (isLoading) palette.textSecondary else palette.onSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_generate_patch_notes_button")
            )
        }
    }
}

@Composable
private fun PixelAiLoadingIndicator(label: String) {
    val palette = LocalPixelPalette.current
    val transition = rememberInfiniteTransition(label = "pixel_ai_loading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pixel_loading_step"
    )
    val activeIdx = phase.toInt() % 6

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surface)
            .border(1.5.dp, palette.primary, RoundedCornerShape(6.dp))
            .padding(10.dp)
            .testTag("pixel_ai_loading_indicator"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val isLit = i == activeIdx || i == (activeIdx + 1) % 6
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isLit) palette.primary else palette.pixelBorder.copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                        .border(1.dp, palette.pixelBorder, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun StealthHiddenContentBox() {
    val palette = LocalPixelPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surface)
            .border(1.5.dp, palette.pixelBorder.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .padding(10.dp)
            .testTag("stealth_redacted_box"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PixelLockIcon(
            isLocked = true,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(palette.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .height(8.dp)
                        .background(palette.textSecondary.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                )
            }
            Text(
                text = "Ocultado pelo Modo Furtivo",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun JournalPinUnlockDialog(
    expectedPin: String,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    val palette = LocalPixelPalette.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixelLockIcon(isLocked = true, modifier = Modifier.size(22.dp))
                Text(
                    text = "DESBLOQUEAR DIÁRIO",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Digite seu PIN de 4 dígitos para revelar os textos do diário:",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { raw ->
                        enteredPin = raw.filter { it.isDigit() }.take(4)
                        errorMsg = null
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    placeholder = { Text("••••") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_pin_unlock_input")
                )
                errorMsg?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.alarmCoral
                    )
                }
            }
        },
        confirmButton = {
            PixelButton(
                text = "DESTRANCAR",
                onClick = {
                    if (enteredPin == expectedPin) {
                        onUnlocked()
                    } else {
                        errorMsg = "PIN incorreto. Tente novamente."
                    }
                },
                containerColor = palette.primary,
                contentColor = palette.onPrimary,
                modifier = Modifier.testTag("journal_pin_confirm_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = palette.textSecondary)
            }
        }
    )
}
