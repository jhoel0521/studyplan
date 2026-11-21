# tecnicas-estudio Specification

## Purpose
Acompaña la ejecución de una sesión con cronómetro, Pomodoro y No molestar, y registra los eventos de estudio (salidas, regresos, tiempo extra, estudio con la app cerrada) para medir la concentración y el respeto del tiempo.

## Requirements

### Requirement: HU-22 Cronometrar sesión en curso
Como estudiante, quiero iniciar, pausar y detener la sesión y ver el tiempo transcurrido mientras estoy en la app para registrar el tiempo real estudiado. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Cronometrar
- **GIVEN** que existe una sesión Pendiente de hoy y no hay otra sesión En curso
- **WHEN** el estudiante pulsa Iniciar y, tras 45 minutos efectivos, Detener
- **THEN** la sesión pasa por En curso a Completada con 45 minutos reales

#### Scenario: Detención en el primer minuto (0 minutos)
- **GIVEN** que la sesión está En curso con menos de 60 segundos acumulados
- **WHEN** el estudiante pulsa Detener (o declara «No, terminé al salir»)
- **THEN** la sesión pasa a Completada con 0 minutos reales sin error de validación

#### Scenario: Sesión única en curso
- **GIVEN** que ya existe una sesión A en estado En curso
- **WHEN** el estudiante intenta iniciar una sesión B en estado Pendiente
- **THEN** el sistema bloquea el inicio simultáneo y presenta el diálogo de recuperación sobre la sesión A para cerrarla o continuar con ella

#### Scenario: App cerrada por completo
- **GIVEN** que la sesión está En curso
- **WHEN** el estudiante cierra la app desde recientes y la abre 20 minutos después
- **THEN** la sesión sigue En curso y el tiempo se calcula desde las marcas de tiempo guardadas (RN-22), tomando la última marca persistida conocida si el proceso fue terminado antes de grabar salida

#### Scenario: Duración distinta a la planificada
- **GIVEN** que la sesión se planificó para 60 minutos
- **WHEN** el estudiante la detiene a los 39 minutos (o a las 3 h 28 min)
- **THEN** se registra la duración real y la diferencia se muestra en el reporte (RN-26)

#### Scenario: Tópico al reanudar pausa manual
- **GIVEN** que el estudiante pausó manualmente una sesión En curso
- **WHEN** pulsa Reanudar
- **THEN** el sistema solicita en un paso el tópico de la pausa (con chips rápidos, detalle opcional en «Otro» y «Sin clasificar» por omisión) antes de reanudar el cronómetro; si la sesión termina desde pausa sin reanudación, ese tramo queda respaldado como «Sin clasificar» en estadísticas

### Requirement: HU-23 Estudiar con técnica Pomodoro configurable
Como estudiante, quiero ejecutar mi sesión en bloques Pomodoro con tiempos configurables para mantener la concentración con descansos programados. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Ciclo
- **GIVEN** que la configuración es 25/5/15 cada 4
- **WHEN** el estudiante inicia la sesión en modo Pomodoro
- **THEN** el sistema deriva las fases de forma determinista desde las marcas de tiempo activas y alterna trabajo y descansos con aviso sonoro o vibración en cada cambio

#### Scenario: Solo trabajo suma
- **GIVEN** que se completaron 4 pomodoros
- **WHEN** el estudiante detiene la sesión
- **THEN** se registran 100 minutos reales, sin contar descansos (RN-11)

#### Scenario: Configurar
- **GIVEN** que el estudiante está en Ajustes > Pomodoro
- **WHEN** cambia el trabajo a 50 minutos mientras otra sesión ya está En curso
- **THEN** la sesión ya iniciada conserva su snapshot original de parámetros y solo las siguientes sesiones usan 50 minutos (RN-17)

#### Scenario: Seguir tras el plan
- **GIVEN** que se completaron los pomodoros que caben en el tiempo planificado
- **WHEN** el estudiante quiere seguir
- **THEN** el sistema ofrece «+1 pomodoro» o «tiempo extra libre» y lo registra como tiempo extra (RN-25)

