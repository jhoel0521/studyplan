# Épica E5: Ejecución de Sesiones en Vivo, Pomodoro, No molestar y Eventos de Estudio

*   **Author:** jhoel0521
*   **Status:** Draft
*   **Date:** 2026-11-16

--------------------------------------------------------------------------------

## Overview

Este documento especifica la **Épica E5 (`tecnicas-estudio`)** de StudyPlan: la ejecución en vivo de sesiones de estudio mediante cronómetro en modo **Libre** o técnica **Pomodoro** configurable, la gestión opcional del modo **No molestar**, el registro persistente de eventos de estudio con marcas de tiempo, los avisos ante salidas de la aplicación o límites horarios, la declaración de estudio con la aplicación cerrada y el reparto de tramos continuos entre sesiones consecutivas.

El diseño cubre las **9 historias de usuario** definidas en [`./openspec/specs/tecnicas-estudio/spec.md`](./openspec/specs/tecnicas-estudio/spec.md) (**37 puntos**: `HU-22`, `HU-23`, `HU-24`, `HU-39`, `HU-40`, `HU-41`, `HU-42`, `HU-43`, `HU-44`), la máquina de estados [`./openspec/specs/sesiones-estudio/spec.md`](./openspec/specs/sesiones-estudio/spec.md) (`RN-07`), las reglas de negocio **`RN-11`, `RN-17`, `RN-18` y `RN-22` a `RN-29`**, y los criterios de calidad **`RNF-02`, `RNF-03`, `RNF-09`, `RNF-10`, `RNF-15`, `RNF-16`, `RNF-17` y `RNF-18`** del capítulo de requisitos [`./docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md`](./docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md), bajo las restricciones arquitectónicas de [`./AGENTS.md`](./AGENTS.md).

De acuerdo con `RN-22` y `RNF-03`, el sistema no ejecuta servicios en primer plano permanentes contando segundos. Toda duración real, pausa, salida, regreso, tiempo extra y fase Pomodoro se calcula a partir de eventos persistidos en almacenamiento local antes de actualizar la interfaz, utilizando el puerto de reloj [`Reloj`](./app/src/main/java/io/github/jhoel0521/studyplan/application/port/out/Reloj.java) para aislar el núcleo en Java puro.

### Glosario de reglas, componentes base y prototipos referenciados

| Término / Código | Significado en el proyecto | Fuente en el repositorio |
|---|---|---|
| `RN-07` | Máquina de estados de la sesión (`Pendiente`, `En curso`, `No realizada`, `Completada`, `Completada tardía`, `Reprogramada`). | [`./openspec/specs/sesiones-estudio/spec.md`](./openspec/specs/sesiones-estudio/spec.md) |
| `RNF-03` | Criterio de consumo moderado de batería y almacenamiento: prohíbe procesos contando segundo a segundo en segundo plano y exige alarmas/trabajos puntuales. | [`./docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md`](./docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md) |
| `E3` / `E5` | Épica 3 (`sesiones-estudio`: planificación y cierre manual, ya archivada) y Épica 5 (`tecnicas-estudio`: ejecución en vivo y eventos). | [`./docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md`](./docs/documento/capitulos/03-requisitos-y-analisis-del-sistema.md) |
| `Reloj` | Puerto de salida del núcleo que abstrae fecha y hora actual para mantener pruebas deterministas en Java puro. | [`Reloj.java`](./app/src/main/java/io/github/jhoel0521/studyplan/application/port/out/Reloj.java) |
| `ContadorActividades` | Observador del ciclo de vida de Activities que detecta cuándo toda la aplicación pasa a segundo plano o regresa a primer plano. | [`ContadorActividades.java`](./app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ContadorActividades.java) |
| `ControlarSesionBloqueo` / `bloqueoActivo` | Caso de uso y ajuste de seguridad (`HU-04`) que bloquea el acceso al volver a primer plano cuando la autenticación biométrica o por PIN está activa. | [`ActividadProtegida.java`](./app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ActividadProtegida.java) |
| `VencerSesionesPendientes` | Caso de uso síncrono ejecutado al abrir o consultar pantallas para pasar sesiones `Pendiente` vencidas a `No realizada` (`RN-08`), respetando `En curso` (`RN-29`). | [`VencerSesionesPendientesService.java`](./app/src/main/java/io/github/jhoel0521/studyplan/application/service/VencerSesionesPendientesService.java) |
| `W7` (`Sesión en curso`) | Prototipo de la pantalla de cronómetro / Pomodoro con botones Pausar, Detener e interruptor por sesión de No molestar. | [`./docs/build/diagramas.py`](./docs/build/diagramas.py) |
| `W10` (`Ajustes`) | Prototipo de la pantalla de configuración global (incluye No molestar global y tiempos Pomodoro `25 / 5 / 15 · 4`). | [`./docs/build/diagramas.py`](./docs/build/diagramas.py) |
| `W13` (`Al volver a la app`) | Prototipo del diálogo que pregunta qué ocurrió tras salir de la aplicación con una sesión en curso. | [`./docs/build/diagramas.py`](./docs/build/diagramas.py) |
| `W14` (`Detener y repartir`) | Prototipo de la pantalla para dividir un tramo continuo entre la sesión en curso y sesiones pendientes consecutivas. | [`./docs/build/diagramas.py`](./docs/build/diagramas.py) |
| `W15` (`Detalle y eventos`) | Prototipo de la vista de detalle de sesión que muestra la línea de tiempo cronológica de eventos registrados. | [`./docs/build/diagramas.py`](./docs/build/diagramas.py) |

## The Problem

Al contrastar la implementación actual de `app/` con [`./openspec/specs/tecnicas-estudio/spec.md`](./openspec/specs/tecnicas-estudio/spec.md), el flujo de ejecución real de sesiones presenta cinco brechas estructurales:

1. **Cronómetro libre y Pomodoro sin soporte en dominio, configuración ni interfaz (`HU-22`, `HU-23`, `RN-07`, `RN-11`, `RN-17`, `RN-26`):** Aunque [`EstadoPendiente.java`](./app/src/main/java/io/github/jhoel0521/studyplan/domain/EstadoPendiente.java) y [`EstadoEnCurso.java`](./app/src/main/java/io/github/jhoel0521/studyplan/domain/EstadoEnCurso.java) ya declaran las transiciones de inicio, pausa y cierre, [`SesionEstudio.java`](./app/src/main/java/io/github/jhoel0521/studyplan/domain/SesionEstudio.java) no expone operaciones para iniciar o detener una sesión en vivo. Además, `EstadoEnCurso` rechaza cierres con `0` minutos acumulados (bloqueando detenciones o salidas en el primer minuto), [`Ajustes.java`](./app/src/main/java/io/github/jhoel0521/studyplan/domain/Ajustes.java) carece de configuración Pomodoro (`25/5/15` cada `4`) y no existe la pantalla de sesión en curso (`W7`).
2. **Inexistencia de registro persistente de eventos de estudio y línea de tiempo (`HU-39`, `RN-22`, `RNF-10`):** El almacenamiento local actual no modela eventos de estudio con marcas de tiempo completas y [`SesionDetalleActivity.java`](./app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/SesionDetalleActivity.java) no muestra ninguna línea de tiempo (`W15`), impidiendo auditar interrupciones o alimentar en el futuro las métricas de E7 (`HU-45`).
3. **Ausencia de detección de salidas, recuperación tras cierre forzado y cierre por vencimiento (`HU-40`, `HU-41`, `RN-23`, `RN-24`, `RN-29`):** [`ContadorActividades.java`](./app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ContadorActividades.java) mantiene únicamente una bandera en memoria para el bloqueo biométrico (`ControlarSesionBloqueo`). Si existe una sesión en curso y el estudiante cambia de aplicación o el sistema finaliza el proceso, no se persiste el evento de salida, no se envía notificación de retorno, no se presenta el diálogo «Al volver a la app» (`W13`) tras superar la autenticación ni se cierran en su última marca conocida las sesiones abandonadas por más de `3` días (`RN-29`).
4. **Falta de avisos de frontera horaria y reparto de tramo continuo (`HU-42`, `HU-43`, `HU-44`, `RN-25`, `RN-27`, `RN-28`):** No existen avisos programados al llegar al fin planificado (`«¿Sigues estudiando?»`) ni al alcanzar el inicio de la siguiente sesión `Pendiente` del día, ni existe el flujo para repartir el tiempo continuo entre sesiones consecutivas (`W14`).
5. **Ausencia de control del modo No molestar (`HU-24`, `RN-18`, `RNF-18`):** Ni la configuración global ni la sesión individual almacenan preferencias de No molestar, ni existe un adaptador de sistema que guarde el filtro previo del dispositivo antes de activarlo y lo restaure al pausar, detener o cerrar por vencimiento.

## Goals

