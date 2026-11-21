package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/**
 * Value Object inmutable que describe la sesión existente con la que ocurre un cruce horario
 * (HU-10, HU-11, HU-14, RN-05).
 */
public final class ConflictoHorario {

    private final long sesionId;
    private final long materiaId;
    private final String nombreMateria;
    private final Fecha fecha;
    private final Hora horaInicio;
    private final Hora horaFin;

    public ConflictoHorario(
            long sesionId,
            long materiaId,
            String nombreMateria,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin) {
        this.sesionId = sesionId;
        this.materiaId = materiaId;
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.horaInicio = Objects.requireNonNull(horaInicio, "horaInicio");
        this.horaFin = Objects.requireNonNull(horaFin, "horaFin");
    }

    public long sesionId() {
        return sesionId;
    }

    public long materiaId() {
        return materiaId;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public Fecha fecha() {
        return fecha;
    }

    public Hora horaInicio() {
        return horaInicio;
    }

    public Hora horaFin() {
        return horaFin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConflictoHorario)) {
            return false;
        }
        ConflictoHorario otro = (ConflictoHorario) o;
        return sesionId == otro.sesionId
                && materiaId == otro.materiaId
                && nombreMateria.equals(otro.nombreMateria)
                && fecha.equals(otro.fecha)
                && horaInicio.equals(otro.horaInicio)
                && horaFin.equals(otro.horaFin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sesionId, materiaId, nombreMateria, fecha, horaInicio, horaFin);
    }
}