### Requirement: HU-24 Activar modo No molestar durante la sesión
Como estudiante, quiero que el teléfono silencie notificaciones mientras estudio para evitar distracciones. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Global
- **GIVEN** que No molestar está activado en Ajustes y la app tiene el permiso
- **WHEN** el estudiante inicia una sesión con preferencia «Heredar ajuste global»
- **THEN** el sistema guarda el filtro previo, activa No molestar y lo restaura al pausar, detener, cambiar de sesión o cerrar por vencimiento (RN-18)

#### Scenario: Forzar por sesión
- **GIVEN** que No molestar está desactivado globalmente
- **WHEN** el estudiante selecciona «Activar en esta sesión» antes de iniciar
- **THEN** solo esa sesión activa No molestar sin alterar la configuración global

#### Scenario: Forzar desactivación por sesión
- **GIVEN** que No molestar está activado globalmente
- **WHEN** el estudiante selecciona «Desactivar en esta sesión» antes de iniciar
- **THEN** esa sesión inicia sin activar No molestar y sin modificar la configuración global

#### Scenario: Sin permiso
- **GIVEN** que la app no tiene acceso a No molestar
- **WHEN** el estudiante activa la opción
- **THEN** el sistema explica el permiso y abre la configuración del sistema para concederlo sin bloquear el resto de funciones (RNF-18)

#### Scenario: Permiso revocado durante la sesión
- **GIVEN** que el usuario revocó el permiso de No molestar mientras la sesión estaba En curso
- **WHEN** se pausa, detiene o cierra por vencimiento la sesión
- **THEN** el sistema comprueba el permiso vigente y omite la restauración sin lanzar excepción ni abortar el cierre (RNF-18)

#### Scenario: Alternar desde la sesión en curso
- **GIVEN** que la sesión está En curso con No molestar apagado
- **WHEN** el estudiante toca el control «No molestar» de la pantalla de la sesión
- **THEN** el sistema activa No molestar solo para esa sesión (guardando el filtro previo como en RN-18), el control muestra su estado «activado» con icono y texto, y al tocarlo de nuevo lo desactiva y restaura el filtro previo, sin cambiar el ajuste global

### Requirement: HU-39 Registrar eventos de estudio
Como estudiante, quiero que la app registre los eventos de mi sesión (inicio, pausas, salidas y regresos, tiempo extra, cambios, fin) con su tópico cuando corresponda para medir mi concentración y uso del tiempo. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Línea de tiempo
- **GIVEN** que una sesión terminó
- **WHEN** el estudiante abre su detalle
- **THEN** el sistema muestra la línea de tiempo de eventos con su hora, tipo y tópico si lo tiene

#### Scenario: Persistencia
- **GIVEN** que se registra un evento
- **WHEN** el sistema operativo termina la app justo después
- **THEN** el evento permanece guardado (RN-22)

#### Scenario: Hora del momento del evento
- **GIVEN** que un evento se registró a las 21:15 con el teléfono en UTC−04:00
- **WHEN** el estudiante viaja, el teléfono pasa a UTC+01:00 y abre el detalle de la sesión
- **THEN** la línea de tiempo sigue mostrando el evento a las 21:15 del mismo día, con la indicación de su zona UTC−04:00

#### Scenario: Cambio de horario durante la sesión
- **GIVEN** que una sesión está En curso cuando el reloj local se atrasa una hora por cambio de horario
- **WHEN** el estudiante la detiene
- **THEN** la duración real se calcula con el tiempo transcurrido, sin perder ni duplicar esa hora (RN-22)

#### Scenario: Tópico guardado en pausa o salida
- **GIVEN** que el estudiante selecciona un tópico (como «PDF / Moodle», «Llamada / mensaje» o «Sin clasificar») al reanudar o resolver un regreso
- **WHEN** el evento queda persistido
- **THEN** el sistema guarda el tópico en el evento para agrupar tiempos en estadísticas

