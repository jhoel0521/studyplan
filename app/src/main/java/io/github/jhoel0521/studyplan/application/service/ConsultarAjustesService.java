package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarAjustes;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;

/**
 * Implementa {@link ConsultarAjustes} leyendo del repositorio.
 *
 * <p>Implementa: HU-03 y HU-04.
 */
public final class ConsultarAjustesService implements ConsultarAjustes {

    private final AjustesRepository repositorio;

    public ConsultarAjustesService(AjustesRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Ajustes ejecutar() {
        return repositorio.obtener();
    }
}
