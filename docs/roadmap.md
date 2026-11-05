# Hoja de Ruta del Proyecto

Este documento presenta los hitos ya incorporados en la aplicación y las líneas de evolución previstas para futuras versiones.

## Hitos Completados (`v1.0` a `v1.4`)

- [x] **Motor de persistencia binaria (`FastBinaryStore`)**: Sustitución de bases de datos pesadas por lectura/escritura secuencial con renombrado atómico `.tmp`.
- [x] **Estructura de código aplanada en `com.example` e interfaz de componentes planos sobre grilla de `8.dp` (`FastComponents`)**: Todos los archivos Kotlin organizados directamente en `app/src/main/java/com/example/`, superficies de color sólido sin bordes ni líneas divisorias, barra de navegación continua de iconos y tipografía general Times New Roman (`R.font.tinos`).
- [x] **Cinco modalidades de solicitud con reglas de validación y paleta de 5 colores**: Cobertura para productos sin registrar, subas/bajas de costo y correcciones de margen por exceso o defecto.
- [x] **Módulo de Boleta de Distribuidor con almacenamiento de los 5 tipos de solicitud**:
  - Creación directa desde la pestaña **Solicitud** con el botón **Guardar en Boleta del Distribuidor** sin necesidad de abrir WhatsApp, avanzando automáticamente el número de fila (`ordenFila`).
  - Visualización en tarjetas con los 5 colores por tipo de solicitud, filtro rápido por tipo (`1` a `5`) y edición rápida en línea.
  - Selección de filas con **doble toque** para renderizar y enviar por WhatsApp una tabla monocroma con el título del tipo de solicitud y el desglose completo de precios, stock y variedades.
  - Plantillas reutilizables desde el Catálogo y sincronización de precios en un toque hacia el Catálogo general.
- [x] **Soporte de variedades con mismo costo**: Declaración individual de stock y recarga por variedad con numeración automática.
- [x] **Re-edición de solicitudes desde el Historial**: Carga completa en el formulario de Solicitud y actualización en el mismo lugar sin duplicar registros ni almacenar fotografías en el historial.
- [x] **Exportación multimodal**: Fichas `16:9` (`1280x720 px`), texto plano (`TXT`), imágenes monocromas de tabla e intercambio de respaldos `.bin` (`FBC1`, `FBD2`, `STR3`) con fusión o reemplazo.

## Próximas Mejoras Planificadas

### Corto Plazo (`v1.5`)

- [ ] **Filtro por rango de fechas en Historial**: Facilitar la búsqueda de ajustes realizados en un turno o semana específica.
- [ ] **Comparador histórico de costos por distribuidor**: Visualizar la evolución de costos de un mismo artículo entre distintas boletas cargadas a lo largo del mes.

### Mediano Plazo (`v1.6`)

- [ ] **Lectura rápida de códigos de barras con cámara**: Integración ligera para completar el campo `Código del Producto` escaneando el envase desde el Catálogo, la Boleta de Distribuidor o la pantalla de Solicitud.
- [ ] **Ajuste masivo de precios por lote**: Herramienta para aplicar una variación porcentual a todas las filas de un distribuidor y recalcular sus ventas sugeridas en serie.
- [ ] **Impresión térmica Bluetooth ESC/POS opcional**: Envío directo de reportes monocromos a impresoras portátiles de salón o depósito.
