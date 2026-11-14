# Proposal

## Why

La aplicación ya cuenta con perfil, ajustes y bloqueo (E1) y catálogo de materias activas y archivadas (E2), pero todavía no permite planificar ni registrar sesiones de estudio. El estudiante ve la meta semanal en `0 / meta h` en Inicio, las materias muestran siempre `0 h` y la regla RN-03 que protege a las materias con historial depende aún del adaptador provisional `SinSesionesDeMateria`. Por orden del backlog y dependencias, la siguiente épica es **E3: Sesiones de estudio** (`RN-07` y `HU-10` a `HU-15`), que además cierra los escenarios pendientes de `HU-02`, `HU-08` y `HU-09`.

## What Changes

- **Máquina de estados del ciclo de vida de la sesión (`RN-07`):** implementación en `domain` (Java puro, sin `android.*` ni excepciones de validación) del patrón **State (GoF)** mediante `EstadoSesion`, `CodigoEstadoSesion` y las seis clases de estado (`EstadoPendiente`, `EstadoEnCurso`, `EstadoNoRealizada`, `EstadoCompletada`, `EstadoCompletadaTardia`, `EstadoReprogramada`) cubriendo los 13 escenarios especificados.
- **Planificar sesión de estudio (`HU-10`):** selección de materia activa, fecha hoy o futura, hora de inicio y fin con validación `horaFin > horaInicio` (`RN-04`), notas opcionales y bloqueo ante solapamientos en el mismo día indicando la sesión en conflicto (`RN-05`). Crea la sesión en estado `PENDIENTE` con la duración planificada en minutos.
- **Registrar sesión pasada (`HU-11`):** registro retroactivo en fechas de 1 a 3 días hacia atrás (`RN-06`) en estado `COMPLETADA_TARDIA` con duración real en minutos (`> 0`, `RN-26`) y suma de horas reales (`RN-11`), rechazando fechas de 4 días o más con aviso explícito del límite de 3 días.
- **Completar sesión (`HU-12`):** marcar una sesión `PENDIENTE` del día como `COMPLETADA`, prellenando la duración real con la planificada pero editable por el estudiante (`RN-26`), sumando al progreso semanal y ocultando o rechazando transiciones no válidas según el estado (`RN-07`).
- **Vencimiento automático y registro tardío (`HU-13`):** transición automática e idempotente (`RN-08`) de toda sesión `PENDIENTE` cuya fecha es anterior a hoy hacia `NO_REALIZADA` al iniciar o reanudar la app y antes de consultar sesiones; en sesiones `NO_REALIZADA` con antigüedad `<= 3` días se ofrece «Registrar tardío» (`COMPLETADA_TARDIA`, `RN-10`), y con `> 3` días solo se ofrece «Reprogramar».
- **Reprogramar sesión conservando el historial (`HU-14`):** reprogramación de una sesión `PENDIENTE` o `NO_REALIZADA` a una nueva fecha (hoy o futura) y nuevo horario validando `RN-04` y `RN-05`: la sesión original pasa a `REPROGRAMADA` y se crea en la misma transacción una nueva sesión `PENDIENTE` vinculada (`sesionOrigenId`, `RN-09`), mostrando el enlace navegable hacia la nueva sesión en el detalle de la original.
- **Eliminar sesión planificada (`HU-15`):** eliminación con diálogo de confirmación únicamente para sesiones en estado `PENDIENTE` (`RN-12`) con recálculo de resúmenes; las demás sesiones quedan protegidas en UI y en dominio.
- **Cierre de pendientes de E1 y E2:**
  - `HU-08` / `RN-03`: sustitución de `SinSesionesDeMateria` por `SesionesDeMateriaRoom` conectado a `SesionDao`, bloqueando la eliminación de materias con sesiones registradas y sugiriendo archivarlas.
  - `HU-09` / `RN-11`: cálculo real de horas acumuladas por materia en `ListarMateriasService` sumando `duracionRealMinutos` de sesiones `COMPLETADA` y `COMPLETADA_TARDIA`.
  - `HU-02`: pantalla de **Inicio** completa en `MainActivity` y `ObtenerResumenInicioService` mostrando horas estudiadas en la semana actual (lunes a domingo, `RN-13`) frente a la meta (`RN-15`), tarjeta de próxima sesión `PENDIENTE`, lista de sesiones de hoy con icono y etiqueta de estado (`RNF-06`) y acceso rápido «+ Nueva sesión» con la fecha de hoy preseleccionada.
