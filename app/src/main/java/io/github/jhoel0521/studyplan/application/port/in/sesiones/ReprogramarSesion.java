package io.github.jhoel0521.studyplan.application.port.in.sesiones;

import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;

/**
 * Puerto de entrada (operación de aplicación): reprogramar una sesión {@code PENDIENTE} o
 * {@code NO_REALIZADA} a otra fecha y horario conservando el historial (HU-14, HU-34, RN-04,
 * RN-05, RN-09).
 */
public interface ReprogramarSesion {

    ResultadoSesion ejecutar(
            long sesionId, Fecha nuevaFecha, Hora nuevaHoraInicio, Hora nuevaHoraFin, String notas);

    ResultadoSesion ejecutar(
            long sesionId,
            Fecha nuevaFecha,
            Hora nuevaHoraInicio,
            Hora nuevaHoraFin,
            String notas,
            AntelacionRecordatorio nuevaAntelacion);
}
