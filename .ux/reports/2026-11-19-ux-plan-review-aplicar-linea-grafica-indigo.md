# ux-plan-review — aplicar-linea-grafica-indigo (2026-11-19)

Estándar: `.ux/standard` 0.2.0 · plan revisado: `openspec/changes/aplicar-linea-grafica-indigo/` (proposal, spec delta `perfil-ajustes`, design, tasks).
Alcance de la revisión: el plan y las 3 guías del lienzo de Claude Design. **No se revisaron los 15 artboards de pantalla** (❔ unverifiable).

## Cobertura
| # | Punto | Estado |
|---|---|---|
| 1 | Usuarios y contexto | ✅ (`users.md`; hábitos y entorno `{{to confirm}}`) |
| 2 | Flujo / Peak-End | PLAN-01 |
| 3 | Estados | PLAN-02 |
| 4 | Plataforma (Android M3) | ✅ tema M3, barra inferior con indicador, nada que invente patrones |
| 5 | Accesibilidad | PLAN-03, PLAN-04, PLAN-05 |
| 6 | Contenido | ✅ no cambia textos; glosario respetado |
| 7 | Estándar visual | PLAN-06 |
| 8 | Formularios | n/a (formularios fuera de alcance) |
| 9 | Riesgo | n/a (no cambia acciones destructivas, solo su estilo) |
| 10 | Rendimiento | ✅ sin trabajo nuevo en el hilo principal |
| 11 | Verificación | ✅ 4.4 corre `ux-check`; ver PLAN-08 |

## Hallazgos
1. **PLAN-01 · major · `hick-options` / `fitts-reach` / `peak-end`** — El plan no declara por pantalla la acción primaria, su zona ni cómo termina el flujo. Añadir en `design.md` una tabla por pantalla: acción primaria única, tamaño/zona del objetivo (tercio inferior) y cierre (Inicio: «Estudiar ahora»; Sesión en curso: Pausar; W13: Volver a la sesión; W14: Confirmar reparto; fin de sesión: confirmación + siguiente paso). Evidencia que lo subiría a blocker: dos acciones de igual peso en una vista.
2. **PLAN-02 · major · `missing-empty-state`** — 3.7 y 3.8 no cubren estados vacío, de carga y de error de Estadísticas y Materias. Añadir a ambas tareas: «verificar con capturas el estado vacío (sin sesiones / sin materias) y el de error, con icono + texto, en claro y oscuro».
3. **PLAN-03 · major · `wcag-a-aa` (1.4.4)** — Se prueba a 130 %, pero el cronómetro de 56 sp y los botones de 52 dp pueden recortarse con la fuente máxima del sistema. Añadir a 4.2: «probar el tamaño de fuente más grande del sistema (≈ 200 %): sin texto cortado ni solapado, cronómetro con `autoSize` o `maxLines`». Evidencia que lo subiría: texto recortado en un dispositivo real.
4. **PLAN-04 · minor · `dark-mode`** — «Versión aclarada del color de materia en oscuro» no tiene tarea ni método. Añadir a 3.8: «definir cómo se aclara el color de materia en oscuro y comprobar que barra y punto alcanzan ≥ 3:1 (WCAG 1.4.11) contra `surface`».
5. **PLAN-05 · minor · `wcag-a-aa` (1.4.11)** — Con el borde de 1 dp en `border-subtle` (≈ 1,3:1 en claro) una tarjeta clicable (p. ej. la próxima sesión de Inicio) no se reconoce como tocable por su borde. Añadir a 3.1: «la tarjeta clicable lleva un indicador no cromático (chevron) o su nombre accesible `role=button`».
6. **PLAN-06 · minor · tokens** — Hay valores nuevos fuera del estándar: la opacidad del chip de materia (15 %), el color de la pista del anillo y la altura de botón. Pasarlos a `tokens.json` con `ux-discover` antes de la tarea 1.1 (el `button-height` ya está).
7. **PLAN-07 · suggestion · `miller-chunks`** — El selector rápido lista materias sin límite. Declarar en 3.2: «con más de 7 materias, las recientes primero y el resto en una lista con búsqueda».
8. **PLAN-08 · suggestion** — `ux-check` solo corre al final (4.4). Añadir «correr `ux-check` sobre los archivos de cada tarea 3.x» para detectar antes la deriva.

## Preguntas para el equipo (decisiones, no hallazgos)
- ¿Los `IconButton` quedan en 48 dp (supuesto del design.md) o también miden 52 dp?
- ¿Se aprueba la fuente Plus Jakarta Sans embebida (OFL)? Bloquea la tarea 2.3.
- ¿`density: comfortable` es definitivo? Los botones de 52 dp quedan entre comfortable (48) y spacious (56).
- `item_bloque_calendario` (color de materia como fondo) queda fuera: ¿cambio aparte?
