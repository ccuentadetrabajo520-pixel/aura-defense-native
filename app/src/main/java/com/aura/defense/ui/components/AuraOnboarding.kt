package com.aura.defense.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSpacing
import com.aura.defense.ui.AuraSurfaceRaised
import kotlinx.coroutines.delay

private data class OnboardingPage(val title: String, val subtitle: String, val description: String, val icon: String)

private val pages = listOf(
    OnboardingPage(
        "AURA DEFENSE",
        "Defensa móvil privada",
        "La primera app de ciberdefensa para Android sin root. Diagnóstico en tiempo real, firewall DNS, detección de amenazas y protección contra ingeniería social.",
        "🛡"
    ),
    OnboardingPage(
        "INTELIGENCIA ACTIVA",
        "Detecta lo invisible",
        "Analiza apps instaladas, enlaces compartidos, notificaciones y redes Wi-Fi. Cada señal se evalúa contra nuestra base de inteligencia de amenazas local.",
        "🔬"
    ),
    OnboardingPage(
        "SIN ROOT, SIN RASTROS",
        "Privacidad total",
        "Aura funciona sin permisos de superusuario. Tu datos nunca salen del dispositivo. El firewall DNS bloquea amenazas a nivel de red antes de que te afecten.",
        "🔒"
    )
)

@Composable
fun AuraOnboarding(onComplete: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "ob-pulse").animateFloat(0.6f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "ob-p")
    var page by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    LaunchedEffect(page) {
        titleAlpha.snapTo(0f)
        progress.snapTo(page.toFloat() / pages.size)
        delay(100)
        titleAlpha.animateTo(1f, tween(600))
        progress.animateTo((page + 1f) / pages.size, tween(400))
    }
    Box(Modifier.fillMaxSize().background(AuraBackground)) {
        Canvas(Modifier.fillMaxSize()) {
            val sp = 48.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 10.dp.toPx()))
            var x = 0f
            while (x < size.width) {
                drawLine(AuraCyan.copy(alpha = 0.025f), Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx(), pathEffect = dash)
                x += sp
            }
            var y = 0f
            while (y < size.height) {
                drawLine(AuraCyan.copy(alpha = 0.025f), Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx(), pathEffect = dash)
                y += sp
            }
            drawCircle(AuraCyan.copy(alpha = 0.04f * pulse.value), size.minDimension * 0.35f, Offset(size.width / 2f, size.height * 0.3f), style = Stroke(1.dp.toPx()))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = AuraSpacing.xxl, vertical = AuraSpacing.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Spacer(Modifier.height(80.dp))
            Text(pages[page].icon, fontSize = 56.sp)
            Spacer(Modifier.height(AuraSpacing.xl))
            Text(pages[page].title, color = AuraCyan.copy(alpha = titleAlpha.value), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AuraSpacing.sm))
            Text(pages[page].subtitle, color = AuraMuted.copy(alpha = titleAlpha.value), fontSize = 14.sp)
            Spacer(Modifier.height(AuraSpacing.xl))
            Text(pages[page].description, color = AuraMuted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = AuraSpacing.md))
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().height(4.dp).background(AuraSurfaceRaised, RoundedCornerShape(2.dp)), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.height(4.dp).width(80.dp * progress.value), color = AuraCyan, shape = RoundedCornerShape(2.dp)) {}
            }
            Row(Modifier.fillMaxWidth().padding(vertical = AuraSpacing.md), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${page + 1} / ${pages.size}", color = AuraMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pages.size) { i ->
                        Surface(
                            modifier = Modifier.padding(2.dp).clickable { if (i > page) page = i },
                            shape = CircleShape,
                            color = if (i == page) AuraCyan else AuraCyan.copy(alpha = 0.2f)
                        ) {
                            Box(Modifier.padding(4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(AuraSpacing.sm))
            if (page < pages.size - 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(50.dp).clickable { page++ },
                    color = AuraSurfaceRaised,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Siguiente", color = AuraCyan, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(50.dp).clickable(onClick = onComplete),
                    color = AuraCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ACTIVAR AURA", color = AuraCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(AuraSpacing.lg))
        }
    }
}
