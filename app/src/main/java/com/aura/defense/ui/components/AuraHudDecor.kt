package com.aura.defense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurfaceRaised

@Composable
public fun HudCornerBrackets(
    modifier: Modifier = Modifier,
    color: Color = AuraCyan.copy(alpha = 0.4f),
    cornerLength: Dp = 14.dp,
    strokeWidth: Dp = 1.dp
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 4.dp.toPx()
            val corner = cornerLength.toPx()
            val stroke = strokeWidth.toPx()

            val topLeftX = inset
            val topLeftY = inset
            val topRightX = size.width - inset
            val topRightY = inset
            val bottomLeftX = inset
            val bottomLeftY = size.height - inset
            val bottomRightX = size.width - inset
            val bottomRightY = size.height - inset

            drawLine(
                color = color,
                start = Offset(topLeftX, topLeftY),
                end = Offset(topLeftX + corner, topLeftY),
                strokeWidth = stroke
            )
            drawLine(
                color = color,
                start = Offset(topLeftX, topLeftY),
                end = Offset(topLeftX, topLeftY + corner),
                strokeWidth = stroke
            )

            drawLine(
                color = color,
                start = Offset(topRightX, topRightY),
                end = Offset(topRightX - corner, topRightY),
                strokeWidth = stroke
            )
            drawLine(
                color = color,
                start = Offset(topRightX, topRightY),
                end = Offset(topRightX, topRightY + corner),
                strokeWidth = stroke
            )

            drawLine(
                color = color,
                start = Offset(bottomLeftX, bottomLeftY),
                end = Offset(bottomLeftX + corner, bottomLeftY),
                strokeWidth = stroke
            )
            drawLine(
                color = color,
                start = Offset(bottomLeftX, bottomLeftY),
                end = Offset(bottomLeftX, bottomLeftY - corner),
                strokeWidth = stroke
            )

            drawLine(
                color = color,
                start = Offset(bottomRightX, bottomRightY),
                end = Offset(bottomRightX - corner, bottomRightY),
                strokeWidth = stroke
            )
            drawLine(
                color = color,
                start = Offset(bottomRightX, bottomRightY),
                end = Offset(bottomRightX, bottomRightY - corner),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
public fun ThreatLevelBar(
    level: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp
) {
    val clampedLevel = level.coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = AuraSurfaceRaised,
                topLeft = Offset.Zero,
                size = Size(width = size.width, height = size.height)
            )

            val fillWidth = size.width * clampedLevel
            val gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(AuraCyan, AuraMuted)
            )

            drawRect(
                brush = gradient,
                topLeft = Offset.Zero,
                size = Size(width = fillWidth, height = size.height)
            )

            drawLine(
                color = AuraCyan.copy(alpha = 0.9f),
                start = Offset(fillWidth, 0f),
                end = Offset(fillWidth, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
public fun GlowingBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AuraCyan
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(
                text = text.uppercase(),
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
