# Arquitectura del Sistema

Este documento describe la organización interna de la aplicación, la estructura de archivos aplanada en `com.example`, las optimizaciones de bajo nivel inspiradas en motores WebAssembly y el flujo de datos entre la interfaz de usuario, los modelos de negocio y el almacenamiento local.

## Estructura Plana en `com.example`

La aplicación sigue el patrón **MVVM (Model - View - ViewModel)** con flujo de datos unidireccional (`StateFlow`). Todos los archivos Kotlin residen directamente en `app/src/main/java/com/example/` bajo el paquete único `package com.example`:

- **Navegación Raíz y Tema Visual**:
  - `MainActivity.kt`: Contenedor principal con encabezado editable del comercio y barra de navegación continua de 4 pestañas (`Catálogo`, `Solicitud`, `Distribuidor`, `Historial`), adaptable como `NavigationRail` lateral en pantallas anchas (`>= 600.dp`) o barra inferior plana en teléfonos.
  - `FastComponents.kt`: Primitivas visuales planas sin bordes ni sombras (`FastButton`, `FastOutlinedButton`, `FastChip`, `FastTextField`, `FastNavIconItem`).
  - `Color.kt`, `Theme.kt`, `Type.kt`: Paleta de colores sólidos, sistema de 5 colores para los tipos de solicitud (`Tipo1Verde`, `Tipo2Azul`, `Tipo3Naranja`, `Tipo4Teal`, `Tipo5Rojo`), contraste entero por desplazamiento de bits (`computeFastLumaScaled` y `getContrastingTextColor`) y configuración tipográfica general en Times New Roman (`R.font.tinos`).
- **Módulo de Catálogo**:
  - `CatalogScreen.kt`, `CatalogViewModel.kt`, `ProductDialog.kt`, `WhatsAppExportDialog.kt` y `BinaryImportModeDialog.kt`.
- **Módulo de Solicitud e Historial**:
  - `RequestScreen.kt`, `RequestViewModel.kt`, `RequestHistoryScreen.kt` y `Crop169Dialog.kt`.
- **Módulo de Boleta de Distribuidor**:
  - `DistributorScreen.kt` y `DistributorViewModel.kt`: Gestión de boletas por proveedor con ordenamiento explícito por número de fila (`ordenFila`), almacenamiento y filtrado de los 5 tipos de solicitud, edición rápida en línea, selección con doble toque para renderizado monocromo hacia WhatsApp, importación de plantillas desde el Catálogo y sincronización de precios hacia el Catálogo general.
- **Modelos de Dominio y Persistencia Binaria**:
  - `Product.kt`, `PriceRequest.kt`, `DistributorInvoice.kt` (`DistributorInvoice` y `DistributorItem`), `VarietyStockItem.kt` y `FastBinaryStore.kt`.
- **Utilidades de Cálculo y Renderizado**:
  - `MathUtils.kt`: Aritmética entera de márgenes `HALF_UP`, formateador entero de Guaraníes sobre buffer fijo sin `DecimalFormat`, tabla de búsqueda `O(1)` (`LATIN1_LUT`) con preservación de `ñ` y tokenizador único de búsquedas.
  - `ImageUtils.kt`: Decodificación submuestreada de fotografías, caché volátil de `Typeface` Times New Roman (`R.font.tinos`) y dibujo en `android.graphics.Canvas` para infografías `16:9` y tablas monocromas de Catálogo, Boleta de Distribuidor e Historial.

## Optimizaciones de Bajo Nivel Estilo WebAssembly

1. **Buffers Lineales Reutilizables y Empaquetado de Bits (`FastBinaryStore`)**:
   - La codificación y decodificación de cadenas UTF-8 en `writeFastString` y `readFastString` opera directamente sobre un buffer lineal reutilizable (`ThreadLocal<ByteArray>` con crecimiento geométrico), evitando crear miles de instancias `ByteArray` por lectura o escritura.
   - Los esquemas binarios actuales (`STP3` para productos, `STR4` para solicitudes y `FBD3` para boletas de distribuidor) agrupan las banderas de presencia de todos los campos opcionales en una única máscara de bits (`flags`) al inicio de cada registro, reduciendo el tamaño en disco y las llamadas de E/S mientras conservan lectura retrocompatible con `STP2`/`STP1`, `STR3`/`STR2`/`STR1` y `FBD2`/`FBD1`.

2. **Tabla de Búsqueda `O(1)` y Formateo Entero sin Asignaciones (`MathUtils`)**:
   - `LATIN1_LUT` resuelve la conversión a minúsculas y eliminación de tildes (preservando la `ñ`) mediante indexación directa de un arreglo `CharArray(256)` en tiempo constante `O(1)`. Si la cadena de entrada ya está normalizada, devuelve la misma referencia `String` sin asignar memoria.
   - `formatearGuaranies` divide el monto `Long` entre `10` en un bucle entero y escribe los dígitos de derecha a izquierda sobre un `CharArray(28)` preasignado en `ThreadLocal`, insertando puntos cada tres cifras sin usar `DecimalFormat`.

3. **Búsqueda Pretokenizada y Claves de Ordenamiento Precalculadas (`sortKey`)**:
   - `CatalogViewModel` y `DistributorViewModel` normalizan y separan los tokens de búsqueda una sola vez por cambio de consulta (`MathUtils.prepareQueryTokens`), evaluando cada producto o fila con `matchesTokens` sin ejecutar `split` ni `replace` dentro del bucle.
   - `Product`, `DistributorItem` y `DistributorInvoice` precalculan `sortKey`, permitiendo que `DistributorItem.ROW_COMPARATOR` ordene filas en `O(N log N)` sin recalcular normalizaciones de texto.
   - Las cadenas de presentación y listas de viñetas en `DistributorItem` y `PriceRequest` se evalúan bajo demanda o con `lazy(LazyThreadSafetyMode.NONE)`, evitando construir cadenas innecesarias al cargar o copiar colecciones en segundo plano.

4. **Contraste por Bits y Caché de `Typeface` (`Color.kt` e `ImageUtils.kt`)**:
   - `computeFastLumaScaled` extrae los canales `R`, `G` y `B` mediante desplazamientos de bits (`ushr 16`, `ushr 8`, `and 0xFF`) y calcula la luminancia entera `(R * 299 + G * 587 + B * 114)` sin conversiones de espacio de color en coma flotante.
   - `ImageUtils` conserva en memoria la instancia de `Typeface` en negrita de `R.font.tinos`, evitando lecturas repetidas de recursos al generar múltiples imágenes para WhatsApp.
