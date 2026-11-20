# Proposal: controles de sesión estilo reproductor y bloqueo de pantalla para estudiar

## Why

El Product Owner revisó la sesión en curso en el emulador el 2026-11-20 y encontró tres problemas.

- La pantalla de la sesión distrae.
- No hay forma de bloquear el teléfono sin que la app lo trate como una salida. Hoy, apagar la pantalla registra «salida», congela el reloj y al volver pregunta «¿seguiste estudiando?» (HU-40, RN-23).
- Fuera de la app solo aparece la notificación «Tu sesión sigue en curso — volver». No tiene controles, solo sale cuando el estudiante ya abandonó la app y no avisa con sonido.

Pidió que la sesión se controle como un reproductor de música: con el teléfono bloqueado o desde la barra de notificaciones se ven la materia, el tiempo y la fase Pomodoro, y se puede pausar o detener sin abrir la app. Pidió también un botón de candado dentro del cronómetro que bloquee el teléfono y cuente como estudio, con una confirmación antes de bloquear.

Es un solo cambio: los tres problemas se resuelven con las mismas piezas (los eventos de pantalla, la notificación y el candado) y se revisan juntos.

## What Changes

- **Notificación de sesión estilo reproductor (HU-49, nueva).**
  - Mientras la sesión esté En curso (la pausa es un evento dentro de En curso) una notificación muestra la materia, el cronómetro, la fase Pomodoro y lo que le falta, con Pausar/Reanudar y Detener.
  - Se ve con el teléfono bloqueado. Con el bloqueo de la app activo se oculta la materia y, en Android 12 o posterior, los botones piden desbloquear el teléfono; en Android 11 o anterior los botones se muestran porque el sistema no puede exigir el desbloqueo.
  - Android 14 permite descartarla con el dedo; si se descarta, se vuelve a mostrar mientras la sesión siga En curso.
  - Un servicio en primer plano que vive solo mientras la sesión está En curso la sostiene.
  - Absorbe la notificación «volver»: el estado de salida pasa a ser un estado de esta misma notificación, con el tiempo detenido.
- **Aviso de salida con sonido y vibración (HU-40, modificada).** Al salir de la app suena y vibra un aviso una sola vez. Hoy se publica en un canal de importancia baja y no avisa.
- **Candado dentro del cronómetro (HU-50, nueva).**
  - Botón de candado dentro del anillo del cronómetro. Antes de bloquear pide confirmación, con «No volver a preguntar». La primera vez explica y solicita el permiso de administrador del dispositivo, limitado a bloquear la pantalla.
  - Bloquear con el candado no es una salida: se registran los eventos «bloqueo de pantalla» y «desbloqueo», el reloj sigue y no aparece la pregunta de W13.
  - Ajustes permite retirar el permiso.
- **Pantalla apagada con StudyPlan en primer plano (HU-40, modificada).** Si la pantalla se apaga sola o con el botón de encendido mientras una pantalla de StudyPlan está en primer plano, se trata igual que el candado. Solo es salida si el estudiante ya había salido de la app.
- **Sin tope de tiempo y sin pregunta por el hueco (decisión del Product Owner).** Si el proceso muere con el teléfono bloqueado o el estudiante desbloquea y abre otra app, la sesión sigue contando como estudio hasta que vuelva a StudyPlan; no se registran eventos ni se pregunta. Una sesión sin hora de fin con el teléfono bloqueado suma horas hasta que se desbloquea o vence (RN-29).
- **No molestar visible en la sesión (HU-24, modificada).** Un control en la pantalla de sesión activa o desactiva No molestar solo para esa sesión, con su estado visible aunque esté apagado.
- **Pantalla de sesión estilo reproductor (HU-03, modificada).** Anillo con el reloj, la materia y el candado dentro. Debajo, una fila: Detener, Cambiar materia, Pausar/Reanudar como botón redondo de 64 dp y No molestar. El botón de 64 dp es la única excepción a «todos los botones con la misma altura», aprobada por el Product Owner.
- **Logo de la app (tarea extra pedida por el Product Owner el 2026-11-20).** El icono de la app, el de
  notificaciones, la pantalla de inicio de Android 12 o posterior y los recursos de Google Play pasan a usar el logo
  `docs/documento/recursos/logos/logo-app.jpg` (anillo blanco con un arco verde azulado y un libro abierto, sobre el
  índigo `#4338CA`). No cambia ninguna historia de usuario ni regla de negocio.
