# Design

## Context

El estado actual de la aplicación (épicas E1, E2 y E3 archivadas en `openspec/specs/`) provee:
- Arquitectura hexagonal en 3 capas en Java puro para `domain` y `application` (comprobada por `ArquitecturaTest`), MVVM en `adapter.in.ui` y persistencia Room v3 (`perfil`, `ajustes`, `materia`, `sesion_estudio`) con esquema `3.json`.
- Value Objects temporales propios en `domain`: `Fecha` (`YYYY-MM-DD`, aritmética gregoriana proléptica compatible con `minSdk 24`, `lunesDeSemana()`, `domingoDeSemana()` según `RN-13`) y `Hora` (`HH:mm`, `0..1439`, `minutosDesdeMedianoche()`, `minutosHasta()`). No se usa `java.time` ni `coreLibraryDesugaring`.
- Patrón State GoF (`EstadoSesion` y `CodigoEstadoSesion`) y helper visual accesible `EstadoSesionVisual` (icono + etiqueta textual + color Material 3) cumpliendo `RNF-06`.
- `DestinoPrincipal` ya define la pestaña `CALENDARIO` con `R.id.nav_calendario` y actividad `null`, lista para habilitarse en el riel lateral (`layout-w600dp`) y la barra inferior.
- En `AGENTS.md` §6.2 se incorporó explícitamente la excepción a `android:minHeight="48dp"` para celdas de cuadrícula de calendario de 7 columnas (L–D) y bloques horarios proporcionales en pantallas pequeñas.

Ver `proposal.md` para la motivación, el alcance estricto (sin migración de Room, sin permisos nuevos y sin `HU-34`) y la lista de descartes.

## Goals / Non-Goals

**Goals:**
- Construir el calendario completo (`HU-16` a `HU-21`) con **Views XML nativas en Java sin dependencias externas**, garantizando control total sobre accesibilidad (`RNF-06`, `AGENTS.md` §6.2), soporte desde pantallas de 5" (`RNF-19`) y compatibilidad API 24–37 (`RNF-17`).
- Resolver el rango visible del calendario con una sola consulta SQLite parametrizada (`listarPorRango`) sobre el índice `index_sesion_estudio_fecha` de Room v3 (sin migración de esquema, `RNF-02`, `RNF-12`).
- Abrir siempre el detalle de cualquier sesión tocada (`SesionDetalleActivity`), resolviendo el hueco de E3 para sesiones pasadas en estado `NO_REALIZADA` (`HU-13`, `HU-14`).
- Preservar el periodo y foco de fecha del estudiante al alternar entre `Día`, `Semana` y `Mes`, y recordar en preferencias locales la última vista seleccionada (iniciando en `Semana` la primera vez).

**Non-Goals:**
- Añadir librerías externas de calendario (`Android-Week-View` y forks quedan descartados por falta de mantenimiento, `targetSdk 37` y dibujo en `Canvas` poco accesible; `kizitonwose/Calendar` queda descartado por carecer de rejilla horaria y exigir `coreLibraryDesugaring`).
- Modificar `StudyPlanDatabase` (`version = 3`), añadir migraciones Room o declarar permisos en `AndroidManifest.xml`.
- Implementar interacción de arrastrar o redimensionar bloques, ejecución en vivo (`EN_CURSO`) o recordatorios (`HU-34`).

## Decisions

### D1. Modelo de dominio puro para el calendario (`domain`)

Toda la aritmética de periodos, transiciones de foco, ventanas horarias, filtrado, resumen y geometría lógica de solapamientos vive en `domain` en Java puro (sin `android.*` ni `androidx.*`, verificable con JUnit y `ArquitecturaTest`):

