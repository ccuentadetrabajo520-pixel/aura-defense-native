package com.aura.defense.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.data.AuraPreferences
import com.aura.defense.ui.components.aura.AuraMotion
import com.aura.defense.ui.components.aura.AuraTermsDialog
import com.aura.defense.ui.components.aura.IntroSceneCanvas
import com.aura.defense.ui.components.aura.VenezuelaFlagCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuraIntroScreen(preferences: AuraPreferences, onFinished: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 6 })
    val scope = rememberCoroutineScope()
    var termsAccepted by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    fun advance() {
        if (pagerState.currentPage < 5) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF14171C))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                Modifier.fillMaxSize().clickable { if (page < 5) advance() }
            ) {
                when (page) {
                    0 -> IntroSceneCanvas("Hola, soy AURA", "Seré tu asistente de ciber defensa para tu dispositivo") {}
                    1 -> IntroSceneCanvas("Esto es lo que hago por ti", "Vigilo tu red · Analizo tus apps · Cuido tu sistema · En tiempo real") {}
                    2 -> IntroSceneCanvas("Bloqueo lo peligroso", "Antes de que toque tus datos. Escudo rojo: amenaza filtrada. Escudo verde: tu información segura.") {}
                    3 -> IntroSceneCanvas("Dentro de tu teléfono", "Protección en tiempo real. Todo se procesa en tu dispositivo: nada de tus datos sale de aquí. Nunca.") {}
                    4 -> IntroSceneCanvas("Orgullo venezolano", "Hecho con orgullo para Venezuela y toda América Latina. Nuestra gente merece estar protegida.") {
                        VenezuelaFlagCanvas(Modifier.fillMaxWidth().height(220.dp).padding(top = 24.dp))
                    }
                    else -> FinalScene(
                        termsAccepted = termsAccepted,
                        onTermsChanged = { termsAccepted = it },
                        onReadTerms = { showTerms = true },
                        onStart = {
                            preferences.setTermsAccepted()
                            preferences.setOnboardingCompleted()
                            onFinished()
                        }
                    )
                }
            }
        }
        if (pagerState.currentPage < 5) {
            Button(
                onClick = ::advance,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp)
            ) { Text("Siguiente") }
        }
        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(6) { index ->
                Box(Modifier.size(if (index == pagerState.currentPage) 22.dp else 7.dp, 7.dp).background(Color(0xFF4DD8E6).copy(alpha = if (index == pagerState.currentPage) 1f else 0.35f), RoundedCornerShape(8.dp)))
            }
        }
    }
    if (showTerms) AuraTermsDialog(onDismiss = { showTerms = false })
}

@Composable
private fun FinalScene(
    termsAccepted: Boolean,
    onTermsChanged: (Boolean) -> Unit,
    onReadTerms: () -> Unit,
    onStart: () -> Unit
) {
    val breath = androidx.compose.animation.core.rememberInfiniteTransition(label = "avatar-breath")
    val avatarScale by breath.animateFloat(1f, 1.03f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(AuraMotion.Breath, easing = AuraMotion.AuraEase), androidx.compose.animation.core.RepeatMode.Reverse), label = "avatar-scale")
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            blink = true
            delay(140)
            blink = false
        }
    }
    Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(210.dp).graphicsLayerSafe(avatarScale)) {
            val head = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.62f)
            val topLeft = Offset((size.width - head.width) / 2f, size.height * 0.1f)
            drawRoundRect(Color(0xFFE8EAED), topLeft, head, androidx.compose.ui.geometry.CornerRadius(34.dp.toPx()))
            val screen = androidx.compose.ui.geometry.Size(head.width * 0.78f, head.height * 0.55f)
            val screenTop = Offset(topLeft.x + head.width * 0.11f, topLeft.y + head.height * 0.22f)
            drawRoundRect(Color(0xFF14171C), screenTop, screen, androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
            val eyeScale = if (blink) 0.15f else 1f
            listOf(0.37f, 0.63f).forEach { x ->
                withTransform({ scale(1f, eyeScale, Offset(screenTop.x + screen.width * x, screenTop.y + screen.height * 0.5f)) }) {
                    drawCircle(Color(0xFF4DD8E6), 10.dp.toPx(), Offset(screenTop.x + screen.width * x, screenTop.y + screen.height * 0.5f))
                }
            }
            drawLine(Color(0xFFB9C1C8), Offset(size.width * 0.35f, size.height * 0.78f), Offset(size.width * 0.65f, size.height * 0.78f), 3.dp.toPx(), StrokeCap.Round)
        }
        Spacer(Modifier.height(16.dp))
        Text("Estoy listo para cuidarte.", color = Color(0xFF4DD8E6), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReadTerms) { Text("Leer Términos y Condiciones") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = termsAccepted, onCheckedChange = onTermsChanged)
                    Text("He leído y acepto los Términos y Condiciones", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onStart, enabled = termsAccepted, modifier = Modifier.fillMaxWidth()) { Text("Empezar") }
            }
        }
    }
}

private fun Modifier.graphicsLayerSafe(scale: Float): Modifier = this.then(Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
})
