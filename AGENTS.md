# AGENTS.md — Reglas para agentes de IA en StudyPlan

Este archivo lo lee **cualquier** asistente de IA (Claude Code, Codex, Gemini, Copilot…)
antes de trabajar en el repositorio. Las decisiones aquí escritas son **humanas** y la IA
no puede cambiarlas por iniciativa propia.

## 1. Qué es el proyecto
StudyPlan es una app **Android nativa en Java** para que un estudiante gestione materias,
planifique sesiones de estudio, defina metas semanales y consulte reportes y estadísticas.
Proyecto académico con intención de publicarse en Google Play.

- Package / applicationId: `io.github.jhoel0521.studyplan` (**no cambiar nunca**).
- minSdk 24 · targetSdk 37 · Gradle Kotlin DSL · version catalog `gradle/libs.versions.toml`.

## 2. Stack fijado
| Capa | Tecnología | Prohibido |
|---|---|---|
| Lenguaje | Java | Kotlin, Compose |
| UI | Views XML + Material Design 3 | librerías UI no aprobadas |
| Arquitectura | MVVM: Activity/Fragment → ViewModel → Repository → DAO | lógica de negocio en Activities |
| Datos | Room (SQLite), offline-first, un usuario local | backend propio, Firebase |
| Respaldo | Google Drive `appDataFolder` + cifrado AES-256-GCM opcional | usar INTERNET para otra cosa |
| Gráficos | MPAndroidChart | |
| Recordatorios | WorkManager / AlarmManager | servicios en primer plano permanentes |

Si hace falta una dependencia nueva, **propónla y espera aprobación humana**.

## 3. Flujo de trabajo obligatorio: Spec-Driven Development (OpenSpec)
Ningún código entra sin una spec aprobada.

1. `/opsx:propose` (u `openspec new change <nombre>`) → `proposal.md`, `specs/`, `design.md`, `tasks.md`.
2. **Revisión humana** de la propuesta. Sin aprobación no se implementa.
3. `/opsx:apply` → implementar tarea por tarea marcando `- [x]` en `tasks.md`.
4. `openspec validate <change> --strict` debe pasar.
5. `/opsx:archive` → las specs pasan a `openspec/specs/` (fuente de verdad).

## 4. Trazabilidad
- Requisitos funcionales = Historias de Usuario `HU-xx`. Reglas de negocio = `RN-xx`. No funcionales = `RNF-xx` (ISO/IEC 25010).
- **Los requisitos los decide el humano (Product Owner).** La IA no inventa HU, RN ni RNF: si falta información, pregunta.
- **No usar casos de uso ni plantillas IEEE 830**: el proyecto es ágil y no mezcla métodos.
- Cada `### Requirement:` de una spec lleva su `HU-xx` en el título.
- Cada clase o test nuevo menciona en su Javadoc la HU que implementa.
- Mensajes de commit: `feat(HU-07): ...`, `test(HU-07): ...`, `docs: ...`.

## 5. Auditoría IA
Cada sesión con IA se registra en `docs/auditoria-ia/bitacora.md` así:
fecha · herramienta/modelo · prompt (resumen) · qué generó · decisión humana
(aceptado / modificado / rechazado) · justificación. La IA **propone** y el humano **decide**.

## 6. Calidad mínima antes de dar una tarea por terminada
- Compila: `./gradlew assembleDebug`.
- Pruebas: `./gradlew test` (y `connectedAndroidTest` cuando aplique).
- Lint sin errores: `./gradlew lint`.
- No accedas a la base de datos desde el hilo principal.
- Toda entrada de usuario se valida (por ejemplo, hora fin > hora inicio).

## 7. Estructura
```
app/                 código Android (Java)
openspec/            specs vigentes (specs/) y cambios en curso (changes/)
docs/                entregas académicas, diagramas, bitácora de auditoría IA
.claude/  .agents/   comandos, skills y subagentes de IA
```
