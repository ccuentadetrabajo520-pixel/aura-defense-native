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
    val eyeBlink = remember { Animatable(1f) }
    val eventWave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3500)
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
        val eyeR = coreR * 0.45f
        listOf(-1f, 1f).forEach { side ->
            val eyeCenter = Offset(c.x + side * coreR * 0.38f, c.y - coreR * 0.05f)
            drawArc(
                color = accent.copy(alpha = eyeBlink.value),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(eyeCenter.x - eyeR, eyeCenter.y - eyeR),
                size = Size(eyeR * 2f, eyeR * 2f),
                style = Stroke(width = coreR * 0.10f, cap = StrokeCap.Round)
            )
            drawCircle(accent.copy(alpha = eyeBlink.value), coreR * 0.10f, Offset(eyeCenter.x, eyeCenter.y + eyeR * 0.35f))
        }
        when (mood) {
            CoreMood.SERIO -> drawLine(accent, Offset(c.x - coreR * 0.30f, c.y + coreR * 0.45f), Offset(c.x + coreR * 0.30f, c.y + coreR * 0.45f), coreR * 0.06f, StrokeCap.Round)
            CoreMood.ALERTA -> drawOval(accent, Offset(c.x - coreR * 0.12f, c.y + coreR * 0.28f), Size(coreR * 0.24f, coreR * 0.36f), style = Stroke(coreR * 0.06f))
            CoreMood.HABLANDO -> drawOval(accent, Offset(c.x - coreR * 0.22f, c.y + coreR * 0.28f), Size(coreR * 0.44f, coreR * 0.18f + coreR * 0.18f * corePulse))
            else -> drawArc(
                color = accent,
                startAngle = 25f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(c.x - coreR * 0.24f, c.y + coreR * 0.25f),
                size = Size(coreR * 0.48f, coreR * 0.30f),
                style = Stroke(width = coreR * 0.06f, cap = StrokeCap.Round)
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
