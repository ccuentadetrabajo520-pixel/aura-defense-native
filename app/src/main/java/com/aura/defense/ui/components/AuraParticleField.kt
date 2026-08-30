package com.aura.defense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

private data class FieldParticle(
        val x: Float,
            val y: Float,
                val vx: Float,
                    val vy: Float,
                        val alpha: Float,
                            val radius: Float
)

@Composable
fun ParticleField(modifier: Modifier = Modifier, particleCount: Int = 30) {
        val particles = remember { mutableStateOf(createFieldParticles(particleCount)) }

            LaunchedEffect(Unit) {
                        while (true) {
                                        delay(33)
                                                    val current = particles.value
                                                                val updated = current.map { p ->
                                                                                var nx = p.x + p.vx
                                                                                                var ny = p.y + p.vy
                                                                                                                if (nx > 1000f) nx = 0f
                                                                                                                                if (nx < 0f) nx = 1000f
                                                                                                                                                if (ny > 1000f) ny = 0f
                                                                                                                                                                if (ny < 0f) ny = 1000f
                                                                                                                                                                                FieldParticle(nx, ny, p.vx, p.vy, p.alpha, p.radius)
                                                                                                                                                                                            }
                                                                                                                                                                                                        particles.value = updated
                        }
            }

                Canvas(modifier = modifier.fillMaxSize()) {
                            val connectionDist = 100.dp.toPx()
                                    val scaleX = size.width / 1000f
                                            val scaleY = size.height / 1000f
                                                    val items = particles.value

                                                            for (i in items.indices) {
                                                                            val p = items[i]
                                                                                        val color = if (i % 2 == 0) AuraCyan else AuraGreen
                                                                                                    drawCircle(color.copy(alpha = p.alpha), p.radius, Offset(p.x * scaleX, p.y * scaleY))
                                                            }

                                                                    for (i in items.indices) {
                                                                                    val a = items[i]
                                                                                                val ax = a.x * scaleX
                                                                                                            val ay = a.y * scaleY
                                                                                                                        for (j in (i + 1) until items.size) {
                                                                                                                                            val b = items[j]
                                                                                                                                                            val bx = b.x * scaleX
                                                                                                                                                                            val by = b.y * scaleY
                                                                                                                                                                                            val dx = ax - bx
                                                                                                                                                                                                            val dy = ay - by
                                                                                                                                                                                                                            val dist = sqrt(dx * dx + dy * dy)
                                                                                                                                                                                                                                            if (dist < connectionDist) {
                                                                                                                                                                                                                                                                    drawLine(AuraCyan.copy(alpha = 0.06f), Offset(ax, ay), Offset(bx, by), 0.5.dp.toPx())
                                                                                                                                                                                                                                            }
                                                                                                                        }
                                                                    }
                }
}

private fun createFieldParticles(count: Int): List<FieldParticle> {
        val result = mutableListOf<FieldParticle>()
            for (i in 0 until count) {
                        result.add(
                                        FieldParticle(
                                                            x = Random.nextFloat() * 1000f,
                                                                            y = Random.nextFloat() * 1000f,
                                                                                            vx = (Random.nextFloat() * 0.6f) - 0.3f,
                                                                                                            vy = (Random.nextFloat() * 0.6f) - 0.3f,
                                                                                                                            alpha = Random.nextFloat() * 0.4f + 0.1f,
                                                                                                                                            radius = Random.nextFloat() * 1.5f + 1f
                                        )
                        )
            }
                return result
}