1. **Ejecución en vivo Libre y Pomodoro (`HU-22`, `HU-23`, `RN-07`, `RN-11`, `RN-17`, `RN-26`):** Permitir iniciar cualquier sesión `Pendiente` de hoy en modo `Libre` o `Pomodoro` configurable (`25 / 5 / 15` cada `4` ciclos por defecto, editable en `Ajustes` sin alterar sesiones ya iniciadas), derivando el tiempo transcurrido y las fases Pomodoro mediante el puerto `Reloj` desde marcas de tiempo persistidas, excluyendo pausas y descansos de las horas reales y admitiendo duraciones de `0` minutos o distintas de lo planificado.
2. **Registro persistente de eventos y línea de tiempo (`HU-39`, `RN-22`, `RNF-10`):** Grabar cada evento relevante en almacenamiento local antes de actualizar la interfaz y presentar la línea de tiempo ordenada en el detalle de la sesión (`W15`).
3. **Resiliencia ante salidas, cierres del proceso y abandono de 3 días (`HU-40`, `HU-41`, `RN-23`, `RN-24`, `RN-29`):** Detectar salidas de la app con emisión de notificación para volver, mostrar el diálogo de recuperación (`W13`) al regresar (siempre después de validar `ActividadProtegida`), tolerar el cierre abrupto desde Recientes apoyándose en la última marca persistida y cerrar automáticamente las sesiones en curso que superen los `3` días sin actividad.
4. **Avisos de fin planificado, invasión de sesión y reparto (`HU-42`, `HU-43`, `HU-44`, `RN-25`, `RN-27`, `RN-28`):** Disparar avisos puntuales al alcanzar la hora de fin planificada y la hora de inicio de otra sesión `Pendiente`, permitir el cambio inmediato entre sesiones sin detener el estudio y ofrecer el reparto proporcional de los intervalos efectivos trabajados al detener (`W14`).
5. **Control y restauración segura de No molestar (`HU-24`, `RN-18`, `RNF-18`):** Configurar globalmente y sobrescribir por sesión el modo No molestar, solicitando permisos del sistema con explicación previa y garantizando la restauración del filtro previo del dispositivo sin fallos si los permisos se deniegan o revocan.

## Non-Goals

*   **Cálculo y visualización de métricas de concentración, puntualidad e interrupciones (`HU-45`, `RN-30`):** Corresponde a la Épica E7 (`estadisticas`), que leerá los eventos persistidos por E5.
*   **Recordatorios programados previos al inicio de sesiones planificadas (`HU-34`):** Corresponden a la Épica E9 (`recordatorios`).
*   **Servicios en primer plano permanentes (`ForegroundService`):** Prohibido por [`./AGENTS.md`](./AGENTS.md) §2, `RN-22` y `RNF-03`.
*   **Cambios en la lógica de planificación, edición o registro tardío ya entregada en E3 y E4:** Se mantienen intactos salvo la exposición de las nuevas transiciones y detalles en vivo.

## Resolved Design Decisions

Todas las incógnitas planteadas al inicio de la especificación quedan cerradas con las siguientes reglas:

1. **Invariante de sesión única en curso:** Solo puede existir **una sesión `En curso` a la vez** en el dispositivo. Si existe una sesión `En curso` (incluso de un día anterior dentro del plazo de `3` días de `RN-29`) y el estudiante intenta iniciar otra sesión `Pendiente`, el sistema intercepta el inicio y abre el diálogo «Al volver a la app» (`W13`) sobre la sesión activa para cerrarla primero (o continuar con ella), evitando estados ambiguos en `HU-40`, `HU-43` y `RN-18`.
2. **Régimen Pomodoro dentro de la app frente a estudio autodeclarado fuera de la app (`RN-11` y `RN-24`):** En modo `Pomodoro`, las fases de trabajo y descanso se derivan de manera determinista a partir del tiempo activo acumulado dentro de la app y del *snapshot* de configuración tomado al iniciar (`RN-17`), contabilizando solo los tramos de trabajo. Al salir de la app, el conteo de fases Pomodoro se suspende; cualquier tramo autodeclarado al volver como estudio con la app cerrada (`RN-24`) suma íntegro al tiempo real efectivo sin simular descansos ficticios fuera de la aplicación.
3. **Semántica de herencia para sesiones existentes y preferencias de No molestar (`RN-18`):** Las sesiones planificadas previamente (o creadas desde el formulario estándar sin pasar por la pantalla en vivo) inician por defecto en técnica `Libre` y con preferencia de No molestar de tres estados en modo **heredar ajuste global** (*heredar*, *forzar activación*, *forzar desactivación*), resolviéndose la migración física en la sección del plan de implementación detallado.

## Critical User Journeys (CUJs)

### CUJ 1: Cronometrar sesión en modo Libre con tolerancia a cierre desde Recientes (`HU-22`, `RN-07`, `RN-22`, `RN-26`)

*   **Given:** Existe una sesión `Pendiente` de hoy planificada de `10:00` a `11:00` y no hay otra sesión `En curso`.
*   **When:** El estudiante abre el detalle de la sesión, pulsa «Iniciar», selecciona modo `Libre` a las `10:00`, cierra por completo la app desde Recientes mientras está en primer plano a las `10:15`, la abre de nuevo a las `10:39` (indicando que siguió estudiando en el diálogo de recuperación) y pulsa «Detener».
*   **Then:** Al iniciar se persiste la marca de inicio y la sesión pasa a `En curso`; al reabrir tras el cierre forzado, el sistema utiliza el marcador de sesión activa o la última marca persistida conocida para mostrar el diálogo de recuperación sin perder el tramo y reconstruye el cronómetro con el puerto `Reloj`; al detener a las `10:39` se persiste la marca de fin, la sesión pasa a `Completada` con `39` minutos reales (`RN-26`) y suma `39` minutos al progreso semanal (`RN-11`). Si el estudiante detiene antes de completar el primer minuto, la sesión cierra válidamente con `0` minutos reales.

### CUJ 2: Ejecutar sesión con técnica Pomodoro configurable y extensión tras el plan (`HU-23`, `RN-11`, `RN-17`, `RN-25`)

*   **Given:** En `Ajustes` la configuración Pomodoro vigente es `25` min de trabajo, `5` min de descanso corto y `15` min de descanso largo cada `4` ciclos; el estudiante inicia una sesión de `120` minutos en modo `Pomodoro` tomando un *snapshot* de esos parámetros (`RN-17`).
*   **When:** Se completan `4` bloques de trabajo (`100` min) alternados con `3` descansos cortos (`15` min), recibiendo aviso sonoro o vibración en cada cambio de fase. A los `115` minutos concluye el cuarto bloque de trabajo; como los `5` minutos restantes hasta los `120` planificados no alcanzan para iniciar otro bloque de `25` minutos, el sistema ofrece «+1 pomodoro» o «tiempo extra libre», y el estudiante elige detener sin añadir extensión.
*   **Then:** La sesión se completa registrando exactamente `100` minutos reales, excluyendo los `15` minutos de descanso (`RN-11`). Si el estudiante hubiera aceptado «+1 pomodoro» o «tiempo extra libre», el tramo posterior queda marcado como tiempo extra (`RN-25`), y si cambia los valores Pomodoro en `Ajustes` mientras esta sesión corre, solo las sesiones nuevas adoptan los valores actualizados (`RN-17`).

### CUJ 3: Consultar la línea de tiempo de eventos en el detalle (`HU-39`, `RN-22`)

*   **Given:** Una sesión pasó a `Completada` tras registrar inicio, pausas, reanudaciones, salidas, regresos, estudio con la app cerrada y fin.
*   **When:** El estudiante abre el detalle de esa sesión (`W15`).
*   **Then:** La pantalla presenta, junto a la duración planificada y real, la línea de tiempo ordenada con cada evento, su hora exacta (`HH:mm`), su tipo legible y la nota asociada cuando exista.

### CUJ 4: Salida de la app durante sesión en curso y recuperación posterior o vencimiento (`HU-40`, `HU-41`, `RN-23`, `RN-24`, `RN-29`)

*   **Given:** Una sesión está `En curso` desde las `08:00` y el bloqueo biométrico o por PIN puede estar activo (`HU-04`).
*   **When:** El estudiante sale de la aplicación a las `08:45` para atender una llamada, vuelve a abrirla a las `17:00` del mismo día o al día siguiente (tocando la notificación «Tu sesión sigue en curso — volver» o el icono de la app) y supera primero la pantalla de autenticación si aplica.
*   **Then:** Al salir a las `08:45` se persiste el evento de salida, la sesión continúa `En curso` sin pausarse y se muestra la notificación de retorno (si `POST_NOTIFICATIONS` está concedido). Al volver, aunque haya cambiado de día (`VencerSesionesPendientes` no vence sesiones `En curso` antes de `3` días según `RN-29`), se muestra el diálogo «Saliste a las 08:45, ¿seguiste estudiando?» (`W13`) con tres opciones:
  1. «No, terminé al salir»: completa la sesión usando `08:45` como fin.
  2. «Sí, estudié hasta las [HH:mm]» con nota opcional: registra el tramo `08:45`–`HH:mm` como estudio con la app cerrada y completa la sesión en esa hora.
  3. «Sigo estudiando»: registra el tramo fuera de la app hasta la hora actual como estudio con la app cerrada y mantiene la sesión `En curso`.
  Si transcurren más de `3` días sin regresar ni declarar, el sistema cierra automáticamente la sesión en la última marca registrada dentro de la app (`RN-29`).

### CUJ 5: Continuar con tiempo extra al llegar el fin planificado (`HU-42`, `RN-25`)

