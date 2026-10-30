# Sistema Móvil de Precios e Inventario

Aplicación nativa Android desarrollada en **Kotlin** y **Jetpack Compose** para la administración ágil de catálogos de productos, cálculo automatizado de márgenes comerciales (`20%`, `25%`, `30%`, `35%`, `50%`), personalización del nombre del local o sitio de trabajo, respaldo binario directo (`.bin`), re-edición de registros del historial y generación de fichas y reportes exportables tanto en **Texto Plano (TXT)** como en **Imagen Render Monocroma** (blanco y negro) optimizados para compartir por WhatsApp.

Diseñada con una arquitectura de baja latencia, **componentes planos nativos (`FastComponents`)** sin subcomposición pesada, barra de navegación plana de solo iconos, historial libre de imágenes pesadas y depuración de librerías externas en desuso (sin Room, Retrofit, OkHttp, Moshi, Coil ni Firebase), permite operar con fluidez (0 ms de bloqueo en interfaz) incluso en dispositivos móviles de gama de entrada (1 GB a 3 GB de RAM) y totalmente fuera de línea (offline-first).

---

## Tabla de Contenidos

1. [Características Principales](#características-principales)
2. [Arquitectura del Sistema y Almacenamiento Binario](#arquitectura-del-sistema-y-almacenamiento-binario)
3. [Componentes Planos de Respuesta Inmediata (FastComponents)](#componentes-planos-de-respuesta-inmediata-fastcomponents)
4. [Importación y Exportación de Respaldos Binarios (.bin)](#importación-y-exportación-de-respaldos-binarios-bin)
5. [Motor de Búsqueda Multicriterio por Tokens](#motor-de-búsqueda-multicriterio-por-tokens)
6. [Personalización del Sitio o Local de Trabajo](#personalización-del-sitio-o-local-de-trabajo)
7. [Reglas Matemáticas y Financieras](#reglas-matemáticas-y-financieras)
8. [Flujos de Solicitud de Cambio de Precios (5 Tipos) y Re-edición de Historial](#flujos-de-solicitud-de-cambio-de-precios-5-tipos-y-re-edición-de-historial)
9. [Exportación en Texto Plano (TXT) e Imagen Render Monocroma](#exportación-en-texto-plano-txt-e-imagen-render-monocroma)
10. [Desglose Detallado de Componentes](#desglose-detallado-de-componentes)
11. [Sistema Visual, Responsividad y Contraste Adaptativo](#sistema-visual-responsividad-y-contraste-adaptativo)
12. [Requisitos e Instalación](#requisitos-e-instalación)

---

## Características Principales

- **Personalización del Local de Trabajo**: Configuración libre y persistente del nombre del comercio o sitio de trabajo desde el menú de opciones del catálogo o desde la barra superior, actualizando instantáneamente el encabezado principal y todos los reportes exportados.
- **Barra de Navegación Plana de Solo Iconos**: Los tres botones del menú principal (**Catálogo**, **Solicitud** e **Historial**) utilizan el componente plano `FastNavIconItem` mostrando únicamente sus iconos centrados sin etiquetas de texto (`"Solicitud 16:9"`, etc.), maximizando el espacio vertical de trabajo y eliminando animaciones innecesarias.
- **Re-edición Completa de Registros del Historial**: Cada solicitud registrada en el Historial incluye un botón **"Editar"** que carga todos sus datos (tipo `1..5`, código, descripción, porcentaje deseado, costos, precios de venta, stock, recarga y lista de variedades individuales) en la pantalla de Solicitud para **actualizar el registro existente en su lugar**, ya sea guardando directamente en el historial (`"Guardar Cambios en Historial"`) o actualizando y enviando por WhatsApp (`"Actualizar y Enviar a WhatsApp"`).
- **Historial Libre de Imágenes Pesadas**: El historial almacena exclusivamente datos estructurados de texto y valores numéricos (`PriceRequest`), sin guardar imágenes ni rutas de fotografías en el historial, ahorrando almacenamiento interno y memoria RAM.
- **Buscador Multicriterio Instantáneo en Catálogo**: Búsqueda reactiva en tiempo real por código principal, código alternativo, descripción (con preservación de `ñ`/`Ñ` e insensibilidad a tildes), precios de costo o venta (con o sin puntos separadores de miles, ej. `12500` o `12.500`), porcentaje de margen (`20%`, `25%`, `30%`, `35%`, `personalizado`) y estado de registro (`completo`, `sin codigo`, `falta registrar`).
- **Importación y Exportación Binaria Nativa (`.bin`)**: Respaldo y restauración directa en formato binario `V2` tanto para el **Catálogo de Productos** como para el **Historial de Solicitudes**, permitiendo elegir al importar entre **Fusionar** (actualizar registros existentes y agregar nuevos) o **Reemplazar todo**.
- **Componentes Planos Ultraligeros (`FastComponents`) y Diseño Responsivo**:
  - **Primitivas Nativas sin Sobrecarga**: Sustitución de `OutlinedTextField`, `FilterChip`, `Button`, `OutlinedButton`, `Checkbox`, `NavigationBar` y `NavigationRail` de Material 3 por primitivas planas de una sola capa (`FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton`, `FastCheckBox`, `FastNavIconItem`).
  - **Adaptabilidad Multi-Pantalla**: Barra inferior plana en pantallas compactas y barra lateral plana (`60.dp`) junto con grillas/paneles de dos columnas en pantallas anchas (`>= 600dp` / `>= 680dp`).
- **Cálculo Automático de Márgenes (`20%`, `25%`, `30%`, `35%`, `50%`) y Redondeo Comercial**: Selección rápida de márgenes predefinidos (`20%`, `25%`, `30%`, `35%`, `50%`) con redondeo comercial automático a múltiplos de `500`, además de filtros rápidos por margen (`20%`, `25%`, `30%`, `35%`, `Personalizado`) en el catálogo principal.
- **Exportación Dual para WhatsApp (Texto Plano TXT o Imagen Render Monocroma)**:
  - **Catálogo de Productos**: Permite enviar un producto individual, una selección múltiple o todo el catálogo eligiendo entre **Texto Plano (TXT)** o **Imagen Render Monocroma** (fondo blanco `#FFFFFF`, texto negro `#111111` y líneas divisorias negras).
  - **Historial de Solicitudes**: Permite exportar todo el historial de solicitudes o registros individuales eligiendo entre **Texto Plano (TXT)** o **Imagen Render Monocroma** generada al vuelo.
- **Contraste Dinámico Garantizado**: Algoritmo de luminancia que asigna automáticamente texto blanco (`#FFFFFF`) sobre fondos oscuros y texto negro (`#111111`) sobre fondos claros en todos los menús, contenedores, botones y campos de texto.

---

## Arquitectura del Sistema y Almacenamiento Binario

El proyecto implementa el patrón **MVVM (Model-View-ViewModel)** con flujo de datos unidireccional (`StateFlow`) y un motor de persistencia binario puro, habiendo eliminado por completo capas ORM y librerías pesadas no utilizadas (`Room`, `Retrofit`, `OkHttp`, `Moshi`, `Coil`, `Firebase`) para reducir el uso de RAM y acelerar el arranque:

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Punto de entrada, barra superior compacta y barra plana de solo iconos
├── data/
│   ├── model/
│   │   ├── Product.kt               # Modelo inmutable de producto, textos precalculados y motor matchesQuery
│   │   └── PriceRequest.kt          # Modelo inmutable de solicitud (sin imágenes en historial) y VarietyStockItem
│   ├── local/
│   │   └── FastBinaryStore.kt       # Motor de persistencia e importación/exportación binaria V2 (.bin)
│   └── repository/
│       └── ProductRepository.kt     # Repositorio unificado de productos, solicitudes, local y respaldos .bin
├── ui/
│   ├── components/
│   │   └── FastComponents.kt        # Primitivas planas (FastTextField, FastChip, FastButton, FastCheckBox, FastNavIconItem)
│   ├── catalog/
│   │   ├── CatalogViewModel.kt      # Filtrado multicriterio (incl. 20%, 25%, 30%, 35%), selección y respaldo .bin
│   │   ├── CatalogScreen.kt         # Pantalla responsiva de catálogo con filas planas de bajo consumo
│   │   ├── ProductDialog.kt         # Modal de creación/edición con porcentajes (20%, 25%, 30%, 35%, 50%)
│   │   └── WhatsAppExportDialog.kt  # Modal de exportación de catálogo (Texto TXT o Imagen Monocroma)
│   ├── request/
│   │   ├── RequestViewModel.kt      # Máquina de estados para los 5 tipos, re-edición de historial y respaldo .bin
│   │   ├── RequestScreen.kt         # Formulario responsivo de 1 o 2 columnas con modo de edición de historial
│   │   ├── Crop169Dialog.kt         # Editor interactivo de recorte 16:9 al adjuntar fotografía para envío inmediato
│   │   └── RequestHistoryScreen.kt  # Historial con botón Editar, exportación TXT/Monocroma y respaldo .bin
│   └── theme/
│       ├── Color.kt                 # Paleta semántica y funciones de contraste por luminancia
│       ├── Theme.kt                 # Configuración Material 3 y colores adaptativos
│       └── Type.kt                  # Definición tipográfica de alta legibilidad
└── util/
    ├── MathUtils.kt                 # Fórmulas de margen, redondeo comercial y normalización UTF-8
    └── ImageUtils.kt                # Renderizado de infografías e imágenes monocromas bajo demanda
```

### Motor `FastBinaryStore` V2 (`.bin`)
Para evitar bloqueos de interfaz en dispositivos con procesadores modestos o almacenamiento flash lento, `FastBinaryStore` mantiene el estado en memoria mediante `StateFlow` y serializa los registros en flujos binarios (`DataOutputStream` / `DataInputStream`) con un buffer de `16 KB`:
- **Cola Conflated + Debounce (`250 ms`)**: Tres canales `Channel<Unit>(Channel.CONFLATED)` independientes colapsan ráfagas de ediciones en una única escritura de disco del estado más reciente (`_productsFlow.value`, `_requestsFlow.value`, `_workplaceNameFlow.value`).
- **Durabilidad Física Real (`fsync` + Movimiento Atómico)**: Cada cambio se escribe en un archivo temporal (`.tmp`), ejecuta `FileOutputStream.fd.sync()` para volcar buffers al almacenamiento físico y reemplaza el archivo `.bin` mediante `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` (con fallback seguro a `renameTo` en API 24–25).
- **Actualización In-Situ por ID (`savePriceRequest`)**: Cuando se re-edita una solicitud existente del historial, `FastBinaryStore` localiza su `id` mediante `indexOfRequestId` y actualiza el registro en su posición sin crear duplicados.
- **Índice Hash $O(1)$ e Inserción Binaria**: Mantiene un `@Volatile Map<String, Product>` en paralelo para búsquedas instantáneas `getProductById` en $O(1)$ e inserta nuevos productos en orden alfabético mediante `binarySearchBy(product.sortKey)`.

---

## Componentes Planos de Respuesta Inmediata (`FastComponents`)

Definidos en `com.example.ui.components.FastComponents.kt`, reemplazan a los controles estándar de Material 3 para reducir la cantidad de nodos en el árbol de composición y eliminar animaciones de elevación o subcomposiciones costosas:

1. **`SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)`**:
   - Constante compartida en toda la aplicación para los botones seleccionables de porcentaje tanto en el alta/edición de productos como en los 5 tipos de solicitudes de precio.
2. **`FastTextField`**:
   - Construido directamente sobre `BasicTextField` con una etiqueta fija superior (`Text`) y un contenedor plano de una sola capa (`Row` con `.background(...)` + `.border(...)`).
   - Evita el `SubcomposeLayout` y las animaciones de etiqueta flotante de `OutlinedTextField`, logrando apertura instantánea del teclado y cero retardo al teclear en formularios con múltiples campos.
3. **`FastChip`**:
   - Reemplaza a `FilterChip` mediante un único `Row` plano con borde de `1.dp`/`1.5.dp` y contraste automático por luminancia (`getContrastingTextColor`).
4. **`FastButton` y `FastOutlinedButton`**:
   - Botones planos de una sola capa sin sombras de elevación ni `Surface` anidados.
5. **`FastCheckBox`**:
   - Casilla de selección ligera dibujada en un `Box` plano de `28.dp`, eliminando transiciones animadas por ítem durante el desplazamiento de listas extensas.
6. **`FastNavIconItem`**:
   - Botón plano de navegación de una sola capa que muestra únicamente el icono centrado sin texto, reemplazando a `NavigationBarItem` y `NavigationRailItem`.

---

## Importación y Exportación de Respaldos Binarios (`.bin`)

El sistema permite exportar e importar archivos binarios nativos (`.bin`) directamente desde el selector de documentos de Android (`ActivityResultContracts.CreateDocument` y `OpenDocument`):

1. **Respaldo de Catálogo (`exportCatalogBinary` / `importCatalogBinary`)**:
   - Accesible desde el menú de opciones (`⋮`) del Catálogo: **"Exportar respaldo binario (.bin)"** e **"Importar respaldo binario (.bin)"**.
   - Al seleccionar un archivo `.bin` para importar, se presenta el diálogo `BinaryImportModeDialog` con dos modos de operación:
     - **Fusionar (Actualizar existentes y agregar nuevos)**: Conserva los productos actuales, actualiza aquellos que coincidan por `codigo` (o `id`) y agrega los productos nuevos manteniendo el orden alfabético.
     - **Reemplazar todo**: Sustituye íntegramente el catálogo actual por el contenido del archivo binario importado.
2. **Respaldo de Historial de Solicitudes (`exportRequestsBinary` / `importRequestsBinary`)**:
   - Accesible desde el menú superior (`⋮`) de la pantalla de Historial: **"Exportar respaldo binario (.bin)"** e **"Importar respaldo binario (.bin)"**.
   - Permite igualmente **Fusionar** (combinando solicitudes por `id` en orden cronológico descendente) o **Reemplazar todo** el historial.

---

## Motor de Búsqueda Multicriterio por Tokens

El buscador del catálogo (`Product.searchIndex` + `Product.matchesQuery`) está diseñado para operar sin latencia de escritura en el campo de texto y con coincidencia flexible de múltiples términos:

1. **Índice Precalculado al Instanciar (`Product.searchIndex`)**:
   Cada instancia de `Product` construye una única vez su cadena indexada con:
   - `codigo` y `codigoAlternativo` normalizados (o etiquetas `"sin codigo sincodigo"` si carece de código).
   - `descripcion` normalizada mediante `MathUtils.normalizarConEnie` (minúsculas, sin tildes diacríticas y conservando `ñ`).
   - `precioCosto` y `precioVenta` tanto en valor numérico crudo (`12500`) como formateados con separadores de miles (`12.500`).
   - `porcentajeRealRedondeado` y `porcentajePersonalizado` con y sin símbolo `%` (`25%`, `25`, `personalizado`).
   - Estado de registro legible (`completo`, `sin codigo`, `falta registrar`, `pendiente`) y `notaRedondeo`.
2. **Evaluación por Tokens Independientes (`Product.matchesQuery`)**:
   - Divide la consulta escrita por el usuario en tokens separados por espacios y verifica que **todos los tokens** estén presentes en `searchIndex` sin importar el orden en que fueron escritos (por ejemplo, `"bandeja 12.500"`, `"100648 35%"` o `"caño sin codigo"`).
3. **Sincronía en Interfaz (`CatalogScreen`)**:
   - En `CatalogScreen`, el `FastTextField` observa directamente `viewModel.searchQuery` en el hilo principal (0 ms de retardo al teclear) mientras el filtrado de la lista se ejecuta en `Dispatchers.Default`.
   - La barra de filtros rápidos incluye: `Todos`, `Completos`, `Sin Código`, `Falta Registrar`, `Seleccionados`, `20%`, `25%`, `30%`, `35%` y `Personalizado`.

---

## Personalización del Sitio o Local de Trabajo

El usuario puede personalizar a voluntad el nombre del local o sitio de trabajo en cualquier momento:
1. **Desde el Menú de Opciones del Catálogo**: Pulsando el botón de tres puntos (`Opciones`) junto a la barra de búsqueda y seleccionando **"Configurar nombre del local"**.
2. **Desde la Barra Superior**: Pulsando directamente sobre el nombre del local o el icono de edición en el encabezado compacto de la aplicación.

El nombre configurado se guarda de forma persistente en `store_config_v1.bin` y reemplaza automáticamente el nombre del comercio en el encabezado y en todas las exportaciones de catálogo, solicitudes e historial.

---

## Reglas Matemáticas y Financieras

Implementadas en `com.example.util.MathUtils` utilizando aritmética entera de 64 bits (`Long`) sin asignaciones de punto flotante:

1. **Porcentaje de Ganancia Real Redondeado** (`calcularPorcentajeRedondeado`):
   $$\text{Porcentaje} = \text{round}\left(\frac{\text{PrecioVenta} - \text{PrecioCosto}}{\text{PrecioCosto}} \times 100\right)$$
2. **Cálculo de Precio de Venta con Redondeo Comercial** (`calcularVentaDesdePorcentaje`):
   - Calcula el precio base aplicando el porcentaje seleccionado (`20%`, `25%`, `30%`, `35%` o `50%`) y aplica redondeo comercial hacia arriba al múltiplo de **500** más cercano (`redondearGuaranies`).
3. **Normalización Lingüística con Preservación de `ñ`** (`normalizarConEnie`):
   - Convierte a minúsculas y elimina tildes diacríticas (`á -> a`, `é -> e`, `í -> i`, `ó -> o`, `ú -> u`, `ü -> u`), conservando intacta la letra `ñ`.

---

## Flujos de Solicitud de Cambio de Precios (5 Tipos) y Re-edición de Historial

El módulo de solicitudes (`RequestScreen.kt` + `RequestViewModel.kt`) opera de manera desacoplada del flujo del catálogo (sin buscadores de productos internos para reducir el uso de memoria y CPU), permitiendo cargar datos desde un ítem del catálogo, re-editar una solicitud existente desde el Historial o ingresarlos manualmente:

| Tipo | Nombre y Título de Solicitud | Orden Exacto de Campos de Costo y Venta | Regla de Stock y Validación |
| :--- | :--- | :--- | :--- |
| **1** | **Falta Registrar**<br>`"Este Producto Falta Registrar"` | 1. Precio de Costo<br>2. Precio de Venta (con botones `20%`, `25%`, `30%`, `35%`, `50%`) | Sin `Stock Anterior` (producto nuevo); admite `Recarga` individual en nombre principal y variedades. Costo > 0 y Venta > 0 |
| **2** | **Costo Subió**<br>`"El costo de este producto subió"` | 1. Precio de Costo Anterior<br>2. Precio de Costo Nuevo<br>3. Precio de Venta Anterior (con botones `20%`, `25%`, `30%`, `35%`, `50%`)<br>4. Precio de Venta Nuevo (con botones `20%`, `25%`, `30%`, `35%`, `50%`) | `Stock` y `Recarga` opcionales por nombre principal y por cada variedad. `Costo Nuevo > Costo Anterior` |
| **3** | **Costo Bajó**<br>`"El costo de este producto bajó"` | 1. Precio de Costo Anterior<br>2. Precio de Costo Nuevo<br>3. Precio de Venta Anterior (con botones `20%`, `25%`, `30%`, `35%`, `50%`)<br>4. Precio de Venta Nuevo (con botones `20%`, `25%`, `30%`, `35%`, `50%`) | `Stock` y `Recarga` opcionales por nombre principal y por cada variedad. `Costo Nuevo < Costo Anterior` |
| **4** | **Excede % deseado**<br>`"El precio de venta excede el (porcentaje deseado)%"` | Selector de `% deseado` (`20%`, `25%`, `30%`, `35%`, `50%`)<br>1. Precio de Costo (único)<br>2. Precio de Venta Anterior (con botones `20%`, `25%`, `30%`, `35%`, `50%`)<br>3. Precio de Venta Nuevo (con botones `20%`, `25%`, `30%`, `35%`, `50%`) | Un solo costo base; `Stock` y `Recarga` opcionales por nombre principal y por cada variedad. `Venta Nueva < Venta Anterior` |
| **5** | **Inferior al % deseado**<br>`"El precio de venta es inferior al (porcentaje deseado)%"` | Selector de `% deseado` (`20%`, `25%`, `30%`, `35%`, `50%`)<br>1. Precio de Costo (único)<br>2. Precio de Venta Anterior (con botones `20%`, `25%`, `30%`, `35%`, `50%`)<br>3. Precio de Venta Nuevo (con botones `20%`, `25%`, `30%`, `35%`, `50%`) | Un solo costo base; `Stock` y `Recarga` opcionales por nombre principal y por cada variedad. `Venta Nueva > Venta Anterior` |

### Re-edición de Solicitudes desde el Historial (`loadRequestForEditing`)
- Al pulsar **"Editar"** en cualquier tarjeta del Historial, `RequestViewModel.loadRequestForEditing` reconstruye el estado completo del formulario incluyendo el desglose individual de variedades (`VarietyStockItem.parseFromFormattedLine`).
- Durante la edición, la parte superior del formulario muestra el indicador **"Editando solicitud del historial"** con el botón **"Cancelar edición"**.
- El usuario puede:
  - Pulsar **"Guardar Cambios en Historial"** para actualizar inmediatamente el registro en `FastBinaryStore` (sin generar imágenes ni abrir WhatsApp) y regresar al Historial.
  - Pulsar **"Actualizar y Enviar a WhatsApp"** para actualizar el registro en el Historial y compartir la ficha recién generada en WhatsApp.

### Contabilización Individual por Variedad y Subtítulos Condicionales (`VarietyStockItem`)
- Tanto el **nombre principal del producto** como cada **variedad agregada** cuentan con campos propios de **Stock** (en Tipos 2 a 5) y **Recarga** (en Tipos 1 a 5).
- En la vista previa, imagen renderizada y exportación en texto plano (TXT), cada viñeta (`· `) muestra únicamente los subtítulos de los datos que fueron declarados explícitamente:
  - Si no se declara ni stock ni recarga: `· Nombre de Variedad` (se eliminan por completo los subtítulos `Stock:` y `Recarga:`).
  - Si solo se declara stock: `· Nombre de Variedad (Stock: X)`.
  - Si solo se declara recarga: `· Nombre de Variedad (Recarga: Y)`.
  - Si se declaran ambos: `· Nombre de Variedad (Stock: X | Recarga: Y)`.

---

## Exportación en Texto Plano (TXT) e Imagen Render Monocroma

Tanto en el **Catálogo de Productos** (`WhatsAppExportDialog.kt`) como en el **Historial de Solicitudes** (`RequestHistoryScreen.kt`), el usuario puede elegir cómo compartir la información por WhatsApp:

1. **Texto Plano (TXT)**: Mensaje estructurado libre de emojis, encabezado por el nombre personalizado del local de trabajo.
2. **Imagen Render Monocroma (`generateMonochromeCatalogBitmap` / `generateMonochromeHistoryBitmap`)**: Renderiza bajo demanda mediante `Canvas` y `StaticLayout` un documento visual de `1080px` de ancho en formato `Bitmap.Config.RGB_565` estrictamente monocromo (**fondo blanco `#FFFFFF`, texto negro `#111111` y líneas divisorias negras `#111111`**), liberando la memoria nativa inmediatamente después de guardar el archivo temporal en caché.

---

## Desglose Detallado de Componentes

- **`MainActivity.kt`**: Barra superior plana de `48.dp` de una sola línea y barra de navegación adaptativa de solo iconos (`FastNavIconItem`) en la parte inferior (`52.dp` en móviles verticales) o lateral (`60.dp` en pantallas horizontales o tablets `>= 600.dp`).
- **`FastComponents.kt`**: Biblioteca interna de controles planos sin elevación ni subcomposición (`FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton`, `FastCheckBox`, `FastNavIconItem`) y definición de `SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)`.
- **`CatalogScreen.kt`**: Lista principal con filas planas de una sola capa (`ProductListItem`), filtros rápidos (`20%`, `25%`, `30%`, `35%`, `Personalizado`), vista adaptativa de 1 o 2 columnas (`maxWidth >= 680.dp`) y respaldo/restauración binaria `.bin` con `BinaryImportModeDialog`.
- **`RequestScreen.kt`**: Armador y editor de solicitudes ligero sin buscador de catálogo acoplado, con controles `FastTextField` y `FastChip`, soporte de edición de registros del historial y disposición automática en 2 columnas (`maxWidth >= 700.dp`).
- **`RequestHistoryScreen.kt`**: Historial cronológico libre de imágenes almacenadas, con tarjetas planas (`RequestHistoryCard`), botón **"Editar"** para re-edición completa, exportación a WhatsApp en TXT/Imagen Monocroma y menú de exportación/importación binaria `.bin`.

---

## Sistema Visual, Responsividad y Contraste Adaptativo

Definido en `com.example.ui.theme.Color.kt`, `Theme.kt` y `FastComponents.kt`:
- **Regla de Luminancia Automática (`getContrastingTextColor`)**:
  Evalúa `backgroundColor.luminance() < 0.45f`.
  - Si el fondo es **oscuro**, aplica texto e iconos en **blanco puro (`#FFFFFF`)**.
  - Si el fondo es **claro**, aplica texto e iconos en **negro profundo (`#111111`)**.

---

## Requisitos e Instalación

### Requisitos Técnicos
- **Android Studio**: Ladybug (2024.2.1) o superior.
- **JDK**: Java 17.
- **SDK Mínimo (`minSdk`)**: API 24 (Android 7.0 Nougat) — compatible con dispositivos de gama baja.
- **SDK Objetivo (`targetSdk` / `compileSdk`)**: API 35 / 36.

### Compilación y Ejecución
1. Compilar el APK de depuración mediante Gradle:
   ```bash
   gradle assembleDebug
   ```
2. Ejecutar las pruebas unitarias locales (Robolectric / JUnit):
   ```bash
   gradle :app:testDebugUnitTest
   ```
