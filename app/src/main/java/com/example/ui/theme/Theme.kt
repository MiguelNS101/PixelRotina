package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalPixelPalette = staticCompositionLocalOf { CatppuccinMochaPalette }

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = label
    )
    return animated
}

private fun PixelPalette.toColorScheme(): ColorScheme {
    return if (themeType.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = accent,
            onTertiary = onAccent,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = pixelBorder
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = accent,
            onTertiary = onAccent,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = pixelBorder
        )
    }
}

@Composable
fun PixelRotinaTheme(
    themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
    content: @Composable () -> Unit
) {
    val target = getPixelPalette(themeType)

    val animatedPalette = PixelPalette(
        themeType = target.themeType,
        background = animateThemeColor(target.background, "bg"),
        surface = animateThemeColor(target.surface, "surface"),
        surfaceElevated = animateThemeColor(target.surfaceElevated, "surfaceElevated"),
        primary = animateThemeColor(target.primary, "primary"),
        onPrimary = animateThemeColor(target.onPrimary, "onPrimary"),
        secondary = animateThemeColor(target.secondary, "secondary"),
        onSecondary = animateThemeColor(target.onSecondary, "onSecondary"),
        accent = animateThemeColor(target.accent, "accent"),
        onAccent = animateThemeColor(target.onAccent, "onAccent"),
        textPrimary = animateThemeColor(target.textPrimary, "textPrimary"),
        textSecondary = animateThemeColor(target.textSecondary, "textSecondary"),
        pixelBorder = animateThemeColor(target.pixelBorder, "pixelBorder"),
        pixelShadow = animateThemeColor(target.pixelShadow, "pixelShadow"),
        waterCyan = animateThemeColor(target.waterCyan, "waterCyan"),
        wellnessGreen = animateThemeColor(target.wellnessGreen, "wellnessGreen"),
        alarmCoral = animateThemeColor(target.alarmCoral, "alarmCoral"),
        snoozeAmber = animateThemeColor(target.snoozeAmber, "snoozeAmber")
    )

    val colorScheme = animatedPalette.toColorScheme()

    CompositionLocalProvider(LocalPixelPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
