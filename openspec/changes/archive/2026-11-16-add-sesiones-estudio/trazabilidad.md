# Trazabilidad: qué specs implementa este change

Este change **no copia requisitos**: los enlaces apuntan a las specs vigentes en [`openspec/specs/`](../../specs/), que siguen siendo la fuente única. Aquí se detalla, regla e historia por historia, qué escenarios cubre `add-sesiones-estudio`, qué queda fuera de alcance para épicas posteriores y qué tareas implementan y verifican cada punto.

Estados: **Cubierto** (implementado y verificado aquí) · **Parcial** (lógica/núcleo implementado aquí; UI o flujo extendido en otra épica) · **Pendiente** (otro change).

## RN-07 · Máquina de estados de la sesión
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-rn-07-máquina-de-estados-de-la-sesión) · Reglas: RN-07, RN-08, RN-09, RN-10, RN-11, RN-24, RN-27, RN-29 · RNF-15

| Escenario | Estado | Tareas |
|---|---|---|
| Pendiente → En curso (`iniciar`) | **Parcial** (transición y validación en `EstadoPendiente` probadas en JUnit; cronómetro UI llega en E5) | 1.2, 1.4 |
| En curso → En curso (`pausar / reanudar`) | **Parcial** (estado `EstadoEnCurso` y prueba JUnit; eventos UI en E5) | 1.2, 1.4 |
| En curso → Completada (`detener`) | **Parcial** (transición en `EstadoEnCurso` y prueba JUnit; cronómetro UI en E5) | 1.2, 1.4 |
| Pendiente → Completada (`marcar completada` mismo día) | Cubierto | 1.2, 1.4, 2.4, 4.5, 5.2 |
| Pendiente → No realizada (`fin del día`, RN-08) | Cubierto | 1.2, 1.4, 2.2, 3.2, 5.2 |
| Pendiente → Reprogramada (`reprogramar`, RN-09) | Cubierto | 1.2, 1.4, 2.6, 4.4, 5.2 |
| No realizada → Reprogramada (`reprogramar`, RN-09) | Cubierto | 1.2, 1.4, 2.6, 4.4, 5.2 |
| No realizada → Completada (registro tardío) (`<= 3` días, RN-10) | Cubierto | 1.2, 1.4, 2.5, 4.5, 5.2 |
| Nueva → Completada (registro tardío) (`1..3` días, RN-06) | Cubierto | 1.3, 1.4, 2.3, 4.4, 5.2 |
| En curso → Completada (`cambiar a la siguiente sesión`, RN-27) | **Parcial** (regla en `EstadoEnCurso` y prueba JUnit; UI en E5) | 1.2, 1.4 |
| En curso → Completada (`declarar fin al volver`, RN-24) | **Parcial** (regla en `EstadoEnCurso` y prueba JUnit; UI en E5) | 1.2, 1.4 |
| En curso → Completada (`plazo de declaración vencido`, RN-29) | **Parcial** (regla en `EstadoEnCurso` y prueba JUnit; UI en E5) | 1.2, 1.4 |
| Estado final sin transiciones (`COMPLETADA`, `COMPLETADA_TARDIA`, `REPROGRAMADA`) | Cubierto | 1.2, 1.4, 4.5, 5.2 |

## HU-10 · Planificar sesión de estudio
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-10-planificar-sesión-de-estudio) · Reglas: RN-04, RN-05 · RNF-04, RNF-06, RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Registro válido (materia activa, hoy o futura, 90 min en `PENDIENTE`) | Cubierto | 1.1, 1.3, 2.1, 3.1, 3.2, 4.4, 5.2 |
| Horario inválido (`horaFin <= horaInicio`, RN-04) | Cubierto | 1.1, 1.3, 2.1, 4.4, 5.2 |
| Cruce bloqueado indicando sesión en conflicto (RN-05) | Cubierto | 1.3, 2.1, 3.2, 4.4, 5.2 |

## HU-11 · Registrar sesión pasada (registro tardío)
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-11-registrar-sesión-pasada-registro-tardío) · Reglas: RN-04, RN-05, RN-06, RN-11, RN-26 · RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Dentro del límite (`1..3` días atrás → `COMPLETADA_TARDIA` y suma horas) | Cubierto | 1.1, 1.3, 2.3, 2.8, 2.9, 4.4, 5.2 |
| Fuera del límite (`>= 4` días atrás → rechazo informando límite de 3 días) | Cubierto | 1.1, 1.3, 2.3, 4.4, 5.2 |

## HU-12 · Completar una sesión
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-12-completar-una-sesión) · Reglas: RN-07, RN-11, RN-26 · RNF-06

