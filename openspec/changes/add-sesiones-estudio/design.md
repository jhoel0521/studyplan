# Design

## Context

Los dos changes previos (`add-configuracion-inicial` y `add-gestion-materias`, ya archivados en `openspec/specs/`) consolidaron:
- Arquitectura hexagonal en 3 capas en Java puro para `domain` y `application` (comprobada automáticamente por `ArquitecturaTest`), MVVM en `adapter.in.ui` y persistencia en Room (`adapter.out.persistence`).
- Convención de validación **sin excepciones** (`Resultado<T>` + códigos en `ErrorValidacion` traducidos en `strings.xml`, `RNF-08` y `RNF-14`), objetos inmutables en `domain` y raíz de composición manual en `StudyPlanApplication` con `ExecutorService` de un solo hilo.
- Base de datos Room v2 (`perfil`, `ajustes`, `materia`) con esquema exportado y pruebas de migración con `MigrationTestHelper`.
- Regla estricta de atributos XML en `AGENTS.md` §6.2 para evitar advertencias en el editor de diseño, Visual Lint y Accessibility Scanner (`tools:context`, `tools:fitsSystemWindows="true"`, `tools:tint`, `tools:text`, `android:id` en `<include>`, `minHeight="48dp"`, `maxWidth="400dp"`/`600dp`, `contentDescription`/`importantForAccessibility` y variantes `layout-w600dp/`).

Dejaron además tres pendientes explícitos que **add-sesiones-estudio** debe completar:
1. Reemplazar `SinSesionesDeMateria` por una consulta real para cumplir `RN-03` (`HU-08`).
2. Calcular horas acumuladas reales por materia (`HU-09`, `RN-11`).
3. Completar la pantalla de Inicio (`HU-02`: progreso semanal real, próxima sesión pendiente, lista de sesiones de hoy con estado y botón rápido «+ Nueva sesión» con fecha de hoy preseleccionada).

## Goals / Non-Goals

**Goals:**
- Implementar la máquina de estados completa `RN-07` siguiendo estrictamente el patrón **State (GoF)** y los principios **SOLID** (SRP por caso de uso, OCP/LSP en estados polimórficos, ISP en puertos de salida, DIP con reloj abstraído y cero `android.*` en el núcleo).
- Garantizar determinismo temporal absoluto en pruebas unitarias de fechas vencidas, ventana de 3 días (`RN-06`, `RN-10`) y semana lunes–domingo (`RN-13`) sin usar `java.time` (manteniendo compatibilidad nativa en `minSdk 24` sin añadir *desugaring*).
- Asegurar integridad referencial en SQLite (`FOREIGN KEY` hacia `materia` con `ON DELETE RESTRICT` como segunda línea de defensa de `RN-03` y autorreferencia en `sesion_origen_id` para `RN-09`) con migración `MIGRATION_2_3` probada contra esquema `3.json`.
- Diseñar todas las pantallas y componentes visuales con accesibilidad completa (`RNF-06`: nunca solo color, objetivos ≥ 48 dp, etiquetas descriptivas) y cero avisos en Layout Validation / Lint.

**Non-Goals:**
- Cronómetro en vivo / Pomodoro / eventos de interrupción / notificación persistente de sesión `EN_CURSO` (Épica E5 `tecnicas-estudio`).
- Vistas de calendario mensual, semanal tipo horario y diaria (Épica E4 `calendario-estudio`).
- Metas semanales específicas por semana o por materia (Épica E6 `metas-semanales`).
- Recordatorios programados con `WorkManager` / `AlarmManager` (Épica E9).

## Decisions

### D1. Modelo de dominio en Java puro, objetos de valor y Patrón State (`domain`)

- **Objetos de valor temporales compatibles con API 24 (`Fecha` y `Hora`):**
  - `Fecha`: valor inmutable basado en año, mes y día (`YYYY-MM-DD`). Valida años bisiestos y longitud de meses del calendario gregoriano mediante aritmética entera pura (algoritmo proleptic Gregorian / Rata Die / día juliano civil), sin usar `java.time` ni `Calendar` mutable. Ofrece: `de(int anio, int mes, int dia)`, `parsear(String iso)`, `iso()`, `esAnteriorA(Fecha)`, `esPosteriorA(Fecha)`, `diasHasta(Fecha otra)` (diferencia exacta en días), `sumarDias(int dias)`, `lunesDeSemana()` y `domingoDeSemana()` (`RN-13`: lunes=1 a domingo=7).
  - `Hora`: valor inmutable basado en minutos desde medianoche (`0..1439`, formato `HH:mm`). Valida `0 <= hora <= 23` y `0 <= minuto <= 59`. Ofrece: `de(int hora, int minuto)`, `parsear(String hhmm)`, `texto()`, `minutosDesdeMedianoche()`, `esPosteriorA(Hora otra)` y `minutosHasta(Hora fin)` (para calcular duración planificada según `RN-04`).
