# Tasks

> Requisito previo: `add-estadisticas` archivado (`Room v7`). No requiere dependencias nuevas en `gradle/libs.versions.toml` (`android.graphics.pdf.PdfDocument`, `ContextCompat.getContextForLanguage` y `androidx.core.content.FileProvider` ya forman parte del SDK/AndroidX vigentes; **no** añadir `espresso-intents`). Cada tarea está dimensionada para `<= 2 h` e incluye su verificación directa.

## Target Scope & File Boundary

### Fase 1 — Cierre de E7 (`fix(HU-31,HU-39)`)
- **Modificar (código y pruebas):**
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadisticasActivity.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EventoEstudioAdapter.java`
  - `app/src/main/res/layout/item_evento_estudio.xml`
  - `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml`
  - `app/src/androidTest/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadisticasEspressoTest.java`
  - `app/src/androidTest/java/io/github/jhoel0521/studyplan/adapter/in/ui/TecnicasEstudioEspressoTest.java`
- **Modificar (diagramas E7):**
  - `docs/build/diagramas_clases.py` y `docs/build/cotejo_clases.py`
  - `docs/documento/recursos/diagramas/diagrama_clases_estadisticas.png` (generado)

### Fases 2–5 — Épica E8 (`feat(HU-32,HU-33)`)
- **Crear:**
  - `app/src/main/java/io/github/jhoel0521/studyplan/domain/ReporteEstudio.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/domain/FormatoExportacionReporte.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/domain/GeneradorCsvReporte.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/application/port/in/GenerarReporteEstudio.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/application/service/GenerarReporteEstudioService.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/application/port/out/ExportadorReporte.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/application/port/out/ResultadoExportacion.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/out/system/ExportadorReporteSistema.java`
  - `app/src/main/res/xml/file_paths.xml`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ReportesViewModel.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ReportesAdapter.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ReportesActivity.java`
  - `app/src/main/res/layout/activity_reportes.xml`
  - `app/src/main/res/layout-w600dp/activity_reportes.xml`
  - `app/src/main/res/layout/item_reporte_materia.xml`
  - `app/src/main/res/drawable/ic_reporte.xml`
  - `app/src/test/java/io/github/jhoel0521/studyplan/fakes/FakeExportadorReporte.java`
  - `app/src/test/java/io/github/jhoel0521/studyplan/domain/CalculadorReporteTest.java`
  - `app/src/test/java/io/github/jhoel0521/studyplan/domain/GeneradorCsvReporteTest.java`
  - `app/src/test/java/io/github/jhoel0521/studyplan/application/service/GenerarReporteEstudioServiceTest.java`
  - `app/src/test/java/io/github/jhoel0521/studyplan/adapter/in/ui/ReportesViewModelTest.java`
  - `app/src/androidTest/java/io/github/jhoel0521/studyplan/adapter/out/system/ExportadorReporteSistemaInstrumentalTest.java`
  - `app/src/androidTest/java/io/github/jhoel0521/studyplan/adapter/in/ui/ReportesEspressoTest.java`
  - `docs/documento/recursos/diagramas/diagrama_clases_reportes.png` (generado)
