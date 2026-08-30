package com.aura.defense.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════
// AURA DEFENSE — DESIGN SYSTEM v2.0
// Premium Cyberdefense Visual Identity
// ═══════════════════════════════════════════════════

// ── CORE PALETTE ──────────────────────────────────
val AuraBackground = Color(0xFF050A10)
val AuraSurface = Color(0xFF0C1820)
val AuraSurfaceRaised = Color(0xFF122A34)
val AuraSurfaceHigh = Color(0xFF1A3844)

val AuraCyan = Color(0xFF00E5FF)
val AuraTeal = Color(0xFF62E6D5)
val AuraGreen = Color(0xFF00FF88)
val AuraAmber = Color(0xFFFFB800)
val AuraRed = Color(0xFFFF3B5C)
val AuraPurple = Color(0xFFB794F6)
val AuraBlue = Color(0xFF3B82F6)
val AuraMuted = Color(0xFF5A7A8A)
val AuraText = Color(0xFFE8F6F3)
val AuraTextSecondary = Color(0xFF7A9AAA)

// ── GLOW COLORS ───────────────────────────────────
val AuraCyanGlow = Color(0x3300E5FF)
val AuraGreenGlow = Color(0x3300FF88)
val AuraRedGlow = Color(0x33FF3B5C)
val AuraCyanDim = Color(0xFF004D57)
val AuraGridLine = Color(0xFF0A1E28)
val AuraScanGlow = Color(0x1800E5FF)
val AuraDangerZone = Color(0x33FF3B5C)
val AuraDataGreen = Color(0xFF00CC6A)
val AuraWarmAmber = Color(0xFFFF9500)

// ── GRADIENTS ─────────────────────────────────────
val AuraGradientPrimary = Brush.horizontalGradient(listOf(AuraCyan, AuraTeal))
val AuraGradientCyan = Brush.verticalGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent))
val AuraGradientDanger = Brush.horizontalGradient(listOf(AuraAmber, AuraRed))
val AuraGradientSuccess = Brush.horizontalGradient(listOf(AuraGreen, AuraTeal))
val AuraGradientShield = Brush.radialGradient(listOf(AuraCyan.copy(alpha = 0.2f), Color.Transparent))
val AuraGradientSurface = Brush.verticalGradient(listOf(AuraSurface, AuraBackground))
val AuraGradientScan = Brush.verticalGradient(listOf(Color.Transparent, AuraCyan.copy(alpha = 0.08f), Color.Transparent))
val AuraGradientDangerZone = Brush.verticalGradient(listOf(Color.Transparent, AuraRed.copy(alpha = 0.1f), Color.Transparent))
val AuraGradientWarm = Brush.horizontalGradient(listOf(AuraWarmAmber, AuraAmber))
val AuraGradientCyber = Brush.linearGradient(listOf(AuraCyan.copy(alpha = 0.2f), AuraPurple.copy(alpha = 0.15f), AuraGreen.copy(alpha = 0.2f)))

// ── TYPOGRAPHY ────────────────────────────────────
val AuraTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 6.sp,
        color = AuraCyan
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp,
        color = AuraText
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 1.sp,
        color = AuraText
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = AuraText
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = AuraText
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        color = AuraText
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        color = AuraText
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        color = AuraTextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
        color = AuraCyan
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = AuraCyan
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 2.5.sp,
        color = AuraCyan
    )
)

// ── SPACING TOKENS ────────────────────────────────
object AuraSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// ── DURATION TOKENS ───────────────────────────────
object AuraDuration {
    const val FAST = 200
    const val NORMAL = 400
    const val SLOW = 800
}

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AuraCyan,
            onPrimary = Color(0xFF001A18),
            secondary = AuraGreen,
            tertiary = AuraAmber,
            error = AuraRed,
            background = AuraBackground,
            surface = AuraSurface,
            surfaceVariant = AuraSurfaceRaised,
            onBackground = AuraText,
            onSurface = AuraText,
            onSurfaceVariant = AuraMuted,
            outline = AuraSurfaceHigh,
            outlineVariant = AuraSurfaceRaised
        ),
        typography = AuraTypography,
        content = content
    )
}
