# Proposal

## Why

HU-05 «Ver progreso en un widget» (prioridad Could, 5 puntos) está especificada en `perfil-ajustes` y declarada en el alcance de la versión 0.1.0. El Product Owner pidió implementarla y probarla en su teléfono. Una primera versión con dos tamaños (2×2 y 4×2) no le gustó: quiere **un solo widget de 1×4** (4 celdas de ancho y 1 de alto) con el logo, las metas semanales y las próximas materias.

## What Changes

- **Un único widget 1×4** que reemplaza a los tamaños 2×2 y 4×2 (decisión del Product Owner, 2026-11-22). De izquierda a derecha:
  - el **logo** de StudyPlan;
  - las **metas semanales**: un anillo pequeño con el porcentaje y «horas realizadas / meta»;
  - las **2 próximas materias**, cada una con su color, nombre, día y hora.
- Tema claro y oscuro con la línea gráfica índigo vigente; estados vacíos con texto: sin perfil, sin meta semanal y sin sesiones próximas.
- **Tocar el widget abre la pantalla de inicio** (HU-05, segundo escenario). Si el bloqueo con huella o PIN (HU-04) está activo, se aplica el flujo de arranque existente.
- Se actualiza cuando cambian los datos de los que depende (sesiones, metas, perfil y materias) y cada 30 minutos para el cambio de día y de semana.
- **Lectura ampliada:** `ResumenInicio` incorpora `proximasSesiones()` (hasta 2). Es un cambio pequeño y compatible en el modelo de lectura y en `ObtenerResumenInicioService`; Inicio sigue usando `proximaSesion()` sin cambios.
- **RNF-19 cambia en el documento** (capítulo 3): «widget de 2×2 y 4×2 celdas» pasa a «widget de 1×4 celdas». RNF-19 no está en `openspec/specs`, solo en el capítulo 3.
- Adaptador nuevo en `adapter.in.ui.configuracion`. Sin dependencias ni permisos nuevos.
- Cadenas en español e inglés, descripciones de accesibilidad y vista previa del widget.
- Documentación: sección del widget en el capítulo 4 y fila de RNF-19 en el capítulo 3; bitácora de auditoría IA.
- Este change **no cambia ninguna HU ni RN**: implementa HU-05 tal como está en la spec (`skip_specs: true`).

**Fuera de alcance** (no son HU aprobadas; si el Product Owner las quiere, van en una HU aparte):
- Mostrar la sesión en curso («Estudiando · 18:42») en el widget.
- Ocultar horas y materias cuando el bloqueo con huella o PIN está activo.
- Un botón «Estudiar ahora» dentro del widget y otros tamaños.
- Elegir qué mostrar (pantalla de configuración del widget).

## Capabilities

### New Capabilities
<!-- Ninguna: HU-05 ya existe en perfil-ajustes. -->

### Modified Capabilities
<!-- Ninguna: no cambian requisitos de las specs (skip_specs). -->

## Impact

- Requisitos: HU-05 (Could) y RNF-19 (texto del capítulo 3); RNF-06 (accesibilidad), RNF-15 y RNF-16. No cambia ninguna HU ni RN.
- Código: `ResumenInicio` y `ObtenerResumenInicioService` (la lista de próximas); en `adapter.in.ui.configuracion`, el proveedor del widget, el modelo de presentación, el dibujante del anillo y el observador; 1 layout, 1 archivo `appwidget-provider` (con variante para Android 12), cadenas y un receptor en el manifiesto; registro del observador en `StudyPlanApplication`.
- Pruebas: JVM del servicio y del modelo; instrumentadas que inflan el layout y comprueban el refresco.
- Documentos: capítulos 3 y 4, bitácora.
- Riesgo principal: una fila de alto 1 es justa para el texto y cada lanzador mide las celdas a su manera; solo se comprueba de verdad en el teléfono.
