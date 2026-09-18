# Fase 3: análisis local explicable de riesgo del dispositivo y aplicaciones

## Objetivo

AURA DEFENSE debe ofrecer un análisis local del estado del dispositivo y de las aplicaciones visibles sin inventar malware, sin requerir root y sin afirmar conclusiones que no estén respaldadas por evidencia verificable.

La fase 3 se centra en tres principios:

1. La evidencia local es limitada por Android y por los permisos que el usuario concede.
2. Un hallazgo solo puede escalar a una conclusión de riesgo grave si existe coincidencia con una fuente firmada, vigente y verificable.
3. La seguridad global se presenta como un desglose explicable, no como un número opaco.

## Alcance

El análisis local incluye:

- Estado del dispositivo: versión Android, parche, bloqueo de pantalla, VPN/DNS, depuración USB y servicios de accesibilidad visibles.
- Aplicaciones visibles: instalador disponible, permisos declarados y concedidos, SDK objetivo, depuración, actualizaciones y coincidencias verificadas.
- Red: eventos DNS reales recogidos por la VPN local o por el monitor del sistema, sin inventar eventos.
- Riesgos de privacidad: permisos de ubicación, micrófono, cámara, SMS, contactos, accesibilidad y superposición.
- Resultados explicables mostrados en interfaz y en informes, cada hallazgo con soporte, límites y recomendación.

## Señales posibles sin root

Las señales que AURA puede observar sin root son limitadas a lo visible por Android:

- Estado de pantalla segura (PIN, patrón, biometría).
- Depuración USB activa (Settings.Global.ADB_ENABLED).
- Servicios de accesibilidad activos.
- Estado de la VPN local y de DNS privado.
- Red activa (Wi‑Fi, datos móviles, Ethernet) si la API lo permite.
- Parámetros de instalación de aplicaciones visibles.
- Permisos declarados y concedidos por las apps visibles.
- Dispersiones de uso del sistema y estado de seguridad del paquete.
- Observar la presencia/ausencia de un instalador, si Android lo expone.
- Detectar si un paquete tiene un icono, un nombre visible o un estado de depuración.

### Señales que no son concluyentes

No pueden decidir malware ni stalkerware por sí solas:

- Nombre de aplicación.
- Permisos sensibles en sí mismos.
- Carencia de icono o nombre visible.
- Heurísticas de similitud con nombres conocidos.
- Combinaciones de permisos sin coincidencia aprobada con una fuente firmada.
- Criterios basados únicamente en la apariencia del paquete.

## Permisos y cuándo se solicitan

El análisis local solo usa permisos que Android permite a una app normal sin root.

| Señal | Permiso o API | Cuándo se usa | Observación |
|---|---|---|---|
| Nombre y versión | PackageManager | Siempre que se liste el paquete | Solo metadatos visibles del usuario |
| Instalador | getInstallerPackageName | Cuando Android lo expone | Puede devolver nulo o no disponible |
| Permisos declarados | GET_PERMISSIONS | Cuando se analiza la app | Solo los permisos manifestados |
| Permisos concedidos | flags de PackageInfo | Cuando existe información del sistema | Requiere permisos ya visibles al instalador |
| SDK objetivo | ApplicationInfo.targetSdkVersion | En la lectura del paquete | Disponible sin root |
| Depuración | FLAG_DEBUGGABLE | Cuando la app es visible | No implica malware |
| Estado de copia de seguridad | FLAG_ALLOW_BACKUP | Cuando se consulta la app | No implica riesgo por sí solo |
| VPN/DNS | ConnectivityManager y Settings.Global | Cuando el usuario usa la protección local | Solo refleja el estado observable |
| Bloqueo de pantalla | KeyguardManager | Cuando el sistema lo permite | refleja seguridad del desbloqueo |
| Accesibilidad | Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES | Al consultar proveedores activos | No es prueba de maldad en solitario |
| Eventos DNS reales | VpnService / red local | Cuando la VPN está activa y hay tráfico observado | No se fabrican eventos |

## Qué no puede hacer Android sin root

- Leer el contenido privado de otra app, su almacenamiento interno o memoria.
- Inspeccionar genéricamente el tráfico de aplicaciones distintas a la propia si no atraviesa la VPN local.
- Detectar rootkits, módulos ocultos, hooks o carga dinámica en otras apps.
- Determinar con certeza si una app es malware sin evidencia adicional y externa de confianza.
- Bloquear o desinstalar paquetes de forma automática.
- Reforzar el sistema por encima de los límites de Android.

