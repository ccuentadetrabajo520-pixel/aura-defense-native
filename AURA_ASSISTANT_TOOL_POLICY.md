# Política de herramientas del asistente AURA

## Separación de confianza

Las instrucciones internas, la evidencia estructurada y las llamadas de herramientas son canales distintos. Texto externo nunca se concatena como instrucciones. Cada entrada externa se etiqueta `UNTRUSTED_DATA`, se limita a su campo y se valida localmente.

Un dominio, nombre de app, feed, notificación o contenido web no puede activar una acción, cambiar la política, pedir permisos ni revelar datos. Se rechazan parámetros vacíos, fuera de rango, no normalizables o que incluyan control de flujo.

## Herramientas de solo lectura

- `get_coverage`: estado DNS/VPN, feed firmado, postura, permisos propios y límites.
- `get_feed_status`: fuente, versión, antigüedad, expiración y validación.
- `get_dns_events`: eventos DNS reales minimizados.
- `get_findings`: hallazgos locales disponibles.
- `get_posture`: postura disponible con fecha.
- `get_alert_evidence`: evidencia de una alerta local.

## Acciones confirmables

`start_dns`, `stop_dns`, `create_temporary_exception`, `remove_temporary_exception`, `clear_local_activity`, `clear_assistant_history` y `generate_local_report` requieren una tarjeta de confirmación emitida por la política. Compartir/exportar y abrir ajustes no están registrados en el asistente hasta tener un adaptador confirmable real. Cada ejecución devuelve éxito o error real, hora, alcance, duración, reversibilidad y, cuando es posible, una acción de deshacer.

## Prohibiciones

No hay herramientas para instalar/desinstalar apps, enviar datos a terceros, modificar ajustes ocultos, bloquear por decisión del modelo, cambiar controles críticos, solicitar permisos invasivos o acceder a contenido privado de otras apps. El proveedor de IA nunca invoca Android directamente.

## Inyección y frecuencia

Se aplica una ventana de frecuencia local a propuestas sensibles, se registran errores redactados y se descartan datos que intenten sobrescribir instrucciones. Una respuesta general nunca se etiqueta como evaluación del teléfono. Un malware solo se confirma con `CONFIRMED_MATCH` respaldado por feed firmado, vigente y activo.
