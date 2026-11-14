package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Puerto de entrada (caso de uso): marcar como completada una sesión pendiente del mismo día
 * guardando la duración real (HU-12, RN-07, RN-26).
 */
public interface CompletarSesion {

    Resultado<SesionEstudio> ejecutar(long sesionId, int duracionRealMinutos);
}
