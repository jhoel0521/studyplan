# Trazabilidad: qué specs implementa este change

Este change **no copia requisitos**: los enlaces apuntan a las specs vigentes en [`openspec/specs/`](../../specs/) y al delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md). Aquí se detalla, historia por historia y regla por regla, qué escenarios cubre `add-tecnicas-estudio`, qué queda fuera de alcance para épicas posteriores y qué tareas implementan y verifican cada punto.

Estados: **Cubierto** (implementado y verificado aquí) · **Parcial** (lógica o datos preparados para otra épica) · **Pendiente** (otro change).

## RN-07 · Máquina de estados de la sesión (exposición en vivo)
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-rn-07-máquina-de-estados-de-la-sesión) · Reglas: RN-07, RN-11, RN-24, RN-27, RN-29 · RNF-15

| Escenario | Estado | Tareas |
|---|---|---|
| Pendiente → En curso (`iniciar`) | Cubierto | 1.2, 2.2, 4.3, 5.1 |
| Pendiente → Completada (`marcar completada` retirado del flujo normal; exige `iniciar` + `detener`) | Cubierto | 1.2, 4.3, 6.1 |
| En curso → En curso (`pausar / reanudar`) | Cubierto | 1.2, 2.2, 4.4, 5.1 |
| En curso → Completada (`detener`, incluye `0` min) | Cubierto | 1.2, 2.2, 4.4, 5.1 |
| En curso → Completada (`cambiar a la siguiente sesión`, RN-27) | Cubierto | 1.2, 2.4, 4.4, 5.1 |
| En curso → Completada (`declarar fin al volver`, RN-24) | Cubierto | 1.2, 2.3, 4.2, 5.1 |
| En curso → Completada (`plazo de declaración vencido`, RN-29) | Cubierto | 1.2, 2.3, 5.1 |

## HU-12 · Completar una sesión (sin edición manual de minutos)
Spec: [`sesiones-estudio`](../../specs/sesiones-estudio/spec.md#requirement-hu-12-completar-una-sesión) (delta en [`specs/sesiones-estudio/spec.md`](specs/sesiones-estudio/spec.md)) · Reglas: RN-07

| Escenario | Estado | Tareas |
|---|---|---|
| Completar (sin caja editable ni botón directo en `PENDIENTE`; vía `Iniciar → En curso → Detener`) | Cubierto | 1.2, 4.3, 5.1 |
| Transición inválida (`REPROGRAMADA`) | Cubierto | 1.2, 4.3 |

*(Nota: `HU-11` y `HU-13` de registro tardío en sesiones pasadas `NO_REALIZADA` ≤ 3 días permanecen intactos como caso especial independiente.)*

## HU-22 · Cronometrar sesión en curso
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-22-cronometrar-sesión-en-curso) (delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md)) · Reglas: RN-07, RN-22, RN-26 · RNF-03, RNF-10

| Escenario | Estado | Tareas |
|---|---|---|
| Cronometrar (Pendiente de hoy → En curso → Completada) | Cubierto | 1.2, 1.3, 2.2, 4.3, 4.4, 5.1 |
| Detención en el primer minuto (`0` minutos reales) | Cubierto | 1.2, 1.3, 2.2, 4.4, 5.1 |
| Sesión única en curso (intercepta segunda sesión con `W13`) | Cubierto | 1.1, 2.2, 4.2, 4.3, 5.1 |
| App cerrada por completo / `SIGKILL` (reconstrucción por marcas) | Cubierto | 1.3, 2.3, 3.2, 3.3, 4.2, 4.4, 5.1 |
| Duración distinta a la planificada (RN-26) | Cubierto | 1.3, 2.2, 4.4, 5.1 |

## HU-23 · Estudiar con técnica Pomodoro configurable
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-23-estudiar-con-técnica-pomodoro-configurable) (delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md)) · Reglas: RN-11, RN-17, RN-25 · RNF-03

| Escenario | Estado | Tareas |
|---|---|---|
| Ciclo (derivación determinista y aviso sonoro/vibración en cambio) | Cubierto | 1.4, 2.2, 3.3, 4.1, 4.4, 5.1 |
| Solo trabajo suma (excluye descansos, RN-11) | Cubierto | 1.3, 1.4, 2.2, 4.4, 5.1 |
| Configurar en Ajustes > Pomodoro con snapshot por sesión (RN-17) | Cubierto | 1.1, 1.2, 2.2, 3.1, 4.3, 5.1 |
| Seguir tras el plan (`+1 pomodoro` o `tiempo extra libre`, RN-25) | Cubierto | 1.4, 2.4, 4.4, 5.1 |

## HU-24 · Activar modo No molestar durante la sesión
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-24-activar-modo-no-molestar-durante-la-sesión) (delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md)) · Reglas: RN-18 · RNF-18

