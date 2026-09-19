# Auditoría de cierre de Fase 4

Fecha: 2026-09-19

## Resultado

Fase 4 queda implementada en la rama de trabajo con procesamiento local basado en reglas, evidencia estructurada, acciones confirmables y cobertura declarada. No se añadió telemetría, analítica, publicidad, permisos invasivos ni red nueva. El runtime on-device futuro está desactivado y AURA no se presenta como IA avanzada. El build verde asociado al hash final debe confirmarse en Codemagic después del push; la compilación local no se usa como criterio porque el entorno falla antes de configurar Gradle con `25.0.4.1`.

## Siete tareas de Fase 4

| Requisito | Implementación | Evidencia de uso real | Prueba asociada | Estado |
|---|---|---|---|---|
| 1. Documentación y diseño | Diseño, privacidad, matriz y política en `AURA_ASSISTANT_DESIGN.md`, `AURA_ASSISTANT_PRIVACY.md`, `AURA_ASSISTANT_TEST_MATRIX.md` y `AURA_ASSISTANT_TOOL_POLICY.md`. | Los documentos definen separación conversación/diagnóstico, datos permitidos, límites Android, nube futura, retención y acciones. | Revisión estática y `git diff --check`. | COMPLETA |
| 2. Arquitectura conversacional y evidencia | `AssistantConversationService`, `AssistantModelProvider`, `RuleBasedLocalAssistantProvider`, `OnDeviceAssistantModelProvider` inerte, `CloudAssistantModelProvider`, `AssistantEvidenceProvider`, `AssistantContextBuilder`, modelos tipados e historial local. Las rutas legacy sin consumidores fueron eliminadas. | El saludo y las preguntas conceptuales pasan por reglas locales sin construir contexto del teléfono; el diagnóstico explícito lee estado DNS, repositorio de feed vigente, eventos DNS, postura y permisos propios. | `AssistantPolicyTest`: saludo, evidencia insuficiente, inyección y tipos de respuesta. | COMPLETA |
| 3. Herramientas y acciones confirmables | `AssistantToolRegistry`, `AssistantActionPolicy`, `AssistantActionExecutor` y callbacks reales de UI. El registro rechaza IDs desconocidos y ya no anuncia share/settings/feed sin adaptador. | DNS usa callbacks de `MainActivity`; excepciones, borrado de actividad/historial y generación de informe llaman stores/archivos reales; cada propuesta valida parámetros y confirmación. | `AssistantPolicyTest`: herramienta no registrada, confirmación, rate limit, validación de dominio y timeline de acciones. | COMPLETA |
| 4. Mapa, timeline e informes | `AssistantCenterScreen`, `AssistantTimelineStore`, `AssistantLocalReportBuilder`. | El mapa muestra estado, fuente, versión, comprobación, expiración, postura y límites. La timeline se alimenta de bloqueos DNS, actualizaciones de feed y acciones persistidas; sin eventos muestra estado vacío. El informe se escribe en almacenamiento privado y no se comparte por defecto. | Prueba de timeline vacía/registro/borrado; revisión de que los eventos no incluyen dominios completos. | COMPLETA |
| 5. Privacidad y notificaciones | `AssistantNotificationService` + `AssistantNotificationPolicy`; listener de terceros no está declarado ni procesa eventos. Notificaciones propias solo para DNS detenido, degradación, feed fallido/caducado y coincidencia DNS respaldada por regla vigente. | El texto de notificación omite dominios, contenido y IDs; historial/timeline usan `SecurePrefs.encryptedOrNull`, migración única y borrado verificable. | `AssistantPolicyTest` cubre estados notificables, migración y borrado; revisión de manifest y listener stub. | COMPLETA |
| 6. Protección contra prompt injection y claims | Datos externos se tratan como valores; `AssistantInputSafety` precede al diagnóstico, y la política valida herramienta y parámetros. Se eliminaron rutas legacy y claims de “primera”, “100%”, “invulnerable” y “protección total”. | Un dominio o texto malicioso no crea una propuesta ni abre evidencia; las acciones solo nacen de intenciones internas del servicio y requieren confirmación. | `AssistantPolicyTest` y búsqueda estática de claims/acciones legacy. | COMPLETA |
| 7. UI, accesibilidad y verificación | Centro de asistente sobrio con tarjetas separadas, niveles persistentes, confirmaciones, descripciones semánticas y sin animaciones propias que simulen análisis. | `Cobertura actual` refleja estados reales; las citas muestran evidencia, fecha, confianza y limitación; acciones DNS muestran `PENDING_REQUEST` o estado terminal real. | Análisis estático de UI; build de Codemagic del hash final pendiente de confirmación; pruebas instrumentadas siguen dependiendo de dispositivo. | IMPLEMENTADA, CIERRE PENDIENTE DE BUILD |

## Respuestas y garantías de producto

- La respuesta exacta ante evidencia insuficiente es: `No tengo evidencia suficiente para confirmar ese riesgo`.
- Una respuesta general nunca se etiqueta como evaluación del teléfono.
- `CONFIRMED_MATCH` no se genera desde el asistente por heurística; una coincidencia DNS solo notifica cuando existe regla identificada, fuente, versión y vigencia en el snapshot firmado.
- No se añadieron llamadas de red, SDKs de analítica, publicidad, trackers ni permisos invasivos.
- No se habilitó lectura de notificaciones de terceros.
- No se instalaron/desinstalaron aplicaciones ni se modificaron ajustes ocultos.

## Limitaciones restantes

- Las pruebas instrumentadas requieren emulador o dispositivo Android.
- La compilación local queda bloqueada antes del compilador por el error ambiental `25.0.4.1`; el build de Codemagic del hash final aún debe confirmarse.
- Android sin root no permite inspeccionar memoria privada, descifrar TLS de otras apps, garantizar ausencia de malware ni cubrir tráfico que no atraviesa el VPN DNS local.
- La nube es una interfaz futura desactivada y no realiza llamadas.

## Auditoría de permisos

| Permiso | Función concreta | Pantalla consumidora | Solicitud | Alternativa sin permiso | Estado |
|---|---|---|---|---|---|
| `CAMERA` | Escanear QR local con CameraX | `QrScannerDialog` desde Herramientas | Solo al pulsar Abrir cámara; nunca al abrir AURA/asistente | Analizar texto o enlace compartido | CONSERVADO, compatible con Play como función iniciada por usuario |
| `RECORD_AUDIO` | Dictado opcional del asistente | `VoiceCommandScreen` | Solo al pulsar el control de voz | Escribir la consulta | CONSERVADO, bajo demanda |
| `ACCESS_FINE_LOCATION` | Descubrimiento LAN/Bluetooth cuando Android lo exige | `AurasScreen` | Solo al pulsar activar ubicación/escaneo LAN | Omitir descubrimiento LAN | CONSERVADO, bajo demanda |
| `ACCESS_COARSE_LOCATION` | Alternativa de precisión para descubrimiento LAN | `AurasScreen` | Se solicita junto con fine cuando el usuario activa la función | Omitir descubrimiento LAN | CONSERVADO, bajo demanda |
| `BLUETOOTH_CONNECT` | Leer dispositivos Bluetooth vinculados en Herramientas | `AuraAdvancedToolsDialog` | Se comprueba antes de escanear; no se solicita al abrir asistente | Mostrar Bluetooth no disponible y abrir ajustes manualmente | CONSERVADO, bajo demanda |

No se eliminaron estos permisos porque cada uno tiene un consumidor funcional identificable. No se solicitan al iniciar AURA ni al abrir el asistente. El listener de notificaciones de terceros no está declarado en el manifest y no procesa notificaciones.
