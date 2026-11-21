# Tasks

Reglas: un `git commit` por tarea al terminarla (`refactor(...)`, `fix(...)`, `test(...)` o `docs: ...`; sin coautor; archivos por ruta; sin push). Este change no cambia HU ni RN: los mensajes citan `RNF-15` o `RNF-16`. Rutas relativas a `app/src/main/java/io/github/jhoel0521/studyplan/` (**main**), `app/src/test/java/io/github/jhoel0521/studyplan/` (**test**) y `app/src/androidTest/java/io/github/jhoel0521/studyplan/` (**androidTest**). Las pruebas de dispositivo se corren **solo en el emulador** (`ANDROID_SERIAL=emulator-5554`), una clase por vez: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<clase>`. Al cierre de cada etapa se corren `./gradlew assembleDebug test lint` y `python docs/build/cotejo_clases.py`, y se actualiza la parte del documento de esa etapa.

## 1. Etapa 0: línea base y herramientas

- [ ] 1.1 Archivar el change `ajustar-notificacion-sesion-sin-reproductor` (precondición; requiere la aprobación del Product Owner de RN-23 y RN-24, tarea 1.1 de ese change) con `/opsx:archive`. Verificación: `openspec list --json` ya no lo muestra como activo y el archivo queda commiteado.
- [ ] 1.2 Tomar la línea base de compilación y pruebas: `./gradlew assembleDebug test lint` y contar pruebas JVM, avisos y errores de `lint`; anotarlos en `docs/auditoria-ia/bitacora.md`. Verificación: la fila de la bitácora tiene los números (hoy 363 pruebas JVM, 0 errores y 53 avisos de `lint`).
- [ ] 1.3 Tomar la línea base del inspector de Android Studio: ejecutar `inspect.bat` de `C:\Program Files\Android\Android Studio\bin` sobre una copia del proyecto en el directorio temporal (el IDE no debe tenerlo abierto), o pedir al Product Owner el XML de «Inspect Code». Guardar solo el resumen por tipo de aviso en la bitácora. Verificación: el resumen lista cada tipo de aviso con su cantidad e incluye los ejemplos conocidos (`fechaLocal`, `horaLocal`, `conTecnica`).
- [ ] 1.4 Crear `docs/build/mapa_paquetes.py` (clase → carpeta destino, con las listas por épica de `docs/build/diagramas_clases.py` y los tipos de dominio del diseño) y `docs/build/mover_paquetes.py` con modo de simulación. Verificación: la simulación termina con 0 clases sin mapa, 0 colisiones de nombre y una lista de archivos que coincide con `git ls-files`.

## 2. Etapa 1: dominio por tipo (RNF-15)

- [ ] 2.1 Mover `domain` a `entity`, `valueobject`, `enumeration`, `state`, `service`, `result` y `readmodel` con el script (`git mv`, `package`, `import`, referencias completas), incluidas las pruebas espejo de `test` y `androidTest`. Modifica: main `domain/**`, y las importaciones en `application/**`, `adapter/**`, `test/**`, `androidTest/**`. Verificación: `./gradlew assembleDebug test` compila y los 363 tests JVM pasan.
- [ ] 2.2 Añadir `test/EstructuraPaquetesTest.java`: ningún `.java` en la raíz de `domain`, cada subcarpeta es una de las siete permitidas, `EstadoSesion` y sus implementaciones están en `state`, y los enums solo en `enumeration`, `state` o `result`. Verificación: la prueba pasa y falla si se deja una clase suelta en `domain`.
- [ ] 2.3 Documentar la etapa 1: `docs/build/cotejo_clases.py` y `docs/build/diagramas_clases.py` leen el dominio por subcarpeta, y el capítulo 4 gana la sección «Estructura de paquetes y tipos de clase» con el árbol de `domain`, la tabla de definiciones (enum, entidad, value object, contrato, servicio) y un ejemplo real de cada uno. Modifica: `docs/documento/capitulos/04-diseno-del-software.md`, `docs/build/cotejo_clases.py`, `docs/build/diagramas_clases.py`. Verificación: `python docs/build/cotejo_clases.py` pasa y `python .apa/scripts/revisar.py --doc docs/documento` no añade marcas.

## 3. Etapa 2: aplicación por caso de uso (RNF-15)

- [ ] 3.1 Mover `application/port/in` a una subcarpeta por caso de uso (`configuracion`, `materias`, `sesiones`, `calendario`, `tecnicas`, `metas`, `estadisticas`, `reportes`, `recordatorios`, `comun`). Verificación: `./gradlew assembleDebug test` pasa.
- [ ] 3.2 Mover `application/service` a las mismas subcarpetas, con sus pruebas espejo en `test/application/service/`. Verificación: `./gradlew test` pasa con los mismos tests.
- [ ] 3.3 Mover `application/port/out` a `persistence` (`*Repository`, `AlmacenDatos`) y `system` (`Reloj`, `MarcadorSalidaApp`, programadores de avisos, `ControladorNoMolestar`, `DisponibilidadBloqueo` y el resto), y sus dobles de `test/fakes`. Verificación: `./gradlew assembleDebug test` pasa y `ArquitecturaTest` sigue en verde.
- [ ] 3.4 Ampliar `EstructuraPaquetesTest` a `application` (nada suelto en `port.in`, `port.out` ni `service`; cada `*Service` en la misma carpeta que su puerto) y documentar la etapa: árbol de `application` en el capítulo 4, cotejo, diagramas y la lista de paquetes de `AGENTS.md` §6. Modifica: `test/EstructuraPaquetesTest.java`, `docs/documento/capitulos/04-diseno-del-software.md`, `AGENTS.md`, `docs/build/*.py`. Verificación: la prueba, el cotejo y `revisar.py` pasan.

## 4. Etapa 3: adaptadores (RNF-15)

- [ ] 4.1 Mover `adapter/out/persistence` a `entity`, `dao` y `repository` (`StudyPlanDatabase`, `Migraciones` y `Mapeadores` se quedan en la raíz). Verificación: `./gradlew assembleDebug test` pasa y `git status app/schemas` no muestra cambios en el esquema exportado.
- [ ] 4.2 Mover `adapter/in/ui` a una subcarpeta por caso de uso y `comun` (la raíz solo conserva `StudyPlanApplication` y `FabricaViewModels`), y actualizar `app/src/main/AndroidManifest.xml` (20 referencias) y `tools:context` de los 47 layouts. Verificación: `./gradlew assembleDebug lint` compila, `lint` no añade errores ni recursos sin usar y el cotejo pasa.
- [ ] 4.3 Mover `adapter/out/system` y `security` si el mapa lo indica y las pruebas de `androidTest` a la estructura espejo. Verificación: `./gradlew assembleDebug` compila el APK de pruebas.
- [ ] 4.4 Correr todas las clases instrumentadas una por una en el emulador (`androidTest`: 42 clases) con un script. Verificación: 0 fallos; si una clase falla por algo distinto del movimiento, anotarlo en la bitácora.
- [ ] 4.5 Documentar la etapa: árbol de adaptadores en el capítulo 4, `AGENTS.md` §6 con la estructura final, diagramas y cotejo; ampliar `EstructuraPaquetesTest` a los adaptadores. Verificación: la prueba, el cotejo y `revisar.py` pasan.

## 5. Etapa 4: avisos del IDE (RNF-15)

- [ ] 5.1 Corregir los posibles `NullPointerException` de `SesionEstudio.finInstante()` (usado en `fechaFinLocal()` y `horaFin()`) con `Optional` o variables locales, y los demás avisos de nulidad de la línea base del dominio. Verificación: test JVM en `test/domain/entity/SesionEstudioTest` que cubre sesión con y sin fin, y `./gradlew test` pasa.
- [ ] 5.2 Eliminar el código sin uso: `SesionEstudio.conTecnica`, `EjecucionHitos.buscarSiguientePendiente`, `CalculadorRangoHorario.calcularMinutoFinRangoActivo`, `FiltroCalendario.conEstadosVisibles` y los métodos que solo usan las pruebas (se elimina el método con su prueba o se justifica por escrito). Verificación: `./gradlew test` pasa y el inspector ya no los lista.
- [ ] 5.3 Corregir el resto de avisos del inspector de `application` y `domain` (nulidad, declaraciones y parámetros sin uso, redundancias) según la línea base. Verificación: `./gradlew test` pasa y el inspector baja el recuento de esas carpetas a 0 o a avisos justificados.
- [ ] 5.4 Corregir el resto de avisos del inspector de `adapter` (presentación y persistencia). Verificación: `./gradlew assembleDebug test lint` pasa y el inspector baja su recuento a 0 o a avisos justificados.
- [ ] 5.5 Atender los avisos de `lint` de Gradle (53 hoy: `UseCompoundDrawables`, `ObsoleteSdkInt`, `GradleDependency`, `SetTextI18n`, `ApplySharedPref`, `PluralsCandidate` y otros): corregir los baratos y justificar por escrito los que no (por ejemplo `GradleDependency` es decisión de versiones). Verificación: `./gradlew lint` con 0 errores y cada aviso restante con su motivo en la bitácora.
- [ ] 5.6 Volver a correr el inspector sobre una copia del proyecto y comparar con la línea base. Verificación: 0 avisos propios sin justificar; la comparación queda en la bitácora.

## 6. Etapa 5: documentación final

- [ ] 6.1 Ordenar el capítulo 4 de forma coherente: la sección de estructura primero, las épicas en el mismo orden que en `openspec/specs/`, sin secciones duplicadas ni títulos antiguos. Modifica: `docs/documento/capitulos/04-diseno-del-software.md`. Verificación: `python .apa/scripts/revisar.py --doc docs/documento` sin marcas nuevas y el índice generado coherente (`python .apa/scripts/generar_docx.py --doc docs/documento` termina sin errores).
- [ ] 6.2 Trasladar los wireframes de baja fidelidad W1 a W15 del Anexo A a «Wireframes y Mockups» del capítulo 4 y resolver esa marca `[[pendiente]]`. Modifica: `docs/documento/capitulos/04-diseno-del-software.md`, `docs/documento/capitulos/95-anexos.md`. Verificación: `revisar.py` muestra una sola marca pendiente (la de diagramas de épicas futuras, que depende del respaldo en Drive).
- [ ] 6.3 Anotar para el autor, sin corregirlas, las inconsistencias de citas y referencias (la cita «ISO (2023)» sin referencia y las referencias «Android (s.f.)» y «Google (s.f.)» sin cita). Verificación: aparecen en la bitácora con su ubicación.
- [ ] 6.4 Regenerar los diagramas y cotejar: `python docs/build/diagramas.py` y `python docs/build/cotejo_clases.py`. Verificación: «Cotejo correcto» y diagramas al día.
- [ ] 6.5 Añadir a `docs/auditoria-ia/bitacora.md` las filas de esta sesión (solicitud, resultado, decisión humana, verificación) con los errores de la IA, y correr la verificación integral: `./gradlew assembleDebug test lint`, `openspec validate reorganizar-paquetes-avisos-documentacion --strict` y las clases instrumentadas una por una. Verificación: resultados anotados.

## Workflow follow-up

- Con permiso explícito del Product Owner, instalar el APK en su teléfono y comprobar que la app arranca y sus pantallas abren igual que antes.
- Archivar el change tras la revisión humana (`/opsx:archive`).
