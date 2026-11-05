# Guía de Contribución

Gracias por tu interés en colaborar con **Sistema de Precios**. Este documento describe las pautas técnicas, convenciones de código y pasos necesarios para proponer mejoras o correcciones.

## Configuración del Entorno de Desarrollo

1. Utiliza **Android Studio Ladybug (2024.2.1)** o una versión posterior compatible con AGP `8.7+` y Kotlin `2.0+`.
2. Configura **JDK 17** como runtime de Gradle en las preferencias del proyecto.
3. Asegúrate de contar con el SDK de Android nivel `36` instalado.
4. Clona el repositorio y ejecuta una compilación inicial para verificar el entorno:
   ```bash
   gradle assembleDebug :app:testDebugUnitTest
   ```

## Reglas de Arquitectura y Rendimiento

Para mantener el bajo uso de memoria RAM, la claridad estructural y la fluidez en dispositivos de gama de entrada, toda contribución debe respetar los siguientes principios:

- **Estructura de paquete plana en `com.example`**: Todos los archivos Kotlin del módulo `app` se ubican directamente en `app/src/main/java/com/example/` bajo `package com.example`. No introduzcas subcarpetas anidadas artificiales.
- **Sin librerías pesadas de persistencia ni capas ORM**: Toda persistencia local se realiza en `FastBinaryStore` mediante buffers lineales reutilizables, colas `Channel.CONFLATED` y escritura atómica con `fsync`. No agregues Room, SQLite wrappers, Retrofit, Moshi ni Coil.
- **Primitivas de interfaz planas (`FastComponents`) sobre grilla de `8.dp`**: Utiliza `FastTextField`, `FastChip`, `FastButton`, `FastOutlinedButton`, `FastCheckBox` y `FastNavIconItem` para evitar el costo de `SubcomposeLayout`, bordes superpuestos y animaciones de elevación en listas densas.
- **Tipografía general Times New Roman**: Mantén el uso de `TimesNewRomanFontFamily` (`R.font.tinos`) tanto en `Type.kt` como en el renderizado de bitmaps (`RGB_565`) en `ImageUtils.kt`.
- **Aritmética monetaria entera**: Los precios en Guaraníes deben manejarse como `Long` y calcularse a través de `MathUtils` (con redondeo `HALF_UP` y redondeo comercial a múltiplos de `500 Gs.`). No utilices `Float`, `Double` ni `DecimalFormat` para procesar o formatear importes monetarios.
- **Sin imágenes persistentes en Historial ni en Boletas de Distribuidor**: Las capturas fotográficas se procesan únicamente en memoria y caché temporal (`cacheDir/shared_images`) para su despacho inmediato por WhatsApp; nunca deben guardarse en `store_requests_v1.bin` ni en `store_distributors_v1.bin`.
- **Etiquetas de prueba (`testTag`)**: Todo botón, campo de entrada o selector interactivo nuevo debe incluir su `Modifier.testTag("nombre_en_snake_case")` correspondiente.

## Flujo de Trabajo para Pull Requests

1. Crea una rama descriptiva a partir de `main`:
   - `feat/descripcion-corta` para nuevas funcionalidades.
   - `fix/descripcion-corta` para corrección de errores.
   - `docs/descripcion-corta` para mejoras en la documentación.
2. Asegúrate de que el código compile sin advertencias críticas y pase las pruebas unitarias:
   ```bash
   gradle :app:testDebugUnitTest assembleDebug
   ```
3. Si modificas el formato binario de `FastBinaryStore`, incrementa la cabecera mágica correspondiente (`STP3`, `STR4`, `FBD3`, `STC2`) y conserva el bloque de lectura de versiones previas para no romper respaldos `.bin` existentes.
4. Registra los cambios relevantes en `CHANGELOG.md` respetando el formato de Keep a Changelog y Versionado Semántico.

## Acuerdo de Licencia

Al enviar una contribución a este repositorio, aceptas que tus aportes se distribuyan bajo los mismos términos de la [Licencia de Código Disponible para Uso No Comercial](LICENSE) que rige al proyecto.