*   **Given:** Una sesión planificada de `10:00` a `11:00` está `En curso`.
*   **When:** El reloj alcanza las `11:00`.
*   **Then:** Se registra el hito de fin planificado y se emite el aviso «¿Sigues estudiando?» con las acciones `[Sigo]` y `[Terminé]`. Si el estudiante pulsa `[Sigo]` o no responde (incluso si denegó notificaciones y solo vuelve más tarde a la app), la sesión sigue `En curso` y todo el tiempo efectivo posterior a las `11:00` se marca como tiempo extra (`RN-25`). Si está en modo `Pomodoro` y las fases planificadas ya concluyeron antes de la hora de fin, el aviso de extensión Pomodoro y el de fin planificado no se duplican en pantalla.

### CUJ 6: Decidir al invadir el horario de la siguiente sesión (`HU-43`, `RN-27`)

*   **Given:** Matemáticas (`11:00`–`12:00`) sigue `En curso` en tiempo extra y existe una sesión `Pendiente` de Inglés planificada para hoy de `12:00` a `13:00`.
*   **When:** El reloj alcanza las `12:00`.
*   **Then:** Se presenta el aviso indicando el inicio de Inglés con dos acciones:
  1. «Cambiar a Inglés»: completa Matemáticas en `12:00`, registra el evento de cambio de sesión y transiciona Inglés a `En curso` con inicio en `12:00` sin detener el estudio (`RN-27`).
  2. «Seguir con Matemáticas»: Inglés permanece en estado `Pendiente` y sigue su ciclo normal.

### CUJ 7: Repartir un tramo continuo entre sesiones consecutivas al detener (`HU-44`, `RN-28`)

*   **Given:** Matemáticas (`10:00`–`11:00`) estuvo `En curso` hasta las `12:10` (con posibles pausas o descansos intermedios dentro del intervalo) e Inglés (`11:00`–`12:00`) sigue `Pendiente` en el mismo día.
*   **When:** El estudiante pulsa «Detener» a las `12:10` y en la pantalla de reparto (`W14`) fija la hora de corte a las `11:05` y confirma «Repartir y completar ambas» (o elige «Todo a Matemáticas»).
*   **Then:** Al repartir en `11:05`, el dominio segmenta únicamente los **intervalos efectivos trabajados** (sin convertir pausas ni descansos Pomodoro previos en minutos estudiados) respecto a la hora de corte `11:05`: Matemáticas pasa a `Completada` con los minutos efectivos hasta las `11:05`, e Inglés transiciona vía `EstadoSesion` (`Pendiente` → `En curso` → `Completada`) con los minutos efectivos desde las `11:05` hasta las `12:10`, ejecutando ambos cambios y sus eventos en una única transacción atómica. Si no existen sesiones `Pendiente` consecutivas solapadas con el tramo estudiado, no se muestra la pantalla de reparto.

### CUJ 8: Activar, forzar activación/desactivación y restaurar modo No molestar (`HU-24`, `RN-18`, `RNF-18`)

*   **Given:** El ajuste global de No molestar está desactivado (caso A) o activado (caso B) en `Ajustes`.
*   **When:** En el caso A, el estudiante fuerza activar No molestar para una sesión puntual antes de iniciarla (concediendo el acceso al sistema cuando se solicita con explicación previa). En el caso B, el estudiante fuerza desactivar No molestar solo para esa sesión. En ambos casos, inicia, pausa y detiene la sesión (o la deja vencer por `RN-29`).
*   **Then:** En el caso A, al iniciar se guarda el filtro de interrupción previo del dispositivo y se activa No molestar, restaurándose el filtro guardado al pausar, detener o vencer (`RN-18`); si el usuario denegó el permiso del sistema, la sesión inicia normalmente sin bloquear la aplicación (`RNF-18`). En el caso B, la sesión inicia sin activar No molestar pese a la configuración global, sin modificar el ajuste global para futuras sesiones.

## Proposed Architecture

La arquitectura mantiene estrictamente las tres capas hexagonales de [`./AGENTS.md`](./AGENTS.md):

```text
+-----------------------------------------------------------------------------+
| CAPA 1 · Presentación y disparadores del sistema (adapter.in.ui)            |
|  - SesionEnCursoActivity / ViewModel (cronómetro visual, Pomodoro, W7)      |
|  - RepartoSesionActivity / ViewModel (corte continuo, W14)                  |
|  - DialogoRecuperacionSesion (W13, después de ActividadProtegida)           |
|  - SesionDetalleActivity (botón Iniciar, switch No molestar, eventos W15)   |
|  - AjustesActivity (No molestar global y parámetros Pomodoro, W10)          |
|  - ContadorActividades (detección de salida/regreso)                        |
|  - ReceptorAvisosSesion + NotificadorSesion (alarmas y acciones rápidas)    |
+--------------------------------------+--------------------------------------+
                                       | llama a puertos de entrada
                                       v
+-----------------------------------------------------------------------------+
| CAPA 2 · Núcleo puro en Java sin android.* ni androidx.*                    |
|  application.port.in / application.service                                  |
|   - IniciarSesion · PausarReanudarSesion · DetenerSesion · RepartirTramo    |
|   - RegistrarSalidaApp · ResolverRegresoApp · CambiarASiguienteSesion       |
|   - ConsultarEstadoEjecucion · ConfigurarPomodoroYNoMolestar                |
|   - VencerSesionesPendientes (cierra EN_CURSO > 3 días por RN-29)           |
|  domain                                                                     |
|   - SesionEstudio (transiciones en vivo + snapshot Pomodoro + 3 estados DND)|
|   - EventoEstudio · CalculadoraTiempoReal · CalculadoraPomodoro             |
|   - CalculadorRepartoTramo · Ajustes (con Pomodoro y No molestar global)    |
+--------------------------------------+--------------------------------------+
                                       ^ implementan puertos de salida
                                       |
+-----------------------------------------------------------------------------+
| CAPA 3 · Infraestructura (adapter.out.persistence · adapter.out.system)     |
|  application.port.out                  Adaptadores concretos                |
|   - SesionRepository         <-------  SesionRepositoryRoom (Room v4)       |
|   - EventoEstudioRepository  <-------  EventoEstudioRepositoryRoom (Room v4)|
|   - AjustesRepository        <-------  AjustesRepositoryRoom (Room v4)      |
|   - Reloj                    <-------  RelojSistema (epochMillis/Fecha/Hora)|
|   - ProgramadorAvisosSesion  <-------  ProgramadorAvisosAlarmManager        |
|   - ControladorNoMolestar    <-------  ControladorNoMolestarSistema         |
|   - MarcadorSalidaApp        <-------  MarcadorSalidaAppPrefs               |
+-----------------------------------------------------------------------------+
```

### Flujo de datos y decisiones arquitectónicas clave

1. **Eventos como fuente de verdad en vivo y materialización al cerrar (`RNF-02`, `RNF-10`):** Durante `En curso`, cada interacción del estudiante o hito registrado se persiste de forma síncrona fuera del hilo principal en el repositorio de eventos antes de reflejar el nuevo estado en UI. Al detener, cambiar de sesión, declarar el fin o repartir un tramo, el servicio de aplicación calcula los minutos reales efectivos mediante `CalculadoraTiempoReal` y actualiza la duración materializada de la sesión y los eventos finales dentro de una sola transacción atómica, evitando escanear eventos al abrir Inicio, Calendario o Materias (`RNF-02`).
2. **Derivación determinista de Pomodoro sin escrituras en segundo plano (`RN-11`, `RN-17`, `RN-22`):** Al iniciar en modo `Pomodoro`, se guarda en la sesión un *snapshot* con los cuatro parámetros vigentes de `Ajustes`. La fase actual, el número de ciclo, los segundos restantes del bloque y el tiempo de trabajo efectivo se calculan de forma pura a partir de los intervalos activos en la app y ese *snapshot*, por lo que cerrar la app o apagar la pantalla no pierde el progreso ni genera escrituras periódicas en segundo plano.
3. **Coordinación entre ciclo de vida, bloqueo biométrico y diálogo de recuperación (`HU-04`, `HU-40`, `HU-41`):** `ContadorActividades` persiste de forma ligera la salida a segundo plano mediante el puerto `MarcadorSalidaApp` cuando todas las pantallas dejan de estar visibles sin rotación de por medio, y dispara el registro del evento de salida junto con la notificación de retorno. Al regresar, las pantallas protegidas ejecutan primero la compuerta de `ActividadProtegida`; solo tras confirmarse la autenticación del estudiante (`alEstarListo()`) se consulta si existe una sesión `En curso` con salida pendiente de declarar y se presenta el diálogo `W13`. Si la app fue terminada desde Recientes estando en primer plano antes de ejecutarse el aviso de parada, el caso de uso de recuperación detecta la sesión `En curso` sin evento de salida posterior y toma como marca de salida la última marca temporal persistida conocida de la sesión.
4. **Alarmas críticas tolerantes a Doze y degradación sin permisos (`RNF-03`, `RNF-18`):** Para los avisos temporales mientras existe una sesión `En curso` (cambio de fase Pomodoro, fin planificado e inicio de la siguiente sesión), el adaptador de sistema programa únicamente la siguiente transición crítica mediante `AlarmManager.setAlarmClock()`, que evita el aplazamiento de Doze sin requerir el permiso restringido de alarma exacta de Android 14+. Si el dispositivo se reinicia, el receptor de arranque reprograma el siguiente hito pendiente. Si el estudiante deniega notificaciones, la app omite la notificación en barra sin fallar y reconcilia los hitos vencidos en pantalla al abrirse.
5. **Seguridad y restauración de No molestar (`RN-18`, `RNF-18`):** Antes de modificar el filtro de interrupción del sistema, el adaptador persiste en disco el filtro previo del dispositivo. Cualquier salida del estado activo (pausa, detención, cambio de sesión, declaración de fin al volver o cierre automático tras `3` días) invoca la restauración comprobando previamente que el permiso siga concedido para evitar excepciones si el estudiante lo revocó en ajustes del sistema.

