# Spec Delta: `sesiones-estudio`

## MODIFIED Requirements

### Requirement: RN-07 Máquina de estados de la sesión
El estado de una sesión SHALL cambiar únicamente por las transiciones listadas en los escenarios; cada estado conoce solo sus acciones y su estado siguiente. Los estados finales (Completada, Completada (registro tardío), Reprogramada) MUST NOT admitir transiciones.

#### Scenario: Pendiente → En curso (iniciar)
- **GIVEN** una sesión en estado Pendiente
- **WHEN** ocurre el evento «iniciar»
- **THEN** la sesión pasa a En curso — Inicia cronómetro o Pomodoro

#### Scenario: En curso → En curso (pausar / reanudar)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «pausar / reanudar»
- **THEN** la sesión pasa a En curso — El tiempo pausado no suma

#### Scenario: En curso → Completada (detener)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «detener»
- **THEN** la sesión pasa a Completada — Guarda la duración real medida

#### Scenario: Pendiente → Completada (marcar completada)
- **GIVEN** una sesión en estado Pendiente del mismo día
- **WHEN** el estudiante desea completarla en el flujo normal
- **THEN** el sistema no permite completarla directamente con minutos editables; debe pasar por «iniciar» (En curso) y luego «detener» para calcular la duración real desde los eventos registrados

#### Scenario: Pendiente → No realizada (fin del día)
- **GIVEN** una sesión en estado Pendiente
- **WHEN** ocurre el evento «fin del día»
- **THEN** la sesión pasa a No realizada — Automático (RN-08)

#### Scenario: Pendiente → Reprogramada (reprogramar)
- **GIVEN** una sesión en estado Pendiente
- **WHEN** ocurre el evento «reprogramar»
- **THEN** la sesión pasa a Reprogramada — Crea una nueva sesión Pendiente enlazada (RN-09)

#### Scenario: No realizada → Reprogramada (reprogramar)
- **GIVEN** una sesión en estado No realizada
- **WHEN** ocurre el evento «reprogramar»
- **THEN** la sesión pasa a Reprogramada — Crea una nueva sesión Pendiente enlazada (RN-09)

#### Scenario: No realizada → Completada (registro tardío) (registrar tardío)
- **GIVEN** una sesión en estado No realizada
- **WHEN** ocurre el evento «registrar tardío»
- **THEN** la sesión pasa a Completada (registro tardío) — Solo si la fecha es de hace 3 días o menos (RN-10)

#### Scenario: Nueva → Completada (registro tardío) (registrar sesión pasada)
- **GIVEN** una sesión nueva
- **WHEN** ocurre el evento «registrar sesión pasada»
- **THEN** la sesión pasa a Completada (registro tardío) — Fecha de hace 3 días o menos (RN-06)

#### Scenario: En curso → Completada (cambiar a la siguiente sesión)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «cambiar a la siguiente sesión»
- **THEN** la sesión pasa a Completada — La siguiente sesión pasa a En curso sin detener el tiempo (RN-27)

#### Scenario: En curso → Completada (declarar fin al volver)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «declarar fin al volver»
- **THEN** la sesión pasa a Completada — El tramo declarado es «estudio con la app cerrada» (RN-24)

#### Scenario: En curso → Completada (plazo de declaración vencido)
- **GIVEN** una sesión en estado En curso
- **WHEN** ocurre el evento «plazo de declaración vencido»
- **THEN** la sesión pasa a Completada — Cierre en la última marca dentro de la app (RN-29)

#### Scenario: Estado final sin transiciones
- **GIVEN** una sesión en estado Completada, Completada (registro tardío) o Reprogramada
- **WHEN** se intenta cualquier evento de cambio de estado
- **THEN** el sistema rechaza el cambio y la sesión conserva su estado

### Requirement: HU-12 Completar una sesión
Como estudiante, quiero completar mi sesión Pendiente del día ejecutándola en vivo para registrar el tiempo real estudiado sin introducir duraciones manuales arbitrarias. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Completar
- **GIVEN** que existe una sesión Pendiente de hoy
- **WHEN** el estudiante abre su detalle
- **THEN** el sistema no muestra campo editable de minutos ni botón de completado directo; la sesión se completa iniciándola (En curso) y deteniéndola (o declarando fin al volver) para calcular la duración real desde los eventos (RN-07)

#### Scenario: Transición inválida
- **GIVEN** que una sesión está Reprogramada
- **WHEN** el estudiante intenta completarla
- **THEN** el sistema no muestra esa acción, porque el estado no la permite (RN-07)
