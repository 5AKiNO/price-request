# Flujos Funcionales de la Aplicación

Este documento detalla los flujos operativos principales de **Sistema de Precios** desde la perspectiva del usuario y su correspondencia con los componentes internos ubicados en `app/src/main/java/com/example/`.

## Gestión del Catálogo de Productos (`CatalogScreen`)

### Buscador Multicriterio por Tokens y Filtros Rápidos

En `CatalogViewModel`, la lista de productos se filtra en un flujo reactivo consolidado sobre `Dispatchers.Default`:

- **Búsqueda pretokenizada (`MathUtils.prepareQueryTokens`)**: Normaliza la consulta una sola vez con `LATIN1_LUT` (preservando la letra `ñ` e ignorando tildes) y verifica cada token contra `Product.searchIndex` mediante `prod.matchesTokens(tokens)`. Permite buscar simultáneamente por nombre, código, precio formateado (`Gs. 9.500` o `9500`), porcentaje (`35%`) o estado.
- **Filtros rápidos en barra superior**:
  - Por estado o selección: `Todos`, `Completos`, `Sin Código`, `Falta Registrar`, `Seleccionados`.
  - Por escala de márgenes comerciales (`SELECTABLE_PERCENTAGES`): `20%`, `25%`, `30%`, `35%`, `50%` y `Personalizado`.
- **Ordenamiento automático**: `FastBinaryStore` mantiene el catálogo ordenado alfabéticamente por `Product.sortKey` mediante inserción binaria (`binarySearchBy`).

### Alta y Edición de Producto (`ProductDialog`)

1. Al pulsar el botón `+` o el icono de edición en una tarjeta, se abre el formulario de producto.
2. El operador ingresa la descripción obligatoria, el código opcional y el precio de costo en Guaraníes.
3. Al seleccionar cualquiera de los botones de la escala comercial (`20%`, `25%`, `30%`, `35%`, `50%`), se calcula el precio de venta mediante `MathUtils.calcularVentaDesdePorcentaje(costo, pct)` y puede aplicarse el redondeo comercial al múltiplo superior de `500 Gs.` (`MathUtils.redondearGuaranies(valor, 500L)`).
4. Si el operador ingresa manualmente un precio de venta distinto, se calcula en vivo el porcentaje real redondeado (`HALF_UP`) mediante `MathUtils.calcularPorcentajeRedondeado(costo, venta)`.

## Creación de Solicitudes de Cambio de Precios (`RequestScreen`)

La pantalla de Solicitud ofrece cinco flujos operativos con validación específica y paleta de 5 colores identificatorios.

### Reglas de los Cinco Tipos de Solicitud

| ID | Modalidad | Color Identificatorio | Campos de Precio Requeridos | Regla de Validación en `RequestViewModel` |
| :--- | :--- | :--- | :--- | :--- |
| `1` | `Falta Registrar` | Verde (`#1B5E20`) | Costo y Venta | `Costo > 0` y `Venta > 0` |
| `2` | `Costo Subió` | Azul (`#0D47A1`) | Costo Anterior, Costo Nuevo, Venta Anterior, Venta Nueva | `Costo Nuevo > Costo Anterior` |
| `3` | `Costo Bajó` | Naranja (`#D84315`) | Costo Anterior, Costo Nuevo, Venta Anterior, Venta Nueva | `Costo Nuevo < Costo Anterior` |
| `4` | `Excede % deseado` | Teal (`#00695C`) | Porcentaje Deseado (`20%..50%`), Costo Único, Venta Anterior, Venta Nueva | `Venta Nueva < Venta Anterior` |
| `5` | `Inferior al % deseado` | Rojo (`#B71C1C`) | Porcentaje Deseado (`20%..50%`), Costo Único, Venta Anterior, Venta Nueva | `Venta Nueva > Venta Anterior` |

### Contabilización Individual por Variedad (`VarietyStockItem`)

- En los tipos `2`, `3`, `4` y `5`, el operador puede declarar el `Stock` actual y la `Recarga` del producto principal. En el tipo `1` (`Falta Registrar`), el campo de stock anterior se omite por tratarse de un producto nuevo, manteniendo el campo de `Recarga / Cantidad Inicial`.
- Cuando varias presentaciones o sabores comparten el mismo costo, pueden agregarse en la sección **Variedades con mismo costo**, cada una con su propio `Stock` y `Recarga` (`VarietyStockItem.formatBulletLine` y `parseFromFormattedLine`).

### Almacenamiento Directo en la Boleta del Distribuidor desde Solicitud

