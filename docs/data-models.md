# Modelos de Datos y Esquema Binario

Este documento especifica las estructuras de datos del dominio definidas en `app/src/main/java/com/example/`, las fórmulas matemáticas enteras de `MathUtils` y la disposición de bytes con empaquetado de bits empleada por `FastBinaryStore`.

## Fórmulas Comerciales Enteras (`MathUtils`)

Todos los cálculos monetarios del proyecto operan en aritmética entera de 64 bits (`Long`) sin punto flotante:

1. **Cálculo de Precio de Venta desde Porcentaje sobre Costo (`calcularVentaDesdePorcentaje`)**:
   $$\text{Precio de Venta} = \frac{\text{Costo} \times (100 + \text{Porcentaje}) + 50}{100}$$
2. **Cálculo de Porcentaje Real con Redondeo `HALF_UP` (`calcularPorcentajeRedondeado`)**:
   $$\text{Porcentaje Redondeado} = \frac{(\text{Venta} - \text{Costo}) \times 100 + \lfloor\text{Costo} / 2\rfloor}{\text{Costo}}$$
   Casos de validación verificados en `ExampleUnitTest`:
   - `Costo = 4.000`, `Venta = 5.500` da `38%`.
   - `Costo = 2.800`, `Venta = 4.000` da `43%`.
   - `Costo = 6.839`, `Venta = 8.500` da `24%`.
3. **Redondeo Comercial a Múltiplos de `500 Gs.` (`redondearGuaranies`)**:
   Si el valor tiene un resto distinto de cero al dividirse por `500`, se ajusta al múltiplo superior de `500 Gs.`:
   $$\text{Valor Redondeado} = \text{Valor} + (500 - (\text{Valor} \bmod 500))$$

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
| `porcentajePersonalizado` | `Int?` | Porcentaje seleccionado de la escala comercial (`20`, `25`, `30`, `35` o `50`). |
| `porcentajeRealRedondeado` | `Int?` | Porcentaje entero redondeado con `HALF_UP`. |
| `estadoRegistro` | `String` | Estado del ítem (`COMPLETO`, `SIN_CODIGO`, `FALTA_REGISTRAR`). |
| `notaRedondeo` | `String?` | Nota opcional de redondeo comercial. |
| `updatedAt` | `Long` | Marca de tiempo Unix en milisegundos. |

Propiedades optimizadas en `Product`:

- `sortKey: String`: Clave normalizada en `O(1)` por carácter mediante `LATIN1_LUT` con preservación de la letra `ñ`.
- `searchIndex: String`: Índice textual precalculado para búsquedas multicriterio por tokens.
- `costoFormateado`, `ventaFormateada`, `ventaCortaFormateada`: Propiedades diferidas (`lazy(LazyThreadSafetyMode.NONE)`) formateadas con el algoritmo entero de `MathUtils.formatearGuaranies`.

### `VarietyStockItem`

Representa una variedad o presentación que comparte el mismo costo dentro de una solicitud o fila de distribuidor (`nombre`, `stock`, `recarga`). Incluye `formatBulletLine(includeStock: Boolean)` para construir la línea de visualización y `parseFromFormattedLine(line: String)` para restaurar el desglose al re-editar desde el Historial o desde la Boleta de Distribuidor.

### `DistributorInvoice` y `DistributorItem`

`DistributorInvoice` agrupa la boleta de un proveedor (`id`, `nombre`, `items: List<DistributorItem>`, `updatedAt`), exponiendo `sortKey` precalculado, `sortedItems` ordenados mediante `DistributorItem.ROW_COMPARATOR` y `nextRowOrder`.

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
| `porcentajeAnteriorRedondeado` | `Int?` | Porcentaje previo redondeado con `HALF_UP`. |
| `precioVentaResultante` | `Long` | Precio de venta resultante en `Gs.`. |
| `porcentajeMargen` | `Int` | Porcentaje resultante redondeado con `HALF_UP`. |
| `stockAnterior` | `String?` | Stock actual declarado para el producto principal. |
| `recargaStock` | `String?` | Cantidad de recarga declarada para el producto principal. |
| `variedadesMismoCosto` | `String?` | Líneas de variedades adicionales que comparten el mismo costo en esa fila. |
| `ultimoTipoSolicitudId` | `Int?` | Identificador del último tipo de solicitud aplicado a la fila. |
| `updatedAt` | `Long` | Marca de tiempo Unix en milisegundos. |

