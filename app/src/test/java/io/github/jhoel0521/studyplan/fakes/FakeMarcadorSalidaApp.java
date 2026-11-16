package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;

/**
 * Falso de {@link MarcadorSalidaApp} para pruebas unitarias (HU-40, HU-41).
 */
public final class FakeMarcadorSalidaApp implements MarcadorSalidaApp {

    private Long sesionId;
    private Long instanteSalida;

    @Override
    public void marcarSalida(long sesionId, long instanteSalidaMilis) {
        this.sesionId = sesionId;
        this.instanteSalida = instanteSalidaMilis;
    }

    @Override
    public Long obtenerSesionActiva() {
        return sesionId;
    }

    @Override
    public Long obtenerInstanteSalida() {
        return instanteSalida;
    }

    @Override
    public void limpiar() {
        this.sesionId = null;
        this.instanteSalida = null;
    }
}
