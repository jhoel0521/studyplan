# Tasks

## 1. Compuertas y dependencias

- [x] 1.1 **Revisión humana.** El estudiante revisa esta propuesta (AGENTS.md §3, paso 2) y responde la pregunta abierta de `proposal.md`: aprobación de `androidx.room:room-testing` 2.8.5 y prioridad y puntos de HU-46 (las demás decisiones ya las dio el 14-nov-2026: nombre saneado, paleta provisional, docente libre y desarchivar). Verificar que la respuesta queda en la bitácora antes de tocar Gradle. El agente no marca esta tarea sin esa confirmación (el estudiante aprobó el 14-nov-2026: «si usemos room-testing, apruebo»; ver la bitácora)
- [x] 1.2 Agregar `room-testing` al catálogo `gradle/libs.versions.toml` y a `app/build.gradle.kts` (solo `androidTestImplementation`) y publicar `app/schemas` como recursos de `androidTest`; verificar que `./gradlew assembleDebug assembleDebugAndroidTest` termina sin errores y que el manifest fusionado no suma permisos

## 2. Dominio y casos de uso (JUnit puro, sin android.*)

- [x] 2.1 Crear `Materia` en `domain` con el saneamiento del nombre (recorte, espacios repetidos colapsados, 1 a 50 caracteres sobre el nombre saneado), la clave de unicidad (sin acentos y en mayúsculas con `Locale.ROOT`) y el docente opcional sin límite (vacío es ausente), y ampliar `ErrorValidacion` con `NOMBRE_MUY_LARGO`, `NOMBRE_DUPLICADO`, `MATERIA_NO_ENCONTRADA` y `MATERIA_YA_ACTIVA`; verificar con pruebas JUnit de nombre vacío, solo espacios, espacios internos repetidos, acentos y mayúsculas («Cálculo» y «CALCULO» dan la misma clave, «Año» y «Ano» no), espacios no separables y de ancho cero, 50 y 51 caracteres, docente vacío y datos válidos (HU-06, RN-02)
- [x] 2.2 Crear `RegistrarMateria`, su servicio y el puerto `MateriaRepository`; verificar con pruebas JUnit y repositorio falso que se registra una materia válida, que «Matemáticas», «matemáticas» y «Matematicas» chocan entre activas (RN-02), que una archivada con ese nombre no impide el registro y que un nombre inválido no guarda nada (HU-06)
- [x] 2.3 Crear `ConsultarMateria` y `EditarMateria` con su servicio; verificar con pruebas JUnit que la edición conserva el id y el estado de archivada, que renombrar a un nombre activo existente se rechaza con `NOMBRE_DUPLICADO` (aplicación de RN-02) y que editar una materia inexistente devuelve `MATERIA_NO_ENCONTRADA` (HU-07)
- [x] 2.4 Crear `ArchivarMateria` y `DesarchivarMateria`; verificar con pruebas JUnit que la archivada deja de salir en las activas y sigue saliendo en las archivadas, que archivar una ya archivada no falla, que desarchivar la devuelve a las activas, que desarchivar con el nombre ya en uso por una activa se rechaza con `NOMBRE_DUPLICADO` y que desarchivar una activa devuelve `MATERIA_YA_ACTIVA` (HU-08, HU-46)
- [x] 2.5 Crear `EliminarMateria` y el puerto `SesionesDeMateria`; verificar con pruebas JUnit que con un puerto falso que devuelve sesiones la eliminación se rechaza sugiriendo archivar (RN-03) y que sin sesiones se elimina de forma definitiva (HU-08)
- [x] 2.6 Crear `ListarMaterias` con la lista de activas en orden alfabético sin distinguir mayúsculas y la lista de archivadas; verificar con pruebas JUnit el orden, la lista vacía y que las horas realizadas valen cero mientras no existan sesiones (HU-09)
- [x] 2.7 Ejecutar `./gradlew test` y la prueba de arquitectura; verificar que pasan con las clases nuevas y que ningún fuente de `domain` ni `application` importa `android.`

## 3. Persistencia (adaptadores de salida)

