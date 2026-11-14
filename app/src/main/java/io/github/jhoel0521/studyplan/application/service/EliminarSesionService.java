package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.EliminarSesion;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link EliminarSesion}: permite borrar únicamente sesiones en estado
 * {@code PENDIENTE} según RN-12 (HU-15).
 */
public final class EliminarSesionService implements EliminarSesion {

    private final SesionRepository sesiones;

    public EliminarSesionService(SesionRepository sesiones) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(long sesionId) {
        SesionEstudio sesion = sesiones.buscarPorId(sesionId).orElse(null);
        if (sesion == null) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Resultado<SesionEstudio> validacion = sesion.validarEliminacion();
        if (!validacion.esValido()) {
            return validacion;
        }
        sesiones.eliminarPorId(sesionId);
        return Resultado.ok(sesion);
    }
}