## Alternatives Considered

### Alternative A: Usar un `ForegroundService` con temporizador periódico contando segundo a segundo

*   **Pros:** Permite mostrar un cronómetro continuo en notificación sin depender de alarmas por transición ni reconstrucción por eventos.
*   **Cons:** Viola directamente la regla explícita del Product Owner (`RN-22`), `AGENTS.md` §2 y `RNF-03` sobre no mantener servicios en primer plano permanentes ni procesos contando segundos.
*   **Reason for Rejection:** Rejected by author: la especificación prohíbe explícitamente servicios en primer plano permanentes y exige cálculo basado en marcas de tiempo persistidas.

### Alternative B: Persistir eventos automáticos en SQLite por cada cambio de fase Pomodoro en segundo plano

*   **Pros:** Dejaría cada bloque de trabajo y descanso guardado explícitamente como fila individual en la base de datos.
*   **Cons:** Con la aplicación cerrada o en Doze sin servicio en primer plano, Android no garantiza ejecución exacta para ir escribiendo cambios de fase en segundo plano, y simula actividad que no corresponde a eventos reales del usuario (YAGNI).
*   **Reason for Rejection:** Rejected by author: derivar las fases determinísticamente en dominio a partir del *snapshot* de configuración y las marcas reales de interacción ofrece el mismo resultado exacto sin escrituras fantasma en segundo plano.

### Alternative C: Añadir `androidx.lifecycle:lifecycle-process` (`ProcessLifecycleOwner`) para detectar salida/entrada de la aplicación

*   **Pros:** Provee un observable único de primer/segundo plano de Jetpack sin llevar conteo manual de Activities.
*   **Cons:** Obliga a aprobar e incorporar una nueva dependencia en `gradle/libs.versions.toml`, duplicando la responsabilidad que ya cumple [`ContadorActividades.java`](./app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ContadorActividades.java) para `HU-04`, y tampoco sobrevive por sí sola a un cierre forzado desde Recientes.
*   **Reason for Rejection:** Rejected by author: ampliar `ContadorActividades` con un marcador ligero persistido y reconciliación por marcas de tiempo resuelve el ciclo de vida sin añadir dependencias nuevas.

### Alternative D: Recalcular `duracionRealMinutos` escaneando todos los eventos en cada lectura de Inicio, Calendario y Materias

*   **Pros:** Elimina la necesidad de mantener sincronizado un campo resumen al cerrar la sesión.
*   **Cons:** Obliga a leer y agregar miles de eventos cada vez que se carga la pantalla principal, el calendario mensual o la lista de materias, poniendo en riesgo la latencia de `RNF-02` (≤ 300 ms con 500 sesiones).
*   **Reason for Rejection:** Rejected by author: mantener los eventos como fuente de verdad durante `En curso` y materializar la duración final en la misma transacción atómica de cierre conserva consultas rápidas en lecturas de rango.

## Risks & Mitigations

| Riesgo | Impacto | Mitigación |
|---|---|---|
| **Cierre abrupto desde Recientes (`SIGKILL`) estando en primer plano** | El ciclo de vida puede no invocar el callback de parada, omitiendo el evento de salida y la bandera en memoria. | Persistir un marcador ligero en disco al detectar salida normal y, al abrir la app con una sesión `En curso` sin marca de salida abierta, usar como instante de salida la última marca temporal persistida de la sesión (el último evento o pausa grabados). |
| **Icono de alarma en barra de estado con `setAlarmClock()` y denegación de `POST_NOTIFICATIONS`** | `setAlarmClock()` muestra el icono del sistema mientras hay una alarma activa; si se deniegan notificaciones (`RNF-18`), los avisos en segundo plano no aparecen. | Programar la alarma exclusivamente mientras exista una sesión `En curso` y cancelarla de inmediato al pausar o detener; explicar en la UI para qué sirven los permisos y ejecutar reconciliación automática al volver a primer plano si no hubo notificación. |
| **Revocación del permiso de No molestar con una sesión en curso** | Llamar al controlador del sistema sin permiso vigente lanza `SecurityException`. | Guardar el filtro previo en disco antes de cambiarlo, comprobar el permiso vigente antes de aplicar y restaurar, y degradar en silencio sin abortar la sesión si el permiso fue revocado (`RNF-18`). |
| **Cierre o salida durante el primer minuto (`0` minutos efectivos)** | La validación actual en `EstadoEnCurso` rechaza `duracionRealMinutos <= 0` y dejaría atrapada la sesión sin poder cerrarse. | Permitir `duracionRealMinutos >= 0` en las transiciones de salida de `EstadoEnCurso`, conservando la exigencia `> 0` únicamente para completado manual (`HU-12`) y registro tardío (`HU-11`, `HU-13`). |
| **Cambio de hora manual o zona horaria durante la sesión** | Las diferencias entre horas locales `HH:mm` cruzarían medianoche o darían valores negativos. | Almacenar en los eventos la marca temporal completa en milisegundos epoch UTC junto con la fecha/hora local visible y acotar las diferencias de tramo con límite inferior `0`. |

## Non-Functional Requirements

*   **`RNF-02` (Desempeño ≤ 300 ms):** Todas las operaciones de lectura/escritura de eventos y sesiones se ejecutan en el ejecutor de fondo de `StudyPlanApplication`; el calendario e inicio leen el campo materializado `duracion_real_minutos` sin escanear `evento_estudio`.
*   **`RNF-03` (Batería y recursos):** Sin servicios en primer plano ni hilos de fondo activos con la pantalla apagada; únicamente `AlarmManager.setAlarmClock()` puntual para la transición activa más próxima y actualización visual de cronómetro por `Handler` de UI solo mientras la pantalla está visible.
*   **`RNF-09` (Offline-first):** Todo el flujo funciona 100 % sin conexión.
*   **`RNF-10` (Tolerancia a fallos):** Cada evento se escribe en almacenamiento persistente antes de confirmarse en UI; el tiempo sobrevive a rotaciones, cierre desde Recientes y terminación del proceso.
*   **`RNF-12` (Seguridad):** `SesionEnCursoActivity` y `RepartoSesionActivity` extienden `ActividadProtegida` heredando bloqueo biométrico/PIN y `FLAG_SECURE`; consultas SQL parametrizadas en Room v4 con migración explícita y sin `fallbackToDestructiveMigration`.
*   **`RNF-15` y `RNF-16` (Mantenibilidad y trazabilidad):** Dominio y aplicación 100 % Java puro validado por `ArquitecturaTest`; reglas `RN-11`, `RN-17`, `RN-18`, `RN-22` a `RN-29` probadas con JUnit.
*   **`RNF-17` y `RNF-18` (Compatibilidad y permisos):** Probado en API 24, 30 y 37; `POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY` y `VIBRATE` gestionados con explicación previa y degradación segura cuando se niegan.

--------------------------------------------------------------------------------

## Detailed Implementation Plan

Esta sección fija todas las decisiones técnicas de implementación sin dejar opciones abiertas al implementador, organizadas en **6 fases incrementales** compilables y verificables (`./gradlew assembleDebug testDebugUnitTest lintDebug`).

### Phasing & Checkpoints