- **Patrón State GoF (`RN-07`):**
  - Enum `CodigoEstadoSesion`: `PENDIENTE`, `EN_CURSO`, `NO_REALIZADA`, `COMPLETADA`, `COMPLETADA_TARDIA`, `REPROGRAMADA`.
  - Interfaz `EstadoSesion` (con implementaciones en `domain`: `EstadoPendiente`, `EstadoEnCurso`, `EstadoNoRealizada`, `EstadoCompletada`, `EstadoCompletadaTardia`, `EstadoReprogramada`) y fábrica `EstadoSesion.de(CodigoEstadoSesion)` para hidratar desde persistencia sin `switch` en la UI.
  - Siguiendo `RNF-14` y el patrón existente de `Resultado.java`, las transiciones **no lanzan excepciones**: devuelven `Resultado<EstadoSesion>` (o el método correspondiente en `SesionEstudio` devuelve `Resultado<SesionEstudio>`). Cada estado implementa únicamente las transiciones válidas y devuelve `Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA)` en las prohibidas:
    - `EstadoPendiente`:
      - `iniciar()` → `EN_CURSO`
      - `marcarCompletada(Fecha sesionFecha, Fecha hoy, int duracionReal)` → `COMPLETADA` solo si `sesionFecha.equals(hoy)` (`RN-07`, `HU-12`) y `duracionReal > 0` (`RN-26`); si `sesionFecha` no es `hoy`, devuelve `COMPLETAR_SOLO_MISMO_DIA`; si `duracionReal <= 0`, `DURACION_REAL_INVALIDA`.
      - `vencer()` → `NO_REALIZADA` (`RN-08`)
      - `reprogramar()` → `REPROGRAMADA` (`RN-09`)
      - Rechaza `pausarReanudar`, `detener`, `registrarTardio`, `cambiarASiguiente`, `declararFinAlVolver`, `cerrarPorPlazoVencido`.
    - `EstadoEnCurso`:
      - Admite `pausarReanudar()` → `EN_CURSO`, `detener(duracionReal)` → `COMPLETADA`, `cambiarASiguiente(duracionReal)` → `COMPLETADA` (`RN-27`), `declararFinAlVolver(duracionReal)` → `COMPLETADA` (`RN-24`), `cerrarPorPlazoVencido(duracionReal)` → `COMPLETADA` (`RN-29`), cubriendo los 13 escenarios de `RN-07` desde el núcleo.
      - Rechaza `vencer` (`RN-29`: una sesión `EN_CURSO` no pasa a `NO_REALIZADA`), `reprogramar`, `registrarTardio`, `marcarCompletada`.
    - `EstadoNoRealizada`:
      - `registrarTardio(int diasAntiguedad, int duracionReal)` → `COMPLETADA_TARDIA` solo si `0 <= diasAntiguedad <= 3` (`RN-10`) y `duracionReal > 0` (`RN-26`); si `diasAntiguedad > 3` devuelve `REGISTRO_TARDIO_FUERA_DE_VENTANA`.
      - `reprogramar()` → `REPROGRAMADA` (`RN-09`).
      - Rechaza todo lo demás.
    - `EstadoCompletada`, `EstadoCompletadaTardia`, `EstadoReprogramada`:
      - Estados finales: `esFinal() == true`. Rechazan toda transición con `TRANSICION_ESTADO_NO_PERMITIDA` y devuelven `false` en `puedeCompletar`, `puedeRegistrarTardio`, `puedeReprogramar`, `puedeEliminar`.
  - Métodos de consulta polimórficos en `EstadoSesion` (para eliminar `switch` en la UI, principio OCP):
    - `codigo()`, `esFinal()`, `sumaHorasReales()` (`true` únicamente en `EstadoCompletada` y `EstadoCompletadaTardia`, `RN-11`), `ocupaHorario()` (`false` únicamente en `EstadoReprogramada`, `RN-05`), `puedeCompletar(Fecha sesionFecha, Fecha hoy)`, `puedeRegistrarTardio(int diasAntiguedad)`, `puedeReprogramar()`, `puedeEliminar()` (`true` únicamente en `EstadoPendiente`, `RN-12`).
