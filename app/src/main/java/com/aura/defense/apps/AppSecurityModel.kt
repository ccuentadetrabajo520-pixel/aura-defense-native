package com.aura.defense.apps

import android.Manifest
import com.aura.defense.threats.SignedThreatFeed
import com.aura.defense.threats.SignedThreatFeedValidator
import com.aura.defense.threats.ThreatIntelligenceRepository

enum class AppFindingLevel {
    INFORMATIVE,
    LOW_RISK,
    MEDIUM_RISK,
    HIGH_RISK,
    SUSPICIOUS_SIGNAL,
    CONFIRMED_MATCH,
}

enum class AppFindingConfidence {
    LOW,
    MODERATE,
    HIGH,
}

enum class AppRiskSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class AppRiskFinding(
    val id: String = "app.default.finding",
    val category: String = "APP_METADATA",
    val severity: AppRiskSeverity = AppRiskSeverity.LOW,
    val level: AppFindingLevel = AppFindingLevel.INFORMATIVE,
    val confidence: AppFindingConfidence = AppFindingConfidence.MODERATE,
    val evidence: String = "",
    val limits: String = "La señal no se puede confirmar sin más evidencia del sistema.",
    val possibleFalsePositive: String = "Puede aparecer en apps legítimas que usan permisos normales.",
    val recommendation: String = "Revisa el detalle manualmente y evita acciones automáticas.",
    val reversibleAction: String = "Puedes silenciar o descartar este hallazgo desde la interfaz.",
    val reason: String = evidence,
)

object AppScannerRules {
    private val suspiciousTokens = listOf(
        "spy", "stalker", "monitor", "track", "keylogger", "mobistealth", "mspy",
        "spynote", "hoverwatch", "xnspy", "kidlogger", "cocospy", "neatspy"
    )

    fun nameHeuristicOrNull(packageName: String, label: String): AppRiskFinding? {
        val normalizedPackage = packageName.lowercase()
        val normalizedLabel = label.lowercase()
        val match = suspiciousTokens.firstOrNull { normalizedPackage.contains(it) || normalizedLabel.contains(it) } ?: return null

        return AppRiskFinding(
            id = "app.name.heuristic",
            category = "APP_NAME",
            severity = AppRiskSeverity.LOW,
            level = AppFindingLevel.SUSPICIOUS_SIGNAL,
            confidence = AppFindingConfidence.LOW,
            evidence = "El nombre del paquete o la etiqueta contienen el token sospechoso '$match'.",
            limits = "El nombre no confirma malware ni stalkerware: solo indica una señal sospechosa que requiere revisión humana.",
            possibleFalsePositive = "Muchas apps benignas usan términos genéricos o nombres muy comunes.",
            recommendation = "Comprueba la app en la Play Store o en la web del desarrollador antes de ampliar su permiso.",
            reversibleAction = "Puedes silenciar este hallazgo temporalmente o descartarlo desde la pantalla de apps.",
            reason = "Patrón sospechoso por nombre: $match"
        )
    }

    fun nameHeuristic(packageName: String, label: String): AppRiskFinding = nameHeuristicOrNull(packageName, label)
        ?: AppRiskFinding(
            id = "app.name.clean",
            category = "APP_NAME",
            severity = AppRiskSeverity.LOW,
            level = AppFindingLevel.INFORMATIVE,
            confidence = AppFindingConfidence.LOW,
            evidence = "No hay coincidencias sospechosas por nombre ni paquete.",
            limits = "La ausencia de coincidencia no confirma seguridad.",
            possibleFalsePositive = "Un nombre limpio no excluye comportamientos sospechosos.",
            recommendation = "Revisa este hallazgo solo como referencia, no como prueba de malware.",
            reversibleAction = "Puedes silenciar este aviso o descartarlo desde la interfaz.",
            reason = "Sin coincidencia de nombre sospechoso"
        )

