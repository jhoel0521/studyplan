# sesiones-estudio Specification

## Purpose
Permite planificar sesiones de estudio y gobierna su ciclo de vida mediante una máquina de estados que conserva el registro histórico de lo estudiado y lo no estudiado.

## Requirements

### Requirement: RN-07 Máquina de estados de la sesión
El estado de una sesión SHALL cambiar únicamente por las transiciones listadas en los escenarios; cada estado conoce solo sus acciones y su estado siguiente. Los estados finales (Completada, Completada (registro tardío), Reprogramada) MUST NOT admitir transiciones.

#### Scenario: Pendiente → En curso (iniciar)
- **GIVEN** una sesión en estado Pendiente
- **WHEN** ocurre el evento «iniciar»
- **THEN** la sesión pasa a En curso — Inicia cronómetro o Pomodoro

#### Scenario: En curso → En curso (pausar / reanudar)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «pausar / reanudar»
- **THEN** la sesión pasa a En curso — El tiempo pausado no suma

#### Scenario: En curso → Completada (detener)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «detener»
- **THEN** la sesión pasa a Completada — Guarda la duración real medida

#### Scenario: Pendiente → Completada (marcar completada)
- **GIVEN** una sesión en estado Pendiente del mismo día
- **WHEN** el estudiante desea completarla en el flujo normal
- **THEN** el sistema no permite completarla directamente con minutos editables; debe pasar por «iniciar» (En curso) y luego «detener» para calcular la duración real desde los eventos registrados

#### Scenario: Pendiente → No realizada (fin del día)
- **GIVEN** una sesión en estado Pendiente
- **WHEN** ocurre el evento «fin del día»
- **THEN** la sesión pasa a No realizada — Automático (RN-08)

#### Scenario: Pendiente → Reprogramada (reprogramar)
- **GIVEN** una sesión en estado Pendiente
- **WHEN** ocurre el evento «reprogramar»
- **THEN** la sesión pasa a Reprogramada — Crea una nueva sesión Pendiente enlazada (RN-09)

#### Scenario: No realizada → Reprogramada (reprogramar)
- **GIVEN** una sesión en estado No realizada
- **WHEN** ocurre el evento «reprogramar»
- **THEN** la sesión pasa a Reprogramada — Crea una nueva sesión Pendiente enlazada (RN-09)

#### Scenario: No realizada → Completada (registro tardío) (registrar tardío)
- **GIVEN** una sesión en estado No realizada
- **WHEN** ocurre el evento «registrar tardío»
- **THEN** la sesión pasa a Completada (registro tardío) — Solo si la fecha es de hace 3 días o menos (RN-10)

#### Scenario: Nueva → Completada (registro tardío) (registrar sesión pasada)
- **GIVEN** una sesión nueva
- **WHEN** ocurre el evento «registrar sesión pasada»
- **THEN** la sesión pasa a Completada (registro tardío) — Fecha de hace 3 días o menos (RN-06)

#### Scenario: En curso → Completada (cambiar a la siguiente sesión)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «cambiar a la siguiente sesión»
- **THEN** la sesión pasa a Completada — La siguiente sesión pasa a En curso sin detener el tiempo (RN-27)

#### Scenario: En curso → Completada (declarar fin al volver)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «declarar fin al volver»
- **THEN** la sesión pasa a Completada — El tramo declarado es «estudio con la app cerrada» (RN-24)

#### Scenario: En curso → Completada (plazo de declaración vencido)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «plazo de declaración vencido»
- **THEN** la sesión pasa a Completada — Cierre en la última marca dentro de la app (RN-29)

#### Scenario: Estado final sin transiciones
- **GIVEN** una sesión en estado Completada, Completada (registro tardío) o Reprogramada
- **WHEN** se intenta cualquier evento de cambio de estado
- **THEN** el sistema rechaza el cambio y la sesión conserva su estado

### Requirement: HU-10 Planificar sesión de estudio
Como estudiante, quiero registrar una sesión con materia, fecha, hora de inicio, hora de fin y notas opcionales para planificar cuándo voy a estudiar cada asignatura. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Registro válido
- **GIVEN** que existe una materia activa
- **WHEN** el estudiante registra una sesión de hoy o futura de 10:00 a 11:30
- **THEN** el sistema la crea en estado Pendiente con 90 minutos planificados

#### Scenario: Sesión que cruza la medianoche
- **GIVEN** que existe una materia activa
- **WHEN** el estudiante registra una sesión el lunes con inicio 22:00 y fin 01:00
- **THEN** el sistema la crea en estado Pendiente desde el lunes 22:00 hasta el martes 01:00, con 180 minutos planificados, e indica que termina al día siguiente (RN-04)

#### Scenario: Horario inválido
- **GIVEN** que el formulario está abierto
- **WHEN** la hora de fin es igual a la de inicio
- **THEN** el sistema muestra «La hora de fin debe ser distinta de la de inicio» y no guarda (RN-04)

#### Scenario: Cruce bloqueado
- **GIVEN** que existe una sesión de 10:00 a 11:30 ese día
- **WHEN** el estudiante registra otra de 11:00 a 12:00 el mismo día
- **THEN** el sistema bloquea el registro e indica con qué sesión se cruza (RN-05)

