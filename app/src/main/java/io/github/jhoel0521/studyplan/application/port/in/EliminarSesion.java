package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Puerto de entrada (caso de uso): eliminar una sesión únicamente si está en estado
 * {@code PENDIENTE} (HU-15, RN-12).
 */
public interface EliminarSesion {

    Resultado<SesionEstudio> ejecutar(long sesionId);
}
