# Design

## Context

Véase [proposal.md](proposal.md) para la motivación y alcance de la Épica E5 (`tecnicas-estudio`).

Estado actual verificado en el repositorio:
- `StudyPlanDatabase` está en `version = 3` (sin tabla `evento_estudio` ni columnas de Pomodoro/No molestar).
- `EstadoPendiente` y `EstadoEnCurso` declaran transiciones de ejecución en vivo (`RN-07`), pero `SesionEstudio` no las expone y `EstadoEnCurso.completarConDuracion` rechaza `duracionRealMinutos <= 0`.
- `ContadorActividades` solo mantiene la bandera en memoria `salioDeLaApp` para `ControlarSesionBloqueo` (`HU-04`).
- `AGENTS.md` §2 y `RN-22`/`RNF-03` prohíben `ForegroundService` permanente contando segundos y exigen `domain` y `application` en Java puro (verificado por `ArquitecturaTest`), `minSdk 24` y `targetSdk 37`.

## Goals / Non-Goals

**Goals:**
- Reconstruir de forma determinista el tiempo real estudiado, pausas, tiempo extra y fases Pomodoro a partir de eventos persistidos (`EventoEstudio`) y el puerto `Reloj` (`long ahoraMilis()`).
- Soportar sesiones `EN_CURSO` multi-día (`RN-29`), cierres de `0` minutos y recuperación ante `SIGKILL` o cierre desde Recientes sin bloqueo del hilo principal.
- Programar avisos temporales críticos tolerantes a Doze sin requerir `SCHEDULE_EXACT_ALARM` y degradar con seguridad cuando se deniegan `POST_NOTIFICATIONS` o `ACCESS_NOTIFICATION_POLICY` (`RNF-18`).
- Migrar `StudyPlanDatabase` de versión `3` a `4` (`MIGRATION_3_4`) preservando datos previos sin `fallbackToDestructiveMigration` (`RNF-12`).

**Non-Goals:**
- Métricas de concentración e interrupciones (`HU-45`, E7), recordatorios previos al inicio (`HU-34`, E9) y respaldo en Drive (E10).
- Añadir dependencias nuevas al version catalog (`ProcessLifecycleOwner` u otras librerías externas).

## Decisions

### 1. Arquitectura hexagonal y paquete `adapter.out.system`
- **Decisión:** Mantener `domain` y `application` 100 % Java puro. Se introducen puertos de salida en `application.port.out` (`EventoEstudioRepository`, `ProgramadorAvisosSesion`, `ControladorNoMolestar`, `MarcadorSalidaApp`, ampliación de `Reloj` y `SesionRepository`) y adaptadores concretos en `adapter.out.persistence` (Room v4) y el nuevo paquete `adapter.out.system` (`ControladorNoMolestarSistema`, `ProgramadorAvisosAlarmManager`, `MarcadorSalidaAppPrefs`).
- **Alternativas consideradas:**
  - *Incluir `NotificationManager` / `AlarmManager` en servicios de aplicación:* rechazado porque violaría `ArquitecturaTest` (`android.*` en `application`).

### 2. Eventos como fuente de verdad en vivo y duración materializada al cerrar
- **Decisión:** Durante `EN_CURSO`, toda interacción relevante (`INICIO`, `PAUSA`, `REANUDACION`, `SALIDA_APP`, `REGRESO_APP`, `FIN_PLANIFICADO`, `TIEMPO_EXTRA`, `ESTUDIO_APP_CERRADA`, `CAMBIO_SESION`, `AJUSTE_MANUAL`, `FIN`) se graba síncronamente fuera del hilo principal en `evento_estudio` con `instante_milis` (epoch UTC) + `Fecha`/`Hora` antes de actualizar la UI (`RNF-10`). Al detener, cambiar o repartir, `CalculadorTiempoReal` calcula los minutos efectivos y actualiza `sesion_estudio.duracion_real_minutos` dentro de la misma `@Transaction` (`completarReparto` en reparto multi-sesión).
- **Alternativas consideradas:**
  - *Recalcular `duracionRealMinutos` escaneando `evento_estudio` en cada lectura de Inicio, Calendario o Materias:* rechazado porque degrada lecturas de rango y amenaza `RNF-02` (≤ 300 ms con 500 sesiones).
  - *Usar `ForegroundService` con contador periódico:* rechazado explícitamente por `RN-22`, `RNF-03` y `AGENTS.md` §2.

