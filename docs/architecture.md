# Arquitectura del Sistema

**Sistema de Precios** está diseñado bajo una arquitectura **MVVM (Model-View-ViewModel)** estrictamente desacoplada, optimizada para operar sin bloqueos en el hilo principal (`0 ms` de latencia de entrada) y con una huella mínima de memoria RAM en dispositivos Android desde API 24 (Android 7.0).

---

## 1. Diagrama General de Capas

```text
+-----------------------------------------------------------------------------+
|                        CAPA DE INTERFAZ (Jetpack Compose)                   |
|                                                                             |
|  +-----------------------+  +-----------------------+  +-----------------+  |
|  |     CatalogScreen     |  |     RequestScreen     |  |  RequestHistory |  |
|  |  (Lista + Buscador)   |  |  (Tipos 1-5 + Edición)|  | (Tarjetas + TXT)|  |
|  +-----------+-----------+  +-----------+-----------+  +--------+--------+  |
|              |                          |                       |           |
|              +--------------------------+-----------------------+           |
|                                         |                                   |
|                    Primitivas Planas (FastComponents.kt)                    |
|     FastTextField | FastChip | FastButton | FastCheckBox | FastNavIconItem  |
+-----------------------------------------+-----------------------------------+
                                          | StateFlow / Eventos Unidireccionales
+-----------------------------------------v-----------------------------------+
|                        CAPA DE PRESENTACIÓN (ViewModels)                    |
|                                                                             |
|           +------------------------+         +------------------------+     |
|           |    CatalogViewModel    |         |    RequestViewModel    |     |
|           | - Filtrado en Default  |         | - Validación Tipos 1-5 |     |
|           | - Selección múltiple   |         | - Re-edición Historial |     |
|           | - Respaldo .bin        |         | - Despacho WhatsApp    |     |
|           +-----------+------------+         +-----------+------------+     |
+-----------------------|----------------------------------|------------------+
                        +-----------------+----------------+
                                          |
+-----------------------------------------v-----------------------------------+
|                         CAPA DE DATOS Y PERSISTENCIA                        |
|                                                                             |
|                       +----------------------------------+                  |
|                       |        ProductRepository         |                  |
|                       +-----------------+----------------+                  |
|                                         |                                   |
|                       +-----------------v----------------+                  |
|                       |       FastBinaryStore (V2)       |                  |
|                       | - Estado en memoria (StateFlow)  |                  |
|                       | - Índice Hash O(1) + Búsqueda Bin|                  |
|                       | - Cola Conflated (Debounce 250ms)|                  |
|                       | - Escritura atómica (.tmp+fsync) |                  |
|                       +----------------------------------+                  |
+-----------------------------------------------------------------------------+
```

---

## 2. Decisiones Clave de Rendimiento y Memoria

### 2.1. Eliminación de Capas Pesadas (Sin ORM ni Clientes de Red)
A diferencia de proyectos convencionales que incorporan bases de datos SQLite/Room, procesadores de anotaciones KSP, clientes HTTP (`Retrofit`/`OkHttp`) o cargadores de imágenes asíncronos (`Coil`), este sistema prescinde deliberadamente de todas esas dependencias por tres motivos técnicos:
1. **Volumen de datos acotado al dominio comercial**: Un catálogo minorista o mayorista local (de `1.000` a `50.000` artículos) cabe holgadamente en memoria como objetos inmutables indexados, permitiendo filtrado instantáneo sin cursores SQL ni mapeo de entidades.
2. **Arranque en frío inmediato**: La lectura secuencial de un archivo binario estructurado (`DataInputStream` con buffer de `16 KB`) es significativamente más rápida y predecible que inicializar un motor SQLite con migraciones y reflexión.
3. **Ahorro de RAM y tamaño de APK**: Al eliminar librerías no esenciales y guardar el historial sin imágenes, el consumo de memoria se mantiene estable incluso tras horas de operación continua.

---

