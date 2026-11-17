# Tasks

> Requisito previo: `add-tiempo-utc` aplicado y archivado (Room v5, `tramo_diario`, `RN-31`).

## 1. Dominio puro en Java (`domain`)

- [x] 1.1 Agregar a `Materia` la meta semanal opcional (validada con `RN-14`) y la marca de materia general, con las reglas de `RN-32` (sin renombrar, archivar, eliminar ni meta), con pruebas en `MateriaTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.2 Crear `MetaSemanal` (semana, materia o total, objetivo, felicitada) y `CalculadorProgresoMetas` (progreso por semana, metas cumplidas y metas que se cruzan en un tramo), con pruebas en `CalculadorProgresoMetasTest.java` (metas independientes `RN-14`, solo realizadas `RN-11`, historial `RN-33`, varias metas en un tramo `RN-16`) verificadas con `./gradlew testDebugUnitTest`.
- [x] 1.3 Agregar a `SesionEstudio` la sesión sin plan (`estudiarAhora`, transición Nueva → En curso de `RN-07`) y excluirla de cruces (`RN-05`), vencimiento (`RN-08`) y «Plan», con pruebas en `SesionEstudioSinPlanTest.java` verificadas con `./gradlew testDebugUnitTest`.

## 2. Puertos y servicios (`application`)

- [x] 2.1 Crear el puerto `MetaSemanalRepository` y su *fake*, y los casos de uso `DefinirMetaSemanal` (`HU-25`, `RN-15`) y meta por materia en `RegistrarMateria` y `EditarMateria` (`HU-26`), con pruebas en `MetasServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.2 Crear `ConsultarProgresoMetas` (Inicio, Materias e historial, `HU-27`, `RN-31`, `RN-33`) y adaptar `ObtenerResumenInicioService` y `ListarMateriasService`, con pruebas en `MetasServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.3 Crear `EstudiarAhora` (`HU-47`: sin sesión En curso, consulta de la Pendiente de hoy, materia nueva, «Estudios varios»), con pruebas en `EstudiarAhoraServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.4 Crear `CambiarDeMateria` (`HU-48`: cierre con `cambiarASiguiente`, destino planificado o sin plan, transacción única con `tramo_diario` y eventos), con pruebas en `CambiarDeMateriaServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.5 Calcular y marcar las felicitaciones del tramo al cerrar (`HU-28`, `RN-16`) en `DetenerSesion`, en la declaración de fin, en el cierre por plazo y en el registro tardío, sin felicitar en `CambiarDeMateria`, con pruebas en `FelicitacionMetasServiceTest.java` verificadas con `./gradlew testDebugUnitTest`.
- [x] 2.6 Proteger «Estudios varios» en `EditarMateriaService`, `ArchivarMateriaService` y `EliminarMateriaService` con `MATERIA_GENERAL_PROTEGIDA`, con pruebas en `MetasServiceTest.java` y `ArquitecturaTest` verificadas con `./gradlew testDebugUnitTest`.

## 3. Persistencia Room v6 (`adapter.out.persistence`)

- [x] 3.1 Ampliar `MateriaEntity`, crear `MetaSemanalEntity`, `MetaSemanalDao` y `MetaSemanalRepositoryRoom`, permitir `plan_fin_utc_milis` nulo en `SesionEntity` y subir `StudyPlanDatabase` a `version = 6` con el *callback* que crea «Estudios varios», verificando el esquema `6.json` con `./gradlew assembleDebug assembleDebugAndroidTest`.
- [x] 3.2 Escribir `Migraciones.MIGRATION_5_6`, con `Migracion6InstrumentalTest.java` (`v5 → v6` con materias y sesiones previas, fila «Estudios varios» única, claves foráneas) verificada con `./gradlew connectedDebugAndroidTest`.

## 4. Presentación (`adapter.in.ui`, `res/`)

- [x] 4.1 Agregar el campo opcional «Meta semanal (h)» a `MateriaFormularioActivity` y la barra «3 / 4 h» a `MateriasAdapter` (`item_materia.xml`), ocultando la edición, el archivado y la eliminación de «Estudios varios», con pruebas de ViewModel y `./gradlew lintDebug` sin errores (`AGENTS.md` §6.2).
- [x] 4.2 Agregar la definición de la meta total de la semana y el acceso al historial de metas desde la tarjeta de progreso de Inicio, con su pantalla de historial y pruebas de ViewModel, verificando `./gradlew testDebugUnitTest lintDebug`.
- [x] 4.3 Agregar el botón flotante extendido «▶ Estudiar ahora» a `contenido_inicio.xml` y el selector de materia compartido (*bottom sheet* con materias, «Estudios varios», «Nueva materia» y técnica) con el diálogo «¿Es tu sesión de …?», con pruebas de ViewModel y `./gradlew lintDebug` sin errores.
- [x] 4.4 Agregar «Cambiar de materia» a `SesionEnCursoActivity` y la pantalla única «¡Sigue así!» con la lista de metas alcanzadas y «Volver al inicio», con pruebas en `SesionEnCursoViewModelTest.java` y `./gradlew lintDebug` sin errores.

## 5. Verificación E2E

- [x] 5.1 Crear `MetasEspressoTest.java`: Estudiar ahora con «Estudios varios», materia nueva en el mismo paso, cambio de Matemáticas a Informática, felicitación única con dos metas, meta por materia en la lista e historial tras cambiar una meta. Ejecutar `./gradlew connectedDebugAndroidTest` con 0 fallos y `openspec validate add-metas-semanales --strict`.
- [x] 5.2 Revisar con `.claude/agents/revisor-java.md` y verificar `./gradlew assembleDebug testDebugUnitTest lintDebug` con 0 errores, 0 `MissingTranslation` y 0 `HardcodedText`.

## 6. Documentación y bitácora

- [x] 6.1 Actualizar el capítulo 3 (`HU-47` y `HU-48` en el Product Backlog y en la épica E6; `RN-16` con el nuevo texto; `RN-32` y `RN-33` en la tabla de reglas), el capítulo 4 (modelo lógico v6, máquina de estados con Nueva → En curso, pantallas nuevas) y los diagramas con `docs/build/diagramas.py` y `docs/build/diagramas_clases.py`, verificando `python docs/build/cotejo_clases.py` y `python .apa/scripts/revisar.py --doc docs/documento`.
- [x] 6.2 Registrar la implementación, las decisiones y las verificaciones en `docs/auditoria-ia/bitacora.md`.

## Workflow follow-up

- Archivar `add-metas-semanales` con `/opsx:archive` tras la revisión humana.
