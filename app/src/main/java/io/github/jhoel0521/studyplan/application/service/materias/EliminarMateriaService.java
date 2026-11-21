package io.github.jhoel0521.studyplan.application.service.materias;

import io.github.jhoel0521.studyplan.application.port.in.materias.EliminarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.SesionesDeMateria;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

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
        Resultado<Materia> eliminable = actual.validarEliminacion();
        if (!eliminable.esValido()) {
            return eliminable;
        }
        if (sesiones.contar(id) > 0) {
            return Resultado.error(ErrorValidacion.MATERIA_CON_SESIONES);
        }
        repositorio.eliminar(id);
        return Resultado.ok(actual);
    }
}
