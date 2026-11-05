# Gestión de Precios y Catálogo

Aplicación Android nativa en Kotlin y Jetpack Compose para comercios minoristas, depósitos y distribuidoras que operan en Guaraníes (Gs.). Permite administrar el catálogo de mercaderías, calcular márgenes sobre precio de venta con redondeo comercial a múltiplos de 50 Gs., gestionar boletas por distribuidor ordenadas por fila con soporte para los cinco tipos de solicitud, generar fichas en formato 16:9 y compartir reportes por WhatsApp o respaldo binario `.bin` sin conexión a internet.

## Propósito del Proyecto

En tiendas minoristas, despensas y comercios locales, la actualización de precios suele depender de cuadernos manuales o mensajes sueltos por WhatsApp cuando un proveedor cambia sus costos. Este flujo genera errores de cálculo en los márgenes, pérdida de historial y demoras al registrar mercadería nueva en caja.

Esta aplicación resuelve ese problema operativo ofreciendo una herramienta directa para el personal de salón, encargados de compras y propietarios:

- Consulta de precios de costo y venta en tienda sin depender de conexión de red.
- Estandarización del cálculo de márgenes comerciales (`20%`, `25%`, `30%`, `35%`, `50%`) sobre el precio de venta con redondeo automático a monedas de `50 Gs.`.
- Emisión de solicitudes de cambio de precio en cinco escenarios operativos, listas para guardarse directamente en la boleta de un distribuidor o enviarse por WhatsApp como infografía `16:9` (`1280x720 px`), texto plano (`TXT`) o tabla monocroma para impresión.
- Gestión de boletas por distribuidor con orden explícito de filas (`ordenFila`), tarjetas codificadas con los 5 colores por tipo de solicitud, selección rápida con doble toque para renderizar una tabla monocroma hacia WhatsApp y sincronización de precios en un toque hacia el Catálogo general.
- Sincronización entre teléfonos del personal mediante archivos binarios `.bin` transferibles por WhatsApp o cable USB.

## Recorrido de Pantallas

La aplicación organiza su operación en un encabezado editable para el nombre del comercio, cuatro vistas principales y una barra de navegación continua de iconos. Toda la tipografía del sistema utiliza Times New Roman (`Tinos`), el código fuente se encuentra aplanado directamente en `app/src/main/java/com/example/` y las vistas se estructuran mediante superficies planas de color sólido sobre una grilla de `8.dp`.

### Vista de Catálogo

La pantalla principal muestra el buscador por código o descripción, filtros por margen (`Todos`, `20%`, `25%`, `30%`, `35%`, `50%`), opciones de ordenamiento y el listado de productos. Cada elemento exhibe descripción, código, precio de costo, precio de venta con su porcentaje de margen y acciones rápidas para crear una solicitud de ajuste, editar o eliminar.

### Vista de Solicitud

Permite seleccionar uno de los cinco motivos de ajuste (`Falta Registrar`, `Costo Subió`, `Costo Bajó`, `Excede % deseado`, `Inferior al % deseado`), vincular opcionalmente la solicitud a un distribuidor específico con su número de fila en la boleta (`ordenFila`), adjuntar una fotografía con recorte interactivo `16:9`, declarar stock y recarga tanto para el producto principal como para sus variedades del mismo costo, y elegir entre dos acciones:

- **Guardar en Boleta del Distribuidor**: Almacena el ítem con cualquiera de los 5 tipos de solicitud y todos sus datos completos directamente en la boleta del distribuidor activo sin abrir WhatsApp, avanzando automáticamente al siguiente número de fila para continuar la carga.
- **Generar Imagen y Enviar a WhatsApp**: Renderiza la infografía `16:9`, guarda el registro en el Historial (y en la boleta del distribuidor si fue seleccionado) y abre el selector de WhatsApp.

### Vista de Boleta de Distribuidor

Permite seleccionar o crear un distribuidor activo y administrar su boleta ordenada por número de fila (`Fila #1`, `Fila #2`, etc.). En esta vista es posible almacenar y consultar los 5 tipos de solicitud con sus tarjetas de colores distintivos (`Verde`, `Azul`, `Naranja`, `Teal`, `Rojo`), filtrar las filas por tipo de solicitud (`Todos los tipos` o `1` a `5`), editar en línea cualquier fila con sus costos anteriores/nuevos, ventas anteriores/nuevas, stock, recarga y variedades, importar plantillas habituales desde el Catálogo, seleccionar filas específicas mediante **doble toque** para renderizar una tabla monocroma con el título del tipo de solicitud y el desglose completo hacia WhatsApp, y sincronizar los precios resultantes directamente al Catálogo general.

### Vista de Historial

