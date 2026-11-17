package io.github.jhoel0521.studyplan.domain;

/**
 * Una materia activa con las horas que lleva estudiadas, tal como se muestra en la lista.
 *
 * <p>Implementa: HU-09 (lista con horas acumuladas). Mientras no existan sesiones las horas realizadas son cero.
 */
public final class ResumenMateria {

    private final Materia materia;
    private final double horasRealizadas;
    private final ProgresoMeta metaSemanaActual;

    public ResumenMateria(Materia materia, double horasRealizadas) {
        this(materia, horasRealizadas, null);
    }

    public ResumenMateria(Materia materia, double horasRealizadas, ProgresoMeta metaSemanaActual) {
        this.materia = materia;
        this.horasRealizadas = horasRealizadas;
        this.metaSemanaActual = metaSemanaActual;
    }

    public Materia materia() {
        return materia;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }

    /** Progreso de la meta semanal de la materia en la semana actual, o {@code null} si no tiene meta. */
    public ProgresoMeta metaSemanaActual() {
        return metaSemanaActual;
    }
}
