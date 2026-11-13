# Marco Teórico y Referencial

## Marco Teórico

### Gestión del tiempo de estudio

La gestión del tiempo es el conjunto de conductas orientadas a usar el tiempo de forma efectiva para alcanzar objetivos: fijar metas y prioridades, planificar y supervisar la ejecución. Macan et al. (1990) encontraron que, en estudiantes universitarios, la percepción de control del tiempo se relaciona con un mejor promedio, mayor satisfacción y menor tensión. Britton y Tesser (1991) mostraron que la planificación de corto plazo y la actitud hacia el tiempo predicen el promedio acumulado mejor que las pruebas de aptitud. StudyPlan implementa estas conductas: planificar sesiones en un calendario, fijar metas semanales y supervisar el cumplimiento.

### Procrastinación académica

Steel (2007) define la procrastinación como el retraso voluntario de una acción prevista pese a esperar consecuencias negativas por ello. Su metaanálisis la describe como un fallo de autorregulación que disminuye cuando las tareas son concretas y cercanas en el tiempo. Por ello la aplicación descompone el estudio en sesiones con fecha y hora, emite recordatorios y registra como *No realizada* toda sesión no cumplida, para que el estudiante tome conciencia de sus postergaciones.

### Establecimiento de metas

Según la teoría del establecimiento de metas de Locke y Latham (2002), las metas específicas y desafiantes producen mejor desempeño que metas vagas como «hacer lo mejor posible», siempre que exista retroalimentación sobre el progreso. La meta semanal de horas, total y por materia, con su barra de progreso y la felicitación al cumplirla, aplica directamente este principio.

### Aprendizaje autorregulado

Zimmerman (2002) describe el aprendizaje autorregulado como un ciclo de tres fases: previsión (planificar y fijar metas), ejecución (autocontrol y autoobservación) y autorreflexión (evaluar los resultados). Los módulos de StudyPlan se corresponden con estas fases: calendario y metas (previsión), sesión en curso con cronómetro, Pomodoro y No molestar (ejecución), y reportes y estadísticas de cumplimiento (autorreflexión).

### Técnica Pomodoro y control de distracciones

La técnica Pomodoro propone trabajar en bloques cortos de concentración, tradicionalmente de 25 minutos, separados por descansos de 5 minutos y por un descanso largo cada cuatro bloques (Cirillo, 2018). StudyPlan la implementa con tiempos configurables y suma como horas de estudio solo los bloques de trabajo. Como complemento, el modo No molestar silencia las notificaciones del teléfono durante la sesión para reducir las interrupciones.

El estudio real no siempre coincide con el plan: el estudiante puede inspirarse y estudiar horas de más, atender una llamada, o dejar el celular y seguir estudiando en grupo. Por eso StudyPlan registra **eventos de estudio** con marca de tiempo (inicio, pausas, salidas y regresos a la app, fin planificado, tiempo extra, cambios de materia) y deja que el estudiante declare el tiempo estudiado con la app cerrada. A partir de esos eventos calcula métricas de concentración, puntualidad y respeto del tiempo, que alimentan la fase de autorreflexión descrita por Zimmerman (2002).

### Ingeniería de requisitos ágil

