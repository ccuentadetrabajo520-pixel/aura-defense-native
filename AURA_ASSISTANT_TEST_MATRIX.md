# Matriz de pruebas del asistente AURA

| Caso | Resultado esperado |
|---|---|
| saludo o pregunta general | `GENERAL_RESPONSE`, sin invocar evidencia del dispositivo |
| pregunta técnica | explicación local, hechos separados de recomendaciones |
| evaluación con postura/feed/evento real | `DEVICE_ASSESSMENT`, fecha, citas, confianza y límites |
| riesgo sin evidencia | exactamente `No tengo evidencia suficiente para confirmar ese riesgo` |
| acción sensible sin confirmación | propuesta, no ejecución |
| solicitud DNS sin confirmación Android | `PENDING_REQUEST`, nunca éxito prematuro |
| permiso VPN denegado o servicio con error | estado terminal `OFF`/`ERROR`, mensaje real y hora |
| detención DNS confirmada | `COMPLETED` solo tras observar `OFF` |
| confirmación aprobada | delegación real, resultado real y registro local |
| dominio/feed/nombre malicioso como instrucción | tratado como dato, sin herramienta ni cambio de política |
| malware | solo `CONFIRMED_MATCH` firmado, vigente y activo |
| borrado | historial/memoria local eliminados y resultado visible |
| Keystore no disponible | no se persisten historial ni timeline sensibles; UI lo declara |
| migración de historial | copia a `SecurePrefs` cifrado y eliminación del JSON legacy |
| sin red | modo local funciona sin llamada externa |
| cobertura activa/parcial/detenida/feed fallido | estados observables, nunca “Protección total” |
| timeline | solo eventos reales con hora, origen, evidencia y resultado |
| informe | local, sin compartir automático, sin conversaciones ni IDs |
| accesibilidad/contraste/movimiento | etiquetas, contraste y reducción de movimiento respetados |
| notificaciones | solo propias de AURA y estados comprobados |

Las pruebas unitarias cubren clasificación, ausencia de acceso en conversación general, evidencia insuficiente, confirmación, inyección y borrado. Las pruebas instrumentadas cubren estado real DNS, mapa de cobertura, acciones y accesibilidad cuando haya dispositivo disponible.
