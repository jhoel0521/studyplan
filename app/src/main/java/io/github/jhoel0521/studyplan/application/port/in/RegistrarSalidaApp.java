package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

/**
 * Puerto de entrada: registrar la salida de la app cuando hay una sesión {@code EN_CURSO}
 * (HU-40, RN-23).
 */
public interface RegistrarSalidaApp {

    /** @return identificador de la sesión activa si se registró salida, o vacío si no aplica */
    Optional<Long> ejecutar();
}