#### Scenario: Cruce entre dos días
- **GIVEN** que existe una sesión del lunes de 22:00 a 01:00
- **WHEN** el estudiante registra otra el martes de 00:30 a 02:00
- **THEN** el sistema bloquea el registro e indica con qué sesión se cruza (RN-05)

#### Scenario: Horario estable ante cambio de zona
- **GIVEN** que una sesión se planificó a las 10:00 con el teléfono en UTC−04:00
- **WHEN** el estudiante cambia la zona horaria del teléfono a UTC+01:00 y abre la sesión
- **THEN** el sistema la sigue mostrando de 10:00 a 11:30 del mismo día, con la indicación de su zona UTC−04:00

### Requirement: HU-11 Registrar sesión pasada (registro tardío)
Como estudiante, quiero registrar una sesión que ya estudié en los últimos 3 días para no perder horas reales si olvidé anotarlas. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Dentro del límite
- **GIVEN** que hoy es jueves
- **WHEN** el estudiante registra una sesión del lunes de la misma semana
- **THEN** el sistema la guarda en estado «Completada (registro tardío)» y suma sus horas (RN-06)

#### Scenario: Fuera del límite
- **GIVEN** que hoy es jueves
- **WHEN** el estudiante intenta registrar una sesión de hace 4 días o más
- **THEN** el sistema rechaza la fecha e informa el límite de 3 días

### Requirement: HU-12 Completar una sesión
Como estudiante, quiero completar mi sesión Pendiente del día ejecutándola en vivo para registrar el tiempo real estudiado sin introducir duraciones manuales arbitrarias. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Completar
- **GIVEN** que existe una sesión Pendiente de hoy
- **WHEN** el estudiante abre su detalle
- **THEN** el sistema no muestra campo editable de minutos ni botón de completado directo; la sesión se completa iniciándola (En curso) y deteniéndola (o declarando fin al volver) para calcular la duración real desde los eventos (RN-07)

#### Scenario: Transición inválida
- **GIVEN** que una sesión está Reprogramada
- **WHEN** el estudiante intenta completarla
- **THEN** el sistema no muestra esa acción, porque el estado no la permite (RN-07)

### Requirement: HU-13 Marcar automáticamente sesiones no realizadas
Como estudiante, quiero que las sesiones que no cumplí queden registradas como No realizadas para tener un historial honesto de mi cumplimiento. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Fin del día
- **GIVEN** que una sesión de hoy sigue Pendiente
- **WHEN** termina el día local en que cae su fin planificado
- **THEN** el sistema la pasa a «No realizada» sin intervención del usuario (RN-08)

#### Scenario: Sesión nocturna aún vigente
- **GIVEN** que una sesión del lunes de 22:00 a 01:00 sigue Pendiente
- **WHEN** llega la medianoche del lunes al martes
- **THEN** la sesión sigue Pendiente y solo pasa a «No realizada» al terminar el martes (RN-08)

#### Scenario: Registro tardío
- **GIVEN** que una sesión de hace 2 días está No realizada
- **WHEN** el estudiante elige «Registrar tardío» e indica la duración
- **THEN** la sesión pasa a «Completada (registro tardío)» y suma horas (RN-10)

#### Scenario: Fuera de plazo
- **GIVEN** que una sesión de hace 5 días está No realizada
- **WHEN** el estudiante la consulta
- **THEN** el sistema ya no ofrece el registro tardío, solo reprogramar

### Requirement: HU-14 Reprogramar sesión conservando el registro
Como estudiante, quiero reprogramar una sesión Pendiente o No realizada a otra fecha y hora para recuperar el estudio perdido sin borrar el registro de que no estudié ese día. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Reprogramar
- **GIVEN** que existe una sesión No realizada del martes
- **WHEN** el estudiante la reprograma al jueves de 16:00 a 17:00
- **THEN** la sesión del martes pasa a «Reprogramada» y se crea una nueva sesión Pendiente el jueves enlazada a la original (RN-09)

#### Scenario: Historial visible
- **GIVEN** que una sesión fue reprogramada
- **WHEN** el estudiante abre la sesión original
- **THEN** el sistema muestra su estado Reprogramada y un enlace a la nueva sesión

#### Scenario: Cruce
- **GIVEN** que el nuevo horario se cruza con otra sesión, aunque sea en el día anterior o siguiente
- **WHEN** el estudiante confirma la reprogramación
- **THEN** el sistema la bloquea (RN-05)

### Requirement: HU-15 Eliminar sesión planificada
Como estudiante, quiero eliminar una sesión Pendiente que ya no voy a hacer para mantener mi plan limpio. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 2 pts).

#### Scenario: Eliminar Pendiente
- **GIVEN** que existe una sesión Pendiente
- **WHEN** el estudiante la elimina y confirma
- **THEN** el sistema la borra y recalcula los resúmenes

#### Scenario: Historial protegido
- **GIVEN** que una sesión está Completada, No realizada o Reprogramada
- **WHEN** el estudiante busca la opción de eliminar
- **THEN** el sistema no la ofrece (RN-12)
