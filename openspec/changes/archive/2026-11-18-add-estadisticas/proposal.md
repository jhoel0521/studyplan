# Proposal

## Why

La épica E7 (estadísticas, `HU-29`, `HU-30`, `HU-31` y `HU-45`) es la siguiente del Product Backlog y habilita la pestaña **Estadísticas** ya reservada en la barra de navegación principal. Durante la exploración práctica en la aplicación antes de diseñar E7, el Product Owner detectó además huecos reales en la medición del tiempo de ejecución (`E5`): al volver desde otra app directo a `SesionEnCursoActivity` no se mostraba el diálogo de regreso (`W13`), el reloj visual seguía avanzando con `SALIDA_APP` sin resolver, las sesiones sin plan (`Estudiar ahora`) mostraban el aviso de fin planificado desde el inicio, no existía opción para retomar tras una interrupción breve sin sumar ese tiempo ni para capturar el **tópico** (motivo) en pausas y salidas (con `Sin clasificar` por omisión), y salir durante un descanso Pomodoro congelaba el descanso. Blindar esas variables en este mismo *change* garantiza que las métricas de **E7** reflejen la realidad y deja un agregador de dominio reutilizable como base directa para **E8 (Reportes)**.

## What Changes

- **Blindaje de captura de tiempo, salidas y tópicos (`tecnicas-estudio`, `RN-22` a `RN-25`, `RN-29`, `RN-30`):**
  - Mostrar el diálogo de recuperación (`W13`) al regresar tras cualquier salida sin resolver en **cualquier pantalla protegida, incluida `SesionEnCursoActivity`** (y pasar el nombre real de la materia en la notificación de retorno), deteniendo el avance visual del cronómetro mientras siga pendiente la respuesta y sin volver a preguntar por salidas ya resueltas.
  - Ocultar el aviso y la tarjeta «¿Sigues estudiando?» (`FIN_PLANIFICADO`) cuando la sesión no tiene horario planificado (`tienePlan() == false`, `HU-47`).
  - Respetar los descansos programados de **Pomodoro**: si el estudiante sale de la app durante `DESCANSO_CORTO` o `DESCANSO_LARGO` y regresa antes de que termine, el descanso continúa sin registrar `SALIDA_APP` ni abrir `W13`; si regresa después de terminado el descanso, se evalúa el tramo excedido desde el fin del descanso.
  - Ampliar `W13` con la 4.ª opción **«No estudié ese rato, retomo ahora»** (graba `REGRESO_APP` sin `ESTUDIO_APP_CERRADA` y continúa `EN_CURSO` sin sumar esos minutos como estudio) y añadir selector rápido de **tópico** (chips en un toque + texto opcional en «Otro» + **`Sin clasificar`** como opción rápida por defecto) tanto en `W13` como al **reanudar** una `PAUSA` manual.
  - Persistir el tópico en `evento_estudio.topico` (`Room v7`, `MIGRATION_6_7`).
- **Estadísticas y métricas de estudio (`estadisticas`: `HU-29`, `HU-30`, `HU-31`, `HU-45`, `RN-11`, `RN-13`, `RN-30`, `RN-31`):**
  - Activar la pestaña `Estadísticas` en `DestinoPrincipal` (`EstadisticasActivity`) con selector de periodo **`Semana | Mes`** y navegación `< anterior | siguiente >` más las subvistas **`Resumen`** y **`Concentración`**.
  - **Total y variación (`HU-29`):** total de horas reales en el periodo desde `TramoDiarioRepository` (`RN-31`) y variación porcentual respecto al periodo anterior (`vs. semana anterior` / `vs. mes anterior`), omitiendo la variación si el periodo previo no tuvo minutos.
  - **Distribución por materia (`HU-30`):** gráfico de anillo interactivo con **`MPAndroidChart`** (`PieChart`, dependencia aprobada explícitamente por el Product Owner) coloreado por materia (incluida «Estudios varios» y materias archivadas con horas en el periodo).
  - **Tasa de cumplimiento (`HU-31`):** vence primero pendientes vencidas (`RN-08`) y calcula el porcentaje y conteo por estado sobre sesiones ya resueltas del periodo (`COMPLETADA`, `COMPLETADA_TARDIA`, `NO_REALIZADA`, `REPROGRAMADA`), excluyendo del denominador las aún `PENDIENTE`/`EN_CURSO`.
  - **Concentración, interrupciones y tópicos (`HU-45`, `RN-30`):** calcula índice de concentración, interrupciones por hora (`PAUSA` + salidas resueltas como *No estudié ese rato* o *Terminé al salir*, sin penalizar salidas declaradas como estudio externo en PDF/Moodle), puntualidad promedio y respeto del tiempo (solo en sesiones con horario planificado), tiempo fuera de la app (informativo), estudio autodeclarado separado (mostrando `Sin clasificar` frente a clasificados) y desglose por **tópicos** de pausas e interrupciones.
