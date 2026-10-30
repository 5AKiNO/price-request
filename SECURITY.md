# Política de Seguridad

## Alcance y Arquitectura Offline-First

**Sistema de Precios** está diseñado como una aplicación Android 100% local (*offline-first*):
- No realiza conexiones a servidores externos ni transmite catálogos o precios por internet de forma autónoma.
- No solicita permisos invasivos de almacenamiento global; utiliza el selector de documentos del sistema (`Storage Access Framework`) para importar/exportar respaldos `.bin` y el selector nativo (`PickVisualMedia`) cuando se adjunta una fotografía temporal para WhatsApp.
- La compartición de archivos temporales con WhatsApp se realiza exclusivamente a través de `FileProvider` con permisos temporales por URI (`FLAG_GRANT_READ_URI_PERMISSION`).

---

## Versiones Soportadas

| Versión | Estado de Soporte |
| :--- | :--- |
| `2.4.x` | Soportada activamente |
| `< 2.4.0` | Sin soporte de parches (se recomienda actualizar a `2.4.0`) |

---

## Áreas Sensibles de Seguridad

1. **Deserialización de archivos `.bin` (`FastBinaryStore.kt`)**:
   - La lectura de archivos binarios valida las cabeceras mágicas (`STP2`, `STR2`, `STC2`), verifica límites máximos de registros (`500.000` productos / `100.000` solicitudes) y acota la longitud máxima de cada cadena (`maxLength`) para prevenir ataques de agotamiento de memoria (*Out-Of-Memory*) mediante archivos `.bin` corruptos o manipulados.
2. **Exposición de Archivos Temporales (`FileProvider`)**:
   - Solo el subdirectorio de caché de exportación se comparte temporalmente al invocar el `Intent.ACTION_SEND`.

---

## Cómo Reportar una Vulnerabilidad

Si detectas un fallo que permita corromper el almacenamiento binario, provocar un cierre inesperado mediante un archivo `.bin` malformado o comprometer el aislamiento de `FileProvider`:

1. **No abras un Issue público con el archivo de explotación adjunto.**
2. Envía un reporte detallado al titular del repositorio indicando:
   - Descripción técnica del vector (por ejemplo, desbordamiento en lectura de `DataInputStream`).
   - Pasos exactos para reproducirlo.
   - Propuesta de mitigación si dispones de ella.
3. Recibirás confirmación de recepción y evaluación técnica en un plazo máximo de 7 días hábiles.
