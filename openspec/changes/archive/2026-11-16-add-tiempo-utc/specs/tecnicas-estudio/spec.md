# Spec Delta: `tecnicas-estudio`

## MODIFIED Requirements

### Requirement: HU-39 Registrar eventos de estudio
Como estudiante, quiero que la app registre los eventos de mi sesión (inicio, pausas, salidas y regresos, tiempo extra, cambios, fin) para saber qué tan concentrado estuve y cuánto respeté mi tiempo de estudio. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts).

#### Scenario: Línea de tiempo
- **GIVEN** que una sesión terminó
- **WHEN** el estudiante abre su detalle
- **THEN** el sistema muestra la línea de tiempo de eventos con su hora y tipo

#### Scenario: Persistencia
- **GIVEN** que se registra un evento
- **WHEN** el sistema operativo termina la app justo después
- **THEN** el evento permanece guardado (RN-22)

#### Scenario: Hora del momento del evento
- **GIVEN** que un evento se registró a las 21:15 con el teléfono en UTC−04:00
- **WHEN** el estudiante viaja, el teléfono pasa a UTC+01:00 y abre el detalle de la sesión
- **THEN** la línea de tiempo sigue mostrando el evento a las 21:15 del mismo día, con la indicación de su zona UTC−04:00

#### Scenario: Cambio de horario durante la sesión
- **GIVEN** que una sesión está En curso cuando el reloj local se atrasa una hora por cambio de horario
- **WHEN** el estudiante la detiene
- **THEN** la duración real se calcula con el tiempo transcurrido, sin perder ni duplicar esa hora (RN-22)
