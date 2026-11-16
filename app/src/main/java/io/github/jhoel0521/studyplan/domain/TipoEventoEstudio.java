package io.github.jhoel0521.studyplan.domain;

/**
 * Tipos de evento persistibles en la línea de tiempo de una sesión en curso (HU-39, RN-22 a RN-29).
 */
public enum TipoEventoEstudio {
    INICIO(true),
    PAUSA(true),
    REANUDACION(true),
    SALIDA_APP(false),
    REGRESO_APP(true),
    FIN_PLANIFICADO(true),
    TIEMPO_EXTRA(true),
    ESTUDIO_APP_CERRADA(false),
    CAMBIO_SESION(true),
    AJUSTE_MANUAL(true),
    FIN(true);

    private final boolean marcaDentroDeApp;

    TipoEventoEstudio(boolean marcaDentroDeApp) {
        this.marcaDentroDeApp = marcaDentroDeApp;
    }

    /**
     * Indica si el evento representa actividad registrada dentro de la app (útil para recuperar el
     * último instante conocido ante cierre abrupto o vencimiento de 3 días según RN-29).
     */
    public boolean esMarcaDentroDeApp() {
        return marcaDentroDeApp;
    }
}
