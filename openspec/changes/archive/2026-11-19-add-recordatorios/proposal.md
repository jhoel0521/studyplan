# Proposal: Recordatorios locales sin conexión para sesiones planificadas (`E9`)

## Why
El estudiante puede planificar sesiones futuras (`HU-10`) y reprogramarlas (`HU-14`), pero no recibe un aviso antes del inicio de cada sesión (`HU-34`), ni dispone del ajuste de antelación predeterminada (`W10`) ni del selector por sesión en el formulario (`W3`). Implementar recordatorios locales sin conexión cierra la épica **E9 (`recordatorios`)** del MVP permitiendo programar avisos configurables, solicitar permisos de notificaciones y alarmas exactas con explicación previa (`RNF-18`), cancelar avisos obsoletos y abrir o iniciar la sesión protegiendo la privacidad bajo bloqueo biométrico/PIN (`HU-04`, `RNF-12`).

## What Changes
- **Modelo de dominio (`domain`):**
  - Introducir el enum `AntelacionRecordatorio` (`SIN_RECORDATORIO(0)`, `MINUTOS_5(5)`, `MINUTOS_15(15)`, `MINUTOS_30(30)`), adoptarlo de forma uniforme en `Ajustes` (`recordatorioPorDefecto = MINUTOS_15`, sustituyendo al atributo conceptual `Ajustes.recordatorioMin` de `diagramas.py`) y en `SesionEstudio` (`antelacionRecordatorio`), añadiendo el método puro `instanteRecordatorioUtcMilis()` (sobre `Instante.sumarMinutos`) y el DTO puro `AvisoRecordatorioPendiente`.
  - Las sesiones pasadas (`HU-11`) y las sesiones sin plan «Estudiar ahora» (`HU-47`) conservan `SIN_RECORDATORIO`.
- **Puertos y servicios de aplicación (`application`):**
  - Definir el puerto de salida segregado `ProgramadorRecordatorios` (`programar(sesionId, instanteDisparoUtcMilis)`, `cancelar(sesionId)`, `cancelarTodasNotificaciones()`) para mantener los avisos `PENDIENTE` separados de `ProgramadorAvisosSesion` (`EN_CURSO`).
  - Incorporar las operaciones de aplicación `ConfigurarRecordatorioPorDefecto` (`ConfigurarRecordatorioPorDefectoService`) y `ReprogramarRecordatoriosPendientes` (`ReprogramarRecordatoriosPendientesService`).
  - Integrar `ProgramadorRecordatorios` en los servicios que crean, reemplazan o extinguen sesiones `PENDIENTE` (`PlanificarSesionService`, `ReprogramarSesionService`, `EliminarSesionService`, `IniciarSesionService`, `DecidirFinPlanificadoOInvasionService`, `RepartirTramoSesionService` y `BorrarDatosService` para limpiar notificaciones visibles tras `borrarTodo()`).
- **Persistencia Room (`v7` -> `v8`):**
  - Añadir la columna `recordatorio_minutos` en `ajustes` (`NOT NULL DEFAULT 15`) y en `sesion_estudio` (admite `NULL`) mediante `Migraciones.MIGRATION_7_8` y esquema `8.json`.