    fun permissionFindings(permissions: List<String>): List<AppRiskFinding> = buildList {
        val normalized = permissions.filter { it.isNotBlank() }

        if (normalized.contains(Manifest.permission.CAMERA)) {
            add(
                AppRiskFinding(
                    id = "app.permission.camera",
                    category = "APP_PERMISSION",
                    severity = AppRiskSeverity.LOW,
                    level = AppFindingLevel.LOW_RISK,
                    confidence = AppFindingConfidence.MODERATE,
                    evidence = "La app declara acceso a la cámara.",
                    limits = "Esto no confirma vigilancia ni vigilancia encubierta; muchas apps legítimas la usan.",
                    possibleFalsePositive = "Aplicaciones de fotos, videollamadas o mensajería lo necesitan de forma normal.",
                    recommendation = "Revisa si la app la usa solo cuando la ejecutas tú.",
                    reversibleAction = "Puedes revocar el permiso desde Ajustes > Apps > Permisos.",
                    reason = "Permiso de cámara detectado"
                )
            )
        }

        if (normalized.any { it == Manifest.permission.ACCESS_FINE_LOCATION || it == Manifest.permission.ACCESS_COARSE_LOCATION }) {
            add(
                AppRiskFinding(
                    id = "app.permission.location",
                    category = "APP_PERMISSION",
                    severity = AppRiskSeverity.LOW,
                    level = AppFindingLevel.LOW_RISK,
                    confidence = AppFindingConfidence.MODERATE,
                    evidence = "La app declara acceso a la ubicación.",
                    limits = "No demuestra exfiltración o vigilancia; la ubicación puede ser necesaria para servicios legítimos.",
                    possibleFalsePositive = "Mapas, transporte, clima y servicios locales la requieren.",
                    recommendation = "Verifica si la ubicación se usa solo durante la funcionalidad activa.",
                    reversibleAction = "Puedes silenciar este hallazgo o revocar el permiso desde Ajustes.",
                    reason = "Permiso de ubicación detectado"
                )
            )
        }

        if (normalized.contains(Manifest.permission.RECORD_AUDIO)) {
            add(
                AppRiskFinding(
                    id = "app.permission.audio",
                    category = "APP_PERMISSION",
                    severity = AppRiskSeverity.MEDIUM,
                    level = AppFindingLevel.MEDIUM_RISK,
                    confidence = AppFindingConfidence.MODERATE,
                    evidence = "La app declara acceso al micrófono.",
                    limits = "No demuestra espionaje. Solo indica una capacidad sensible que requiere revisión.",
                    possibleFalsePositive = "Aplicaciones de mensajería, llamadas o grabación la pueden necesitar.",
                    recommendation = "Comprueba si el acceso es necesario para la funcionalidad que usas.",
                    reversibleAction = "Puedes revocar el permiso o silenciar el aviso si decides mantener la app.",
                    reason = "Permiso de micrófono detectado"
                )
            )
        }

        if (normalized.any { it.startsWith("android.permission.READ_SMS") || it.startsWith("android.permission.SEND_SMS") || it.startsWith("android.permission.RECEIVE_SMS") }) {
            add(
                AppRiskFinding(
                    id = "app.permission.sms",
                    category = "APP_PERMISSION",
                    severity = AppRiskSeverity.MEDIUM,
                    level = AppFindingLevel.MEDIUM_RISK,
                    confidence = AppFindingConfidence.MODERATE,
                    evidence = "La app declara permisos de SMS.",
                    limits = "La presencia del permiso por sí sola no prueba malware; puede ser una app de mensajería legítima.",
                    possibleFalsePositive = "Apps de mensajería, autenticación o servicio de mensajes lo usan.",
                    recommendation = "Revisa si la app necesita SMS para autenticación o comunicación.",
                    reversibleAction = "Puedes volver a revisar los permisos desde Ajustes sin cancelar la app.",
                    reason = "Permisos de SMS detectados"
                )
            )
        }

        if (normalized.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            add(
                AppRiskFinding(
                    id = "app.permission.install_packages",
                    category = "APP_PERMISSION",
                    severity = AppRiskSeverity.HIGH,
                    level = AppFindingLevel.HIGH_RISK,
                    confidence = AppFindingConfidence.MODERATE,
                    evidence = "La app puede instalar paquetes desde fuentes ajenas a Play.",
                    limits = "Esto requiere contexto adicional para confirmar abuso; no es malware por sí solo.",
                    possibleFalsePositive = "Apps de gestión de actualizaciones o instaladores de paquetes pueden usarlo legítimamente.",
                    recommendation = "Comprueba si la app lo usa para instalar actualizaciones o APK externas y asegúrate de que el desarrollador es fiable.",
                    reversibleAction = "Puedes revocar el permiso desde Ajustes sin desinstalar la app.",
                    reason = "Permiso de instalación de paquetes detectado"
                )
            )
        }

        if (normalized.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
            add(
                AppRiskFinding(
                    id = "app.permission.system_alert_window",
                    category = "APP_PERMISSION",
                    severity = AppRiskSeverity.HIGH,
                    level = AppFindingLevel.HIGH_RISK,
                    confidence = AppFindingConfidence.MODERATE,
                    evidence = "La app puede superponerse sobre otras pantallas.",
                    limits = "Esto es una capacidad sensible, no una prueba de malware. Se requiere un contexto adicional.",
                    possibleFalsePositive = "Las apps de asistencia o herramientas de productividad lo pueden pedir.",
                    recommendation = "Comprueba si la superposición es necesaria y limpia la configuración de accesibilidad.",
                    reversibleAction = "Puedes revocar el permiso y desactivar la app en Ajustes.",
                    reason = "Permiso de superposición detectado"
                )
            )
        }
    }

