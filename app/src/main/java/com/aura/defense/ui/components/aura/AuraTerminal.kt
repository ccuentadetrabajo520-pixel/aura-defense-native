package com.aura.defense.ui.components.aura

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.defense.monitor.AuraProcessLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TerminalBackground = Color(0xFF0A0C0A)
private val TerminalBorder = Color(0xFF1E4620)
private val TerminalGreen = Color(0xFF22C55E)

@Composable
fun AuraTerminal(
    entries: List<AuraProcessLog.ProcessEntry>,
    modifier: Modifier
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isResumed = lifecycleState == Lifecycle.State.RESUMED
    val cursorAlpha by rememberInfiniteTransition(label = "terminal_cursor")
        .animateFloat(
        initialValue = 1f,
        targetValue = if (isResumed) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "terminal_cursor_alpha"
    )
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val visibleEntries = entries.takeLast(7)

    Column(
        modifier = modifier
            .background(TerminalBackground, RoundedCornerShape(12.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF667066), RoundedCornerShape(50))
                        .padding(4.dp)
                )
            }
            Text(
                "AURA://PROCESOS",
                color = TerminalGreen.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (visibleEntries.isEmpty()) {
                Text(
                    "Aún no veo nada preocupante. Sigo vigilando.",
                    color = TerminalGreen.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            } else {
                visibleEntries.forEach { entry ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(220)) +
                            slideInVertically(
                                animationSpec = tween(220),
                                initialOffsetY = { 8 }
                            )
                    ) {
                        Text(
                            "${dateFormat.format(Date(entry.timestamp))} [${entry.category}] ${entry.message}",
                            color = TerminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }
            Text(
                "█",
                color = TerminalGreen.copy(alpha = cursorAlpha),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
