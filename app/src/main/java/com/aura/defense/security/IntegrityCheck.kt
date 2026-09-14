package com.aura.defense.security

import android.content.Context
import android.content.pm.PackageManager

object IntegrityCheck {
    /** Firma SHA-256 del certificado con el que AURA fue instalada. */
    fun ownSignatureSha256(context: Context): String? = runCatching {
        val packageManager = context.packageManager
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return null
        if (signatures.isEmpty()) return null
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.digest(signatures[0].toByteArray()).joinToString("") { "%02x".format(it) }
    }.getOrNull()

    fun report(context: Context): String {
        val hash = ownSignatureSha256(context) ?: "No disponible"
        com.aura.defense.monitor.AuraProcessLog.log(
            "Integridad de AURA: firma SHA-256 = $hash",
            "SISTEMA"
        )
        return hash
    }
}