- **Entidad `SesionEstudio` y fábricas de dominio:**
  - Campos inmutables: `id` (`Long`), `materiaId` (`long`), `fecha` (`Fecha`), `horaInicio` (`Hora`), `horaFin` (`Hora`), `duracionPlanificadaMinutos` (`int`), `duracionRealMinutos` (`Integer`), `estado` (`EstadoSesion`), `notas` (`String` opcional saneada, máx. 500 caracteres), `sesionOrigenId` (`Long` opcional para vínculo `RN-09`).
  - Fábricas estáticas devolviendo `Resultado<SesionEstudio>`:
    - `planificar(long materiaId, Fecha fecha, Fecha hoy, Hora inicio, Hora fin, String notas, Long sesionOrigenId)`: exige `fecha >= hoy` (`FECHA_PLANIFICACION_PASADA`), `fin > inicio` (`HORARIO_FIN_NO_POSTERIOR`, `RN-04`), notas ≤ 500 (`NOTAS_SESION_DEMASIADO_LARGAS`), crea en `EstadoPendiente` con `duracionPlanificadaMinutos = inicio.minutosHasta(fin)`.
    - `registrarPasada(long materiaId, Fecha fecha, Fecha hoy, Hora inicio, Hora fin, int duracionRealMinutos, String notas)`: exige `1 <= fecha.diasHasta(hoy) <= 3` (`FECHA_PASADA_FUERA_DE_VENTANA`, `RN-06`), `fin > inicio` (`RN-04`), `duracionRealMinutos > 0` (`DURACION_REAL_INVALIDA`, `RN-26`), crea en `EstadoCompletadaTardia`.
    - `restaurar(...)`: reconstruye una sesión leída desde Room.
  - Detección de solapamiento (`RN-05`): `seSolapaCon(Fecha fecha, Hora inicio, Hora fin)` retorna `true` si `estado.ocupaHorario()`, `this.fecha.equals(fecha)`, `inicio.minutosDesdeMedianoche() < this.horaFin.minutosDesdeMedianoche()` y `fin.minutosDesdeMedianoche() > this.horaInicio.minutosDesdeMedianoche()` (sesiones adyacentes como `10:00–11:00` y `11:00–12:00` no se cruzan).
- **Ampliación de `ErrorValidacion` y objetos de resultado con conflicto:**
  - Nuevos códigos en `ErrorValidacion`: `MATERIA_SESION_OBLIGATORIA`, `MATERIA_INACTIVA`, `FECHA_SESION_OBLIGATORIA`, `FECHA_SESION_FORMATO_INVALIDO`, `FECHA_PLANIFICACION_PASADA`, `FECHA_PASADA_FUERA_DE_VENTANA`, `HORA_INICIO_OBLIGATORIA`, `HORA_FIN_OBLIGATORIA`, `HORA_FORMATO_INVALIDO`, `HORARIO_FIN_NO_POSTERIOR`, `DURACION_REAL_INVALIDA`, `NOTAS_SESION_DEMASIADO_LARGAS`, `SESION_SOLAPADA`, `SESION_NO_ENCONTRADA`, `TRANSICION_ESTADO_NO_PERMITIDA`, `COMPLETAR_SOLO_MISMO_DIA`, `REGISTRO_TARDIO_FUERA_DE_VENTANA`, `SESION_NO_ELIMINABLE`.
  - Value Object `ConflictoHorario` (`sesionId`, `materiaId`, `nombreMateria`, `fecha`, `horaInicio`, `horaFin`) y `ResultadoSesion` (`sesion`, `error`, `conflicto`), que permiten a los servicios devolver exactamente con qué sesión chocó un intento de registro o reprogramación (`HU-10` escenario 3 y `HU-14` escenario 3) manteniendo el contrato libre de excepciones (`RNF-14`).

### D2. Puertos de entrada, salida y servicios de aplicación (`application`)

