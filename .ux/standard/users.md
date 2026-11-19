# Users

> Fuente: documento del proyecto (cap. 1 y 3: RN-01, HU-xx). Lo no documentado queda `{{to confirm}}` hasta entrevistar usuarios reales (ver user-questions.md).

## Profiles
### estudiante — Estudiante universitario
- **Who**: único rol de la v0.1.0 (RN-01); un usuario por dispositivo, sin cuentas. Nivel de experiencia: {{to confirm}}
- **Context of use**: autocontrol del estudio con la app abierta al planificar y durante la sesión; puede salir de la app en medio de una sesión (RN-23). Entorno (biblioteca, casa, transporte): {{to confirm}}
- **Devices**: teléfono Android (minSdk 24); solo móvil en este cambio.
- **Frequent tasks** (más frecuente primero): iniciar una sesión de estudio (cronómetro o Pomodoro) · ver el progreso de la semana · planificar sesiones en el calendario · revisar estadísticas · gestionar materias · registrar una sesión pasada (máx. 3 días). Orden real de frecuencia: {{to confirm}}
- **Pain points today**: {{to confirm}} (pendiente de entrevistas)
- **Special needs**: modo oscuro y claro, español/inglés, sin conexión (offline-first), bloqueo con huella o PIN, accesibilidad RNF-06 (no depender solo del color, TalkBack).
- **Device skill**: usa el teléfono a diario; densidad `comfortable` confirmada por el Product Owner (2026-11-19).
- **Apps they use** (`config.yaml → apps[].users`): studyplan

## Glossary (one term per concept)
| UI term | Meaning | Do not use |
|---|---|---|
| Sesión | bloque de estudio planificado o registrado | Clase, Tarea |
| Materia | asignatura que el estudiante cursa | Curso, Asignatura |
| Meta semanal | horas objetivo de la semana | Objetivo, Cuota |
| Descanso | pausa dentro de Pomodoro | Break |
| No realizada | sesión vencida sin completar (RN-07) | Vencida, Fallida, Perdida |

Fuente de los términos: strings.xml y specs del proyecto; revisar con el equipo.

## Product principles (en orden de prioridad)
1. Empezar a estudiar en pocos toques (Estudiar ahora): el flujo principal nunca se esconde.
2. Nunca perder el registro de estudio del usuario (offline-first, recuperación al volver, RN-23).
3. Estados siempre legibles sin depender del color: icono + etiqueta (RNF-06).
4. Calma visual para concentrarse: un solo acento, sin ruido.