- **`VistaCalendario` (enum):** `DIA`, `SEMANA`, `MES`. El valor inicial por defecto es `SEMANA`.
- **`FiltroCalendario` (inmutable):**
  - `Long materiaId` (nullable: `null` = todas las materias).
  - `boolean incluirArchivadas` (indica si se muestran las sesiones de materias archivadas y si aparecen en el filtro de materias; por defecto `true` para conservar visibilidad del historial según `HU-08`, conmutable por el estudiante).
  - `Set<CodigoEstadoSesion> estadosVisibles` (por defecto todos los estados de `CodigoEstadoSesion` para cumplir `HU-19`, permitiendo filtrar por estado si el estudiante lo selecciona).
- **`PeriodoCalendario` (inmutable + lógica pura de navegación y foco):**
  - Campos: `VistaCalendario vista`, `Fecha fechaFoco`, `Fecha diaSeleccionado` (nullable cuando se navega de mes sin haber tocado un día concreto).
  - **Rango de consulta y rango visible (`lunes` a `domingo` según `RN-13`):**
    - `DIA`: visible `[fechaFoco, fechaFoco]`.
    - `SEMANA`: visible `[fechaFoco.lunesDeSemana(), fechaFoco.domingoDeSemana()]`.
    - `MES`: mes visible `(fechaFoco.anio(), fechaFoco.mes())` desde día `1` hasta `Fecha.diasDelMes(anio, mes)`. Para construir las filas completas de 7 columnas (35 o 42 celdas), el rango de consulta de la cuadrícula abarca desde `primerDiaDelMes.lunesDeSemana()` hasta `ultimoDiaDelMes.domingoDeSemana()`, marcando `delMesActual = false` en los días de meses adyacentes para atenuarlos en UI.
  - **Navegación (`anterior()`, `siguiente()`):**
    - En `DIA`: `fechaFoco.sumarDias(±1)` y `diaSeleccionado = nuevoFoco`.
    - En `SEMANA`: `fechaFoco.sumarDias(±7)` y actualiza `diaSeleccionado` manteniendo el mismo día de la semana (o `null` si no aplica).
    - En `MES`: avanza o retrocede 1 mes calendario (`Fecha.de(anioNuevo, mesNuevo, 1)`); si el nuevo mes no coincide con el mes de `diaSeleccionado`, limpia `diaSeleccionado = null` para evitar seleccionar por error un día de un mes no visible.
  - **Cambio de vista conservando el foco (`cambiarVista(VistaCalendario nuevaVista, Fecha hoy)`):**
    - `MES → SEMANA`: si `diaSeleccionado` pertenece al mes visible, `nuevoFoco = diaSeleccionado`; de lo contrario, `nuevoFoco = Fecha.de(fechaFoco.anio(), fechaFoco.mes(), 1)` (primera semana del mes visible).
    - `MES → DIA`: si `diaSeleccionado` pertenece al mes visible, `nuevoFoco = diaSeleccionado`; si `hoy` pertenece al mes visible y no había día seleccionado, `nuevoFoco = hoy`; de lo contrario, `nuevoFoco = Fecha.de(fechaFoco.anio(), fechaFoco.mes(), 1)`.
    - `SEMANA → MES`: toma el mes de `diaSeleccionado` (si pertenece a la semana visible) o de `hoy` (si pertenece a la semana visible) o del lunes de la semana visible.
    - `SEMANA → DIA`: usa `diaSeleccionado` si cae en la semana visible; si no, `hoy` si cae en la semana visible; si no, el lunes de la semana visible (`fechaFoco.lunesDeSemana()`).
    - `DIA → SEMANA` o `DIA → MES`: conserva `fechaFoco` como `fechaFoco` y `diaSeleccionado`.
