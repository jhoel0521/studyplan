# Design

## Problem Statement

La app usa el tema Material 3 por defecto. No tiene identidad visual propia, y el Product Owner definió «Índigo Digital» (paleta clara y oscura, tipografía, formas y reglas de uso) para toda la app, solo móvil. Hay que llevar esa línea gráfica al código **sin cambiar el comportamiento funcional**: el cambio toca recursos (`res/`), un puñado de clases de presentación que pintan colores y las pruebas que lo verifican. Motivación y alcance: ver [proposal.md](proposal.md).

## Context

Estado observado en el repositorio (verificado el 2026-11-19):

- `res/values/themes.xml`: `Base.Theme.StudyPlan` hereda de `Theme.Material3.DayNight.NoActionBar` sin atributos propios, y define `Widget.StudyPlan.BotonGrupo` (`minHeight 48dp`, `strokeWidth 1dp`, `strokeColor ?attr/colorOnPrimary`), usado por `activity_materia_formulario.xml` a través de `GrupoBotones.unir(...)` (que fuerza esquinas con `RelativeCornerSize(0.5f)` / `AbsoluteCornerSize(0)`).
- `res/values-night/themes.xml`: declara `Base.Theme.StudyPlan` sin ningún `<item>`. `res/values/colors.xml`: solo los 8 colores `materia_*`. No existen `dimens.xml`, `attrs.xml`, `styles.xml` ni `res/font/`.
- Estilos explícitos en los layouts de `res/layout/` y `res/layout-w600dp/`: `Widget.Material3.Button.OutlinedButton` (22), `Widget.Material3.CardView.Outlined` (14), `?attr/materialButtonOutlinedStyle` (12), `Widget.Material3.Button.TonalButton` (10), `Widget.Material3.Button.IconButton.Outlined` (6), `Widget.Material3.Button.IconButton` (6), `Widget.StudyPlan.BotonGrupo` (3), `?attr/materialCardViewOutlinedStyle` (3), `Widget.Material3.Button.TextButton` (2), `Widget.Material3.CardView.Elevated` (1, `item_bloque_calendario.xml`). Un estilo explícito **no hereda** del tema: hay que reemplazarlo.
- No hay hex reales en los layouts: solo `tools:background` y `tools:tint` de vista previa. Hay valores fijos `layout_height="48dp"` en 3 `IconButton.Outlined` de `activity_sesion_formulario.xml` (y los mismos 3 en `layout-w600dp/`).
- Colores en Java que hay que alinear: `EstadoSesionVisual` (mapa de 6 estados a atributos: `PENDIENTE` y `COMPLETADA` → `colorPrimary`, `EN_CURSO` → `colorTertiary`, `NO_REALIZADA` → `colorError`, `COMPLETADA_TARDIA` → `colorSecondary`, `REPROGRAMADA` → `colorOnSurfaceVariant`); `CalendarioMesAdapter` (`colorPrimaryContainer`, `colorSurface`, `colorPrimary`, `colorOutlineVariant`); `CalendarioActivity` (`MaterialColors.getColor` y `tarjeta.setStrokeColor(item.colorMateria())`); `EstadisticasActivity` (colores del `PieChart` con `m.colorMateria()`); `MateriasAdapter`, `EstadisticasAdapter`, `ReportesAdapter`, `MainActivity`, `SesionDetalleActivity` (indicador de color de materia por `setColorFilter`/`ColorStateList`).
- `CalendarioActivity` (L625, L629) y `FelicitacionMetasActivity` (L63) usan `TextAppearance_Material3_*` de la librería directamente, no atributos del tema.
- `minSdk 24`: `android:textFontWeight` exige API 28; el peso se elige con una fuente estática por peso.
- `ActividadProtegida` oculta la vista raíz (`View.INVISIBLE`) hasta autenticar; las pruebas instrumentadas usan `Pruebas.reiniciarEstado()`.
- Pruebas JVM existentes: `ArquitecturaTest` (sin Robolectric, sin `org.json`); `app/build.gradle.kts` solo declara `junit` y `arch.core.testing` en `testImplementation`.
- `activity_sesion_en_curso.xml` **no tiene hoy ningún indicador de progreso**: solo `textoFasePomodoro`, `textoCronometro`, `textoEstadoEnCurso` y `textoEstadoNoMolestar`. El anillo del artboard es un elemento visual nuevo.
- Estándar de diseño vigente: `.ux/standard/` 0.2.2 (tokens con contraste verificado: 43 pares, claro y oscuro). `density: comfortable`, entrada táctil, solo móvil.

## Goals / Non-Goals

### Goals

- Que el tema entregue la identidad visual por atributos: ningún layout nombra un color ni una forma.
- Un solo estilo base de botón y una sola forma de tarjeta, con variantes que solo cambian el estilo.
- Que el color de materia sea solo acento (barra de 4 dp, punto de 10 dp, chip al 15 %), legible en claro y oscuro.
- Verificación automática de contraste y de las reglas de uso.

### Non-Goals

- Quedan fuera los «recientes primero» y el buscador del selector, los estados de carga y de error de `EstadisticasViewModel` y `ListaMateriasViewModel` y un diálogo posterior a la sesión (change futuro con HU/RN aprobadas). La revisión del Product Owner del 2026-11-20 sí incorpora el botón «▶ Comenzar» del selector y el aviso de sesión en curso (decisiones 20 y 21).
- No se rediseña `layout-w600dp/`: solo se reemplazan estilos y alturas fijas en 4 archivos que ya los declaran, sin añadir IDs.
- No se cambia el modelo del color de materia (ARGB en Room) ni su paleta provisional.
- No se cambia el ícono del launcher (`ic_launcher_background.xml` sigue en `#3DDC84`): queda pendiente, decisión del Product Owner.
- Sin Compose, Material You dinámico ni librerías nuevas.

