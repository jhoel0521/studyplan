# Proposal

## Why

Sesiones, calendario, metas por materia y estadísticas (E3, E4, E6 y E7) necesitan una materia a la cual asociarse, y la pestaña «Materias» de la barra de navegación ya existe pero está deshabilitada. Por orden del backlog y por dependencias, la siguiente épica es **E2 Gestión de materias** (HU-06 a HU-09). Es pequeña, no depende de sesiones y obliga a resolver dos decisiones que heredarán todas las épicas siguientes: la **primera migración de Room** (v1 a v2) y la **navegación entre pestañas**.

## What Changes

- Pantalla **Materias** (pestaña de la barra de navegación): lista alfabética de materias activas con color, nombre, docente y horas realizadas, sección plegable «Archivadas (n)» y botón «+» (HU-09, wireframe W2). **Parcial**: sin sesiones, las horas realizadas son siempre `0 h`.
- Pantalla **Formulario de materia** para crear y editar (HU-06, HU-07): nombre (1 a 50 caracteres), color y docente opcional. El nombre se **sanea antes de guardarse**: se recortan los espacios de los extremos y se colapsan los repetidos; la unicidad entre las materias activas (RN-02) se decide con una clave normalizada (sin acentos ni distinción de mayúsculas, en mayúsculas) que también se guarda. Esta ampliación de RN-02 la pidió el Product Owner el 14-nov-2026.
- **Color:** el estudiante elige cualquier color en un modal como el `input type="color"` de HTML (selector de matiz y saturación, barra de brillo, campo hexadecimal y ocho colores rápidos); el formulario solo muestra un círculo con el color y su código. El color se guarda como entero ARGB opaco. Los ocho colores rápidos son provisionales: el Product Owner define la paleta definitiva en una especificación final.
- **Botones del formulario:** «Guardar», «Archivar» y «Eliminar» se presentan como un grupo de botones unidos, como el `btn-group` de Bootstrap.
- **Barra de navegación:** se corrige una franja en blanco doble bajo la barra (el relleno de los botones del sistema se aplicaba dos veces).
- **Docente:** es solo un texto opcional de la materia (no existe un rol docente) y no tiene límite de caracteres definido.
- **Archivar** y **eliminar** desde el formulario (HU-08). La regla RN-03 se implementa en el núcleo con un puerto `SesionesDeMateria`; mientras no existan sesiones, su adaptador responde siempre «cero sesiones». **Parcial**: los escenarios que dependen de sesiones reales los completa el change de sesiones (ver `trazabilidad.md`).
- **Room versión 2** con la tabla `materia` y la primera migración escrita a mano (`MIGRATION_1_2`), probada con `room-testing`. No hay migración destructiva.
- **Navegación entre pestañas**: una Activity por pestaña, sin dependencias nuevas, con el destino «Materias» habilitado y una regla de pila de retroceso común a todas las épicas (ver `design.md`, D3).
- **Desarchivar** una materia (HU-46, nueva): la pidió el Product Owner para practicar. La redactó la IA como un delta de la capacidad `gestion-materias` (ver `specs/`); la prioridad y los puntos son propuestos.
- Diagramas y documento: modelo lógico y diccionario de datos generados desde el esquema de Room (4.2), diagrama de clases de E2 (4.4), diagrama de secuencia «registrar materia», flujo de pantallas actualizado. Las capturas reales del producto **no** se incluyen en el documento todavía: el diseño visual final se define en una especificación propia.

**Fuera de alcance:** sesiones y horas reales (E3), color de la materia en el calendario (E4), meta semanal por materia (HU-26, E6), copia de seguridad.

## Capabilities

### New Capabilities
- Ninguna.

### Modified Capabilities
- `gestion-materias`: se agrega HU-46.

Este change implementa los requisitos de la capacidad `gestion-materias` de `add-requisitos-base` (enlazados en `trazabilidad.md`; la línea base no se archiva hasta cerrar la épica, «todo o nada») y agrega **un requisito nuevo, HU-46 Desarchivar materia**, como delta en `specs/gestion-materias/spec.md`. También amplía RN-02 (saneamiento del nombre) en el capítulo 3 del documento.

## Impact

- **Código:** `domain` (Materia), `application` (casos de uso de materias y los puertos `MateriaRepository` y `SesionesDeMateria`), `adapter.out.persistence` (entidad, DAO, migración), `adapter.in.ui` (dos Activities, ViewModels, adaptador de la lista y la navegación entre pestañas).
- **Datos:** Room pasa de la versión 1 a la 2. Los datos del perfil y los ajustes se conservan.
- **Dependencias (requieren aprobación humana):** `androidx.room:room-testing` 2.8.5 (solo pruebas instrumentadas; verificada hoy en el repositorio de Google, la misma versión que Room). `RecyclerView` llega ya como dependencia transitiva de Material. **`com.github.skydoves:colorpickerview` 2.5.0** (Maven Central, Apache-2.0, minSdk 21; versión verificada el 14-nov-2026 en `maven-metadata.xml`, publicada el 16-sep-2026), para las vistas del selector de color del modal: la aprobó el estudiante el 14-nov-2026 al elegir esa opción y aprobar el plan. Es una librería de un tercero para algo pequeño.
- **Documento:** secciones 4.2.1, 4.2.2, 4.3 y 4.4 del capítulo 4; la bitácora se amplía.
- **Sin cambios de requisitos:** las HU, RN y RNF son las de la línea base.

## Decisiones del Product Owner (14-nov-2026)

- Nombre de materia: único y guardado saneado (recorte, espacios repetidos y clave sin acentos ni mayúsculas).
- Paleta de colores: se define al final en una especificación propia; ahora es provisional.
- Docente: texto libre opcional, sin límite de caracteres, sin rol docente.
- Desarchivar: sí, para practicar (HU-46).

## Pregunta abierta

- ¿Se aprueba `room-testing` 2.8.5 para probar la migración de la base? La alternativa sin dependencia nueva es construir a mano la base v1 en una prueba, que no valida el esquema contra el JSON versionado.
- Prioridad y puntos de HU-46 (propuestos: Could, 2 puntos).
