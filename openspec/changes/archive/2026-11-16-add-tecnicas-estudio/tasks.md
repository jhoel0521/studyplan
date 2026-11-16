# Tasks

## 1. Dominio puro en Java (`domain`)

- [x] 1.1 Crear `TecnicaEstudio`, `TipoEventoEstudio` (con `esMarcaDentroDeApp()`), `PreferenciaNoMolestar` (tri-estado y conversión `Boolean`), `ConfiguracionPomodoro` (rangos `1..180`, `1..60`, `1..90`, `1..12` y `porDefecto()`) y ampliar `ErrorValidacion` y `Ajustes` (`noMolestarGlobal`, `pomodoro`), con pruebas unitarias en `AjustesPomodoroTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.2 Crear `EventoEstudio` (inmutable con `instanteMilis`, `Fecha`, `Hora`, `nota <= 500`), actualizar `EstadoEnCurso.completarConDuracion` (`>= 0`) y `EstadoPendiente.puedeCompletar` (`false` para exigir flujo en vivo `HU-12`/`RN-07`), exponer en `SesionEstudio` las transiciones en vivo (`iniciar`, `pausarOReanudar`, `detener`, `cambiarASiguiente`, `declararFinAlVolver`, `cerrarPorPlazoVencido`, `conPreferenciaNoMolestar`, *snapshot* Pomodoro y `debeActivarNoMolestar`), con pruebas unitarias en `SesionEstudioEnCursoTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.3 Crear `IntervaloEfectivo` y `CalculadorTiempoReal` (reconstrucción de intervalos, cálculo `LIBRE` / `POMODORO`, `estaPausadaActualmente`, `tieneSalidaSinResolver`, `obtenerUltimoInstanteDentroDeApp`), con pruebas en `CalculadorTiempoRealTest.java` (pausas, `0` min, multi-día `RN-29`, fallo sin salida previa `SIGKILL`) verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.4 Crear `FasePomodoro`, `EstadoPomodoroCalculado` y `CalculadorPomodoro` (`pomodorosQueCabenEnPlan` y derivación determinista de fases/extensión), con pruebas en `CalculadorPomodoroTest.java` (`25/5/15/4` en `120` min, descansos excluidos `RN-11`, `+1 pomodoro`, tiempo extra libre) verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.5 Crear `CalculadorRepartoTramo` y `ResultadoReparto`, ampliar `DetalleSesion` (`eventos`, `puedeIniciar`) y crear `ResumenEjecucionSesion`, `DiagnosticoRegresoApp` y `CandidataInvasion`, con pruebas en `CalculadorRepartoTramoTest.java` y `ArquitecturaTest` verificadas con `./gradlew testDebugUnitTest`.

## 2. Puertos y servicios de aplicación (`application`)

- [x] 2.1 Ampliar `Reloj` (`ahoraMilis()`), `SesionRepository` (`buscarEnCurso()`, `listarEnCurso()`, `completarReparto(...)`) y crear puertos `EventoEstudioRepository`, `ProgramadorAvisosSesion`, `ControladorNoMolestar` y `MarcadorSalidaApp`, actualizando los *fakes* de prueba (`FakeReloj`, `FakeAjustesRepository`, `FakeSesionRepository` y nuevos *fakes*) y verificando compilación de pruebas con `./gradlew testDebugUnitTest`.
- [x] 2.2 Implementar `ConfigurarAjustesEstudio` (`RN-17`), `IniciarSesion` (invariante única `EN_CURSO` -> `SESION_YA_EN_CURSO`, *snapshot* Pomodoro, `INICIO`, No molestar, alarma), `PausarReanudarSesion`, `DetenerSesion` y `ConsultarEjecucionSesion`, con pruebas unitarias en `EjecucionSesionServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.3 Implementar `RegistrarSalidaApp` (`RN-23`), `EvaluarRecuperacionSesion` (cierre `> 3` días `RN-29`, regreso con `SALIDA_APP` o recuperación de `SIGKILL`), `ResolverRegresoApp` (`NO_SEGUI`, `HASTA_HORA`, `SIGO_ESTUDIANDO`, `RN-24`), y ampliar `VencerSesionesPendientesService` (`RN-29`), con pruebas en `RecuperacionSalidaServiceTest.java` y `VencerSesionesPendientesServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.4 Implementar `DecidirFinPlanificadoOInvasion` (`HU-42`, `HU-23`, `HU-43`/`RN-27`), `RepartirTramoSesion` (`HU-44`/`RN-28`) y ampliar `ConsultarSesionService` y `BorrarDatosService`, con pruebas en `RepartoTramoServiceTest.java` y `ArquitecturaTest` verificadas con `./gradlew testDebugUnitTest`.

## 3. Persistencia Room v4 y adaptadores de sistema (`adapter.out.*`)

- [x] 3.1 Ampliar `AjustesEntity`, `SesionEntity`, crear `EventoEstudioEntity` y `EventoEstudioDao`, actualizar `SesionDao` (`buscarEnCurso`, `listarEnCurso`, `@Transaction completarReparto`), `Mapeadores`, `StudyPlanDatabase` (`version = 4`, esquema `4.json`) y `Migraciones.MIGRATION_3_4`, verificando compilación y exportación de esquema con `./gradlew assembleDebug assembleDebugAndroidTest`.
- [x] 3.2 Implementar `EventoEstudioRepositoryRoom`, actualizar `SesionRepositoryRoom` y `RelojSistema`, y crear `Migracion4InstrumentalTest.java` (`MigrationTestHelper` `v3 -> v4` con datos previos y FK en cascada) y `EventoEstudioRepositoryRoomTest.java`, verificando `./gradlew testDebugUnitTest`.
- [x] 3.3 Crear paquete `adapter.out.system` con `ControladorNoMolestarSistema` (persistencia de `interruption_filter_previo`, verificación de permiso al aplicar/restaurar), `ProgramadorAvisosAlarmManager` (`setAlarmClock()` y cancelación) y `MarcadorSalidaAppPrefs`, junto con pruebas unitarias de comportamiento y verificación de `./gradlew assembleDebug testDebugUnitTest lintDebug`.

