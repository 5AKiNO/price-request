# Preguntas Frecuentes

## ¿Cómo almaceno e interactúo con los 5 tipos de solicitud dentro de la sección de Distribuidores?

Tienes dos formas directas y complementarias de hacerlo:

1. **Desde la pestaña Solicitud (recomendado para cargar una boleta completa con variedades)**:
   - Selecciona cualquiera de los 5 tipos de solicitud (`Falta Registrar`, `Costo Subió`, `Costo Bajó`, `Excede % deseado`, `Inferior al % deseado`).
   - En el bloque **Vincular con Boleta de Distribuidor**, elige el distribuidor (o crea uno nuevo con `+ Nuevo Distribuidor`) y verifica el número de **Fila en Boleta**.
   - Completa los datos del producto, costos, ventas, stock y variedades, y pulsa el botón **Guardar en Boleta del Distribuidor**. El registro se guarda directamente en ese distribuidor sin abrir WhatsApp y el número de fila avanza automáticamente al siguiente para que sigas cargando.
2. **Desde la pestaña Distribuidor**:
   - Pulsa **Cargar Solicitud (Fila #N)** para saltar al formulario de Solicitud con el distribuidor y la siguiente fila ya precargados, o bien usa el botón **Agregar Fila** / **Editar en línea** dentro de la misma pestaña para elegir el tipo (`1` a `5`), ajustar costos/ventas y guardar al instante.
   - Usa los chips superiores (`Todos los tipos`, `1: Falta Registrar` .. `5: Inferior %`) para filtrar las tarjetas por color y motivo, y haz **doble toque** sobre cualquier fila para seleccionarla y enviar su render monocromo a WhatsApp.

## ¿Por qué al aplicar un 30% sobre un costo de Gs. 3.500 el precio de venta da Gs. 5.000 y no Gs. 4.550?

Porque la aplicación calcula el **margen real sobre el precio de venta** (`Costo / (1 - Margen)`), que es el estándar contable minorista para que el descuento o ganancia sobre el total facturado coincida con el porcentaje elegido:

- Con costo `Gs. 3.500` y margen `30%`: `3.500 / (1 - 0,30) = Gs. 5.000`.
- Sobre esos `Gs. 5.000` de venta, la ganancia bruta es `Gs. 1.500`, que representa exactamente el `30%` de `5.000`.

Además, todo precio sugerido se redondea automáticamente al múltiplo de `50 Gs.` más cercano para evitar vueltos inexistentes en efectivo.

## ¿Por qué los botones de porcentaje solo muestran 20%, 25%, 30%, 35% y 50%?

Esos cinco valores corresponden a los escalones de margen comercial estandarizados para operación rápida en tienda. Si un artículo específico requiere un precio distinto, puedes escribir manualmente cualquier monto en el campo **Precio de Venta (Gs.)** y la aplicación calculará y mostrará el porcentaje exacto resultante.

## ¿Qué diferencia hay entre "Excede % deseado" e "Inferior al % deseado"?

Ambos modos se utilizan cuando el **precio de costo no cambió**, pero el precio de venta actual en góndola o caja está desfasado respecto al margen objetivo del comercio:

- **Excede % deseado**: El producto se estaba vendiendo demasiado caro. Por eso el sistema exige que el **Precio de Venta Nuevo sea menor** al Precio de Venta Anterior.
- **Inferior al % deseado**: El producto estaba dejando menos margen del requerido. Por eso el sistema exige que el **Precio de Venta Nuevo sea mayor** al Precio de Venta Anterior.

## ¿Las fotos tomadas en la pestaña Solicitud ocupan espacio en el Historial o en el Distribuidor?

No. La fotografía recortada en `16:9` se utiliza únicamente para renderizar la ficha infográfica que se comparte por WhatsApp en ese momento. Tanto en el Historial (`store_requests_v1.bin`) como en las Boletas de Distribuidor (`store_distributors_v1.bin`) solo se guardan los datos de texto, códigos, variedades y precios, manteniendo los archivos de respaldo ligeros y sin saturar la memoria del teléfono.

## ¿Cómo puedo corregir un error en una solicitud o fila de distribuidor que ya guardé?

- **En la Boleta del Distribuidor**: Abre la pestaña **Distribuidor**, toca el icono de lápiz en la tarjeta de la fila para editarla en línea (incluyendo su tipo de solicitud `1..5`, costos y ventas) o ábrela en el formulario completo de Solicitud.
- **En el Historial**: Abre la pestaña **Historial**, pulsa **Editar** sobre el registro, modifica el valor en la pantalla de **Solicitud** y pulsa **Guardar Cambios en Historial** o **Actualizar y Enviar a WhatsApp**.

## ¿Qué diferencia hay entre "Fusionar" y "Reemplazar todo" al importar un archivo `.bin`?

- **Fusionar y actualizar existentes**: Mantiene los productos, boletas o solicitudes que ya tienes en tu teléfono, actualiza los que coincidan con el archivo importado y agrega los nuevos. Es la opción recomendada cuando varios empleados relevan precios y desean unir su trabajo.
- **Reemplazar todo**: Borra el listado actual del teléfono y deja exactamente los registros contenidos en el archivo `.bin` seleccionado.

## ¿La aplicación necesita internet para funcionar?

No. Toda la base de productos, las boletas de distribuidores, el cálculo de precios, la generación de imágenes `16:9` o tablas monocromas y la exportación de archivos `.bin` funcionan 100% fuera de línea. Solo se requiere conexión en el momento en que decidas enviar un mensaje a través de WhatsApp.