- **`CalculadorRangoHorario` (ventana de scroll inicial `± 2 horas` y layout de solapamientos):**
  - La rejilla de `Semana` y `Día` siempre representa las **24 horas completas (`0..1440` minutos, `00:00`–`24:00`)**.
  - Calcula `minutoScrollInicial`:
    - Si hay sesiones visibles en el periodo (`Semana` o `Día`), toma `minInicio = min(s.horaInicio().minutosDesdeMedianoche())` y `maxFin = max(s.horaFin().minutosDesdeMedianoche())`, enfocando inicialmente desde `max(0, minInicio - 120)` (`- 2 h`), con referencia visual de rango activo hasta `min(1440, maxFin + 120)` (`+ 2 h`).
    - Si no hay sesiones visibles en el periodo, enfoca a las `08:00` (`480` min) —o `max(0, ahora - 120)` si se está viendo `hoy` y `ahora` cae fuera de la mañana temprana— manteniendo todo el día desplazable.
  - **Columnas paralelas ante concurrencia visual (`RN-05`, `RN-09`):** aunque dos sesiones activas no pueden cruzarse (`RN-05`), una sesión `REPROGRAMADA` conserva su horario original y una nueva sesión `PENDIENTE` (u otra) puede ocupar ese mismo horario. El calculador asigna `(indiceColumna, totalColumnas)` dentro de cada día para que ambas tarjetas compartan el ancho sin taparse.
- **Modelos de lectura inmutables para UI:**
  - `ItemSesionCalendario`: `sesionId`, `materiaId`, `nombreMateria`, `colorMateria`, `materiaArchivada`, `fecha`, `horaInicio`, `horaFin`, `duracionPlanificadaMinutos`, `duracionRealMinutos`, `estado`.
  - `BloqueHorarioCalendario`: `ItemSesionCalendario item`, `int diaColumna` (`0..6`), `int minutoInicio`, `int minutoFin`, `int indiceSolapamiento`, `int totalSolapamientos`.
  - `CeldaDiaMes`: `Fecha fecha`, `boolean delMesActual`, `boolean esHoy`, `boolean esSeleccionado`, `List<Integer> coloresMaterias` (distintos colores únicos en orden de aparición), `int minutosTotalesDia`, `int cantidadSesiones`.
  - `ResumenPeriodoCalendario`: `int minutosPlanificados`, `int minutosRealizados`.
    - `Real` (`RN-11`): suma `duracionRealMinutos` de sesiones cuyo estado cumple `estado.sumaHorasReales()` (`COMPLETADA` y `COMPLETADA_TARDIA`) dentro del rango visible y del filtro aplicado.
    - `Plan`: suma `duracionPlanificadaMinutos` de las sesiones dentro del rango visible y del filtro aplicado cuyo estado cumple `estado.ocupaHorario()` (excluyendo `REPROGRAMADA`, evitando contar doble el tiempo reprogramado en el mismo periodo). *(Nota de diseño sobre Room v3: al no existir migración de esquema, Room v3 no distingue en BD entre `COMPLETADA_TARDIA` derivada de `NO_REALIZADA` y `COMPLETADA_TARDIA` creada con `HU-11`; ambas conservan `duracion_planificada_minutos > 0` y se contabilizan uniformemente sin alterar tablas).*
  - `DatosCalendario`: agrupa `PeriodoCalendario`, `FiltroCalendario`, `ResumenPeriodoCalendario`, `List<CeldaDiaMes>` (para `MES`), `List<ItemSesionCalendario>` del día seleccionado (para `MES`), `List<BloqueHorarioCalendario>` (para `SEMANA` y `DIA`), `int minutoScrollInicial`, `List<ResumenMateria>` disponibles para el filtro y `Fecha hoy` + `Hora ahora`.

### D2. Puertos y servicio de aplicación (`application`)

- **Ampliación de `SesionRepository` (`application.port.out`):**
  - Se añade únicamente: `List<SesionEstudio> listarPorRango(Fecha desde, Fecha hasta);` (ordenado por `fecha ASC, horaInicio ASC`).
