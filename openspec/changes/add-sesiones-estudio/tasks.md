# Tasks

> **Flujo de trabajo obligatorio (`AGENTS.md` §3 y política de commits del proyecto):**
> 1. Ningún bloque se implementa sin aprobación humana de esta propuesta.
> 2. Al completar cada bloque se ejecutan las pruebas aplicables, `ArquitecturaTest` y `./gradlew lintDebug`, se realiza `git commit` por ruta explícita (sin coautor de IA) y `git push`.
> 3. Cada layout XML nuevo o modificado debe cumplir al 100 % el checklist de `AGENTS.md` §6.2.

---

## Compuerta previa

- [x] 0.1 **Revisión humana.** El estudiante revisa y aprueba la propuesta `add-sesiones-estudio` (`proposal.md`, `design.md`, `trazabilidad.md` y `tasks.md`) conforme a `AGENTS.md` §3 antes de escribir código en `app/`. *(No requiere nuevas dependencias en Gradle)*. Aprobado por el estudiante al invocar `/openspec-apply-change add-sesiones-estudio` (2026-11-14).

---

## Bloque 1 — Dominio en Java puro y Patrón State (`domain`)

- [x] 1.1 Implementar los Value Objects inmutables `Fecha` (formato ISO `YYYY-MM-DD`, aritmética gregoriana proleptic pura compatible con API 24, `diasHasta`, `sumarDias`, `lunesDeSemana` y `domingoDeSemana` para `RN-13`) y `Hora` (formato `HH:mm`, `0..1439` minutos, `esPosteriorA`, `minutosHasta` para `RN-04`) en `domain`; verificar con `FechaHoraTest` (años bisiestos, fin de mes/año, lunes y domingo exactos, límites `00:00` y `23:59`, horarios iguales e invertidos).
- [x] 1.2 Ampliar `ErrorValidacion` con los códigos de sesión y crear `CodigoEstadoSesion`, la interfaz `EstadoSesion` y las seis clases de estado del patrón State GoF (`EstadoPendiente`, `EstadoEnCurso`, `EstadoNoRealizada`, `EstadoCompletada`, `EstadoCompletadaTardia`, `EstadoReprogramada`) devolviendo `Resultado` sin lanzar excepciones (`RN-07`, `RNF-14`); verificar con `EstadoSesionTest` los 13 escenarios completos de `RN-07`, incluyendo condición de mismo día al completar (`HU-12`), ventana de 3 días en registro tardío (`RN-10`), protección de eliminación (`RN-12`), rechazo en estados finales y consultas de capacidades (`puedeCompletar`, `puedeRegistrarTardio`, `puedeReprogramar`, `puedeEliminar`, `sumaHorasReales`).
- [x] 1.3 Crear la entidad inmutable `SesionEstudio`, `ConflictoHorario` y `ResultadoSesion` en `domain`: fábricas `planificar` (`HU-10`, `fecha >= hoy`, `RN-04`, notas ≤ 500), `registrarPasada` (`HU-11`, `1 <= diasAntiguedad <= 3` `RN-06`, duración real `> 0` `RN-26`) y `restaurar`, además de `seSolapaCon` (`RN-05`, excluyendo `REPROGRAMADA` y permitiendo sesiones contiguas); verificar con `SesionEstudioTest` e incluir la verificación en `ArquitecturaTest` (`domain` sin `android.*`).
- [x] 1.4 Ejecutar `./gradlew testDebugUnitTest lintDebug`, realizar `git commit` por ruta explícita (`feat(RN-07): implementar dominio y maquina de estados de sesion de estudio`) y ejecutar `git push`.

---

## Bloque 2 — Puertos de salida, casos de uso y servicios (`application`)