### Requirement: HU-40 Recibir aviso al salir de la app durante una sesión
Como estudiante, quiero recibir una notificación al salir de la app durante trabajo activo y que el regreso se detecte en cualquier pantalla para no perder el hilo ni distorsionar el reloj, sin preguntas por salidas breves. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Salida
- **GIVEN** que hay una sesión En curso fuera de descanso Pomodoro
- **WHEN** el estudiante sale de la app para atender una llamada o abrir un PDF y pasa 1 minuto sin volver
- **THEN** se registra el evento «salida» con la hora en que salió, la sesión NO se pausa en BD, el cronómetro visual deja de avanzar mientras la salida siga sin resolver, suena y vibra un aviso una sola vez (en silencio si hay música sonando, HU-49) con «Tu sesión sigue en curso — volver» y el nombre de la materia, y la notificación de la sesión (HU-49) pasa a mostrar ese mismo estado (RN-23)

#### Scenario: Salida larga con música y pantalla bloqueada
- **GIVEN** que el estudiante estudia Álgebra y a los 15 minutos sale de la app, abre YouTube, pone música y bloquea el teléfono
- **WHEN** vuelve a la app a los 25 minutos, aunque el aviso de 1 minuto no haya llegado a tiempo con el teléfono dormido
- **THEN** se registra la salida a los 15 minutos, no a los 16, y aparece la pregunta «Saliste a las 15:00, ¿seguiste estudiando?» sobre los 10 minutos completos fuera, sin restar ni mezclar el minuto de gracia (RN-23)

#### Scenario: Regreso dentro de la ventana de gracia
- **GIVEN** que hay una sesión En curso y el estudiante salió de la app hace menos de 1 minuto
- **WHEN** vuelve a la app, por ejemplo desde la notificación tras usar el botón de bloquear o tras mirar otra app
- **THEN** no se registra salida ni regreso, el cronómetro no se detiene, no suena ningún aviso y no aparece la pregunta «¿Seguiste estudiando?», ni siquiera si la app se cierra y se vuelve a abrir en ese minuto (RN-23)

#### Scenario: Regreso
- **GIVEN** que el estudiante salió de la app durante trabajo activo y ya se registró la salida (1 minuto o más fuera)
- **WHEN** toca la notificación o vuelve a cualquier pantalla protegida, incluida la pantalla de sesión en curso
- **THEN** se presenta el diálogo de recuperación sobre esa última salida registrada y sin resolver y, al continuar, se registra el evento «regreso»

#### Scenario: Salida dentro de descanso Pomodoro
- **GIVEN** que la sesión está en curso en fase de descanso corto o largo Pomodoro
- **WHEN** el estudiante sale de la app y vuelve antes de que finalice ese descanso
- **THEN** el sistema no registra salida ni interrupción y el descanso sigue corriendo normalmente

#### Scenario: Exceso sobre descanso Pomodoro
- **GIVEN** que la sesión estaba en un descanso Pomodoro de 5 minutos cuando el estudiante salió de la app
- **WHEN** regresa 12 minutos después (7 minutos tras finalizar el descanso)
- **THEN** el sistema considera el descanso completado y abre la recuperación desde el instante en que terminó el descanso hasta el momento actual

#### Scenario: Pantalla apagada con StudyPlan en primer plano
- **GIVEN** que hay una sesión En curso y una pantalla de StudyPlan está en primer plano
- **WHEN** la pantalla se apaga sola por inactividad o con el botón de encendido
- **THEN** el sistema lo trata como el bloqueo de HU-50: registra «bloqueo de pantalla», no registra salida, el reloj sigue y al desbloquear y volver no aparece la pregunta «¿Seguiste estudiando?» (RN-23)

#### Scenario: Pantalla apagada después de salir
- **GIVEN** que el estudiante salió de la app y ya se registró la salida (RN-23)
- **WHEN** la pantalla se apaga
- **THEN** no se registra ningún evento adicional: la salida sigue sin resolver y la pregunta «¿Seguiste estudiando?» aparece al volver

### Requirement: HU-41 Declarar estudio con la app cerrada
Como estudiante, quiero indicar al volver qué hice mientras estuve fuera de la app (si seguí estudiando con otro material o si fue una interrupción sin estudiar) para registrar mis horas reales y clasificar mis pausas. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Volver más tarde
- **GIVEN** que salió de la app a las 8:45 con la sesión En curso y vuelve a las 17:00
- **WHEN** el estudiante supera el bloqueo biométrico/PIN si está activo y responde «Sí, hasta las 11:00» con tópico o nota «en grupo» en el diálogo «Saliste a las 8:45, ¿seguiste estudiando?»
- **THEN** se registra el tramo 8:45–11:00 como «estudio con la app cerrada» sumándolo íntegro incluso si la sesión inició en Pomodoro, y la sesión pasa a Completada (RN-24)

