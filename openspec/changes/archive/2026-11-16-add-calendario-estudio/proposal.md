# Proposal

## Why

Las épicas E1 (configuración e inicio), E2 (gestión de materias) y E3 (sesiones de estudio) ya están implementadas y archivadas en `openspec/specs/`, pero la pestaña «Calendario» (`DestinoPrincipal.CALENDARIO`) sigue deshabilitada (`null`). Además, `MainActivity` es hoy el único punto de entrada a `SesionDetalleActivity` y solo lista sesiones de hoy y la próxima `PENDIENTE`, por lo que las sesiones `NO_REALIZADA` de días anteriores quedan inaccesibles desde la UI para su registro tardío (`HU-13`) o reprogramación (`HU-14`), y tampoco es visible todavía el color actualizado de una materia en el calendario (`HU-07`). Por prioridad del Product Backlog (19 puntos *Must* + 6 *Should*) y prioridad académica del docente, la siguiente épica es **E4: Calendario de estudio** (`HU-16` a `HU-21`, `RNF-02`, `RNF-04`, `RNF-06`, `RNF-09`, `RNF-16`, `RNF-17`, `RNF-19`).

## What Changes

- **Habilitar la pestaña Calendario (`DestinoPrincipal.CALENDARIO` → `CalendarioActivity`):** pantalla protegida por `ActividadProtegida` (`HU-04`, `RNF-12`) con barra de navegación reutilizable (`[Inicio, Calendario]` en la pila de retroceso) y variante `layout-w600dp/`.
- **Selector de vista (`Día | Semana | Mes`) con memoria local y conservación de foco:** abre por defecto en **`Semana`** la primera vez, recuerda en preferencias locales la última vista dejada y conserva el periodo/fecha de enfoque al cambiar entre vistas (si en `Mes` hay un día seleccionado dentro del mes visible, pivota sobre él; si no —p. ej., tras navegar dos meses atrás—, usa la primera semana o el día 1 de ese mes sin perder la ubicación).
- **Vista mensual (`HU-16`):** cuadrícula nativa `RecyclerView` con `GridLayoutManager` de 7 columnas (lunes a domingo, `RN-13`), celdas de meses adyacentes en tono atenuado, resaltado de `hoy`, indicadores de color de materias con sesión y suma compacta de horas por día, navegación por flechas y deslizamiento horizontal, y lista inferior (`RecyclerView`) que al tocar un día muestra sus sesiones con materia, horario, duración e icono + etiqueta textual de estado (`RNF-06`). Prevalece `HU-16` sobre `flujo_pantallas.png`.
- **Vista semanal tipo horario (`HU-17`):** 7 columnas (L–D, `RN-13`) ajustadas al ancho de pantalla (aplicando la excepción de `AGENTS.md` §6.2 para celdas de cuadrícula y bloques proporcionales en pantallas pequeñas, `RNF-19`) sobre una rejilla horaria nativa de 24 horas (`00:00`–`24:00`) con auto-scroll inicial en `[min(horaInicio) - 2 h, max(horaFin) + 2 h]` (acotado a `00:00`–`24:00`, o `08:00` si no hay sesiones), tarjetas `MaterialCardView` proporcionales a la duración con el color de la materia e indicadores de estado (`HU-19`), línea en la hora actual en la columna de `hoy` y navegación semanal por flechas o deslizamiento.
- **Vista diaria (`HU-18`):** línea de tiempo vertical de 24 horas (`00:00`–`24:00`) con auto-scroll `± 2 h`, bloques `MaterialCardView` proporcionales con materia, horario, duración e icono + etiqueta de estado (`RNF-06`), y línea en la hora actual cuando el día visible es `hoy`.
- **Indicadores de estado y leyenda (`HU-19`, `RNF-06`):** cada sesión mostrada en cualquiera de las tres vistas incluye icono y etiqueta propios de su estado (`EstadoSesionVisual`: `PENDIENTE`, `EN_CURSO`, `COMPLETADA`, `COMPLETADA_TARDIA`, `NO_REALIZADA`, `REPROGRAMADA`) sin depender solo del color, además de un acceso en cabecera para abrir la **Leyenda de estados**.
- **Crear sesión desde el calendario mediante pulsación larga y sostenida (`HU-20`):** mantener pulsado (*long press*) un día en `Mes` o una franja horaria libre en `Semana`/`Día` cuya fecha sea igual o posterior a `hoy` (`fecha >= hoy`) abre `SesionFormularioActivity` en `MODO_PLANIFICAR` con esa fecha precargada (y en `Semana`/`Día`, con `horaInicio` de la franja y `horaFin = +60 min` sugerida). En fechas pasadas (`< hoy`) no se dispara creación desde el calendario. Un toque simple sobre cualquier sesión existente (pasada, de hoy o futura) abre `SesionDetalleActivity`, resolviendo el hueco de E3 sobre sesiones `NO_REALIZADA` pasadas (`HU-13`, `HU-14`).
- **Filtro por materia (incluyendo archivadas) y resumen de cabecera (`HU-21`, `RN-11`):** selector de materia (`Todas` o materia concreta) con opción para incluir materias archivadas (`HU-08`) y resumen en cabecera del periodo visible (`Plan X h · Real Y h`, excluyendo en `Plan` las sesiones `REPROGRAMADA` y registros pasados sin planificar, y sumando en `Real` las sesiones `COMPLETADA` y `COMPLETADA_TARDIA`).
- **Consulta por rango en Room v3 (`listarPorRango`) sin migración:** nuevo método `listarPorRango(Fecha, Fecha)` en `SesionRepository`, `SesionRepositoryRoom` y `SesionDao` sobre la tabla `sesion_estudio` existente (`version = 3`, índice `index_sesion_estudio_fecha`), cargando el rango del calendario en una sola consulta fuera del hilo principal (`RNF-02`).
- **Cero librerías externas de calendario y cero permisos nuevos:** implementación 100 % en Views XML nativas y Java usando directamente `Fecha` y `Hora` del dominio; sin `coreLibraryDesugaring`, sin dependencias externas adicionales y sin cambios en permisos de `AndroidManifest.xml`.
- **Documentación, diagramas y trazabilidad:** corrección de `flujo_pantallas.png` en `docs/build/diagramas.py` (prevalece `HU-16` y se mantiene `HU-34` en E9), actualización de `diagramas_clases.py`, `cotejo_clases.py`, capítulos 3 y 4 y `docs/auditoria-ia/bitacora.md`.