## Decisions

Cada decisión indica la alternativa descartada. Las del Product Owner están marcadas y fechadas.

### 1. Paleta: roles en `colors.xml` (claro y oscuro) y un solo mapeo en `themes.xml`

`res/values/colors.xml` declara un color por rol del estándar con el nombre `sp_<rol>` (guiones convertidos en guion bajo) y `res/values-night/colors.xml` redefine **los mismos nombres** con los valores oscuros. `res/values/themes.xml` asigna cada `@color/sp_*` a su atributo una sola vez; así `values-night/themes.xml` no necesita repetir `<item>`. Los `materia_*` no se duplican en `values-night/`. La fuente de valores es `.ux/standard/tokens.json`.

| Rol del estándar (`semantic.<modo>.color.*`) | Recurso | Atributo del tema |
|---|---|---|
| `bg` | `sp_bg` | `android:colorBackground` |
| `surface` | `sp_surface` | `colorSurface`, `colorSurfaceContainerLowest`, `colorSurfaceContainerLow` |
| `surface-variant` | `sp_surface_variant` | `colorSurfaceContainer`, `colorSurfaceContainerHigh`, `colorSurfaceContainerHighest`, `colorSurfaceVariant` |
| `text` | `sp_text` | `colorOnSurface`, `colorOnBackground` |
| `text-muted` | `sp_text_muted` | `colorOnSurfaceVariant` |
| `border` | `sp_border` | `colorOutline` |
| `border-subtle` | `sp_border_subtle` | `colorOutlineVariant` |
| `action` | `sp_action` | `colorPrimary` |
| `on-action` | `sp_on_action` | `colorOnPrimary` |
| `action-container` | `sp_action_container` | `colorPrimaryContainer` |
| `on-action-container` | `sp_on_action_container` | `colorOnPrimaryContainer` |
| `success` (verde azulado) | `sp_success` | `colorSecondary` y `colorTertiary` |
| `on-success` | `sp_on_success` | `colorOnSecondary` y `colorOnTertiary` |
| `secondary-container` | `sp_secondary_container` | `colorSecondaryContainer` y `colorTertiaryContainer` |
| `on-secondary-container` | `sp_on_secondary_container` | `colorOnSecondaryContainer` y `colorOnTertiaryContainer` |
| `danger` | `sp_danger` | `colorError` |
| `on-danger` | `sp_on_danger` | `colorOnError` |
| `surface-variant` (reutilizado) | `sp_surface_variant` | `colorErrorContainer` |
| `danger` (reutilizado) | `sp_danger` | `colorOnErrorContainer` |
| `warning` | `sp_warning` | atributo propio `colorWarning` |
| `on-warning` | `sp_on_warning` | atributo propio `colorOnWarning` |
| `ring-track` | `sp_ring_track` | usado directo por el estilo del anillo |
| `focus` | `sp_action` | sin atributo propio (usa `colorPrimary`) |
| `text` / `surface` (invertidos) | `sp_text`, `sp_surface` | `colorSurfaceInverse`, `colorOnSurfaceInverse`; `colorPrimaryInverse` = `sp_action` |

`colorTertiary*` se iguala a `colorSecondary*` para que ningún componente herede el morado por defecto de Material. `colorErrorContainer` y `colorOnErrorContainer` reutilizan colores ya verificados (`danger` sobre `surface-variant` pasa 5,47:1 en claro y se verifica en oscuro) para no inventar un color nuevo. `colorWarning` y `colorOnWarning` se declaran en `res/values/attrs.xml` (`format="color"`).

Los valores escalares van en `res/values/dimens.xml`: `button_height` = 48 dp, `radius_card` = 20 dp, `radius_button` = 14 dp, `radius_chip` = 8 dp, `radius_card_compact` = 8 dp, `spacing_screen` = 16 dp, `spacing_card_gap` = 12 dp, y `chip_materia_alpha` = 0,15 (`<item format="float" type="dimen">`).

*Alternativa descartada:* hex dentro del tema. Duplicaría la fuente de valores y no se podría comprobar contra `tokens.json`.

### 2. Botones: un estilo base y variantes por estilo (decisión del Product Owner, 2026-11-19: 48 dp)

`Widget.StudyPlan.Button` (padre `Widget.Material3.Button`): `android:minHeight` = `@dimen/button_height`, `android:insetTop` y `android:insetBottom` = 0 (así el aspecto visual mide lo mismo que el área táctil), `cornerRadius` = `@dimen/radius_button`, `android:textAppearance` = `@style/TextAppearance.StudyPlan.Button` (16 sp). La altura es `minHeight`, nunca `layout_height`, para que crezca con la fuente.

