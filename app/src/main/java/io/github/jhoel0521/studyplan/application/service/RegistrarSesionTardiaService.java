package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarSesionTardia;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link RegistrarSesionTardia}: convierte una sesión {@code NO_REALIZADA} de hace
 * 3 días o menos en {@code COMPLETADA_TARDIA} (HU-13, RN-07, RN-10, RN-26, RN-31).
 */
public final class RegistrarSesionTardiaService implements RegistrarSesionTardia {

    private final SesionRepository sesiones;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public RegistrarSesionTardiaService(SesionRepository sesiones, Reloj reloj) {
        this(sesiones, null, reloj);
    }

    public RegistrarSesionTardiaService(
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
        Resultado<SesionEstudio> tardia =
                sesion.registrarTardio(reloj.hoy(), duracionRealMinutos);
        if (!tardia.esValido()) {
            return tardia;
        }
        sesiones.actualizar(tardia.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    sesionId,
                    RepartidorPorDia.repartir(tardia.valor(), null, 0L, duracionRealMinutos));
        }
        return tardia;
    }
}
