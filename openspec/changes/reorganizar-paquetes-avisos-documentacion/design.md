# Design: reorganizar-paquetes-avisos-documentacion

## Context

### Problema

Dentro de cada capa de la arquitectura hexagonal, los archivos están sueltos en una sola carpeta. Quien abre el proyecto no puede distinguir una entidad de un value object, un enum, un contrato (puerto) o una clase de lectura, ni saber a qué épica pertenece una clase. Además, el inspector de código de Android Studio muestra avisos que `lint` de Gradle no cuenta, y el capítulo 4 del documento no explica la estructura. Motivación y alcance: ver [proposal.md](proposal.md).

### Estado del código (verificado)

| Paquete | Archivos | Subcarpetas |
|---|---|---|
| `domain` | 71 | 0 |
| `application.port.in` | 46 | 0 |
| `application.port.out` | 17 | 0 |
| `application.service` | 50 | 0 |
| `adapter.in.ui` | 70 | 0 |
| `adapter.out.persistence` | 27 | 0 |
| `adapter.out.system` | 9 | 0 |
| `adapter.out.security` | 1 | 0 |

`adapter.out.drive` no existe todavía (el respaldo en Google Drive no está implementado) aunque `AGENTS.md` §6 lo lista.

- `domain` no tiene clases ni miembros con visibilidad de paquete. **Fuera del dominio sí hay 21 clases de nivel superior sin `public`**: `application.service` (4: `DetectorFelicitacionMetas`, `EjecucionHitos`, `NombresDeMateria`, `ValidadorSesiones`), `adapter.in.ui` (16: `AcentoMateria`, `AplicadorApariencia`, `ColorHex`, `EstadoSesionVisual`, `FlujoBloqueoPantalla`, `FormatoHoras`, `FormatoSesion`, `GrupoBotones`, `MensajesDeError`, `ProgramadorRetardado`, `ProgramadorRetardadoHandler`, `ReportesAdapter`, `RolColorEstado`, `SalidaConGracia`, `SelectorEstudioBottomSheet`, `TextosSesion`) y `adapter.out.persistence` (1: `Mapeadores`, con métodos estáticos también sin `public`). Hay además métodos estáticos sin `public` en otras clases. Las pruebas espejo (`ColorHexTest`, `MapeadoresTest`, `SalidaConGraciaTest`) y los dobles (`FakeProgramadorRetardado`, `Pruebas`) dependen de estar en el mismo paquete.
- `ArquitecturaTest` recorre los fuentes con `Files.walk`, así que sigue vigilando las subcarpetas.
- `StudyPlanDatabase` da nombre a la carpeta del esquema exportado (`app/schemas/io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase/1..8.json`).
- `AndroidManifest.xml` nombra 20 componentes con nombre relativo (`.adapter.in.ui.X`) y 1 más en `adapter.out.system` (`ReceptorAdministradorBloqueo`); 47 layouts (37 en `layout/`, 10 en `layout-w600dp/`) declaran `tools:context`. No hay `proguard-rules.pro` ni R8 activado.
- Lo que ve el Product Owner en Android Studio es el inspector de código del IDE, distinto de `lint` de Gradle (53 avisos, 0 errores). `SesionEstudio.finInstante()` devuelve `null` si no hay fin, y `fechaFinLocal()` (línea 469) y `horaFin()` (484) lo usan tras comprobar `planFinUtcMilis == null`: el aviso de `NullPointerException` es probablemente un falso positivo de análisis de flujo. Un uso sin esa guarda es `ObtenerResumenInicioService.java:143` (`candidata.finInstante().esPosteriorA(...)`). `SesionEstudio.conTecnica`, `EjecucionHitos.buscarSiguientePendiente`, `CalculadorRangoHorario.calcularMinutoFinRangoActivo` y `FiltroCalendario.conEstadosVisibles` no se usan en ninguna parte, ni en pruebas. Existe una carpeta `Inspections/` en la raíz con 4 exportaciones XML del inspector, ignorada por git.
- `docs/build/cotejo_clases.py` lista los fuentes con `glob("*.java")` no recursivo (línea 30) y compara los PNG de los diagramas byte a byte; `docs/build/java_fuentes.py` ya usa `rglob`; `docs/build/diagramas_clases.py` agrupa las clases por épica en listas históricas, que no bastan para construir el mapa (no hay lista `comun`).

## Goals / Non-Goals

**Goals:**
- Que cada archivo esté en una carpeta que diga qué es y a qué épica pertenece.
- Que el IDE no muestre avisos evitables del código propio y que cada aviso restante tenga su motivo escrito.
- Que el documento explique la estructura y refleje el código real.