- [x] 3.1 Crear la *Entity* `MateriaEntity`, su DAO, subir `StudyPlanDatabase` a la versión 2 y escribir `MIGRATION_1_2` registrada en el constructor, sin migración destructiva (RNF-12); verificar que `app/schemas/.../2.json` se genera y se versiona junto a `1.json`
- [x] 3.2 Escribir el *mapper*, `MateriaRepositoryRoom` y el adaptador provisional `SinSesionesDeMateria` (siempre cero, con su advertencia en el Javadoc); verificar con pruebas instrumentadas con Room en memoria: guardar y leer (con su clave), editar, archivar, desarchivar, eliminar, orden alfabético y que `borrarTodo()` también vacía `materia` (HU-06 a HU-09)
- [x] 3.3 Escribir la prueba de migración con `MigrationTestHelper`: crear la base v1 con perfil y ajustes, migrar a v2 y validar el esquema contra `2.json`; verificar que perfil y ajustes siguen presentes y que la tabla `materia` existe y está vacía. Actualizar la prueba «la base abre con la versión 1» a la versión 2

## 4. Navegación entre pestañas

- [x] 4.1 Crear `DestinoPrincipal` (Inicio, Calendario, Estadísticas, Materias, con su Activity y si está implementado) y generalizar `enlazarBarraInferior(destinoActivo)` con la regla de pila `[Inicio, pestaña]` (D3); verificar con Espresso que la pestaña activa queda seleccionada, que las no implementadas siguen deshabilitadas, que Atrás desde Materias vuelve a Inicio y que Atrás desde Inicio sale de la app
- [x] 4.2 Aplicar el patrón de envoltorios a Inicio: `contenido_inicio.xml` incluido por `activity_main.xml` y por su variante `layout-w600dp`; verificar que `lint` no reporta errores y que la pantalla se ve igual que antes en el teléfono y en la simulación de 1600×2560

## 5. Presentación (adaptadores de entrada)

- [x] 5.1 Crear `contenido_materias.xml`, `item_materia.xml` y los envoltorios `activity_materias.xml` y `layout-w600dp/activity_materias.xml` con `tools:context`, `tools:text`, componentes Material 3, textos en `@string`, objetivos de 48 dp y sin anchos completos en botones ni campos (HU-09); verificar que `lint` no reporta errores. **Revisión humana:** el estudiante confirma que abre en la vista de diseño; el agente no marca la tarea sin esa confirmación (el estudiante abrió los layouts en Android Studio el 14-nov-2026 y confirmó que se ven bien)
- [x] 5.2 Crear `activity_materia_formulario.xml` con nombre, docente opcional y paleta de colores, con las mismas convenciones (HU-06 a HU-08); verificar que `lint` no reporta errores. **Revisión humana:** el estudiante confirma que abre en la vista de diseño (el estudiante abrió los layouts en Android Studio el 14-nov-2026 y confirmó que se ven bien)
- [x] 5.3 Crear `ListaMateriasViewModel`, `MateriasActivity` y su `ListAdapter` con el estado vacío, la sección plegable de archivadas y el botón «+» (HU-09); verificar con pruebas JUnit del ViewModel (lista, vacía, archivadas) y que no hay acceso a la base en el hilo principal
- [x] 5.4 Crear `MateriaFormularioViewModel` y `MateriaFormularioActivity` para registrar, editar, archivar, desarchivar y eliminar con confirmación, mostrando los errores por código desde `strings.xml` (HU-06 a HU-08 y HU-46); verificar con pruebas JUnit del ViewModel que cada error publica su código y que cancelar la confirmación no elimina
- [x] 5.5 Agregar los textos en `values/strings.xml` y `values-en/strings.xml` y la paleta **provisional** de colores en recursos (la definitiva la fija el Product Owner al final); verificar que `lint` no reporta `MissingTranslation`
- [x] 5.6 Escribir las pruebas Espresso del flujo: registrar una materia y verla en la lista, rechazar un nombre duplicado (también con otras mayúsculas, sin acentos y con espacios de más), editar, archivar y verla en «Archivadas», desarchivarla, eliminar una sin historial, y la lista vacía (HU-06 a HU-09 y HU-46); verificar que pasan
- [x] 5.7 Revisar los avisos del IDE de los archivos nuevos: sin botones ni campos de ancho completo, con variante `layout-w600dp`, `@NonNull` en los parámetros sobrescritos y sin `Optional.get()` en pruebas; verificar con una exportación `Inspections` hecha por el estudiante que no hay avisos nuevos (`./gradlew lint` no incluye esas inspecciones) (el estudiante exportó `Inspections` el 14-nov-2026; la IA revisó los 35 avisos y corrigió los reales)
- [x] 5.8 Reemplazar la paleta, el campo hexadecimal y los deslizadores del formulario por un modal de selección de color (`SelectorColorDialogo`, con la librería ColorPickerView 2.5.0 que el estudiante aprobó el 14-nov-2026): el formulario muestra un círculo con el color y su código y tocarlo abre el modal, con selector de matiz y saturación para arrastrar, barra de brillo, campo hexadecimal y los ocho colores rápidos como atajos (HU-06, HU-07); verificar con pruebas unitarias de `ColorHex`, pruebas Espresso del modal (atajo, hexadecimal válido e incompleto, tocar el selector y cancelar), del guardado y de la rotación, y con capturas del emulador; verificar que `assembleRelease` (R8) compila con la librería
- [x] 5.9 Presentar «Guardar», «Archivar» y «Eliminar» como un grupo de botones unidos, como el `btn-group` de Bootstrap (`GrupoBotones` y el estilo `Widget.StudyPlan.BotonGrupo`), y corregir la franja en blanco doble bajo la barra de navegación (`ActividadBase.onInsets`); verificar con capturas del emulador en teléfono y en pantalla de 1600×2560

