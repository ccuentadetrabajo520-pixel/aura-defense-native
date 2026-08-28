package com.aura.defense.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraCyanGlow
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurfaceHigh
import com.aura.defense.ui.AuraTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val radius: Float,
    val baseAlpha: Float
)

@Composable
fun AuraCoreIntro() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "radarRotation"
    )

    val corePulse = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "corePulse"
    )

    val sonarPhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "sonar"
    )

    val scanPhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "scanLine"
    )

    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val letterSpacing = remember { Animatable(12.sp) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(800)
        titleAlpha.animateTo(1f, tween(2000))
        letterSpacing.animateTo(6.sp, tween(2000))
        delay(600)
        subtitleAlpha.animateTo(1f, tween(1500))
        progress.animateTo(1f, tween(3000, easing = LinearEasing))
    }

    val particles = remember {
        List(25) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.0003f + Random.nextFloat() * 0.0008f,
                radius = 1f + Random.nextFloat() * 2f,
                baseAlpha = 0.2f + Random.nextFloat() * 0.5f
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.36f
            val baseRadius = w * 0.28f

            drawBackgroundGrid(w, h)
            drawDataStreams(w, h, scanPhase.value)
            drawSonarRings(cx, cy, baseRadius, sonarPhase.value)
            drawParticlesField(particles)
            drawRadarSystem(cx, cy, baseRadius, rotation.value, corePulse.value)
            drawScanLine(w, h, scanPhase.value)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(220.dp))
            Text(
                text = "AURA DEFENSE",
                style = MaterialTheme.typography.displayLarge,
                color = AuraCyan.copy(alpha = titleAlpha.value),
                letterSpacing = letterSpacing.value,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "DEFENSA MÓVIL PRIVADA",
                style = MaterialTheme.typography.labelSmall,
                color = AuraTextSecondary.copy(alpha = subtitleAlpha.value),
                textAlign = TextAlign.Center
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().align(Alignment.BottomCenter)) {
            val barW = size.width * 0.6f
            val barH = 2.dp.toPx()
            val barX = (size.width - barW) / 2f
            val barY = size.height - 60.dp.toPx()

            drawRect(
                color = AuraSurfaceHigh,
                topLeft = Offset(barX, barY),
                size = Size(barW, barH)
            )
            drawRect(
                color = AuraCyan.copy(alpha = 0.9f),
                topLeft = Offset(barX, barY),
                size = Size(barW * progress.value, barH)
            )
            if (progress.value > 0.02f) {
                drawCircle(
                    color = AuraCyan.copy(alpha = 0.6f),
                    radius = 4.dp.toPx(),
                    center = Offset(barX + barW * progress.value, barY + barH / 2f)
                )
            }
        }
    }
}

private fun DrawScope.drawBackgroundGrid(w: Float, h: Float) {
    val gridColor = AuraCyan.copy(alpha = 0.035f)
    val spacing = 42.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 8.dp.toPx()))
    var x = 0f
    while (x < w) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.5f, pathEffect = dash)
        x += spacing
    }
    var y = 0f
    while (y < h) {
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f, pathEffect = dash)
        y += spacing
    }
}

private fun DrawScope.drawDataStreams(w: Float, h: Float, phase: Float) {
    val streamColor = AuraCyan.copy(alpha = 0.04f)
    val brightColor = AuraCyan.copy(alpha = 0.08f)
    val lineSpacing = 18.dp.toPx()
    val leftX = 12.dp.toPx()
    val rightX = w - 12.dp.toPx()

    for (i in 0 until 6) {
        val yBase = ((phase * h * 1.5f + i * lineSpacing * 3.7f) % (h + 200f)) - 100f
        drawLine(streamColor, Offset(leftX, yBase), Offset(leftX, yBase + 80.dp.toPx()), strokeWidth = 1.dp.toPx())
        drawLine(streamColor, Offset(rightX, yBase + 40f), Offset(rightX, yBase + 120.dp.toPx()), strokeWidth = 1.dp.toPx())
        drawCircle(brightColor, radius = 2.dp.toPx(), center = Offset(leftX, yBase))
        drawCircle(brightColor, radius = 2.dp.toPx(), center = Offset(rightX, yBase + 40f))
    }
}