| Escenario | Estado | Tareas |
|---|---|---|
| Completar sesión `PENDIENTE` de hoy con duración real editable y suma semanal | Cubierto | 1.2, 2.4, 2.9, 4.3, 4.5, 5.2 |
| Transición inválida (`REPROGRAMADA` u otro estado no permite completar) | Cubierto | 1.2, 2.4, 4.5, 5.2 |

## HU-13 · Marcar automáticamente sesiones no realizadas
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-13-marcar-automáticamente-sesiones-no-realizadas) · Reglas: RN-08, RN-10, RN-11

| Escenario | Estado | Tareas |
|---|---|---|
| Fin del día (vencimiento automático de `PENDIENTE` con `fecha < hoy` a `NO_REALIZADA`) | Cubierto | 2.2, 2.7, 2.9, 3.2, 4.3, 5.2 |
| Registro tardío desde `NO_REALIZADA` (`<= 3` días → `COMPLETADA_TARDIA`) | Cubierto | 1.2, 2.5, 4.5, 5.2 |
| Fuera de plazo (`> 3` días → no ofrece registro tardío, solo reprogramar) | Cubierto | 1.2, 2.5, 2.7, 4.5, 5.2 |

## HU-14 · Reprogramar sesión conservando el registro
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-14-reprogramar-sesión-conservando-el-registro) · Reglas: RN-04, RN-05, RN-09

| Escenario | Estado | Tareas |
|---|---|---|
| Reprogramar `PENDIENTE` o `NO_REALIZADA` → original `REPROGRAMADA` + nueva `PENDIENTE` enlazada | Cubierto | 1.2, 2.6, 3.2, 4.4, 4.5, 5.2 |
| Historial visible (abrir sesión `REPROGRAMADA` muestra enlace a la nueva sesión) | Cubierto | 2.7, 3.2, 4.5, 5.2 |
| Cruce bloqueado al reprogramar (RN-05) | Cubierto | 2.6, 4.4, 5.2 |

## HU-15 · Eliminar sesión planificada
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-15-eliminar-sesión-planificada) · Reglas: RN-12

| Escenario | Estado | Tareas |
|---|---|---|
| Eliminar `PENDIENTE` con confirmación y recalcular resúmenes | Cubierto | 1.2, 2.8, 3.2, 4.5, 5.2 |
| Historial protegido (`COMPLETADA`, `COMPLETADA_TARDIA`, `NO_REALIZADA`, `REPROGRAMADA`) | Cubierto | 1.2, 2.8, 4.5, 5.2 |

## Cierre de pendientes de E1 y E2
- **HU-02 · Ver resumen en la pantalla de inicio** ([`perfil-ajustes`](../../specs/perfil-ajustes/spec.md#requirement-hu-02-ver-resumen-en-la-pantalla-de-inicio) · RN-11, RN-13, RN-15 · RNF-01, RNF-04):
  - «Resumen con datos» (progreso semanal lunes–domingo, próxima sesión pendiente y sesiones de hoy con icono y texto de estado): **Cubierto** en tareas 2.9, 4.3, 5.3.
  - «Acceso rápido» (botón «+ Nueva sesión» con fecha de hoy preseleccionada en ≤ 3 toques): **Cubierto** en tareas 4.3, 4.4, 5.3.
- **HU-07 · Editar materia** ([`gestion-materias`](../../specs/gestion-materias/spec.md#requirement-hu-07-editar-materia)):
  - «las sesiones siguen asociadas tras editar nombre/color/docente»: **Cubierto** en tareas 3.1, 4.3, 5.3 («el calendario muestra el nuevo color» queda para E4).
- **HU-08 · Archivar o eliminar materia** ([`gestion-materias`](../../specs/gestion-materias/spec.md#requirement-hu-08-archivar-o-eliminar-materia) · RN-03):
  - «Eliminar protegida» con consulta real en Room (`SesionesDeMateriaRoom`): **Cubierto** en tareas 3.3, 5.3.
  - «sus sesiones se conservan en reportes y estadísticas» tras archivar: las sesiones persisten vinculadas por `ON DELETE RESTRICT` (`3.1`), quedando reportes/estadísticas para E7 y E8.
- **HU-09 · Listar materias con horas acumuladas** ([`gestion-materias`](../../specs/gestion-materias/spec.md#requirement-hu-09-listar-materias-con-horas-acumuladas) · RN-11):
  - «horas realizadas» reales por materia: **Cubierto** en tareas 2.10, 3.2, 5.3.

## Fuera de alcance en este change
- `calendario-estudio` (`HU-16` a `HU-21`): Épica E4.
- `tecnicas-estudio` (`HU-22` a `HU-24`, `HU-39` a `HU-44` en UI/eventos en vivo): Épica E5.
- `metas-semanales` (`HU-25` a `HU-28`): Épica E6.
- `estadisticas`, `reportes`, `recordatorios`, `respaldo-privacidad` (`HU-29` a `HU-37`, `HU-05`): Épicas E7 a E10.
