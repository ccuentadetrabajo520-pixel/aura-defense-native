# AURA DEFENS

> Asistente local de ciberdefensa, con voz opcional y evidencia verificable,
> para Android sin root. Hecho en Venezuela.

## Qué es AURA

AURA no es un antivirus tradicional ni una VPN comercial. Es un asistente conversacional de ciberdefensa que vive en el dispositivo Android y combina un firewall DNS local, análisis de aplicaciones, postura de seguridad, herramientas de red y orientación de seguridad en español.

Su defensa de red usa una `VpnService` local para filtrar consultas DNS, con inteligencia embebida y feeds públicos actualizables. También reúne señales de aplicaciones, red, notificaciones, integridad y estado del dispositivo para producir hallazgos trazables. La interfaz es conversacional y tiene un núcleo holográfico animado, rostro y voz.

Todas las capacidades están limitadas por las APIs de Android y por los permisos que el usuario concede. AURA procura decir cuando un dato no está disponible y no presenta una inferencia como una certeza.

## Características (verificadas en código)

### Defensa de red

- Firewall DNS local mediante `VpnService`, con bloqueo por categorías y feeds públicos actualizables de StevenBlack y URLhaus, además de inteligencia embebida.
- El estado observable es único: `OFF`, `REQUESTING_PERMISSION`, `STARTING`, `ACTIVE_DNS_ONLY`, `DEGRADED`, `STOPPING` o `ERROR`. Solo `ACTIVE_DNS_ONLY` se muestra como **Protección DNS activa**.
- La cobertura demostrable es DNS UDP/53 IPv4 que atraviesa el descriptor de AURA. La interfaz muestra como no cubiertos HTTPS/TLS, DoH, DoT, QUIC, TCP/UDP arbitrario e IPv6.
- Cada bloqueo conserva evidencia local mínima: motivo, categoría, fuente, versión/fecha del feed y hora. Los eventos tienen retención limitada, pueden permitirse temporalmente y pueden borrarse desde Defensa.
- Feeds de dominios y una blocklist de IP de URLhaus; el código conserva caché y registra el estado de las fuentes.
- Perfiles por categoría: Equilibrado, Estricto y Permitir todo, con allowlist y blocklist manual.
- Detección de posible DNS hijacking mediante comparación entre resolución del sistema y una consulta DoH de Cloudflare.
- Detección de red insegura y evaluación de la postura de seguridad, con sugerencias para revisar o reforzar la configuración.
- Registro de bloqueos DNS y procesos observables en la consola de AURA.

### Defensa del dispositivo

- Escáner de aplicaciones instaladas: permisos declarados, instalador de origen y señales de riesgo.
- Detección heurística de un patrón de troyano bancario mediante instalación reciente, origen externo y señales de accesibilidad, SMS o superposición.
- Correlación de señales para identificar un posible stalkerware: administrador del dispositivo, listener de notificaciones y señales del escáner.
- Verificación de postura: root, Magisk, depuración USB, bloqueo de pantalla, servicios de accesibilidad y otras señales visibles para Android.
- Edad de dominios mediante RDAP; los dominios recientes pueden generar una alerta de riesgo.
- Monitor de tráfico por aplicación con `NetworkStats`, incluyendo bytes transferidos en las últimas 24 horas.
- Análisis local de enlaces, archivos compartidos y códigos QR, además de herramientas de red, Bluetooth y LAN.

### Asistente local

- Conversación local en español, con una base de conocimiento curada de 49 llamadas `entry(` verificadas en código y conceptos adicionales compactos; incluye señales, explicaciones y recomendaciones.
- Consulta real de dominios contra la inteligencia disponible: el asistente puede responder si un dominio aparece en los feeds y consultar su edad por RDAP.
- Conversación general separada de evaluaciones locales basadas en evidencia estructurada.
- Acciones sensibles con confirmación explícita; AURA no instala ni desinstala aplicaciones, modifica ajustes ocultos ni bloquea por decisión del modelo.
- Voz bidireccional: reconocimiento de voz mediante el servicio de Android y respuesta hablada mediante TTS cuando el dispositivo lo permite.
- Informes exportables y herramientas de diagnóstico; no existe en el código actual una capacidad independiente identificable como "modo pánico".

### Autodefensa