| Estilo | Padre | Uso |
|---|---|---|
| `Widget.StudyPlan.Button` | `Widget.Material3.Button` | acción primaria (relleno) |
| `Widget.StudyPlan.Button.Tonal` | `Widget.Material3.Button.TonalButton` | acción secundaria |
| `Widget.StudyPlan.Button.Outlined` | `Widget.Material3.Button.OutlinedButton` | acción de apoyo |
| `Widget.StudyPlan.Button.Text` | `Widget.Material3.Button.TextButton` | acción mínima y botones de diálogo |
| `Widget.StudyPlan.Button.Destructive` | `Widget.StudyPlan.Button.Outlined` | contorno y texto en `colorError` |
| `Widget.StudyPlan.Button.Icon` | `Widget.Material3.Button.IconButton` | solo icono: `wrap_content`, `minWidth` y `minHeight` = `@dimen/button_height` |
| `Widget.StudyPlan.Button.Icon.Outlined` | `Widget.Material3.Button.IconButton.Outlined` | solo icono con contorno |
| `Widget.StudyPlan.BotonGrupo` | `Widget.StudyPlan.Button` | se conserva con `strokeWidth 1dp` y `strokeColor ?attr/colorOnPrimary`; `GrupoBotones` no cambia |

Enlaces en el tema: `materialButtonStyle` → `Widget.StudyPlan.Button`, `materialButtonOutlinedStyle` → `.Outlined`, `materialButtonTonalStyle` → `.Tonal`, `materialIconButtonStyle` → `.Icon`, `materialIconButtonOutlinedStyle` → `.Icon.Outlined`, `borderlessButtonStyle` → `.Text`. Como un `style=` explícito no hereda del tema, cada `Widget.Material3.Button.*` explícito de los layouts se reemplaza por el `Widget.StudyPlan.Button.*` equivalente (tabla de arriba). Los botones de los diálogos (`MaterialAlertDialog`) toman `.Text` mediante un `ThemeOverlay.StudyPlan.MaterialAlertDialog` enlazado con `materialAlertDialogTheme` (`buttonBarPositiveButtonStyle`, `buttonBarNegativeButtonStyle`, `buttonBarNeutralButtonStyle`).

`ExtendedFloatingActionButton`: `Widget.StudyPlan.ExtendedFab` (padre `Widget.Material3.ExtendedFloatingActionButton.Icon.Primary`, que ya usa `colorPrimaryContainer` y `colorOnPrimaryContainer`), `minHeight` = `@dimen/button_height`, forma de `@dimen/radius_button`. `BottomNavigationView`: `Widget.StudyPlan.BottomNavigation` con `itemActiveIndicatorStyle` propio (indicador de 64 × 32 dp en `colorPrimaryContainer`; por defecto Material usaría `colorSecondaryContainer`).

`MaterialButtonToggleGroup` (selector de técnica): sus botones usan `.Outlined` sin sobrescribir `cornerRadius`, y el grupo conserva las esquinas interiores rectas de Material; es la única excepción a la separación entre botones de 8 dp.

Si 48 dp se ve grande, se cambia `button_height`. Por debajo de 48 dp solo se puede acortar el dibujo con `insetTop`/`insetBottom` manteniendo 48 dp de área táctil (AGENTS.md §6.2).

*Alternativas descartadas:* 52 dp (guía de Claude Design) por preferencia del Product Owner; enlazar solo `materialButtonStyle` (dejaría sin efecto los botones con estilo explícito).

### 3. Tarjetas por estilo de tema

`Widget.StudyPlan.Card` (padre `Widget.Material3.CardView.Outlined`): `cardElevation` 0 dp, `strokeWidth` 1 dp, `strokeColor` `?attr/colorOutlineVariant`, `cardBackgroundColor` `?attr/colorSurface`, forma de `@dimen/radius_card`. Se enlaza con `materialCardViewStyle` y `materialCardViewOutlinedStyle`; los 14 `Widget.Material3.CardView.Outlined` explícitos pasan a `Widget.StudyPlan.Card`. `Widget.StudyPlan.Card.Compact` (forma de `@dimen/radius_card_compact`) reemplaza a `Widget.Material3.CardView.Elevated` de `item_bloque_calendario.xml` y retira sus `cardCornerRadius="6dp"` y `cardElevation="1dp"` (el bloque horario es proporcional a la duración y queda exento de 48 dp por AGENTS.md §6.2).

### 4. Tipografía por fuentes estáticas, sin `fontFamily` en la raíz del tema

Plus Jakarta Sans (aprobada por el Product Owner el 2026-11-19; licencia OFL) entra como cuatro archivos en `res/font/`: `plus_jakarta_sans_medium.ttf` (500), `plus_jakarta_sans_semibold.ttf` (600), `plus_jakarta_sans_bold.ttf` (700) y `plus_jakarta_sans_extrabold.ttf` (800). El 600 se usa en las filas del selector de materias. Roboto no se embebe: viene con Android (`sans-serif` para 400 y `sans-serif-medium` para 500). No se declara `fontFamily` en la raíz del tema para no contaminar el cuerpo. El texto de la licencia va en `docs/licencias/OFL-PlusJakartaSans.txt`, fuera de `res/` (un `.txt` dentro de `res/font/` rompe `aapt2`).

Los 13 `textAppearance` del tema que usan los layouts se redefinen con estilos `TextAppearance.StudyPlan.*`:

