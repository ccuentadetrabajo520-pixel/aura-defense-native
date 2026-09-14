package com.aura.defense.ui.components.aura

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun AuraMood(
    mood: String,
    modifier: Modifier = Modifier
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isResumed = lifecycleState == Lifecycle.State.RESUMED
    val breath = remember { Animatable(0.92f) }
    val blink = rememberInfiniteTransition(label = "mood_blink")
        .animateFloat(
            initialValue = 1f,
            targetValue = if (isResumed) 0.25f else 1f,
            animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse),
            label = "mood_blink_alpha"
        )

    LaunchedEffect(mood, isResumed) {
        if (mood == "ORGULLOSO") {
            breath.snapTo(1f)
            if (isResumed) {
                breath.animateTo(1.08f, tween(180))
                breath.animateTo(1f, tween(220))
                delay(1100)
            }
        } else if (isResumed) {
            breath.animateTo(1f, tween(220))
        }
    }

    val color = when (mood) {
        "SERIO" -> Color(0xFFFBBF24)
        "HABLANDO" -> Color(0xFF4DD8E6)
        "ORGULLOSO" -> Color(0xFF86EFAC)
        "ESCANEANDO" -> Color(0xFF22C55E)
        else -> Color(0xFF4DD8E6)
    }
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = 7.dp.toPx() * breath.value
        drawCircle(color.copy(alpha = 0.15f), radius * 3f, center)
        drawCircle(color.copy(alpha = 0.3f), radius * 2f, center)
        drawCircle(color, radius, center)
        if (mood == "SERIO") {
            drawLine(color, center.copy(x = center.x - 12.dp.toPx(), y = center.y - 2.dp.toPx()), center.copy(x = center.x - 3.dp.toPx(), y = center.y + 2.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, center.copy(x = center.x + 3.dp.toPx(), y = center.y + 2.dp.toPx()), center.copy(x = center.x + 12.dp.toPx(), y = center.y - 2.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        } else if (mood == "ESCANEANDO") {
            drawCircle(Color.Transparent, radius * 1.8f, center, style = Stroke(1.5.dp.toPx()))
        } else {
            drawCircle(color.copy(alpha = if (isResumed) blink.value else 1f), radius * 1.55f, center, style = Stroke(1.dp.toPx()))
        }
    }
}
