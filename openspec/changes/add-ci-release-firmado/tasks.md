# Tasks

**Reglas**
- Rutas relativas a la raíz del repositorio. Sin commits ni `push` salvo pedido explícito (`AGENTS.md` §4); si se piden, un commit por tarea (`ci: ...`, `build: ...` o `docs: ...`), sin coautor.
- **La IA no crea la clave, los secretos ni las cuentas ni sube nada a GitHub:** el Product Owner lo hace con las instrucciones del `README.md`. La IA no lee ni imprime ningún secreto.
- En esta máquina, `ANDROID_HOME=$HOME/Android/Sdk` y, hasta la tarea 1.3, `bash ./gradlew`.
- Compuerta de las tareas de build: `./gradlew assembleDebug assembleDebugAndroidTest test lint` sigue en verde sin ninguna variable de firma.

## 1. Configuración de build (RNF-15)

- [x] 1.1 Modificar `app/build.gradle.kts`: `versionCode` y `versionName` desde `providers.gradleProperty("versionCode")` y `("versionName")` con `1` y `"1.0"` por defecto, y `signingConfigs.create("release")` solo si existen las cuatro variables `STUDYPLAN_KEYSTORE_*`/`STUDYPLAN_KEY_*`, asignada al tipo `release`. Verificación: con las variables sin definir, `./gradlew assembleDebug assembleDebugAndroidTest test lint` pasa y `assembleRelease` produce un APK sin firmar; con una clave de prueba **temporal creada y borrada dentro de la sesión** (no la del Product Owner), `assembleRelease` produce un APK que `apksigner verify` acepta.
- [x] 1.2 Modificar `.gitignore` para bloquear `*.jks`, `*.keystore`, `keystore.properties` y `upload-key*`. Verificación: `git check-ignore -v` confirma cada patrón con un archivo temporal de ejemplo.
- [x] 1.3 Dar el bit de ejecución a `gradlew` en git (`git update-index --chmod=+x gradlew`). Verificación: `git ls-files --stage gradlew` muestra el modo `100755` y `./gradlew --version` funciona sin `bash`.
- [x] 1.4 Compilar la release con R8: `./gradlew bundleRelease assembleRelease` sin clave; leer los avisos y errores de R8 y, solo si el compilador los pide, crear `app/proguard-rules.pro` con reglas mínimas (Room, MPAndroidChart, `colorpickerview`) y referenciarlo desde `release`. Verificación: `bundleRelease` termina sin errores, el APK de release se puede inspeccionar con `apkanalyzer` y las reglas añadidas, si hay, quedan comentadas con su motivo.

## 2. Workflows de GitHub Actions (RNF-15, RNF-16)

- [x] 2.1 Crear `.github/workflows/ci.yml`: disparadores `push` a `main` y `pull_request`; `permissions: contents: read`; `concurrency` que cancela la ejecución anterior de la rama; JDK 21, `gradle/actions/setup-gradle` y aceptación de licencias del SDK; paso `./gradlew assembleDebug assembleDebugAndroidTest test lint`; subida de `app/build/reports` y `app/build/test-results` si falla. Verificación: el YAML se analiza sin errores (`python3 -c "import yaml; yaml.safe_load(open(...))"`), un revisor de sintaxis como `actionlint` (si está disponible) no da errores y las versiones de cada acción están anotadas en la bitácora para verificación manual.
- [x] 2.2 Crear `.github/workflows/release.yml`: disparador `push` de etiquetas `v*.*.*`; validar el formato `^v[0-9]+\.[0-9]+\.[0-9]+$` y que la confirmación está en `main`; derivar `versionName` y `versionCode = X*1000000 + Y*1000 + Z`; ejecutar `test` y `lint`; decodificar `KEYSTORE_BASE64` a un archivo temporal pasando los secretos solo por `env`; `bundleRelease` y `assembleRelease`; borrar el keystore al terminar; subir artefactos y crear un **borrador** de GitHub Release con `gh`; `permissions: contents: write` solo en este job. Verificación: el YAML se analiza sin errores, ningún paso imprime un secreto (revisión línea a línea) y se simula localmente el cálculo de `versionCode` con `1.2.3 -> 1002003`.

## 3. Documentación (RNF-16)

- [x] 3.1 Añadir a `README.md` la sección «Compilación, CI y publicación»: qué hace cada workflow; cómo crear la clave de subida con `keytool` en la máquina del Product Owner; cómo guardar una copia fuera del repositorio; cómo cargar los cuatro secretos en GitHub; cómo publicar una versión con una etiqueta; y qué falta para subir a Play (primera subida manual y cuenta de servicio). Añadir a `AGENTS.md` la regla de no versionar ni imprimir claves ni contraseñas. Verificación: las instrucciones se siguen paso a paso sin necesitar nada que no esté escrito, y `git diff` de `AGENTS.md` solo añade esa regla.
- [x] 3.2 Registrar la sesión en `docs/auditoria-ia/bitacora.md`: solicitud, resultado, decisión humana, versiones de acciones por verificar y errores de la IA. Verificación: filas añadidas sin reescribir las anteriores.

## 4. Puesta en marcha (la hace el Product Owner)

- [ ] 4.1 El Product Owner crea la clave de subida y los cuatro secretos en GitHub, sube el change y comprueba que el CI queda en verde en un pull request de prueba. Verificación: captura o enlace de la ejecución del CI en verde, anotado en la bitácora.
- [ ] 4.2 El Product Owner empuja una etiqueta de prueba (por ejemplo `v0.1.0`) y comprueba que el release genera un AAB y un APK firmados (`apksigner verify` y `jarsigner`/`bundletool` sobre el AAB), y los instala en su teléfono con `adb install -r` si la firma es la de su instalación. Verificación: resultado anotado en la bitácora; las fallas se corrigen antes de archivar.

## Workflow follow-up

- Archivar el change tras la puesta en marcha (`/opsx:archive`).
- Un change posterior, solo si el Product Owner lo aprueba: subida a la pista interna de Google Play con una cuenta de servicio (la primera versión se sube a mano en la consola).
