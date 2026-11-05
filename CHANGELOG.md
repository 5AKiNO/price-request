# Registro de Cambios (Changelog)

Todos los cambios relevantes de **Sistema de Precios** se documentan en este archivo.
El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y el proyecto sigue el esquema de [Versionado Semántico](https://semver.org/lang/es/).

## [2.6.0] - 2026-11-05

### Añadido
- **Tabla de búsqueda estática `O(1)` (`LATIN1_LUT`)**: Arreglo de 256 entradas en `MathUtils.normalizarConEnie` que resuelve minúsculas y eliminación de tildes (preservando la letra `ñ`) en tiempo constante y devuelve la misma referencia `String` cuando el texto no requiere cambios.
- **Formateador monetario entero de Guaraníes**: Implementación en `MathUtils.formatearGuaranies` sobre un buffer fijo `CharArray(28)` en `ThreadLocal` mediante divisiones enteras por `10`, eliminando `DecimalFormat` y las asignaciones de objetos por llamada.
- **Pre-tokenizador de consultas (`MathUtils.prepareQueryTokens` y `matchesTokens`)**: Tokenización única por cambio de búsqueda en `CatalogViewModel` y `DistributorViewModel`, evitando invocar `split` y `replace` dentro de cada producto o fila.
- **Empaquetado por máscara de bits (`bitmask`) y buffer lineal UTF-8 en `FastBinaryStore`**: Cabeceras `STP3` (catálogo), `STR4` (solicitudes) y `FBD3` (boletas de distribuidor) que agrupan la presencia de campos opcionales en un entero de banderas y reutilizan un `ThreadLocal<ByteArray>` para lectura y escritura de cadenas, manteniendo retrocompatibilidad con `STP2`, `STP1`, `STR3`, `STR2`, `STR1`, `FBD2` y `FBD1`.
- **Contraste por desplazamiento de bits y caché de fuente**: Función `computeFastLumaScaled` basada en aritmética entera `(R * 299 + G * 587 + B * 114)` en `Color.kt` e `ImageUtils.kt`, junto con caché `@Volatile` del `Typeface` `R.font.tinos`.

### Cambiado
- **Evaluación diferida en modelos y ordenamiento**: Propiedades `lazy(LazyThreadSafetyMode.NONE)` y getters bajo demanda en `Product`, `DistributorItem` y `PriceRequest`, clave `sortKey` precalculada y comparador compartido `DistributorItem.ROW_COMPARATOR`.
- **Consolidación de flujos reactivos**: Unificación de operadores `combine` sobre `Dispatchers.Default` en `CatalogViewModel` y `DistributorViewModel`, eliminando clases de tuplas intermedias.

## [2.5.0] - 2026-11-04

### Añadido
- **Módulo de Boleta de Distribuidor (`DistributorScreen`, `DistributorViewModel`, `DistributorInvoice`, `DistributorItem`)**: Cuarta sección principal en la barra de navegación continua para gestionar productos de un distribuidor activo ordenados por número de fila (`ordenFila`).
- **Almacenamiento de los 5 tipos de solicitud en Distribuidores (`FBD2`/`FBD3`)**: Soporte en cada fila de la boleta para conservar el tipo de solicitud (`1` a `5`), título, porcentaje deseado, costo anterior y nuevo, venta anterior y resultante, margen, stock, recarga y variedades del mismo costo.
- **Botón directo `"Guardar en Boleta del Distribuidor"` en `RequestScreen`**: Permite registrar cualquiera de los 5 tipos de solicitud directamente en la fila indicada del proveedor sin abrir WhatsApp, avanzando automáticamente al siguiente número de fila disponible.
- **Tarjetas con los 5 colores por tipo y filtro rápido en `DistributorScreen`**: Visualización cromática por motivo (`Tipo1Verde`, `Tipo2Azul`, `Tipo3Naranja`, `Tipo4Teal`, `Tipo5Rojo`), chips de filtrado por tipo (`1` a `5`) y edición rápida en línea.
- **Selección con doble toque para render monocromo (`RGB_565`)**: Marcado rápido de filas en la boleta mediante doble toque para generar y enviar por WhatsApp una tabla monocroma (`generateMonochromeDistributorBitmap`) con el título del tipo de solicitud y el desglose completo de precios, stock y variedades por fila.
- **Plantillas desde Catálogo y sincronización en un toque**: Importación de productos habituales del Catálogo hacia la boleta del proveedor y sincronización directa de los precios resultantes hacia el Catálogo general (`syncDistributorItemsToCatalog`).
- **Tipografía general Times New Roman (`R.font.tinos`)**: Aplicación uniforme de la fuente serif tanto en la interfaz de Jetpack Compose (`Type.kt`) como en el motor de renderizado de bitmaps (`ImageUtils.kt`).

### Cambiado
- **Estructura de código aplanada en `com.example`**: Todos los archivos Kotlin del módulo `app` se organizan directamente en `app/src/main/java/com/example/` bajo `package com.example`, eliminando subcarpetas anidadas innecesarias.
- **Superficies planas sobre grilla de `8.dp`**: Interfaz construida sin bordes superpuestos ni líneas divisorias, utilizando separación espacial en múltiplos de `8.dp`.

## [2.4.0] - 2026-10-29

### Añadido
- **Re-edición directa en Historial**: Botón `"Editar"` en cada tarjeta de `RequestHistoryScreen` que restaura todos los campos de la solicitud en `RequestScreen` (incluyendo desglose de variedades con `VarietyStockItem.parseFromFormattedLine`) y actualiza el registro existente en su posición (`FastBinaryStore.savePriceRequest`).
- **Doble acción en modo edición**: Botones `"Guardar Cambios en Historial"` (actualización inmediata sin abrir WhatsApp) y `"Actualizar y Enviar a WhatsApp"`.
- **Componente `FastNavIconItem`**: Botón plano de navegación de una sola capa que muestra únicamente el icono centrado sin etiquetas de texto.
- **Filtro rápido de `35%` y `30%` en Catálogo**: Incorporación de filtros directos por margen comercial en la barra superior de `CatalogScreen`.

### Cambiado
- **Escala de márgenes comerciales**: Sustitución del botón de `40%` por `35%` en toda la aplicación (`SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)`).
- **Reglas de validación para Solicitudes Tipo 4 y Tipo 5**:
  - **Tipo 4 (Excede % deseado)**: Permite y exige que el precio de venta nuevo sea menor al precio de venta anterior (`Venta Nueva < Venta Anterior`).
  - **Tipo 5 (Inferior al % deseado)**: Permite y exige que el precio de venta nuevo sea mayor al precio de venta anterior (`Venta Nueva > Venta Anterior`).
- **Barra de navegación principal**: Eliminación de textos en los botones del menú principal para maximizar el área útil de trabajo.

### Eliminado
- **Persistencia de imágenes en Historial**: Se eliminaron los campos de rutas de imagen en `PriceRequest` y su serialización en `FastBinaryStore`. Las fotografías adjuntas en solicitudes se procesan únicamente en memoria y caché temporal para su despacho inmediato por WhatsApp.
- **Dependencias y capas ORM no utilizadas**: Se removieron las clases residuales de `Room` (`AppDatabase`, `ProductDao`, `PriceRequestDao`) y se desactivaron las librerías pesadas en desuso (`Room`, `KSP`, `Retrofit`, `OkHttp`, `Moshi`, `Coil`, `Firebase`) para reducir el tamaño del APK y el consumo de memoria RAM.

## [2.2.0] - 2026-09-18

### Añadido
- **Primitivas de interfaz `FastComponents`**: Creación de `FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton` y `FastCheckBox` para eliminar el costo de `SubcomposeLayout` y animaciones de elevación en listas densas.
- **Exportación en Imagen Render Monocroma (`RGB_565`)**: Generación bajo demanda de listados de catálogo e historial en blanco y negro de alto contraste para envío por WhatsApp.
- **Respaldo e importación binaria V2 (`.bin`)**: Soporte de cabeceras `STP2` (catálogo), `STR2` (solicitudes) y `STC2` (configuración de local) con modos de importación **Fusionar** y **Reemplazar todo**.

## [2.0.0] - 2026-07-10

### Añadido
- **Motor `FastBinaryStore`**: Reemplazo de base de datos relacional por persistencia binaria directa con buffer de `16 KB`, colas `Channel.CONFLATED` y escritura atómica con `fsync`.
- **Buscador multicriterio por tokens**: Indexación precalculada en `Product.searchIndex` con soporte para preservación de la letra `ñ` e insensibilidad a tildes.
- **Cinco flujos de Solicitud de Cambio de Precios**: Implementación de las modalidades Falta Registrar (Tipo 1), Costo Subió (Tipo 2), Costo Bajó (Tipo 3), Excede % deseado (Tipo 4) e Inferior al % deseado (Tipo 5), con contabilización individual por variedad (`VarietyStockItem`).

## [1.0.0] - 2026-04-02

### Añadido
- Versión inicial del catálogo móvil de precios, cálculo de márgenes con redondeo comercial a múltiplos de `500` y exportación básica de fichas de productos por WhatsApp.
