## ADDED Requirements

### Requirement: HU-49 Controlar la sesión desde la notificación y la pantalla bloqueada
Como estudiante, quiero ver y controlar mi sesión desde la barra de notificaciones y con el teléfono bloqueado, como un reproductor de música, para pausarla o detenerla sin abrir la app ni distraerme. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts; pedida por el Product Owner el 2026-11-20, redacción de la IA).

#### Scenario: Notificación mientras la sesión está En curso
- **GIVEN** que el estudiante inicia una sesión de Álgebra con técnica Libre
- **WHEN** abre la barra de notificaciones o bloquea el teléfono
- **THEN** ve una notificación con el logo de la app, la materia «Álgebra», el tiempo transcurrido que avanza solo y los tres botones «Pausar», «Detener» y «No molestar», que permanece mientras la sesión siga En curso, incluida la pausa

#### Scenario: Visible en la pantalla de bloqueo
- **GIVEN** que hay una sesión En curso y el teléfono está bloqueado, sin el bloqueo de la app activo
- **WHEN** el estudiante enciende la pantalla
- **THEN** la notificación aparece en la pantalla de bloqueo como una tarjeta pequeña, sin sonido ni vibración, con el logo, el tiempo, la fase Pomodoro si la hay y los tres botones

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
- **THEN** muestra «Tu sesión sigue en curso — volver» con el tiempo detenido en el instante de la salida, y sus botones siguen disponibles

#### Scenario: Descartar la notificación
- **GIVEN** que la notificación de sesión está visible, el teléfono está desbloqueado y el sistema permite descartarla
- **WHEN** el estudiante la desliza
- **THEN** la notificación vuelve a mostrarse mientras la sesión siga En curso, y desaparece al detener la sesión

#### Scenario: Privacidad con el bloqueo de la app activo
- **GIVEN** que el estudiante activó el bloqueo con huella o PIN de la app (HU-04)
- **WHEN** la notificación se muestra con el teléfono bloqueado
- **THEN** muestra «Sesión en curso» y el tiempo sin el nombre de la materia, y en Android 12 o posterior sus botones piden desbloquear el teléfono antes de actuar (RNF-12)

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

## MODIFIED Requirements

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

### Requirement: HU-40 Recibir aviso al salir de la app durante una sesión
Como estudiante, quiero recibir una notificación al salir de la app durante trabajo activo y que el regreso se detecte en cualquier pantalla para no perder el hilo ni distorsionar el reloj. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Salida
- **GIVEN** que hay una sesión En curso fuera de descanso Pomodoro
- **WHEN** el estudiante sale de la app para atender una llamada o abrir un PDF
- **THEN** se registra el evento «salida», la sesión NO se pausa en BD, el cronómetro visual deja de avanzar mientras la salida siga sin resolver, suena y vibra un aviso una sola vez con «Tu sesión sigue en curso — volver» y el nombre de la materia, y la notificación de la sesión (HU-49) pasa a mostrar ese mismo estado (RN-23)

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

#### Scenario: Pantalla apagada con StudyPlan en primer plano
- **GIVEN** que hay una sesión En curso y una pantalla de StudyPlan está en primer plano
- **WHEN** la pantalla se apaga sola por inactividad o con el botón de encendido
- **THEN** el sistema lo trata como el bloqueo de HU-50: registra «bloqueo de pantalla», no registra salida, el reloj sigue y al desbloquear y volver no aparece la pregunta «¿Seguiste estudiando?» (RN-23)

#### Scenario: Pantalla apagada después de salir
- **GIVEN** que el estudiante salió de la app y ya se registró la salida (RN-23)
- **WHEN** la pantalla se apaga
- **THEN** no se registra ningún evento adicional: la salida sigue sin resolver y la pregunta «¿Seguiste estudiando?» aparece al volver
