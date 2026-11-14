# Design

## Context

Estado observado en `app/`: una sola clase, `MainActivity`, con `activity_main.xml` («Hello World!»); tema `Theme.Material3.DayNight.NoActionBar`; Java 11; minSdk 24, targetSdk 37; el catálogo `gradle/libs.versions.toml` solo trae AppCompat, Material, ConstraintLayout, `activity-ktx` y las librerías de prueba (no hay Room, Lifecycle ni Biometric). El manifest declara `android:allowBackup="true"`.

Restricciones vigentes (ver `AGENTS.md`): Java, Views XML + Material 3, arquitectura hexagonal en tres capas con MVVM dentro de la presentación, Room offline-first, sin base en el hilo principal, sin dependencias nuevas sin aprobación humana. Los requisitos son los de `add-requisitos-base`: HU-01 a HU-04 y HU-38 (parcial), RN-01, RN-14, RN-15, RN-21 y los RNF-01, 07, 08, 09, 12, 13, 14, 15, 16 y 17. Motivación y alcance: ver `proposal.md`. Los diagramas de referencia son `arquitectura_hexagonal.png` y `flujo_pantallas.png` (en `docs/documento/recursos/diagramas/`).

## Goals / Non-Goals

**Goals:**
- Dejar una estructura de paquetes y reglas de dependencia que las demás épicas copien sin cambios.
- Que cada pantalla se pueda revisar leyendo su layout XML y su clase: ids, `setContentView`, `onCreate` y los listeners visibles y con nombre.
- Que dominio y aplicación se prueben con JUnit puro, sin emulador.

**Non-Goals:**
- Navegación con pestañas inferiores: llega con la primera épica que tenga más de una pantalla principal.
- Copia de seguridad, cifrado y restauración; widget; datos de sesiones o metas por semana.
- Inyección de dependencias con librería y Navigation Component.

## Decisions

### D1. Tres capas y una regla de dependencia comprobable
Paquetes bajo `io.github.jhoel0521.studyplan`: `domain` (modelo y reglas), `application.port.in` (casos de uso, interfaces), `application.port.out` (interfaces que el núcleo exige), `application.service` (implementan los casos de uso), `adapter.in.ui` (Activity, ViewModel, layouts), `adapter.out.persistence` (Room) y `adapter.out.security` (disponibilidad del bloqueo). `domain` y `application` no importan `android.*` ni `androidx.*`.
Se comprueba con una prueba JUnit que recorre los fuentes de esos paquetes y falla si encuentra esos imports.
*Alternativa descartada:* ArchUnit. Funciona, pero es una dependencia más.

### D2. Puertos y casos de uso de este change
Entrada: `ConsultarEstadoArranque` (¿falta el perfil?, ¿hay bloqueo?), `ConfigurarPerfil`, `ConsultarPerfil`, `ObtenerResumenInicio`, `ConfigurarApariencia` (tema e idioma), `ConsultarAjustes`, `ConfigurarBloqueo`, `ControlarSesionBloqueo` (estado «bloqueada» en memoria) y `BorrarDatos`.
Salida: `PerfilRepository`, `AjustesRepository`, `DisponibilidadBloqueo` (¿el teléfono tiene huella, PIN o patrón?) y `AlmacenDatos` (borrado de todos los datos locales).
Los nombres son definitivos para este change; los de otros changes se definen en su propio diseño.

### D3. Modelo mínimo, sin inventar reglas
`Perfil` (nombre, meta semanal en horas) y `Ajustes` (tema, idioma, bloqueo activo). Validaciones: el nombre no puede quedar vacío tras recortar espacios («Ingresa tu nombre», HU-01); la meta debe ser mayor que 0 y menor o igual a 168 (RN-14, RNF-14). Solo hay un perfil (RN-01). Las enumeraciones de tema y de idioma viven en `domain`. No se usa `java.time`, porque con minSdk 24 exigiría *desugaring*, que no se necesita todavía.