- **Dependencia y regla de AGENTS.md.**
  - Se añade `androidx.media:media` al catálogo de versiones (aprobada por el Product Owner el 2026-11-20; la versión exacta se confirma al implementar).
  - Se aclara AGENTS.md §2: siguen prohibidos los servicios en primer plano permanentes y se permite uno solo mientras haya una sesión En curso.

## Capabilities

### New Capabilities
Ninguna. Las dos historias nuevas pertenecen a la ejecución de la sesión, que ya está en `tecnicas-estudio`.

### Modified Capabilities
- `tecnicas-estudio`:
  - ADDED: HU-49 (controles de sesión en la notificación y la pantalla bloqueada) y HU-50 (bloquear la pantalla para estudiar).
  - MODIFIED: HU-40 (aviso de salida con sonido; la pantalla apagada con StudyPlan en primer plano ya no es salida) y HU-24 (control de No molestar en la sesión en curso).
- `perfil-ajustes`: MODIFIED HU-03. «Botones uniformes» admite la excepción del control principal de la sesión en curso y se añade el escenario «Controles de la sesión en curso».

## Fuera de alcance

- Reproducir audio o integrarse con los controles multimedia de otras apps. La sesión sí publica una `MediaSession` mientras está En curso (sin audio) para que Android muestre la tarjeta de control en la pantalla de bloqueo; los botones de los audífonos se ignoran.
- Bloquear otras apps o impedir que el estudiante salga de StudyPlan (modo kiosco o fijar pantalla).
- Detectar que el estudiante desbloqueó y abrió otra app, y limitar las horas con el teléfono bloqueado (decisión del Product Owner).
- Cambiar de materia, repartir el tramo o responder W13 desde la notificación; esas acciones siguen dentro de la app.
- Widgets en la pantalla de inicio del teléfono, Wear OS y Android Auto.
- Usar un servicio de accesibilidad para bloquear la pantalla, por la política de Google Play.
- Cambiar la forma de calcular el tiempo real (RN-22) o las estadísticas de concentración (RN-30).

## Impact

- **Historias y reglas:**
  - HU-49 y HU-50 son nuevas. Se modifican HU-03, HU-24 y HU-40.
  - RN-23 cambia: la pantalla apagada con StudyPlan en primer plano no es salida.
  - RNF afectados: RNF-03 (batería con la pantalla apagada), RNF-06 (iconos con nombre accesible y control de 64 dp), RNF-12 (privacidad en la pantalla bloqueada) y RNF-18 (permisos de notificaciones, servicio en primer plano y administrador del dispositivo).
- **Dominio:** `TipoEventoEstudio` gana `BLOQUEO_PANTALLA` y `DESBLOQUEO`. Se guardan por nombre en `evento_estudio.tipo`, así que no hace falta migrar Room.
- **Aplicación:** casos de uso nuevos `RegistrarBloqueoPantalla`, `RegistrarDesbloqueo` y `AlternarNoMolestarSesion`. No cambian los servicios existentes de la sesión: la notificación se refresca desde un decorador de `ProgramadorAvisosSesion`.
- **Adaptadores:** nuevos `ServicioSesionEnCurso`, `ProgramadorAvisosConNotificacion`, `BloqueoPantallaSistema`, `ReceptorAdministradorBloqueo` y `PreferenciasBloqueoPantalla`. Se amplían `NotificadorSesion`, `ReceptorAvisosSesion`, `ContadorActividades`, `EventoEstudioAdapter`, `SesionEnCursoActivity`, `AjustesActivity` y `StudyPlanApplication`.
- **Manifiesto:** permisos `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE`, el servicio con su subtipo y el receptor de administrador con `res/xml/politicas_bloqueo.xml`.
- **Dependencias:** `androidx.media:media` (nueva, aprobada).
- **Recursos gráficos:** `ic_launcher_foreground.png`, `ic_launcher_monochrome.png`, `ic_launcher_background.xml`, los iconos heredados `mipmap-*`, `ic_notificacion.png` (cinco densidades), `ic_splash.xml`, `values-v31/themes.xml` y los archivos de Google Play en `docs/documento/recursos/logos/play-store/`.
- **Documentación:** capítulo 3 (tabla de HU, tarjetas HU-49 y HU-50, HU-24 y HU-40), capítulo 4 (pantalla de la sesión y diagramas de clases), AGENTS.md §2 y la bitácora.
- **Google Play:** el servicio `specialUse` y el administrador del dispositivo exigen una declaración en la consola y una explicación visible al usuario. El riesgo y el plan B están en design.md.
