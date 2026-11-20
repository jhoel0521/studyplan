# Design: controles de sesión estilo reproductor y bloqueo de pantalla

## Context

**Problema.** El Product Owner revisó la sesión en curso en el emulador (2026-11-20; bitácora, sesión 20, filas 141 a 143) y encontró tres fallos:
1. Apagar la pantalla cuenta como «salida»: congela el reloj y al volver pregunta «¿seguiste estudiando?», aunque el estudiante estaba estudiando.
2. No hay un botón para bloquear el teléfono desde el cronómetro.
3. Fuera de la app la sesión no se puede controlar: la única notificación (`NotificadorSesion.mostrarAvisoRetorno`) solo aparece tras una salida, no tiene botones y usa un canal de importancia baja, así que no avisa.

Los motivos completos están en `proposal.md` y los escenarios en `specs/`. Este documento describe cómo se hace.

**Estado actual del código** (todo verificado leyendo el código):
- `ContadorActividades.onActivityStopped` cuenta las actividades iniciadas y, cuando la cuenta llega a 0, llama a `RegistrarSalidaApp.ejecutar()`. No distingue apagar la pantalla de cambiar de app.
- `ProgramadorAvisosSesion` (puerto de salida) programa los hitos de la sesión con `AlarmManager` y los cancela. Los servicios lo llaman en cada cambio de estado: siete mediante `EjecucionHitos.programarProximoHito` (`CambiarDeMateria`, `DecidirFinPlanificadoOInvasion`, `EstudiarAhora`, `IniciarSesion`, `PausarReanudarSesion`, `RegistrarSalidaApp`, `ResolverRegresoApp`) y el resto con `cancelarAvisos`.
- `ConsultarEjecucionSesion.ejecutar(sesionId)` devuelve `ResumenEjecucionSesion` con la materia, los milisegundos efectivos, si está pausada, el estado Pomodoro, si hay una salida por resolver y si No molestar está activo. Es todo lo que necesita la notificación.
- «En pausa» no es un estado de la sesión: es un evento dentro de En curso. La notificación y el servicio duran toda la sesión En curso, pausa incluida.
- Los canales: `CANAL_SESION_ACTIVA` (importancia baja) y `CANAL_AVISOS_ESTUDIO` (importancia alta).
- No molestar se guarda por sesión como `PreferenciaNoMolestar` (`HEREDAR_GLOBAL`, `FORZAR_ACTIVAR`, `FORZAR_DESACTIVAR`) y `ControladorNoMolestar` expone `tienePermiso()`, `aplicar(boolean)` y `restaurarSiAplica()`.
- No hay ningún servicio propio ni permisos `FOREGROUND_SERVICE*` en el manifiesto.

**Restricciones.** Java, minSdk 24, targetSdk 37; el núcleo (`domain`, `application`) es Java puro (`ArquitecturaTest`); las dependencias nuevas necesitan aprobación humana (AGENTS.md §2).

## Hechos de plataforma verificados el 2026-11-20

Fuentes: documentación oficial de Android (developer.android.com).
- **Android 14 deja descartar la notificación de un servicio en primer plano** aunque use `setOngoing(true)`. Sigue sin poder descartarse con el teléfono bloqueado o con «Borrar todo». Quedan exentas las notificaciones de llamadas (CallStyle), de controladores de política de dispositivo y las multimedia. Esta revisión corrige la premisa del primer borrador, que decía lo contrario. Fuente: «Behavior changes: all apps», Android 14.
- **Un servicio en primer plano no puede iniciarse desde segundo plano** si se apunta a Android 12 o posterior, salvo excepciones. Documentadas: un receptor de `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED` o `MY_PACKAGE_REPLACED`, y que el usuario toque un botón de una notificación. En Android 15 los receptores de `BOOT_COMPLETED` no pueden iniciar los tipos `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` ni `microphone`; `specialUse` no está en esa lista. La excepción de las alarmas exactas solo vale para una acción pedida por el usuario, así que **no** se puede suponer para una alarma de hito. Fuente: «Restrictions on starting a foreground service from the background».
- **`setAuthenticationRequired(true)` en una acción de notificación existe desde Android 12 (API 31)**; no hay equivalente antes. Es independiente de la visibilidad de la notificación. Fuente: referencia de `Notification.Action.Builder`.
- **No verificado:** que Google Play acepte el servicio `specialUse` y el administrador del dispositivo en una app de consumo. Se declara en la consola al publicar (Open Questions).

## Goals / Non-Goals

