package io.github.jhoel0521.studyplan.application.port.out;

/**
 * Puerto de salida ligero para persistir la marca de salida de la app de una sesión en curso
 * (HU-40, HU-41, RN-23, RN-29).
 */
public interface MarcadorSalidaApp {

    void marcarSalida(long sesionId, long instanteSalidaMilis);

    Long obtenerSesionActiva();

    Long obtenerInstanteSalida();

    void limpiar();
}
