package com.aura.defense.ui.components.aura

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VenezuelaFlagCanvas(modifier: Modifier = Modifier) {
    var litStar by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            for (index in 0 until 8) {
                litStar = index
                delay(120)
            }
            delay(800)
        }
    }
    val transition = rememberInfiniteTransition(label = "flag-breeze")
    val rotation by transition.animateFloat(
        -1.5f,
        1.5f,
        infiniteRepeatable(tween(3000, easing = AuraMotion.AuraEase), RepeatMode.Reverse),
        label = "flag-rotation"
    )
    Canvas(modifier = modifier) {
        val flagWidth = size.width * 0.9f
        val flagHeight = flagWidth * 0.62f
        val left = (size.width - flagWidth) / 2f
        val top = (size.height - flagHeight) / 2f
        val radius = 12.dp.toPx()
        rotate(rotation, Offset(size.width / 2f, size.height / 2f)) {
            drawRoundRect(Color(0xFFFCD116), topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(flagWidth, flagHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius))
            drawRect(Color(0xFF00247D), Offset(left, top + flagHeight / 2f), androidx.compose.ui.geometry.Size(flagWidth, flagHeight / 4f))
            drawRect(Color(0xFFCF142B), Offset(left, top + flagHeight * 0.75f), androidx.compose.ui.geometry.Size(flagWidth, flagHeight / 4f))
            val center = Offset(left + flagWidth / 2f, top + flagHeight * 0.625f)
            val arcRadius = flagWidth * 0.22f
            for (index in 0 until 8) {
                val angle = Math.toRadians(205.0 + index * (130.0 / 7.0))
                val point = Offset(center.x + cos(angle).toFloat() * arcRadius, center.y + sin(angle).toFloat() * arcRadius)
                val alpha = if (index == litStar) 1f else 0.25f
                drawCircle(Color.White.copy(alpha = alpha * 0.2f), 10.dp.toPx(), point)
                drawPath(starPath(point, 5.dp.toPx(), 2.dp.toPx()), Color.White.copy(alpha = alpha))
            }
            drawRoundRect(Color.Transparent, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(flagWidth, flagHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius), style = Stroke(1.dp.toPx()))
        }
    }
}

private fun starPath(center: Offset, outerRadius: Float, innerRadius: Float): Path {
    val path = Path()
    for (index in 0 until 10) {
        val angle = Math.toRadians(-90.0 + index * 36.0)
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
