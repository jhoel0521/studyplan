# Proposal

## Why

La app usa hoy el tema Material 3 por defecto (`Theme.Material3.DayNight.NoActionBar` sin atributos propios) y no tiene identidad visual propia: `colors.xml` solo guarda la paleta provisional de materias y `values-night/themes.xml` no declara ningún atributo. El Product Owner definió en Claude Design la línea gráfica «Índigo Digital» (3 guías y 15 pantallas, claro y oscuro) y quiere aplicarla a toda la app, solo móvil, antes de la evaluación de usabilidad.

## What Changes

- **Tokens:** roles de color Material 3 en claro y oscuro (`colorPrimary` índigo, `colorSecondary` verde azulado, contenedores, superficies, `colorOutlineVariant`, `colorError`) y un atributo propio `colorWarning` (ámbar, sesión en pausa). Fuente única: `.ux/standard/tokens.json` (estándar 0.2.2, 43 pares de contraste verificados). Se corrige `danger` claro de `#DC2626` a `#B91C1C`, porque no llegaba a 4,5:1 sobre `bg` ni sobre `surface-variant`.
- **Estilos base:** un único `Widget.StudyPlan.Button` (48 dp de alto, radio 14 dp, texto 16 sp; la altura es un único valor ajustable) del que heredan todos los botones, con variantes que solo cambian el estilo; tarjetas con elevación 0 dp y borde de 1 dp; formas (tarjeta 20 dp, botón 14 dp, chip 8 dp); tipografía Plus Jakarta Sans (títulos y números) + Roboto (cuerpo).
- **Pantallas** (una por tarea, solo restilización): Inicio, selector «Estudiar ahora», sesión en curso (cronómetro de 56 sp con un anillo de progreso por fase, elemento visual nuevo), recuperación al volver (W13), repartir tramo (W14), detalle y eventos (W15), Estadísticas y Materias, más el bloque horario del calendario (el color de materia deja de pintar su borde y queda solo como barra) y el formulario de materia y el de sesión, que usan estilos que cambian.
- **Reglas de uso:** el color de la materia es solo acento (barra de 4 dp, punto de 10 dp, chip al 15 %), nunca fondo de tarjeta ni de botón, y se ajusta en claro y oscuro para llegar a 3:1; la importancia de una acción se marca solo por estilo (relleno, tonal, contorno, texto), nunca por tamaño; sin hex sueltos en layouts.
- **Revisión del Product Owner en el emulador (2026-11-20):**
  - El saludo de Inicio ocupa como máximo dos líneas y termina en «…»; un nombre muy largo ya no rompe la pantalla (HU-02).
  - Ningún botón cambia de altura: se quita la partición de palabras con guion; las etiquetas van en una línea y se reducen hasta 12 sp si no caben, y en el formulario de materia Guardar, Archivar y Eliminar pasan a iconos con su nombre accesible (HU-03).
  - «Estudiar ahora» muestra la materia elegida y empieza solo con un botón «▶ Comenzar» (HU-47).
  - Si hay una sesión En curso, Inicio la muestra con «Volver a la sesión» y el botón flotante abre esa sesión en vez de intentar otra; esto corrige un incumplimiento del escenario vigente «Ya hay una sesión En curso» de HU-47 (HU-02, HU-47).
- **Revisión final:** `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew test`, pruebas instrumentadas de humo y verificación de contraste en ambos temas.

El cambio es sobre todo visual. Por decisión del Product Owner del 2026-11-20 incluye además dos ajustes de flujo en «Estudiar ahora» y en el aviso de sesión en curso de Inicio, que amplían el resumen de Inicio con la sesión En curso sin tocar la base de datos (el color de materia sigue guardándose como ARGB). No hay cambios **BREAKING**.

## Capabilities

### New Capabilities
<!-- Ninguna: no existe una HU de identidad visual y la IA no la inventa. Si el Product Owner crea una, este cambio se reparte entre ella y perfil-ajustes. -->

