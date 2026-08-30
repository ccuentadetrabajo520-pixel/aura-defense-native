package com.aura.defense.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import kotlinx.coroutines.delay

@Composable
fun LiveStatusIndicator(
    isActive: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_status_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_status_alpha"
    )

    val dotColor = if (isActive) AuraGreen else AuraRed
    val statusTextColor = if (isActive) AuraGreen else AuraRed
    val statusText = if (isActive) "ACTIVO" else "INACTIVO"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(6.dp),
            shape = CircleShape,
            color = dotColor.copy(alpha = pulseAlpha)
        ) {
        }

        Spacer(modifier = Modifier.size(6.dp))

        Text(
            text = label,
            color = AuraMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.size(4.dp))

        Text(
            text = statusText,
            color = statusTextColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ScanningShield(score: Int, modifier: Modifier = Modifier.size(120.dp)) {
    val rotation = rememberInfiniteTransition(label = "shield_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_rotation_value"
    )

    val scoreColor = when {
        score >= 85 -> AuraGreen
        score >= 60 -> AuraAmber
        else -> AuraRed
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(size.width, size.height) * 0.42f
            val points = listOf(
                Offset(cx + radius, cy),
                Offset(cx + radius * 0.75f, cy - radius * 0.9f),
                Offset(cx - radius * 0.75f, cy - radius * 0.9f),
                Offset(cx - radius, cy),
                Offset(cx - radius * 0.75f, cy + radius * 0.9f),
                Offset(cx + radius * 0.75f, cy + radius * 0.9f)
            )

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (index in 1 until points.size) {
                    lineTo(points[index].x, points[index].y)
                }
                close()
            }

            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        AuraCyan.copy(alpha = 0.08f),
                        AuraBackground.copy(alpha = 0.0f)
                    ),
                    center = Offset(cx, cy),
                    radius = radius * 1.8f
                )
            )

            drawPath(
                path = path,
                color = AuraCyan.copy(alpha = 0.3f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            val topVertex = points[1]
            val scanLineStart = Offset(cx, cy)
            val scanLineEnd = Offset(
                cx + (topVertex.x - cx) * 0.9f,
                cy + (topVertex.y - cy) * 0.9f
            )
            val scanAngle = Math.toRadians(rotation.value.toDouble())
            val rotatedEndX = cx + (scanLineEnd.x - cx) * Math.cos(scanAngle).toFloat() - (scanLineEnd.y - cy) * Math.sin(scanAngle).toFloat()
            val rotatedEndY = cy + (scanLineEnd.x - cx) * Math.sin(scanAngle).toFloat() + (scanLineEnd.y - cy) * Math.cos(scanAngle).toFloat()

            drawLine(
                color = AuraCyan.copy(alpha = 0.15f),
                start = Offset(cx, cy),
                end = Offset(rotatedEndX, rotatedEndY),
                strokeWidth = 1.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score.toString(),
                color = scoreColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "/100",
                color = AuraMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
