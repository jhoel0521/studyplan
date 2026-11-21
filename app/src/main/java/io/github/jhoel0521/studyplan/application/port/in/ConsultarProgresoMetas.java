package io.github.jhoel0521.studyplan.application.port.in;

import java.util.List;

import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;

/**
 * Caso de uso: consultar el progreso actual y el historial semanal de metas ({@code HU-27},
 * {@code RN-31}, {@code RN-33}).
 */
public interface ConsultarProgresoMetas {

    /** Progreso de la meta total en la semana actual. */
    ProgresoMeta progresoSemanaActualTotal();

    /** Historial semanal (semana actual y anteriores registradas), ordenado de más reciente a más antigua. */
    List<ProgresoMeta> historialMetas();
}