- [x] 2.1 Definir el puerto de salida `Reloj` (`hoy()`, `ahora()`) y `SesionRepository` en `application.port.out`, e implementar `PlanificarSesion` + `PlanificarSesionService` (`HU-10`, valida materia existente y activa, fecha, `RN-04` y cruce `RN-05` con `ConflictoHorario` detallado); verificar con `PlanificarSesionServiceTest` (alta válida 90 min, materia inexistente/archivada, fecha pasada, `RN-04`, cruce con nombre de materia y rango, sesiones adyacentes sin cruce).
- [x] 2.2 Implementar `VencerSesionesPendientes` + `VencerSesionesPendientesService` (`HU-13`, `RN-08`, convierte sesiones `PENDIENTE` con `fecha < hoy` en `NO_REALIZADA` respetando `EN_CURSO` `RN-29`); verificar idempotencia con `VencerSesionesPendientesServiceTest`.
- [x] 2.3 Implementar `RegistrarSesionPasada` + `RegistrarSesionPasadaService` (`HU-11`, `RN-06`: `1..3` días atrás en `COMPLETADA_TARDIA`, rechazo en `>= 4` días o futuro, `RN-04`, `RN-05`, `RN-26`); verificar con `RegistrarSesionPasadaServiceTest` (lunes desde jueves permitido, domingo desde jueves rechazado).
- [x] 2.4 Implementar `CompletarSesion` + `CompletarSesionService` (`HU-12`, `RN-07`, `RN-26`: completa `PENDIENTE` de hoy registrando duración real editable `> 0` y rechaza si es `REPROGRAMADA` u otro día); verificar con `CompletarSesionServiceTest`.
- [x] 2.5 Implementar `RegistrarSesionTardia` + `RegistrarSesionTardiaService` (`HU-13`, `RN-10`: convierte `NO_REALIZADA` de hace `<= 3` días en `COMPLETADA_TARDIA`, rechaza si `> 3` días); verificar con `RegistrarSesionTardiaServiceTest`.
- [x] 2.6 Implementar `ReprogramarSesion` + `ReprogramarSesionService` (`HU-14`, `RN-09`: reprograma `PENDIENTE` o `NO_REALIZADA` a fecha `>= hoy`, valida `RN-04` y `RN-05` excluyendo la original, marca original `REPROGRAMADA` y crea nueva `PENDIENTE` con `sesionOrigenId`); verificar con `ReprogramarSesionServiceTest`.
- [x] 2.7 Implementar `ConsultarSesion` + `ConsultarSesionService` (ejecuta vencimiento automático, devuelve `DetalleSesion` con materia, acciones permitidas y sesión hija si está `REPROGRAMADA`, `HU-12` a `HU-15`); verificar con `ConsultarSesionServiceTest`.
- [x] 2.8 Implementar `EliminarSesion` + `EliminarSesionService` (`HU-15`, `RN-12`: solo elimina `PENDIENTE`, rechaza `COMPLETADA`, `COMPLETADA_TARDIA`, `NO_REALIZADA` y `REPROGRAMADA`); verificar con `EliminarSesionServiceTest`.
- [x] 2.9 Ampliar `ResumenInicio` + `DetalleSesionInicio` y actualizar `ObtenerResumenInicioService` (`HU-02`, `RN-08`, `RN-11`, `RN-13`, `RN-15`: vence pendientes, suma minutos reales de lunes a domingo de la semana actual, determina próxima sesión `PENDIENTE` y lista sesiones de hoy ordenadas con estado y color/nombre de materia); verificar con `ObtenerResumenInicioServiceTest`.
- [x] 2.10 Actualizar `ListarMateriasService` (`HU-09`, `RN-11`: suma minutos reales de sesiones `COMPLETADA` y `COMPLETADA_TARDIA` por materia); verificar con `ListarMateriasServiceTest` que `PENDIENTE`, `NO_REALIZADA` y `REPROGRAMADA` no suman horas y las completadas sí.
- [x] 2.11 Ejecutar `./gradlew testDebugUnitTest lintDebug` (incluido `ArquitecturaTest`), realizar `git commit` por ruta explícita (`feat(HU-10): implementar casos de uso de sesiones y resumen real`) y ejecutar `git push`.

---

## Bloque 3 — Persistencia Room v3, migración y adaptadores reales (`adapter.out.persistence`)

