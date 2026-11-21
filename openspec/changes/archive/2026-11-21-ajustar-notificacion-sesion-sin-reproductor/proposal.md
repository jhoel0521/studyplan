# Proposal

## Why

La notificación de la sesión (HU-49) se construyó como un reproductor de música: usa una sesión multimedia en estado «reproduciendo». En el teléfono del estudiante eso (1) le quita a YouTube Music el control de los botones y de la prioridad, (2) hace que HyperOS dibuje una tarjeta de reproductor que solo muestra «Pausa», y (3) no es lo que el Product Owner pidió: StudyPlan **no compite con la reproducción multimedia**, solo ofrece una notificación con botones, como la de un mensaje de WhatsApp. Además, salir un instante de la app (por ejemplo al bloquear con el botón de la app y volver por la notificación) registra una salida y pregunta «¿qué hiciste?», y los avisos suenan encima de la música.

## What Changes

- La notificación de la sesión pasa a ser una notificación normal con acciones: logo, cronómetro del sistema (o cuenta atrás Pomodoro), título y fase, y los tres botones «Pausar/Reanudar», «Detener» y «No molestar». Deja de ser un reproductor.
- Se elimina la sesión multimedia, el estilo de reproductor y la dependencia `androidx.media` (aprobada antes por el humano; ahora se retira).
- **Ventana de gracia de 1 minuto** al salir de la app: si el estudiante vuelve antes de que pase 1 minuto, no se registra salida, el cronómetro no se detiene, no suena ningún aviso y no aparece la pregunta «¿Seguiste estudiando?». Pasado 1 minuto fuera, la salida se registra con la hora real en que salió y todo sigue como hasta ahora.
- Los avisos de la app (volver, hitos, metas y recordatorio previo de la sesión) no cortan ni compiten con la música: si hay música sonando, se publican en silencio; el sonido directo duplicado se elimina.
- Se corrige que el canal de notificación anterior se borre en cada refresco: se borra una sola vez.
- Se mantiene `ServicioSesionEnCurso` por ahora; se decidirá si se quita tras probar en el Xiaomi.
- **BREAKING (comportamiento)**: RN-23 cambia: la salida ya no se registra al instante sino pasado 1 minuto; requiere aprobación del Product Owner.

**Fuera de alcance:** quitar `ServicioSesionEnCurso`; cambiar el valor de 1 minuto a configurable; tocar los eventos de bloqueo/desbloqueo (HU-50) salvo lo indicado; nuevos botones; la medición de batería y el ux-check pendientes del change archivado `add-controles-sesion-bloqueo` (tarea 7.2).

## Capabilities

### New Capabilities
<!-- Ninguna -->

### Modified Capabilities
- `tecnicas-estudio`: HU-49 (la notificación ya no es un reproductor; no corta la música; avisos silenciosos con música; canal creado una vez) y HU-40 (ventana de gracia de 1 minuto y salida con la hora real). HU-41 y HU-50 no cambian de requisito: la pregunta de HU-41 solo aparece tras una salida registrada.

## Impact

- Historias: HU-49, HU-40 (y HU-41 por efecto: la pregunta aparece menos); reglas RN-23 y RN-24 (redacción en `docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md`, a aprobar por el Product Owner); relacionadas HU-24, HU-50. RNF afectado: RNF-06 (accesibilidad: el estado no depende solo del color) se mantiene.
- Código: `adapter/in/ui/NotificadorSesion`, `SesionMultimedia` (se borra), `ContadorActividades` y un programador de retardo nuevo, `StudyPlanApplication`; `RegistrarSalidaApp` y su servicio (instante de salida); `app/build.gradle.kts` y `gradle/libs.versions.toml` (se retira `androidx.media`).
- Pruebas: JUnit de `RegistrarSalidaAppService` y `ContadorActividades`; instrumentadas `NotificacionSesionEspressoTest`, `AccionesNotificacionEspressoTest`, `BloqueoPantallaEspressoTest`, `ContadorActividadesInstrumentadoTest` y `Pruebas`.
- Documento: capítulos 3 y 4, diagramas, bitácora de auditoría IA.
