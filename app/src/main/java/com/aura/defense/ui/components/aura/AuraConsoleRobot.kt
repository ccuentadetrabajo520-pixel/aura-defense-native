package com.aura.defense.ui.components.aura

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AuraConsoleRobot(
    modifier: Modifier,
    serious: Boolean,
    scanning: Boolean,
    eventCount: Int
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isResumed = lifecycleState == Lifecycle.State.RESUMED
    val transition = rememberInfiniteTransition(label = "aura_robot")
    val scanPosition by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = if (isResumed) 0.82f else 0.18f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scan_position"
    )
    val blinkScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isResumed) 0.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                1f at 0
                1f at 3360
                0.15f at 3430
                1f at 3500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "robot_blink"
    )
    val headRotation = remember { Animatable(0f) }
    val armAngle = remember { Animatable(20f) }
    var isAnimating by remember { mutableStateOf(false) }
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#14171C")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    LaunchedEffect(eventCount, isResumed) {
        if (eventCount > 0 && isResumed && !isAnimating) {
            isAnimating = true
            try {
                headRotation.animateTo(-4f, tween(200))
                armAngle.animateTo(-35f, tween(250, easing = FastOutSlowInEasing))
                kotlinx.coroutines.delay(1200)
                armAngle.animateTo(20f, tween(300))
                headRotation.animateTo(0f, tween(300))
            } finally {
                isAnimating = false
            }
        }
    }

    Canvas(modifier) {
        val scale = min(size.width, size.height) / 240f
        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        val faceSize = Size(116f * scale, 82f * scale)
        val faceTop = center.y - faceSize.height * 0.58f
        val accent = Color(0xFF4DD8E6)
        val amber = Color(0xFFFBBF24)
        val shell = Brush.linearGradient(
            colors = listOf(Color(0xFFE8EAED), Color(0xFFC9CDD3)),
            start = Offset(0f, faceTop),
            end = Offset(0f, faceTop + faceSize.height)
        )
        val stroke = (2f * scale).coerceAtLeast(1f)

        fun drawGlow(point: Offset, color: Color, radius: Float) {
            drawCircle(color.copy(alpha = 0.15f), radius * 2.7f, point)
            drawCircle(color.copy(alpha = 0.3f), radius * 1.8f, point)
            drawCircle(color.copy(alpha = 1f), radius, point)
        }

        withTransform({ rotate(headRotation.value, center) }) {
            drawRoundRect(
                brush = shell,
                topLeft = Offset(center.x - faceSize.width / 2f, faceTop),
                size = faceSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f * scale)
            )
            drawRoundRect(
                color = Color(0xFF14171C),
                topLeft = Offset(center.x - faceSize.width * 0.42f, faceTop + faceSize.height * 0.17f),
                size = Size(faceSize.width * 0.84f, faceSize.height * 0.66f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale),
                style = Stroke(stroke)
            )
            val eyeY = faceTop + faceSize.height * 0.48f
            val eyeGap = faceSize.width * 0.2f
            if (serious) {
                drawLine(amber, Offset(center.x - eyeGap - 9f * scale, eyeY - 3f * scale), Offset(center.x - eyeGap + 9f * scale, eyeY + 3f * scale), stroke * 1.5f, StrokeCap.Round)
                drawLine(amber, Offset(center.x + eyeGap - 9f * scale, eyeY + 3f * scale), Offset(center.x + eyeGap + 9f * scale, eyeY - 3f * scale), stroke * 1.5f, StrokeCap.Round)
                drawGlow(Offset(center.x - eyeGap, eyeY), amber, 2.5f * scale)
                drawGlow(Offset(center.x + eyeGap, eyeY), amber, 2.5f * scale)
            } else if (scanning) {
                val scanX = center.x - faceSize.width * 0.35f + faceSize.width * 0.7f * scanPosition
                drawGlow(Offset(scanX, eyeY), accent, 3f * scale)
                drawLine(accent, Offset(scanX - 13f * scale, eyeY), Offset(scanX + 13f * scale, eyeY), stroke, StrokeCap.Round)
            } else {
                val eyeScale = blinkScale
                drawArc(accent, 200f, 140f, false, Offset(center.x - eyeGap - 10f * scale, eyeY - 6f * scale * eyeScale), Size(20f * scale, 14f * scale * eyeScale), style = Stroke(stroke))
                drawArc(accent, 200f, 140f, false, Offset(center.x + eyeGap - 10f * scale, eyeY - 6f * scale * eyeScale), Size(20f * scale, 14f * scale * eyeScale), style = Stroke(stroke))
                drawGlow(Offset(center.x - eyeGap, eyeY), accent, 2f * scale)
                drawGlow(Offset(center.x + eyeGap, eyeY), accent, 2f * scale)
            }
        }

        val shoulder = Offset(center.x + faceSize.width * 0.38f, faceTop + faceSize.height + 20f * scale)
        val armLength = 48f * scale
        val radians = Math.toRadians(armAngle.value.toDouble())
        val hand = Offset(
            shoulder.x - cos(radians).toFloat() * armLength,
            shoulder.y + sin(radians).toFloat() * armLength
        )
        drawLine(Color(0xFFC9CDD3), shoulder, hand, 9f * scale, StrokeCap.Round)
        drawCircle(Color(0xFFE8EAED), 6f * scale, hand)

        val torsoTop = faceTop + faceSize.height + 18f * scale
        drawRoundRect(
            brush = shell,
            topLeft = Offset(center.x - 43f * scale, torsoTop),
            size = Size(86f * scale, 72f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * scale)
        )
        drawRoundRect(
            color = Color(0xFF14171C),
            topLeft = Offset(center.x - 22f * scale, torsoTop + 20f * scale),
            size = Size(44f * scale, 22f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f * scale)
        )
        drawContext.canvas.nativeCanvas.drawText(
            "AURA",
            center.x,
            torsoTop + 35f * scale,
            textPaint
        )
    }
}