#### Scenario: No siguió
- **GIVEN** que aparece la pregunta al volver
- **WHEN** el estudiante responde «No, terminé al salir»
- **THEN** la sesión se completa con la hora de salida como fin (admitiendo 0 minutos si ocurrió en el primer minuto)

#### Scenario: Sigue estudiando
- **GIVEN** que aparece la pregunta al volver
- **WHEN** el estudiante responde «Sigo estudiando» seleccionando un tópico como «PDF / Moodle» o «Sin clasificar»
- **THEN** el tramo fuera de la app se registra como «estudio con la app cerrada» con ese tópico, se graba el regreso y la sesión continúa En curso

#### Scenario: Estudio externo rápido sin clasificar
- **GIVEN** que aparece el diálogo de regreso tras una salida
- **WHEN** el estudiante confirma «Sigo estudiando» sin elegir una categoría específica
- **THEN** el tramo queda clasificado por defecto con el tópico «Sin clasificar», suma como estudio real y permanece visible aparte en estadísticas

#### Scenario: Retomar sin sumar el tiempo fuera
- **GIVEN** que el estudiante salió 4 minutos para atender una llamada o descanso y vuelve para continuar estudiando
- **WHEN** elige «No estudié ese rato, retomo ahora» y un tópico (o «Sin clasificar»)
- **THEN** el sistema registra el regreso con ese tópico sin crear «estudio con la app cerrada», no suma esos 4 minutos al tiempo real estudiado y reanuda la sesión En curso

#### Scenario: Solo pregunta por la última salida
- **GIVEN** que el estudiante salió y resolvió una salida anterior al volver a la app
- **WHEN** vuelve a salir y regresa por segunda vez
- **THEN** el diálogo solo pregunta por el último tramo abierto y nunca vuelve a mostrar eventos ya resueltos

#### Scenario: Al día siguiente
- **GIVEN** que la sesión quedó En curso desde ayer (dentro de 3 días)
- **WHEN** el estudiante abre la app
- **THEN** se hace la misma pregunta y la sesión no se marcó No realizada (RN-29)

#### Scenario: Vencimiento tras más de 3 días
- **GIVEN** que la sesión quedó En curso por más de 3 días sin regreso ni declaración
- **WHEN** el estudiante abre la app o se ejecuta la verificación de vencimiento
- **THEN** el sistema cierra automáticamente la sesión como Completada en la última marca registrada dentro de la app y restaura No molestar (RN-29)

### Requirement: HU-42 Continuar con tiempo extra al llegar al fin planificado
Como estudiante, quiero que al llegar la hora de fin de una sesión planificada la app me pregunte si sigo estudiando para aprovechar mi inspiración sin perder el registro. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Pregunta
- **GIVEN** que la sesión planificada termina a las 11:00 y sigue En curso
- **WHEN** llegan las 11:00
- **THEN** aparece la notificación «¿Sigues estudiando?» con [Sigo] y [Terminé] sin duplicar en pantalla el aviso de extensión Pomodoro si las fases ya habían concluido (RN-25)

#### Scenario: Sin respuesta
- **GIVEN** que el estudiante no responde (o denegó notificaciones y regresa luego a la app)
- **WHEN** pasa el tiempo
- **THEN** la sesión sigue En curso y el tiempo posterior se marca como tiempo extra

#### Scenario: Sesión sin horario planificado
- **GIVEN** que una sesión inició mediante «Estudiar ahora» sin horario de fin planificado
- **WHEN** el estudiante permanece en la pantalla de sesión en curso
- **THEN** el sistema no muestra la tarjeta ni el aviso de «¿Sigues estudiando?» por fin planificado

