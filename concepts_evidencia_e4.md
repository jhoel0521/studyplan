# Evidencia y decisiones para la planificación de E4 (StudyPlan)

Fecha: 2026-11-16. Actualizado con las decisiones del Product Owner tras revisar la primera versión de este documento.

## 1. Restricciones del proyecto (de `AGENTS.md`, decididas por el humano)

- App Android nativa en **Java**. Prohibido escribir código del proyecto en Kotlin o Compose.
- UI: Views XML + Material Design 3. Librerías de UI nuevas solo con aprobación humana.
- Arquitectura hexagonal en 3 capas: `domain` y `application` son Java puro, sin `android.*` ni `androidx.*`.
- Room (SQLite), offline-first, un usuario local. Sin backend propio ni servicios en primer plano permanentes.
- `minSdk 24`, `targetSdk 37`, `applicationId` fijo (`io.github.jhoel0521.studyplan`).
- Flujo OpenSpec: ningún código entra sin un change aprobado por el humano (`AGENTS.md` §3).
- **La IA no inventa historias (HU), reglas (RN) ni requisitos no funcionales (RNF).** Arrastrar o redimensionar eventos en el calendario no está en ninguna HU y queda fuera.
- Cada layout XML nuevo o modificado debe cumplir al 100 % el checklist de `AGENTS.md` §6.2 (áreas táctiles `>= 48dp`, `contentDescription`, `importantForAccessibility`, `tools:context`, `tools:fitsSystemWindows="true"`, `tools:tint`, `tools:text`, `maxWidth`, estado nunca dependiente solo del color `RNF-06`).

## 2. Estado de las épicas previas y cierre previo obligatorio de E3

- **E1** (perfil, inicio, ajustes), **E2** (materias) y **E3** (sesiones) están implementadas.
- En la verificación unitaria y estática previa se registraron `./gradlew testDebugUnitTest` (199 pruebas unitarias, 0 fallos) y `lintDebug` sin errores.
- **`openspec/changes/add-sesiones-estudio`:** tiene las **33 tareas marcadas** (`[x]`: 1 en Compuerta previa + 4 en Bloque 1 + 11 en Bloque 2 + 4 en Bloque 3 + 6 en Bloque 4 + 3 en Bloque 5 + 4 en Bloque 6) y pasa `openspec validate add-sesiones-estudio --strict`, pero **aún no está archivado**.
- **`docs/auditoria-ia/bitacora.md`:** la **fila 70** ya figura como aceptada parcialmente (`**Aceptado** (aprobación humana previa de 0.1) y **Pendiente** de revisión final de entrega`), y solo las **filas 71–75** están íntegramente como `**Pendiente** de revisión humana`.
- **Cierre previo obligatorio de E3 antes de proponer E4:**
  1. Ejecutar `connectedDebugAndroidTest` en emulador para confirmar y dejar registrada la línea base instrumentada (Espresso y migraciones Room).
  2. Cerrar las filas **70–75** de `docs/auditoria-ia/bitacora.md` tras la revisión humana.
  3. Archivar `openspec/changes/add-sesiones-estudio` mediante `/opsx:archive`.
- **Hueco funcional de E3 que E4 resuelve:** `MainActivity` es hoy el único acceso a `SesionDetalleActivity` y solo muestra `proximaSesion` (`PENDIENTE` desde hoy) y `sesionesDeHoy`. Una sesión en estado `NO_REALIZADA` de días anteriores no es alcanzable desde la interfaz actual, por lo que el registro tardío (`HU-13`) y la reprogramación (`HU-14`) de sesiones pasadas quedarán operativas al abrirse desde el calendario de E4.
- **Estado técnico heredado:**
  - `SesionRepository` solo expone `listarPorFecha(Fecha)`; todavía no existe consulta por rango de fechas.
  - `DestinoPrincipal` mantiene `CALENDARIO` y `ESTADISTICAS` con actividad `null` (pestañas visibles pero deshabilitadas).
  - `Fecha` y `Hora` son Value Objects inmutables propios en `domain`, sin `java.time` (decisión de E3 para compatibilidad nativa con `minSdk 24`). No hay `coreLibraryDesugaring` en `app/build.gradle.kts`.
  - `AndroidManifest.xml` no declara ningún `<uses-permission>`.
  - `StudyPlanDatabase` está en **versión 3** (`app/schemas/.../3.json`), con la tabla `sesion_estudio` ya indexada por `fecha` (`index_sesion_estudio_fecha`).

