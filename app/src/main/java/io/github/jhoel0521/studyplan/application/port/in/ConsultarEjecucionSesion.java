package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.readmodel.ResumenEjecucionSesion;

/**
 * Puerto de entrada: consultar el estado calculado en vivo de una sesión en curso (HU-22, HU-23, HU-42, HU-43).
 */
public interface ConsultarEjecucionSesion {

    Optional<ResumenEjecucionSesion> ejecutar(long sesionId);
}
