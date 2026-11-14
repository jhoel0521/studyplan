package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Elemento de lectura para representar una sesión en la pantalla de Inicio junto con el nombre y
 * color de su materia (HU-02, RNF-06).
 */
public final class DetalleSesionInicio {

    private final long sesionId;
    private final long materiaId;
    private final String nombreMateria;
    private final int colorMateria;
    private final Fecha fecha;
    private final Hora horaInicio;
    private final Hora horaFin;
    private final int duracionPlanificadaMinutos;
    private final Integer duracionRealMinutos;
    private final CodigoEstadoSesion estado;

    public DetalleSesionInicio(
            long sesionId,
            long materiaId,
            String nombreMateria,
            int colorMateria,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            CodigoEstadoSesion estado) {
        this.sesionId = sesionId;
        this.materiaId = materiaId;
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.horaInicio = Objects.requireNonNull(horaInicio, "horaInicio");
        this.horaFin = Objects.requireNonNull(horaFin, "horaFin");
        this.duracionPlanificadaMinutos = duracionPlanificadaMinutos;
        this.duracionRealMinutos = duracionRealMinutos;
        this.estado = Objects.requireNonNull(estado, "estado");
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

    public int colorMateria() {
        return colorMateria;
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

    public int duracionPlanificadaMinutos() {
        return duracionPlanificadaMinutos;
    }

    public Integer duracionRealMinutos() {
        return duracionRealMinutos;
    }

    public CodigoEstadoSesion estado() {
        return estado;
    }
}
