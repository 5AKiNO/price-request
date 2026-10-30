# Flujos Funcionales y Operativos

Este documento describe paso a paso cómo interactúa el operador o encargado de tienda con los tres módulos principales de **Sistema de Precios**: **Catálogo**, **Solicitud** e **Historial**.

---

## 1. Navegación Principal de Solo Iconos

La aplicación utiliza una barra plana de tres botones con iconos sin etiquetas de texto (`FastNavIconItem`) para maximizar el espacio vertical en pantallas móviles y adaptarse como barra lateral (`60.dp`) en pantallas horizontales o tablets:

```text
+-------------------------------------------------------------------+
| [ Icono Catálogo ]      [ Icono Solicitud ]     [ Icono Historial ]|
|   Módulo 1: Stock        Módulo 2: Cálculos      Módulo 3: Auditoría|
+-------------------------------------------------------------------+
```

---

## 2. Flujo 1: Administración y Búsqueda en el Catálogo

### 2.1. Alta y Edición de Productos (`ProductDialog`)
1. El usuario pulsa el botón **`+`** en el Catálogo o selecciona un producto existente.
2. Ingresa o edita:
   - **Código principal** (opcional; si se omite, el producto se clasifica como `"Sin Código"`).
   - **Código alternativo** (opcional).
   - **Descripción del producto**.
   - **Precio de Costo** (en guaraníes / enteros).
   - **Selección de Margen Comercial**: Puede pulsar cualquiera de los botones rápidos (`20%`, `25%`, `30%`, `35%`, `50%`) para calcular automáticamente el Precio de Venta redondeado al múltiplo de `500` superior, o escribir manualmente un **Precio de Venta** personalizado.
3. Al confirmar, el producto se inserta en orden alfabético en memoria y se persiste de forma asíncrona en `catalog_fast_v2.bin`.

### 2.2. Búsqueda Multicriterio por Tokens
El operador escribe cualquier combinación de palabras o números separados por espacios en el buscador superior:
- Ejemplo 1: `bandeja 35%` -> Muestra todos los artículos cuya descripción contenga `"bandeja"` y cuyo margen sea del `35%`.
- Ejemplo 2: `caño 12.500` -> Respeta la letra `ñ` (diferenciándola de `n`), ignora tildes (`á/é/í/ó/ú`) y reconoce precios con o sin puntos separadores de miles (`12500` o `12.500`).
- Filtros rápidos de un toque en la barra superior: `Todos`, `Completos`, `Sin Código`, `Falta Registrar`, `Seleccionados`, `20%`, `25%`, `30%`, `35%` y `Personalizado`.

---

## 3. Flujo 2: Creación de Solicitudes de Cambio de Precios (5 Tipos)

El módulo de **Solicitud** permite generar reportes estandarizados para autorizar o notificar ajustes de precios por WhatsApp.

```text
+--------------------------------------------------------------------------+
| 1. Seleccionar Tipo de Solicitud (1 a 5)                                 |
| 2. Completar Código (opcional), Descripción y Variedades (Stock/Recarga) |
| 3. Ingresar Costos y Precios de Venta (con botones 20%, 25%, 30%, 35%, 50%)|
| 4. (Opcional) Adjuntar foto al vuelo con recorte 16:9 solo para el envío |
| 5. Enviar a WhatsApp -> Guarda texto y números en Historial (sin imagen) |
+--------------------------------------------------------------------------+
```

### Reglas de Negocio y Validación por Tipo de Solicitud

| Tipo | Motivo Comercial | Campos Solicitados | Regla de Validación Estricta |
| :--- | :--- | :--- | :--- |
| **Tipo 1**<br>*Falta Registrar* | Producto nuevo en tienda aún no dado de alta en sistema central. | - Precio de Costo<br>- Precio de Venta<br>- Recarga (opcional) | `Costo > 0` y `Venta > 0`. No solicita Stock Anterior. |
| **Tipo 2**<br>*Costo Subió* | El proveedor incrementó el costo de reposición del artículo. | - Costo Anterior y Costo Nuevo<br>- Venta Anterior y Venta Nueva<br>- Stock y Recarga (opcionales) | Exige `Costo Nuevo > Costo Anterior`. |
| **Tipo 3**<br>*Costo Bajó* | El proveedor redujo el costo del artículo. | - Costo Anterior y Costo Nuevo<br>- Venta Anterior y Venta Nueva<br>- Stock y Recarga (opcionales) | Exige `Costo Nuevo < Costo Anterior`. |
| **Tipo 4**<br>*Excede % deseado* | El precio de venta actual supera el margen objetivo (`20%..50%`) y se solicita **bajar** el precio de venta. | - Selector de `% deseado`<br>- Precio de Costo (único)<br>- Venta Anterior y Venta Nueva<br>- Stock y Recarga (opcionales) | Exige **`Venta Nueva < Venta Anterior`** (el nuevo precio propuesto debe ser menor al anterior). |
| **Tipo 5**<br>*Inferior al % deseado* | El precio de venta actual quedó por debajo del margen objetivo (`20%..50%`) y se solicita **subir** el precio de venta. | - Selector de `% deseado`<br>- Precio de Costo (único)<br>- Venta Anterior y Venta Nueva<br>- Stock y Recarga (opcionales) | Exige **`Venta Nueva > Venta Anterior`** (el nuevo precio propuesto debe ser mayor al anterior). |

### Contabilización por Variedad (`VarietyStockItem`)
Cuando un producto tiene múltiples sabores, colores o presentaciones bajo el mismo precio:
- Se puede declarar `Stock` y `Recarga` tanto en el nombre principal como en cada variedad agregada.
- Si una variedad no declara `Stock` ni `Recarga`, se imprime limpia (`· Durazno`) sin subtítulos vacíos.

---

## 4. Flujo 3: Re-edición de Solicitudes desde el Historial

Si un operador detecta un dato pendiente o necesita actualizar el stock, costo o precio de una solicitud ya registrada en el **Historial**:

```text
[ Pantalla Historial ]
       |
       |  1. Pulsa el botón "Editar" en la tarjeta del registro
       v
[ Pantalla Solicitud (Modo Edición Activo) ]
  - Muestra banner: "Editando solicitud del historial" + botón "Cancelar edición"
  - Carga Tipo (1..5), Código, Descripción, % deseado, Costos, Ventas y Variedades
       |
       +---> Opción A: Pulsa "Guardar Cambios en Historial"
       |     -> Actualiza el registro en su mismo ID en FastBinaryStore
       |     -> Regresa automáticamente a la pantalla de Historial
       |
       +---> Opción B: Pulsa "Actualizar y Enviar a WhatsApp"
             -> Actualiza el registro en su mismo ID en FastBinaryStore
             -> Genera la ficha actualizada y abre el selector de envío a WhatsApp
```

> **Nota sobre Memoria y Almacenamiento**: El Historial nunca guarda fotografías ni imágenes renderizadas. Toda imagen generada para WhatsApp es efímera y solo existe en la caché temporal de envío.
