package com.aura.defense.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun AuraConsoleRobot(
    modifier: Modifier,
    serious: Boolean,
    scanning: Boolean,
    eventCount: Int
) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1.45f)) {
        Canvas(Modifier.fillMaxWidth()) {
            val center = Offset(size.width / 2f, size.height * 0.52f)
            val scale = min(size.width, size.height) / 260f
            val stroke = (2.2f * scale).coerceAtLeast(1f)
            val accent = if (serious) Color(0xFFF87171) else Color(0xFF22C55E)
            val muted = Color(0xFF244C2B)
            val faceWidth = 116f * scale
            val faceHeight = 82f * scale
            val faceTop = center.y - faceHeight / 2f

            drawCircle(muted.copy(alpha = 0.2f), 92f * scale, center)
            drawLine(
                muted,
                Offset(center.x, faceTop - 28f * scale),
                Offset(center.x, faceTop - 8f * scale),
                stroke,
                StrokeCap.Round
            )
            drawCircle(accent, 4f * scale, Offset(center.x, faceTop - 32f * scale))
            drawRoundRect(
                topLeft = Offset(center.x - faceWidth / 2f, faceTop),
                size = androidx.compose.ui.geometry.Size(faceWidth, faceHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * scale),
                style = Stroke(stroke),
                color = accent.copy(alpha = 0.85f)
            )
            val eyeY = center.y - 5f * scale
            val eyeGap = 25f * scale
            drawCircle(accent, 7f * scale, Offset(center.x - eyeGap, eyeY))
            drawCircle(accent, 7f * scale, Offset(center.x + eyeGap, eyeY))
            if (scanning) {
                drawLine(
                    Color(0xFF86EFAC),
                    Offset(center.x - faceWidth * 0.36f, center.y + 20f * scale),
                    Offset(center.x + faceWidth * 0.36f, center.y + 20f * scale),
                    stroke,
                    StrokeCap.Round
                )
            } else {
                drawLine(
                    accent,
                    Offset(center.x - 20f * scale, center.y + 22f * scale),
                    Offset(center.x + 20f * scale, center.y + 22f * scale),
                    stroke,
                    StrokeCap.Round
                )
            }
            val shoulderY = faceTop + faceHeight + 24f * scale
            drawLine(
                muted,
                Offset(center.x - 38f * scale, shoulderY),
                Offset(center.x - 64f * scale, shoulderY + 30f * scale),
                stroke,
                StrokeCap.Round
            )
            drawLine(
                muted,
                Offset(center.x + 38f * scale, shoulderY),
                Offset(center.x + 64f * scale, shoulderY + 30f * scale),
                stroke,
                StrokeCap.Round
            )
            repeat(eventCount.coerceAtMost(4)) { index ->
                val y = shoulderY + (index * 8f * scale)
                drawLine(
                    accent.copy(alpha = 0.35f),
                    Offset(center.x - 16f * scale, y),
                    Offset(center.x + 16f * scale, y),
                    stroke,
                    StrokeCap.Round
                )
            }
        }
    }
}