| Atributo del tema | Familia y peso | Tamaño | Uso |
|---|---|---|---|
| `textAppearanceDisplayMedium` | Jakarta 800 | 56 sp | cronómetro |
| `textAppearanceHeadlineLarge` | Jakarta 800 | 32 sp | horas de la semana en Inicio |
| `textAppearanceHeadlineMedium` | Jakarta 800 | 24 sp | título de pantalla, saludo |
| `textAppearanceHeadlineSmall` | Jakarta 800 | 22 sp | título de barra y de hoja |
| `textAppearanceTitleLarge` | Jakarta 700 | 20 sp | título de sección grande |
| `textAppearanceTitleMedium` | Jakarta 700 | 16 sp | título de tarjeta |
| `textAppearanceTitleSmall` | Jakarta 700 | 14 sp | subtítulo |
| `textAppearanceBodyLarge` | Roboto 400 | 16 sp | texto destacado |
| `textAppearanceBodyMedium` | Roboto 400 | 14 sp | cuerpo |
| `textAppearanceBodySmall` | Roboto 400 | 12 sp | texto auxiliar |
| `textAppearanceLabelLarge` | Jakarta 700 | 16 sp | botones |
| `textAppearanceLabelMedium` | Roboto 500 | 12 sp | etiquetas y chips |
| `textAppearanceLabelSmall` | Roboto 500 | 12 sp | etiquetas mínimas |

`CalendarioActivity` (L625, L629) y `FelicitacionMetasActivity` (L63) dejan de usar `TextAppearance_Material3_*` y pasan a `R.style.TextAppearance_StudyPlan_*`.

### 5. Estados: dos mapeos separados

**a) Estado de la sesión (`CodigoEstadoSesion`).** Se añade el enum `RolColorEstado` (`PRIMARIO`, `SECUNDARIO`, `ERROR`, `VARIANTE`) y un método puro `EstadoSesionVisual.rolDe(CodigoEstadoSesion)` sin imports de `android.*`, para probarlo en JVM; el descriptor traduce el rol al atributo. Se retira `colorTertiary`. Iconos y etiquetas no cambian (RNF-06); el glosario usa «No realizada», nunca «vencida».

| `CodigoEstadoSesion` | Hoy | Nuevo rol → atributo |
|---|---|---|
| `PENDIENTE` | `colorPrimary` | `PRIMARIO` → `colorPrimary` |
| `EN_CURSO` | `colorTertiary` | `PRIMARIO` → `colorPrimary` |
| `NO_REALIZADA` | `colorError` | `ERROR` → `colorError` |
| `COMPLETADA` | `colorPrimary` | `SECUNDARIO` → `colorSecondary` |
| `COMPLETADA_TARDIA` | `colorSecondary` | `SECUNDARIO` → `colorSecondary` (se distingue de la anterior por icono y etiqueta) |
| `REPROGRAMADA` | `colorOnSurfaceVariant` | `VARIANTE` → `colorOnSurfaceVariant` |

**b) Fase del cronómetro (`SesionEnCursoActivity`).**

| Situación | Color del anillo y de la etiqueta | Etiqueta de texto |
|---|---|---|
| Trabajo (Pomodoro o Libre) | `colorPrimary` | «Estudiando» |
| Pausada | `colorWarning` | «En pausa» |
| Descanso (corto o largo) | `colorSecondary` | «Estudiando» con la fase «Descanso corto/largo · Ciclo n/m» en `textoFasePomodoro` |

El anillo es un `CircularProgressIndicator` (ya incluido en `material:1.10.0`) de 220 dp con `trackThickness` 14 dp, `indicatorSize` 220 dp, `trackColor` = `sp_ring_track` y progreso determinado. En Pomodoro: progreso = 1 − `milisRestantesEnFase` / duración de la fase, tomando la duración configurada (RN-17). **Regla de respaldo:** si la duración de la fase no está expuesta por `EstadoPomodoro` o por la configuración, el anillo se muestra al 100 % sin progreso y se anota como pendiente; no se cambia el dominio en este change. En la técnica Libre, sin plan, el anillo se muestra al 100 % en `colorPrimary`, sin animación. El `textoCronometro` va centrado dentro del anillo con `layout_width="0dp"`, `app:autoSizeTextType="uniform"`, `autoSizeMinTextSize="24sp"`, `autoSizeMaxTextSize="56sp"` y `maxLines="1"`; la etiqueta de estado queda fuera del texto del cronómetro.

### 6. Color de materia: un único helper puro

Se crea `ColorMateria` en `adapter.in.ui`, **sin imports de `android.*`** (aritmética sRGB y HSL con enteros ARGB) para que lo pruebe JUnit sin Robolectric. Métodos: `paraSuperficie(int argb, int superficieArgb)` devuelve el mismo color si su contraste contra la superficie es ≥ 3:1 (WCAG 1.4.11); si no, mueve la luminosidad HSL en pasos de 0,02 (hacia más oscuro sobre una superficie clara, hacia más claro sobre una oscura) hasta cumplir 3:1 o llegar al extremo, y `conAlfa(int argb, float alfa, int superficieArgb)` compone el relleno del chip. Los consumidores resuelven la superficie con `MaterialColors.getColor(vista, colorSurface)` del `Context` actual, de modo que sigue válido al rotar o cambiar de tema.

| Elemento | Estructura | Color |
|---|---|---|
| Barra | `View` de 4 dp de ancho, `margin` 14 dp arriba y abajo, esquinas de 2 dp | `paraSuperficie` |
| Punto | `View` de 10 dp con forma ovalada | `paraSuperficie` |
| Chip | `TextView` con fondo de `radius_chip` | `conAlfa(color, chip_materia_alpha, superficie)`; texto en `colorOnSurface` |
| Segmentos de reparto (W14) | barra de 36 dp de alto con esquinas de 14 dp, un tramo por materia | `paraSuperficie` |
| Porciones del `PieChart` | color de cada porción | `paraSuperficie` |
| Bloque horario del calendario | solo la franja de 4 dp; el borde pasa a `colorOutlineVariant` y se quita `setStrokeColor(item.colorMateria())` | `paraSuperficie` |

