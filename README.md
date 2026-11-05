# Sistema de Precios

Aplicación Android nativa en Kotlin y Jetpack Compose para comercios minoristas, depósitos y distribuidoras que operan en Guaraníes (`Gs.`). Permite administrar el catálogo de mercaderías, calcular porcentajes de ganancia con redondeo `HALF_UP` y redondeo comercial a múltiplos de `500 Gs.`, gestionar boletas por distribuidor ordenadas por fila con almacenamiento de los cinco tipos de solicitud, generar fichas e imágenes monocromas `RGB_565` y compartir reportes por WhatsApp o respaldo binario `.bin` sin conexión a internet.

## Propósito del Proyecto

En tiendas minoristas, despensas y comercios locales, la actualización de precios suele depender de cuadernos manuales o mensajes sueltos por WhatsApp cuando un proveedor modifica sus costos. Este flujo genera errores de cálculo en los márgenes, pérdida de historial y demoras al registrar mercadería nueva en caja.

**Sistema de Precios** resuelve ese problema operativo ofreciendo una herramienta de bajo consumo de memoria para el personal de salón, encargados de compras y propietarios:

- Consulta inmediata de precios de costo y venta en tienda mediante búsqueda pretokenizada con preservación estricta de la letra `ñ` e insensibilidad a tildes.
- Escala de porcentajes comerciales estandarizada (`20%`, `25%`, `30%`, `35%`, `50%`) con cálculo entero (`HALF_UP`) y redondeo opcional a múltiplos de `500 Gs.`.
- Emisión de solicitudes de cambio de precio en cinco modalidades operativas (`Falta Registrar`, `Costo Subió`, `Costo Bajó`, `Excede % deseado`, `Inferior al % deseado`), listas para guardarse en la boleta de un distribuidor o enviarse por WhatsApp como infografía con foto `16:9`, imagen render monocroma (`RGB_565`) o texto plano (`TXT`).
- Gestión de boletas por distribuidor con orden explícito de filas (`ordenFila`), tarjetas codificadas con los 5 colores por tipo de solicitud, selección rápida con doble toque para renderizar una tabla monocroma hacia WhatsApp y sincronización de precios en un toque hacia el Catálogo general.
- Respaldo e intercambio entre dispositivos mediante archivos binarios `.bin` (`FastBinaryStore` con cabeceras `STP3`, `STR4`, `FBD3` y `STC2`) con modos **Fusionar** y **Reemplazar todo**.

## Recorrido de Pantallas

La aplicación organiza su operación en un encabezado editable para el nombre del local comercial, cuatro vistas principales y una barra de navegación continua de iconos (`FastNavIconItem`) sin etiquetas de texto para maximizar el área útil de trabajo. Toda la tipografía utiliza Times New Roman (`Tinos` vía `R.font.tinos`), el código fuente se encuentra aplanado directamente en `app/src/main/java/com/example/` y las vistas se estructuran mediante superficies planas de color sólido sobre una grilla de `8.dp`.

### Vista de Catálogo (`CatalogScreen`)

Muestra el buscador multicriterio por tokens (código, descripción, precio o porcentaje), filtros rápidos (`Todos`, `Completos`, `Sin Código`, `Falta Registrar`, `Seleccionados`, `20%`, `25%`, `30%`, `35%`, `50%`, `Personalizado`) y el listado de productos ordenado alfabéticamente por `sortKey`. Cada tarjeta exhibe descripción, código, precio de costo, precio de venta con su porcentaje real redondeado y acciones rápidas para crear una solicitud de ajuste, editar o eliminar.

### Vista de Solicitud (`RequestScreen`)

Permite seleccionar uno de los cinco tipos de solicitud (`1: Falta Registrar`, `2: Costo Subió`, `3: Costo Bajó`, `4: Excede % deseado`, `5: Inferior al % deseado`), vincular opcionalmente el registro a un distribuidor específico con su número de fila en boleta (`ordenFila`), adjuntar una fotografía con recorte interactivo `16:9`, declarar stock y recarga tanto para el producto principal como para sus variedades del mismo costo (`VarietyStockItem`), y elegir entre dos acciones:

- **Guardar en Boleta del Distribuidor**: Almacena el ítem con cualquiera de los 5 tipos de solicitud y todos sus datos completos directamente en la boleta del distribuidor activo sin abrir WhatsApp, avanzando automáticamente al siguiente número de fila disponible.
- **Generar Imagen y Enviar a WhatsApp**: Renderiza la ficha en `RGB_565`, guarda el registro en el Historial (y en la boleta del distribuidor si fue seleccionado) y abre WhatsApp.

### Vista de Boleta de Distribuidor (`DistributorScreen`)

Permite seleccionar o crear un distribuidor activo y administrar su boleta ordenada por número de fila (`Fila 1`, `Fila 2`, etc.). En esta vista es posible almacenar y consultar los 5 tipos de solicitud con sus tarjetas de colores distintivos (`Tipo1Verde`, `Tipo2Azul`, `Tipo3Naranja`, `Tipo4Teal`, `Tipo5Rojo`), filtrar las filas por tipo de solicitud (`Todos los tipos` o `1` a `5`), editar en línea cualquier fila, importar plantillas habituales desde el Catálogo, seleccionar filas específicas mediante **doble toque** para renderizar una tabla monocroma con el título del tipo de solicitud y el desglose completo hacia WhatsApp, y sincronizar los precios resultantes directamente al Catálogo general.

