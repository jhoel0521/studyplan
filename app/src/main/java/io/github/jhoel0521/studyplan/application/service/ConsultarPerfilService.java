package io.github.jhoel0521.studyplan.application.service;

import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarPerfil;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.domain.Perfil;

/**
 * Implementa el caso de uso {@link ConsultarPerfil} leyendo el perfil del repositorio.
 *
 * <p>Implementa: HU-01 y HU-02.
 */
public final class ConsultarPerfilService implements ConsultarPerfil {

    private final PerfilRepository repositorio;

    public ConsultarPerfilService(PerfilRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Perfil> ejecutar() {
        return repositorio.obtener();
    }
}
