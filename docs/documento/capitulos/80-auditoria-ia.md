# Auditoría IA {.sin-numero}

Esta sección responde a la indicación del docente de que todo uso de inteligencia artificial (IA) en el proyecto debe quedar documentado. Por eso se agrega al índice como un punto especial y forma parte de este documento, con el mismo peso que los capítulos del proyecto. Aquí se declara con qué herramientas se trabajó (A.1), qué límites se fijaron (A.2), cómo se controló el flujo de trabajo (A.3), **qué hizo la IA**, **qué decidió el estudiante** y **cómo se verificó cada resultado** (A.4), qué riesgos se identificaron (A.5) y qué aprendizaje deja la experiencia (A.6). Su propósito es doble: que el estudiante pueda defender cada artefacto generado y que el docente pueda auditar el proceso.

## A.1. Declaración de uso de IA {.sin-numero}

Tabla: Herramientas de IA utilizadas {#tabla22}

| Herramienta | Versión / modelo | Uso | Periodo |
|---|---|---|---|
| Claude Code (Anthropic) | Modelo Claude Opus 5.5 | Asistente agéntico: entrevista de requisitos, redacción, specs, scripts y documentación | Desde el 04/11/2026 |
| OpenSpec (Fission-AI) | CLI 1.14.3, esquema spec-driven | Estructura y validación de especificaciones (SDD) | Desde el 04/11/2026 |

Nota: Todo contenido generado por IA fue revisado por el estudiante antes de la entrega. Las referencias bibliográficas propuestas por la IA deben verificarse una por una (DOI o URL) antes de la entrega final.

## A.2. Protocolo de trabajo humano–IA {.sin-numero}

El protocolo define límites claros entre lo que decide el humano y lo que ejecuta la IA. Está escrito en AGENTS.md (reglas para cualquier agente) y CLAUDE.md (reglas de Claude Code) en la raíz del repositorio, y se refuerza en openspec/config.yaml, que inyecta el contexto del proyecto en cada instrucción que OpenSpec entrega a la IA. A raíz del incidente descrito en A.4, se agregó la regla explícita: **la IA no inventa requisitos; si falta información, pregunta**.

Tabla: Reparto de responsabilidades entre el estudiante y la IA {#tabla23}

| Decide / hace el estudiante | Propone / ejecuta la IA |
|---|---|
| Definir requisitos, reglas de negocio y criterios de calidad | Conducir la entrevista y redactar a partir de las respuestas |
| Elegir stack (Java nativo), package name y metodología | Presentar alternativas con ventajas y desventajas |
| Aprobar o rechazar cada change de OpenSpec | Redactar proposal, specs, design y tasks |
| Revisar, aceptar y defender el código generado | Generar código Java y pruebas a partir de specs aprobadas |
| Verificar referencias, versiones y métricas | Ejecutar validaciones y registrar cada sesión en la bitácora |

Reglas no negociables para la IA: no cambiar el lenguaje (Java), el applicationId ni el minSdk; no agregar dependencias sin aprobación; no inventar requisitos; no hacer commits sin pedido explícito; y no implementar nada que no tenga un *change* aprobado.

## A.3. Sistema OpenSpec en el proyecto {.sin-numero}

OpenSpec se inicializó con el comando openspec init --tools claude,agents --language es. El comando creó la carpeta openspec/ (specs/ para las especificaciones vigentes y changes/ para los cambios en curso), el archivo openspec/config.yaml y las *skills* y comandos para Claude Code (.claude/) y para otros agentes (.agents/). La @tabla:tabla24 describe la estructura del repositorio.

Tabla: Estructura del repositorio StudyPlan {#tabla24}

| Ruta | Contenido |
|---|---|
| app/ | Módulo Android en Java (io.github.jhoel0521.studyplan), creado por el estudiante con Android Studio |
| openspec/config.yaml | Contexto y reglas del proyecto que OpenSpec entrega a la IA en cada artefacto |
| openspec/changes/add-requisitos-base/ | Change de esta entrega: proposal.md, design.md, tasks.md y 10 specs |
| openspec/specs/ | Especificaciones vigentes (se llenan al archivar cada change) |
| AGENTS.md · CLAUDE.md | Reglas para agentes de IA (comunes y propias de Claude Code) |
| .claude/agents/ | Subagentes: analista-requisitos, revisor-java, auditor-ia |
| .claude/commands/opsx/ · .claude/skills/ | Comandos /opsx:propose, apply, archive, explore, sync, update |
| .agents/skills/ | Las mismas skills de OpenSpec para otros agentes compatibles |
| docs/build/ | requisitos.py (fuente única), gen_specs.py, diagramas.py, generar_docx.py, actualizar_word.ps1 |
| docs/diagramas/ | Modelo de dominio, máquina de estados, ciclo Scrum+SDD y wireframes |
| docs/auditoria-ia/bitacora.md | Bitácora detallada de cada sesión con IA |
| docs/entrega1/ | Este documento |

El ciclo de vida de un requisito en OpenSpec es: (a) /opsx:propose crea un *change* con la propuesta (por qué), las especificaciones delta (qué), el diseño (cómo) y las tareas; (b) el humano revisa y aprueba; (c) /opsx:apply implementa las tareas y las marca como completadas; (d) openspec validate --strict verifica el formato; y (e) /opsx:archive fusiona las especificaciones en openspec/specs/, que pasan a ser la fuente de verdad del sistema.

Cada historia de usuario se transforma en un *Requirement* y cada criterio de aceptación en un *Scenario*. La máquina de estados (RN-07) también se especifica como *Requirement*, con un escenario por transición. Ejemplo de specs/sesiones-estudio/spec.md:

```
### Requirement: HU-10 Planificar sesión de estudio
Como estudiante, quiero registrar una sesión con materia, fecha, ... El sistema SHALL
cumplir los escenarios siguientes (prioridad Must, 5 pts).
#### Scenario: Cruce bloqueado
- **GIVEN** que existe una sesión de 10:00 a 11:30 ese día
- **WHEN** el estudiante registra otra de 11:00 a 12:00 el mismo día
- **THEN** el sistema bloquea el registro e indica con qué sesión se cruza (RN-05)
```

Evidencia de validación: openspec validate add-requisitos-base --strict respondió «Change 'add-requisitos-base' is valid» tanto en la primera versión de los requisitos como en la versión corregida tras la entrevista.

## A.4. Bitácora de decisiones {.sin-numero}

La @tabla:tabla25 resume las decisiones de la primera sesión de trabajo. La más relevante es la número 8: la IA redactó 19 historias, reglas de dominio y requisitos no funcionales **sin preguntar** al Product Owner, y además asumió por su cuenta que la aplicación no usaría internet. El estudiante lo detectó y lo rechazó, y los requisitos se rehicieron mediante entrevista. La bitácora completa está en docs/auditoria-ia/bitacora.md.

Tabla: Resumen de la bitácora de la sesión del 4 de noviembre de 2026 {#tabla25}

| # | Solicitud / propuesta | Resultado de la IA | Decisión humana y justificación |
|---|---|---|---|
| 1 | Planificar la primera entrega a partir del índice, la consigna y el mockup | Plan de trabajo y preguntas sobre formato, stack y metodología | **Modificado.** El estudiante eligió Word APA, Scrum + SDD e incluir las HU |
| 2 | Elegir el stack tecnológico | Ofreció React Native, Flutter, Tauri o Laravel | **Rechazado.** El docente pidió una app nativa; se eligió Java porque se enseña en clase |
| 3 | Incluir HU y casos de uso | Recomendó HU con criterios de aceptación | **Aceptado con restricción.** Por advertencia del docente, no se mezclan HU con casos de uso |
| 4 | Estructura del proyecto | Propuso una carpeta de documentación separada | **Modificado.** La raíz es el proyecto Android Studio, con docs/, openspec/, AGENTS.md y CLAUDE.md dentro |
| 5 | Package name para Google Play | Explicó que es permanente y propuso io.github.jhoel0521.studyplan | **Aceptado.** Basado en la cuenta de GitHub del estudiante, que creó el proyecto en Android Studio |
| 6 | Inicializar OpenSpec | Ejecutó openspec init; la versión 1.14.3 usa config.yaml en lugar de project.md | **Aceptado.** El contexto del proyecto se escribió en config.yaml |
| 7 | Generar diagramas | Intentó descargar PlantUML; la política de permisos lo bloqueó | **Aceptado.** Diagramas generados con Pillow, reproducibles con diagramas.py |
| 8 | Redactar requisitos (v1) | 19 HU, reglas y 16 RNF redactados sin consultar; supuso «sin internet» | **Rechazado.** «No me preguntaste reglas de dominio, requisitos funcionales y no funcionales». Se exigió entrevista |
| 9 | Entrevista de levantamiento | 5 rondas de preguntas (@tabla:tabla4) | **Decidido por el humano.** Offline-first con backup tipo WhatsApp, máquina de estados, metas total y por materia, etc. |
| 10 | Calendario de estudio | Vista mensual básica (HU-10 v1) | **Modificado.** El docente lo destacó: vistas de día, semana y mes, estados, creación, filtro (HU-16 a HU-21) |
| 11 | Escenarios de estudio real | La v2 solo decía «Detener guarda la duración real» | **Modificado.** El PO narró escenarios (inspiración, llamada, app cerrada, estudio en grupo, cambio de materia) que originaron HU-39 a HU-45 y RN-22 a RN-30 |
| 12 | Diseño del cronómetro | Preguntó si contar tiempo con un proceso en segundo plano | **Decidido por el humano.** «Registrar timestamps, mostrar el cronómetro solo en la app»; coincide con RNF-03 (batería) |
| 13 | Actualizar índice del Word | El script de automatización de Word (COM) se colgó dos veces | **Pendiente.** El estudiante actualiza los campos en Word (Ctrl+A, F9) |
| 14 | Requisitos v3 y documento | 45 HU, 30 RN, 19 RNF, 10 specs validadas | **Pendiente** de revisión humana (tareas 2.1 a 2.3 de tasks.md) |

La @tabla:bitacora_nov22 resume las sesiones del 22 y 23 de noviembre de 2026, dedicadas a la épica de respaldo y a la publicación en Google Play. Se incluyen los errores de la IA que el estudiante detectó: una corrección que no bastó, una acción sobre su teléfono sin permiso expreso y una afirmación que presentó como hecho sin comprobarla. El detalle completo está en las filas 165 a 169 de la bitácora del repositorio.

Tabla: Resumen de la bitácora de las sesiones del 22 y 23 de noviembre de 2026 {#bitacora_nov22 anchos=0.7,4,6,5.8 tam=9}

| # | Solicitud / propuesta | Resultado de la IA | Decisión humana y justificación |
|---|---|---|---|
| 1 | Implementar la épica 10: respaldo y privacidad | Auditó el plan, corrigió la especificación, implementó el núcleo, los adaptadores y las pantallas; 456 pruebas unitarias sin fallos | **Aceptado.** El estudiante aprobó las dependencias, guardar la contraseña con Android KeyStore y el respaldo apagado por defecto |
| 2 | Vincular la cuenta de Google en el teléfono | El primer arreglo (pedir el alcance del correo) no bastó; el segundo (consultar el correo con el token de acceso) funcionó | **Aceptado con corrección.** El estudiante probó y reportó el fallo; la IA había supuesto la causa sin comprobarla |
| 3 | Probar respaldo y restauración con la cuenta real | La IA no pudo hacerlo | **Hecho por el humano.** El estudiante borró sus datos en el teléfono y los restauró desde Drive |
| 4 | Actualizar la app en el teléfono | Copió la base y los ajustes, reinstaló y restauró los datos | **Aceptado con reserva.** La IA actuó sobre el teléfono más allá de lo pedido; el estudiante fijó la regla de no operarlo sin permiso expreso |
| 5 | Firma y release | Comprobó que existen los cuatro secretos y, por orden del estudiante, borró el release anterior y creó otro sobre el commit con la épica | **Decidido por el humano.** La clave de subida la creó el estudiante en otra computadora |
| 6 | Crear la app y completar las declaraciones de Play Console | Operó la consola con confirmación en cada paso sensible; 11 declaraciones guardadas | **Decidido por el humano** en detalles de acceso, público objetivo y seguridad de los datos |
| 7 | Video para el permiso de servicio en primer plano | Grabó en un emulador un recorrido de 6 min 36 s y lo subió a YouTube | **Decidido por el humano.** El estudiante eligió visibilidad pública |
| 8 | Explicar por qué el logo no aparecía en la consola | Afirmó, sin comprobarlo, que aparecería tras la aprobación de Google | **Rechazado.** El estudiante la corrigió: el logo aparece al guardar la ficha, no al aprobarse |

Nota: Elaboración propia a partir de la bitácora del repositorio.

## A.5. Riesgos del uso de IA y mitigación {.sin-numero}

Tabla: Riesgos identificados en el uso de IA y su mitigación {#tabla26}

| Riesgo | Ejemplo | Mitigación |
|---|---|---|
| Requisitos inventados | Ocurrió en esta entrega: la IA redactó HU, reglas y RNF sin preguntar | Entrevista obligatoria; regla en AGENTS.md; columna «Origen» en las reglas de negocio |
| Supuestos no declarados | Asumir «sin internet» cuando el usuario quería respaldo en la nube | Toda suposición se marca como «Derivada (confirmar)» y se pregunta |
| Alucinación de referencias o datos | Citas, DOI o versiones inexistentes | Verificación humana de cada referencia; solo fuentes con DOI o URL oficial |
| Desvío del stack | Generar Kotlin o Jetpack Compose por costumbre | Restricción en config.yaml y AGENTS.md; revisión con revisor-java |
| Código inseguro o de baja calidad | SQL concatenado, consultas en el hilo principal, cifrado débil | RNF-12, RNF-14 y RNF-15, Lint, StrictMode y revisión humana |
| Pérdida de autoría y comprensión | Entregar código que el estudiante no entiende | La Definition of Done exige que el estudiante explique el código; defensa oral |
| Actuar fuera de lo pedido en equipos o cuentas del estudiante | La IA abrió, cerró y reinstaló la app en el teléfono del estudiante sin permiso expreso | Regla: ningún toque, apertura ni instalación en el teléfono sin permiso expreso, y confirmación antes de cada acción hacia fuera (publicar, declarar, subir) |
| Afirmaciones sin comprobar presentadas como hechos | Decir que el logo de la consola dependía de la aprobación de Google | El estudiante verifica contra la consola y la IA registra sus errores en la bitácora (fila 169) |

## A.6. Reflexión: ¿qué diferencia a un programador en la era de la IA? {.sin-numero}

El docente plantea que, en la era de la IA y los agentes, cualquiera puede pedir código a una máquina, del mismo modo que un médico podría pedírselo. Esta primera entrega ofrece un ejemplo concreto de la respuesta. La IA produjo en minutos un conjunto de requisitos bien redactado, con formato correcto y validado por OpenSpec, pero **no era el sistema que el usuario quería**. Suponía una aplicación sin internet, sin respaldo, sin máquina de estados, sin Pomodoro y con un calendario mínimo. Detectar que faltaba preguntar, exigir la entrevista y tomar decisiones de dominio fue trabajo humano: cómo tratar una sesión no realizada, cómo conservar el registro o cuándo es válido un registro tardío. Lo mismo ocurrió con los escenarios de estudio real (inspirarse, olvidar el celular, estudiar en grupo): la IA no los había previsto, y fueron el origen de siete historias nuevas.

El valor del programador está en **especificar** qué debe hacer el sistema con criterios verificables, **validar** que lo generado cumple ese contrato y **defender** cada decisión técnica. Un médico con IA puede obtener código que compila, pero no sabría juzgar si responde al problema correcto. El desarrollo guiado por especificaciones convierte ese criterio profesional en un proceso explícito, auditable y repetible.
