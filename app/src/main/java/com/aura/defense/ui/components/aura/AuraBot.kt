package com.aura.defense.ui.components.aura

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin


enum class BotMood { IDLE, SCANNING, THINKING, SERIO, ALERTA, ORGULLOSO, HABLANDO }

@Composable
fun AuraBot(
    mood: BotMood,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    isSpeaking: Boolean = false,
    pointAtTerminalEvent: Int = 0
) {
    val floatTransition = rememberInfiniteTransition(label = "bot-float")
    val floatPhase by floatTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3400), RepeatMode.Reverse), label = "float"
    )
    val blinkTransition = rememberInfiniteTransition(label = "bot-blink")
    val blinkScale by blinkTransition.animateFloat(
        1f, 0.08f,
        infiniteRepeatable(tween(120, delayMillis = 3500), RepeatMode.Reverse),
        label = "blink"
    )
    val pulseTransition = rememberInfiniteTransition(label = "bot-pulse")
    val pulse by pulseTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(if (mood == BotMood.ALERTA) 500 else 1400), RepeatMode.Reverse), label = "pulse"
    )
    val particleTransition = rememberInfiniteTransition(label = "bot-particles")
    val particlePhase by particleTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(8000), RepeatMode.Restart), label = "particles"
    )
    val pointingAngle = remember { Animatable(15f) }
    var pointing by remember { mutableStateOf(false) }
    LaunchedEffect(pointAtTerminalEvent) {
        if (pointAtTerminalEvent > 0 && !pointing) {
            pointing = true
            pointingAngle.animateTo(-80f, tween(250, easing = FastOutSlowInEasing))
            kotlinx.coroutines.delay(1200)
            pointingAngle.animateTo(15f, tween(300))
            pointing = false
        }
    }
    val eyeColor by animateColorAsState(
        when (mood) {
            BotMood.SERIO -> Color(0xFFFFB300)
            BotMood.ALERTA -> Color(0xFFE53935)
            BotMood.ORGULLOSO -> Color(0xFF22C55E)
            else -> Color(0xFF4DD8E6)
        }, tween(300), label = "eye-color"
    )
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val unit = minOf(size.width, size.height) / 100f
        val center = Offset(size.width / 2f, size.height / 2f)
        val lift = sin(floatPhase * Math.PI).toFloat() * 1.5f * unit
        val shadowScale = 1f - lift / (2f * unit)
        drawOval(
            Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(center.x - 35f * unit, center.y + 38f * unit),
            size = Size(70f * unit, 8f * unit * shadowScale)
        )
        drawCircle(
            (if (mood == BotMood.ALERTA) Color(0xFFE53935) else Color(0xFF4DD8E6)).copy(alpha = 0.06f + pulse * 0.08f),
            70f * unit,
            Offset(center.x, center.y + lift)
        )
        withTransform({ translate(0f, lift) }) {
            drawBotBody(center, unit)
            val headY = center.y - 31f * unit + sin(floatPhase * Math.PI + 0.9).toFloat() * 0.5f * unit
            withTransform({ rotate(sin(floatPhase * Math.PI * 2).toFloat() * 1.5f, Offset(center.x, headY)) }) {
                drawBotHead(center.x, headY, unit, eyeColor, mood, blinkScale, pulse, textMeasurer, isSpeaking)
            }
            drawBotArms(center, unit, mood, pointingAngle.value)
            drawParticles(center, unit, particlePhase)
        }
    }
}

private fun DrawScope.drawBotBody(center: Offset, u: Float) {
    drawRoundRect(
        brush = Brush.radialGradient(listOf(Color(0xFFF4F6F9), Color(0xFFB9C2CE))),
        topLeft = Offset(center.x - 29f * u, center.y + 5f * u),
        size = Size(58f * u, 40f * u),
        cornerRadius = CornerRadius(20f * u)
    )
    drawRoundRect(
        Color(0xFF161A21), Offset(center.x - 17f * u, center.y + 18f * u),
        Size(34f * u, 14f * u), CornerRadius(6f * u)
    )
    drawRoundRect(
        Color(0xFF9AA3AF).copy(alpha = 0.4f), Offset(center.x - 29f * u, center.y + 5f * u),
        Size(58f * u, 40f * u), CornerRadius(20f * u), style = Stroke(0.7f * u)
    )
    drawRect(Color(0xFF8A94A3), Offset(center.x - 3f * u, center.y - 1f * u), Size(6f * u, 4f * u))
}

