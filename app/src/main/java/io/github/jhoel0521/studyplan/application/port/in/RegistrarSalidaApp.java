package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.SalidaRegistrada;

/**
 * Puerto de entrada: registrar la salida de la app cuando hay una sesión {@code EN_CURSO} fuera
 * de descanso Pomodoro (HU-40, RN-23).
 */
public interface RegistrarSalidaApp {

    /**
     * Registra la salida con la hora en que ocurrió, que puede ser anterior a la actual porque la salida se
     * confirma tras la ventana de gracia de 1 minuto (HU-40, RN-23).
     *
     * @param instanteSalidaMs instante real de la salida, en milisegundos
     * @return datos de la sesión y materia si se registró salida, o vacío si no aplica
     */
    Optional<SalidaRegistrada> ejecutar(long instanteSalidaMs);
}
