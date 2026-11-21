# Design: reorganizar-paquetes-avisos-documentacion

## Context

Motivación y alcance: ver [proposal.md](proposal.md). Estado verificado en el código:

| Paquete | Archivos | Subcarpetas |
|---|---|---|
| `domain` | 71 | 0 |
| `application.port.in` | 46 | 0 |
| `application.port.out` | 17 | 0 |
| `application.service` | 50 | 0 |
| `adapter.in.ui` | 70 | 0 |
| `adapter.out.persistence` | 27 | 0 |
| `adapter.out.system` / `security` | 9 / 1 | 0 |

- `domain` mezcla clases de siete tipos: 5 entidades con fábrica `Resultado` (`Materia`, `SesionEstudio`, `EventoEstudio`, `Perfil`, `MetaSemanal`), value objects (`Fecha`, `Hora`, `ConfiguracionPomodoro`, `Instante`), 14 enums, el patrón State (`EstadoSesion` y 5 estados), 8 reglas sin estado (6 `Calculador*`, `RepartidorPorDia`, `GeneradorCsvReporte`), `Resultado` y unas 28 clases de lectura (`Resumen*`, `Detalle*`, `Datos*`).
- El dominio no tiene clases ni miembros `package-private` (0 de 71), así que dividirlo en subcarpetas no rompe visibilidad. `ArquitecturaTest` recorre los fuentes con `Files.walk`, de modo que sigue vigilando las subcarpetas.
- `docs/build/diagramas_clases.py` ya agrupa las clases por épica (listas `PRESENTACION_*`, `ENTRADA_*`, `SERVICIOS_*`, `SALIDA_*`, `DOMINIO_*`, `PERSISTENCIA_*`), y esa agrupación es la fuente para los casos de uso.
- Lo que ve el estudiante en Android Studio es el inspector de código del IDE, distinto de `lint` de Gradle (53 avisos, 0 errores). Ejemplos verificados: `SesionEstudio.finInstante()` devuelve `null` si no hay fin y se usa como `finInstante().fechaLocal()` y `finInstante().horaLocal()` (líneas 469 y 484); `SesionEstudio.conTecnica` no se usa (línea 669). Un escaneo grueso añade `EjecucionHitos.buscarSiguientePendiente`, `CalculadorRangoHorario.calcularMinutoFinRangoActivo` y `FiltroCalendario.conEstadosVisibles`, y unos 14 métodos usados solo desde pruebas.
- El capítulo 4 describe las capas pero organiza los componentes por épica, sin explicar los tipos de clase; hay 2 marcas `[[pendiente]]` y el capítulo 3 tiene 1 cita sin referencia y `90-referencias.md` 2 referencias sin cita.

## Goals / Non-Goals

**Goals:**
- Que cada archivo esté en una carpeta que diga qué es y a qué caso de uso pertenece.
- Que el IDE y `lint` no muestren avisos evitables del código propio.
- Que el documento explique la estructura y refleje el código real.

**Non-Goals:**
- Cambiar comportamiento, esquema de Room, nombres de clases o de recursos; solo cambian paquetes.
- Resolver la bibliografía: las inconsistencias se listan para el autor.

## Decisions

### 1. Estructura objetivo (carpetas en inglés para lo técnico, en español para el caso de uso)

```
domain/
  entity/        Materia, SesionEstudio, EventoEstudio, Perfil, MetaSemanal, Ajustes
  valueobject/   Fecha, Hora, Instante, IntervaloEfectivo, ConfiguracionPomodoro,
                 FiltroCalendario, PeriodoCalendario, PeriodoEstadisticas
  enumeration/   los 12 enums de valores (TecnicaEstudio, FasePomodoro, TipoEventoEstudio, ...)
  state/         EstadoSesion (contrato) + EstadoPendiente/EnCurso/Completada/... + CodigoEstadoSesion
  service/       6 Calculador*, RepartidorPorDia, GeneradorCsvReporte (reglas sin estado)
  result/        Resultado, ErrorValidacion
  readmodel/     Resumen*, Detalle*, Datos*, Estadisticas*, Progreso*, ... (lo que se devuelve a la pantalla)

application/
  port/in/<caso de uso>/     contratos de entrada (un caso de uso por interfaz)
  port/out/persistence/      *Repository, AlmacenDatos
  port/out/system/           Reloj, MarcadorSalidaApp, ProgramadorAvisos*, ControladorNoMolestar, ...
  service/<caso de uso>/     implementación de los puertos de entrada

adapter/
  in/ui/<caso de uso>/       Activity, ViewModel, adaptador de lista y diálogo del caso de uso
  in/ui/comun/               ActividadBase, ActividadProtegida, aplicadores de apariencia, formatos
  in/ui/                     (raíz) solo StudyPlanApplication y FabricaViewModels
  out/persistence/{entity,dao,repository}/   Room; StudyPlanDatabase y Migraciones quedan en la raíz
  out/system/  out/security/  out/drive/     sin subcarpetas por ahora
```

