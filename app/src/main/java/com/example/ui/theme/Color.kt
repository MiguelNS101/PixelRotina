package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class PixelThemeType(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean
) {
    CATPPUCCIN_MOCHA(
        id = "CATPPUCCIN_MOCHA",
        title = "Catppuccin Mocha",
        subtitle = "Escuro acolhedor com Mauve, Teal, Pêssego e base Espresso",
        isDark = true
    ),
    CATPPUCCIN_MACCHIATO(
        id = "CATPPUCCIN_MACCHIATO",
        title = "Catppuccin Macchiato",
        subtitle = "Escuro suave aveludado com Lavanda, Verde Chá e Rosewater",
        isDark = true
    ),
    NOITE_ACONCHEGANTE(
        id = "NOITE_ACONCHEGANTE",
        title = "Noite Aconchegante",
        subtitle = "Azul noturno profundo com Ciano calmo e Lavanda suave",
        isDark = true
    ),
    FLORESTA_NOTURNA(
        id = "FLORESTA_NOTURNA",
        title = "Floresta Noturna",
        subtitle = "Musgo escuro relaxante com Verde Esmeralda e luz de vagalume",
        isDark = true
    ),
    CAFE_EXPRESSO(
        id = "CAFE_EXPRESSO",
        title = "Café Expresso 16-Bit",
        subtitle = "Torra escura quente com Creme Caramelo, Menta e Cereja",
        isDark = true
    ),
    CYBER_LOFI_DARK(
        id = "CYBER_LOFI_DARK",
        title = "Lo-Fi Meia-Noite",
        subtitle = "Ardósia escuro com Verde Matcha, Rosa Retrô e Âmbar",
        isDark = true
    ),
    PASTEL_RETRO(
        id = "PASTEL_RETRO",
        title = "Pastel Retrô",
        subtitle = "Lavanda, menta, pêssego e creme aconchegante",
        isDark = false
    ),
    NATUREZA_LOFI(
        id = "NATUREZA_LOFI",
        title = "Natureza Lo-Fi",
        subtitle = "Verde folha, azul céu, amarelo sol e terracota",
        isDark = false
    ),
    POR_DO_SOL_16BIT(
        id = "POR_DO_SOL_16BIT",
        title = "Pôr do Sol 16-Bit",
        subtitle = "Coral vibrante, dourado quente, ameixa e areia",
        isDark = false
    ),
    OCEANO_PIXEL(
        id = "OCEANO_PIXEL",
        title = "Oceano Pixel",
        subtitle = "Turquesa relaxante, azul brisa e espuma do mar",
        isDark = false
    );

    companion object {
        fun fromId(id: String?): PixelThemeType {
            return entries.firstOrNull { it.id == id } ?: CATPPUCCIN_MOCHA
        }
    }
}

data class PixelPalette(
    val themeType: PixelThemeType,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val pixelBorder: Color,
    val pixelShadow: Color,
    val waterCyan: Color,
    val wellnessGreen: Color,
    val alarmCoral: Color,
    val snoozeAmber: Color
)

// Official Catppuccin Mocha inspired Pixel Art Palette
val CatppuccinMochaPalette = PixelPalette(
    themeType = PixelThemeType.CATPPUCCIN_MOCHA,
    background = Color(0xFF1E1E2E),      // Base
    surface = Color(0xFF181825),         // Mantle
    surfaceElevated = Color(0xFF313244), // Surface0
    primary = Color(0xFFCBA6F7),         // Mauve
    onPrimary = Color(0xFF11111B),       // Crust
    secondary = Color(0xFF94E2D5),       // Teal
    onSecondary = Color(0xFF11111B),
    accent = Color(0xFFFAB387),          // Peach
    onAccent = Color(0xFF11111B),
    textPrimary = Color(0xFFCDD6F4),     // Text
    textSecondary = Color(0xFFA6ADC8),   // Subtext0
    pixelBorder = Color(0xFF585B70),     // Surface2
    pixelShadow = Color(0xFF11111B),     // Crust
    waterCyan = Color(0xFF89DCEB),       // Sky
    wellnessGreen = Color(0xFFA6E3A1),   // Green
    alarmCoral = Color(0xFFF38BA8),      // Red / Pink
    snoozeAmber = Color(0xFFF9E2AF)      // Yellow
)

