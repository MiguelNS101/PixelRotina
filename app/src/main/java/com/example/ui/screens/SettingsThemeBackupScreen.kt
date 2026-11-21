package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateUtils
import com.example.data.HeroArtPreference
import com.example.ui.RoutineUiState
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelLockIcon
import com.example.ui.theme.LocalPixelPalette
import com.example.ui.theme.PixelThemeType
import com.example.ui.theme.getPixelPalette

@Composable
fun SettingsThemeBackupScreen(
    uiState: RoutineUiState,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    hasCalendarPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onSelectTheme: (PixelThemeType) -> Unit,
    onSelectHeroArt: (HeroArtPreference) -> Unit,
    onSelectSnoozeMinutes: (Int) -> Unit,
    onShareToGoogleDrive: () -> Unit,
    onExportJsonFile: () -> Unit,
    onImportJsonFile: (Boolean) -> Unit,
    onSaveQuickLocalBackup: () -> Unit,
    onRestoreQuickLocalBackup: () -> Unit,
    onStopRingingAlarm: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onUpdateJournalPin: (String) -> Unit = {},
    onToggleStealthMode: () -> Unit = {},
    onUpdateGeminiApiKey: (String) -> Unit = {},
    onUpdateGeminiModelName: (String) -> Unit = {},
    onToggleDailyCheckInNotif: (Boolean) -> Unit = {},
    onUpdateDailyCheckInTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onTestDailyCheckInNotif: () -> Unit = {},
    onTogglePromptMoodAfterRoutine: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val darkThemes = PixelThemeType.entries.filter { it.isDark }
    val lightThemes = PixelThemeType.entries.filter { !it.isDark }

    var isHeroArtCardExpanded by rememberSaveable { mutableStateOf(false) }
    var isThemeCardExpanded by rememberSaveable { mutableStateOf(false) }
    var isDailyCheckInCardExpanded by rememberSaveable { mutableStateOf(true) }
    var isJournalPrivacyExpanded by rememberSaveable { mutableStateOf(false) }
    var isGeminiCardExpanded by rememberSaveable { mutableStateOf(false) }
    var pinDraft by rememberSaveable(uiState.settings.journalPin) {
        mutableStateOf(uiState.settings.journalPin)
    }
    var geminiKeyDraft by rememberSaveable(uiState.settings.geminiApiKey) {
        mutableStateOf(uiState.settings.geminiApiKey)
    }

    val activeArtPref = uiState.settings.heroArtPreference
    val activeArtThumbRes = activeArtPref.drawableRes
        ?: activeArtPref.resolveForHour().drawableRes
        ?: R.drawable.img_pixel_hero_banner

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_backup_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Minimizable Background Art Selector for "Sua Rotina de Hoje" Card
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isHeroArtCardExpanded = !isHeroArtCardExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("toggle_hero_art_card_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ARTE DO CARD 'HOJE'",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Text(
                                text = "Atual: ${activeArtPref.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PixelBadge(
                                text = "${HeroArtPreference.entries.size} CENÁRIOS",
                                containerColor = palette.accent,
                                contentColor = palette.onAccent
                            )
                            PixelBadge(
                                text = if (isHeroArtCardExpanded) "▲ MINIMIZAR" else "▼ ESCOLHER",
                                containerColor = palette.surface,
                                contentColor = palette.textPrimary
                            )
                        }
                    }

                    // Compact summary preview when minimized
                    if (!isHeroArtCardExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.surface)
                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                .clickable { isHeroArtCardExpanded = true }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = activeArtThumbRes),
                                contentDescription = activeArtPref.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "▶ ${activeArtPref.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Toque para ver todas as ${HeroArtPreference.entries.size} artes do dia",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isHeroArtCardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Escolha se a ilustração do card principal muda automaticamente ao longo do dia ou fixe seu cenário favorito:",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HeroArtPreference.entries.forEach { artPref ->
                                    val isSelected = uiState.settings.heroArtPreference == artPref
                                    val thumbRes = artPref.drawableRes
                                        ?: artPref.resolveForHour().drawableRes
                                        ?: R.drawable.img_pixel_hero_banner

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) palette.primary.copy(alpha = 0.16f) else palette.surface
                                            )
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.5.dp,
                                                color = if (isSelected) palette.primary else palette.pixelBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onSelectHeroArt(artPref) }
                                            .padding(10.dp)
                                            .testTag("hero_art_option_${artPref.id.lowercase()}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = thumbRes),
                                            contentDescription = artPref.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 64.dp, height = 42.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isSelected) "▶ ${artPref.title}" else artPref.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = artPref.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Minimizable Multi-Theme Pixel Art Picker (Exclusive to Settings Screen)
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isThemeCardExpanded = !isThemeCardExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("toggle_theme_card_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TEMAS PIXEL ART",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Text(
                                text = "Ativo: ${uiState.settings.themeType.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PixelBadge(
                                text = "${PixelThemeType.entries.size} TEMAS",
                                containerColor = palette.secondary,
                                contentColor = palette.onSecondary
                            )
                            PixelBadge(
                                text = if (isThemeCardExpanded) "▲ MINIMIZAR" else "▼ ESCOLHER",
                                containerColor = palette.surface,
                                contentColor = palette.textPrimary
                            )
                        }
                    }

                    // Compact preview of the active theme when minimized
                    if (!isThemeCardExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ThemeOptionRow(
                            theme = uiState.settings.themeType,
                            isSelected = true,
                            onSelect = { isThemeCardExpanded = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = isThemeCardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Personalize as cores do seu app (inclui Catppuccin Mocha, Macchiato e paletas noturnas relaxantes):",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "🌙 TEMAS ESCUROS & CATPPUCCIN (${darkThemes.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                darkThemes.forEach { theme ->
                                    ThemeOptionRow(
                                        theme = theme,
                                        isSelected = uiState.settings.themeType == theme,
                                        onSelect = { onSelectTheme(theme) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "☀️ TEMAS CLAROS RETRÔ (${lightThemes.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                lightThemes.forEach { theme ->
                                    ThemeOptionRow(
                                        theme = theme,
                                        isSelected = uiState.settings.themeType == theme,
                                        onSelect = { onSelectTheme(theme) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.25 Minimizable Configurable End-of-Day Daily Check-in & Routine Mood Card
        item {
            val checkInEnabled = uiState.settings.dailyCheckInNotifEnabled
            val checkInHour = uiState.settings.dailyCheckInNotifHour
            val checkInMinute = uiState.settings.dailyCheckInNotifMinute
            val promptAfterRoutine = uiState.settings.promptMoodAfterRoutineCheckIn
            val formattedCheckInTime = String.format(java.util.Locale.US, "%02d:%02d", checkInHour, checkInMinute)

            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isDailyCheckInCardExpanded = !isDailyCheckInCardExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("toggle_daily_checkin_card_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CHECK-IN DIÁRIO & HUMOR",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Text(
                                text = if (checkInEnabled) {
                                    "Notificação diária às $formattedCheckInTime • Média nas rotinas"
                                } else {
                                    "Notificação do fim do dia desativada"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PixelBadge(
                                text = if (checkInEnabled) "🌙 $formattedCheckInTime" else "OFF",
                                containerColor = if (checkInEnabled) palette.wellnessGreen else palette.surface,
                                contentColor = if (checkInEnabled) palette.onPrimary else palette.textSecondary
                            )
                            PixelBadge(
                                text = if (isDailyCheckInCardExpanded) "▲" else "▼",
                                containerColor = palette.surface,
                                contentColor = palette.textPrimary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isDailyCheckInCardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Receba uma notificação interativa no fim do dia para registrar Humor e Energia em 2 toques (sem precisar abrir o app).",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.surface)
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                    .clickable { onToggleDailyCheckInNotif(!checkInEnabled) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("settings_daily_checkin_enabled_toggle"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lembrete no fim do dia",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                                PixelBadge(
                                    text = if (checkInEnabled) "✓ ATIVO" else "DESATIVADO",
                                    containerColor = if (checkInEnabled) palette.wellnessGreen else palette.surfaceElevated,
                                    contentColor = if (checkInEnabled) palette.onPrimary else palette.textSecondary
                                )
                            }

                            if (checkInEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "HORÁRIO DO CHECK-IN ($formattedCheckInTime)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PixelButton(
                                        text = "-1H",
                                        onClick = { onUpdateDailyCheckInTime((checkInHour + 23) % 24, checkInMinute) },
                                        containerColor = palette.surface,
                                        contentColor = palette.textPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("daily_checkin_hour_minus")
                                    )
                                    PixelButton(
                                        text = "+1H",
                                        onClick = { onUpdateDailyCheckInTime((checkInHour + 1) % 24, checkInMinute) },
                                        containerColor = palette.surface,
                                        contentColor = palette.textPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("daily_checkin_hour_plus")
                                    )
                                    PixelButton(
                                        text = "-15M",
                                        onClick = {
                                            val totalMins = (checkInHour * 60 + checkInMinute - 15 + 1440) % 1440
                                            onUpdateDailyCheckInTime(totalMins / 60, totalMins % 60)
                                        },
                                        containerColor = palette.surface,
                                        contentColor = palette.textPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("daily_checkin_min_minus")
                                    )
                                    PixelButton(
                                        text = "+15M",
                                        onClick = {
                                            val totalMins = (checkInHour * 60 + checkInMinute + 15) % 1440
                                            onUpdateDailyCheckInTime(totalMins / 60, totalMins % 60)
                                        },
                                        containerColor = palette.surface,
                                        contentColor = palette.textPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("daily_checkin_min_plus")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(20 to 0, 21 to 0, 22 to 0, 22 to 30).forEach { (h, m) ->
                                        val isChosen = checkInHour == h && checkInMinute == m
                                        val label = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isChosen) palette.primary else palette.surface)
                                                .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                                .clickable { onUpdateDailyCheckInTime(h, m) }
                                                .padding(vertical = 8.dp)
                                                .testTag("daily_checkin_preset_${h}_${m}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isChosen) palette.onPrimary else palette.textPrimary
                                            )
                                        }
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
                                    .clickable { onTogglePromptMoodAfterRoutine(!promptAfterRoutine) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("settings_prompt_mood_after_routine_toggle"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Perguntar humor após concluir rotina",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "Atualiza sua média diária ao longo do dia",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                PixelBadge(
                                    text = if (promptAfterRoutine) "✓ SIM" else "NÃO",
                                    containerColor = if (promptAfterRoutine) palette.secondary else palette.surfaceElevated,
                                    contentColor = if (promptAfterRoutine) palette.onSecondary else palette.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.surface)
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "💡 Pelas notificações de rotina: toque em 'OK + Humor' para concluir a tarefa e escolher Humor (passo 1/2) e Energia (passo 2/2) direto na barra!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            PixelButton(
                                text = "🌙 TESTAR NOTIFICAÇÃO DE CHECK-IN AGORA",
                                onClick = onTestDailyCheckInNotif,
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_test_daily_checkin_button")
                            )
                        }
                    }
                }
            }
        }

        // 2.5 Minimizable Journal Privacy (PIN & Stealth Mode) Card
        item {
            val hasPin = uiState.settings.journalPin.isNotEmpty()
            val isStealth = uiState.settings.stealthModeEnabled
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isJournalPrivacyExpanded = !isJournalPrivacyExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("toggle_journal_privacy_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelLockIcon(
                                isLocked = hasPin || isStealth,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "PRIVACIDADE DO DIÁRIO (PIN)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.primary
                                )
                                Text(
                                    text = if (hasPin) {
                                        "PIN ativo • Modo Furtivo: ${if (isStealth) "Ligado" else "Desligado"}"
                                    } else {
                                        "Sem PIN • Modo Furtivo: ${if (isStealth) "Ligado" else "Desligado"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        PixelBadge(
                            text = if (isJournalPrivacyExpanded) "▲ MINIMIZAR" else "▼ AJUSTAR",
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary
                        )
                    }

                    AnimatedVisibility(
                        visible = isJournalPrivacyExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "O Modo Furtivo oculta o conteúdo das notas na aba Diário. Se você definir um PIN de 4 dígitos, ele será exigido para revelar os textos novamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.surface)
                                    .border(1.5.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                    .clickable { onToggleStealthMode() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("settings_stealth_mode_toggle"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Modo Furtivo no Diário",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                                PixelBadge(
                                    text = if (isStealth) "🔒 ATIVO" else "🔓 DESATIVADO",
                                    containerColor = if (isStealth) palette.snoozeAmber else palette.surfaceElevated,
                                    contentColor = if (isStealth) palette.onSecondary else palette.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pinDraft,
                                onValueChange = { raw ->
                                    pinDraft = raw.filter { it.isDigit() }.take(4)
                                },
                                label = {
                                    Text(
                                        text = if (hasPin) "Alterar PIN (4 dígitos)" else "Definir PIN de 4 dígitos",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                placeholder = { Text("••••") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.primary,
                                    unfocusedBorderColor = palette.pixelBorder,
                                    focusedContainerColor = palette.surface,
                                    unfocusedContainerColor = palette.surface
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_journal_pin_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PixelButton(
                                    text = "🔐 SALVAR PIN",
                                    onClick = {
                                        if (pinDraft.length == 4) {
                                            onUpdateJournalPin(pinDraft)
                                        }
                                    },
                                    containerColor = if (pinDraft.length == 4) palette.primary else palette.surface,
                                    contentColor = if (pinDraft.length == 4) palette.onPrimary else palette.textSecondary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_save_pin_button")
                                )
                                if (hasPin) {
                                    PixelButton(
                                        text = "REMOVER PIN",
                                        onClick = {
                                            pinDraft = ""
                                            onUpdateJournalPin("")
                                        },
                                        containerColor = palette.alarmCoral,
                                        contentColor = palette.onPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_remove_pin_button")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.75 Minimizable Optional Gemini AI Configuration Card
        item {
            val hasGeminiKey = uiState.settings.geminiApiKey.isNotBlank()
            val selectedModel = uiState.settings.geminiModelName
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isGeminiCardExpanded = !isGeminiCardExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("toggle_gemini_ai_card_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INTELIGÊNCIA ARTIFICIAL (GEMINI)",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary
                            )
                            Text(
                                text = if (hasGeminiKey) {
                                    "Chave configurada • $selectedModel"
                                } else {
                                    "Opcional • Operando 100% offline"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PixelBadge(
                                text = if (hasGeminiKey) "ATIVO" else "OFFLINE",
                                containerColor = if (hasGeminiKey) palette.wellnessGreen else palette.surface,
                                contentColor = if (hasGeminiKey) palette.onPrimary else palette.textSecondary
                            )
                            PixelBadge(
                                text = if (isGeminiCardExpanded) "▲ MINIMIZAR" else "▼ AJUSTAR",
                                containerColor = palette.surface,
                                contentColor = palette.textPrimary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isGeminiCardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Opcional. A chave fica salva apenas localmente no DataStore do aparelho. Sem chave, todos os gráficos e recursos do diário funcionam 100% offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = geminiKeyDraft,
                                onValueChange = { geminiKeyDraft = it },
                                label = {
                                    Text(
                                        text = "Chave API Gemini (AIza...)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                placeholder = { Text("Cole sua chave Gemini aqui") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.primary,
                                    unfocusedBorderColor = palette.pixelBorder,
                                    focusedContainerColor = palette.surface,
                                    unfocusedContainerColor = palette.surface
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_gemini_api_key_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "MODELO GEMINI",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "gemini-2.5-flash" to "2.5 FLASH",
                                    "gemini-2.5-pro" to "2.5 PRO"
                                ).forEach { (modelId, label) ->
                                    val isChosen = selectedModel == modelId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isChosen) palette.primary else palette.surface)
                                            .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                            .clickable { onUpdateGeminiModelName(modelId) }
                                            .padding(vertical = 10.dp)
                                            .testTag("settings_gemini_model_${modelId.replace(".", "_")}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isChosen) palette.onPrimary else palette.textPrimary
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
                                    text = "💾 SALVAR CHAVE",
                                    onClick = { onUpdateGeminiApiKey(geminiKeyDraft.trim()) },
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_save_gemini_key_button")
                                )
                                PixelButton(
                                    text = "LIMPAR CHAVE",
                                    onClick = {
                                        geminiKeyDraft = ""
                                        onUpdateGeminiApiKey("")
                                    },
                                    containerColor = palette.alarmCoral,
                                    contentColor = palette.onPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_clear_gemini_key_button")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Offline-First Backup & Google Drive Sync
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SAVE LOCAL & GOOGLE DRIVE",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primary
                        )
                        PixelBadge(
                            text = "OFFLINE-FIRST",
                            containerColor = palette.secondary,
                            contentColor = palette.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Seus dados ficam 100% salvos no aparelho. Exporte seu arquivo de save (.json) ou envie direto para o seu Google Drive a qualquer momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    if (uiState.settings.lastBackupTimestamp > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Último save gerado às ${DateUtils.formatTime(uiState.settings.lastBackupTimestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.wellnessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    PixelButton(
                        text = "☁️ ENVIAR SAVE PARA O GOOGLE DRIVE",
                        onClick = onShareToGoogleDrive,
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_gdrive_backup_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixelButton(
                            text = "💾 EXPORTAR .JSON",
                            onClick = onExportJsonFile,
                            containerColor = palette.secondary,
                            contentColor = palette.onSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_file_button")
                        )
                        PixelButton(
                            text = "📂 IMPORTAR .JSON",
                            onClick = { onImportJsonFile(false) },
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_file_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixelButton(
                            text = "⚡ SALVAR RÁPIDO",
                            onClick = onSaveQuickLocalBackup,
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_local_save_button")
                        )
                        PixelButton(
                            text = "⚡ RESTAURAR RÁPIDO",
                            onClick = onRestoreQuickLocalBackup,
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_local_restore_button")
                        )
                    }
                }
            }
        }

        // 4. Smart Snooze, Onboarding & Background Alarm Status
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LEMBRETES FORA DO APP & APÓS REINÍCIO",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tempo padrão do botão 'Adiar' na notificação:",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 30, 60).forEach { mins ->
                            val isSelected = uiState.settings.defaultSnoozeMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) palette.primary else palette.surface)
                                    .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectSnoozeMinutes(mins) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${mins}m",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) palette.onPrimary else palette.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    PermissionStatusRow(
                        title = "Notificações com Botão OK",
                        isGranted = hasNotificationPermission,
                        actionText = "Permitir",
                        onAction = onRequestNotificationPermission
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        title = "Alarmes Exatos & Persistência no Boot",
                        isGranted = canScheduleExactAlarms,
                        actionText = "Configurar",
                        onAction = onOpenExactAlarmSettings
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        title = "Sincronização com Calendário Android",
                        isGranted = hasCalendarPermission,
                        actionText = "Permitir",
                        onAction = onRequestCalendarPermission
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    PixelButton(
                        text = "✨ REVER BOAS-VINDAS (ONBOARDING)",
                        onClick = onReopenOnboarding,
                        containerColor = palette.surface,
                        contentColor = palette.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reopen_onboarding_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    PixelButton(
                        text = "🔕 PARAR QUALQUER ALARME TOCANDO AGORA",
                        onClick = onStopRingingAlarm,
                        containerColor = palette.alarmCoral,
                        contentColor = palette.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stop_ringing_alarm_button")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ThemeOptionRow(
    theme: PixelThemeType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val themePalette = getPixelPalette(theme)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(themePalette.background)
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) themePalette.primary else themePalette.pixelBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp)
            .testTag("theme_option_${theme.id.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isSelected) "▶ ${theme.title} (Ativo)" else theme.title,
                style = MaterialTheme.typography.titleSmall,
                color = themePalette.textPrimary
            )
            Text(
                text = theme.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = themePalette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                themePalette.primary,
                themePalette.secondary,
                themePalette.accent,
                themePalette.surfaceElevated
            ).forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, themePalette.pixelBorder, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    val palette = LocalPixelPalette.current
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            PixelBadge(
                text = "✓ ATIVO",
                containerColor = palette.wellnessGreen,
                contentColor = palette.onPrimary
            )
        } else {
            PixelBadge(
                text = actionText.uppercase(),
                containerColor = palette.snoozeAmber,
                contentColor = palette.onPrimary,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}
