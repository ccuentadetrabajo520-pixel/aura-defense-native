# Límites reales del análisis local de AURA DEFENSE

## Visión general

El análisis local no sustituye a un antivirus, a un análisis forense ni a la auditoría de un fabricante. Se basa en las señales que Android permite observar a una app normal sin root.

## Señales permitidas sin root

- Estado de bloqueo de pantalla.
- Estado de la VPN local y del DNS privado.
- ADB habilitado.
- Servicios de accesibilidad activos.
- Metadata de paquetes visibles: nombre, versión, instalador, SDK e icono.
- Permisos declarados y concedidos por apps visibles.
- Configuraciones de depuración, backup y actualizaciones del sistema.
- Eventos DNS que cruzan la propia VpnService.

## Señales prohibidas o no confiables sin root

- Lectura de memoria privada de apps ajenas.
- Inspección del almacenamiento interno o el contenido cifrado de apps ajenas.
- Comprobación del malware oculto en memoria.
- Afirmación de que una app es stalkerware solo porque su nombre o icono parezcan sospechosos.
- Afirmar que una app es malware sin una regla firmada y vigente.

## Impacto de la versión y del fabricante

- Android 8.0+ (API 26+) proporciona la base mínima del análisis.
- Android 10+ y 11+ reducen la visibilidad del paquete y la información expuesta.
- Algunos OEM ocultan señales, deshabilitan API clave o limitan la visibilidad del sistema.
- La combinación de biblioteca del fabricante, permisos del usuario y la política de privacidad del OEM puede reducir la cobertura de análisis.

## Privacidad y retención

- Los datos se mantienen locales por defecto.
- El usuario puede borrar historial, silenciar alertas y eliminar excepciones.
- Si un dato no está disponible, la interfaz lo debe reflejar como cobertura parcial o no disponible.
- Las decisiones deben basarse en evidencia local y no en suposiciones.

## Severidad y confianza

El análisis separa severidad del riesgo impactante y confianza de la probabilidad de que la señal sea real.

- Se puede tener severidad alta y confianza baja si la evidencia es débil.
- Un nombre sospechoso no crea malware confirmado.
- La coincidencia con un feed firmado puede elevar la confianza a `CONFIRMED_MATCH`.

## Reglas de la fase 3

- `CONFIRMED_MATCH` solo existe con coincidencia verificable y cadena de evidencia.
- `SUSPICIOUS_SIGNAL` es el máximo para heurísticas sin feed firmado.
- La interfaz debe indicar claramente si la información es parcial.
- Ninguna acción automática de desinstalación, bloqueo o modificación.

## Permisos finales del manifiesto

La app conserva solo los permisos necesarios para la funcionalidad real del análisis local y VPN del usuario:

- `INTERNET` para accesos de red de la VPN y telemetry local.
- `ACCESS_NETWORK_STATE` y `ACCESS_WIFI_STATE` para estado de conectividad y red local.
- `FOREGROUND_SERVICE` para mantener la VPN/servicios de primer plano.
- `POST_NOTIFICATIONS` para notificaciones de usuario relevantes del propio sistema.
- `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` solo para análisis local explícito y no para vigilancia encubierta.
- `BLUETOOTH_CONNECT` solo si se usa para descubrir dispositivos del entorno con consentimiento del usuario.

No se conservan ni se justifican permisos como:

- `QUERY_ALL_PACKAGES`
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`
- `READ_CONTACTS`, `READ_CALL_LOG`, `READ_SMS`, `RECEIVE_SMS`, `SYSTEM_ALERT_WINDOW`, `REQUEST_INSTALL_PACKAGES`
- `NotificationListenerService` o `BIND_ACCESSIBILITY_SERVICE`

## Verificación del feed y firma Ed25519

La validación de feeds no usa HMAC ni claves compartidas.

- El payload canónico se firma con Ed25519.
- La verificación se hace con una clave pública distribuida externamente, no con una clave privada incrustada en la app.
- El feed debe incluir regla, fuente, versión, evidencia, expiración y tamaño.
- Se rechaza feed expirado, alterado, replay, downgrade o con certificado no vigente.
- El valor de `CONFIRMED_MATCH` solo aparece si la validación oficial del feed es correcta y la regla sigue vigente.