- **Persistencia Room v3:** nueva entidad `SesionEntity` (`sesion_estudio`), `SesionDao`, `MIGRATION_2_3` probada con `MigrationTestHelper` y esquema `3.json` versionado.
- **Calidad visual y accesibilidad XML (`AGENTS.md` §6.2):** todos los layouts nuevos y los existentes pendientes cumplen `tools:context`, `tools:fitsSystemWindows="true"`, `tools:tint`, `tools:text`, `android:id` en `<include>`, `minHeight="48dp"`, `contentDescription` / `importantForAccessibility`, límites `maxWidth="400dp"` / `600dp` y variante `layout-w600dp/`.
- **Documentación y trazabilidad en paralelo:** actualización de diagramas (`diagramas_clases.py`, `cotejo_clases.py`, modelo lógico y diccionario desde `3.json`, secuencia de sesión, flujo de pantallas), capítulo 4 y bitácora IA.

**Fuera de alcance:**
- Vistas de calendario mensual, semanal y diario, leyenda y filtro por materia (`HU-16` a `HU-21`, Épica E4 `calendario-estudio`).
- Cronómetro en vivo en estado `EN_CURSO`, técnica Pomodoro configurable, modo No molestar y línea de tiempo de eventos (`HU-22` a `HU-24`, `HU-39` a `HU-44`, Épica E5 `tecnicas-estudio`). En E3 la máquina de estados `RN-07` implementa y prueba en JUnit las transiciones de `EN_CURSO`, pero su ejecución en UI corresponde a E5.
- Metas semanales específicas por semana o por materia y felicitación motivacional (`HU-25` a `HU-28`, Épica E6 `metas-semanales`).
- Gráficos MPAndroidChart, reportes PDF/CSV, recordatorios con `WorkManager`/`AlarmManager`, widget y copia de seguridad en Drive (E7 a E10).

## Capabilities

### New Capabilities
- Ninguna (todos los requisitos funcionales y de negocio ya forman parte de las specs consolidadas en `openspec/specs/`).

### Modified Capabilities
- `sesiones-estudio`: implementa `RN-07` y las historias `HU-10`, `HU-11`, `HU-12`, `HU-13`, `HU-14` y `HU-15`.
- `gestion-materias`: completa los escenarios pendientes de `HU-08` (`RN-03` real), `HU-07` (sesiones conservan la asociación tras editar materia) y `HU-09` (`RN-11` horas acumuladas reales).
- `perfil-ajustes`: completa los escenarios pendientes de `HU-02` («Resumen con datos» y «Acceso rápido» con fecha de hoy preseleccionada).

*(No se generan delta specs en este change — `skip_specs: true` — porque no se crean ni modifican requisitos de las specs vigentes).*

## Impact

- **Código Java (`app/`):**
  - `domain`: `Fecha`, `Hora`, `CodigoEstadoSesion`, `EstadoSesion` + 6 clases concretas State, `SesionEstudio`, `ConflictoHorario`, `DetalleSesion`, `DetalleSesionInicio`, `ErrorValidacion` (ampliado), `ResumenInicio` (ampliado).
  - `application.port.out`: `Reloj`, `SesionRepository`. Se mantiene `SesionesDeMateria` (cambiando su adaptador real).
  - `application.port.in` y `application.service`: `PlanificarSesion`, `RegistrarSesionPasada`, `CompletarSesion`, `VencerSesionesPendientes`, `RegistrarSesionTardia`, `ReprogramarSesion`, `EliminarSesion`, `ConsultarSesion`; ampliación de `ObtenerResumenInicioService` y `ListarMateriasService`.
  - `adapter.out.persistence`: `SesionEntity`, `SesionDao`, `SesionMapper`, `SesionRepositoryRoom`, `SesionesDeMateriaRoom` (elimina `SinSesionesDeMateria`), `RelojSistema`, `Migraciones.MIGRATION_2_3`, `StudyPlanDatabase` v3 y `app/schemas/.../3.json`.
  - `adapter.in.ui`: `MainActivity`, `InicioViewModel`, `SesionInicioAdapter`, `SesionFormularioActivity`, `SesionFormularioViewModel`, `SesionDetalleActivity`, `SesionDetalleViewModel`, `FabricaViewModels`, `StudyPlanApplication`, traducciones `values/strings.xml` y `values-en/strings.xml`, íconos vectoriales de estado y layouts XML (`layout/` y `layout-w600dp/`).
- **Dependencias:** ninguna dependencia nueva en `gradle/libs.versions.toml` ni en `app/build.gradle.kts` (`MaterialDatePicker` y `MaterialTimePicker` ya vienen incluidos en `com.google.android.material:material`).
- **Base de datos:** migración incremental `v2 → v3` sin pérdida de perfil, ajustes ni materias existentes.
- **Documento académico:** actualización de `docs/build/diagramas_clases.py`, `docs/build/cotejo_clases.py`, figuras en `docs/documento/recursos/diagramas/`, secciones 4.2, 4.3 y 4.4 de `docs/documento/capitulos/04-diseno-del-software.md`, y `docs/auditoria-ia/bitacora.md`.
