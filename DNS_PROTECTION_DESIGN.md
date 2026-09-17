# Diseño de protección DNS de AURA DEFENSE

## Alcance de la Fase 1

AURA ofrecerá únicamente un firewall DNS local basado en `VpnService`. La promesa observable será **Protección DNS activa** cuando el servicio haya establecido un túnel y esté procesando consultas DNS verificables.

Esta fase no implementa ni promete un túnel completo. No se interceptan ni reenvían arbitrariamente IPv4, IPv6, TCP o UDP. El flujo de datos protegido es el de consultas DNS que realmente entren al descriptor de la VPN y puedan ser parseadas por `DnsPacketCodec`.

## Restricciones heredadas de la línea base

- Android de consumo sin root y con consentimiento explícito de `VpnService`.
- Una app normal no puede inspeccionar TLS, leer claves de otras apps, forzar exclusividad frente a otra VPN ni impedir que el sistema mate el proceso.
- El feed local/remoto puede estar vacío, inválido, obsoleto o indisponible. Un fallo de inteligencia no bloqueará por defecto.
- El listener de notificaciones, `QUERY_ALL_PACKAGES`, ubicación y otros accesos especiales no son prerrequisitos para el firewall DNS.
- No se añadirá telemetría, analítica, SDK remoto ni una fuente externa nueva.

## Arquitectura

```text
UI / MainActivity
  -> solicitud de permiso VpnService
  -> AuraVpnService
       -> DnsProtectionStateStore (estado emitido en memoria + snapshot local no autoritativo)
       -> VpnService.Builder (solo rutas DNS explícitas IPv4/IPv6)
       -> DnsPacketCodec (parseo DNS)
       -> DnsDecisionEngine (allowlist, blocklist, inteligencia válida)
       -> DnsActivityStore (eventos mínimos, retención y borrado)
       -> upstream DNS por socket protegido
       -> notificación foreground / logs redactados
  -> estado observado del servicio, nunca una preferencia guardada
```

### Fuentes de decisión

1. Allowlist temporal o permanente local: `ALLOW`, con precedencia explícita y motivo.
2. Blocklist local: `BLOCK`, con motivo `manual_rule`.
3. Indicadores válidos de `ThreatIntelligenceEngine` y `ThreatFeedManager`: `BLOCK` si la categoría pertenece al perfil activo.
4. Dominio sin coincidencia: `ALLOW` operacionalmente, con resultado interno `UNKNOWN` si no hay evidencia suficiente. No se bloquea por defecto.
5. Entrada corrupta, paquete no parseable o fallo de la decisión: `ERROR`; no se inventa un bloqueo y se intenta reenviar solo si el paquete es una consulta DNS válida.

La fuente, categoría, versión/fecha del feed y hora quedan en la evidencia de un bloqueo. El dominio se normaliza y se almacena con el mínimo necesario; no se conservan URL completas, payloads DNS, IPs del usuario ni contenido de tráfico.

## Máquina de estados única

```text
OFF
  -> REQUESTING_PERMISSION
  -> STARTING
  -> ACTIVE_DNS_ONLY
  -> DEGRADED
  -> STOPPING
  -> ERROR
```

### Significado y transiciones

| Estado | Significado | Transiciones válidas |
|---|---|---|
| `OFF` | No hay protección DNS activa ni solicitud en curso | `REQUESTING_PERMISSION`, `STARTING` |
| `REQUESTING_PERMISSION` | Android está mostrando o esperando la autorización del usuario | `STARTING`, `OFF`, `ERROR` |
| `STARTING` | El servicio está creando el foreground service, cargando decisión y estableciendo el descriptor VPN | `ACTIVE_DNS_ONLY`, `DEGRADED`, `ERROR`, `STOPPING` |
| `ACTIVE_DNS_ONLY` | El descriptor existe, el hilo DNS funciona y la actividad está observable | `DEGRADED`, `STOPPING`, `ERROR` |
| `DEGRADED` | El descriptor puede existir, pero se perdió una cobertura o dependencia concreta | `ACTIVE_DNS_ONLY`, `STOPPING`, `ERROR` |
| `STOPPING` | Se están cerrando descriptor, hilo, sockets y feed | `OFF`, `ERROR` |
| `ERROR` | El último inicio/flujo terminó con un error accionable | `OFF`, `REQUESTING_PERMISSION`, `STARTING` |

La única etiqueta equivalente a protección activa es `ACTIVE_DNS_ONLY`. `DEGRADED` debe transportar una razón concreta, por ejemplo `DNS_PARSE_FAILURE`, `UPSTREAM_UNAVAILABLE`, `FEED_INVALID` o `NETWORK_UNAVAILABLE`. El servicio emite cada transición con una versión monotónica y una marca de tiempo; la UI observa ese flujo y valida de nuevo al volver a primer plano.

La preferencia `service_active` solo sirve como historial diagnóstico. Nunca habilita por sí sola una etiqueta activa después de recrear la app o el proceso.

## Cobertura honesta

La notificación, Inicio y Defensa mostrarán:

- `Protección DNS activa` únicamente en `ACTIVE_DNS_ONLY`.
- `Protección DNS degradada` con la pérdida concreta en `DEGRADED`.
- `Permiso pendiente`, `VPN revocada`, `Feed inválido`, `Red no disponible` o `Fallo interno` cuando corresponda.
- Cubierto: consultas DNS parseables que atraviesan la VPN y el reenvío upstream implementado.
- No cubierto: HTTPS/TLS, contenido de URL, tráfico no DNS, QUIC/DoH/DoT de otras apps, TCP/UDP arbitrario, y cualquier ruta que no atraviese el descriptor VPN.

No habrá selector ni texto de “túnel completo” en el flujo de esta fase.

## Errores y recuperación

- **Permiso pendiente**: permanecer en `REQUESTING_PERMISSION`; ofrecer acción para volver a solicitarlo.
- **VPN revocada o competida**: pasar a `ERROR` u `DEGRADED` con acción de reactivar; limpiar recursos.
- **Establecimiento fallido**: cerrar descriptor parcial, detener hilos y pasar a `ERROR`.
- **Feed inválido/ausente**: conservar reglas locales válidas, marcar `DEGRADED` con `FEED_INVALID`; las consultas desconocidas no se bloquean.
- **Upstream sin respuesta**: marcar `DEGRADED` con `UPSTREAM_UNAVAILABLE`, registrar solo el tipo de error y no el dominio; aplicar backoff limitado.
- **Cambio de red**: el sistema vuelve a encaminar sockets protegidos; se reintenta con backoff acotado, sin bucle agresivo.
- **Batería restringida/OEM**: mostrar advertencia accionable si se detecta incapacidad de mantener el servicio; no prometer continuidad.
- **Reinicio**: `BootReceiver` no inicia silenciosamente la VPN. Se restaura solo si Android permite el servicio y la autorización del usuario sigue vigente; en otro caso queda `OFF`/`REQUESTING_PERMISSION`.

Backoff propuesto: 1 s, 2 s, 4 s, 8 s, máximo 30 s, con un máximo de intentos por ventana y reinicio del contador al recuperar actividad. La tarea periódica de feed no debe mantener el VPN despierto innecesariamente.

## Privacidad, retención y diagnóstico

- Eventos de bloqueo: dominio normalizado mínimo, motivo, categoría, fuente, versión/fecha de feed y hora.
- Retención por defecto: sesión actual y un máximo acotado de eventos; el usuario puede elegir una duración menor y borrar toda la actividad.
- Los eventos conservados se protegerán con almacenamiento local privado/cifrado disponible para la app; no se exportan automáticamente.
- Logs de diagnóstico: códigos de estado, contadores y causas generales. Nunca dominio, URL, payload DNS, IP, identificador personal o contenido de notificación.
- El borrado de actividad elimina eventos y contadores locales sin modificar allowlist/blocklist salvo acción explícita del usuario.
- No se añade comunicación externa. Los upstream/feed existentes permanecen sujetos a HTTPS, límites y caché local.

## Batería y ciclo de vida

- Un único hilo de lectura por servicio, cerrado junto con el descriptor.
- Sockets upstream con timeout corto y `protect(socket)` antes de enviar.
- Sin polling rápido desde la UI para decidir protección; la UI observa el estado emitido y solo revalida en lifecycle.
- `stopVpn`, `onDestroy`, `onTaskRemoved` y `onRevoke` son idempotentes y limpian descriptor, hilo, sockets, feed y estado observable.
- Foreground service con notificación clara de DNS local, tipo `specialUse` conforme a la configuración existente y sin afirmar VPN completa.

## Plan de pruebas

### Unitarias

- Todas las transiciones válidas e inválidas de la máquina de estados.
- Condición de protección activa solo en `ACTIVE_DNS_ONLY`.
- `ALLOW`, `BLOCK`, `UNKNOWN` y `ERROR`.
- Precedencia allowlist/blocklist/feed y excepciones temporales caducadas.
- Feed ausente, caducado, inválido o vacío.
- Regla malformada, dominio inválido y fallo de resolución/upstream.
- Redacción de logs y ausencia de dominio, URL, IP o payload.
- Retención mínima, contador y borrado de actividad.

### Instrumentadas

- Consentimiento de VPN, inicio, parada, revocación y recreación del servicio.
- Cambio de red y socket protegido.
- Descriptor activo con consulta DNS de prueba y respuesta bloqueada/permitida.
- Reinicio de proceso: la UI no usa una preferencia como estado activo.

### UI

- `Protección DNS activa` nunca aparece fuera de `ACTIVE_DNS_ONLY`.
- `DEGRADED` muestra la razón concreta.
- Estado vacío y lista de actividad sin datos sensibles.
- Acción de permitir temporalmente exige confirmación y tiene caducidad.
- Reducir movimiento y content descriptions accesibles.

La matriz manual por versión Android, fabricante, otra VPN, red IPv4/IPv6, DNS cifrado y restricciones de batería vive en `DNS_PROTECTION_TEST_MATRIX.md`.