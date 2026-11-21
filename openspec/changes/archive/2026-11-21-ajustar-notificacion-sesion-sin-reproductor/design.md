# Design: ajustar-notificacion-sesion-sin-reproductor

## Context

### Problema

La notificación de la sesión En curso (HU-49) se construyó como un reproductor: `NotificadorSesion` usa `MediaStyle` con el token de `SesionMultimedia`, una `MediaSessionCompat` en estado reproduciendo que absorbe los botones de los audífonos. En el teléfono del Product Owner (HyperOS) YouTube Music quedó en PAUSED y StudyPlan en PLAYING, y la tarjeta de bloqueo mostró solo «Pausa». Además:

- `ContadorActividades` decide a los 1,2 s de detenerse la última Activity si fue bloqueo o salida y registra `SALIDA_APP` al instante, así que una salida breve (por ejemplo abrir YouTube 3 s tras usar el botón de bloquear) abre la pregunta «¿Seguiste estudiando?».
- `mostrarHito` suena dos veces (canal de importancia alta y `Ringtone.play()` directo en `vibrarYSonar`) y los avisos suenan encima de la música.
- `asegurarCanales` llama a `deleteNotificationChannel` del canal antiguo en cada refresco.

Motivación y alcance: ver [proposal.md](proposal.md). Requisitos: [specs/tecnicas-estudio/spec.md](specs/tecnicas-estudio/spec.md).

### Trazabilidad

| Requisito | Decisiones |
|---|---|
| HU-49 (notificación, música, canal) · RNF-12 (privacidad) · RNF-18 (permiso denegado) · RNF-06 (el estado no depende solo del color) | 1, 5, 6, 7 |
| HU-40 (salida y ventana de gracia) · HU-41 (la pregunta solo tras una salida registrada) · RN-23 · RN-24 | 2, 3, 4, 8, 9 |

### Estado del código verificado

- Al volver a primer plano, `ContadorActividades.onActivityStarted` pone `pendienteEvaluarRecuperacion = true` cuando antes hubo `salioDeLaApp`. `EvaluarRecuperacionSesionService` (rama del último tramo) inserta entonces un `SALIDA_APP` en la última marca dentro de la app y abre la pregunta, aunque no se haya registrado ninguna salida. Con una ventana de gracia que no escribe eventos, esto dispararía una falsa salida.
- `Handler.postDelayed` usa `uptimeMillis`, que se detiene cuando el teléfono duerme con la pantalla apagada.
- `app/build.gradle.kts` solo tiene JUnit y `arch.core.testing`: no hay Mockito ni Robolectric, y los métodos de Android no se pueden usar en pruebas JVM.
- `NotificadorSesion` con `privada` y sesión no corriendo omite el tiempo (`notif_sesion_privada_texto`), contra HU-49 («Sesión en curso» y el tiempo).

## Goals / Non-Goals

**Goals:**
- Notificación normal con botones que no interfiere con el audio de otras apps.
- Salir y volver en menos de 1 minuto no genera evento, aviso, cronómetro detenido ni pregunta; una salida de 1 minuto o más se registra con la hora real en que ocurrió.
- Avisos que no cortan ni duplican sonido sobre la música.

**Non-Goals:**
- Quitar `ServicioSesionEnCurso`: se mantiene en este change; se decide en un change aparte tras probar en el dispositivo del Product Owner.
- Hacer configurable el minuto de gracia.
- Garantizar que HyperOS muestre los botones colapsados en la pantalla de bloqueo: la spec los exige solo al expandir la notificación.

## Decisions

### 1. Notificación normal con acciones

Se quita `MediaStyle`, `estiloReproductor`, `SesionMultimedia` y `androidx.media` (`libs.androidx.media` en `app/build.gradle.kts` y su entrada en `gradle/libs.versions.toml`). Se construye con `NotificationCompat` normal: cronómetro del sistema o cuenta atrás Pomodoro, `setOnlyAlertOnce(true)`, `setOngoing(true)`, `setDeleteIntent` hacia `ACCION_REPUBLICAR` (ya existe; en Android 14 el usuario puede descartar la notificación del servicio), categoría `CATEGORY_STATUS`, `setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)` porque sin `MediaStyle` Android 12 o posterior retrasa hasta 10 s la notificación del servicio, y los tres botones con texto. Con `privada`, el tiempo se muestra también en pausa (corrige `privada && !corriendo`). El texto «No molestar activado» no debe truncarse a 360 dp (se verifica). Sin permiso de No molestar, el botón abre la pantalla del sistema con `PendingIntent.getActivity`, como hoy.

*Alternativas:* conservar la sesión multimedia sin `MediaStyle` (sigue quitando la prioridad a la música); `BigTextStyle` o `MessagingStyle` (no aportan nada al cronómetro ni a los botones).

### 2. Ventana de gracia con salida retroactiva

`ContadorActividades` captura `instanteSalidaMs` con el puerto `Reloj` en `onActivityStopped`. Tras decidir (a los 1,2 s) que fue salida y no bloqueo, en lugar de registrarla programa la espera de `GRACIA_SALIDA_MS = 60_000`. Si una Activity vuelve a iniciarse antes de vencer y el reloj real confirma que pasó menos del minuto, se cancela y no se escribe nada. Si vence, se llama a `RegistrarSalidaApp.ejecutar(instanteSalidaMs)` y se muestra el aviso. Así la salida queda en la hora real en que ocurrió («Saliste a las 15:00») y el minuto de gracia solo cuenta como estudio cuando se cancela, nunca se resta de una salida confirmada.

