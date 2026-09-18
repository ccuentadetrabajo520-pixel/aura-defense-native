# Matriz de pruebas: análisis local de seguridad del dispositivo

## Objetivo

Validar que los análisis locales de AURA respetan los límites de Android, no fabrican malware, presentan evidencia y ofrecen una UX honesta.

## Casos de prueba

### 1. Evitar afirmaciones incorrectas

- No se debe mostrar “malware confirmado” sin `CONFIRMED_MATCH`.
- Un nombre de aplicación sospechoso debe producir `SUSPICIOUS_SIGNAL` y no una severidad crítica.
- Permisos comunes del sistema no deben generar falsos positivos de malware.

### 2. Cobertura parcial

- Si el sistema no expone una señal, la UI debe mostrar “Cobertura parcial” o equivalente.
- Cuando una API no está disponible, el estado debe reflejar que la señal no se puede verificar.
- La UI debe distinguir entre “no disponible” y “no se puede confirmar”.

### 3. Modelo de hallazgos

- Cada hallazgo incluye id estable, categoría, severidad, confianza, evidencia, límites, falso positivo, recomendación y acción reversible.
- `CONFIRMED_MATCH` requiere fuente, regla y validez verificable.
- `SUSPICIOUS_SIGNAL` no puede usarse para afirmar malware.

### 4. Privacidad y almacenamiento local

- El análisis no envía datos fuera del dispositivo por defecto.
- La exportación de informes debe ser explícita y gestionada por el usuario.
- El historial puede borrarse, los silencios pueden desactivarse y las excepciones pueden eliminarse.

### 5. Puntuación explicable

- La puntuación global debe desglosarse por componentes: dispositivo, red, aplicaciones y su evidencia.
- El valor no es una nota opaca sin explicación.

### 6. UI y asistentes

- La pantalla de aplicaciones debe avisar del alcance parcial de Android.
- La red solo muestra eventos DNS reales.
- El asistente responde: “No tengo evidencia suficiente para confirmar ese riesgo” cuando falta evidencia.

## Datos de prueba sugeridos

| Caso | Entrada | Resultado esperado |
|---|---|---|
| nombre sospechoso | paquete `spy` y sin feed | `SUSPICIOUS_SIGNAL`, no `CRITICAL` |
| permisos comunes | `CAMERA`, `LOCATION`, `RECORD_AUDIO` sin match | `LOW_RISK`/`MEDIUM_RISK`, nunca malware |
| coincidencia firmada | regla con id y feed vigente | `CONFIRMED_MATCH` |
| API no disponible | `Build.VERSION.SDK_INT` o una API no soportada | estado “No disponible” + cobertura parcial |
| historial | borrar historial | elimina eventos previos en almacenamiento local |
| excepciones | eliminar excepción | la app deja de estar silenciada |

## Criterio de aprobación

La Fase 3 queda validada solo si todas las pruebas relevantes pasan y ningún test permite una afirmación de malware sin evidencia firmada.
