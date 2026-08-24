package com.aura.defense.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AuraBackground = Color(0xFF071216)
val AuraSurface = Color(0xFF10242A)
val AuraSurfaceRaised = Color(0xFF163139)
val AuraCyan = Color(0xFF62E6D5)
val AuraGreen = Color(0xFF8CE6A0)
val AuraAmber = Color(0xFFFFC66D)
val AuraRed = Color(0xFFFF8A86)
val AuraMuted = Color(0xFF8BA6A4)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AuraCyan,
            onPrimary = Color(0xFF00201D),
            secondary = AuraGreen,
            tertiary = AuraAmber,
            error = AuraRed,
            background = AuraBackground,
            surface = AuraSurface,
            surfaceVariant = AuraSurfaceRaised,
            onBackground = Color(0xFFE5F4F1),
            onSurface = Color(0xFFE5F4F1),
            onSurfaceVariant = AuraMuted
        ),
        content = content
    )
}