*Alternativas descartadas:* registrar al instante y anular al volver (escribe y borra eventos); sellar la salida al vencer el minuto (pierde la hora real y reduce el tramo declarado).

### 3. Falso cierre abrupto al volver dentro de la gracia

Al cancelar una gracia pendiente en el mismo proceso, `onActivityStarted` no activa `pendienteEvaluarRecuperacion` (se mantienen `sesion.alVolverAPrimerPlano()` para HU-04 y `NotificadorSesion.reconciliar`). Si el sistema o el estudiante cierran la app dentro del minuto, el proceso nuevo arranca con `pendienteEvaluarRecuperacion = true` y se aplica el camino de cierre abrupto de hoy (inserta la salida en la última marca y pregunta): es el comportamiento actual y no se cambia en este change. Que `ServicioSesionEnCurso` mantenga vivo el proceso reduce ese caso.

### 4. Reloj real y aviso del minuto

Al volver, la comparación se hace siempre con `Reloj` y no con el temporizador: si ya pasó el minuto (por ejemplo el teléfono durmió con la pantalla apagada y el temporizador no avanzó), se registra la salida retroactiva con `instanteSalidaMs` y se abre la recuperación. El aviso a los 60 s usa `Handler`, sin permisos nuevos; con el teléfono dormido puede retrasarse o no llegar, y la salida igualmente queda registrada al volver. *Alternativa descartada por decisión del Product Owner:* `AlarmManager` (aviso puntual, pero una alarma extra y posible permiso de alarma exacta).

### 5. `RegistrarSalidaApp.ejecutar(long instanteSalidaMs)`

Firma única: se actualizan todos los llamadores (`ContadorActividades` y los tests que hoy llaman a `ejecutar()` sin argumento; se localizan con `grep`). `RegistrarSalidaAppService` evalúa guardas y descanso Pomodoro con `instanteSalidaMs` (si la salida ocurrió en un descanso, se corta en el fin del descanso como en «Exceso sobre descanso Pomodoro»), aborta si el último evento persistido tiene `instante > instanteSalidaMs` (por ejemplo una pausa y reanudación durante la gracia) y, dentro del ejecutor, revalida que no haya una Activity en primer plano para no avisar con la app abierta.

### 6. Avisos silenciosos con música

No se crea un puerto `EstadoAudio` en el núcleo: lo consulta solo `NotificadorSesion` (capa de presentación). `AudioManager.isMusicActive()` se obtiene con un proveedor `package-private` sustituible en pruebas, restablecido en el reinicio de estado de `Pruebas`. Aplica a todos los avisos del canal `CANAL_AVISOS_ESTUDIO`: volver, hito, meta alcanzada y recordatorio previo (HU-34). Con música: `setSilent(true)` y sin vibración. Sin música: en Android 8 o posterior el canal pone sonido y vibración; en Android 7 se usa `setDefaults(DEFAULT_ALL)`. Se elimina `vibrarYSonar()` y `Ringtone.play()` (sonido duplicado y vibración doble).

*Alternativas descartadas:* puerto `EstadoAudio` (ningún caso de uso lo consume); un canal silencioso extra (el estudiante tendría que elegir entre dos canales).

### 7. Canal antiguo

`asegurarCanales` borra `canal_sesion_activa` solo si `getNotificationChannel` no es nulo.

### 8. Pruebas sin Mockito ni Robolectric

`ContadorActividades` recibe abstracciones Java puras: `ProgramadorRetardado` (publicar/cancelar con retraso), `Reloj` y un proveedor de «pantalla apagada». La implementación de producción usa `Handler` y `PowerManager`; las pruebas JVM usan dobles manuales con prefijo `Fake`. Para Espresso se añade `ContadorActividades.fijarGraciaParaPruebas(long ms)`, restablecido en el reinicio de estado de `Pruebas`, así `BloqueoPantallaEspressoTest` (que hoy espera `SALIDA_APP` en 10 s) no depende de 60 s reales.

### 9. Documento

RN-23 se reescribe en `docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md` con la ventana de gracia y RN-24 se aclara («al volver tras una salida registrada»). Ambas requieren aprobación del Product Owner antes de aplicar (tarea 1.1).

## Risks / Trade-offs

- [HyperOS no muestra los botones colapsados en el bloqueo] → La spec exige los botones al expandir; si ni así aparecen, se registra como limitación y se evalúa otro mecanismo en un change aparte.
- [Cierre de la app dentro del minuto] → Camino de cierre abrupto actual (pregunta al volver); el servicio reduce la probabilidad.
- [Aviso del minuto retrasado con el teléfono dormido] → La salida se registra bien al volver con la hora real.
- [Música detectada solo para reproducción multimedia] → `isMusicActive` no cubre otros audios; una llamada ya silencia las notificaciones por el modo del sistema.
- [Temporizador de 60 s en pruebas] → Tiempo de gracia inyectable y `fijarGraciaParaPruebas`.
