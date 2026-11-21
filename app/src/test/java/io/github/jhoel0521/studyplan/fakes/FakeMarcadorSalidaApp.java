package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.MarcadorSalidaApp;

/**
 * Falso de {@link MarcadorSalidaApp} para pruebas unitarias (HU-40, HU-41).
 */
public final class FakeMarcadorSalidaApp implements MarcadorSalidaApp {

    private Long sesionId;
    private Long instanteSalida;
    private Long sesionDescansoId;
    private Long finDescansoMilis;

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
    public void marcarFinDescansoPomodoro(long sesionId, long finDescansoMilis) {
        this.sesionDescansoId = sesionId;
        this.finDescansoMilis = finDescansoMilis;
    }

    @Override
    public Long obtenerSesionDescansoPomodoro() {
        return sesionDescansoId;
    }

    @Override
    public Long obtenerFinDescansoPomodoro() {
        return finDescansoMilis;
    }

    @Override
    public void limpiarFinDescansoPomodoro() {
        this.sesionDescansoId = null;
        this.finDescansoMilis = null;
    }

    @Override
    public void limpiar() {
        this.sesionId = null;
        this.instanteSalida = null;
        this.sesionDescansoId = null;
        this.finDescansoMilis = null;
    }
}
