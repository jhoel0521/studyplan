package io.github.jhoel0521.studyplan.domain.entity;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Snapshot semanal de una meta de estudio (total si {@code materiaId == null}, o por materia)
 * para preservar el historial ante cambios posteriores ({@code HU-25} a {@code HU-28},
 * {@code RN-13}, {@code RN-14}, {@code RN-15}, {@code RN-16}, {@code RN-33}).
 */
public final class MetaSemanal {

    private final Long id;
    private final Fecha semanaInicio;
    private final Long materiaId;
    private final double horasObjetivo;
    private final boolean felicitada;

    private MetaSemanal(
            Long id,
            Fecha semanaInicio,
            Long materiaId,
            double horasObjetivo,
            boolean felicitada) {
        this.id = id;
        this.semanaInicio = Objects.requireNonNull(semanaInicio, "semanaInicio").lunesDeSemana();
        this.materiaId = materiaId;
        this.horasObjetivo = horasObjetivo;
        this.felicitada = felicitada;
    }

    /** Crea una meta semanal validando el rango de horas ({@code RN-14}). */
    public static Resultado<MetaSemanal> crear(
            Fecha fechaEnSemana, Long materiaId, double horasObjetivo) {
        if (fechaEnSemana == null) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_OBLIGATORIA);
        }
        if (!Double.isFinite(horasObjetivo)
                || horasObjetivo <= 0
                || horasObjetivo > Perfil.META_MAXIMA_HORAS) {
            return Resultado.error(ErrorValidacion.META_FUERA_DE_RANGO);
        }
        return Resultado.ok(new MetaSemanal(
                null, fechaEnSemana.lunesDeSemana(), materiaId, horasObjetivo, false));
    }

    /** Reconstruye una fila persistida de {@code meta_semanal}. */
    public static MetaSemanal restaurar(
            Long id,
            Fecha semanaInicio,
            Long materiaId,
            double horasObjetivo,
            boolean felicitada) {
        return new MetaSemanal(id, semanaInicio, materiaId, horasObjetivo, felicitada);
    }

    public MetaSemanal conId(long nuevoId) {
        return new MetaSemanal(nuevoId, semanaInicio, materiaId, horasObjetivo, felicitada);
    }

    /** Actualiza el objetivo de esta semana validando {@code RN-14}. */
    public Resultado<MetaSemanal> conObjetivo(double nuevasHoras) {
        if (!Double.isFinite(nuevasHoras)
                || nuevasHoras <= 0
                || nuevasHoras > Perfil.META_MAXIMA_HORAS) {
            return Resultado.error(ErrorValidacion.META_FUERA_DE_RANGO);
        }
        return Resultado.ok(new MetaSemanal(id, semanaInicio, materiaId, nuevasHoras, felicitada));
    }

    public MetaSemanal marcarFelicitada() {
        return new MetaSemanal(id, semanaInicio, materiaId, horasObjetivo, true);
    }

    public Long id() {
        return id;
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

    public double horasObjetivo() {
        return horasObjetivo;
    }

    public boolean felicitada() {
        return felicitada;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetaSemanal)) {
            return false;
        }
        MetaSemanal otra = (MetaSemanal) o;
        return Double.compare(horasObjetivo, otra.horasObjetivo) == 0
                && felicitada == otra.felicitada
                && Objects.equals(id, otra.id)
                && semanaInicio.equals(otra.semanaInicio)
                && Objects.equals(materiaId, otra.materiaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, semanaInicio, materiaId, horasObjetivo, felicitada);
    }
}
