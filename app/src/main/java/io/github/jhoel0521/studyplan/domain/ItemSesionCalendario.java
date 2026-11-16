package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Proyección inmutable de una sesión de estudio enriquecida con datos de su materia para el
 * calendario (HU-16 a HU-21, RNF-06).
 */
public final class ItemSesionCalendario {

    private final long sesionId;
    private final long materiaId;
    private final String nombreMateria;
    private final int colorMateria;
    private final boolean materiaArchivada;
    private final Fecha fecha;
    private final Hora horaInicio;
    private final Hora horaFin;
    private final int duracionPlanificadaMinutos;
    private final Integer duracionRealMinutos;
    private final EstadoSesion estado;

    public ItemSesionCalendario(
            long sesionId,
            long materiaId,
            String nombreMateria,
            int colorMateria,
            boolean materiaArchivada,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            EstadoSesion estado) {
        this.sesionId = sesionId;
        this.materiaId = materiaId;
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.materiaArchivada = materiaArchivada;
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

    public boolean materiaArchivada() {
        return materiaArchivada;
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

    public EstadoSesion estado() {
        return estado;
    }

    public CodigoEstadoSesion codigoEstado() {
        return estado.codigo();
    }
}
