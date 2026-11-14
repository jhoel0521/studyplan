package io.github.jhoel0521.studyplan.domain;

/**
 * Una materia activa con las horas que lleva estudiadas, tal como se muestra en la lista.
 *
 * <p>Implementa: HU-09 (lista con horas acumuladas). Mientras no existan sesiones las horas realizadas son cero.
 */
public final class ResumenMateria {

    private final Materia materia;
    private final double horasRealizadas;

    public ResumenMateria(Materia materia, double horasRealizadas) {
        this.materia = materia;
        this.horasRealizadas = horasRealizadas;
    }

    public Materia materia() {
        return materia;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }
}
