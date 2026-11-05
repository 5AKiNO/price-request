# Política de Seguridad

## Alcance y Modelo de Seguridad

**Sistema de Precios** opera de forma local en el dispositivo Android sin servidores remotos, cuentas en la nube ni transmisión automática de telemetría. Los aspectos principales de seguridad del proyecto comprenden:

1. **Integridad de archivos binarios (`.bin`)**: El motor `FastBinaryStore` valida las cabeceras mágicas (`STP3`/`STP2`/`STP1` para catálogo, `STR4`/`STR3`/`STR2`/`STR1` para solicitudes, `FBD3`/`FBD2`/`FBD1` para boletas de distribuidores y `STC2`/`STC1` para configuración) y aplica límites estrictos de cantidad de registros (`MAX_SAFE_ITEMS = 250_000`) y de longitud de cadena (`MAX_STRING_BYTES = 1_048_576`) antes de reservar o leer en el buffer lineal, previniendo desbordamientos de memoria (`OutOfMemoryError`) ante archivos corruptos o manipulados.
2. **Aislamiento de archivos compartidos (`FileProvider`)**: Las imágenes generadas para WhatsApp se escriben en el subdirectorio privado `cacheDir/shared_images` y se exponen temporalmente mediante permisos de URI de lectura (`FLAG_GRANT_READ_URI_PERMISSION`) sin requerir permisos de almacenamiento global.
3. **Permisos de mínimo privilegio**: La aplicación solo solicita el permiso de `CAMERA` en tiempo de ejecución cuando el operador pulsa el botón de cámara, y utiliza el selector nativo de medios de Android (`PickVisualMedia`) para abrir imágenes de galería sin pedir permisos de lectura sobre todo el almacenamiento del teléfono.

## Versiones Soportadas

| Versión | Estado de Soporte |
| :--- | :--- |
| `2.6.x` | Soportada con actualizaciones de seguridad |
| `2.4.x - 2.5.x` | Soporte de compatibilidad de archivos `.bin` (se recomienda actualizar a `2.6.x`) |
| `< 2.4.0` | Sin soporte activo |

## Cómo Reportar una Vulnerabilidad

Si detectas un problema de seguridad (por ejemplo, un archivo `.bin` malformado capaz de bloquear la aplicación o un problema de permisos en el `FileProvider`):

1. **No publiques el archivo de explotación en un issue público** si puede afectar a otros usuarios.
2. Comunícate de forma privada con el mantenedor del repositorio a través de los canales de contacto del perfil de GitHub o abre un reporte de seguridad privado en la pestaña **Security / Advisories** del repositorio.
3. Incluye en tu reporte:
   - Descripción técnica del fallo y el componente afectado (`FastBinaryStore`, `ImageUtils`, `UniversalTakePictureContract`, etc.).
   - Pasos detallados para reproducir el comportamiento.
   - Versión de Android y modelo de dispositivo donde se verificó.

Recibirás una respuesta inicial en un plazo estimado de 7 días hábiles con la evaluación del reporte y el plan de corrección.
