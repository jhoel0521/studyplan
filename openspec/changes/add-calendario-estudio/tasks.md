# Tasks

> **Flujo de trabajo obligatorio (`AGENTS.md` §3 y §6):**
> 1. Ningún bloque de código se implementa sin aprobación humana de esta propuesta (`0.1`).
> 2. Al completar cada bloque se ejecutan las pruebas aplicables, `ArquitecturaTest` y `./gradlew lintDebug`, se realiza `git commit` por ruta explícita (sin coautor de IA) y `git push` solo cuando corresponda/se autorice.
> 3. Cada layout XML nuevo o modificado debe cumplir al 100 % el checklist de `AGENTS.md` §6.2 (aplicando la excepción explícita para celdas de 7 columnas y bloques horarios proporcionales en pantallas pequeñas).

---

## 0. Compuerta previa

- [x] 0.1 **Revisión humana.** El estudiante revisa y aprueba la propuesta `add-calendario-estudio` (`proposal.md`, `specs/calendario-estudio/spec.md`, `design.md`, `trazabilidad.md` y `tasks.md`) conforme a `AGENTS.md` §3 antes de escribir código en `app/`. *(No requiere nuevas dependencias en Gradle, ni migración de Room, ni permisos Android).* Aprobado por el estudiante el 2026-11-16 (confirmado enfoque nativo Android Views XML + Java).

---

## 1. Dominio puro del calendario (`domain`)

- [x] 1.1 Crear `VistaCalendario`, `FiltroCalendario` y `PeriodoCalendario` en `domain` (Java puro, sin `android.*` ni `androidx.*`, sobre `Fecha`, `CodigoEstadoSesion` y `RN-13` lunes–domingo): cálculo de rango visible por vista, cuadrícula extendida del mes (35 o 42 celdas), navegación anterior/siguiente y reglas de conservación de foco al cambiar de vista; verificar con `PeriodoCalendarioTest` (retroceder 2 meses y cambiar a `Semana` tomando la 1.ª semana si no hay día seleccionado, conservar día seleccionado entre vistas, cambio de año y febrero bisiesto).
- [x] 1.2 Crear `CalculadorRangoHorario`, `ItemSesionCalendario`, `BloqueHorarioCalendario`, `CeldaDiaMes`, `ResumenPeriodoCalendario` y `DatosCalendario` en `domain`: ventana auto-scroll `± 2 h` sobre rejilla 00:00–24:00 (y `08:00` por defecto sin sesiones), reparto de columnas paralelas ante concurrencia visual (`REPROGRAMADA` + sesión activa en el mismo horario), y totales `Plan / Real` (`Real` por `RN-11` y `Plan` excluyendo `REPROGRAMADA`); verificar con `CalculadorRangoHorarioTest` y `ArquitecturaTest`.
- [x] 1.3 Ejecutar `./gradlew testDebugUnitTest lintDebug`, realizar `git commit` por ruta explícita (`feat(HU-16): implementar modelo de dominio y calculo de calendario`) y ejecutar `git push`.

---

## 2. Puertos y servicio de aplicación (`application`)

- [x] 2.1 Ampliar `SesionRepository` (`application.port.out`) con `listarPorRango(Fecha desde, Fecha hasta)` e implementar `ConsultarCalendario` + `ConsultarCalendarioService` (`application.port.in`, `application.service`): vence pendientes primero (`RN-08`), carga materias activas y archivadas, consulta el rango en una sola llamada, filtra por materia/archivadas/estados (`HU-21`), arma celdas de `Mes` o bloques de `Semana`/`Día` y calcula resumen `Plan / Real` únicamente sobre el periodo visible; verificar con `ConsultarCalendarioServiceTest` (repositorio falso en memoria) y `ArquitecturaTest`.
- [x] 2.2 Ejecutar `./gradlew testDebugUnitTest lintDebug`, realizar `git commit` por ruta explícita (`feat(HU-16): implementar caso de uso ConsultarCalendario y consulta por rango`) y ejecutar `git push`.

---

## 3. Persistencia Room v3 sin migración (`adapter.out.persistence`)

- [x] 3.1 Añadir `listarPorRango(String desde, String hasta)` en `SesionDao` (`SELECT * FROM sesion_estudio WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha ASC, hora_inicio ASC`, aprovechando `index_sesion_estudio_fecha` en Room v3 sin cambiar versión ni esquema) e implementarlo en `SesionRepositoryRoom`; verificar con `SesionRepositoryRoomTest` (rango inclusivo ordenado por fecha y hora inicio ascendentes, rango vacío y comprobación de que `StudyPlanDatabase` permanece en versión 3 y `AndroidManifest.xml` sin permisos).
- [x] 3.2 Ejecutar `./gradlew assembleDebug testDebugUnitTest lintDebug`, realizar `git commit` por ruta explícita (`feat(HU-16): agregar consulta por rango en Room v3 sin migracion`) y ejecutar `git push`.

---

## 4. Presentación UI nativa, ViewModel y layouts XML accesibles (`adapter.in.ui` y `res/`)

