# Spec Delta — Reportes (E8)

## Purpose

Genera reportes de un periodo con horas planificadas frente a reales y permite exportarlos.

## ADDED Requirements

### Requirement: HU-32 Generar reporte de un periodo
Como estudiante, quiero generar un reporte semanal o mensual con horas planificadas, reales y cumplimiento por materia para evaluar mi desempeño con objetividad. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: Reporte
- **GIVEN** que existen sesiones en el periodo
- **WHEN** el estudiante genera el reporte semanal
- **THEN** el sistema muestra por materia: horas planificadas, reales, cumplimiento (reales / planificadas × 100) y sesiones no realizadas, más los totales

#### Scenario: Sin datos
- **GIVEN** que no hay sesiones en el periodo
- **WHEN** genera el reporte
- **THEN** el sistema informa que no hay datos

### Requirement: HU-33 Exportar y compartir reporte
Como estudiante, quiero exportar el reporte en PDF o CSV y compartirlo para guardarlo o mostrarlo a un tutor. El sistema SHALL cumplir los escenarios siguientes (prioridad Should, 5 pts).

#### Scenario: PDF
- **GIVEN** que hay un reporte generado
- **WHEN** el estudiante elige Compartir > PDF
- **THEN** el sistema crea el PDF y abre el selector para compartir de Android

#### Scenario: CSV
- **GIVEN** que hay un reporte generado
- **WHEN** elige Compartir > CSV
- **THEN** el sistema genera un CSV en UTF-8 con una fila por materia
