# Spec Delta

## MODIFIED Requirements

### Requirement: HU-20 Crear sesión desde el calendario
Como estudiante, quiero mantener pulsado un día u hora libre del calendario para crear una sesión para planificar directamente sobre mi agenda. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Hora libre
- **GIVEN** que la vista Semana o Día está abierta en una fecha igual o posterior a hoy
- **WHEN** el estudiante mantiene pulsada de forma sostenida una franja libre a las 15:00 del viernes
- **THEN** el sistema abre «Nueva sesión» en modo planificar con fecha viernes, inicio 15:00 y fin sugerido 16:00 precargados

#### Scenario: Día en la vista Mes
- **GIVEN** que la vista Mes está abierta en una fecha igual o posterior a hoy
- **WHEN** el estudiante mantiene pulsado de forma sostenida un día
- **THEN** el sistema abre «Nueva sesión» en modo planificar con esa fecha precargada

#### Scenario: Fecha pasada sin creación desde el calendario
- **GIVEN** que cualquier vista del calendario muestra un día anterior a hoy
- **WHEN** el estudiante mantiene pulsado ese día o una franja horaria de ese día
- **THEN** el sistema no abre el formulario de creación desde el calendario

### Requirement: HU-21 Filtrar y resumir el calendario
Como estudiante, quiero filtrar el calendario por materia (incluyendo opcionalmente archivadas) y ver el total de horas planificadas y realizadas para analizar mi dedicación a una materia en el tiempo. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Filtro
- **GIVEN** que hay sesiones de varias materias activas y archivadas
- **WHEN** el estudiante filtra por «Matemáticas» o habilita incluir materias archivadas para seleccionar una archivada
- **THEN** todas las vistas muestran solo las sesiones que cumplen el filtro hasta restablecerlo

#### Scenario: Resumen
- **GIVEN** que una vista está abierta
- **WHEN** el estudiante consulta la cabecera
- **THEN** el sistema muestra las horas planificadas y realizadas del día, la semana o el mes visible según el filtro activo