*   **Fase 1 — Dominio puro en Java (`domain`):** Modelos `TecnicaEstudio`, `TipoEventoEstudio`, `EventoEstudio`, `ConfiguracionPomodoro`, `PreferenciaNoMolestar`, `FasePomodoro`, `EstadoPomodoroCalculado`, `IntervaloEfectivo`, `CalculadorTiempoReal`, `CalculadorPomodoro`, `CalculadorRepartoTramo`, `ResultadoReparto`, `ResumenEjecucionSesion`, `DiagnosticoRegresoApp`, `CandidataInvasion`, ampliación de `Ajustes`, `ErrorValidacion`, `EstadoEnCurso` (`>= 0`), `DetalleSesion` y `SesionEstudio`. *Checkpoint:* `./gradlew testDebugUnitTest lintDebug` + `ArquitecturaTest` en verde.
*   **Fase 2 — Puertos y servicios de aplicación (`application`):** Ampliación de `Reloj`, `AjustesRepository`, `SesionRepository` y creación de `EventoEstudioRepository`, `ProgramadorAvisosSesion`, `ControladorNoMolestar`, `MarcadorSalidaApp`, los 9 casos de uso de entrada y servicios de E5, ampliación de `ConsultarSesionService` y `VencerSesionesPendientesService`. *Checkpoint:* `./gradlew testDebugUnitTest lintDebug` + `ArquitecturaTest` en verde.
*   **Fase 3 — Persistencia Room v4 (`MIGRATION_3_4`) y adaptadores de sistema (`adapter.out.*`):** Tabla `evento_estudio`, columnas nuevas en `ajustes` y `sesion_estudio`, `StudyPlanDatabase` versión `4` con esquema `4.json`, `Migracion4InstrumentalTest`, implementaciones Room y adaptadores en `adapter.out.system`. *Checkpoint:* `./gradlew assembleDebug testDebugUnitTest lintDebug` y pruebas Room/migración.
*   **Fase 4 — Presentación, avisos y layouts XML (`adapter.in.ui`, `res/`, `AndroidManifest.xml`):** Permisos en manifiesto, canales y `ReceptorAvisosSesion`, ampliación de `ContadorActividades`, `ActividadProtegida` (detección de recuperación tras salida), `AjustesActivity`, `SesionDetalleActivity`, nuevas pantallas `SesionEnCursoActivity` (`W7`) y `RepartoSesionActivity` (`W14`), diálogo `DialogoRecuperacionSesion` (`W13`), textos `es`/`en` y layouts XML accesibles (`AGENTS.md` §6.2). *Checkpoint:* `./gradlew assembleDebug testDebugUnitTest lintDebug`.
*   **Fase 5 — Pruebas instrumentadas E2E y revisión con `revisor-java`:** Suite Espresso de E5, pruebas de receptores/notificaciones con permisos denegados (`RNF-18`), auditoría con `.claude/agents/revisor-java.md` y corrida `connectedDebugAndroidTest` en API 24/30/37. *Checkpoint:* 0 fallos en unit tests, Espresso y lint.
*   **Fase 6 — Diagramas, documentación APA 7 y bitácora:** Actualización de `docs/build/diagramas_clases.py` (incluyendo paquete `adapter/out/system`), `docs/build/cotejo_clases.py`, `docs/documento/capitulos/04-diseno-del-software.md`, `docs/auditoria-ia/bitacora.md` y generación `.docx`.

### Target Files to Create/Modify

#### 1. `app/src/main/java/io/github/jhoel0521/studyplan/domain/` (Fase 1)

1. **`TecnicaEstudio.java` [Create]**: Enum `{ LIBRE, POMODORO }`.
2. **`TipoEventoEstudio.java` [Create]**: Enum `{ INICIO, PAUSA, REANUDACION, SALIDA_APP, REGRESO_APP, FIN_PLANIFICADO, TIEMPO_EXTRA, ESTUDIO_APP_CERRADA, CAMBIO_SESION, AJUSTE_MANUAL, FIN }` con helper booleano `esMarcaDentroDeApp()` (`true` para todos excepto `ESTUDIO_APP_CERRADA`).
3. **`PreferenciaNoMolestar.java` [Create]**: Enum `{ HEREDAR_GLOBAL, FORZAR_ACTIVAR, FORZAR_DESACTIVAR }` con método `boolean resolver(boolean noMolestarGlobal)` y conversión desde/hacia `Boolean` (`null` → `HEREDAR_GLOBAL`, `Boolean.TRUE` → `FORZAR_ACTIVAR`, `Boolean.FALSE` → `FORZAR_DESACTIVAR`).
4. **`ConfiguracionPomodoro.java` [Create]**: Value Object inmutable con campos `trabajoMinutos` (default `25`, rango `1..180`), `descansoCortoMinutos` (default `5`, rango `1..60`), `descansoLargoMinutos` (default `15`, rango `1..90`), `ciclosHastaLargo` (default `4`, rango `1..12`). Fábrica `Resultado<ConfiguracionPomodoro> crear(int trabajo, int descansoCorto, int descansoLargo, int ciclos)` validando rangos con `ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA`, y método `porDefecto()`.
5. **`EventoEstudio.java` [Create]**: Entidad inmutable con campos: `Long id`, `long sesionId`, `TipoEventoEstudio tipo`, `long instanteMilis`, `Fecha fecha`, `Hora hora`, `String nota` (saneada, máx. `500` caracteres). Fábricas `crear(...)` y `restaurar(...)`, más `conId(long id)`.
6. **`ErrorValidacion.java` [Modify]**: Añadir códigos:
   *   `CONFIGURACION_POMODORO_INVALIDA`
   *   `SESION_YA_EN_CURSO`
   *   `HORA_DECLARADA_FUERA_DE_RANGO` (para `HU-41`: la hora declarada es anterior a la salida o posterior al momento actual)
   *   `CORTE_REPARTO_INVALIDO` (para `HU-44`: hora de corte fuera del rango del tramo continuo)
   *   `SIN_SESIONES_PARA_REPARTIR`
7. **`Ajustes.java` [Modify]**: Añadir campos inmutables `boolean noMolestarGlobal` (default `false`) y `ConfiguracionPomodoro pomodoro` (default `ConfiguracionPomodoro.porDefecto()`). Conservar constructor previo `Ajustes(Tema, Idioma, boolean)` delegando al nuevo constructor completo para mantener compatibilidad con pruebas previas, añadiendo `conNoMolestarGlobal(boolean)` y `conPomodoro(ConfiguracionPomodoro)`.
8. **`EstadoEnCurso.java` [Modify]**: En `completarConDuracion(int duracionRealMinutos)` cambiar la condición `duracionRealMinutos <= 0` por `duracionRealMinutos < 0` para permitir cierres de `0` minutos efectivos sin error. Conservar `> 0` en `EstadoPendiente.marcarCompletada`, `EstadoNoRealizada.registrarTardio` y `SesionEstudio.registrarPasada`.
9. **`SesionEstudio.java` [Modify]**:
   *   Añadir campos inmutables: `TecnicaEstudio tecnica` (default `LIBRE`), `PreferenciaNoMolestar preferenciaNoMolestar` (default `HEREDAR_GLOBAL`) y `ConfiguracionPomodoro snapshotPomodoro` (`null` mientras no ha iniciado en modo `POMODORO`).
   *   Conservar firmas existentes de `planificar(...)`, `registrarPasada(...)` y `restaurar(...)` sobrecargándolas para aceptar los nuevos campos con valores por defecto seguros (`LIBRE`, `HEREDAR_GLOBAL`, `null`).
   *   Añadir métodos de transición delegando en `EstadoSesion` (`RN-07`):
     *   `Resultado<SesionEstudio> iniciar(Fecha hoy, TecnicaEstudio tecnica, PreferenciaNoMolestar prefNoMolestar, ConfiguracionPomodoro pomodoroVigente)` (exige `fecha.equals(hoy)` —si no, devuelve `TRANSICION_ESTADO_NO_PERMITIDA`— y guarda `snapshotPomodoro` si `tecnica == POMODORO`).
     *   `Resultado<SesionEstudio> pausarOReanudar()`
     *   `Resultado<SesionEstudio> detener(int duracionRealMinutos)`
     *   `Resultado<SesionEstudio> cambiarASiguiente(int duracionRealMinutos)`
     *   `Resultado<SesionEstudio> declararFinAlVolver(int duracionRealMinutos)`
     *   `Resultado<SesionEstudio> cerrarPorPlazoVencido(int duracionRealMinutos)`
     *   `Resultado<SesionEstudio> conPreferenciaNoMolestar(PreferenciaNoMolestar pref)` (solo en `PENDIENTE` o antes de iniciar).
   *   Helper `debeActivarNoMolestar(boolean noMolestarGlobal)`: delega en `preferenciaNoMolestar.resolver(noMolestarGlobal)`.
10. **`IntervaloEfectivo.java` y `CalculadorTiempoReal.java` [Create]**:
    *   `IntervaloEfectivo`: Value Object inmutable con `long inicioMilis`, `long finMilis` y `boolean dentroDeApp` (`true` para tramos dentro de la app, `false` para `ESTUDIO_APP_CERRADA`).
    *   `CalculadorTiempoReal`: clase de utilidad pura en `domain` que reconstruye intervalos y tiempos a partir de `List<EventoEstudio>` ordenados por `instanteMilis` y `long ahoraMilis`:
        *   `List<IntervaloEfectivo> construirIntervalosActivos(List<EventoEstudio> eventos, long ahoraMilis)`: empareja `(INICIO|REANUDACION|REGRESO_APP)` con su cierre `(PAUSA|SALIDA_APP|FIN|CAMBIO_SESION|ESTUDIO_APP_CERRADA)` o con `ahoraMilis` si sigue abierto dentro de la app; también distingue intervalos autodeclarados con la app cerrada (`ESTUDIO_APP_CERRADA`: `tSalida` hasta `tFinDeclarado`, con `dentroDeApp = false`).
        *   `int calcularMinutosReales(SesionEstudio sesion, List<EventoEstudio> eventos, long finMilis)`: para `LIBRE` suma todos los intervalos dentro de la app más los tramos `ESTUDIO_APP_CERRADA` truncando a minutos enteros (`(int) (totalSegundos / 60)`, mínimo `0`); para `POMODORO` aplica `CalculadorPomodoro` a la suma de intervalos dentro de la app (restando descansos) y suma íntegros los tramos `ESTUDIO_APP_CERRADA`.
        *   `boolean estaPausadaActualmente(List<EventoEstudio> eventos)` y `boolean tieneSalidaSinResolver(List<EventoEstudio> eventos)`: determina si el último evento relevante fue `PAUSA` o `SALIDA_APP`.
        *   `long obtenerUltimoInstanteDentroDeApp(List<EventoEstudio> eventos, long fallbackInicio)`: devuelve el `instanteMilis` del último evento donde `tipo.esMarcaDentroDeApp()` para `RN-29` y recuperación ante `SIGKILL`.
