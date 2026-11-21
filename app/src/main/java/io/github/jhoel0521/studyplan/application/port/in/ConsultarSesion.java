package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;

/**
 * Puerto de entrada (caso de uso): consultar el detalle de una sesión con sus acciones permitidas
 * por la máquina de estados y enlace de reprogramación (HU-12 a HU-15, RN-07, RN-09, RN-10, RN-12).
 */
public interface ConsultarSesion {

    Optional<DetalleSesion> ejecutar(long sesionId);
}
