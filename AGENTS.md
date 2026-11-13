# AGENTS.md — Reglas para agentes de IA en StudyPlan

Este archivo lo lee **cualquier** asistente de IA (Claude Code, Codex, Gemini, Copilot…)
antes de trabajar en el repositorio (`CLAUDE.md` solo remite aquí: hay una única fuente de reglas).
Las decisiones aquí escritas son **humanas** y la IA
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
- Mensajes de commit: `feat(HU-07): ...`, `test(HU-07): ...`, `docs: ...`. **Sin coautor ni líneas de atribución de IA** (la IA se declara en el README y la bitácora).
- No hacer `git commit` ni `git push` sin pedido explícito. Agregar archivos por ruta, no con `git add -A`.

## 5. Auditoría IA (exigida por el docente)
- La *Auditoría IA* es un **punto especial que se agrega al índice por indicación del docente** en
  **cada** entrega. Nunca escribir que "no forma parte del documento/índice".
- Estructura fija: A.1 Declaración de uso (herramienta, modelo, periodo) · A.2 Protocolo humano–IA ·
  A.3 Sistema OpenSpec · A.4 Bitácora de decisiones · A.5 Riesgos y mitigación · A.6 Reflexión.
- Cada sesión con IA agrega filas a `docs/auditoria-ia/bitacora.md`: solicitud · resultado de la IA ·
  decisión humana (Aceptado / Modificado / Rechazado + justificación) · **verificación**.
  Las entregas posteriores **amplían** la bitácora, no reescriben las anteriores.
- Registrar siempre los errores de la IA (requisitos inventados, supuestos no declarados).
- Toda referencia, DOI, versión o métrica propuesta por la IA se marca para verificación manual.
- Mantener coherencia de la mención en portada, 1.4 Alcance y 2.2.7 (herramientas de IA).
- La IA **propone**, el humano **decide**.

## 6. Calidad mínima antes de dar una tarea por terminada
- Compila: `./gradlew assembleDebug`.
- Pruebas: `./gradlew test` (y `connectedAndroidTest` cuando aplique).
- Lint sin errores: `./gradlew lint`.
- No accedas a la base de datos desde el hilo principal.
- Toda entrada de usuario se valida (por ejemplo, hora fin > hora inicio).

## 7. Estructura y documento académico
El documento (entrega) se escribe en Markdown y se genera con **apa-kit** (skills `apa-*` en `.claude/skills/` y
`.agents/skills/`, motor en `.apa/scripts/`). Los `.md` son la fuente; nunca se edita el `.docx` desde la IA.

- Generar: `python .apa/scripts/generar_docx.py --doc docs/documento` (salida en `docs/documento/dist/`).
- Revisar: `python .apa/scripts/revisar.py --doc docs/documento`. Estado: `--estado`.
- Capítulos en `docs/documento/capitulos/*.md`; figuras en `docs/documento/recursos/`; opciones en `documento.toml`.
- Los diagramas se regeneran con `python docs/build/diagramas.py` (salida en `docs/documento/recursos/diagramas/`).
- Datos personales (autor, docente, universidad) solo en `docs/documento/.env` (ignorado); en los `.md`, `{{CLAVE}}`.
- No se versionan `.env`, `dist/`, `original/`, `*.docx` ni `*.pdf`. No editar el Word que el autor ya modificó a mano.
- No editar `.apa/scripts/` ni las skills instaladas; si apa-kit falla, se reporta en su repositorio.

```
app/                 código Android (Java)
openspec/            specs vigentes (specs/) y cambios en curso (changes/)
docs/documento/      documento de la entrega: capitulos/, recursos/, documento.toml, .env, dist/
docs/auditoria-ia/   bitácora de auditoría IA
docs/build/          scripts propios (diagramas.py)
.apa/                motor de apa-kit
.agents/  .claude/   skills (compartidas / de Claude Code) y subagentes de Claude Code
```

## 8. Reglas de comportamiento para los agentes
- Responde en español.
- Nunca cambies `applicationId`, `namespace`, el lenguaje (Java) ni minSdk.
- Antes de implementar, comprueba que existe un change aprobado en `openspec/changes/`.
- Al terminar una sesión, anota en la bitácora qué pidió el usuario, qué generaste y qué quedó pendiente de revisión humana.

## 9. Comandos y subagentes
- OpenSpec: `/opsx:explore` (explorar sin código) · `/opsx:propose "<idea>"` (proposal, specs, design y tasks) ·
  `/opsx:apply` (implementar un change aprobado) · `/opsx:archive` (archivar un change terminado).
- Subagentes de Claude Code (`.claude/agents/`): `analista-requisitos` (HU, criterios y RNF) · `revisor-java`
  (Java/Android contra MVVM, Room y specs) · `auditor-ia` (bitácora y trazabilidad).
