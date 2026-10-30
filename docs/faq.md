# Preguntas Frecuentes (FAQ) y Escenarios Comunes

Respuestas directas a las dudas operativas y técnicas más habituales al utilizar **Sistema de Precios**.

---

## 1. Operación Diaria y Solicitudes de Precio

### ¿Por qué en el Tipo 4 ("Excede % deseado") el sistema me pide un precio de venta menor al anterior?
Porque el **Tipo 4** se utiliza cuando el precio de venta actual en góndola supera el porcentaje deseado por la tienda (por ejemplo, se estaba vendiendo con un `+55%` cuando la meta es `35%`). En consecuencia, la solicitud tiene como propósito **reducir** el precio de venta (`Precio de Venta Nuevo < Precio de Venta Anterior`).
Si lo que necesitas es **aumentar** un precio de venta que quedó desfasado por debajo de la meta, debes seleccionar el **Tipo 5 ("Inferior al % deseado")**, donde la regla exige `Precio de Venta Nuevo > Precio de Venta Anterior`.

### ¿Cómo corrijo una solicitud que ya guardé o envié en el Historial?
1. Ve a la pestaña de **Historial** (tercer icono de la barra de navegación).
2. En la tarjeta de la solicitud que deseas corregir, pulsa el botón **"Editar"**.
3. La aplicación cargará todos los datos (incluyendo las variedades, su stock y recarga) en la pantalla de **Solicitud** mostrando el aviso *"Editando solicitud del historial"*.
4. Modifica los valores necesarios y elige:
   - **"Guardar Cambios en Historial"**: Sobrescribe el registro existente sin abrir WhatsApp.
   - **"Actualizar y Enviar a WhatsApp"**: Sobrescribe el registro y comparte la nueva ficha.

### ¿Por qué no se guardan las fotos en el Historial de Solicitudes?
Por una decisión deliberada de rendimiento y ahorro de espacio. En comercios que generan decenas o cientos de solicitudes semanales, almacenar fotografías recortadas e infografías dentro del historial satura rápidamente el almacenamiento interno y la memoria RAM en teléfonos de gama de entrada.
Cuando adjuntas una foto en la pantalla de **Solicitud**, la imagen se recorta en formato `16:9` y se envía a WhatsApp al instante, pero en el Historial solo se conserva el registro estructurado de texto y valores numéricos.

---

## 2. Catálogo y Búsqueda

### ¿Cómo busco productos que no tienen código de barras o que están al 35%?
- Puedes usar los **filtros rápidos** debajo del buscador (`Sin Código`, `Falta Registrar`, `20%`, `25%`, `30%`, `35%`, `Personalizado`).
- O escribir directamente en el buscador términos combinados como:
  - `sin codigo PLASTICO`
  - `35% bandeja`
  - `15.000` o `15000` (encuentra tanto costos como precios de venta con ese monto).

### ¿El buscador distingue entre la letra `n` y la `ñ`?
Sí. El normalizador lingüístico (`MathUtils.normalizarConEnie`) elimina las tildes de las vocales (`á -> a`, `é -> e`, etc.) para que no tengas que escribir acentos en el teclado móvil, pero **preserva estrictamente la letra `ñ`** como carácter distinto de la `n` (por ejemplo, `"caño"` no se mezcla con `"cano"`).

---

## 3. Respaldos Binarios (`.bin`) y Cambio de Teléfono

### ¿Cómo paso todo mi catálogo e historial a otro teléfono Android?
1. En el teléfono origen, abre el menú (`⋮`) del **Catálogo** y selecciona **"Exportar respaldo binario (.bin)"**. Guarda el archivo y envíalo por WhatsApp, cable USB o carpeta compartida al nuevo dispositivo.
2. En el teléfono destino, abre el menú (`⋮`) del **Catálogo**, elige **"Importar respaldo binario (.bin)"** y selecciona el archivo recibido.
3. Elige si deseas **Fusionar** (combinar con lo que ya tenga el teléfono) o **Reemplazar todo**.
4. Repite el mismo proceso desde el menú (`⋮`) de la pantalla de **Historial** si deseas trasladar también las solicitudes históricas.

### ¿La aplicación necesita internet para funcionar?
No. Toda la base binaria, el buscador por tokens, el cálculo de márgenes y el generador de imágenes monocromas funcionan **100% fuera de línea (offline)**. Solo necesitas conexión en el momento en que decidas enviar un mensaje o imagen a través de WhatsApp.