**Non-Goals:**
- Cambiar comportamiento, esquema de Room, nombres de clases o de recursos. Solo cambian paquetes, con una excepción declarada: algunas clases pasan a `public` (decisión 3).
- Quitar `ServicioSesionEnCurso` o implementar el respaldo en Drive.
- Resolver la bibliografía.

## Decisions

### 1. Arquitectura propuesta

```text
domain/
  entity/        Materia, SesionEstudio, EventoEstudio, Perfil, MetaSemanal, Ajustes
  valueobject/   Fecha, Hora, Instante, ConfiguracionPomodoro, IntervaloEfectivo,
                 FiltroCalendario, PeriodoCalendario, PeriodoEstadisticas
  enumeration/   enums de valores (TecnicaEstudio, FasePomodoro, TipoEventoEstudio, Tema, Idioma, ...)
  state/         EstadoSesion (contrato), EstadoPendiente/EnCurso/Completada/... y CodigoEstadoSesion
  service/       reglas sin estado: Calculador*, RepartidorPorDia, GeneradorCsvReporte
  result/        Resultado y ErrorValidacion
  readmodel/     lo que se devuelve a la pantalla: Resumen*, Detalle*, Datos*, ...

application/
  port/in/<épica>/       contratos de entrada, una interfaz por caso de uso
  port/out/persistence/  *Repository, AlmacenDatos
  port/out/system/       Reloj, MarcadorSalidaApp, ProgramadorAvisos*, ControladorNoMolestar, ...
  service/<épica>/       implementación de los contratos de entrada

adapter/
  in/ui/<épica>/         Activity, ViewModel, adaptador de lista y diálogo de la épica
  in/ui/comun/           ActividadBase, ActividadProtegida, aplicadores de apariencia, formatos
  in/ui/                 raíz: solo StudyPlanApplication, FabricaViewModels y lo que nombra el manifiesto (decisión 4)
  out/persistence/       entity/, dao/, repository/ (con Mapeadores); StudyPlanDatabase y Migraciones en la raíz
  out/system/            plano (9 archivos)
  out/security/          plano (1 archivo)
```

Las épicas son `configuracion`, `materias`, `sesiones`, `calendario`, `tecnicas`, `metas`, `estadisticas`, `reportes` y `recordatorios`, más `comun` para lo compartido. Las carpetas técnicas van en inglés (son términos de la arquitectura) y las de épica en español (son nombres del dominio del proyecto). Las dependencias entre capas no cambian y `ArquitecturaTest` las sigue vigilando: `domain` no importa `application` ni `adapter`; `application` no importa `adapter`; ninguno de los dos importa `android.*`.

### 2. Orden de decisión del tipo y de la épica

El tipo de una clase de `domain` se decide con estas preguntas, en este orden; la primera que se cumple gana:

| Pregunta | Carpeta |
|---|---|
| ¿Es un enum? | `enumeration` |
| ¿Forma parte de la máquina de estados de la sesión? | `state` |
| ¿Tiene identidad y cambia en el tiempo? | `entity` |
| ¿Es una regla o cálculo sin estado? | `service` |
| ¿Es un dato que se arma para devolverlo a la pantalla? | `readmodel` |
| Si no, es inmutable y se define por su valor | `valueobject` |

Dos excepciones, escritas una sola vez: `ErrorValidacion` (enum) va a `result` junto a `Resultado`, y `CodigoEstadoSesion` (enum) va a `state`. Con ellas, los 14 enums son 12 en `enumeration`, 1 en `result` y 1 en `state`. Los casos dudosos (`ResultadoSesion`, `ResultadoReparto`, `CierreSesion`, `ConflictoHorario`, `CandidataInvasion`, `EstadoPomodoroCalculado`, `MetaAlcanzada`, `SalidaRegistrada`, `ListadoMaterias`, `DiagnosticoRegresoApp`, `AvisoRecordatorioPendiente`, `ReporteEstudio`, `PreferenciaNoMolestar`) se resuelven con ese orden y la tabla resultante se guarda en el mapa de la decisión 5.

La épica de una clase de `application` o `adapter.in.ui` sale de las listas por épica de `diagramas_clases.py`, con estas reglas:
- Una clase que figura en más de una épica o que sirve a varias va a `comun`.
- Un `*Service` va a la carpeta del puerto de entrada que implementa; si implementa puertos de épicas distintas, va a `comun`.
- `RelojSistema` (implementa `Reloj`, no es Room) va a `adapter/out/system`; `AlmacenDatosRoom` y `SesionesDeMateriaRoom` van a `repository/`; `TramoDiarioEntity` a `entity/`, `TramoDiarioDao` a `dao/` y `TramoDiarioRepositoryRoom` a `repository/`.
- El script falla si una clase tiene 0 o más de 1 carpeta candidata, y avisa (no falla) si una carpeta pasa de 15 archivos, para que `comun` no se vuelva otro cajón.