// Official Catppuccin Macchiato inspired Pixel Art Palette
val CatppuccinMacchiatoPalette = PixelPalette(
    themeType = PixelThemeType.CATPPUCCIN_MACCHIATO,
    background = Color(0xFF24273A),      // Base
    surface = Color(0xFF1E2030),         // Mantle
    surfaceElevated = Color(0xFF363A4F), // Surface0
    primary = Color(0xFFB7BDF8),         // Lavender
    onPrimary = Color(0xFF181926),       // Crust
    secondary = Color(0xFF8BD5CA),       // Teal
    onSecondary = Color(0xFF181926),
    accent = Color(0xFFF5A97F),          // Peach
    onAccent = Color(0xFF181926),
    textPrimary = Color(0xFFCAD3F5),     // Text
    textSecondary = Color(0xFFA5ADCB),   // Subtext0
    pixelBorder = Color(0xFF5B6078),     // Surface2
    pixelShadow = Color(0xFF181926),     // Crust
    waterCyan = Color(0xFF91D7E3),       // Sky
    wellnessGreen = Color(0xFFA6DA95),   // Green
    alarmCoral = Color(0xFFED8796),      // Red
    snoozeAmber = Color(0xFFEED49F)      // Yellow
)

val NoiteAconchegantePalette = PixelPalette(
    themeType = PixelThemeType.NOITE_ACONCHEGANTE,
    background = Color(0xFF141829),
    surface = Color(0xFF1F2640),
    surfaceElevated = Color(0xFF293252),
    primary = Color(0xFF5CE1E6),
    onPrimary = Color(0xFF0B2229),
    secondary = Color(0xFFB388FF),
    onSecondary = Color(0xFF1A0F33),
    accent = Color(0xFFFF9E80),
    onAccent = Color(0xFF2D120A),
    textPrimary = Color(0xFFF3F6FF),
    textSecondary = Color(0xFFA7B3D6),
    pixelBorder = Color(0xFF4A588A),
    pixelShadow = Color(0xFF0B0E1A),
    waterCyan = Color(0xFF4DD0E1),
    wellnessGreen = Color(0xFF69F0AE),
    alarmCoral = Color(0xFFFF6E7F),
    snoozeAmber = Color(0xFFFFD166)
)

val FlorestaNoturnaPalette = PixelPalette(
    themeType = PixelThemeType.FLORESTA_NOTURNA,
    background = Color(0xFF111C16),
    surface = Color(0xFF17261E),
    surfaceElevated = Color(0xFF22362B),
    primary = Color(0xFF68D391),
    onPrimary = Color(0xFF0B1911),
    secondary = Color(0xFF4FD1C5),
    onSecondary = Color(0xFF0A1D1B),
    accent = Color(0xFFF6AD55),
    onAccent = Color(0xFF221304),
    textPrimary = Color(0xFFE6FFFA),
    textSecondary = Color(0xFFA3C2B3),
    pixelBorder = Color(0xFF3B5E4C),
    pixelShadow = Color(0xFF09110C),
    waterCyan = Color(0xFF63B3ED),
    wellnessGreen = Color(0xFF68D391),
    alarmCoral = Color(0xFFFC8181),
    snoozeAmber = Color(0xFFF6E05E)
)

val CafeExpressoPalette = PixelPalette(
    themeType = PixelThemeType.CAFE_EXPRESSO,
    background = Color(0xFF1C1613),
    surface = Color(0xFF251D19),
    surfaceElevated = Color(0xFF332823),
    primary = Color(0xFFE6AE65),
    onPrimary = Color(0xFF241709),
    secondary = Color(0xFF7CC6A6),
    onSecondary = Color(0xFF10241B),
    accent = Color(0xFFE07A5F),
    onAccent = Color(0xFF210C07),
    textPrimary = Color(0xFFF5EBE6),
    textSecondary = Color(0xFFC4B2A9),
    pixelBorder = Color(0xFF5C473E),
    pixelShadow = Color(0xFF110D0B),
    waterCyan = Color(0xFF72B5C4),
    wellnessGreen = Color(0xFF81B29A),
    alarmCoral = Color(0xFFE06C75),
    snoozeAmber = Color(0xFFF2CC8F)
)

val CyberLofiDarkPalette = PixelPalette(
    themeType = PixelThemeType.CYBER_LOFI_DARK,
    background = Color(0xFF0F141C),
    surface = Color(0xFF161E2B),
    surfaceElevated = Color(0xFF202B3D),
    primary = Color(0xFF58E6A9),
    onPrimary = Color(0xFF072116),
    secondary = Color(0xFFFF75A0),
    onSecondary = Color(0xFF260913),
    accent = Color(0xFFFFD166),
    onAccent = Color(0xFF241A02),
    textPrimary = Color(0xFFEDF2F7),
    textSecondary = Color(0xFF9FB0C7),
    pixelBorder = Color(0xFF3B4D6B),
    pixelShadow = Color(0xFF080B10),
    waterCyan = Color(0xFF4CC9F0),
    wellnessGreen = Color(0xFF58E6A9),
    alarmCoral = Color(0xFFFF5D8F),
    snoozeAmber = Color(0xFFFFD166)
)

