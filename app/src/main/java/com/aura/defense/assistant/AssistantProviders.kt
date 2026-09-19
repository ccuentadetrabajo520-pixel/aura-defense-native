package com.aura.defense.assistant

import android.content.Context
import com.aura.defense.ThreatIntelligenceRepositoryProvider
import com.aura.defense.security.PostureResult
import com.aura.defense.threats.ThreatIntelligenceRepository
import com.aura.defense.vpn.DnsFirewallStore
import com.aura.defense.vpn.DnsProtectionStateStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface AssistantModelProvider {
    suspend fun respond(input: String, level: AssistantExplanationLevel): AssistantResponse
}

class LocalAssistantModelProvider : AssistantModelProvider {
    override suspend fun respond(input: String, level: AssistantExplanationLevel): AssistantResponse {
        val normalized = input.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, "Escribe una pregunta y te responderé.")
        if (normalized.matches(Regex("(hola|buenos dias|buenas tardes|buenas noches|hey)[!. ]*"))) {
            return AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, "Hola. Puedo explicar ciberseguridad o revisar evidencia local cuando me lo pidas.")
        }
        val knowledge = when {
            normalized.contains("phishing") -> "El phishing intenta suplantar a una persona o servicio para obtener datos. Verifica el dominio desde un canal oficial y no entregues credenciales desde enlaces inesperados."
            normalized.contains("ransomware") -> "El ransomware intenta bloquear o cifrar datos para exigir un rescate. Mantén copias separadas y evita abrir archivos inesperados."
            normalized.contains("dns") -> "DNS traduce nombres de dominio a direcciones. Puede filtrar destinos conocidos, pero no inspecciona el contenido cifrado ni garantiza que un sitio sea legítimo."
            normalized.contains("seguro") || normalized.contains("protegido") -> INSUFFICIENT_EVIDENCE
            else -> null
        }
        return AssistantResponse(
            AssistantResponseType.GENERAL_RESPONSE,
            knowledge ?: "Puedo explicar conceptos de seguridad sin consultar tu teléfono. Para evaluar el dispositivo necesito que solicites una revisión local explícita.",
            limitations = listOf("Esta respuesta general no es una evaluación del teléfono.")
        )
    }
}

class CloudAssistantModelProvider : AssistantModelProvider {
    override suspend fun respond(input: String, level: AssistantExplanationLevel): AssistantResponse =
        AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, "El proveedor en la nube está desactivado por defecto y no realiza llamadas.")
}

class AssistantEvidenceProvider(
    private val context: Context,
    private val repository: ThreatIntelligenceRepository = ThreatIntelligenceRepositoryProvider.get(context),
    private val postureProvider: () -> PostureResult
) {
    fun read(): AssistantEvidenceContext {
        val state = DnsProtectionStateStore.current()
        val feed = repository.activeFeed()
        val now = System.currentTimeMillis()
        val dnsEvents = DnsFirewallStore(context).blockedEvents().takeLast(20).map {
            AssistantDnsEvent(it.category, it.source, it.feedVersion, it.ruleId, it.timestamp)
        }
        val posture = postureProvider()
        val requestedPermissions = runCatching {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
                .requestedPermissions.orEmpty().associateWith { permission ->
                    androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
        }.getOrDefault(emptyMap())
        return AssistantEvidenceContext(
            coverage = AssistantCoverage(
                dnsStatus = state.status.name,
                feedStatus = repository.state().name,
                feedSource = feed?.source ?: "No disponible",
                feedVersion = feed?.version ?: "No disponible",
                feedUpdatedAt = feed?.feedTimestamp?.let { format(it) } ?: "No disponible",
                feedExpiresAt = feed?.expiresAt,
                postureStatus = posture.status,
                postureTimestamp = posture.timestamp,
                appCoverage = "Parcial: Android solo expone señales visibles y permisos consultables",
                privacy = "Procesamiento local; historial y actividad borrables",
                auraPermissions = requestedPermissions,
                limitations = listOf(
                    "No inspecciona memoria privada ni descifra TLS de otras apps.",
                    "La protección DNS depende de las consultas que atraviesan el VPN local.",
                    "Android sin root limita la cobertura de apps y servicios.",
                    "Lectura tomada a las ${format(now)}."
                )
            ),
            dnsEvents = dnsEvents,
            findings = posture.findings.map { AssistantFinding(it.title, it.severity.name, it.evidence, posture.timestamp) }
        )
    }

    private fun format(value: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(value))
}