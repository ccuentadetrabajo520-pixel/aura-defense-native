package com.aura.defense.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.aura.defense.history.ScoreHistoryStore
import com.aura.defense.reports.AuraPdfReportBuilder
import com.aura.defense.security.BreachCheckResult
import com.aura.defense.security.DeviceIntegrityChecker
import com.aura.defense.security.HaveIBeenPwnedChecker
import com.aura.defense.security.IntegrityResult
import com.aura.defense.security.PermissionAuditor
import com.aura.defense.security.PermissionAuditResult
import com.aura.defense.security.SecurityCertGenerator
import com.aura.defense.security.SecurityCertificate
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSpacing
import com.aura.defense.ui.AuraSurfaceRaised
import java.io.File

@Composable
fun AuraToolsHubDialog(onDismiss: () -> Unit) {
    var activeTool by remember { mutableStateOf<String?>(null) }
    var integrityResult by remember { mutableStateOf<IntegrityResult?>(null) }
    var auditResult by remember { mutableStateOf<PermissionAuditResult?>(null) }
    var breachResult by remember { mutableStateOf<BreachCheckResult?>(null) }
    var certificate by remember { mutableStateOf<SecurityCertificate?>(null) }
    var scoreHistory by remember { mutableStateOf<List<com.aura.defense.history.ScoreEntry>>(emptyList()) }
    var passwordInput by remember { mutableStateOf("") }
    var checkingHibp by remember { mutableStateOf(false) }
    var runningAudit by remember { mutableStateOf(false) }
    var runningIntegrity by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scoreStore = remember { ScoreHistoryStore(ctx) }

    when {
        activeTool == "integrity" && integrityResult != null -> {
            IntegrityCheckDialog(
                result = integrityResult!!,
                onDismiss = {
                    activeTool = null
                    integrityResult = null
                }
            )
        }

        activeTool == "certificate" && certificate != null -> {
            SecurityCertDialog(
                certificate = certificate!!,
                onDismiss = {
                    activeTool = null
                    certificate = null
                }
            )
        }

        else -> {
            AuraHudDialog(
                onDismissRequest = onDismiss,
                title = { Text("Herramientas Avanzadas") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolButton("Integridad del dispositivo") {
                            if (!runningIntegrity) {
                                runningIntegrity = true
                                integrityResult = DeviceIntegrityChecker(ctx).check()
                                runningIntegrity = false
                                activeTool = "integrity"
                            }
                        }

                        ToolButton("Auditoría de permisos de apps") {
                            if (!runningAudit) {
                                runningAudit = true
                                auditResult = PermissionAuditor(ctx).audit()
                                runningAudit = false
                                activeTool = "audit"
                            }
                        }

                        ToolDescription("Verificación de contraseñas (HaveIBeenPwned)")
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Contraseña a verificar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                if (passwordInput.isNotBlank() && !checkingHibp) {
                                    checkingHibp = true
                                    breachResult = HaveIBeenPwnedChecker(ctx).checkPassword(passwordInput)
                                    checkingHibp = false
                                }
                            },
                            enabled = passwordInput.isNotBlank() && !checkingHibp
                        ) {
                            Text(if (checkingHibp) "Verificando..." else "Verificar")
                        }

                        breachResult?.let { br ->
                            if (br.error != null) {
                                Text("Error: ${br.error}", color = AuraRed, fontSize = 12.sp)
                            } else if (br.isBreached) {
                                Text("${br.passwordMasked}: encontrada en ${br.breachCount} filtraciones", color = AuraRed, fontSize = 12.sp)
                            } else {
                                Text("${br.passwordMasked}: no encontrada en filtraciones", color = AuraGreen, fontSize = 12.sp)
                            }
                            Text("Verificado: ${br.checkedAt}", color = AuraMuted, fontSize = 10.sp)
                        }

                        ToolButton("Certificado de seguridad Aura") {
                            val integrity = integrityResult ?: DeviceIntegrityChecker(ctx).check()
                            integrityResult = integrity
                            val score = if (integrity.totalChecks > 0) {
                                (integrity.passedCount * 100) / integrity.totalChecks
                            } else 0
                            certificate = SecurityCertGenerator(ctx).generate(
                                score = score,
                                status = if (score >= 80) "Seguro" else if (score >= 60) "Moderado" else "Vulnerable",
                                findings = emptyList(),
                                integrityResult = integrity
                            )
                            activeTool = "certificate"
                        }

                        ToolButton("Historial de puntuaciones") {
                            scoreHistory = scoreStore.getScores()
                            activeTool = "scores"
                        }

                        ToolButton("Generar reporte PDF") {
                            val output = File(ctx.filesDir, "aura-report-${System.currentTimeMillis()}.pdf")
                            val generated = AuraPdfReportBuilder(ctx).generate(
                                score = 0,
                                status = "Pendiente",
                                findings = emptyList(),
                                appCount = 0,
                                riskyAppCount = 0,
                                vpnActive = false,
                                dnsStatus = "Desconocido",
                                outputPath = output
                            )
                            if (generated) {
                                runCatching {
                                    val uri = FileProvider.getUriForFile(
                                        ctx,
                                        "${ctx.packageName}.fileprovider",
                                        output
                                    )
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    ctx.startActivity(Intent.createChooser(share, "Compartir reporte PDF"))
                                }.onFailure {
                                    Toast.makeText(ctx, "PDF generado: ${output.absolutePath}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(ctx, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                            }
                        }

                        if (activeTool == "audit" && auditResult != null) {
                            Spacer(modifier = Modifier.height(AuraSpacing.sm))
                            Text("AUDITORÍA DE PERMISOS", color = AuraCyan, fontSize = 13.sp)
                            Text(
                                "Apps analizadas: ${auditResult!!.totalApps} | con permisos peligrosos: ${auditResult!!.appsWithDangerousPermissions}",
                                color = AuraMuted,
                                fontSize = 11.sp
                            )
                            Text("Evaluado: ${auditResult!!.timestamp}", color = AuraMuted, fontSize = 10.sp)
                            auditResult!!.entries.take(6).forEach { entry ->
                                val riskColor = when (entry.riskLevel) {
                                    com.aura.defense.security.PermissionRisk.CRITICAL -> AuraRed
                                    com.aura.defense.security.PermissionRisk.DANGEROUS -> AuraAmber
                                    com.aura.defense.security.PermissionRisk.SENSITIVE -> AuraCyan
                                    else -> AuraMuted
                                }
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("[${entry.riskLevel}] ${entry.appName}", color = riskColor, fontSize = 12.sp)
                                    Text(entry.reason, color = AuraMuted, fontSize = 10.sp)
                                }
                            }
                        }

                        if (activeTool == "scores") {
                            Spacer(modifier = Modifier.height(AuraSpacing.sm))
                            Text("HISTORIAL DE PUNTUACIONES", color = AuraCyan, fontSize = 13.sp)
                            val trend = scoreStore.getTrend()
                            val average = scoreStore.getAverageScore()
                            Text(
                                "Tendencia: $trend | Promedio: ${if (average >= 0) "$average/100" else "Sin datos"}",
                                color = AuraMuted,
                                fontSize = 11.sp
                            )
                            scoreHistory.reversed().take(10).forEach { entry ->
                                Text(
                                    "${entry.formattedTime} — ${entry.score}/100 — ${entry.status}",
                                    color = AuraMuted,
                                    fontSize = 11.sp
                                )
                            }
                            if (scoreHistory.isEmpty()) {
                                Text("No hay puntuaciones guardadas aún.", color = AuraMuted, fontSize = 11.sp)
                            }
                        }

                        Text(
                            "Todas las herramientas se ejecutan localmente. HIBP usa k-anonymity y la contraseña nunca sale del dispositivo.",
                            color = AuraMuted,
                            fontSize = 10.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            )
        }
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick = onClick),
        color = AuraSurfaceRaised,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.2f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = AuraSpacing.md),
            color = AuraCyan,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ToolDescription(text: String) {
    Text(text, color = AuraMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
}
