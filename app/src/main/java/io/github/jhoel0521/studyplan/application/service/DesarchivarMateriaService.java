package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.DesarchivarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Implementa {@link DesarchivarMateria}: la materia vuelve a las activas solo si ninguna otra activa usa ya su nombre.
 *
 * <p>Implementa: HU-46 y RN-02.
 */
public final class DesarchivarMateriaService implements DesarchivarMateria {

    private final MateriaRepository repositorio;

    public DesarchivarMateriaService(MateriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Resultado<Materia> ejecutar(long id) {
        Materia actual = repositorio.obtener(id).orElse(null);
        if (actual == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        if (!actual.archivada()) {
            return Resultado.error(ErrorValidacion.MATERIA_YA_ACTIVA);
        }
        if (NombresDeMateria.hayActivaConClave(repositorio, actual.clave(), id)) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        return Resultado.ok(repositorio.guardar(actual.conArchivada(false)));
    }
}
