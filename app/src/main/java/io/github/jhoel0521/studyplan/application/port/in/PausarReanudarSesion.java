package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Puerto de entrada: pausar o reanudar una sesión en curso (HU-22, RN-07, RN-18).
 */
public interface PausarReanudarSesion {

    Resultado<SesionEstudio> ejecutar(long sesionId);
}
