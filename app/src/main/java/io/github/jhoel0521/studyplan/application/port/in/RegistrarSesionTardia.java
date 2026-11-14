package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Puerto de entrada (caso de uso): registrar tardíamente una sesión {@code NO_REALIZADA} de hace
 * 3 días o menos pasándola a {@code COMPLETADA_TARDIA} (HU-13, RN-10, RN-26).
 */
public interface RegistrarSesionTardia {

    Resultado<SesionEstudio> ejecutar(long sesionId, int duracionRealMinutos);
}
