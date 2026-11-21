package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Progreso de una meta semanal (total o por materia) en una semana concreta ({@code HU-27},
 * {@code RN-11}, {@code RN-13}, {@code RN-14}, {@code RN-31}, {@code RN-33}).
 */
public final class ProgresoMeta {

    private final Fecha semanaInicio;
    private final Long materiaId;
    private final String nombreMateria;
    private final double horasRealizadas;
    private final double horasObjetivo;
    private final boolean estimada;

    public ProgresoMeta(
            Fecha semanaInicio,
            Long materiaId,
            String nombreMateria,
            double horasRealizadas,
            double horasObjetivo,
            boolean estimada) {
        this.semanaInicio = Objects.requireNonNull(semanaInicio, "semanaInicio");
        this.materiaId = materiaId;
        this.nombreMateria = nombreMateria == null ? "" : nombreMateria;
        this.horasRealizadas = Math.max(0.0, horasRealizadas);
        this.horasObjetivo = horasObjetivo;
        this.estimada = estimada;
    }

    public Fecha semanaInicio() {
        return semanaInicio;
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

    public double horasRealizadas() {
        return horasRealizadas;
    }

    public double horasObjetivo() {
        return horasObjetivo;
    }

    public boolean estimada() {
        return estimada;
    }

    public boolean cumplida() {
        return horasObjetivo > 0 && horasRealizadas + 1e-6 >= horasObjetivo;
    }

    public int porcentaje() {
        if (horasObjetivo <= 0) {
            return 0;
        }
        return (int) Math.min(100L, Math.round((horasRealizadas / horasObjetivo) * 100.0));
    }
}
