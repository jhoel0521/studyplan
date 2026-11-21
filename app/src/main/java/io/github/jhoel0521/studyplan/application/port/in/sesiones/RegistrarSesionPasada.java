package io.github.jhoel0521.studyplan.application.port.in.sesiones;

import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;

/**
 * Puerto de entrada (caso de uso): registrar una sesión estudiada dentro de los últimos 3 días
 * directamente en estado {@code COMPLETADA_TARDIA} (HU-11, RN-04, RN-05, RN-06, RN-26).
 */
public interface RegistrarSesionPasada {

    ResultadoSesion ejecutar(
            long materiaId,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            int duracionRealMinutos,
            String notas);
}
