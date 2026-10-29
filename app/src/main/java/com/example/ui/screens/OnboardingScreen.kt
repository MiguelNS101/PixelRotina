package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.HeroArtPreference
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelBlockProgressBar
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.theme.LocalPixelPalette
import com.example.ui.theme.PixelThemeType
import com.example.ui.theme.getPixelPalette

@Composable
fun OnboardingScreen(
    selectedTheme: PixelThemeType,
    selectedHeroArt: HeroArtPreference,
    hasNotificationPermission: Boolean,
    hasCalendarPermission: Boolean,
    onSelectTheme: (PixelThemeType) -> Unit,
    onSelectHeroArt: (HeroArtPreference) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onFinishOnboarding: () -> Unit
) {
    val palette = LocalPixelPalette.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("onboarding_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header with Step Progress & Skip Button
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelSpriteIcon(
                        spriteType = PixelSpriteType.LEAF_WELLNESS,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PIXELROTINA • BEM-VINDO",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.primary
                    )
                }
                PixelBadge(
                    text = "PULAR",
                    containerColor = palette.surface,
                    contentColor = palette.textSecondary,
                    modifier = Modifier
                        .clickable(onClick = onFinishOnboarding)
                        .testTag("onboarding_skip_button")
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            PixelBlockProgressBar(
                progress = (currentStep + 1).toFloat() / totalSteps.toFloat(),
                totalBlocks = totalSteps,
                activeColor = palette.wellnessGreen
            )
        }

        // Step Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                label = "onboarding_step_transition"
            ) { step ->
                when (step) {
                    0 -> OnboardingStepWelcomeAndTheme(
                        selectedTheme = selectedTheme,
                        selectedHeroArt = selectedHeroArt,
                        onSelectTheme = onSelectTheme,
                        onSelectHeroArt = onSelectHeroArt
                    )
                    1 -> OnboardingStepRemindersAndAlarms()
                    else -> OnboardingStepPermissionsAndCloud(
                        hasNotificationPermission = hasNotificationPermission,
                        hasCalendarPermission = hasCalendarPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onRequestCalendarPermission = onRequestCalendarPermission
                    )
                }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                PixelButton(
                    text = "◀ VOLTAR",
                    onClick = { currentStep-- },
                    containerColor = palette.surfaceElevated,
                    contentColor = palette.textPrimary,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("onboarding_back_button")
                )
            }
            PixelButton(
                text = if (currentStep < totalSteps - 1) "PRÓXIMO ▶" else "✨ COMEÇAR MINHA ROTINA",
                onClick = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onFinishOnboarding()
                    }
                },
                containerColor = if (currentStep < totalSteps - 1) palette.primary else palette.wellnessGreen,
                contentColor = palette.onPrimary,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("onboarding_next_button")
            )
        }
    }
}