El punto actual de la materia (32/24/36 dp según pantalla) baja a 10 dp donde acompaña a un texto; el selector de color de `dialogo_color.xml` y `item_color.xml` conserva sus muestras grandes porque ahí el color es el contenido, y su marca `ic_check` usa `tools:tint` solo en vista previa y en ejecución se tiñe con el color que contraste mejor (`colorOnPrimary` o `colorOnSurface`, el que alcance ≥ 3:1 contra la muestra).

### 7. Jerarquía de acciones: se conservan los roles del código y se aplica el énfasis de la guía (Product Owner, 2026-11-19)

Una sola acción primaria (relleno) por vista; el resto va en tonal, contorno o texto. El Product Owner aprobó este criterio; la guía de Claude Design y el código actual coinciden en que en Inicio el relleno es «Nueva sesión» y «Estudiar ahora» es el FAB tonal. Un error de la primera propuesta de este change decía lo contrario y se corrige aquí.

| Pantalla | Primaria (relleno) | Secundarias |
|---|---|---|
| Inicio | «Nueva sesión» | «Estudiar ahora» (FAB tonal), «Editar meta» y «Historial de metas» (contorno) |
| Selector rápido (`bottom_sheet_selector_estudio`) | «Comenzar» | «Nueva materia» (contorno), técnica Libre/Pomodoro (grupo de botones alternables) |
| Hay sesión planificada (diálogo existente) | «Iniciar la planificada» | «Empezar sesión nueva» (contorno) |
| Sesión en curso | «Pausar» / «Reanudar» | «Detener» y «Cambiar de materia» (contorno); en los avisos, la primera acción en tonal y la segunda en contorno |
| Al volver (W13, `MaterialAlertDialogBuilder`) | «Confirmar» (botón positivo del diálogo, estilo `.Text` de diálogo) | ninguna; el diálogo conserva su `RadioGroup` y su botón único |
| Repartir tramo (W14) | «Guardar reparto» | «− 5 min» y «+ 5 min» (contorno) |
| Detalle y eventos (W15) | ninguna (pantalla de consulta) | acciones de estado en tonal; «Eliminar sesión» en contorno rojo (`.Destructive`) |
| Estadísticas y Materias | ninguna | alternadores de período y botón de agregar materia en tonal |

Los objetivos táctiles miden ≥ 48 dp con ≥ 8 dp entre ellos (excepción de §6.2 para las celdas del calendario y el grupo de botones alternables).

### 8. Pares de botones y texto ampliado

Los pares de botones en fila de `contenido_inicio`, `activity_sesion_en_curso` y `activity_reparto_sesion` pasan de `LinearLayout` con pesos a `androidx.constraintlayout.helper.widget.Flow` (`flow_wrapMode="chain"`, `wrap_content`), para que bajen de línea con la fuente más grande del sistema sin recortarse. Las tarjetas clicables usan un `AccessibilityDelegateCompat` (`ViewCompat.setAccessibilityDelegate`) que declara la clase `android.widget.Button` en `onInitializeAccessibilityNodeInfo` (no se usa `android:accessibilityClassName`, que exige API 28) y llevan un chevron `ic_chevron_der`; el borde de 1 dp (≈ 1,3:1) no basta para reconocerlas como tocables. `@android:drawable/ic_media_play` de `contenido_inicio.xml` pasa a un vector del proyecto.

### 9. Estados vacíos

Solo se restilizan los estados vacíos que ya existen en `strings.xml` (por ejemplo `inicio_sin_sesiones_hoy`, `sesion_detalle_sin_eventos`, `historial_metas_vacio`): icono + texto con colores del tema. No se añaden estados de carga ni de error (ver Non-Goals).

### 10. Visual por pantalla (transcrito del diseño de Claude Design)

Este bloque reemplaza la lectura del lienzo, que es un enlace privado. Medidas en dp.

