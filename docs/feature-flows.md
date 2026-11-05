# Flujos Funcionales de la Aplicación

Este documento detalla los flujos operativos principales de la aplicación desde la perspectiva del usuario y su correspondencia con los componentes internos ubicados en `app/src/main/java/com/example/`.

## Gestión del Catálogo de Productos

### Búsqueda, Filtrado y Ordenamiento

En `CatalogScreen`, la lista de productos se recalcula de forma reactiva en `CatalogViewModel.recomputeFilteredList` cada vez que cambia cualquiera de los siguientes parámetros:

- **Texto de búsqueda (`searchQuery`)**: Filtra por coincidencias parciales sin distinción de mayúsculas ni acentos (preservando estrictamente la letra `ñ` mediante `MathUtils.normalizarConEnie`) tanto en `descripcion` como en `codigo`.
- **Filtro de margen (`selectedPercentageFilter`)**: Permite aislar productos cuyo margen redondeado coincida con `20%`, `25%`, `30%`, `35%` o `50%`, o mostrar `Todos`.
- **Ordenamiento (`sortOption`)**:
  - `Alfabetico (A-Z)` / `Alfabetico (Z-A)`
  - `Precio Venta (Menor a Mayor)` / `Precio Venta (Mayor a Menor)`
  - `Precio Costo (Menor a Mayor)` / `Precio Costo (Mayor a Menor)`

### Alta y Edición de Producto (`ProductDialog`)

1. Al pulsar el botón `+` o el icono de lápiz en un producto existente, se abre `ProductDialog`.
2. El operador ingresa la descripción obligatoria, el código opcional y el precio de costo en Guaraníes.
3. Al tocar cualquiera de los chips de porcentaje (`20%`, `25%`, `30%`, `35%`, `50%`), la aplicación calcula el precio de venta sugerido usando `MathUtils.calcularPrecioVenta(costo, pct)` y lo redondea al múltiplo de `50 Gs.` más cercano.
4. Si el operador escribe manualmente un precio de venta distinto, el diálogo calcula en vivo el porcentaje resultante mediante `MathUtils.calcularPorcentajeMargen`.

## Creación de Solicitudes de Ajuste de Precios

La vista de Solicitud (`RequestScreen`) ofrece cinco modos de operación seleccionables en una fila horizontal de opciones con su paleta de 5 colores identificatorios.

### Reglas de los Cinco Tipos de Solicitud

| ID | Nombre en Interfaz | Color Identificatorio | Campos de Precio Requeridos | Regla de Validación en `RequestViewModel` |
| :--- | :--- | :--- | :--- | :--- |
| `1` | `Falta Registrar` | Verde (`#2E7D32`) | Costo y Venta | `Costo > 0` y `Venta > 0` |
| `2` | `Costo Subió` | Azul (`#1565C0`) | Costo Anterior, Costo Nuevo, Venta Anterior, Venta Nueva | `Costo Nuevo > Costo Anterior` |
| `3` | `Costo Bajó` | Naranja (`#EF6C00`) | Costo Anterior, Costo Nuevo, Venta Anterior, Venta Nueva | `Costo Nuevo < Costo Anterior` |
| `4` | `Excede % deseado` | Teal (`#00695C`) | Porcentaje Deseado (`20%..50%`), Costo Único, Venta Anterior, Venta Nueva | `Venta Nueva < Venta Anterior` |
| `5` | `Inferior al % deseado` | Rojo (`#C62828`) | Porcentaje Deseado (`20%..50%`), Costo Único, Venta Anterior, Venta Nueva | `Venta Nueva > Venta Anterior` |

### Declaración de Stock, Recarga y Variedades

- En los tipos `2`, `3`, `4` y `5`, el operador puede declarar opcionalmente el `Stock Principal` y la `Recarga Principal`. En el tipo `1` (`Falta Registrar`), el campo de stock anterior se omite por tratarse de un artículo nuevo, manteniendo el campo `Recarga / Cantidad Inicial`.
- Si un producto tiene varias presentaciones o sabores que comparten exactamente el mismo costo y precio de venta, el usuario puede agregarlos en la sección **Variedades con mismo costo**.
- Cada variedad agregada se numera automáticamente si la descripción principal no termina en número (`1- Bandeja Roja`, `2- Bandeja Azul`, etc.) e incluye sus propios campos editables de `Stock` y `Recarga`.

### Almacenamiento Directo en la Boleta del Distribuidor desde Solicitud

Para almacenar cualquiera de los 5 tipos de solicitud dentro de un distribuidor específico:

1. En `RequestScreen`, en el bloque **Vincular con Boleta de Distribuidor**, el operador selecciona el distribuidor activo (o crea uno nuevo con `+ Nuevo Distribuidor`) y define el número de **Fila en Boleta** (`ordenFila`).
2. Completa los campos del tipo de solicitud elegido (`1` a `5`), incluyendo precios anteriores/nuevos, stock, recarga y variedades.
3. Dispone de dos botones de confirmación:
   - **`Guardar en Boleta del Distribuidor`**: Valida las reglas del tipo `1..5`, almacena el ítem con todos sus datos en la fila indicada del distribuidor (`FastBinaryStore.upsertDistributorItem`) sin abrir WhatsApp, limpia el formulario e incrementa automáticamente el número de fila al siguiente disponible para continuar cargando artículos de la factura.
   - **`Generar Imagen y Enviar a WhatsApp`**: Guarda el ítem tanto en el Historial como en la boleta del distribuidor (si fue seleccionado) y genera la ficha `16:9` para compartirla por WhatsApp.

