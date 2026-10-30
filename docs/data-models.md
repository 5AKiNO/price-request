# Modelos de Datos y Reglas Matemáticas

Este documento detalla las estructuras de datos inmutables del dominio (`com.example.data.model`) y las fórmulas financieras (`com.example.util.MathUtils`) que gobiernan el cálculo de precios y márgenes.

---

## 1. Modelo `Product` (`Product.kt`)

Representa un artículo del catálogo comercial. Es una `data class` inmutable que precalcula sus cadenas de visualización e indexación al momento de instanciarse para no repetir formateos durante el desplazamiento (`LazyColumn`) ni durante las búsquedas.

### Propiedades Persistidas
| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `id` | `String` | Identificador único del producto (UUID o clave generada). |
| `codigo` | `String` | Código de barras o código interno principal (vacío si es `"Sin Código"`). |
| `codigoAlternativo` | `String` | Segundo código de referencia opcional. |
| `descripcion` | `String` | Nombre comercial del artículo. |
| `precioCosto` | `Long` | Costo unitario de compra en valores enteros (Guaraníes). |
| `precioVenta` | `Long` | Precio final de venta al público en valores enteros. |
| `porcentajePersonalizado` | `Int?` | Margen objetivo asignado (`20`, `25`, `30`, `35`, `50` o `null` si fue libre). |
| `notaRedondeo` | `String` | Nota informativa cuando se aplicó redondeo comercial a múltiplo de `500`. |
| `pendienteRegistro` | `Boolean` | `true` cuando el artículo está marcado como `"Falta Registrar"` en sistema central. |
| `fechaActualizacion` | `Long` | Marca de tiempo en milisegundos (`System.currentTimeMillis()`). |

### Propiedades Precalculadas en Memoria (No consumen CPU al dibujar)
- `porcentajeRealRedondeado: Int`: Margen real obtenido entre `precioCosto` y `precioVenta`.
- `costoFormateado: String` y `ventaFormateada: String`: Textos con separador de miles (`"12.500"`).
- `codigosDisplay: String`: Etiqueta lista para mostrar en fila (`"100648"` o `"SIN CÓDIGO"`).
- `sortKey: String`: Clave normalizada para ordenamiento alfabético con soporte de `ñ`.
- `searchIndex: String`: Índice multicriterio en minúsculas para coincidencias por tokens en `matchesQuery`.

---

## 2. Modelo `PriceRequest` (`PriceRequest.kt`)

Representa un registro en el **Historial de Solicitudes**. Por diseño de ahorro de memoria y almacenamiento, **no incluye ningún campo de imagen ni URI de archivo**.

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `id` | `String` | Identificador único de la solicitud (conservado al re-editar desde el Historial). |
| `productId` | `String` | ID del producto vinculado (o cadena vacía si se redactó manualmente). |
| `tipoSolicitud` | `Int` | Identificador del tipo de solicitud (`1` a `5`). |
| `porcentajeDeseado` | `Int` | Porcentaje de referencia (`20`, `25`, `30`, `35`, `50`), usado en Tipos 4 y 5. |
| `codigo` | `String` | Código del artículo declarado en la solicitud. |
| `descripcion` | `String` | Nombre del producto principal. |
| `variedades` | `String` | Lista de variedades formateadas separadas por salto de línea (`\n`). |
| `costoAnterior` | `Long` | Costo previo (usado en Tipos 2 y 3). |
| `costoNuevo` | `Long` | Costo actual o nuevo costo de reposición. |
| `ventaAnterior` | `Long` | Precio de venta anterior (usado en Tipos 2, 3, 4 y 5). |
| `ventaNueva` | `Long` | Precio de venta nuevo propuesto. |
| `stockActual` | `Int?` | Stock físico del ítem principal (opcional en Tipos 2 a 5). |
| `cantidadRecarga` | `Int?` | Cantidad de recarga del ítem principal (opcional en Tipos 1 a 5). |
| `mensajeGenerado` | `String` | Cuerpo completo en texto plano listo para reenviar o exportar. |
| `fechaCreacion` | `Long` | Marca temporal de creación o última edición. |

---

## 3. Modelo `VarietyStockItem` (`PriceRequest.kt`)

Modela una variedad individual dentro de una solicitud (ej. sabores, talles o colores) y soporta reconstrucción bidireccional (`formatLine` <-> `parseFromFormattedLine`) para permitir la **re-edición completa desde el Historial**:

```text
VarietyStockItem(nombre = "Naranja", stock = "6", recarga = "12")
   |
   +--> formatLine(includeStock = true)
   |    Resultado: "Naranja (Stock: 6 | Recarga: 12)"
   |
   +--> VarietyStockItem.parseFromFormattedLine("· Naranja (Stock: 6 | Recarga: 12)")
        Reconstruye: VarietyStockItem(nombre = "Naranja", stock = "6", recarga = "12")
```

---

## 4. Enumeración `RequestType` (`PriceRequest.kt`)

Define los 5 tipos de solicitud admitidos:

1. `FALTA_REGISTRAR (1)`: `"Este Producto Falta Registrar"`
2. `COSTO_SUBIO (2)`: `"El costo de este producto subió"`
3. `COSTO_BAJO (3)`: `"El costo de este producto bajó"`
4. `PRECIO_EXCEDE_PORCENTAJE (4)`: `"El precio de venta excede el {porcentaje}%"`
5. `PRECIO_INFERIOR_PORCENTAJE (5)`: `"El precio de venta es inferior al {porcentaje}%"`

---

## 5. Fórmulas Financieras (`MathUtils.kt`)

Todas las operaciones monetarias utilizan enteros de 64 bits (`Long`) para evitar errores de redondeo IEEE 754:

### 5.1. Escala de Porcentajes Seleccionables
```kotlin
val SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)
```

### 5.2. Precio de Venta con Redondeo Comercial a Múltiplos de 500
Dado un `costo > 0` y un `porcentaje` seleccionado:
1. Se calcula el valor exacto:
   $$\text{VentaExacta} = \text{round}\left(\text{Costo} \times \left(1 + \frac{\text{Porcentaje}}{100}\right)\right)$$
2. Si $\text{VentaExacta} \pmod{500} \neq 0$, se redondea hacia arriba al siguiente múltiplo de `500`:
   $$\text{VentaRedondeada} = \left(\lfloor \text{VentaExacta} / 500 \rfloor + 1\right) \times 500$$

### 5.3. Porcentaje de Margen Real Redondeado
$$\text{MargenReal} = \text{round}\left(\frac{\text{PrecioVenta} - \text{PrecioCosto}}{\text{PrecioCosto}} \times 100\right)$$
