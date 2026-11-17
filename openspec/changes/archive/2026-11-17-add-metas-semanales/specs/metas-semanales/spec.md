# Spec Delta: `metas-semanales`

## MODIFIED Requirements

### Requirement: HU-26 Definir meta semanal por materia
Como estudiante, quiero fijar una meta de horas semanales para cada materia que elija para equilibrar mi estudio entre asignaturas. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Meta por materia
- **GIVEN** que existe la materia «Historia»
- **WHEN** el estudiante le asigna 4 horas semanales en su formulario
- **THEN** el sistema guarda la meta y la muestra en la lista de materias

#### Scenario: Meta opcional
- **GIVEN** que el estudiante registra la materia «Oratoria»
- **WHEN** deja vacía la meta semanal y guarda
- **THEN** el sistema crea la materia sin meta propia y sus horas siguen sumando a la meta total

#### Scenario: Vigente entre semanas
- **GIVEN** que «Historia» tiene una meta de 4 horas semanales
- **WHEN** empieza una semana nueva
- **THEN** la meta de 4 horas sigue vigente sin que el estudiante la vuelva a definir

#### Scenario: Independencia
- **GIVEN** que la meta total es 10 h y las metas por materia suman 12 h
- **WHEN** el estudiante guarda
- **THEN** el sistema lo acepta, porque las metas son independientes (RN-14)

#### Scenario: Inválida
- **GIVEN** que el formulario de materia está abierto
- **WHEN** el estudiante ingresa 0 o más de 168 horas como meta
- **THEN** el sistema muestra un error y no guarda (RN-14)

### Requirement: HU-27 Ver progreso de metas
Como estudiante, quiero ver cuántas horas llevo frente a la meta total y a cada meta por materia para ajustar mi ritmo antes de que termine la semana. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Cálculo
- **GIVEN** que la meta total es 20 h y hay 12 h realizadas
- **WHEN** el estudiante consulta su progreso
- **THEN** el sistema muestra «12 / 20 h» y una barra al 60 %

#### Scenario: Solo realizadas
- **GIVEN** que hay sesiones Pendientes y No realizadas
- **WHEN** se calcula el progreso
- **THEN** solo suman las Completadas y Completadas (registro tardío) (RN-11)

#### Scenario: Metas independientes
- **GIVEN** que la meta total es 14 h, la de Matemáticas es 6 h y en la semana solo se estudiaron 8 h de Matemáticas
- **WHEN** el estudiante consulta su progreso
- **THEN** el sistema muestra la meta de Matemáticas cumplida (8 / 6 h) y la meta total sin cumplir (8 / 14 h) (RN-14)

#### Scenario: Corte de semana
- **GIVEN** que una sesión se estudió el domingo de 23:30 a 00:30 del lunes
- **WHEN** se calcula el progreso de cada semana
- **THEN** 30 minutos suman a la semana del domingo y 30 minutos a la semana siguiente (RN-31)

#### Scenario: Historial conservado
- **GIVEN** que la semana pasada la meta de Matemáticas era 6 h y se cumplió
- **WHEN** el estudiante cambia la meta de Matemáticas a 10 h
- **THEN** la semana pasada sigue registrada como cumplida con su meta de 6 h y la nueva meta rige desde la semana actual (RN-33)

#### Scenario: Consultar semanas anteriores
- **GIVEN** que existen semanas pasadas con metas
- **WHEN** el estudiante consulta el historial de metas
- **THEN** el sistema muestra por cada semana la meta, las horas realizadas y si se cumplió (RN-33)

### Requirement: HU-28 Recibir mensaje motivacional
Como estudiante, quiero recibir una felicitación al alcanzar una meta para mantener la constancia. El sistema SHALL cumplir los escenarios siguientes (prioridad Could, 2 pts).

#### Scenario: Meta alcanzada
- **GIVEN** que el progreso es menor que la meta
- **WHEN** el estudiante termina de estudiar y la sesión completada hace que la alcance
- **THEN** el sistema muestra «¡Sigue así!» con la meta alcanzada y el botón «Volver al inicio»

#### Scenario: Varias metas en una sola felicitación
- **GIVEN** que el estudiante estudió Matemáticas, cambió a Informática y luego detuvo
- **WHEN** ese tramo alcanzó la meta de Matemáticas y la meta total
- **THEN** el sistema muestra una sola felicitación que lista ambas metas (RN-16)

#### Scenario: Sin interrupción al cambiar de materia
- **GIVEN** que el estudiante alcanza la meta de Matemáticas
- **WHEN** cambia a otra materia sin detener
- **THEN** el sistema no muestra la felicitación en ese momento y la incluye al terminar de estudiar

#### Scenario: Una vez
- **GIVEN** que ya se felicitó una meta esta semana
- **WHEN** se completa otra sesión
- **THEN** esa meta no se vuelve a felicitar (RN-16)
