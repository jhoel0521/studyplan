# Standard changelog

The version of the standard increases with every change to `.ux/standard/`. Reports record the version they were made against.

## [0.1.0] — 2026-11-19
- Initial standard created by ux-init / ux-discover.
- Requested by: ux-init (automatic) · Reason: initial setup

## [0.2.0] — 2026-11-19
- Línea gráfica «Índigo Digital»: tokens de color claro/oscuro, radios, escala tipográfica, botón de 52 dp; users.md, brand.md y references.md completados con lo documentado.
- Requested by: jhoel0521 · Reason: preparar el change aplicar-linea-grafica-indigo
- Impacto: el tema actual (Material 3 por defecto) y `colors.xml` no coinciden con los tokens; se alinean en ese change.

## [0.2.1] — 2026-11-19
- Botón único de 48 dp (antes 52 dp en la guía de Claude Design): el Product Owner prefiere botones más pequeños; coincide con la densidad `comfortable` (48 dp táctil). Si se ve grande, se reduce solo el aspecto visual manteniendo 48 dp de área táctil.
- Token nuevo `opacity.chip-materia` = 0,15 (PLAN-06 de la revisión).
- Requested by: jhoel0521 · Reason: respuesta a ux-plan-review

## [0.2.2] — 2026-11-19
- `danger` claro pasa de `#DC2626` a `#B91C1C` (nuevo `red.700`): `#DC2626` daba 4,43:1 sobre `bg` y 4,08:1 sobre `surface-variant`, por debajo de 4,5:1; con `#B91C1C` da 5,94:1 y 5,47:1.
- Roles nuevos: `on-danger`, `on-success`, `on-warning` y `ring-track` (claro y oscuro); pares de contraste de `danger`, `text` y `text-muted` contra `bg` y `surface-variant`, y de cada `on-*` contra su color.
- `density: comfortable` confirmada por el Product Owner (2026-11-19).
- Requested by: jhoel0521 · Reason: revisión EGM del change aplicar-linea-grafica-indigo (B-01, PO-4)
