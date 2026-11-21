package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConsultarEstadoArranque;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.domain.enumeration.EstadoArranque;

/**
 * Implementa {@link ConsultarEstadoArranque}: lee los ajustes (lo que desbloquea la sesión si el bloqueo está
 * inactivo) y decide qué pantalla corresponde. Sin perfil gana el primer uso, porque no hay datos que proteger.
 *
 * <p>Implementa: HU-01 y HU-04.
 */
public final class ConsultarEstadoArranqueService implements ConsultarEstadoArranque {

    private final PerfilRepository perfil;
    private final AjustesRepository ajustes;
    private final SesionBloqueo sesion;

    public ConsultarEstadoArranqueService(PerfilRepository perfil, AjustesRepository ajustes, SesionBloqueo sesion) {
        this.perfil = perfil;
        this.ajustes = ajustes;
        this.sesion = sesion;
    }

    @Override
    @SuppressWarnings("SimplifyOptionalCallChains") // Optional.isEmpty() no existe antes de Android 13 (minSdk 24)
    public EstadoArranque ejecutar() {
        sesion.alCambiarBloqueo(ajustes.obtener().bloqueoActivo());
        if (!perfil.obtener().isPresent()) {
            return EstadoArranque.SIN_PERFIL;
        }
        return sesion.estaBloqueada() ? EstadoArranque.BLOQUEADA : EstadoArranque.LISTA;
    }
}