**Fuera de alcance:**
- Arrastrar o redimensionar bloques de sesión en el calendario (no corresponde a ninguna HU).
- Recordatorio antes de la sesión (`HU-34`, Épica E9 `recordatorios`) ni columnas nuevas en SQLite (sin migración de Room v3).
- Ejecución de sesión en vivo (`EN_CURSO`), cronómetro, técnica Pomodoro, modo No molestar y eventos de estudio (`HU-22` a `HU-24`, `HU-39` a `HU-44`, Épica E5 `tecnicas-estudio`), junto con todos los permisos Android de E5/E9 (`POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY`, `VIBRATE`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`).
- Metas semanales por materia o por semana (`HU-25` a `HU-28`, Épica E6), estadísticas con MPAndroidChart (E7), reportes PDF/CSV (E8), widget (`HU-05`) y respaldo en Drive (E10).

## Capabilities

### New Capabilities
- Ninguna (todas las capacidades forman parte de `openspec/specs/`).

### Modified Capabilities
- `calendario-estudio`: implementa `HU-16`, `HU-17`, `HU-18`, `HU-19`, `HU-20` y `HU-21`, y actualiza en delta spec los escenarios de `HU-20` (creación mediante pulsación larga y sostenida exclusivamente en `fecha >= hoy` en las tres vistas) y `HU-21` (opción para incluir materias archivadas en el filtro).

*(Sin cambios de requisitos —no requieren delta spec—: `gestion-materias` completa el escenario pendiente de `HU-07` «el calendario muestra el nuevo color» y mantiene visible el historial archivado en filtros según `HU-08`; `sesiones-estudio` habilita desde la UI el acceso a sesiones `NO_REALIZADA` de días anteriores para `HU-13` y `HU-14`.)*

## Impact

- **Código Java (`app/`):**
  - `domain`: `VistaCalendario`, `FiltroCalendario`, `PeriodoCalendario`, `CalculadorRangoHorario`, `CeldaDiaMes`, `ItemSesionCalendario`, `BloqueHorarioCalendario`, `ResumenPeriodoCalendario`, `DatosCalendario`.
  - `application.port.out`: ampliación de `SesionRepository` con `listarPorRango(Fecha desde, Fecha hasta)`.
  - `application.port.in` y `application.service`: `ConsultarCalendario` y `ConsultarCalendarioService`.
  - `adapter.out.persistence`: ampliación de `SesionDao` y `SesionRepositoryRoom` con `listarPorRango` (sobre Room v3, sin migración).
  - `adapter.in.ui`: `DestinoPrincipal`, `CalendarioActivity`, `CalendarioViewModel`, `CalendarioMesAdapter`, `VistaRejillaHorario` / renderizado nativo de franjas y bloques `MaterialCardView`, `PreferenciaVistaCalendario`, ampliación de `SesionFormularioActivity` (`EXTRA_HORA_INICIO`, `EXTRA_HORA_FIN`), `FabricaViewModels`, `StudyPlanApplication`, `AndroidManifest.xml`, `values/strings.xml`, `values-en/strings.xml` y layouts XML (`layout/` y `layout-w600dp/`).
- **Dependencias:** ninguna dependencia nueva en `gradle/libs.versions.toml` ni en `app/build.gradle.kts`.
- **Base de datos y permisos:** `StudyPlanDatabase` se mantiene en **versión 3** sin migración; `AndroidManifest.xml` mantiene cero `<uses-permission>`.
- **Documento académico y scripts:** `docs/build/diagramas.py`, `docs/build/diagramas_clases.py`, `docs/build/cotejo_clases.py`, capítulo 3 (si aplica por ajuste de criterio en `HU-20`/`HU-21`), capítulo 4 y `docs/auditoria-ia/bitacora.md`.
