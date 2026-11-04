# StudyPlan

Aplicación Android nativa (Java) para gestionar y planificar el tiempo de estudio: materias,
sesiones con máquina de estados, calendario de estudio (día, semana y mes), Pomodoro, eventos de
estudio (tiempo extra, estudio con la app cerrada), metas semanales, estadísticas, reportes y
respaldo en Google Drive. Funciona offline-first.

| | |
|---|---|
| Tipo | Proyecto académico de la materia Aplicaciones Móviles (datos de autor y docente en `docs/build/.env`, no versionado) |
| Package | `io.github.jhoel0521.studyplan` · minSdk 24 · versión objetivo 0.1.0 |
| Metodología | Scrum + Spec-Driven Development con [OpenSpec](https://github.com/Fission-AI/OpenSpec) |

## Estado

| Entrega | Contenido | Estado |
|---|---|---|
| 1 | Cap. 1 a 3.2.2 (introducción, marco teórico, metodología y requisitos) + Auditoría IA | En revisión |

Requisitos de la línea base: 45 historias de usuario, 30 reglas de negocio y 19 requisitos no
funcionales (ISO/IEC 25010:2023), especificados en
[`openspec/changes/add-requisitos-base`](openspec/changes/add-requisitos-base).

## Estructura

```
app/                 código Android (Java)
openspec/            specs vigentes (specs/) y cambios en curso (changes/)
docs/build/          requisitos.py (fuente única) y generadores de specs, diagramas y Word
docs/diagramas/      modelo de dominio, máquina de estados, ciclo Scrum+SDD, wireframes W1–W15
docs/auditoria-ia/   bitácora del uso de IA
docs/entrega*/       documentos finales generados (ignorados por git)
AGENTS.md CLAUDE.md  reglas para agentes de IA · .claude/ .agents/ comandos, skills y subagentes
```

## Regenerar la documentación

```bash
python docs/build/gen_specs.py                    # requisitos.py -> specs de OpenSpec
openspec validate add-requisitos-base --strict
python docs/build/diagramas.py                    # diagramas y wireframes (Pillow)
cp docs/build/.env.example docs/build/.env      # completar portada (autor, materia, docente...)
python docs/build/generar_docx.py                 # ¡sobrescribe docs/entrega1/StudyPlan_Entrega1.docx!
```

Los documentos finales (`docs/entrega*/`, `*.docx`, `*.pdf`) y `docs/build/.env` **no se versionan**.
Sin `.env`, la portada sale con marcadores genéricos en amarillo.

> El documento de la Entrega 1 ya tiene ediciones manuales del autor (logo, portada y ajustes).
> **No vuelvas a ejecutar `generar_docx.py` sobre él**; úsalo solo como base de las siguientes entregas.
> En Word, pulsa Ctrl+A y luego F9 para actualizar el índice y las listas de tablas y figuras.

## Auditoría de IA

Por indicación del docente, **todo uso de IA se documenta**. La *Auditoría IA* es un punto especial
que se agrega al índice de **cada entrega**, con estructura fija (A.1 a A.6): declaración de uso,
protocolo humano–IA, sistema OpenSpec, bitácora de decisiones, riesgos y mitigación, y reflexión.

| Herramienta | Uso |
|---|---|
| Claude Code (Anthropic), modelo Claude Opus 5.5 | Entrevista de levantamiento de requisitos, redacción de specs y documentación, scripts de generación |
| OpenSpec 1.14.3 (Fission-AI) | Estructura y validación de especificaciones (SDD) |

- Bitácora acumulativa (cada sesión agrega filas, nunca reescribe las anteriores):
  [docs/auditoria-ia/bitacora.md](docs/auditoria-ia/bitacora.md). Cada fila registra solicitud,
  resultado de la IA, decisión humana y verificación.
- Los errores de la IA (requisitos inventados, supuestos no declarados) se registran siempre: son
  evidencia de control humano.
- Toda referencia, DOI, versión o métrica propuesta por la IA se verifica a mano antes de entregar.
- Regla del proyecto: **la IA no inventa requisitos; si falta información, pregunta.** Ver [AGENTS.md](AGENTS.md).
- Los commits son de autoría del estudiante; la IA se declara aquí y en la bitácora, no como coautora.
