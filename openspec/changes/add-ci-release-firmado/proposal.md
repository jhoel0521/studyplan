# Proposal

## Why

StudyPlan no tiene ninguna automatización: no existe la carpeta `.github`, así que nadie comprueba que cada cambio compile, pase las 384 pruebas JVM y `lint`. Tampoco hay forma de producir un binario de publicación: `app/build.gradle.kts` no declara `signingConfigs` y solo se compila en debug, con la clave de cada máquina. Eso ya causó un problema real: el APK compilado en Linux no se pudo instalar sobre la app del teléfono (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). El Product Owner tiene una cuenta de desarrollador y quiere publicar el proyecto, y para eso hace falta una clave de firma estable y un proceso repetible.

## What Changes

- **Workflow de integración continua** (`.github/workflows/ci.yml`): en cada push a `main` y en cada pull request ejecuta `assembleDebug`, `assembleDebugAndroidTest`, `test` y `lint`, con el JDK del proyecto y caché de Gradle. Sube los informes de pruebas y de `lint` si falla. No usa secretos.
- **Workflow de release** (`.github/workflows/release.yml`): al empujar una etiqueta `vX.Y.Z` compila un **AAB y un APK firmados** con una clave de subida (*upload key*), y los deja como artefactos y en un borrador de GitHub Release (no público).
- **Firma configurable sin romper el build local**: `app/build.gradle.kts` crea `signingConfigs.release` solo si existen las variables de entorno de la clave. Sin ellas, el build local y el CI siguen funcionando como hoy. `versionCode` y `versionName` se toman de propiedades de Gradle con los valores actuales por defecto (`1` y `1.0`); el workflow de release los deriva de la etiqueta.
- **La clave nunca entra al repositorio**: el keystore y sus contraseñas viven solo en secretos de GitHub (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), y `.gitignore` bloquea `*.jks`, `*.keystore` y `keystore.properties`. **La IA no crea la clave ni los secretos: los crea y los carga el Product Owner**, con las instrucciones que deja este change.
- **`gradlew` ejecutable en git** (hoy no lo es: da «Permiso denegado» en Linux).
- **Comprobación de la compilación de release**: `release` ya habilita la optimización (R8) pero nunca se ha compilado; el change la compila una vez y corrige lo que rompa.
- **Documentación**: sección «Compilación, CI y publicación» en `README.md` (crear la clave de subida, copiar los secretos, publicar una versión) y una regla en `AGENTS.md` (nunca versionar claves).
- Este change **no cambia ninguna HU ni RN** (`skip_specs: true`): es infraestructura.

**Fuera de alcance:**
- Subir a Google Play: requiere una cuenta de servicio y que la primera versión de una app nueva se suba a mano en la consola. Queda como un change posterior, solo si el Product Owner lo aprueba.
- Google Cloud y OAuth para Drive, y la épica de respaldo.
- Publicar en Play, crear la ficha de la tienda y aceptar términos: lo hace el Product Owner.
- Correr las pruebas instrumentadas en el CI (necesitan un emulador con KVM): el CI solo las compila.
- Crear claves, secretos o cuentas.

## Capabilities

### New Capabilities
<!-- Ninguna: infraestructura de construcción, sin comportamiento de la app. -->

### Modified Capabilities
<!-- Ninguna (skip_specs). -->

## Impact

- Requisitos: RNF-15 (mantenibilidad) y RNF-16 (cada cambio rastreable); no cambia ninguna HU ni RN.
- Archivos nuevos: `.github/workflows/ci.yml` y `.github/workflows/release.yml`.
- Archivos modificados: `app/build.gradle.kts`, `.gitignore`, `README.md`, `AGENTS.md`, el modo de `gradlew` y la bitácora.
- Sin dependencias nuevas en la app. Las acciones de GitHub (`checkout`, `setup-java`, `setup-gradle`, `upload-artifact`) son de terceros y sus versiones se verifican a mano.
- `applicationId`, `namespace`, `minSdk` y `targetSdk` no cambian.
- Riesgo principal: la primera compilación de release con R8 puede fallar o dejar fuera clases por reflexión (Room, MPAndroidChart); y una clave de subida perdida complica publicar.
