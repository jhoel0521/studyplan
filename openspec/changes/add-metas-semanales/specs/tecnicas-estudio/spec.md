# Spec Delta: `tecnicas-estudio`

## ADDED Requirements

### Requirement: HU-47 Estudiar ahora sin planificar
Como estudiante, quiero empezar a estudiar una materia en el momento, sin haber planificado la sesión, para registrar el estudio que surge sin aviso. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts; propuesta de la IA, aceptada por el Product Owner).

#### Scenario: Acceso desde Inicio
- **GIVEN** que el estudiante está en la pantalla de inicio y no hay una sesión En curso
- **WHEN** pulsa el botón flotante «▶ Estudiar ahora»
- **THEN** el sistema le pide la materia, con «Estudios varios» y «Nueva materia» entre las opciones, y la técnica de estudio

#### Scenario: Empezar sin plan
- **GIVEN** que el estudiante eligió «Matemáticas» y la materia no tiene sesiones Pendientes hoy
- **WHEN** confirma
- **THEN** el sistema crea una sesión de Matemáticas sin horario planificado y la pone En curso en ese instante (RN-07)

#### Scenario: Sin materias registradas
- **GIVEN** que el estudiante no registró ninguna materia
- **WHEN** usa «Estudiar ahora» con «Estudios varios»
- **THEN** la sesión empieza En curso y sus horas suman a la meta total (RN-32)

#### Scenario: Materia nueva en el mismo paso
- **GIVEN** que el estudiante quiere estudiar «Oratoria», que no está registrada
- **WHEN** elige «Nueva materia», escribe el nombre, elige un color y confirma
- **THEN** el sistema registra la materia (RN-02) y la sesión empieza En curso

#### Scenario: La materia tiene una sesión Pendiente hoy
- **GIVEN** que Inglés tiene una sesión Pendiente hoy de 11:00 a 12:00 y son las 10:40
- **WHEN** el estudiante elige Inglés en «Estudiar ahora»
- **THEN** el sistema pregunta «¿Es tu sesión de Inglés de las 11:00?»; si responde que sí, inicia esa sesión, y si responde que no, crea una sesión sin plan y la de las 11:00 sigue Pendiente

#### Scenario: Sin plan no cuenta como planificado
- **GIVEN** que una sesión sin plan quedó Completada con 50 minutos reales
- **WHEN** el estudiante consulta el resumen del calendario
- **THEN** los 50 minutos suman en «Real» y no suman en «Plan»

#### Scenario: Sin plan no bloquea cruces
- **GIVEN** que hay una sesión sin plan En curso desde las 10:40
- **WHEN** llega la hora de la sesión Pendiente de Inglés de las 11:00
- **THEN** el sistema avisa y deja seguir o cambiar como con cualquier sesión En curso (RN-27), sin tratarlo como cruce de horario (RN-05)

#### Scenario: Ya hay una sesión En curso
- **GIVEN** que ya existe una sesión En curso
- **WHEN** el estudiante pulsa «▶ Estudiar ahora»
- **THEN** el sistema no crea otra y abre la sesión En curso

### Requirement: HU-48 Cambiar de materia durante la sesión
Como estudiante, quiero pasar a estudiar otra materia sin detener el tiempo para reflejar que cambié de tema, por ejemplo al llegar compañeros de otra asignatura. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts; propuesta de la IA, aceptada por el Product Owner).

#### Scenario: Cambiar a una materia sin sesión Pendiente
- **GIVEN** que Matemáticas está En curso y la materia Informática no tiene sesiones Pendientes hoy
- **WHEN** el estudiante elige «Cambiar de materia» e Informática
- **THEN** Matemáticas pasa a Completada en ese instante y una sesión sin plan de Informática pasa a En curso sin detener el tiempo (RN-07, RN-27)

#### Scenario: Cambiar a la sesión planificada
- **GIVEN** que Matemáticas está En curso a las 11:20 e Inglés tiene una sesión Pendiente hoy de 11:00 a 12:00
- **WHEN** el estudiante elige «Cambiar de materia» e Inglés
- **THEN** el sistema pregunta «¿Es tu sesión de Inglés de las 11:00?»; si responde que sí, Matemáticas pasa a Completada e Inglés pasa a En curso conservando su horario planificado

#### Scenario: Cambiar a una materia nueva o a Estudios varios
- **GIVEN** que una sesión está En curso
- **WHEN** el estudiante elige «Cambiar de materia» y luego «Nueva materia» o «Estudios varios»
- **THEN** el sistema registra la materia nueva si hace falta y la sesión de esa materia pasa a En curso sin detener el tiempo

#### Scenario: Misma materia
- **GIVEN** que Matemáticas está En curso
- **WHEN** el estudiante abre «Cambiar de materia»
- **THEN** el sistema no ofrece Matemáticas como destino