## 3. Decisión de arquitectura UI: cero librerías externas de calendario

### Librerías candidatas rechazadas (decisión humana, 2026-11-16)
- **`Android-Week-View`** (`alamkanak`, `thellmund`, `Quivr`, `ribot`): sin mantenimiento activo, sin compatibilidad verificada con `targetSdk 37` y basadas en dibujo directo sobre `Canvas`, lo que complica cumplir el checklist de accesibilidad obligatorio de `AGENTS.md` §6.2 y `RNF-06` (`minHeight="48dp"`, `contentDescription`, navegación por foco en TalkBack y estado con icono + etiqueta textual).
- **`kizitonwose/Calendar`** (`com.kizitonwose.calendar:view`): no provee rejilla por horas para las vistas de Semana y Día, y exige activar `coreLibraryDesugaring` en `minSdk 24`, rompiendo la decisión de E3 de usar `Fecha` y `Hora` propios sin `java.time` y sumando riesgo sobre el conflicto de versiones de `kotlinx-serialization` ya documentado en `app/build.gradle.kts`.
- Referencias visuales descartadas para código: *ilamy Calendar* (React + Tailwind) y *CalendarFX* (JavaFX escritorio).

### Enfoque elegido para E4
- **Views XML nativas en Java sin dependencias nuevas** en `gradle/libs.versions.toml` ni `app/build.gradle.kts`, operando directamente sobre `Fecha` y `Hora` del dominio:
  - **Vista Mes (`HU-16`):** `RecyclerView` con `GridLayoutManager` de 7 columnas (lunes a domingo, `RN-13`) + lista inferior (`RecyclerView`) con las sesiones del día seleccionado (reutilizando iconos y etiquetas de `EstadoSesionVisual`).
  - **Vistas Semana (`HU-17`) y Día (`HU-18`):** contenedor nativo con `ScrollView` + rejilla horaria y tarjetas `MaterialCardView` para los bloques de sesión proporcionales a su duración, permitiendo control total de accesibilidad (`RNF-06`, `AGENTS.md` §6.2) y soporte de pantalla desde 5" (`RNF-19`).

## 4. Alcance estricto de E4 (sin migración de Room ni permisos)