### 2.2. Primitivas Planas de Interfaz (`FastComponents.kt`)
Los controles estándar de Material 3 (`OutlinedTextField`, `FilterChip`, `NavigationBarItem`) utilizan `SubcomposeLayout`, capas múltiples de `Surface`, sombras de elevación y animaciones de estado que penalizan el tiempo de composición cuando se renderizan decenas de elementos simultáneamente.

El módulo `com.example.ui.components.FastComponents` sustituye esos controles por primitivas planas de una sola capa:

| Componente Plano | Reemplaza a | Ventaja Arquitectónica |
| :--- | :--- | :--- |
| `FastTextField` | `OutlinedTextField` | Basado en `BasicTextField` + `Row` plano; sin `SubcomposeLayout` ni etiquetas flotantes animadas. |
| `FastChip` | `FilterChip` | Un único `Row` con fondo sólido, borde de `1.dp` y contraste calculado por luminancia. |
| `FastButton` / `FastOutlinedButton` | `Button` / `OutlinedButton` | Sin sombras de elevación ni capas intermedias de `Surface`. |
| `FastCheckBox` | `Checkbox` | `Box` ligero de `28.dp` para selección fluida durante el desplazamiento del catálogo. |
| `FastNavIconItem` | `NavigationBarItem` / `NavigationRailItem` | Botón de navegación de solo icono sin etiqueta de texto ni transiciones costosas. |

---

### 2.3. Motor Binario `FastBinaryStore`
Ubicado en `com.example.data.local.FastBinaryStore`, gestiona tres archivos independientes en el almacenamiento interno privado (`context.filesDir`):
- `catalog_fast_v2.bin`: Catálogo de productos ordenado alfabéticamente por `sortKey`.
- `requests_fast_v2.bin`: Historial cronológico de solicitudes (exclusivamente texto y números, sin rutas de imagen).
- `store_config_v1.bin`: Nombre personalizado del local o sitio de trabajo.

#### Garantías de Concurrencia y Durabilidad
- **Lectura O(1) en memoria**: Expone `productsFlow`, `requestsFlow` y `workplaceNameFlow` como `StateFlow` respaldados por un mapa `@Volatile Map<String, Product>` para consultas directas por `id`.
- **Escritura desacoplada con colapso de ráfagas**: Tres canales `Channel<Unit>(Channel.CONFLATED)` agrupan modificaciones rápidas en una ventana de `250 ms` antes de serializar a disco en `Dispatchers.IO`.
- **Transacción atómica en sistema de archivos**:
  1. Escribe el flujo completo en un archivo temporal (`*.bin.tmp`).
  2. Ejecuta `FileOutputStream.fd.sync()` para asegurar el volcado físico a la memoria flash.
  3. Reemplaza el archivo definitivo mediante `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.

---

## 3. Estructura de Paquetes del Código Fuente

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Contenedor principal y navegación plana por iconos
├── data/
│   ├── local/
│   │   └── FastBinaryStore.kt       # Persistencia binaria atómica e importación/exportación .bin
│   ├── model/
│   │   ├── Product.kt               # Entidad inmutable de producto e índice de búsqueda por tokens
│   │   └── PriceRequest.kt          # Entidad de solicitud (sin imágenes) y VarietyStockItem
│   └── repository/
│       └── ProductRepository.kt     # Fachada única de acceso a datos para los ViewModels
├── ui/
│   ├── catalog/                     # Pantalla de catálogo, modal de producto y exportación
│   ├── components/                  # Biblioteca interna FastComponents
│   ├── request/                     # Formulario de solicitudes (Tipos 1-5), recorte 16:9 e historial
│   └── theme/                       # Paleta semántica y contraste dinámico por luminancia
└── util/
    ├── ImageUtils.kt                # Generación bajo demanda de fichas e imágenes monocromas RGB_565
    └── MathUtils.kt                 # Cálculo entero de márgenes, redondeo a 500 y normalización UTF-8
```