- **Inicio:** barra con «StudyPlan» (22 sp/800); saludo «¡Hola, [Nombre]!» (24 sp/800); tarjeta de progreso a ancho completo con fondo `colorPrimary` (radio 20), «Progreso de la semana» (14 sp, blanco), «12 / 20 h» (32 sp/800) y barra de progreso de 8 dp; fila de «Editar meta» e «Historial de metas»; «Nueva sesión» con icono `+`; tarjeta de próxima sesión (barra de 4 dp, «Próxima sesión pendiente», materia, hora); título «Sesiones de hoy» (16 sp/700) y tarjetas por sesión con barra, materia, hora y chip de estado («Completada» en `colorSecondaryContainer`, «Pendiente» en `colorPrimaryContainer`); «Estudiar ahora» como FAB extendido tonal abajo a la derecha; barra inferior con píldora activa en `colorPrimaryContainer` y etiquetas «Inicio», «Calendario», «Estadíst.», «Materias».
- **Selector rápido:** hoja inferior (esquinas superiores de 28) con asa, título «¿Qué vas a estudiar?» (22 sp/800), lista de materias con botón de opción, punto de 10 y nombre (16 sp/600; fila de 52 con fondo `colorPrimaryContainer` si está elegida), «Nueva materia» en contorno, grupo «Técnica de estudio» con segmentos Libre y Pomodoro (el activo con icono de verificación y fondo `colorPrimaryContainer`) y «Comenzar» en relleno.
- **Hay sesión planificada:** diálogo centrado (radio 28) con título «¿Es tu sesión de [Materia]?», texto de apoyo y dos botones apilados: «Iniciar la planificada» (relleno) y «Empezar sesión nueva» (contorno).
- **Sesión en curso:** barra con flecha de volver y «Sesión en curso»; punto + materia + horario; línea de fase («Trabajo · Ciclo 2/4»); anillo de 220 con el cronómetro (52 a 56 sp/800) y una etiqueta de estado debajo; «Modo No molestar activo» con icono; fila «Pausar»/«Reanudar» (relleno) + «Detener» (contorno); «Cambiar de materia» (contorno); tarjetas de aviso («¿Sigues estudiando?», «Invadiendo horario de…», «Pomodoros planificados completados») con dos acciones.
- **Al volver (W13):** el diálogo existente («Saliste a las 8:45, ¿seguiste estudiando?») con sus 3 opciones y los campos de hora, motivo y nota; botón «Confirmar».
- **Repartir tramo (W14):** título, resumen del tramo («Estudiaste 10:00 – 12:10», total y extra), sesión que se cruzó en tarjeta con barra, «Repartir el tiempo» con la barra de 36 dp de dos colores y su selector de corte, horas de inicio, corte y fin, dos chips con el tiempo de cada materia, «Hora de corte», «− 5 min» y «+ 5 min», y «Guardar reparto» al pie.
- **Detalle y eventos (W15):** título «Detalle de sesión», materia, chip de estado, fecha y horario, «Planificada» y «Real estudiada», «Notas», línea de tiempo de eventos (hora y texto por evento, como hoy: `item_evento_estudio.xml` no lleva iconos y no se añaden) y «Eliminar sesión» en contorno rojo al final.
- **Estadísticas:** pestañas Semana/Mes; «Resumen» (horas y variación), «Tasa de cumplimiento» con chips Completadas, Tardías y No realizadas (con icono y texto), «Concentración», «Estudio fuera de la app», «Distribución por materia» (el `PieChart` existente, con punto, nombre y porcentaje por materia) y «Tópicos de pausas e interrupciones».
- **Materias:** lista de tarjetas con barra, nombre, docente y horas; «Archivadas (n)»; botón tonal de agregar.

### 17. Revisión del Product Owner (2026-11-20): botones de altura fija

La partición de palabras con guion (`hyphenationFrequency`, `breakStrategy`) y `GrupoBotones.permitirDosLineas(...)` hacían crecer los botones con la fuente grande («Guar-dar» en tres líneas), contra la regla de que todos miden lo mismo. Se retiran. Las etiquetas de botón van en una sola línea; en los segmentos (botones de un `MaterialButtonToggleGroup`, que ocupan el ancho con peso igual) el texto se autoajusta de 16 a 12 sp con `TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration` desde `GrupoBotones`, porque `MaterialButtonToggleGroup` fija una línea con «…» desde código y el XML no basta. Si el autoajuste no actúa con alto `wrap_content`, el alto de esos botones se fija en `@dimen/button_height` y `ReglasEstiloLayoutTest` acepta ese valor (y solo ese). Una prueba instrumentada comprueba que, con la fuente normal, todos los botones de texto visibles de cada pantalla miden `button_height`.

*Alternativa descartada:* dejar crecer el botón (rompe la regla del Product Owner).

### 18. Formulario de materia: acciones como iconos

Guardar, Archivar y Eliminar, que no cabían en pantalla chica, pasan a botones de icono de 48 × 48 dp dentro del mismo grupo (`GrupoBotones.unir` no cambia, PO-5), con tres vectores nuevos del proyecto (`ic_guardar`, `ic_archivar`, `ic_eliminar`, trazados de Material Symbols, licencia Apache 2.0) en lugar de emojis, `android:contentDescription` con el nombre de la acción (`TalkBack`) y `android:tooltipText` con el mismo texto. Eliminar conserva su confirmación.

### 19. Saludo de Inicio con nombre largo

El nombre del perfil no tiene límite al guardarse y no se recorta ni se convierte en apodo: solo al mostrarlo, `saludo` usa `maxLines="2"` y `ellipsize="end"`. El lector de pantalla recibe el texto completo, que es el que tiene la vista aunque se dibuje recortado.

### 20. «Estudiar ahora»: elegir y comenzar

Hoy tocar una fila del selector inicia la sesión sin aviso. Pasa a ser una elección: la fila tocada queda marcada (indicador de opción y fondo `colorPrimaryContainer`, como el artboard), las demás se desmarcan, y un botón relleno «▶ Comenzar» (`ic_reproducir`), desactivado sin elección, llama al mismo `Listener.alElegirMateria(...)` con la materia y la técnica. «Nueva materia» conserva su panel y su botón propio. Las pruebas Espresso que empezaban tocando una fila pasan a tocar la fila y luego «Comenzar».

### 21. Sesión en curso visible en Inicio

`ResumenInicio` añade `sesionEnCurso()` (un `DetalleSesionInicio` o `null`), que `ObtenerResumenInicioService` obtiene del puerto existente `SesionRepository.buscarEnCurso()`; el núcleo sigue sin `android.*`. Inicio muestra arriba una tarjeta `colorPrimaryContainer` con la materia y el botón «Volver a la sesión», y el botón flotante cambia su texto a «Volver a la sesión» y abre `SesionEnCursoActivity` sin pasar por el selector (escenario «Ya hay una sesión En curso» de HU-47, que la app no cumplía). Que `SesionEnCursoActivity` lea el repositorio directamente es un atajo previo y queda fuera de este change.

