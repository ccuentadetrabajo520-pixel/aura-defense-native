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

@Composable
fun AuraCoreIntro() {
    val transition = rememberInfiniteTransition(label = "aura-core-intro")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "aura-core-pulse"
    )
    Box(modifier = Modifier.fillMaxSize().background(AuraBackground), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - 28.dp.toPx())
            val base = size.minDimension * 0.18f
            listOf(1f, 1.35f, 1.7f).forEachIndexed { index, factor ->
                drawCircle(AuraCyan.copy(alpha = 0.24f - index * 0.06f), base * factor * pulse, center, style = Stroke(1.5.dp.toPx()))
            }
            drawCircle(AuraGreen.copy(alpha = 0.2f), base * 0.52f, center)
            drawCircle(AuraCyan, base * 0.24f, center)
            val nodes = listOf(
                Offset(center.x - base * 1.7f, center.y),
                Offset(center.x + base * 1.7f, center.y),
                Offset(center.x, center.y - base * 1.7f),
                Offset(center.x, center.y + base * 1.7f)
            )
            nodes.forEach { node ->
                drawLine(AuraCyan.copy(alpha = 0.45f), center, node, 1.dp.toPx(), StrokeCap.Round)
                drawCircle(AuraGreen, 4.dp.toPx(), node)
            }
            drawLine(AuraCyan.copy(alpha = 0.18f), Offset(0f, center.y), Offset(center.x - base * 1.9f, center.y), 1.dp.toPx())
            drawLine(AuraCyan.copy(alpha = 0.18f), Offset(center.x + base * 1.9f, center.y), Offset(size.width, center.y), 1.dp.toPx())
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 230.dp)) {
            Text("AURA DEFENS", color = AuraCyan, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("Defensa móvil privada", color = AuraMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