### 3. Derivación determinista de Pomodoro con *snapshot* en sesión
- **Decisión:** Al iniciar una sesión en modo `POMODORO`, `SesionEstudio` guarda un *snapshot* inmutable de `ConfiguracionPomodoro` (`trabajo`, `descansoCorto`, `descansoLargo`, `ciclosHastaLargo`), cumpliendo `RN-17`. `CalculadorPomodoro` deriva fase actual, ciclo y tiempo efectivo exclusivamente desde los intervalos activos dentro de la app y ese *snapshot*. Fuera de la app, el conteo de fases Pomodoro se suspende y `ESTUDIO_APP_CERRADA` suma íntegro (`RN-24`).
- **Alternativas consideradas:**
  - *Persistir filas en `evento_estudio` por cada cambio automático de fase en segundo plano:* rechazado porque sin `ForegroundService` Android no garantiza ejecución exacta en Doze para escribir ticks/fases y genera escrituras fantasma sin interacción real.

### 4. Ciclo de vida, recuperación post-autenticación y tolerancia a `SIGKILL`
- **Decisión:** Ampliar `ContadorActividades` para persistir `(sesionId, instanteSalidaMilis)` mediante `MarcadorSalidaAppPrefs` cuando `iniciadas == 0 && !isChangingConfigurations()`, registrando `SALIDA_APP` y emitiendo notificación de retorno (`HU-40`). Al volver, `ActividadProtegida.alEstarListo()` ejecuta `EvaluarRecuperacionSesion` **después** de superar biometría/PIN (`HU-04`) para mostrar `DialogoRecuperacionSesion` (`W13`). Si la app fue terminada desde Recientes en primer plano antes de `onStop`, `CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp` toma la última marca persistida conocida.
- **Alternativas consideradas:**
  - *Añadir `androidx.lifecycle:lifecycle-process` (`ProcessLifecycleOwner`):* rechazado para evitar dependencias nuevas cuando `ContadorActividades` ya existe y tampoco resuelve por sí solo `SIGKILL` en primer plano.

### 5. Alarmas críticas con `AlarmManager.setAlarmClock()` y degradación `RNF-18`
- **Decisión:** `ProgramadorAvisosAlarmManager` programa únicamente el próximo hito crítico activo (`CAMBIO_FASE_POMODORO`, `FIN_PLANIFICADO` o `INVASION_SESION`) usando `AlarmManager.setAlarmClock()` hacia `ReceptorAvisosSesion` (que también escucha `BOOT_COMPLETED`). Esto elude Doze sin exigir `SCHEDULE_EXACT_ALARM` (denegado por defecto en Android 14+/API 37). En UI, `SesionEnCursoActivity` actualiza el cronómetro cada `1000` ms con `Handler` solo entre `onStart()` y `onStop()`.
- **Alternativas consideradas:**
  - *`WorkManager` o alarmas inexactas para fases Pomodoro de 5 min:* rechazado porque Doze las difiere 9–15+ min, desfasando descansos y avisos horarios.
  - *Solicitar `SCHEDULE_EXACT_ALARM`:* rechazado por fricción innecesaria en Android 14+ y políticas de Play Store.

### 6. No molestar de tres estados y restauración segura
- **Decisión:** `PreferenciaNoMolestar` (`HEREDAR_GLOBAL`, `FORZAR_ACTIVAR`, `FORZAR_DESACTIVAR`) se mapea a `Boolean` nullable en `SesionEntity` (`null`, `true`, `false`). `ControladorNoMolestarSistema` persiste `interruption_filter_previo` en `SharedPreferences` antes de activar `INTERRUPTION_FILTER_PRIORITY` y comprueba `isNotificationPolicyAccessGranted()` tanto al aplicar como al restaurar en pausa, detención, cambio o vencimiento (`RN-29`).