### D4. Room v1 para perfil y ajustes
Dos tablas de una fila cada una (perfil y ajustes), con *mappers* hacia el dominio: las *Entity* nunca salen de `adapter.out.persistence`. Si no hay fila de ajustes, el adaptador devuelve los valores por defecto (tema «del sistema», español, sin bloqueo); así `borrarTodo()` restablece los ajustes sin lógica extra. `exportSchema = true` con el esquema guardado en `app/schemas/` para poder escribir migraciones. Sin `fallbackToDestructiveMigration` ni `allowMainThreadQueries`: son datos del estudiante.
*Alternativa descartada:* `SharedPreferences` para los ajustes. Evitaría un parpadeo de tema al arrancar, pero partiría los datos en dos almacenes y se apartaría de Room, fijado en el stack. Se acepta el parpadeo (ver Riesgos).

### D5. Cinco Activities, sin Navigation
`MainActivity` (Inicio y punto de entrada), `PerfilInicialActivity`, `AutenticacionActivity`, `AjustesActivity` y `BorrarDatosActivity`, cada una con su `activity_*.xml`. Es lo que pide el flujo de `flujo_pantallas.png` para este alcance. Las Activities viven en `adapter.in.ui` (por ejemplo `io.github.jhoel0521.studyplan.adapter.in.ui.MainActivity`): el manifest y `tools:context` usan el nombre con su paquete, y `MainActivity` se mueve desde la raíz en la tarea 4.8. La primera épica que necesite pestañas decide si pasa a Fragments y Navigation.
El acceso a Ajustes es una acción en la barra superior de Inicio. La barra de navegación inferior (Inicio, Calendario, Estadísticas, Materias; wireframe W8) es un layout propio y reutilizable que cada pantalla principal incluye con `<include>` (tarea 4.19); en este change solo Inicio está activa. En el diagrama de flujo esa transición figura «a confirmar con el Product Owner».

### D6. Arranque y bloqueo
`MainActivity` pregunta a `ConsultarEstadoArranque` (en segundo plano) y reacciona: sin perfil abre `PerfilInicialActivity`; con bloqueo activo y sesión bloqueada abre `AutenticacionActivity`; si no, muestra Inicio.
El «volver a primer plano» (HU-04) se detecta con `Application.ActivityLifecycleCallbacks`, contando Activities iniciadas y sin contar los cambios de configuración (rotación). Un componente puro de `application.service` guarda el estado «bloqueada» en memoria y **nace bloqueado** hasta leer los ajustes: si Android termina el proceso y restaura otra Activity, la app no muestra datos sin autenticar. Una clase base de las Activities protegidas revisa ese estado en `onStart()`.
*Alternativa descartada:* `ProcessLifecycleOwner` (artefacto `lifecycle-process`), por ser otra dependencia.

### D7. Huella o PIN
`AutenticacionActivity` muestra una pantalla de bloqueo sin datos y lanza `BiometricPrompt` (`androidx.biometric`) con huella o credencial del dispositivo. El diálogo es parte de la UI, por eso vive en `adapter.in.ui`. La disponibilidad (escenario «Sin método de bloqueo») se consulta con `BiometricManager` desde un adaptador de salida que implementa `DisponibilidadBloqueo`.

### D8. Hilos y ViewModel
Los casos de uso son métodos síncronos de Java puro. El ViewModel los ejecuta en un `ExecutorService` de un solo hilo, creado por la raíz de composición, y publica el resultado con `LiveData.postValue`. No hay corrutinas ni RxJava. El ViewModel no guarda `Context` ni vistas.

### D9. Raíz de composición a mano
`StudyPlanApplication` (declarada con `android:name` en el manifest) crea la base Room, los adaptadores y los servicios, y los expone a las Activities. Cada ViewModel se crea con un `ViewModelProvider.Factory` propio. *Alternativa descartada:* Hilt o Dagger (dependencia; pediste no agregar una).

### D10. Tema e idioma
Room es la única fuente de tema e idioma; la UI los aplica al cargar los ajustes (el modo noche vive solo en el proceso y el idioma lo guarda además AppCompat). Tema: `AppCompatDelegate.setDefaultNightMode` con claro, oscuro y del sistema. Idioma: `AppCompatDelegate.setApplicationLocales`, con `values/strings.xml` en español (por defecto) y `values-en/strings.xml`, más `locales_config.xml` y el servicio de AppCompat para guardar el idioma en versiones anteriores a Android 13. Todos los textos van en recursos (RNF-08).

