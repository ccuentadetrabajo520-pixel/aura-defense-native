package com.aura.defense.ui.components.aura

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val AuraCyan = Color(0xFF4DD8E6)
private val AuraText = Color(0xFFE8EAED)

@Composable
fun IntroScene(imageRes: Int, title: String, subtitle: String) {
    val transition = rememberInfiniteTransition(label = "ken-burns")
    val scale by transition.animateFloat(
        1f,
        1.06f,
        infiniteRepeatable(tween(8000, easing = AuraMotion.AuraEase), RepeatMode.Reverse),
        label = "ken-burns-scale"
    )
    Box(Modifier.fillMaxSize()) {
        if (imageRes != 0) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.78f
                        )
                    )
                }
            )
        } else {
            IntroSceneCanvas(title, subtitle) {}
            return
        }
        IntroCopy(title, subtitle)
    }
}

@Composable
fun IntroSceneCanvas(title: String, subtitle: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF14171C), Color(0xFF1E2228), Color(0xFF14171C)))
        )
    ) {
        content()
        IntroCopy(title, subtitle)
    }
}

@Composable
private fun IntroCopy(title: String, subtitle: String) {
    var visibleTitle by remember(title) { mutableStateOf("") }
    LaunchedEffect(title) {
        visibleTitle = ""
        title.forEach { character ->
            visibleTitle += character
            delay(40)
        }
    }
    var subtitleVisible by remember(title) { mutableStateOf(false) }
    LaunchedEffect(visibleTitle) {
        if (visibleTitle == title) subtitleVisible = true
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            visibleTitle,
            color = AuraCyan,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            subtitle,
            color = AuraText.copy(alpha = if (subtitleVisible) 0.88f else 0f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(0.8f))
    }
}
