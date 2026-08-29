package com.aura.defense.ui.components
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraCyanGlow
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurfaceHigh
import com.aura.defense.ui.AuraTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(val x: Float, val y: Float, val speed: Float, val radius: Float, val baseAlpha: Float)

@Composable
fun AuraCoreIntro() {
        val tr = rememberInfiniteTransition(label = "sp")
            val rotation = tr.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "r")
                val corePulse = tr.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "p")
                    val sonar = tr.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "s")
                        val scan = tr.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "l")
                            val titleA = remember { Animatable(0f) }
                                val subA = remember { Animatable(0f) }
                                    val ls = remember { Animatable(12f) }
                                        val prog = remember { Animatable(0f) }
                                            LaunchedEffect(Unit) { delay(800); titleA.animateTo(1f, tween(2000)); ls.animateTo(6f, tween(2000)); delay(600); subA.animateTo(1f, tween(1500)); prog.animateTo(1f, tween(3000, easing = LinearEasing)) }
                                                val particles = remember { List(25) { Particle(Random.nextFloat(), Random.nextFloat(), 0.0003f + Random.nextFloat() * 0.0008f, 1f + Random.nextFloat() * 2f, 0.2f + Random.nextFloat() * 0.5f) } }
                                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                                                Canvas(Modifier.fillMaxSize()) {
                                                                                val w = size.width; val h = size.height; val cx = w / 2f; val cy = h * 0.36f; val br = w * 0.28f
                                                                                            drawGrid(w, h); drawStreams(w, h, scan.value); drawSonar(cx, cy, br, sonar.value)
                                                                                                        drawParts(particles); drawRadar(cx, cy, br, rotation.value, corePulse.value); drawScan(w, h, scan.value)
                                                                }
                                                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center), verticalArrangement = Arrangement.Center) {
                                                                                        Spacer(Modifier.height(220.dp))
                                                                                                    Text("AURA DEFENSE", style = MaterialTheme.typography.displayLarge, color = AuraCyan.copy(alpha = titleA.value), letterSpacing = ls.value.sp, textAlign = TextAlign.Center)
                                                                                                                Spacer(Modifier.height(10.dp))
                                                                                                                            Text("DEFENSA MÓVIL PRIVADA", style = MaterialTheme.typography.labelSmall, color = AuraTextSecondary.copy(alpha = subA.value), textAlign = TextAlign.Center)
                                                                        }
                                                                                Canvas(Modifier.fillMaxSize().align(Alignment.BottomCenter)) {
                                                                                                val bw = size.width * 0.6f; val bh = 2.dp.toPx(); val bx = (size.width - bw) / 2f; val by = size.height - 60.dp.toPx()
                                                                                                            drawRect(AuraSurfaceHigh, Offset(bx, by), Size(bw, bh))
                                                                                                                        drawRect(AuraCyan.copy(alpha = 0.9f), Offset(bx, by), Size(bw * prog.value, bh))
                                                                                                                                    if (prog.value > 0.02f) drawCircle(AuraCyan.copy(alpha = 0.6f), 4.dp.toPx(), Offset(bx + bw * prog.value, by + bh / 2f))
                                                                                }
                                                    }
}

private fun DrawScope.drawGrid(w: Float, h: Float) {
        val c = AuraCyan.copy(alpha = 0.035f); val sp = 42.dp.toPx(); val d = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 8.dp.toPx()))
            var x = 0f; while (x < w) { drawLine(c, Offset(x, 0f), Offset(x, h), 0.5f, pathEffect = d); x += sp }
                var y = 0f; while (y < h) { drawLine(c, Offset(0f, y), Offset(w, y), 0.5f, pathEffect = d); y += sp }
}

private fun DrawScope.drawStreams(w: Float, h: Float, phase: Float) {
        val sc = AuraCyan.copy(alpha = 0.04f); val bc = AuraCyan.copy(alpha = 0.08f); val ls = 18.dp.toPx(); val lx = 12.dp.toPx(); val rx = w - 12.dp.toPx()
            for (i in 0 until 6) {
                        val yb = ((phase * h * 1.5f + i * ls * 3.7f) % (h + 200f)) - 100f
                                drawLine(sc, Offset(lx, yb), Offset(lx, yb + 80.dp.toPx()), 1.dp.toPx())
                                        drawLine(sc, Offset(rx, yb + 40f), Offset(rx, yb + 120.dp.toPx()), 1.dp.toPx())
                                                drawCircle(bc, 2.dp.toPx(), Offset(lx, yb)); drawCircle(bc, 2.dp.toPx(), Offset(rx, yb + 40f))
            }
}

