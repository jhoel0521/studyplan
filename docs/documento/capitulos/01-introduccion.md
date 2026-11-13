# Introducción

## Introducción y Contexto del Problema

La vida universitaria exige que el estudiante administre de forma autónoma su tiempo entre clases, trabajos prácticos, exámenes y, con frecuencia, actividades laborales o familiares. A diferencia de la educación secundaria, donde el horario lo estructura la institución, en la universidad gran parte del aprendizaje ocurre en horas de estudio independiente que el propio estudiante debe planificar, ejecutar y evaluar.

La literatura muestra que esta autogestión es un problema real. En un metaanálisis sobre la procrastinación, Steel (2007) estima que entre el 80 % y el 95 % de los estudiantes universitarios posterga sus tareas académicas, y relaciona este comportamiento con un menor rendimiento. Los estudios sobre gestión del tiempo indican, a su vez, que la planificación a corto plazo predice mejores calificaciones (Britton & Tesser, 1991) y que la percepción de control sobre el propio tiempo se asocia con mayor rendimiento y menor estrés (Macan et al., 1990).

En la práctica, muchos estudiantes planifican de manera informal (cuadernos, notas sueltas o calendarios genéricos) y no registran cuántas horas estudian realmente frente a las que planificaron, ni qué sesiones dejaron de cumplir. Sin ese dato no pueden saber a qué materias dedican poco tiempo, si avanzan hacia sus objetivos o si su plan es realista. Las aplicaciones de calendario o de tareas existentes no están orientadas a medir horas de estudio por materia ni a comparar lo planificado con lo ejecutado.

El presente proyecto propone **StudyPlan**, una aplicación móvil nativa para Android de **autocontrol del estudio**. Permite al estudiante gestionar sus materias, planificar sesiones en un calendario de estudio (vistas de día, semana y mes), ejecutar las sesiones con técnicas como Pomodoro, fijar metas semanales y consultar reportes y estadísticas que reflejan honestamente lo estudiado y lo no estudiado. El proyecto se desarrolla con asistencia de inteligencia artificial (IA) mediante desarrollo guiado por especificaciones (*Spec-Driven Development*) y documenta y audita cada decisión, de modo que el estudiante pueda defender técnicamente todo artefacto generado.

## Justificación del Proyecto

**Justificación académica.** El proyecto aplica el ciclo completo de ingeniería de software: levantamiento y especificación de requisitos, diseño, implementación y pruebas. Usa una metodología ágil (Scrum) con artefactos ágiles coherentes, historias de usuario y criterios de aceptación, sin mezclar técnicas de metodologías tradicionales.

**Justificación técnica.** Se desarrolla en Java nativo para Android, el lenguaje enseñado en la materia, con componentes oficiales de Android Jetpack (Room, ViewModel, LiveData, WorkManager) y Material Design 3. La aplicación es *offline-first*: funciona por completo sin conexión, sin servidor propio, y solo usa internet para la copia de seguridad opcional en el Google Drive del propio usuario. El uso de OpenSpec permite que la IA genere código a partir de especificaciones validadas y que cada línea de código pueda rastrearse hasta una historia de usuario.

**Justificación social.** Una herramienta gratuita que ayuda a planificar el estudio y a visualizar el progreso fortalece la autorregulación del aprendizaje (Zimmerman, 2002) y el establecimiento de metas (Locke & Latham, 2002), factores asociados al éxito académico. La aplicación se publicará en Google Play.

**Justificación profesional.** Ante la pregunta del docente sobre qué diferencia a un programador de otra persona que usa IA, este proyecto sostiene que la diferencia está en **especificar, validar y defender**. La sección *Auditoría IA* muestra un caso concreto: la IA redactó requisitos sin consultar y el estudiante los rechazó y los levantó mediante una entrevista.

## Objetivos

### Objetivo General

Desarrollar una aplicación móvil nativa para Android, en lenguaje Java, que permita a los estudiantes universitarios gestionar sus materias, planificar y ejecutar sesiones de estudio en un calendario, establecer metas semanales y consultar reportes y estadísticas de sus horas de estudio, aplicando Scrum y desarrollo guiado por especificaciones con asistencia auditada de inteligencia artificial.

### Objetivos Específicos

- Levantar y especificar los requisitos funcionales como historias de usuario con criterios de aceptación, las reglas de negocio del dominio y los requisitos no funcionales según ISO/IEC 25010:2023, versionándolos como especificaciones en OpenSpec.
- Implementar la gestión de materias y la planificación de sesiones de estudio gobernadas por una máquina de estados que conserve el historial de cumplimiento.
- Implementar un calendario de estudio con vistas de día, semana tipo horario y mes, que muestre el estado de cada sesión y permita planificar desde él.
- Implementar técnicas de apoyo a la concentración: cronómetro, Pomodoro configurable y modo No molestar.
- Implementar metas semanales totales y por materia, estadísticas y reportes exportables en PDF y CSV.
- Proteger los datos del estudiante mediante copia de seguridad configurable en Google Drive con cifrado opcional, bloqueo biométrico y borrado selectivo.
- Validar la aplicación con pruebas unitarias, pruebas instrumentadas y una evaluación de usabilidad con estudiantes, y documentar y auditar el uso de IA durante todo el desarrollo.

## Alcance del Proyecto

La versión 0.1.0 de StudyPlan, primer entregable funcional, comprende:

- Aplicación Android nativa (Java) para Android 7.0 (API 24) o superior, en español e inglés, con tema claro y oscuro.
- Un único rol de usuario, el estudiante, sin cuentas propias. Los datos se almacenan en el dispositivo (SQLite mediante Room) y la aplicación funciona sin conexión (*offline-first*).
- Módulos: perfil, inicio y ajustes (bloqueo con huella o PIN y widget); gestión de materias; sesiones de estudio con máquina de estados; calendario de estudio (día, semana y mes); técnicas y eventos de estudio (cronómetro por marcas de tiempo, Pomodoro, No molestar, tiempo extra y estudio con la app cerrada); metas semanales; estadísticas y métricas de concentración; reportes; recordatorios; y respaldo y privacidad.
- Copia de seguridad en el Google Drive del propio usuario, con frecuencia configurable y cifrado opcional con contraseña. Es el único uso de internet.
- Publicación en Google Play con el identificador permanente *io.github.jhoel0521.studyplan*.

Quedan **fuera del alcance** de esta versión: roles adicionales (tutor o docente), un servidor o backend propio, la sincronización en tiempo real entre dispositivos, las versiones para iOS y web, la integración con calendarios externos y la gamificación avanzada. Estas funciones podrán evaluarse en versiones posteriores.

En cuanto a la documentación, esta primera entrega cubre los capítulos 1 a 3.2.2 del índice del proyecto, más la sección *Auditoría IA*, un punto especial que se agrega al índice por indicación del docente para documentar el uso de inteligencia artificial.
