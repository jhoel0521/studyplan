package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Evento inmutable registrado durante la ejecución en vivo de una sesión de estudio
 * (HU-39, RN-22 a RN-29).
 */
public final class EventoEstudio {

    public static final int LONGITUD_MAXIMA_NOTA = 500;

    private final Long id;
    private final long sesionId;
    private final TipoEventoEstudio tipo;
    private final long instanteMilis;
    private final Fecha fecha;
    private final Hora hora;
    private final String nota;

    private EventoEstudio(
            Long id,
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            Fecha fecha,
            Hora hora,
            String nota) {
        this.id = id;
        this.sesionId = sesionId;
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.instanteMilis = instanteMilis;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.hora = Objects.requireNonNull(hora, "hora");
        this.nota = nota == null ? "" : nota;
    }

    /** Crea un evento validando que la nota no supere 500 caracteres. */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            Fecha fecha,
            Hora hora,
            String nota) {
        if (sesionId <= 0) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        String saneada = SesionEstudio.sanearNotas(nota);
        if (saneada.length() > LONGITUD_MAXIMA_NOTA) {
            return Resultado.error(ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS);
        }
        return Resultado.ok(new EventoEstudio(null, sesionId, tipo, instanteMilis, fecha, hora, saneada));
    }

    /** Reconstruye un evento persistido desde base de datos. */
    public static EventoEstudio restaurar(
            Long id,
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            Fecha fecha,
            Hora hora,
            String nota) {
        return new EventoEstudio(
                id, sesionId, tipo, instanteMilis, fecha, hora, SesionEstudio.sanearNotas(nota));
    }

    public EventoEstudio conId(long nuevoId) {
        return new EventoEstudio(nuevoId, sesionId, tipo, instanteMilis, fecha, hora, nota);
    }

    public Long id() {
        return id;
    }

    public long sesionId() {
        return sesionId;
    }

    public TipoEventoEstudio tipo() {
        return tipo;
    }

    public long instanteMilis() {
        return instanteMilis;
    }

    public Fecha fecha() {
        return fecha;
    }

    public Hora hora() {
        return hora;
    }

    public String nota() {
        return nota;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventoEstudio)) {
            return false;
        }
        EventoEstudio otro = (EventoEstudio) o;
        return sesionId == otro.sesionId
                && instanteMilis == otro.instanteMilis
                && Objects.equals(id, otro.id)
                && tipo == otro.tipo
                && fecha.equals(otro.fecha)
                && hora.equals(otro.hora)
                && nota.equals(otro.nota);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sesionId, tipo, instanteMilis, fecha, hora, nota);
    }
}
