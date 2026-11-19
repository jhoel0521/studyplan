# Spec Delta: `recordatorios`

## MODIFIED Requirements

### Requirement: HU-34 Recibir recordatorio de sesión
Como estudiante, quiero recibir una notificación configurable (Sin recordatorio, 5, 15 o 30 minutos) antes de cada sesión planificada para no olvidar mis sesiones de estudio. El sistema SHALL cumplir los escenarios siguientes (prioridad Must, 3 pts).

#### Scenario: Notificación
- **GIVEN** que existe una sesión Pendiente de 10:00 a 11:30 con aviso de 15 minutos
- **WHEN** se alcanza el instante de aviso (`09:45`) con permiso de notificaciones vigente
- **THEN** el sistema muestra una notificación con el nombre vigente de la materia, el horario local (`10:00 – 11:30`, añadiendo la zona original si difiere de la zona actual) y la acción directa «Iniciar»

#### Scenario: Permiso
- **GIVEN** que el teléfono usa Android 13 (API 33) o superior sin permiso de notificaciones concedido
- **WHEN** el estudiante activa una antelación de 5, 15 o 30 minutos en Ajustes o en el selector o banner del formulario de sesión
- **THEN** el sistema explica antes por qué necesita el permiso y lo solicita; si el estudiante lo niega, guarda igualmente la configuración o sesión sin bloquear el guardado y mantiene visible en pantalla un aviso persistente con acceso directo a los ajustes de notificaciones del sistema

#### Scenario: Permiso de alarmas exactas en Android 12 o superior
- **GIVEN** que el teléfono usa Android 12 (API 31) o superior sin permiso de alarmas exactas concedido
- **WHEN** el estudiante activa recordatorios y consulta Ajustes o pulsa el aviso correspondiente
- **THEN** el sistema explica su utilidad ante restricciones de ahorro de batería y ofrece ir a los ajustes de alarmas del sistema; si permanece denegado, la app sigue operando y programa los avisos mediante respaldo inexacto del sistema (`setAndAllowWhileIdle`)

#### Scenario: Cancelación
- **GIVEN** que la sesión tuvo una transición fuera de Pendiente (reprogramada, iniciada manualmente, iniciada o completada por invasión/reparto de otra sesión, o vencida como No realizada), fue eliminada o el estudiante ejecutó el borrado de datos locales
- **WHEN** llega el instante del aviso original o se consulta la bandeja del sistema
- **THEN** el sistema cancela la alarma y retira la notificación asociada de modo que no se muestra el aviso original

#### Scenario: Ajuste predeterminado y preselección al planificar
- **GIVEN** que Ajustes guarda la antelación predeterminada en `15 minutos antes` (valor inicial, configurable entre `Sin recordatorio`, `5`, `15` y `30` minutos sin alterar sesiones ya creadas)
- **WHEN** el estudiante abre el formulario para planificar una nueva sesión
- **THEN** el formulario preselecciona la antelación predeterminada y permite cambiarla a `Sin recordatorio`, `5`, `15` o `30` minutos antes de guardar

#### Scenario: Conservación al reprogramar y exclusión en sesiones pasadas o sin plan
- **GIVEN** que el estudiante reprograma una sesión existente, cambia el formulario al modo de sesión pasada (`HU-11`) o inicia una sesión sin horario planificado «Estudiar ahora» (`HU-47`)
- **WHEN** se abre el formulario o se registra la sesión
- **THEN** al reprogramar se preselecciona la antelación heredada de la sesión origen; en el modo de sesión pasada el selector queda oculto y tanto las sesiones pasadas como las sesiones «Estudiar ahora» se guardan siempre con `Sin recordatorio`

#### Scenario: Visualización de la antelación en el detalle
- **GIVEN** que existe una sesión con horario planificado
- **WHEN** el estudiante abre la pantalla de detalle de esa sesión
- **THEN** el sistema muestra la antelación de recordatorio configurada (o `Sin recordatorio`) junto a los datos de la sesión, y oculta ese dato en sesiones pasadas o sin plan

#### Scenario: Apertura de detalle o inicio directo desde la notificación
- **GIVEN** que se muestra la notificación de recordatorio de una sesión Pendiente
- **WHEN** el estudiante toca el cuerpo de la notificación o pulsa la acción «Iniciar» (aun si la sesión comienza tras la medianoche dentro de su ventana de aviso)
- **THEN** tocar el cuerpo abre el detalle de la sesión para elegir técnica y No molestar, y pulsar «Iniciar» abre la pantalla de sesión en curso iniciando directamente la sesión (`Pendiente → En curso`) con técnica Libre y No molestar heredado (o muestra recuperación/aviso si ya existe otra sesión activa), sin limpiar la tarea activa de la app

#### Scenario: Privacidad bajo bloqueo activo
- **GIVEN** que el bloqueo con huella o PIN está activado (`HU-04`, `RNF-12`)
- **WHEN** se emite la notificación de recordatorio en pantalla bloqueada o el estudiante toca la notificación o la acción «Iniciar»
- **THEN** la notificación oculta materia y horario tras una versión pública genérica en la pantalla de bloqueo, y al interactuar exige primero completar la autenticación antes de mostrar datos o iniciar la sesión

#### Scenario: Ventana de aviso vencida al guardar
- **GIVEN** que falta menos tiempo que la antelación elegida para que inicie la sesión (`inicio − antelación <= ahora < inicio`)
- **WHEN** el estudiante ajusta el horario o la antelación y guarda o reprograma la sesión
- **THEN** el formulario muestra un aviso informativo indicando que el recordatorio ya quedó en el pasado y guarda la sesión sin emitir una notificación inmediata

#### Scenario: Recuperación tras reinicio o actualización de la app
- **GIVEN** que existen sesiones Pendientes con recordatorio activo cuando el dispositivo completa el arranque, se actualiza el paquete de la aplicación o el usuario concede alarmas exactas en ajustes
- **WHEN** el sistema recibe el evento correspondiente
- **THEN** el sistema reprograma todos los avisos futuros (`disparo > ahora`) y muestra de inmediato la notificación únicamente para los recordatorios cuyo instante de disparo ya venció mientras la sesión aún no inicia (`disparo <= ahora < inicio`)
