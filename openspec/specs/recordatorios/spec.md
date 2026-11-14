# recordatorios Specification

## Purpose
Avisa al estudiante antes de que inicie una sesión planificada.

## Requirements

### Requirement: HU-34 Recibir recordatorio de sesión
Como estudiante, quiero recibir una notificación configurable (5, 15 o 30 minutos) antes de cada sesión para no olvidar mis sesiones planificadas. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Notificación
- **GIVEN** que existe una sesión Pendiente a las 10:00 con aviso de 15 minutos
- **WHEN** el reloj marca las 09:45
- **THEN** el sistema muestra una notificación con la materia y el horario, con acceso directo para iniciarla

#### Scenario: Permiso
- **GIVEN** que el teléfono usa Android 13 o superior y no se concedió el permiso de notificaciones
- **WHEN** el estudiante activa un recordatorio
- **THEN** el sistema explica para qué sirve y solicita el permiso

#### Scenario: Cancelación
- **GIVEN** que la sesión se reprogramó, eliminó o completó antes
- **WHEN** llega la hora del aviso original
- **THEN** no se muestra la notificación