- **Modificar:**
  - `app/src/main/java/io/github/jhoel0521/studyplan/domain/SesionEstudio.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/domain/CalculadorEstadisticas.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/application/service/ConsultarCalendarioService.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/application/service/BorrarDatosService.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadisticasActivity.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadisticasAdapter.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/MainActivity.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/StudyPlanApplication.java`
  - `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/FabricaViewModels.java`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/layout/contenido_estadisticas.xml`
  - `app/src/main/res/menu/menu_inicio.xml`
  - `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml`
  - `app/src/test/java/io/github/jhoel0521/studyplan/application/service/BorrarDatosServiceTest.java`
  - `docs/build/diagramas_clases.py`, `docs/build/cotejo_clases.py`, `docs/build/diagramas.py`, `docs/documento/capitulos/04-diseno-del-software.md`, `docs/auditoria-ia/bitacora.md` y PNGs regenerados (`diagrama_clases_configuracion.png`, diagrama de flujo).

---

## 1. Cierre y corrección de bugs de E7 *(hito de commit separado: `fix(HU-31,HU-39): ...`)*

- [x] 1.1 Corregir fracción de cumplimiento en UI (`HU-31`) y estabilizar `EstadisticasEspressoTest`
  - En `EstadisticasActivity.renderizarResumen`, reemplazar `cumplidas + e.noRealizadas()` por `e.totalCerradas()` para mostrar `(x de y)` incluyendo reprogramadas.
  - En `EstadisticasEspressoTest`, sembrar `s1`, `s2` (archivada), `s3` («Estudios varios»), una sesión `REPROGRAMADA` y una sesión `PENDIENTE` futura todas en `hoy` sin llamar a `registrarPasada` (usando `app.reloj().ahoraInstante().desfaseMinutos()`), validando la fracción con `reprogramadas > 0` tanto en vista `Semana` como `Mes`.
  - Verificar: `./gradlew assembleDebug testDebugUnitTest lintDebug` y ejecución de `EstadisticasEspressoTest` mediante `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.jhoel0521.studyplan.adapter.in.ui.EstadisticasEspressoTest`.

- [x] 1.2 Mostrar tópicos en línea de tiempo de sesión (`HU-39`)
  - En `EventoEstudioAdapter.EventoHolder.enlazar`, reutilizar `textoNota` (`R.id.textoNotaEvento`) usando `TopicosUi.etiqueta(...)` cuando `evento.topico().isPresent()` (combinado como `"tópico · nota"` con recurso `%1$s · %2$s` en `es`/`en` cuando exista `evento.nota()`), actualizando `tools:text` en `item_evento_estudio.xml`.
  - Añadir aserción en `TecnicasEstudioEspressoTest` comprobando que el tópico persistido es visible en `SesionDetalleActivity`.
  - Verificar: `./gradlew assembleDebug lintDebug` y `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.jhoel0521.studyplan.adapter.in.ui.TecnicasEstudioEspressoTest`.

- [x] 1.3 Generar diagrama de clases de E7 y cerrar el hito de commit separado
  - Añadir `clases_estadisticas()` en `docs/build/diagramas_clases.py`, incorporarla a `generar()`, ampliar `IMAGENES` y `cotejar_imagenes()` en `docs/build/cotejo_clases.py` y actualizar la sección de E7 en `04-diseno-del-software.md`.
  - Verificar: `python docs/build/diagramas_clases.py`, `python docs/build/cotejo_clases.py`, `./gradlew testDebugUnitTest lintDebug connectedDebugAndroidTest`.
  - Dejar listo para el commit separado `fix(HU-31,HU-39): corregir fraccion de cumplimiento, topicos en linea de tiempo y diagrama E7` previo a iniciar la Sección 2 (ejecutar `git commit` solo tras confirmación humana según `AGENTS.md` §4).

---

## 2. Dominio puro (`domain`, Java 100 % sin `android.*` ni siquiera en comentarios)

- [x] 2.1 Unificar reparto `RN-31` y agregar cálculo de reporte (`HU-32`)
  - Añadir `SesionEstudio.minutosPlanificadosEnRango(Fecha inicio, Fecha fin)` (cortando en medianoche cuando `cruzaMedianoche()` y asignando `0` min al día siguiente cuando `horaFin == 00:00`) y reutilizarlo en `ConsultarCalendarioService` y `CalculadorEstadisticas`.
  - Crear `ReporteEstudio` (con `periodo`, `generadoEnMilis`, `List<DesgloseMateriaPeriodo> materias`, totales escalares, `cumplimientoTotal()` y `esSinDatos()` igual a `materias.isEmpty()`) y añadir `CalculadorEstadisticas.calcularReporte(...)` reutilizando `DesgloseMateriaPeriodo` (unión `reales > 0 || planificados > 0 || noRealizadas > 0`, atribuyendo `NO_REALIZADA` a `sesion.fecha()`, excluyendo `REPROGRAMADA` vía `estado.ocupaHorario()`, orden determinista descendente por reales → planificados → `nombreMateria` → `materiaId`, fallback ante catálogo nulo), sin alterar el comportamiento existente de `CalculadorEstadisticasTest` (`1970`).
  - Crear `CalculadorReporteTest.java` cubriendo: cruce semanal/mensual `23:00–01:00`, borde `22:00–00:00`, `NO_REALIZADA` nocturno en frontera sin doble conteo, materia con `0 h` reales y planificadas/no realizadas, «Estudiar ahora» (`cumplimiento == null`), cumplimiento `> 100 %`, exclusión `REPROGRAMADA`, «Estudios varios» y archivadas, sesión de `0` min sin plan (`esSinDatos() == true`), empates y `ArquitecturaTest` (como prueba de regresión hexagonal).
  - Verificar: `./gradlew testDebugUnitTest`.

- [x] 2.2 Implementar `FormatoExportacionReporte` y `GeneradorCsvReporte` seguro (`HU-33`)
  - Crear `FormatoExportacionReporte { PDF, CSV }` y `GeneradorCsvReporte` en `domain` retornando `byte[]` UTF-8 con prefijo BOM (`0xEF 0xBB 0xBF`), delimitador `,`, saltos `\r\n`, decimales con punto `.` (`Locale.ROOT`), horas `%.1f`, porcentaje entero redondeado sin `%` (o `"—"` cuando `plan == 0`), cabeceras directas en fila 1 (localizadas por parámetro `es`/`en`), rango `"YYYY-MM-DD – YYYY-MM-DD"` en `Periodo`, fila final `Total`, escape RFC 4180 (`""`), sanitización `CWE-1236` (prefijo `'` ante `=`, `+`, `-`, `@`, `\t`, `\r` en textos de usuario) y guarda si `reporte.esSinDatos()`.
  - Crear `GeneradorCsvReporteTest.java` ejecutando bajo `Locale.setDefault(new Locale("es", "BO"))` y `Locale.US`, validando bytes iniciales BOM `EF BB BF`, `\r\n`, comas/comillas/tildes/eñes, fórmulas `=cmd|'/C'!A0` neutralizadas, `—` sin plan, fila `Total` y `ArquitecturaTest` (regresión).
  - Verificar: `./gradlew testDebugUnitTest`.

