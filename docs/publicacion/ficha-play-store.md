# Ficha de Google Play y declaraciones de StudyPlan (borrador)

Borrador preparado por la IA el 2026-11-23 para que el Product Owner lo copie a Play Console. **Todo lo marcado
`[VERIFICAR]` hay que comprobarlo en la propia consola o en la ayuda de Play**: los formularios y límites cambian y la IA
no los pudo consultar. Los textos describen solo lo que la app hace hoy (specs en `openspec/specs/`); si se cambia una
función, hay que cambiar el texto.

## 1. Datos fijos

| Campo | Valor |
|---|---|
| Nombre de la app | StudyPlan (máximo 30 caracteres) |
| Identificador | `io.github.jhoel0521.studyplan` (no cambia nunca) |
| Tipo | App · Gratuita · sin anuncios · sin compras |
| Categoría sugerida | Educación (alternativa: Productividad) `[VERIFICAR]` |
| Política de privacidad | <https://jhoel0521.github.io/studyplan-privacy/> (quitar antes el aviso «en desarrollo») |
| Correo de contacto | el que el Product Owner decida publicar (no se escribe aquí) |
| Idioma predeterminado | Español; traducción al inglés |

## 2. Textos de la ficha

### Español

**Descripción breve** (máximo 80 caracteres):

> Planifica tu estudio: materias, sesiones, metas, Pomodoro y respaldo opcional.

**Descripción completa** (máximo 4000 caracteres):

> StudyPlan te ayuda a organizar tu tiempo de estudio sin complicaciones y sin necesitar una cuenta.
>
> • Materias: registra tus materias con color y organiza todo por ellas.
> • Sesiones de estudio: planifica sesiones con día, hora y duración, y llévalas por estados (pendiente, en curso,
>   completada, reprogramada).
> • Calendario: mira tu semana, tu día y tu mes, con tus sesiones y eventos de estudio.
> • Técnicas de estudio: temporizador Pomodoro y modo No molestar mientras estudias.
> • Metas semanales: define cuántas horas quieres estudiar y sigue tu progreso, con historial de metas.
> • Estadísticas y reportes: gráficos de tu tiempo por materia y reportes de un periodo que puedes exportar en PDF o CSV
>   y compartir.
> • Recordatorios: avisos antes de cada sesión.
> • Widget de progreso: tu avance de la semana en la pantalla de inicio.
> • Bloqueo opcional con huella o PIN del teléfono.
> • Copia de seguridad opcional en Google Drive: si la activas, se guarda en una carpeta privada de tu Drive que solo
>   StudyPlan puede ver, y puedes protegerla con una contraseña. Viene apagada.
> • Borrar mi información: elige si borrar los datos del teléfono, de Drive o de ambos.
>
> Tus datos están en tu teléfono. StudyPlan funciona sin conexión; solo usa internet para la copia de seguridad en
> Google Drive, si tú la activas. Sin anuncios.

### English

**Short description** (max 80 characters):

> Plan your study: subjects, sessions, goals, Pomodoro and optional backup.

**Full description** (max 4000 characters):

> StudyPlan helps you organize your study time simply, with no account required.
>
> • Subjects: add your subjects with a color and keep everything organized by subject.
> • Study sessions: plan sessions with a day, time and duration, and follow them through their states (pending, in
>   progress, completed, rescheduled).
> • Calendar: see your week, day and month with your sessions and study events.
> • Study techniques: Pomodoro timer and Do Not Disturb while you study.
> • Weekly goals: set how many hours you want to study and track your progress, with a goal history.
> • Statistics and reports: charts of your time per subject and period reports you can export as PDF or CSV and share.
> • Reminders: alerts before each session.
> • Progress widget: your weekly progress on the home screen.
> • Optional lock with your phone's fingerprint or PIN.
> • Optional backup to Google Drive: if you turn it on, it is saved in a private folder in your Drive that only
>   StudyPlan can see, and you can protect it with a password. It is off by default.
> • Delete my information: choose whether to delete data from the phone, from Drive, or both.
>
> Your data lives on your phone. StudyPlan works offline; it only uses the internet for the Google Drive backup, if you
> turn it on. No ads.

## 3. Gráficos y capturas

| Elemento | Requisito `[VERIFICAR]` | Estado |
|---|---|---|
| Ícono de la app | PNG 512 × 512 | Listo: `docs/documento/recursos/logos/play-store/icono-512.png` |
| Gráfico de función | 1024 × 500 | Listo: `.../play-store/grafico-funcion-1024x500.png` |
| Capturas de teléfono | mínimo 2, hasta 8 | **Pendiente** |

Capturas sugeridas (en este orden, con datos de ejemplo y sin nombre real ni correo): Inicio con la meta semanal ·
Calendario de la semana · Materias · Ejecución de una sesión con Pomodoro · Estadísticas · Copia de seguridad.

## 4. Seguridad de los datos (Data safety) — respuestas propuestas