- **Base arquitectónica para E8 (`reportes`):** encapsular el agregado por periodo y materia (`EstadisticasPeriodo` / `DesgloseMateriaPeriodo`: horas planificadas, reales, cumplimiento y no realizadas) en `domain` y `application` para reutilizarlo directamente en la generación de reportes PDF/CSV de E8 sin duplicar cálculos.

**Fuera de alcance:**
- Generación y exportación de reportes en PDF o CSV (`E8`: `HU-32`, `HU-33`), que consumirá el modelo base aquí definido en el siguiente *change*.
- Recordatorios previos de sesión (`E9`: `HU-34`), widget de inicio (`HU-05`) y respaldo en Google Drive (`E10`: `HU-35` a `HU-38`, diferido fuera del MVP).
- Bloqueo físico del dispositivo por código (`DevicePolicyManager.lockNow()`), para evitar requerir permisos de administrador del dispositivo.
- Modificar el avance del bloque Pomodoro actual cuando el estudiante declara `ESTUDIO_APP_CERRADA`: conserva `RN-24` (suma íntegra aparte al total real sin distorsionar el temporizador dentro de la app).

## Capabilities

### New Capabilities
- Ninguna (las capacidades ya existen en `openspec/specs/`).

### Modified Capabilities
- `tecnicas-estudio`: ajuste de `HU-22`, `HU-39`, `HU-40`, `HU-41` y `HU-42` para recuperar `W13` al volver directo a `SesionEnCursoActivity`, pausar el reloj visual ante `SALIDA_APP` abierta, excluir descansos Pomodoro programados mientras duren, ocultar fin planificado en sesiones sin plan, añadir la opción de retomar sin sumar tiempo y capturar `topico` (incluido `Sin clasificar`) en pausas y salidas.
- `estadisticas`: precisión de `HU-29`, `HU-30`, `HU-31` y `HU-45` (`RN-30`, `RN-31`) con selector `Semana | Mes`, gráfico de anillo con `MPAndroidChart`, reglas de exclusión para sesiones sin plan/tardías y pendientes vigentes, desglose de tiempo `Sin clasificar` y resumen agrupado de tópicos, dejando además el modelo agregado base para E8.

## Impact

- **Código Java (`app/`):**
  - `domain`: `TopicoEventoEstudio` (claves canónicas en BD), `EventoEstudio` (con `topico` opcional), `SalidaRegistrada` (sesión y materia), `ResumenEjecucionSesion` (estado de salida pendiente), `CalculadorEstadisticas`, modelos de resultado (`EstadisticasPeriodo`, `ConcentracionPeriodo`, `DesgloseMateriaPeriodo`, `ResumenTopico`), ajustes en `CalculadorPomodoro` / `CalculadorTiempoReal`.
  - `application`: `ConsultarEstadisticas` / `ConsultarEstadisticasService`, `EventoEstudioRepository.listarPorRangoSesiones(...)` (consulta directa con `JOIN`), `MarcadorSalidaApp` (marca separada de fin de descanso Pomodoro), `RegistrarSalidaApp` (retorno `Optional<SalidaRegistrada>` con `MateriaRepository`), adaptación de `EvaluarRecuperacionSesionService`, `ResolverRegresoAppService`, `PausarReanudarSesionService` y `ConsultarEjecucionSesionService`.
  - `adapter.out.persistence` y `adapter.out.system`: `EventoEstudioEntity` (columna `topico`), `EventoEstudioDao`, `EventoEstudioRepositoryRoom`, `Mapeadores`, `StudyPlanDatabase` v7 (`Migraciones.MIGRATION_6_7` + `7.json` + `Migracion7InstrumentalTest`) y `MarcadorSalidaAppPrefs`.
  - `adapter.in.ui` y `res/`: `EstadisticasActivity`, `EstadisticasViewModel`, `EstadisticasAdapter` / layouts XML (`layout/` y `layout-w600dp/`), conexión en `DestinoPrincipal`, `FabricaViewModels`, `StudyPlanApplication`, corrección en `ActividadProtegida`, `ContadorActividades`, ampliación de `DialogoRecuperacionSesion` (`W13`), diálogo rápido de tópico al reanudar en `SesionEnCursoActivity` y textos `es`/`en`.
- **Dependencias:** incorporación aprobada de **`MPAndroidChart`** (`com.github.PhilJay:MPAndroidChart:v3.1.0`) en `gradle/libs.versions.toml`, `settings.gradle.kts` (JitPack acotado con `includeGroup("com.github.PhilJay")`) y `app/build.gradle.kts`.
- **RNF afectados:** `RNF-02`, `RNF-06`, `RNF-07`, `RNF-08`, `RNF-09`, `RNF-10`, `RNF-12`, `RNF-14`, `RNF-15`, `RNF-16`, `RNF-17` y `RNF-19`.
- **Documento académico:** actualización de `RN-23`, `RN-24`, `RN-30` (para confirmación final en Cap. 3 y `Auditoría IA`), diagramas de clases/BD OO/secuencias y capítulo 5.
