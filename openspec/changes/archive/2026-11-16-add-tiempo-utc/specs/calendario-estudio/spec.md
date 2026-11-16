# Spec Delta: `calendario-estudio`

## MODIFIED Requirements

### Requirement: HU-17 Ver calendario semanal tipo horario
Como estudiante, quiero ver la semana de lunes a domingo como un horario por horas con bloques de sesiones para organizar mi estudio igual que mi horario de clases. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 8 pts).

#### Scenario: Vista semanal
- **GIVEN** que hay sesiones en la semana
- **WHEN** el estudiante elige la vista Semana
- **THEN** el sistema muestra 7 columnas (L–D) con franjas horarias y cada sesión como un bloque del color de su materia, de alto proporcional a su duración

#### Scenario: Sesión que cruza la medianoche
- **GIVEN** que existe una sesión del lunes de 22:00 a 01:00
- **WHEN** el estudiante abre la vista Semana
- **THEN** el sistema muestra un bloque el lunes de 22:00 a 24:00 y otro el martes de 00:00 a 01:00, ambos de la misma sesión y con el mismo estado

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

#### Scenario: Sesión que viene del día anterior
- **GIVEN** que existe una sesión del lunes de 22:00 a 01:00
- **WHEN** el estudiante abre la vista Día del martes
- **THEN** el sistema muestra el bloque de 00:00 a 01:00 e indica que la sesión empezó el lunes

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

#### Scenario: Corte estricto en la medianoche
- **GIVEN** que una sesión se estudió el lunes de 22:00 a 01:00 y quedó Completada con 180 minutos reales
- **WHEN** el estudiante consulta el resumen del lunes y luego el del martes
- **THEN** el lunes suma 2 h reales y el martes 1 h real (RN-31)

#### Scenario: Corte estricto entre semanas
- **GIVEN** que una sesión se estudió el domingo de 23:30 a 00:30 del lunes siguiente y quedó Completada con 60 minutos reales
- **WHEN** el estudiante consulta el resumen de cada semana
- **THEN** la semana del domingo suma 30 minutos reales y la semana siguiente otros 30 minutos (RN-31, RN-13)