## Gestión de la Boleta de Distribuidor (`DistributorScreen`)

La pestaña **Distribuidor** (`DistributorScreen` y `DistributorViewModel`) centraliza todos los productos y solicitudes asociados a un proveedor específico:

1. **Selector de Distribuidor y Filtro por los 5 Tipos de Solicitud**:
   - Permite alternar entre los distribuidores registrados o crear/renombrar/eliminar proveedores.
   - Debajo del selector de distribuidor se muestra una fila de chips para filtrar las filas almacenadas por tipo de solicitud (`Todos los tipos`, `1: Falta Registrar`, `2: Costo Subió`, `3: Costo Bajó`, `4: Excede %`, `5: Inferior %`).
2. **Tarjetas de Fila con los 5 Colores y Desglose Completo**:
   - Cada `DistributorItem` se ordena por `ordenFila` ascendente y se presenta con el color correspondiente a su `tipoId` (`1` a `5`), mostrando el número de fila, el título del motivo, las viñetas de productos y variedades con su stock y recarga, el código y las líneas completas de costos y ventas (anteriores y nuevos según el tipo).
3. **Edición Rápida en Línea o en el Formulario Completo**:
   - Al tocar el icono de edición rápida en una tarjeta, se despliega un panel en línea que permite cambiar el número de fila, el tipo de solicitud (`1` a `5`), el margen deseado (`20%..50%`), el código, la descripción, el stock/recarga, los costos y las ventas con recálculo automático en vivo.
   - También es posible abrir la fila en el formulario completo de la pestaña **Solicitud** o pulsar el botón superior **Cargar Solicitud (Fila #N)** para registrar una nueva fila con soporte de variedades.
4. **Selección con Doble Toque y Renderizado Monocromo para WhatsApp**:
   - Al realizar **doble toque** sobre cualquier tarjeta de la boleta, la fila alterna su estado de selección (`selectedItemIds`).
   - El panel de acciones permite pulsar **Imagen a WhatsApp** o **Exportar Boleta** para generar una tabla monocroma (`ImageUtils.generateMonochromeDistributorBitmap`) o un mensaje `TXT` con los ítems seleccionados (o toda la boleta si no hay selección manual), incluyendo el encabezado de fila, el título del tipo de solicitud y el desglose completo de precios, stock y variedades.
5. **Plantillas Reutilizables y Sincronización al Catálogo General**:
   - **Plantilla Catálogo**: Permite marcar productos habituales del Catálogo general para incorporarlos a la boleta del distribuidor con numeración de fila correlativa.
   - **Sincronizar al Catálogo**: Actualiza o inserta en un toque los precios de costo y venta resultantes de la boleta dentro del Catálogo principal (`CatalogViewModel`).

## Captura Fotográfica y Recorte 16:9 (`Crop169Dialog`)

1. En `RequestScreen`, el usuario puede tomar una foto con la cámara (`UniversalTakePictureContract`) o elegir una imagen desde la galería (`PickVisualMedia` con respaldo `GetContent`).
2. Antes de adjuntarla a la solicitud, se abre `Crop169Dialog`, que presenta un visor con proporción fija `16:9` y guías de tercios.
3. Mediante gestos táctiles de pellizco y arrastre (`detectTransformGestures`), el operador encuadra la zona relevante del producto o factura.
4. Al confirmar con `Recortar 16:9`, `ImageUtils.cropBitmapTo169FromView` extrae un bitmap de `1280x720 px` listo para integrarse en la ficha final.

## Re-edición de Solicitudes desde el Historial

Cuando un operador necesita corregir un monto, variedad o código de una solicitud ya registrada en la pestaña de Historial:

1. En `RequestHistoryScreen`, cada elemento de historial incluye el botón `Editar`.
2. Al pulsarlo, `MainActivity` invoca `requestViewModel.loadRequestForEditing(request)` y cambia la vista activa a la pestaña de Solicitud.
3. El formulario carga todos los datos guardados de la solicitud (`selectedTipoId`, `porcentajeDeseado`, `codigoProducto`, `descripcion`, `stockAnterior`, `recargaStock`, `variedadesList`, distribuidor, fila, costos y precios de venta) y muestra un indicador superior: **Editando solicitud del historial** junto al botón `Cancelar edición`.
4. El operador dispone de dos acciones de guardado:
   - **`Guardar Cambios en Historial`**: Valida los campos, actualiza el registro existente conservando su mismo `id` y posición en `store_requests_v1.bin`, limpia el formulario y regresa a la pestaña de Historial.
   - **`Actualizar y Enviar a WhatsApp`**: Actualiza el registro en el historial y genera simultáneamente la nueva infografía `16:9` para compartirla por WhatsApp.

## Respaldo e Intercambio de Archivos `.bin`

Tanto en Catálogo como en Distribuidor e Historial, el menú superior de tres puntos ofrece las opciones de exportación e importación binaria `.bin`:

1. **Exportación**: Abre el selector de documentos del sistema (`CreateDocument`) sugiriendo el nombre `catalogo_<comercio>.bin`, `boletas_distribuidores_<comercio>.bin` o `historial_<comercio>.bin` y escribe el flujo binario.
2. **Importación**: Al elegir un archivo `.bin` (`OpenDocument`), se presenta `BinaryImportModeDialog` con dos estrategias:
   - **Fusionar y actualizar existentes**: Conserva los registros actuales del teléfono, actualiza los que coincidan y agrega los registros nuevos.
   - **Reemplazar todo**: Sustituye la lista local por el contenido del archivo importado.
