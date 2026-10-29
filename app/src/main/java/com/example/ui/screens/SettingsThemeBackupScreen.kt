package com.example.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateUtils
import com.example.data.HeroArtPreference
import com.example.ui.RoutineUiState
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
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
    modifier: Modifier = Modifier
) {
    val palette = LocalPixelPalette.current
    val darkThemes = PixelThemeType.entries.filter { it.isDark }
    val lightThemes = PixelThemeType.entries.filter { !it.isDark }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_backup_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Background Art Selector for "Sua Rotina de Hoje" Card
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ARTE DO CARD 'HOJE'",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primary
                        )
                        PixelBadge(
                            text = "7 CENÁRIOS",
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Escolha se a ilustração do card principal muda automaticamente ao longo do dia ou fixe seu cenário favorito:",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroArtPreference.entries.forEach { artPref ->
                            val isSelected = uiState.settings.heroArtPreference == artPref
                            val thumbRes = artPref.drawableRes ?: artPref.resolveForHour().drawableRes ?: R.drawable.img_pixel_hero_banner

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

        // 2. Multi-Theme Pixel Art Picker (Exclusive to Settings Screen)
        item {
            PixelCard(containerColor = palette.surfaceElevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEMAS PIXEL ART",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primary
                        )
                        PixelBadge(
                            text = "${PixelThemeType.entries.size} TEMAS",
                            containerColor = palette.secondary,
                            contentColor = palette.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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