11. **`FasePomodoro.java` y `EstadoPomodoroCalculado.java` [Create]**:
    *   `FasePomodoro`: enum `{ TRABAJO, DESCANSO_CORTO, DESCANSO_LARGO, ESPERANDO_EXTENSION, TIEMPO_EXTRA_LIBRE }`.
    *   `EstadoPomodoroCalculado`: `FasePomodoro faseActual`, `int cicloActual`, `int ciclosPlanificadosMaximos`, `long segundosRestantesEnBloque`, `long segundosTrabajoAcumulados`, `long instanteProximoCambioMilis`, `boolean bloqueExtraActivo`.
12. **`CalculadorPomodoro.java` [Create]**: Lógica determinista pura:
    *   Dada `duracionPlanificadaMinutos` y `ConfiguracionPomodoro`, calcula `int pomodorosQueCabenEnPlan`: simula bloques `(trabajo + descanso)` mientras el siguiente bloque `trabajo` quepa íntegramente en la duración planificada restante (`trabajoAcumulado + descansos + trabajo <= duracionPlanificadaMinutos`, mínimo `1` bloque si `duracionPlanificadaMinutos >= trabajo`). Ejemplo: en `120` min con `25/5/15/4`, caben `4` bloques de trabajo (`115` min con `3` descansos cortos de `5` min); los `5` min restantes no alcanzan otro bloque de `25` min.
    *   Dado `long segundosActivosEnApp`, `ConfiguracionPomodoro`, `int pomodorosQueCaben`, `int pomodorosExtraAceptados` (contando eventos `TIEMPO_EXTRA` de `+1 pomodoro`) y `boolean enTiempoExtraLibre`, determina `EstadoPomodoroCalculado` sin depender de un servicio periódico.
13. **`CalculadorRepartoTramo.java` y `ResultadoReparto.java` [Create]**:
    *   `ResultadoReparto`: inmutable con `long sesionActualId`, `int minutosSesionActual`, `long sesionSiguienteId`, `int minutosSesionSiguiente`, `long corteMilis`.
    *   `CalculadorRepartoTramo`: dada la sesión en curso, sus `IntervaloEfectivo`, la sesión `PENDIENTE` consecutiva del mismo día cuyo inicio planificado sea menor que `finReal` (`horaInicioMilis < finRealMilis`) y el `long corteMilis` elegido en `W14`, valida `inicioReal <= corteMilis <= finReal` (`CORTE_REPARTO_INVALIDO`), divide los intervalos efectivos alrededor de `corteMilis` (conservando `minutosSesionActual + minutosSesionSiguiente == totalMinutosEfectivos`) y retorna `Resultado<ResultadoReparto>`.
14. **`DetalleSesion.java` [Modify]**, **`ResumenEjecucionSesion.java` [Create]**, **`DiagnosticoRegresoApp.java` [Create]** y **`CandidataInvasion.java` [Create]**:
    *   `DetalleSesion`: añadir `List<EventoEstudio> eventos` (`Collections.emptyList()` en constructor previo para compatibilidad) y `boolean puedeIniciar` (`PENDIENTE` y `fecha.equals(hoy)`).
    *   `CandidataInvasion`: `long sesionId`, `String nombreMateria`, `Hora horaInicio`, `Hora horaFin`.
    *   `DiagnosticoRegresoApp`: `long sesionId`, `String nombreMateria`, `long instanteSalidaMilis`, `Hora horaSalida`, `boolean recuperadoTrasCierreAbrupto`, `String notaSugerida`.
    *   `ResumenEjecucionSesion`: DTO de lectura para `SesionEnCursoActivity` (`SesionEstudio sesion`, `String nombreMateria`, `int colorMateria`, `boolean pausada`, `boolean noMolestarEfectivo`, `long segundosEfectivosAcumulados`, `long segundosEnAppAcumulados`, `long segundosExtra`, `EstadoPomodoroCalculado estadoPomodoro`, `boolean finPlanificadoAlcanzado`, `boolean requierePreguntaExtension`, `CandidataInvasion sesionInvadida`, `List<CandidataInvasion> candidatasReparto`).

#### 2. `app/src/main/java/io/github/jhoel0521/studyplan/application/` (Fase 2)

1. **Puertos de salida (`application.port.out`):**
   *   `Reloj.java` [Modify]: añadir `long ahoraMilis();`.
   *   `SesionRepository.java` [Modify]: añadir `Optional<SesionEstudio> buscarEnCurso();`, `List<SesionEstudio> listarEnCurso();` y `void completarReparto(SesionEstudio actualCompletada, List<EventoEstudio> eventosActual, SesionEstudio siguienteCompletada, List<EventoEstudio> eventosSiguiente);` (atómica).
   *   `EventoEstudioRepository.java` [Create]: `EventoEstudio guardar(EventoEstudio evento);`, `void guardarTodos(List<EventoEstudio> eventos);`, `List<EventoEstudio> listarPorSesion(long sesionId);` (ordenados `ASC` por `instanteMilis`), `void eliminarPorSesion(long sesionId);`.
   *   `ProgramadorAvisosSesion.java` [Create]: `void programarProximoAviso(long sesionId, long instanteMilis, TipoAvisoSesion tipo, String tituloMateria);`, `void cancelarAvisos();`. (`TipoAvisoSesion`: `{ CAMBIO_FASE_POMODORO, FIN_PLANIFICADO, INVASION_SESION }`).
   *   `ControladorNoMolestar.java` [Create]: `boolean tienePermiso();`, `void aplicarSiCorresponde(boolean activar);`, `void restaurarSiAplica();`.
   *   `MarcadorSalidaApp.java` [Create]: `void marcarSalida(long sesionId, long instanteMilis);`, `Optional<Long> obtenerInstanteSalidaPendiente(long sesionId);`, `void limpiar();`.
2. **Puertos de entrada (`application.port.in`) y servicios (`application.service`):**
   *   `ConfigurarAjustesEstudio.java` + `ConfigurarAjustesEstudioService.java` [Create]: actualiza `noMolestarGlobal` y `ConfiguracionPomodoro` en `AjustesRepository` validando `RN-17`.
   *   `IniciarSesion.java` + `IniciarSesionService.java` [Create]: valida que no exista otra sesión `EN_CURSO` (si existe, devuelve error `SESION_YA_EN_CURSO` con su id para mostrar el diálogo pendiente `W13`), transiciona `PENDIENTE` → `EN_CURSO` con *snapshot* Pomodoro si aplica, persiste evento `INICIO`, aplica `ControladorNoMolestar`, programa el próximo aviso con `ProgramadorAvisosSesion` y retorna `ResultadoSesion`.
   *   `PausarReanudarSesion.java` + `PausarReanudarSesionService.java` [Create]: alterna pausa/reanudación en `EN_CURSO`, persiste `PAUSA` o `REANUDACION`, restaura o reaplica `ControladorNoMolestar` y cancela/reprograma el próximo aviso.
   *   `DetenerSesion.java` + `DetenerSesionService.java` [Create]: calcula duración real efectiva (`LIBRE` o `POMODORO`), consulta si hay sesiones `PENDIENTE` consecutivas del día elegibles para reparto (`HU-44`); si se detiene directamente sin repartir, persiste `FIN`, actualiza sesión a `COMPLETADA`, restaura `ControladorNoMolestar`, cancela avisos y limpia `MarcadorSalidaApp`.
   *   `RepartirTramoSesion.java` + `RepartirTramoSesionService.java` [Create]: aplica `CalculadorRepartoTramo`, transiciona ambas sesiones vía `EstadoSesion`, persiste `AJUSTE_MANUAL` + `FIN` de ambas en transacción atómica, restaura `ControladorNoMolestar` y cancela avisos.
   *   `RegistrarSalidaApp.java` + `RegistrarSalidaAppService.java` [Create]: si existe sesión `EN_CURSO` y no está pausada ni tiene `SALIDA_APP` abierta, persiste evento `SALIDA_APP` y actualiza `MarcadorSalidaApp` (`RN-23`).
   *   `EvaluarRecuperacionSesion.java` + `EvaluarRecuperacionSesionService.java` [Create]: al regresar a primer plano tras `ActividadProtegida`, detecta sesión `EN_CURSO`:
       *   Si la antigüedad supera `3` días (`RN-29`), cierra automáticamente en la última marca dentro de la app (`cerrarPorPlazoVencido`), graba `FIN`, restaura No molestar y limpia marcador.
       *   Si `tieneSalidaSinResolver` (por `SALIDA_APP` previo o recuperación de cierre abrupto desde Recientes tomando `obtenerUltimoInstanteDentroDeApp`), persiste `REGRESO_APP` idempotente y devuelve `DiagnosticoRegresoApp` para mostrar `W13`.
   *   `ResolverRegresoApp.java` + `ResolverRegresoAppService.java` [Create]: ejecuta la respuesta elegida en `W13` (`NO_SEGUI` → completa en `tSalida`; `HASTA_HORA` → valida `tSalida <= tDeclarada <= ahora`, graba `ESTUDIO_APP_CERRADA` con nota opcional y `FIN` y completa; `SIGO_ESTUDIANDO` → graba `ESTUDIO_APP_CERRADA` hasta `ahora`, mantiene `EN_CURSO`, reaplica No molestar y reprograma siguiente aviso).
   *   `DecidirFinPlanificadoOInvasion.java` + `DecidirFinPlanificadoOInvasionService.java` [Create]: soporta `[Sigo]` / `[Terminé]` (`HU-42`), `[+1 pomodoro]` / `[Tiempo extra libre]` (`HU-23`), `[Cambiar a siguiente]` / `[Seguir]` (`HU-43`, `RN-27`: en transacción cierra la actual en `tCambio` con `CAMBIO_SESION` + `FIN` e inicia la siguiente `PENDIENTE` con `INICIO`, reevaluando No molestar y alarmas).
   *   `ConsultarEjecucionSesion.java` + `ConsultarEjecucionSesionService.java` [Create]: reconstruye `ResumenEjecucionSesion` en tiempo real desde `Reloj`, eventos y `Ajustes`, registrando `FIN_PLANIFICADO` de forma idempotente si `ahora >= horaFin` y reprogramando el próximo aviso pendiente si corresponde.
   *   `ConsultarSesionService.java` [Modify]: inyecta `EventoEstudioRepository`, adjunta `eventos` y `puedeIniciar = (estado == PENDIENTE && fecha.equals(hoy))`.
   *   `VencerSesionesPendientesService.java` [Modify]: además de vencer sesiones `PENDIENTE` anteriores a hoy (`RN-08`), verifica las sesiones `EN_CURSO` con antigüedad `> 3` días (`RN-29`) y las cierra en su última marca conocida dentro de la app restaurando No molestar.
   *   `BorrarDatosService.java` [Modify]: cuando borra datos del teléfono, invoca `controladorNoMolestar.restaurarSiAplica()`, `programadorAvisos.cancelarAvisos()` y `marcadorSalida.limpiar()`.