### `PriceRequest`

Representa una solicitud de cambio de precio almacenada en el Historial (`store_requests_v1.bin`) sin persistir rutas de imágenes, conservando `id`, `timestamp`, `tipoId` (`1..5`), `titulo`, `productoYaRegistrado`, `codigo`, `descripcion`, `porcentajeDeseado`, `precioCostoAnterior`, `precioCostoNuevo`, `precioVentaAnterior`, `porcentajeAnteriorRedondeado`, `precioVentaNuevo`, `porcentajeNuevoRedondeado`, `stockAnterior`, `recargaStock`, `variedadesMismoCosto`, `distributorId` y `ordenFilaBoleta`.

## Especificación del Esquema Binario (`FastBinaryStore`)

Los archivos binarios se escriben con un buffer de `32 KB`, escritura atómica sobre archivo temporal `.tmp` con `fsync` y un buffer lineal reutilizable (`ThreadLocal<ByteArray>`) para codificar cadenas UTF-8 (`writeFastString` / `readFastString`) sin asignar memoria en el heap por cada campo.

### Catálogo (`store_products_v1.bin` - Cabecera `STP3`)

- **Cabecera Mágica**: `0x53545033` (`"STP3"`, retrocompatible en lectura con `"STP2"` y `"STP1"`).
- **Máscara de Bits por Producto (`1 byte unsigned`)**:
  - `bit 0`: `codigo` presente
  - `bit 1`: `codigoAlternativo` presente
  - `bit 2`: `precioCosto` presente
  - `bit 3`: `precioVenta` presente
  - `bit 4`: `porcentajePersonalizado` presente
  - `bit 5`: `porcentajeRealRedondeado` presente
  - `bit 6`: `notaRedondeo` presente

### Historial de Solicitudes (`store_requests_v1.bin` - Cabecera `STR4`)

- **Cabecera Mágica**: `0x53545234` (`"STR4"`, retrocompatible en lectura con `"STR3"`, `"STR2"` y `"STR1"`).
- **Máscara de Bits por Solicitud (`2 bytes unsigned short`)**:
  - `bit 0`: `distributorId` presente
  - `bit 1`: `ordenFilaBoleta` presente
  - `bit 2`: `productoYaRegistrado` activo
  - `bit 3`: `codigoEfectivo` presente
  - `bit 4`: `porcentajeDeseado` presente
  - `bit 5`: `precioCostoAnterior` presente
  - `bit 6`: `precioVentaAnterior` presente
  - `bit 7`: `porcentajeAnteriorRedondeado` presente
  - `bit 8`: `stockAnterior` presente
  - `bit 9`: `recargaStock` presente
  - `bit 10`: `variedadesMismoCosto` presente

### Boletas de Distribuidor (`store_distributors_v1.bin` - Cabecera `FBD3`)

- **Cabecera Mágica**: `0x46424433` (`"FBD3"`, retrocompatible en lectura con `"FBD2"` y `"FBD1"`).
- **Máscara de Bits por Fila `DistributorItem` (`1 byte unsigned`)**:
  - `bit 0`: `codigo` presente
  - `bit 1`: `porcentajeDeseado` presente
  - `bit 2`: `precioCostoAnterior` presente
  - `bit 3`: `precioVentaAnterior` presente
  - `bit 4`: `porcentajeAnteriorRedondeado` presente
  - `bit 5`: `stockAnterior` presente
  - `bit 6`: `recargaStock` presente
  - `bit 7`: `variedadesMismoCosto` presente

### Configuración de Local (`store_config_v1.bin` - Cabecera `STC2`)

- **Cabecera Mágica**: `0x53544332` (`"STC2"`, retrocompatible en lectura con `"STC1"`).
- **Contenido**: Longitud en bytes (`Int`) seguida de los bytes UTF-8 del nombre del local comercial.