- **Sistema Android, permisos y UI (`adapter`):**
  - Implementar `ProgramadorRecordatorios` dentro de `ProgramadorAvisosAlarmManager` mediante acción de `Intent` independiente (`ACCION_RECORDATORIO_SESION`) y `requestCode` aislado por `sesionId`, utilizando `setExactAndAllowWhileIdle` en API < 31 o con `SCHEDULE_EXACT_ALARM` concedido, respaldo `setAndAllowWhileIdle` bajo restricciones Doze y protección frente a cuota del sistema.
  - Ampliar `ReceptorAvisosSesion` y `AndroidManifest.xml` para revalidar en Room antes de notificar (filtrando mutaciones concurrentes tras `RM`/`EDIT`/`START`), y reprogramar tras `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` y `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, emitiendo de inmediato los avisos vencidos cuya sesión aún no inicia.
  - Ampliar `NotificadorSesion` sobre `CANAL_AVISOS_ESTUDIO` mostrando materia y rango horario local (`HH:mm – HH:mm` con zona si difiere), visibilidad `VISIBILITY_PRIVATE` con versión pública genérica si `bloqueoActivo` es verdadero (`HU-04`, `RNF-12`), apertura del detalle en `SesionDetalleActivity` al tocar el cuerpo y arranque directo «Iniciar» en `SesionEnCursoActivity` (`LIBRE` + `HEREDAR_GLOBAL`) sin trampolín ni `CLEAR_TASK`, corrigiendo `puedeIniciar()` en cruces de medianoche.
  - Actualizar `AjustesActivity` (`W10`), `SesionFormularioActivity` (`W3`, incluyendo `layout-w600dp`), `SesionDetalleActivity` (incluyendo `layout-w600dp`), el flujo explicativo no bloqueante para `POST_NOTIFICATIONS` (Android 13+) y `SCHEDULE_EXACT_ALARM` (Android 12+) con banner persistente en pantalla (`RNF-18`) y aviso informativo si la ventana del recordatorio ya pasó al planificar.

### Decisiones confirmadas por el Product Owner
- **PO-1 (Antelación inicial y opciones):** `Ajustes.porDefecto()` inicia en `15 minutos antes` (coherente con `W3` y `W10`), y el selector ofrece `{Sin recordatorio, 5, 15, 30}` minutos; cambiar el ajuste global afecta únicamente a sesiones planificadas a partir de ese momento.
- **PO-2 (Cuerpo vs. acción «Iniciar»):** tocar el cuerpo abre `SesionDetalleActivity` para elegir técnica/No molestar manualmente; pulsar «Iniciar» abre directamente `SesionEnCursoActivity` e inicia con `TecnicaEstudio.LIBRE` y `PreferenciaNoMolestar.HEREDAR_GLOBAL` (previa autenticación si hay bloqueo activo, sin trampolín y sin `CLEAR_TASK`).
- **PO-3 (`SCHEDULE_EXACT_ALARM` y `RNF-18`):** declarar `android.permission.SCHEDULE_EXACT_ALARM` (nunca `USE_EXACT_ALARM`), explicarlo al estudiante junto al acceso hacia los ajustes del sistema según `RNF-18`, usar `setExactAndAllowWhileIdle` si está concedido y degradar a `setAndAllowWhileIdle` si se deniega.
- **PO-4 (Ventana ya vencida):** si `inicio - antelación <= ahora < inicio` al guardar/reprogramar, se guarda la sesión sin alarma y se muestra un texto informativo en el formulario; tras `BOOT_COMPLETED`, si el aviso venció antes del inicio, se notifica de inmediato.
- **PO-5 (`POST_NOTIFICATIONS` y detalle):** solicitar el permiso con explicación previa al activar una antelación en Ajustes o en el desplegable/banner del formulario (sin bloquear «Guardar»), mostrar un banner persistente en pantalla si se niega (en lugar de `Snackbar` efímero destruido al cerrar el formulario) y mostrar la antelación configurada en `SesionDetalleActivity`.

## Out of Scope
- Respaldo y restauración en Google Drive (`E10`: `HU-35`, `HU-36`, `HU-37`).
- Widget de pantalla de inicio (`HU-05`) y rediseño visual de pantallas ajenas a recordatorios.
- Posponer (*snooze*) desde la notificación o antelaciones personalizadas distintas de `{Sin recordatorio, 5, 15, 30}` minutos.
- Declarar el permiso restringido `USE_EXACT_ALARM` o agregar dependencias externas nuevas en Gradle (incluido WorkManager).
- Recordatorios recurrentes independientes de sesiones concretas o notificaciones remotas en red.

## Capabilities

### New Capabilities
- Ninguna capacidad nueva; `HU-34` refina la capacidad existente `recordatorios`.

### Modified Capabilities
- `recordatorios`: Refinar `HU-34` (*Recibir recordatorio de sesión*) cubriendo `{Sin recordatorio, 5, 15, 30}` min, ajuste predeterminado en `Ajustes`, preselección/ocultación en el formulario, explicación y estado persistente para `POST_NOTIFICATIONS` (Android 13+) y `SCHEDULE_EXACT_ALARM` (Android 12+, `RNF-18`), cancelación ante salidas de `PENDIENTE` o borrado de datos, apertura en `SesionDetalleActivity` vs. inicio directo en `SesionEnCursoActivity`, privacidad en pantalla de bloqueo (`HU-04`, `RNF-12`), y recuperación tras `BOOT_COMPLETED` / actualización de paquete.

## Impact
- **IDs afectados:**
  - `HU-34` (`Must`, 3 pts): funcionalidad central de recordatorios locales.
  - `HU-04` y `RNF-12`: respeto del bloqueo biométrico/PIN al abrir/iniciar y ocultamiento de datos en pantalla de bloqueo (`VISIBILITY_PRIVATE`), además de almacenamiento privado en Room.
  - `HU-07`: lectura actualizada del nombre de materia en Room al dispararse la alarma.
  - `HU-38` y `RN-21`: limpieza de notificaciones de recordatorio activas tras borrar los datos locales.
  - `HU-47`: exclusión de recordatorios en sesiones sin plan «Estudiar ahora».
  - `RN-04`, `RN-07`, `RN-08`, `RN-09`, `RN-12`, `RN-27`, `RN-28`: transiciones y cancelaciones al planificar, iniciar, reprogramar, eliminar, vencer, cambiar a siguiente o repartir tramo.
  - `RNF-02`: consultas Room fuera del hilo principal (`goAsync()` en receptor y ejecutores en fondo).
  - `RNF-03`: sin servicios en primer plano permanentes (`AlarmManager`).
  - `RNF-06`, `RNF-07`, `RNF-08`: accesibilidad (`48dp`, `AGENTS.md` §6.2), Material 3 claro/oscuro y textos bilingües `es`/`en`.
  - `RNF-10`, `RNF-14`, `RNF-15`, `RNF-16`, `RNF-17`, `RNF-18`: persistencia ante rotación, validación de entradas, pruebas JUnit/instrumentadas, trazabilidad `HU -> spec -> código`, compatibilidad `API 24..37` y gestión explicativa de permisos.
- **Código y recursos afectados:** véase la sección `Target Scope & File Boundary` en [design.md](design.md).
- **Dependencias Gradle:** Ninguna dependencia externa nueva; se utilizan `AlarmManager`, `NotificationCompat`, `ActivityResultContracts.RequestPermission` y Room existentes.
