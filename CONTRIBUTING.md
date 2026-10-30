# Guía de Contribución

Gracias por tu interés en mejorar **Sistema de Precios**. Este documento detalla las reglas técnicas, de diseño y de licencia que rigen todas las colaboraciones en el repositorio.

---

## 1. Condiciones de Licencia (Uso No Comercial)

Antes de enviar cualquier aporte, ten en cuenta que este proyecto se distribuye bajo una **Licencia de Uso Estrictamente No Comercial** ([`LICENSE`](./LICENSE)).
Al enviar un *Pull Request*, aceptas que tu contribución se incorpore bajo los mismos términos de protección no comercial del repositorio.

---

## 2. Principios de Ingeniería del Proyecto

Cualquier cambio o nueva funcionalidad debe respetar cuatro reglas técnicas innegociables:

1. **Consumo Mínimo de Memoria RAM**:
   - No agregues librerías externas pesadas (ORM, clientes HTTP, cargadores de imágenes o frameworks de inyección de dependencias) para tareas que pueden resolverse con la biblioteca estándar de Kotlin y el SDK nativo de Android.
   - En el renderizado de imágenes para WhatsApp (`ImageUtils.kt`), utiliza siempre `Bitmap.Config.RGB_565` y libera explícitamente el bitmap (`bitmap.recycle()`) en un bloque `finally` tras volcarlo al archivo temporal.
2. **Interfaz Plana de Baja Latencia (`FastComponents`)**:
   - Evita introducir componentes de Material 3 con `SubcomposeLayout` pesado (como `OutlinedTextField` o `FilterChip` estándar) dentro de listas o formularios con múltiples entradas.
   - Utiliza las primitivas definidas en `ui/components/FastComponents.kt` (`FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton`, `FastCheckBox`, `FastNavIconItem`).
3. **Historial Libre de Imágenes**:
   - El historial de solicitudes (`PriceRequest`) solo debe almacenar texto estructurado y valores numéricos (`Long`, `Int`, `String`). Nunca persistas rutas de fotografías ni bitmaps en el historial.
4. **Compatibilidad del Formato Binario (`.bin`)**:
   - Si propones modificar los campos de `Product` o `PriceRequest`, debes mantener la lectura retrocompatible en `FastBinaryStore.kt` para no invalidar los archivos de respaldo `.bin` existentes de las tiendas.

---

## 3. Flujo de Trabajo para Colaborar

1. **Verifica las incidencias abiertas**: Revisa la pestaña de *Issues* para confirmar si el error o mejora ya fue reportado.
2. **Crea una rama descriptiva**:
   - `fix/descripcion-corta` para correcciones de errores.
   - `feat/descripcion-corta` para nuevas mejoras funcionales.
   - `perf/descripcion-corta` para optimizaciones de memoria o velocidad.
   - `docs/descripcion-corta` para mejoras en la documentación.
3. **Verifica la compilación y las pruebas locales**:
   ```bash
   gradle :app:testDebugUnitTest
   gradle assembleDebug
   ```
4. **Asegura la trazabilidad de interfaz (`testTag`)**:
   - Todo botón, campo de texto o selector interactivo nuevo debe incluir su `Modifier.testTag("nombre_en_snake_case")` y sus textos visibles deben respetar la paleta de contraste automático (`getContrastingTextColor`).
5. **Abre el Pull Request**:
   - Describe qué problema resuelve el cambio, cómo se probó y confirma que no incrementa el uso de memoria ni introduce dependencias externas.
