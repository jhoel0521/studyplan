package io.github.jhoel0521.studyplan.adapter.in.ui.sesiones;

import androidx.annotation.Nullable;

import io.github.jhoel0521.studyplan.domain.readmodel.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;

/**
 * Evento único resultante de una acción sobre una sesión de estudio (formulario o detalle).
 *
 * <p>Implementa: HU-10, HU-11, HU-12, HU-13, HU-14 y HU-15.
 */
public final class EventoSesion {

    /** Tipos de resultado posibles en las pantallas de sesión. */
    public enum Tipo {
        PLANIFICADA,
        PASADA_REGISTRADA,
        REPROGRAMADA,
        INICIADA,
        COMPLETADA,
        TARDIA_REGISTRADA,
        ELIMINADA,
        ERROR
    }

    private final Tipo tipo;
    @Nullable
    private final Long sesionId;
    @Nullable
    private final ErrorValidacion error;
    @Nullable
    private final ConflictoHorario conflicto;

    private EventoSesion(
            Tipo tipo,
            @Nullable Long sesionId,
            @Nullable ErrorValidacion error,
            @Nullable ConflictoHorario conflicto) {
        this.tipo = tipo;
        this.sesionId = sesionId;
        this.error = error;
        this.conflicto = conflicto;
    }

    public static EventoSesion ok(Tipo tipo) {
        return new EventoSesion(tipo, null, null, null);
    }

    public static EventoSesion ok(Tipo tipo, long sesionId) {
        return new EventoSesion(tipo, sesionId, null, null);
    }

    public static EventoSesion error(ErrorValidacion error) {
        return new EventoSesion(Tipo.ERROR, null, error, null);
    }

    public static EventoSesion fallo(ResultadoSesion resultado) {
        return new EventoSesion(Tipo.ERROR, null, resultado.error(), resultado.conflicto());
    }

    public Tipo tipo() {
        return tipo;
    }

    @Nullable
    public Long sesionId() {
        return sesionId;
    }

    @Nullable
    public ErrorValidacion error() {
        return error;
    }

    @Nullable
    public ConflictoHorario conflicto() {
        return conflicto;
    }
}
