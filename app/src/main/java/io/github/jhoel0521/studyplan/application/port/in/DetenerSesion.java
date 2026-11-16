package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Puerto de entrada: detener una sesión en curso y materializar su duración real (HU-22, RN-07, RN-26).
 */
public interface DetenerSesion {

    Resultado<SesionEstudio> ejecutar(long sesionId);
}