### Vista de Historial (`RequestHistoryScreen`)

Almacena localmente las solicitudes emitidas con sus datos de costo, venta, fecha, distribuidor, fila y variedades (sin persistir fotografías para ahorrar memoria RAM y almacenamiento).Cada tarjeta incluye el botón **Editar**, que restaura todos los campos y variedades en `RequestScreen` y habilita las acciones **Guardar Cambios en Historial** (sin abrir WhatsApp) y **Actualizar y Enviar a WhatsApp**.

## Funcionalidades Principales

- **Buscador multicriterio pretokenizado**: Indexación precalculada en `Product.searchIndex` y `DistributorItem.searchIndex` con tabla de búsqueda `O(1)` (`LATIN1_LUT`) que preserva la letra `ñ` e ignora tildes.
- **Cinco modalidades de solicitud con validación de negocio**:
  - `Tipo 1 (Falta Registrar)`: Alta de producto con costo y venta inicial.
  - `Tipo 2 (Costo Subió)`: Exige que el costo nuevo sea mayor al costo anterior (`Costo Nuevo > Costo Anterior`).
  - `Tipo 3 (Costo Bajó)`: Exige que el costo nuevo sea menor al costo anterior (`Costo Nuevo < Costo Anterior`).
  - `Tipo 4 (Excede % deseado)`: Costo único; exige que el precio de venta nuevo sea menor al precio de venta anterior (`Venta Nueva < Venta Anterior`).
  - `Tipo 5 (Inferior al % deseado)`: Costo único; exige que el precio de venta nuevo sea mayor al precio de venta anterior (`Venta Nueva > Venta Anterior`).
- **Boleta de Distribuidor con soporte íntegro de los 5 tipos de solicitud**:
  - Almacenamiento ordenado por número de fila (`ordenFila`) editable y reordenable.
  - Tarjetas codificadas con la paleta de 5 colores según el tipo de solicitud y chips de filtrado rápido por tipo (`1` a `5`).
  - Selección rápida mediante **doble toque** para exportar a WhatsApp una tabla monocroma (`RGB_565`) con el título del tipo de solicitud y el desglose completo de precios, stock y variedades por fila.
  - Importación de plantillas de productos habituales desde el Catálogo y sincronización directa de precios hacia el Catálogo general.
- **Motor binario `FastBinaryStore` optimizado**:
  - Escritura atómica con archivo temporal `.tmp`, `fsync` y colas `Channel.CONFLATED`.
  - Buffer lineal reutilizable (`ThreadLocal<ByteArray>`) para codificación UTF-8 sin asignaciones por campo y empaquetado de nulidad por máscara de bits (`STP3`, `STR4`, `FBD3`), manteniendo compatibilidad de lectura con `STP2`/`STP1`, `STR3`/`STR2`/`STR1`, `FBD2`/`FBD1` y `STC2`/`STC1`.

## Instalación y Compilación

### Requisitos Previos

- Android Studio Ladybug (2024.2.1) o superior.
- JDK 17 configurado en Gradle.
- Android SDK con `compileSdk 36` y `minSdk 24` (Android 7.0+).

### Pasos para Ejecutar el Proyecto

1. Clonar el repositorio:
   ```bash
   git clone <url-del-repositorio>
   cd <nombre-del-repositorio>
   ```
2. Compilar el paquete de depuración:
   ```bash
   gradle assembleDebug
   ```
3. Ejecutar la suite de pruebas unitarias:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. El archivo instalable se generará en `app/build/outputs/apk/debug/app-debug.apk`.

## Mapa de Documentación

- [Arquitectura del Sistema](docs/architecture.md): Estructura plana en `com.example`, primitivas `FastComponents`, motor `FastBinaryStore` y optimizaciones de memoria lineal.
- [Flujos Funcionales](docs/feature-flows.md): Recorridos del catálogo, las cinco solicitudes, la boleta de distribuidor, el doble toque y la re-edición en historial.
- [Formatos de Exportación](docs/export-formats.md): Especificación de fichas con foto `16:9`, imágenes render monocromas (`RGB_565`), mensajes `TXT` y respaldos `.bin`.
- [Modelos de Datos y Esquema Binario](docs/data-models.md): Estructura de `Product`, `PriceRequest`, `DistributorInvoice`, `DistributorItem`, `VarietyStockItem` y cabeceras binarias `STP3`, `STR4`, `FBD3` y `STC2`.
- [Preguntas Frecuentes](docs/faq.md): Respuestas sobre el cálculo de porcentajes, redondeo a múltiplos de `500 Gs.`, boletas de distribuidor y modos de importación `.bin`.
- [Hoja de Ruta](docs/roadmap.md): Hitos completados (`v1.0.0` a `v2.6.0`) y mejoras planificadas.
- [Guía de Contribución](CONTRIBUTING.md): Reglas de arquitectura, estilo y pruebas.
- [Registro de Cambios](CHANGELOG.md): Historial cronológico de versiones bajo Versionado Semántico.
- [Política de Seguridad](SECURITY.md): Integridad de archivos `.bin` y reporte de vulnerabilidades.