- **Puertos de salida (`application.port.out`):**
  - `Reloj`: `Fecha hoy()`, `Hora ahora()`. Evita acoplar lógica de fechas al reloj del sistema; en pruebas JUnit se inyecta un reloj fijo.
  - `SesionRepository`:
    - `SesionEstudio guardar(SesionEstudio sesion)`
    - `void actualizar(SesionEstudio sesion)`
    - `SesionEstudio reprogramar(SesionEstudio originalReprogramada, SesionEstudio nuevaPendiente)` (operación atómica en transacción Room)
    - `Optional<SesionEstudio> buscarPorId(long id)`
    - `Optional<SesionEstudio> buscarReprogramadaHijaDe(long sesionOrigenId)`
    - `List<SesionEstudio> listarPorFecha(Fecha fecha)` (ordenadas por `horaInicio ASC`)
    - `List<SesionEstudio> listarPendientesDesde(Fecha desde)` (ordenadas por `fecha ASC, horaInicio ASC`)
    - `List<SesionEstudio> listarPendientesAnterioresA(Fecha hoy)`
    - `int sumarMinutosRealesPorMateria(long materiaId)`
    - `int sumarMinutosRealesEnRango(Fecha desde, Fecha hasta)`
    - `void eliminarPorId(long id)`
  - `SesionesDeMateria` (ya existente): conserva su firma `int contar(long materiaId)`; se sustituye el adaptador provisional `SinSesionesDeMateria` por `SesionesDeMateriaRoom`.
- **Puertos de entrada y servicios (`application.port.in`, `application.service`):**
  - `VencerSesionesPendientes` + `VencerSesionesPendientesService`: consulta `listarPendientesAnterioresA(reloj.hoy())`, aplica `sesion.vencer()` y persiste las que cambiaron a `NO_REALIZADA` (`HU-13`, `RN-08`). Es idempotente y se ejecuta automáticamente al abrir Inicio, antes de listar/consultar sesiones y al comprobar arranque.
  - `PlanificarSesion` + `PlanificarSesionService`: valida entrada de materia activa, fecha `>= hoy`, horario (`RN-04`), comprueba solapamientos en `fecha` contra sesiones que ocupan horario (`RN-05`) y, si hay cruce, devuelve `ResultadoSesion.conConflicto(...)` con nombre de materia y rango horario; si es válida, persiste en `PENDIENTE` (`HU-10`).
  - `RegistrarSesionPasada` + `RegistrarSesionPasadaService`: valida materia activa, `1 <= diasAntiguedad <= 3` (`RN-06`), horario (`RN-04`), duración real (`> 0`), solapamientos (`RN-05`) y guarda en `COMPLETADA_TARDIA` (`HU-11`, `RN-11`).
  - `CompletarSesion` + `CompletarSesionService`: ejecuta transición `marcarCompletada(reloj.hoy(), duracionReal)` vía `EstadoSesion` (`HU-12`, `RN-07`, `RN-26`) y actualiza en `SesionRepository`.
  - `RegistrarSesionTardia` + `RegistrarSesionTardiaService`: calcula `diasAntiguedad = sesion.fecha().diasHasta(reloj.hoy())`, ejecuta transición `registrarTardio(diasAntiguedad, duracionReal)` vía `EstadoSesion` (`HU-13`, `RN-10`) y actualiza en `SesionRepository`.
  - `ReprogramarSesion` + `ReprogramarSesionService`: valida nueva fecha `>= hoy`, nuevo horario (`RN-04`), solapamientos en la nueva fecha ignorando la propia sesión original (`RN-05`), transiciona original → `REPROGRAMADA` vía `EstadoSesion`, planifica nueva sesión `PENDIENTE` con `sesionOrigenId = original.id()` y persiste en una transacción (`HU-14`, `RN-09`).
  - `EliminarSesion` + `EliminarSesionService`: verifica `sesion.validarEliminacion()` vía `EstadoSesion` (`PENDIENTE` únicamente, `RN-12`) y elimina (`HU-15`).
  - `ConsultarSesion` + `ConsultarSesionService`: vence pendientes primero, carga la sesión, su materia (nombre y color), calcula capacidades permitidas (`puedeCompletar`, `puedeRegistrarTardio`, `puedeReprogramar`, `puedeEliminar`) y, si es `REPROGRAMADA`, adjunta el enlace (`sesionDestinoId` y resumen fecha/horario) buscando por `sesionOrigenId` (`HU-12` a `HU-15`).
  - Actualización de `ListarMateriasService` (`HU-09`, `RN-11`): inyecta `SesionRepository` y calcula `horasRealizadas = sesiones.sumarMinutosRealesPorMateria(materia.id()) / 60.0`.
  - Actualización de `ObtenerResumenInicioService` (`HU-02`, `RN-08`, `RN-11`, `RN-13`, `RN-15`): inyecta `PerfilRepository`, `SesionRepository`, `MateriaRepository`, `Reloj` y `VencerSesionesPendientes`. Primero ejecuta el vencimiento automático; calcula `lunes = hoy.lunesDeSemana()` y `domingo = hoy.domingoDeSemana()`; suma minutos reales del rango y obtiene `horasRealizadas = minutos / 60.0`; construye las `DetalleSesionInicio` para `hoy` y determina la próxima sesión `PENDIENTE` a partir de `hoy` (priorizando la primera cuyo fin no ha pasado aún hoy o en días futuros, y si todas las pendientes de hoy superaron la hora de fin pero siguen hoy pendientes, mostrando la primera pendiente del día).

