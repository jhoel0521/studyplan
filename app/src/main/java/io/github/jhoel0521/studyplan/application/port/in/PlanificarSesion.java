package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;

/**
 * Puerto de entrada (operación de aplicación): planificar una nueva sesión de estudio
 * (HU-10, HU-34, RN-04, RN-05).
 */
public interface PlanificarSesion {

    ResultadoSesion ejecutar(
            long materiaId, Fecha fecha, Hora horaInicio, Hora horaFin, String notas);

    ResultadoSesion ejecutar(
            long materiaId,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            String notas,
            AntelacionRecordatorio antelacion);
}
