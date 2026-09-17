# AURA DEFENSE: línea base de seguridad (fase 0)

Fecha de auditoría: 2026-09-17
Plataforma objetivo: Android de consumo, `minSdk 26`, `targetSdk 34`, sin root.
Alcance: estado del árbol `main` observado en esta auditoría. No se evaluó un APK firmado ni comportamiento en todos los fabricantes.

## Criterio de clasificación

- **Real**: existe un mecanismo Android ejecutable, una ruta de datos comprobable y un resultado observable.
- **Parcial**: el mecanismo existe, pero su cobertura, activación, frescura, permisos o comprobación es limitada.
- **Interfaz sin backend**: la UI ofrece una capacidad que no tiene una implementación conectada o no está conectada al flujo principal.
- **No permitida sin root**: Android no concede el control requerido a una app normal. Puede haber una aproximación parcial, pero no debe prometerse la capacidad completa.

## Inventario de pantallas y UX

| Superficie | Evidencia | Clasificación | Límite verificable |
|---|---|---|---|
| Arranque, términos y onboarding | `ui/AuraAppRoot.kt`, `ui/screens/AuraIntroScreen.kt`, `data/AuraPreferences.kt` | Real | Guarda preferencias locales; no es una señal de protección.
| Consola | `ui/AuraMainShell.kt`, `ui/screens/AuraConsoleScreen.kt` | Parcial | Muestra postura, escaneo y VPN; la postura no equivale a tráfico protegido.
| Inicio, Defensa, Apps, Auras | `ui/AuraMainShell.kt`, `ui/screens/AppScreens.kt` | Parcial | Las cuatro rutas están conectadas; cada indicador depende de permisos, servicio o datos disponibles.
| Escaneo y VPN heredados | `ui/screens/ScanScreen.kt`, `ui/screens/VpnScreen.kt` | Parcial / interfaz no conectada al shell principal | Implementan acciones locales, pero no son las pantallas que decide `AuraMainShell`.
| Herramientas, QR, archivos, enlaces, bóveda, informes, historial | `ui/components/*Dialog.kt`, `tools/`, `files/`, `vault/`, `reports/`, `history/` | Parcial | Son análisis locales o exportaciones; no son detección antimalware completa ni verificación de servidor.
| Asistente y voz | `ai/interaction/VirtualAssistant.kt`, `ai/voice/*`, `ai/CopilotBrain.kt` | Parcial | Respuesta determinista/local y comandos; no hay modelo remoto ni evidencia de una IA autónoma.
| Auras/LAN | `ui/screens/AppScreens.kt`, `lan/AuraLanDiscovery.kt` | Parcial | Broadcast UDP en red local, sin autenticación criptográfica ni garantía de identidad del peer.

## Permisos y componentes Android

