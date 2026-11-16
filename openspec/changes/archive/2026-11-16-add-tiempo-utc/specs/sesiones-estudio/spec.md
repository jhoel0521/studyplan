# Spec Delta: `sesiones-estudio`

## MODIFIED Requirements

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
