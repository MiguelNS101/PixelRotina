package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalPixelPalette
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class PixelSpriteType(val labelPt: String) {
    WATER_DROP("Gota d'Água"),
    SUN_MORNING("Sol Radiante"),
    MOON_REST("Lua & Sono"),
    LEAF_WELLNESS("Folha Verde"),
    HEART_CARE("Coração"),
    ALARM_BELL("Sino Alarme"),
    BOOK_FOCUS("Livro & Estudo"),
    CALENDAR_GRID("Calendário"),
    SPARKLE_OK("Estrela Pixel"),
    DUMBBELL_FITNESS("Halter Treino"),
    PILL_HEALTH("Cápsula Saúde"),
    COFFEE_MUG("Xícara Café/Chá"),
    MUSIC_NOTE("Nota Musical"),
    HOME_CARE("Casa & Ordem"),
    PAW_PET("Patinha Pet"),
    LOTUS_MIND("Flor de Lótus"),
    GAMEPAD_HOBBY("Controle Retrô")
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalPixelPalette.current.surfaceElevated,
    borderColor: Color = LocalPixelPalette.current.pixelBorder,
    shadowColor: Color = LocalPixelPalette.current.pixelShadow,
    shadowOffset: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_press"
    )

    Surface(
        color = containerColor,
        shape = shape,
        border = BorderStroke(2.5.dp, borderColor),
        modifier = modifier
            .padding(bottom = shadowOffset, end = shadowOffset)
            .drawBehind {
                val fullOffsetPx = shadowOffset.toPx()
                val remainingOffsetPx = fullOffsetPx * (1f - pressProgress * 0.75f)
                val cornerPx = 6.dp.toPx()
                val borderPx = 2.dp.toPx()
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(remainingOffsetPx, remainingOffsetPx),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
                drawRoundRect(
                    color = borderColor.copy(alpha = 0.7f),
                    topLeft = Offset(remainingOffsetPx, remainingOffsetPx),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = borderPx)
                )
            }
            .graphicsLayer {
                val travelPx = shadowOffset.toPx() * 0.75f * pressProgress
                translationX = travelPx
                translationY = travelPx
            }
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * Physical Retro Arcade/Pixel Button that physically depresses down-right into its shadow
 * with spring physics and tactile haptic feedback when pressed.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalPixelPalette.current.primary,
    contentColor: Color = LocalPixelPalette.current.onPrimary,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val palette = LocalPixelPalette.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(5.dp)
    val shadowOffset = 3.5.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pixel_button_press"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Surface(
        color = containerColor,
        shape = shape,
        border = BorderStroke(2.dp, palette.pixelBorder),
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .padding(bottom = shadowOffset, end = shadowOffset)
            .drawBehind {
                // As the button moves down-right by travelPx, the relative shadow shrinks so the
                // shadow stays fixed on the desk while the physical button cap sinks into it!
                val fullOffsetPx = shadowOffset.toPx()
                val remainingOffsetPx = fullOffsetPx * (1f - pressProgress * 0.85f)
                val cornerPx = 5.dp.toPx()
                drawRoundRect(
                    color = palette.pixelBorder,
                    topLeft = Offset(remainingOffsetPx, remainingOffsetPx),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            }
            .graphicsLayer {
                val travelPx = shadowOffset.toPx() * 0.85f * pressProgress
                translationX = travelPx
                translationY = travelPx
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PixelBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val border = LocalPixelPalette.current.pixelBorder
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .background(containerColor, shape)
            .border(1.5.dp, border, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PixelBlockProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    totalBlocks: Int = 10,
    activeColor: Color = LocalPixelPalette.current.secondary,
    inactiveColor: Color = LocalPixelPalette.current.surface
) {
    val palette = LocalPixelPalette.current
    val filledBlocks = (progress.coerceIn(0f, 1f) * totalBlocks).toInt()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(inactiveColor, RoundedCornerShape(4.dp))
            .border(2.dp, palette.pixelBorder, RoundedCornerShape(4.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 0 until totalBlocks) {
            val isFilled = i < filledBlocks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(
                        if (isFilled) activeColor else palette.pixelBorder.copy(alpha = 0.14f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

private data class PixelParticleSpec(
    val startXFraction: Float,
    val startYFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val sizeDp: Float,
    val colorIndex: Int,
    val isCrossShape: Boolean
)

/**
 * Full-screen non-blocking 8-bit Pixel Art Confetti & Sparkle Particle Burst
 * triggered whenever the user completes a routine task.
 */
@Composable
fun PixelConfettiOverlay(
    triggerId: Int,
    modifier: Modifier = Modifier
) {
    if (triggerId <= 0) return
    val palette = LocalPixelPalette.current
    val progress = remember(triggerId) { Animatable(0f) }

    val particles = remember(triggerId) {
        val rng = Random(triggerId * 9973 + 42)
        List(48) { idx ->
            val angle = rng.nextDouble(-Math.PI * 0.92, -Math.PI * 0.08).toFloat()
            val speed = rng.nextFloat() * 0.65f + 0.35f
            PixelParticleSpec(
                startXFraction = 0.18f + (idx % 5) * 0.16f + (rng.nextFloat() - 0.5f) * 0.08f,
                startYFraction = 0.42f + (rng.nextFloat() - 0.5f) * 0.14f,
                velocityX = cos(angle) * speed * (if (rng.nextBoolean()) 1f else -1f),
                velocityY = sin(angle) * speed * 1.15f,
                sizeDp = rng.nextInt(6, 12).toFloat(),
                colorIndex = idx % 6,
                isCrossShape = idx % 5 == 0
            )
        }
    }

    LaunchedEffect(triggerId) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1250, easing = LinearEasing)
        )
    }

    val t = progress.value
    if (t >= 1f) return

    val colors = listOf(
        palette.wellnessGreen,
        palette.primary,
        palette.secondary,
        palette.accent,
        palette.waterCyan,
        palette.snoozeAmber
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Animação de confete em pixel art" }
    ) {
        val gridSnap = 4.dp.toPx().coerceAtLeast(2f)
        val alpha = (1f - (t * t)).coerceIn(0f, 1f)
        val gravity = 1.25f

        particles.forEach { p ->
            val rawX = (p.startXFraction + p.velocityX * t) * size.width
            val rawY = (p.startYFraction + p.velocityY * t + 0.5f * gravity * t * t) * size.height

            val snappedX = (rawX / gridSnap).roundToInt() * gridSnap
            val snappedY = (rawY / gridSnap).roundToInt() * gridSnap
            val pxSize = p.sizeDp.dp.toPx()
            val color = colors[p.colorIndex].copy(alpha = alpha)
            val outlineColor = palette.pixelBorder.copy(alpha = alpha * 0.85f)

            if (snappedX in 0f..size.width && snappedY in 0f..size.height) {
                if (p.isCrossShape) {
                    val arm = pxSize * 0.45f
                    drawRect(
                        color = color,
                        topLeft = Offset(snappedX - arm, snappedY),
                        size = Size(pxSize + arm * 2, pxSize)
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(snappedX, snappedY - arm),
                        size = Size(pxSize, pxSize + arm * 2)
                    )
                } else {
                    drawRect(
                        color = outlineColor,
                        topLeft = Offset(snappedX + 2f, snappedY + 2f),
                        size = Size(pxSize, pxSize)
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(snappedX, snappedY),
                        size = Size(pxSize, pxSize)
                    )
                }
            }
        }
    }
}

@Composable
fun PixelSpriteIcon(
    spriteType: PixelSpriteType,
    modifier: Modifier = Modifier.size(28.dp),
    tintOverride: Color? = null,
    description: String = spriteType.name
) {
    val palette = LocalPixelPalette.current
    val grid = spriteType.pixelGrid()

    val mainColor = tintOverride ?: when (spriteType) {
        PixelSpriteType.WATER_DROP -> palette.waterCyan
        PixelSpriteType.SUN_MORNING -> palette.snoozeAmber
        PixelSpriteType.MOON_REST -> palette.primary
        PixelSpriteType.LEAF_WELLNESS -> palette.wellnessGreen
        PixelSpriteType.HEART_CARE -> palette.alarmCoral
        PixelSpriteType.ALARM_BELL -> palette.alarmCoral
        PixelSpriteType.BOOK_FOCUS -> palette.primary
        PixelSpriteType.CALENDAR_GRID -> palette.secondary
        PixelSpriteType.SPARKLE_OK -> palette.wellnessGreen
        PixelSpriteType.DUMBBELL_FITNESS -> palette.alarmCoral
        PixelSpriteType.PILL_HEALTH -> palette.waterCyan
        PixelSpriteType.COFFEE_MUG -> palette.snoozeAmber
        PixelSpriteType.MUSIC_NOTE -> palette.primary
        PixelSpriteType.HOME_CARE -> palette.secondary
        PixelSpriteType.PAW_PET -> palette.snoozeAmber
        PixelSpriteType.LOTUS_MIND -> palette.wellnessGreen
        PixelSpriteType.GAMEPAD_HOBBY -> palette.accent
    }
    val highlightColor = Color.White.copy(alpha = 0.85f)

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 8f
        val cellH = size.height / 8f
        grid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    '1' -> mainColor
                    '2' -> highlightColor
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

fun PixelSpriteType.pixelGrid(): List<String> = when (this) {
    PixelSpriteType.WATER_DROP -> listOf(
        "...11...",
        "..1111..",
        ".121111.",
        ".121111.",
        "11211111",
        "11111111",
        ".111111.",
        "..1111.."
    )
    PixelSpriteType.SUN_MORNING -> listOf(
        "1..11..1",
        ".112211.",
        "..2222..",
        "11222211",
        "11222211",
        "..2222..",
        ".112211.",
        "1..11..1"
    )
    PixelSpriteType.MOON_REST -> listOf(
        "..1111..",
        ".11221..",
        "1122....",
        "112.....",
        "112.....",
        "1122....",
        ".11221..",
        "..1111.."
    )
    PixelSpriteType.LEAF_WELLNESS -> listOf(
        "....1111",
        "..111221",
        ".1122111",
        ".121111.",
        ".11111..",
        "..111...",
        ".11.....",
        "1......."
    )
    PixelSpriteType.HEART_CARE -> listOf(
        ".11..11.",
        "12211111",
        "12111111",
        "11111111",
        ".111111.",
        "..1111..",
        "...11...",
        "........"
    )
    PixelSpriteType.ALARM_BELL -> listOf(
        "11....11",
        ".111111.",
        ".122111.",
        "11211111",
        "11111111",
        "11111111",
        "..1..1..",
        "........"
    )
    PixelSpriteType.BOOK_FOCUS -> listOf(
        ".111111.",
        "12222211",
        "12111211",
        "12222211",
        "12111211",
        "12222211",
        "11111111",
        ".111111."
    )
    PixelSpriteType.CALENDAR_GRID -> listOf(
        ".1.11.1.",
        "11111111",
        "12222221",
        "12112121",
        "12222221",
        "12122121",
        "12222221",
        "11111111"
    )
    PixelSpriteType.SPARKLE_OK -> listOf(
        "...11...",
        "...11...",
        ".112211.",
        "11222211",
        "11222211",
        ".112211.",
        "...11...",
        "...11..."
    )
    PixelSpriteType.DUMBBELL_FITNESS -> listOf(
        ".11..11.",
        "121..121",
        "11122111",
        "11122111",
        "121..121",
        ".11..11.",
        "........",
        "........"
    )
    PixelSpriteType.PILL_HEALTH -> listOf(
        "....111.",
        "...12211",
        "..121111",
        ".11111..",
        "12211...",
        "1221....",
        ".11.....",
        "........"
    )
    PixelSpriteType.COFFEE_MUG -> listOf(
        "..2..2..",
        ".2..2...",
        "111111..",
        "12222111",
        "111111.1",
        "11111111",
        ".11111..",
        "1111111."
    )
    PixelSpriteType.MUSIC_NOTE -> listOf(
        "..111111",
        "..122221",
        "..11...1",
        "..11...1",
        ".111..11",
        "1211.121",
        "111..111",
        "........"
    )
    PixelSpriteType.HOME_CARE -> listOf(
        "...11...",
        "..1221..",
        ".112211.",
        "11111111",
        ".122221.",
        ".121121.",
        ".121121.",
        ".111111."
    )
    PixelSpriteType.PAW_PET -> listOf(
        ".11..11.",
        "121..121",
        "....11..",
        "11..121.",
        "121111..",
        ".112211.",
        ".122221.",
        "..1111.."
    )
    PixelSpriteType.LOTUS_MIND -> listOf(
        "...11...",
        ".1.12.1.",
        "11122111",
        "12122121",
        "11111111",
        ".112211.",
        "..1111..",
        "...22..."
    )
    PixelSpriteType.GAMEPAD_HOBBY -> listOf(
        "........",
        ".111111.",
        "11211121",
        "12221211",
        "11211111",
        "111..111",
        ".1....1.",
        "........"
    )
}

fun PixelSpriteType.defaultMainColorArgb(): Int = when (this) {
    PixelSpriteType.WATER_DROP -> 0xFF38BDF8.toInt()
    PixelSpriteType.SUN_MORNING -> 0xFFF59E0B.toInt()
    PixelSpriteType.MOON_REST -> 0xFF818CF8.toInt()
    PixelSpriteType.LEAF_WELLNESS -> 0xFF34D399.toInt()
    PixelSpriteType.HEART_CARE -> 0xFFF43F5E.toInt()
    PixelSpriteType.ALARM_BELL -> 0xFFFB7185.toInt()
    PixelSpriteType.BOOK_FOCUS -> 0xFFA78BFA.toInt()
    PixelSpriteType.CALENDAR_GRID -> 0xFF60A5FA.toInt()
    PixelSpriteType.SPARKLE_OK -> 0xFF10B981.toInt()
    PixelSpriteType.DUMBBELL_FITNESS -> 0xFFF97316.toInt()
    PixelSpriteType.PILL_HEALTH -> 0xFF2DD4BF.toInt()
    PixelSpriteType.COFFEE_MUG -> 0xFFD97706.toInt()
    PixelSpriteType.MUSIC_NOTE -> 0xFFC084FC.toInt()
    PixelSpriteType.HOME_CARE -> 0xFF60A5FA.toInt()
    PixelSpriteType.PAW_PET -> 0xFFFBBF24.toInt()
    PixelSpriteType.LOTUS_MIND -> 0xFF4ADE80.toInt()
    PixelSpriteType.GAMEPAD_HOBBY -> 0xFFF472B6.toInt()
}

/**
 * Renders the 8x8 PixelSpriteType into a crisp Android Bitmap with a retro pixel frame
 * for use as the notification LargeIcon.
 */
fun PixelSpriteType.createNotificationLargeBitmap(sizePx: Int = 128): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(
        sizePx,
        sizePx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
    }

    val borderPx = (sizePx * 0.06f).coerceAtLeast(3f)
    val padPx = (sizePx * 0.16f).coerceAtLeast(8f)

    // Outer pixel border
    paint.color = 0xFF1E1E2E.toInt()
    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

    // Inner dark badge background
    paint.color = 0xFF282A3A.toInt()
    canvas.drawRect(
        borderPx,
        borderPx,
        sizePx - borderPx,
        sizePx - borderPx,
        paint
    )

    val grid = pixelGrid()
    val mainColor = defaultMainColorArgb()
    val highlightColor = 0xEEFFFFFF.toInt()
    val cellW = (sizePx - padPx * 2f) / 8f
    val cellH = (sizePx - padPx * 2f) / 8f

    grid.forEachIndexed { rowIdx, rowStr ->
        rowStr.forEachIndexed { colIdx, ch ->
            when (ch) {
                '1' -> paint.color = mainColor
                '2' -> paint.color = highlightColor
                else -> paint.color = 0
            }
            if (paint.color != 0) {
                val left = padPx + colIdx * cellW
                val top = padPx + rowIdx * cellH
                canvas.drawRect(
                    left,
                    top,
                    left + cellW + 0.5f,
                    top + cellH + 0.5f,
                    paint
                )
            }
        }
    }

    return bmp
}

/**
 * Renders a monochrome silhouette Android Bitmap of the 8x8 PixelSpriteType
 * for use with IconCompat.createWithBitmap(...) as the notification SmallIcon.
 */
fun PixelSpriteType.createNotificationSmallBitmap(sizePx: Int = 64): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(
        sizePx,
        sizePx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    val padPx = sizePx * 0.08f
    val cellW = (sizePx - padPx * 2f) / 8f
    val cellH = (sizePx - padPx * 2f) / 8f

    pixelGrid().forEachIndexed { rowIdx, rowStr ->
        rowStr.forEachIndexed { colIdx, ch ->
            if (ch == '1' || ch == '2') {
                val left = padPx + colIdx * cellW
                val top = padPx + rowIdx * cellH
                canvas.drawRect(
                    left,
                    top,
                    left + cellW + 0.5f,
                    top + cellH + 0.5f,
                    paint
                )
            }
        }
    }
    return bmp
}

/**
 * Daily completion medal tiers:
 * - BRONZE: 50%+ of daily routine check-ins completed
 * - SILVER: 75%+ of daily routine check-ins completed
 * - GOLD: 100% of daily routine check-ins completed (all tasks of the day done!)
 */
enum class PixelMedalTier(
    val thresholdPercent: Int,
    val titlePt: String,
    val shortLabelPt: String,
    val encouragementPt: String,
    val discMainColor: Color,
    val discHighlightColor: Color,
    val discShadowColor: Color,
    val ribbonColor: Color
) {
    NONE(
        thresholdPercent = 0,
        titlePt = "Sem Medalha",
        shortLabelPt = "META 50%",
        encouragementPt = "Complete 50% das tarefas de hoje para conquistar a Medalha de Bronze!",
        discMainColor = Color(0xFF565B75),
        discHighlightColor = Color(0xFF7B819E),
        discShadowColor = Color(0xFF393D52),
        ribbonColor = Color(0xFF4A4E69)
    ),
    BRONZE(
        thresholdPercent = 50,
        titlePt = "Medalha de Bronze",
        shortLabelPt = "BRONZE • 50%",
        encouragementPt = "Medalha de Bronze conquistada (50%+)! Rumo à Prata (75%)!",
        discMainColor = Color(0xFFD9823B),
        discHighlightColor = Color(0xFFF6B27A),
        discShadowColor = Color(0xFF8F4E19),
        ribbonColor = Color(0xFF38BDF8)
    ),
    SILVER(
        thresholdPercent = 75,
        titlePt = "Medalha de Prata",
        shortLabelPt = "PRATA • 75%",
        encouragementPt = "Medalha de Prata conquistada (75%+)! Falta pouco para o Ouro (100%)!",
        discMainColor = Color(0xFFC7D2E6),
        discHighlightColor = Color(0xFFF8FAFC),
        discShadowColor = Color(0xFF7C8BA6),
        ribbonColor = Color(0xFFA78BFA)
    ),
    GOLD(
        thresholdPercent = 100,
        titlePt = "Medalha de Ouro",
        shortLabelPt = "OURO • 100%",
        encouragementPt = "Dia 100% Completo! Você conquistou a Medalha de Ouro de hoje!",
        discMainColor = Color(0xFFFACC15),
        discHighlightColor = Color(0xFFFEF08A),
        discShadowColor = Color(0xFFCA8A04),
        ribbonColor = Color(0xFFF43F5E)
    );

    companion object {
        fun fromCompletionPercent(percent: Int, hasActiveRoutines: Boolean = true): PixelMedalTier {
            if (!hasActiveRoutines) return NONE
            return when {
                percent >= 100 -> GOLD
                percent >= 75 -> SILVER
                percent >= 50 -> BRONZE
                else -> NONE
            }
        }
    }
}

/**
 * 10x10 8-bit Pixel Art Medal Icon with V-ribbon, metallic rim, and center star emblem.
 */
@Composable
fun PixelMedalIcon(
    tier: PixelMedalTier,
    isUnlocked: Boolean = tier != PixelMedalTier.NONE,
    modifier: Modifier = Modifier.size(34.dp),
    description: String = tier.titlePt
) {
    val palette = LocalPixelPalette.current
    val effectiveTier = if (isUnlocked) tier else PixelMedalTier.NONE

    // 10x10 grid:
    // 'R' = Ribbon, 'B' = Dark Pixel Outline, 'M' = Medal Main, 'H' = Highlight/Star, 'S' = Shadow
    val medalGrid = listOf(
        ".RR....RR.",
        ".RRR..RRR.",
        "..RRRRRR..",
        "...BBBB...",
        "..BHHMMB..",
        ".BHMHHMMB.",
        ".BMHHHHMB.",
        ".BMMHHSSB.",
        "..BMSSSB..",
        "...BBBB..."
    )

    val ribbon = if (isUnlocked) effectiveTier.ribbonColor else palette.textSecondary.copy(alpha = 0.25f)
    val main = if (isUnlocked) effectiveTier.discMainColor else palette.textSecondary.copy(alpha = 0.22f)
    val highlight = if (isUnlocked) effectiveTier.discHighlightColor else palette.textSecondary.copy(alpha = 0.35f)
    val shadow = if (isUnlocked) effectiveTier.discShadowColor else palette.textSecondary.copy(alpha = 0.16f)
    val outline = if (isUnlocked) palette.pixelBorder else palette.pixelBorder.copy(alpha = 0.35f)

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 10f
        val cellH = size.height / 10f

        medalGrid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    'R' -> ribbon
                    'B' -> outline
                    'M' -> main
                    'H' -> highlight
                    'S' -> shadow
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }

        // Extra 8-bit corner sparkles when Gold (100%) is unlocked
        if (isUnlocked && tier == PixelMedalTier.GOLD) {
            drawRect(
                color = effectiveTier.discHighlightColor,
                topLeft = Offset(0f, 4f * cellH),
                size = Size(cellW, cellH)
            )
            drawRect(
                color = effectiveTier.discHighlightColor,
                topLeft = Offset(9f * cellW, 3f * cellH),
                size = Size(cellW, cellH)
            )
        }
    }
}

/**
 * 8x8 Pixel Art Streak Flame Icon for consecutive consistency streaks.
 */
@Composable
fun PixelStreakFlameIcon(
    isActive: Boolean,
    modifier: Modifier = Modifier.size(26.dp),
    description: String = "Sequência de consistência"
) {
    val palette = LocalPixelPalette.current
    val flameGrid = listOf(
        "....1...",
        "...111..",
        "..11211.",
        ".112221.",
        "11223211",
        "11233311",
        ".113311.",
        "..1111.."
    )

    val outerColor = if (isActive) Color(0xFFF97316) else palette.textSecondary.copy(alpha = 0.32f)
    val midColor = if (isActive) Color(0xFFFACC15) else palette.textSecondary.copy(alpha = 0.22f)
    val coreColor = if (isActive) Color(0xFFFEF08A) else palette.textSecondary.copy(alpha = 0.45f)

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 8f
        val cellH = size.height / 8f
        flameGrid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    '1' -> outerColor
                    '2' -> midColor
                    '3' -> coreColor
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

fun moodLabelPt(level: Int): String = when (level.coerceIn(1, 5)) {
    1 -> "Esgotado"
    2 -> "Baixo"
    3 -> "Estável"
    4 -> "Bem"
    else -> "Radiante"
}

fun energyLabelPt(level: Int): String = when (level.coerceIn(1, 5)) {
    1 -> "Crítica"
    2 -> "Baixa"
    3 -> "Média"
    4 -> "Alta"
    else -> "Máxima"
}

/**
 * 8x8 Pixel Art Mood Face Icon (levels 1 to 5).
 */
@Composable
fun PixelMoodIcon(
    level: Int,
    modifier: Modifier = Modifier.size(28.dp),
    tintOverride: Color? = null,
    description: String = "Humor: ${moodLabelPt(level)}"
) {
    val palette = LocalPixelPalette.current
    val clamped = level.coerceIn(1, 5)
    val mainColor = tintOverride ?: when (clamped) {
        1 -> palette.alarmCoral
        2 -> palette.snoozeAmber
        3 -> palette.waterCyan
        4 -> palette.secondary
        else -> palette.wellnessGreen
    }
    val outlineColor = palette.pixelBorder
    val featureColor = Color(0xFF1A1B26)
    val highlightColor = Color.White.copy(alpha = 0.88f)

    // 8x8 grid: 'B' = border, '1' = face fill, '2' = eyes/mouth, 'H' = highlight
    val grid = when (clamped) {
        1 -> listOf(
            "..BBBB..",
            ".B1111B.",
            "B121121B",
            "B111111B",
            "B112211B",
            "B121121B",
            ".B1111B.",
            "..BBBB.."
        )
        2 -> listOf(
            "..BBBB..",
            ".B1111B.",
            "B121121B",
            "B111111B",
            "B112211B",
            "B121111B",
            ".B1111B.",
            "..BBBB.."
        )
        3 -> listOf(
            "..BBBB..",
            ".BH111B.",
            "B121121B",
            "B111111B",
            "B122221B",
            "B111111B",
            ".B1111B.",
            "..BBBB.."
        )
        4 -> listOf(
            "..BBBB..",
            ".BH111B.",
            "B121121B",
            "B111111B",
            "B121121B",
            "B112211B",
            ".B1111B.",
            "..BBBB.."
        )
        else -> listOf(
            "..BBBB..",
            ".BHH11B.",
            "B121121B",
            "BH1111HB",
            "B122221B",
            "B112211B",
            ".B1111B.",
            "..BBBB.."
        )
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 8f
        val cellH = size.height / 8f
        grid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    'B' -> outlineColor
                    '1' -> mainColor
                    '2' -> featureColor
                    'H' -> highlightColor
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 8x8 Pixel Art Energy Battery Icon filling from 1 bar (alert red) to 5 bars (full green).
 */
@Composable
fun PixelEnergyIcon(
    level: Int,
    modifier: Modifier = Modifier.size(28.dp),
    tintOverride: Color? = null,
    description: String = "Energia: ${energyLabelPt(level)} ($level de 5)"
) {
    val palette = LocalPixelPalette.current
    val clamped = level.coerceIn(1, 5)
    val barColor = tintOverride ?: when (clamped) {
        1 -> palette.alarmCoral
        2 -> Color(0xFFF97316)
        3 -> palette.snoozeAmber
        4 -> palette.secondary
        else -> palette.wellnessGreen
    }
    val emptySlotColor = palette.pixelBorder.copy(alpha = 0.16f)
    val highlightColor = Color.White.copy(alpha = 0.72f)
    val outlineColor = palette.pixelBorder

    // 8x8 vertical battery: row 6 = bar 1 (bottom) .. row 2 = bar 5 (top)
    val grid = listOf(
        "..BBBB..",
        ".BBBBBB.",
        ".B5555B.",
        ".B4444B.",
        ".B3333B.",
        ".B2222B.",
        ".B1111B.",
        ".BBBBBB."
    )

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 8f
        val cellH = size.height / 8f
        grid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    'B' -> outlineColor
                    '1', '2', '3', '4', '5' -> {
                        val barIndex = ch.digitToInt()
                        if (barIndex <= clamped) {
                            if (colIdx == 2) highlightColor else barColor
                        } else {
                            emptySlotColor
                        }
                    }
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 8x8 Pixel Art Dice Icon for rolling a writing prompt.
 */
@Composable
fun PixelDiceIcon(
    modifier: Modifier = Modifier.size(22.dp),
    tintOverride: Color? = null,
    description: String = "Sortear gatilho de escrita"
) {
    val palette = LocalPixelPalette.current
    val borderColor = palette.pixelBorder
    val bodyColor = tintOverride ?: palette.surfaceElevated
    val pipColor = palette.primary

    val grid = listOf(
        ".BBBBBB.",
        "B111111B",
        "B1P11P1B",
        "B111111B",
        "B11PP11B",
        "B1P11P1B",
        "B111111B",
        ".BBBBBB."
    )

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 8f
        val cellH = size.height / 8f
        grid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    'B' -> borderColor
                    '1' -> bodyColor
                    'P' -> pipColor
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 8x8 Pixel Art Padlock Icon for Stealth Mode and locked Time Capsules.
 */
@Composable
fun PixelLockIcon(
    isLocked: Boolean = true,
    modifier: Modifier = Modifier.size(22.dp),
    tintOverride: Color? = null,
    description: String = if (isLocked) "Protegido com cadeado" else "Desbloqueado"
) {
    val palette = LocalPixelPalette.current
    val borderColor = palette.pixelBorder
    val bodyColor = tintOverride ?: if (isLocked) palette.snoozeAmber else palette.wellnessGreen
    val shackleColor = palette.textSecondary
    val keyholeColor = Color(0xFF1A1B26)

    val grid = if (isLocked) {
        listOf(
            "..SSSS..",
            ".S....S.",
            ".S....S.",
            "BBBBBBBB",
            "B111111B",
            "B11KK11B",
            "B11KK11B",
            "BBBBBBBB"
        )
    } else {
        listOf(
            "...SSSS.",
            "..S....S",
            ".......S",
            "BBBBBBBB",
            "B111111B",
            "B11KK11B",
            "B11KK11B",
            "BBBBBBBB"
        )
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = description }
    ) {
        val cellW = size.width / 8f
        val cellH = size.height / 8f
        grid.forEachIndexed { rowIdx, rowStr ->
            rowStr.forEachIndexed { colIdx, ch ->
                val color = when (ch) {
                    'B' -> borderColor
                    'S' -> shackleColor
                    '1' -> bodyColor
                    'K' -> keyholeColor
                    else -> null
                }
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Renders the 8x8 Pixel Art Mood Face (levels 1 to 5) into a crisp anti-alias-free Android Bitmap
 * for full-art custom pixel notifications.
 */
fun createPixelMoodBitmap(
    level: Int,
    sizePx: Int = 64
): android.graphics.Bitmap {
    val clamped = level.coerceIn(1, 5)
    val bmp = android.graphics.Bitmap.createBitmap(
        sizePx,
        sizePx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
    }

    val mainColor = when (clamped) {
        1 -> 0xFFF38BA8.toInt() // Coral (Esgotado)
        2 -> 0xFFFAB387.toInt() // Amber/Peach (Baixo)
        3 -> 0xFF74C7EC.toInt() // Cyan (Estável)
        4 -> 0xFFCBA6F7.toInt() // Lavender (Bem)
        else -> 0xFFA6E3A1.toInt() // Mint Green (Radiante)
    }
    val outlineColor = 0xFF11111B.toInt()
    val featureColor = 0xFF181825.toInt()
    val highlightColor = 0xEBFFFFFF.toInt()

    val grid = when (clamped) {
        1 -> listOf(
            "..BBBB..",
            ".B1111B.",
            "B121121B",
            "B111111B",
            "B112211B",
            "B121121B",
            ".B1111B.",
            "..BBBB.."
        )
        2 -> listOf(
            "..BBBB..",
            ".B1111B.",
            "B121121B",
            "B111111B",
            "B112211B",
            "B121111B",
            ".B1111B.",
            "..BBBB.."
        )
        3 -> listOf(
            "..BBBB..",
            ".BH111B.",
            "B121121B",
            "B111111B",
            "B122221B",
            "B111111B",
            ".B1111B.",
            "..BBBB.."
        )
        4 -> listOf(
            "..BBBB..",
            ".BH111B.",
            "B121121B",
            "B111111B",
            "B121121B",
            "B112211B",
            ".B1111B.",
            "..BBBB.."
        )
        else -> listOf(
            "..BBBB..",
            ".BHH11B.",
            "B121121B",
            "BH1111HB",
            "B122221B",
            "B112211B",
            ".B1111B.",
            "..BBBB.."
        )
    }

    val cellW = sizePx / 8f
    val cellH = sizePx / 8f
    grid.forEachIndexed { rowIdx, rowStr ->
        rowStr.forEachIndexed { colIdx, ch ->
            val color = when (ch) {
                'B' -> outlineColor
                '1' -> mainColor
                '2' -> featureColor
                'H' -> highlightColor
                else -> 0
            }
            if (color != 0) {
                paint.color = color
                val left = colIdx * cellW
                val top = rowIdx * cellH
                canvas.drawRect(left, top, left + cellW + 0.5f, top + cellH + 0.5f, paint)
            }
        }
    }
    return bmp
}

/**
 * Renders the 8x8 Pixel Art Energy Battery (levels 1 to 5) into a crisp anti-alias-free Android Bitmap
 * for full-art custom pixel notifications.
 */
fun createPixelEnergyBitmap(
    level: Int,
    sizePx: Int = 64
): android.graphics.Bitmap {
    val clamped = level.coerceIn(1, 5)
    val bmp = android.graphics.Bitmap.createBitmap(
        sizePx,
        sizePx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
    }

    val barColor = when (clamped) {
        1 -> 0xFFF38BA8.toInt() // Crítica (Coral)
        2 -> 0xFFF97316.toInt() // Baixa (Orange)
        3 -> 0xFFF9E2AF.toInt() // Média (Amber)
        4 -> 0xFF89B4FA.toInt() // Alta (Sky Blue)
        else -> 0xFFA6E3A1.toInt() // Máxima (Mint Green)
    }
    val outlineColor = 0xFF11111B.toInt()
    val emptySlotColor = 0x38FFFFFF
    val highlightColor = 0xC4FFFFFF.toInt()

    val grid = listOf(
        "..BBBB..",
        ".BBBBBB.",
        ".B5555B.",
        ".B4444B.",
        ".B3333B.",
        ".B2222B.",
        ".B1111B.",
        ".BBBBBB."
    )

    val cellW = sizePx / 8f
    val cellH = sizePx / 8f
    grid.forEachIndexed { rowIdx, rowStr ->
        rowStr.forEachIndexed { colIdx, ch ->
            val color = when (ch) {
                'B' -> outlineColor
                '1', '2', '3', '4', '5' -> {
                    val barIndex = ch.digitToInt()
                    if (barIndex <= clamped) {
                        if (colIdx == 2) highlightColor else barColor
                    } else {
                        emptySlotColor
                    }
                }
                else -> 0
            }
            if (color != 0) {
                paint.color = color
                val left = colIdx * cellW
                val top = rowIdx * cellH
                canvas.drawRect(left, top, left + cellW + 0.5f, top + cellH + 0.5f, paint)
            }
        }
    }
    return bmp
}

/**
 * Renders the 10x10 Pixel Art Medal (None/Bronze/Silver/Gold) into an anti-alias-free Android Bitmap
 * for home screen widgets and custom notifications.
 */
fun createPixelMedalBitmap(
    tier: PixelMedalTier,
    isUnlocked: Boolean = tier != PixelMedalTier.NONE,
    sizePx: Int = 80
): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(
        sizePx,
        sizePx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
    }

    val effectiveTier = if (isUnlocked) tier else PixelMedalTier.NONE
    val medalGrid = listOf(
        ".RR....RR.",
        ".RRR..RRR.",
        "..RRRRRR..",
        "...BBBB...",
        "..BHHMMB..",
        ".BHMHHMMB.",
        ".BMHHHHMB.",
        ".BMMHHSSB.",
        "..BMSSSB..",
        "...BBBB..."
    )

    val ribbon = if (isUnlocked) effectiveTier.ribbonColor.toArgb() else 0x5594A3B8
    val main = if (isUnlocked) effectiveTier.discMainColor.toArgb() else 0x5564748B
    val highlight = if (isUnlocked) effectiveTier.discHighlightColor.toArgb() else 0x77CBD5E1
    val shadow = if (isUnlocked) effectiveTier.discShadowColor.toArgb() else 0x44475569
    val outline = if (isUnlocked) 0xFF11111B.toInt() else 0x8811111B.toInt()

    val cellW = sizePx / 10f
    val cellH = sizePx / 10f
    medalGrid.forEachIndexed { rowIdx, rowStr ->
        rowStr.forEachIndexed { colIdx, ch ->
            val color = when (ch) {
                'R' -> ribbon
                'B' -> outline
                'M' -> main
                'H' -> highlight
                'S' -> shadow
                else -> 0
            }
            if (color != 0) {
                paint.color = color
                val left = colIdx * cellW
                val top = rowIdx * cellH
                canvas.drawRect(left, top, left + cellW + 0.5f, top + cellH + 0.5f, paint)
            }
        }
    }

    if (isUnlocked && tier == PixelMedalTier.GOLD) {
        paint.color = effectiveTier.discHighlightColor.toArgb()
        canvas.drawRect(0f, 4f * cellH, cellW, 5f * cellH, paint)
        canvas.drawRect(9f * cellW, 3f * cellH, 10f * cellW, 4f * cellH, paint)
    }

    return bmp
}

/**
 * Renders the 8x8 Pixel Art Streak Flame into an anti-alias-free Android Bitmap for widgets.
 */
fun createPixelStreakFlameBitmap(
    isActive: Boolean,
    sizePx: Int = 64
): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(
        sizePx,
        sizePx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
    }

    val flameGrid = listOf(
        "....1...",
        "...111..",
        "..11211.",
        ".112221.",
        "11223211",
        "11233311",
        ".113311.",
        "..1111.."
    )

    val outerColor = if (isActive) 0xFFF97316.toInt() else 0x6694A3B8
    val midColor = if (isActive) 0xFFFACC15.toInt() else 0x4464748B
    val coreColor = if (isActive) 0xFFFEF08A.toInt() else 0x88CBD5E1.toInt()

    val cellW = sizePx / 8f
    val cellH = sizePx / 8f
    flameGrid.forEachIndexed { rowIdx, rowStr ->
        rowStr.forEachIndexed { colIdx, ch ->
            val color = when (ch) {
                '1' -> outerColor
                '2' -> midColor
                '3' -> coreColor
                else -> 0
            }
            if (color != 0) {
                paint.color = color
                val left = colIdx * cellW
                val top = rowIdx * cellH
                canvas.drawRect(left, top, left + cellW + 0.5f, top + cellH + 0.5f, paint)
            }
        }
    }
    return bmp
}

/**
 * Renders a crisp segmented 8-bit pixel progress bar Bitmap for home screen widgets.
 */
fun createPixelProgressBarBitmap(
    percent: Int,
    widthPx: Int = 360,
    heightPx: Int = 22,
    fillColorArgb: Int = 0xFFA6E3A1.toInt(),
    borderColorArgb: Int = 0xFF11111B.toInt(),
    bgColorArgb: Int = 0xFF1E2030.toInt()
): android.graphics.Bitmap {
    val clamped = percent.coerceIn(0, 100)
    val bmp = android.graphics.Bitmap.createBitmap(
        widthPx,
        heightPx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = false
        style = android.graphics.Paint.Style.FILL
    }

    val border = 3f
    // Outer pixel border
    paint.color = borderColorArgb
    canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), paint)

    // Inner opaque dark track
    paint.color = bgColorArgb
    canvas.drawRect(border, border, widthPx - border, heightPx - border, paint)

    // 10 crisp pixel segments
    val segments = 10
    val filledSegments = ((clamped + 5) / 10).coerceIn(0, segments)
    val innerW = widthPx - border * 2f - 4f
    val segGap = 3f
    val segW = (innerW - segGap * (segments - 1)) / segments
    val top = border + 2f
    val bottom = heightPx - border - 2f
    val highlightArgb = androidx.core.graphics.ColorUtils.blendARGB(
        fillColorArgb,
        0xFFFFFFFF.toInt(),
        0.38f
    )
    val emptySlotArgb = 0xFF31354D.toInt()

    for (i in 0 until segments) {
        val left = border + 2f + i * (segW + segGap)
        if (i < filledSegments) {
            paint.color = fillColorArgb
            canvas.drawRect(left, top, left + segW, bottom, paint)
            // Retro top highlight line
            paint.color = highlightArgb
            canvas.drawRect(left, top, left + segW, top + 3f, paint)
        } else {
            paint.color = emptySlotArgb
            canvas.drawRect(left, top, left + segW, bottom, paint)
        }
    }
    return bmp
}


