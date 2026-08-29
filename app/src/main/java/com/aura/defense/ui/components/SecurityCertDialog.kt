package com.aura.defense.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.security.SecurityCertificate
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSpacing

@Composable
fun SecurityCertDialog(certificate: SecurityCertificate, onDismiss: () -> Unit) {
        val ctx = LocalContext.current
            val classification = when {
                        certificate.score >= 90 -> "EJEMPLAR - Excelente postura de seguridad"
                                certificate.score >= 80 -> "SEGURO - Buena postura con mejoras menores"
                                        certificate.score >= 60 -> "MODERADO - Se recomienda mejorar la seguridad"
                                                certificate.score >= 40 -> "VULNERABLE - Brechas significativas detectadas"
                                                        else -> "CRITICO - Se requiere accion inmediata"
            }
                val classificationColor = when {
                            certificate.score >= 80 -> AuraGreen
                                    certificate.score >= 60 -> AuraCyan
                                            certificate.score >= 40 -> com.aura.defense.ui.AuraAmber
                                                    else -> AuraRed
                }
                    AuraHudDialog(
                                onDismissRequest = onDismiss,
                                        title = { Text("Certificado de Seguridad Aura") },
                                                text = {
                                                                Column(
                                                                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                                                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                                    Text("${certificate.score}/100", color = AuraCyan, fontSize = 28.sp)
                                                                                                    Text(certificate.status, color = if (certificate.score >= 60) AuraGreen else AuraRed, fontSize = 14.sp)
                                                                                                                    Spacer(Modifier.height(AuraSpacing.sm))
                                                                                                                                    Text("Integridad: ${if (certificate.integrityPassed) "APROBADA" else "NO APROBADA"}", color = if (certificate.integrityPassed) AuraGreen else AuraRed, fontSize = 13.sp)
                                                                                                                                                    Text("Controles: ${certificate.passedChecks}/${certificate.totalChecks} aprobados", color = AuraMuted, fontSize = 12.sp)
                                                                                                                                                                    Text("Dispositivo: ${certificate.deviceId}", color = AuraMuted, fontSize = 12.sp)
                                                                                                                                                                                    Text("Generado: ${certificate.generatedAt}", color = AuraMuted, fontSize = 12.sp)
                                                                                                                                                                                                    Spacer(Modifier.height(AuraSpacing.sm))
                                                                                                                                                                                                                    Text(classification, color = classificationColor, fontSize = 12.sp)
                                                                                                                                                                                                                                    if (certificate.findings.isNotEmpty()) {
                                                                                                                                                                                                                                                            Spacer(Modifier.height(AuraSpacing.sm))
                                                                                                                                                                                                                                                                                Text("Hallazgos:", color = AuraCyan, fontSize = 11.sp)
                                                                                                                                                                                                                                                                                                    certificate.findings.take(10).forEach { finding ->
                                                                                                                                                                                                                                                                                                                            Text("  - $finding", color = AuraMuted, fontSize = 10.sp)
                                                                                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                    }
                                                                }
                                                },
                                                        confirmButton = {
                                                                        TextButton(onClick = {
                                                                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                                                                                    type = "text/plain"
                                                                                                                                        putExtra(Intent.EXTRA_TEXT, certificate.toText())
                                                                                                                                                            putExtra(Intent.EXTRA_SUBJECT, "Aura Defense - Security Certificate")
                                                                                            }
                                                                                                            ctx.startActivity(Intent.createChooser(intent, "Compartir certificado"))
                                                                        }) { Text("Compartir") }
                                                                                    TextButton(onClick = {
                                                                                                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                                                                                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("cert", certificate.toText()))
                                                                                    }) { Text("Copiar") }
                                                                                                TextButton(onClick = onDismiss) { Text("Cerrar") }
                                                        }
                    )
}
