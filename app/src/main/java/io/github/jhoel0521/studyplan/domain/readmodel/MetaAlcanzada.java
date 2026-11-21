package io.github.jhoel0521.studyplan.domain.readmodel;

/**
 * Meta semanal alcanzada en un tramo recién cerrado para mostrar una única felicitación acumulada
 * ({@code HU-28}, {@code RN-16}).
 */
public final class MetaAlcanzada {

    private final Long materiaId;
    private final String nombreMateria;
    private final double horasObjetivo;
    private final double horasRealizadas;

    public MetaAlcanzada(
            Long materiaId,
            String nombreMateria,
            double horasObjetivo,
            double horasRealizadas) {
        this.materiaId = materiaId;
        this.nombreMateria = nombreMateria == null ? "" : nombreMateria;
        this.horasObjetivo = horasObjetivo;
        this.horasRealizadas = horasRealizadas;
    }

    public Long materiaId() {
        return materiaId;
    }

    public boolean esMetaTotal() {
        return materiaId == null;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public double horasObjetivo() {
        return horasObjetivo;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }
}
