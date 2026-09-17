package com.aura.defense.ui.components.aura

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class CoreMood { BOOT, IDLE, SCANNING, THINKING, SERIO, ALERTA, ORGULLOSO, HABLANDO }

@Composable
fun AuraCoreHologram(
    mood: CoreMood,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    eventPulse: Int = 0
) {
    val cyan = Color(0xFF4DD8E6)
    val accent by animateColorAsState(
        when (mood) {
            CoreMood.SERIO -> Color(0xFFFFB300)
            CoreMood.ALERTA -> Color(0xFFE53935)
            CoreMood.ORGULLOSO -> Color(0xFF22C55E)
            else -> cyan
        }, tween(300), label = "core-accent"
    )
    val infinite = rememberInfiniteTransition(label = "core")
    val ring1 by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "ring-outer")
    val ring2 by infinite.animateFloat(360f, 0f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ring-middle")
    val ring3 by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "ring-inner")
    val corePulse by infinite.animateFloat(
        0.92f, 1.06f,
        infiniteRepeatable(tween(if (mood == CoreMood.ALERTA) 500 else 1600), RepeatMode.Reverse),
        label = "core-pulse"
    )
    val particlePhase by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "particle-phase")
    val eyeX by infinite.animateFloat(-0.8f, 0.8f, infiniteRepeatable(tween(2100), RepeatMode.Reverse), label = "eye-x")
    val eyeY by infinite.animateFloat(-0.5f, 0.5f, infiniteRepeatable(tween(3300), RepeatMode.Reverse), label = "eye-y")
    val eyeJitter by infinite.animateFloat(-0.15f, 0.15f, infiniteRepeatable(tween(80), RepeatMode.Reverse), label = "eye-jitter")
    val heartBeat by infinite.animateFloat(1f, 1.18f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "heart-beat")
    val scanPhase by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "eye-scan")
    val dataPhase by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "data-lines")
    val eyeBlink = remember { Animatable(1f) }
    val eventWave = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        while (true) {
            kotlinx.coroutines.delay(if (mood == CoreMood.HABLANDO) 1800 else 4000)
            eyeBlink.animateTo(0.1f, tween(120))
            eyeBlink.animateTo(1f, tween(120))
        }
    }
    LaunchedEffect(eventPulse) {
        if (eventPulse > 0) {
            eventWave.snapTo(0f)
            eventWave.animateTo(1f, tween(700, easing = LinearEasing))
        }
    }
    val speedFactor = if (mood == CoreMood.SCANNING) 3f else 1f

    Canvas(modifier = modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxR = this.size.minDimension / 2f
        fun glowCircle(radius: Float, color: Color, stroke: Float) {
            for (pass in 0..2) {
                drawCircle(color.copy(alpha = 0.12f * (pass + 1)), radius + pass * stroke, c, style = Stroke(stroke))
            }
        }
        listOf(0.30f, 0.48f, 0.68f, 0.92f).forEach { ratio ->
            drawCircle(cyan.copy(alpha = 0.06f), maxR * ratio, c, style = Stroke(1f))
        }
        rotate(ring1 * speedFactor, c) {
            repeat(8) { index ->
                val topLeft = Offset(c.x - maxR * 0.92f, c.y - maxR * 0.92f)
                drawArc(
                    color = accent.copy(alpha = 0.55f),
                    startAngle = index.toFloat() * 45f,
                    sweepAngle = 28f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(maxR * 1.84f, maxR * 1.84f),
                    style = Stroke(width = maxR * 0.015f, cap = StrokeCap.Round)
                )
            }
        }
        rotate(ring2 * speedFactor, c) {
            drawArc(
                color = cyan.copy(alpha = 0.35f),
                startAngle = 210f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(c.x - maxR * 0.70f, c.y - maxR * 0.70f),
                size = Size(maxR * 1.40f, maxR * 1.40f),
                style = Stroke(width = maxR * 0.008f)
            )
            listOf(0f, 120f, 240f).forEach { angle ->
                val radians = (angle + ring2 * speedFactor) * (PI.toFloat() / 180f)
                val point = Offset(c.x + maxR * 0.70f * cos(radians), c.y + maxR * 0.70f * sin(radians))
                drawCircle(Color.White.copy(alpha = 0.9f), maxR * 0.012f, point)
                drawCircle(accent.copy(alpha = 0.6f), maxR * 0.022f, point, style = Stroke(maxR * 0.004f))
            }
        }
        rotate(ring3 * speedFactor, c) {
            repeat(36) { index ->
                val radians = index * 10f * (PI.toFloat() / 180f)
                val outer = maxR * 0.52f
                val inner = maxR * if (index % 3 == 0) 0.47f else 0.495f
                drawLine(accent.copy(alpha = if (index % 3 == 0) 0.7f else 0.3f), Offset(c.x + outer * cos(radians), c.y + outer * sin(radians)), Offset(c.x + inner * cos(radians), c.y + inner * sin(radians)), maxR * 0.004f)
            }
        }
        if (eventWave.value > 0f) {
            drawCircle(cyan.copy(alpha = 0.4f * (1f - eventWave.value)), maxR * (0.3f + 0.65f * eventWave.value), c, style = Stroke(maxR * 0.02f))
        }
        val coreR = maxR * 0.26f * corePulse
        glowCircle(coreR, accent, maxR * 0.012f)
        drawCircle(Color.Black.copy(alpha = 0.55f), coreR, c)
        drawCircle(accent.copy(alpha = 0.10f), coreR, c)
        val eyeUnit = coreR / 10f
        val eyeSeparation = coreR * 0.42f
        val eyeOffsetX = when (mood) {
            CoreMood.SCANNING -> (scanPhase * 2f - 1f) * 1.8f * eyeUnit
            else -> eyeX * eyeUnit
        }
        val eyeOffsetY = when (mood) {
            CoreMood.THINKING -> -1.5f * eyeUnit + eyeJitter * eyeUnit
            CoreMood.ALERTA -> eyeJitter * eyeUnit
            else -> eyeY * eyeUnit
        }
        val scleraWidth = if (mood == CoreMood.ALERTA) 7f else 6f
        val scleraHeight = if (mood == CoreMood.ALERTA) 5f else 4f
        val irisRadius = if (mood == CoreMood.SERIO) 0.8f else 1.1f
        val irisColor = if (mood == CoreMood.ALERTA) Color(0xFFE53935) else accent
        listOf(-1f, 1f).forEach { side ->
            val eyeCenter = Offset(c.x + side * eyeSeparation + eyeOffsetX, c.y - coreR * 0.05f + eyeOffsetY)
            val eyeScaleY = eyeBlink.value
            if (mood == CoreMood.ORGULLOSO) {
                drawHeart(eyeCenter, 2f * eyeUnit * heartBeat, irisColor)
            } else {
                drawOval(
                    color = cyan.copy(alpha = 0.5f * eyeScaleY),
                    topLeft = Offset(eyeCenter.x - scleraWidth * eyeUnit / 2f, eyeCenter.y - scleraHeight * eyeUnit / 2f),
                    size = Size(scleraWidth * eyeUnit, scleraHeight * eyeUnit * eyeScaleY),
                    style = Stroke(width = 0.5f * eyeUnit)
                )
                val irisCenter = Offset(eyeCenter.x, eyeCenter.y + if (mood == CoreMood.SERIO) 0f else eyeOffsetY * 0.2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.9f), irisColor.copy(alpha = 0.8f), Color.Transparent),
                        center = irisCenter,
                        radius = irisRadius * eyeUnit
                    ),
                    radius = irisRadius * eyeUnit,
                    center = irisCenter
                )
                drawCircle(Color.Black.copy(alpha = 0.8f), 0.9f * eyeUnit, irisCenter)
                drawCircle(Color.White.copy(alpha = 0.95f), 0.4f * eyeUnit, Offset(irisCenter.x - 0.35f * eyeUnit, irisCenter.y - 0.35f * eyeUnit))
                if (mood == CoreMood.SERIO) {
                    drawLine(irisColor, Offset(eyeCenter.x - 3.2f * eyeUnit, eyeCenter.y - 1.2f * eyeUnit), Offset(eyeCenter.x + 3.2f * eyeUnit, eyeCenter.y - 1.2f * eyeUnit), 0.7f * eyeUnit, StrokeCap.Round)
                }
                if (mood == CoreMood.SCANNING) {
                    drawArc(
                        color = cyan.copy(alpha = 0.55f),
                        startAngle = scanPhase * 360f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(eyeCenter.x - 4f * eyeUnit, eyeCenter.y - 4f * eyeUnit),
                        size = Size(8f * eyeUnit, 8f * eyeUnit),
                        style = Stroke(width = 0.35f * eyeUnit)
                    )
                }
            }
        }
        if (mood == CoreMood.SCANNING) {
            repeat(3) { index ->
                val y = c.y - coreR * 0.42f + index * coreR * 0.42f
                val x = c.x - coreR * 0.62f + ((dataPhase + index * 0.28f) % 1f) * coreR * 1.24f
                drawLine(cyan.copy(alpha = 0.3f), Offset(x, y), Offset(x + coreR * 0.25f, y), coreR * 0.018f, StrokeCap.Round)
            }
        }
        if (mood == CoreMood.THINKING || mood == CoreMood.SCANNING) {
            repeat(20) { index ->
                rotate(ring2 * 0.12f, c) {
                    drawArc(
                        color = accent.copy(alpha = 0.35f),
                        startAngle = index.toFloat() * 18f,
                        sweepAngle = 4f,
                        useCenter = false,
                        topLeft = Offset(c.x - coreR * 0.82f, c.y - coreR * 0.82f),
                        size = Size(coreR * 1.64f, coreR * 1.64f),
                        style = Stroke(width = coreR * 0.025f)
                    )
                }
            }
        }
        when (mood) {
            CoreMood.SERIO -> drawLine(accent, Offset(c.x - coreR * 0.30f, c.y + coreR * 0.45f), Offset(c.x + coreR * 0.30f, c.y + coreR * 0.45f), coreR * 0.06f, StrokeCap.Round)
            CoreMood.ALERTA -> drawOval(accent, Offset(c.x - coreR * 0.12f, c.y + coreR * 0.28f), Size(coreR * 0.24f, coreR * 0.36f), style = Stroke(coreR * 0.06f))
            CoreMood.HABLANDO -> {
                drawOval(accent.copy(alpha = 0.3f), Offset(c.x - coreR * 0.22f, c.y + coreR * 0.28f), Size(coreR * 0.44f, coreR * (0.15f + 0.45f * corePulse)))
                drawOval(accent, Offset(c.x - coreR * 0.22f, c.y + coreR * 0.28f), Size(coreR * 0.44f, coreR * (0.15f + 0.45f * corePulse)), style = Stroke(coreR * 0.05f))
            }
            CoreMood.ORGULLOSO -> drawArc(
                color = accent,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(c.x - coreR * 0.30f, c.y + coreR * 0.20f),
                size = Size(coreR * 0.60f, coreR * 0.44f),
                style = Stroke(width = coreR * 0.06f, cap = StrokeCap.Round)
            )
            else -> drawArc(
                color = accent,
                startAngle = 45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(c.x - coreR * 0.25f, c.y + coreR * 0.20f),
                size = Size(coreR * 0.50f, coreR * 0.38f),
                style = Stroke(width = coreR * 0.05f, cap = StrokeCap.Round)
            )
        }
        repeat(10) { index ->
            val direction = if (index % 2 == 0) 1f else -1.4f
            val angle = (particlePhase * 360f * direction + index * 36f) * (PI.toFloat() / 180f)
            val radius = maxR * (0.58f + 0.35f * sin(index.toFloat() * 1.7f))
            val alpha = 0.35f + 0.3f * kotlin.math.abs(sin(index.toFloat() + particlePhase * 6.28f))
            drawCircle(accent.copy(alpha = alpha), maxR * 0.006f, Offset(c.x + radius * cos(angle), c.y + radius * sin(angle)))
        }
        if (mood == CoreMood.SCANNING) {
            rotate(ring1 * 2f, c) {
                drawArc(
                    color = cyan.copy(alpha = 0.08f),
                    startAngle = -20f,
                    sweepAngle = 40f,
                    useCenter = true,
                    topLeft = Offset(c.x - maxR * 0.92f, c.y - maxR * 0.92f),
                    size = Size(maxR * 1.84f, maxR * 1.84f)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + radius * 1.5f)
        cubicTo(
            center.x - radius * 2f, center.y,
            center.x - radius, center.y - radius * 1.6f,
            center.x, center.y - radius * 0.5f
        )
        cubicTo(
            center.x + radius, center.y - radius * 1.6f,
            center.x + radius * 2f, center.y,
            center.x, center.y + radius * 1.5f
        )
    }
    drawPath(
        path,
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), color, Color.Transparent), center, radius * 2f)
    )
}
