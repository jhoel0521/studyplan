package io.github.jhoel0521.studyplan.application.port.in.sesiones;

import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;

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
