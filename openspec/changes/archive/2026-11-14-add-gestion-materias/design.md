# Design

## Context

`add-configuracion-inicial` dejó la arquitectura hexagonal en tres capas, Room v1 (perfil y ajustes), la barra de navegación reutilizable con las pestañas Calendario, Estadísticas y Materias deshabilitadas, y una regla de calidad: los avisos del IDE se corrigen, no se silencian. Esta épica es la primera que añade una tabla de negocio, y por eso fija tres decisiones para todas las siguientes.

Los datos de Materia salen de la spec `gestion-materias`, RN-02, RN-03 y el wireframe W2, no del cuadro del modelo de dominio: nombre, color, docente opcional y estado archivada.

## Decisions

### D1. Dominio y casos de uso
`Materia` (id, nombre, clave, color como entero ARGB, docente opcional, archivada) en `domain`, sin `android.*`.

**Saneamiento del nombre (decisión del Product Owner, amplía RN-02):** el nombre guardado se recorta y colapsa los espacios repetidos («  Cálculo   II » pasa a «Cálculo II»), conservando mayúsculas y acentos tal como se escribieron para mostrarlos; la **clave** se calcula quitando los acentos (`Normalizer` NFD, que es Java puro) y pasando a mayúsculas con `Locale.ROOT`, y es la que decide la unicidad. Así «matemáticas», «MATEMÁTICAS» y «Matematicas» son el mismo nombre. **La ñ se conserva** (en español es otra letra: «Año» y «Ano» son nombres distintos); es una decisión de la IA, no del Product Owner, y se somete a su confirmación. Los espacios se colapsan incluyendo los no separables, los de ancho fijo y los de ancho cero. El largo de 1 a 50 caracteres se mide sobre el nombre ya saneado. El docente es un texto libre opcional, sin límite definido: se recortan los extremos y el vacío se guarda como ausente. Los errores son códigos en `ErrorValidacion` (`NOMBRE_VACIO` se reutiliza; se agregan `NOMBRE_MUY_LARGO`, `NOMBRE_DUPLICADO`, `MATERIA_NO_ENCONTRADA` y `MATERIA_YA_ACTIVA`), igual que en el perfil, para que los textos vivan en `strings.xml` (RNF-08).

Puertos de entrada: `RegistrarMateria`, `EditarMateria`, `ConsultarMateria`, `ListarMaterias` (activas y archivadas), `ArchivarMateria`, `DesarchivarMateria` (HU-46) y `EliminarMateria`. Puertos de salida: `MateriaRepository` y `SesionesDeMateria` (¿cuántas sesiones tiene esta materia?). La unicidad (RN-02) la comprueba el servicio comparando la **clave** contra las materias **activas** (al registrar, editar y desarchivar); no es un índice único de la base porque una materia archivada puede repetir el nombre de una activa. El hilo único de ejecución serializa las escrituras, así que no hay carrera entre dos registros.

### D2. Room v2 y la migración
Tabla `materia` (id autogenerado, nombre, `nombre_clave`, color, docente nulo, archivada), con un índice no único sobre `nombre_clave` para la búsqueda. `MIGRATION_1_2` solo crea la tabla; no toca perfil ni ajustes. Se mantiene la regla de D4 de `add-configuracion-inicial`: sin `fallbackToDestructiveMigration`. El esquema `2.json` se exporta a `app/schemas/` y se versiona junto con `1.json`.

**Cómo se prueba (decisión que se somete a aprobación):** con `MigrationTestHelper` de `room-testing` 2.8.5, que abre la base v1 desde `1.json`, la migra y valida el esquema contra `2.json`; exige agregar `app/schemas` a los recursos de `androidTest`. La alternativa sin dependencia nueva es crear a mano la base v1 con SQL en la prueba, que no valida el esquema contra el JSON. Se prefiere `room-testing` porque comprueba justamente el archivo que se versiona.

