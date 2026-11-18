# Tasks

> Requisito previo: `add-metas-semanales` archivado (Room v6, `tramo_diario`, `meta_semanal`). Dependencia nueva aprobada por el Product Owner: `MPAndroidChart`.

## 0. Dependencias y configuración Gradle

- [x] 0.1 Añadir JitPack en `settings.gradle.kts` acotado con `content { includeGroup("com.github.PhilJay") }`, declarar `com.github.PhilJay:MPAndroidChart:v3.1.0` en `gradle/libs.versions.toml` y `app/build.gradle.kts`, y verificar descarga, compilación e integridad R8 con `./gradlew assembleDebug assembleRelease`.

## 1. Dominio puro en Java (`domain`)

- [x] 1.1 Crear `TopicoEventoEstudio` (claves canónicas en BD + `SIN_CLASIFICAR` y `OTRO`) y `SalidaRegistrada` (`sesionId`, `nombreMateria`), y ampliar `EventoEstudio` con `topico` opcional (`Optional.empty()` ante `NULL`/vacío histórico) manteniendo compatibilidad de constructores, con pruebas en `EventoEstudioTest.java` y `ArquitecturaTest` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.2 Ampliar `ResumenEjecucionSesion` con `salidaPendienteResolver`, añadir utilidades puras en `CalculadorPomodoro` para detectar fase de descanso y fin proyectado, y ajustar `CalculadorTiempoReal` para calcular tiempo fuera sin sumar (`SALIDA_APP → REGRESO_APP` sin `ESTUDIO_APP_CERRADA`), pausas con cierre sin `REANUDACION` y duración agrupable por tópico, con pruebas en `CalculadorPomodoroTest.java` y `CalculadorTiempoRealTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.3a Crear `PeriodoEstadisticas` (delegando exclusivamente en `PeriodoCalendario.rangoVisibleInicio()` y `rangoVisibleFin()`), `DesgloseMateriaPeriodo` (incluyendo `minutosPlanificadosDeSesiones`, `sesionesNoRealizadas` y `cumplimientoPlanificado` como base de E8), `EstadisticasPeriodo` y la primera parte de `CalculadorEstadisticas` (`HU-29` total y variación semanal/mensual con corte `RN-31`, `HU-30` distribución incl. «Estudios varios» y archivadas, `HU-31` cumplimiento excluyendo vigentes), con pruebas en `CalculadorEstadisticasTest.java` y `ArquitecturaTest` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.3b Crear `ResumenTopico`, `ConcentracionPeriodo` y completar `CalculadorEstadisticas` para `HU-45` / `RN-30` (concentración sin `ESTUDIO_APP_CERRADA`, interrupciones sin penalizar estudio externo, puntualidad sobre sesiones con plan e `INICIO` en `/ 60_000.0`, respeto sobre `COMPLETADA` con plan e `INICIO`, guardas para denominador `0`, exclusión de tardías, estudio externo separado con `Sin clasificar` y `resumenTopicos` con respaldo `SIN_CLASIFICAR` en pausas sin `REANUDACION`), verificadas en `CalculadorEstadisticasTest.java` con `./gradlew testDebugUnitTest`.

## 2. Puertos y servicios (`application`)

- [x] 2.1a Ampliar `MarcadorSalidaApp` con marca separada de fin de descanso Pomodoro (`marcarFinDescansoPomodoro`, `obtenerFinDescansoPomodoro`, `limpiarFinDescansoPomodoro`), ampliar `RegistrarSalidaApp` para devolver `Optional<SalidaRegistrada>` consultando `MateriaRepository`, ajustar `RegistrarSalidaAppService` para omitir `SALIDA_APP` y notificaciones dentro de descanso Pomodoro y actualizar `EvaluarRecuperacionSesionService` para resolver descansos cumplidos o excesos desde `finDescansoMs`, con pruebas en `RecuperacionSalidaServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.1b Ampliar `EventoEstudioRepository` con `listarPorRangoSesionesUtc(long, long)`, añadir sobrecargas retrocompatibles con `topico` en `PausarReanudarSesion` y `ResolverRegresoApp` (opción `RETOMAR_SIN_SUMAR` y respaldo `SIN_CLASIFICAR`), actualizar *fakes* y adaptar `ConsultarEjecucionSesionService` (`salidaPendienteResolver` y `sesion.tienePlan()` en `mostrarFinPlanificado`), con pruebas en `EjecucionSesionServiceTest.java` y `RecuperacionSalidaServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.2 Crear el puerto `ConsultarEstadisticas` y `ConsultarEstadisticasService` orquestando `VencerSesionesPendientes`, `TramoDiarioRepository`, `SesionRepository`, `MateriaRepository`, `EventoEstudioRepository` y `Reloj` sobre el agregador reutilizable para E7 y E8, con pruebas en `ConsultarEstadisticasServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.