- [x] 4.1 Implementar `PreferenciaVistaCalendario` (preferencias locales con `VistaCalendario.SEMANA` por defecto), `CalendarioViewModel` y ampliar `SesionFormularioActivity` con lectura opcional de `EXTRA_HORA_INICIO` y `EXTRA_HORA_FIN` cuando `savedInstanceState == null` en `MODO_PLANIFICAR`; verificar con `CalendarioViewModelTest` (vista inicial `SEMANA`, persistencia de última vista, cambio de vista conservando foco, cambio de filtros y recarga).
- [x] 4.2 Habilitar `DestinoPrincipal.CALENDARIO` (`CalendarioActivity.class`) y registrar `CalendarioActivity` en `AndroidManifest.xml` (`exported="false"`), añadir textos es/en en `values/strings.xml` y `values-en/strings.xml`, y crear los layouts XML nativos (`activity_calendario.xml`, `layout-w600dp/activity_calendario.xml`, `contenido_calendario.xml`, `item_celda_mes.xml`, `item_bloque_calendario.xml`, `dialogo_leyenda_calendario.xml`, `dialogo_filtros_calendario.xml`) cumpliendo `AGENTS.md` §6.2; verificar con `./gradlew lintDebug` sin errores ni traducciones faltantes.
- [x] 4.3 Implementar `CalendarioMesAdapter` y `CalendarioActivity` (extiende `ActividadProtegida` con `aplicarInsets()` y `enlazarBarraInferior(DestinoPrincipal.CALENDARIO)`): selector `Día | Semana | Mes`, navegador `‹` / `›` y swipe horizontal, resumen `Plan X h · Real Y h` (`HU-21`), filtros por materia/archivadas/estados (`HU-21`), diálogo de Leyenda (`HU-19`), vista `Mes` (`RecyclerView` 7 columnas, días adyacentes atenuados, colores y horas por celda, lista inferior de sesiones al tocar un día `HU-16`), vistas `Semana` (`HU-17`) y `Día` (`HU-18`) con rejilla nativa 24 h en `ScrollView` + tarjetas `MaterialCardView` proporcionales con `EstadoSesionVisual` + línea de hora actual + auto-scroll `± 2 h`, toque simple a `SesionDetalleActivity` y pulsación larga sostenida en `fecha >= hoy` a `SesionFormularioActivity` (`HU-20`); actualizar `FabricaViewModels` y `StudyPlanApplication`, y verificar con `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- [x] 4.4 Realizar `git commit` por ruta explícita (`feat(HU-16): implementar pantallas nativas de calendario Mes, Semana y Dia`) y ejecutar `git push`.

---

## 5. Pruebas instrumentadas E2E y revisión con `revisor-java`

- [ ] 5.1 Escribir `CalendarioActivityEspressoTest` verificando en UI: arranque inicial en `Semana` y memoria de última vista, conservación de foco tras navegar entre meses y cambiar de vista, vista `Mes` (hoy resaltado, indicadores, toque en día lista sesiones debajo `HU-16`), vistas `Semana` y `Día` (bloques proporcionales, icono + etiqueta `HU-19` y leyenda), creación mediante pulsación larga sostenida en `fecha >= hoy` y ausencia de creación en fechas pasadas (`HU-20`), filtros de materia/archivadas/estados + cabecera `Plan / Real` (`HU-21`), reflejo de cambio de color tras editar materia (`HU-07`), apertura de sesión `NO_REALIZADA` pasada en `SesionDetalleActivity` (`HU-13`/`HU-14`), y rotación sin pérdida de estado (`RNF-10`).
- [ ] 5.2 Ejecutar revisión con `revisor-java` (`.claude/agents/revisor-java.md`) sobre todo el código y XML de E4, corregir hallazgos, comprobar `./gradlew assembleDebug test lint` y `connectedDebugAndroidTest` en emuladores disponibles (`RNF-17`), realizar `git commit` por ruta explícita (`test(HU-16): agregar pruebas Espresso E2E y cierre de E4`) y ejecutar `git push`.

---

## 6. Documentación académica, diagramas y auditoría IA

- [ ] 6.1 Corregir `flujo_pantallas()` en `docs/build/diagramas.py` (prevalece `HU-16`: en `Mes` tocar un día lista sus sesiones debajo; `Semana`/`Día` crean por pulsación larga en `fecha >= hoy`; mantener `HU-34` en E9), actualizar `docs/build/diagramas_clases.py` y `docs/build/cotejo_clases.py` con las nuevas clases de E4 y diagrama de secuencia de consulta de calendario, y regenerar figuras comprobando `python docs/build/cotejo_clases.py` con 0 desfases.
- [ ] 6.2 Actualizar `docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md` (criterios de `HU-20` y `HU-21` acorde con el delta spec) y `docs/documento/capitulos/04-diseno-del-software.md` (secciones 4.2, 4.3 y 4.4 con las figuras regeneradas), actualizar `docs/auditoria-ia/bitacora.md`, validar con `python .apa/scripts/revisar.py --doc docs/documento`, generar el `.docx` con `python .apa/scripts/generar_docx.py --doc docs/documento` y confirmar `openspec validate add-calendario-estudio --strict`.
- [ ] 6.3 Realizar `git commit` por ruta explícita (`docs: actualizar diagramas, capitulos 3 y 4 y bitacora para epica E4`) y ejecutar `git push`.

---

## Workflow follow-up

- Archivar el change `add-calendario-estudio` con `/opsx:archive` una vez completados los 6 bloques y aprobada la verificación final por el estudiante (`AGENTS.md` §3 paso 5), incorporando en `openspec/specs/calendario-estudio/spec.md` los deltas de `HU-20` y `HU-21`.
- Las épicas posteriores (`E5` ejecución en vivo/Pomodoro, `E6` metas, `E7` estadísticas, `E8` reportes, `E9` recordatorios `HU-34` y `E10` respaldo Drive) se planificarán en sus propios changes OpenSpec sin contaminar E4.
