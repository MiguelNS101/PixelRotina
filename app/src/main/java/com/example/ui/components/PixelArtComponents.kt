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

enum class PixelSpriteType {
    WATER_DROP,
    SUN_MORNING,
    MOON_REST,
    LEAF_WELLNESS,
    HEART_CARE,
    ALARM_BELL,
    BOOK_FOCUS,
    CALENDAR_GRID,
    SPARKLE_OK
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
    val grid: List<String> = when (spriteType) {
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
    }

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
