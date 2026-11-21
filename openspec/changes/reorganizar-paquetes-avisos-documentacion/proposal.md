# Proposal

## Why

La arquitectura hexagonal de StudyPlan está bien separada en capas, pero dentro de cada capa los archivos están sueltos: `domain` tiene 71 archivos y 0 subcarpetas, `application.port.in` 46, `application.service` 50, `adapter.in.ui` 70 y `adapter.out.persistence` 27. Nadie que abra el proyecto puede distinguir una entidad de un value object, un enum, un contrato (puerto) o una clase de lectura. Además, Android Studio muestra avisos del inspector de código (por ejemplo `fechaLocal` y `horaLocal` con posible `NullPointerException`, o `conTecnica` nunca usado) que `lint` de Gradle no cuenta, y el capítulo 4 del documento creció épica por épica sin explicar la estructura. El Product Owner pidió un solo change, por etapas, con tres objetivos.

## What Changes

- **Etapa 1 a 3, reorganizar paquetes por tipo y por caso de uso (sin cambiar el comportamiento):**
  - `domain` se divide por tipo: `entity`, `valueobject`, `enumeration`, `state`, `service`, `result` y `readmodel`.
  - `application.port.in` y `application.service` se dividen por épica: `configuracion`, `materias`, `sesiones`, `calendario`, `tecnicas`, `metas`, `estadisticas`, `reportes`, `recordatorios` y `comun`. `application.port.out` se divide en `persistence` y `system`.
  - `adapter.in.ui` se divide por la misma lista de épicas más `comun`; `adapter.out.persistence` se divide en `entity`, `dao` y `repository`; `adapter.out.system` y `security` se quedan planos.
  - Lo que el manifiesto nombra y el sistema persiste (`StudyPlanApplication`, `ReceptorAdministradorBloqueo`, `ReceptorAvisosSesion`, `ServicioSesionEnCurso`) y `StudyPlanDatabase` no se mueven.
  - Se actualizan manifiesto, layouts (`tools:context`), pruebas, scripts de documentación y las frases de `AGENTS.md` §6, §6.1 y §6.2 que citan paquetes.
  - **Excepción declarada:** 21 clases con visibilidad de paquete fuera de `domain` pasan a `public` o se agrupan junto a su único cliente; es el único cambio de código de esas etapas.
- **Etapa 4, eliminar los avisos del inspector de Android Studio:** línea base con el inspector por línea de comandos (o exportada por el Product Owner) y un perfil de inspección identificado; triaje que revisa el Product Owner; se corrigen los avisos reales (posibles `NullPointerException`, código sin uso, otros) y los demás se justifican por escrito. Meta: todos los avisos de la línea base resueltos o justificados, y `lint` sin errores. De los 53 avisos de `lint` de Gradle solo se corrigen los de una línea.
- **Etapa 5, finalizar la documentación:** sección nueva «Estructura de paquetes y tipos de clase» en el capítulo 4 (qué es un enum, una entidad, un value object, un contrato y un servicio, con un ejemplo real de cada uno); capítulo 4 ordenado de forma coherente; wireframes del Anexo A trasladados donde indica la marca pendiente; diagramas de clases y cotejo al día; bitácora de auditoría IA.
- Este change **no cambia ningún requisito**: es un refactor, una limpieza y documentación (`skip_specs: true`).

**Fuera de alcance:**
- Cambiar el comportamiento, las pantallas, la base de datos o su esquema (`StudyPlanDatabase` no cambia de paquete) y mover componentes que el sistema guarda por nombre de clase.
- Quitar `ServicioSesionEnCurso`, implementar el respaldo en Google Drive o escribir capítulos de entregas futuras.
- Decidir citas y referencias bibliográficas: las inconsistencias del capítulo 3 y de `90-referencias.md` se listan para que las resuelva el autor; la IA no inventa referencias.
- Nuevas dependencias (el change no añade ninguna), un comparador de firmas, un modo de reversión del script y módulos Gradle.
- Crear la carpeta `adapter.out.drive`: el respaldo en Drive no existe todavía.

**Precondición:** el change `ajustar-notificacion-sesion-sin-reproductor` debe estar archivado y commiteado antes de mover archivos, para no mezclar un refactor grande con trabajo abierto.

## Capabilities

### New Capabilities
<!-- Ninguna: no cambia el comportamiento del sistema. -->

### Modified Capabilities
<!-- Ninguna: refactor, limpieza de avisos y documentación (skip_specs). -->

## Impact

- Requisitos: RNF-15 (reglas de dominio aisladas y probadas, mantenibilidad) y RNF-16 (cada cambio rastreable); no cambia ninguna HU ni RN.
- Código: unas 280 clases de `domain`, `application`, `adapter.in.ui` y `adapter.out.persistence` cambian de paquete; el manifiesto (referencias a las Activities) y 47 layouts con `tools:context`; todas las pruebas JVM e instrumentadas siguen la nueva estructura.
- Herramientas y documentos: `docs/build/cotejo_clases.py`, `docs/build/diagramas_clases.py`, `docs/build/comparar_pruebas.py` (nuevo), `AGENTS.md` §6, §6.1 y §6.2, capítulo 4, bitácora. El mapa y el script de movimiento son temporales y se retiran al cerrar la etapa 3.
- Riesgo principal: cambio mecánico muy grande; se hace por etapas, con compilación y pruebas al cierre de cada una.