#### 3. `app/src/main/java/io/github/jhoel0521/studyplan/adapter/out/` (Fase 3)

1. **`persistence/AjustesEntity.java` [Modify]**: Añadir campos SQL:
   *   `@ColumnInfo(name = "no_molestar_global") public boolean noMolestarGlobal;`
   *   `@ColumnInfo(name = "pomodoro_trabajo_min") public int pomodoroTrabajoMin = 25;`
   *   `@ColumnInfo(name = "pomodoro_descanso_corto_min") public int pomodoroDescansoCortoMin = 5;`
   *   `@ColumnInfo(name = "pomodoro_descanso_largo_min") public int pomodoroDescansoLargoMin = 15;`
   *   `@ColumnInfo(name = "pomodoro_ciclos_hasta_largo") public int pomodoroCiclosHastaLargo = 4;`
2. **`persistence/SesionEntity.java` [Modify]**: Añadir campos SQL:
   *   `@NonNull @ColumnInfo(name = "tecnica") public String tecnica = "LIBRE";`
   *   `@Nullable @ColumnInfo(name = "forzar_no_molestar") public Boolean forzarNoMolestar;` (mapeado a `PreferenciaNoMolestar`)
   *   `@Nullable @ColumnInfo(name = "pomodoro_trabajo_min") public Integer pomodoroTrabajoMin;`
   *   `@Nullable @ColumnInfo(name = "pomodoro_descanso_corto_min") public Integer pomodoroDescansoCortoMin;`
   *   `@Nullable @ColumnInfo(name = "pomodoro_descanso_largo_min") public Integer pomodoroDescansoLargoMin;`
   *   `@Nullable @ColumnInfo(name = "pomodoro_ciclos_hasta_largo") public Integer pomodoroCiclosHastaLargo;`
3. **`persistence/EventoEstudioEntity.java` [Create]**: `@Entity(tableName = "evento_estudio", foreignKeys = @ForeignKey(entity = SesionEntity.class, parentColumns = "id", childColumns = "sesion_id", onDelete = ForeignKey.CASCADE), indices = { @Index("sesion_id"), @Index("instante_milis") })` con `id`, `sesionId`, `tipo` (`String`), `instanteMilis` (`long`), `fecha` (`String`), `hora` (`String`), `nota` (`String` nullable).
4. **`persistence/EventoEstudioDao.java` [Create]**, **`persistence/EventoEstudioRepositoryRoom.java` [Create]**, **`persistence/SesionDao.java` [Modify]**, **`persistence/SesionRepositoryRoom.java` [Modify]**, **`persistence/Mapeadores.java` [Modify]**, **`persistence/RelojSistema.java` [Modify]**, **`persistence/StudyPlanDatabase.java` [Modify]**:
   *   `StudyPlanDatabase`: sube a `version = 4`, registra `EventoEstudioEntity.class` y `Migraciones.MIGRATION_3_4`, exporta `app/schemas/.../4.json`.
   *   `SesionDao`: añadir `buscarEnCurso()`, `listarEnCurso()`, y `@Transaction completarReparto(...)`.
   *   `RelojSistema`: implementar `ahoraMilis()` devolviendo `System.currentTimeMillis()`.
5. **`persistence/Migraciones.java` [Modify]**: Añadir `MIGRATION_3_4` ejecutando:
   *   `ALTER TABLE ajustes ADD COLUMN no_molestar_global INTEGER NOT NULL DEFAULT 0`
   *   `ALTER TABLE ajustes ADD COLUMN pomodoro_trabajo_min INTEGER NOT NULL DEFAULT 25`
   *   `ALTER TABLE ajustes ADD COLUMN pomodoro_descanso_corto_min INTEGER NOT NULL DEFAULT 5`
   *   `ALTER TABLE ajustes ADD COLUMN pomodoro_descanso_largo_min INTEGER NOT NULL DEFAULT 15`
   *   `ALTER TABLE ajustes ADD COLUMN pomodoro_ciclos_hasta_largo INTEGER NOT NULL DEFAULT 4`
   *   `ALTER TABLE sesion_estudio ADD COLUMN tecnica TEXT NOT NULL DEFAULT 'LIBRE'`
   *   `ALTER TABLE sesion_estudio ADD COLUMN forzar_no_molestar INTEGER`
   *   `ALTER TABLE sesion_estudio ADD COLUMN pomodoro_trabajo_min INTEGER`
   *   `ALTER TABLE sesion_estudio ADD COLUMN pomodoro_descanso_corto_min INTEGER`
   *   `ALTER TABLE sesion_estudio ADD COLUMN pomodoro_descanso_largo_min INTEGER`
   *   `ALTER TABLE sesion_estudio ADD COLUMN pomodoro_ciclos_hasta_largo INTEGER`
   *   `CREATE TABLE IF NOT EXISTS evento_estudio (...)` con FK `ON DELETE CASCADE` a `sesion_estudio(id)` e índices `index_evento_estudio_sesion_id` e `index_evento_estudio_instante_milis`.
6. **Paquete `app/src/main/java/io/github/jhoel0521/studyplan/adapter/out/system/` [Create]**:
   *   `ControladorNoMolestarSistema.java`: usa `NotificationManager.isNotificationPolicyAccessGranted()` (minSdk 24), guarda `interruption_filter_previo` en `SharedPreferences("studyplan_no_molestar", MODE_PRIVATE)` antes de aplicar `INTERRUPTION_FILTER_PRIORITY` y restaura el valor guardado (limpiando la preferencia) al pausar/detener.
   *   `ProgramadorAvisosAlarmManager.java`: programa y cancela un `PendingIntent` (`FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`) hacia `ReceptorAvisosSesion` usando `AlarmManager.setAlarmClock(...)`.
   *   `MarcadorSalidaAppPrefs.java`: persiste de forma síncrona mínima (`SharedPreferences`) el par `(sesionId, instanteSalidaMilis)` al detectar salida a segundo plano.

#### 4. `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/`, `res/` y `AndroidManifest.xml` (Fase 4)

1. **`AndroidManifest.xml` [Modify]**:
   *   Añadir `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`, `<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />`, `<uses-permission android:name="android.permission.VIBRATE" />`, `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`.
   *   Registrar `SesionEnCursoActivity` (`exported="false"`), `RepartoSesionActivity` (`exported="false"`) y `ReceptorAvisosSesion` (`exported="false"`, `<intent-filter>` para `BOOT_COMPLETED` / acciones de notificación).
2. **`NotificadorSesion.java` [Create]** y **`ReceptorAvisosSesion.java` [Create]** (en `adapter.in.ui`):
   *   Canales (`studyplan_sesion_activa`, `studyplan_pomodoro_cambio`, `studyplan_avisos_horario`).
   *   Notificación de salida (`HU-40`: «Tu sesión sigue en curso — volver», abre `MainActivity`/`SesionEnCursoActivity`), notificación de fin planificado (`HU-42`: «¿Sigues estudiando?» con acciones `[Sigo]` y `[Terminé]`), notificación de cambio de sesión (`HU-43`: «Cambiar a <Materia>» y «Seguir»), notificación + vibración/sonido de cambio de fase Pomodoro (`HU-23`). Comprueba permiso `POST_NOTIFICATIONS` en API 33+ antes de notificar (`RNF-18`).