### D3. Base de datos Room versión 3 (`adapter.out.persistence`)

- Nueva tabla `sesion_estudio` representada por `SesionEntity`:
  - `id`: `long`, `@PrimaryKey(autoGenerate = true)`
  - `materia_id`: `long` (`FOREIGN KEY` → `materia(id) ON DELETE RESTRICT`, coherente con `RN-03`)
  - `fecha`: `String` (ISO `YYYY-MM-DD`, `@NonNull`)
  - `hora_inicio`: `String` (`HH:mm`, `@NonNull`)
  - `hora_fin`: `String` (`HH:mm`, `@NonNull`)
  - `duracion_planificada_minutos`: `int`
  - `duracion_real_minutos`: `Integer` (nullable; solo informado al completar o registrar tardío)
  - `estado`: `String` (`CodigoEstadoSesion.name()`, `@NonNull`)
  - `notas`: `String` (nullable)
  - `sesion_origen_id`: `Long` (nullable, `FOREIGN KEY` → `sesion_estudio(id) ON DELETE SET NULL`, `RN-09`)
  - Índices: `materia_id`, `fecha`, `estado`, `sesion_origen_id`.
- **Migración `MIGRATION_2_3`:** crea `sesion_estudio` y sus cuatro índices sin modificar `perfil`, `ajustes` ni `materia`; `StudyPlanDatabase` pasa a `version = 3` (sin `fallbackToDestructiveMigration`), exporta `app/schemas/.../3.json` y `Migracion3InstrumentalTest` verifica la migración `v2 → v3` conservando datos previos con `MigrationTestHelper`. La prueba existente de apertura de versión se actualiza a versión 3.
- **Adaptadores reales:** `SesionRepositoryRoom` (incluyendo método `@Transaction` en `SesionDao` para reprogramación atómica), `SesionesDeMateriaRoom` (reemplaza `SinSesionesDeMateria`) y `RelojSistema` (usa `java.util.Calendar` para devolver `Fecha` y `Hora` locales sin `java.time`). `AlmacenDatos.borrarTodo()` usa `clearAllTables()`, por lo que también borra `sesion_estudio` en orden seguro ante claves foráneas.

### D4. Pantallas, interacción UI y reglas XML estrictas (`adapter.in.ui`)

- **Inicio completo (`MainActivity`, `InicioViewModel`, `SesionInicioAdapter`, `contenido_inicio.xml`, `layout-w600dp/activity_main.xml`, `item_sesion_inicio.xml`):**
  - Integra sobre `contenido_inicio.xml`:
    - Saludo y barra de progreso semanal con horas reales (`X,X / Y h`).
    - Botón principal «+ Nueva sesión» (`minHeight="48dp"`) que abre `SesionFormularioActivity` en modo planificar con fecha de hoy preseleccionada (`HU-02` escenario 2; si no hay materias activas, muestra aviso con acceso directo a crear materia).
    - Tarjeta de **Próxima sesión pendiente** (visible cuando existe al menos una pendiente hoy o futura; al tocarla abre `SesionDetalleActivity`).
    - Sección **Sesiones de hoy** (`RecyclerView` con `SesionInicioAdapter` o mensaje de estado vacío); cada fila `item_sesion_inicio.xml` muestra franja/indicador de color de materia (`importantForAccessibility="no"`, `tools:tint`), nombre de materia, rango horario y **icono + etiqueta textual de estado** (`RNF-06`). Tocar una fila abre `SesionDetalleActivity`.
  - Cumple `RNF-04`: desde Inicio al formulario hay 1 toque.