### 3. Visibilidad de paquete

La regla para las 21 clases sin `public` fuera de `domain`, y para sus métodos estáticos sin `public`: va a la carpeta de su **único** cliente; si la usan dos o más épicas, va a `comun` y se hace `public`. `Mapeadores` va a `repository/` junto a los `*RepositoryRoom`, que son sus únicos clientes; `StudyPlanDatabase.asegurarMateriaGeneral` se hace `public` porque lo usan `AlmacenDatosRoom`, `Migraciones` y una prueba de migración. Pasar una clase a `public` es el único cambio de código de las etapas 1 a 3 y se anota en la bitácora con su motivo. Las pruebas y los dobles que dependen del mismo paquete (`Pruebas`, `FakeProgramadorRetardado`, `ColorHexTest`, `MapeadoresTest`) se mueven junto con la clase que prueban. La simulación del script (decisión 5) imprime, por cada clase sin `public`, quién la usa y desde qué carpeta, y falla ante un uso cruzado que la regla no cubra.

### 4. Lo que no se mueve

- `StudyPlanDatabase` y `Migraciones` (el esquema exportado depende del nombre completo).
- Lo que el manifiesto nombra y el sistema persiste: `StudyPlanApplication`, `ReceptorAdministradorBloqueo` (administrador del dispositivo), `ReceptorAvisosSesion` y `ServicioSesionEnCurso`. Un administrador activado o una alarma programada pueden perderse si cambia el nombre de la clase. Las Activities sí se mueven.
- `adapter.out.system` y `adapter.out.security`: se quedan planos (9 y 1 archivos). `adapter.out.drive` no se crea.
- Recursos (`R`), nombres de tablas y de preferencias.
- Verificación previa a mover: `grep` de nombres completos de clase en `res/xml`, `res/navigation`, `SharedPreferences` y `build.gradle.kts` (hoy sin resultados fuera de `tools:context`).

### 5. Mapa y script temporales

`docs/build/mapa_paquetes.py` (clase → carpeta) y `docs/build/mover_paquetes.py` existen solo para ejecutar el movimiento. El script mueve los `.java` con `git mv`, reescribe `package`, **añade** los `import` que antes no hacían falta por compartir paquete y quita los redundantes, y reescribe `import` con comodín, `{@link}` y `@see` de Javadoc, referencias completas en cadenas (por ejemplo el literal de `CandadoEspressoTest.java:48`), el patrón relativo con punto del manifiesto y de `tools:context` (`.adapter.in.ui.X`), y los comentarios que citan paquetes. Es determinista, tiene modo de simulación y falla ante clases sin mapa, colisiones de nombre entre carpetas (también por mayúsculas, por el sistema de archivos de Windows) y usos cruzados de clases sin `public`. Al cerrar la etapa 3 se archivan o borran: desde entonces la verdad son las carpetas, vigiladas por `EstructuraPaquetesTest`, y `cotejo_clases.py` y los diagramas las recorren.

### 6. Etapas y comprobaciones

Etapa 0 línea base; etapa 1 `domain`; etapa 2 `application`; etapa 3 `adapter`; etapa 4 avisos del IDE; etapa 5 documentación. Las etapas 1 a 3 **no modifican cuerpos de método**: si una tarea lo necesita, se aparta a la etapa 4 (la única excepción es la visibilidad de la decisión 3). La puerta de **cada tarea de movimiento** es `./gradlew assembleDebug assembleDebugAndroidTest test`; la de **cada etapa** añade `lint`, `python docs/build/cotejo_clases.py` (con los PNG de diagramas regenerados) y la comparación del número y los nombres de las pruebas JVM con la línea base: en las etapas 1 a 3 deben ser exactamente los mismos. La etapa 3 cierra con las clases instrumentadas una por una en el emulador. `EstructuraPaquetesTest` comprueba: nada suelto en las raíces de `domain`, `application.port.in`, `application.port.out` y `application.service`; cada subcarpeta de `domain` es una de las siete permitidas; cada `*Service` está junto a su puerto; y cada `tools:context` de los 47 layouts resuelve a un `.java` real.

### 7. Avisos del IDE

