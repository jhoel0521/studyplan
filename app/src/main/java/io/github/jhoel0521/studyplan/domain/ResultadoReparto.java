package io.github.jhoel0521.studyplan.domain;

/**
 * Resultado inmutable del reparto proporcional de un tramo continuo entre dos sesiones
 * (HU-44, RN-28).
 */
public final class ResultadoReparto {

    private final long inicioTramoMilis;
    private final long corteMilis;
    private final long finTramoMilis;
    private final int minutosPrimeraSesion;
    private final int minutosSegundaSesion;

    public ResultadoReparto(
            long inicioTramoMilis,
            long corteMilis,
            long finTramoMilis,
            int minutosPrimeraSesion,
            int minutosSegundaSesion) {
        this.inicioTramoMilis = inicioTramoMilis;
        this.corteMilis = corteMilis;
        this.finTramoMilis = finTramoMilis;
        this.minutosPrimeraSesion = Math.max(0, minutosPrimeraSesion);
        this.minutosSegundaSesion = Math.max(0, minutosSegundaSesion);
    }

    public long inicioTramoMilis() {
        return inicioTramoMilis;
    }

    public long corteMilis() {
        return corteMilis;
    }

    public long finTramoMilis() {
        return finTramoMilis;
    }

    public int minutosPrimeraSesion() {
        return minutosPrimeraSesion;
    }

    public int minutosSegundaSesion() {
        return minutosSegundaSesion;
    }
}
