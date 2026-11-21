package io.github.jhoel0521.studyplan.adapter.in.ui;

import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;

/**
 * Rol de color de cada estado de sesión (design.md, decisión 5a). Java puro, sin {@code android.*}, para probar el
 * mapeo en la JVM; {@link EstadoSesionVisual} traduce cada rol a su atributo del tema. El color nunca va solo: cada
 * estado conserva su icono y su etiqueta (RNF-06).
 *
 * <p>Implementa: HU-03 (escenario «Estado sin depender del color»), RNF-06, RNF-07.
 */
enum RolColorEstado {
    /** {@code colorPrimary} (índigo). */
    PRIMARIO,
    /** {@code colorSecondary} (verde azulado). */
    SECUNDARIO,
    /** {@code colorError}. */
    ERROR,
    /** {@code colorOnSurfaceVariant}. */
    VARIANTE;

    static RolColorEstado de(CodigoEstadoSesion estado) {
        switch (estado) {
            case PENDIENTE:
            case EN_CURSO:
                return PRIMARIO;
            case NO_REALIZADA:
                return ERROR;
            case COMPLETADA:
            case COMPLETADA_TARDIA:
                return SECUNDARIO;
            case REPROGRAMADA:
            default:
                return VARIANTE;
        }
    }
}