- Anti-screenshot mediante `FLAG_SECURE` y protección contra interacción con overlays, para reducir riesgos de captura y tapjacking sobre su propia interfaz.
- Vigía propio: comprueba si la VPN dejó de estar activa, si el acceso de notificaciones fue revocado, si hay señales de cambio de paquete o si la aplicación está siendo retirada.
- Verificación de integridad de su propia firma mediante SHA-256.
- Datos sensibles protegidos con `EncryptedSharedPreferences` y Android Keystore, con manejo explícito cuando Keystore no está disponible.
- Caja negra y registros exportables mediante `FileProvider`, incluyendo el log de procesos y diagnósticos disponibles.

### Transparencia radical

- Consola con registros reales de procesos y bloqueos observables en tiempo de ejecución.
- La interfaz prioriza Cobertura actual, evidencia y límites; cualquier puntuación interna no equivale a una certificación de seguridad.
- Confianza declarada: cuando falta un dato, AURA lo expresa y no inventa una respuesta.

## Qué AURA NO hace (límites reales de Android sin root)

- No inspecciona la memoria privada de otras aplicaciones ni puede garantizar la detección de rootkits o componentes ocultos fuera de su alcance.
- No lee ni descifra el tráfico cifrado de otras aplicaciones. La protección implementada es DNS local UDP/53 IPv4; no es un túnel completo ni una VPN comercial de privacidad integral.
- No cubre automáticamente DNS cifrado de otras aplicaciones (DoH/DoT), QUIC, TCP/UDP arbitrario, IPv6 ni tráfico que no atraviese `VpnService`.
- No elimina malware ya instalado ni desinstala aplicaciones por el usuario.
- No garantiza detectar todas las amenazas, troyanos, stalkerware, fraudes, hijacking o exfiltraciones. Sus detectores son heurísticos y pueden producir falsos positivos.
- No puede asegurar la destrucción física de archivos en almacenamiento flash, snapshots o copias de seguridad.
- No sustituye las protecciones del sistema Android, un antivirus especializado, una auditoría profesional ni un proceso de respuesta a incidentes.

La honestidad sobre estos límites es parte del diseño.

## Privacidad

El procesamiento principal y la base de conocimiento funcionan localmente. No hay cuentas, analytics ni telemetría operativa de AURA. Las conexiones salientes verificables en el código son:

- Descarga de feeds públicos de dominios e IP maliciosos desde GitHub/StevenBlack y URLhaus, más el feed remoto propio de AURA.
- Resolución DNS de comparación mediante DoH de Cloudflare.
- Consulta RDAP del dominio que el usuario solicita verificar.
- Verificación opcional de un correo mediante Have I Been Pwned, solo si el usuario introduce una API key y ejecuta la herramienta.
- Verificación opcional de contraseñas mediante el endpoint de rangos de Have I Been Pwned, sin enviar la contraseña completa.

No existe integración con MalwareBazaar en el código actual. Las notificaciones, aplicaciones instaladas, cámara, ubicación, Bluetooth y red solo se consultan para las funciones que el usuario activa y autoriza. Los eventos DNS locales no se escriben en logs de diagnóstico con dominios, URLs, IPs o payloads; la actividad conservada tiene retención limitada y borrado manual. Nada más sale del dispositivo según las conexiones identificadas en el código.

## Asistente Fase 4

El asistente separa conversación general de diagnóstico local. El modo predeterminado es local y no envía contexto fuera del dispositivo. Las evaluaciones usan únicamente cobertura DNS/VPN, estado del feed firmado, eventos DNS reales, hallazgos, postura y límites de Android; no convierten nombres de apps, dominios o contenido externo en instrucciones.

La interfaz muestra siempre **Cobertura actual**, evidencia y límites. El mapa declara qué observa cada capa y qué queda fuera de alcance. Las acciones sensibles requieren confirmación explícita y las acciones no soportadas no se ejecutan.

Ejemplos de respuestas honestas:

