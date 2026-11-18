package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.annotation.IdRes;

import io.github.jhoel0521.studyplan.R;

/**
 * Pestañas de la barra de navegación (inferior en teléfonos, riel lateral desde 600 dp). Cada una es una Activity;
 * una pestaña se habilita cuando su épica está implementada: basta con marcarla aquí.
 *
 * <p>Implementa: HU-09 (la pestaña Materias) y la decisión D3 de {@code add-gestion-materias} (navegación común a
 * todas las épicas).
 */
public enum DestinoPrincipal {
    INICIO(R.id.nav_inicio, MainActivity.class),
    CALENDARIO(R.id.nav_calendario, CalendarioActivity.class),
    ESTADISTICAS(R.id.nav_estadisticas, EstadisticasActivity.class),
    MATERIAS(R.id.nav_materias, MateriasActivity.class);

    @IdRes
    private final int idMenu;
    private final Class<?> actividad;

    DestinoPrincipal(@IdRes int idMenu, Class<?> actividad) {
        this.idMenu = idMenu;
        this.actividad = actividad;
    }

    @IdRes
    public int idMenu() {
        return idMenu;
    }

    /** @return la Activity de la pestaña, o {@code null} si todavía no está implementada */
    public Class<?> actividad() {
        return actividad;
    }

    public boolean implementado() {
        return actividad != null;
    }

    /** @return el destino de un elemento del menú, o {@code null} si el id no corresponde a ninguno */
    public static DestinoPrincipal deIdMenu(@IdRes int id) {
        for (DestinoPrincipal destino : values()) {
            if (destino.idMenu == id) {
                return destino;
            }
        }
        return null;
    }
}
