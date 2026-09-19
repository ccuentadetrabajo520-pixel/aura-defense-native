# Operaciones de inteligencia de amenazas con firma Ed25519

## Principio de diseño

AURA DEFENSE no debe confirmar malware solo con heurísticas de nombre, icono o permisos. La confirmación requiere una regla de inteligencia con firma asimétrica Ed25519 y una clave pública válida del remitente.

## Requisitos de seguridad

- No se usa HMAC ni claves compartidas.
- No se distribuye ninguna clave privada dentro del APK, repo ni logs.
- La app solo valida la firma con una clave pública autorizada.
- El feed debe ser íntegro, vigente, sin replay ni downgrade.
- `CONFIRMED_MATCH` solo se emite si la regla, la fuente y la clave validan el payload.

## Flujo recomendado de edición y distribución

1. El operador externo genera un par Ed25519 y nunca almacena la clave privada en el repositorio.
2. Publica la clave pública en un canal autenticado del proyecto y la versiona con `publicKeyId`.
3. Genera el payload canónico con los campos:
   - `ruleId`
   - `source`
   - `version`
   - `evidence`
   - `expiresAt`
   - `size`
   - `indicators` (lista de dominios y metadatos con `indicatorType`, `category`, `severity`, `updatedAt`)
4. Calcula el hash SHA-256 del payload canónico.
5. Firma el payload canónico con la clave privada Ed25519 usando la herramienta externa `tools/sign_threat_feed.py`.
6. Publica el feed firmado a través de una URL HTTPS autorizada y comprobada.
7. La app valida la firma, la expiración, el tamaño, el checksum, el `publicKeyId`, el replay y la regla esperada antes de activar el feed.
8. El feed solo se activa si la validación completa pasa; en caso contrario, el repositorio mantiene el último feed válido y la protección DNS permanece degradada.

## Formato canónico e interoperabilidad

El payload firmado es UTF-8, sin BOM, sin espacios y con separadores JSON `,` y `:`. Sus claves aparecen en este orden: `ruleId`, `source`, `version`, `evidence`, `expiresAt` e `indicators`. Los indicadores se ordenan por `id` y cada indicador usa, en este orden, `id`, `indicator`, `indicatorType`, `category`, `severity`, `descriptionEs`, `source` y `updatedAt`. Las cadenas usan escapes JSON estándar y no ASCII-escapean Unicode. `size` es el número de bytes UTF-8 del payload y `checksum` es su SHA-256 hexadecimal; la firma Ed25519 se codifica en Base64.

El vector versionado en `test-fixtures/crypto/` contiene el feed, la clave pública y la firma esperada. Android lo valida en `ThreatIntelligenceRepositoryTest`; la comprobación independiente se ejecuta con `python3 tools/test_sign_threat_feed.py`. La clave privada utilizada para generar el vector no se versiona.

## Desarrollo y producción

La variante `dev` es explícitamente no productiva. No tiene una URL de inteligencia activa por defecto: las pruebas locales inyectan el JSON firmado mediante `updateFromSignedManifest`, y una URL HTTPS de pruebas controlada solo puede habilitarse con `AURA_THREAT_MANIFEST_URL_DEBUG`. Su clave pública de pruebas se proporciona con `AURA_THREAT_PUBLIC_KEY_DEBUG` cuando se necesita una descarga local. No se debe usar ningún dominio ficticio como fuente activa.

`prodRelease` permanece bloqueado durante la generación de BuildConfig si faltan `AURA_THREAT_PUBLIC_KEY` o `AURA_THREAT_MANIFEST_URL`, y también rechaza una URL que no use HTTPS.

## Publicación y rotación externa

- La clave privada debe vivir fuera de GitHub, del APK y de cualquier log o artefacto compilado.
- El comando de firma debe leer la clave desde `AURA_THREAT_SIGNING_KEY` del entorno y nunca imprimirla.
- La rotación requiere una nueva clave pública, una transición de validación y una confirmación de reemplazo del `publicKeyId`.
- El revocado debe descartarse inmediatamente y el sistema debe mantener el último feed válido si una clave nueva no es aceptada.
- Un rollback solo se acepta si la clave nueva no valida el feed o si el feed pretendidamente nuevo es un downgrade o replay.

## Criterios de rechazo

Se rechaza cualquier feed que:

- tenga firma nula o inválida;
- use una clave pública no reconocida;
- haya sido alterado o reordenado;
- esté vencido;
- detecte una regla no vigente o incompatible;
- repita la misma firma dentro del tiempo de ventana de replay;
- tenga un tamaño o checksum que no coincida con su payload canónico.

## Política de despliegue

- La clave privada vive fuera del repositorio y fuera del APK.
- La clave pública se mantiene en un almacenamiento seguro del operador y dentro de los mecanismos de verificación de la app.
- Los cambios de clave requieren una rotación documentada y una etapa de transición.
- La aplicación debe mostrar evidencia explicable y no asumir peligro sin validación real.
- Los feeds no firmados nunca pueden activar DNS blocking ni redefinir la protección activa del dispositivo.
- En producción, cualquier URL o clave pública vacía debe dejar la app en estado `FAILED` y no habilitar la protección segura.