### 7. Reparto continuo sobre intervalos efectivos y cierres de `0` minutos
- **Decisión:** `EstadoEnCurso.completarConDuracion` acepta `duracionRealMinutos >= 0` (manteniendo `> 0` en registro tardío/manual). `CalculadorRepartoTramo` segmenta los `IntervaloEfectivo` ya filtrados respecto a `corteMilis` sin convertir pausas ni descansos Pomodoro en tiempo real; `RepartoSesionActivity` (`W14`) ofrece `Slider` en pasos de `5` min sincronizado con botones accesibles `− 5 min` / `+ 5 min` (`minHeight="48dp"`).

### 8. Eliminación del completado directo con minutos editables en `PENDIENTE` (`RN-07`, `HU-12`)
- **Decisión:** En `SesionDetalleActivity`, `grupoDuracionRealDetalle` (`campoDuracionRealDetalle`) se muestra **únicamente** cuando `detalle.puedeRegistrarTardio()` (`NO_REALIZADA` ≤ 3 días, caso especial `HU-13`). Para sesiones `PENDIENTE` de hoy se retira `botonCompletarSesion` («Marcar completada») y `EstadoPendiente.puedeCompletar(...)` retorna `false`, obligando a completar las sesiones del flujo normal exclusivamente mediante acciones de la máquina de estados (`Iniciar → En curso → Detener` o recuperación `W13`). El registro tardío (`HU-11`, `HU-13`) permanece intacto al tratarse de un estado y flujo especial separado.

## Risks / Trade-offs

- **[Cierre abrupto `SIGKILL` desde Recientes en primer plano]** → `onActivityStopped` puede no ejecutarse; mitigado tomando `obtenerUltimoInstanteDentroDeApp` (último evento/pausa persistido) al reabrir sin `SALIDA_APP` abierta.
- **[Icono de alarma en barra de estado con `setAlarmClock()` y denegación de `POST_NOTIFICATIONS`]** → `setAlarmClock()` muestra el icono del sistema mientras hay sesión `EN_CURSO`; mitigado programando solo durante `EN_CURSO`, cancelando al pausar/detener y reconciliando hitos vencidos al abrir la app si se denegaron notificaciones (`RNF-18`).
- **[Revocación de `ACCESS_NOTIFICATION_POLICY` con sesión activa]** → Evitado `SecurityException` comprobando `isNotificationPolicyAccessGranted()` antes de restaurar el filtro guardado.
- **[Cambio de reloj o cruce de medianoche multi-día]** → Almacenar `instante_milis` en UTC junto a `Fecha`/`Hora` locales y acotar diferencias de tramo a `>= 0`.
- **[Invariante de sesión única `EN_CURSO`]** → Si existe una sesión activa previa al intentar iniciar otra, `IniciarSesion` retorna `SESION_YA_EN_CURSO` y la UI abre `W13` para resolverla primero.

## Migration Plan

1. **Room `3 → 4` (`MIGRATION_3_4` en `Migraciones.java`):**
   - `ajustes`: añadir `no_molestar_global INTEGER NOT NULL DEFAULT 0`, `pomodoro_trabajo_min INTEGER NOT NULL DEFAULT 25`, `pomodoro_descanso_corto_min INTEGER NOT NULL DEFAULT 5`, `pomodoro_descanso_largo_min INTEGER NOT NULL DEFAULT 15`, `pomodoro_ciclos_hasta_largo INTEGER NOT NULL DEFAULT 4`.
   - `sesion_estudio`: añadir `tecnica TEXT NOT NULL DEFAULT 'LIBRE'`, `forzar_no_molestar INTEGER`, `pomodoro_trabajo_min INTEGER`, `pomodoro_descanso_corto_min INTEGER`, `pomodoro_descanso_largo_min INTEGER`, `pomodoro_ciclos_hasta_largo INTEGER`.
   - Crear tabla `evento_estudio` con FK `ON DELETE CASCADE` a `sesion_estudio(id)` e índices en `sesion_id` e `instante_milis`.
2. **Verificación:** exportar esquema `4.json` y validar migración con `MigrationTestHelper` en `Migracion4InstrumentalTest` conservando perfil, ajustes, materias y sesiones v3 intactas. Sin rollback destructivo (`fallbackToDestructiveMigration` prohibido).
