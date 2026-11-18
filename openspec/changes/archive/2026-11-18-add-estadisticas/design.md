# Design

## Context

Motivación completa en [proposal.md](proposal.md). Este *change* implementa la épica E7 (`HU-29`, `HU-30`, `HU-31` y `HU-45`) y corrige los fallos de registro detectados en E5 (`HU-22`, `HU-39`, `HU-40`, `HU-41`, `HU-42`) para garantizar que las métricas reflejen el tiempo real. Parte del estado de `add-metas-semanales` (Room v6):

- **Navegación y datos existentes:** `DestinoPrincipal` reserva la pestaña `ESTADISTICAS(R.id.nav_estadisticas, null)`. `TramoDiarioRepository` suma minutos reales por día local aplicando corte estricto en medianoche (`RN-31`), y `SesionRepository` consulta sesiones por rango de fechas; «Estudios varios» (`es_general = 1`) representa la materia general protegida.
- **Vacíos en ejecución y recuperación:**
  - `EventoEstudioEntity` guarda `(id, sesion_id, tipo, instante_milis, desfase_minutos, nota)` sin campo de tópico estructurado, y `EventoEstudioRepository` solo consulta por `sesionId`.
  - `ActividadProtegida` omite la evaluación de recuperación en `SesionEnCursoActivity`: al regresar desde otra app a `SesionEnCursoActivity` no se presenta `W13` (el diálogo modal `DialogoRecuperacionSesion` que pregunta «Saliste a las HH:mm, ¿seguiste estudiando?» y ofrece las opciones de resolución al volver).
  - `ConsultarEjecucionSesionService` no detiene el incremento visual del reloj en `SesionEnCursoActivity` mientras exista una `SALIDA_APP` sin resolver ni valida `sesion.tienePlan()` antes de mostrar la tarjeta de fin planificado.
  - `ResolverRegresoApp` carece de una opción para retomar la sesión tras una interrupción sin sumar el tiempo fuera, y `RegistrarSalidaAppService` trata salidas ocurridas durante descansos Pomodoro igual que salidas en tiempo de trabajo (además de pasar `""` en lugar del nombre real de materia a `NotificadorSesion`).

## Goals / Non-Goals

**Goals:**
- Registrar y diferenciar con precisión tiempo activo en la app, estudio externo clasificado y sin clasificar (`ESTUDIO_APP_CERRADA`), pausas manuales con tópico, descansos Pomodoro y tiempo fuera no académico.
- Implementar la pantalla de Estadísticas (`HU-29`, `HU-30`, `HU-31`, `HU-45`) con selector `Semana | Mes`, gráfico de anillo `MPAndroidChart` y subvista `Concentración` respetando la arquitectura hexagonal en Java puro.
- Entregar un agregador puro de dominio (`CalculadorEstadisticas` y `EstadisticasPeriodo` con `DesgloseMateriaPeriodo`) reutilizable directamente por **E8 (Reportes)**.

**Non-Goals:**
- Exportación PDF o CSV (`E8`), recordatorios (`E9`), widget (`HU-05`) ni respaldo en Google Drive (`E10`, fuera del MVP).
- Bloqueo físico del dispositivo (`DevicePolicyManager.lockNow()`) o permisos de administrador del dispositivo.
- Alterar el avance del temporizador Pomodoro dentro de la app cuando ocurre `ESTUDIO_APP_CERRADA` (`RN-24`: los tramos declarados con la app cerrada suman íntegros al tiempo real total sin avanzar los ciclos internos dentro de la app).

## Decisions