- [ ] 3.1 Crear `SesionEntity` (`sesion_estudio`, FK a `materia(id) ON DELETE RESTRICT`, autorreferencia `sesion_origen_id` con `ON DELETE SET NULL`, 4 índices), `SesionDao` (con `@Transaction` para reprogramación atómica y consultas parametrizadas), `SesionMapper` y `RelojSistema`. Actualizar `StudyPlanDatabase` a `version = 3` con `Migraciones.MIGRATION_2_3` y exportar `app/schemas/.../3.json`.
- [ ] 3.2 Implementar `SesionRepositoryRoom` y reemplazar `SinSesionesDeMateria` por `SesionesDeMateriaRoom` conectado a `SesionDao.contarPorMateria`, actualizando `StudyPlanApplication` para componer el repositorio real, `RelojSistema`, `SesionesDeMateriaRoom` y los nuevos casos de uso (`HU-08`, `RN-03`).
- [ ] 3.3 Escribir `SesionRepositoryRoomTest` (CRUD, consultas por fecha y rango, suma real por materia y semana excluyendo no completadas, reprogramación transaccional, búsqueda de sesión hija, `ON DELETE RESTRICT` y `borrarTodo()`) y `Migracion3InstrumentalTest` con `MigrationTestHelper` (`v2 → v3` contra `3.json` conservando perfil, ajustes y materias, más actualización del test de versión actual).
- [ ] 3.4 Ejecutar `./gradlew assembleDebug testDebugUnitTest lintDebug` (y pruebas instrumentadas de BD/migración si hay emulador), realizar `git commit` por ruta explícita (`feat(HU-10): agregar persistencia Room v3 y adaptadores reales de sesiones`) y ejecutar `git push`.

---

## Bloque 4 — Presentación UI, ViewModels y layouts XML accesibles (`adapter.in.ui` y `res/`)

- [ ] 4.1 Agregar los 6 íconos vectoriales de estado en `res/drawable/` (`ic_estado_*.xml`), textos es/en en `values/strings.xml` y `values-en/strings.xml` (mensajes de error para cada nuevo código de `ErrorValidacion` y etiquetas de estado `RNF-06`) y actualizar `AndroidManifest.xml` con `SesionFormularioActivity` y `SesionDetalleActivity`.
- [ ] 4.2 Pulir todos los layouts existentes pendientes de ajustes menores (`activity_ajustes.xml`, `activity_materia_formulario.xml`, `activity_autenticacion.xml`, `item_materia.xml`, `item_encabezado_archivadas.xml`) para asegurar conformidad total con `AGENTS.md` §6.2 (`maxWidth`, `minHeight="48dp"`, `contentDescription`, `importantForAccessibility`, `tools:tint`, `tools:fitsSystemWindows="true"`).
- [ ] 4.3 Actualizar `InicioViewModel`, `MainActivity`, `SesionInicioAdapter`, `contenido_inicio.xml` y crear `item_sesion_inicio.xml` (con su soporte `w600dp`): progreso semanal real, botón «+ Nueva sesión» con fecha de hoy preseleccionada (`HU-02`), tarjeta de próxima sesión pendiente y lista de sesiones de hoy con color + icono + texto de estado (`RNF-06`), siguiendo estrictamente `AGENTS.md` §6.2 y enlazar vistas/listeners con nombre; verificar con `InicioViewModelTest`.
- [ ] 4.4 Crear `SesionFormularioViewModel`, `SesionFormularioActivity`, `activity_sesion_formulario.xml` y `layout-w600dp/activity_sesion_formulario.xml` soportando modos Planificar (`HU-10`), Sesión pasada (`HU-11`) y Reprogramar (`HU-14`), con selector Material de materia activa, fecha (`MaterialDatePicker` / campo validado), horas (`MaterialTimePicker` / campo validado), duración real editable y notas, cumpliendo todos los atributos de `AGENTS.md` §6.2; verificar con `SesionFormularioViewModelTest`.
- [ ] 4.5 Crear `SesionDetalleViewModel`, `SesionDetalleActivity`, `activity_sesion_detalle.xml` y `layout-w600dp/activity_sesion_detalle.xml` con detalle completo, enlace a la sesión reprogramada destino (`HU-14`), acciones dinámicas por estado (`Completar` con duración editable `HU-12`, `Registrar tardío` ≤ 3 días `HU-13`, `Reprogramar` `HU-14`, `Eliminar` con confirmación solo en `PENDIENTE` `HU-15`) y cumplimiento íntegro de `AGENTS.md` §6.2; verificar con `SesionDetalleViewModelTest` y actualizar `FabricaViewModels` + `StudyPlanApplication`.
- [ ] 4.6 Ejecutar `./gradlew assembleDebug testDebugUnitTest lintDebug`, realizar `git commit` por ruta explícita (`feat(HU-10): implementar pantallas de sesiones e inicio completo`) y ejecutar `git push`.

