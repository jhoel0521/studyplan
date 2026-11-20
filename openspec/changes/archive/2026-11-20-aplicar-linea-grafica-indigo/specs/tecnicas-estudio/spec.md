## MODIFIED Requirements

### Requirement: HU-47 Estudiar ahora sin planificar
Como estudiante, quiero empezar a estudiar una materia en el momento, sin haber planificado la sesión, para registrar el estudio que surge sin aviso. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 5 pts; propuesta de la IA, aceptada por el Product Owner).

#### Scenario: Acceso desde Inicio
- **GIVEN** que el estudiante está en la pantalla de inicio y no hay una sesión En curso
- **WHEN** pulsa el botón flotante «▶ Estudiar ahora»
- **THEN** el sistema le pide la materia, con «Estudios varios» y «Nueva materia» entre las opciones, y la técnica de estudio, y la sesión no empieza hasta que pulsa «▶ Comenzar»

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
- **WHEN** el estudiante pulsa el botón flotante de la pantalla de inicio
- **THEN** el sistema no crea otra ni pide materia: el botón dice «Volver a la sesión» y abre la sesión En curso

#### Scenario: Materia elegida visible
- **GIVEN** que el estudiante abrió «Estudiar ahora»
- **WHEN** toca una materia de la lista
- **THEN** esa fila queda marcada con su indicador y su fondo, las demás se desmarcan, y el botón «▶ Comenzar», desactivado mientras no haya materia elegida, se activa
