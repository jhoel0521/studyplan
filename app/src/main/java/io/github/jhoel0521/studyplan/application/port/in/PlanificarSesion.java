package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;

/**
 * Puerto de entrada (caso de uso): planificar una nueva sesión de estudio (HU-10, RN-04, RN-05).
 */
public interface PlanificarSesion {

    ResultadoSesion ejecutar(
            long materiaId, Fecha fecha, Hora horaInicio, Hora horaFin, String notas);
}
