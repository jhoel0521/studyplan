package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.EliminarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.SesionesDeMateria;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Implementa {@link EliminarMateria}: una materia con sesiones no se elimina (RN-03), solo se archiva.
 *
 * <p>Implementa: HU-08 (Eliminar protegida y Eliminar sin historial) y RN-03.
 */
public final class EliminarMateriaService implements EliminarMateria {

    private final MateriaRepository repositorio;
    private final SesionesDeMateria sesiones;

    public EliminarMateriaService(MateriaRepository repositorio, SesionesDeMateria sesiones) {
        this.repositorio = repositorio;
        this.sesiones = sesiones;
    }

    @Override
    public Resultado<Materia> ejecutar(long id) {
        Materia actual = repositorio.obtener(id).orElse(null);
        if (actual == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        if (sesiones.contar(id) > 0) {
            return Resultado.error(ErrorValidacion.MATERIA_CON_SESIONES);
        }
        repositorio.eliminar(id);
        return Resultado.ok(actual);
    }
}