private fun DrawScope.drawBotHead(
    centerX: Float, top: Float, u: Float, eyeColor: Color, mood: BotMood,
    blinkScale: Float, pulse: Float, textMeasurer: androidx.compose.ui.text.TextMeasurer, speaking: Boolean
) {
    val head = Size(62f * u, 44f * u)
    drawCircle(Color(0xFFC9CFD8), 3.5f * u, Offset(centerX - 32f * u, top + 22f * u))
    drawCircle(Color(0xFFC9CFD8), 3.5f * u, Offset(centerX + 32f * u, top + 22f * u))
    if (mood == BotMood.THINKING || mood == BotMood.HABLANDO || speaking) {
        drawCircle(Color(0xFF4DD8E6).copy(alpha = 0.45f + pulse * 0.55f), 1.5f * u, Offset(centerX - 32f * u, top + 22f * u))
        drawCircle(Color(0xFF4DD8E6).copy(alpha = 0.45f + pulse * 0.55f), 1.5f * u, Offset(centerX + 32f * u, top + 22f * u))
    }
    drawRoundRect(Brush.radialGradient(listOf(Color(0xFFF4F6F9), Color(0xFFB9C2CE))), Offset(centerX - 31f * u, top), head, CornerRadius(18f * u))
    val screen = Rect(centerX - 23f * u, top + 8f * u, centerX + 23f * u, top + 36f * u)
    drawRoundRect(Color(0xFF0D1117), screen.topLeft, screen.size, CornerRadius(12f * u))
    val text = textMeasurer.measure("AURA", TextStyle(color = eyeColor, fontSize = 5f * u))
    drawText(text, Offset(centerX - text.size.width / 2f, top + 10.5f * u))
    drawLine(eyeColor.copy(alpha = 0.5f + pulse * 0.5f), Offset(centerX - 5f * u, top + 17f * u), Offset(centerX + 5f * u, top + 17f * u), 1.2f * u, StrokeCap.Round)
    drawEyes(centerX, top + 26f * u, u, eyeColor, mood, blinkScale, pulse)
    val mouthY = top + 32f * u
    if (speaking || mood == BotMood.HABLANDO) {
        drawOval(eyeColor, Offset(centerX - 3f * u, mouthY - (1.5f + pulse * 3.5f) * u), Size(6f * u, (1.5f + pulse * 3.5f) * 2f * u))
    } else if (mood == BotMood.ALERTA) {
        drawOval(eyeColor, Offset(centerX - 1.5f * u, mouthY - 2f * u), Size(3f * u, 4f * u), style = Stroke(0.8f * u))
    } else if (mood == BotMood.SERIO) {
        drawLine(eyeColor, Offset(centerX - 3f * u, mouthY), Offset(centerX + 3f * u, mouthY), 0.8f * u, StrokeCap.Round)
    } else {
        drawArc(eyeColor, 25f, 100f, false, Offset(centerX - 3f * u, mouthY - 2f * u), Size(6f * u, 4f * u), Stroke(0.8f * u))
    }
}

private fun DrawScope.drawEyes(cx: Float, cy: Float, u: Float, color: Color, mood: BotMood, blink: Float, pulse: Float) {
    val gap = 6f * u
    when (mood) {
        BotMood.SERIO -> {
            drawRoundRect(color, Offset(cx - gap - 3.5f * u, cy - u), Size(7f * u, 2f * u), CornerRadius(u))
            drawRoundRect(color, Offset(cx + gap - 3.5f * u, cy - u), Size(7f * u, 2f * u), CornerRadius(u))
        }
        BotMood.ALERTA -> listOf(-gap, gap).forEach { x -> drawEyeGlow(Offset(cx + x, cy), color, 4f * u * (1f + pulse * 0.12f)); drawCircle(Color(0xFF0D1117), u, Offset(cx + x, cy)) }
        BotMood.THINKING -> listOf(-gap, gap).forEach { x -> drawEyeGlow(Offset(cx + x + 1.5f * u, cy - 1.5f * u), color, 2f * u) }
        BotMood.ORGULLOSO -> listOf(-gap, gap).forEach { x -> drawHeart(Offset(cx + x, cy), 3f * u, color, 1f + pulse * 0.15f) }
        else -> listOf(-gap, gap).forEach { x ->
            drawArc(color, 180f, 180f, false, Offset(cx + x - 4.5f * u, cy - 4.5f * u), Size(9f * u, 9f * u), Stroke(1.2f * u))
            drawCircle(color, 1.2f * u, Offset(cx + x, cy + 2.5f * u * blink))
        }
    }
}

private fun DrawScope.drawEyeGlow(point: Offset, color: Color, radius: Float) {
    drawCircle(color.copy(alpha = 0.15f), radius * 1.15f, point)
    drawCircle(color.copy(alpha = 0.35f), radius * 1.05f, point)
    drawCircle(color, radius, point)
}

private fun DrawScope.drawHeart(center: Offset, radius: Float, color: Color, scale: Float) {
    val path = Path()
    path.moveTo(center.x, center.y + radius * 1.5f * scale)
    path.cubicTo(center.x - radius * 2f * scale, center.y, center.x - radius * scale, center.y - radius * 1.6f * scale, center.x, center.y - radius * 0.5f * scale)
    path.cubicTo(center.x + radius * scale, center.y - radius * 1.6f * scale, center.x + radius * 2f * scale, center.y, center.x, center.y + radius * 1.5f * scale)
    drawPath(path, color)
}

private fun DrawScope.drawBotArms(center: Offset, u: Float, mood: BotMood, angle: Float) {
    val rightAngle = when (mood) { BotMood.SCANNING, BotMood.THINKING -> -45f; BotMood.ALERTA -> -20f; else -> angle }
    listOf(-1f to 15f, 1f to rightAngle).forEach { (side, degrees) ->
        val pivot = Offset(center.x + side * 28f * u, center.y + 9f * u)
        val rad = Math.toRadians(degrees.toDouble())
        val end = Offset(pivot.x + side * cos(rad).toFloat() * 18f * u, pivot.y + sin(rad).toFloat() * 18f * u)
        drawLine(Color(0xFFB9C2CE), pivot, end, 4f * u, StrokeCap.Round)
        drawCircle(Color(0xFFC9CFD8), 2.5f * u, end)
    }
}

private fun DrawScope.drawParticles(center: Offset, u: Float, phase: Float) {
    repeat(8) { index ->
        val angle = (index * 45f + phase * 360f) * Math.PI / 180.0
        val radius = (36f + sin(phase * Math.PI * 2 + index).toFloat() * 3f) * u
        drawCircle(Color(0xFF4DD8E6).copy(alpha = 0.3f + 0.4f * ((sin(phase * Math.PI * 2 + index) + 1f) / 2f)), 0.8f * u, Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun AuraBotPreview() {
    androidx.compose.foundation.layout.Row {
        BotMood.values().forEach { mood -> AuraBot(mood = mood, modifier = Modifier.size(160.dp), size = 160.dp) }
    }
}
