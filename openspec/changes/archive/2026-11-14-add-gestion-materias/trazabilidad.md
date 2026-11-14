# Trazabilidad: qué specs implementa este change

Este change **no copia requisitos**: los enlaces apuntan a la spec `gestion-materias` de `add-requisitos-base`, que sigue siendo la única fuente. Aquí se lee, historia por historia, qué escenarios se cubren, cuáles quedan pendientes y qué tareas los implementan.

Estados: **Cubierto** (el escenario se implementa y se prueba aquí) · **Parcial** (se implementa una parte) · **Pendiente** (lo completa otro change).

## HU-06 · Registrar materia
Spec: [`gestion-materias`](../../../specs/gestion-materias/spec.md#requirement-hu-06-registrar-materia) · Reglas: RN-02 · RNF-06, RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Registro válido | Cubierto (el color se elige en un modal con selector y campo hexadecimal) | 2.1, 2.2, 5.4, 5.6, 5.8 |
| Nombre duplicado | Cubierto, también con otras mayúsculas, sin acentos y con espacios de más (ampliación de RN-02 pedida por el Product Owner) | 2.1, 2.2, 5.4, 5.6 |

## HU-07 · Editar materia
Spec: [`gestion-materias`](../../../specs/gestion-materias/spec.md#requirement-hu-07-editar-materia) · RNF-06, RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Edición | Parcial: se edita nombre, color y docente conservando el id; «el calendario muestra el nuevo color» y «las sesiones siguen asociadas» dependen de sesiones y calendario | 2.3, 3.2, 5.4, 5.6, 5.8 |

## HU-08 · Archivar o eliminar materia
Spec: [`gestion-materias`](../../../specs/gestion-materias/spec.md#requirement-hu-08-archivar-o-eliminar-materia) · Reglas: RN-03 · RNF-06, RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Archivar | Parcial: la materia sale de las listas de selección y se muestra en «Archivadas»; «sus sesiones se conservan en reportes y estadísticas» lo completan E3 y E7 | 2.4, 5.4 |
| Eliminar protegida | Parcial: el núcleo lo rechaza con un puerto falso que devuelve sesiones; el adaptador real lo trae el change de sesiones (hoy `SinSesionesDeMateria` responde cero) | 2.5 |
| Eliminar sin historial | Cubierto | 2.5, 5.4, 5.6 |

## HU-46 · Desarchivar materia (nueva)
Spec: [`gestion-materias`](../../../specs/gestion-materias/spec.md#requirement-hu-46-desarchivar-materia) (agregada por este change) · Reglas: RN-02 · RNF-06, RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Desarchivar | Cubierto | 2.4, 3.2, 5.4, 5.6 |
| Nombre ya en uso | Cubierto | 2.4, 5.4, 5.6 |

## HU-09 · Listar materias con horas acumuladas
Spec: [`gestion-materias`](../../../specs/gestion-materias/spec.md#requirement-hu-09-listar-materias-con-horas-acumuladas) · RNF-06, RNF-14

| Escenario | Estado | Tareas |
|---|---|---|
| Lista | Parcial: color, nombre, docente y orden alfabético; las horas realizadas son `0 h` hasta que existan sesiones (E3) | 2.6, 5.3, 5.6 |
| Lista vacía | Cubierto | 5.3, 5.6 |

## Pendientes que este change deja a otros

- **Change de sesiones (E3):** reemplazar `SinSesionesDeMateria` por una consulta real, calcular las horas realizadas y completar los escenarios parciales de HU-07, HU-08 y HU-09. Es **obligatorio** antes de dar HU-08 por cumplida.
- **Change de calendario (E4):** mostrar el color de la materia.
- **Change de metas (E6):** meta semanal por materia (HU-26); la lista del wireframe mostraría esa meta.