val PastelRetroPalette = PixelPalette(
    themeType = PixelThemeType.PASTEL_RETRO,
    background = Color(0xFFFFFDF7),
    surface = Color(0xFFFFF5EB),
    surfaceElevated = Color(0xFFFFFFFF),
    primary = Color(0xFF7E57FF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF38C6B6),
    onSecondary = Color(0xFF103430),
    accent = Color(0xFFFF7A63),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF2B2340),
    textSecondary = Color(0xFF675D7E),
    pixelBorder = Color(0xFF392E56),
    pixelShadow = Color(0xFFD9C9F2),
    waterCyan = Color(0xFF2AB7CA),
    wellnessGreen = Color(0xFF3CC480),
    alarmCoral = Color(0xFFFF647C),
    snoozeAmber = Color(0xFFFFB03A)
)

val NaturezaLofiPalette = PixelPalette(
    themeType = PixelThemeType.NATUREZA_LOFI,
    background = Color(0xFFF5F8EE),
    surface = Color(0xFFE9F0DC),
    surfaceElevated = Color(0xFFFFFFFF),
    primary = Color(0xFF368557),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4A90E2),
    onSecondary = Color(0xFFFFFFFF),
    accent = Color(0xFFE89B25),
    onAccent = Color(0xFF281B05),
    textPrimary = Color(0xFF1F3325),
    textSecondary = Color(0xFF526A58),
    pixelBorder = Color(0xFF294531),
    pixelShadow = Color(0xFFC8D8B8),
    waterCyan = Color(0xFF3898EC),
    wellnessGreen = Color(0xFF368557),
    alarmCoral = Color(0xFFD95B43),
    snoozeAmber = Color(0xFFE89B25)
)

val PorDoSol16BitPalette = PixelPalette(
    themeType = PixelThemeType.POR_DO_SOL_16BIT,
    background = Color(0xFFFFF8F2),
    surface = Color(0xFFFDECE3),
    surfaceElevated = Color(0xFFFFFFFF),
    primary = Color(0xFFE0534C),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7B4B94),
    onSecondary = Color(0xFFFFFFFF),
    accent = Color(0xFFF4A261),
    onAccent = Color(0xFF2C1706),
    textPrimary = Color(0xFF36202D),
    textSecondary = Color(0xFF725365),
    pixelBorder = Color(0xFF4C2B3E),
    pixelShadow = Color(0xFFF2CCBA),
    waterCyan = Color(0xFF3AA6B9),
    wellnessGreen = Color(0xFF43AA8B),
    alarmCoral = Color(0xFFE0534C),
    snoozeAmber = Color(0xFFF4A261)
)

val OceanoPixelPalette = PixelPalette(
    themeType = PixelThemeType.OCEANO_PIXEL,
    background = Color(0xFFF0FAFC),
    surface = Color(0xFFDFF3F7),
    surfaceElevated = Color(0xFFFFFFFF),
    primary = Color(0xFF008B9E),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2EC4B6),
    onSecondary = Color(0xFF062C28),
    accent = Color(0xFFFF6B6B),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF12323B),
    textSecondary = Color(0xFF486C78),
    pixelBorder = Color(0xFF1A4B59),
    pixelShadow = Color(0xFFBBE3EC),
    waterCyan = Color(0xFF00A8E8),
    wellnessGreen = Color(0xFF2EC4B6),
    alarmCoral = Color(0xFFFF6B6B),
    snoozeAmber = Color(0xFFFFB703)
)

fun getPixelPalette(themeType: PixelThemeType): PixelPalette {
    return when (themeType) {
        PixelThemeType.CATPPUCCIN_MOCHA -> CatppuccinMochaPalette
        PixelThemeType.CATPPUCCIN_MACCHIATO -> CatppuccinMacchiatoPalette
        PixelThemeType.NOITE_ACONCHEGANTE -> NoiteAconchegantePalette
        PixelThemeType.FLORESTA_NOTURNA -> FlorestaNoturnaPalette
        PixelThemeType.CAFE_EXPRESSO -> CafeExpressoPalette
        PixelThemeType.CYBER_LOFI_DARK -> CyberLofiDarkPalette
        PixelThemeType.PASTEL_RETRO -> PastelRetroPalette
        PixelThemeType.NATUREZA_LOFI -> NaturezaLofiPalette
        PixelThemeType.POR_DO_SOL_16BIT -> PorDoSol16BitPalette
        PixelThemeType.OCEANO_PIXEL -> OceanoPixelPalette
    }
}