### 1. Tópico estructurado en `evento_estudio` con claves canónicas (`Room v7`, `MIGRATION_6_7`)
- **Decisión:** agregar la columna nullable `topico TEXT` en `evento_estudio` (Room v7) y definir `TopicoEventoEstudio` en `domain` persistiendo **exclusivamente claves canónicas estables** en base de datos, traduciendo la etiqueta solo en la capa UI (`strings.xml` en `es` y `en` según `RNF-08`):
  - Estudio fuera de la app: `PDF_MOODLE`, `LIBRO_APUNTES`, `GRUPO_CLASE`, `OTRO`, `SIN_CLASIFICAR`.
  - Pausas e interrupciones: `DESCANSO_COMIDA`, `LLAMADA_MENSAJE`, `OTRA_APP`, `OTRO`, `SIN_CLASIFICAR`.
  - Cuando el estudiante elige `OTRO`, `topico` almacena `"OTRO"` y `nota` almacena el detalle libre opcional. En eventos previos a v7 (`topico` es `NULL` o vacío), `Mapeadores` lo expone como `Optional.empty()` en `EventoEstudio` (ausencia histórica, distinta de `SIN_CLASIFICAR` elegido o asignado explícitamente en v7).
- **Alternativa considerada:** codificar el tópico dentro de `nota` con prefijos de texto (`[TOPICO] ...`), descartada porque mezcla categorías estadísticas con texto libre, complica consultas y rompe la limpieza entre idiomas.

### 2. Captura de tópicos en pausas y regreso (`W13`), contratos de retorno y «Sin clasificar»
- **Decisión:**
  - **En pausas manuales:** `PausarReanudarSesion` conserva `ejecutar(sesionId)` por retrocompatibilidad y suma `ejecutar(sesionId, String topico, String nota)`. Al pulsar **Reanudar** en `SesionEnCursoActivity`, se presenta un diálogo rápido en un toque con chips de tópico (`SIN_CLASIFICAR` por defecto) y el código canónico elegido se guarda en el evento `REANUDACION` (modelo *append-only* sin mutar eventos pasados).
  - **En salidas (`W13`):** `DialogoRecuperacionSesion` (`W13`) muestra 4 opciones: `NO_SEGUI` (*«No, terminé al salir»*), `HASTA_HORA` (*«Sí, hasta una hora específica»*), `SIGO_ESTUDIANDO` (*«Sigo estudiando»*) y la nueva **`RETOMAR_SIN_SUMAR`** (*«No estudié ese rato, retomo ahora»*). `RETOMAR_SIN_SUMAR` graba `REGRESO_APP` con el tópico seleccionado sin crear `ESTUDIO_APP_CERRADA` (tipo de `EventoEstudio` que delimita el tramo estudiado fuera de la app desde `SALIDA_APP`), reanuda `EN_CURSO` y no suma minutos al tiempo real. Si el usuario confirma `SIGO_ESTUDIANDO` sin marcar otro chip, el valor asignado por omisión es `SIN_CLASIFICAR`, sumando estudio real y quedando visible aparte en estadísticas.
  - **Recuperación en `SesionEnCursoActivity` y contrato de `RegistrarSalidaApp`:** `ActividadProtegida` habilita la evaluación de recuperación en `SesionEnCursoActivity` (manteniendo exclusión solo en `RepartoSesionActivity` y `AutenticacionActivity`). `ResumenEjecucionSesion` añade `salidaPendienteResolver`; mientras sea `true`, `SesionEnCursoActivity` detiene el incremento visual por segundo hasta resolver `W13` (diálogo mostrado una única vez por salida abierta). Además, `RegistrarSalidaApp` amplía su retorno de `Optional<Long>` a `Optional<SalidaRegistrada>` (`sesionId` y `nombreMateria`), consultando `MateriaRepository` en `RegistrarSalidaAppService` para pasarlo a `ContadorActividades` → `NotificadorSesion.mostrarAvisoRetorno(...)`.
- **Alternativa considerada:** pedir el tópico al pulsar **Pausar** en lugar de **Reanudar**; descartada porque frena al estudiante justo al interrumpir y aún no sabe cuánto pausó ni si cambió de actividad.