| Escenario | Estado | Tareas |
|---|---|---|
| Global (`HEREDAR_GLOBAL`, guarda y restaura filtro previo) | Cubierto | 1.1, 2.2, 3.1, 3.3, 4.3, 4.4, 5.1 |
| Forzar activación por sesión (`FORZAR_ACTIVAR`) | Cubierto | 1.1, 1.2, 2.2, 3.3, 4.3, 4.4, 5.1 |
| Forzar desactivación por sesión (`FORZAR_DESACTIVAR`) | Cubierto | 1.1, 1.2, 2.2, 3.3, 4.3, 5.1 |
| Sin permiso o permiso revocado (explicación previa y degradación `RNF-18`) | Cubierto | 3.3, 4.3, 4.4, 5.1 |

## HU-39 · Registrar eventos de estudio
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-39-registrar-eventos-de-estudio) · Reglas: RN-22 · RNF-02, RNF-10

| Escenario | Estado | Tareas |
|---|---|---|
| Línea de tiempo en detalle de sesión (`W15`) | Cubierto | 1.2, 1.5, 2.4, 3.2, 4.3, 5.1 |
| Persistencia antes de actualizar UI / ante cierre del SO | Cubierto | 1.2, 2.2, 3.1, 3.2, 5.1 |

## HU-40 · Recibir aviso al salir de la app durante una sesión
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-40-recibir-aviso-al-salir-de-la-app-durante-una-sesión) · Reglas: RN-23 · RNF-18

| Escenario | Estado | Tareas |
|---|---|---|
| Salida (`SALIDA_APP`, no pausa, notificación «Tu sesión sigue en curso — volver») | Cubierto | 2.3, 3.3, 4.1, 4.2, 5.1 |
| Regreso (al tocar notificación o abrir app registra `REGRESO_APP`) | Cubierto | 2.3, 4.2, 5.1 |

## HU-41 · Declarar estudio con la app cerrada
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-41-declarar-estudio-con-la-app-cerrada) (delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md)) · Reglas: RN-24, RN-29 · RNF-10

| Escenario | Estado | Tareas |
|---|---|---|
| Volver más tarde (tras `ActividadProtegida`, crédito íntegro `ESTUDIO_APP_CERRADA` y nota) | Cubierto | 1.3, 2.3, 4.2, 5.1 |
| No siguió («No, terminé al salir», incluye `0` min) | Cubierto | 1.2, 1.3, 2.3, 4.2, 5.1 |
| Sigue estudiando (registra tramo fuera de app y sigue `EN_CURSO`) | Cubierto | 2.3, 4.2, 5.1 |
| Al día siguiente (respeto de `EN_CURSO` por `VencerSesionesPendientes`) | Cubierto | 2.3, 4.2, 5.1 |
| Vencimiento tras más de 3 días (`RN-29`, cierre automático en última marca dentro de app) | Cubierto | 1.3, 2.3, 3.3, 5.1 |

## HU-42 · Continuar con tiempo extra al llegar al fin planificado
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-42-continuar-con-tiempo-extra-al-llegar-al-fin-planificado) (delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md)) · Reglas: RN-25 · RNF-18

| Escenario | Estado | Tareas |
|---|---|---|
| Pregunta (`FIN_PLANIFICADO` y aviso «¿Sigues estudiando?» sin duplicar Pomodoro) | Cubierto | 2.2, 2.4, 3.3, 4.1, 4.4, 5.1 |
| Sin respuesta (tiempo posterior marcado como `TIEMPO_EXTRA`) | Cubierto | 1.3, 2.4, 4.4, 5.1 |

## HU-43 · Decidir al llegar la hora de otra sesión
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-43-decidir-al-llegar-la-hora-de-otra-sesión) · Reglas: RN-27

| Escenario | Estado | Tareas |
|---|---|---|
| Cambiar («Cambiar a <Materia>» completa actual e inicia siguiente en transacción) | Cubierto | 1.2, 2.4, 3.3, 4.1, 4.4, 5.1 |
| Seguir (la siguiente sesión permanece `PENDIENTE`) | Cubierto | 2.4, 4.4, 5.1 |

## HU-44 · Repartir un tramo continuo entre sesiones
Spec: [`tecnicas-estudio`](../../specs/tecnicas-estudio/spec.md#requirement-hu-44-repartir-un-tramo-continuo-entre-sesiones) (delta en [`specs/tecnicas-estudio/spec.md`](specs/tecnicas-estudio/spec.md)) · Reglas: RN-28

| Escenario | Estado | Tareas |
|---|---|---|
| Repartir (segmentación de intervalos efectivos en transacción atómica, `W14`) | Cubierto | 1.5, 2.1, 2.4, 3.1, 4.5, 5.1 |
| Sin sesiones siguientes (no se ofrece repartir) | Cubierto | 2.2, 4.4, 5.1 |

## Fuera de alcance en este change
- `estadisticas` (`HU-45`, `RN-30`, métricas de concentración alimentadas por `evento_estudio`): Épica E7 (queda preparada la tabla `evento_estudio`).
- `metas-semanales` (`HU-25` a `HU-28`): Épica E6.
- `reportes` (`HU-32`, `HU-33`): Épica E8.
- `recordatorios` (`HU-34`): Épica E9.
- `respaldo-privacidad` (`HU-35` a `HU-37`, `HU-05`): Épica E10.
