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
| Arquitectura | Hexagonal (puertos y adaptadores) en 3 capas: presentación (Activity/Fragment + ViewModel, MVVM) → núcleo (casos de uso + dominio, Java puro) ← infraestructura (Room, Drive, huella o PIN) | lógica de negocio en Activities; `android.*` en `domain` o `application`; que el núcleo dependa de un adaptador |
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
- Cada layout XML abre en el editor de diseño de Android Studio y declara `tools:context` de su Activity o Fragment; la clase enlaza sus vistas por id.
- Los eventos del ciclo de vida y los listeners (`onCreate`, `onClick`…) van en métodos con nombre, para que se vean en la revisión de código.
- Paquetes bajo `io.github.jhoel0521.studyplan`: `domain`, `application.port.in`, `application.port.out`, `application.service`, `adapter.in.ui`, `adapter.out.persistence`, `adapter.out.drive`, `adapter.out.security`.
- Toda entrada de usuario se valida (por ejemplo, hora fin > hora inicio).

### 6.1 Principios SOLID y Arquitectura Hexagonal en Java
- **SRP (Responsabilidad Única):** Cada caso de uso tiene su puerto de entrada (`application.port.in.<CasoDeUso>`) y su servicio (`application.service.<CasoDeUso>Service`). Las `Activity` solo enlazan vistas, observan `LiveData` y delegan al `ViewModel`.
- **OCP / LSP (Patrón State GoF):** El ciclo de vida de entidades con máquina de estados (por ejemplo `EstadoSesion` para `RN-07`) se implementa mediante polimorfismo de clases de estado en `domain`, evitando `switch`/`if` dispersos.
- **ISP (Segregación de Interfaces):** Los puertos de salida se segregan por responsabilidad (`MateriaRepository`, `SesionRepository`, `SesionesDeMateria`, `PreferenciasRepository`, `Reloj`).
- **DIP (Inversión de Dependencias):** `domain` y `application` son Java 100 % puro (cero imports de `android.*` o `androidx.*`, verificado por `ArquitecturaTest`). La obtención de fecha/hora actual u otras dependencias de sistema entran por puertos en `application.port.out` (como `Reloj`).

### 6.2 Checklist obligatorio de atributos XML (Layout Validation, Visual Lint y Accesibilidad)
Ningún layout nuevo o modificado (`activity_*.xml`, `contenido_*.xml`, `dialog_*.xml`, `item_*.xml`, `view_*.xml`) se da por válido sin cumplir todos estos puntos:
- **Nodo raíz:** declarar `xmlns:android`, `xmlns:app`, `xmlns:tools` (cuando aplique), `tools:context=".adapter.in.ui.NombreActivity"` y `tools:fitsSystemWindows="true"` para previsualización limpia en el editor de diseño.
- **Layouts incluidos (`contenido_*.xml` y `<include>`):** el nodo raíz de `contenido_*.xml` declara `android:layout_width="match_parent"` y `android:layout_height="match_parent"` para que su vista previa individual no colapse; la etiqueta `<include>` en el layout padre define obligatoriamente `android:id="@+id/..."`, `android:layout_width="0dp"`, `android:layout_height="0dp"` y las restricciones `app:layout_constraint*`.
- **Accesibilidad (`RNF-06`, TalkBack y Accessibility Scanner):**
  - Todo control táctil o interactivo (`MaterialButton`, `TextInputEditText`, `RadioButton`, `MaterialSwitch`, `CheckBox`, filas clicables, selectores) declara `android:minHeight="48dp"` (o altura explícita `>= 48dp`, y `android:minWidth="48dp"` en botones o iconos compactos).
  - Todo componente visual sin texto propio que transmita información o sea interactivo (`FloatingActionButton`, `ImageButton`, `LinearProgressIndicator`, selectores de color/brillo, celdas) declara `android:contentDescription="@string/..."`.
  - Todo `ImageView` o vista puramente decorativa acompañada de texto descriptivo declara `android:importantForAccessibility="no"`.
  - Ningún estado depende únicamente del color (`RNF-06`): siempre se acompaña de icono y etiqueta textual.
- **Contraste en vista de diseño (`tools:tint`):** cuando un `ImageView` utiliza un vector blanco tintado en tiempo de ejecución o por tema (como `@drawable/ic_circulo` o `@drawable/ic_check`), declarar `tools:tint="?attr/colorPrimary"` (o `tools:tint="?attr/colorOnPrimary"` según el fondo) para evitar falsos avisos de bajo contraste.
- **Límites de ancho en pantallas grandes (Visual Lint y `layout-w600dp/`):**
  - `TextInputLayout` y `TextInputEditText`: `android:layout_width="match_parent"`, `android:maxWidth="400dp"` y `android:layout_gravity="start"` (o ancho fijo acotado, p. ej. `150dp`, para códigos cortos).
  - `TextView` y `LinearProgressIndicator` dentro de `LinearLayout`: `android:layout_width="wrap_content"` + `android:maxWidth="600dp"`.
  - `TextView` y `LinearProgressIndicator` dentro de `ConstraintLayout` con `0dp`: `app:layout_constraintWidth_max="600dp"` y `app:layout_constraintHorizontal_bias="0"`.
  - `MaterialButton`, `RadioButton` y `MaterialSwitch`: `android:layout_width="wrap_content"` (nunca `match_parent` sin límite).
- **Datos de muestra (`tools:text`):** todo `TextView` poblado dinámicamente desde Java declara `tools:text="..."` representativo.

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
  (Java/Android contra la arquitectura hexagonal, Room y specs) · `auditor-ia` (bitácora y trazabilidad).
