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

1. El operador externo genera un par Ed25519.
2. Publica la clave pública en un canal autenticado del proyecto.
3. Genera el payload canónico con los campos:
   - `ruleId`
   - `source`
   - `version`
   - `evidence`
   - `expiresAt`
   - `size`
4. Calcula el hash SHA-256 del payload canónico.
5. Firma el payload canónico con la clave privada Ed25519.
6. Distribuye el feed como JSON o payload firmado con la firma y la huella del contenido.
7. La app valida la firma, la expiración, el tamaño, el checksum y la regla esperada.

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
