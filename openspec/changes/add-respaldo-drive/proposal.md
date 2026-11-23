# Proposal: Respaldo cifrado y restauración en Google Drive (`add-respaldo-drive`)

## Why
StudyPlan ya implementa la capa local de datos (Room v7), el borrado local (`HU-38`) y la UI base de Ajustes, pero aún no cuenta con respaldo en la nube ni cifrado opcional (`HU-35`, `HU-36`, `HU-37`, `RN-19`, `RN-20`, `RN-21`). Si el estudiante cambia o pierde el teléfono, pierde todo su historial académico.

## What Changes
- **Dependencias aprobadas (`AGENTS.md` §2):** añadir `com.google.android.gms:play-services-auth` (para `AuthorizationClient` con scope `drive.appdata`) y `androidx.work:work-runtime` (`WorkManager` para respaldos periódicos con restricción de red).
- **Permiso `INTERNET` acotado (`AGENTS.md` §2):** declarar `android.permission.INTERNET` únicamente para comunicarse con Google Drive `appDataFolder`.
- **Núcleo puro (`domain` + `application`):**
  - Serialización/deserialización JSON versionada (v1) de `perfil`, `ajustes`, `materias`, `sesiones`, `eventos`, `tramos` y `metas` (`InstantaneaRespaldo`).
  - Cifrado simétrico opcional `PBKDF2WithHmacSHA256` + `AES-256-GCM` en Java estándar (`RN-20`), sin almacenar la contraseña en texto plano.
  - Puertos de salida en `application.port.out.system`: `ClienteGoogleDrive`, `ProgramadorRespaldo`, `CifradorRespaldo`.
  - Casos de uso en `configuracion`: `ConfigurarRespaldo`, `RespaldarAhora`, `RestaurarRespaldo` y ampliación de `BorrarDatos` para soportar `AlcanceBorrado.DRIVE` y `AMBOS` (`RN-21`).
- **Infraestructura (`adapter.out.drive` y `adapter.out.persistence`):**
  - Implementación `ClienteGoogleDriveRest` usando `HttpURLConnection` contra Drive REST API v3 (`spaces=appDataFolder`), evitando dependencias pesadas de `google-api-client`.
  - `WorkerRespaldoDrive` y `ProgramadorRespaldoWorkManager` (`Nunca`, `Diaria`, `Semanal`, `Mensual`, `Solo manual`, con `NetworkType.CONNECTED` y reintento automático).
  - Restauración transaccional en Room (`AlmacenDatosRoom`) preservando integridad referencial.
- **UI (`AjustesActivity` y primer uso `PerfilInicialActivity`):**
  - Vincular/desvincular cuenta Google, elegir frecuencia, activar/cambiar contraseña con advertencia explícita (`RN-20`), pulsar «Respaldar ahora», ver fecha/tamaño del último respaldo, restaurar al iniciar la app o desde Ajustes, y borrar en teléfono / Drive / ambos con doble confirmación (`RN-21`).

## Capabilities
### Modified Capabilities
- `respaldo-privacidad`: concreta los criterios técnicos y de UX de `HU-35`, `HU-36`, `HU-37` y `HU-38` (`appDataFolder`, AES-256-GCM, reintento sin conexión y restauración inicial).

## Impact
- **Paquetes nuevos:** `io.github.jhoel0521.studyplan.adapter.out.drive` (previsto en `AGENTS.md` §6).
- **Dependencias nuevas:** `play-services-auth` y `work-runtime` en `gradle/libs.versions.toml` y `app/build.gradle.kts`.
- **Requisitos externos (Product Owner):** habilitar *Google Drive API* y configurar OAuth Android en Google Cloud Console con el `applicationId` y SHA-1 de debug/release.
