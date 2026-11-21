package io.github.jhoel0521.studyplan.application.service.configuracion;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarBloqueo;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.DisponibilidadBloqueo;

/**
 * Implementa {@link ConfigurarBloqueo}: solo activa el bloqueo si el teléfono tiene un método de bloqueo.
 *
 * <p>Implementa: HU-04 (escenarios «Activar bloqueo» y «Sin método de bloqueo») y RNF-12.
 */
public final class ConfigurarBloqueoService implements ConfigurarBloqueo {

    private final AjustesRepository ajustes;
    private final DisponibilidadBloqueo disponibilidad;
    private final SesionBloqueo sesion;

    public ConfigurarBloqueoService(AjustesRepository ajustes, DisponibilidadBloqueo disponibilidad,
                                    SesionBloqueo sesion) {
        this.ajustes = ajustes;
        this.disponibilidad = disponibilidad;
        this.sesion = sesion;
    }

    @Override
    public ResultadoActivacion activar() {
        if (!disponibilidad.estaDisponible()) {
            return ResultadoActivacion.SIN_METODO_DE_BLOQUEO;
        }
        ajustes.guardar(ajustes.obtener().conBloqueoActivo(true));
        sesion.alCambiarBloqueo(true);
        return ResultadoActivacion.ACTIVADO;
    }

    @Override
    public void desactivar() {
        ajustes.guardar(ajustes.obtener().conBloqueoActivo(false));
        sesion.alCambiarBloqueo(false);
    }
}
