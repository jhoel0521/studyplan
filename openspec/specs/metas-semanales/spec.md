# metas-semanales Specification

## Purpose
Permite fijar una meta semanal de horas total y metas por materia, seguir su avance y recibir refuerzo positivo al cumplirlas.

## Requirements

### Requirement: HU-25 Definir meta semanal total
Como estudiante, quiero fijar cuántas horas en total quiero estudiar en la semana para tener un objetivo concreto y medible. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Meta válida
- **GIVEN** que es una semana nueva
- **WHEN** el estudiante define 20 horas
- **THEN** el sistema guarda la meta para esa semana

#### Scenario: Por defecto
- **GIVEN** que no definió meta para la semana
- **WHEN** se abre la pantalla de inicio
- **THEN** se aplica la meta predeterminada del perfil (RN-15)

#### Scenario: Inválida
- **GIVEN** que el formulario está abierto
- **WHEN** ingresa 0 o más de 168 horas
- **THEN** el sistema muestra un error y no guarda (RN-14)

### Requirement: HU-26 Definir meta semanal por materia
Como estudiante, quiero fijar una meta de horas semanales para cada materia que elija para equilibrar mi estudio entre asignaturas. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Meta por materia
- **GIVEN** que existe la materia «Historia»
- **WHEN** el estudiante le asigna 4 horas semanales
- **THEN** el sistema guarda la meta y la muestra en la lista de materias

#### Scenario: Independencia
- **GIVEN** que la meta total es 10 h y las metas por materia suman 12 h
- **WHEN** el estudiante guarda
- **THEN** el sistema lo acepta, porque las metas son independientes (RN-14)

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

### Requirement: HU-28 Recibir mensaje motivacional
Como estudiante, quiero recibir una felicitación al alcanzar una meta para mantener la constancia. El sistema SHALL cumplir los escenarios siguientes (prioridad Could, 2 pts).

#### Scenario: Meta alcanzada
- **GIVEN** que el progreso es menor que la meta
- **WHEN** una sesión completada hace que la alcance
- **THEN** el sistema muestra «¡Sigue así!» con el botón «Volver al inicio»

#### Scenario: Una vez
- **GIVEN** que ya se mostró la felicitación de esa meta esta semana
- **WHEN** se completa otra sesión
- **THEN** no se vuelve a mostrar (RN-16)