## Target Scope & File Boundary

**Crear**
- `app/src/main/res/values/dimens.xml`, `app/src/main/res/values/attrs.xml`, `app/src/main/res/values/styles.xml`, `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/font/plus_jakarta_sans_medium.ttf`, `plus_jakarta_sans_semibold.ttf`, `plus_jakarta_sans_bold.ttf`, `plus_jakarta_sans_extrabold.ttf`
- `docs/licencias/OFL-PlusJakartaSans.txt`
- `app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/ColorMateria.java` y `RolColorEstado.java`
- `app/src/test/java/io/github/jhoel0521/studyplan/adapter/in/ui/ContrasteTemaTest.java`, `ReglasEstiloLayoutTest.java`, `EstadoSesionVisualTest.java`, `ColorMateriaContrasteTest.java`
- `app/src/androidTest/java/io/github/jhoel0521/studyplan/adapter/in/ui/LineaGraficaEspressoTest.java`

**Modificar**
- Recursos: `values/colors.xml`, `values/themes.xml` (y `values-night/themes.xml` solo si una prueba lo exige).
- Layouts de `res/layout/`: `activity_ajustes`, `activity_materia_formulario`, `activity_reparto_sesion`, `activity_sesion_detalle`, `activity_sesion_en_curso`, `activity_sesion_formulario`, `bottom_sheet_selector_estudio`, `contenido_calendario`, `contenido_estadisticas`, `contenido_inicio`, `contenido_materias`, `contenido_reportes`, `dialogo_color`, `dialogo_filtros_calendario`, `dialogo_recuperacion_sesion`, `item_bloque_calendario`, `item_celda_mes`, `item_color`, `item_estadistica_materia`, `item_estadistica_topico`, `item_evento_estudio`, `item_historial_meta`, `item_materia`, `item_reporte_materia`, `item_selector_materia`, `item_sesion_inicio`.
- Layouts de `res/layout-w600dp/` (solo estilos y alturas fijas, sin IDs nuevos): `activity_reparto_sesion`, `activity_sesion_detalle`, `activity_sesion_en_curso`, `activity_sesion_formulario`.
- Java (`adapter/in/ui/`): `EstadoSesionVisual`, `CalendarioActivity`, `CalendarioMesAdapter`, `EstadisticasActivity`, `EstadisticasAdapter`, `ReportesAdapter`, `MateriasAdapter`, `MainActivity`, `SelectorEstudioBottomSheet`, `SesionDetalleActivity`, `SesionEnCursoActivity`, `RepartoSesionActivity`, `FelicitacionMetasActivity`.
- Documentación: `docs/documento/capitulos/04-diseno-del-software.md`, `docs/auditoria-ia/bitacora.md`.

**No se modifica ni se borra:** `GrupoBotones.java`, `MateriaFormularioActivity.java` (conserva `GrupoBotones.unir`), `docs/build/diagramas_clases.py` y `cotejo_clases.py`, `domain/`, `application/` y `adapter/out/`, `AndroidManifest.xml`, `app/build.gradle.kts` (sin dependencias nuevas), `ic_launcher_*`.

**Revisión del 2026-11-20:** crear `res/drawable/ic_guardar.xml`, `ic_archivar.xml`, `ic_eliminar.xml`; modificar `domain/ResumenInicio.java`, `application/service/ObtenerResumenInicioService.java`, `adapter/in/ui/InicioViewModel.java`, `MainActivity.java`, `SelectorEstudioBottomSheet.java`, `GrupoBotones.java`, `res/layout/contenido_inicio.xml`, `bottom_sheet_selector_estudio.xml`, `item_selector_materia.xml`, `activity_materia_formulario.xml`, `values/styles.xml`, `values/strings.xml` y `values-en/strings.xml`, la prueba JUnit del servicio de resumen y las pruebas Espresso que empezaban desde una fila del selector.

**Borrar:** ningún archivo.

## Test Plan

Pruebas JVM (`./gradlew test`, sin Robolectric ni `org.json`):
- `ContrasteTemaTest`: lee `colors.xml` y `values-night/colors.xml` con `DocumentBuilderFactory` y `../.ux/standard/tokens.json` con un lector JSON mínimo escrito en la propia prueba; comprueba que cada rol `sp_*` coincide con su token y que todos los pares de `ux-kit.contrast-pairs` cumplen su mínimo, en claro y oscuro. Falla si falta un rol en `values-night/colors.xml`.
- `ReglasEstiloLayoutTest`: recorre `res/layout/` y `res/layout-w600dp/`; falla si encuentra un hex (`#RGB`, `#ARGB`, `#RRGGBB` o `#AARRGGBB`) fuera de atributos `tools:`, un `layout_height` en dp fijo en un `MaterialButton`/`ExtendedFloatingActionButton`, o un `style="@style/Widget.Material3.Button` / `CardView` explícito.
- `EstadoSesionVisualTest`: los 6 `CodigoEstadoSesion` devuelven el rol de la tabla de la decisión 5a.
- `ColorMateriaContrasteTest`: `paraSuperficie` devuelve ≥ 3:1 para cada color de `materia_*` (incluido `#FB8C00`, hoy 2,37:1 sobre blanco) contra las superficies clara y oscura, y deja intacto un color que ya cumple; `conAlfa` compone el relleno esperado.
- `ArquitecturaTest` sigue en verde: `ColorMateria` y `RolColorEstado` no importan `android.*`.

