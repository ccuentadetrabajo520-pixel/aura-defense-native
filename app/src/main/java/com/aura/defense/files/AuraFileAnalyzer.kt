package com.aura.defense.files

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.security.MessageDigest
import java.io.File

 data class AuraFileAnalysis(
    val name: String,
    val size: Long,
    val extension: String,
    val sha256: String,
    val isApk: Boolean,
    val packageName: String?,
    val version: String?,
    val targetSdk: Int?,
    val requestedPermissions: List<String>,
    val sensitivePermissions: List<String>,
    val risk: String,
    val reasons: List<String>
)

class AuraFileAnalyzer(private val context: Context) {
    fun analyze(uri: Uri): AuraFileAnalysis = runCatching {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf("_display_name", "_size"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) ?: "archivo" else "archivo"
        } ?: uri.lastPathSegment.orEmpty().ifBlank { "archivo" }
        val size = resolver.query(uri, arrayOf("_size"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } ?: 0L
        val extension = name.substringAfterLast('.', "").lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        resolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(8192)
            var count: Int
            while (stream.read(buffer).also { count = it } >= 0) if (count > 0) digest.update(buffer, 0, count)
        }
        val apk = extension == "apk"
        val packageInfo = if (apk) readApk(uri) else null
        val permissions = packageInfo?.requestedPermissions?.toList().orEmpty()
        val sensitive = permissions.filter { it in SENSITIVE_PERMISSIONS }
        val reasons = buildList {
            if (extension in setOf("exe", "bat", "sh", "js", "vbs")) add("Extensión ejecutable requiere revisión")
            if (sensitive.isNotEmpty()) add("Solicita permisos sensibles")
            if (size > 100L * 1024 * 1024) add("Archivo de gran tamaño")
            if (isEmpty()) add("No se observaron señales locales por extensión o metadatos")
        }
        AuraFileAnalysis(name, size, extension, digest.digest().joinToString("") { "%02x".format(it) }, apk, packageInfo?.packageName, packageInfo?.versionName, packageInfo?.applicationInfo?.targetSdkVersion, permissions, sensitive, if (sensitive.isNotEmpty() || extension in setOf("exe", "bat", "sh", "js", "vbs")) "Riesgo potencial" else "Sin señales", reasons)
    }.getOrElse { AuraFileAnalysis("archivo", 0L, "", "No disponible", false, null, null, null, emptyList(), emptyList(), "No disponible", listOf("No se pudo analizar el archivo")) }

    private fun readApk(uri: Uri): PackageInfo? = runCatching {
        val cached = File.createTempFile("aura-apk-", ".apk", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> cached.outputStream().use(input::copyTo) } ?: return null
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(cached.absolutePath, PackageManager.GET_PERMISSIONS)
        } finally {
            cached.delete()
        }
    }.getOrNull()

    private companion object { val SENSITIVE_PERMISSIONS = setOf("android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.RECORD_AUDIO", "android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS", "android.permission.REQUEST_INSTALL_PACKAGES") }
}
