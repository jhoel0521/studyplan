package io.github.jhoel0521.studyplan.domain;

/**
 * Estado final {@code COMPLETADA} del patrón State para {@link SesionEstudio}
 * (HU-12, RN-07, RN-11).
 */
public final class EstadoCompletada implements EstadoSesion {

    public static final EstadoCompletada INSTANCIA = new EstadoCompletada();

    private EstadoCompletada() {
    }

    @Override
    public CodigoEstadoSesion codigo() {
        return CodigoEstadoSesion.COMPLETADA;
    }

    @Override
    public boolean esFinal() {
        return true;
    }

    @Override
    public boolean sumaHorasReales() {
        return true;
    }
}