- **Prevalece `HU-16` sobre `flujo_pantallas.png`:** tocar un día en la vista Mes lista debajo las sesiones de ese día (no navega a la vista Día). Como parte de E4 se actualizará `docs/build/diagramas.py` (`flujo_pantallas()`, caja `cal`) para corregir la discrepancia.
- **`HU-34` («aviso») fuera de E4:** permanece en la épica **E9** (`openspec/specs/recordatorios/spec.md`); no se agrega campo de recordatorio ni columna nueva en E4.
- **Sin permisos nuevos en el manifiesto:** todos los permisos de notificaciones, alarma exacta, vibración y No molestar (`POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY`, `VIBRATE`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`) pertenecen a **E5** / **E9** y quedan fuera de E4.
- **Núcleo y persistencia sobre Room v3 (sin migración):**
  - Añadir `listarPorRango(Fecha desde, Fecha hasta)` en `application.port.out.SesionRepository`, `SesionRepositoryRoom` y `SesionDao` (`SELECT * FROM sesion_estudio WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha ASC, hora_inicio ASC`, aprovechando el índice `index_sesion_estudio_fecha` de Room v3).
  - Permite cargar el día, semana o mes visible en una única consulta fuera del hilo principal (`RNF-02`, `RNF-12`) y resuelve el hueco funcional de E3 al habilitar la navegación desde el calendario a `SesionDetalleActivity` para sesiones pasadas (`NO_REALIZADA`, `HU-13` y `HU-14`).

## 5. Decisiones funcionales cerradas por el Product Owner para las vistas de E4 (Mes / Semana / Día)

1. **Vista inicial y persistencia de la última vista (`1.1`):**
   - Al abrir `Calendario` por primera vez, la vista por defecto es **`Semana`**.
   - Cuando el estudiante cambia entre `Día`, `Semana` y `Mes`, se guarda la última vista elegida para restaurarla en aperturas siguientes (en preferencias locales de UI, sin tocar el esquema Room v3).
2. **Conservación del foco de fecha al cambiar de vista (`1.2`):**
   - Nunca se pierde el periodo al que el estudiante navegó:
     - De **`Mes` → `Semana` o `Día`**: si hay un día seleccionado dentro del mes visible, se usa esa fecha como foco; si no hay un día seleccionado en ese mes (p. ej. retrocedió 2 meses con las flechas), se toma el **primer día / primera semana del mes visible**.
     - De **`Semana` → `Mes`**: abre el mes correspondiente a la semana visible (o al día seleccionado dentro de ella).
     - De **`Semana` → `Día`**: abre el día seleccionado en esa semana (o `hoy` si pertenece a la semana visible; si no, el lunes de esa semana).
     - De **`Día` → `Semana` o `Mes`**: conserva ese mismo día como foco.
3. **Vista Mes (`HU-16`, `HU-19`, `HU-20`):**
   - **Celda del día:** muestra el número del día, los indicadores de color de las materias con sesión ese día y la suma de horas del día en formato compacto; al **tocar el día**, la lista inferior muestra todas las sesiones de ese día con materia, horario, duración e icono + etiqueta textual de estado (`HU-16`, `HU-19`, `RNF-06`).
   - **Días de meses adyacentes:** las celdas de la cuadrícula de 7 columnas (L–D) que pertenecen al mes anterior o siguiente se muestran en **tono atenuado**.
   - **Toque corto vs. pulsación larga:** el toque corto selecciona el día para ver sus sesiones debajo; la **pulsación larga y sostenida** está reservada exclusivamente para **crear/planificar sesión en fechas `>= hoy`**.
4. **Vistas Semana (`HU-17`) y Día (`HU-18`) — Rejilla de 24 h con enfoque `± 2 h` y excepción de `48dp`:**
   - **Rejilla de 24 horas (`00:00`–`24:00`) con auto-scroll `± 2 horas`:** al mostrar una semana o un día con sesiones, la vista posiciona el rango visible comenzando **2 horas antes de la sesión más temprana** (`min(horaInicio) - 2 h`, acotado a `00:00`) y asegurando margen hasta **2 horas después de la última** (`max(horaFin) + 2 h`, acotado a `24:00`), manteniendo desplazables las 24 horas completas (si no hay sesiones en el rango, posiciona el scroll inicial en horario diurno base, p. ej. `08:00`).
   - **7 columnas en `Semana` sin `minWidth/minHeight="48dp"` forzado:** las 7 columnas (L–D) se ajustan al ancho de pantalla en móviles y la altura de cada bloque es proporcional a la duración (`HU-17`), aplicando la excepción documentada en `AGENTS.md` §6.2 para pantallas pequeñas.
   - **Línea de hora actual (`HU-18`):** se dibuja en la vista `Día` (cuando es `hoy`) y en la columna de `hoy` de la vista `Semana`.
   - **Sesiones `REPROGRAMADA`:** si coexisten en el mismo horario con la nueva sesión activa (`RN-05`, `RN-09`), se muestran diferenciadas con su icono `↻` (`EstadoSesionVisual`) o pueden ocultarse/filtrarse según los filtros activos.
5. **Creación con pulsación larga y sostenida en fechas `>= hoy` (`HU-20`) y apertura de detalle:**
   - Tanto en `Mes` (sobre un día) como en `Semana` y `Día` (sobre una franja horaria libre), la creación de una nueva sesión se dispara mediante **pulsación larga y sostenida (long press)** únicamente cuando la **fecha es igual o posterior a `hoy` (`fecha >= hoy`)**, abriendo `SesionFormularioActivity` en `MODO_PLANIFICAR` con la fecha precargada (y en `Semana`/`Día` con `horaInicio` de la franja tocada y `horaFin = +60 min` sugerida).
   - Un **toque simple** sobre cualquier sesión existente (pasada, de hoy o futura) abre `SesionDetalleActivity`.
6. **Filtro por materia y resumen de cabecera (`HU-21`, `RN-11`):**
   - **Filtros:** selector de materia (`Todas` o una materia específica) más opción para **incluir/mostrar materias archivadas** en el filtro y en el calendario (`HU-08`).
   - **Resumen en cabecera (`Plan X h · Real Y h`):**
     - `Plan`: suma `duracionPlanificadaMinutos` de las sesiones del periodo visible (y filtro activo) **excluyendo `REPROGRAMADA`** (para no duplicar tiempo reprogramado) y excluyendo sesiones registradas directamente como pasadas sin planificación previa.
     - `Real`: suma `duracionRealMinutos` de las sesiones `COMPLETADA` y `COMPLETADA_TARDIA` (`RN-11`) del periodo visible (y filtro activo).
