package com.aura.defense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AuraBlue,
    secondary = AuraCyan,
    background = AuraBackground,
    surface = AuraSurface,
    onPrimary = Color.Black,
    onBackground = AuraWhite,
    onSurface = AuraWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AuraDarkBlue,
    secondary = AuraCyan,
    background = Color(0xFFF5F7FA),
    surface = AuraWhite,
    onPrimary = AuraWhite,
    onBackground = AuraDarkBlue,
    onSurface = AuraDarkBlue
)

@Composable
fun AuraDefenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraTypography,
        content = content
    )
}