En los métodos ágiles los requisitos se expresan como **historias de usuario**: descripciones breves de una funcionalidad desde la perspectiva del usuario con el formato «Como [rol], quiero [funcionalidad] para [beneficio]» (Cohn, 2004). Una buena historia cumple los criterios **INVEST**: independiente, negociable, valiosa, estimable, pequeña y comprobable (Wake, 2003). Cada historia se completa con **criterios de aceptación** escritos como *Dado / Cuando / Entonces*, forma que proviene del desarrollo guiado por comportamiento (North, 2006) y permite convertir cada criterio en una prueba. Las historias se priorizan con **MoSCoW** (Must, Should, Could, Won't; Clegg & Barker, 1994) y se estiman en puntos de historia. Las **reglas de negocio** del dominio se documentan aparte y se referencian desde los criterios de aceptación. Así una misma regla, por ejemplo el límite de tres días para el registro tardío, no se repite ni se contradice entre historias.

Los requisitos no funcionales se expresan como **criterios de calidad** según el modelo de calidad del producto ISO/IEC 25010:2023, que define nueve características: adecuación funcional, eficiencia de desempeño, compatibilidad, capacidad de interacción (antes usabilidad), fiabilidad, seguridad, mantenibilidad, flexibilidad (antes portabilidad) y protección (*safety*) (International Organization for Standardization [ISO], 2023).

### Máquinas de estado y patrón State

Una máquina de estados describe los estados posibles de un objeto y los eventos que provocan las transiciones entre ellos. El patrón de diseño **State** (Gamma et al., 1994) implementa esta idea encapsulando cada estado en una clase propia: cada estado sabe únicamente qué acciones admite y a qué estado pasa, sin conocer el contexto completo. En StudyPlan la sesión de estudio sigue una máquina de estados (sección 3.2.1) que impide transiciones inválidas y conserva el registro histórico de lo no estudiado.

### Desarrollo guiado por especificaciones con IA

El *Spec-Driven Development* (SDD) es un enfoque en el que la especificación del comportamiento se escribe y se aprueba **antes** del código, y se convierte en el contrato contra el cual se genera y verifica la implementación. Con asistentes de IA, el SDD reduce el riesgo de que el modelo invente requisitos o se desvíe del alcance, porque el asistente trabaja sobre un documento versionado y validable. OpenSpec es una herramienta de código abierto que implementa este flujo mediante *changes* (propuesta, especificaciones delta, diseño y tareas) que, una vez implementados, se archivan y actualizan las especificaciones vigentes del sistema (Fission-AI, 2026).

## Marco Tecnológico

La @tabla:tabla1 resume las tecnologías seleccionadas. La justificación de cada elección se detalla a continuación y en el documento de diseño del *change* de OpenSpec.

Tabla: Tecnologías seleccionadas para StudyPlan {#tabla1}

| Capa / necesidad | Tecnología | Versión / nivel | Uso en el proyecto |
|---|---|---|---|
| Lenguaje | Java | Java 11 (compatibilidad del proyecto) | Todo el código de la aplicación |
| Plataforma | Android SDK | minSdk 24 · targetSdk 37 | Ejecución en Android 7.0 o superior |
| IDE y build | Android Studio · Gradle (Kotlin DSL) | AGP 9.4.1 · version catalog | Compilación, emulación, depuración |
| Arquitectura | MVVM con Android Jetpack | ViewModel, LiveData | Separación de UI y lógica |
| Persistencia | Room sobre SQLite | Jetpack Room | Base de datos local y offline |
| Interfaz | Views XML + Material Design 3 | Material Components | Componentes, tema claro/oscuro |
| Calendario | Vistas propias (RecyclerView y Canvas) | Java puro | Vistas de día, semana y mes |
| Gráficos | MPAndroidChart | 3.1.0 | Gráfico de anillo y barras |
| Segundo plano | WorkManager / AlarmManager | Jetpack WorkManager | Recordatorios, respaldos, cierre del día |
| Respaldo | Google Drive API (appDataFolder) · Credential Manager | Alcance drive.appdata | Copia de seguridad del usuario |
| Seguridad | javax.crypto (AES-256-GCM, PBKDF2) · androidx.biometric | Java SE / Jetpack | Cifrado del respaldo y bloqueo |
| Sistema | NotificationManager · AppWidgetProvider | Android SDK | No molestar y widget |
| Ciclo de vida | ProcessLifecycleOwner (lifecycle-process) | Jetpack Lifecycle | Detectar salida y regreso a la app |
| Fechas | java.time con desugaring | Core library desugaring | LocalDate/LocalTime en API 24 |
| Pruebas | JUnit 4 · Espresso | 4.13.2 · 3.5.1 | Pruebas unitarias e instrumentadas |
| IA y especificación | Claude Code · OpenSpec | Claude Opus 5.5 · OpenSpec 1.14.3 | Generación asistida y SDD |

Nota: Las versiones de la plataforma corresponden a la plantilla generada por Android Studio el 4 de noviembre de 2026 (gradle/libs.versions.toml). Las demás dependencias se agregarán en los sprints de implementación, con aprobación humana previa.

### Java y la plataforma Android

Android admite Java y Kotlin. Desde 2019 Google promueve Kotlin como lenguaje preferente (Android Developers, s.f.-e), pero Java mantiene soporte completo en el SDK, en Android Studio y en Jetpack. Se eligió **Java** porque es el lenguaje exigido y enseñado en la materia, lo que permite al estudiante comprender y defender cada línea generada. Para usar java.time con minSdk 24 se habilita el *desugaring* de bibliotecas de Java 8+ (Android Developers, s.f.-c). La @tabla:tabla2 compara las alternativas evaluadas.

Tabla: Comparación de alternativas de lenguaje y framework {#tabla2}

| Criterio | Java nativo | Kotlin nativo | Flutter (Dart) |
|---|---|---|---|
| Exigido / enseñado en la materia | Sí | No | No |
| Aplicación nativa Android | Sí | Sí | No (motor propio de renderizado) |
| Soporte de Jetpack (Room, WorkManager) | Completo | Completo y preferente | Mediante plugins |
| Capacidad del estudiante para defender el código | Alta | Media | Baja |
| Multiplataforma (iOS) | No | No (salvo KMP) | Sí |
| Decisión | **Seleccionado** | Descartado | Descartado |

Nota: Elaboración propia. La decisión del lenguaje fue humana y quedó registrada en la bitácora de Auditoría IA.

### Arquitectura MVVM y Android Jetpack

La *Guía de arquitectura de apps* de Android recomienda separar la interfaz de usuario de la capa de datos, con una fuente única de verdad y un flujo de datos unidireccional (Android Developers, s.f.-d). StudyPlan adopta el patrón Modelo–Vista–Modelo de Vista (MVVM): las *Activities* y *Fragments* solo muestran datos y capturan eventos; los *ViewModel* mantienen el estado de pantalla y sobreviven a la rotación; los *Repository* encapsulan el acceso a datos; y los *DAO* de Room ejecutan las consultas. Las reglas de negocio, incluida la máquina de estados, viven en una capa de dominio en Java puro, sin dependencias de Android, que puede probarse con JUnit.

### Room y SQLite

Room es una capa de abstracción sobre SQLite que valida las consultas SQL en tiempo de compilación, obliga a usar consultas parametrizadas y gestiona migraciones de esquema (Android Developers, s.f.-a). Los datos de StudyPlan son relacionales (una materia agrupa muchas sesiones) y deben funcionar sin conexión, por lo que Room resulta más adecuado que SharedPreferences (no relacional) o Firebase (requiere conexión y almacena datos en servidores de terceros).

### Material Design 3

Material Design 3 es el sistema de diseño de Google que define componentes, color, tipografía, temas claro y oscuro y pautas de accesibilidad (Google, s.f.). Su uso garantiza una interfaz familiar para el usuario de Android y apoya los criterios de accesibilidad (RNF-06) y estética (RNF-07).

### Servicios del sistema: WorkManager, No molestar, biometría y widget

WorkManager es la API recomendada para trabajo en segundo plano diferible y garantizado (Android Developers, s.f.-f). Se usará para los recordatorios, el respaldo periódico y el cierre automático del día (paso de sesiones pendientes a no realizadas). El modo No molestar se controla mediante la política de interrupciones de *NotificationManager*, que requiere que el usuario conceda el acceso. El bloqueo de la aplicación usa *BiometricPrompt*, que admite huella, rostro o el PIN del dispositivo (Android Developers, s.f.-b). El widget se implementa con *AppWidgetProvider*, compatible con Java. MPAndroidChart (Jahoda, 2019) dibuja los gráficos de estadísticas. Para los eventos de estudio no se usa un proceso que cuente segundos: se guardan marcas de tiempo en Room y el cronómetro solo se dibuja cuando la app está visible. *ProcessLifecycleOwner* detecta cuándo la app pasa a segundo plano y vuelve, para registrar las salidas y regresos y avisar al estudiante.

### Copia de seguridad en Google Drive y cifrado

La copia de seguridad se inspira en la de WhatsApp: el usuario vincula su cuenta Google, elige una frecuencia (nunca, diaria, semanal, mensual o solo manual) y puede proteger el archivo con una contraseña. Se usa la carpeta de datos de aplicación de Google Drive (*appDataFolder*), oculta al usuario y accesible solo por la aplicación (Google for Developers, s.f.). Si el usuario define una contraseña, el archivo se cifra en el teléfono con AES-256 en modo GCM, con una clave derivada de la contraseña mediante PBKDF2 (Turan et al., 2010). Así, ni Google ni el desarrollador pueden leer el contenido.

### Herramientas de IA: Claude Code y OpenSpec

Claude Code es un asistente de programación agéntico de Anthropic que opera en la terminal y el IDE: lee el repositorio, ejecuta comandos y propone cambios que el desarrollador aprueba (Anthropic, 2026). OpenSpec (versión 1.14.3) se integra con Claude Code mediante comandos (/opsx:propose, /opsx:apply, /opsx:archive) y *skills* instaladas en las carpetas .claude/ y .agents/ del repositorio. Las reglas de trabajo de los agentes están en AGENTS.md y CLAUDE.md. Su uso concreto se documenta en la sección *Auditoría IA*.