### 3. Salidas durante descansos Pomodoro, `MarcadorSalidaApp` y sesiones sin plan
- **Decisión:**
  - `MarcadorSalidaApp` (implementado por `MarcadorSalidaAppPrefs` sobre `SharedPreferences` para sobrevivir a `SIGKILL`) amplía su contrato separando explícitamente las marcas de salida real y las de descanso Pomodoro mediante `marcarFinDescansoPomodoro(long sesionId, long finDescansoMilis)`, `obtenerFinDescansoPomodoro()` y `limpiarFinDescansoPomodoro()`, evitando confundir un descanso activo con una salida sin resolver.
  - En `RegistrarSalidaAppService`, usando `CalculadorPomodoro`, si la sesión está en `DESCANSO_CORTO` o `DESCANSO_LARGO`, **no** se graba `SALIDA_APP`, se guarda el fin proyectado `finDescansoMs = ahoraMs + estadoPomodoro.milisRestantesEnFase()` mediante `marcarFinDescansoPomodoro(...)` y el servicio devuelve `Optional.empty()` (sin emitir notificación de retorno).
  - Al volver en `EvaluarRecuperacionSesionService`: si existe fin de descanso pendiente y `ahoraMs <= finDescansoMs`, se limpia la marca sin abrir `W13` (el descanso transcurrió normalmente). Si `ahoraMs > finDescansoMs`, se persiste `SALIDA_APP` anclado en `finDescansoMs`, se limpia la marca de descanso y se abre `W13` exclusivamente por `[finDescansoMs .. ahoraMs]`.
  - En `ConsultarEjecucionSesionService`, `mostrarFinPlanificado` evalúa primero `sesion.tienePlan()` para que sesiones sin horario de fin (`Estudiar ahora`) no muestren «¿Sigues estudiando?».
- **Alternativas consideradas:** (a) grabar `SALIDA_APP` también durante el descanso y filtrarlo al volver — descartada porque `CalculadorTiempoReal` congelaría el reloj de descanso mientras `SALIDA_APP` estuviera abierta; (b) programar un aviso diferido nuevo en `ProgramadorAvisosSesion` para `finDescansoMs` — descartada para evitar sobre-ingeniería, bastando evaluar `ahoraMs > finDescansoMs` al volver.

