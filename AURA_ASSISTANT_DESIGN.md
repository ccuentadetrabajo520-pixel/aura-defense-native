# Diseño del asistente AURA

## Propósito

AURA es un asistente local de ciberdefensa para Android sin root. Conversa sobre seguridad, pero solo evalúa el dispositivo cuando recibe evidencia estructurada y verificable desde APIs locales de AURA.

## Arquitectura

`AssistantConversationService` recibe texto y un nivel de explicación. Para conversación general usa `LocalAssistantModelProvider` y no crea contexto del teléfono. Para diagnóstico solicita explícitamente un `AssistantContextBuilder`, que compone únicamente estado DNS/VPN, snapshot del feed firmado, eventos DNS, hallazgos, postura, permisos propios y límites Android.

`AssistantEvidenceProvider` lee esas fuentes locales. `AssistantToolRegistry` expone herramientas declaradas como solo lectura o acciones confirmables. `AssistantActionPolicy` valida intención, parámetros, confirmación y frecuencia antes de delegar en una función real. `AssistantHistoryRepository` conserva historial local redactado y borrable.

Los modelos `AssistantResponse`, `AssistantCitation`, `AssistantProposedAction` y `AssistantActionResult` transportan clasificación, evidencia, confianza, limitaciones y resultado sin permitir que el modelo llame Android directamente.

## Clasificación

- `GENERAL_RESPONSE`: saludo, explicación o conversación; no usa datos del dispositivo.
- `DEVICE_ASSESSMENT`: diagnóstico con fecha, citas, confianza, fuente/regla/versión cuando aplica y límites.
- `EVIDENCE_BASED_RECOMMENDATION`: hecho observado, incertidumbre, recomendación, acción disponible y cobertura.

Si falta evidencia para confirmar un riesgo, la respuesta es exactamente: `No tengo evidencia suficiente para confirmar ese riesgo`.

## Flujo de herramientas

Los datos externos (dominios, nombres de apps, feeds y contenido web) se etiquetan como no confiables. Se normalizan y validan como datos, nunca como instrucciones. El proveedor local propone texto; el registro de herramientas y la política ejecutan o rechazan. Ninguna acción sensible se ejecuta sin confirmación explícita.

## Cobertura

El mapa muestra `Cobertura actual`, no una puntuación opaca ni una garantía. Cada capa declara qué observa, qué protege, qué no puede proteger, última comprobación y límites Android sin root.

## UI

La pantalla principal es un centro de mando sereno: encabezado de cobertura, conversación diferenciada por clasificación, tarjetas de evidencia, propuestas separadas y línea de tiempo local. El avatar solo refleja estados reales. Animaciones breves respetan `prefers-reduced-motion`/la preferencia equivalente de Android; contraste, TalkBack, tamaño de fuente e internacionalización son requisitos de cada control.

## Offline y nube futura

El modo local funciona sin red con la base de conocimiento y evidencia previamente disponible. `CloudAssistantModelProvider` es una interfaz futura, desactivada por defecto: requerirá consentimiento granular, mostrará los campos a enviar, redactará y minimizará el contexto, nunca enviará dominios completos, nombres de apps, conversaciones, IDs o hallazgos sensibles por defecto, y permitirá borrar historial y volver al modo local.

## Limitaciones actuales

Android sin root no permite inspeccionar memoria privada de otras apps, descifrar TLS ajeno, garantizar ausencia de malware, cubrir DoH/DoT/QUIC/TCP/IPv6 no observados ni controlar ajustes ocultos. La protección DNS mantiene su implementación y sus feeds firmados existentes.
