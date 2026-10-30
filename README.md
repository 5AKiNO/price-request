# Sistema de Precios

![Android CI](https://img.shields.io/badge/compilaci%C3%B3n-estable-2ea043)
![Android API](https://img.shields.io/badge/Android-API%2024%2B%20(7.0%2B)-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20FastComponents-4285F4)
![Versión](https://img.shields.io/badge/versi%C3%B3n-2.4.0-1f6feb)
![Licencia](https://img.shields.io/badge/licencia-Uso%20No%20Comercial-d73a4a)

**Aplicación Android de inventario, cálculo de márgenes y solicitudes de ajuste de precios para comercios locales, desarrollada en Kotlin y Jetpack Compose.**

**Sistema de Precios** resuelve la operación diaria de tiendas minoristas y mayoristas que necesitan consultar costos al instante, calcular precios de venta con márgenes estandarizados (`20%`, `25%`, `30%`, `35%`, `50%`) y redondeo comercial a múltiplos de `500`, auditar diferencias de precios en piso de venta, re-editar solicitudes históricas y despachar reportes por **WhatsApp** en **Texto Plano (TXT)** o **Imagen Render Monocroma** funcionando **100% fuera de línea (offline-first)** en teléfonos modestos desde 1 GB de RAM.

---

## Tabla de Navegación Rápida

- [¿Por qué existe este proyecto?](#por-qué-existe-este-proyecto)
- [¿Para quién está pensado? (Guías por Perfil)](#para-quién-está-pensado-guías-por-perfil)
- [Vista Previa del Producto (Interfaz en Texto)](#vista-previa-del-producto-interfaz-en-texto)
- [Cómo lo usa un comercio en el día a día](#cómo-lo-usa-un-comercio-en-el-día-a-día)
- [Características Principales](#características-principales)
- [Ecosistema de Documentación (`docs/`)](#ecosistema-de-documentación-docs)
- [Inicio Rápido e Instalación](#inicio-rápido-e-instalación)
- [Resumen de Arquitectura](#resumen-de-arquitectura)
- [Preguntas Frecuentes (FAQ)](#preguntas-frecuentes-faq)
- [Hoja de Ruta (Roadmap)](#hoja-de-ruta-roadmap)
- [Licencia (Prohibición de Uso Comercial por Terceros)](#licencia-prohibición-de-uso-comercial-por-terceros)

---

## ¿Por qué existe este proyecto?

En muchos comercios locales, depósitos y autoservicios, la actualización de precios sigue dependiendo de cuadernos, calculadoras manuales o mensajes desestructurados de WhatsApp entre el piso de venta y la administración:
- El personal de salón detecta productos que **faltan registrar**, artículos cuyo **costo subió o bajó** en la factura del proveedor, o precios en góndola que **exceden o quedan por debajo del margen objetivo (`20%..50%`)**.
- Las aplicaciones empresariales tradicionales exigen servidores en la nube, suscripciones mensuales o teléfonos de gama alta que se bloquean al filtrar miles de artículos.

**Sistema de Precios** fue construido para eliminar esa fricción:
1. **Respuesta inmediata (0 ms de bloqueo)**: Utiliza primitivas planas propias (`FastComponents`) y un motor binario directo (`FastBinaryStore` V2) sin bases de datos SQLite/ORM pesadas.
2. **Estandarización operativa**: Convierte cualquier ajuste de precio o conteo de variedades en una solicitud verificada lista para compartir por WhatsApp en segundos.
3. **Portabilidad total sin servidores**: Todo el catálogo y el historial se respaldan e importan en archivos `.bin` compactos en cuestión de milisegundos.

---

## ¿Para quién está pensado? (Guías por Perfil)

| Perfil | Objetivo Principal | Por dónde empezar |
| :--- | :--- | :--- |
| **Dueño de Tienda / Gerente** | Definir el nombre del comercio, controlar que los márgenes (`20%`, `25%`, `30%`, `35%`, `50%`) se cumplan y mantener respaldos `.bin` seguros. | Ver [Cómo lo usa un comercio](#cómo-lo-usa-un-comercio-en-el-día-a-día) y [Formatos de Respaldo `.bin`](./docs/export-formats.md). |
| **Encargado de Ventas / Salón** | Buscar precios en segundos, relevar stock/recarga por variedad y enviar solicitudes de cambio de precio por WhatsApp. | Ver [Flujos Funcionales y Tipos 1 a 5](./docs/feature-flows.md). |
| **Administrador / Operador de Caja** | Auditar el Historial, **re-editar solicitudes** cuando cambia un stock o costo, y exportar listados en TXT o Imagen Monocroma. | Ver [Flujo de Re-edición de Historial](./docs/feature-flows.md#4-flujo-3-re-edición-de-solicitudes-desde-el-historial) y [Preguntas Frecuentes](./docs/faq.md). |
| **Desarrollador Android** | Compilar el proyecto, estudiar el motor `FastBinaryStore` o proponer mejoras respetando el límite de memoria RAM. | Ver [Arquitectura del Sistema](./docs/architecture.md), [Modelos de Datos](./docs/data-models.md) y [Guía de Contribución](./CONTRIBUTING.md). |

---

## Vista Previa del Producto (Interfaz en Texto)

La interfaz prescinde de adornos innecesarios y utiliza una barra de navegación plana de **solo iconos** (Catálogo, Solicitud e Historial) para aprovechar el 100% de la pantalla:

```text
+-----------------------------------------------------------------------+
| COMERCIAL SAN JOSÉ (Editar)                         [Exportar] [ + ]  |
+-----------------------------------------------------------------------+
| [ Buscar por código, descripción, 35%, 12.500...          ] [ X ] [⋮] |
| (Todos) (Completos) (Sin Código) (Falta Reg.) (20%) (25%) (30%) (35%) |
+-----------------------------------------------------------------------+
| [ ] [100648] Jugo en Sobre 25g                           [solicitud]  |
|     Costo: 1.500 Gs.   |   Venta: 2.000 Gs.   |   Margen: +33%        |
+-----------------------------------------------------------------------+
| [ ] [SIN CÓDIGO] Bandeja Plástica Mediana                [solicitud]  |
|     Costo: 10.000 Gs.  |   Venta: 13.500 Gs.  |   Margen: +35%        |
+-----------------------------------------------------------------------+
|          [ Icono Catálogo ]   [ Icono Solicitud ]   [ Icono Historial ]|
+-----------------------------------------------------------------------+
```

### Pantalla de Solicitud y Modo de Re-edición desde Historial

```text
+-----------------------------------------------------------------------+
| Editando solicitud del historial                   [Cancelar edición] |
+-----------------------------------------------------------------------+
| Tipo: (1.Falta Reg) (2.Subió) (3.Bajó) ([4.Excede %]) (5.Inferior %)  |
| % Deseado: (20%) (25%) (30%) ([35%]) (50%)                            |
| Código: [ 100648       ]  Producto: [ Jugo en Sobre 25g             ] |
| Stock Principal: [ 18  ]  Recarga Principal: [ 24  ]                  |
| Variedades: · Naranja (Stock: 6 | Recarga: 12)  · Durazno (Stock: 12) |
|-----------------------------------------------------------------------|
| Precio de Costo:          [ 1.500 ] Gs.                               |
| Precio de Venta Anterior: [ 2.500 ] Gs.  (20%) (25%) (30%) (35%) (50%)|
| Precio de Venta Nuevo:    [ 2.000 ] Gs.  (20%) (25%) (30%) [35%] (50%)|
| * Regla Tipo 4 verificada: Venta Nueva (2.000) < Venta Anterior (2.500)|
+-----------------------------------------------------------------------+
| [ Guardar Cambios en Historial ]   [ Actualizar y Enviar a WhatsApp ] |
+-----------------------------------------------------------------------+
```

---

## Cómo lo usa un comercio en el día a día

```text
  [1. Consulta en Salón]          [2. Detección de Ajuste]         [3. Envío y Auditoría]
El vendedor busca el artículo   Selecciona el Tipo de Solicitud   Envía la ficha en TXT o
por código, nombre o margen     (1 a 5), aplica el margen del     Imagen Monocroma por WhatsApp.
(ej. "bandeja 35%") en el       20%, 25%, 30%, 35% o 50% y        El registro queda en Historial
Catálogo offline.               carga el stock por variedad.      listo para re-editarse.
```

---

## Características Principales

- **Catálogo con Buscador Multicriterio por Tokens**: Búsqueda instantánea por código principal, código alternativo, descripción (con preservación de la letra `ñ` e insensibilidad a tildes), costo, precio de venta (con o sin puntos separadores de miles) y filtros directos por margen (`20%`, `25%`, `30%`, `35%`, `Personalizado`).
- **Cinco Modalidades de Solicitud de Cambio de Precios**:
  1. **Tipo 1 (Falta Registrar)**: Alta de productos nuevos con costo, venta y recarga opcional.
  2. **Tipo 2 (Costo Subió)**: Validación estricta `Costo Nuevo > Costo Anterior`.
  3. **Tipo 3 (Costo Bajó)**: Validación estricta `Costo Nuevo < Costo Anterior`.
  4. **Tipo 4 (Excede % deseado)**: Ajuste a la baja cuando el precio actual supera el margen meta (`Venta Nueva < Venta Anterior`).
  5. **Tipo 5 (Inferior al % deseado)**: Ajuste al alza cuando el precio actual está por debajo del margen meta (`Venta Nueva > Venta Anterior`).
- **Re-edición Completa desde el Historial**: Botón `"Editar"` en cada registro del Historial que carga todos sus datos y variedades en la pantalla de Solicitud para sobrescribir el registro en su lugar (`"Guardar Cambios en Historial"` o `"Actualizar y Enviar a WhatsApp"`).
- **Historial Ultraligero Libre de Imágenes**: El historial almacena únicamente datos estructurados de texto y números (`PriceRequest`), evitando saturar la memoria flash o la RAM con imágenes.
- **Exportación Dual para WhatsApp**:
  - **Texto Plano (TXT)**: Mensajes limpios con subtítulos condicionales de `Stock` y `Recarga` por variedad.
  - **Imagen Render Monocroma (`RGB_565`)**: Tablas e infografías en blanco y negro (`#FFFFFF` / `#111111`) generadas al vuelo y liberadas inmediatamente de la memoria nativa.
- **Respaldo e Importación Binaria (`.bin`)**: Copias de seguridad completas de Catálogo e Historial con modos **Fusionar** y **Reemplazar todo**.

---

## Ecosistema de Documentación (`docs/`)

Para profundizar en cada aspecto técnico y funcional del proyecto, consulta la documentación modular:

| Documento | Contenido |
| :--- | :--- |
| [**Arquitectura del Sistema**](./docs/architecture.md) | Diseño MVVM, primitivas `FastComponents`, motor `FastBinaryStore` V2 y optimización de memoria RAM. |
| [**Flujos Funcionales y Operativos**](./docs/feature-flows.md) | Guía paso a paso del Catálogo, reglas de los 5 Tipos de Solicitud y ciclo de re-edición del Historial. |
| [**Formatos de Exportación y `.bin`**](./docs/export-formats.md) | Especificación de bytes de los archivos `.bin` (`STP2`, `STR2`, `STC2`), salida TXT e Imagen Monocroma `RGB_565`. |
| [**Modelos de Datos y Fórmulas**](./docs/data-models.md) | Estructura de `Product`, `PriceRequest`, `VarietyStockItem` y aritmética entera con redondeo a `500`. |
| [**Preguntas Frecuentes (FAQ)**](./docs/faq.md) | Solución a dudas comunes sobre los Tipos 4 y 5, búsqueda con `ñ`, respaldos `.bin` y uso sin internet. |
| [**Hoja de Ruta (Roadmap)**](./docs/roadmap.md) | Hitos entregados y mejoras planificadas para próximas versiones. |
| [**Guía de Contribución**](./CONTRIBUTING.md) | Reglas de código, criterios de rendimiento y proceso para enviar *Pull Requests*. |
| [**Registro de Cambios**](./CHANGELOG.md) | Historial detallado de versiones (`v1.0.0` a `v2.4.0`). |

---

## Inicio Rápido e Instalación

### Requisitos del Entorno
- **JDK**: Java 17.
- **Android SDK**: `minSdk 24` (Android 7.0 Nougat) / `targetSdk 35` / `compileSdk 36`.
- **Gradle**: Configurado con Version Catalog (`gradle/libs.versions.toml`).

### Compilación desde Terminal
```bash
# 1. Ejecutar pruebas unitarias y de lógica comercial
gradle :app:testDebugUnitTest

# 2. Compilar el paquete instalable de depuración (APK)
gradle :app:assembleDebug
```
El archivo instalable se generará en `app/build/outputs/apk/debug/app-debug.apk`.

---

## Resumen de Arquitectura

El proyecto está construido exclusivamente con **Kotlin** y **Jetpack Compose** sobre tres pilares:
1. **`FastComponents` (`ui/components/FastComponents.kt`)**: Controles planos de una sola capa (`FastTextField`, `FastChip`, `FastButton`, `FastCheckBox`, `FastNavIconItem`) sin `SubcomposeLayout` ni sombras costosas.
2. **`FastBinaryStore` (`data/local/FastBinaryStore.kt`)**: Almacenamiento binario con índice hash $O(1)$ en memoria, colas `Channel.CONFLATED` (`250 ms`) y volcado físico atómico (`fsync` + `ATOMIC_MOVE`).
3. **Cero Dependencias Innecesarias**: Sin ORM (`Room`), sin clientes de red (`Retrofit`/`OkHttp`), sin librerías de carga de imágenes (`Coil`) y sin servicios en la nube, garantizando velocidad constante en hardware modesto.

*(Consulta el detalle completo en [`docs/architecture.md`](./docs/architecture.md)).*

---

## Preguntas Frecuentes (FAQ)

- **¿Por qué el Tipo 4 exige un precio menor al anterior y el Tipo 5 un precio mayor?**
  Porque el **Tipo 4** corrige un precio que *excede* el margen deseado (baja el precio de venta), mientras que el **Tipo 5** corrige un precio que quedó *inferior* al margen deseado (sube el precio de venta).
- **¿Puedo editar una solicitud del Historial sin volver a escribir todas las variedades?**
  Sí. Al pulsar **"Editar"** en el Historial, todas las variedades con su stock y recarga individual se reconstruyen automáticamente en el formulario de Solicitud.
- **¿Se guardan imágenes dentro del Historial?**
  No. Para proteger la memoria RAM y el almacenamiento del teléfono, el Historial guarda únicamente texto y valores numéricos.

*(Ver más respuestas en [`docs/faq.md`](./docs/faq.md)).*

---

## Hoja de Ruta (Roadmap)

- [x] Motor binario V2 (`STP2`/`STR2`/`STC2`) con fusión de respaldos `.bin`.
- [x] Escala de márgenes comerciales (`20%`, `25%`, `30%`, `35%`, `50%`) y filtros rápidos en Catálogo.
- [x] Interfaz plana `FastComponents` y barra de navegación de solo iconos.
- [x] Re-edición in-situ de registros del Historial libre de imágenes.
- [ ] Ajuste masivo de porcentajes por lote de productos seleccionados (`v2.5.x`).
- [ ] Filtro por rango de fechas en Historial (`v2.5.x`).

*(Consulta la planificación completa en [`docs/roadmap.md`](./docs/roadmap.md)).*

---

## Licencia (Prohibición de Uso Comercial por Terceros)

Este repositorio se publica bajo una **Licencia de Código Disponible para Uso Estrictamente No Comercial** (ver archivo [`LICENSE`](./LICENSE)).

- **Permitido**: Estudio del código, auditoría técnica, ejecución personal/privada sin fines de lucro y envío de mejoras a este repositorio oficial.
- **Estrictamente Prohibido**: Queda prohibido a cualquier usuario o tercero de GitHub vender, revender, alquilar, ofrecer como servicio pagado (SaaS), cobrar por instalación/soporte, publicar en tiendas de aplicaciones o monetizar directa o indirectamente este software o trabajos derivados. Todos los derechos comerciales pertenecen exclusivamente al autor y titular del repositorio.
