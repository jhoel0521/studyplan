# Bitácora de Auditoría IA — StudyPlan

Formato de cada entrada: **fecha · herramienta/modelo · solicitud · qué generó la IA · decisión humana · justificación**.
Decisiones posibles: Aceptado · Modificado · Rechazado · Decidido por el humano · Pendiente.

---

## Sesión 1 — 2026-11-04 · Claude Code (Claude Opus 5.5) · OpenSpec 1.14.3

**Entradas del estudiante:** índice del proyecto (PDF), consigna «Modelado de requerimientos» (PDF), mockup de StudyPlan (imagen), enunciado del docente (proyecto 7: app móvil para gestionar horas de estudio).
**Lema del docente:** «En esta era de IA y agentes, ¿qué diferencia a un programador de un médico? ¿Qué no puede hacer un médico con la IA y los agentes?»

| # | Solicitud / tema | Qué hizo la IA | Decisión humana | Justificación |
|---|---|---|---|---|
| 1 | Planificar la entrega (cap. 1 a 3.2.2 + Auditoría IA) | Plan y preguntas sobre formato, stack y metodología | Modificado | Word APA; Scrum + SDD con OpenSpec |
| 2 | Stack | Ofreció React Native, Flutter, Tauri, Laravel | **Rechazado** | El docente pidió app nativa; Java porque se enseña en clase |
| 3 | HU vs. casos de uso | Recomendó HU + criterios | Aceptado con restricción | Indirecta del docente: las tesis fallan por mezclar métodos ágiles y tradicionales. Sin casos de uso |
| 4 | Ubicación del proyecto | Propuso una carpeta aparte para documentos | **Modificado** | La raíz es el proyecto Android Studio, con docs/, openspec/, AGENTS.md, CLAUDE.md, .agents/ y .claude/ dentro |
| 5 | Package name | Explicó que en Play es permanente; propuso `io.github.jhoel0521.studyplan` | Aceptado | No hay dominio propio; se ancla a la cuenta de GitHub. El estudiante creó el proyecto en Android Studio (Java, minSdk 24) |
| 6 | `openspec init --tools claude,agents --language es` | Creó openspec/, config.yaml, skills y comandos /opsx:* | Aceptado | OpenSpec 1.14.3 usa `config.yaml` (no `project.md`); el contexto se escribió ahí |
| 7 | Diagramas | Intentó descargar PlantUML; la política de permisos lo bloqueó | Aceptado | Se usó Pillow (local) con el script reproducible `docs/build/diagramas.py` |
| 8 | Requisitos v1 | **Redactó 19 HU, reglas y 16 RNF sin consultar** y asumió «sin internet» | **Rechazado** | El estudiante: «No me preguntaste reglas de dominio, requisitos funcionales y no funcionales. ¿Por qué crees que tendremos un servidor?» |
| 9 | Entrevista de levantamiento (5 rondas) | Preguntas cerradas con opción libre | Decidido por el humano | Ver «Respuestas de la entrevista» abajo |
| 10 | Calendario | Solo tenía la vista mensual (v1) | **Modificado** | «Necesitamos ver mi calendario de estudio, es una página muy importante que mencionó el docente»: vistas de día, semana y mes |
| 11 | Escenarios de estudio real | La v2 solo decía «Detener guarda la duración real» | **Modificado** | El PO narró escenarios que originaron HU-39 a HU-45 y RN-22 a RN-30 |
| 12 | Diseño del cronómetro | — | Decidido por el humano | «Nunca tuve un proceso contando segundo a segundo: registrar timestamps y mostrar el cronómetro solo si está en la app» |
| 13 | Word COM (actualizar índice y PDF) | El script se colgó dos veces (proceso cerrado con permiso del estudiante) | Pendiente | El estudiante actualiza los campos en Word: Ctrl+A, F9 |
| 14 | Requisitos v3 + documento | 45 HU, 30 RN, 19 RNF, 10 specs; `openspec validate --strict` OK | **Pendiente** | Revisión humana: tareas 2.1 a 2.3 de `tasks.md` |

### Respuestas de la entrevista (Product Owner)

- **Servidor:** solo local y offline. Copia de seguridad encriptada en Google. El usuario puede borrar su información cuando quiera.
- **Usuarios:** es una app de autocontrol; para la 0.1.0 solo el estudiante. «Me gustaría agregar cosas, pero primero el entregable 0.1.0.»
- **Metas:** total y por materia, **independientes**.
- **Extras en la 0.1.0:** modo No molestar, técnicas de estudio (Pomodoro), recordatorios, cronómetro, exportar PDF/CSV, mensaje motivacional.
- **Backup:** «como WhatsApp»: Nunca, Diaria, Semanal, Mensual, etc. Contraseña **opcional**.
- **Sesiones pasadas:** con límite de **3 días**.
- **Cruces de horario:** **se bloquean**.
- **No molestar:** configuración global de la app; si está activa, se puede forzar en cada sesión.
- **Borrado:** el usuario elige qué borrar (teléfono, Drive o ambos).
- **Estados:** una sesión vencida pasa a «No realizada»; se puede reprogramar sin perder el registro del día no estudiado. «Máquina de estados: un estado no necesita saber todo el contexto, solo qué hace y cuál es el siguiente estado.» El registro tardío se llama «Completada (registro tardío)».
- **Pomodoro:** clásico configurable. Al terminar el plan, ofrecer ciclos extra o tiempo extra libre.
- **Calidad:** modo oscuro, bloqueo con huella/PIN, multiidioma, widget.
- **Calendario:** vistas de día, semana (tipo horario) y mes; estados; crear desde el calendario; resumen y filtro.
- **Fin planificado:** sigue y pregunta; mismo trato si se estudia menos o más. Se registra **lo real**.
- **Invasión de otra sesión:** avisar y decidir. «Terminé Matemáticas y continué con Inglés sin tocar el celular.»
- **Eventos de estudio:** registrar cuándo se sale de la app y enviar notificación para volver. Si se cierra la app por completo (llamada a las 8:45, se vuelve a las 11:00, a las 17:00 o al día siguiente, estudiando en grupo), **el usuario decide** hasta qué hora estudió o si sigue estudiando, y ese tramo se marca como evento «estudio con la app cerrada».
- **Métricas:** índice de concentración, puntualidad, respeto del tiempo, tiempo fuera de la app.

### Supuestos de la IA pendientes de confirmar
- **RN-12:** solo se eliminan sesiones Pendientes (derivada de RN-09).
- **RN-29:** una sesión En curso se puede declarar hasta 3 días después; vencido el plazo, se cierra en la última marca registrada en la app.
- **RN-13:** la semana va de lunes a domingo (tomado del mockup).
- Valores objetivo de los RNF (2 s, 200 ms, SUS ≥ 70, cobertura ≥ 70 %): son propuestas de la IA.
- **Referencias bibliográficas:** las propuso la IA. **Verificar cada DOI/URL** antes de la entrega.

### Lecciones
1. La IA produce artefactos con formato impecable que pueden no corresponder a lo que el usuario necesita. El formato válido (`openspec validate`) no garantiza que los requisitos sean correctos.
2. Los escenarios narrados por el usuario revelan reglas de dominio que ninguna plantilla anticipa.
3. Nueva regla en AGENTS.md: **la IA no inventa requisitos; si falta información, pregunta.**