### Requirement: HU-43 Decidir al llegar la hora de otra sesión
Como estudiante, quiero que, si sigo estudiando cuando empieza mi siguiente sesión, la app me avise y me deje seguir o cambiar para pasar de una materia a otra sin cortar mi concentración. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Cambiar
- **GIVEN** que Matemáticas está En curso y a las 12:00 empieza Inglés
- **WHEN** el estudiante elige «Cambiar a Inglés»
- **THEN** Matemáticas pasa a Completada a las 12:00 e Inglés pasa a En curso sin detener el tiempo (RN-27)

#### Scenario: Seguir
- **GIVEN** que aparece el aviso
- **WHEN** el estudiante elige «Seguir con Matemáticas»
- **THEN** Inglés queda Pendiente y sigue su ciclo normal

### Requirement: HU-44 Repartir un tramo continuo entre sesiones
Como estudiante, quiero repartir al detener el tiempo estudiado entre la sesión actual y las siguientes del día para reflejar que terminé una materia y seguí con otra sin tocar el celular. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Repartir
- **GIVEN** que Matemáticas (10:00–11:00) estuvo En curso hasta las 12:10 e Inglés estaba planificada de 11:00 a 12:00
- **WHEN** el estudiante detiene y elige «Repartir» fijando la hora de corte a las 11:05
- **THEN** el sistema segmenta los intervalos efectivos trabajados (sin convertir pausas ni descansos Pomodoro en tiempo real) y completa ambas sesiones en una sola transacción atómica (RN-28)

#### Scenario: Sin sesiones siguientes
- **GIVEN** que no hay sesiones Pendientes consecutivas
- **WHEN** el estudiante detiene
- **THEN** no se ofrece repartir

### Requirement: HU-47 Estudiar ahora sin planificar
Como estudiante, quiero empezar a estudiar una materia en el momento, sin haber planificado la sesión, para registrar el estudio que surge sin aviso. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts; propuesta de la IA, aceptada por el Product Owner).

#### Scenario: Acceso desde Inicio
- **GIVEN** que el estudiante está en la pantalla de inicio y no hay una sesión En curso
- **WHEN** pulsa el botón flotante «▶ Estudiar ahora»
- **THEN** el sistema le pide la materia, con «Estudios varios» y «Nueva materia» entre las opciones, y la técnica de estudio, y la sesión no empieza hasta que pulsa «▶ Comenzar»

#### Scenario: Empezar sin plan
- **GIVEN** que el estudiante eligió «Matemáticas» y la materia no tiene sesiones Pendientes hoy
- **WHEN** confirma
- **THEN** el sistema crea una sesión de Matemáticas sin horario planificado y la pone En curso en ese instante (RN-07)

#### Scenario: Sin materias registradas
- **GIVEN** que el estudiante no registró ninguna materia
- **WHEN** usa «Estudiar ahora» con «Estudios varios»
- **THEN** la sesión empieza En curso y sus horas suman a la meta total (RN-32)

#### Scenario: Materia nueva en el mismo paso
- **GIVEN** que el estudiante quiere estudiar «Oratoria», que no está registrada
- **WHEN** elige «Nueva materia», escribe el nombre, elige un color y confirma
- **THEN** el sistema registra la materia (RN-02) y la sesión empieza En curso

#### Scenario: La materia tiene una sesión Pendiente hoy
- **GIVEN** que Inglés tiene una sesión Pendiente hoy de 11:00 a 12:00 y son las 10:40
- **WHEN** el estudiante elige Inglés en «Estudiar ahora»
- **THEN** el sistema pregunta «¿Es tu sesión de Inglés de las 11:00?»; si responde que sí, inicia esa sesión, y si responde que no, crea una sesión sin plan y la de las 11:00 sigue Pendiente

#### Scenario: Sin plan no cuenta como planificado
- **GIVEN** que una sesión sin plan quedó Completada con 50 minutos reales
- **WHEN** el estudiante consulta el resumen del calendario
- **THEN** los 50 minutos suman en «Real» y no suman en «Plan»

#### Scenario: Sin plan no bloquea cruces
- **GIVEN** que hay una sesión sin plan En curso desde las 10:40
- **WHEN** llega la hora de la sesión Pendiente de Inglés de las 11:00
- **THEN** el sistema avisa y deja seguir o cambiar como con cualquier sesión En curso (RN-27), sin tratarlo como cruce de horario (RN-05)

