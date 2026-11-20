# ux-check — studyplan (change aplicar-linea-grafica-indigo) · 2026-11-19

Estándar: `.ux/standard` 0.2.2 (`ux-kit.status: defined`) · plataforma: android, `input: touch`, `density: comfortable`.
Alcance: rutas explícitas (`res/layout`, `res/layout-w600dp`, `res/values*`, `res/color`, `res/drawable`, `adapter/in/ui`), porque el árbol de trabajo ya tenía cambios ajenos antes del change. 156 archivos escaneados.

## Veredicto
✅ Sin bloqueantes abiertos (1 bloqueante real encontrado y **corregido** en esta revisión; 1 candidato del escáner descartado como falso positivo).

| Severidad | Encontrados | Corregidos | Abiertos |
|---|---|---|---|
| blocker | 1 (+1 falso positivo) | 1 | 0 |
| major | 1 | 0 | 1 (decisión del PO) |
| suggestion | 2 | 0 | 2 |

## Hallazgos
1. **blocker · `fitts-spacing` · Fitts — corregido.** Acciones opuestas con menos de 16 dp: «Pausar» / «Detener» (12 dp) y «Sigo» / «Terminé» (8 dp) en `activity_sesion_en_curso.xml` (móvil y `w600dp`), y «Eliminar sesión» a 8 dp de «Reprogramar» en `activity_sesion_detalle.xml` (móvil y `w600dp`). Arreglo: `flow_horizontalGap`/`flow_verticalGap` = 16 dp y `layout_marginTop` = 16 dp.
2. **descartado · `touch-target-platform` (AND-TARGET)** — `contenido_inicio.xml:162`: el escáner lee `layout_width="0dp"` (ancho por restricciones de `ConstraintLayout`) como tamaño real. La tarjeta ocupa todo el ancho y mide ≥ 72 dp. Candidato a issue de ux-kit.
3. **major · `primary-action-competition` · Hick — decisión del PO.** `activity_sesion_detalle.xml` (W15) no tiene acción primaria: sus acciones de estado son tonales y del mismo peso. Es lo que aprobó el Product Owner (design.md, decisión 7: pantalla de consulta). Propuesta: registrarlo en `config.yaml → exceptions` si el equipo lo confirma.
4. **suggestion · `type-levels` · Estética-usabilidad.** Inicio usa 6 tamaños (barra 20, saludo 24, horas 32, títulos/botones 16, cuerpo 14, etiquetas 12); el máximo recomendado es 4. Opción: barra superior a 16 o 24 sp para unificar.
5. **suggestion · `aesthetic-consistency`.** En el formulario de materia, «Eliminar» va relleno dentro del grupo de botones unidos (se conservó `GrupoBotones`, PO-5), mientras que en el resto de la app lo destructivo va en contorno rojo.

## Drift
- `token-drift`: ninguno. Colores, radios y alturas salen de `@color/sp_*`, `@dimen/*` y atributos del tema.
- `literal-design-values`: ninguno en atributos reales; los hex del inventario (`#1565c0`×3, `#2e7d32`×2, `#1e88e5`, `#ffffff`) son `tools:` de vista previa y `#3ddc84` es el launcher (fuera de alcance).
- Deriva inversa: ninguna.

## Checklist ux-laws (pantallas del change)
| Regla | Inicio | Selector | En curso | W13 | W14 | W15 | Estadísticas | Materias | Calendario |
|---|---|---|---|---|---|---|---|---|---|
| Fitts · objetivos ≥ 48 dp | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (excepción §6.2 de bloques) |
| Fitts · zona alcanzable | ❔ | ✅ | ✅ | ✅ | ✅ | ❔ | ❔ | ✅ | ❔ |
| Fitts · separación (≥ 16 dp opuestas) | ✅ | ✅ | ✅ (corregido) | ✅ | ✅ | ✅ (corregido) | ✅ | ✅ | ✅ |
| Hick · ≤ 7 opciones | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Hick · CTA destacada | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ (decisión PO) | ✅ | ✅ | ✅ |
| Jakob · convenciones | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Miller · listas > 7 | ✅ | ❔ (sin agrupar con muchas materias; fuera de alcance PO-1) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Estética · ≤ 4 tamaños | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Estética · paleta/radios coherentes | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ (grupo formulario) | ✅ |
| Densidad (`comfortable`) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

❔ = no verificable desde el código (zona de alcance según el contenido desplazable).

## Coherencia entre pantallas
Color de materia: barra de 4 dp en tarjetas (Inicio, Materias, calendario, W14), punto de 10 dp junto a un nombre (selector, en curso, detalle, estadísticas, reportes) y chip al 15 % (W14), siempre ajustado con `ColorMateria` (≥ 3:1). Botones alternables, tonales, chips y barra inferior usan `colorPrimaryContainer` en toda la app.