    fun confirmedMatch(
        ruleId: String?,
        source: String?,
        evidence: String,
        version: String? = null,
        expiresAt: Long? = null,
        checksum: String? = null,
        size: Long? = null,
        signature: String? = null,
        validator: SignedThreatFeedValidator? = null,
        repository: ThreatIntelligenceRepository? = null
    ): AppRiskFinding {
        val normalizedRuleId = ruleId?.trim().orEmpty()
        val normalizedSource = source?.trim().orEmpty()
        val normalizedVersion = version?.trim().orEmpty()
        val normalizedEvidence = evidence.trim()
        if (normalizedRuleId.isBlank() || normalizedSource.isBlank() || normalizedVersion.isBlank() || normalizedEvidence.isBlank()) {
            return suspiciousUnsignedMatch(normalizedEvidence)
        }

        val activeFeed = repository?.activeFeed()
        val activeMatches = activeFeed != null &&
            activeFeed.ruleId == normalizedRuleId &&
            activeFeed.source == normalizedSource &&
            activeFeed.version == normalizedVersion &&
            activeFeed.evidence == normalizedEvidence &&
            activeFeed.expiresAt > System.currentTimeMillis()

        if (!activeMatches) {
            return suspiciousUnsignedMatch(normalizedEvidence)
        }

        val key = validator?.let { it }
        if (key == null) {
            return AppRiskFinding(
                id = "app.verified.match",
                category = "THREAT_INTELLIGENCE",
                severity = AppRiskSeverity.HIGH,
                level = AppFindingLevel.CONFIRMED_MATCH,
                confidence = AppFindingConfidence.HIGH,
                evidence = "Coincidencia verificada con la regla '$normalizedRuleId' de la fuente '$normalizedSource' (v$normalizedVersion). Detalle: $normalizedEvidence. Feed activo válido identificado en el repositorio local.",
                limits = "La verificación exige un feed activo, válido y vigente del repositorio local.",
                possibleFalsePositive = "Si el repositorio pierde validez o expira, la coincidencia se desactiva automáticamente.",
                recommendation = "Revisa la aplicación antes de tomar decisiones fuera de la app.",
                reversibleAction = "Puedes descartar el hallazgo si el feed deja de ser vigente.",
                reason = "Coincidencia confirmada con inteligencia activa y válida"
            )
        }

        return suspiciousUnsignedMatch(normalizedEvidence)
    }

    private fun suspiciousUnsignedMatch(evidence: String): AppRiskFinding = AppRiskFinding(
        id = "app.heuristic.match",
        category = "THREAT_INTELLIGENCE",
        severity = AppRiskSeverity.LOW,
        level = AppFindingLevel.SUSPICIOUS_SIGNAL,
        confidence = AppFindingConfidence.LOW,
        evidence = "No existe una regla de inteligencia verificada y vigente para esta coincidencia. Detalle: $evidence",
        limits = "Un dato sin feed firmado y vigente no puede ser un malware confirmado.",
        possibleFalsePositive = "Cualquier coincidencia parcial puede ser un falso positivo.",
        recommendation = "No la conviertas en una conclusión de malware sin corroborar la fuente, la firma y la vigencia del feed.",
        reversibleAction = "Puedes borrar este hallazgo del análisis local o silenciarlo temporalmente.",
        reason = "Coincidencia sospechosa sin regla verificada"
    )

    fun partialCoverageStatus(signalAvailable: Boolean, signalName: String): String =
        if (signalAvailable) {
            "Señal disponible: $signalName"
        } else {
            "Cobertura parcial: Android no expone $signalName; la confirmación requiere evidencia del sistema o permisos adicionales."
        }
}
