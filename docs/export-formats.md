# Formatos de Exportación y Almacenamiento Binario

**Sistema de Precios** utiliza tres formatos especializados según el propósito de la operación:
1. **Formato Binario Nativo (`.bin`)**: Para persistencia local ultrarrápida y copias de seguridad completas entre dispositivos.
2. **Formato de Texto Plano (`TXT`)**: Para envío inmediato de reportes legibles en chats de WhatsApp.
3. **Formato de Imagen Render Monocroma (`RGB_565` JPEG)**: Para compartir tablas e infografías en blanco y negro de alto contraste sin depender de visores externos.

---

## 1. Especificación del Formato Binario V2 (`.bin`)

Implementado en `com.example.data.local.FastBinaryStore`, emplea `DataOutputStream` y `DataInputStream` en orden de bytes *Big-Endian* estándar de la JVM.

### 1.1. Archivo de Catálogo (`catalog_fast_v2.bin`)
- **Cabecera Mágica (4 bytes ASCII)**: `STP2` (`0x53 0x54 0x50 0x32`).
- **Cantidad de Registros (`Int` - 4 bytes)**: Total de productos $N$ ($0 \le N \le 500.000$).
- **Cuerpo de Registros ($N$ bloques secuenciales)**:
  Cada producto se serializa con la siguiente secuencia exacta:
  1. `id` (`String` con longitud prefijada `Int` + bytes `UTF-8`, máx. 128 chars).
  2. `codigo` (`String` `UTF-8`, máx. 128 chars).
  3. `codigoAlternativo` (`String` `UTF-8`, máx. 128 chars).
  4. `descripcion` (`String` `UTF-8`, máx. 512 chars).
  5. `precioCosto` (`Long` - 8 bytes).
  6. `precioVenta` (`Long` - 8 bytes).
  7. `porcentajePersonalizado` (`Int` - 4 bytes; `-1` indica `null`).
  8. `notaRedondeo` (`String` `UTF-8`, máx. 256 chars).
  9. `pendienteRegistro` (`Boolean` - 1 byte).
  10. `fechaActualizacion` (`Long` - 8 bytes, epoch ms).

### 1.2. Archivo de Historial de Solicitudes (`requests_fast_v2.bin`)
- **Cabecera Mágica (4 bytes ASCII)**: `STR2` (`0x53 0x54 0x52 0x32`).
- **Cantidad de Registros (`Int` - 4 bytes)**: Total de solicitudes $M$ ($0 \le M \le 100.000$).
- **Cuerpo de Registros ($M$ bloques secuenciales, sin campos de imagen)**:
  1. `id` (`String` `UTF-8`, máx. 128 chars).
  2. `productId` (`String` `UTF-8`, máx. 128 chars).
  3. `tipoSolicitud` (`Int` - 4 bytes, valores `1..5`).
  4. `porcentajeDeseado` (`Int` - 4 bytes, ej. `20`, `25`, `30`, `35`, `50`).
  5. `codigo` (`String` `UTF-8`, máx. 128 chars).
  6. `descripcion` (`String` `UTF-8`, máx. 512 chars).
  7. `variedades` (`String` `UTF-8`, máx. 4096 chars).
  8. `costoAnterior` (`Long` - 8 bytes).
  9. `costoNuevo` (`Long` - 8 bytes).
  10. `ventaAnterior` (`Long` - 8 bytes).
  11. `ventaNueva` (`Long` - 8 bytes).
  12. `stockActual` (`Int` - 4 bytes; `Int.MIN_VALUE` indica `null`).
  13. `cantidadRecarga` (`Int` - 4 bytes; `Int.MIN_VALUE` indica `null`).
  14. `mensajeGenerado` (`String` `UTF-8`, máx. 8192 chars).
  15. `fechaCreacion` (`Long` - 8 bytes, epoch ms).

### 1.3. Archivo de Configuración del Local (`store_config_v1.bin`)
- **Cabecera Mágica (4 bytes ASCII)**: `STC2`.
- **Nombre del Local (`String` `UTF-8`, máx. 120 chars)**: Nombre personalizado del comercio.

### 1.4. Modos de Importación `.bin`
Al importar un respaldo `.bin` desde el menú de **Catálogo** o **Historial**, el usuario elige entre:
- **Fusionar**: Actualiza los registros existentes que coincidan por clave (`codigo`/`id` en Catálogo, `id` en Historial) e inserta los registros nuevos sin borrar el resto.
- **Reemplazar todo**: Sustituye por completo la colección actual por los registros contenidos en el archivo `.bin`.

---

## 2. Formato de Texto Plano (`TXT`) para WhatsApp

### 2.1. Mensaje de Solicitud de Cambio de Precios
Estructura generada automáticamente por `RequestViewModel.buildPreviewMessage`:

```text
[LOCAL: COMERCIAL SAN JOSÉ]
El precio de venta excede el 35%

Código: 100648
Producto:
· Jugo en Sobre 25g (Stock: 18 | Recarga: 24)
Variedades:
· Naranja (Stock: 6 | Recarga: 12)
· Durazno (Stock: 12)
· Manzana
Precio de Costo: 1.500 Gs.
Precio de Venta Anterior: 2.500 Gs. (+67%)
Precio de Venta Nuevo: 2.000 Gs. (+33%)
```

### 2.2. Exportación de Catálogo o Historial en Texto Plano
```text
COMERCIAL SAN JOSÉ - CATÁLOGO DE PRECIOS
Total exportado: 2 productos
----------------------------------------
[100648] Jugo en Sobre 25g
Costo: 1.500 Gs. | Venta: 2.000 Gs. (+33%)
----------------------------------------
[SIN CÓDIGO] Bandeja Plástica Mediana
Costo: 10.000 Gs. | Venta: 13.500 Gs. (+35%)
```

---

## 3. Formato de Imagen Render Monocroma (`RGB_565`)

Implementado en `com.example.util.ImageUtils`:
- **Dimensiones**: `1080 px` de ancho por alto dinámico calculado según la cantidad de líneas (`StaticLayout`), con límite de seguridad de `14.000 px` de altura.
- **Configuración de Color**: `Bitmap.Config.RGB_565` (consume exactamente la mitad de memoria RAM que `ARGB_8888`, `2 bytes` por píxel en lugar de `4 bytes`).
- **Estética Estrictamente Monocroma**:
  - Fondo: Blanco puro (`#FFFFFF`).
  - Texto y encabezados: Negro profundo (`#111111`).
  - Líneas divisorias: Trazo sólido negro (`#111111`).
- **Ciclo de Vida Efímero**: El bitmap se comprime en un archivo temporal en `cacheDir/shared_images/`, se entrega a WhatsApp mediante `FileProvider` y se invoca inmediatamente `bitmap.recycle()` para liberar la memoria nativa.