**Goals:**
- Una sola notificación de sesión, que cambia de estado (corriendo, pausa, salida, fase Pomodoro) en lugar de multiplicarse.
- El tiempo de la notificación lo cuenta el sistema (`setUsesChronometer`); el proceso no ejecuta ningún temporizador con la pantalla apagada.
- Apagar la pantalla con StudyPlan en primer plano, con el candado o con el botón de encendido, sigue una sola ruta de código.
- No cambiar los servicios existentes de la sesión: la notificación se refresca desde un decorador de `ProgramadorAvisosSesion` (decisión 4).

**Non-Goals:**
- Reproducir audio: la `MediaSession` que se publica es solo la que hace que Android muestre la tarjeta de control en la pantalla de bloqueo; los botones físicos de los audífonos no la controlan.
- Detectar que el estudiante desbloqueó y abrió otra app. Mientras no vuelva a StudyPlan cuenta como estudio (decisión 2).
- Poner un tope de horas al estudio con el teléfono bloqueado.

## Architecture

```
Toque en el candado ─► diálogo ─► BloqueoPantallaSistema.bloquear()  (lockNow)
Botón de encendido ──┐                                  │
Apagado automático ──┴─► pantalla apagada ─► ContadorActividades.onActivityStopped
                                                   │ isInteractive() == false
                                                   ▼
                                     RegistrarBloqueoPantalla ─► evento BLOQUEO_PANTALLA
Volver a StudyPlan ─► ContadorActividades.onActivityStarted ─► RegistrarDesbloqueo ─► evento DESBLOQUEO

Cambio de estado de la sesión ─► servicio existente ─► ProgramadorAvisosSesion
                                                         │ (decorador ProgramadorAvisosConNotificacion)
                                                         ├─► AlarmManager (programar o cancelar, como hoy)
                                                         └─► NotificadorSesion.refrescar(sesionId)
                                                                  │ lee ConsultarEjecucionSesion
                                                                  ▼
                                              ServicioSesionEnCurso (primer plano) ─► notificación
Botón de la notificación ─► ReceptorAvisosSesion (PAUSAR, REANUDAR, DETENER) ─► casos de uso existentes
```

## Decisions

### 1. Eventos `BLOQUEO_PANTALLA` y `DESBLOQUEO`
- **Qué:** dos valores nuevos en `TipoEventoEstudio`, con `marcaDentroDeApp = true` (cuentan como tiempo dentro de la app).
- **Por qué sin migración:** `evento_estudio.tipo` guarda el nombre del enum.
- **Efecto en el código:**
  - `CalculadorTiempoReal` ya ignora los tipos desconocidos (`default: break`) y `CalculadorEstadisticas` no usa el enum: el tiempo y la concentración no cambian y solo hay que añadir pruebas.
  - `EventoEstudioAdapter.etiquetaDe` sí cambia: su `default` devuelve «Fin», así que sin los dos casos nuevos el detalle de la sesión mostraría «Fin».
  - `obtenerUltimoInstanteDentroDeApp` devolverá el instante del bloqueo como última marca. Es lo que se quiere: el bloqueo cuenta como estudio.
- **Alternativa descartada:** registrar `SALIDA_APP` con un tópico «Bloqueo voluntario». Contaminaría las métricas de salidas (RN-30) y obligaría a filtrar en cada consulta.

### 2. Detección del bloqueo y del desbloqueo, sin temporizadores
- **Bloqueo:** `ContadorActividades.onActivityStopped`, con la cuenta en 0, comprueba `PowerManager.isInteractive()`. Si es `false`, llama a `RegistrarBloqueoPantalla`; si es `true`, sigue el flujo actual de `RegistrarSalidaApp`. El candado, el botón de encendido y el apagado automático son el mismo caso.
- **`RegistrarBloqueoPantalla`:** registra el evento solo con una sesión En curso, sin salida pendiente y si el último evento no es ya `BLOQUEO_PANTALLA` (evita duplicados). Durante un descanso Pomodoro también lo registra: es neutro.
- **Desbloqueo:** `ContadorActividades.onActivityStarted` llama a `RegistrarDesbloqueo`, que registra `DESBLOQUEO` solo si el último evento es `BLOQUEO_PANTALLA`. El instante es el de volver a la app.
- **Si el estudiante desbloquea y abre otra app:** no se detecta, y el tiempo sigue contando como estudio hasta que vuelva a StudyPlan. Es decisión del Product Owner del 2026-11-20 («sigue estudiando»). Por eso desaparecen la comprobación a 2 segundos y la regla RN-34 que el primer borrador proponía.
- **Proceso terminado con el teléfono bloqueado:** no se hace nada: el bloqueo queda abierto, la sesión sigue En curso y cuenta como estudio, sin evento nuevo ni pregunta. Una prueba lo fija.
- **Qué se asume, y cómo se comprueba:** que `isInteractive()` ya es `false` cuando llega `onActivityStopped` tras un apagado. Es una suposición todavía sin probar: la comprueba la tarea 3.2 y solo corre en el emulador. Si otro dispositivo se comporta distinto, el peor caso es el comportamiento actual (una salida y la pregunta al volver), nunca una pérdida de datos.
- **Alternativa descartada:** un receptor de `SCREEN_OFF` y `USER_PRESENT` dentro del servicio. Daría el instante exacto del desbloqueo, pero dependería de que el servicio esté vivo y aun así necesitaría `ContadorActividades` para distinguir bloqueo de salida; es más piezas para un dato que ya no se necesita.