Almacena localmente las solicitudes emitidas con sus datos de costo, venta, fecha, distribuidor, fila y variedades (sin guardar fotografías para conservar espacio en almacenamiento y memoria). Desde cada registro es posible reabrir la solicitud en el formulario principal para corregir sus datos y actualizarla en su misma posición, o bien exportar el historial en texto plano (`TXT`), imagen monocroma o respaldo `.bin`.

## Flujo de Trabajo en un Comercio

1. **Carga de boleta de proveedor con los 5 tipos de solicitud**: Al recibir la factura de un distribuidor, el operador selecciona el proveedor en la pestaña **Distribuidor** y pulsa **Cargar Solicitud (Fila #N)** (o abre la pestaña **Solicitud** eligiendo el distribuidor). Completa cualquiera de los 5 tipos de solicitud y pulsa **Guardar en Boleta del Distribuidor** para almacenar fila por fila sin salir de la aplicación.
2. **Revisión por colores, edición en línea y doble toque**: En la pestaña **Distribuidor**, cada fila se muestra con el color correspondiente a su tipo de solicitud (`1` a `5`) y puede filtrarse por motivo. Con **doble toque** sobre las tarjetas deseadas, el operador marca los artículos a reportar y genera en un toque una imagen monocroma de tabla con el título del motivo, stock, variedades y desglose de precios lista para enviar por WhatsApp.
3. **Sincronización con el Catálogo general**: Una vez verificados los costos y márgenes en la boleta del distribuidor, el operador pulsa **Sincronizar al Catálogo** para actualizar o dar de alta automáticamente todos los productos de la boleta en el Catálogo principal.
4. **Envío de ficha individual 16:9**: Cuando un artículo puntual requiere evidencia fotográfica, el usuario adjunta la foto con encuadre `16:9` en la pestaña **Solicitud** y pulsa **Generar Imagen y Enviar a WhatsApp**.
5. **Respaldo entre dispositivos**: Al cierre de turno, el administrador exporta los archivos `.bin` de Catálogo, Boletas de Distribuidor o Historial desde el menú superior y los comparte con los demás teléfonos del local.

## Funcionalidades Principales

- **Catálogo con búsqueda y filtros de margen**: Filtrado por subcadenas de código o descripción, filtro por márgenes comerciales (`20%` a `50%`) y ordenamiento alfabético o por precio.
- **Cinco modalidades de solicitud con validación de negocio**:
  - `Falta Registrar`: Alta de producto con costo y venta inicial.
  - `Costo Subió`: Exige que el costo nuevo sea mayor al costo anterior.
  - `Costo Bajó`: Exige que el costo nuevo sea menor al costo anterior.
  - `Excede % deseado`: Costo único; exige que la venta nueva sea menor a la venta anterior para corregir un sobreprecio.
  - `Inferior al % deseado`: Costo único; exige que la venta nueva sea mayor a la venta anterior para recomponer el margen.
- **Boleta de Distribuidor con soporte íntegro de los 5 tipos de solicitud**:
  - Almacenamiento ordenado por número de fila (`ordenFila`) editable y reordenable.
  - Tarjetas codificadas con la paleta de 5 colores según el tipo de solicitud y chips de filtrado rápido por tipo (`1` a `5`).
  - Selección rápida mediante **doble toque** para exportar a WhatsApp una tabla monocroma con el título del tipo de solicitud y el desglose completo de precios, stock y variedades por fila.
  - Importación de plantillas de productos habituales desde el Catálogo y sincronización directa de precios hacia el Catálogo general.
- **Gestión de variedades y stock por ítem**: Soporte para declarar múltiples sabores, medidas o presentaciones que comparten el mismo costo, cada una con su propio stock actual y cantidad de recarga.
- **Re-edición directa desde el Historial o desde la Boleta del Distribuidor**: Carga completa de cualquier solicitud o fila en el formulario principal para actualizar sus valores sin duplicar el registro.
- **Motor propio de exportación multimodal**:
  - Infografía `16:9` (`1280x720 px`) renderizada con `android.graphics.Canvas` y tipografía Times New Roman.
  - Imagen monocroma (blanco y negro) de catálogo, boleta de distribuidor o historial para lectura o impresión térmica.
  - Texto plano (`TXT`) formateado para envío directo por WhatsApp.
  - Archivos binarios `.bin` (`FastBinaryStore`) para copia de seguridad e intercambio entre dispositivos con modos de fusión o reemplazo total.

## Guía Rápida según tu Rol

### Propietario o Encargado de Tienda

- Define el nombre de tu local en el encabezado superior; ese nombre identificará todas las planillas, boletas e imágenes enviadas por WhatsApp.
- Revisa las boletas por distribuidor filtrando por cualquiera de los 5 tipos de solicitud y sincroniza los precios aprobados hacia el Catálogo general con un toque.
- Realiza copias de seguridad periódicas desde el menú de tres puntos (`Exportar respaldo binario .bin`) tanto en Catálogo como en Distribuidores e Historial.

### Operador de Salón o Depósito

- Para cargar una factura de un proveedor sin interrupciones, selecciona el distribuidor en la pestaña **Solicitud**, elige el tipo de solicitud (`1` a `5`), completa los datos y pulsa **Guardar en Boleta del Distribuidor**. El número de fila aumentará automáticamente para el siguiente ítem.
- En la pestaña **Distribuidor**, haz **doble toque** sobre las filas que quieras incluir en el reporte y pulsa **Imagen a WhatsApp** para enviar la tabla monocroma con todos los detalles de cada solicitud.
- Si te equivocaste en un monto después de crear una solicitud, puedes editarla tanto en línea dentro de la pestaña **Distribuidor** como desde la pestaña **Historial**.

### Desarrollador o Colaborador Técnico

- Revisa la arquitectura interna y el formato de persistencia en `docs/architecture.md` y `docs/data-models.md`.
- Todo el código fuente de la aplicación se aloja de forma plana y directa en `app/src/main/java/com/example/` bajo `package com.example`, utilizando los componentes planos definidos en `FastComponents.kt`, superficies sin bordes ni divisores sobre una grilla de `8.dp`, y la familia tipográfica Times New Roman (`R.font.tinos`).
- Antes de abrir un Pull Request, consulta `CONTRIBUTING.md` y verifica que las pruebas unitarias (`gradle :app:testDebugUnitTest`) y el chequeo de compilación (`gradle assembleDebug`) finalicen sin errores.

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
3. Ejecutar la suite de pruebas locales:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. El archivo instalable se generará en `app/build/outputs/apk/debug/app-debug.apk`.

## Resumen de Arquitectura

El proyecto aplica una arquitectura MVVM con estructura de paquete aplanada en `com.example` (`app/src/main/java/com/example/`) sin dependencias innecesarias en tiempo de ejecución:

- **Pantallas y Estado Reactivo (`MainActivity.kt`, `CatalogScreen.kt`, `RequestScreen.kt`, `DistributorScreen.kt`, `RequestHistoryScreen.kt`)**: Interfaces en Jetpack Compose conectadas a `CatalogViewModel`, `RequestViewModel` y `DistributorViewModel` mediante `StateFlow`, tipografía Times New Roman (`R.font.tinos`) y componentes planos sin bordes (`FastComponents.kt`).
- **Modelos y Persistencia Binaria (`Product.kt`, `PriceRequest.kt`, `DistributorInvoice.kt`, `VarietyStockItem.kt`, `FastBinaryStore.kt`)**: Modelos de dominio inmutables y motor de almacenamiento binario secuencial basado en `DataOutputStream` y `DataInputStream` con escritura atómica mediante archivos temporales `.tmp`.
- **Utilidades de Cálculo y Renderizado (`MathUtils.kt`, `ImageUtils.kt`)**: Aritmética entera de márgenes con redondeo comercial `HALF_UP` y generador de bitmaps `16:9` y tablas monocromas para WhatsApp.

## Mapa de Documentación

- [Arquitectura del Sistema](docs/architecture.md): Diseño interno en `com.example`, flujo de estado, componentes planos y decisiones de rendimiento.
- [Flujos Funcionales](docs/feature-flows.md): Recorridos paso a paso del catálogo, las cinco solicitudes, la boleta de distribuidor, el doble toque y la re-edición de historial.
- [Formatos de Exportación](docs/export-formats.md): Especificación de mensajes TXT, tablas monocromas de catálogo/distribuidor/historial, fichas 16:9 y archivos `.bin`.
- [Modelos de Datos y Esquema Binario](docs/data-models.md): Estructura de `Product`, `PriceRequest`, `DistributorInvoice`, `DistributorItem`, `VarietyStockItem` y layout de bytes en disco.
- [Preguntas Frecuentes](docs/faq.md): Respuestas sobre cómo almacenar los 5 tipos de solicitud en distribuidores, redondeo a 50 Gs., cálculo sobre venta y respaldos `.bin`.
- [Hoja de Ruta](docs/roadmap.md): Hitos completados y mejoras planificadas para próximas versiones.
- [Guía de Contribución](CONTRIBUTING.md): Reglas de estilo, estructura plana en `com.example`, pruebas y flujo de ramas.
- [Historial de Cambios](CHANGELOG.md): Registro cronológico de versiones.
- [Política de Seguridad](SECURITY.md): Reporte de vulnerabilidades e integridad de archivos `.bin`.

## Licencia

Este proyecto se distribuye bajo una **Licencia de Código Disponible para Uso No Comercial**. Queda prohibido a terceros utilizar este repositorio, su código fuente o sus binarios compilados con fines comerciales o de lucro. Consulta el archivo [LICENSE](LICENSE) para leer los términos completos.
