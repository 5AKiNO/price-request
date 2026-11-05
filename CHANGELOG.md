# Historial de Cambios

Todos los cambios relevantes de este proyecto se documentan en este archivo. El formato está basado en Keep a Changelog.

## [1.5.0] - 2026-11-05

### Agregado

- Tabla de búsqueda estática `O(1)` (`LATIN1_LUT` de 256 entradas) en `MathUtils.normalizarConEnie` con retorno directo de referencia cuando la cadena no requiere transformación.
- Formateador monetario entero de Guaraníes (`MathUtils.formatearGuaranies`) sobre buffer fijo `CharArray(28)` en `ThreadLocal`, eliminando el uso de `DecimalFormat` y las asignaciones de objetos por llamada.
- Pre-tokenizador de consultas (`MathUtils.prepareQueryTokens` y `matchesTokens`) para evaluar búsquedas en Catálogo y Boleta de Distribuidor sin ejecutar `split` ni `replace` por cada ítem.
- Empaquetado de nulidad por máscara de bits (`bitmask`) y codificación UTF-8 sobre buffer lineal reutilizable en `FastBinaryStore` (`STP3`, `STR4`, `FBD3`), manteniendo lectura 100% retrocompatible con todos los archivos `.bin` anteriores (`STP2`, `STP1`, `STR3`, `STR2`, `STR1`, `FBD2`, `FBD1`).
- Cálculo entero de luminancia por desplazamiento de bits (`computeFastLumaScaled`) en `Color.kt` e `ImageUtils.kt` y caché volátil de `Typeface` para `R.font.tinos`.

### Modificado

- Evaluación diferida (`lazy(LazyThreadSafetyMode.NONE)`) y propiedades bajo demanda en `Product`, `DistributorItem` y `PriceRequest`, junto con clave `sortKey` precalculada y comparador compartido `DistributorItem.ROW_COMPARATOR`.
- Consolidación de flujos `StateFlow` en `CatalogViewModel` y `DistributorViewModel` sobre `Dispatchers.Default`, eliminando cadenas de `.combine()` anidadas y clases de tuplas intermedias.

## [1.4.0] - 2026-11-05

### Agregado

- Cuarta sección principal **Boleta de Distribuidor** (`DistributorScreen`, `DistributorViewModel`, `DistributorInvoice`, `DistributorItem`) en la barra de navegación continua para gestionar productos exclusivos de un proveedor activo ordenados por número de fila (`ordenFila`).
- Soporte completo en `DistributorItem` y `FastBinaryStore` (`FBD2`) para almacenar, filtrar, editar y renderizar los **5 tipos de solicitud** (`Falta Registrar`, `Costo Subió`, `Costo Bajó`, `Excede % deseado`, `Inferior al % deseado`) con todos sus campos de costo anterior/nuevo, venta anterior/nueva, margen, stock, recarga y variedades.
- Botón directo **Guardar en Boleta del Distribuidor** en `RequestScreen` que permite cargar cualquiera de los 5 tipos de solicitud directamente en la fila indicada del distribuidor sin abrir WhatsApp, incrementando automáticamente el número de fila al siguiente disponible.
- Tarjetas con la paleta de 5 colores por tipo de solicitud y chips de filtro por tipo (`1` a `5`) dentro de `DistributorScreen`.
- Selección de filas específicas mediante **doble toque** en la Boleta de Distribuidor para generar y enviar por WhatsApp una imagen monocroma de tabla (`generateMonochromeDistributorBitmap`) que incluye el título del tipo de solicitud y el desglose completo de precios, stock y variedades por fila.
- Edición rápida en línea dentro de la boleta, importación de plantillas de productos habituales desde el Catálogo y sincronización de precios en un toque hacia el Catálogo general.

### Modificado

- Reorganización y aplanado del código fuente Kotlin directamente en `app/src/main/java/com/example/` bajo `package com.example`, eliminando subdirectorios anidados innecesarios y manteniendo intacta la compatibilidad con Gradle y Android.
- Esquema binario de solicitudes actualizado a `STR3` para conservar el vínculo opcional con distribuidor y número de fila de boleta, manteniendo retrocompatibilidad con `FBR2` y `FBR1`.

## [1.3.0] - 2026-11-04

### Agregado

- Tipografía general Times New Roman (`Tinos` vía `R.font.tinos`) aplicada tanto en la interfaz de Jetpack Compose como en el motor de dibujo de bitmaps (`ImageUtils`).
- Re-edición directa de solicitudes desde la pestaña de Historial, precargando todos los campos en el formulario de Solicitud y actualizando el registro existente en su misma posición.
- Botón `Guardar Cambios en Historial` durante el modo de edición para actualizar un registro sin necesidad de reenviar la imagen por WhatsApp.
- Flujo de integración continua en GitHub Actions (`.github/workflows/android-ci.yml`) para compilación Debug, Lint y pruebas unitarias.
- Documentación técnica modular en la carpeta `docs/` y plantillas de colaboración del repositorio.

### Modificado

- Rediseño visual con superficies planas de color sólido sin bordes ni líneas divisorias, organizadas sobre una grilla uniforme de `8.dp`.
- Barra de navegación continua de iconos sin contenedores encajonados ni bordes.
- Las solicitudes guardadas en el Historial ya no almacenan rutas ni archivos de imagen, reduciendo el consumo de almacenamiento interno y memoria RAM.
- Optimización de colores sólidos precalculados y eliminación de llamadas de transparencia dinámica en tiempo de recomposición.

### Eliminado

- Dependencias no utilizadas de bases de datos SQLite y librerías de carga de imágenes externas en `app/build.gradle.kts`, manteniendo únicamente el motor binario nativo `FastBinaryStore`.
