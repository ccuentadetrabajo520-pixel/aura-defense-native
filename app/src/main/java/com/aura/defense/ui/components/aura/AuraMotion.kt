package com.aura.defense.ui.components.aura

import androidx.compose.animation.core.CubicBezierEasing

object AuraMotion {
    const val Micro = 150
    const val Short = 220
    const val Medium = 350
    const val Long = 650
    const val Breath = 2800
    val AuraEase = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}