### 4. Agregador compartido para E7 y E8 (`CalculadorEstadisticas` y `ConsultarEstadisticas`)
- **Decisión:** crear `PeriodoEstadisticas` (delegando exclusivamente en `PeriodoCalendario.rangoVisibleInicio()` y `PeriodoCalendario.rangoVisibleFin()` para `SEMANA` lunes–domingo y `MES`, sin usar `rangoConsultaInicio()`/`Fin()`), `EstadisticasPeriodo`, `ConcentracionPeriodo`, `DesgloseMateriaPeriodo`, `ResumenTopico` y `CalculadorEstadisticas` en Java puro (`domain`), orquestados por `ConsultarEstadisticasService`.
- **Consulta de eventos:** ampliar `EventoEstudioRepository` con `listarPorRangoSesionesUtc(long desdeUtcMilis, long hastaUtcMilis)` implementado en `EventoEstudioDao` mediante una **única consulta directa con `JOIN sesion_estudio`** (aprovechando los índices existentes `sesion_id` y `plan_inicio_utc_milis` y evitando el límite de 999 variables SQLite sin paginar lotes manuales).
- **Reglas y fórmulas exactas (`RN-30`, `RN-31`, `HU-29` a `HU-31`, `HU-45`):**
  - **Horas totales y variación (`HU-29`):** suma `TramoDiarioRepository.sumarMinutosEnRango` (corte diario estricto en medianoche `RN-31`) para periodo actual y periodo anterior; omite variación cuando el periodo previo suma `0`.
  - **Distribución por materia (`HU-30`):** toma `TramoDiarioRepository.sumarPorMateriaEnRango` (incluida «Estudios varios» y materias archivadas con minutos `> 0`) y calcula porcentaje y color ARGB.
  - **Cumplimiento (`HU-31`):** ejecuta `VencerSesionesPendientes` (`RN-08`) antes de consultar; sobre sesiones cerradas del rango (`COMPLETADA`, `COMPLETADA_TARDIA`, `NO_REALIZADA`, `REPROGRAMADA`), calcula `(totalCerradas == 0) ? null : (completadas + tardias) * 100.0 / totalCerradas` e informa aparte las aún `PENDIENTE` o `EN_CURSO`.
  - **Base compartida para E8 (`HU-32`):** cada `DesgloseMateriaPeriodo` expone también `minutosPlanificadosDeSesiones` (suma de `duracionPlanificadaMinutos` de sesiones del rango) y `sesionesNoRealizadas`, junto con `cumplimientoPlanificado` (`minutosPlanificados == 0 ? null : minutosReales * 100.0 / minutosPlanificados`).
  - **Concentración, interrupciones y tópicos (`HU-45`, `RN-30`):**
    - `HU-29` y `HU-30` usan corte diario estricto `RN-31` (`TramoDiarioRepository`), mientras `HU-31` y `HU-45` operan por sesión dentro del periodo visible (sin segmentar eventos `PAUSA`/`SALIDA_APP` a medianoche).
    - `dentroAppMs`: suma intervalos activos dentro de la app mediante `CalculadorTiempoReal.calcularMilisDentroDeApp` en sesiones con evento `INICIO` (excluye siempre `ESTUDIO_APP_CERRADA` y sesiones `COMPLETADA_TARDIA`; en Pomodoro sigue `RN-30` incluyendo la permanencia dentro de la app salvo `PAUSA` manual).
    - `pausasManualesMs`: suma intervalos `[PAUSA .. REANUDACION]`; si una `PAUSA` termina directamente por `FIN`, `CAMBIO_SESION` o cierre por vencimiento de 3 días sin `REANUDACION`, suma `[PAUSA .. cierre]`.
    - `concentración (%)`: `(dentroAppMs + pausasManualesMs == 0) ? null : dentroAppMs * 100.0 / (dentroAppMs + pausasManualesMs)`. `ESTUDIO_APP_CERRADA` no forma parte de `dentroAppMs` ni del denominador y se muestra aparte.
    - `interrupcionesPorHora`: `(dentroAppMs == 0) ? 0.0 : totalInterrupciones / (dentroAppMs / 3_600_000.0)`, contando cada `PAUSA` manual y cada `SALIDA_APP` resuelta como no académica (`REGRESO_APP` sin `ESTUDIO_APP_CERRADA` o `FIN` desde `NO_SEGUI`); las salidas resueltas con `ESTUDIO_APP_CERRADA` no se cuentan como interrupción.
    - `puntualidadPromedioMin`: solo sobre sesiones con `tienePlan() == true` (`duracionPlanificadaMinutos > 0`) que registraron evento `INICIO` (excluyendo `Estudiar ahora` y `COMPLETADA_TARDIA`): promedio de `(primerInicio.instanteMilis() - sesion.planInicioUtcMilis()) / 60_000.0` (o `null` si no hay sesiones elegibles).
    - `respetoDelTiempo (%)`: solo sobre sesiones cerradas en `COMPLETADA` con `tienePlan() == true` (`duracionPlanificadaMinutos > 0`) e evento `INICIO` (excluyendo `Estudiar ahora`, `EN_CURSO` y `COMPLETADA_TARDIA`): `(sum(duracionPlanificada) == 0) ? null : sum(duracionReal) * 100.0 / sum(duracionPlanificada)`.
    - `tiempoFueraAppMinutos`: suma intervalos `[SALIDA_APP .. REGRESO_APP | FIN]` sin `ESTUDIO_APP_CERRADA` (informativo, no descuenta horas).
    - `estudioFueraAppMinutos`: suma tramos `[SALIDA_APP .. ESTUDIO_APP_CERRADA]`, separando el subtotal con clave `SIN_CLASIFICAR` del resto de tópicos académicos.
    - `resumenTopicos` (pausas e interrupciones): agrupa exclusivamente `PAUSA` (usando la clave de `REANUDACION` o respaldo `SIN_CLASIFICAR` si cerró sin `REANUDACION` o si procede de v6 sin tópico) y `SALIDA_APP` no académica (`RETOMAR_SIN_SUMAR` o `NO_SEGUI`), devolviendo conteo y minutos acumulados; `ESTUDIO_APP_CERRADA` queda fuera de este bloque y se presenta en el apartado de estudio externo.