### 3. Servicio en primer plano `ServicioSesionEnCurso`
- **Para qué:** sostiene la notificación con el proceso vivo mientras la sesión está En curso, de modo que sus botones respondan con el teléfono bloqueado y el sistema no mate el proceso con facilidad. **No** impide descartar la notificación (hecho de plataforma verificado); eso lo resuelve el `deleteIntent` de la decisión 4.
- **Vida:** se inicia cuando la sesión pasa a En curso y se detiene cuando deja de estarlo. No ejecuta temporizadores y no lleva lógica de negocio. Tipo `specialUse`, con el subtipo «Cronómetro de la sesión de estudio con controles para pausar y detener». `mediaPlayback` se descarta porque exige reproducir medios.
- **Desde dónde puede iniciarse** (todas rutas documentadas como exentas o con la app visible):
  1. Al iniciar la sesión, con la app en primer plano.
  2. Al tocar un botón de la notificación (por ejemplo «Reanudar»).
  3. Desde los receptores de `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED`.
- **Lo que no puede iniciarlo:** una alarma de hito. Cuando el hito cambia la fase Pomodoro, solo actualiza la notificación con `notify`; si el servicio no está vivo, intenta iniciarlo y, si el sistema lo rechaza (`ForegroundServiceStartNotAllowedException`), publica la notificación sin primer plano.
- **API:** el proyecto usa una versión de AndroidX Core anterior a la 1.10 (ya falló otra API de esa versión en la sesión 15 de la bitácora), así que no se usa `ServiceCompat`. Se bifurca a mano: API 34 o posterior, `startForeground(id, notificación, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`; API 29 a 33, `startForeground` con el tipo 0; API 28 o anterior, `startForeground` de dos argumentos.
- **Reinicio:** `START_NOT_STICKY`. Si el sistema lo reinicia sin sesión En curso, se detiene de inmediato.
- **Alternativa descartada:** solo `setOngoing(true)` más `deleteIntent`, sin servicio. El Product Owner eligió el servicio el 2026-11-20 («Sí, aprobar ambas»; bitácora, fila 143). Queda como plan B si Google Play rechaza `specialUse`.

### 4. La notificación
- **Quién la construye:** `NotificadorSesion.refrescar(sesionId)` lee `ConsultarEjecucionSesion` y construye la notificación; no hay un modelo de dominio nuevo.
- **Cuándo se refresca:** el decorador `ProgramadorAvisosConNotificacion` (en `adapter.out.system`, implementa `ProgramadorAvisosSesion`) delega en el programador real y después llama a `refrescar`. Así no cambia ningún servicio ni sus constructores. `refrescar` relee el estado guardado: si la sesión ya no está En curso, retira la notificación y detiene el servicio.
- **Estados:**

| Estado | Tiempo | Botones | Título |
|---|---|---|---|
| Corriendo, libre | cronómetro del sistema desde el inicio efectivo | Pausar, Detener, No molestar | «Sesión en curso» (con la materia) |
| Corriendo, Pomodoro | cuenta atrás del sistema hasta el fin de la fase | Pausar, Detener, No molestar | «Trabajo · Ciclo 2/4» |
| Pausa | cronómetro apagado, texto con el tiempo acumulado | Reanudar, Detener, No molestar | «En pausa» |
| Salida sin resolver | cronómetro apagado, texto con el tiempo hasta la salida | Pausar, Detener, No molestar | «Tu sesión sigue en curso — volver» |

  El tercer botón muestra el estado de No molestar («activado» o «apagado»), llama a `AlternarNoMolestarSesion` y, si la
  app no tiene acceso, abre la pantalla del sistema donde se concede. La notificación lleva el logo de la app como icono
  grande (el icono adaptativo dibujado en un bitmap, porque `BitmapFactory` no lee los XML adaptativos) y muestra los tres
  botones también en la vista compacta, que es la que se ve en la pantalla de bloqueo.

