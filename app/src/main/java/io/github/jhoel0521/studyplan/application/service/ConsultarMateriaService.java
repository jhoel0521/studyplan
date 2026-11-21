package io.github.jhoel0521.studyplan.application.service;

import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.materias.ConsultarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.entity.Materia;

/**
 * Implementa {@link ConsultarMateria}.
 *
 * <p>Implementa: HU-07.
 */
public final class ConsultarMateriaService implements ConsultarMateria {

    private final MateriaRepository repositorio;

    public ConsultarMateriaService(MateriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Materia> ejecutar(long id) {
        return repositorio.obtener(id);
    }
}