#### Scenario: Ya hay una sesión En curso
- **GIVEN** que ya existe una sesión En curso
- **WHEN** el estudiante pulsa el botón flotante de la pantalla de inicio
- **THEN** el sistema no crea otra ni pide materia: el botón dice «Volver a la sesión» y abre la sesión En curso

#### Scenario: Materia elegida visible
- **GIVEN** que el estudiante abrió «Estudiar ahora»
- **WHEN** toca una materia de la lista
- **THEN** esa fila queda marcada con su indicador y su fondo, las demás se desmarcan, y el botón «▶ Comenzar», desactivado mientras no haya materia elegida, se activa

### Requirement: HU-48 Cambiar de materia durante la sesión
Como estudiante, quiero pasar a estudiar otra materia sin detener el tiempo para reflejar que cambié de tema, por ejemplo al llegar compañeros de otra asignatura. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts; propuesta de la IA, aceptada por el Product Owner).

#### Scenario: Cambiar a una materia sin sesión Pendiente
- **GIVEN** que Matemáticas está En curso y la materia Informática no tiene sesiones Pendientes hoy
- **WHEN** el estudiante elige «Cambiar de materia» e Informática
- **THEN** Matemáticas pasa a Completada en ese instante y una sesión sin plan de Informática pasa a En curso sin detener el tiempo (RN-07, RN-27)

#### Scenario: Cambiar a la sesión planificada
- **GIVEN** que Matemáticas está En curso a las 11:20 e Inglés tiene una sesión Pendiente hoy de 11:00 a 12:00
- **WHEN** el estudiante elige «Cambiar de materia» e Inglés
- **THEN** el sistema pregunta «¿Es tu sesión de Inglés de las 11:00?»; si responde que sí, Matemáticas pasa a Completada e Inglés pasa a En curso conservando su horario planificado

#### Scenario: Cambiar a una materia nueva o a Estudios varios
- **GIVEN** que una sesión está En curso
- **WHEN** el estudiante elige «Cambiar de materia» y luego «Nueva materia» o «Estudios varios»
- **THEN** el sistema registra la materia nueva si hace falta y la sesión de esa materia pasa a En curso sin detener el tiempo

#### Scenario: Misma materia
- **GIVEN** que Matemáticas está En curso
- **WHEN** el estudiante abre «Cambiar de materia»
- **THEN** el sistema no ofrece Matemáticas como destino

### Requirement: HU-49 Controlar la sesión desde la notificación y la pantalla bloqueada
Como estudiante, quiero ver y controlar mi sesión desde la barra de notificaciones y con el teléfono bloqueado, con una notificación con botones, como la de un mensaje, para pausarla o detenerla sin abrir la app ni distraerme y sin interferir con mi música. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts; pedida por el Product Owner el 2026-11-20, redacción de la IA; ajustada el 2026-11-21: notificación normal, no reproductor).

#### Scenario: Notificación mientras la sesión está En curso
- **GIVEN** que el estudiante inicia una sesión de Álgebra con técnica Libre
- **WHEN** abre la barra de notificaciones o bloquea el teléfono
- **THEN** ve una notificación con el logo de la app, la materia «Álgebra», el tiempo transcurrido que avanza solo y los tres botones «Pausar», «Detener» y «No molestar», sin tarjeta ni controles de reproductor, que permanece mientras la sesión siga En curso, incluida la pausa

#### Scenario: Visible en la pantalla de bloqueo
- **GIVEN** que hay una sesión En curso y el teléfono está bloqueado, sin el bloqueo de la app activo
- **WHEN** el estudiante enciende la pantalla
- **THEN** la notificación aparece en la pantalla de bloqueo como cualquier notificación de mensaje, sin sonido ni vibración, con el logo, el tiempo y la fase Pomodoro si la hay, y los tres botones quedan disponibles al expandirla, igual que en la barra de notificaciones con el teléfono desbloqueado

