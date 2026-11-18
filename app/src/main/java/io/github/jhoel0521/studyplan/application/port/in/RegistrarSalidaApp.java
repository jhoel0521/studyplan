package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.SalidaRegistrada;

/**
 * Puerto de entrada: registrar la salida de la app cuando hay una sesión {@code EN_CURSO} fuera
 * de descanso Pomodoro (HU-40, RN-23).
 */
public interface RegistrarSalidaApp {

    /** @return datos de la sesión y materia si se registró salida, o vacío si no aplica */
    Optional<SalidaRegistrada> ejecutar();
}