private fun DrawScope.drawSonar(cx: Float, cy: Float, r: Float, ph: Float) {
        for (i in 0..2) { val p = ((ph + i * 0.33f) % 1f); val cr = r * (0.2f + p * 0.9f); drawCircle(AuraCyan.copy(alpha = (1f - p) * 0.12f), cr, Offset(cx, cy), style = Stroke(1.dp.toPx())) }
}

private fun DrawScope.drawParts(particles: List<Particle>) {
        val t = System.currentTimeMillis().coerceAtMost(60000).toFloat()
            particles.forEach { p ->
                    var y = ((p.y - p.speed * t) % 1.2f + 1.2f) % 1.2f - 0.1f
                            val a = p.baseAlpha * (1f - (y / 1.1f).coerceIn(0f, 1f))
                                    if (a > 0.02f) drawCircle(AuraCyan.copy(alpha = a), p.radius, Offset(p.x * size.width, y * size.height))
                                        }
}

private fun DrawScope.drawRadar(cx: Float, cy: Float, r: Float, rot: Float, pulse: Float) {
        floatArrayOf(0.35f, 0.65f, 1f).zip(floatArrayOf(0.14f, 0.09f, 0.05f)).forEach { (f, a) -> drawCircle(AuraCyan.copy(alpha = a), r * f, Offset(cx, cy), style = Stroke(0.8.dp.toPx())) }
            val cl = r * 1.05f; val cc = AuraCyan.copy(alpha = 0.06f)
                drawLine(cc, Offset(cx - cl, cy), Offset(cx + cl, cy), 0.5.dp.toPx()); drawLine(cc, Offset(cx, cy - cl), Offset(cx, cy + cl), 0.5.dp.toPx())
                    val sr = Math.toRadians(rot.toDouble()); val ex = cx + r * cos(sr).toFloat(); val ey = cy + r * sin(sr).toFloat()
                        drawLine(AuraCyan.copy(alpha = 0.7f), Offset(cx, cy), Offset(ex, ey), 1.5.dp.toPx())
                            drawArc(Brush.sweepGradient(listOf(AuraCyan.copy(alpha = 0.18f), AuraCyan.copy(alpha = 0f)), Offset(cx, cy)), rot - 45f, 90f, true, Offset(cx - r, cy - r), Size(r * 2, r * 2), alpha = 0.5f, blendMode = BlendMode.Plus)
                                floatArrayOf(0f, 90f, 180f, 270f).forEach { ang ->
                                        val rd = Math.toRadians((ang + rot * 0.5).toDouble()); val nr = r * 0.35f
                                                val np = ((sin(System.currentTimeMillis() * 0.003 + ang) + 1f) / 2f).toFloat()
                                                        drawCircle(AuraGreen.copy(alpha = 0.3f + np * 0.5f), 3.dp.toPx(), Offset(cx + nr * cos(rd).toFloat(), cy + nr * sin(rd).toFloat()))
                                                            }
                                                                drawCircle(AuraCyanGlow, 26.dp.toPx(), Offset(cx, cy)); drawCircle(AuraCyan.copy(alpha = pulse * 0.2f), 18.dp.toPx(), Offset(cx, cy))
                                                                    drawCircle(AuraCyan.copy(alpha = pulse), 5.dp.toPx(), Offset(cx, cy))
}

private fun DrawScope.drawScan(w: Float, h: Float, ph: Float) {
        val y = ph * h; val g = Brush.verticalGradient(listOf(AuraCyan.copy(alpha = 0.06f), AuraCyan.copy(alpha = 0f)), y - 40.dp.toPx(), y + 40.dp.toPx())
            drawRect(g, Offset(0f, y - 40.dp.toPx()), Size(w, 80.dp.toPx())); drawLine(AuraCyan.copy(alpha = 0.08f), Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
}