#### Scenario: Botón No molestar de la notificación
- **GIVEN** que la sesión está En curso y la app tiene acceso a No molestar
- **WHEN** el estudiante toca «No molestar» en la notificación
- **THEN** No molestar se activa solo para esa sesión y el botón pasa a «No molestar activado»; al tocarlo de nuevo se desactiva (HU-24); si la app no tiene el acceso, el botón abre la pantalla del sistema donde se concede

#### Scenario: Fase Pomodoro y tiempo restante
- **GIVEN** que la sesión usa Pomodoro y está en el segundo ciclo de trabajo con 12 minutos por delante
- **WHEN** el estudiante mira la notificación
- **THEN** la notificación indica la fase «Trabajo · Ciclo 2/4» y una cuenta atrás de 12 minutos, y al cambiar de fase se actualiza sin abrir la app (HU-23)

#### Scenario: Pausar y reanudar desde la notificación
- **GIVEN** que la sesión está En curso y la app no está en primer plano
- **WHEN** el estudiante toca «Pausar» en la notificación y luego «Reanudar»
- **THEN** la sesión registra la pausa y la reanudación con sus horas (HU-39), mientras está en pausa la notificación muestra el tiempo detenido y el botón «Reanudar», y el tramo pausado queda con el tópico «Sin clasificar», que el estudiante puede cambiar después desde el detalle

#### Scenario: Detener desde la notificación
- **GIVEN** que la sesión está En curso
- **WHEN** el estudiante toca «Detener» en la notificación
- **THEN** la sesión pasa a Completada con su duración real (HU-22), la notificación desaparece y, si alcanzó una meta, aparece en su lugar un aviso de felicitación que abre la felicitación de metas al tocarlo (HU-28)

#### Scenario: Tocar la notificación
- **GIVEN** que la notificación de sesión está visible
- **WHEN** el estudiante la toca fuera de los botones
- **THEN** se abre la pantalla de la sesión en curso, pasando antes por la autenticación si el bloqueo de la app está activo (HU-04)

#### Scenario: Salida de la app en la notificación
- **GIVEN** que hay una sesión En curso y el estudiante salió de la app (HU-40)
- **WHEN** mira la notificación
- **THEN** muestra «Tu sesión sigue en curso — volver» con el tiempo detenido en el instante de la salida (la salida se registra tras 1 minuto fuera, HU-40), y sus botones siguen disponibles

#### Scenario: Descartar la notificación
- **GIVEN** que la notificación de sesión está visible, el teléfono está desbloqueado y el sistema permite descartarla
- **WHEN** el estudiante la desliza
- **THEN** la notificación vuelve a mostrarse mientras la sesión siga En curso, y desaparece al detener la sesión

#### Scenario: Privacidad con el bloqueo de la app activo
- **GIVEN** que el estudiante activó el bloqueo con huella o PIN de la app (HU-04)
- **WHEN** la notificación se muestra con el teléfono bloqueado
- **THEN** muestra «Sesión en curso» y el tiempo, también cuando la sesión está en pausa, sin el nombre de la materia, y en Android 12 o posterior sus botones piden desbloquear el teléfono antes de actuar (RNF-12)

#### Scenario: Botones en Android 11 o anterior
- **GIVEN** que el teléfono usa Android 11 o anterior, donde el sistema no puede exigir el desbloqueo para una acción de notificación
- **WHEN** la notificación se muestra con el teléfono bloqueado
- **THEN** los botones «Pausar» y «Detener» siguen visibles y actúan sin pedir desbloqueo, y si el bloqueo de la app está activo el nombre de la materia sigue oculto

#### Scenario: La sesión termina por otra vía
- **GIVEN** que la notificación de sesión está visible
- **WHEN** la sesión se detiene desde la app, se completa por vencimiento (RN-29) o se cambia a otra sesión (HU-43)
- **THEN** la notificación desaparece o pasa a mostrar la nueva sesión En curso, sin quedar una notificación de una sesión que ya no está En curso

#### Scenario: Notificaciones denegadas
- **GIVEN** que el estudiante denegó el permiso de notificaciones
- **WHEN** inicia una sesión
- **THEN** la sesión funciona igual dentro de la app y la pantalla de la sesión indica que los controles en la notificación están desactivados, con acceso a la configuración del sistema (RNF-18)