---

## 3. Puertos y servicios de aplicación (`application`)

- [x] 3.1 Crear caso de uso de reporte, puerto de exportación y limpieza en `BorrarDatosService`
  - Crear `GenerarReporteEstudio` (`application.port.in`) y `GenerarReporteEstudioService` (`application.service`, venciendo pendientes vía `VencerSesionesPendientes.ejecutar()`, leyendo `TramoDiarioRepository`, `SesionRepository`, `MateriaRepository` y `Reloj.ahoraMilis()`).
  - Crear `ExportadorReporte` y `ResultadoExportacion` en `application.port.out` (devolviendo `uriContenido` `String` o `ResultadoExportacion.Error { SIN_DATOS, ERROR_ESCRITURA }`, sin usar `ErrorValidacion` ni mencionar `android.*`/`androidx.*` en Javadocs) con `limpiarTemporales()`.
  - Ampliar `BorrarDatosService` (con sobrecarga de constructor retrocompatible) invocando `exportador.limpiarTemporales()` cuando `alcance == AlcanceBorrado.TELEFONO` (`HU-38`).
  - Crear `FakeExportadorReporte.java` y `GenerarReporteEstudioServiceTest.java` (incluyendo vencimiento automático `RN-08` antes de calcular), ampliar `BorrarDatosServiceTest.java` y verificar `ArquitecturaTest` (regresión).
  - Verificar: `./gradlew testDebugUnitTest`.

---

## 4. Exportación PDF/CSV, `FileProvider`, UI XML y puntos de entrada (`adapter.*`, `res/`)