### D11. Borrado local y ruta de regreso
`BorrarDatos` con alcance «teléfono» invoca `AlmacenDatos.borrarTodo()`, que vacía todas las tablas (cubre las tablas futuras) y restablece los valores por defecto. Como `AppCompatDelegate` debe llamarse en el hilo principal, la UI restablece después el tema («del sistema») y el idioma (español explícito, para que no dependa del idioma del teléfono), limpia la pila y abre `PerfilInicialActivity` (HU-38, RN-21). La doble confirmación es un diálogo de dos pasos. Las opciones «Drive» y «ambos» no se muestran hasta el change de copia de seguridad.

### D12. Convenciones para que XML y Java se revisen juntos
- Cada `activity_*.xml`: raíz con `android:id`, `tools:context=".NombreActivity"`, `tools:text` con datos de ejemplo para la vista previa, componentes Material 3, sin `android:text` fijo (usa `@string`), objetivos táctiles de 48 dp y `contentDescription` donde corresponda.
- Todas las Activities conservan el manejo de *insets* de la plantilla (`EdgeToEdge.enable` y el listener de `WindowInsets`), reunido en un método con nombre `aplicarInsets()` de la clase base. Se supone que con targetSdk 37 Android exige borde a borde; **verificarlo** en la documentación vigente.
- Cada Activity: `setContentView(R.layout.activity_x)` en `onCreate`, luego un método `enlazarVistas()` con los `findViewById`, y los listeners con referencia a método con nombre (`btnGuardar.setOnClickListener(this::onGuardarClick)`), nunca lambdas anónimas.
*Alternativas descartadas:* ViewBinding (genera clases que no aparecen al revisar el código) y `android:onClick` en el XML (el enlace por nombre se rompe al refactorizar).

### D13. Pruebas
JUnit puro para `domain` y `application` con puertos falsos (RNF-15). Instrumentadas con Room en memoria para la persistencia y el borrado, y con Espresso (ya en el catálogo) para los flujos de pantalla. Pruebas de ViewModel con la regla de ejecución instantánea de `core-testing`, dependencia solo de pruebas que se pide aprobar. La huella y el PIN no se automatizan: se verifican a mano en dispositivo y se registran en la bitácora. JaCoCo se configura con el plugin de Gradle ya incluido, para medir las metas de RNF-15.

## Risks / Trade-offs

- **Combinar huella y credencial en `BiometricPrompt` puede variar entre API 24, 28-29 y 30+** → verificar en emuladores API 24, 30 y 37 (RNF-17) y en la documentación vigente antes de dar HU-04 por cumplida. Esta afirmación es una suposición de la IA, marcada para verificación manual.
- **Parpadeo del tema al arrancar** (se lee de Room fuera del hilo principal) → el tema base sigue al sistema, así que solo ocurre si el estudiante eligió lo contrario; se mide en RNF-01 (arranque en frío ≤ 2 s).
- **`android:allowBackup="true"`**: la copia automática de Android podría restaurar datos que el estudiante borró o copiarlos fuera de su control (RNF-13) → no se toca aquí; la política de respaldo la define el change de copia de seguridad. Queda anotado.
- **Idioma cambiado desde los ajustes del sistema (Android 13 o superior)**: como Room manda, el valor guardado podría sobrescribirlo al reabrir → se prueba; si molesta, AppCompat pasa a ser la fuente del idioma.
- **Rotación**: contar Activities podría volver a bloquear la app al girar el teléfono → se excluyen los cambios de configuración y se prueba con Espresso.
- **HU-02 y HU-38 se cumplen a medias** → se prueban solo los escenarios que este change cubre; los demás quedan listados en `tasks.md` como pendientes de sus changes.
- **Más clases por funcionalidad** (puertos, *mappers*, servicios) → es el costo asumido de la arquitectura hexagonal; se compensa con casos de uso de una sola responsabilidad.
- **El editor de diseño de Android Studio no se puede comprobar por línea de comandos** → `./gradlew lint` y `assembleDebug` detectan errores de XML, pero que el layout abra en la vista previa lo confirma una persona.

## Open Questions

- ¿Longitud máxima del nombre de perfil? Las specs no la fijan; se implementa solo «no vacío». Si el Product Owner la define, se agrega una validación.
- ¿La meta semanal admite decimales (por ejemplo 7,5 h)? RN-14 solo fija el rango; se acepta decimal hasta que se indique otra cosa.
