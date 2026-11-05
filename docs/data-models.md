# Modelos de Datos y Esquema Binario

Este documento especifica las estructuras de datos del dominio definidas en `app/src/main/java/com/example/`, las fórmulas matemáticas de precios y la disposición de bytes con empaquetado de bits empleada por `FastBinaryStore`.

## Modelos de Dominio (`package com.example`)

### `Product`

Representa un artículo registrado en el catálogo del comercio:

| Campo | Tipo Kotlin | Descripción |
| :--- | :--- | :--- |
| `id` | `String` | Identificador único del producto (UUID). |
| `codigo` | `String?` | Código opcional del artículo (código interno o de barras). |
| `codigoAlternativo` | `String?` | Código secundario opcional. |
| `descripcion` | `String` | Nombre o descripción comercial del producto. |
| `precioCosto` | `Long?` | Precio de costo unitario en Guaraníes (`Gs.`). |
| `precioVenta` | `Long?` | Precio de venta al público en Guaraníes (`Gs.`). |
| `porcentajePersonalizado` | `Int?` | Porcentaje comercial seleccionado (`20`, `25`, `30`, `35` o `50`). |
| `porcentajeRealRedondeado` | `Int?` | Margen entero redondeado con `HALF_UP`. |
| `estadoRegistro` | `String` | Estado del ítem (`COMPLETO`, `SIN_CODIGO`, `FALTA_REGISTRAR`). |
| `notaRedondeo` | `String?` | Nota opcional de ajuste. |
| `updatedAt` | `Long` | Marca de tiempo Unix en milisegundos. |

Propiedades optimizadas en `Product`:

- `sortKey: String`: Clave normalizada con preservación de `ñ` mediante `LATIN1_LUT`.
- `costoFormateado`, `ventaFormateada`, `ventaCortaFormateada`: Propiedades diferidas (`lazy(LazyThreadSafetyMode.NONE)`) formateadas con aritmética entera.
- `matchesTokens(tokens: Array<String>): Boolean`: Evaluación lineal sobre `searchIndex` sin asignaciones por ítem.

### `VarietyStockItem`

Representa una línea de producto o variedad que comparte el mismo costo dentro de una solicitud o fila de distribuidor (`nombre`, `stock`, `recarga`). Su método `formatBulletLine(includeStock: Boolean)` construye la línea de presentación omitiendo paréntesis vacíos cuando no se declararon valores de stock o recarga.

### `DistributorInvoice` y `DistributorItem`

`DistributorInvoice` agrupa la boleta activa de un proveedor específico (`id`, `nombre`, `items: List<DistributorItem>`, `updatedAt`), exponiendo `sortKey` precalculado, `sortedItems` ordenados mediante `DistributorItem.ROW_COMPARATOR` y `nextRowOrder`.

Cada `DistributorItem` almacena una fila de la boleta con soporte completo para cualquiera de los **5 tipos de solicitud**:

| Campo | Tipo Kotlin | Descripción |
| :--- | :--- | :--- |
| `id` | `String` | Identificador único de la fila dentro de la boleta. |
| `ordenFila` | `Int` | Número de fila asignado en la boleta (`1, 2, 3...`). |
| `tipoId` | `Int` | Tipo de solicitud almacenado en la fila (`1` a `5`). |
| `porcentajeDeseado` | `Int?` | Margen objetivo seleccionado (`20`, `25`, `30`, `35` o `50`). |
| `titulo` | `String` | Título descriptivo del motivo de la solicitud. |
| `codigo` | `String?` | Código opcional del producto. |
| `descripcion` | `String` | Nombre principal del artículo en la fila. |
| `precioCostoAnterior` | `Long?` | Costo previo en `Gs.` (utilizado en los tipos `2` y `3`). |
| `precioCostoNuevo` | `Long` | Costo actual o nuevo en `Gs.`. |
| `precioVentaAnterior` | `Long?` | Precio de venta previo en `Gs.` (utilizado en los tipos `2`, `3`, `4` y `5`). |
| `porcentajeAnteriorRedondeado` | `Int?` | Porcentaje de margen previo redondeado. |
| `precioVentaResultante` | `Long` | Precio de venta resultante en `Gs.`. |
| `porcentajeMargen` | `Int` | Porcentaje de margen resultante redondeado. |
| `stockAnterior` | `String?` | Stock actual declarado para el producto principal. |
| `recargaStock` | `String?` | Cantidad de recarga o ingreso declarada para el producto principal. |
| `variedadesMismoCosto` | `String?` | Líneas de variedades adicionales que comparten el mismo costo en esa fila. |
| `ultimoTipoSolicitudId` | `Int?` | Identificador del último tipo de solicitud aplicado a la fila. |
| `updatedAt` | `Long` | Marca de tiempo Unix en milisegundos. |