La línea base se toma con `inspect.bat` sobre una copia del proyecto (el IDE no debe tenerlo abierto) o con el XML que exporte el Product Owner; se guarda con ella el nombre y la versión del perfil de inspección usado. Se parte de `Inspections/` si sirve. Para cada aviso de nulidad: primero se confirma si es real; si no lo es, variable local; si hace falta un contrato, un método como `tieneFin()`; `Optional` solo al final, porque cambiar la firma de `finInstante()` obliga a tocar cuatro servicios (`ObtenerResumenInicioService`, `PlanificarSesionService`, `RegistrarSesionPasadaService`, `ReprogramarSesionService`) y `SesionEstudio`. No se añade ninguna librería de anotaciones (sería una dependencia nueva que aprobar). El código que no se usa en ninguna parte se elimina; el que solo usan las pruebas se decide en un triaje que revisa el Product Owner. **Meta:** todos los avisos de la línea base resueltos o justificados por escrito con su motivo; nada de `@SuppressWarnings` sin explicación. Los 53 avisos de `lint` de Gradle no son el objetivo de este change: se corrigen solo los de una línea y sin cambio de comportamiento, y el resto se anota para otro change.

### 8. Documentación y `AGENTS.md`

El capítulo 4 gana la sección «Estructura de paquetes y tipos de clase» con el árbol, una lista única de tipos (la de la decisión 2) con una frase y un ejemplo real de cada uno (enum, entidad, value object, contrato o puerto, servicio, estado y clase de lectura), y una nota sobre los nombres homónimos `domain/service` frente a `application/service` y `domain/entity` frente a `adapter/out/persistence/entity`. `AGENTS.md` cambia, con la aprobación de este change, exactamente estas frases: la lista de paquetes de §6 (línea 67; se marca `adapter.out.drive` como «aún no existe»), la regla SRP de §6.1 (línea 71, `application.port.in.<CasoDeUso>` y `application.service.<CasoDeUso>Service`, que pasan a nombrar la épica además de la clase), las referencias a `domain` y `application.port.out` de §6.1 (líneas 72 y 74) y la regla `tools:context` de §6.2 (línea 78, que pasa a `.adapter.in.ui.<épica>.NombreActivity`). Ninguna otra regla se altera.

### Alternativas consideradas

| Alternativa | Motivo del rechazo |
|---|---|
| Organizar todo por épica, con las capas dentro de cada una (package by feature) | Contradice el reparto por capa que fija `AGENTS.md` y mezclaría tipos distintos en cada carpeta |
| Módulos Gradle por capa | El aislamiento ya lo verifica `ArquitecturaTest`; añade complejidad de compilación sin necesidad hoy |
| Distinguir el tipo solo por sufijos de nombre | No se ve en el árbol de carpetas ni se puede verificar con una prueba de estructura |
| Mover con «Move Class» del IDE en lugar del script | Se evalúa con una muestra en la tarea 1.4; el script se justifica por ser repetible y revisable y por añadir imports y reescribir manifiesto y layouts de forma uniforme |
| Un oráculo de firmas (`javap`), modo `--revert` y rama aparte | Sobreingeniería para un movimiento que se hace una vez; `git revert` por tarea y la puerta de compilación y pruebas bastan |
| Un change por objetivo | El Product Owner pidió un solo change por etapas |

## Risks / Trade-offs

- [Visibilidad de paquete rompe la compilación al repartir] → Regla de la decisión 3, simulación que falla ante usos cruzados y compilación de `androidTest` en cada tarea.
- [Imports, manifiesto, `tools:context` o literales sin reescribir] → El script cubre esos patrones, `assembleDebugAndroidTest` y `lint` detectan referencias rotas y `EstructuraPaquetesTest` comprueba los layouts; el literal de `CandadoEspressoTest` queda en la lista del script.
- [Mover componentes que el sistema guarda por nombre] → No se mueven (decisión 4); la app aún no está publicada y hay un solo teléfono de pruebas.
- [Pérdida de `git blame` en archivos pequeños] → `git mv` y la edición de `package` van en el mismo commit; se usa `git log --follow` y se acepta la pérdida donde falle.
- [Una carpeta `comun` demasiado grande] → Aviso de umbral en la simulación; si pasa de 15 archivos, se subdivide por tipo.
- [Corregir avisos cambia código que funciona] → Las etapas 1 a 3 no tocan cuerpos de método; en la etapa 4 cada corrección lleva su prueba y el cambio esperado en el número de pruebas se anota en la bitácora.
- [El mapa y el script se quedan obsoletos] → Se archivan al cerrar la etapa 3; la estructura la vigila `EstructuraPaquetesTest`.
- Seguridad y escalabilidad no aplican: no cambia el comportamiento ni los datos.

## Open Questions

- Si la aprobación de RN-23 y RN-24 del change anterior no llega, la etapa 0 se bloquea y se avisa al Product Owner; no se trabaja en una rama aparte. No cambia el diseño.