Prueba instrumentada (`LineaGraficaEspressoTest`, `./gradlew connectedDebugAndroidTest`): abre Inicio, el selector, la sesión en curso, el detalle, el reparto, Estadísticas, Materias y el calendario en claro y oscuro, sin fallar. Precondición: usa `Pruebas.reiniciarEstado()` y el bloqueo desactivado, como `InicioEspressoTest`, de modo que `ActividadProtegida` no deje la raíz `INVISIBLE`; se corre primero una vez sin cambios de código para confirmar la línea base.

Comprobaciones automatizadas de cierre: `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedDebugAndroidTest`, `node .ux/scripts/tokens.mjs .ux/standard/tokens.json`, `python docs/build/cotejo_clases.py`, `python .apa/scripts/revisar.py --doc docs/documento`, `openspec validate aplicar-linea-grafica-indigo --strict`.

Inspecciones manuales: fuente al ≈ 200 %, ancho de 360 dp, Accessibility Scanner sin errores críticos, `layout-w600dp/` sin romperse, y capturas claro/oscuro de cada pantalla de la lista junto al bloque «Visual por pantalla».

## Risks / Trade-offs

- **Anillo nuevo:** requiere conocer la duración de la fase de Pomodoro. Si no está expuesta, se aplica la regla de respaldo de la decisión 5b (100 % sin progreso) y queda registrado como pendiente.
- **Herencia silenciosa:** pantallas fuera de la lista (calendario, reportes, ajustes, autenticación, formularios y metas) cambian de aspecto con el tema. Mitigación: tarea de revisión visual, lint y `ReglasEstiloLayoutTest`.
- **Botones de 48 dp:** pueden verse grandes en algunas pantallas; se ajusta el único valor `button_height` o se acorta el dibujo con insets sin bajar el área táctil.
- **`EstadoSesionVisual`:** `COMPLETADA` y `COMPLETADA_TARDIA` comparten color; se distinguen por icono y etiqueta (RNF-06).
- **Pruebas existentes:** `TecnicasEstudioEspressoTest` y similares dependen de IDs y textos; este change no cambia ninguno, y la prueba de humo nueva se corre primero como línea base.
- **Hex en vista previa:** los `tools:background` y `tools:tint` con hex se mantienen solo como vista previa del editor y la prueba los ignora.
- **Árbol de trabajo con cambios previos:** los commits de este change añaden solo sus archivos, por ruta.

## References

- Propuesta: [proposal.md](proposal.md) · Tareas: [tasks.md](tasks.md) · Spec delta: [perfil-ajustes](specs/perfil-ajustes/spec.md) · Spec vigente: [perfil-ajustes](../../specs/perfil-ajustes/spec.md)
- Estándar de diseño: [tokens.json](../../../.ux/standard/tokens.json) · [brand.md](../../../.ux/standard/brand.md) · [users.md](../../../.ux/standard/users.md) · [CHANGELOG](../../../.ux/standard/CHANGELOG.md) · [config.yaml](../../../.ux/config.yaml)
- Revisión UX del plan: [ux-plan-review](../../../.ux/reports/2026-11-19-ux-plan-review-aplicar-linea-grafica-indigo.md)
- Reglas del proyecto: [AGENTS.md](../../../AGENTS.md) (§2, §6 y §6.2) · [config de OpenSpec](../../config.yaml)
- Tema actual: [themes.xml](../../../app/src/main/res/values/themes.xml) · [themes (night)](../../../app/src/main/res/values-night/themes.xml) · [colors.xml](../../../app/src/main/res/values/colors.xml)
- Java con colores: [EstadoSesionVisual](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/EstadoSesionVisual.java) · [CalendarioActivity](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/CalendarioActivity.java) · [CalendarioMesAdapter](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/CalendarioMesAdapter.java) · [GrupoBotones](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/GrupoBotones.java) · [SesionEnCursoActivity](../../../app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/SesionEnCursoActivity.java)
- Layouts clave: [contenido_inicio](../../../app/src/main/res/layout/contenido_inicio.xml) · [bottom_sheet_selector_estudio](../../../app/src/main/res/layout/bottom_sheet_selector_estudio.xml) · [activity_sesion_en_curso](../../../app/src/main/res/layout/activity_sesion_en_curso.xml) · [dialogo_recuperacion_sesion](../../../app/src/main/res/layout/dialogo_recuperacion_sesion.xml) · [activity_reparto_sesion](../../../app/src/main/res/layout/activity_reparto_sesion.xml) · [activity_sesion_detalle](../../../app/src/main/res/layout/activity_sesion_detalle.xml) · [contenido_estadisticas](../../../app/src/main/res/layout/contenido_estadisticas.xml) · [contenido_materias](../../../app/src/main/res/layout/contenido_materias.xml) · [item_bloque_calendario](../../../app/src/main/res/layout/item_bloque_calendario.xml) · [activity_materia_formulario](../../../app/src/main/res/layout/activity_materia_formulario.xml) · [activity_sesion_formulario](../../../app/src/main/res/layout/activity_sesion_formulario.xml)
- Origen del diseño visual (no necesario para implementar; el bloque «Visual por pantalla» lo transcribe): lienzo de Claude Design «StudyPlan — Línea gráfica Índigo Digital», 2026-11-18.