- **Alternativas consideradas:** (a) consultas agregadas SQL `SUM`/`GROUP BY` para calcular métricas en BD — descartada porque duplica la reconstrucción cronológica de `CalculadorTiempoReal` fuera del dominio Java puro; (b) `listarPorSesiones(List<Long>)` paginado en lotes de 500 IDs — descartada frente a la consulta única con `JOIN`.

### 5. Gráficos (`MPAndroidChart`), accesibilidad e interfaz (`EstadisticasActivity`)
- **Decisión:** incorporar `com.github.PhilJay:MPAndroidChart:v3.1.0` declarando `https://jitpack.io` en `settings.gradle.kts` acotado con `content { includeGroup("com.github.PhilJay") }`, su entrada en `gradle/libs.versions.toml` y verificando la optimización R8 en `release` bajo AGP `9.4.1` / `compileSdk 37`. `PieChart` se configura en modo anillo con `android:importantForAccessibility="no"`, delegando la lectura TalkBack a la lista textual accesible por materia (`RNF-06`).
- **UI:** `EstadisticasActivity` extiende `ActividadProtegida`, enlaza `DestinoPrincipal.ESTADISTICAS`, incluye selector `Semana | Mes`, botones `<` / `>`, `MaterialButtonToggleGroup` (`Resumen` | `Concentración`) y layouts dedicados en `layout/` y `layout-w600dp/` cumpliendo `AGENTS.md` §6.2.
- **Alternativa considerada:** implementar el gráfico de anillo mediante `View` personalizada con `Canvas` — descartada porque el stack aprobado del proyecto en `AGENTS.md` §2 y `openspec/config.yaml` fija `MPAndroidChart` y el Product Owner aprobó su inclusión.

## Risks / Trade-offs

- **Resolución y R8 con `MPAndroidChart:v3.1.0` vía JitPack.** Puede fallar en compilación `release` o descarga → validar `./gradlew assembleDebug assembleRelease` desde la tarea `0.1` y aislar el grupo en `settings.gradle.kts`.
- **Compatibilidad de interfaces y tests existentes.** Ampliar `PausarReanudarSesion`, `ResolverRegresoApp`, `RegistrarSalidaApp` y `MarcadorSalidaApp` impacta llamadas actuales → mantener sobrecargas `default` en puertos de entrada donde aplique y actualizar el *fake* de `MarcadorSalidaApp` y `RegistrarSalidaApp` en la misma tarea.
- **Regresión en `TecnicasEstudioEspressoTest` tras activar `W13` en `SesionEnCursoActivity`.** → Cubrir el regreso directo, el descanso Pomodoro y el tópico al reanudar en pruebas E2E específicas.

## Migration Plan

`MIGRATION_6_7`, en una transacción:
1. Ejecutar ``ALTER TABLE `evento_estudio` ADD COLUMN `topico` TEXT`` (admite `NULL` para eventos previos a v7; al mapear en `Mapeadores`, `NULL`/vacío se interpreta como `Optional.empty()`, distinto de `SIN_CLASIFICAR` explícito en v7).
2. Registrar `Migraciones.MIGRATION_6_7` en `StudyPlanDatabase.abrir(...)` (`addMigrations(..., Migraciones.MIGRATION_6_7)`) y subir la versión a `7` exportando `app/schemas/.../7.json`.
3. Verificar migración `v6 → v7` con `MigrationTestHelper` en `Migracion7InstrumentalTest`.