1. En el bloque **Vincular con Boleta de Distribuidor** de `RequestScreen`, el operador selecciona el proveedor activo (o crea uno nuevo con `+ Nuevo Distribuidor`) y define el número de **Fila en Boleta** (`ordenFila`).
2. Completa cualquiera de los 5 tipos de solicitud (`1` a `5`) con sus precios, stock, recarga y variedades.
3. Dispone de dos acciones de confirmación:
   - **`Guardar en Boleta del Distribuidor`**: Valida las reglas del tipo `1..5`, almacena el `DistributorItem` completo en la fila indicada del distribuidor (`FastBinaryStore.upsertDistributorItem`) sin abrir WhatsApp, limpia el formulario y avanza automáticamente el número de fila al siguiente disponible.
   - **`Generar Imagen y Enviar a WhatsApp`**: Guarda el registro tanto en el Historial como en la boleta del distribuidor (si fue seleccionado) y despacha la ficha gráfica en formato `RGB_565` hacia WhatsApp.

## Gestión de la Boleta de Distribuidor (`DistributorScreen`)

1. **Selector de Distribuidor y Filtro por los 5 Tipos de Solicitud**:
   - Permite elegir el distribuidor activo, crear uno nuevo, renombrarlo o eliminarlo.
   - Incluye chips superiores para filtrar las filas almacenadas por tipo de solicitud (`Todos los tipos`, `1: Falta Registrar`, `2: Costo Subió`, `3: Costo Bajó`, `4: Excede %`, `5: Inferior %`).
2. **Tarjetas de Fila con los 5 Colores y Desglose Completo**:
   - Las filas se ordenan por `ordenFila` ascendente mediante `DistributorItem.ROW_COMPARATOR` y muestran su tarjeta con el color del tipo de solicitud (`1` a `5`), el título del motivo, las viñetas de productos y variedades con stock/recarga, el código y el desglose de costos y ventas.
3. **Edición Rápida en Línea o Carga en Formulario Completo**:
   - Cada tarjeta puede editarse en línea (cambiando fila, tipo `1..5`, margen deseado `20%..50%`, código, descripción, costos y ventas) o abrirse en `RequestScreen`.
4. **Selección con Doble Toque y Render Monocromo (`RGB_565`) para WhatsApp**:
   - Al hacer **doble toque** sobre cualquier tarjeta, la fila se marca o desmarca (`selectedItemIds`).
   - Al pulsar **Imagen a WhatsApp** o **Exportar Boleta**, se genera una imagen monocroma en blanco y negro (`ImageUtils.generateMonochromeDistributorBitmap`) o un listado `TXT` con las filas seleccionadas, incluyendo `Fila X · [Título del Tipo de Solicitud]` y el desglose completo de precios, stock y variedades.
5. **Plantillas desde Catálogo y Sincronización al Catálogo General**:
   - **Plantilla Catálogo**: Importa artículos habituales del Catálogo hacia la boleta del distribuidor asignando filas correlativas.
   - **Sincronizar al Catálogo**: Actualiza o crea en el Catálogo general todos los productos de la boleta con sus nuevos costos y precios de venta.

## Re-edición Directa en Historial (`RequestHistoryScreen`)

1. Cada tarjeta de `RequestHistoryScreen` cuenta con el botón **Editar**.
2. Al pulsarlo, `RequestViewModel.loadFromHistoryRequest` restaura todos los campos de la solicitud en `RequestScreen` (incluyendo el desglose de variedades mediante `VarietyStockItem.parseFromFormattedLine`, el distribuidor y la fila de boleta).
3. El modo edición ofrece doble acción:
   - **`Guardar Cambios en Historial`**: Actualiza el registro existente en su misma posición dentro de `FastBinaryStore` sin abrir WhatsApp y regresa al Historial.
   - **`Actualizar y Enviar a WhatsApp`**: Actualiza el registro en el Historial y genera simultáneamente la nueva ficha gráfica para enviarla por WhatsApp.

## Respaldo e Importación Binaria (`.bin`)

En Catálogo, Boleta de Distribuidor e Historial, el menú superior permite exportar e importar archivos `.bin` (`STP3`, `FBD3`, `STR4`) con selector de modo en `BinaryImportModeDialog`:

- **Fusionar y actualizar existentes**: Conserva los registros actuales del dispositivo, actualiza las coincidencias (por código o `sortKey` en catálogo y distribuidores, o por `id` en historial) e incorpora los registros nuevos.
- **Reemplazar todo**: Sustituye la colección local por el contenido íntegro del archivo `.bin` importado.
