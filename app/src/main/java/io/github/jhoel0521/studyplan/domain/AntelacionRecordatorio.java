package io.github.jhoel0521.studyplan.domain;

/**
 * Antelación configurable para emitir recordatorios locales antes de una sesión planificada
 * ({@code HU-34}, {@code RNF-15}).
 */
public enum AntelacionRecordatorio {
    SIN_RECORDATORIO(0),
    MINUTOS_5(5),
    MINUTOS_15(15),
    MINUTOS_30(30);

    private final int minutos;

    AntelacionRecordatorio(int minutos) {
        this.minutos = minutos;
    }

    /** Minutos de anticipación antes del inicio planificado ({@code 0} cuando está desactivado). */
    public int minutos() {
        return minutos;
    }

    /** Indica si esta configuración debe programar y mostrar aviso previo. */
    public boolean activo() {
        return minutos > 0;
    }

    /** Duración en milisegundos de anticipación respecto al instante de inicio. */
    public long milisAntes() {
        return minutos * Instante.MILIS_POR_MINUTO;
    }

    /** Devuelve los minutos o {@code null} cuando corresponde a {@link #SIN_RECORDATORIO}. */
    public Integer aMinutosNullable() {
        return activo() ? minutos : null;
    }

    /**
     * Convierte un entero persistido al valor del enum usando {@code respaldo} si {@code valor}
     * no pertenece al conjunto permitido ({@code null} y {@code 0} mapean a {@link #SIN_RECORDATORIO}).
     */
    public static AntelacionRecordatorio deMinutos(
            Integer valor, AntelacionRecordatorio respaldo) {
        AntelacionRecordatorio porDefecto = respaldo == null ? SIN_RECORDATORIO : respaldo;
        if (valor == null || valor == 0) {
            return SIN_RECORDATORIO;
        }
        for (AntelacionRecordatorio opcion : values()) {
            if (opcion.minutos == valor) {
                return opcion;
            }
        }
        return porDefecto;
    }
}
