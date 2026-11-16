# Design

## Context

Motivación y alcance en [proposal.md](proposal.md). Parte del estado que deja `add-tiempo-utc` (Room v5):

- `sesion_estudio` tiene `plan_inicio_utc_milis`, `plan_fin_utc_milis` (obligatorio) y `desfase_minutos`; `tramo_diario(sesion_id, fecha_local, minutos)` guarda los minutos reales por día local (`RN-31`).
- `Perfil.metaSemanalHoras` ya existe y valida `RN-14` (`0 < h ≤ 168`). `ObtenerResumenInicioService` ya compara las horas de la semana con esa meta.
- `materia` no tiene meta ni marca de materia general. `IniciarSesionService` exige una sesión existente en estado Pendiente y devuelve `SESION_YA_EN_CURSO` si hay otra activa.
- `SesionEstudio.cambiarASiguiente` (`RN-27`) ya completa la sesión actual y pone En curso otra sin detener el tiempo.

## Goals / Non-Goals

**Goals:**
- Metas con historial estable por semana, sin recalcular semanas cerradas cuando cambia una meta.
- Estudiar y cambiar de materia en dos toques desde Inicio o desde la sesión en curso (`RNF-04`).
- Reutilizar la máquina de estados, el registro de eventos y el cierre de E5 sin duplicarlos.

**Non-Goals:**
- Calcular estadísticas o tasas sobre el historial (E7).
- Notificaciones o avisos para la sesión sin plan.

## Decisions

### 1. Meta vigente en la materia y en el perfil; foto por semana en `meta_semanal`
- **Decisión:** la meta vigente vive en `materia.meta_semanal_horas` (opcional) y en `perfil.metaSemanalHoras`. La tabla `meta_semanal(semana_inicio, materia_id NULL = total, horas_objetivo, felicitada)` guarda una foto por semana y meta, con clave única `(semana_inicio, materia_id)`. La foto se crea la primera vez que se consulta o se cierra una sesión en esa semana. Cambiar una meta actualiza la foto de la semana actual y la vigente, nunca las pasadas (`RN-33`). Fijar la meta total solo para la semana actual (`HU-25`) escribe únicamente la foto.
- **Cumplida no se guarda:** se deriva de `horas_objetivo` y de la suma de `tramo_diario` de esa semana. El registro tardío (`RN-06`, hasta 3 días) puede completar una semana recién cerrada, y así el resultado refleja las horas reales sin una columna que se desincronice.
- **Semanas sin foto:** al consultar el historial, una semana pasada sin foto (porque no se abrió la app) se completa con la meta vigente de ese momento, y se marca como estimada en la interfaz.
- **Alternativas consideradas:**
  - *Meta por materia solo por semana (modelo del diagrama original):* rechazado por el Product Owner, porque obliga a redefinirla cada lunes.
  - *Meta sin historial:* rechazado por el Product Owner, porque las semanas pasadas se verían contra la meta nueva.

### 2. Sesión sin plan
- **Decisión:** `plan_fin_utc_milis` pasa a admitir `NULL`, que significa «sin horario planificado». `duracion_planificada_minutos = 0`, y `plan_inicio_utc_milis` es el instante en que empezó. `SesionEstudio.estudiarAhora(materia, instante, técnica)` crea la sesión directamente En curso: es la transición Nueva → En curso de `RN-07`, con el evento `INICIO`. Las reglas que usan el plan la ignoran: cruces `RN-05`, vencimiento `RN-08`, aviso de fin `HU-42` y total «Plan» del calendario.
- **Alternativas consideradas:**
  - *Crear una sesión Pendiente con una duración estimada e iniciarla:* rechazado por el Product Owner. La estimación no refleja la realidad, inflaría el «Plan» y obligaría a recortar cruces y cortes de medianoche.

