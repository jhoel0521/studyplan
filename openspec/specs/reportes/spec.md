# reportes Specification

## Purpose
Genera reportes de un periodo con horas planificadas frente a reales y permite exportarlos.

## Requirements

### Requirement: HU-32 Generar reporte de un periodo
Como estudiante, quiero generar un reporte semanal o mensual con horas planificadas, reales y cumplimiento por materia para evaluar mi desempeño con objetividad. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Reporte
- **GIVEN** que existen sesiones en el periodo
- **WHEN** el estudiante genera el reporte semanal
- **THEN** el sistema muestra por materia: horas planificadas, reales, cumplimiento (`reales / planificadas × 100`) y sesiones no realizadas, más los totales del periodo

#### Scenario: Vista mensual y navegación de periodos
- **GIVEN** que el estudiante está en la pantalla de Reportes
- **WHEN** cambia entre Semana y Mes o navega al periodo anterior/siguiente
- **THEN** el sistema recalcula todas las filas por materia y los totales para el nuevo periodo aplicando el corte diario estricto en medianoche (`RN-31`) tanto en horas reales como en horas planificadas que cruzan medianoche

#### Scenario: Materias con horas planificadas o sesiones no realizadas sin horas reales
- **GIVEN** que una materia tuvo sesiones planificadas o sesiones en estado No realizada durante el periodo pero suma `0` minutos reales
- **WHEN** se genera el reporte del periodo
- **THEN** esa materia aparece en la tabla con sus horas planificadas, `0.0 h` reales, `0 %` de cumplimiento (cuando tuvo horas planificadas `> 0`) y su conteo de sesiones no realizadas, sin ser omitida

#### Scenario: Atribución única de sesiones no realizadas que cruzan medianoche
- **GIVEN** que una sesión en estado No realizada cruza la medianoche entre el último día de un periodo y el primer día del siguiente
- **WHEN** se generan los reportes de ambos periodos consecutivos
- **THEN** la sesión no realizada se contabiliza únicamente en el periodo correspondiente a su fecha de inicio, sin duplicar el conteo entre periodos

#### Scenario: Sesiones sin plan, reprogramadas, pendientes, Estudios varios y materias archivadas
- **GIVEN** que en el periodo existen horas reales en «Estudios varios» o en una materia archivada, sesiones iniciadas con «Estudiar ahora» (sin plan), sesiones Pendientes del periodo o sesiones Reprogramadas
- **WHEN** se genera el reporte
- **THEN** «Estudios varios» y las materias archivadas con actividad aparecen en el reporte, las sesiones sin plan suman reales sin sumar planificadas (`cumplimiento = —` cuando la materia queda con `0 h` planificadas, sin división por cero), las sesiones Reprogramadas no duplican horas planificadas (`RN-05`) y las sesiones que ocupan horario (incluidas las Pendientes vigentes del periodo) suman sus horas planificadas coherentes con el Calendario

#### Scenario: Sin datos
- **GIVEN** que ninguna materia registra minutos reales, horas planificadas ni sesiones no realizadas en el periodo visible (incluyendo periodos con solo sesiones Reprogramadas o sesiones sin plan detenidas con `0` minutos)
- **WHEN** genera el reporte
- **THEN** el sistema informa que no hay datos y deshabilita las acciones de exportación manteniendo activa la navegación de periodos

#### Scenario: Acceso desde Estadísticas e Inicio
- **GIVEN** que el estudiante consulta Estadísticas en un periodo semanal o mensual o se encuentra en Inicio
- **WHEN** pulsa el botón «Reportes» en Estadísticas o el acceso de Reportes en la barra superior de Inicio
- **THEN** se abre Reportes directamente sobre ese periodo (o la semana actual desde Inicio), conservando las cuatro pestañas fijas de la barra inferior

### Requirement: HU-33 Exportar y compartir reporte
Como estudiante, quiero exportar el reporte en PDF o CSV y compartirlo para guardarlo o mostrarlo a un tutor. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: PDF
- **GIVEN** que hay un reporte generado con datos
- **WHEN** el estudiante pulsa Exportar PDF
- **THEN** el sistema genera localmente el archivo PDF del reporte (encabezado del periodo, fecha de generación, tabla por materia y fila de totales con soporte multipágina) sin conexión a internet ni permisos externos de almacenamiento y abre el selector para compartir de Android con permiso de lectura temporal

#### Scenario: CSV
- **GIVEN** que hay un reporte generado con datos
- **WHEN** pulsa Exportar CSV
- **THEN** el sistema genera un CSV en UTF-8 con una fila directa de encabezados con unidades, una fila por cada materia del reporte y la fila final de totales, y abre el selector para compartir de Android

#### Scenario: Compatibilidad del CSV con Google Sheets y Excel para Windows
- **GIVEN** que el reporte contiene nombres de materias con tildes, eñes, comas, comillas o prefijos interpretables como fórmulas y métricas numéricas
- **WHEN** se genera el archivo CSV
- **THEN** el archivo inicia con prefijo BOM UTF-8 (`\uFEFF`), utiliza coma `,` como delimitador RFC 4180, decimales con punto `.` (`Locale.ROOT`), escape entre comillas dobles cuando corresponde, neutralización de celdas de texto con posible inyección de fórmulas y fin de línea `CRLF` (`\r\n`), abriendo columnas y caracteres UTF-8 directamente en Google Sheets e importándose limpio mediante el asistente de texto/CSV en Excel para Windows

#### Scenario: Exportación inactiva sin datos
- **GIVEN** que el periodo visible no tiene datos para reportar
- **WHEN** se muestra la pantalla de Reportes
- **THEN** las opciones Exportar PDF y Exportar CSV permanecen deshabilitadas hasta cambiar a un periodo con actividad
