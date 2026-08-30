package com.aura.defense.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ScanLineOverlay(modifier: Modifier = Modifier) {
    val scanProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            scanProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4000)
            )
            scanProgress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraBackground.copy(alpha = 0.25f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineHeight = 1.dp.toPx()
            val glowHeight = 20.dp.toPx()
            val lineY = scanProgress.value * size.height
            val glowTop = (lineY - (glowHeight / 2f)).coerceAtLeast(0f)
            val glowBottom = (lineY + (glowHeight / 2f)).coerceAtMost(size.height)

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AuraCyan.copy(alpha = 0f),
                        AuraCyan.copy(alpha = 0.18f),
                        AuraCyan.copy(alpha = 0.18f),
                        AuraCyan.copy(alpha = 0f)
                    )
                ),
                topLeft = Offset(0f, glowTop),
                size = Size(width = size.width, height = glowBottom - glowTop)
            )

            drawRect(
                color = AuraCyan.copy(alpha = 0.35f),
                topLeft = Offset(0f, lineY),
                size = Size(width = size.width, height = lineHeight)
            )
        }
    }
}

@Composable
fun PulseRing(modifier: Modifier = Modifier, color: Color = AuraCyan) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    Canvas(modifier = modifier) {
        val center = Offset(x = size.width / 2f, y = size.height / 2f)
        val startRadius = 10.dp.toPx()
        val endRadius = 60.dp.toPx()
        val radius = startRadius + (endRadius - startRadius) * pulseProgress
        val alpha = 0.6f * (1f - pulseProgress)
        val stroke = 1.5.dp.toPx()

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AuraCyan,
    fontSize: TextUnit = 14.sp,
    speedMs: Int = 50
) {
    var visibleCharCount by remember(text) { mutableStateOf(0) }
    val cursorAlpha = rememberInfiniteTransition(label = "typewriter_cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    LaunchedEffect(text) {
        visibleCharCount = 0
        while (visibleCharCount < text.length) {
            delay(speedMs.toLong())
            visibleCharCount += 1
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visibleText = text.substring(0, visibleCharCount.coerceIn(0, text.length))
        Text(
            text = visibleText,
            color = color,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace
        )

        if (visibleCharCount < text.length) {
            Text(
                text = "|",
                color = color.copy(alpha = cursorAlpha.value),
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DataStreamColumn(modifier: Modifier = Modifier) {
    val hexChars = "0123456789ABCDEF"
    var stream by remember {
        mutableStateOf(
            List(6) {
                buildString {
                    repeat(8) {
                        append(hexChars[Random.nextInt(hexChars.length)])
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            stream = List(6) {
                buildString {
                    repeat(8) {
                        append(hexChars[Random.nextInt(hexChars.length)])
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(120.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        stream.forEach { line ->
            Text(
                text = line,
                color = AuraCyan.copy(alpha = 0.12f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
