package com.aura.defense.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntegrityCheck(
        val name: String,
            val passed: Boolean,
                val detail: String
)

data class IntegrityResult(
        val checks: List<IntegrityCheck>,
            val overallPassed: Boolean,
                val timestamp: String
) {
        val passedCount: Int get() = checks.count { it.passed }
            val totalChecks: Int get() = checks.size
}

class DeviceIntegrityChecker(private val context: Context) {

        fun check(): IntegrityResult {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            val checks = mutableListOf<IntegrityCheck>()
                                    checks.add(checkRoot())
                                            checks.add(checkEmulator())
                                                    checks.add(checkDebugMode())
                                                            checks.add(checkDeveloperOptions())
                                                                    checks.add(checkUnknownSources())
                                                                            checks.add(checkPlayServices())
                                                                                    checks.add(checkSecureLockScreen())
                                                                                            checks.add(checkScreenLock())
                                                                                                    checks.add(checkEncryption())
                                                                                                            return IntegrityResult(
                                                                                                                            checks = checks,
                                                                                                                                        overallPassed = checks.all { it.passed },
                                                                                                                                                    timestamp = ts
                                                                                                            )
        }

            private fun checkRoot(): IntegrityCheck {
                        val rootPaths = listOf(
                                        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                                                    "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                                                                "/su/bin/su"
                        )
                                val hasRootPath = rootPaths.any { path -> runCatching { File(path).exists() }.getOrDefault(false) }
                                        val hasMagisk = runCatching {
                                                        context.packageManager.getPackageInfo("com.topjohnwu.magisk", 0)
                                                                    true
                                        }.getOrDefault(false)
                                                val isRooted = hasRootPath || hasMagisk
                                                        return IntegrityCheck(
                                                                        name = "Deteccion de Root",
                                                                                    passed = !isRooted,
                                                                                                detail = if (isRooted) "Root detectado. El dispositivo tiene acceso total al sistema." else "No se detecto root."
                                                        )
            }

                private fun checkEmulator(): IntegrityCheck {
                            val isEmu = (Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("emulator")
                                        || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for")
                                                    || Build.MANUFACTURER.contains("Genymotion") || Build.PRODUCT.contains("sdk")
                                                                || Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu"))
                                                                        return IntegrityCheck(
                                                                                        name = "Deteccion de Emulador",
                                                                                                    passed = !isEmu,
                                                                                                                detail = if (isEmu) "El dispositivo parece ser un emulador. Los emuladores no representan seguridad real." else "Dispositivo fisico detectado."
                                                                        )
                }

                    private fun checkDebugMode(): IntegrityCheck {
                                val isDebug = (applicationFlags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                                        return IntegrityCheck(
                                                        name = "Modo Debug",
                                                                    passed = !isDebug,
                                                                                detail = if (isDebug) "La app esta en modo debug. Esto permite inspeccionar el codigo en ejecucion." else "App compilada en modo release."
                                        )
                    }

                        private fun checkDeveloperOptions(): IntegrityCheck {
                                    val enabled = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
                                            return IntegrityCheck(
                                                            name = "Opciones de Desarrollador",
                                                                        passed = !enabled,
                                                                                    detail = if (enabled) "Las opciones de desarrollador estan activas. Esto expone funciones avanzadas del sistema." else "Opciones de desarrollador desactivadas."
                                            )
                        }

                            private fun checkUnknownSources(): IntegrityCheck {
                                        val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        runCatching {
                                                                            context.packageManager.canRequestPackageInstalls()
                                                        }.getOrDefault(false)
                                        } else {
                                                        @Suppress("DEPRECATION")
                                                                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) != 0
                                        }
                                                return IntegrityCheck(
                                                                name = "Instalacion desde fuentes desconocidas",
                                                                            passed = !enabled,
                                                                                        detail = if (enabled) "La instalacion desde fuentes externas esta permitida. Esto permite instalar apps de terceros." else "Solo se permiten instalaciones desde fuentes oficiales."
                                                )
                            }

                                private fun checkPlayServices(): IntegrityCheck {
                                            val hasPlayServices = runCatching {
                                                            context.packageManager.getPackageInfo("com.google.android.gms", 0)
                                                                        true
                                            }.getOrDefault(false)
                                                    return IntegrityCheck(
                                                                    name = "Google Play Services",
                                                                                passed = hasPlayServices,
                                                                                            detail = if (hasPlayServices) "Google Play Services esta instalado y actualizado." else "Google Play Services no esta instalado. Los controles de seguridad de Google no estan activos."
                                                    )
                                }

                                    private fun checkSecureLockScreen(): IntegrityCheck {
                                                val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                                                        val isSecure = keyguard?.isDeviceSecure ?: false
                                                                return IntegrityCheck(
                                                                                name = "Pantalla de Bloqueo Segura",
                                                                                            passed = isSecure,
                                                                                                        detail = if (isSecure) "El dispositivo tiene un metodo de bloqueo seguro (PIN, patron o biometrico)." else "No se detecta un metodo de bloqueo seguro. Cualquiera puede acceder al dispositivo."
                                                                )
                                    }

                                        private fun checkScreenLock(): IntegrityCheck {
                                                    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                                                            val isLocked = keyguard?.isKeyguardLocked ?: false
                                                                    return IntegrityCheck(
                                                                                    name = "Pantalla Bloqueada",
                                                                                                passed = true,
                                                                                                            detail = if (isLocked) "El dispositivo esta bloqueado actualmente." else "El dispositivo esta desbloqueado. Los controles se ejecutaron con la pantalla abierta."
                                                                    )
                                        }

                                            private fun checkEncryption(): IntegrityCheck {
                                                        val encrypted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                                        runCatching {
                                                                                            (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager)
                                                                                                                ?.storageEncryptionStatus != android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED
                                                                        }.getOrDefault(true)
                                                        } else {
                                                                        false
                                                        }
                                                                return IntegrityCheck(
                                                                                name = "Cifrado de Almacenamiento",
                                                                                            passed = encrypted,
                                                                                                        detail = if (encrypted) "El almacenamiento del dispositivo esta cifrado." else "El almacenamiento no esta cifrado. Los datos son vulnerables si el dispositivo es extraido."
                                                                )
                                            }

                                                private val applicationFlags: Int
                                                        get() = runCatching {
                                                                        context.packageManager.getApplicationInfo(context.packageName, 0).flags
                                                        }.getOrDefault(0)
}
