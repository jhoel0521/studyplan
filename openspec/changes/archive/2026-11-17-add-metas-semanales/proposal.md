# Proposal

## Why

La épica E6 (metas semanales, `HU-25` a `HU-28`) es la siguiente del Product Backlog. Al probar la app, el Product Owner detectó además que solo se puede estudiar lo que antes se planificó y registró como materia. No se puede empezar a estudiar sin plan, ni estudiar algo suelto (por ejemplo, unas clases de oratoria), ni pasar de una materia a otra en plena sesión. Como las horas de ese estudio deben sumar a las metas, el Product Owner decidió el 16-nov-2026 resolver ambas cosas en este change. Depende de `add-tiempo-utc` (`RN-31`), que debe archivarse antes.

## What Changes

- **Meta semanal total (`HU-25`, `RN-13`, `RN-14`, `RN-15`):** el estudiante fija las horas de la semana actual (lunes a domingo). Si no la fija, vale la meta predeterminada del perfil.
- **Meta por materia como dato de la materia (`HU-26`, `RN-14`):** cada materia puede tener una meta semanal opcional, que se define en su formulario y sigue vigente semana tras semana hasta que se cambie. Es independiente de la meta total.
- **Historial de metas (`HU-27`, nueva `RN-33`):** se conserva la meta de cada semana. Cambiar una meta afecta la semana actual y las siguientes, nunca las pasadas, y se puede consultar qué semanas se cumplieron.
- **Progreso (`HU-27`, `RN-11`, `RN-31`):** horas reales frente a la meta total en Inicio y frente a la meta de cada materia en Materias, con el corte estricto por semana de `RN-31`.
- **Felicitación única y acumulada (`HU-28`, `RN-16` modificada):** al terminar de estudiar aparece una sola felicitación que reúne todas las metas alcanzadas en ese tramo. Cambiar de materia no la interrumpe. Cada meta se felicita una sola vez por semana.
- **Estudiar ahora (nueva `HU-47`):** botón flotante «▶ Estudiar ahora», solo en Inicio. Se elige la materia (una existente, «Estudios varios» o una nueva con nombre y color) y la sesión empieza En curso sin horario planificado. Si la materia tiene una sesión Pendiente hoy, el sistema pregunta si es esa.
- **Cambiar de materia (nueva `HU-48`):** desde la sesión En curso se elige otra materia. La actual se completa en ese instante y la nueva empieza sin detener el tiempo, con la misma pregunta si la materia tiene una sesión Pendiente hoy.
- **Materia «Estudios varios» (nueva `RN-32`):** viene registrada desde la instalación. No se renombra, archiva ni elimina, no tiene meta propia, y sus horas suman a la meta total. Permite estudiar sin registrar ninguna materia.
- **Nueva transición en `RN-07`:** Nueva → En curso («estudiar ahora»). La sesión sin plan no ocupa horario planificado: no cuenta en el «Plan» del calendario ni bloquea cruces (`RN-05`).

**Propuesta IA, aceptada por el Product Owner el 16-nov-2026:** la numeración `HU-47` (Must, 5 pts), `HU-48` (Must, 3 pts), `RN-32` y `RN-33`, y que la sesión de «Estudiar ahora» no tenga horario planificado ni duración estimada.

**Fuera de alcance:**
- Estadísticas, reportes y tasa de cumplimiento (`HU-29` a `HU-33`, `HU-45`): usarán el historial de metas cuando se implementen.
- Widget de progreso (`HU-05`) y recordatorios (`HU-34`).
- Metas diarias o mensuales: no existen en el backlog.
- Editar la meta predeterminada del perfil desde Ajustes.
- Avisos de duración estimada en la sesión sin plan.

## Capabilities

### New Capabilities
- Ninguna.

### Modified Capabilities
- `metas-semanales`: `HU-26` (meta en la materia, vigente entre semanas), `HU-27` (progreso por materia, historial, `RN-31`, `RN-33`) y `HU-28` (felicitación única y acumulada, `RN-16`).
- `tecnicas-estudio`: nuevas `HU-47` (Estudiar ahora) y `HU-48` (Cambiar de materia).
- `sesiones-estudio`: `RN-07` (transición Nueva → En curso).
- `gestion-materias`: `HU-07` y `HU-08` (la materia «Estudios varios» no se edita, archiva ni elimina, `RN-32`) y `HU-09` (la lista muestra el progreso de la meta de cada materia).
- `calendario-estudio`: `HU-21` (la sesión sin plan suma en «Real» pero no en «Plan»). El delta parte del texto de `HU-21` que deja `add-tiempo-utc`, por lo que ese change se archiva antes.

## Impact

- **Código Java (`app/`):** dominio de metas (`MetaSemanal`, cálculo de progreso y de metas alcanzadas en un tramo), ampliación de `Materia` y `SesionEstudio` (sesión sin plan, transición nueva en `EstadoNueva` o equivalente), casos de uso `DefinirMetaSemanal`, `ConsultarProgresoMetas`, `EstudiarAhora` y `CambiarDeMateria`, y pantallas de Inicio, Materias, formulario de materia, sesión en curso y felicitación.
- **Base de datos:** Room v6 (`MIGRATION_5_6`). Columnas `meta_semanal_horas` y `es_general` en `materia`, la fila «Estudios varios», la tabla `meta_semanal` y el fin planificado opcional en `sesion_estudio`.
- **Dependencias:** ninguna nueva.
- **RNF afectados:** `RNF-02`, `RNF-04` (estudiar en 2 toques desde Inicio), `RNF-05`, `RNF-06` (botón con icono y texto), `RNF-08`, `RNF-09`, `RNF-15` (JUnit de `RN-11` a `RN-16`, `RN-32` y `RN-33`), `RNF-16` y `RNF-17`.
- **Documento:** capítulo 3 (`HU-47`, `HU-48` en el backlog; `RN-16` modificada; `RN-32` y `RN-33`; épica E6), capítulo 4 (modelo lógico v6, máquina de estados con la transición nueva, pantallas) y bitácora.