- **Formulario de sesión (`SesionFormularioActivity`, `SesionFormularioViewModel`, `activity_sesion_formulario.xml`, `layout-w600dp/activity_sesion_formulario.xml`):**
  - Soporta 3 modos explícitos (`MODO_PLANIFICAR`, `MODO_PASADA`, `MODO_REPROGRAMAR`), con selector Material entre «Planificar sesión» y «Registrar sesión pasada» cuando se abre desde creación (y bloqueado al modo reprogramar cuando viene de `MODO_REPROGRAMAR`):
    - **Materia activa (`HU-10`, `HU-11`):** lista desplegable `TextInputLayout` (`Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu`) + `MaterialAutoCompleteTextView`; en modo reprogramar muestra la materia fija de la sesión original.
    - **Fecha y horas:** campos accesibles (`minHeight="48dp"`, `maxWidth="400dp"`) que muestran la fecha (`YYYY-MM-DD`) y horas (`HH:mm`) editables o elegibles mediante `MaterialDatePicker` / `MaterialTimePicker` (ya disponibles en Material Components sin dependencias extra), facilitando además entrada determinista en Espresso y TalkBack.
    - **Duración calculada y duración real:** en planificación muestra resumen vivo de duración planificada; en modo sesión pasada (`HU-11`) prellena la duración real con `horaFin - horaInicio` y permite editar los minutos reales (`RN-26`).
    - **Notas opcionales:** campo multilínea opcional (`maxWidth="400dp"`, máx. 500 caracteres).
    - **Retroalimentación de errores (`RN-04`, `RN-05`, `RN-06`):** cada `ErrorValidacion` se traduce desde `strings.xml`; en `SESION_SOLAPADA` se interpola el nombre de la materia y el rango horario en conflicto (`ConflictoHorario`).
- **Detalle de sesión (`SesionDetalleActivity`, `SesionDetalleViewModel`, `activity_sesion_detalle.xml`, `layout-w600dp/activity_sesion_detalle.xml`):**
  - Muestra materia (con círculo de color `importantForAccessibility="no"` y `tools:tint`), fecha, horario, duración planificada, duración real (si fue completada/tardía), **icono + etiqueta textual del estado** (`RNF-06`) y notas.
  - Cuando la sesión está `REPROGRAMADA`, muestra tarjeta y botón de enlace a la nueva sesión hija (`HU-14` escenario 2), abriendo su detalle al tocarlo.
  - Controles condicionales gobernados exclusivamente por `EstadoSesion`:
    - Si `puedeCompletar` (`PENDIENTE` de hoy): campo editable «Duración real (min)» prellenado con la duración planificada + botón «Completar sesión» (`HU-12`).
    - Si `puedeRegistrarTardio` (`NO_REALIZADA` y `antiguedad <= 3` días): campo editable «Duración real (min)» + botón «Registrar tardío» (`HU-13`, `RN-10`). Si es `NO_REALIZADA` con `> 3` días, se oculta «Registrar tardío» y se muestra aviso informativo del vencimiento de plazo de 3 días.
    - Si `puedeReprogramar` (`PENDIENTE` o `NO_REALIZADA`): botón «Reprogramar» que abre `SesionFormularioActivity` en `MODO_REPROGRAMAR` (`HU-14`).
    - Si `puedeEliminar` (`PENDIENTE`): botón «Eliminar sesión» que abre `MaterialAlertDialogBuilder` de confirmación (`HU-15`); para cualquier otro estado el botón permanece oculto (`RN-12`).
