package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;

/**
 * Puerto de entrada (caso de uso): reprogramar una sesión {@code PENDIENTE} o
 * {@code NO_REALIZADA} a otra fecha y horario conservando el historial (HU-14, RN-04, RN-05, RN-09).
 */
public interface ReprogramarSesion {

    ResultadoSesion ejecutar(
            long sesionId, Fecha nuevaFecha, Hora nuevaHoraInicio, Hora nuevaHoraFin, String notas);
}