#### Scenario: No interfiere con la música
- **GIVEN** que el estudiante escucha música en otra app y hay una sesión En curso
- **WHEN** la notificación de la sesión se publica, se actualiza o se pulsa uno de sus botones
- **THEN** la música no se pausa, no baja de volumen y los botones de los audífonos o del reproductor siguen controlando la música, no la sesión

#### Scenario: Avisos en silencio con música sonando
- **GIVEN** que hay música sonando y la sesión emite un aviso (volver a la app, hito de la sesión, meta alcanzada o recordatorio previo de la sesión, HU-34)
- **WHEN** el aviso se publica
- **THEN** aparece en la barra de notificaciones sin sonido propio ni interrumpir la música, y sin música sonando mantiene el sonido y la vibración de su canal una sola vez, sin sonido duplicado

#### Scenario: Canal de la notificación creado una sola vez
- **GIVEN** que el teléfono tiene el canal antiguo de la notificación de la sesión de una versión anterior
- **WHEN** la app actualiza la notificación varias veces durante la sesión
- **THEN** el canal antiguo se elimina una sola vez y la notificación conserva el ajuste de canal que el estudiante haya elegido

### Requirement: HU-50 Bloquear la pantalla para estudiar
Como estudiante, quiero bloquear el teléfono desde el cronómetro de la sesión sin que cuente como distracción para estudiar sin la tentación de la pantalla. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts; pedida por el Product Owner el 2026-11-20, redacción de la IA).

#### Scenario: Candado dentro del cronómetro
- **GIVEN** que la sesión está En curso
- **WHEN** el estudiante ve el cronómetro de la pantalla de la sesión
- **THEN** dentro del anillo del cronómetro hay un botón con icono de candado y nombre accesible «Bloquear pantalla»

#### Scenario: Confirmación antes de bloquear
- **GIVEN** que la sesión está En curso y el estudiante no marcó «No volver a preguntar»
- **WHEN** toca el candado
- **THEN** el sistema pregunta «Se apagará la pantalla y tu sesión seguirá contando. Puedes pausarla o detenerla desde la notificación» con [Bloquear] y [Cancelar], y la casilla «No volver a preguntar»

#### Scenario: Primer uso y permiso del sistema
- **GIVEN** que la app todavía no tiene permiso para bloquear la pantalla
- **WHEN** el estudiante confirma «Bloquear» por primera vez
- **THEN** el sistema explica que el permiso solo sirve para apagar y bloquear la pantalla, abre la pantalla del sistema para concederlo y, si lo concede, bloquea el teléfono; si lo rechaza, no bloquea y la sesión sigue igual (RNF-18)

#### Scenario: Bloquear cuenta como estudio
- **GIVEN** que la sesión está En curso fuera de pausa
- **WHEN** el estudiante bloquea el teléfono con el candado y lo desbloquea 25 minutos después volviendo a StudyPlan
- **THEN** se registran los eventos «bloqueo de pantalla» y «desbloqueo», los 25 minutos cuentan como estudio, el reloj nunca se congela y no aparece la pregunta «¿Seguiste estudiando?» (W13)

#### Scenario: Bloqueo sin confirmación
- **GIVEN** que el estudiante marcó «No volver a preguntar» y la app tiene el permiso
- **WHEN** toca el candado
- **THEN** el teléfono se bloquea de inmediato

#### Scenario: Proceso terminado con el teléfono bloqueado
- **GIVEN** que el estudiante bloqueó el teléfono con el candado y el sistema terminó el proceso de la app
- **WHEN** abre StudyPlan horas después
- **THEN** la sesión sigue En curso, el tiempo sigue contando como estudio, no se registra ningún evento nuevo por ese hueco y no se pregunta nada (RN-22)

#### Scenario: Retirar el permiso
- **GIVEN** que la app tiene el permiso de bloquear la pantalla
- **WHEN** el estudiante pulsa «Quitar permiso de bloqueo» en Ajustes
- **THEN** el sistema retira el permiso, el estudiante puede desinstalar la app sin pasos extra, y el próximo toque del candado vuelve a pedirlo