| Capacidad | Evidencia | Clasificación | Limitaciones Android/Google Play |
|---|---|---|---|
| Inventario de paquetes | `AndroidManifest.xml` (`QUERY_ALL_PACKAGES`), `apps/AppScanner.kt`, `security/PermissionAuditor.kt` | Parcial | La visibilidad amplia de paquetes está restringida por Google Play y exige justificar una función principal permitida. El resultado depende de visibilidad y OEM.
| Cámara y QR | `CAMERA`, `ui/components/QrScannerDialog.kt`, dependencias CameraX/ML Kit | Real | Requiere consentimiento runtime; analiza el contenido capturado, no prueba que un enlace sea seguro por sí mismo.
| Micrófono/voz | `RECORD_AUDIO`, `ai/AuraVoice.kt`, `ai/voice/*` | Parcial | Requiere consentimiento runtime y políticas de datos; no hay protección de llamadas ni escucha permanente demostrada.
| Ubicación y LAN | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ui/AuraMainShell.kt`, `lan/*` | Parcial | El descubrimiento local se puede ejecutar tras permiso; no demuestra seguridad de la red ni geolocalización continua.
| Uso de apps | `PACKAGE_USAGE_STATS`, `security/PrivacyAuditor.kt` | Parcial | Es acceso especial concedido desde Ajustes, no permiso runtime. Sin él, `lastUsed` queda desconocido.
| Almacenamiento | `READ_EXTERNAL_STORAGE`, `files/AuraFileAnalyzer.kt`, File Picker/URI | Parcial | En Android moderno el permiso está deprecado/restringido; un URI concedido por el usuario no equivale a acceso al almacenamiento completo.
| Notificaciones | `POST_NOTIFICATIONS`, `notifications/AuraNotificationListenerService.kt` | Parcial | El listener requiere activación explícita en Ajustes y trata contenido sensible; la app solo extrae URLs de notificaciones recibidas.
| Bluetooth/Wi-Fi/multicast | `BLUETOOTH*`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE` | Parcial | El manifiesto declara permisos, pero no hay evidencia de un escáner Bluetooth conectado al flujo principal; multicast no es garantía de visibilidad de todos los peers.
| Internet y estado de red | `INTERNET`, `ACCESS_NETWORK_STATE`, `ThreatFeedService.kt`, `ThreatFeedManager.kt` | Real / parcial | HTTPS y límites de tamaño/tiempo existen; las fuentes externas pueden fallar, cambiar o quedar obsoletas.
| Foreground service | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `vpn/AuraVpnService.kt` | Real | Android 14 exige tipo, declaración y revisión de uso especial; Play puede rechazar usos que no encajen en una categoría válida.
| Arranque | `RECEIVE_BOOT_COMPLETED`, `vpn/BootReceiver.kt` | Parcial | El receptor solo registra que debe reactivarse. Android no permite iniciar silenciosamente una VPN de usuario tras reinicio.

## Servicios, receptores y proveedor

| Componente | Evidencia | Clasificación | Observación |
|---|---|---|---|
| VPN local DNS | `vpn/AuraVpnService.kt`, `vpn/AuraFullTunnelEngine.kt`, `vpn/DnsPacketCodec.kt` | Real, alcance parcial | Establece `VpnService`, intercepta DNS UDP/53 y reenvía a `1.1.1.1`; no es túnel completo ni inspecciona todo el tráfico HTTPS. Requiere aprobación del usuario y compite con otra VPN.
| Listener de notificaciones | `notifications/AuraNotificationListenerService.kt` | Real, alcance parcial | Analiza URLs de texto publicado y guarda alertas locales; no ve notificaciones no entregadas ni contenido protegido por apps.
| Quick Settings tile | `quicktile/AuraQuickTileService.kt` | Real, alcance parcial | Abre AURA; el tile no activa directamente la VPN.
| Boot receiver | `vpn/BootReceiver.kt` | Real como señal, no como autoarranque | No restablece protección automáticamente.
| Self-destruct receiver | `monitor/SelfDestructWatcher.kt` | Parcial | Reacciona a cambios/remoción de paquetes según el sistema; no puede impedir una desinstalación ni proteger contra root.
| FileProvider | `AndroidManifest.xml`, `res/xml/file_paths.xml` | Real | Comparte informes/logs mediante URI no exportada y permisos temporales.
| `service/NotificationService.kt`, `service/VpnService.kt`, `threats/ThreatFeedService.kt` | Es wrappers/helper, no declaraciones del manifiesto | Parcial | No deben confundirse con servicios Android en ejecución.

## Datos, repositorios y persistencia

| Módulo | Evidencia | Clasificación |
|---|---|---|
| Preferencias e identidad | `data/AuraPreferences.kt`, `data/SecurePrefs.kt` | Real | Estado local; `SecurePrefs` protege preferencias concretas, no convierte todos los archivos en almacenamiento seguro.
| DNS/firewall | `vpn/DnsFirewallStore.kt`, `vpn/ProfileManager.kt` | Real, alcance parcial | Guarda perfiles, allowlist/blocklist y eventos; la efectividad depende de que el túnel esté realmente establecido.
| Inteligencia de amenazas JSON | `threats/ThreatIntelligenceRepository.kt`, `threats/ThreatIntelligenceEngine.kt`, `assets/threats.json`, `feed/threats.json` | Real, parcial | Valida JSON, tamaño, campos y checksum si existe; la base incluida no prueba actualidad ni cobertura.
| Feed hosts legacy | `vpn/ThreatFeedManager.kt` | Real, parcial | Descarga tres feeds y usa caché/ETag; no hay firma criptográfica de fuente y el estado es global en memoria.
| Escaneo de apps/permisos | `apps/AppScanner.kt`, `engine/MalwareDetectionEngine.kt`, `security/PermissionAuditor.kt` | Parcial | Heurísticas sobre metadatos, permisos, accesibilidad e instalador; no analiza código, memoria, comportamiento completo ni apps invisibles.
| Privacidad/telemetría | `security/PrivacyAuditor.kt`, `data/DeviceTelemetry.kt` | Parcial | Lee señales permitidas por Android; los accesos especiales pueden faltar y algunos valores se degradan a “No disponible”.
| Historial/informes | `history/*`, `reports/*`, `vault/AuraVault.kt` | Real, alcance parcial | Persistencia local y exportación; no hay sincronización remota ni garantía forense.

## Red y detección

| Capacidad | Evidencia | Clasificación |
|---|---|---|
| Firewall DNS local | `vpn/AuraVpnService.kt`, `vpn/ThreatBridge.kt`, `vpn/DnsFirewallStore.kt` | Real, parcial |
| Feed remoto HTTPS | `threats/ThreatFeedService.kt`, `ThreatIntelligenceRepository.kt`, `vpn/ThreatFeedManager.kt` | Real, parcial |
| Detección de dominios/URLs | `ThreatIntelligenceEngine.kt`, `tools/LinkAnalyzer.kt`, `security/SocialEngineDetector.kt` | Real, parcial |
| Malware/stalkerware heurístico | `engine/MalwareDetectionEngine.kt`, `apps/AppScanner.kt` | Parcial |
| Captura de paquetes, MITM, inspección TLS | Ausente; Android sandbox/VpnService no entrega claves TLS de otras apps | No permitida sin root / no prometible |
| Bloqueo de todas las conexiones o desinstalación forzada | Ausente y fuera de APIs normales | No permitida sin root |
| Descubrimiento LAN | `lan/AuraLanDiscovery.kt` | Parcial |

## Contrato de estado protegido

La auditoría encontró que la puntuación general podía producir `status = "Protegido"` en `security/SecurityPostureEngine.kt` sin significar que la VPN estuviera funcionando. Además, la pestaña Defensa, el Inicio y el informe de emergencia tenían rutas que podían depender solo de `isVpnRunning` o de la puntuación.

Desde esta fase, `security/ProtectionReadiness.kt` exige simultáneamente:

1. `VpnService` observado como activo.
2. Al menos una entrada de feed cargada en memoria.
3. Motor de inteligencia con indicadores activos.

`ui/AuraAppRoot.kt` actualiza el tamaño del feed y `ui/AuraMainShell.kt` usa ese contrato antes de pasar `"Protegido"` a `DefenseScreen`, `HomeScreen` o el informe de emergencia. El contrato está cubierto por `ProtectionReadinessTest.kt`. La puntuación de postura sigue siendo diagnóstico del dispositivo y no debe interpretarse como cobertura de red.

## Modelo de amenazas: Android de consumo sin root

### Activos

- Consultas DNS, dominios visitados y alertas derivadas de notificaciones.
- Identidad AURA, preferencias, listas DNS, historial, informes y bóveda local.
- Contenido compartido, archivos analizados, QR, voz y metadatos de apps instaladas.
- Disponibilidad e integridad del túnel VPN y de la base de amenazas.

### Actores y escenarios

| Actor/escenario | Vector | Impacto | Controles actuales | Residual |
|---|---|---|---|---|
| App maliciosa instalada | Overlay/tapjacking, accesibilidad, lectura de notificaciones, paquetes | Robo de interacción o datos | Bloqueo de toques obstruidos, hallazgos de accesibilidad, `FLAG_SECURE` | Alto: no puede revocar permisos de otra app ni garantizar que no exista captura por hardware/OEM.
| Phishing/malware remoto | Dominio malicioso, feed obsoleto o DNS fuera del túnel | Navegación a contenido dañino | Blocklists, heurísticas URL, VPN DNS | Alto: cobertura DNS, no inspección TLS, feeds no firmados.
| Feed comprometido o indisponible | MITM de infraestructura, repositorio alterado, respuesta inválida | Falsos negativos o bloqueo incorrecto | HTTPS, validación JSON, tamaño/checksum opcional, caché | Alto: no hay firma/verificación de pinning para todas las fuentes.
| Otra VPN o revocación del sistema | Competencia por `VpnService`, kill del proceso, batería/OEM | Protección ausente sin que el usuario lo note | `isRunning`, capacidades de red, estado persistido, watchdog | Medio/alto: no se puede garantizar exclusividad ni ejecución continua.
| Pérdida/robo del dispositivo | Acceso físico, backup, extracción de archivos | Exposición de historial/informes | `allowBackup=false`, bóveda y preferencias seguras parciales, `FLAG_SECURE` | Medio: no elimina todos los artefactos locales ni protege contra dispositivo desbloqueado.
| Usuario concede permisos excesivos | Cámara, micrófono, ubicación, listener, paquetes | Privacidad y superficie de ataque | Onboarding y auditores | Medio: Play y Android dejan la decisión final al usuario.
| Entorno rooteado/debuggable | Manipulación de proceso, archivos o red | Pérdida de integridad | `DeviceIntegrityChecker`, hallazgos de debug/root | Crítico: fuera del modelo sin root; no es controlable por la app.
| Peer LAN no autenticado | Broadcast UDP y respuesta falsificada | Confusión de identidad/telemetría | IDs y timeout | Alto: protocolo sin autenticación ni cifrado.

### Supuestos y fuera de alcance

La línea base supone un dispositivo no rooteado, bootloader no comprometido, Android funcionando correctamente y un usuario que revisa permisos. AURA no puede prometer antimalware kernel, antivirus de archivos en tiempo real, inspección de contenido cifrado, protección contra otra VPN, ni resistencia a un atacante con root.

## Indicadores de madurez (1 mínimo, 10 objetivo)

| Dominio | Nivel actual | Evidencia | Para alcanzar 10 |
|---|---:|---|---|
| Red | 4/10 | VPN DNS local funcional, perfiles, blocklists y estado verificable | Pruebas OEM, fail-closed verificable, cobertura IPv4/IPv6 completa, telemetría de caída, autenticación de feed y límites de privacidad.
| Inteligencia de amenazas | 4/10 | Base incluida, feed remoto, validación JSON y caché | Fuentes firmadas/versionadas, provenance, frescura medible, deduplicación consistente y pruebas de regresión de cobertura.
| Privacidad | 4/10 | `allowBackup=false`, `FLAG_SECURE`, almacenamiento local, auditores | Minimización formal, consentimiento granular, política de retención, cifrado de todos los artefactos y revisión de datos Play.
| IA | 2/10 | Reglas locales, conocimiento y comandos de voz | Definir límites, evaluación reproducible, trazabilidad de fuentes, protección de prompts/datos y proveedor explícito si se usa remoto.
| UX | 4/10 | Shell Compose con estados de VPN, escaneo, diálogos y onboarding | Estados honestos por módulo, accesibilidad, pruebas por dispositivo, errores accionables y eliminar rutas heredadas no conectadas.
| Supply chain | 3/10 | Gradle reproducible en intención, dependencias declaradas y ProGuard | Lockfiles/verificación de dependencias, SBOM, firmas de artefactos, escaneo CI, actualización controlada y procedencia de feeds.

## Hallazgos críticos y riesgos de lanzamiento

1. **La protección de red no es completa**: `VpnService` solo maneja el flujo DNS local y no inspecciona HTTPS ni todo el tráfico. No debe comunicarse como VPN de privacidad total.
2. **La inteligencia tiene dos rutas y dos estados**: `ThreatIntelligenceEngine` usa JSON validado, mientras `ThreatFeedManager` usa hosts externos con caché. La fuente, frescura y estado deben presentarse por separado.
3. **La visibilidad de paquetes y el listener de notificaciones son superficies reguladas**: pueden bloquear publicación o requerir declaración/justificación en Google Play.
4. **La LAN carece de autenticación**: un peer puede falsificar una respuesta UDP.
5. **La comprobación de build está bloqueada en este entorno**: Gradle falla antes de compilar con la versión de SDK `25.0.4.1`; el IDE no reporta errores en los archivos tocados. Se debe repetir en CI o con un SDK instalado correctamente.

## Tres tareas de mayor impacto

1. Unificar `ThreatIntelligenceEngine` y `ThreatFeedManager` bajo un único estado de inteligencia firmado, fresco y observable por la UI.
2. Definir y probar el alcance exacto del firewall DNS, incluyendo caída del servicio, competencia con otra VPN, IPv6 y fabricantes Android representativos.
3. Preparar revisión de privacidad/Google Play: justificar o retirar `QUERY_ALL_PACKAGES`, documentar notification listener y accesos especiales, y publicar una política de retención/cifrado de datos.

## Evidencia de verificación ejecutada

- El IDE no reportó errores en `ProtectionReadiness.kt`, `AuraAppRoot.kt`, `AuraMainShell.kt` ni `ProtectionReadinessTest.kt`.
- `./gradlew :app:testDebugUnitTest --tests com.aura.defense.security.ProtectionReadinessTest` no pudo ejecutarse: el entorno Gradle falló con `25.0.4.1` antes de compilar. No se debe interpretar como test aprobado.