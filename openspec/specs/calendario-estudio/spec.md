# calendario-estudio Specification

## Purpose
Muestra las sesiones en vistas de día, semana y mes con su estado, y permite crear sesiones, filtrar por materia y ver resúmenes de horas desde el calendario.

## Requirements

### Requirement: HU-16 Ver calendario mensual
Como estudiante, quiero ver un calendario del mes con los días que tienen sesiones para visualizar mi carga de estudio del mes. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Vista mensual
- **GIVEN** que hay sesiones en el mes
- **WHEN** el estudiante abre Calendario en vista Mes
- **THEN** el sistema resalta hoy y marca cada día con sesiones usando el color de sus materias

#### Scenario: Seleccionar día
- **GIVEN** que la vista Mes está abierta
- **WHEN** el estudiante toca un día
- **THEN** el sistema lista debajo las sesiones de ese día con materia, horario y estado

#### Scenario: Navegar
- **GIVEN** que la vista Mes está abierta
- **WHEN** el estudiante desliza o pulsa las flechas
- **THEN** el sistema muestra el mes anterior o siguiente

### Requirement: HU-17 Ver calendario semanal tipo horario
Como estudiante, quiero ver la semana de lunes a domingo como un horario por horas con bloques de sesiones para organizar mi estudio igual que mi horario de clases. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 8 pts).

#### Scenario: Vista semanal
- **GIVEN** que hay sesiones en la semana
- **WHEN** el estudiante elige la vista Semana
- **THEN** el sistema muestra 7 columnas (L–D) con franjas horarias y cada sesión como un bloque del color de su materia, de alto proporcional a su duración

#### Scenario: Navegar semanas
- **GIVEN** que la vista Semana está abierta
- **WHEN** el estudiante desliza a la izquierda
- **THEN** el sistema muestra la semana siguiente

### Requirement: HU-18 Ver calendario diario
Como estudiante, quiero ver un día con sus horas y las sesiones en línea de tiempo para enfocarme en lo que tengo que hacer hoy. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Vista día
- **GIVEN** que hoy hay sesiones
- **WHEN** el estudiante elige la vista Día
- **THEN** el sistema muestra las horas del día y las sesiones como bloques con materia, horario y estado, y una línea en la hora actual

### Requirement: HU-19 Ver el estado de las sesiones en el calendario
Como estudiante, quiero distinguir en el calendario las sesiones completadas, pendientes, no realizadas y reprogramadas para ver de un vistazo mi cumplimiento. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Indicadores
- **GIVEN** que hay sesiones en distintos estados
- **WHEN** el estudiante ve cualquier vista del calendario
- **THEN** cada sesión muestra un ícono y una etiqueta propios de su estado, sin depender solo del color

#### Scenario: Leyenda
- **GIVEN** que el estudiante no reconoce un ícono
- **WHEN** abre la leyenda del calendario
- **THEN** el sistema explica cada ícono de estado

### Requirement: HU-20 Crear sesión desde el calendario
Como estudiante, quiero tocar un día u hora libre del calendario para crear una sesión para planificar directamente sobre mi agenda. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Hora libre
- **GIVEN** que la vista Semana o Día está abierta
- **WHEN** el estudiante toca una franja libre a las 15:00 del viernes
- **THEN** el sistema abre «Nueva sesión» con fecha viernes e inicio 15:00 precargados

#### Scenario: Día en la vista Mes
- **GIVEN** que la vista Mes está abierta
- **WHEN** el estudiante mantiene pulsado un día
- **THEN** el sistema abre «Nueva sesión» con esa fecha

### Requirement: HU-21 Filtrar y resumir el calendario
Como estudiante, quiero filtrar el calendario por materia y ver el total de horas planificadas y realizadas para analizar mi dedicación a una materia en el tiempo. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Filtro
- **GIVEN** que hay sesiones de varias materias
- **WHEN** el estudiante filtra por «Matemáticas»
- **THEN** todas las vistas muestran solo las sesiones de esa materia hasta quitar el filtro

#### Scenario: Resumen
- **GIVEN** que una vista está abierta
- **WHEN** el estudiante consulta la cabecera
- **THEN** el sistema muestra las horas planificadas y realizadas del día, la semana o el mes visible