### `PriceRequest`

Representa una solicitud de ajuste de precio almacenada en el historial con sus datos completos de tipo (`1` a `5`), costos anteriores/nuevos, ventas anteriores/nuevas, stock, recarga, variedades y vínculo opcional con distribuidor (`distributorId`, `ordenFilaBoleta`).

## Especificación del Esquema Binario con Máscaras de Bits (`FastBinaryStore`)

Los archivos binarios se escriben en orden **Big-Endian** utilizando un buffer lineal reutilizable (`ThreadLocal<ByteArray>`) para codificar y decodificar cadenas UTF-8 sin asignaciones intermedias. Cada entidad incluye una máscara de bits (`flags`) que indica qué campos opcionales están presentes en el flujo:

### Esquema de Catálogo (`store_products_v1.bin` - `STP3`)

- **Encabezado Mágico**: `0x53545033` (`"STP3"`, compatible en lectura con `"STP2"` y `"STP1"`).
- **Máscara de Bits por Producto (`1 byte unsigned`)**:
  - `bit 0`: `codigo` presente
  - `bit 1`: `codigoAlternativo` presente
  - `bit 2`: `precioCosto` presente
  - `bit 3`: `precioVenta` presente
  - `bit 4`: `porcentajePersonalizado` presente
  - `bit 5`: `porcentajeRealRedondeado` presente
  - `bit 6`: `notaRedondeo` presente

### Esquema de Boletas de Distribuidor (`store_distributors_v1.bin` - `FBD3`)

- **Encabezado Mágico**: `0x46424433` (`"FBD3"`, compatible en lectura con `"FBD2"` y `"FBD1"`).
- **Máscara de Bits por Fila `DistributorItem` (`1 byte unsigned`)**:
  - `bit 0`: `codigo` presente
  - `bit 1`: `porcentajeDeseado` presente
  - `bit 2`: `precioCostoAnterior` presente
  - `bit 3`: `precioVentaAnterior` presente
  - `bit 4`: `porcentajeAnteriorRedondeado` presente
  - `bit 5`: `stockAnterior` presente
  - `bit 6`: `recargaStock` presente
  - `bit 7`: `variedadesMismoCosto` presente

### Esquema de Historial (`store_requests_v1.bin` - `STR4`)

- **Encabezado Mágico**: `0x53545234` (`"STR4"`, compatible en lectura con `"STR3"`, `"STR2"` y `"STR1"`).
- **Máscara de Bits por Solicitud `PriceRequest` (`2 bytes unsigned short`)**:
  - `bit 0`: `distributorId` presente
  - `bit 1`: `ordenFilaBoleta` presente
  - `bit 2`: `productoYaRegistrado` verdadero
  - `bit 3`: `codigoEfectivo` presente
  - `bit 4`: `porcentajeDeseado` presente
  - `bit 5`: `precioCostoAnterior` presente
  - `bit 6`: `precioVentaAnterior` presente
  - `bit 7`: `porcentajeAnteriorRedondeado` presente
  - `bit 8`: `stockAnterior` presente
  - `bit 9`: `recargaStock` presente
  - `bit 10`: `variedadesMismoCosto` presente
