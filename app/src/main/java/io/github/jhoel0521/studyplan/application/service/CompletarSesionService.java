package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.CompletarSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link CompletarSesion}: completa una sesión {@code PENDIENTE} del día actual
 * guardando la duración real (HU-12, RN-07, RN-26, RN-31).
 */
public final class CompletarSesionService implements CompletarSesion {

    private final SesionRepository sesiones;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public CompletarSesionService(SesionRepository sesiones, Reloj reloj) {
        this(sesiones, null, reloj);
    }

    public CompletarSesionService(
            SesionRepository sesiones, TramoDiarioRepository tramos, Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.tramos = tramos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(long sesionId, int duracionRealMinutos) {
        SesionEstudio sesion = sesiones.buscarPorId(sesionId).orElse(null);
        if (sesion == null) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Resultado<SesionEstudio> completada = sesion.completar(reloj.hoy(), duracionRealMinutos);
        if (!completada.esValido()) {
            return completada;
        }
        sesiones.actualizar(completada.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    sesionId,
                    RepartidorPorDia.repartir(completada.valor(), null, 0L, duracionRealMinutos));
        }
        return completada;
    }
}
