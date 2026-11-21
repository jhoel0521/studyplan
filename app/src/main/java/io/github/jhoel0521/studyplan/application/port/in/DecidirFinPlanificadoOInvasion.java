package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Puerto de entrada: ejecutar la decisión del estudiante ante el fin planificado, la extensión de
 * Pomodoro o la invasión de la siguiente sesión (HU-23, HU-42, HU-43, RN-25, RN-27).
 */
public interface DecidirFinPlanificadoOInvasion {

    enum DecisionFinOInvasion {
        SIGO_ESTUDIANDO,
        MAS_UN_POMODORO,
        TIEMPO_EXTRA_LIBRE,
        TERMINE,
        CAMBIAR_A_SIGUIENTE
    }

    Resultado<SesionEstudio> ejecutar(
            long sesionActualId, DecisionFinOInvasion decision, Long siguienteSesionId);
}
