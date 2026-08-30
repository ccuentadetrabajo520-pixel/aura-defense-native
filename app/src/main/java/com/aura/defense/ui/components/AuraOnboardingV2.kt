package com.aura.defense.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGradientCyan
import com.aura.defense.ui.AuraGradientShield
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSurfaceRaised
import com.aura.defense.ui.AuraText
import com.aura.defense.ui.AuraTextSecondary
import kotlinx.coroutines.delay

private data class OnboardingPageV2(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: String,
    val accentColor: Color
)

@Composable
fun AuraOnboardingV2(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPageV2(
            title = "AURA DEFENSE",
            subtitle = "Primera app de ciberdefensa sin root del mundo",
            description = "Diagnóstico en tiempo real, firewall DNS, detección de amenazas, VPN local y protección contra ingeniería social. Todo funciona directamente en tu dispositivo.",
            icon = "[shield]",
            accentColor = AuraCyan
        ),
        OnboardingPageV2(
            title = "INTELIGENCIA ACTIVA",
            subtitle = "Detecta lo invisible",
            description = "Analiza apps instaladas, enlaces, notificaciones y redes Wi-Fi contra una base de inteligencia local. Escaneo SMS, TOTP 2FA y geofencing avanzado.",
            icon = "[radar]",
            accentColor = AuraGreen
        ),
        OnboardingPageV2(
            title = "FIREWALL DNS + VPN",
            subtitle = "Protección a nivel de red",
            description = "Bloquea dominios maliciosos antes de que carguen. Túnel VPN completo opcional. Todo el tráfico se filtra localmente sin servidores externos.",
            icon = "[lock]",
            accentColor = AuraAmber
        ),
        OnboardingPageV2(
            title = "PRIVACIDAD TOTAL",
            subtitle = "Sin rastros, sin cloud",
            description = "Cero telemetría. Cero datos en servidores. Cero root requerido. Tu información nunca sale del dispositivo. Código abierto y auditable.",
            icon = "[eye-off]",
            accentColor = AuraRed
        )
    )

    var page by remember { mutableIntStateOf(0) }
    val currentPage = pages[page]
    val titleAlpha = remember { Animatable(1f) }
    val subtitleAlpha = remember { Animatable(1f) }
    val descriptionAlpha = remember { Animatable(1f) }

    LaunchedEffect(page) {
        titleAlpha.snapTo(0f)
        subtitleAlpha.snapTo(0f)
        descriptionAlpha.snapTo(0f)
        delay(50)
        titleAlpha.animateTo(1f, animationSpec = tween(400))
        subtitleAlpha.animateTo(1f, animationSpec = tween(400))
        descriptionAlpha.animateTo(1f, animationSpec = tween(400))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
    ) {
        ScanLineOverlay()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            DataStreamColumn()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AuraGradientShield)
        )

        Crossfade(
            targetState = page,
            animationSpec = tween(500),
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageData = pages[pageIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(0.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pageData.icon,
                        color = pageData.accentColor,
                        fontSize = 40.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = pageData.title,
                        color = pageData.accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = pageData.subtitle,
                        color = AuraText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = pageData.description,
                        color = AuraTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { index, onboardingPage ->
                            val active = index == pageIndex
                            val dotColor = if (active) onboardingPage.accentColor else onboardingPage.accentColor.copy(alpha = 0.2f)

                            Surface(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clickable { page = index },
                                shape = CircleShape,
                                color = dotColor
                            ) {
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (pageIndex == pages.lastIndex) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onComplete() },
                            shape = RoundedCornerShape(12.dp),
                            color = AuraSurfaceRaised,
                            border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "COMENZAR",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                color = AuraCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { page = pageIndex + 1 },
                            shape = RoundedCornerShape(12.dp),
                            color = AuraBackground,
                            border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Siguiente",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                color = AuraCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