private fun DrawScope.drawSonarRings(cx: Float, cy: Float, radius: Float, phase: Float) {
    for (i in 0..2) {
        val p = ((phase + i * 0.33f) % 1f)
        val r = radius * (0.2f + p * 0.9f)
        val alpha = (1f - p) * 0.12f
        drawCircle(
            AuraCyan.copy(alpha = alpha),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawParticlesField(particles: List<Particle>) {
    val time = System.currentTimeMillis().coerceAtMost(60000).toFloat()
    particles.forEach { particle ->
        var y = particle.y - particle.speed * time
        y = ((y % 1.2f) + 1.2f) % 1.2f - 0.1f
        val px = particle.x * size.width
        val py = y * size.height
        val fade = 1f - (y / 1.1f).coerceIn(0f, 1f)
        val alpha = particle.baseAlpha * fade
        if (alpha > 0.02f) {
            drawCircle(AuraCyan.copy(alpha = alpha), radius = particle.radius, center = Offset(px, py))
        }
    }
}

private fun DrawScope.drawRadarSystem(cx: Float, cy: Float, radius: Float, rotation: Float, pulse: Float) {
    val ringFracs = floatArrayOf(0.35f, 0.65f, 1.0f)
    val ringAlphas = floatArrayOf(0.14f, 0.09f, 0.05f)
    ringFracs.forEachIndexed { i, frac ->
        drawCircle(
            AuraCyan.copy(alpha = ringAlphas[i]),
            radius = radius * frac,
            center = Offset(cx, cy),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }

    val crossLen = radius * 1.05f
    val crossColor = AuraCyan.copy(alpha = 0.06f)
    drawLine(crossColor, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), strokeWidth = 0.5.dp.toPx())
    drawLine(crossColor, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), strokeWidth = 0.5.dp.toPx())

    val sweepRad = Math.toRadians(rotation.toDouble())
    val endX = cx + radius * cos(sweepRad).toFloat()
    val endY = cy + radius * sin(sweepRad).toFloat()
    drawLine(AuraCyan.copy(alpha = 0.7f), Offset(cx, cy), Offset(endX, endY), strokeWidth = 1.5.dp.toPx())

    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(AuraCyan.copy(alpha = 0.18f), AuraCyan.copy(alpha = 0.0f)),
            center = Offset(cx, cy)
        ),
        startAngle = rotation - 45f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        alpha = 0.5f,
        blendMode = BlendMode.Add
    )

    val cardinals = floatArrayOf(0f, 90f, 180f, 270f)
    cardinals.forEach { angle ->
        val rad = Math.toRadians((angle + rotation * 0.5).toDouble())
        val nr = radius * 0.35f
        val nx = cx + nr * cos(rad).toFloat()
        val ny = cy + nr * sin(rad).toFloat()
        val nodePulse = (sin(System.currentTimeMillis() * 0.003 + angle) + 1f) / 2f
        drawCircle(AuraGreen.copy(alpha = 0.3f + nodePulse * 0.5f), radius = 3.dp.toPx(), center = Offset(nx, ny))
    }

    drawCircle(AuraCyanGlow, radius = 26.dp.toPx(), center = Offset(cx, cy))
    drawCircle(AuraCyan.copy(alpha = pulse * 0.2f), radius = 18.dp.toPx(), center = Offset(cx, cy))
    drawCircle(AuraCyan.copy(alpha = pulse), radius = 5.dp.toPx(), center = Offset(cx, cy))
}

private fun DrawScope.drawScanLine(w: Float, h: Float, phase: Float) {
    val y = phase * h
    val gradient = Brush.verticalGradient(
        colors = listOf(
            AuraCyan.copy(alpha = 0.06f),
            AuraCyan.copy(alpha = 0.0f)
        ),
        startY = y - 40.dp.toPx(),
        endY = y + 40.dp.toPx()
    )
    drawRect(gradient, Offset(0f, y - 40.dp.toPx()), Size(w, 80.dp.toPx()))
    drawLine(AuraCyan.copy(alpha = 0.08f), Offset(0f, y), Offset(w, y), strokeWidth = 0.5.dp.toPx())
}