- **Canal y pantalla de bloqueo:** la notificación va en `canal_sesion_activa_v2`, de importancia normal pero sin sonido,
  vibración ni luz, con visibilidad pública. El canal original era de importancia baja («silenciosa») y los teléfonos
  Xiaomi con HyperOS ocultan esas notificaciones en la pantalla de bloqueo (se comprobó con `dumpsys` y una captura en el
  teléfono del Product Owner: el canal figuraba con `importance=2` y la notificación no aparecía). Como la importancia de un
  canal ya creado no se puede subir por código, se estrena un id nuevo y se elimina el anterior.
- **Bloqueo o salida con retraso:** `ContadorActividades` espera 1,2 s tras detenerse la última actividad y entonces decide:
  con la pantalla apagada registra el bloqueo; con la pantalla encendida, la salida. En ese mismo teléfono el aviso de
  «volver» apareció tras bloquear con el botón de encendido, es decir, la pantalla se apagó después de detenerse la actividad
  y el bloqueo se tomó por una salida.
- **Aviso de salida:** además de cambiar de estado, `ContadorActividades` sigue llamando a `NotificadorSesion.mostrarAvisoRetorno`, que pasa de `CANAL_SESION_ACTIVA` (importancia baja) a `CANAL_AVISOS_ESTUDIO` (importancia alta), que ya existe. Es el que suena y vibra una vez.
- **Descartada con el dedo:** `setDeleteIntent` hacia un receptor que vuelve a publicar la notificación mientras la sesión siga En curso. No se puede simular el deslizamiento en una prueba (el sistema no deja a la app cancelar la notificación de un servicio en primer plano), así que se comprueba solo que el receptor responde; el deslizamiento real queda para la verificación manual de 7.2.
- **Plantilla y tarjeta de la pantalla de bloqueo:** `androidx.media.app.NotificationCompat.MediaStyle` con una `MediaSession` (`SesionMultimedia`) que existe solo mientras la sesión está En curso. Esa sesión es lo que hace que Android (y HyperOS en el teléfono del Product Owner, comprobado con una captura de YouTube Music) dibuje la tarjeta de control en la pantalla de bloqueo, con el logo como carátula, la materia como título, la fase o el estado como subtítulo, los tres botones y la barra de progreso. Con Pomodoro la barra avanza durante la fase (duración desde la configuración de la sesión); con la sesión libre no hay duración y solo se ve el tiempo. Los eventos de los botones físicos de los audífonos se ignoran. Dependencia `androidx.media:media`, aprobada por el Product Owner; versión 1.7.1.
- **Acciones:** `PendingIntent.getBroadcast` con `FLAG_IMMUTABLE`, hacia `ReceptorAvisosSesion` (no exportado), con las acciones `ACCION_PAUSAR`, `ACCION_REANUDAR` y `ACCION_DETENER` y el `sesionId` como extra. El receptor ignora una acción cuyo `sesionId` no sea el de la sesión En curso actual, de modo que una acción tardía de una sesión que ya terminó no hace nada (como los `PendingIntent` usan `FLAG_UPDATE_CURRENT`, solo hay un botón por acción y su `sesionId` es siempre el de la sesión actual), y usa `goAsync()` con el `ejecutor`. Reanudar usa el tópico `SIN_CLASIFICAR`; Detener usa `ejecutarConMetas` y, si hay metas, publica el aviso de felicitación.
- **Privacidad:**

| Bloqueo de la app (HU-04) | Android 12 o posterior | Android 11 o anterior |
|---|---|---|
| Desactivado | materia visible, botones sin pedir desbloqueo | igual |
| Activado | sin la materia; los botones piden desbloquear el teléfono (`setAuthenticationRequired(true)`) | sin la materia; botones sin pedir desbloqueo |

  Con el bloqueo de la app activado, la versión pública (`publicVersion`) muestra «Sesión en curso» y el tiempo. Cambiar el bloqueo de la app en Ajustes con una sesión En curso refresca la notificación.
