# Proposal

## Why

Una sesión guarda hoy su momento como tres textos locales sin zona horaria (`fecha`, `hora_inicio`, `hora_fin`). Por eso no se puede planificar una sesión nocturna que cruce la medianoche (por ejemplo, de 22:00 a 01:00), un cambio de zona horaria o un viaje desplaza el historial, y las horas reales se suman al día de inicio aunque se hayan estudiado al día siguiente. La épica E6 (metas semanales) necesita contar las horas con un corte estricto por día y por semana, así que este change va antes que `add-metas-semanales`. Es una decisión del Product Owner del 16-nov-2026.

## What Changes

- **BREAKING (modelo de datos):** el horario de una sesión pasa a guardarse como instantes absolutos: inicio y fin en milisegundos UTC, más el desfase horario local en minutos del momento en que se planificó. Desaparecen las columnas `fecha`, `hora_inicio` y `hora_fin` de `sesion_estudio`.
- **BREAKING (modelo de datos):** cada evento de estudio conserva su `instante_milis` UTC y suma su desfase local en minutos. Desaparecen sus columnas de texto `fecha` y `hora`.
- **Sesiones que cruzan la medianoche (`HU-10`, `RN-04`):** si la hora de fin es anterior a la de inicio, la sesión termina al día siguiente. Una hora de fin igual a la de inicio sigue siendo inválida.
- **Cruces sobre instantes (`HU-10`, `HU-14`, `RN-05`):** el cruce se evalúa sobre el intervalo completo, también entre dos días.
- **No realizada en sesiones nocturnas (`HU-13`, `RN-08`):** una sesión Pendiente pasa a «No realizada» al terminar el día local en que cae su fin planificado, no el de su inicio.
- **Nueva regla `RN-31`, corte estricto por día y semana (`HU-02`, `HU-17`, `HU-18`, `HU-21`):** cada minuto de estudio cuenta en el día y en la semana locales en que ocurrió. Un tramo que cruza la medianoche se divide en ella. No se pueden pasar horas de un día o semana a otro.
- **Historial estable (`HU-39`):** fechas y horas se muestran con el desfase guardado en cada marca, así que viajar o cambiar la zona del teléfono no altera el pasado.
- **Migración Room v4 → v5** sin pérdida de datos (`RNF-10`, `RNF-14`).
- **Documento académico:** se agrega `RN-31` a la tabla de reglas, se actualiza el texto de `RN-04` y `RN-08`, el modelo lógico v5 y el diccionario de datos.

**Fuera de alcance:**
- Metas semanales, «Estudiar ahora», «Cambiar de materia» y la materia «Estudios varios» (`HU-25` a `HU-28`, `HU-47`, `HU-48`): van en `add-metas-semanales`.
- Estadísticas y reportes (`HU-29` a `HU-33`, `HU-45`): usarán `RN-31` cuando se implementen.
- Elegir la zona horaria a mano o mostrar varias zonas a la vez.
- Sesiones planificadas de 24 horas o más.

## Capabilities

### New Capabilities
- Ninguna.

### Modified Capabilities
- `sesiones-estudio`: `HU-10` (sesión que cruza la medianoche, `RN-04`, cruce entre días `RN-05`), `HU-13` (`RN-08` sobre el día del fin planificado) y `HU-14` (cruce al reprogramar sobre instantes).
- `calendario-estudio`: `HU-17` y `HU-18` (una sesión nocturna aparece en los dos días que ocupa) y `HU-21` (resumen por día y semana con `RN-31`).
- `perfil-ajustes`: `HU-02` (las horas realizadas de la semana se cuentan con `RN-31`).
- `tecnicas-estudio`: `HU-39` (la línea de tiempo muestra la hora local del momento del evento, estable ante cambios de zona).

## Impact

- **Código Java (`app/`):** nuevo objeto de valor de instante en `domain`, ampliación del puerto `Reloj`, `SesionEstudio` y sus estados, validaciones de horario y cruce, `CalculadorTiempoReal`, `ConsultarCalendarioService`, `ObtenerResumenInicioService`, `VencerSesionesPendientesService`, el formulario y el detalle de sesión, el calendario y la línea de tiempo. Unas 22 clases usan hoy la fecha y las horas de la sesión.
- **Base de datos:** `StudyPlanDatabase` sube a la versión 5 (`MIGRATION_4_5`). Se reconstruyen `sesion_estudio` y `evento_estudio` y se crea la tabla de minutos por día `tramo_diario`.
- **Dependencias:** ninguna nueva.
- **RNF afectados:** `RNF-02` (consultas por rango con 500 sesiones), `RNF-08` (fechas y horas en formato local), `RNF-10` y `RNF-14` (migración sin pérdidas), `RNF-15` (pruebas JUnit de `RN-04`, `RN-05`, `RN-08` y `RN-31`), `RNF-17` (API 24, sin `java.time`).
- **Documento:** capítulo 3 (tabla de reglas), capítulo 4 (modelo lógico v5, diccionario de datos, diagramas) y `docs/auditoria-ia/bitacora.md`.
