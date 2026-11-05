# Guía de Contribución

Gracias por tu interés en colaborar con este proyecto. Este documento describe las pautas técnicas, convenciones de código y pasos necesarios para proponer mejoras o correcciones.

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
- **Sin librerías pesadas de persistencia**: Toda persistencia local se realiza en `FastBinaryStore` mediante `DataOutputStream` y `DataInputStream`. No agregues Room, SQLite wrappers ni ORMs externos.
- **Componentes de interfaz planos sobre grilla de 8.dp**: Utiliza los componentes de `FastComponents.kt` (`FastButton`, `FastOutlinedButton`, `FastChip`, `FastTextField`, `FastNavIconItem`) con superficies planas de color sólido, sin bordes ni líneas divisorias, y espaciados múltiplos de `8.dp` (`8.dp`, `16.dp`, `24.dp`, `32.dp`, `40.dp`, `48.dp`).
- **Tipografía general Times New Roman**: Mantén el uso de `TimesNewRomanFontFamily` (`R.font.tinos`) tanto en `Type.kt` como en el renderizado de bitmaps en `ImageUtils.kt`.
- **Aritmética monetaria entera**: Los precios en Guaraníes deben manejarse como `Long` y calcularse a través de `MathUtils`. No utilices `Float` o `Double` para almacenar importes monetarios.
- **Sin imágenes persistentes en el Historial ni en las Boletas de Distribuidor**: Las capturas fotográficas se procesan únicamente en memoria y caché temporal para generar la ficha `16:9` al momento de compartir; nunca deben guardarse en `store_requests_v1.bin` ni en `store_distributors_v1.bin`.
- **Etiquetas de prueba (`testTag`)**: Todo botón, campo de entrada o selector interactivo nuevo debe incluir su `Modifier.testTag("nombre_en_snake_case")` correspondiente.

## Flujo de Trabajo para Pull Requests

1. Crea una rama descriptiva a partir de `main`:
   - `feat/descripcion-corta` para nuevas funcionalidades.
   - `fix/descripcion-corta` para corrección de errores.
   - `docs/descripcion-corta` para mejoras en la documentación.
2. Asegúrate de que el código compile sin advertencias críticas y pase los chequeos locales:
   ```bash
   gradle :app:testDebugUnitTest assembleDebug
   ```
3. Si modificas el formato binario de `FastBinaryStore`, incrementa el encabezado mágico (`FBC1`, `FBD2`, `STR3`) y conserva el método de lectura de versiones previas para no romper archivos `.bin` existentes.
4. Abre el Pull Request detallando el problema resuelto, los archivos modificados y cómo fue verificado.

## Acuerdo de Licencia

Al enviar una contribución a este repositorio, aceptas que tus aportes se distribuyan bajo los mismos términos de la [Licencia de Código Disponible para Uso No Comercial](LICENSE) que rige al proyecto.
