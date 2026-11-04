# Spec Delta — Estadísticas (E7)

## Purpose

Muestra indicadores de dedicación: horas totales, comparación entre semanas, distribución por materia y tasa de cumplimiento.

## ADDED Requirements

### Requirement: HU-29 Ver total de horas y comparación semanal
Como estudiante, quiero ver el total de horas de la semana y su variación respecto a la anterior para saber si estoy mejorando. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Variación
- **GIVEN** que esta semana hay 15 h y la anterior 12 h
- **WHEN** el estudiante abre Estadísticas
- **THEN** el sistema muestra «15 h» y «↑ 25 % vs. semana anterior»

#### Scenario: Sin datos previos
- **GIVEN** que no hay datos de la semana anterior
- **WHEN** abre Estadísticas
- **THEN** se muestra el total sin comparación

### Requirement: HU-30 Ver distribución de horas por materia
Como estudiante, quiero ver un gráfico de anillo con el porcentaje de horas por materia para identificar mis áreas de mejora. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Gráfico
- **GIVEN** que hay horas de varias materias en el periodo
- **WHEN** el estudiante abre Estadísticas
- **THEN** el sistema muestra un gráfico de anillo con el color de cada materia y su porcentaje

#### Scenario: Periodo
- **GIVEN** que el gráfico está visible
- **WHEN** el estudiante cambia entre Semana y Mes
- **THEN** el gráfico se recalcula

### Requirement: HU-31 Ver tasa de cumplimiento
Como estudiante, quiero ver cuántas sesiones cumplí, no realicé o reprogramé en el periodo para medir mi constancia y no solo mis horas. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Cumplimiento
- **GIVEN** que en la semana hubo 8 completadas, 1 tardía, 2 no realizadas y 1 reprogramada
- **WHEN** el estudiante abre Estadísticas
- **THEN** el sistema muestra el conteo por estado y una tasa de cumplimiento del 75 % (9 de 12)

### Requirement: HU-45 Ver métricas de concentración y respeto del tiempo
Como estudiante, quiero ver mi índice de concentración, interrupciones, puntualidad, respeto del tiempo y tiempo fuera de la app para mejorar la calidad de mi estudio y no solo la cantidad. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Métricas
- **GIVEN** que en la semana hubo sesiones con eventos
- **WHEN** el estudiante abre Estadísticas > Concentración
- **THEN** el sistema muestra concentración (%), interrupciones por hora, puntualidad promedio, respeto del tiempo (%) y tiempo fuera de la app (RN-30)

#### Scenario: Autodeclarado separado
- **GIVEN** que hubo tramos de estudio con la app cerrada
- **WHEN** se calculan las métricas
- **THEN** esos tramos suman horas reales pero se muestran aparte y no entran en el índice de concentración
