# Design

## Context

Motivación y alcance en [proposal.md](proposal.md). Estado verificado en el repositorio:

- `StudyPlanDatabase` está en `version = 4`.
- `sesion_estudio` guarda `fecha` (`YYYY-MM-DD`), `hora_inicio` y `hora_fin` (`HH:MM`) como texto local sin zona. `SesionEstudio.validarDatosComunes` exige `fin.esPosteriorA(inicio)`, así que una sesión de 22:00 a 01:00 se rechaza.
- `evento_estudio` ya guarda `instante_milis` (UTC) y además `fecha` y `hora` locales en texto.
- `SesionDao` filtra y suma por `fecha BETWEEN :desde AND :hasta`, y cada sesión suma todos sus minutos reales al día de inicio.
- No hay *core library desugaring*: `java.time` no está disponible en API 24 y 25 (`RNF-17`). `RelojSistema` usa `java.util.Calendar`.
- SQLite en API 24 no admite `ALTER TABLE ... DROP COLUMN` (llega con SQLite 3.35, API 34).

## Goals / Non-Goals

**Goals:**
- Un único modelo de tiempo en sesiones y eventos: instante UTC más desfase local del momento.
- Contar cada minuto real en el día y la semana locales en que ocurrió (`RN-31`), con consultas por semana dentro de `RNF-02`.
- Migrar los datos de la versión 4 sin pérdida.

**Non-Goals:**
- Guardar el identificador de zona (`America/La_Paz`) o reglas de horario de verano.
- Cambiar la máquina de estados (`RN-07`) o las reglas de plazo de 3 días (`RN-06`, `RN-10`, `RN-29`), que siguen usando la fecha local de inicio.

## Decisions

### 1. Objeto de valor `Instante` en `domain`
- **Decisión:** `Instante(long utcMilis, int desfaseMinutos)`, inmutable y en Java puro, con `fechaLocal()`, `horaLocal()`, `inicioDelDiaLocal()` y comparación por `utcMilis`. La conversión es aritmética simple: `local = utcMilis + desfaseMinutos * 60 000`, y el día sale de `Math.floorDiv` sobre 86 400 000. `Fecha` y `Hora` se mantienen como vistas locales para la interfaz.
- **Alternativas consideradas:**
  - *Texto ISO 8601 con desfase:* rechazado por el Product Owner. Se compara mal como texto cuando los desfases son distintos y necesita conversión en cada consulta.
  - *Solo UTC, convirtiendo con la zona actual del teléfono:* rechazado. Al viajar, el historial cambiaría de día y de semana y una meta cerrada podría cambiar de resultado.
  - *`java.time`:* no disponible en API 24 sin *desugaring*, que sería una dependencia nueva.

### 2. Puerto `Reloj`
- **Decisión:** se agrega `Instante ahoraInstante()`. `RelojSistema` lo arma con `System.currentTimeMillis()` y `TimeZone.getDefault().getOffset(millis) / 60 000`. Las pruebas usan `FakeReloj` con desfase fijo.

### 3. Sesión con inicio y fin absolutos
- **Decisión:** `SesionEstudio` guarda `inicio` y `fin` planificados como `utcMilis` más un único `desfaseMinutos`, el del momento en que se planificó. Las columnas se llaman `plan_inicio_utc_milis` y `plan_fin_utc_milis`, y los accesores `planInicioUtcMilis()` y `planFinUtcMilis()` (decisión del Product Owner): marcan que son el horario planificado, y que el tiempo real sale siempre de `evento_estudio`. `fecha()`, `horaInicio()` y `horaFin()` quedan como accesores derivados, para no reescribir la interfaz que solo muestra datos. Las reglas pasan a instantes:
  - `RN-04`: `fin > inicio` y duración menor de 24 h.
  - `RN-05`: `seSolapaCon(otra)` compara intervalos absolutos y detecta cruces entre días.
  - `RN-08`: vence al terminar el día local en que cae el `fin`, no el del inicio.
- **Formulario:** se piden fecha, hora de inicio y hora de fin. Si la hora de fin es anterior a la de inicio, la sesión termina al día siguiente y el formulario lo muestra con la etiqueta «Termina al día siguiente». Si es igual, es error.
- **Alternativas consideradas:**
  - *Pedir fecha de fin en el formulario:* rechazado porque agrega un campo a cada registro para un caso poco frecuente y va contra `RNF-04`.

