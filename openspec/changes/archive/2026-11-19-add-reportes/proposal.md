# Proposal

## Why

La épica **E8 (`reportes`: `HU-32`, `HU-33`)** forma parte del alcance MVP (`E1–E9`) y ofrece una vista comparativa semanal o mensual por materia (horas planificadas, reales, cumplimiento y sesiones no realizadas) exportable en **PDF** y **CSV** (`UTF-8`) compatible con **Google Sheets** y **Microsoft Excel en Windows**. Adicionalmente, la auditoría de **E7 (`add-estadisticas`)** identificó dos defectos reales frente a sus requisitos vigentes (`EstadisticasActivity` omite las sesiones reprogramadas en la fracción de cumplimiento de `HU-31` y `EventoEstudioAdapter` no muestra `evento.topico()` en la línea de tiempo de `HU-39`), junto con fechas frágiles en `EstadisticasEspressoTest` al ejecutarse en lunes o domingos; se corrige y cierra E7 en un commit separado al inicio del plan antes de construir E8.

## What Changes

- **Corrección y cierre de E7 (commit separado `fix(HU-31,HU-39): ...` al concluir la Sección 1 de `tasks.md`, sin alterar las specs aprobadas):**
  - Corregir `EstadisticasActivity` (`HU-31`) para mostrar `e.totalCerradas()` en el denominador de la fracción `(x de y)`, manteniendo consistencia exacta con la tasa calculada en el dominio, y añadir cobertura en `EstadisticasEspressoTest` con `reprogramadas > 0`.
  - Actualizar `EventoEstudioAdapter` (`HU-39`) para mostrar el tópico localizado (`TopicosUi.etiqueta`) dentro de `textoNotaEvento` (formato `"tópico · nota"` cuando también exista nota), sin alterar la estructura del layout, y añadir verificación en `TecnicasEstudioEspressoTest`.
  - Estabilizar las fechas de `EstadisticasEspressoTest` sembrando sesiones cerradas y futuras en `hoy` sin depender de `registrarPasada` (que exige `1..3` días de antigüedad y falla los lunes), usando el desfase horario actual del reloj y cubriendo cualquier día de la semana o cambio de mes.
  - Añadir el diagrama de clases de estadísticas (`diagrama_clases_estadisticas.png`) en `docs/build/diagramas_clases.py` y actualizar `docs/build/cotejo_clases.py`.
- **Reporte semanal o mensual por materia (`reportes`: `HU-32`):**
  - Añadir la pantalla **`ReportesActivity`** (`actividad` protegida por `ActividadProtegida`, layouts `layout/` y `layout-w600dp/`, Material 3, bilingüe `es`/`en`, `RNF-06`, `RNF-08`, `RNF-10`, `RNF-12`) con selector `Semana | Mes`, navegación `< anterior | siguiente >`, tarjeta resumen de totales del periodo, lista por materia (horas planificadas, reales, `%` cumplimiento y sesiones no realizadas) y estado claro «Sin datos» con acciones de exportación deshabilitadas cuando no haya actividad.
  - Ubicar el acceso contextual mediante un **botón delineado visible `botonAbrirReportes`** dentro del desplazamiento de `EstadisticasActivity` (heredando el periodo y fecha foco activos sin comprimir el gráfico de anillo) y un **atajo `accion_reportes` en la barra superior de `MainActivity`** (`menu_inicio.xml`), conservando las cuatro pestañas fijas de la barra inferior (`DestinoPrincipal`).
  - Reutilizar el modelo existente **`DesgloseMateriaPeriodo`** y ampliar **`CalculadorEstadisticas`** junto al nuevo agregado **`ReporteEstudio`** y caso de uso **`GenerarReporteEstudio`** (`GenerarReporteEstudioService`):
    - Extraer **`SesionEstudio.minutosPlanificadosEnRango`** como fuente única `RN-31` de reparto en medianoche compartida por Calendario, Estadísticas y Reportes.
    - Incluir toda materia con minutos reales en `TramoDiario`, horas planificadas en el rango o sesiones `NO_REALIZADA` atribuidas al periodo de `sesion.fecha()` (incluidas materias con `0` h reales, «Estudios varios» y archivadas).
    - Excluir sesiones `REPROGRAMADA` de horas planificadas (`RN-05`), evitar división por cero (`cumplimiento = null` / `—` cuando `planificadas == 0`) y devolver `esSinDatos()` cuando no existan filas activas.
