package com.aura.defense.ui.components.aura

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import com.aura.defense.R

public enum class AuraMood(val res: Int) {
    IDLE(R.drawable.aura_icon),
    THINKING(R.drawable.mood_pensando),
    SERIO(R.drawable.mood_serio),
    ALERTA(R.drawable.mood_asustado),
    ORGULLOSO(R.drawable.mood_enamorado)
}

@Composable
fun AuraFace(
    mood: AuraMood,
    inBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringColor by animateColorAsState(
        targetValue = when (mood) {
            AuraMood.IDLE, AuraMood.THINKING -> Color(0xFF4DD8E6)
            AuraMood.SERIO -> Color(0xFFFBBF24)
            AuraMood.ALERTA -> Color(0xFFEF4444)
            AuraMood.ORGULLOSO -> Color(0xFF22C55E)
        },
        animationSpec = tween(350),
        label = "aura-face-ring"
    )
    val period = if (mood == AuraMood.ALERTA) 600 else if (mood == AuraMood.ORGULLOSO) 900 else 2800
    val target = if (mood == AuraMood.ALERTA) 1.08f else if (mood == AuraMood.ORGULLOSO) 1.12f else 1.03f
    val breathing = if (inBackground) {
        1f
    } else {
        rememberInfiniteTransition(label = "aura-face-breathing").animateFloat(
            initialValue = 1f,
            targetValue = target,
            animationSpec = infiniteRepeatable(tween(period), RepeatMode.Reverse),
            label = "aura-face-scale"
        ).value
    }

    Box(
        modifier = modifier
            .scale(breathing)
            .clip(CircleShape)
            .border(2.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = mood, animationSpec = tween(280), label = "aura-face-crossfade") { currentMood ->
            Image(
                painter = painterResource(currentMood.res),
                contentDescription = "AURA ${currentMood.name.lowercase()}",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
    }
}