### 4. Desfase por evento y corte por día (`RN-31`)
- **Decisión:** cada `EventoEstudio` guarda su propio desfase. Un nuevo `RepartidorPorDia` en `domain` toma los `IntervaloEfectivo` de `CalculadorTiempoReal` y los corta en cada medianoche local, usando el desfase del evento que abre el intervalo. Para sesiones sin eventos (registro tardío, `HU-11` y `HU-13`), reparte la duración real declarada en proporción a cómo se superpone el intervalo planificado con cada día.

### 5. Minutos por día materializados en `tramo_diario`
- **Decisión:** nueva tabla `tramo_diario(sesion_id, fecha_local, minutos)`, con clave foránea en cascada e índice por `fecha_local`. Se escribe en la misma transacción que cierra la sesión (detener, cambiar, repartir, registro tardío, cierre por plazo). Inicio, calendario y, después, metas y estadísticas suman desde esta tabla con `fecha_local BETWEEN :lunes AND :domingo`.
- **Alternativas consideradas:**
  - *Recalcular desde `evento_estudio` en cada lectura:* rechazado por `RNF-02` (300 ms con 500 sesiones), por la misma razón por la que E5 materializó `duracion_real_minutos`.
  - *Guardar `fecha_local` en `sesion_estudio`:* no resuelve el reparto de una sesión entre dos días.

### 6. Consultas de sesiones por rango local
- **Decisión:** para listar sesiones de una semana local se consulta por `plan_inicio_utc_milis` entre los límites UTC del rango, ampliados 14 h a cada lado (el mayor desfase posible). Luego `domain` filtra por la fecha local real de cada sesión. Índice por `plan_inicio_utc_milis`.

### 7. Presentación con el desfase guardado
- **Decisión:** las horas se muestran con el desfase de cada marca (`RNF-08`, formato local). Si ese desfase no es el actual del teléfono, se añade la indicación «UTC−04:00», para que nunca dependa solo del contexto.

## Risks / Trade-offs

- **Sesiones existentes sin zona.** Al migrar, se les asigna el desfase actual del teléfono. → Para usuarios en Bolivia (sin horario de verano) el resultado es exacto. El riesgo queda anotado en la bitácora.
- **Horario de verano dentro de una sesión planificada.** El fin planificado se muestra con el desfase del inicio y puede verse una hora corrido. → Solo afecta a la vista del plan: los minutos reales salen de los eventos, cada uno con su desfase.
- **Viaje en medio de un tramo continuo.** El corte en la medianoche usa el desfase del evento que abre el tramo. → Caso raro; la suma total no cambia, solo en qué día cae.
- **Cambio grande sobre tres épicas archivadas (E3, E4, E5).** → Las pruebas JUnit e instrumentadas existentes se mantienen como red de regresión (`SesionesEspressoTest`, `TecnicasEstudioEspressoTest`, calendario).

## Migration Plan

`MIGRATION_4_5`, en una sola transacción:

1. Crear `sesion_estudio_nueva` con `plan_inicio_utc_milis`, `plan_fin_utc_milis` y `desfase_minutos`, sin `fecha`, `hora_inicio` ni `hora_fin`.
2. Copiar las filas convirtiendo `fecha` + hora local a UTC con el desfase actual del teléfono, leído en la migración desde `TimeZone.getDefault()`, que es código de adaptador.
3. Crear `evento_estudio_nueva` con `desfase_minutos`, calculado exactamente para cada fila como la diferencia entre su `fecha` + `hora` local y su `instante_milis`, redondeada a minutos.
4. Borrar las tablas viejas, renombrar las nuevas y recrear índices y claves foráneas.
5. Crear `tramo_diario` y llenarla con una fila por sesión `COMPLETADA` o `COMPLETADA_TARDIA` en su fecha local de inicio. Antes de v5 no podía haber sesiones que cruzaran la medianoche, salvo las `EN_CURSO` de varios días, que aún no se han cerrado.
6. Exportar el esquema `5.json` y probar `v4 → v5` con `MigrationTestHelper`, con datos reales previos.

**Rollback:** no hay migración descendente. Si v5 falla en las pruebas, se publica sin ella; la versión instalada sigue en v4.
