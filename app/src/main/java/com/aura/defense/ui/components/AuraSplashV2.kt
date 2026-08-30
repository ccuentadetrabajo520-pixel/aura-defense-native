package com.aura.defense.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import kotlinx.coroutines.delay

@Composable
fun AuraSplashV2() {
    val auraAlpha = remember { Animatable(0f) }
    val defenseAlpha = remember { Animatable(0f) }
    val lineProgress = remember { mutableStateOf(0f) }
    val lineWidth by animateFloatAsState(
        targetValue = lineProgress.value,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "line_width"
    )

    LaunchedEffect(Unit) {
        auraAlpha.animateTo(1f, animationSpec = tween(1000, delayMillis = 500))
        defenseAlpha.animateTo(1f, animationSpec = tween(800, delayMillis = 1000))
        lineProgress.value = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
    ) {
        ParticleField(
            modifier = Modifier.fillMaxSize(),
            particleCount = 15
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            ScanningShield(score = 0, modifier = Modifier.size(140.dp))

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AURA",
                color = AuraCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 12.sp,
                modifier = Modifier.alpha(auraAlpha.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "DEFENSE",
                color = AuraMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
                modifier = Modifier.alpha(defenseAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TypewriterText(
                text = "CIBERDEFENSA SIN ROOT",
                color = AuraGreen,
                fontSize = 11.sp,
                speedMs = 80
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .width((60.dp * lineWidth).coerceAtLeast(0.dp))
                    .height(1.dp)
                    .background(AuraCyan.copy(alpha = 0.3f))
            )
        }
    }
}
