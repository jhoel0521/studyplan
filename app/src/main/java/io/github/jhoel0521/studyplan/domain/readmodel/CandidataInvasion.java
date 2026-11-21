package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/**
 * Sesión planificada inmediatamente posterior cuyo horario está siendo invadido por tiempo extra
 * (HU-43, HU-44, RN-27, RN-28).
 */
public final class CandidataInvasion {

    private final long sesionId;
    private final String nombreMateria;
    private final int colorMateria;
    private final Hora horaInicio;
    private final Hora horaFin;

    public CandidataInvasion(
            long sesionId,
            String nombreMateria,
            int colorMateria,
            Hora horaInicio,
            Hora horaFin) {
        this.sesionId = sesionId;
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.horaInicio = Objects.requireNonNull(horaInicio, "horaInicio");
        this.horaFin = Objects.requireNonNull(horaFin, "horaFin");
    }

    public long sesionId() {
        return sesionId;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public int colorMateria() {
        return colorMateria;
    }

    public Hora horaInicio() {
        return horaInicio;
    }

    public Hora horaFin() {
        return horaFin;
    }
}
