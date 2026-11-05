# Formatos de Exportación e Intercambio

**Sistema de Precios** cuenta con cuatro mecanismos de salida diseñados para cubrir la comunicación diaria por WhatsApp y el respaldo binario entre dispositivos. Todos los formatos gráficos se renderizan en memoria mediante `android.graphics.Canvas` en configuración `Bitmap.Config.RGB_565` (que consume la mitad de memoria RAM que `ARGB_8888`) y utilizan la tipografía Times New Roman (`R.font.tinos`) almacenada en caché.

## Ficha Infográfica de Solicitud (`1080 px` de ancho, `RGB_565`)

Al confirmar una solicitud desde `RequestScreen`, `ImageUtils.generateInfographicBitmap` construye un bitmap JPEG comprimido al `85%` en `cacheDir/shared_images` y lo envía directamente a WhatsApp mediante `FileProvider` sin requerir permisos de almacenamiento externo.

### Estructura Visual de la Ficha

- **Fotografía superior opcional en proporción `16:9` (`1080 x 607 px`)**:
  - Cuando el operador adjunta una captura desde cámara o galería y la encuadra en `Crop169Dialog`, la imagen recortada en `16:9` se dibuja en la franja superior ocupando los `1080 px` de ancho.
- **Bloque informativo inferior con fondo según el tipo de solicitud (`1` a `5`)**:
  - Fondo suave de alto contraste (`#F4FBF5` para Tipo 1, `#F4F8FE` para Tipo 2, `#FFF8F2` para Tipo 3, `#F2FBF9` para Tipo 4 y `#FFF5F5` para Tipo 5) con texto en negro (`#111111`) validado por luminancia entera (`computeFastLumaScaled`).
  - Incluye el nombre del local comercial, el título del tipo de solicitud, las viñetas de productos y variedades con su `Stock` y `Recarga`, el código del producto, el número de fila en boleta (si aplica) y el desglose de precios de costo y venta con sus porcentajes redondeados.

## Imagen Render Monocroma (`Blanco y Negro`, `RGB_565`)

Diseñada para enviar listados completos o selecciones puntuales por WhatsApp con máxima legibilidad y bajo peso de archivo:

- **Dimensiones y configuración**: Ancho fijo de `1080 px`, altura dinámica calculada a partir de la suma exacta de bloques `StaticLayout` y configuración `Bitmap.Config.RGB_565` sobre fondo blanco (`#FFFFFF`) con texto negro (`#111111`).
- **Catálogo (`generateMonochromeCatalogBitmap`)**: Lista los productos seleccionados o filtrados con su descripción, código, precio de costo, precio de venta y porcentaje de ganancia.
- **Boleta de Distribuidor (`generateMonochromeDistributorBitmap`)**: Renderiza las filas marcadas mediante **doble toque** (o la boleta completa) ordenadas por `ordenFila`, mostrando en cada bloque `Fila X · [Título del Tipo de Solicitud]`, todas las viñetas de productos y variedades con su stock y recarga, el código y las líneas completas de costos y ventas anteriores/nuevos según el `tipoId` (`1` a `5`).
- **Historial (`generateMonochromeHistoryBitmap`)**: Agrupa las solicitudes seleccionadas con su título, fecha/hora, viñetas de variedades con stock/recarga, código y desglose de precios.

## Texto Plano (`TXT`) para WhatsApp

Disponible en Catálogo (`WhatsAppExportDialog`), Boleta de Distribuidor (`DistributorExportDialog`) e Historial (`HistoryExportDialog`). Permite previsualizar el mensaje formateado antes de compartirlo en WhatsApp.

### Ejemplo de Salida TXT de Boleta de Distribuidor

```text
*Boleta de Distribuidor: Distribuidora Norte*
Local: Mi Local Comercial

*Fila 1 · El precio de venta excede el 30%*
· Galletita Dulce 200g (Stock: 10 | Recarga: 24)
Precio de Costo Gs.: 3.500
Precio de Venta Anterior Gs.: 6.000 (42%)
Precio de Venta Nuevo Gs.: 5.000 (30%)

*Fila 2 · El costo de este producto subió*
· 1- Yerba Mate 500g (Stock: 4 | Recarga: 12)
· 2- Yerba Mate Compuesta 500g (Stock: 2 | Recarga: 6)
Código: 100200
Precio de Costo Anterior Gs.: 7.000
Precio de Costo Nuevo Gs.: 8.000
Precio de Venta Anterior Gs.: 9.500 (26%)
Precio de Venta Nuevo Gs.: 10.650 (25%)

Total de filas: 2
```

## Respaldo Binario Nativo (`.bin`)

Los archivos `.bin` permiten respaldar las colecciones del dispositivo o transferirlas a otro teléfono con modos **Fusionar** y **Reemplazar todo**:

| Colección | Archivo Interno | Cabecera Actual (4 bytes) | Cabeceras Retrocompatibles Soportadas |
| :--- | :--- | :--- | :--- |
| Catálogo de Productos | `store_products_v1.bin` | `STP3` (`0x53545033`) | `STP2` (`0x53545032`), `STP1` (`0x53545031`) |
| Historial de Solicitudes | `store_requests_v1.bin` | `STR4` (`0x53545234`) | `STR3` (`0x53545233`), `STR2` (`0x53545232`), `STR1` (`0x53545231`) |
| Boletas de Distribuidor | `store_distributors_v1.bin` | `FBD3` (`0x46424433`) | `FBD2` (`0x46424432`), `FBD1` (`0x46424431`) |
| Configuración de Local | `store_config_v1.bin` | `STC2` (`0x53544332`) | `STC1` (`0x53544331`) |

El detalle de los modelos y las máscaras de bits de cada cabecera se documenta en [Modelos de Datos y Esquema Binario](data-models.md).