---

## Bloque 5 — Pruebas instrumentadas E2E y validación de calidad

- [ ] 5.1 Escribir `SesionesActivityEspressoTest` verificando en UI: planificar sesión válida (`HU-10`), bloqueo de horario inválido (`RN-04`) y cruce indicando sesión en conflicto (`RN-05`), registrar sesión pasada ≤ 3 días y rechazo a ≥ 4 días (`HU-11`), completar sesión del día con duración editada (`HU-12`), transición desde `NO_REALIZADA` (`HU-13`), reprogramación conservando enlace navegable a nueva sesión (`HU-14`) y eliminación confirmada de `PENDIENTE` más ocultación de eliminar en completada/reprogramada (`HU-15`), y rotación sin pérdida de estado (`RNF-10`).
- [ ] 5.2 Actualizar `InicioActivityEspressoTest` (resumen con datos reales y acceso rápido «+ Nueva sesión» con hoy preseleccionado, `HU-02`) y `MateriasActivityEspressoTest` (horas acumuladas reales `HU-09` y rechazo real al eliminar materia con sesiones `HU-08` / `RN-03`).
- [ ] 5.3 Ejecutar revisión con el agente `revisor-java` (`.claude/agents/revisor-java.md`) sobre todo el código y XML de E3, corregir hallazgos, comprobar `./gradlew assembleDebug test lint` y `connectedDebugAndroidTest` (en emuladores disponibles según `RNF-17`), realizar `git commit` por ruta explícita (`test(HU-10): agregar pruebas Espresso E2E y cierre de E3`) y ejecutar `git push`.

---

## Bloque 6 — Documentación académica, diagramas y auditoría IA

- [ ] 6.1 Actualizar `docs/build/diagramas_clases.py` y `docs/build/cotejo_clases.py` con las nuevas clases de E3 en `domain`, `application.*`, `adapter.out.persistence` y `adapter.in.ui`, añadir generación del modelo lógico y diccionario desde `app/schemas/.../3.json`, añadir diagrama de clases de E3 y secuencia de planificar/completar sesión, y regenerar figuras con `python docs/build/diagramas.py` y `python docs/build/diagramas_clases.py`.
- [ ] 6.2 Actualizar `docs/documento/capitulos/04-diseno-del-software.md` (secciones 4.2, 4.3 y 4.4 con las figuras regeneradas y la máquina de estados / migración v3) y ejecutar `python docs/build/cotejo_clases.py` comprobando que pasa sin desfases.
- [ ] 6.3 Actualizar `docs/auditoria-ia/bitacora.md` con las sesiones de implementación de E3, validar con `python .apa/scripts/revisar.py --doc docs/documento`, generar el `.docx` con `python .apa/scripts/generar_docx.py --doc docs/documento` y confirmar `openspec validate add-sesiones-estudio --strict`.
- [ ] 6.4 Realizar `git commit` por ruta explícita (`docs: actualizar diagramas, capitulo 4 y bitacora para epica E3`) y ejecutar `git push`.

---

## Workflow follow-up

- Archivar el change `add-sesiones-estudio` con `/opsx:archive` una vez completados los 6 bloques y aprobada la verificación final por el estudiante (`AGENTS.md` §3 paso 5).
- Las historias de calendario (`HU-16` a `HU-21`, E4) y ejecución en vivo / Pomodoro (`HU-22` a `HU-24`, `HU-39` a `HU-44`, E5) se planificarán en los siguientes changes OpenSpec reutilizando `Fecha`, `Hora`, `EstadoSesion`, `SesionRepository` y `Reloj`.
