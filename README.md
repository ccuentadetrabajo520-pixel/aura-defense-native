# AURA DEFENSE

## Estado real

AURA DEFENSE es una aplicación Android nativa en Jetpack Compose. El proyecto compila la base de la interfaz y contiene funciones locales de auditoría, análisis de aplicaciones, escaneo QR, informes, historial, bóveda, TOTP, descubrimiento LAN, monitorización de notificaciones y un servicio VPN de filtrado DNS.

Estas funciones son herramientas de apoyo y diagnóstico. No sustituyen al sistema de seguridad de Android, a un antivirus, a una auditoría profesional ni a copias de seguridad.

## Qué protege y qué no protege

- El generador de contraseñas usa `SecureRandom` y no guarda las contraseñas generadas.
- Las entradas TOTP se cifran con Android Keystore y fallan explícitamente si el Keystore no está disponible.
- El servicio VPN actual inspecciona principalmente consultas DNS locales sobre UDP/53 y las reenvía a un DNS ascendente. No es un túnel completo.
- La VPN no afirma bloquear DoH, DoT, QUIC, IPv6 ni todo el tráfico de aplicaciones. El modo de túnel completo no debe considerarse implementado hasta contar con reenvío y pruebas de todo el tráfico.
- La limpieza reforzada sobrescribe y elimina archivos de la aplicación, pero en almacenamiento flash, wear-leveling, snapshots o copias de seguridad no se puede garantizar la destrucción física de los datos.
- Los análisis de aplicaciones, enlaces, red, notificaciones y amenazas son heurísticos y pueden producir falsos positivos o no detectar una amenaza.

## Funciones implementadas

Auditoría de postura, escaneo local de aplicaciones y permisos, análisis de enlaces y archivos compartidos, escáner QR, informes TXT/JSON/HTML/PDF, historial, modo emergencia, monitorización opcional de notificaciones, escáner Bluetooth, herramientas TOTP y bóveda cifrada.

## Funciones demo o limitadas

La inteligencia de amenazas local depende de los datos disponibles en `assets/threats.json` y de la conectividad configurada. La VPN es un filtro DNS local, no una VPN de privacidad integral. El escáner LAN, las comprobaciones de integridad, la generación de certificados y los contadores de seguridad no constituyen una certificación de seguridad.

## Permisos sensibles

- `QUERY_ALL_PACKAGES`: permite enumerar aplicaciones instaladas para el escáner y la auditoría de permisos; es especialmente restrictivo en Google Play.
- `CAMERA`: se solicita solo para el escáner QR.
- `ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION`: se usan para funciones de ubicación y descubrimiento LAN/Bluetooth; Android puede exigir ubicación para ciertos resultados Bluetooth/Wi-Fi.
- `BLUETOOTH` hasta Android 11 y `BLUETOOTH_CONNECT` en versiones nuevas: permiten leer dispositivos Bluetooth vinculados.
- `INTERNET` y `ACCESS_NETWORK_STATE`: se usan para comprobaciones de red, consultas DNS y fuentes de amenazas.
- `CHANGE_WIFI_MULTICAST_STATE`: se usa para descubrimiento LAN.
- `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE`: mantienen activo el servicio VPN iniciado por el usuario.
- Servicio de notificaciones: el usuario debe habilitar manualmente el acceso de notification listener en Ajustes; las notificaciones pueden contener datos privados.
- VPN: Android muestra y controla el consentimiento para crear el túnel local.

No se solicitan permisos SMS: la auditoría solo revisa permisos declarados por otras aplicaciones y no lee mensajes del usuario.

## Privacidad

Los informes pueden incluir telemetría del dispositivo, nombres de aplicaciones, dominios bloqueados y resultados de análisis. Revísalos antes de compartirlos. El acceso a notificaciones, cámara, ubicación, Bluetooth y aplicaciones instaladas es opcional según la función. No incluyas secretos TOTP ni contraseñas en incidencias o informes.

## Compilación

Requisitos: Android SDK con API 35, JDK 17 y acceso a las dependencias de Gradle.

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

El APK se genera en `app/build/outputs/apk/debug/` o `app/build/outputs/apk/release/`. El flujo de Codemagic ejecuta `assembleRelease`; para distribuirlo hay que configurar el keystore y los secretos de firma en Codemagic. Sin ellos, el artefacto no debe considerarse firmado para publicación.

## Integridad

AURA registra al iniciar la aplicación la huella SHA-256 del certificado con el que fue instalada. Para verificar una distribución, compara el valor `Integridad de AURA: firma SHA-256 = ...` del registro local con la huella publicada junto al release firmado. Un valor distinto indica que el APK debe considerarse reempaquetado o firmado con otro certificado. La huella oficial se publicará con el primer release firmado de distribución.

## Codemagic

El workflow `aura-android-release` prepara el SDK, limpia el proyecto y compila release. La firma de distribución está pendiente de configurar mediante secretos y un keystore protegidos en Codemagic.

## Licencia

Este repositorio todavía no incluye una licencia. No se debe reutilizar ni redistribuir el código hasta que el propietario elija y añada una licencia explícita.

Las vulnerabilidades deben reportarse según [SECURITY.md](SECURITY.md).