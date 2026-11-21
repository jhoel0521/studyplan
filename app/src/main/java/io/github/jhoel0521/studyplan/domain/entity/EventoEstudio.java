package io.github.jhoel0521.studyplan.domain.entity;

import java.util.Objects;
import java.util.Optional;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

/**
 * Evento inmutable registrado durante la ejecución en vivo de una sesión de estudio
 * (HU-39, RN-22 a RN-29, RN-30).
 */
public final class EventoEstudio {

    public static final int LONGITUD_MAXIMA_NOTA = 500;

    private final Long id;
    private final long sesionId;
    private final TipoEventoEstudio tipo;
    private final long instanteMilis;
    private final int desfaseMinutos;
    private final String nota;
    private final TopicoEventoEstudio topico;

    private EventoEstudio(
            Long id,
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            int desfaseMinutos,
            String nota,
            TopicoEventoEstudio topico) {
        this.id = id;
        this.sesionId = sesionId;
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.instanteMilis = instanteMilis;
        this.desfaseMinutos = desfaseMinutos;
        this.nota = nota == null ? "" : nota;
        this.topico = topico;
    }

    /** Crea un evento a partir de un {@link Instante} validando la nota (HU-39). */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            Instante instante,
            String nota) {
        return crear(sesionId, tipo, instante, nota, null);
    }

    /** Crea un evento a partir de un {@link Instante} con tópico opcional (HU-39, RN-30). */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            Instante instante,
            String nota,
            TopicoEventoEstudio topico) {
        if (instante == null) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_OBLIGATORIA);
        }
        return crear(sesionId, tipo, instante.utcMilis(), instante.desfaseMinutos(), nota, topico);
    }

    /** Crea un evento con marca UTC y desfase horario validando la nota (HU-39). */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            int desfaseMinutos,
            String nota) {
        return crear(sesionId, tipo, instanteMilis, desfaseMinutos, nota, null);
    }

    /** Crea un evento con marca UTC, desfase horario y tópico opcional (HU-39, RN-30). */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            int desfaseMinutos,
            String nota,
            TopicoEventoEstudio topico) {
        if (sesionId <= 0) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        String saneada = SesionEstudio.sanearNotas(nota);
        if (saneada.length() > LONGITUD_MAXIMA_NOTA) {
            return Resultado.error(ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS);
        }
        return Resultado.ok(new EventoEstudio(
                null, sesionId, tipo, instanteMilis, desfaseMinutos, saneada, topico));
    }

    /** Crea un evento calculando el desfase desde su fecha y hora locales respecto a {@code instanteMilis}. */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            Fecha fecha,
            Hora hora,
            String nota) {
        return crear(sesionId, tipo, instanteMilis, fecha, hora, nota, null);
    }

    /** Crea un evento con fecha/hora local y tópico opcional (HU-39, RN-30). */
    public static Resultado<EventoEstudio> crear(
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            Fecha fecha,
            Hora hora,
            String nota,
            TopicoEventoEstudio topico) {
        int desfase = calcularDesfaseMinutos(instanteMilis, fecha, hora);
        return crear(sesionId, tipo, instanteMilis, desfase, nota, topico);
    }

    /** Reconstruye un evento persistido desde base de datos v5. */
    public static EventoEstudio restaurar(
            Long id,
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            int desfaseMinutos,
            String nota) {
        return restaurar(id, sesionId, tipo, instanteMilis, desfaseMinutos, nota, null);
    }

    /** Reconstruye un evento persistido desde base de datos v7 con columna {@code topico}. */
    public static EventoEstudio restaurar(
            Long id,
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            int desfaseMinutos,
            String nota,
            String topicoClave) {
        return new EventoEstudio(
                id,
                sesionId,
                tipo,
                instanteMilis,
                desfaseMinutos,
                SesionEstudio.sanearNotas(nota),
                TopicoEventoEstudio.desdeClave(topicoClave).orElse(null));
    }

    /** Reconstruye un evento persistido desde base de datos v4. */
    public static EventoEstudio restaurar(
            Long id,
            long sesionId,
            TipoEventoEstudio tipo,
            long instanteMilis,
            Fecha fecha,
            Hora hora,
            String nota) {
        return restaurar(
                id,
                sesionId,
                tipo,
                instanteMilis,
                calcularDesfaseMinutos(instanteMilis, fecha, hora),
                nota);
    }

    private static int calcularDesfaseMinutos(long instanteMilis, Fecha fecha, Hora hora) {
        if (fecha == null || hora == null) {
            return 0;
        }
        long localMilis = fecha.diasDesdeEpoca() * Instante.MILIS_POR_DIA
                + hora.minutosDesdeMedianoche() * Instante.MILIS_POR_MINUTO;
        long diffMin = Math.round((localMilis - instanteMilis) / (double) Instante.MILIS_POR_MINUTO);
        return (int) Math.max(
                Instante.DESFASE_MINIMO_MINUTOS,
                Math.min(Instante.DESFASE_MAXIMO_MINUTOS, diffMin));
    }

    public EventoEstudio conId(long nuevoId) {
        return new EventoEstudio(nuevoId, sesionId, tipo, instanteMilis, desfaseMinutos, nota, topico);
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

    public int desfaseMinutos() {
        return desfaseMinutos;
    }

    public Instante instante() {
        return Instante.de(instanteMilis, desfaseMinutos);
    }

    public Fecha fecha() {
        return instante().fechaLocal();
    }

    public Hora hora() {
        return instante().horaLocal();
    }

    public String nota() {
        return nota;
    }

    public Optional<TopicoEventoEstudio> topico() {
        return Optional.ofNullable(topico);
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
                && desfaseMinutos == otro.desfaseMinutos
                && Objects.equals(id, otro.id)
                && tipo == otro.tipo
                && nota.equals(otro.nota)
                && topico == otro.topico;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sesionId, tipo, instanteMilis, desfaseMinutos, nota, topico);
    }
}
