package com.aura.defense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var radius: Float
)

@Composable
fun ParticleField(modifier: Modifier = Modifier, particleCount: Int = 30) {
    val particles = remember { mutableStateOf(initParticles(particleCount = particleCount)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(33)
            val updatedList = particles.value.toMutableList()
            val width = 1000f
            val height = 1000f

            for (index in updatedList.indices) {
                val particle = updatedList[index]
                particle.x += particle.vx
                particle.y += particle.vy

                if (particle.x < 0f) {
                    particle.x = width
                } else if (particle.x > width) {
                    particle.x = 0f
                }

                if (particle.y < 0f) {
                    particle.y = height
                } else if (particle.y > height) {
                    particle.y = 0f
                }
            }

            particles.value = updatedList
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val connectionDistance = 100.dp.toPx()

        val liveParticles = particles.value
        val updatedParticles = liveParticles.toMutableList()

        for (index in updatedParticles.indices) {
            val particle = updatedParticles[index]
            val color = if (index % 2 == 0) AuraCyan else AuraGreen
            drawCircle(
                color = color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(particle.x, particle.y)
            )
        }

        for (index in updatedParticles.indices) {
            val first = updatedParticles[index]
            for (innerIndex in (index + 1) until updatedParticles.size) {
                val second = updatedParticles[innerIndex]
                val deltaX = first.x - second.x
                val deltaY = first.y - second.y
                val distance = kotlin.math.sqrt((deltaX * deltaX) + (deltaY * deltaY))
                if (distance < connectionDistance) {
                    drawLine(
                        color = AuraCyan.copy(alpha = 0.06f),
                        start = Offset(first.x, first.y),
                        end = Offset(second.x, second.y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
        }
    }
}

private fun initParticles(particleCount: Int): MutableList<Particle> {
    val particles = mutableListOf<Particle>()
    for (index in 0 until particleCount) {
        val x = Random.nextFloat() * 1000f
        val y = Random.nextFloat() * 1000f
        val vx = (Random.nextFloat() * 0.6f) - 0.3f
        val vy = (Random.nextFloat() * 0.6f) - 0.3f
        val alpha = Random.nextFloat() * 0.4f + 0.1f
        val radius = Random.nextFloat() * 1.5f + 1f
        val particle = Particle(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            alpha = alpha,
            radius = radius
        )
        particles.add(particle)
    }
    return particles
}
