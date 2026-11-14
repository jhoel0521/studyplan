package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.EditarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Implementa {@link EditarMateria}: conserva el id y el estado de archivada. Si la materia está activa, el nuevo
 * nombre no puede coincidir con el de otra activa (RN-02); una archivada puede repetir nombres.
 *
 * <p>Implementa: HU-07, RN-02 y RNF-14.
 */
public final class EditarMateriaService implements EditarMateria {

    private final MateriaRepository repositorio;

    public EditarMateriaService(MateriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Resultado<Materia> ejecutar(long id, String nombre, int color, String docente) {
        Materia actual = repositorio.obtener(id).orElse(null);
        if (actual == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        Resultado<Materia> editada = actual.editar(nombre, color, docente);
        if (!editada.esValido()) {
            return editada;
        }
        if (!actual.archivada() && NombresDeMateria.hayActivaConClave(repositorio, editada.valor().clave(), id)) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        return Resultado.ok(repositorio.guardar(editada.valor()));
    }
}
