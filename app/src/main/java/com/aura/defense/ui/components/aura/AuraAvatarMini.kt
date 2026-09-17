package com.aura.defense.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun AuraAvatarMini(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    protectionState: String = "OFF"
) {
    Canvas(
        modifier = modifier
            .size(38.dp)
            .clickable(onClick = onClick)
    ) {
        val scale = min(size.width, size.height) / 38f
        val center = Offset(size.width / 2f, size.height / 2f)
        val shell = Brush.linearGradient(
            listOf(Color(0xFFE8EAED), Color(0xFFC9CDD3))
        )
        drawCircle(Color(0xFF0A0C0A), 17f * scale, center)
        val halo = when (protectionState) {
            "ACTIVE_DNS_ONLY" -> Color(0xFF39E6B0)
            "STARTING" -> Color(0xFF4DD8E6)
            "DEGRADED" -> Color(0xFFFFB800)
            else -> Color(0xFF7D8794)
        }
        drawCircle(halo.copy(alpha = if (protectionState == "ACTIVE_DNS_ONLY") 0.28f else 0.18f), 17f * scale, center)
        drawRoundRect(
            brush = shell,
            topLeft = Offset(center.x - 12f * scale, center.y - 13f * scale),
            size = androidx.compose.ui.geometry.Size(24f * scale, 22f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f * scale)
        )
        drawRoundRect(
            color = Color(0xFF14171C),
            topLeft = Offset(center.x - 9f * scale, center.y - 8f * scale),
            size = androidx.compose.ui.geometry.Size(18f * scale, 11f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
        )
        drawCircle(halo, 2f * scale, Offset(center.x - 4f * scale, center.y - 2.5f * scale))
        drawCircle(halo, 2f * scale, Offset(center.x + 4f * scale, center.y - 2.5f * scale))
        drawLine(
            halo,
            Offset(center.x - 4f * scale, center.y + 5f * scale),
            Offset(center.x + 4f * scale, center.y + 5f * scale),
            1.4f * scale,
            StrokeCap.Round
        )
    }
}
