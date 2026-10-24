# Sistema Móvil de Precios e Inventario

Aplicación nativa Android desarrollada en **Kotlin** y **Jetpack Compose (Material Design 3)** para la administración ágil de catálogos de productos, cálculo automatizado de márgenes comerciales, personalización del nombre del local o sitio de trabajo y generación de fichas y reportes exportables tanto en **Texto Plano (TXT)** como en **Imagen Render Monocroma** (blanco y negro) optimizados para compartir por WhatsApp.

Diseñada con una arquitectura de baja latencia y consumo reducido de memoria RAM, permite operar con fluidez (0 ms de bloqueo en interfaz) incluso en dispositivos móviles de gama de entrada (1 GB a 3 GB de RAM) y totalmente fuera de línea (offline-first).

---

## Tabla de Contenidos

1. [Características Principales](#características-principales)
2. [Arquitectura del Sistema y Almacenamiento Binario](#arquitectura-del-sistema-y-almacenamiento-binario)
3. [Personalización del Sitio o Local de Trabajo](#personalización-del-sitio-o-local-de-trabajo)
4. [Reglas Matemáticas y Financieras](#reglas-matemáticas-y-financieras)
5. [Flujos de Solicitud de Cambio de Precios (5 Tipos)](#flujos-de-solicitud-de-cambio-de-precios-5-tipos)
6. [Exportación en Texto Plano (TXT) e Imagen Render Monocroma](#exportación-en-texto-plano-txt-e-imagen-render-monocroma)
7. [Desglose Detallado de Componentes](#desglose-detallado-de-componentes)
8. [Sistema Visual y Contraste Adaptativo](#sistema-visual-y-contraste-adaptativo)
9. [Requisitos e Instalación](#requisitos-e-instalación)

---

## Características Principales

- **Personalización del Local de Trabajo**: Configuración libre y persistente del nombre del comercio o sitio de trabajo desde el menú de opciones del catálogo o desde la barra superior, actualizando instantáneamente el encabezado principal y todos los reportes exportados.
- **Catálogo Reactivo de Alta Velocidad**: Búsqueda instantánea por código, descripción o precios con soporte completo para caracteres en español (`ñ`, `Ñ`, vocales acentuadas) y filtros rápidos por estado o porcentaje de margen.
- **Cálculo Automático de Márgenes y Redondeo Comercial**: Selección rápida de márgenes predefinidos (`20%`, `25%`, `30%`, `40%`, `50%`) con redondeo comercial automático a múltiplos de `500`.
- **Gestor de 5 Tipos de Solicitudes de Precio**: Formularios especializados para alta de productos, subidas/bajadas de costo y ajustes de precio de venta sobre costo único.
- **Exportación Dual (Texto Plano TXT o Imagen Render Monocroma)**:
  - **Catálogo de Productos**: Permite enviar un producto individual, una selección múltiple o todo el catálogo eligiendo mediante un botón entre **Texto Plano (TXT)** o **Imagen Render Monocroma** (fondo blanco `#FFFFFF`, texto negro `#111111` y líneas divisorias negras, sin necesidad de fotos de cámara ni galería).
  - **Historial de Solicitudes**: Permite exportar todo el historial de solicitudes o registros individuales eligiendo entre **Texto Plano (TXT)** o **Imagen Render Monocroma** con estructura idéntica a las consultas y solicitudes de cambio de precio.
- **Recorte Interactivo 16:9 Opcional**: Editor táctil en pantalla completa con pellizco de zoom, arrastre libre y cuadrícula de tercios cuando se desea adjuntar evidencia fotográfica en solicitudes individuales.
- **Contraste Dinámico Garantizado**: Algoritmo de luminancia que asigna automáticamente texto blanco (`#FFFFFF`) sobre fondos oscuros y texto negro (`#111111`) sobre fondos claros en todos los menús, tarjetas, botones y campos de texto.

---

## Arquitectura del Sistema y Almacenamiento Binario

El proyecto implementa el patrón **MVVM (Model-View-ViewModel)** con flujo de datos unidireccional (`StateFlow`) y un motor de persistencia híbrido enfocado en velocidad extrema:

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Punto de entrada, barra superior con nombre del local y navegación
├── data/
│   ├── model/
│   │   ├── Product.kt               # Modelo de producto e índice de búsqueda precomputado
│   │   └── PriceRequest.kt          # Modelo de solicitud de cambio de precio e historial
│   ├── local/
│   │   ├── FastBinaryStore.kt       # Motor de persistencia binaria directa (.bin) con escritura atómica
│   │   ├── AppDatabase.kt           # Definición de base de datos Room
│   │   ├── ProductDao.kt            # Consultas SQL de respaldo para catálogo
│   │   ├── PriceRequestDao.kt       # Consultas SQL de respaldo para historial
│   │   └── DatabaseSeeder.kt        # Inicializador de entorno limpio para producción
│   └── repository/
│       └── ProductRepository.kt     # Repositorio unificado de productos, solicitudes y nombre del local
├── ui/
│   ├── catalog/
│   │   ├── CatalogViewModel.kt      # Lógica de filtrado con debounce, selección, nombre del local y exportación
│   │   ├── CatalogScreen.kt         # Pantalla de catálogo, diálogo de nombre del local y acciones rápidas
│   │   ├── ProductDialog.kt         # Modal de creación/edición con guardado y envío directo
│   │   └── WhatsAppExportDialog.kt  # Modal de exportación de catálogo (Texto TXT o Imagen Monocroma)
│   ├── request/
│   │   ├── RequestViewModel.kt      # Máquina de estados y validaciones para los 5 tipos de solicitud
│   │   ├── RequestScreen.kt         # Formulario dinámico por tipo de solicitud y captura fotográfica opcional
│   │   ├── Crop169Dialog.kt         # Editor interactivo de recorte 16:9 sobre imagen completa
│   │   └── RequestHistoryScreen.kt  # Historial de solicitudes con exportación en TXT o Imagen Monocroma
│   └── theme/
│       ├── Color.kt                 # Paleta semántica y funciones de contraste por luminancia
│       ├── Theme.kt                 # Configuración Material 3 y colores adaptativos para campos de texto
│       └── Type.kt                  # Definición tipográfica de alta legibilidad
└── util/
    ├── MathUtils.kt                 # Fórmulas de margen, redondeo comercial y normalización UTF-8
    └── ImageUtils.kt                # Renderizado de infografías e imágenes monocromas de catálogo e historial
```

### Motor `FastBinaryStore` (`.bin`)
Para evitar bloqueos de interfaz en dispositivos con procesadores modestos o almacenamiento lento, `FastBinaryStore` mantiene el estado en memoria mediante `StateFlow` y serializa los registros directamente en flujos binarios (`DataOutputStream` / `DataInputStream`):
- **Escritura Atómica**: Cada cambio se escribe primero en un archivo temporal (`.tmp`) con cabecera mágica de validación (`MAGIC_PRODUCTS = 0x53545031`, `MAGIC_REQUESTS = 0x53545231`, `MAGIC_CONFIG = 0x53544331`) y luego reemplaza atómicamente al archivo `.bin` definitivo (`store_products_v1.bin`, `store_requests_v1.bin`, `store_config_v1.bin`).
- **Concurrencia Segura**: Utiliza `Mutex` independientes para catálogo, historial y configuración del local sobre `Dispatchers.IO`, garantizando lecturas instantáneas en el hilo principal.

---

## Personalización del Sitio o Local de Trabajo

El usuario puede personalizar a voluntad el nombre del local o sitio de trabajo en cualquier momento:
1. **Desde el Menú de Opciones del Catálogo**: Pulsando el botón de tres puntos (`Opciones`) junto a la barra de búsqueda y seleccionando **"Configurar nombre del local"**.
2. **Desde la Barra Superior (`TopAppBar`)**: Pulsando directamente sobre el nombre del local o el icono de edición en el encabezado de la aplicación.

El nombre configurado se guarda de forma persistente en `store_config_v1.bin` y reemplaza automáticamente el nombre del comercio en:
- El encabezado principal de la aplicación (`TopAppBar`).
- Los mensajes de exportación del catálogo en Texto Plano (TXT) y en Imagen Render Monocroma.
- Los reportes de exportación del historial de solicitudes en Texto Plano (TXT) y en Imagen Render Monocroma.
- Las fichas generadas en el gestor de solicitudes.

---

## Reglas Matemáticas y Financieras

Implementadas en `com.example.util.MathUtils`:

1. **Porcentaje de Ganancia Real Redondeado** (`calcularPorcentajeRedondeado`):
   $$\text{Porcentaje} = \text{round}\left(\frac{\text{PrecioVenta} - \text{PrecioCosto}}{\text{PrecioCosto}} \times 100\right)$$
   Devuelve un entero (`Int`) exacto para mostrar en insignias y fichas sin decimales confusos.

2. **Cálculo de Precio de Venta con Redondeo Comercial** (`calcularVentaDesdePorcentaje`):
   - Calcula el precio base aplicando el porcentaje seleccionado (`20%`, `25%`, `30%`, `40%` o `50%`):
     $$\text{VentaCruda} = \text{round}\left(\text{PrecioCosto} \times \left(1 + \frac{\text{Porcentaje}}{100}\right)\right)$$
   - Aplica redondeo comercial hacia arriba al múltiplo de **500** más cercano (`redondear Guaranies`):
     $$\text{Resto} = \text{VentaCruda} \bmod 500$$
     Si $\text{Resto} > 0$, se suma $(500 - \text{Resto})$ al valor crudo.

3. **Normalización Lingüística con Preservación de `ñ`** (`normalizarConEnie`):
   - Convierte el texto a minúsculas y elimina tildes diacríticas (`á -> a`, `é -> e`, `í -> i`, `ó -> o`, `ú -> u`, `ü -> u`), pero **conserva intacta la letra `ñ`**.

---

## Flujos de Solicitud de Cambio de Precios (5 Tipos)

El módulo de solicitudes (`RequestScreen.kt` + `RequestViewModel.kt`) adapta dinámicamente los campos de entrada, las validaciones de negocio y el orden de salida en la ficha según el tipo de solicitud seleccionado:

| Tipo | Nombre de Solicitud | Orden Exacto de Campos de Costo y Venta | Regla de Validación |
| :--- | :--- | :--- | :--- |
| **1** | **Falta Registrar** | 1. Precio de Costo<br>2. Precio de Venta (con botones `20%` a `50%`) | Costo > 0 y Venta > 0 |
| **2** | **Costo Subió** | 1. Precio de Costo Anterior<br>2. Precio de Costo Nuevo<br>3. Precio de Venta Anterior (con botones `20%` a `50%`)<br>4. Precio de Venta Nuevo (con botones `20%` a `50%`) | `Costo Nuevo > Costo Anterior` |
| **3** | **Costo Bajó** | 1. Precio de Costo Anterior<br>2. Precio de Costo Nuevo<br>3. Precio de Venta Anterior (con botones `20%` a `50%`)<br>4. Precio de Venta Nuevo (con botones `20%` a `50%`) | `Costo Nuevo < Costo Anterior` |
| **4** | **Supera % deseado** | 1. Precio de Costo (único)<br>2. Precio de Venta Anterior (con botones `20%` a `50%`)<br>3. Precio de Venta Nuevo (con botones `20%` a `50%`) | Un solo costo base; `Venta Nueva > Venta Anterior` |
| **5** | **Menor a % deseado** | 1. Precio de Costo (único)<br>2. Precio de Venta Anterior (con botones `20%` a `50%`)<br>3. Precio de Venta Nuevo (con botones `20%` a `50%`) | Un solo costo base; `Venta Nueva < Venta Anterior` |

En todos los tipos se permite agregar múltiples **variedades o presentaciones con el mismo costo**, las cuales se listan automáticamente con viñetas (`· `) bajo el encabezado `Nombre Producto:`.

---

## Exportación en Texto Plano (TXT) e Imagen Render Monocroma

Tanto en el **Catálogo de Productos** (`WhatsAppExportDialog.kt`) como en el **Historial de Solicitudes** (`RequestHistoryScreen.kt`), el usuario dispone de un selector de formato mediante botones para elegir cómo enviar la información:

1. **Texto Plano (TXT)**:
   - Genera un mensaje estructurado libre de emojis, encabezado por el nombre personalizado del local de trabajo y con separadores de texto limpio, listo para enviarse como mensaje de texto por WhatsApp.
2. **Imagen Render Monocroma (`generateMonochromeCatalogBitmap` / `generateMonochromeHistoryBitmap`)**:
   - No requiere fotos de cámara ni de galería.
   - Renderiza mediante `Canvas` y `StaticLayout` un documento visual de `1080px` de ancho estrictamente monocromo (**fondo blanco `#FFFFFF`, texto negro `#111111` y líneas divisorias negras `#111111`**).
   - Utiliza exactamente la misma disposición tipográfica que las solicitudes y consultas de cambios de precios (`Nombre Producto:`, lista con viñetas `· `, `Código`, `Precio de Costo Gs.` y `Precio de Venta Gs. (%)`).

---

## Desglose Detallado de Componentes

### 1. Capa de Datos (`com.example.data`)
- **`Product` (`Product.kt`)**: Entidad principal del catálogo. Almacena `id`, `codigo`, `codigoAlternativo`, `descripcion`, `precioCosto`, `precioVenta`, `porcentajePersonalizado`, `porcentajeRealRedondeado`, `estadoRegistro` (`COMPLETO`, `SIN_CODIGO`, `FALTA_REGISTRAR`) y una propiedad `@Ignore val searchIndex` precalculada al instanciar el objeto para filtrado $O(1)$ sin asignaciones de memoria repetitivas.
- **`PriceRequest` (`PriceRequest.kt`)**: Entidad de solicitudes generadas. Registra `tipoId` (`1..5`), `titulo`, URIs de imagen, códigos, descripción principal, `variedadesMismoCosto`, precios de costo/venta anteriores y nuevos, márgenes redondeados y datos opcionales de stock (`stockAnterior`, `recargaStock`).
- **`FastBinaryStore` (`FastBinaryStore.kt`)**: Almacén binario singleton en memoria y disco que expone `productsFlow`, `requestsFlow` y `workplaceNameFlow`.
- **`ProductRepository` (`ProductRepository.kt`)**: Abstracción de acceso a datos consumida por `CatalogViewModel` y `RequestViewModel`.

### 2. Módulo de Catálogo (`com.example.ui.catalog`)
- **`CatalogViewModel`**: Combina el flujo de productos, el nombre personalizado del local, la consulta de búsqueda con `debounce(90L)` ejecutada en `Dispatchers.Default`, el filtro activo y el conjunto de IDs seleccionados.
- **`CatalogScreen`**:
  - Barra de búsqueda reactiva y menú de opciones con acceso directo a **"Configurar nombre del local"** y **"Enviar todo el catálogo (TXT o Imagen)"**.
  - Fila horizontal de `FilterChip` (`Todos`, `Completos`, `Sin Código`, `Falta Registrar`, `Seleccionados`, `20%`, `25%`, `Personalizado`).
  - Carrusel de tarjetas de acceso rápido (`QuickProductCard`) y lista principal (`ProductListItem`) con acciones para compartir individualmente, crear solicitud, editar o eliminar.
- **`WorkplaceNameDialog`**: Diálogo modal para personalizar y guardar el nombre del sitio o local de trabajo.
- **`ProductDialog`**: Modal de registro y edición de productos con cálculo reactivo de precio de venta según botones de porcentaje (`20%` a `50%`) y botón directo **"Guardar y Compartir (TXT o Imagen)"**.
- **`WhatsAppExportDialog`**: Modal de exportación que permite elegir entre **Texto Plano (TXT)** o **Imagen Monocroma**, seleccionar qué campos incluir (`Código`, `Precio de Costo`, `Precio de Venta`, `% Margen`) y visualizar una vista previa antes de enviar.

### 3. Módulo de Solicitudes e Historial (`com.example.ui.request`)
- **`RequestScreen`**:
  - Selector horizontal para los 5 tipos de solicitud.
  - Captura fotográfica opcional mediante `UniversalTakePictureContract` o selector de galería `PickVisualMedia`.
  - Buscador autocompletable de productos registrados para los tipos 2, 3, 4 y 5.
  - Componente reutilizable `SelectablePercentageAndPriceBlock` que vincula cada campo de precio de venta con su grupo exclusivo de botones seleccionables de `20%` a `50%`.
- **`Crop169Dialog`**: Modal de pantalla completa que dibuja la fotografía completa sobre un `Canvas` y superpone un marco interactivo con relación de aspecto `16:9` bloqueada.
- **`RequestHistoryScreen` y `HistoryExportDialog`**: Lista cronológica de solicitudes con botón superior **"Exportar Historial"** y botón por tarjeta **"TXT / Monocromo"**, permitiendo enviar tanto el historial completo como solicitudes individuales en texto plano o en imagen render monocroma.

### 4. Procesamiento Gráfico (`com.example.util.ImageUtils`)
- **`decodeBitmapFromUri`**: Decodifica imágenes calculando `inSampleSize` para ajustarlas a un máximo de `1280px` en formato `Bitmap.Config.RGB_565` (50% menos consumo de RAM que `ARGB_8888`) y corrige la orientación EXIF.
- **`cropTo16By9`**: Extrae la subregión exacta seleccionada por el usuario manteniendo proporción `16:9`.
- **`generateInfographicBitmap`**: Renderiza en un `Canvas` de `1080px` de ancho la foto `16:9` opcional a sangre completa seguida del nombre del local, el título de la solicitud, la lista de variedades con viñetas y los precios en tipografía monocroma de alto contraste.
- **`generateMonochromeCatalogBitmap` y `generateMonochromeHistoryBitmap`**: Generadores de imágenes monocromas (`#FFFFFF` sobre `#111111`) para exportar el catálogo completo o el historial de solicitudes como imagen sin necesidad de fotografías.

---

## Sistema Visual y Contraste Adaptativo

Definido en `com.example.ui.theme.Color.kt` y `Theme.kt`:
- **Regla de Luminancia Automática (`getContrastingTextColor`)**:
  Evalúa `backgroundColor.luminance() < 0.45f`.
  - Si el fondo es **oscuro**, aplica texto e iconos en **blanco puro (`#FFFFFF`)**.
  - Si el fondo es **claro**, aplica texto e iconos en **negro profundo (`#111111`)**.
- **Campos de Texto Adaptativos (`adaptiveTextFieldColors`)**: Ajusta automáticamente el color del texto ingresado, etiquetas, placeholders, iconos y cursores de todos los `OutlinedTextField` según la luminancia de su contenedor.

---

## Requisitos e Instalación

### Requisitos Técnicos
- **Android Studio**: Ladybug (2024.2.1) o superior.
- **JDK**: Java 17.
- **SDK Mínimo (`minSdk`)**: API 24 (Android 7.0 Nougat) — compatible con dispositivos de gama baja.
- **SDK Objetivo (`targetSdk` / `compileSdk`)**: API 35 / 36.

### Compilación y Ejecución
1. Clonar el repositorio:
   ```bash
   git clone <url-del-repositorio>
   cd <nombre-del-directorio>
   ```
2. Compilar el APK de depuración mediante Gradle:
   ```bash
   gradle assembleDebug
   ```
3. Ejecutar las pruebas unitarias locales (Robolectric / JUnit):
   ```bash
   gradle :app:testDebugUnitTest
   ```
