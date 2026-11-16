package io.github.jhoel0521.studyplan.domain;

/**
 * Intervalo efectivo de estudio reconstruido desde eventos persistidos (HU-22, HU-41, HU-44).
 */
public final class IntervaloEfectivo {

    private final long inicioMilis;
    private final long finMilis;
    private final boolean esAppCerrada;

    public IntervaloEfectivo(long inicioMilis, long finMilis, boolean esAppCerrada) {
        this.inicioMilis = inicioMilis;
        this.finMilis = Math.max(inicioMilis, finMilis);
        this.esAppCerrada = esAppCerrada;
    }

    public long inicioMilis() {
        return inicioMilis;
    }

    public long finMilis() {
        return finMilis;
    }

    public boolean esAppCerrada() {
        return esAppCerrada;
    }

    public long duracionMilis() {
        return Math.max(0L, finMilis - inicioMilis);
    }
}