## 3. Persistencia Room v7 y marcador de sistema (`adapter.out.*`)

- [x] 3.1 Añadir la columna nullable `topico` en `EventoEstudioEntity`, actualizar `Mapeadores` (`null`/vacío → ausencia vs. `SIN_CLASIFICAR` explícito), implementar `listarPorRangoSesionesUtc` mediante consulta SQL directa con `JOIN sesion_estudio` en `EventoEstudioDao` / `EventoEstudioRepositoryRoom`, implementar los métodos de descanso Pomodoro en `MarcadorSalidaAppPrefs`, registrar `Migraciones.MIGRATION_6_7` en `StudyPlanDatabase.abrir(...)` (`version = 7`) y añadir pruebas unitarias de mapeo en `MapeadoresTest.java`, verificando `7.json` con `./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest`.
- [x] 3.2 Crear `Migracion7InstrumentalTest.java` (`v6 → v7` preservando eventos previos con `topico` nulo y eventos nuevos con clave canónica) y ampliar `EventoEstudioRepositoryRoomTest.java` (consulta `JOIN` en rango UTC y casos límite), verificadas con `./gradlew connectedDebugAndroidTest`.

## 4. Presentación XML, blindaje de ejecución y Estadísticas (`adapter.in.ui`, `res/`)

- [x] 4.1a Habilitar `comprobarRecuperacionSesionActiva()` al regresar a `SesionEnCursoActivity` en `ActividadProtegida`, propagar `SalidaRegistrada` desde `ContadorActividades` a `NotificadorSesion` con el nombre real de la materia y detener el incremento visual en `SesionEnCursoActivity` mientras `salidaPendienteResolver` sea `true`, con pruebas en `SesionEnCursoViewModelTest.java` verificadas con `./gradlew testDebugUnitTest lintDebug`.
- [x] 4.1b Añadir el selector rápido de tópico al pulsar Reanudar en `SesionEnCursoActivity` y agregar la 4.ª opción *«No estudié ese rato, retomo ahora»* + chips traducidos de tópico (`SIN_CLASIFICAR` por defecto en `Sigo estudiando`) en `DialogoRecuperacionSesion` y `dialogo_recuperacion_sesion.xml` (`es`/`en`), con pruebas en `SesionEnCursoViewModelTest.java` y `./gradlew lintDebug` sin errores (`AGENTS.md` §6.2).
- [x] 4.2a Crear `EstadisticasViewModel` y conectar `ConsultarEstadisticas` en `StudyPlanApplication` y `FabricaViewModels`, con pruebas en `EstadisticasViewModelTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 4.2b Crear `EstadisticasAdapter`, `EstadisticasActivity`, los layouts en `layout/` y `layout-w600dp/` (selector `Semana | Mes`, `< anterior | siguiente >`, `Resumen | Concentración`, `PieChart` con `android:importantForAccessibility="no"` + lista textual accesible `RNF-06`, tarjetas `HU-29` a `HU-45`, estudio externo separado con `Sin clasificar` y desglose por tópico), conectar `DestinoPrincipal.ESTADISTICAS` y `AndroidManifest.xml`, verificando `./gradlew assembleDebug lintDebug` sin errores (`AGENTS.md` §6.2).

## 5. Verificación E2E, revisión y trazabilidad

- [x] 5.1 Crear `EstadisticasEspressoTest.java` (pestaña Estadísticas, variación `Semana | Mes`, gráfico y lista por materia incl. «Estudios varios» y archivada, tasa de cumplimiento excluyendo pendientes futuras, subvista Concentración con tópicos y `Sin clasificar`) y ampliar `TecnicasEstudioEspressoTest.java` (regreso directo a `SesionEnCursoActivity` abriendo `W13`, 4.ª opción sin sumar, pausa con tópico al reanudar, ocultamiento de fin planificado en «Estudiar ahora» y salida dentro vs. fuera de descanso Pomodoro), verificando `./gradlew connectedDebugAndroidTest` y `openspec validate add-estadisticas --strict`.
- [x] 5.2 Revisar contra `.claude/agents/revisor-java.md` y verificar la compilación, pruebas y lint completos con `./gradlew assembleDebug testDebugUnitTest lintDebug` (0 errores, 0 `MissingTranslation`, 0 `HardcodedText`).
- [x] 5.3 Registrar en `docs/auditoria-ia/bitacora.md` las decisiones, errores corregidos, verificación manual de versión `MPAndroidChart` y resultados de pruebas; y alinear diagramas y capítulos del documento tras validar la implementación.

## Workflow follow-up

- Revisar el código e interfaz en emulador real antes de archivar con `/opsx:archive` (`openspec archive add-estadisticas`).
- A partir de `DesgloseMateriaPeriodo` y `ConsultarEstadisticas`, redactar el siguiente *change* para **E8 (`reportes`: `HU-32` y `HU-33`)**.