- **Nuevo caso de uso `ConsultarCalendario` + `ConsultarCalendarioService` (`application.port.in`, `application.service`):**
  - Dependencias: `SesionRepository`, `MateriaRepository`, `Reloj`, `VencerSesionesPendientes`.
  - Ejecución (`ejecutar(PeriodoCalendario periodo, FiltroCalendario filtro)`):
    1. Invoca `vencerPendientes.ejecutar()` (`RN-08`) para que toda sesión `PENDIENTE` pasada aparezca ya como `NO_REALIZADA` y habilite `HU-13`/`HU-14` al tocarla.
    2. Carga materias (`activas()` y `archivadas()`).
    3. Consulta `sesiones.listarPorRango(rangoConsultaInicio, rangoConsultaFin)` en una única llamada.
    4. Filtra sesiones según `FiltroCalendario` (`materiaId`, `incluirArchivadas`, `estadosVisibles`).
    5. Construye los resúmenes (`Plan / Real` acotados estrictamente al periodo visible del mes/semana/día y no a los días atenuados de meses adyacentes en `MES`), las celdas de `MES` con la lista de sesiones del `diaSeleccionado`, o los bloques de `SEMANA`/`DIA` con el cálculo de columnas simultáneas y `minutoScrollInicial`.

### D3. Persistencia Room v3 sin migración y preferencia de última vista

- **`SesionDao` (`adapter.out.persistence`):**
  - Añadir `@Query("SELECT * FROM sesion_estudio WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha ASC, hora_inicio ASC") public abstract List<SesionEntity> listarPorRango(String desde, String hasta);`.
  - Utiliza directamente el índice existente `index_sesion_estudio_fecha` en `3.json`. `StudyPlanDatabase` conserva `version = 3`.
- **`SesionRepositoryRoom`:** implementa `listarPorRango(Fecha desde, Fecha hasta)` delegando en `dao.listarPorRango(desde.iso(), hasta.iso())` y mapeando con `Mapeadores`.
- **`PreferenciaVistaCalendario` (`adapter.in.ui`):** guarda/lee la última `VistaCalendario` en `SharedPreferences` privadas (clave de UI local, fuera de Room, valor por defecto `VistaCalendario.SEMANA`).

### D4. Presentación con Views XML nativas y accesibilidad (`adapter.in.ui` y `res/`)

- **Activación de pestaña y manifest:**
  - `DestinoPrincipal.CALENDARIO` pasa a `CalendarioActivity.class` (`AndroidManifest.xml`: `exported="false"`).
  - `CalendarioActivity` extiende `ActividadProtegida` (`HU-04`, `RNF-12`), aplica `aplicarInsets()` y `enlazarBarraInferior(DestinoPrincipal.CALENDARIO)` y recarga datos en `alEstarListo()` para reflejar cambios al regresar desde `SesionFormularioActivity` o `SesionDetalleActivity` (sin consultas en el hilo principal, `RNF-02`).
  - Layout envoltorio `activity_calendario.xml` y `layout-w600dp/activity_calendario.xml` siguiendo exactamente el patrón de `activity_main.xml` e incluyendo `contenido_calendario.xml`.
- **Controles de cabecera y filtros (`HU-19`, `HU-21`):**
  - `MaterialToolbar` con título «Calendario» y menú con acción **Leyenda** (`HU-19`: abre `MaterialAlertDialogBuilder` con un layout accesible `dialogo_leyenda_calendario.xml` listando los 6 estados con su icono `ic_estado_*`, color de tema y explicación) y **Filtros** (o barra de filtros compacta integrada + diálogo para filtros avanzados: selección de materia `Todas` / específica, interruptor `Incluir materias archivadas` y chips de estados).
  - Selector segmentado `MaterialButtonToggleGroup` con tres botones `Día`, `Semana`, `Mes` (con `minHeight="48dp"`).
  - Navegador de periodo (`‹` botón anterior `48dp`, etiqueta centrada del rango p. ej. `16 – 22 nov 2026` o `Noviembre 2026`, botón hoy / volver a hoy opcional si facilita usabilidad, `›` botón siguiente `48dp`) y tarjeta/resumen `Plan X h · Real Y h` (`HU-21`).
  - Gesto de deslizamiento horizontal (`GestureDetector` sobre el área del calendario) para avanzar o retroceder día/semana/mes (`HU-16`, `HU-17`) sin interferir con el scroll vertical de `ScrollView`.