- [x] 4.1 Implementar `FileProvider` y `ExportadorReporteSistema` (`PdfDocument` + CSV) con prueba instrumentada
  - Declarar `androidx.core.content.FileProvider` (`${applicationId}.fileprovider`, `exported="false"`, `grantUriPermissions="true"`) en `AndroidManifest.xml` y crear `res/xml/file_paths.xml` (`<cache-path name="reportes" path="reportes/" />`).
  - Implementar `ExportadorReporteSistema` en `adapter.out.system`: resuelve cadenas en el idioma activo con `ContextCompat.getContextForLanguage(...)` (usando `@string/materia_general_nombre` = `"Miscellaneous study"` en inglés y sufijo archivada), genera `studyplan_reporte_<inicio>_<fin>_<ts>.pdf|csv` en `cacheDir/reportes/` reteniendo los 3 archivos más recientes (y borrado completo en `limpiarTemporales()`), renderiza PDF A4 (`595x842` pt independiente de densidad, colores `Color.BLACK`/grises sobre blanco frente a tema oscuro, `StaticLayout`/elipsis para 50+ caracteres, cabecera repetida en páginas 2..N y salto preventivo antes de `Total`), y captura errores E/S devolviendo `ResultadoExportacion.Error.ERROR_ESCRITURA`.
  - Crear `ExportadorReporteSistemaInstrumentalTest.java` comprobando generación real de CSV (`EF BB BF`, coma, `CRLF`, idioma `es`/`en`), PDF multipágina con `>= 40` materias validado con `android.graphics.pdf.PdfRenderer` (`pageCount >= 2` y cabecera `%PDF-`), resolución de `content://` vía `FileProvider` y `limpiarTemporales()`.
  - Verificar: `./gradlew assembleDebug testDebugUnitTest lintDebug` y `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.jhoel0521.studyplan.adapter.out.system.ExportadorReporteSistemaInstrumentalTest`.

- [x] 4.2 Implementar `ReportesViewModel`, cadenas `es`/`en`, `ic_reporte.xml` y cableado DI (`StudyPlanApplication`, `FabricaViewModels`)
  - Crear `ReportesViewModel` recibiendo `GenerarReporteEstudio`, `ExportadorReporte`, `Reloj` y `Executor`: inicialización idempotente de periodo desde extras iniciales sin sobreescribir tras rotación (`RNF-10`), control de versión para descartar respuestas obsoletas ante navegación rápida `<` / `>`, `exportando`, y eventos *one-shot* con `consumirExportacion()` / `consumirError()`.
  - Añadir `ic_reporte.xml`, cadenas de UI/exportación/errores en `res/values/strings.xml` y `res/values-en/strings.xml`, y registrar `generarReporteEstudio()` / `exportadorReporte()` / `ReportesViewModel` en `StudyPlanApplication` y `FabricaViewModels`.
  - Crear `ReportesViewModelTest.java` cubriendo inicialización inicial vs periodo recibido, cambio `SEMANA ↔ MES`, navegación `<`/`>`, bloqueo `exportando`, emisión y consumo único de `uriCompartir` y manejo de `ResultadoExportacion.Error`.
  - Verificar: `./gradlew assembleDebug testDebugUnitTest lintDebug` (0 errores, 0 `MissingTranslation`).

- [x] 4.3 Implementar layouts, `ReportesAdapter` y `ReportesActivity` (`extends ActividadProtegida`)
  - Crear `activity_reportes.xml` (raíz `android:id="@+id/main"`, `MaterialToolbar` con `ic_volver`, sin barra inferior), `layout-w600dp/activity_reportes.xml` (columna centrada `560dp`), e `item_reporte_materia.xml` en 2 líneas compatible con `320dp–360dp`; validar manualmente cada atributo del checklist de `AGENTS.md` §6.2 (`tools:context`, `tools:fitsSystemWindows`, `minHeight="48dp"`, `maxWidth`, `tools:text`, `contentDescription`, `tools:tint`, sin depender solo de color).
  - Extraer `EstadisticasAdapter.nombreVisible(Context, DesgloseMateriaPeriodo)` para compartir formato `esGeneral`/`archivada`, crear `ReportesAdapter` (`%.1f h`) y `ReportesActivity` registrándola en `AndroidManifest.xml` (`exported="false"`), deshabilitando `botonExportarPdf` y `botonExportarCsv` sin datos o mientras exporta, consumiendo `uriCompartir` **antes** de `Intent.createChooser` con `ClipData.newRawUri`, `FLAG_GRANT_READ_URI_PERMISSION` y `try/catch (ActivityNotFoundException -> Snackbar)`.
  - Verificar: `./gradlew assembleDebug testDebugUnitTest lintDebug` y revisión visual/layout del checklist `AGENTS.md` §6.2.

