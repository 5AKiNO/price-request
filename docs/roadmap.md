# Hoja de Ruta del Proyecto

Este documento presenta la evolución cronológica de **Sistema de Precios** desde su versión inicial hasta la arquitectura actual y las mejoras previstas para próximas entregas.

## Hitos Completados (`v1.0.0` a `v2.6.0`)

- [x] **`v1.0.0` (Base de Catálogo y Redondeo Comercial)**: Catálogo móvil de precios en Guaraníes, cálculo de porcentajes con redondeo `HALF_UP`, redondeo comercial a múltiplos de `500 Gs.` y exportación básica de fichas por WhatsApp.
- [x] **`v2.0.0` (Motor `FastBinaryStore` y Cinco Flujos de Solicitud)**: Reemplazo de base de datos relacional por persistencia binaria directa con colas `Channel.CONFLATED` y escritura atómica con `fsync`; buscador multicriterio por tokens con preservación de la letra `ñ`; e implementación de los 5 tipos de solicitud con contabilización individual por variedad (`VarietyStockItem`).
- [x] **`v2.2.0` (Primitivas `FastComponents`, Render Monocromo `RGB_565` y Binarios V2)**: Creación de `FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton` y `FastCheckBox`; exportación de imágenes monocromas `RGB_565` de alto contraste; y soporte de respaldos `.bin` (`STP2`, `STR2`, `STC2`) con modos **Fusionar** y **Reemplazar todo**.
- [x] **`v2.4.0` (Re-edición en Historial, Escala de `35%` y Limpieza de ORM)**: Botón `Editar` en Historial con doble acción (`Guardar Cambios en Historial` y `Actualizar y Enviar a WhatsApp`), componente `FastNavIconItem` de solo icono, sustitución de `40%` por `35%`, eliminación de imágenes persistentes en Historial y remoción de librerías ORM/red en desuso.
- [x] **`v2.5.0` (Boleta de Distribuidor con los 5 Tipos de Solicitud y Paquete Plano `com.example`)**: Cuarta sección principal para administrar boletas por proveedor ordenadas por fila (`ordenFila`), almacenamiento y filtrado de los 5 tipos de solicitud con sus 5 colores, botón directo `Guardar en Boleta del Distribuidor` desde `RequestScreen`, selección con **doble toque** para render monocromo a WhatsApp, sincronización al Catálogo general y tipografía Times New Roman (`R.font.tinos`).
- [x] **`v2.6.0` (Optimizaciones de Bajo Nivel Estilo WebAssembly)**: Tabla `O(1)` `LATIN1_LUT` para normalización sin asignaciones innecesarias, formateador entero de Guaraníes sin `DecimalFormat`, búsqueda pretokenizada única por consulta, empaquetado por máscara de bits (`STP3`, `STR4`, `FBD3`) con buffer lineal reutilizable y cálculo de luminancia entera por desplazamiento de bits.

## Próximas Mejoras Planificadas

### Corto Plazo (`v2.7.0`)

- [ ] **Filtro por rango de fechas en Historial**: Facilitar la consulta de solicitudes emitidas en un turno o semana determinada.
- [ ] **Comparador histórico de costos por distribuidor**: Visualizar la variación de costos de un mismo artículo entre distintas cargas de boleta de un proveedor.

### Mediano Plazo (`v2.8.0`)

- [ ] **Lectura rápida de códigos de barras con cámara**: Escaneo directo para completar el campo `Código` en Catálogo, Solicitud y Boleta de Distribuidor.
- [ ] **Ajuste porcentual por lote en Boleta de Distribuidor**: Aplicación de un porcentaje de incremento o descuento a todas las filas seleccionadas de un proveedor en una sola operación.