- **Vista Mes (`HU-16`, `HU-19`, `HU-20`):**
  - Cabecera fija de 7 columnas `L M X J V S D` (`RN-13`).
  - `RecyclerView` con `GridLayoutManager(this, 7)` y `CalendarioMesAdapter` (`item_celda_mes.xml`):
    - Excepción de `AGENTS.md` §6.2 aplicada a las celdas de 7 columnas (`RNF-19`) para evitar desbordamiento horizontal/vertical en 5".
    - Muestra número de día, estado visual diferenciado para `esHoy` y `esSeleccionado`, tono atenuado para `delMesActual == false`, puntos/indicadores de color de las materias (hasta 4 puntos + indicador `+`) y suma compacta de horas (`p. ej. 2,5 h`) cuando hay sesiones; incluye `contentDescription` dinámico accesible por TalkBack.
    - **Toque corto** en una celda: selecciona ese día (`diaSeleccionado`) y actualiza la lista inferior `Sesiones del <fecha>` (incluso si se toca un día atenuado de mes adyacente, opcionalmente enfocando ese día para ver sus sesiones debajo sin saltos bruscos).
    - **Pulsación larga y sostenida (`OnLongClickListener`)** en una celda con `fecha >= hoy`: abre `SesionFormularioActivity` en `MODO_PLANIFICAR` con `EXTRA_FECHA`. En `fecha < hoy` no inicia creación.
  - Lista inferior de sesiones del día seleccionado: `RecyclerView` reutilizando `SesionInicioAdapter` e `item_sesion_inicio.xml` (`RNF-06`: color + icono + etiqueta de estado); tocar cualquier fila abre `SesionDetalleActivity`.
- **Vistas Semana (`HU-17`) y Día (`HU-18`):**
  - Implementadas con componentes XML y Java nativos sin `Canvas` opaco para bloques interactivos:
    - Cabecera de días (7 columnas `L..D` con número de día y marca de `hoy` en `Semana`; cabecera de fecha en `Día`).
    - `ScrollView` vertical que envuelve un `ConstraintLayout`/`FrameLayout` nativo (`VistaHorarioCalendario` o contenedor estructurado en `contenido_calendario.xml`) con 24 filas de `00:00` a `23:00` (eje horario a la izquierda + líneas divisorias de hora y columnas).
    - Sobre la rejilla horaria se añaden vistas infladas de tarjeta `MaterialCardView` (`item_bloque_calendario.xml`, una vista real por sesión con altura proporcional a `(minFin - minInicio) * alturaPorMinuto`, borde/acento del color de la materia, título de materia, rango horario en `Día` o bloques anchos, e **icono + texto/etiqueta de estado** según `EstadoSesionVisual` para `RNF-06`, además de `contentDescription` descriptivo completo para TalkBack).
    - En **Semana**, las 7 columnas se reparten proporcionalmente al ancho disponible (aplicando la excepción de `AGENTS.md` §6.2 para 7 columnas en pantallas pequeñas, `RNF-19`). Para evitar texto ilegible cuando un bloque es corto (p. ej., 30 min), la tarjeta prioriza icono de estado + nombre abreviado/elipsado de materia manteniendo `contentDescription` íntegro; en `Día`, muestra materia, horario completo, icono y etiqueta de estado.
    - **Línea de hora actual (`HU-18`):** vista horizontal resaltada posicionada en `ahora.minutosDesdeMedianoche()` sobre la columna de `hoy` (en `Semana` y `Día` cuando `hoy` está visible).
    - **Auto-scroll `± 2 horas`:** al cargar o cambiar a `Semana`/`Día`, `scrollView.post(...)` desplaza suavemente al píxel correspondiente a `minutoScrollInicial`.
    - **Interacciones sobre el horario:**
      - Toque corto sobre una tarjeta `MaterialCardView` de sesión → `SesionDetalleActivity`.
      - **Pulsación larga y sostenida** sobre una franja horaria libre (`00:00`..`23:00`) cuya columna corresponde a `fecha >= hoy` → abre `SesionFormularioActivity` en `MODO_PLANIFICAR` con `EXTRA_FECHA`, `EXTRA_HORA_INICIO = HH:00` y `EXTRA_HORA_FIN = (HH+1):00` (si es `23:00`, `23:59`). Si la columna es `fecha < hoy`, no abre creación.
