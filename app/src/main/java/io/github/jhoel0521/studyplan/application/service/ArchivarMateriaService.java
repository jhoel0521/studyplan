package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.ArchivarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Implementa {@link ArchivarMateria}. Archivar una materia ya archivada no falla.
 *
 * <p>Implementa: HU-08.
 */
public final class ArchivarMateriaService implements ArchivarMateria {

    private final MateriaRepository repositorio;

    public ArchivarMateriaService(MateriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Resultado<Materia> ejecutar(long id) {
        Materia actual = repositorio.obtener(id).orElse(null);
        if (actual == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        Resultado<Materia> archivada = actual.intentarArchivar(true);
        if (!archivada.esValido()) {
            return archivada;
        }
        return Resultado.ok(actual.archivada() ? actual : repositorio.guardar(archivada.valor()));
    }
}
