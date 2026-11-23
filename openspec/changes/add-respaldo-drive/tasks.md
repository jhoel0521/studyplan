# Tasks

**Reglas**
- Antes de implementar el Bloque 0, el Product Owner confirma las dependencias nuevas (`play-services-auth` y `work-runtime`) y configura Google Cloud Console (`drive.googleapis.com` + OAuth Android con SHA-1).
- Compuerta por bloque: `./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` en verde (incluyendo `ArquitecturaTest` y `EstructuraPaquetesTest`).
- Cero `android.*` / `androidx.*` en `domain` y `application`.

## 0. Dependencias y manifiesto

- [x] 0.1 Añadir `play-services-auth` y `androidx.work:work-runtime` en `gradle/libs.versions.toml` y `app/build.gradle.kts`, y declarar `<uses-permission android:name="android.permission.INTERNET" />` en `app/src/main/AndroidManifest.xml` (exclusivo para Google Drive `appDataFolder`, `AGENTS.md` §2). Verificación: `./gradlew assembleDebug` en verde.

## 1. Núcleo puro (`domain` + `application`)

- [x] 1.1 Crear `FrecuenciaRespaldo` (`domain/enumeration`), `InstantaneaRespaldo` y `MetadatosRespaldo` (`domain/readmodel`), y `SerializadorRespaldo` en `domain/service` (o interfaz en `application`) con pruebas unitarias de ida y vuelta para todas las entidades (`perfil`, `ajustes`, `materia`, `sesion_estudio`, `evento_estudio`, `tramo_diario`, `meta_semanal`). Verificación: `./gradlew testDebugUnitTest` (`domain.*` y `ArquitecturaTest`).
- [x] 1.2 Crear puertos de salida en `application/port/out/system`: `CifradorRespaldo`, `ClienteGoogleDrive`, `ProgramadorRespaldo`, `AlmacenCredencialRespaldo`, y ampliar `AlmacenDatos` con `restaurarTodo(...)`. Crear puertos de entrada y servicios en `application/*/configuracion`: `ConfigurarRespaldo`, `RespaldarAhora`, `RestaurarRespaldo` y soporte `DRIVE`/`AMBOS` en `BorrarDatosService`. Verificación: pruebas unitarias con fakes para respaldo normal, cifrado, contraseña incorrecta, sin conexión y borrado selectivo (`HU-35` a `HU-38`).

## 2. Infraestructura (`adapter.out.security`, `adapter.out.drive`, `adapter.out.persistence`)

- [x] 2.1 Implementar `CifradorRespaldoAesGcm` (`PBKDF2WithHmacSHA256` + `AES/GCM/NoPadding`) y `AlmacenCredencialRespaldoKeystore` (cifrando la contraseña local en Android KeyStore para respaldos automáticos) en `adapter.out.security` + pruebas unitarias/instrumentadas de cifrado y descifrado con contraseña correcta e incorrecta.
- [x] 2.2 Implementar `SerializadorRespaldoJson` (usando `org.json` / adaptador sin contaminar `domain`), `AutenticadorGoogleDrive` (`Identity.getAuthorizationClient`), `ClienteGoogleDriveRest` (`appDataFolder` vía `HttpURLConnection`), `WorkerRespaldoDrive` y `ProgramadorRespaldoWorkManager` en `adapter.out.drive`, y `restaurarTodo(...)` transaccional en `AlmacenDatosRoom`. Verificación: pruebas instrumentadas de `AlmacenDatosRoom.restaurarTodo` y `WorkerRespaldoDrive` con cliente fake.

## 3. Presentación XML (`Ajustes` y primer uso)

- [x] 3.1 Actualizar `AjustesViewModel`, `AjustesActivity` y `contenido_ajustes.xml` (sección Copia de seguridad: vincular cuenta Google, frecuencia `Nunca`..`Solo manual`, contraseña opcional con diálogo de advertencia `RN-20`, botón «Respaldar ahora», botón «Restaurar», fecha/tamaño del último respaldo, y selector de alcance en «Borrar mi información» con doble confirmación `RN-21`), cumpliendo `AGENTS.md` §6.2 y `es`/`en`.
- [x] 3.2 Añadir acceso a «Restaurar desde Google Drive» en `PerfilInicialActivity` (`activity_perfil_inicial.xml` / `layout-w600dp/`) para teléfonos nuevos (`HU-37`). Verificación: `./gradlew assembleDebug testDebugUnitTest lintDebug` con 0 errores y 0 `MissingTranslation` / `HardcodedText`.

## 3b. Casos añadidos por la auditoría

- [x] 3b.1 Cubrir con pruebas del núcleo: apagado por defecto, respaldo fallido visible, desvincular, restaurar sin respaldo, respaldo de versión más nueva y normalización de ajustes/sesiones tras restaurar con reprogramación de recordatorios (`HU-35`, `HU-37`).

## 4. Verificación E2E, documentación y prueba real

- [x] 4.1 Añadir pruebas Espresso para la configuración de respaldo, advertencia de contraseña y doble confirmación de borrado selectivo en `Ajustes` y arranque en `PerfilInicialActivity` (con `ClienteGoogleDrive` fake en pruebas o flujo completo), y ejecutar `./gradlew connectedDebugAndroidTest` en emulador.
- [x] 4.2 Prueba manual del Product Owner en dispositivo/emulador con cuenta Google real (vincular Drive, respaldar con contraseña, borrar local, restaurar y comprobar historial), más actualización de capítulos 3–4, diagramas (`adapter.out.drive`) y `docs/auditoria-ia/bitacora.md`. **Hecho el 2026-11-23:** el Product Owner borró sus datos en su teléfono y los restauró desde Google Drive; capítulos 3 a 5, diagramas y bitácora actualizados.