Casos de uso (épicas): `configuracion`, `materias`, `sesiones`, `calendario`, `tecnicas`, `metas`, `estadisticas`, `reportes`, `recordatorios`, más `comun` para lo compartido.

Criterio para decidir el tipo: con identidad y cambia en el tiempo → `entity`; inmutable y se define por su valor → `valueobject`; lista cerrada de valores → `enumeration`; contrato de la máquina de estados y sus implementaciones → `state`; regla sin estado → `service`; combina datos para mostrarlos → `readmodel`. *Alternativas descartadas:* organizar todo por caso de uso (mezcla tipos y oculta el dominio compartido); dejar los enums junto a su entidad (el estudiante pidió distinguirlos por tipo).

### 2. Mapa explícito y script de movimiento

Un archivo `docs/build/mapa_paquetes.py` guarda, para cada clase, su carpeta destino (las listas de caso de uso salen de `diagramas_clases.py` y el tipo de dominio de la tabla anterior). Un script de movimiento lee ese mapa, mueve los `.java` con `git mv`, reescribe `package`, `import` y referencias completas en fuentes, pruebas, `AndroidManifest.xml` y `tools:context` de los layouts, y falla si alguna clase del código no está en el mapa. El mapa es la fuente única para `cotejo_clases.py`.

### 3. Etapas y comprobación al cierre de cada una

Etapa 0: archivar el change anterior y tomar la línea base (compilación, pruebas, `lint`, avisos del IDE). Etapa 1: `domain`. Etapa 2: `application`. Etapa 3: `adapter`. Etapa 4: avisos del IDE. Etapa 5: documentación final. Cada etapa termina con `./gradlew assembleDebug test lint`, el cotejo de clases y su parte del documento; la etapa 3 añade las pruebas instrumentadas una clase por vez en el emulador. Se hace un commit por tarea.

### 4. Cosas que no se mueven

`StudyPlanDatabase` permanece en `adapter.out.persistence` porque Room exporta el esquema en una carpeta con su nombre completo (`app/schemas/...StudyPlanDatabase`) y las pruebas de migración la leen. `StudyPlanApplication` permanece en la raíz de `adapter.in.ui` por el manifiesto. Los recursos (`R`) y los nombres de tablas no cambian.

### 5. Avisos del IDE

El inspector se ejecuta por línea de comandos (`inspect.bat` de Android Studio, sobre una copia del proyecto si el IDE lo tiene abierto) o el Product Owner exporta «Inspect Code» a XML; esa lista es la línea base. Los posibles `NullPointerException` del dominio se corrigen con `Optional` o variables locales, sin anotaciones (`domain` no puede importar `androidx` y una librería de anotaciones sería una dependencia que aprobar). El código sin uso se elimina si ningún test lo exige; el que solo usan pruebas se elimina junto con su prueba o se justifica. Un aviso que no tenga arreglo razonable se documenta con su motivo en lugar de silenciarlo con `@SuppressWarnings` sin explicación.

### 6. Documentación

Sección «Estructura de paquetes y tipos de clase» al comienzo de «Diseño de Componentes y Módulos», con el árbol, una tabla de definiciones y un ejemplo real de cada tipo; los diagramas de clases y `cotejo_clases.py` pasan a recorrer subcarpetas; `AGENTS.md` §6 actualiza la lista de paquetes. Los wireframes W1 a W15 del Anexo A se trasladan donde indica la marca pendiente. La marca de diagramas de clases de épicas futuras (respaldo en Drive) se mantiene porque esas épicas no existen. Las citas y referencias inconsistentes no se corrigen sin el autor.

## Risks / Trade-offs

- [Movimiento masivo con imports y `tools:context` rotos] → Script con mapa explícito, compilación y pruebas al cierre de cada etapa, y un commit por tarea para poder revertir una etapa.
- [Un archivo movido que el manifiesto o un layout aún nombra] → El script reescribe manifiesto y layouts; `lint` y `assembleDebug` detectan referencias rotas.
- [Cambiar `AGENTS.md` §6, que es decisión humana] → La aprobación de este change por el Product Owner cubre la actualización; no se altera ninguna otra regla.
- [Corregir avisos cambia código que funciona] → Cada corrección con su prueba, y los 363 tests JVM más las clases instrumentadas siguen en verde.
- [`inspect.bat` no corre con el IDE abierto sobre el mismo proyecto] → Copia del proyecto en el directorio temporal o exportación manual del Product Owner.