- **Saludo:** “Hola. Puedo explicar ciberseguridad o revisar evidencia local cuando me lo pidas.”
- **Pregunta técnica:** explica el concepto y aclara que una explicación general no evalúa el teléfono.
- **“¿Mi teléfono está seguro?”:** “No tengo evidencia suficiente para confirmar ese riesgo”.
- **DNS detenido:** muestra “Cobertura actual: DNS detenida” y propone iniciar protección con confirmación.
- **Señal sospechosa:** muestra el hecho observado, la incertidumbre y la recomendación; no declara malware.
- **Coincidencia confirmada:** solo usa esa clasificación con `CONFIRMED_MATCH` respaldado por feed firmado, vigente y activo.
- **Evidencia insuficiente:** usa exactamente “No tengo evidencia suficiente para confirmar ese riesgo”.

Detalles: [AURA_ASSISTANT_DESIGN.md](AURA_ASSISTANT_DESIGN.md), [AURA_ASSISTANT_PRIVACY.md](AURA_ASSISTANT_PRIVACY.md), [AURA_ASSISTANT_TOOL_POLICY.md](AURA_ASSISTANT_TOOL_POLICY.md) y [AURA_ASSISTANT_TEST_MATRIX.md](AURA_ASSISTANT_TEST_MATRIX.md).

## Documentación de la Fase 1

- [Línea base de seguridad](BASELINE_SECURITY_AUDIT.md)
- [Diseño de protección DNS](DNS_PROTECTION_DESIGN.md)
- [Matriz manual de pruebas DNS](DNS_PROTECTION_TEST_MATRIX.md)

## Requisitos

Android 8.0+ (API 26). Sin root. Sin cuenta. Compilación actual con `compileSdk` y `targetSdk` 34, JDK 17 y Android SDK. La voz depende de los servicios disponibles en el dispositivo.

## Compilación y distribución

- CI/CD: Codemagic (`codemagic.yaml`).
- Canal de pruebas: APK debug generada por `assembleDebug` y firmada por el sistema de compilación.
- Release con keystore estable: en preparación; el flujo actual de Codemagic compila la APK debug.

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

Los artefactos aparecen en `app/build/outputs/apk/debug/` o `app/build/outputs/apk/release/`. La firma de distribución debe configurarse antes de publicar un release.

## Verificación de integridad

AURA registra al iniciar la aplicación la huella SHA-256 del certificado con el que fue instalada. Para verificar una distribución, compara el valor `Integridad de AURA: firma SHA-256 = ...` del registro local con la huella publicada junto al release firmado. Un valor distinto indica que la APK debe considerarse reempaquetada o firmada con otro certificado. La huella oficial se publicará con el primer release firmado de distribución.

## Inteligencia de amenazas — atribuciones

- StevenBlack hosts (MIT).
- URLhaus de abuse.ch.
- OpenPhish Community, presente en los datos de inteligencia embebidos.

Gracias a sus mantenedores. Las listas pueden contener errores ajenos a AURA; permite o bloquea dominios manualmente y revisa el contexto de cada hallazgo.

## Estructura del proyecto

```text
com.aura.defense/
├── vpn/             # VPN local y firewall DNS
├── threats/         # Inteligencia y feeds de amenazas
├── apps/            # Escáner de aplicaciones
├── security/        # Postura, integridad y herramientas de seguridad
├── ai/              # Copilot, conocimiento, voz y edad de dominios
├── monitor/         # Correlación, logs y autodefensa
├── guardian/        # Evaluación y recomendaciones del guardián
├── scheduler/       # Comprobaciones programadas
├── notifications/   # Listener y protección de notificaciones
├── files/           # Análisis de archivos
├── tools/           # Herramientas de análisis
├── vault/           # Bóveda local
├── lan/             # Descubrimiento LAN
├── history/         # Historial y línea base
├── reports/         # Informes
├── ui/              # Interfaz Compose y núcleo holográfico
└── data/            # Preferencias, telemetría y repositorios
```

## Roadmap

- v1.2: firma del feed de inteligencia, integración de Play Integrity y Protección Avanzada de Android (AAPM).
- v2.0: firewall por aplicación, con bloqueo de Internet para aplicaciones individuales.

Estas metas no se presentan como funciones disponibles en la versión actual.

## Estado

Proyecto público en desarrollo activo. La rama `main` contiene una auditoría ofensiva interna y capacidades verificables en el código fuente de este repositorio. Los resultados siguen dependiendo de la versión de Android, permisos, conectividad, datos disponibles y comportamiento de cada dispositivo.

Las vulnerabilidades deben reportarse según [SECURITY.md](SECURITY.md).