### 3. Casos de uso `EstudiarAhora` y `CambiarDeMateria`
- **Decisión:** `EstudiarAhora` comprueba que no haya sesión En curso. Si la materia tiene una sesión Pendiente hoy, devuelve una consulta de confirmación con la más cercana a la hora actual; la UI pregunta y llama a `IniciarSesion` o a `EstudiarAhora` con «crear sin plan». `CambiarDeMateria` cierra la actual con `cambiarASiguiente` y abre la destino, en una única transacción que también escribe `tramo_diario` y los eventos `CAMBIO_SESION`. Ambos aceptan una materia nueva (nombre y color) y la registran con `RegistrarMateria` (`RN-02`) dentro de la misma operación.

### 4. Materia «Estudios varios»
- **Decisión:** columna `materia.es_general` y una única fila creada en la migración y en la primera instalación. El nombre se muestra desde `strings.xml` («Estudios varios» / «Miscellaneous study») para respetar `RNF-08`; el nombre guardado solo sirve como clave. `EditarMateria`, `ArchivarMateria`, `EliminarMateria` y la meta por materia la rechazan con un error nuevo (`MATERIA_GENERAL_PROTEGIDA`). No ocupa el nombre para `RN-02`: el estudiante no puede crear otra «Estudios varios».

### 5. Felicitación acumulada
- **Decisión:** al cerrar un tramo (detener, declarar fin, cierre por plazo, registro tardío), `ConsultarProgresoMetas` compara el progreso antes y después de las sesiones cerradas en ese tramo, incluidas las que cerró `CambiarDeMateria`. Devuelve las metas que cruzaron su objetivo y aún tienen `felicitada = false`, y las marca como felicitadas en la misma transacción. `SesionEnCursoActivity` muestra una única pantalla «¡Sigue así!» con la lista. `CambiarDeMateria` no devuelve felicitaciones.

### 6. Interfaz
- **Inicio:** `ExtendedFloatingActionButton` «▶ Estudiar ahora» con `contentDescription`, junto al actual «Nueva sesión», que sigue planificando. Si hay una sesión En curso, el botón lleva a ella.
- **Selector de materia:** *bottom sheet* con las materias activas, «Estudios varios» y «Nueva materia» (campos de nombre y color en línea) y la técnica. Lo comparten «Estudiar ahora» y «Cambiar de materia».
- **Materias:** campo opcional «Meta semanal (h)» en el formulario; barra y «3 / 4 h» en la lista.
- **Historial de metas:** pantalla accesible desde la tarjeta de progreso de Inicio.

## Risks / Trade-offs

- **Fotos semanales creadas tarde.** Si el estudiante no abre la app una semana, esa foto se crea con la meta vigente cuando la abra. → Se marca como estimada; un cambio de meta posterior ya no la altera.
- **Sesión sin plan olvidada En curso.** Al no tener fin planificado, no hay aviso de fin. → Siguen valiendo la notificación de salida (`HU-40`), el diálogo de regreso (`HU-41`) y el cierre a los 3 días (`RN-29`).
- **La consulta «¿Es tu sesión de …?» con varias Pendientes hoy.** → Se ofrece la más cercana a la hora actual; elegir otra es reprogramar y queda fuera de alcance.

## Migration Plan

`MIGRATION_5_6`, en una transacción:

1. `ALTER TABLE materia ADD COLUMN meta_semanal_horas REAL` (nulo) y `ADD COLUMN es_general INTEGER NOT NULL DEFAULT 0`, e insertar la fila «Estudios varios» con `es_general = 1`.
2. Reconstruir `sesion_estudio` para que `plan_fin_utc_milis` admita `NULL`, ya que SQLite en API 24 no permite cambiar la restricción de una columna.
3. Crear `meta_semanal` con su clave única y su clave foránea a `materia` en cascada.
4. Exportar `6.json` y probar `v5 → v6` con `MigrationTestHelper`.

En una instalación nueva, la fila «Estudios varios» se crea con un `RoomDatabase.Callback.onCreate`.