@Composable
private fun OnboardingStepWelcomeAndTheme(
    selectedTheme: PixelThemeType,
    selectedHeroArt: HeroArtPreference,
    onSelectTheme: (PixelThemeType) -> Unit,
    onSelectHeroArt: (HeroArtPreference) -> Unit
) {
    val palette = LocalPixelPalette.current
    val previewDrawable = selectedHeroArt.resolveForHour().drawableRes ?: R.drawable.img_pixel_hero_night

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PixelCard(containerColor = palette.surfaceElevated) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Image(
                        painter = painterResource(id = previewDrawable),
                        contentDescription = "Arte de boas-vindas em Pixel Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        PixelBadge(
                            text = "PASSO 1 DE 3 • SEU ESTILO",
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ORGANIZE SEU DIA COM LEVEZA",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Um companheiro de bem-estar em Pixel Art para lembrar de beber água, manter seus horários e descansar — sem gamificação punitiva e sem pressão.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                }
            }
        }

        PixelCard(containerColor = palette.surfaceElevated) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ESCOLHA SEU TEMA INICIAL",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val featuredThemes = listOf(
                    PixelThemeType.CATPPUCCIN_MOCHA,
                    PixelThemeType.CATPPUCCIN_MACCHIATO,
                    PixelThemeType.CAFE_EXPRESSO,
                    PixelThemeType.FLORESTA_NOTURNA,
                    PixelThemeType.PASTEL_RETRO,
                    PixelThemeType.NATUREZA_LOFI
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    featuredThemes.forEach { theme ->
                        val tPal = getPixelPalette(theme)
                        val isSelected = selectedTheme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(tPal.background)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) tPal.primary else tPal.pixelBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectTheme(theme) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSelected) "▶ ${theme.title}" else theme.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = tPal.textPrimary
                                )
                                Text(
                                    text = theme.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tPal.textSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(tPal.primary, tPal.secondary, tPal.accent).forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(1.dp, tPal.pixelBorder, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Você pode trocar entre todos os 10 temas e personalizar a arte de fundo a qualquer momento na aba Ajustes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepRemindersAndAlarms() {
    val palette = LocalPixelPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PixelCard(containerColor = palette.surfaceElevated) {
            Column(modifier = Modifier.padding(16.dp)) {
                PixelBadge(
                    text = "PASSO 2 DE 3 • FORA DO APP & NO BOOT",
                    containerColor = palette.secondary,
                    contentColor = palette.onSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "COMO FUNCIONAM SEUS LEMBRETES",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Todos os avisos funcionam com o app fechado e são reagendados automaticamente se você reiniciar o celular.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        }

        OnboardingFeatureCard(
            sprite = PixelSpriteType.WATER_DROP,
            badgeText = "1 TOQUE NA NOTIFICAÇÃO",
            title = "Avisos de Intervalo com 'OK! Feito'",
            description = "Configure lembretes a cada 45 min, 1h ou 2h (como beber água ou alongar) e confirme direto no botão 'OK! Feito' da notificação sem abrir o app."
        )

        OnboardingFeatureCard(
            sprite = PixelSpriteType.SUN_MORNING,
            badgeText = "SEM CULPA OU PRESSÃO",
            title = "Horários Específicos & Reagendamento Suave",
            description = "Se você estiver ocupado na hora de uma tarefa, use a Soneca Rápida (+15 min) na notificação ou reagende suavemente no painel Hoje."
        )

        OnboardingFeatureCard(
            sprite = PixelSpriteType.ALARM_BELL,
            badgeText = "DESPERTADOR PERSISTENTE",
            title = "Modo Alarme Real (Toca e Vibra)",
            description = "Para compromissos essenciais ou hora de dormir, ative o Modo Alarme: ele vibra e toca som de alarme contínuo até você desligar."
        )
    }
}

@Composable
private fun OnboardingStepPermissionsAndCloud(
    hasNotificationPermission: Boolean,
    hasCalendarPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit
) {
    val palette = LocalPixelPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PixelCard(containerColor = palette.surfaceElevated) {
            Column(modifier = Modifier.padding(16.dp)) {
                PixelBadge(
                    text = "PASSO 3 DE 3 • TUDO PRONTO",
                    containerColor = palette.accent,
                    contentColor = palette.onAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CALENDÁRIO, PERMISSÕES & NUVEM",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ative as permissões abaixo para receber seus avisos fora do app e sincronizar com o Calendário do seu Android:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        }

        PixelCard(containerColor = palette.surfaceElevated) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 Notificações & Alarmes",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Necessário para exibir os avisos com botão 'OK! Feito' fora do app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (hasNotificationPermission) {
                        PixelBadge(
                            text = "✓ ATIVADO",
                            containerColor = palette.wellnessGreen,
                            contentColor = palette.onPrimary
                        )
                    } else {
                        PixelButton(
                            text = "ATIVAR",
                            onClick = onRequestNotificationPermission,
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                    }
                }
            }
        }

        PixelCard(containerColor = palette.surfaceElevated) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📅 Calendário do Android",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Opcional: mostra seus compromissos do mês na aba Calendário e exporta rotinas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (hasCalendarPermission) {
                        PixelBadge(
                            text = "✓ CONECTADO",
                            containerColor = palette.wellnessGreen,
                            contentColor = palette.onPrimary
                        )
                    } else {
                        PixelButton(
                            text = "CONECTAR",
                            onClick = onRequestCalendarPermission,
                            containerColor = palette.secondary,
                            contentColor = palette.onSecondary
                        )
                    }
                }
            }
        }

        PixelCard(containerColor = palette.surface) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelSpriteIcon(
                    spriteType = PixelSpriteType.SPARKLE_OK,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "100% Offline-First + Backup no Google Drive",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ao concluir uma tarefa, aproveite a chuva de confetes em pixel art! Seus dados ficam salvos no aparelho e podem ser exportados ao Google Drive quando quiser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OnboardingFeatureCard(
    sprite: PixelSpriteType,
    badgeText: String,
    title: String,
    description: String
) {
    val palette = LocalPixelPalette.current
    PixelCard(containerColor = palette.surfaceElevated) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(palette.surface, RoundedCornerShape(6.dp))
                    .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                PixelSpriteIcon(
                    spriteType = sprite,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                PixelBadge(
                    text = badgeText,
                    containerColor = palette.surface,
                    contentColor = palette.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}