## 4. Presentación, avisos y layouts XML (`adapter.in.ui`, `res/`, `AndroidManifest.xml`)

- [x] 4.1 Declarar permisos (`POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED`) y componentes en `AndroidManifest.xml`, crear `NotificadorSesion` (canales y comprobación `POST_NOTIFICATIONS`) y `ReceptorAvisosSesion` (`BOOT_COMPLETED`, acciones de notificación y alarmas), y actualizar `FabricaViewModels` y `StudyPlanApplication`, verificando `./gradlew assembleDebug lintDebug`.
- [x] 4.2 Ampliar `ContadorActividades` (detección de salida/regreso en hilo de fondo vía `MarcadorSalidaApp` y `RegistrarSalidaApp`) y `ActividadProtegida.alEstarListo()` (presentación de `DialogoRecuperacionSesion` `W13` tras biometría/PIN), crear `DialogoRecuperacionSesion` y `dialogo_recuperacion_sesion.xml` (con `strings.xml` `es`/`en`), verificando `./gradlew assembleDebug lintDebug`.
- [x] 4.3 Ampliar `AjustesActivity`, `AjustesViewModel` y `activity_ajustes.xml` (`W10`: switch No molestar con diálogo explicativo + campos Pomodoro `minHeight="48dp"`, `maxWidth="400dp"`), y ampliar `SesionDetalleActivity`, `SesionDetalleViewModel`, `activity_sesion_detalle.xml` (`layout/` y `layout-w600dp/`), `EventoEstudioAdapter` e `item_evento_estudio.xml` (retirar `botonCompletarSesion` y restringir `grupoDuracionRealDetalle` solo a `puedeRegistrarTardio()` de `HU-13`; añadir selector técnica, 3 estados DND, botón Iniciar/Ir a sesión en curso, línea de tiempo `W15`), verificando `./gradlew assembleDebug lintDebug`.
- [x] 4.4 Crear `SesionEnCursoActivity`, `SesionEnCursoViewModel` y `activity_sesion_en_curso.xml` (`layout/` y `layout-w600dp/`, `W7`: cronómetro con `Handler` en `onStart`/`onStop`, fase/ciclo Pomodoro, Pausar/Reanudar/Detener, switch No molestar, tarjetas contextuales de extensión/fin/invasión), con pruebas unitarias en `SesionEnCursoViewModelTest.java` verificadas con `./gradlew testDebugUnitTest lintDebug`.
- [x] 4.5 Crear `RepartoSesionActivity`, `RepartoSesionViewModel` y `activity_reparto_sesion.xml` (`layout/` y `layout-w600dp/`, `W14`: barra proporcional + `Slider` paso `5` min + botones `− 5 min` / `+ 5 min` de `48dp`), con pruebas unitarias en `RepartoSesionViewModelTest.java` verificadas con `./gradlew assembleDebug testDebugUnitTest lintDebug`.

## 5. Verificación E2E, migración en dispositivo y auditoría técnica

- [x] 5.1 Crear `TecnicasEstudioEspressoTest.java` cubriendo Libre (`HU-22`), Pomodoro y Ajustes (`HU-23`), No molestar con permiso denegado (`HU-24`, `RNF-18`), línea de tiempo (`HU-39`), diálogo `W13` (`HU-41`), reparto `W14` (`HU-44`) y rotación (`RNF-10`), y ejecutar `./gradlew connectedDebugAndroidTest` en emulador verificando `Migracion4InstrumentalTest`, `EventoEstudioRepositoryRoomTest` y `TecnicasEstudioEspressoTest` con 0 fallos.
- [x] 5.2 Ejecutar auditoría con `.claude/agents/revisor-java.md`, verificar `./gradlew assembleDebug testDebugUnitTest lintDebug` (0 errores, 0 `MissingTranslation`, 0 `HardcodedText`) y `openspec validate add-tecnicas-estudio --strict`.

## 6. Documentación, diagramas y bitácora

- [x] 6.1 Actualizar `AGENTS.md` (paquete `adapter.out.system` en §6), `docs/build/diagramas.py` (regenerar `maquina_estados.png` sin la flecha directa `marcar completada [mismo día]`), `docs/build/diagramas_clases.py` (bloque `add-tecnicas-estudio` y paquete `adapter/out/system`), ejecutar `python docs/build/cotejo_clases.py` (0 desfases) y actualizar `docs/documento/capitulos/04-diseno-del-software.md` (modelo lógico v4, diccionario de datos, diagramas), verificando `python .apa/scripts/revisar.py --doc docs/documento`.
- [x] 6.2 Registrar la implementación, decisiones y verificaciones de E5 en `docs/auditoria-ia/bitacora.md`.

## Workflow follow-up

- Archivar el change `add-tecnicas-estudio` con `/opsx:archive` tras la revisión y aprobación humana de todas las tareas y pruebas.
