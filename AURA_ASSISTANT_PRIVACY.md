# Privacidad del asistente AURA

## Por defecto

El procesamiento conversacional basado en reglas y el diagnóstico ocurren en el dispositivo. No se añade un LLM ni se afirma IA avanzada: el runtime on-device futuro está desactivado. No se añaden analítica, publicidad, trackers, SDKs de telemetría ni llamadas de red por esta fase. El proveedor de respuesta no recibe acceso directo a Android, VPN, red, almacenamiento ni datos privados: recibe solo objetos estructurados que el proveedor de evidencia decide exponer.

## Evidencia

Puede usarse estado real DNS/VPN, cobertura actual, estado del feed firmado vigente, eventos DNS reales minimizados, hallazgos locales, postura disponible, permisos propios de AURA y límites Android. Los dominios, nombres de apps, feeds y contenido web son datos no confiables y no se tratan como instrucciones.

Las conversaciones generales no leen el teléfono. Una evaluación del dispositivo declara fecha/hora, citas, confianza, fuente/regla/versión del feed si existe y limitaciones. Sin evidencia suficiente no se confirma un riesgo.

## Almacenamiento, retención y borrado

El historial del asistente es opcional, local, redactado y limitado. No guarda secretos, tokens, credenciales, payloads privados ni conversaciones completas cuando no son necesarias para el registro. El usuario puede borrar conversaciones, acciones, memoria conversacional e historial local. La actividad DNS conserva la retención ya definida por AURA y puede borrarse desde Defensa; sus eventos de timeline se eliminan junto con esa actividad.

## Acciones

Iniciar o detener DNS, crear o retirar excepciones, borrar actividad, borrar memoria/historial, generar informes, compartir informes y abrir ajustes opcionales requieren confirmación. La confirmación muestra efecto, duración, alcance y reversibilidad; el resultado se registra con hora y error real. El asistente nunca instala/desinstala apps, modifica ajustes ocultos, bloquea automáticamente ni accede al contenido de otras apps.

## Nube futura

La nube estará desactivada por defecto y no tiene llamadas activas. Si se implementa, requerirá consentimiento granular, explicación de campos, minimización y redacción. No enviará dominios completos, nombres de apps, IDs, conversaciones ni hallazgos sensibles por defecto. El usuario podrá borrar el historial y volver al proveedor local.

## Android sin root

La cobertura depende de versión, fabricante, permisos y estado observable. No implica protección total, ausencia de malware ni inspección de tráfico cifrado. AURA no usa accesibilidad abusiva, lectura de SMS, captura de credenciales, keylogging, vigilancia oculta ni lectura de notificaciones de terceros por defecto.
