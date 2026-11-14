# Trazabilidad: qué specs implementa este change

Este change **no copia requisitos**: los enlaces apuntan a las specs de `add-requisitos-base`, que siguen siendo la única fuente. Aquí se lee, historia por historia, qué escenarios se cubren, cuáles quedan pendientes y en qué tareas se implementan.

Estados: **Cubierto** (el escenario se implementa y se prueba aquí) · **Parcial** (se implementa una parte) · **Pendiente** (lo completa otro change).

## HU-01 · Configurar perfil inicial
Spec: [`perfil-ajustes`](../../../specs/perfil-ajustes/spec.md#requirement-hu-01-configurar-perfil-inicial) · Reglas: RN-01, RN-14, RN-15 · RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Primer uso | Cubierto | 2.1, 4.3 a 4.5 |
| Nombre vacío | Cubierto | 2.1, 4.4, 4.5 |

## HU-02 · Ver resumen en la pantalla de inicio
Spec: [`perfil-ajustes`](../../../specs/perfil-ajustes/spec.md#requirement-hu-02-ver-resumen-en-la-pantalla-de-inicio) · Reglas: RN-15 · RNF-01

| Escenario | Estado | Tareas |
|---|---|---|
| Resumen con datos | **Pendiente** (necesita sesiones y metas por semana) | change de sesiones y metas |
| Acceso rápido «Nueva sesión» | **Pendiente** (necesita el registro de sesión) | change de sesiones |
| Saludo y progreso `0 / meta` con estados vacíos | Parcial (parte de «Resumen con datos») | 2.4, 2.5, 4.6 a 4.8 |

## HU-03 · Configurar tema e idioma
Spec: [`perfil-ajustes`](../../../specs/perfil-ajustes/spec.md#requirement-hu-03-configurar-tema-e-idioma) · RNF-07, RNF-08

| Escenario | Estado | Tareas |
|---|---|---|
| Tema | Cubierto | 2.2, 4.9 a 4.11 |
| Idioma | Cubierto | 2.2, 4.9 a 4.11 |

## HU-04 · Bloquear la app con huella o PIN
Spec: [`perfil-ajustes`](../../../specs/perfil-ajustes/spec.md#requirement-hu-04-bloquear-la-app-con-huella-o-pin) · RNF-12, RNF-17

| Escenario | Estado | Tareas |
|---|---|---|
| Activar bloqueo | Cubierto | 2.3, 3.3, 4.2, 4.12 a 4.14 |
| Sin método de bloqueo | Cubierto | 2.3, 3.3, 4.10 |

## HU-38 · Borrar mi información
Spec: [`respaldo-privacidad`](../../../specs/respaldo-privacidad/spec.md#requirement-hu-38-borrar-mi-información) · Reglas: RN-21 · RNF-13

| Escenario | Estado | Tareas |
|---|---|---|
| Elegir alcance | **Parcial**: solo «Borrar del teléfono»; «Drive» y «ambos» llegan con la copia de seguridad | 2.6, 3.2, 4.15 a 4.17 |
| Cancelar | Cubierto | 4.16, 4.17 |

## No incluidas en este change
HU-05 (widget) y HU-35 a HU-37 (copia de seguridad, cifrado y restauración) quedan fuera, como se declara en `proposal.md`.
