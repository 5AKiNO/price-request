# Sistema Móvil de Precios e Inventario

Aplicación Android nativa desarrollada en **Kotlin** y **Jetpack Compose (Material Design 3)** para la administración ágil de catálogos de productos, cálculo bidireccional de márgenes de ganancia en **Guaraníes (`Gs.`)** y generación de fichas infográficas en formato **`16:9`** listas para compartir directamente por **WhatsApp**.

Diseñada desde cero bajo una arquitectura de **ultra-bajo consumo de memoria RAM y CPU**, permite operar con fluidez instantánea incluso en dispositivos móviles de gama de entrada (2 GB – 3 GB de RAM) y totalmente fuera de línea (offline-first).

---

## Tabla de Contenidos

1. [Características Principales](#1-características-principales)
2. [Arquitectura General del Sistema](#2-arquitectura-general-del-sistema)
3. [Desglose Detallado de Todos los Componentes](#3-desglose-detallado-de-todos-los-componentes)
   - [3.1 Capa de Navegación y Tema de Alto Contraste Adaptativo](#31-capa-de-navegación-y-tema-de-alto-contraste-adaptativo)
   - [3.2 Módulo 1: Catálogo de Productos y Gestor de Precios](#32-módulo-1-catálogo-de-productos-y-gestor-de-precios)
   - [3.3 Módulo 2: Generador de Solicitudes y Fichas Visuales 16:9](#33-módulo-2-generador-de-solicitudes-y-fichas-visuales-169)
   - [3.4 Módulo 3: Historial de Solicitudes](#34-módulo-3-historial-de-solicitudes)
   - [3.5 Capa de Datos y Almacenamiento Binario Ultraligero](#35-capa-de-datos-y-almacenamiento-binario-ultraligero)
   - [3.6 Motores Transversales de Cálculo e Imagen](#36-motores-transversales-de-cálculo-e-imagen)
4. [Reglas Matemáticas y de Negocio](#4-reglas-matemáticas-y-de-negocio)
5. [Optimizaciones para Dispositivos de Gama Baja](#5-optimizaciones-para-dispositivos-de-gama-baja)
6. [Requisitos, Instalación y Compilación](#6-requisitos-instalación-y-compilación)

---

## 1. Características Principales

* **Catálogo Limpio para Producción:** Base de datos inicial 100% limpia sin registros de prueba precargados, lista para registrar inventario real desde el primer arranque.
* **Buscador Multicriterio con Preservación de `ñ` / `Ñ`:** Búsqueda reactiva con `debounce(150ms)` ejecutada en hilos secundarios (`Dispatchers.Default`). Elimina tildes diacríticas (`á, é, í, ó, ú, ü`) para facilitar las consultas pero preserva estrictamente la letra **`ñ`** como carácter distintivo del español.
* **Sincronización Bidireccional de Precios:** Cálculo automático entre Precio de Costo, Precio de Venta y botones de margen seleccionables (**`20%`, `25%`, `30%`, `40%`, `50%`**).
* **Sistema de 5 Tipos de Solicitud de Precio:** Flujos especializados para alta de productos faltantes, subidas/bajadas de costo y ajustes por margen superior o inferior al deseado, cada uno con codificación cromática de alta legibilidad.
* **Múltiples Variedades por Costo Idéntico:** Capacidad de agrupar bajo una misma solicitud el producto principal y todas sus variedades (sabores, medidas, presentaciones) que comparten el mismo costo, listándolas con viñetas (`· `) tanto en pantalla como en la imagen generada.
* **Cámara Universal y Recorte Interactivo `16:9` Real:** Captura compatible con cualquier fabricante de Android (incluyendo cámaras personalizadas con fallback en memoria y `ClipData` seguro) y visor interactivo que muestra la foto completa permitiendo mover y redimensionar un recuadro `16:9` exacto sobre la imagen.
* **Renderizado Infográfico Sin Bordes (`1080 px`):** Generación nativa en `Canvas` de fichas visuales en formato JPEG al 85% con fotografía `16:9` a ancho completo, tipografía negra de alto impacto sobre contenedores claros y envío directo de la imagen a WhatsApp.
* **Contraste Dinámico Automático (Blanco / Negro):** Todos los menús, tarjetas, diálogos, listas desplegables y campos de texto evalúan la luminancia de su fondo en tiempo real: **fondo oscuro → texto blanco (`#FFFFFF`)**; **fondo claro → texto negro (`#111111`)**.

---

## 2. Arquitectura General del Sistema

El proyecto implementa una arquitectura **MVVM (Model-View-ViewModel)** reactiva combinada con un motor de persistencia binaria directa en memoria (`FastBinaryStore`) y respaldo estructurado:

```text
┌───────────────────────────────────────────────────────────────────────────┐
│                   CAPA DE INTERFAZ (Jetpack Compose + M3)                 │
│                                                                           │
│  ┌─────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│  │   Módulo Catálogo   │  │  Módulo Solicitudes  │  │ Módulo Historial │  │
│  │  - CatalogScreen    │  │  - RequestScreen     │  │ - RequestHistory │  │
│  │  - QuickProductCard │  │  - Crop169Dialog     │  │   Screen         │  │
│  │  - ProductListItem  │  │  - SelectablePctBlock│  │ - RequestHistory │  │
│  │  - ProductDialog    │  │  - UniversalCamera   │  │   Card           │  │
│  │  - WhatsAppDialog   │  │                      │  │                  │  │
│  └──────────┬──────────┘  └──────────┬───────────┘  └────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
              ▼                        ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    CAPA DE ESTADO Y LÓGICA (ViewModels)                   │
│                                                                           │
│  ┌───────────────────────────────┐      ┌──────────────────────────────┐  │
│  │       CatalogViewModel        │      │       RequestViewModel       │  │
│  │ - StateFlow + Debounce 150ms  │      │ - Flujos por Tipo (1..5)     │  │
│  │ - Filtrado en Default Thread  │      │ - Sincronización al Catálogo │  │
│  │ - Exportación Texto WhatsApp  │      │ - Orquestación de Render 16:9│  │
│  └───────────────┬───────────────┘      └──────────────┬───────────────┘  │
└──────────────────┼─────────────────────────────────────┼──────────────────┘
                   │                                     │
                   ▼                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│              MOTORES TRANSVERSALES Y PERSISTENCIA ULTRALIGERA             │
│                                                                           │
│  ┌─────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│  │      MathUtils      │  │      ImageUtils      │  │ FastBinaryStore  │  │
│  │ - Redondeo Half-Up  │  │ - Submuestreo EXIF   │  │ - RAM StateFlow  │  │
│  │ - Moneda Gs. Entera │  │ - Recorte Exacto 16:9│  │ - Escritura .tmp │  │
│  │ - Normalizador Ñ/ñ  │  │ - Render Canvas 1080p│  │   Atómica (.dat) │  │
│  └─────────────────────┘  └──────────────────────┘  └──────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Desglose Detallado de Todos los Componentes

### 3.1 Capa de Navegación y Tema de Alto Contraste Adaptativo

* **`MainActivity` y `MainAppRoot`**:
  * Punto de entrada de la aplicación con soporte `enableEdgeToEdge()` e insectos seguros de barra de estado y barra de navegación (`WindowInsets.statusBars` y `WindowInsets.navigationBars`).
  * Gestiona la navegación principal entre las tres vistas troncales (`CATALOGO`, `NUEVA_SOLICITUD`, `HISTORIAL`) mediante `NavigationBar` e intercepta el botón físico/gesto de retroceso con `BackHandler` para regresar al catálogo desde pantallas secundarias.
  * Permite enviar un producto seleccionado directamente desde el catálogo hacia el generador de solicitudes `16:9` precargando todos sus datos.
* **`Color.kt` y `Theme.kt`**:
  * Define la paleta semántica para los 5 tipos de solicitud evitando tonos de bajo contraste:
    1. **Tipo 1 (Falta Registrar):** Verde Esmeralda (`#1B5E20` / Contenedor `#E8F5E9`).
    2. **Tipo 2 (Costo Subió):** Azul Intenso (`#0D47A1` / Contenedor `#E3F2FD`).
    3. **Tipo 3 (Costo Bajó):** Naranja Fuerte (`#D84315` / Contenedor `#FFF3E0`).
    4. **Tipo 4 (Supera % Deseado):** Teal / Turquesa Oscuro (`#00695C` / Contenedor `#E0F2F1`).
    5. **Tipo 5 (Menor a % Deseado):** Rojo Carmesí (`#B71C1C` / Contenedor `#FFEBEE`).
  * **`getContrastingTextColor(backgroundColor)`**: Función determinista que calcula la luminancia del color de fondo; devuelve blanco puro (`#FFFFFF`) cuando la luminancia es `< 0.45f` (fondo oscuro) y negro profundo (`#111111`) cuando el fondo es claro.
  * **`adaptiveTextFieldColors(containerColor, borderColor)`**: Configura automáticamente todos los `OutlinedTextField` para que el texto ingresado, etiquetas, placeholders, iconos, cursores y textos de soporte respeten la regla blanco sobre oscuro / negro sobre claro.

---

### 3.2 Módulo 1: Catálogo de Productos y Gestor de Precios

* **`CatalogScreen`**:
  * **Buscador Reactivo Multicriterio (`catalog_search_bar`)**: Permite buscar simultáneamente por código de barras/referencia, descripción (respetando la `ñ`), precio de costo o precio de venta.
  * **Menú Desplegable de Selección**: Opciones rápidas para seleccionar todos los productos visibles en el filtro actual o limpiar la selección activa.
  * **Barra Horizontal de Filtros (`LazyRow`)**: Chips optimizados con claves estables (`key`) y `contentType` para filtrar por `Todos`, `Completos`, `Sin Código`, `Falta Registrar`, `Seleccionados`, `20%`, `25%` y `Personalizado`.
  * **Estado Vacío Ligero (Empty State)**: Cuando el catálogo está vacío, presenta un acceso directo para registrar el primer producto sin consumir memoria en vistas innecesarias.
* **`QuickProductCard`**:
  * Tarjeta compacta dentro del carrusel horizontal de acceso rápido/recientes (`190.dp x 118.dp`).
  * Muestra código, insignia de porcentaje real con borde de color, descripción en dos líneas, precio de venta formateado en `Gs.` y botones de acción rápida para **Compartir individualmente** o **Editar**.
* **`ProductListItem`**:
  * Componente principal de cada fila del `LazyColumn` del catálogo.
  * Incluye casilla de selección (`Checkbox`), insignia de código (`Cód: ...`), alerta visual si el estado es `FALTA_REGISTRAR`, desglose de **Costo**, **Venta** y **Margen Real Redondeado**, y cuatro acciones directas en cabecera:
    1. **Compartir producto por WhatsApp** (envío inmediato de texto formateado).
    2. **Crear Ficha `16:9`** (abre el Módulo 2 con el producto precargado).
    3. **Editar producto** (abre `ProductDialog`).
    4. **Eliminar producto**.
* **`ProductDialog`**:
  * Diálogo modal para alta y edición de productos del catálogo.
  * Campos: Código (opcional), Descripción (obligatoria, soporte completo UTF-8 con `ñ`), Precio de Costo (`Gs.`), grupo de botones seleccionables de porcentaje (**`20%`, `25%`, `30%`, `40%`, `50%`**) y Precio de Venta (`Gs.`).
  * Calcula en vivo el **Margen Real Redondeado** y ofrece dos flujos de guardado: **Guardar Producto** o **Guardar y Compartir por WhatsApp**.
* **`WhatsAppExportDialog`**:
  * Diálogo de exportación por lotes para todos los productos marcados en el catálogo.
  * Permite activar o desactivar dinámicamente qué datos incluir en el listado (`Código`, `Precio de Costo`, `Precio de Venta`, `% Margen`), mostrando una vista previa monoespaciada instantánea antes de abrir WhatsApp.
* **`CatalogViewModel`**:
  * Administra el flujo de estado `CatalogUiState` combinando la lista reactiva de `ProductRepository`, la consulta de búsqueda con `debounce(150L)` y el filtro activo sobre `Dispatchers.Default`.

---

### 3.3 Módulo 2: Generador de Solicitudes y Fichas Visuales 16:9

* **`RequestScreen`**:
  * **Banner Informativo y Selector de Tipo (1 a 5)**: Adapta sus bordes, contenedores y estructura del formulario según la opción elegida.
  * **Sección Multimedia (`16:9`)**:
    * Botones para capturar fotografía desde **Cámara** (con solicitud de permiso `Manifest.permission.CAMERA` en tiempo de ejecución) o elegir imagen desde **Galería** (`PickVisualMedia` con fallback universal `GetContent`).
    * Vista previa `16:9` de la imagen recortada con opción de reencuadrar en cualquier momento.
  * **Sección de Datos del Producto y Variedades**:
    * En los tipos `2, 3, 4 y 5`, incluye un buscador desplegable (`ExposedDropdownMenuBox`) conectado al catálogo para autocompletar código, descripción y precios actuales.
    * Campos de **Código (Opcional)** y **Descripción / Nombre Principal (Obligatorio)** disponibles en los 5 tipos de solicitud.
    * **Gestor de Variedades con Mismo Costo**: Permite añadir múltiples nombres o variedades adicionales que comparten el mismo costo; muestra una vista previa con viñetas (`· `) idéntica a como se imprimirá en la ficha gráfica.
    * Campos opcionales de inventario: **Stock Anterior** y **Recarga Stock**.
  * **Sección de Valores de Costo y Venta (Orden Estricto por Tipo)**:
    * **Tipo 1 (*Falta Registrar*):**
      1. Precio de Costo (`Gs.`)
      2. Precio de Venta (`Gs.`) con botones seleccionables (`20%` a `50%`)
    * **Tipos 2 y 3 (*Subida y Bajada de Costo*):**
      1. Precio de Costo Anterior (`Gs.`)
      2. Precio de Costo Nuevo (`Gs.`)
      3. Precio de Venta Anterior (`Gs.`) con botones seleccionables (`20%` a `50%`)
      4. Precio de Venta Nuevo (`Gs.`) con botones seleccionables (`20%` a `50%`)
    * **Tipos 4 y 5 (*Cambio de Precio de Venta al Subir y Bajar — Costo Único*):**
      1. Precio de Costo (`Gs.`, una sola vez)
      2. Precio de Venta Anterior (`Gs.`) con botones seleccionables (`20%` a `50%`)
      3. Precio de Venta Nuevo (`Gs.`) con botones seleccionables (`20%` a `50%`)
* **`SelectablePercentageAndPriceBlock`**:
  * Componente reutilizable que vincula un campo de Precio de Venta con su insignia de porcentaje activo y la fila exclusiva de botones seleccionables **`20%`, `25%`, `30%`, `40%`, `50%`**.
* **`UniversalTakePictureContract`**:
  * Contrato de cámara personalizado que genera un archivo temporal vía `FileProvider`, adjunta `ClipData` con permisos de lectura/escritura para máxima compatibilidad en cualquier versión de Android y dispone de lectura de respaldo (`extras["data"]`) para cámaras de gama baja.
* **`Crop169Dialog`**:
  * Visor interactivo a pantalla completa que dibuja la fotografía original íntegra centrada en pantalla y superpone un recuadro `16:9` con regla de los tercios y esquinas reforzadas.
  * Permite arrastrar libremente el recuadro `16:9` sobre cualquier zona de la imagen y ajustar su escala (`30%` a `100%`) mediante gestos multitáctiles o control deslizable (`Slider`), garantizando que el área recortada coincida exactamente con la selección visual del usuario.
* **`RequestViewModel`**:
  * Orquesta la lógica bidireccional de precios anteriores y nuevos, valida los campos requeridos, invoca la generación de la infografía en segundo plano (`Dispatchers.IO`), guarda la solicitud en el historial y actualiza automáticamente el catálogo general de productos.

---

### 3.4 Módulo 3: Historial de Solicitudes

* **`RequestHistoryScreen` y `RequestHistoryCard`**:
  * Lista cronológica optimizada (`LazyColumn` con `key` y `contentType`) de todas las solicitudes generadas.
  * Cada tarjeta adopta el contenedor claro y el borde del color identificativo de su tipo (`1` a `5`), mostrando fecha/hora, lista con viñetas (`· `) del producto y sus variedades, código, stock anterior/recarga y el desglose de precios en el mismo orden estricto del formulario.
  * Incluye botón directo **Reenviar Ficha a WhatsApp** (que comparte el archivo JPEG renderizado sin necesidad de regenerarlo) y botón de eliminación individual.

---

### 3.5 Capa de Datos y Almacenamiento Binario Ultraligero

* **`FastBinaryStore`**:
  * Motor de almacenamiento binario directo diseñado para eliminar cuellos de botella en dispositivos con procesadores básicos y almacenamiento lento:
    * Mantiene el estado activo en memoria RAM mediante `MutableStateFlow` para lecturas instantáneas en `O(1)`.
    * Serializa colecciones completas de `Product` y `PriceRequest` usando `BufferedOutputStream` y `DataOutputStream` sobre archivos binarios compactos (`products_v2.bin` y `requests_v2.bin`).
    * Implementa **escritura atómica transaccional** escribiendo primero en un archivo temporal (`.tmp`) y renombrándolo al archivo definitivo para evitar corrupción de datos ante cierres inesperados o cortes de batería.
* **`Product` y `PriceRequest` (`@Immutable`)**:
  * Modelos de dominio inmutables anotados con `@Immutable` para permitir que el compilador de Jetpack Compose omita recomposiciones innecesarias.
  * `Product` precalcula al instanciarse el campo `searchIndex` normalizando código, descripción (con `ñ`), costo y venta una sola vez.
* **`AppDatabase`, `ProductDao`, `PriceRequestDao`, `ProductEntity`, `PriceRequestEntity`**:
  * Esquema estructurado con índices explícitos en `codigo` y `descripcion` y soporte de migración limpia sin datos semilla de prueba.
* **`ProductRepository`**:
  * Repositorio centralizado (Singleton) que expone flujos reactivos (`Flow<List<Product>>` y `Flow<List<PriceRequest>>`) y coordina las operaciones CRUD de productos y solicitudes.

---

### 3.6 Motores Transversales de Cálculo e Imagen

* **`MathUtils`**:
  * Encapsula toda la aritmética comercial en enteros largos (`Long`) para evitar errores de coma flotante:
    * `redondearHalfUp(valor: Double): Int`
    * `calcularPorcentajeRedondeado(costo: Long, venta: Long): Int`
    * `calcularVentaDesdePorcentaje(costo: Long, porcentaje: Int): Long`
    * `formatearGuaranies(monto: Long): String` (formato con separador de miles por puntos, ej. `13.500`).
    * `normalizarConEnie(input: String): String` (elimina diacríticos salvo `ñ`/`Ñ`).
* **`ImageUtils`**:
  * **`decodeBitmapFromUri`**: Calcula `inSampleSize` leyendo primero solo las dimensiones (`inJustDecodeBounds = true`) para limitar la textura en RAM a un máximo de `1280x720 px` y corrige automáticamente la rotación según los metadatos `ExifInterface`.
  * **`cropTo16By9`**: Extrae con precisión sub-píxel el rectángulo normalizado seleccionado en `Crop169Dialog` y lo escala a `1280x720 px` (`RGB_565` / `ARGB_8888`), reciclando bitmaps intermedios cuando corresponde.
  * **`generateInfographicBitmap`**: Renderiza en un `Canvas` nativo de `1080 px` de ancho la ficha para WhatsApp:
    1. Cabecera superior con el color identificativo del tipo de solicitud y título de alto contraste.
    2. Fotografía `16:9` a **ancho completo (`1080x607 px`) sin bordes laterales ni insignias superpuestas**.
    3. Bloque de **Nombre Producto** con lista de puntos (`· `) para el nombre principal y todas las variedades declaradas, con ajuste automático de línea (`wrapTextLines`), más código y stock opcionales.
    4. Tarjeta inferior de precios con fondo claro del tipo de solicitud, borde coloreado de `4 px` y tipografía negra extra-grande en el orden exacto de cada tipo de solicitud.
  * **`saveBitmapToCacheAndGetUri` y `shareImageOnlyToWhatsApp`**: Comprime la ficha en **JPEG al 85% de calidad**, expone un `content://` URI seguro mediante `FileProvider` y lanza el `Intent.ACTION_SEND` (`image/jpeg`) dirigido a WhatsApp.

---

## 4. Reglas Matemáticas y de Negocio

1. **Moneda Entera (`Gs.`):** Todos los importes monetarios se almacenan y procesan como enteros de 64 bits (`Long`), sin decimales, formateados con puntos de miles (`Gs. 25.000`).
2. **Fórmula de Margen Real sobre Costo:**
   $$\text{Margen Exacto (\%)} = \left(\frac{\text{Precio Venta} - \text{Precio Costo}}{\text{Precio Costo}}\right) \times 100$$
3. **Regla de Redondeo Half-Up (`>= 0.5`):**
   * Si la parte decimal del porcentaje es **$\ge 0.5$**, se redondea al entero inmediatamente superior (`Math.floor(valor + 0.5)`).
   * Si la parte decimal es **$< 0.5$**, se mantiene el entero inferior.
4. **Cálculo de Precio de Venta desde Porcentaje Seleccionable (`20%`, `25%`, `30%`, `40%`, `50%`):**
   $$\text{Precio Venta} = \text{RedondearHalfUp}\left(\text{Precio Costo} \times \left(1 + \frac{\text{Porcentaje}}{100}\right)\right)$$
5. **Normalización Lingüística con Preservación de `ñ`:**
   * Antes de descomponer caracteres en `NFD`, las letras `ñ` y `Ñ` se protegen mediante un token interno, se eliminan las marcas diacríticas Unicode (`\p{InCombiningDiacriticalMarks}+`) de las vocales acentuadas y finalmente se restauran como `ñ`, permitiendo distinguir y buscar con exactitud términos comerciales en español.

---

## 5. Optimizaciones para Dispositivos de Gama Baja

| Área | Técnica Implementada | Beneficio en Gama Baja (2 GB – 3 GB RAM) |
| :--- | :--- | :--- |
| **Almacenamiento** | `FastBinaryStore` con `DataInputStream`/`DataOutputStream` y caché en RAM | Carga inicial y escrituras en milisegundos sin sobrecarga de ORM ni bloqueos de hilo principal. |
| **Búsqueda** | Índice precalculado `searchIndex` en cada `Product` + `debounce(150ms)` en `Dispatchers.Default` | Cero tirones (jank) al escribir rápido en catálogos extensos. |
| **Listas Compose** | Claves únicas (`key = { it.id }`), `contentType`, modelos `@Immutable` y `derivedStateOf` | Reutilización óptima de nodos en `LazyColumn` y `LazyRow` con mínimas recomposiciones. |
| **Fotografía y Canvas** | Submuestreo `BitmapFactory.Options.inSampleSize` a `1280x720 px` y salida JPEG al 85% | Prevención estricta de `OutOfMemoryError` (OOM) al abrir cámara o galería. |
| **Interfaz Gráfica** | Bordes definidos y colores sólidos de alto contraste sin desenfoques ni sombras pesadas | Renderizado fluido a 60 FPS en GPUs básicas. |

---

## 6. Requisitos, Instalación y Compilación

### Requisitos Previos
* **Android Studio:** Ladybug (2024.2.1) o superior.
* **JDK:** Java 17 o superior.
* **Android SDK:** `minSdk = 24` (Android 7.0 Nougat) / `targetSdk = 35` / `compileSdk = 35`.

### Pasos para Clonar y Compilar

1. **Clonar el repositorio:**
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd <NOMBRE_DEL_DIRECTORIO>
   ```

2. **Compilar el APK de depuración mediante Gradle:**
   ```bash
   gradle assembleDebug
   ```
   *(O utilizando el wrapper en entornos locales: `./gradlew assembleDebug`)*

3. **Ejecutar pruebas unitarias y de validación local:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

4. **Ubicación del APK generado:**
   El archivo instalable se generará en:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
