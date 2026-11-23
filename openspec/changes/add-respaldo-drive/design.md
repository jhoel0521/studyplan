# Design: Respaldo en Google Drive (`add-respaldo-drive`)

## Architecture Overview
Siguiendo la arquitectura hexagonal (`AGENTS.md` §2 y §6):
- **Núcleo (`domain` + `application`) — 100 % Java puro:**
  - `domain/readmodel/InstantaneaRespaldo`: snapshot inmutable con versión de formato (`versionFormato = 1`), `fechaExportacionUtcMilis`, `perfil`, `ajustes`, `materias`, `sesiones`, `eventos`, `tramos`, `metas`.
  - `domain/enumeration/FrecuenciaRespaldo`: `NUNCA`, `DIARIA`, `SEMANAL`, `MENSUAL`, `SOLO_MANUAL` (`RN-19`).
  - `application/port/out/system/CifradorRespaldo`: cifra/descifra `byte[]` con contraseña opcional (`AES-256-GCM`).
  - `application/port/out/system/ClienteGoogleDrive`: `subirRespaldo(byte[])`, `descargarRespaldo()`, `eliminarRespaldo()`, `obtenerMetadatosUltimoRespaldo()`.
  - `application/port/out/system/ProgramadorRespaldo`: `programar(FrecuenciaRespaldo)`, `cancelar()`.
  - `application/port/in/configuracion/`: `ConfigurarRespaldo`, `RespaldarAhora`, `RestaurarRespaldo`, más ampliación de `BorrarDatosService` para `DRIVE` y `AMBOS`.

- **Infraestructura (`adapter.out.drive`, `adapter.out.security`, `adapter.out.persistence`):**
  - `adapter/out/security/CifradorRespaldoAesGcm`: usa `javax.crypto` (`PBKDF2WithHmacSHA256`, 120 000 iteraciones, salt aleatorio de 16 bytes, IV de 12 bytes, `AES/GCM/NoPadding` tag 128 bits). Cabecera binaria propia `SPBK1` + bandera cifrado/plano (`0`/`1`) + salt + IV + ciphertext.
  - `adapter/out/drive/AutenticadorGoogleDrive`: usa `Identity.getAuthorizationClient(context)` (`com.google.android.gms:play-services-auth`) con `Scope("https://www.googleapis.com/auth/drive.appdata")` para obtener el `accessToken` sin usar la API deprecada `GoogleSignIn`.
  - `adapter/out/drive/ClienteGoogleDriveRest`: llamadas HTTPS directas con `HttpURLConnection` a `https://www.googleapis.com/drive/v3/files` y `https://www.googleapis.com/upload/drive/v3/files` filtrando `spaces=appDataFolder` y nombre fijo `studyplan_backup.spbk` (reemplazo/actualización atómica sobre un único archivo).
  - `adapter/out/drive/WorkerRespaldoDrive` + `ProgramadorRespaldoWorkManager`: `PeriodicWorkRequest` (1 día / 7 días / 30 días) con `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`.

- **Persistencia local (`AlmacenDatosRoom`):**
  - Añadir método transaccional `restaurarTodo(InstantaneaRespaldo)` en `AlmacenDatosRoom` / `AlmacenDatos` (`runInTransaction`), limpiando tablas actuales e insertando en orden referencial (`perfil`, `ajustes`, `materia`, `sesion_estudio`, `evento_estudio`, `tramo_diario`, `meta_semanal`).
  - Guardar metadatos del último respaldo (fecha UTC, tamaño en bytes, correo vinculado, indicador de contraseña configurada en `EncryptedSharedPreferences` / `KeyStore` o hash/uso en memoria) sin guardar la contraseña en claro.

## Correcciones de la auditoría (2026-11-23)
- La base actual es **Room v8** (no v7). `InstantaneaRespaldo` lleva `versionFormato` propio (1) y `versionEsquema` informativa; un respaldo con `versionFormato` mayor se rechaza sin tocar datos.
- Al restaurar se **normalizan** los ajustes ligados al dispositivo (bloqueo biométrico/PIN, No molestar) y se descartan sesiones «En curso»; después se reprograman recordatorios y avisos.
- El respaldo nace **apagado** (`FrecuenciaRespaldo.NUNCA`, sin cuenta). Lo activa el estudiante.
- En segundo plano `AuthorizationClient` no puede mostrar consentimiento: si exige interacción, el worker termina con fallo registrado (`Result.failure`) y Ajustes ofrece «Volver a autorizar»; `Result.retry()` solo se usa para errores de red.
- Con la app en estado «Prueba» de Google Cloud la autorización puede caducar a los 7 días: el fallo visible cubre ese caso.
- El respaldo automático de Android (`allowBackup`) queda **fuera de este change** por decisión del Product Owner.
- Decisión aprobada por el Product Owner: contraseña de respaldo protegida con Android KeyStore para el modo automático (se borra al quitar la contraseña, al desvincular o al borrar datos).

## Decisión de implementación: instantánea genérica por tablas
`InstantaneaRespaldo` guarda cada tabla como lista de filas (mapa columna → valor) en vez de siete clases tipadas. Así el núcleo no duplica las entidades de Room y una columna nueva no rompe el formato. `SerializadorRespaldo` (JSON a mano, Java puro, en `domain/service`) reemplaza a `SerializadorRespaldoJson` del adaptador, de modo que se prueba en la JVM sin `org.json`. Los resultados de las operaciones son el enum `ResultadoRespaldo` (sin excepciones).

## Key Decisions & Tradeoffs
- **Drive REST v3 con `HttpURLConnection` vs. `google-api-services-drive`:** `HttpURLConnection` + `org.json` (incluido en Android SDK) evita conflictos de dependencias transitivas (Guava, Apache HTTP) y mantiene el APK liviano y compatible con R8.
- **Contraseña opcional en respaldo automático:** si el usuario configura respaldo automático con contraseña (`HU-36`), para que `WorkManager` pueda cifrar en segundo plano sin pedir contraseña cada noche, la clave derivada o contraseña se resguarda cifrada con **Android KeyStore** (`AES-GCM` con alias local en `adapter.out.security`), borrándose al desactivar contraseña o borrar datos. En un teléfono nuevo al restaurar, siempre se solicita la contraseña al usuario.