### Modified Capabilities
- `perfil-ajustes`: HU-02 (Ver resumen en la pantalla de inicio): aviso de sesión En curso con «Volver a la sesión» y saludo limitado a dos líneas.
- `tecnicas-estudio`: HU-47 (Estudiar ahora sin planificar): materia elegida visible, botón «▶ Comenzar» y, con una sesión En curso, el botón flotante abre esa sesión.
- `perfil-ajustes`: HU-03 (Configurar tema e idioma). El tema claro, oscuro y del sistema deben mostrar la línea gráfica Índigo Digital con contraste ≥ 4,5:1 en texto (RNF-06, RNF-07), sin mezclar la paleta de la materia con la del tema, con botones uniformes y legible con texto ampliado.

Requisitos no funcionales afectados: RNF-06 (accesibilidad, contraste, estados no solo por color, 48 dp), RNF-07 (estética coherente en claro y oscuro con Material 3), RNF-08 (textos solo en `strings.xml`, sin romper es/en).

## Out of Scope

- **Funcionalidad nueva:** «recientes primero» y buscador del selector con más de 7 materias, estados de carga y de error en `EstadisticasViewModel` y `ListaMateriasViewModel`, y un diálogo posterior a la sesión. Quedan para un change futuro con HU/RN aprobadas por el Product Owner. (El botón «▶ Comenzar» del selector y el aviso de sesión en curso sí entran, por la revisión del 2026-11-20.)
- **Pantallas anchas (`layout-w600dp/`):** no se rediseñan. Solo se reemplazan estilos y alturas fijas en los 4 archivos que ya los declaran, sin IDs nuevos (el cambio es solo móvil).
- **Pantallas no listadas** (calendario salvo su bloque horario, reportes, ajustes, autenticación, historial de metas): reciben el tema por herencia y los estilos explícitos se reemplazan de forma mecánica; solo se corrige lo que lint o la revisión visual marquen.
- Paleta definitiva de colores de materia (sigue como decisión aparte del Product Owner, ver comentario de `colors.xml`).
- Colores del PDF/CSV exportado (`ExportadorReporteSistema`).
- Ícono del launcher (fondo `#3DDC84`): decisión pendiente.
- Dependencias de librería nuevas.

## Impact

- Recursos nuevos: `values/dimens.xml`, `values/attrs.xml`, `values/styles.xml`, `values-night/colors.xml`, `res/font/` (4 archivos). Modificados: `values/colors.xml`, `values/themes.xml` y 26 layouts de `res/layout/` más 4 de `res/layout-w600dp/` (lista exacta en design.md, «Target Scope & File Boundary»).
- Java (acotado a presentación): `ColorMateria` y `RolColorEstado` nuevos, y colores o estilos de texto en `EstadoSesionVisual`, `CalendarioActivity`, `CalendarioMesAdapter`, `EstadisticasActivity`, `EstadisticasAdapter`, `ReportesAdapter`, `MateriasAdapter`, `MainActivity`, `SelectorEstudioBottomSheet`, `SesionDetalleActivity`, `SesionEnCursoActivity`, `RepartoSesionActivity` y `FelicitacionMetasActivity`. `domain/`, `application/` y `adapter/out/` no se tocan.
- Dependencias: ninguna librería. La fuente Plus Jakarta Sans se embebe en `res/font/` (licencia OFL, guardada en `docs/licencias/`), aprobada por el Product Owner el 2026-11-19 (AGENTS.md §2).
- Revisión del 2026-11-20: `ResumenInicio` y `ObtenerResumenInicioService` (la sesión En curso, con el puerto existente `SesionRepository.buscarEnCurso()`), `InicioViewModel`, `MainActivity`, `SelectorEstudioBottomSheet`, `GrupoBotones`, layouts de Inicio, del selector y del formulario de materia, 3 iconos vectoriales nuevos y las pruebas Espresso que tocaban una fila del selector para empezar.
- Pruebas: JUnit de contraste, de reglas de estilo, del mapeo de estados y del helper de color de materia; instrumentada de humo en claro y oscuro.
- Trazabilidad/auditoría: bitácora `docs/auditoria-ia/bitacora.md` y capítulo 4 del documento (diseño visual).
