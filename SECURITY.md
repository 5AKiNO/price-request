# Política de Seguridad

## Alcance y Modelo de Seguridad

Esta aplicación opera de forma local en el dispositivo Android sin servidores remotos, cuentas en la nube ni transmisión automática de telemetría. Los aspectos principales de seguridad del proyecto comprenden:

1. **Integridad de archivos binarios (`.bin`)**: El motor `FastBinaryStore` valida encabezados mágicos (`FBC1` para catálogo y `FBR2`/`FBR1` para historial) y límites de cantidad de registros (`0..200.000` productos y `0..100.000` solicitudes) antes de reservar memoria en colecciones, previniendo desbordamientos de memoria (`OutOfMemoryError`) ante archivos corruptos o manipulados.
2. **Aislamiento de archivos compartidos (`FileProvider`)**: Las imágenes generadas para WhatsApp se escriben en el subdirectorio privado `cacheDir/shared_images` y se exponen temporalmente mediante permisos de URI de lectura (`FLAG_GRANT_READ_URI_PERMISSION`) sin requerir permisos de almacenamiento global.
3. **Permisos de mínimo privilegio**: La aplicación solo solicita el permiso de `CAMERA` en tiempo de ejecución cuando el operador pulsa el botón de cámara, y utiliza el selector nativo de medios de Android (`PickVisualMedia`) para abrir imágenes de galería sin pedir permisos de lectura sobre todo el almacenamiento del teléfono.

## Versiones Soportadas

| Versión | Estado de Soporte |
| :--- | :--- |
| `1.3.x` | Soportada con actualizaciones de seguridad |
| `< 1.3.0` | Sin soporte activo (se recomienda actualizar a la última versión) |

## Cómo Reportar una Vulnerabilidad

Si detectas un problema de seguridad (por ejemplo, un archivo `.bin` malformado capaz de bloquear la aplicación o un problema de permisos en el `FileProvider`):

1. **No publiques el archivo de explotación en un issue público** si puede afectar a otros usuarios.
2. Comunícate de forma privada con el mantenedor del repositorio a través de los canales de contacto del perfil de GitHub o abre un reporte de seguridad privado en la pestaña **Security -> Advisories** del repositorio.
3. Incluye en tu reporte:
   - Descripción técnica del fallo y el componente afectado (`FastBinaryStore`, `ImageUtils`, `UniversalTakePictureContract`, etc.).
   - Pasos detallados para reproducir el comportamiento.
   - Versión de Android y modelo de dispositivo donde se verificó.

Recibirás una respuesta inicial en un plazo estimado de 7 días hábiles con la evaluación del reporte y el plan de corrección.
