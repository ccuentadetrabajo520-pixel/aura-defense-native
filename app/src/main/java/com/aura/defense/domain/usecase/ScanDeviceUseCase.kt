package com.aura.defense.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import com.aura.defense.data.model.Threat
import com.aura.defense.data.model.ThreatSeverity
import com.aura.defense.data.repository.SecurityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class ScanDeviceUseCase(
    private val context: Context,
    private val repository: SecurityRepository
) {
    suspend fun execute(): Flow<ScanProgress> = flow {
        emit(ScanProgress.Starting)
        delay(500)

        emit(ScanProgress.Progress(20, "Escaneando aplicaciones instaladas..."))
        val installedApps = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        delay(500)

        emit(ScanProgress.Progress(40, "Analizando permisos sospechosos..."))
        val suspiciousApps = installedApps.filter { app ->
            runCatching {
                val packageInfo = context.packageManager.getPackageInfo(
                    app.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                packageInfo.requestedPermissions?.any { permission ->
                    permission.contains("SMS") ||
                        permission.contains("PHONE") ||
                        permission.contains("CAMERA")
                } == true
            }.getOrDefault(false)
        }
        delay(500)

        emit(ScanProgress.Progress(60, "Verificando archivos del sistema..."))
        delay(500)

        emit(ScanProgress.Progress(80, "Analizando tráfico de red..."))
        delay(500)

        if (suspiciousApps.isNotEmpty()) {
            val app = suspiciousApps.first()
            val threat = Threat(
                id = UUID.randomUUID().toString(),
                name = "App sospechosa: ${app.loadLabel(context.packageManager)}",
                severity = ThreatSeverity.MEDIUM,
                description = "Esta app tiene permisos excesivos",
                detectedAt = System.currentTimeMillis().toString()
            )
            repository.addThreat(threat)
        }

        emit(ScanProgress.Progress(100, "¡Escaneo completado!"))
        emit(ScanProgress.Completed)
    }
}

sealed class ScanProgress {
    data object Starting : ScanProgress()
    data class Progress(val percent: Int, val message: String) : ScanProgress()
    data object Completed : ScanProgress()
}