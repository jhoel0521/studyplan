# Spec Delta: `perfil-ajustes`

## MODIFIED Requirements

### Requirement: HU-02 Ver resumen en la pantalla de inicio
Como estudiante, quiero ver mi avance de la meta semanal, mi próxima sesión y las sesiones de hoy para saber en segundos qué me toca estudiar y cómo voy. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Resumen con datos
- **GIVEN** que existen una meta y sesiones planificadas
- **WHEN** el estudiante abre la pantalla de inicio
- **THEN** el sistema muestra las horas realizadas sobre la meta con una barra de progreso, la próxima sesión Pendiente y las sesiones de hoy con su estado

#### Scenario: Horas de la semana con corte estricto
- **GIVEN** que el domingo anterior el estudiante estudió de 23:00 a 01:00 del lunes y quedó Completada
- **WHEN** abre la pantalla de inicio el lunes
- **THEN** las horas realizadas de la semana actual incluyen solo la hora estudiada desde las 00:00 del lunes (RN-31)

#### Scenario: Acceso rápido
- **GIVEN** que el estudiante está en la pantalla de inicio
- **WHEN** pulsa «Nueva sesión»
- **THEN** el sistema abre el registro de sesión con la fecha de hoy preseleccionada