Hechos de la app: sin servidor propio, sin anuncios, sin SDK de analítica ni de publicidad (dependencias en
`gradle/libs.versions.toml`: Room, Material, MPAndroidChart, play-services-auth, work-runtime). Los datos viven en el
teléfono. La única salida de datos es la copia opcional a `appDataFolder` del Drive del propio usuario.

| Pregunta | Respuesta propuesta | Nota |
|---|---|---|
| ¿La app recopila o comparte datos? | Sí, de forma **opcional** (solo la copia en Drive) `[VERIFICAR]` | Si Play considera que enviar al Drive del propio usuario no es «recopilar», el formulario lo dirá; decidir con la ayuda de Play en pantalla |
| ¿Se cifran los datos en tránsito? | Sí (HTTPS a la API de Drive) | `ClienteGoogleDriveRest` |
| ¿Se pueden pedir borrar los datos? | Sí: «Borrar mi información» (teléfono, Drive o ambos) | HU-38 |
| Datos de la copia | perfil (nombre y horas de estudio), materias, sesiones, eventos, tramos y metas | Cifrado opcional con contraseña (AES-256-GCM) |
| ¿Se comparten con terceros? | No | El Drive es el del propio usuario |
| ¿Datos de ubicación, contactos, fotos, mensajes, finanzas? | No | |
| ¿Hay compras, anuncios o cuenta obligatoria? | No · No · No (la cuenta de Google es solo para el respaldo) | |

Aviso: `android:allowBackup="true"` está activo en el manifiesto (decisión del Product Owner de no tocarlo). Eso
significa que el respaldo automático de Android también puede copiar los datos al Drive del usuario. Hay que tenerlo en
cuenta al responder el formulario y en la política de privacidad `[VERIFICAR]`.

## 5. Clasificación de contenido (cuestionario IARC) — respuestas propuestas `[VERIFICAR]`

App de organización personal sin contenido de terceros: violencia, sexo, lenguaje, sustancias controladas, juego de
azar, y contenido generado por usuarios visible a otros → **No** en todos. Sin interacción entre usuarios, sin compartir
ubicación. Resultado esperado: apta para todo público. Público objetivo: indicar el rango que decida el Product Owner
(si incluye menores de 13 años, Play aplica la política de familias `[VERIFICAR]`).

## 6. Permisos que Play puede pedir justificar `[VERIFICAR cada uno]`

| Permiso declarado | Para qué se usa | Qué justificar |
|---|---|---|
| `INTERNET` | Solo copia en Google Drive (opcional) | Nada especial |
| `POST_NOTIFICATIONS` | Recordatorios de sesión | Permiso en tiempo de ejecución |
| `SCHEDULE_EXACT_ALARM` | Recordatorios a la hora exacta | Declaración de alarmas exactas |
| `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE` | Un único servicio mientras hay una sesión «En curso» (HU-49) | Declaración del tipo de servicio en primer plano: puede pedir una descripción y un video corto |
| `ACCESS_NOTIFICATION_POLICY` | Activar «No molestar» mientras se estudia | Acceso a No molestar, concedido por el usuario en Ajustes |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar recordatorios al reiniciar | Nada especial |
| `VIBRATE` | Vibración de avisos | Nada especial |

## 7. Pendiente del Product Owner

1. Declarar la condición de comerciante (acción obligatoria en la cuenta, antes de febrero de 2027).
2. Crear la app, pegar los textos, subir los gráficos y las capturas.
3. Responder los formularios con la tabla 4 y 5 como punto de partida.
4. Actualizar la política de privacidad (quitar «en desarrollo» y mencionar el respaldo opcional y `allowBackup`).
5. Subir el primer AAB firmado a una pista interna o cerrada.

## 8. Declaración del servicio en primer plano (borrador, falta el video)

Play exige, para `FOREGROUND_SERVICE_SPECIAL_USE`, un **vínculo a un video** y una descripción. El video lo graba y publica (por ejemplo, no listado en YouTube) el Product Owner: debe mostrar iniciar una sesión, la notificación con Pausar, Detener y No molestar, y la pantalla de bloqueo. Descripción verificada contra `AndroidManifest.xml` y HU-49:

> StudyPlan usa un único servicio en primer plano mientras hay una sesión de estudio En curso. El servicio sostiene una notificación persistente, visible también en la pantalla de bloqueo, con la materia, el tiempo transcurrido (o la fase y el tiempo restante del Pomodoro) y los botones Pausar, Detener y No molestar, para que el estudiante controle su sesión sin abrir la app. Se inicia de inmediato cuando el estudiante empieza la sesión: el cronómetro y los controles deben estar disponibles en ese momento, y si el servicio se detuviera, la notificación con los controles desaparecería mientras la sesión sigue En curso. El servicio permanece durante una pausa y se detiene cuando la sesión termina, se detiene o se cancela. No hay ningún otro servicio en primer plano, y no se usa sin una sesión En curso.