- **Ampliación menor de `SesionFormularioActivity`:**
  - Lee opcionalmente `EXTRA_HORA_INICIO` y `EXTRA_HORA_FIN` cuando `savedInstanceState == null` (solo en `MODO_PLANIFICAR`) para precargar la hora al venir desde `HU-20`.

### D5. Documentación, diagramas y entregas por bloque

- Siguiendo `AGENTS.md` §4 y §6–7, el change se divide en 6 bloques (`tasks.md`): Dominio → Aplicación → Persistencia Room v3 → Presentación UI → Pruebas Espresso E2E + `revisor-java` → Diagramas y documentación APA 7.
- En el Bloque 6 se corrige `flujo_pantallas()` en `docs/build/diagramas.py` (cambia «Mes: tocar un día abre el Día» por «Mes: tocar un día lista sus sesiones» y «Semana y Día: long press en fecha >= hoy»), se actualiza `diagramas_clases.py` y `cotejo_clases.py` con las nuevas clases, y se actualiza `03-requisitos-y-analisis-del-sistema.md` (si aplica al delta de `HU-20`/`HU-21`), `04-diseno-del-software.md` y `docs/auditoria-ia/bitacora.md`.

## Risks / Trade-offs

- **Legibilidad de 7 columnas en teléfonos de 5" (`RNF-19` vs `RNF-06`):** comprimir 7 columnas en ~300 dp disponibles deja ~40–45 dp por columna en `Semana`. → *Mitigación:* aplicar la excepción documentada en `AGENTS.md` §6.2, usar tarjetas `MaterialCardView` compactas con color de materia + icono de estado + nombre elipsado (`maxLines=1`/`2`), `contentDescription` completo para TalkBack, y permitir tocar el bloque para abrir el detalle o cambiar a la vista `Día` conservando la fecha.
- **Limitación de `Plan` sobre `COMPLETADA_TARDIA` en Room v3 sin migración:** Room v3 no almacena un indicador de si una sesión `COMPLETADA_TARDIA` nació planificada (`HU-13`) o como registro tardío directo (`HU-11`). → *Mitigación:* excluir únicamente `REPROGRAMADA` del total `Plan` (evitando doble conteo tras reprogramaciones) y dejar documentada la regla en `Design` y en el Javadoc de `ConsultarCalendarioService`; si E7/E8 exige separarlo en reportes, se abordará en una migración futura.
- **Detección de pulsación larga sobre un `ScrollView` vertical (`HU-20`):** un desplazamiento vertical rápido no debe disparar creación accidental y el *long press* en una franja libre debe detectar la fecha y la hora exactas tocadas. → *Mitigación:* usar `GestureDetector` / `OnLongClickListener` por celda horaria (`24 × 7` o `24 × 1` franjas declaradas explícitamente o en cuadrícula estructurada) de modo que Android cancele automáticamente la pulsación larga al detectar desplazamiento vertical o swipe horizontal, y facilite además pruebas deterministas con Espresso (`longClick()`).
- **Mantenimiento de `cotejo_clases.py`:** toda clase nueva agregada en `app/` exige su registro en `diagramas_clases.py`. → *Mitigación:* el Bloque 6 actualiza `diagramas_clases.py` y valida `python docs/build/cotejo_clases.py`.
