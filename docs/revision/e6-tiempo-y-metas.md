# Revisión conjunta: `add-tiempo-utc` + `add-metas-semanales`

Documento índice para revisar juntas las dos propuestas OpenSpec de la épica E6 de StudyPlan. No agrega requisitos: los requisitos y el diseño están en los archivos enlazados, y esos son los que se revisan.

## Orden y dependencia

1. `add-tiempo-utc` se aplica y se archiva primero (Room v4 → v5).
2. `add-metas-semanales` se apoya en él (Room v5 → v6): usa `tramo_diario` y la regla `RN-31`, y su delta de `HU-21` parte del texto que deja el primero.

## Artefactos a revisar

| | `add-tiempo-utc` | `add-metas-semanales` |
|---|---|---|
| Propuesta | [proposal.md](../../openspec/changes/add-tiempo-utc/proposal.md) | [proposal.md](../../openspec/changes/add-metas-semanales/proposal.md) |
| Diseño | [design.md](../../openspec/changes/add-tiempo-utc/design.md) | [design.md](../../openspec/changes/add-metas-semanales/design.md) |
| Tareas | [tasks.md](../../openspec/changes/add-tiempo-utc/tasks.md) | [tasks.md](../../openspec/changes/add-metas-semanales/tasks.md) |
| Specs delta | [sesiones-estudio](../../openspec/changes/add-tiempo-utc/specs/sesiones-estudio/spec.md) · [calendario-estudio](../../openspec/changes/add-tiempo-utc/specs/calendario-estudio/spec.md) · [perfil-ajustes](../../openspec/changes/add-tiempo-utc/specs/perfil-ajustes/spec.md) · [tecnicas-estudio](../../openspec/changes/add-tiempo-utc/specs/tecnicas-estudio/spec.md) | [metas-semanales](../../openspec/changes/add-metas-semanales/specs/metas-semanales/spec.md) · [tecnicas-estudio](../../openspec/changes/add-metas-semanales/specs/tecnicas-estudio/spec.md) · [sesiones-estudio](../../openspec/changes/add-metas-semanales/specs/sesiones-estudio/spec.md) · [gestion-materias](../../openspec/changes/add-metas-semanales/specs/gestion-materias/spec.md) · [calendario-estudio](../../openspec/changes/add-metas-semanales/specs/calendario-estudio/spec.md) |

Specs vigentes contra las que se comparan: [`openspec/specs/`](../../openspec/specs/). Reglas del proyecto: [`AGENTS.md`](../../AGENTS.md). Código: [`app/src/main/java/io/github/jhoel0521/studyplan/`](../../app/src/main/java/io/github/jhoel0521/studyplan/).

## Decisiones ya tomadas por el Product Owner

Se pueden cuestionar si generan un problema concreto, pero son decisiones humanas:

- Tiempo guardado como instante UTC en milisegundos más desfase local en minutos, en sesiones y eventos.
- Corte estricto: cada minuto cuenta en el día y la semana locales en que ocurrió (`RN-31`).
- Se pueden planificar sesiones que cruzan la medianoche.
- La meta por materia es opcional, vive en la materia y se conserva el historial semanal de todas las metas (`RN-33`).
- Una sola felicitación acumulada al terminar de estudiar.
- «Estudiar ahora» crea una sesión sin horario planificado. «Estudios varios» es una materia fija, sin meta, que suma a la total (`RN-32`).
- Dos changes, en ese orden.

## Dónde mirar con más atención

- **Coherencia entre los dos changes:** dependencias, el delta encadenado de `HU-21` y que ninguno contradiga al otro.
- **Migraciones:** `sesion_estudio` se reconstruye en v5 y otra vez en v6 (para que `plan_fin_utc_milis` admita nulo). ¿Conviene hacerlo una sola vez?
- **Reglas que usan el plan:** si `RN-04`, `RN-05`, `RN-08`, `HU-42` y el «Plan» del calendario quedan bien definidos para sesiones nocturnas y para sesiones sin plan.
- **Historial de metas y registro tardío:** que una semana ya cerrada pueda cambiar de resultado por un registro tardío (`RN-06`).
- **Tamaño de las tareas:** la regla del proyecto es de 2 horas como máximo por tarea, cada una con su prueba.
