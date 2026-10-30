# Registro de Cambios (Changelog)

Todos los cambios relevantes de **Sistema de Precios** se documentan en este archivo.
El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y el proyecto sigue el esquema de [Versionado Semántico](https://semver.org/lang/es/).

---

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
- **Barra de navegación principal**: Eliminación de textos en los tres botones del menú principal (Catálogo, Solicitud e Historial) para maximizar el área útil de trabajo.

### Eliminado
- **Persistencia de imágenes en Historial**: Se eliminaron los campos de rutas de imagen en `PriceRequest` y su serialización en `FastBinaryStore`. Las fotografías adjuntas en solicitudes se procesan únicamente en memoria y caché temporal para su despacho inmediato por WhatsApp.
- **Dependencias y capas ORM no utilizadas**: Se removieron las clases residuales de `Room` (`AppDatabase`, `ProductDao`, `PriceRequestDao`) y se desactivaron las librerías pesadas en desuso (`Room`, `KSP`, `Retrofit`, `OkHttp`, `Moshi`, `Coil`, `Firebase`) para reducir el tamaño del APK y el consumo de memoria RAM.

---

## [2.2.0] - 2026-09-18

### Añadido
- **Primitivas de interfaz `FastComponents`**: Creación de `FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton` y `FastCheckBox` para eliminar el costo de `SubcomposeLayout` y animaciones de elevación en listas densas.
- **Exportación en Imagen Render Monocroma (`RGB_565`)**: Generación bajo demanda de listados de catálogo e historial en blanco y negro de alto contraste para envío por WhatsApp.
- **Respaldo e importación binaria V2 (`.bin`)**: Soporte de cabeceras `STP2` (catálogo), `STR2` (solicitudes) y `STC2` (configuración de local) con modos de importación **Fusionar** y **Reemplazar todo**.

---

## [2.0.0] - 2026-07-10

### Añadido
- **Motor `FastBinaryStore`**: Reemplazo de base de datos relacional por persistencia binaria directa con buffer de `16 KB`, colas `Channel.CONFLATED` y escritura atómica con `fsync`.
- **Buscador multicriterio por tokens**: Indexación precalculada en `Product.searchIndex` con soporte para preservación de la letra `ñ` e insensibilidad a tildes.
- **Cinco flujos de Solicitud de Cambio de Precios**: Implementación de las modalidades Falta Registrar (Tipo 1), Costo Subió (Tipo 2), Costo Bajó (Tipo 3), Excede % deseado (Tipo 4) e Inferior al % deseado (Tipo 5), con contabilización individual por variedad (`VarietyStockItem`).

---

## [1.0.0] - 2026-04-02

### Añadido
- Versión inicial del catálogo móvil de precios, cálculo de márgenes con redondeo comercial a múltiplos de `500` y exportación básica de fichas de productos por WhatsApp.
