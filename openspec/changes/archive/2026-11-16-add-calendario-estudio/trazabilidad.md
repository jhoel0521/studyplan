# Trazabilidad: qué specs implementa este change

Este change implementa la épica **E4: Calendario de estudio** (`HU-16` a `HU-21`), referenciando las specs vigentes en [`openspec/specs/`](../../specs/) y aplicando el delta spec de [`specs/calendario-estudio/spec.md`](specs/calendario-estudio/spec.md) para los ajustes decididos por el Product Owner el 2026-11-16 sobre `HU-20` y `HU-21`.

Estados: **Cubierto** (implementado y verificado en E4) · **Parcial** (lógica/navegación en E4; detalle adicional en otra épica) · **Pendiente** (otro change).

## HU-16 · Ver calendario mensual
Spec: [`calendario-estudio`](../../specs/calendario-estudio/spec.md#requirement-hu-16-ver-calendario-mensual) · Reglas: RN-13 · RNF-02, RNF-06, RNF-19

| Escenario | Estado | Tareas |
|---|---|---|
| Vista mensual (hoy resaltado, días con color de materias, suma de horas y días adyacentes atenuados) | Cubierto | 1.1, 1.2, 2.1, 3.1, 4.2, 4.3, 5.1 |
| Seleccionar día (toque corto lista debajo las sesiones con materia, horario y estado) | Cubierto | 1.1, 2.1, 4.3, 5.1, 6.1 |
| Navegar (deslizar horizontalmente o pulsar flechas mes anterior/siguiente) | Cubierto | 1.1, 4.1, 4.3, 5.1 |

## HU-17 · Ver calendario semanal tipo horario
Spec: [`calendario-estudio`](../../specs/calendario-estudio/spec.md#requirement-hu-17-ver-calendario-semanal-tipo-horario) · Reglas: RN-13 · RNF-02, RNF-06, RNF-19

| Escenario | Estado | Tareas |
|---|---|---|
| Vista semanal (7 columnas L–D, rejilla 24 h con auto-scroll `± 2 h`, bloques `MaterialCardView` proporcionales con color de materia) | Cubierto | 1.1, 1.2, 2.1, 3.1, 4.2, 4.3, 5.1 |
| Navegar semanas (deslizar horizontalmente o pulsar flechas semana anterior/siguiente) | Cubierto | 1.1, 4.1, 4.3, 5.1 |

## HU-18 · Ver calendario diario
Spec: [`calendario-estudio`](../../specs/calendario-estudio/spec.md#requirement-hu-18-ver-calendario-diario) · RNF-02, RNF-06

| Escenario | Estado | Tareas |
|---|---|---|
| Vista día (rejilla 24 h con auto-scroll `± 2 h`, bloques con materia, horario y estado, y línea en la hora actual) | Cubierto | 1.1, 1.2, 2.1, 4.2, 4.3, 5.1 |

## HU-19 · Ver el estado de las sesiones en el calendario
Spec: [`calendario-estudio`](../../specs/calendario-estudio/spec.md#requirement-hu-19-ver-el-estado-de-las-sesiones-en-el-calendario) · Reglas: RN-07 · RNF-06

| Escenario | Estado | Tareas |
|---|---|---|
| Indicadores (icono y etiqueta textual por estado en todas las vistas, sin depender solo del color) | Cubierto | 1.2, 4.2, 4.3, 5.1 |
| Leyenda (diálogo accesible explicando cada ícono y etiqueta de estado) | Cubierto | 4.2, 4.3, 5.1 |

## HU-20 · Crear sesión desde el calendario (delta spec)
Spec delta: [`specs/calendario-estudio/spec.md`](specs/calendario-estudio/spec.md#requirement-hu-20-crear-sesión-desde-el-calendario) · RNF-04

| Escenario | Estado | Tareas |
|---|---|---|
| Hora libre (pulsación larga sostenida en franja libre de `Semana` o `Día` con `fecha >= hoy` precarga fecha, inicio y fin sugerido `+60 min`) | Cubierto | 4.1, 4.3, 5.1 |
| Día en la vista Mes (pulsación larga sostenida en día `>= hoy` precarga fecha en modo planificar) | Cubierto | 4.3, 5.1 |
| Fecha pasada sin creación desde el calendario (`fecha < hoy` no dispara creación por pulsación larga) | Cubierto | 4.3, 5.1 |

## HU-21 · Filtrar y resumir el calendario (delta spec)
Spec delta: [`specs/calendario-estudio/spec.md`](specs/calendario-estudio/spec.md#requirement-hu-21-filtrar-y-resumir-el-calendario) · Reglas: RN-11

| Escenario | Estado | Tareas |
|---|---|---|
| Filtro (por materia, opción de incluir materias archivadas y filtros de estado consistentes entre `Día`, `Semana` y `Mes`) | Cubierto | 1.2, 2.1, 4.1, 4.2, 4.3, 5.1 |
| Resumen (cabecera con horas planificadas `Plan` y realizadas `Real` del periodo visible según filtro) | Cubierto | 1.2, 2.1, 4.3, 5.1 |

## Cierre de pendientes heredados de E2 y E3
- **HU-07 · Editar materia** ([`gestion-materias`](../../specs/gestion-materias/spec.md#requirement-hu-07-editar-materia)): completa «y el calendario muestra el nuevo color» al leer el catálogo vigente de materias en `ConsultarCalendarioService` y reflejarlo en celdas y bloques (tareas 2.1, 4.3, 5.1).
- **HU-13 · Registro tardío** y **HU-14 · Reprogramar** ([`sesiones-estudio`](../../specs/sesiones-estudio/spec.md)): resuelve el hueco funcional de E3 habilitando tocar cualquier sesión `NO_REALIZADA` pasada en `Mes`, `Semana` o `Día` para abrir `SesionDetalleActivity` (tareas 2.1, 3.1, 4.3, 5.1).

## Fuera de alcance en este change
- `HU-22` a `HU-24`, `HU-39` a `HU-44` (`tecnicas-estudio`): Épica E5 (y permisos asociados).
- `HU-25` a `HU-28` (`metas-semanales`): Épica E6.
- `HU-29` a `HU-31`, `HU-45` (`estadisticas`): Épica E7.
- `HU-32`, `HU-33` (`reportes`): Épica E8.
- `HU-34` (`recordatorios`): Épica E9.
- `HU-35` a `HU-37`, `HU-05` (`respaldo-privacidad`, widget): Épicas E10 / pendiente E1.
