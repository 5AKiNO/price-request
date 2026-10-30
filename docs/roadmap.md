# Hoja de Ruta del Producto (Roadmap)

Este documento presenta la evolución de **Sistema de Precios**, los hitos ya entregados en producción y las líneas de trabajo planificadas para futuras versiones manteniendo el compromiso de bajo consumo de memoria RAM.

---

## 1. Hitos Completados

### Fase 1: Núcleo Comercial y Cálculo de Márgenes (Completado)
- [x] Gestión local de catálogo con código principal, código alternativo, descripción, costo y precio de venta.
- [x] Cálculo automático de precio de venta con redondeo comercial al múltiplo de `500` superior.
- [x] Los 5 flujos de Solicitud de Cambio de Precios (Falta Registrar, Costo Subió, Costo Bajó, Excede % deseado, Inferior al % deseado).
- [x] Contabilización desglosada por variedad (`Stock` y `Recarga` opcionales por ítem).

### Fase 2: Motor Binario V2 y Exportación Dual (Completado)
- [x] Reemplazo de almacenamiento relacional pesado por `FastBinaryStore` (`.bin` con escritura atómica `fsync` y colas `Channel.CONFLATED`).
- [x] Importación y exportación de respaldos `.bin` con opciones **Fusionar** y **Reemplazar todo**.
- [x] Exportación a WhatsApp en **Texto Plano (TXT)** y en **Imagen Render Monocroma (`RGB_565`)**.
- [x] Buscador multicriterio instantáneo por tokens con preservación de la letra `ñ`.

### Fase 3: Interfaz Plana de Costo Cero y Re-edición de Historial (Completado - v2.4.0)
- [x] Sustitución de componentes pesados por primitivas planas (`FastTextField`, `FastChip`, `FastButton`, `FastCheckBox`, `FastNavIconItem`).
- [x] Actualización de la escala de márgenes comerciales a `20%`, `25%`, `30%`, `35%` y `50%` (incluyendo filtros rápidos de `30%` y `35%` en el catálogo).
- [x] Corrección de reglas de validación en Solicitudes Tipo 4 (`Venta Nueva < Venta Anterior`) y Tipo 5 (`Venta Nueva > Venta Anterior`).
- [x] Barra de navegación principal de solo iconos sin texto.
- [x] Re-edición completa de solicitudes desde el Historial actualizando el registro existente en su lugar.
- [x] Eliminación de imágenes en el Historial y limpieza total de dependencias externas no utilizadas (`Room`, `KSP`, `Retrofit`, `OkHttp`, `Moshi`, `Coil`, `Firebase`).

---

## 2. Próximos Hitos Planificados

### Corto Plazo (`v2.5.x`)
- [ ] **Ajuste masivo de precios por lote en Catálogo**: Permitir aplicar un nuevo porcentaje (`20%`, `25%`, `30%`, `35%`, `50%`) a todos los productos seleccionados en una sola acción atómica.
- [ ] **Filtro por rango de fecha en Historial**: Filtrar solicitudes del día actual, últimos 7 días o mes en curso sin recargar toda la lista.
- [ ] **Detección de códigos duplicados al vuelo**: Aviso instantáneo dentro de `ProductDialog` mientras se escribe un código que ya pertenece a otro artículo.

### Mediano Plazo (`v2.6.x`)
- [ ] **Importación directa desde texto tabulado (CSV/TSV ligero)**: Parser en streaming con buffer de `8 KB` para migrar planillas de proveedores directamente al formato `.bin` sin usar librerías externas.
- [ ] **Plantillas de encabezado personalizables por sucursal**: Cambio rápido entre varios nombres de local o depósitos configurados.