Las pruebas que cambian por diseño: «la base abre con la versión 1» pasa a versión 2; `borrarTodo` debe vaciar también `materia`.

### D3. Navegación entre pestañas (vale para todas las épicas)
**Una Activity por pestaña, sin Navigation ni dependencias nuevas.** Se mantiene la decisión D5 de `add-configuracion-inicial` («sin Navigation») y se amplía:

- `Inicio` es la raíz. Al pulsar otra pestaña se inicia su Activity y la actual se cierra, salvo `Inicio`, que se abre con `CLEAR_TOP` y `SINGLE_TOP`. Así la pila de retroceso es siempre `[Inicio, pestaña]`: Atrás desde una pestaña vuelve a Inicio y Atrás desde Inicio sale de la app.
- Un enumerado `DestinoPrincipal` (Inicio, Calendario, Estadísticas, Materias) con su clase de Activity y si está implementado. `enlazarBarraInferior(destinoActivo)` habilita solo los implementados y deja de tener las deshabilitaciones escritas a mano; cada épica solo marca su destino como implementado.
- **Variantes de diseño:** cada pantalla principal necesita su versión de teléfono (barra inferior) y de `layout-w600dp` (riel lateral). Para no duplicar el contenido, cada pantalla tiene un `contenido_<pantalla>.xml` que incluyen los dos envoltorios `activity_<pantalla>.xml`.
- Se prueba con Espresso: pestaña seleccionada correcta y comportamiento de Atrás.

El riel de navegación en pantallas de 600 dp o más ya existe desde `add-configuracion-inicial`; la hizo la IA para corregir un aviso del IDE y **ninguna historia lo pidió**. Se documenta aquí para que el Product Owner lo confirme.

### D4. Pantallas
- **Materias:** lista con `RecyclerView` (llega transitivo por Material) y `ListAdapter`, con la sección plegable de archivadas y un botón «+». Tocar una materia abre el formulario de edición. Estado vacío con invitación a registrar la primera (HU-09).
- **Formulario de materia:** nombre, docente opcional y color de una paleta **provisional** mínima (la definitiva la fija el Product Owner al final); en edición, los botones «Archivar» o «Desarchivar» según el estado y «Eliminar» (con confirmación). Las dos Activities siguen las convenciones de D12 de `add-configuracion-inicial`: `tools:context`, `enlazarVistas()`, listeners con nombre.
- **Selector de color (D4b):** el formulario muestra un círculo con el color y su código (`#1E88E5`); tocarlo abre `SelectorColorDialogo`, un `MaterialAlertDialogBuilder` con un layout propio (`dialogo_color.xml`) que usa solo las **vistas** de la librería ColorPickerView (`ColorPickerView` y `BrightnessSlideBar`), no su diálogo predefinido, porque este no tiene campo hexadecimal. El modal sincroniza el selector, el campo hexadecimal (`ColorHex`, Java puro y con pruebas), la vista previa y los ocho colores rápidos; el color es siempre opaco. Un hexadecimal incompleto no cierra el modal y muestra el error. La librería está escrita en Java, pero arrastra `kotlin-stdlib` de forma transitiva (ya presente en el proyecto por otras librerías).
- **Botones unidos y barra de navegación (D4c):** «Guardar», «Archivar» y «Eliminar» forman un grupo de botones del mismo color primario con esquinas redondeadas solo en los extremos (`GrupoBotones`); con un solo botón, en el alta, se redondea por completo. `ActividadBase.onInsets` deja de sumar el relleno inferior en las pantallas con barra de navegación, porque Material ya lo reserva y quedaba una franja en blanco doble.
- **Calidad del IDE como criterio desde el primer layout:** sin botones ni campos de ancho completo (ancho máximo o según contenido), variante `layout-w600dp`, `@NonNull` en los parámetros que sobrescriben métodos del SDK, sin `Optional.get()` en pruebas y sin lambdas anónimas como listeners. Se comprueba con la exportación `Inspections` del IDE, porque `./gradlew lint` no incluye esas inspecciones.

