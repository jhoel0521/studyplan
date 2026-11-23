# Design: widget de progreso semanal 1×4 (HU-05)

## Context

Motivación y alcance en `proposal.md`; los requisitos son HU-05 (`openspec/specs/perfil-ajustes`, sin cambios) y RNF-19 (ahora 1×4 en el capítulo 3).

**Estado actual del código** (leído, no supuesto):
- `ObtenerResumenInicio.ejecutar()` devuelve `Optional<ResumenInicio>` con horas realizadas, meta, `proximaSesion` y `sesionesDeHoy`. `ObtenerResumenInicioService` obtiene la próxima de la lista ordenada `listarPendientesDesde(hoy)`: la primera cuyo fin es posterior a ahora; si no hay, la primera de la lista.
- `StudyPlanApplication` es la raíz de composición: expone `ejecutor()` (un solo hilo) y `obtenerResumenInicio()`.
- Persistencia en Room con las tablas `sesion_estudio`, `meta_semanal`, `perfil` y `materia`.
- La pantalla de inicio es `MainActivity`, que decide el arranque (perfil, bloqueo con huella o PIN).
- Ya existe una primera versión del widget (2×2 y 4×2) que este diseño reemplaza.

**Restricciones:** Java, minSdk 24; `domain` y `application` sin `android.*` (`ArquitecturaTest`); sin dependencias nuevas; `EstructuraPaquetesTest` exige cada clase en la carpeta de su épica; `ReglasEstiloLayoutTest` prohíbe colores hex en layouts.

## Goals / Non-Goals

**Goals:**
- El widget lee los mismos datos que Inicio, sin duplicar el criterio de «cuál es la próxima».
- Nunca accede a la base de datos desde el hilo principal.
- Se refresca al cambiar los datos y al cambiar de día.

**Non-Goals:**
- Las ideas fuera de alcance de `proposal.md`.
- Actualizar el widget con el proceso de la app muerto: los cambios de datos solo ocurren con la app viva; el resto lo cubre la actualización periódica.

## Decisions

**1. Las 2 próximas se obtienen ampliando `ResumenInicio`** con `proximasSesiones()` (lista de hasta 2, no modificable), calculada en `ObtenerResumenInicioService` con el mismo recorrido de `seleccionarProxima`: la primera es la misma que `proximaSesion()` y la segunda es la siguiente pendiente de la lista. Inicio no cambia. *Alternativa descartada:* un caso de uso nuevo solo para el widget, porque duplicaría la regla de «cuál es la próxima». *Coste:* se toca `domain.readmodel` y `application`, con pruebas en `ObtenerResumenInicioServiceTest`.

**2. El widget es un adaptador de entrada en `adapter.in.ui.configuracion`** (épica `configuracion`, donde vive HU-02/HU-05). Clases: `WidgetProgresoProvider` (extiende `AppWidgetProvider`), `ModeloWidgetProgreso` (qué mostrar), `DibujanteAnilloProgreso` (el anillo) y `ObservadorProgresoWidget` (cuándo refrescar).

**3. El disparador de refresco es el rastreador de invalidación de Room** sobre `sesion_estudio`, `meta_semanal`, `perfil` y `materia`, registrado una vez en `StudyPlanApplication` (ya implementado). Los avisos seguidos se agrupan. *Alternativa descartada:* solo `updatePeriodMillis`, cuyo mínimo es de 30 minutos.

**4. Lectura fuera del hilo principal con `goAsync()`** en `StudyPlanApplication.ejecutor()`; con la lectura fallida se conserva lo que el widget ya mostraba.

**5. Un solo layout 1×4**, `widget_progreso_1x4.xml`, sin umbrales de ancho: `minWidth` de 4 celdas (250 dp), `minHeight` de 1 celda (40 dp) y `resizeMode` horizontal. En Android 12 o posterior, una variante `xml-v31` añade `targetCellWidth=4`, `targetCellHeight=1` y la vista previa. Se descarta elegir entre dos layouts por ancho: ya no hay dos tamaños.

**6. Distribución de la fila** (de izquierda a derecha): logo (`@mipmap/ic_launcher`, el ícono real de la app, 36 dp, decorativo) · anillo pequeño con el porcentaje dentro y, al lado, «Esta semana» y «horas / meta» · hasta 2 líneas de próxima materia, cada una con su barra de color, y «materia · día · hora». Con una fila de alto 1 solo caben 2 líneas cortas de texto. Si hay una sola próxima, se muestra una línea.

**7. El anillo es un `Bitmap` dibujado con `Canvas`** (`DibujanteAnilloProgreso`, ~40 dp): `RemoteViews` no admite vistas propias ni un `ProgressBar` circular fiel al del cronómetro.

**8. `ModeloWidgetProgreso` decide el contenido y es Java puro, probado en JVM.** Estado `NORMAL`, `SIN_PERFIL`, `SIN_META` o `SIN_SESION`; porcentaje acotado a 0–100; lista de hasta 2 próximas. Sin perfil → «Abre StudyPlan para empezar»; meta 0 → «Define tu meta semanal»; sin próximas → «Sin sesiones próximas». Los textos con formato salen de `strings.xml` (es/en); las horas usan `FormatoHoras`.

**9. Tocar el widget abre `MainActivity`** con un `PendingIntent` `FLAG_IMMUTABLE`; el bloqueo (HU-04) lo resuelve el flujo de arranque. Todo el widget es una sola área táctil.

**10. Actualización periódica de 30 minutos** (`updatePeriodMillis=1800000`, el mínimo del sistema) para el cambio de día y de semana. *Aceptado:* tras la medianoche puede mostrar el dato viejo hasta 30 minutos.

**11. Sin permisos ni dependencias nuevas.** Solo el `receiver` con `APPWIDGET_UPDATE` y el `meta-data` del `appwidget-provider`.

## Risks / Trade-offs

- [Una fila de alto 1 es muy justa para dos materias y la meta] → textos cortos de 1 línea, `ellipsize`, y comprobación en el teléfono real del Product Owner; si no cabe, se reduce a 1 próxima en otro ajuste.
- [Cada lanzador mide las celdas distinto] → se declaran tamaños mínimos y se prueba en el teléfono.
- [Cambiar de tema del sistema no refresca el widget al instante] → se corrige en el siguiente refresco (máximo 30 minutos); se anota como limitación.
- [El observador de Room dispara en cada escritura de esas tablas] → el refresco es una lectura pequeña y los avisos seguidos se agrupan.
- [Ampliar `ResumenInicio` toca el núcleo] → solo se añade un método y una lista; las pruebas existentes de Inicio deben seguir en verde sin cambios.
- [No se pueden correr las instrumentadas en la máquina de la IA (sin KVM)] → la comprobación real es la prueba en el teléfono (tarea 5.1).

## Migration Plan

Sin migración de datos ni de esquema. El widget 2×2/4×2 ya instalado en el teléfono desaparece al actualizar: el usuario lo vuelve a agregar. Para revertir basta quitar el `receiver` y las clases del widget; `proximasSesiones()` puede quedarse.

## Open Questions

- Texto exacto de los estados vacíos: el Product Owner puede ajustarlo en la revisión sin cambiar el enfoque.
