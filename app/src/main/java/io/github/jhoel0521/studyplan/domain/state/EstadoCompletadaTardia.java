package io.github.jhoel0521.studyplan.domain.state;

import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Estado final {@code COMPLETADA_TARDIA} del patrón State para {@link SesionEstudio}
 * (HU-11, HU-13, RN-06, RN-07, RN-10, RN-11).
 */
public final class EstadoCompletadaTardia implements EstadoSesion {

    public static final EstadoCompletadaTardia INSTANCIA = new EstadoCompletadaTardia();

    private EstadoCompletadaTardia() {
    }

    @Override
    public CodigoEstadoSesion codigo() {
        return CodigoEstadoSesion.COMPLETADA_TARDIA;
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