### D5. Partes que dependen de sesiones
`SesionesDeMateria` tiene un adaptador provisional (`SinSesionesDeMateria`) que responde siempre cero. El núcleo y sus pruebas sí cubren RN-03 con un puerto falso que devuelve sesiones. El adaptador provisional queda anotado en `trazabilidad.md` para que el change de sesiones lo reemplace por una consulta real; sin eso, eliminar una materia con sesiones sería posible. Las horas de la lista son `0 h` hasta entonces.

### D6. Borrado de datos
`AlmacenDatos.borrarTodo()` usa `clearAllTables()`, que ya cubre la tabla `materia`. HU-38 no cambia; solo se agrega la verificación.

### D7. Diagramas
- **Modelo lógico (4.2.1) y diccionario de datos (4.2.2):** se generan desde `app/schemas/.../2.json` con `docs/build/diagramas_clases.py`, para que no se desfasen del esquema real. Cada columna necesita su descripción en el script: si falta, el script falla.
- **Clases de E2 (4.4)**. Las firmas de métodos y los campos se leen de los fuentes Java (`docs/build/java_fuentes.py`) en lugar de escribirse a mano, así que el diagrama no puede mentir sobre los métodos. El cotejo (`cotejo_clases.py`) comprueba además que los PNG y las secciones 4.2 coinciden con lo que se dibujaría hoy desde el código y el esquema; renombrar un método lo hace fallar. El diagrama de clases de `add-configuracion-inicial` se regenera igual.
- **Secuencia «registrar materia»** atravesando las tres capas: no existía ningún diagrama de secuencia. Cada mensaje apunta a un `clase.método` real y el script falla si alguno no existe.
- **Flujo de pantallas** con las pestañas y la lista de materias con su formulario.
- **Sin capturas reales en el documento (decisión del estudiante, 14-nov-2026):** la sección 4.3.1 es de wireframes de baja fidelidad; las pantallas actuales todavía no tienen el diseño visual final (colores, tipografía), que se definirá en una especificación propia, y mostrarlas mezclaría el prototipo de baja fidelidad con el producto. Las capturas de trabajo se guardan sin versionar (`docs/documento/recursos/capturas/` está ignorada) y se usarán cuando exista ese diseño.

## Risks / Trade-offs

- **La migración corrompe datos de v1** → `MigrationTestHelper` abre una base v1 con perfil y ajustes y comprueba que siguen ahí tras migrar.
- **Una materia con sesiones se podría eliminar** por el adaptador provisional → solo mientras no existan sesiones; queda como tarea obligatoria del change de sesiones y anotado en la trazabilidad.
- **El diálogo de huella o PIN y la navegación:** el selector de pestaña devuelve `false` tras iniciar la otra pantalla para que la barra de la pantalla actual conserve su pestaña y Atrás vuelva a ella con la selección correcta.
- **Una Activity por pestaña recrea la pantalla al cambiar** → aceptable con pocas pestañas; si la fluidez fuera un problema (RNF-02) se mide y se reconsidera.
- **Los ocho colores rápidos son provisionales** y se reemplazan con la especificación final del Product Owner; el color se guarda como entero y no como índice de paleta, así que cambiarlos no exige migrar datos.
- **Librería de un tercero:** si ColorPickerView dejara de mantenerse, el modal se reemplaza sin tocar el núcleo ni la base: solo `SelectorColorDialogo` y `dialogo_color.xml` dependen de ella.
- **Mostrar el nombre tal como se escribió** (no en mayúsculas) es una decisión de la IA: el Product Owner pidió guardarlo «saneado» y la mayúscula solo se aplica a la clave de unicidad. Si prefiere verlo en mayúsculas, es un cambio de presentación.
- **El riel de navegación** no fue pedido por una HU: queda a confirmar por el Product Owner.