## 6. Diagramas y documento

- [x] 6.1 Escribir `docs/build/esquema_db.py`, que lee `app/schemas/.../2.json` y genera el modelo lógico y la tabla del diccionario de datos; trasladarlos a las secciones 4.2.1 y 4.2.2 del capítulo 4; verificar que cada tabla y cada columna del JSON aparece en el diagrama y en el diccionario
- [x] 6.2 Agregar el diagrama de clases de E2 a la sección 4.4 y actualizar el de `add-configuracion-inicial`; leer las firmas de métodos de los fuentes Java (`docs/build/java_fuentes.py`) en vez de escribirlas a mano y ampliar `docs/build/cotejo_clases.py` para comprobar que los diagramas y las secciones 4.2 coinciden con el código y el esquema; verificar que el cotejo pasa y que falla si se renombra un método en el código (comprobado el 14-nov-2026)
- [x] 6.3 Agregar el diagrama de secuencia «registrar materia» (Activity, ViewModel, caso de uso, puerto, adaptador y Room) a la sección 4.4; verificar que cada mensaje corresponde a un método real
- [x] 6.4 Actualizar `flujo_pantallas.png` con las pestañas y la lista de materias con su formulario (sección 4.3); verificar que coincide con la navegación implementada
- [x] 6.7 Actualizar el capítulo 3 y sus anexos: RN-02 con el saneamiento del nombre (origen: decisión del Product Owner del 14-nov-2026), HU-46 en el backlog, la tabla de la épica E2 y la matriz de trazabilidad, y los totales de historias y escenarios; verificar que ninguna cifra del documento queda desfasada
- [x] 6.6 Generar el documento con `python .apa/scripts/generar_docx.py --doc docs/documento` y revisarlo con `revisar.py`; verificar que no hay referencias sin citar nuevas

## 7. Integración y cierre

- [x] 7.1 Instalar la versión anterior (commit `5c2918b`) en un emulador, crear el perfil, instalar encima la versión nueva y comprobar que el perfil se conserva y que Materias funciona; recorrer el flujo completo en los emuladores API 24, 30 y 37 y registrar el resultado en la bitácora
- [x] 7.2 Ejecutar `./gradlew assembleDebug test lint` y `./gradlew connectedAndroidTest`; verificar que terminan sin errores y que el arranque en frío de Materias es de 2 s o menos (RNF-01)
- [x] 7.3 Revisar el código con el subagente `revisor-java` y corregir los hallazgos de severidad alta; verificar que una segunda revisión no los repite
- [x] 7.4 Actualizar la bitácora con la sesión, los errores de la IA y la verificación de cada tarea, y anotar que HU-07, HU-08 y HU-09 quedan parciales y que HU-46 es nueva; verificar que `openspec validate add-gestion-materias --strict` pasa

## Workflow follow-up

- Tomar las capturas reales del producto para la sección 4.3.1 del documento cuando exista la especificación de diseño visual (decisión del estudiante: ahora las pantallas son de baja fidelidad y no se presentan).
- Archivar `add-requisitos-base`, `add-configuracion-inicial` y este change juntos al finalizar la épica, «todo o nada» (decisión del estudiante).
- El change de sesiones (E3) reemplaza `SinSesionesDeMateria`, calcula las horas realizadas y completa los escenarios parciales; es obligatorio antes de dar HU-08 por cumplida.
- El documento se escribe en paralelo al código; generar el Word final no forma parte de este change.