## Límites por versión Android y fabricante

- Android 8.0+ (API 26+) es el mínimo para la mayor parte de la funcionalidad visible.
- Android 10+ introduce restricciones más estrictas sobre acceso a paquetes, permisos y metadatos.
- Android 11+ limita severamente la visibilidad de paquetes, por lo que AURA debe describir el análisis como parcial cuando el sistema no expone la señal.
- Algunos fabricantes ocultan información, desactivan permisos o alteran la configuración de seguridad, por ejemplo con capas de personalización, privacidad reforzada o compatibilidad de OEM.
- El estado de DNS privado y la VPN dependen de la versión y del fabricante, así como del soporte del sistema.
- El análisis local debe mostrarse siempre como cobertura parcial cuando una señal no está disponible.

## Riesgos de privacidad

- La aplicación solo comparte contenido con el home del usuario si se exporta un informe local o se activa una acción explícita.
- El análisis local nunca debe enviar los paquetes, permisos o nombres de apps fuera del dispositivo por defecto.
- AURA debe conservar solo datos mínimos esenciales para la explicación del riesgo y para la línea base local.
- Los informes exportables deben ser explícitos y localmente gestionados por el usuario.

## Determinación de severidad y confianza

Cada hallazgo tiene un nivel de severidad y de confianza por separado.

- Severidad refleja el impacto potencial si la condición existe.
- Confianza refleja cuánto apoyo tiene la evidencia observada.

### Escala de severidad establecida

- INFORMATIVE: señal útil pero no accionable ni sospechosa.
- LOW_RISK: riesgo de configuración o privilegio mínimo, sin evidencia de amenaza concreta.
- MEDIUM_RISK: señal de riesgo razonable, pero aún no concluyente.
- HIGH_RISK: riesgo serio, con impacto real si se confirma.
- SUSPICIOUS_SIGNAL: la señal sugiere actividad sospechosa o patrón, pero no constituye malware confirmado.
- CONFIRMED_MATCH: solo cuando existe una coincidencia verificable con un feed firmado, vigente y con identificador de regla/fuente.

### Reglas estrictas

- Un nombre de aplicación, permisos sensibles, falta de icono o heurísticas no pueden producir HIGH_RISK ni CRITICAL ni “malware confirmado”.
- SUSPICIOUS_SIGNAL es el máximo para una inferencia basada solo en metadata local.
- CONFIRMED_MATCH exige una regla con identificador, fuente y validez actual.
- Si no hay evidencia suficiente, el asistente debe responder: “No tengo evidencia suficiente para confirmar ese riesgo”.

## Modelo de hallazgo obligatorio

Cada hallazgo debe incluir:

- id estable
- categoría
- severidad
- confianza
- evidencia
- límites
- posible falso positivo
- recomendación
- acción reversible

La acción reversible refiere a medidas que el usuario puede deshacer sin afectar al aparato de forma irreversible, por ejemplo silenciar la recomendación, borrar eventos o descartar un hallazgo específico.

## Recomendaciones operativas

- No desinstalar, bloquear ni modificar apps automáticamente.
- Mostrar siempre una nota de cobertura parcial cuando la señal se limita por Android.
- Mantener los datos locales por defecto.
- Permitir al usuario silenciar temporalmente hallazgos, borrar historial y eliminar excepciones.
- Mostrar el desglose total de la puntuación global: componentes y hallazgos, no una cifra aislada.

## Integración en la interfaz

La UX debe incluir:

- Estado del dispositivo con Android, parche, bloqueo de pantalla, protección DNS/VPN real y señales disponibles.
- Riesgos con evidencia, confianza, limitaciones y acción recomendada.
- Aplicaciones indicando cobertura parcial cuando Android no pueda exponer una señal.
- Red con únicamente eventos DNS reales.
- Mensajes del asistente basados solo en hallazgos locales y con la frase exacta: “No tengo evidencia suficiente para confirmar ese riesgo”.

## Criterio formal de rechazo

La fase 3 no puede considerarse finalizada si:

- hay `QUERY_ALL_PACKAGES` sin justificación compatible con Google Play,
- se afirma malware sin `CONFIRMED_MATCH`,
- un nombre o permisos cuentan como “malware confirmado”,
- la puntuación global no está explicada o no se muestra el desglose,
- o la interfaz comunica certeza que el sistema no puede validar.
