# Formatos de Exportación e Intercambio

La aplicación cuenta con cuatro mecanismos de salida diseñados para cubrir tanto la comunicación diaria por WhatsApp como el respaldo e impresión de listas de precios y boletas de distribuidores. Todos los formatos gráficos se renderizan con tipografía Times New Roman (`R.font.tinos`).

## Ficha Infográfica 16:9 (`1280 x 720 px`)

Cada vez que se envía una solicitud desde `RequestScreen`, `ImageUtils.generateInfographicBitmap` construye un bitmap JPEG en proporción `16:9` (`1280x720 px` al `92%` de calidad) que se envía como archivo de imagen sin texto adjunto adicional.

### Distribución Visual de la Ficha 16:9

- **Con fotografía adjunta**:
  - **Mitad superior (`1280 x 360 px`)**: Muestra la fotografía recortada en `16:9` ocupando todo el ancho, con una etiqueta en la esquina superior izquierda que indica el nombre del comercio y la fecha/hora de emisión.
  - **Mitad inferior (`1280 x 360 px`)**: Panel de información con fondo de color según el tipo de solicitud (`Tipo1Verde`, `Tipo2Azul`, `Tipo3Naranja`, `Tipo4Teal` o `Tipo5Rojo`) y tipografía Times New Roman de alto contraste. La columna izquierda lista el título del motivo, las viñetas de productos/variedades (con su stock y recarga entre paréntesis) y el código; la columna derecha presenta los precios de costo y venta con sus porcentajes de margen.
- **Sin fotografía adjunta**:
  - El panel de datos ocupa el lienzo completo de `1280x720 px`.
  - Un algoritmo de ajuste tipográfico (`fitsFullCard`) evalúa escalas desde `1.42x` hasta `0.45x` para aprovechar el espacio disponible sin truncar líneas cuando existen múltiples variedades.

## Texto Plano (`TXT`) para WhatsApp

Disponible en los diálogos de exportación de Catálogo (`WhatsAppExportDialog`), Boleta de Distribuidor (`DistributorExportDialog`) e Historial (`HistoryExportDialog`). Permite previsualizar el mensaje antes de abrir WhatsApp.

### Ejemplo de Salida TXT de Catálogo

```text
*CATÁLOGO DE PRODUCTOS - DESPENSA CENTRAL*
Total de productos: 2

1. *Bandeja de Cartón N1*
   Cód: 100648
   Costo: Gs. 3.500 | Venta: Gs. 5.000 (30%)

2. *Vaso Plástico 200ml*
   Costo: Gs. 2.000 | Venta: Gs. 3.000 (33%)
```

### Ejemplo de Salida TXT de Boleta de Distribuidor (con los 5 Tipos de Solicitud)

```text
*BOLETA DE DISTRIBUIDOR: DISTRIBUIDORA NORTE*
Comercio: DESPENSA CENTRAL
Filas incluidas: 2

Fila #1 - *Producto que su Precio de Costo Subió*
   · 1- Yerba Mate 500g (Stock: 4 unid. | Recarga: +12 unid.)
   · 2- Yerba Mate Compuesta 500g (Stock: 2 unid. | Recarga: +6 unid.)
   Código: 100200
   Precio de Costo Anterior Gs.: 7.000
   Precio de Costo Nuevo Gs.: 8.000
   Precio de Venta Anterior Gs.: 9.500 (26%)
   Precio de Venta Nuevo Gs.: 10.650 (25%)

Fila #2 - *Producto que su Precio de Venta Excede el 30%*
   · Galletita Dulce 200g (Stock: 10 unid. | Recarga: +24 unid.)
   Precio de Costo Gs.: 3.500
   Precio de Venta Anterior Gs.: 6.000 (42%)
   Precio de Venta Nuevo Gs.: 5.000 (30%)
```

### Ejemplo de Salida TXT de Historial

```text
*HISTORIAL DE SOLICITUDES - DESPENSA CENTRAL*
Total de registros: 1

1. *Producto que su Precio de Costo Subió* (04/11/2026 14:30)
   · 1- Bandeja de Cartón N1 (Stock: 5 unid. | Recarga: +24 unid.)
   · 2- Bandeja de Cartón N2 (Stock: 2 unid. | Recarga: +12 unid.)
   Código: 100648
   Precio de Costo Anterior Gs.: 3.000
   Precio de Costo Nuevo Gs.: 3.500
   Precio de Venta Anterior Gs.: 4.500 (33%)
   Precio de Venta Nuevo Gs.: 5.000 (30%)
```

## Imagen Monocroma (`Blanco y Negro`)

Pensada para lectura rápida en pantalla, envío de filas seleccionadas por doble toque a WhatsApp o impresión en impresoras térmicas o de oficina:

- **Dimensiones**: Ancho fijo de `1200 px` y alto dinámico calculado en dos pasadas (`measureOnly = true` para medir la altura exacta y `measureOnly = false` para dibujar sobre el `Bitmap`). Límite de seguridad de `14.000 px` de altura máxima.
- **Estilo**: Fondo blanco (`#FFFFFF`), tipografía Times New Roman en negro (`#111111`) y separación espacial uniforme sin depender de colores ni degradados.
- **Uso en Catálogo**: `ImageUtils.generateMonochromeCatalogBitmap` enumera cada producto con su código, costo, precio de venta y porcentaje.
- **Uso en Boleta de Distribuidor**: `ImageUtils.generateMonochromeDistributorBitmap` renderiza las filas seleccionadas por doble toque (o la boleta completa) ordenadas por `ordenFila`, incluyendo en cada fila su encabezado `Fila #X · [Título del Tipo de Solicitud]`, todas las viñetas de productos y variedades con su stock y recarga, el código y el desglose completo de precios anteriores y nuevos según el tipo (`1` a `5`).
- **Uso en Historial**: `ImageUtils.generateMonochromeHistoryBitmap` agrupa cada solicitud con su motivo, fecha, lista de variedades con stock/recarga y desglose de precios anteriores y nuevos.

## Respaldo Binario Nativo (`.bin`)

Los archivos `.bin` permiten respaldar la información completa del dispositivo o transferirla a otro teléfono sin pérdida de precisión ni sobrecarga de parseo de texto:

| Tipo de Archivo | Nombre Sugerido | Encabezado Mágico (4 bytes) | Contenido |
| :--- | :--- | :--- | :--- |
| Catálogo | `catalogo_<comercio>.bin` | `FBC1` (`0x46 0x42 0x43 0x31`) | Lista íntegra de objetos `Product` |
| Boletas de Distribuidor | `boletas_distribuidores_<comercio>.bin` | `FBD2` (`0x46 0x42 0x44 0x32`) | Lista íntegra de objetos `DistributorInvoice` y sus `DistributorItem` con los 5 tipos de solicitud (retrocompatible con `FBD1`) |
| Historial | `historial_<comercio>.bin` | `STR3` (`0x53 0x54 0x52 0x33`) | Lista íntegra de objetos `PriceRequest` con soporte de distribuidor y fila (retrocompatible con `FBR2` y `FBR1`) |

El detalle byte por byte de estos esquemas se documenta en [Modelos de Datos y Esquema Binario](data-models.md).
