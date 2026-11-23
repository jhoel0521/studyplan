# Design: CI y release firmado

## Context

Motivación y alcance en `proposal.md`. No hay requisitos de HU: es infraestructura (RNF-15, RNF-16).

**Estado actual** (leído en el repositorio, no supuesto):
- No existe `.github/`. El remoto es `github.com/jhoel0521/studyplan`.
- `app/build.gradle.kts`: AGP 9.4.1, Gradle 9.6, `compileSdk` 37, Java 11 como nivel de fuente, `versionCode = 1` y `versionName = "1.0"` fijos, sin `signingConfigs`. El tipo `release` tiene `optimization { enable = true; packageScope = ... }` y nunca se ha compilado.
- La compuerta de calidad vigente es `./gradlew assembleDebug assembleDebugAndroidTest test lint` (AGENTS.md §6). En esta máquina hay que definir `ANDROID_HOME` y `gradlew` no tiene el bit de ejecución.
- `.gitignore` no excluye llaves.
- Las pruebas instrumentadas (41 clases) necesitan emulador; en el CI solo se compilan.

**Restricciones:** sin dependencias nuevas en la app; las acciones de GitHub de terceros se fijan por versión y se marcan para verificación humana (AGENTS.md §5); la IA no maneja secretos.

## Goals / Non-Goals

**Goals:**
- Cada push y pull request se comprueba con la misma compuerta que se usa a mano.
- Una etiqueta `vX.Y.Z` produce un AAB y un APK firmados, reproducibles.
- El build local sin clave sigue funcionando exactamente igual.
- Ningún secreto en el repositorio ni en los registros.

**Non-Goals:**
- Subir a Play, correr instrumentadas en el CI y cualquier configuración de Google Cloud.

## Decisions

**1. Dos workflows separados.** `ci.yml` (sin secretos, para todo push y pull request, incluidos los de forks) y `release.yml` (con secretos, solo por etiqueta). Separarlos evita que un pull request ajeno alcance los secretos. *Alternativa descartada:* un solo workflow con condiciones: mezcla permisos y es más fácil exponer un secreto por error.

**2. CI: la misma compuerta que se usa a mano**, `./gradlew assembleDebug assembleDebugAndroidTest test lint`, en `ubuntu-latest` con JDK 21 (AGP 9 pide 17 o más y la máquina del autor usa 21), caché de Gradle con `gradle/actions/setup-gradle` y cancelación de ejecuciones anteriores de la misma rama (`concurrency`). Si falla, sube `app/build/reports` y `app/build/test-results` como artefacto. El SDK de Android ya viene en el runner; se aceptan las licencias con `sdkmanager` para que AGP pueda bajar `android-37`.

**3. Firma con variables de entorno, opcional.** En `app/build.gradle.kts`, `signingConfigs.create("release")` se declara solo si existen `STUDYPLAN_KEYSTORE_PATH`, `STUDYPLAN_KEYSTORE_PASSWORD`, `STUDYPLAN_KEY_ALIAS` y `STUDYPLAN_KEY_PASSWORD`, y entonces `release` la usa. Sin ellas no se asigna ninguna: el build local y el CI no cambian. *Alternativa descartada:* un `keystore.properties` local, que invita a dejar la clave en el disco del proyecto; se mantiene solo la ruta por entorno.

**3b. El workflow de release crea el keystore en un archivo temporal** (`$RUNNER_TEMP`) desde `KEYSTORE_BASE64`, lo apunta con la variable de ruta y lo borra al terminar. Los secretos se pasan por `env` de un paso, nunca por línea de comandos, para que no aparezcan en el registro.

**4. Versionado derivado de la etiqueta.** El workflow valida `^v[0-9]+\.[0-9]+\.[0-9]+$`, toma `versionName = X.Y.Z` y calcula `versionCode = X*1000000 + Y*1000 + Z` (monótono y determinista; admite hasta 999 de menor y parche). Se pasan como `-PversionName` y `-PversionCode`; `build.gradle.kts` los lee con `providers.gradleProperty` y conserva `1` y `"1.0"` por defecto. Play exige que el `versionCode` solo suba, y esta fórmula lo garantiza mientras las etiquetas suban. *Alternativa descartada:* `github.run_number`, que no se puede reproducir localmente ni refleja la versión.

**5. La etiqueta debe estar en `main`.** El workflow comprueba que la confirmación de la etiqueta es descendiente de `origin/main` antes de firmar, para que una rama sin revisar no produzca un binario firmado.

**6. Resultado del release: artefactos y borrador de GitHub Release.** Se compilan `bundleRelease` (AAB) y `assembleRelease` (APK); se suben como artefactos del workflow y se adjuntan a un **borrador** de GitHub Release creado con la CLI `gh` (ya instalada en el runner), sin acción de terceros. El borrador no es público: el Product Owner decide si lo publica. Antes de firmar, el workflow corre `test` y `lint`.

**7. Primera compilación de release y R8.** `release` habilita la optimización pero nunca se ha construido: Room, MPAndroidChart y `colorpickerview` pueden necesitar reglas. La primera tarea compila `bundleRelease` local sin clave, lee los avisos de R8 y añade `proguard-rules.pro` solo con lo que el compilador pida. La prueba de que el binario optimizado funciona (abrir la app y sus pantallas principales) es **manual, en el teléfono del Product Owner**, porque no hay emulador aquí.

**8. Control de acceso del repositorio.** `permissions: contents: read` en el CI y `contents: write` solo en el job de release. Los secretos solo existen en el entorno del job de release.

**9. Nunca llaves en git.** `.gitignore` bloquea `*.jks`, `*.keystore`, `keystore.properties` y `upload-key*`. `README.md` explica cómo crear la clave con `keytool` **en la máquina del Product Owner**, guardar una copia fuera del repositorio y cargar los cuatro secretos. `AGENTS.md` gana una regla: ninguna clave, contraseña ni token se escribe, versiona ni imprime.

**10. `gradlew` ejecutable en git** con `git update-index --chmod=+x gradlew`, para que el CI y Linux no necesiten `bash ./gradlew`.

## Risks / Trade-offs

- [R8 elimina o renombra clases usadas por reflexión y el release falla solo en ejecución] → compilar `bundleRelease`, añadir reglas mínimas y exigir una prueba manual en el teléfono antes de publicar.
- [Perder la clave de subida] → Play permite restablecerla, pero con trámite; se documenta guardar una copia fuera del repositorio y la contraseña en un gestor.
- [Un secreto en un registro] → pasarlos solo por `env`, no hacer `echo` de ellos, y usar los enmascarados de GitHub.
- [El runner no trae la plataforma `android-37`] → aceptar licencias para que AGP la descargue; la primera ejecución es más lenta.
- [Las versiones de las acciones de terceros] → se fijan a una versión mayor y se marcan para verificación manual; si una cambia, el CI lo avisa.
- [El CI no corre las instrumentadas] → quedan en la compuerta manual del autor (AGENTS.md §6) hasta que haya un emulador estable en el CI.
- [Una etiqueta equivocada produce una versión mal numerada] → el workflow valida el formato antes de compilar.

## Migration Plan

Sin migración de datos. Orden: 1) configuración de build y `.gitignore`, 2) CI, 3) release, 4) el Product Owner crea la clave y los secretos, 5) primera etiqueta de prueba en una rama o repositorio de pruebas. Revertir es borrar `.github/workflows` y volver a la versión anterior de `app/build.gradle.kts`; la app no cambia.

## Open Questions

- ¿Quieres que el CI se ejecute también en ramas distintas de `main`? Se propone solo `main` y los pull requests.