- **Alternativa descartada:** una notificación estándar con tres acciones y sin `MediaStyle`. Se pierde el aspecto de reproductor que pidió el Product Owner; queda como plan B si `androidx.media` no aporta nada visible en las capturas (tarea 6.2).

### 5. El candado de bloqueo dentro del cronómetro
- **Dónde:** botón de icono `botonBloquear` (candado, 48 dp) dentro del anillo `anilloProgresoEnCurso`, debajo del tiempo y del estado.
- **Flujo:** toque → diálogo de confirmación (salvo «No volver a preguntar») → si la app no es administradora, explicación y pantalla del sistema (`ACTION_ADD_DEVICE_ADMIN`, con `ActivityResultLauncher`) → `lockNow()`. Si se rechaza el permiso, no se bloquea.
- **Piezas:** `BloqueoPantallaSistema` (`isAdminActive`, `lockNow`, `removeActiveAdmin`) y `ReceptorAdministradorBloqueo` con `res/xml/politicas_bloqueo.xml` que declara solo `<force-lock/>`, ambos en `adapter.out.system`; `PreferenciasBloqueoPantalla` guarda «No volver a preguntar» (preferencia de interfaz, igual que `MarcadorSalidaAppPrefs`). Ajustes ofrece «Quitar permiso de bloqueo».
- **Alternativas descartadas:** el servicio de accesibilidad (`GLOBAL_ACTION_LOCK_SCREEN`), por la política de Google Play; fijar la pantalla (`startLockTask`), que no apaga la pantalla; una pantalla negra dentro de la app, que no es un bloqueo real. El Product Owner pidió el candado con bloqueo real.

### 6. No molestar en la sesión
- **Qué:** `AlternarNoMolestarSesion`. Lee `ResumenEjecucionSesion.noMolestarActivo()` y cambia la preferencia de la sesión a `FORZAR_ACTIVAR` o `FORZAR_DESACTIVAR`; después llama a `ControladorNoMolestar.aplicar(true)` o `restaurarSiAplica()` (RN-18). Sin permiso (`tienePermiso()` es falso), no cambia nada y devuelve un error para que la pantalla lleve a la configuración. El control es un botón conmutable con icono y texto, no solo color.
- **Qué hay que comprobar al implementar:** que `SesionEstudio` permita cambiar la preferencia de una sesión En curso; si no, se añade un método y `Mapeadores` ya la persiste (`forzarNoMolestar`).

### 7. Pantalla de la sesión estilo reproductor
- **Fila inferior:** Detener, Cambiar de materia, Pausar o Reanudar (botón flotante de 64 dp, la única excepción a la altura común, aprobada por el Product Owner) y No molestar, los otros tres de 48 dp con etiqueta. El candado va dentro del anillo (decisión 5).
- **Ids:** se conservan `botonDetenerEnCurso`, `botonCambiarMateria` y `botonPausarReanudar` (que pasa de `MaterialButton` a `FloatingActionButton`); nuevos `botonBloquear` y `botonNoMolestar`.
- **Pruebas de estilo:** `ReglasEstiloLayoutTest` y `LineaGraficaEspressoTest` admiten un `FloatingActionButton` de 64 dp y solo ese.
- **Tablet:** se aplica igual en `layout-w600dp/activity_sesion_en_curso.xml`.

### 8. Regla de AGENTS.md §2
Se aclara «servicios en primer plano permanentes» con «se permite uno solo mientras haya una sesión En curso (HU-49)». La aprobó el Product Owner el 2026-11-20 (bitácora, fila 143).

### 9. Logo de la app
- **Origen:** `docs/documento/recursos/logos/logo-app.jpg` (2048 × 2048, JPEG). Un script aísla el símbolo del fondo por
  proyección de color (blanco y verde azulado sobre el índigo del archivo, con el borde suavizado), de modo que el símbolo
  se recompone sobre el índigo `#4338CA` del token `sp_action` y no sobre el tono ligeramente distinto del JPEG.
- **Dónde se usa cada recurso:**

