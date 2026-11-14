package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link VencerSesionesPendientes}: pasa a {@code NO_REALIZADA} toda sesión
 * {@code PENDIENTE} anterior a hoy sin tocar sesiones {@code EN_CURSO} (HU-13, RN-08, RN-29).
 */
public final class VencerSesionesPendientesService implements VencerSesionesPendientes {

    private final SesionRepository sesiones;
    private final Reloj reloj;

    public VencerSesionesPendientesService(SesionRepository sesiones, Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public int ejecutar() {
        Fecha hoy = reloj.hoy();
        List<SesionEstudio> candidatas = sesiones.listarPendientesAnterioresA(hoy);
        int vencidas = 0;
        for (SesionEstudio sesion : candidatas) {
            if (!sesion.fecha().esAnteriorA(hoy)) {
                continue;
            }
            Resultado<SesionEstudio> transicion = sesion.vencer();
            if (transicion.esValido()) {
                sesiones.actualizar(transicion.valor());
                vencidas++;
            }
        }
        return vencidas;
    }
}
