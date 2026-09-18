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
