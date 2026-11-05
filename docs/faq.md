# Preguntas Frecuentes

## ¿Cómo almaceno e interactúo con los 5 tipos de solicitud dentro de la sección de Distribuidores?

Tienes dos formas directas de hacerlo:

1. **Desde la pestaña Solicitud (recomendado para cargar una boleta completa con variedades)**:
   - Selecciona cualquiera de los 5 tipos de solicitud (`1: Falta Registrar`, `2: Costo Subió`, `3: Costo Bajó`, `4: Excede % deseado`, `5: Inferior al % deseado`).
   - En el bloque **Vincular con Boleta de Distribuidor**, elige el distribuidor (o crea uno nuevo con `+ Nuevo Distribuidor`) y verifica el número de **Fila en Boleta**.
   - Completa los datos del producto, costos, ventas, stock y variedades, y pulsa el botón **Guardar en Boleta del Distribuidor**. El registro se guarda directamente en ese distribuidor sin abrir WhatsApp y el número de fila avanza automáticamente al siguiente para continuar la carga.
2. **Desde la pestaña Distribuidor**:
   - Pulsa **Cargar Solicitud (Fila #N)** para abrir el formulario de Solicitud con el distribuidor y la siguiente fila ya precargados, o usa el botón **Agregar Fila** / **Editar en línea** dentro de la misma pestaña para elegir el tipo (`1` a `5`), ajustar costos y ventas y guardar al instante.
   - Usa los chips superiores (`Todos los tipos`, `1: Falta Registrar` .. `5: Inferior %`) para filtrar las tarjetas por color y motivo, y haz **doble toque** sobre cualquier fila para seleccionarla y enviar su render monocromo (`RGB_565`) a WhatsApp.

## ¿Cómo calcula la aplicación el precio de venta y el porcentaje redondeado?

La aplicación utiliza aritmética entera (`MathUtils`) con redondeo matemático `HALF_UP`:

- **Precio de venta sugerido desde porcentaje**: `((Costo * (100 + Porcentaje)) + 50) / 100`. Por ejemplo, para un costo de `Gs. 2.400` con `25%`, el precio de venta resultante es `Gs. 3.000`.
- **Porcentaje real redondeado (`HALF_UP`)**: `(((Venta - Costo) * 100) + (Costo / 2)) / Costo`. Por ejemplo, con costo `Gs. 4.000` y venta `Gs. 5.500`, el porcentaje es `37,5%`, que se redondea hacia arriba a `38%`.
- **Redondeo comercial opcional a múltiplos de `500 Gs.`**: Permite ajustar el precio de venta al múltiplo de `500 Gs.` inmediato superior cuando el comercio desea evitar monedas fraccionarias.

## ¿Por qué la escala de botones de margen muestra 20%, 25%, 30%, 35% y 50%?

Desde la versión `2.4.0`, se sustituyó el botón de `40%` por `35%` (`SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)`) para reflejar la escala comercial habitual en tienda, e igualmente se incorporaron los filtros rápidos de `30%` y `35%` en la barra superior del Catálogo. Si un artículo requiere otro valor, puedes escribir cualquier precio de venta manualmente y el sistema calculará su porcentaje exacto.

## ¿Qué diferencia hay entre "Excede % deseado" (Tipo 4) e "Inferior al % deseado" (Tipo 5)?

Ambos flujos se emplean cuando el **precio de costo se mantiene**, pero el precio de venta actual debe corregirse:

- **Tipo 4 (Excede % deseado)**: Permite y exige que el precio de venta nuevo sea menor al precio de venta anterior (`Venta Nueva < Venta Anterior`).
- **Tipo 5 (Inferior al % deseado)**: Permite y exige que el precio de venta nuevo sea mayor al precio de venta anterior (`Venta Nueva > Venta Anterior`).

## ¿Las fotografías adjuntas en las solicitudes ocupan espacio en el Historial o en el Distribuidor?

No. Desde la versión `2.4.0` se eliminó la persistencia de imágenes en el Historial. Las fotografías recortadas en proporción `16:9` se procesan únicamente en memoria (`RGB_565`) y en la caché temporal `cacheDir/shared_images` para su envío inmediato por WhatsApp.

## ¿Cómo funciona la re-edición directa en el Historial?

1. En la pestaña **Historial**, pulsa el botón **Editar** dentro de la tarjeta de la solicitud que deseas corregir.
2. Se abrirá `RequestScreen` restaurando todos los campos de la solicitud, incluido el desglose de variedades con su stock y recarga (`VarietyStockItem.parseFromFormattedLine`).
3. Elige entre las dos acciones disponibles:
   - **Guardar Cambios en Historial**: Actualiza el registro existente en su misma posición sin abrir WhatsApp.
   - **Actualizar y Enviar a WhatsApp**: Actualiza el registro en el Historial y envía la nueva ficha corregida a WhatsApp.

## ¿Qué diferencia hay entre "Fusionar" y "Reemplazar todo" al importar un archivo `.bin`?

- **Fusionar**: Conserva los registros actuales del teléfono, actualiza los que coincidan con el archivo importado y agrega los nuevos.
- **Reemplazar todo**: Sustituye la lista actual del teléfono por los registros contenidos en el archivo `.bin` seleccionado (`STP3`/`STP2`/`STP1`, `STR4`/`STR3`/`STR2`/`STR1` o `FBD3`/`FBD2`/`FBD1`).
