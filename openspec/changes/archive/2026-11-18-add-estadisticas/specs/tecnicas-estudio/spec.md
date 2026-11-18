# Spec Delta: `tecnicas-estudio`

## MODIFIED Requirements

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
Como estudiante, quiero recibir una notificación al salir de la app durante trabajo activo y que el regreso se detecte en cualquier pantalla para no perder el hilo ni distorsionar el reloj. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Salida
- **GIVEN** que hay una sesión En curso fuera de descanso Pomodoro
- **WHEN** el estudiante sale de la app para atender una llamada, abrir un PDF o apagar la pantalla
- **THEN** se registra el evento «salida», la sesión NO se pausa en BD, el cronómetro visual deja de avanzar mientras la salida siga sin resolver y aparece la notificación «Tu sesión sigue en curso — volver» con el nombre de la materia (RN-23)

#### Scenario: Regreso
- **GIVEN** que el estudiante salió de la app durante trabajo activo
- **WHEN** toca la notificación o vuelve a cualquier pantalla protegida, incluida la pantalla de sesión en curso
- **THEN** se presenta el diálogo de recuperación sobre esa última salida sin resolver y, al continuar, se registra el evento «regreso»

#### Scenario: Salida dentro de descanso Pomodoro
- **GIVEN** que la sesión está en curso en fase de descanso corto o largo Pomodoro
- **WHEN** el estudiante sale de la app y vuelve antes de que finalice ese descanso
- **THEN** el sistema no registra salida ni interrupción y el descanso sigue corriendo normalmente

#### Scenario: Exceso sobre descanso Pomodoro
- **GIVEN** que la sesión estaba en un descanso Pomodoro de 5 minutos cuando el estudiante salió de la app
- **WHEN** regresa 12 minutos después (7 minutos tras finalizar el descanso)
- **THEN** el sistema considera el descanso completado y abre la recuperación desde el instante en que terminó el descanso hasta el momento actual

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