- **Cumplimiento del checklist XML de `AGENTS.md` §6.2:**
  - Cada layout nuevo y la revisión de los existentes (`activity_ajustes.xml`, `activity_materia_formulario.xml`, `activity_autenticacion.xml`, `item_materia.xml`, `item_encabezado_archivadas.xml`) cumplen: `tools:context`, `tools:fitsSystemWindows="true"`, `tools:tint`, `tools:text`, `android:id` en `<include>`, `match_parent` en raíces de `contenido_*.xml`, `minHeight="48dp"` en objetivos táctiles, `maxWidth="400dp"` + `layout_gravity="start"` en campos de entrada, `maxWidth="600dp"` / `layout_constraintWidth_max="600dp"` + `layout_constraintHorizontal_bias="0"` en textos/barras de progreso, `wrap_content` en botones/interruptores/radios, `contentDescription` en elementos visuales no textuales y variante `layout-w600dp/`.
  - Se definen 6 íconos vectoriales propios de estado en `res/drawable/` (`ic_estado_pendiente.xml`, `ic_estado_en_curso.xml`, `ic_estado_no_realizada.xml`, `ic_estado_completada.xml`, `ic_estado_completada_tardia.xml`, `ic_estado_reprogramada.xml`) para cumplir `RNF-06`.

### D5. Flujo de entregas por bloque (commits modulares y documentación en paralelo)

- Siguiendo las instrucciones del estudiante y `AGENTS.md` §4 y §7:
  - El trabajo de `/opsx:apply` se divide en 6 bloques secuenciales (Dominio → Aplicación → Persistencia → Presentación → Pruebas Espresso → Documentación/Diagramas).
  - Al finalizar cada bloque se ejecutan las pruebas correspondientes + `lintDebug`, se verifica la ausencia de imports prohibidos (`ArquitecturaTest`), se hace `git commit` por ruta explícita (formato `feat(HU-xx): ...`, `test(HU-xx): ...`, `docs: ...`, sin líneas de coautor ni atribución de IA) y `git push`.
  - Al cierre (Bloque 6), se actualizan `docs/build/diagramas_clases.py`, `docs/build/cotejo_clases.py`, los PNG desde código y `3.json`, `docs/documento/capitulos/04-diseno-del-software.md`, `docs/auditoria-ia/bitacora.md`, y se valida el documento APA con `.apa/scripts/revisar.py` y `generar_docx.py`.

## Risks / Trade-offs

- **Sin `java.time` en `minSdk 24`:** escribir aritmética gregoriana pura en `Fecha` exige verificar exhaustivamente años bisiestos, cambios de mes/año y fronteras lunes–domingo con JUnit, pero evita añadir *core library desugaring* al build sin autorización ni complejidad adicional en `build.gradle.kts`.
- **Reemplazo de `SinSesionesDeMateria` y cambio de comportamiento en `MateriasActivity`:** una materia a la que se le hayan creado sesiones en pruebas pasará a bloquear su eliminación (`RN-03`). Las pruebas existentes que verificaban «eliminar sin historial» deben seguir usando materias sin sesiones, y se añade prueba específica con sesiones reales.
- **Pruebas temporales en `MainActivity` y Espresso (`ANDROID_SERIAL` / reloj del dispositivo):** al usar `MaterialDatePicker`/`MaterialTimePicker` o reloj real en Espresso, los horarios de prueba deben ser tolerantes a la hora real del emulador (o apoyarse en una fecha futura / fecha de hoy con formato editable + reloj inyectable en pruebas unitarias de casos de uso y `ViewModel`). Se evita usar `Thread.sleep` usando aserciones limpias e Idling/ejecución determinista donde corresponda.
- **Mantenimiento del cotejo automático de diagramas (`cotejo_clases.py`):** cada clase nueva agregada en `app/` hace fallar `cotejo_clases.py` si no figura en `diagramas_clases.py`; por eso el Bloque 6 actualiza el script y verifica `python docs/build/cotejo_clases.py` antes del cierre.

## Open Questions

- Ninguna bloqueante para comenzar `/opsx:apply`: no se introducen librerías externas nuevas (`MaterialDatePicker`, `MaterialTimePicker`, `Room`, `RecyclerView`, `ViewModel` y `LiveData` ya están en `gradle/libs.versions.toml`) y todas las reglas de negocio (`RN-03` a `RN-13`, `RN-15`, `RN-26`) están definidas en `openspec/specs/`. Solo queda confirmar la aprobación humana de esta propuesta (`AGENTS.md` §3, paso 2) antes de implementar el Bloque 1.