- **Exportación PDF / CSV y compartir en Android (`reportes`: `HU-33`):**
  - Implementar en `domain` **`FormatoExportacionReporte`** y **`GeneradorCsvReporte`** en Java puro: CSV **RFC 4180** en **UTF-8 con BOM (`0xEF 0xBB 0xBF` / `\uFEFF`)**, separador coma `,`, saltos `\r\n`, decimales con punto `.` (`Locale.ROOT`), mitigación `CWE-1236` (prefijo `'` ante campos de usuario iniciados por `=`, `+`, `-`, `@`, `\t` o `\r`), encabezados en fila 1 con unidades, una fila por materia y fila final de totales; compatible directamente con **Google Sheets** e importable en **Excel para Windows** mediante *Datos > Desde texto/CSV* preservando tildes y eñes.
  - Definir el puerto de salida **`ExportadorReporte`** (`application.port.out`) con tipo resultado propio **`ResultadoExportacion`** (independiente de `ErrorValidacion` de negocio) y método **`limpiarTemporales()`** invocado desde `BorrarDatosService` al borrar datos del teléfono (`HU-38`).
  - Implementar **`ExportadorReporteSistema`** (`adapter.out.system`) usando **`android.graphics.pdf.PdfDocument`** nativo (A4 sin librerías externas) y archivos temporales en `cacheDir/reportes/` con retención de los 3 últimos archivos, resolviendo cadenas en el idioma activo (`ContextCompat.getContextForLanguage`) y entregando el **`content://` URI** mediante **`androidx.core.content.FileProvider`** (`AndroidManifest.xml` + `res/xml/file_paths.xml`) para abrir el selector (`Intent.ACTION_SEND` + `ClipData` + `FLAG_GRANT_READ_URI_PERMISSION`) sin conexión ni permisos externos.

**Fuera de alcance:**
- Recordatorios previos de sesión (`E9`: `HU-34`), widget de inicio (`HU-05`) y respaldo/restauración en Google Drive (`E10`: `HU-35` a `HU-38`, diferido fuera del MVP).
- Selector de delimitador CSV configurable (`';'` frente a `','`), doble variante CSV o generación binaria `.xlsx`: se adopta un único formato CSV estándar RFC 4180 en UTF-8 con BOM.
- Exportación del historial detallado de eventos o filtros por materia dentro del reporte (`HU-32` cubre totales y tabla por materia en periodo semanal/mensual).
- Nuevas librerías de terceros en `gradle/libs.versions.toml` (`PdfDocument` y `FileProvider` ya forman parte del SDK/AndroidX vigentes).

## Capabilities

### New Capabilities
- Ninguna (la capacidad `reportes` ya existe en `openspec/specs/`).

### Modified Capabilities
- `reportes`: precisión de escenarios para `HU-32` (inclusión de materias con `0` h reales y sesiones planificadas/no realizadas, atribución nocturna de `NO_REALIZADA` y reparto `RN-31`, sesiones sin plan con `—`, exclusión de `REPROGRAMADA`, coherencia de `HU-11`/`PENDIENTE` y estado «Sin datos», acceso desde `Estadísticas` e `Inicio`) y `HU-33` (dos acciones de exportación PDF/CSV inactivas sin datos, PDF nativo offline vía `FileProvider` y CSV RFC 4180 UTF-8 con BOM compatible con Google Sheets y asistente de Excel en Windows).

## Impact

- **Cierre E7 (`app/` y `docs/build/`):** `EstadisticasActivity`, `EventoEstudioAdapter`, `item_evento_estudio.xml`, `EstadisticasEspressoTest`, `TecnicasEstudioEspressoTest`, `diagramas_clases.py`, `cotejo_clases.py`, `04-diseno-del-software.md`.
- **E8 Reportes (`app/`):**
  - `domain`: `SesionEstudio` (`minutosPlanificadosEnRango`), `CalculadorEstadisticas` (`calcularReporte`), `DesgloseMateriaPeriodo` (reutilizado), `ReporteEstudio`, `FormatoExportacionReporte`, `GeneradorCsvReporte`.
  - `application`: `GenerarReporteEstudio` (`port.in`), `GenerarReporteEstudioService` (`service`), `ExportadorReporte` y `ResultadoExportacion` (`port.out`), ampliación de `ConsultarCalendarioService` (reuso de `minutosPlanificadosEnRango`) y `BorrarDatosService` (`limpiarTemporales()`).
  - `adapter.out.system`, `AndroidManifest.xml` y `res/`: `ExportadorReporteSistema` (`PdfDocument` + CSV en `cacheDir/reportes/`), `FileProvider`, `res/xml/file_paths.xml`.
  - `adapter.in.ui` y `res/`: `ReportesActivity` (`extends ActividadProtegida`), `ReportesViewModel`, `ReportesAdapter`, ajuste en `EstadisticasActivity` (`contenido_estadisticas.xml`), `MainActivity` (`menu_inicio.xml`), `FabricaViewModels`, `StudyPlanApplication`, `activity_reportes.xml` (`layout/` y `layout-w600dp/`), `item_reporte_materia.xml`, `ic_reporte.xml` y cadenas `es`/`en`.
- **RNF afectados:** `RNF-02`, `RNF-06`, `RNF-07`, `RNF-08`, `RNF-09`, `RNF-10`, `RNF-11`, `RNF-12`, `RNF-14`, `RNF-15`, `RNF-16` y `RNF-17`.
- **Documentación y auditoría:** actualización de `docs/build/diagramas_clases.py`, `docs/build/cotejo_clases.py`, `docs/build/diagramas.py`, `docs/documento/capitulos/04-diseno-del-software.md` y `docs/auditoria-ia/bitacora.md`.
