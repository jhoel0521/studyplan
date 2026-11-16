# Tasks

## 1. Dominio puro en Java (`domain`)

- [x] 1.1 Crear `Instante` (`utcMilis`, `desfaseMinutos`, `fechaLocal()`, `horaLocal()`, `inicioDelDiaLocal()`, comparación), con pruebas en `InstanteTest.java` para desfases negativos y positivos, medianoche y fechas anteriores a 1970, verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.2 Cambiar `SesionEstudio` a inicio y fin absolutos con un desfase, conservar `fecha()`, `horaInicio()` y `horaFin()` como accesores derivados, y aplicar `RN-04` (fin anterior a inicio = día siguiente, igual = error, menos de 24 h) y `seSolapaCon` entre días (`RN-05`), con pruebas en `SesionEstudioTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.3 Agregar el desfase a `EventoEstudio` y crear `RepartidorPorDia` (corte de `IntervaloEfectivo` en la medianoche local y reparto proporcional del registro tardío), con pruebas en `RepartidorPorDiaTest.java` (22:00–01:00, domingo–lunes, pausa que cruza la medianoche, cambio de horario) verificadas con `./gradlew testDebugUnitTest`.

## 2. Puertos y servicios (`application`)

- [x] 2.1 Agregar `ahoraInstante()` a `Reloj`, agregar a `SesionRepository` la consulta por rango UTC y a un nuevo puerto `TramoDiarioRepository` la suma por rango de fechas locales, y actualizar los *fakes* (`FakeReloj` con desfase fijo), verificando la compilación de pruebas con `./gradlew testDebugUnitTest`.
- [x] 2.2 Adaptar `PlanificarSesionService`, `RegistrarSesionPasadaService`, `ReprogramarSesionService` y `ValidadorSesiones` a instantes (`RN-04`, `RN-05`), con pruebas en sus `*ServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.3 Adaptar `VencerSesionesPendientesService` a `RN-08` sobre el día del fin planificado, manteniendo `RN-29` para las sesiones En curso, con pruebas en `VencerSesionesPendientesServiceTest.java` (sesión nocturna aún vigente a medianoche) verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.4 Escribir `tramo_diario` en cada cierre (`DetenerSesion`, cambio a la siguiente, `RepartirTramoSesion`, registro tardío y cierre por plazo), con pruebas en `EjecucionSesionServiceTest.java` y `RepartoTramoServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.5 Cambiar `ObtenerResumenInicioService` y `ConsultarCalendarioService` para sumar desde `tramo_diario` y listar por rango local (`RN-31`), con pruebas en `ObtenerResumenInicioServiceTest.java` y `ConsultarCalendarioServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.

## 3. Persistencia Room v5 (`adapter.out.persistence`)

- [x] 3.1 Cambiar `SesionEntity` (`inicio_utc_milis`, `fin_utc_milis`, `desfase_minutos`, índice por inicio), `EventoEstudioEntity` (`desfase_minutos`, sin `fecha` ni `hora`), crear `TramoDiarioEntity` y su DAO, y actualizar `SesionDao`, `Mapeadores` y `StudyPlanDatabase` (`version = 5`), verificando el esquema `5.json` con `./gradlew assembleDebug assembleDebugAndroidTest`.
- [x] 3.2 Escribir `Migraciones.MIGRATION_4_5` (reconstrucción de tablas, conversión con el desfase del teléfono, desfase exacto por evento y llenado de `tramo_diario`), con `Migracion5InstrumentalTest.java` (`MigrationTestHelper` `v4 → v5` con sesiones, eventos y claves foráneas) verificada con `./gradlew connectedDebugAndroidTest`.
- [x] 3.3 Implementar `ahoraInstante()` en `RelojSistema` y `TramoDiarioRepositoryRoom`, con `TramoDiarioRepositoryRoomTest.java` verificada con `./gradlew connectedDebugAndroidTest`.
- [x] 3.4 Renombrar las columnas `inicio_utc_milis` y `fin_utc_milis` de `sesion_estudio` a `plan_inicio_utc_milis` y `plan_fin_utc_milis` (y su índice), y los campos y accesores Java a `planInicioUtcMilis` y `planFinUtcMilis`, directamente en `SesionEntity`, `SesionDao`, `Mapeadores`, `SesionEstudio`, `RepartidorPorDia`, `MIGRATION_4_5`, los *fakes* y las pruebas, sin migración nueva porque v5 no se ha publicado; regenerar `5.json` y verificar con `./gradlew assembleDebug testDebugUnitTest` y `Migracion5InstrumentalTest` en `./gradlew connectedDebugAndroidTest`.

## 4. Presentación (`adapter.in.ui`, `res/`)

- [x] 4.1 Ajustar `SesionFormularioActivity` y su `ViewModel` para aceptar el fin al día siguiente con la etiqueta «Termina al día siguiente» y el nuevo error de fin igual a inicio (`strings.xml` `es` y `en`), con pruebas en `SesionFormularioViewModelTest.java` y `./gradlew lintDebug` sin errores.
- [x] 4.2 Mostrar las horas con el desfase guardado y la indicación de zona cuando difiere del teléfono en `FormatoSesion`, `SesionDetalleActivity` y `EventoEstudioAdapter`, con pruebas unitarias del formateador verificadas con `./gradlew testDebugUnitTest lintDebug`.
- [x] 4.3 Dibujar las sesiones nocturnas en dos bloques en las vistas Semana y Día del calendario e indicar el día de inicio en la vista Día, con pruebas en `CalendarioViewModelTest.java` y `./gradlew lintDebug` sin errores.

## 5. Verificación E2E

- [x] 5.1 Agregar a `SesionesEspressoTest` el caso de planificar de 22:00 a 01:00 y verlo en la Semana en dos días, ejecutar `./gradlew connectedDebugAndroidTest` (incluidas las pruebas de E3, E4 y E5 como regresión) con 0 fallos, y ejecutar `openspec validate add-tiempo-utc --strict`.
- [x] 5.2 Revisar con `.claude/agents/revisor-java.md` y verificar `./gradlew assembleDebug testDebugUnitTest lintDebug` con 0 errores y `ArquitecturaTest` en verde.

## 6. Documentación y bitácora

- [x] 6.1 Actualizar el capítulo 3 (`RN-04` y `RN-08` con el nuevo texto, `RN-31` en la tabla de reglas con origen «Product Owner, 16-nov-2026») y el capítulo 4 (modelo lógico v5, diccionario de `tramo_diario` y de las columnas nuevas) mediante `docs/build/diagramas.py` y `docs/build/diagramas_clases.py`, verificando `python docs/build/cotejo_clases.py` con 0 desfases y `python .apa/scripts/revisar.py --doc docs/documento`.
- [x] 6.2 Registrar la implementación, las decisiones y las verificaciones en `docs/auditoria-ia/bitacora.md`.

## Workflow follow-up

- Archivar `add-tiempo-utc` con `/opsx:archive` tras la revisión humana, antes de aplicar `add-metas-semanales`.
