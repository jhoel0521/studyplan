# Spec Delta

## MODIFIED Requirements

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
