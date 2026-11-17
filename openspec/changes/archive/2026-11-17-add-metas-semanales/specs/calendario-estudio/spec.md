# Spec Delta: `calendario-estudio`

## MODIFIED Requirements

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

#### Scenario: Sesión sin plan en el resumen
- **GIVEN** que en la semana hay una sesión sin plan Completada con 50 minutos reales
- **WHEN** el estudiante consulta la cabecera de la vista Semana
- **THEN** los 50 minutos suman en las horas realizadas y no en las planificadas (HU-47)
