# Design

## Context

Este documento define el diseño técnico del cambio [`add-reportes`](proposal.md), que cierra las desviaciones detectadas en la épica **E7** (`estadisticas`) e implementa la épica **E8** (`reportes`, [spec.md](specs/reportes/spec.md)).

### Definición de conceptos locales del proyecto
- **Épicas e historias citadas:** **E7** (`estadisticas`: `HU-29` horas totales/variación, `HU-30` distribución por materia, `HU-31` tasa de cumplimiento; [spec E7](../../specs/estadisticas/spec.md)) y **E8** (`reportes`: `HU-32` reporte de periodo, `HU-33` exportación PDF/CSV; [spec E8](../../specs/reportes/spec.md)); **E5** (`tecnicas-estudio`: `HU-22` cronómetro/0 min, `HU-39` línea de tiempo de eventos, `HU-40` aviso al salir; [spec E5](../../specs/tecnicas-estudio/spec.md)); **HU-04** (bloqueo biométrico/PIN), **HU-11** (registro de sesión pasada), **HU-38** (borrar datos del teléfono).
- **Reglas de negocio y no funcionales ([AGENTS.md](../../../AGENTS.md)):** `RN-05` (sesiones `REPROGRAMADA` no ocupan horario), `RN-08` (vencer sesiones `PENDIENTE` anteriores a hoy como `NO_REALIZADA`), `RN-31` (corte estricto diario en medianoche usando `TramoDiario`), `RNF-06` (accesibilidad sin depender solo de color, controles `48dp`), `RNF-07` (tema claro/oscuro), `RNF-08` (bilingüe `es`/`en`), `RNF-10` (preservación en rotación), `RNF-11` (offline sin `INTERNET`), `RNF-12` (`ActividadProtegida` / `FLAG_SECURE`, sin BD en hilo principal), `RNF-14` (`Resultado` sin excepciones de validación), `RNF-15` (núcleo Java puro probado en JVM sin `android.*`), **W8** (barra inferior fija de 4 pestañas).
- **Piezas existentes reutilizadas:**
  - [TramoDiarioRepository](../../../app/src/main/java/io/github/jhoel0521/studyplan/application/port/out/TramoDiarioRepository.java): suma minutos reales por día local (`RN-31`).
  - [SesionEstudio](../../../app/src/main/java/io/github/jhoel0521/studyplan/domain/SesionEstudio.java) y [EstadoSesion](../../../app/src/main/java/io/github/jhoel0521/studyplan/domain/EstadoSesion.java): patrón State donde `estado.ocupaHorario()` devuelve `false` en `EstadoReprogramada` y `true` en los demás estados; `registrarPasada(...)` (`HU-11`) fija `planFinUtcMilis != null` (`tienePlan() == true`, limitación heredada de Room v3 documentada en [ConsultarCalendarioService](../../../app/src/main/java/io/github/jhoel0521/studyplan/application/service/ConsultarCalendarioService.java#L39-L42)).
  - [DesgloseMateriaPeriodo](../../../app/src/main/java/io/github/jhoel0521/studyplan/domain/DesgloseMateriaPeriodo.java): modelo de dominio creado en E7 como base compartida para `HU-30` y `HU-32` (contiene `minutosReales()`, `horasReales()`, `minutosPlanificadosDeSesiones()`, `sesionesNoRealizadas()` y `cumplimientoPlanificado()`).
  - [EstadisticasPeriodo](../../../app/src/main/java/io/github/jhoel0521/studyplan/domain/EstadisticasPeriodo.java): expone `totalCerradas()` (`completadas + completadasTardias + noRealizadas + reprogramadas`) y mantiene `distribucionMaterias()` filtrada a `minutosReales > 0` para `HU-30`.
  - [Resultado](../../../app/src/main/java/io/github/jhoel0521/studyplan/domain/Resultado.java) y [ErrorValidacion](../../../app/src/main/java/io/github/jhoel0521/studyplan/domain/ErrorValidacion.java): tipo de retorno reservado exclusivamente a validaciones de negocio del dominio.
  - [ActividadProtegida](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ActividadProtegida.java) y [ContadorActividades](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ContadorActividades.java): controlan bloqueo `HU-04`, detección de salida `HU-40` y carga diferida en `alEstarListo()`.
  - [BorrarDatosService](../../../app/src/main/java/io/github/jhoel0521/studyplan/application/service/BorrarDatosService.java): ejecuta el borrado total local (`HU-38`).
  - Herramientas de documentación: [diagramas_clases.py](../../../docs/build/diagramas_clases.py), [diagramas.py](../../../docs/build/diagramas.py) y [cotejo_clases.py](../../../docs/build/cotejo_clases.py).

### Estado inicial y brechas a resolver
1. **Cierre de E7:** [EstadisticasActivity](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadisticasActivity.java#L226) omite `e.reprogramadas()` en `(x de y)`; [EventoEstudioAdapter](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EventoEstudioAdapter.java#L67-L77) no muestra `evento.topico()` en la línea de tiempo de `SesionDetalleActivity`; [EstadisticasEspressoTest](../../../app/src/androidTest/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadisticasEspressoTest.java#L56-L98) falla en lunes (`registrarPasada` rechaza `diasHasta == 0`), fin de mes y domingos (`hoy.sumarDias(1)`); y `diagramas_clases.py` / `cotejo_clases.py` carecen del diagrama de estadísticas.
2. **E8 (`reportes`):** falta la pantalla y caso de uso de reporte con materias que tengan `0` h reales pero sí horas planificadas o `NO_REALIZADA`, el reparto uniforme de minutos planificados nocturnos en `domain`, el generador CSV seguro RFC 4180 + BOM UTF-8, el exportador PDF nativo y el `FileProvider` en [AndroidManifest.xml](../../../app/src/main/AndroidManifest.xml).

## Goals / Non-Goals

### Goals
- Dejar E7 conforme a `HU-31` y `HU-39` en un commit separado, con `EstadisticasEspressoTest` ejecutable todos los días de la semana y `cotejo_clases.py` al día.
- Reutilizar `DesgloseMateriaPeriodo`, `CalculadorEstadisticas` y un único reparto `RN-31` en `SesionEstudio` para alinear Calendario, Estadísticas y Reportes sin duplicar modelos ni consultas SQL.
- Garantizar pureza hexagonal verificada por [ArquitecturaTest](../../../app/src/test/java/io/github/jhoel0521/studyplan/ArquitecturaTest.java) (cero referencias a `android.*` o `androidx.*`, ni siquiera en Javadocs de `domain` y `application`).
- Exportar CSV RFC 4180 (`UTF-8` con BOM, mitigación `CWE-1236`) y PDF A4 multipágina (`PdfDocument`) sin librerías externas, en caché privada con retención segura, limpieza bajo `HU-38`, soporte de tema oscuro (`RNF-07`) y compartición vía `FileProvider`.

### Non-Goals
- Alterar los requisitos ya aprobados de `estadisticas` o `tecnicas-estudio`.
- Crear un servicio pasamanos de exportación en `application.service` ni una segunda variante CSV con delimitador `;`.
- Debilitar `ContadorActividades` suprimiendo el bloqueo `HU-04` o detección `HU-40` al abandonar la app vía `ACTION_SEND`.
- Exportar detalle individual de sesiones/eventos o incluir recordatorios (`E9`) y Google Drive (`E10`).

## Proposed Architecture

```text
  [adapter.in.ui]
    MainActivity (menu_inicio) ──┐
    EstadisticasActivity       ──┼─► ReportesActivity (extends ActividadProtegida, id="@+id/main")
                                 │        │ observes LiveData + consumes one-shot URI / Error
                                 │        ▼
                                 │   ReportesViewModel (Executor single-thread de StudyPlanApplication)
                                 │        ├──► [application.port.in] GenerarReporteEstudio
                                 │        └──► [application.port.out] ExportadorReporte
                                 ▼
  [application.service]
    GenerarReporteEstudioService
      ├─► VencerSesionesPendientes.ejecutar() (RN-08, antes de leer)
      ├─► TramoDiarioRepository + SesionRepository + MateriaRepository + Reloj
      └─► CalculadorEstadisticas.calcularReporte(...) ──► ReporteEstudio + DesgloseMateriaPeriodo
    BorrarDatosService ──► ExportadorReporte.limpiarTemporales() (HU-38)

  [domain] (Java 100 % puro)
    SesionEstudio.minutosPlanificadosEnRango(inicio, fin)   ◄── compartido por Calendario, E7 y E8
    CalculadorEstadisticas.calcularReporte(...)              ◄── unión reales>0 | plan>0 | noRealizadas>0
    ReporteEstudio (periodo, generadoEnMilis, List<DesgloseMateriaPeriodo>, totales escalares)
    FormatoExportacionReporte { PDF, CSV } + GeneradorCsvReporte (BOM UTF-8, RFC 4180, CWE-1236)

  [adapter.out.system] + [AndroidManifest / FileProvider]
    ExportadorReporteSistema implements ExportadorReporte
      ├─► ContextCompat.getContextForLanguage(context) (RNF-08, R.string.materia_general_nombre)
      ├─► CSV (GeneradorCsvReporte) / PDF A4 (android.graphics.pdf.PdfDocument, 595x842 pt)
      ├─► cacheDir/reportes/studyplan_reporte_<inicio>_<fin>_<ts>.(pdf|csv) (retención 3 archivos)
      └─► ResultadoExportacion (ok(content:// URI) | error(SIN_DATOS | ERROR_ESCRITURA))
```

### Contratos de interfaces y concurrencia
- **Regla de `ArquitecturaTest`:** `ANDROID_COMPLETO` escanea expresiones `androidx?.[a-z]+.[A-Za-z]` incluso en comentarios; los archivos bajo `domain` y `application` no deben nombrar ningún paquete de la plataforma ni en Javadoc.
- **Concurrencia:** `StudyPlanApplication` utiliza `Executors.newSingleThreadExecutor()`, serializando automáticamente la escritura de `VencerSesionesPendientes` con las lecturas Room de `GenerarReporteEstudioService` y las operaciones de `ExportadorReporte` / `BorrarDatosService`.

## Decisions

### 1. Cierre aislado de E7 (`HU-31`, `HU-39`, pruebas y diagramas)
- **Decisión:**
  - En `EstadisticasActivity.renderizarResumen`, sustituir `evaluables` por `e.totalCerradas()` para que la fracción `(x de y)` incluya sesiones reprogramadas igual que el porcentaje de dominio (`HU-31`).
  - En `EventoEstudioAdapter`, reutilizar la vista existente `textoNota` (`R.id.textoNotaEvento` en [item_evento_estudio.xml](../../../app/src/main/res/layout/item_evento_estudio.xml)) para mostrar `TopicosUi.etiqueta(...)` cuando `evento.topico().isPresent()` (incluyendo `SIN_CLASIFICAR` si se persistió), combinándolo como `"tópico · nota"` mediante un recurso `strings.xml` (`%1$s · %2$s`) cuando además exista `evento.nota()`.
  - En `EstadisticasEspressoTest`, sembrar todas las sesiones (`s1` en `Cálculo`, `s2` en materia archivada, `s3` en «Estudios varios», una `REPROGRAMADA` para validar la fracción `(x de y)` en UI, y la sesión `PENDIENTE` futura) en `hoy` sin llamar a `SesionEstudio.registrarPasada` (usando `planificar -> iniciar -> detener` o `SesionEstudio.restaurar` con `app.reloj().ahoraInstante().desfaseMinutos()`), evitando fallos los lunes (`diasHasta == 0`), domingos (`hoy.sumarDias(1)`) y cambios de mes (`Mes`).
  - Añadir la función `clases_estadisticas()` en `docs/build/diagramas_clases.py` y ampliar `IMAGENES`/`cotejar_imagenes()` en `docs/build/cotejo_clases.py` antes del commit separado de cierre E7.
- **Alternativas consideradas:**
  - *Añadir un segundo `TextView` para tópico en `item_evento_estudio.xml`:* descartado porque `textoNotaEvento` ya ocupa la segunda línea sangrada (`48dp`) y permanece vacío cuando no hay nota adicional, evitando inflación extra por fila.
  - *Mantener `registrarPasada` desplazando la fecha en `EstadisticasEspressoTest`:* descartado porque en lunes cualquier fecha `<= hoy` dentro de la semana actual viola `1 <= diasHasta <= 3` (`FECHA_PASADA_FUERA_DE_VENTANA`) y en el primer día de mes `hoy.sumarDias(-1)` queda fuera de la vista mensual.
  - *Mezclar estas correcciones dentro de los commits de E8:* descartado para preservar la trazabilidad limpia entre la corrección de requisitos ya archivados (`fix(HU-31,HU-39)`) y la nueva épica (`feat(HU-32,HU-33)`).

### 2. Reutilización del núcleo y reparto único `RN-31` (`HU-32`)
- **Decisión:**
  - **Fuente única de corte diario para horas planificadas:** incorporar en `SesionEstudio` el método puro `minutosPlanificadosEnRango(Fecha inicio, Fecha fin)` que suma `duracionPlanificadaMinutos` si la sesión no cruza medianoche y pertenece al rango, o solo los tramos de cada día dentro de `[inicio, fin]` cuando `cruzaMedianoche()` (tratando `horaFin == 00:00` con `0` min el segundo día). `ConsultarCalendarioService` y `CalculadorEstadisticas` delegan en este método.
  - **Reutilizar `DesgloseMateriaPeriodo` y `CalculadorEstadisticas`:** no crear `FilaReporteMateria`, `TotalesReporte`, `CalculadorReporte` ni `TextosReporte` en `domain`. Crear `ReporteEstudio` en `domain` (con `periodo`, `generadoEnMilis`, `List<DesgloseMateriaPeriodo> materias`, totales escalares `minutosPlanificadosTotales`, `minutosRealesTotales`, `sesionesNoRealizadasTotales`, `cumplimientoTotal()` y `esSinDatos()` igual a `materias.isEmpty()`) calculado por `CalculadorEstadisticas.calcularReporte(...)`.
  - **Reglas de inclusión, borde nocturno y orden:**
    - Incluye toda materia con `minutosReales > 0`, `minutosPlanificadosEnRango > 0` o `sesionesNoRealizadas > 0`.
    - El conteo discreto `sesionesNoRealizadas` de reporte se asigna únicamente al periodo que contiene `sesion.fecha()` para no duplicar una `NO_REALIZADA` nocturna entre periodos adyacentes, **sin** modificar la firma/comportamiento global de `CalculadorEstadisticas.calcular(...)` (cuyas pruebas existentes en `CalculadorEstadisticasTest` usan timestamps de `1970` en rango `2026`).
    - Se conserva `EstadoReprogramada.ocupaHorario() == false` (`RN-05`) y la consistencia con `ConsultarCalendarioService` respecto a `HU-11` (`COMPLETADA_TARDIA` ocupa horario) y sesiones `PENDIENTE` vigentes dentro del periodo. Sesiones «Estudiar ahora» cerradas con `0` minutos sin otra actividad resultan en `esSinDatos() == true`.
    - Ordenamiento determinista: descendente por `minutosReales`, descendente por `minutosPlanificadosDeSesiones`, ascendente insensible a mayúsculas por `materia.nombre()` y por `materiaId` (con fallback `""` y color neutro `0xFF607D8B` si la materia faltara en el catálogo).
  - **Aplicación:** `GenerarReporteEstudio` / `GenerarReporteEstudioService` ejecuta `VencerSesionesPendientes.ejecutar()` (`RN-08`), lee `TramoDiarioRepository`, `SesionRepository`, `MateriaRepository` y `Reloj.ahoraMilis()`, y delega en `CalculadorEstadisticas.calcularReporte(...)`.
- **Alternativas consideradas:**
  - *Calcular el agregado mediante consultas `@Query` SQL directas en DAO:* descartado porque duplicaría la lógica de corte `RN-31` y `estado.ocupaHorario()` fuera del dominio Java puro comprobado por `ArquitecturaTest`.
  - *Incluir materias con `0` reales dentro de `EstadisticasPeriodo.distribucionMaterias()`:* descartado porque el gráfico de anillo de `HU-30` solo representa distribución de horas estudiadas (`> 0`); separar `calcularReporte(...)` reutiliza `DesgloseMateriaPeriodo` sin alterar `HU-30`.

### 3. Puntos de entrada sin alterar `DestinoPrincipal` (`W8`)
- **Decisión:**
  - Conservar las 4 pestañas fijas de `menu_barra_inferior.xml`.
  - **En `EstadisticasActivity` (`contenido_estadisticas.xml`):** ubicar `MaterialButton` delineado `botonAbrirReportes` (`minHeight="48dp"`, `wrap_content`) **dentro de `contenedorResumenEstadisticas` (`NestedScrollView`)** justo tras la tarjeta de variación o de cumplimiento, evitando comprimir la altura fija sobre el scroll del `PieChart`. Al pulsar, lanza `ReportesActivity` pasando extras `EXTRA_TIPO_PERIODO` (`"SEMANA"`/`"MES"`) y `EXTRA_FECHA_FOCO` (`"YYYY-MM-DD"`).
  - **En `MainActivity` (`menu_inicio.xml`):** añadir `<item android:id="@+id/accion_reportes" android:icon="@drawable/ic_reporte" android:title="@string/reportes_titulo" app:showAsAction="ifRoom" />` **antes** de `accion_ajustes` para mantener Ajustes en el borde derecho, renombrando `onAjustesClick` a `onMenuBarraSuperiorClick` (actualizando la referencia en `04-diseno-del-software.md`). Desde Inicio abre `ReportesActivity` en la semana actual.
- **Alternativas consideradas:**
  - *Añadir una 5.ª pestaña en `BottomNavigationView` o colocar `botonAbrirReportes` fuera del `NestedScrollView`:* descartadas porque saturan la barra inferior (`W8`) o reducen el espacio vertical útil del gráfico de anillo en pantallas de baja altura.

### 4. Interfaz `ReportesActivity`, `ReportesViewModel` y diseño responsivo (`RNF-06`, `RNF-08`, `RNF-10`, `RNF-12`)
- **Decisión:**
  - `ReportesActivity` hereda de `ActividadProtegida` (raíz con `android:id="@+id/main"` obligatorio para `aplicarInsets()`), barra superior con botón volver (`ic_volver`) y **sin** barra inferior (mismo patrón que `HistorialMetasActivity` y `SesionDetalleActivity`), con variante `layout-w600dp/activity_reportes.xml` centrada en columna de `560dp`.
  - Controles: selector `Semana | Mes` (`MaterialButtonToggleGroup`), navegación `< rango >`, tarjeta de totales del periodo (horas planificadas `%.1f h`, horas reales `%.1f h`, `%` cumplimiento o `—`, total no realizadas), **dos botones explícitos** `botonExportarPdf` y `botonExportarCsv` (`wrap_content`, `minHeight="48dp"`, deshabilitados cuando `esSinDatos()` o `exportando == true`), estado vacío claro (`Sin datos`) y `RecyclerView` con `ReportesAdapter`.
  - `item_reporte_materia.xml` en distribución compacta de **dos líneas** para soportar anchos estrechos (`320dp–360dp`) sin desborde horizontal: línea 1 con indicador de color + nombre (incluyendo `Estudios varios`/`Miscellaneous study` y sufijo `(Archivada)`); línea 2 con texto resumido `Plan: %.1f h · Real: %.1f h · Cumpl.: %s · No real.: %d`, cumpliendo el checklist XML de `AGENTS.md` §6.2.
  - **`ReportesViewModel`:** inicializa el periodo una única vez con los extras del `Intent` antes de `alEstarListo()` (protegido frente a rotaciones `RNF-10`), utiliza un contador de versión para descartar cargas obsoletas ante clics rápidos en `<` / `>`, expone `exportando` (evitando doble pulsación), y maneja eventos *one-shot* `uriCompartir` / `errorExportacion` con `consumirExportacion()` y `consumirError()`.
- **Alternativas consideradas:**
  - *Tabla horizontal rígida de 5 columnas por fila:* descartada porque se recorta o fuerza desplazamiento horizontal en teléfonos de `320dp–360dp`.
  - *Botón único «Compartir» con submenú desplegable:* descartado porque dos botones explícitos alinean el diálogo directo con `specs/reportes/spec.md` (`Exportar PDF` / `Exportar CSV`), permiten deshabilitarlos de forma visible sin datos y facilitan accesibilidad TalkBack y pruebas Espresso en 1 toque.

### 5. Formato CSV UTF-8 con BOM y mitigación `CWE-1236` (`HU-33`)
- **Decisión:** `GeneradorCsvReporte` en `domain` genera un `byte[]` UTF-8 con prefijo BOM (`0xEF 0xBB 0xBF`):
  - **Delimitador y tipos:** coma `,` estándar RFC 4180, saltos `\r\n`, decimales con punto `.` (`Locale.ROOT`), horas formateadas a 1 decimal (`%.1f`), cumplimiento como entero redondeado (`Math.round(...)` sin `%` para habilitar fórmulas numéricas en hojas de cálculo, o `"—"` si `plan == 0`).
  - **Esquema tabular sin filas previas:**
    - Fila 1 (`es`): `Periodo,Materia,Horas planificadas (h),Horas reales (h),Cumplimiento (%),Sesiones no realizadas`
    - Fila 1 (`en`): `Period,Subject,Planned hours (h),Real hours (h),Compliance (%),Missed sessions`
    - Filas por materia: rango `"YYYY-MM-DD – YYYY-MM-DD"`, nombre localizado (usando `@string/materia_general_nombre` = `"Miscellaneous study"` en `en`), horas planificadas, reales, cumplimiento y no realizadas.
    - Fila final de totales: rango `"YYYY-MM-DD – YYYY-MM-DD"`, `"Total"`, suma planificada, suma real, cumplimiento total y total de no realizadas.
  - **Seguridad y validación:** aplica comillas RFC 4180 (`""`) ante `,`, `"`, `\r`, `\n`; antepone `'` a campos de texto de usuario cuyo primer carácter (incluidos tras recorte inicial) sea `=`, `+`, `-`, `@`, `\t` o `\r` (`CWE-1236`); rechaza generación si `reporte.esSinDatos()`.
- **Alternativas consideradas:**
  - *Añadir `sep=,` al inicio o una segunda opción con delimitador `;`:* descartado porque Google Sheets imprime `sep=,` como primera celda literal y la doble variante añade fricción UI fuera del alcance. Se documenta la apertura directa en Google Sheets y la importación UTF-8 vía *Datos > Desde texto/CSV* en Excel Windows regional.

### 6. Exportación PDF nativa, puerto `ExportadorReporte`, caché y `FileProvider` (`HU-33`, `HU-38`)
- **Decisión:**
  - **Puerto `ExportadorReporte` (`application.port.out`) y `ResultadoExportacion`:** prescindir de un servicio pasamanos intermedio en `application.service` (`ReportesViewModel` invoca directo a `ExportadorReporte`, igual que `CalendarioViewModel` usa `PreferenciaVistaCalendario`). `ExportadorReporte` recibe `(ReporteEstudio reporte, FormatoExportacionReporte formato)` y devuelve `ResultadoExportacion` (tipo propio con `uriContenido()` o `Error { SIN_DATOS, ERROR_ESCRITURA }`, sin contaminar `domain.ErrorValidacion` ni propagar rutas de archivo `java.io.File`), más `limpiarTemporales()` invocado por `BorrarDatosService.ejecutar(AlcanceBorrado.TELEFONO)`.
  - **`ExportadorReporteSistema` (`adapter.out.system`):**
    - Resuelve las etiquetas de exportación usando `ContextCompat.getContextForLanguage(context)` para respetar el idioma activo de la app incluso en Android `< API 33` (`RNF-08`).
    - Escribe archivos ASCII seguros `studyplan_reporte_<inicio>_<fin>_<timestamp>.<pdf|csv>` bajo `cacheDir/reportes/`, manteniendo los últimos **3** archivos generados (en lugar de vaciar la carpeta en cada exportación) para no romper lecturas activas de una app receptora al compartir PDF y CSV seguidos.
    - Para **PDF** (`android.graphics.pdf.PdfDocument`, tamaño A4 fijo `595x842` pt independiente de `DisplayMetrics.density`): usa colores explícitos de alto contraste sobre fondo blanco (`Color.BLACK`, grises oscuros) inmunes al modo oscuro (`RNF-07`), pinta título StudyPlan, rango y fecha de generación desde `reporte.generadoEnMilis()` (`Reloj`), ajusta nombres largos con `StaticLayout`/elipsis sin pisar columnas numéricas, repite la cabecera de tabla en páginas 2..N y fuerza salto preventivo si la fila `Total` quedara huérfana. Captura excepciones E/S devolviendo `Error.ERROR_ESCRITURA`.
    - Devuelve `FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", archivo).toString()`.
  - **Manifiesto y compartición en `ReportesActivity`:** declarar `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true">` y `res/xml/file_paths.xml` (`<cache-path name="reportes" path="reportes/" />`). Al recibir el evento único, `ReportesActivity` llama a `modelo.consumirExportacion()` **antes** de lanzar `Intent.createChooser(intent, ...)` con `ACTION_SEND`, `EXTRA_STREAM`, `ClipData.newRawUri("", uri)`, tipo MIME exacto (`application/pdf` o `text/csv`), `FLAG_GRANT_READ_URI_PERMISSION`, y captura `ActivityNotFoundException` mostrando `Snackbar`.
- **Alternativas consideradas:**
  - *Incorporar librerías externas (`iText`/`PdfBox`) o `WebView` + `PrintDocumentAdapter`:* descartadas para evitar nuevas dependencias sin aprobación (`AGENTS.md` §2) y porque `PrintDocumentAdapter` prioriza impresión frente al envío directo de archivo adjunto `ACTION_SEND` pedido en `HU-33`; *SAF `ACTION_CREATE_DOCUMENT`* puede evaluarse a futuro sin cambiar `ReporteEstudio`, pero aquí `Intent.ACTION_SEND` cubre tanto guardar localmente como enviar a un tutor en un único selector.

## Risks / Trade-offs

- **CSV con delimitador `,` en Windows Excel con configuración regional español (`;`)** → El BOM `\uFEFF` preserva tildes y eñes y Google Sheets abre columnas directamente, pero el doble clic directo en Excel Windows con regional `;` requiere usar *Datos > Desde texto/CSV*; queda explícitamente aclarado en `spec.md` y textos de aceptación para no sobredimensionar garantías en pruebas JUnit de bytes.
- **Re-bloqueo y detección de salida al volver de `Intent.createChooser` (`HU-04`, `HU-40`)** → `ContadorActividades` marcará `salioDeLaApp = true` al abrir el selector externo y re-autenticará al volver si el bloqueo está activo (comportamiento deseado de privacidad `RNF-12`); consumir el evento `consumirExportacion()` antes de lanzar el chooser y mantener carga idempotente evita bucles al reanudar.
- **Carrera al compartir PDF y CSV seguidos vs. privacidad `HU-38`** → Retener los últimos 3 archivos en `cacheDir/reportes/` evita borrar un `Uri` aún abierto por otra app, mientras `BorrarDatosService` invoca `limpiarTemporales()` para garantizar borrado completo al limpiar datos del teléfono.
- **Desbordamiento PDF o tema oscuro (`RNF-07`) y saltos de página** → Se fija lienzo A4 sin escala por densidad, colores fijos sobre blanco, `StaticLayout`/elipsis en columna materia y prueba instrumentada con `PdfRenderer` verificando `>= 2` páginas con `40+` materias.
- **Fallo de `cotejo_clases.py` y desfase con cambios en `MainActivity`/`EstadisticasActivity`** → Toda clase nueva se registra en `CLASES_POR_PAQUETE` y en los diagramas de `diagramas_clases.py`, actualizando también `diagrama_clases_configuracion.png` y `diagramas.py`.
- **Cambios de zona horaria / horario de verano (`DST`) en sesiones nocturnas** → `SesionEstudio.minutosPlanificadosEnRango` trabaja sobre `fecha()`, `fechaFinLocal()` y `Hora` locales guardadas en la sesión con su `desfaseMinutos` persistido, manteniéndose determinista en cambios de zona horaria igual que `ConsultarCalendarioService`.