- [x] 4.4 Conectar puntos de entrada en `EstadisticasActivity` e `Inicio` (`MainActivity`)
  - Añadir `MaterialButton` delineado `botonAbrirReportes` dentro de `contenedorResumenEstadisticas` (`NestedScrollView` en `contenido_estadisticas.xml`, sin reducir espacio del `PieChart`), lanzando `ReportesActivity` con `EXTRA_TIPO_PERIODO` y `EXTRA_FECHA_FOCO` en `EstadisticasActivity.java`.
  - Añadir `accion_reportes` antes de `accion_ajustes` en `menu_inicio.xml` y refactorizar `MainActivity.onAjustesClick` → `MainActivity.onMenuBarraSuperiorClick` abriendo `ReportesActivity` en la semana actual.
  - Verificar: `./gradlew assembleDebug testDebugUnitTest lintDebug` y regresión en `EstadisticasEspressoTest`.

---

## 5. Verificación E2E, compilación Release y documentación

- [x] 5.1 Crear y ejecutar `ReportesEspressoTest.java` y validar OpenSpec
  - Cubrir: entrada desde `Estadísticas` en vista `Mes`/periodo previo validando coincidencia de periodo, entrada desde `Inicio` (`accion_reportes`), cambio `Semana | Mes` y `<`/`>`, materias con `0.0 h` reales y `no realizadas`, «Estudios varios» y archivada, sesión sin plan (`—`), exclusión `REPROGRAMADA`, tolerancia decimal `,`/`.`, rotación `recreate()` (`RNF-10`), estado «Sin datos» deshabilitando `botonExportarPdf`/`botonExportarCsv` y preservación de las 4 pestañas de `BottomNavigationView`.
  - Verificar: `./gradlew connectedDebugAndroidTest` (suite completa sin fallos ni omitidos) y `openspec validate add-reportes --strict`.

- [x] 5.2 Verificar Release/R8, actualizar diagramas y cerrar auditoría IA
  - Verificar compilación debug/release con R8 y suite completa: `./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug` (0 errores).
  - Revisar código nuevo/modificado contra `.claude/agents/revisor-java.md` (0 hallazgos `Blocker`/`Major`).
  - Actualizar `docs/build/diagramas_clases.py` (`CLASES_POR_PAQUETE` con todas las clases E8, `clases_reportes()` y regeneración de `diagrama_clases_configuracion.png`), `docs/build/diagramas.py` (flecha continua «Ver reporte» en L526–527) y `docs/build/cotejo_clases.py`, ejecutando `python docs/build/diagramas_clases.py`, `python docs/build/diagramas.py` y `python docs/build/cotejo_clases.py`.
  - Actualizar `docs/documento/capitulos/04-diseno-del-software.md` (`onMenuBarraSuperiorClick` en L166 e inserción de diagramas E7/E8 en L232) y registrar los hitos de E7 y E8 en `docs/auditoria-ia/bitacora.md` (incluyendo punto de verificación manual de importación CSV en Google Sheets / Excel para Windows).

## Workflow follow-up

- Probar manualmente en dispositivo físico o emulador AVD el selector compartir de PDF y CSV, verificando apertura en Google Sheets y mediante *Datos > Desde texto/CSV* en Excel para Windows (y anotar el resultado en la bitácora).
- Archivar `add-reportes` mediante `/opsx:archive` (`openspec archive add-reportes`) antes de proponer el siguiente *change* para **E9 (`recordatorios`: `HU-34`)**.