| Dónde se ve | Recurso | Regla |
|---|---|---|
| Icono del celular (adaptativo) | `drawable-nodpi/ic_launcher_foreground.png` (1024 px) y `drawable/ic_launcher_background.xml` (color `sp_action`) | El anillo mide el 58 % del lienzo de 108 dp (62,6 dp), dentro del círculo de seguridad de 66 dp |
| Icono temático (Android 13) | `drawable-nodpi/ic_launcher_monochrome.png` | Silueta de un solo color, sin fondo |
| Iconos heredados (API 24 y 25) | `mipmap-*/ic_launcher.webp` y `ic_launcher_round.webp` | Cuadrado redondeado y círculo sobre el índigo |
| Notificaciones y pantalla de bloqueo | `drawable-*/ic_notificacion.png` (24 dp en cinco densidades) | Solo blanco sobre transparente; sustituye a los iconos de estado como icono pequeño |
| Pantalla de inicio (Android 12 o posterior) | `drawable/ic_splash.xml` y `values-v31/themes.xml` | Fondo `sp_action`; el símbolo cabe en el círculo de 192 dp |
| Google Play (icono) | `docs/documento/recursos/logos/play-store/icono-512.png` | 512 × 512, PNG de 32 bits con alfa, cuadrado y a sangre completa |
| Google Play (gráfico de función) | `docs/documento/recursos/logos/play-store/grafico-funcion-1024x500.png` | 1024 × 500, 24 bits, sin transparencia; índigo, símbolo y «StudyPlan» en Plus Jakarta Sans |

- **Splash:** el mapa de bits se declara sin envoltorio. Con un `layer-list` de tamaño fijo el sistema ampliaba el símbolo
  unas 1,85 veces y el círculo de 192 dp recortaba el anillo (medido en el emulador).
- **Tema:** se redefine solo `Theme.StudyPlan` en `values-v31/` (está vacío en `values/`), porque un estilo con el mismo
  nombre en un directorio calificado reemplaza al del directorio por defecto y no se combina.
- **Alternativa descartada:** redibujar el logo como vector. Es más nítido a cualquier tamaño, pero reproduce a mano un
  diseño que ya existe; se deja como mejora si el Product Owner entrega el logo en SVG.

## Risks / Trade-offs

- **Google Play puede rechazar el uso de `specialUse` o del administrador del dispositivo.** Mitigación: explicaciones visibles antes de cada permiso, política mínima `force-lock`, y plan B (decisión 3 y decisión 4) sin tocar el núcleo. Quitar el candado exigiría publicar otra versión.
- **Un administrador del dispositivo activo impide desinstalar la app directamente.** Mitigación: «Quitar permiso de bloqueo» en Ajustes y el texto de explicación lo dicen.
- **Estudio contado de más:** al no detectar «desbloqueó y abrió otra app» ni limitar las horas con el teléfono bloqueado, una sesión Libre puede acumular tiempo sin estudiar. Es decisión consciente del Product Owner. La sesión sigue venciendo por la regla RN-29 y el estudiante puede corregir con el ajuste manual (HU-39).
- **Notificaciones denegadas (Android 13 o posterior):** la sesión funciona sin los controles y la pantalla lo avisa.
- **Fabricantes que matan servicios:** el tiempo no depende del proceso (RN-22); solo se pierde la rapidez de los botones de la notificación.
- **`isInteractive()` y el orden de `onStop`:** verificado solo en el emulador (decisión 2).
- **Acción tardía sobre una sesión ya terminada:** la validación de `sesionId` del receptor la ignora y reconcilia la notificación (decisión 4).
- **`MediaSession` sin audio:** Android la trata como la de un reproductor: otras apps de control de medios la pueden listar y, si el estudiante oye música, la pantalla de bloqueo muestra una tarjeta por cada sesión. Se mitiga ignorando los botones de los audífonos y liberando la sesión al terminar el estudio. Si Google Play considerara indebido publicar una sesión multimedia sin audio, el plan B es la notificación normal con los mismos tres botones.
- **Consumo de batería (RNF-03):** un servicio en primer plano sin temporizadores. Se mide en la tarea 7.2 con `dumpsys batterystats` en el emulador durante una sesión de 30 minutos con la pantalla apagada.

## Migration Plan

- Sin migración de Room: los tipos nuevos se guardan por nombre.
- Si hay una sesión En curso al actualizar la app, `MY_PACKAGE_REPLACED` inicia el servicio y la notificación aparece sola.
- **Para revertir:** quitar el servicio y los receptores del manifiesto. Conservar los dos valores del enum en `TipoEventoEstudio`: un respaldo de Drive o una base con esos eventos fallaría al leerlos si se eliminan.

## Open Questions

- La declaración del servicio `specialUse` y del administrador del dispositivo para la consola de Google Play se redacta al preparar la publicación; no cambia lo que se construye.
- La versión exacta de `androidx.media:media` se confirma en Maven al hacer la tarea 1.2. La elección de la versión queda sujeta a la aprobación del Product Owner.
