# Proposal

## Why

El estudiante universitario no tiene una herramienta sencilla que le permita planificar sus horas de estudio por materia, ver ese plan en un calendario, registrar honestamente lo que estudió y lo que no, y medir su avance frente a metas semanales. Este change fija la línea base de requisitos de la versión 0.1.0. Los requisitos salen de la entrevista con el Product Owner del 04/11/2026, para que toda implementación, humana o asistida por IA, se haga contra un contrato verificable.

## What Changes

- 45 historias de usuario (HU-01 a HU-45) en 10 épicas, con sus criterios de aceptación como escenarios GIVEN/WHEN/THEN.
- 30 reglas de negocio (RN-01 a RN-30): entre ellas la máquina de estados de la sesión (RN-07), que conserva el registro de las sesiones no realizadas y reprogramadas, y los eventos de estudio con marca de tiempo (RN-22 a RN-30), que registran salidas de la app, tiempo extra y estudio con la app cerrada.
- 19 requisitos no funcionales (RNF-01 a RNF-19, ISO/IEC 25010:2023) como criterios de calidad de la Definition of Done. Están documentados en `docs/entrega1` y resumidos en `design.md`.
- La aplicación pasa de "sin internet" (suposición de la IA, rechazada) a **offline-first**: todo funciona sin conexión salvo el respaldo y la restauración en Google Drive.
- No se modifica código en `app/`: este change es solo de especificación.

**Fuera de alcance (v0.1.0):** roles adicionales (tutor o docente), servidor o backend propio, sincronización en tiempo real entre dispositivos, iOS y web, integración con Google Calendar, gamificación avanzada.

## Capabilities

### New Capabilities
- `perfil-ajustes`: perfil local, pantalla de inicio, tema, idioma, bloqueo biométrico y widget (HU-01 a HU-05).
- `gestion-materias`: alta, edición, listado y archivado de materias (HU-06 a HU-09).
- `sesiones-estudio`: planificación, registro tardío, máquina de estados, reprogramación con historial y eliminación (RN-07, HU-10 a HU-15).
- `calendario-estudio`: vistas de mes, semana tipo horario y día, estados visibles, creación desde el calendario, filtro y resumen (HU-16 a HU-21).
- `tecnicas-estudio`: cronómetro por marcas de tiempo, Pomodoro, No molestar, eventos de estudio, aviso al salir, estudio con la app cerrada, tiempo extra, cambio y reparto entre sesiones (HU-22 a HU-24, HU-39 a HU-44).
- `metas-semanales`: meta total y metas por materia, progreso y felicitación (HU-25 a HU-28).
- `estadisticas`: totales, comparación semanal, distribución por materia, cumplimiento y métricas de concentración (HU-29 a HU-31, HU-45).
- `reportes`: reporte de periodo y exportación PDF/CSV (HU-32, HU-33).
- `recordatorios`: notificaciones previas a las sesiones (HU-34).
- `respaldo-privacidad`: respaldo en Google Drive con frecuencia configurable y cifrado opcional, restauración y borrado selectivo (HU-35 a HU-38).

### Modified Capabilities
- Ninguna (proyecto nuevo).

## Impact

- Código: ninguno en este change. Los siguientes changes crearán los paquetes `ui`, `viewmodel`, `domain` y `data` en `app/src/main/java/io/github/jhoel0521/studyplan/`.
- Dependencias previstas, cada una con aprobación humana: Room, Lifecycle, WorkManager, MPAndroidChart, androidx.biometric, Credential Manager / Google Sign-In, Google Drive REST API y core library desugaring.
- Permisos previstos: `INTERNET` (solo para el respaldo), `POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY` (No molestar), `SCHEDULE_EXACT_ALARM` si hace falta y `USE_BIOMETRIC`.
- Documentación: `docs/entrega1/StudyPlan_Entrega1.docx`.
