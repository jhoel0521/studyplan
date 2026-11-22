# Tasks

**Reglas**
- Rutas relativas a `app/src/main/java/io/github/jhoel0521/studyplan/` (**main**), `app/src/test/java/io/github/jhoel0521/studyplan/` (**test**), `app/src/androidTest/java/io/github/jhoel0521/studyplan/` (**androidTest**) y `app/src/main/res/` (**res**).
- Sin commits ni `push` salvo pedido explícito (`AGENTS.md` §4). Cada clase y cada prueba nuevas citan HU-05 en su Javadoc.
- Compuerta de cada tarea de código: `bash ./gradlew assembleDebug assembleDebugAndroidTest test` (con `ANDROID_HOME=$HOME/Android/Sdk` en esta máquina). Compuerta de cada grupo: la anterior más `lint` sin errores ni avisos nuevos.
- Las pruebas instrumentadas solo se corren si hay emulador o teléfono; la IA no instala nada en el teléfono sin permiso explícito del Product Owner.

## 1. Lectura de las 2 próximas materias (HU-05)

- [x] 1.1 Ampliar `main/domain/readmodel/ResumenInicio.java` con `proximasSesiones()` (hasta 2, lista no modificable; los constructores existentes la dejan vacía) y calcularla en `main/application/service/configuracion/ObtenerResumenInicioService.java` con el mismo recorrido que `proximaSesion()` (la primera es la misma). Modifica: esos dos archivos. Verificación: casos nuevos en `test/application/service/configuracion/ObtenerResumenInicioServiceTest` (0, 1 y 3 pendientes; la primera de la lista es `proximaSesion()`), y las pruebas de Inicio siguen en verde sin cambios.

## 2. Modelo de presentación (HU-05)

- [x] 2.1 Crear `main/adapter/in/ui/configuracion/ModeloWidgetProgreso.java` (estado `NORMAL`, `SIN_PERFIL`, `SIN_META` o `SIN_SESION`, porcentaje 0–100, horas y meta, y la lista de hasta 2 próximas), sin `android.*`. Verificación: `test/adapter/in/ui/configuracion/ModeloWidgetProgresoTest` con sin perfil, meta 0, 0 %, 60 %, más de 100 % (se acota), 0, 1 y 2 próximas.
- [x] 2.3 Crear `main/adapter/in/ui/configuracion/ObservadorProgresoWidget.java` (`InvalidationTracker.Observer` sobre `sesion_estudio`, `meta_semanal`, `perfil` y `materia`, con agrupación de eventos seguidos) y registrarlo en `StudyPlanApplication`. Verificación: `androidTest/.../ObservadorProgresoWidgetTest` (se corre en el teléfono o el emulador). *Hecho en la primera versión del widget; no cambia.*

## 3. Widget 1×4 (HU-05, RNF-19)

- [x] 3.1 Reemplazar `res/layout/widget_progreso_2x2.xml` y `widget_progreso_4x2.xml` por `res/layout/widget_progreso_1x4.xml` (logo, anillo con porcentaje, «Esta semana» y «horas / meta», hasta 2 líneas de próxima materia con su barra de color y el mensaje de estado vacío); actualizar `res/xml/widget_progreso_info.xml` y `res/xml-v31/widget_progreso_info.xml` al tamaño 1×4; ajustar las cadenas es/en. Verificación: `assembleDebug assembleDebugAndroidTest`, `lint` sin avisos nuevos y `ReglasEstiloLayoutTest` en verde; sin `contentDescription` faltantes ni colores hex en el layout.
- [x] 3.2 Adaptar `main/adapter/in/ui/configuracion/WidgetProgresoProvider.java` a un solo layout (sin umbral de ancho ni elección 2×2/4×2) que rellene el logo, el anillo, las 2 próximas y los estados vacíos, y quitar lo que ya no se usa. Verificación: compuerta de tarea; `androidTest/.../WidgetProgresoProviderTest` infla el layout en cada estado (se corre en el teléfono o el emulador).

## 4. Calidad y documentación

- [x] 4.1 Comprobar la arquitectura y los avisos: `bash ./gradlew assembleDebug assembleDebugAndroidTest test lint`, `ArquitecturaTest` y `EstructuraPaquetesTest` en verde y `python3 docs/build/comparar_pruebas.py` con solo las pruebas nuevas de más. Verificación: salidas anotadas en la bitácora.
- [x] 4.2 Documentar: cambiar la fila de RNF-19 del capítulo 3 a «widget de 1×4 celdas», reescribir la sección «Widget de progreso semanal» del capítulo 4 y añadir las filas de la sesión a `docs/auditoria-ia/bitacora.md` (con el cambio de decisión del Product Owner y los errores de la IA). Verificación: `python3 .apa/scripts/revisar.py --doc docs/documento` sin marcas nuevas.

## 5. Prueba en el teléfono del Product Owner

- [ ] 5.1 Con permiso explícito del Product Owner, instalar el APK de debug por `adb install -r` (sin `uninstall`, para conservar los datos) y comprobar en el teléfono: agregar el widget 1×4; ver el logo, la meta («horas / meta» y porcentaje) y las 2 próximas materias; completar una sesión y ver el cambio; tocar el widget y ver la pantalla de inicio; estados vacíos; tema claro y oscuro. Verificación: resultado anotado en la bitácora; las fallas se corrigen antes de archivar.

## Workflow follow-up

- Archivar el change tras la revisión humana y la prueba en el teléfono (`/opsx:archive`).
- Si el Product Owner quiere las ideas fuera de alcance, se piden como una HU nueva en un `/opsx:propose` aparte.
