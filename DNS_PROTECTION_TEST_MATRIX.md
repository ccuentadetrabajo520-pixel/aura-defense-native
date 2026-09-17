# Matriz manual de pruebas: protección DNS

Esta matriz debe ejecutarse en un dispositivo físico o emulador con Android 8.0+ y repetirse tras cambios del sistema, fabricante, upstream o feed. La etiqueta **Protección DNS activa** solo es correcta en `ACTIVE_DNS_ONLY`.

| ID | Entorno/acción | Resultado esperado | Evidencia |
|---|---|---|---|
| DNS-01 | Android 8/9, primera activación, permiso VPN aceptado | `REQUESTING_PERMISSION` -> `STARTING` -> `ACTIVE_DNS_ONLY`; notificación dice DNS local | Estado emitido, notificación, log redactado |
| DNS-02 | Permiso VPN rechazado | `REQUESTING_PERMISSION` -> `OFF`; no aparece protección activa | UI y estado |
| DNS-03 | Android 10/11, detener desde AURA | `STOPPING` -> `OFF`; descriptor, hilo y foreground terminan | `dumpsys vpn`, UI |
| DNS-04 | Android 12/13, revocar VPN desde Ajustes | `ERROR` con VPN revocada; no se conserva etiqueta activa | UI, log, `dumpsys` |
| DNS-05 | Android 14, target 34 | Foreground service válido, notificación persistente y tipo especial declarado | Logcat, notificación |
| DNS-06 | DNS UDP/53 IPv4 permitido | La consulta atraviesa el túnel y recibe respuesta upstream | Captura local controlada, contador |
| DNS-07 | Dominio en regla local/feed válido | `BLOCK`, respuesta negativa y evento con motivo, categoría, fuente, versión y hora | Actividad DNS |
| DNS-08 | Dominio desconocido | `UNKNOWN` operativo: se reenvía, no se bloquea por defecto | Actividad/estado |
| DNS-09 | Regla malformada o paquete no interpretable | `ERROR`/`DEGRADED` según causa; no bloqueo por defecto | Estado y log sin dominio |
| DNS-10 | Feed remoto apagado, caché válida | Se usa caché/base incluida y se comunica versión/antigüedad | UI, estado |
| DNS-11 | Feed vacío, inválido o caducado | `DEGRADED` con `FEED_INVALID`; reglas desconocidas no bloquean | UI |
| DNS-12 | Upstream `1.1.1.1` sin respuesta | `DEGRADED` con upstream no disponible; backoff limitado; no bucle agresivo | Logcat, batería |
| DNS-13 | Cambiar entre perfiles | La siguiente consulta usa el perfil nuevo sin reiniciar servicio | Decisión y UI |
| DNS-14 | Permitir temporalmente un bloqueo | Confirmación de usuario, `ALLOW` durante 15 minutos, luego vuelve a decidir; excepción reversible | Actividad y preferencias seguras |
| DNS-15 | Borrar actividad | Eventos y contador desaparecen; allowlist/blocklist no cambian | UI y almacenamiento |
| DNS-16 | Reiniciar proceso/app con preferencia antigua activa | La UI valida el servicio real; no muestra activa por una preferencia | UI, estado |
| DNS-17 | Otra VPN activa | AURA no afirma protección si su servicio/descriptor no está activo | Estado y UI del sistema |
| DNS-18 | IPv6 DNS, DoH/DoT, QUIC, TCP/53 y tráfico no DNS | Se comunica como no cubierto; no se presenta túnel completo | UI, documentación |
| DNS-19 | Restricción de batería del fabricante | Se informa riesgo accionable; no se garantiza continuidad silenciosa | Ajustes OEM y estado |
| DNS-20 | `FLAG_SECURE`, exportación de diagnóstico | Logs no contienen dominio, URL, IP, payload ni identificadores personales | Archivo compartido y test |
| DNS-21 | Reducir movimiento y lector de pantalla | La UI sigue operable, content descriptions presentes y no depende de animación | Accesibilidad Android |
| DNS-22 | Google Play pre-release | Revisar `QUERY_ALL_PACKAGES`, notification listener, foreground special use y Data safety | Checklist de publicación |

## Dispositivos mínimos

- Emulador Pixel: API 26, 28, 30, 33 y 34.
- Al menos un Samsung y un Xiaomi/Oppo con ahorro de batería activo.
- Wi-Fi IPv4, red IPv6 y cambio Wi-Fi/datos móviles.
- Prueba con y sin otra VPN instalada, sin root y con bootloader normal.

## Evidencia requerida para salida

Guardar solo capturas, estados y contadores no sensibles. No adjuntar dominios reales, URLs, IPs residenciales, payloads DNS ni notificaciones personales. Una prueba se considera fallida si la UI muestra protección activa fuera de `ACTIVE_DNS_ONLY`, si bloquea ante `UNKNOWN/ERROR`, o si describe la capacidad como túnel completo.