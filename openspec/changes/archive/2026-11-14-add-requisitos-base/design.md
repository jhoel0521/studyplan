# Design

## Context

El proyecto parte de la plantilla *Empty Views Activity* de Android Studio: Java, minSdk 24, targetSdk 37, Gradle Kotlin DSL y version catalog. Por ahora solo existe `MainActivity`. Las restricciones de stack están en `openspec/config.yaml` y `AGENTS.md`. La motivación del change está en proposal.md (Why). Las reglas de negocio y los RNF completos están en `docs/build/requisitos.py`.

## Goals / Non-Goals

**Goals:**
- Fijar la arquitectura, el modelo de dominio y la máquina de estados que deben respetar todos los changes de implementación.
- Fijar los RNF como criterios de calidad verificables.

**Non-Goals:**
- Diccionario de datos y modelo lógico de la BD (Entrega 2, cap. 4.2).
- Diseño de alta fidelidad. El mockup de referencia existe y los wireframes de baja fidelidad están en `docs/diagramas/`.

## Decisions

1. **Java nativo frente a Kotlin o Flutter.** Lo exige la materia y es lo que se enseña en clase. Java mantiene soporte completo en el SDK y en Jetpack.
2. **MVVM con Jetpack (ViewModel y LiveData).** Separa la UI de la lógica, sobrevive a la rotación (RNF-10) y permite probar los ViewModel con JUnit (RNF-15).
3. **Room sobre SQLite, offline-first.** Los datos son relacionales y deben funcionar sin conexión (RNF-09). Room valida el SQL en compilación (RNF-14).
4. **Patrón State para `SesionEstudio` (RN-07).** Cada estado es una clase que implementa `EstadoSesion` y expone solo las acciones que admite (`iniciar`, `completar`, `reprogramar`, `registrarTardio`, `vencer`), que devuelven el siguiente estado. Se eligió frente a un `switch` sobre un enum porque cada estado se prueba de forma aislada y agregar un estado no modifica los demás. En Room se persiste como texto, el nombre del estado.
5. **Reprogramar crea una sesión nueva (RN-09).** La original pasa a `REPROGRAMADA` y la nueva guarda `sesionOrigenId`. Así se conserva el registro de incumplimiento que pidió el Product Owner.
6. **Paso automático a No realizada (RN-08).** Un trabajo diario de WorkManager, más una verificación al abrir la app, pasa a `NO_REALIZADA` las sesiones Pendientes de días anteriores. Esto es idempotente.
7. **Respaldo tipo WhatsApp en Google Drive.** Se usa la carpeta oculta de la app (`appDataFolder`, alcance `drive.appdata`) con Credential Manager / Google Sign-In. El trabajo es periódico con WorkManager, con restricción de red y la frecuencia de RN-19. Se eligió frente al Auto Backup de Android porque el Product Owner quiere elegir la frecuencia y respaldar manualmente.
8. **Cifrado opcional AES-256-GCM.** La clave se deriva de la contraseña con PBKDF2 (sal aleatoria guardada junto al archivo). No se usa Android Keystore, porque su clave no sale del dispositivo y no permitiría restaurar en un teléfono nuevo.
9. **Calendario con vistas propias (mes, semana y día).** Vista Mes con `RecyclerView` en cuadrícula; Semana y Día con una vista personalizada (`Canvas`) de franjas horarias. Es Java puro y no agrega dependencias.
10. **No molestar con `NotificationManager.setInterruptionFilter`.** Requiere acceso a la política de notificaciones. Se guarda el filtro previo para restaurarlo (RN-18).
11. **Widget con `AppWidgetProvider` y `RemoteViews`.** Se descarta Glance porque está basado en Kotlin y Compose.
12. **Bloqueo con `BiometricPrompt`** (`BIOMETRIC_WEAK | DEVICE_CREDENTIAL`) al volver a primer plano.
13. **Semana ISO (lunes a domingo, RN-13).** Fechas con `java.time` mediante desugaring.
14. **Eventos de estudio con marca de tiempo (RN-22).** La tabla `evento_estudio` (sesionId, tipo, instante, nota) es la fuente del tiempo de una sesión: la duración se calcula sumando los tramos entre eventos. No hay servicio contando segundos; el cronómetro en pantalla es un `Handler` que redibuja solo mientras la Activity está visible. Esto cumple RNF-03 (batería) y RNF-10 (sobrevive al cierre forzado). Se descartó un *foreground service* permanente.
15. **Detección de salida y regreso (RN-23).** `ProcessLifecycleOwner` (lifecycle-process) emite ON_STOP/ON_START de la app; con sesión En curso se registra el evento y se publica o retira la notificación «Tu sesión sigue en curso».
16. **Declaración al volver (RN-24, RN-29).** Al abrir la app con una sesión En curso cuyo último evento es «salida», se muestra el diálogo de declaración antes de cualquier otra pantalla. El cierre automático por plazo vencido lo hace el mismo trabajo diario de RN-08.
17. **Avisos programados de fin y de siguiente sesión (RN-25, RN-27)** con AlarmManager o WorkManager, cancelados al detener la sesión.

### Requisitos no funcionales (Definition of Done)
| ID | Característica ISO 25010 | Criterio resumido |
|---|---|---|
| RNF-01 a 03 | Eficiencia de desempeño | arranque ≤ 2 s; calendario ≤ 300 ms; APK ≤ 15 MB |
| RNF-04 a 08 | Capacidad de interacción | ≤ 3 toques; SUS ≥ 70; accesibilidad; tema oscuro; es/en |
| RNF-09 a 11 | Fiabilidad | offline-first; sin pérdida de datos; respaldo íntegro |
| RNF-12 a 14 | Seguridad | AES-256-GCM opcional; drive.appdata; biometría; borrado selectivo |
| RNF-15/16 | Mantenibilidad | patrón State probado; cobertura ≥ 70 % en domain; trazabilidad |
| RNF-17/18 | Compatibilidad | API 24+; permisos con degradación controlada |
| RNF-19 | Flexibilidad | pantallas de 5" a 7"; widget |

## Risks / Trade-offs

- [Riesgo] La IA genera Kotlin o Compose por costumbre → Mitigación: restricción en `config.yaml` y `AGENTS.md` y revisión con `revisor-java`.
- [Riesgo] Google exige verificación de la app para alcances de Drive → Mitigación: `drive.appdata` es un alcance no sensible. Hay que registrar la pantalla de consentimiento OAuth antes de publicar.
- [Riesgo] El usuario olvida la contraseña del respaldo → Mitigación: advertencia explícita (HU-36). La contraseña es opcional.
- [Riesgo] El trabajo diario de RN-08 no corre si el sistema mata la app → Mitigación: la verificación también se hace al abrir la app y es idempotente.
- [Riesgo] La vista semanal es poco legible en pantallas de 5" → Mitigación: desplazamiento horizontal y prueba en emulador pequeño (RNF-19).
- [Trade-off] Un solo usuario local, sin sincronización en tiempo real; el respaldo cubre el cambio de teléfono.

## Open Questions

- ~~Si el docente acepta cambiar "3.3.1 Casos de Uso" por "User Story Mapping".~~ Resuelto por el estudiante (13-nov-2026): la sección 3.3.1 se renombra «Historias de Usuario»; no se usan casos de uso. No afecta a las specs.
- RN-12 (solo se eliminan sesiones Pendientes) se derivó de RN-09 y el Product Owner debe confirmarla.