3. **`ContadorActividades.java` [Modify]**, **`ActividadProtegida.java` [Modify]**, **`StudyPlanApplication.java` [Modify]**, **`FabricaViewModels.java` [Modify]**:
   *   `ContadorActividades` invoca `alSalirDeApp()` (cuando `iniciadas == 0 && !actividad.isChangingConfigurations()`) y `alVolverDeApp()` en el hilo de fondo de la app, disparando `RegistrarSalidaApp` y `NotificadorSesion`.
   *   `ActividadProtegida` en `alEstarListo()` verifica mediante `EvaluarRecuperacionSesion` si existe un regreso pendiente de declarar (`W13`) y presenta `DialogoRecuperacionSesion` (garantizando que nunca se muestre antes de desbloquear huella/PIN).
4. **`AjustesActivity.java` [Modify]**, **`AjustesViewModel.java` [Modify]**, **`res/layout/activity_ajustes.xml` [Modify]**:
   *   Sección «Técnicas y concentración» (`W10`): interruptor «No molestar en sesiones» (al activarlo sin permiso muestra diálogo explicativo y abre `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`) y campos configurables de Pomodoro (`Trabajo`, `Descanso corto`, `Descanso largo`, `Ciclos hasta descanso largo`, todos cumpliendo `AGENTS.md` §6.2: `minHeight="48dp"`, `maxWidth="400dp"`).
5. **`SesionDetalleActivity.java` [Modify]**, **`SesionDetalleViewModel.java` [Modify]**, **`res/layout/activity_sesion_detalle.xml` [Modify]**, **`res/layout-w600dp/activity_sesion_detalle.xml` [Modify]`**:
   *   Cuando `puedeIniciar` (`PENDIENTE` y `fecha == hoy`), muestra selector `RadioGroup` de técnica (`Libre` / `Pomodoro`), selector de tres opciones para No molestar (`Heredar ajuste global` / `Activar en esta sesión` / `Desactivar en esta sesión`) y botón «Iniciar sesión» (`HU-22`, `HU-23`, `HU-24`).
   *   Cuando la sesión actual está `EN_CURSO`, ofrece el botón «Ir a sesión en curso» hacia `SesionEnCursoActivity`.
   *   Cuando existen eventos registrados, muestra la sección **«Eventos»** (`RecyclerView` con `EventoEstudioAdapter` e `item_evento_estudio.xml`, hora `HH:mm`, tipo y nota, `W15`).
6. **`SesionEnCursoActivity.java` [Create]**, **`SesionEnCursoViewModel.java` [Create]**, **`res/layout/activity_sesion_en_curso.xml` [Create]**, **`res/layout-w600dp/activity_sesion_en_curso.xml` [Create]`**:
   *   Extiende `ActividadProtegida`; muestra materia, rango planificado, indicador de modo `Libre` / `Pomodoro` (`W7`), reloj principal actualizado cada `1000` ms mediante `Handler(Looper.getMainLooper())` exclusivamente entre `onStart()` y `onStop()` (calculado desde `Reloj.ahoraMilis()`, sin escribir ticks en Room), fase Pomodoro y ciclo (`p. ej. Trabajo · 2 de 4`), botones «Pausar» / «Reanudar» y «Detener», interruptor «No molestar» (`forzarNoMolestar`, solicitando `ACCESS_NOTIFICATION_POLICY` con explicación previa si falta permiso), tiempo real acumulado y tarjetas contextuales para extensión Pomodoro (`+1 pomodoro` / `Tiempo extra libre`), fin planificado (`Sigo` / `Terminé`) e invasión de siguiente sesión (`Cambiar a <Materia>` / `Seguir`).
7. **`RepartoSesionActivity.java` [Create]**, **`RepartoSesionViewModel.java` [Create]**, **`res/layout/activity_reparto_sesion.xml` [Create]**, **`res/layout-w600dp/activity_reparto_sesion.xml` [Create]`**:
   *   Implementa `W14` extendiendo `ActividadProtegida`: barra visual proporcional + `Slider` de Material 3 con pasos de `5` minutos acompañado de dos botones accesibles `− 5 min` y `+ 5 min` (`minHeight="48dp"`) sincronizados sobre la hora de corte, mostrando en vivo los minutos resultantes de la sesión actual y la sesión siguiente, más los botones «Repartir y completar ambas» y «Todo a [Materia actual]».
8. **`DialogoRecuperacionSesion.java` [Create]**, **`res/layout/dialogo_recuperacion_sesion.xml` [Create]**, **`EventoEstudioAdapter.java` [Create]**, **`res/layout/item_evento_estudio.xml` [Create]**, **`res/values/strings.xml` y `res/values-en/strings.xml` [Modify]**.

#### 5. Pruebas y Auditoría (Fase 5)

1. **Pruebas unitarias (`app/src/test/java/io/github/jhoel0521/studyplan/`):**
   *   `domain/CalculadorTiempoRealTest.java`: pausas, reanudaciones, cierres de `0` min, multi-día `RN-29`, fallo sin salida previa (`SIGKILL`).
   *   `domain/CalculadorPomodoroTest.java`: `25/5/15/4` en `120` min (`4` bloques = `100` min trabajo / `15` min descanso, restos `< 25` min), descanso largo en ciclo `4`, `+1 pomodoro` y tiempo extra libre (`RN-11`, `RN-17`).
   *   `domain/CalculadorRepartoTramoTest.java`: reparto con pausas previas, hora de corte en extremos (`0` min), corte fuera de rango (`CORTE_REPARTO_INVALIDO`), actualización de `EstadoSesion`.
   *   `domain/AjustesPomodoroTest.java` y `domain/SesionEstudioEnCursoTest.java`: validaciones Pomodoro, tres estados de No molestar, invariantes de transición.
   *   `application/service/EjecucionSesionServiceTest.java`, `RecuperacionSalidaServiceTest.java`, `RepartoTramoServiceTest.java` y `VencerSesionesPendientesServiceTest.java` (ampliado para `RN-29` > 3 días).
   *   `adapter/in/ui/SesionEnCursoViewModelTest.java` y `RepartoSesionViewModelTest.java`.
   *   Actualización de fakes: `FakeReloj.java`, `FakeAjustesRepository.java`, `FakeSesionRepository.java`, más `FakeEventoEstudioRepository.java`, `FakeProgramadorAvisos.java`, `FakeControladorNoMolestar.java`, `FakeMarcadorSalidaApp.java`.
2. **Pruebas instrumentadas (`app/src/androidTest/java/io/github/jhoel0521/studyplan/`):**
   *   `adapter/out/persistence/Migracion4InstrumentalTest.java`: migra `v3 -> v4` con `MigrationTestHelper` validando contra `4.json`, verificando que perfil, ajustes, materias y sesiones previas se conservan (`tecnica = 'LIBRE'`, `forzar_no_molestar = NULL`) y `evento_estudio` existe con cascada al borrar sesión.
   *   `adapter/out/persistence/EventoEstudioRepositoryRoomTest.java`: inserción, consulta ordenada, transacción atómica de reparto.
   *   `adapter/in/ui/TecnicasEstudioEspressoTest.java`: flujo completo Libre (`HU-22`), Pomodoro y configuración en Ajustes (`HU-23`), flujo No molestar con permiso denegado (`HU-24`, `RNF-18`), línea de tiempo de eventos (`HU-39`), recuperación en diálogo `W13` (`HU-41`), reparto de tramo continuo en `W14` (`HU-44`) y rotación sin pérdida de estado (`RNF-10`).

#### 6. Documentación y Diagramas (Fase 6)

*   `AGENTS.md` [Modify]: añadir `adapter.out.system` al listado de paquetes de la sección §6.
*   `docs/build/diagramas_clases.py` [Modify]: añadir bloque `add-tecnicas-estudio` y paquete `"adapter/out/system"` en `CLASES_POR_PAQUETE`, más generación de modelo lógico v4 y secuencia de sesión en curso.
*   `docs/build/cotejo_clases.py` [Modify/Run]: verificar 0 desfases contra código y `4.json`.
*   `docs/documento/capitulos/04-diseno-del-software.md` [Modify]: actualizar modelo lógico, diccionario de datos v4, diagramas de clases/secuencia y diseño.
*   `docs/auditoria-ia/bitacora.md` [Modify]: registrar implementación y verificación de E5.

### Verification Plan

*   **Compilation/Build:** `./gradlew assembleDebug assembleDebugAndroidTest`
*   **Unit Tests & Architecture:** `./gradlew testDebugUnitTest` (incluye `ArquitecturaTest` verificando 0 imports `android.*`/`androidx.*` en `domain` y `application`)
*   **Static Analysis & Linting:** `./gradlew lintDebug` (0 errores, 0 `MissingTranslation`, 0 `HardcodedText`)
*   **E2E / Integration:** `./gradlew connectedDebugAndroidTest` sobre emuladores API 24, 30 y 37 (`RNF-17`), verificando `Migracion4InstrumentalTest`, `TecnicasEstudioEspressoTest` y escenarios con permisos denegados (`RNF-18`)
*   **Docs Consistency:** `python docs/build/cotejo_clases.py` y `python .apa/scripts/revisar.py --doc docs/documento`
