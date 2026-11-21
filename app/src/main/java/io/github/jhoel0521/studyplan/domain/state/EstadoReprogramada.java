package io.github.jhoel0521.studyplan.domain.state;

import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Estado final {@code REPROGRAMADA} del patrón State para {@link SesionEstudio}
 * (HU-14, RN-05, RN-07, RN-09).
 */
public final class EstadoReprogramada implements EstadoSesion {

    public static final EstadoReprogramada INSTANCIA = new EstadoReprogramada();

    private EstadoReprogramada() {
    }

    @Override
    public CodigoEstadoSesion codigo() {
        return CodigoEstadoSesion.REPROGRAMADA;
    }

    @Override
    public boolean esFinal() {
        return true;
    }

    @Override
    public boolean ocupaHorario() {
        return false;
    }
}
