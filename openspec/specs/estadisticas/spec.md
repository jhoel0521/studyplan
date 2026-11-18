# estadisticas Specification

## Purpose
Muestra indicadores de dedicación: horas totales, comparación entre semanas, distribución por materia y tasa de cumplimiento.

## Requirements

### Requirement: HU-29 Ver total de horas y comparación semanal
Como estudiante, quiero ver el total de horas del periodo y su variación respecto al anterior para saber si estoy mejorando. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Variación
- **GIVEN** que esta semana hay 15 h y la anterior 12 h
- **WHEN** el estudiante abre Estadísticas
- **THEN** el sistema muestra «15 h» y «↑ 25 % vs. semana anterior»

#### Scenario: Sin datos previos
- **GIVEN** que no hay datos de la semana anterior
- **WHEN** abre Estadísticas
- **THEN** se muestra el total sin comparación

#### Scenario: Corte estricto por día y semana
- **GIVEN** que una sesión se estudió el domingo de 23:00 a 01:00 del lunes
- **WHEN** el estudiante consulta la semana del lunes
- **THEN** solo se contabiliza la hora ocurrida desde las 00:00 del lunes (RN-31)

#### Scenario: Comparación en vista mensual
- **GIVEN** que el estudiante cambia a vista Mes con 40 h reales y 50 h en el mes anterior
- **WHEN** consulta Estadísticas
- **THEN** el sistema muestra «40 h» y «↓ 20 % vs. mes anterior»

### Requirement: HU-30 Ver distribución de horas por materia
Como estudiante, quiero ver un gráfico de anillo con el porcentaje de horas por materia para identificar mis áreas de mejora. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Gráfico
- **GIVEN** que hay horas de varias materias en el periodo
- **WHEN** el estudiante abre Estadísticas
- **THEN** el sistema muestra un gráfico de anillo con el color de cada materia y su porcentaje

#### Scenario: Periodo
- **GIVEN** que el gráfico está visible
- **WHEN** el estudiante cambia entre Semana y Mes o navega al periodo anterior/siguiente
- **THEN** el gráfico y los totales se recalculan para ese periodo

#### Scenario: Incluye Estudios varios y materias archivadas con horas
- **GIVEN** que en el periodo hay horas registradas en «Estudios varios» y en una materia posteriormente archivada
- **WHEN** el estudiante abre Estadísticas
- **THEN** ambas aparecen en el gráfico y en el desglose con sus horas reales del periodo

#### Scenario: Sin horas registradas en el periodo
- **GIVEN** que no existen minutos estudiados en el periodo visible
- **WHEN** abre Estadísticas
- **THEN** el sistema muestra un estado vacío claro sin ocultar la navegación de periodo

### Requirement: HU-31 Ver tasa de cumplimiento
Como estudiante, quiero ver cuántas sesiones cumplí, no realicé o reprogramé en el periodo para medir mi constancia y no solo mis horas. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 3 pts).

#### Scenario: Cumplimiento
- **GIVEN** que en la semana hubo 8 completadas, 1 tardía, 2 no realizadas y 1 reprogramada
- **WHEN** el estudiante abre Estadísticas
- **THEN** el sistema muestra el conteo por estado y una tasa de cumplimiento del 75 % (9 de 12)

#### Scenario: Exclusión de sesiones aún pendientes o en curso
- **GIVEN** que en la semana actual hay 3 sesiones cerradas (2 completadas, 1 no realizada) y 2 sesiones Pendientes para mañana
- **WHEN** el estudiante abre Estadísticas
- **THEN** vence las pendientes anteriores a hoy (RN-08), calcula «67 % (2 de 3)» excluyendo las vigentes del denominador e informa aparte cuántas siguen pendientes

#### Scenario: Periodo sin sesiones cerradas
- **GIVEN** que el periodo no tiene sesiones completadas, tardías, no realizadas ni reprogramadas
- **WHEN** consulta Estadísticas
- **THEN** el sistema muestra los conteos en 0 e indica que aún no hay sesiones cerradas para calcular tasa

### Requirement: HU-45 Ver métricas de concentración y respeto del tiempo
Como estudiante, quiero ver mi índice de concentración, interrupciones, puntualidad, respeto del tiempo, tiempo fuera de la app y tópicos de pausas o salidas para mejorar la calidad de mi estudio. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Métricas
- **GIVEN** que en la semana hubo sesiones con eventos
- **WHEN** el estudiante abre Estadísticas > Concentración
- **THEN** el sistema muestra concentración (%) calculada sobre tiempo en la app frente a pausas manuales, interrupciones por hora, puntualidad promedio, respeto del tiempo (%) y tiempo fuera de la app (RN-30)

#### Scenario: Autodeclarado separado
- **GIVEN** que hubo tramos de estudio con la app cerrada, parte clasificados como «PDF / Moodle» y parte como «Sin clasificar»
- **WHEN** se calculan las métricas
- **THEN** esos tramos suman horas reales totales pero se muestran aparte (desglosando cuánto quedó «Sin clasificar») y no entran en el índice de concentración

#### Scenario: Salidas para estudiar no penalizan interrupciones
- **GIVEN** que durante una sesión de 60 minutos en la app el estudiante salió una vez para leer un PDF (declarado como «Sigo estudiando»), hizo 1 pausa manual para descansar y salió 1 vez sin estudiar para atender una llamada («No estudié ese rato, retomo ahora»)
- **WHEN** consulta Estadísticas > Concentración
- **THEN** el sistema contabiliza 2 interrupciones (la pausa manual y la salida no académica), excluye la salida de estudio externo, mantiene el tiempo fuera no académico aparte del denominador de concentración y muestra las interrupciones por hora correspondientes

#### Scenario: Desglose de tópicos de pausas e interrupciones
- **GIVEN** que el estudiante registró pausas y salidas no académicas con tópicos como «Descanso / comida», «Llamada / mensaje» y «Sin clasificar», además de estudio externo con «PDF / Moodle»
- **WHEN** abre Estadísticas > Concentración
- **THEN** el sistema presenta el desglose agrupado de pausas e interrupciones con cantidad y duración por tópico (excluyendo el estudio externo, visible en su bloque aparte)

#### Scenario: Exclusión de sesiones sin plan y registros tardíos
- **GIVEN** que en el periodo hubo sesiones iniciadas con «Estudiar ahora» (sin plan) y sesiones en estado Completada (registro tardío)
- **WHEN** se calculan puntualidad, respeto del tiempo y concentración
- **THEN** todas suman horas reales y cumplimiento, las sin plan se excluyen solo de puntualidad promedio y respeto del tiempo, y los registros tardíos (sin eventos de ejecución) se excluyen de concentración y puntualidad
