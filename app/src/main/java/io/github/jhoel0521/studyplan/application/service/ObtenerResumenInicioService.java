package io.github.jhoel0521.studyplan.application.service;

import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;

/**
 * Implementa, de forma parcial, {@link ObtenerResumenInicio}: nombre, horas realizadas y meta. Como aún no existen
 * sesiones, las horas realizadas son 0. Quedan pendientes los escenarios «Resumen con datos» y «Acceso rápido» de HU-02.
 *
 * <p>Implementa: HU-02 y RN-15 (la meta es la predeterminada del perfil).
 */
public final class ObtenerResumenInicioService implements ObtenerResumenInicio {

    /** Hasta que existan sesiones no hay horas realizadas. */
    static final double HORAS_REALIZADAS_SIN_SESIONES = 0;

    private final PerfilRepository perfiles;

    public ObtenerResumenInicioService(PerfilRepository perfiles) {
        this.perfiles = perfiles;
    }

    @Override
    public Optional<ResumenInicio> ejecutar() {
        return perfiles.obtener().map(p ->
                new ResumenInicio(p.nombre(), HORAS_REALIZADAS_SIN_SESIONES, p.metaSemanalHoras()));
    }
}
