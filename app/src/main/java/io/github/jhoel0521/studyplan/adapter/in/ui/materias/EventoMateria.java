package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;

/**
 * Resultado de una acción del formulario de materia: qué se hizo o el código de error que lo impidió.
 *
 * <p>Implementa: HU-06, HU-07, HU-08 y HU-46.
 */
public final class EventoMateria {

    /** Acciones del formulario. */
    public enum Tipo { GUARDADA, ARCHIVADA, DESARCHIVADA, ELIMINADA, ERROR }

    private final Tipo tipo;
    private final ErrorValidacion error;

    private EventoMateria(Tipo tipo, ErrorValidacion error) {
        this.tipo = tipo;
        this.error = error;
    }

    public static EventoMateria de(Tipo tipo) {
        return new EventoMateria(tipo, null);
    }

    public static EventoMateria error(ErrorValidacion error) {
        return new EventoMateria(Tipo.ERROR, error);
    }

    public Tipo tipo() {
        return tipo;
    }

    /** @return el código de error, o {@code null} si la acción salió bien */
    public ErrorValidacion error() {
        return error;
    }
}
