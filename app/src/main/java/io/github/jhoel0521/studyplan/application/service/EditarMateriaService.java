package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.materias.EditarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Implementa {@link EditarMateria}: conserva el id y el estado de archivada. Si la materia está activa, el nuevo
 * nombre no puede coincidir con el de otra activa (RN-02); una archivada puede repetir nombres.
 *
 * <p>Implementa: HU-07, HU-26, RN-02, RN-14, RN-32, RN-33 y RNF-14.
 */
public final class EditarMateriaService implements EditarMateria {

    private final MateriaRepository repositorio;
    private final MetaSemanalRepository metas;
    private final Reloj reloj;

    public EditarMateriaService(MateriaRepository repositorio) {
        this(repositorio, null, null);
    }

    public EditarMateriaService(
            MateriaRepository repositorio, MetaSemanalRepository metas, Reloj reloj) {
        this.repositorio = repositorio;
        this.metas = metas;
        this.reloj = reloj;
    }

    @Override
    public Resultado<Materia> ejecutar(long id, String nombre, int color, String docente) {
        Materia actual = repositorio.obtener(id).orElse(null);
        if (actual == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        return ejecutarSobre(actual, nombre, color, docente, actual.metaSemanalHoras());
    }

    @Override
    public Resultado<Materia> ejecutar(
            long id, String nombre, int color, String docente, Double metaSemanalHoras) {
        Materia actual = repositorio.obtener(id).orElse(null);
        if (actual == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        return ejecutarSobre(actual, nombre, color, docente, metaSemanalHoras);
    }

    private Resultado<Materia> ejecutarSobre(
            Materia actual,
            String nombre,
            int color,
            String docente,
            Double metaSemanalHoras) {
        Resultado<Materia> editada = actual.editar(nombre, color, docente, metaSemanalHoras);
        if (!editada.esValido()) {
            return editada;
        }
        if (!actual.archivada()
                && NombresDeMateria.hayActivaConClave(
                        repositorio, editada.valor().clave(), actual.id())) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        Materia guardada = repositorio.guardar(editada.valor());
        if (metaSemanalHoras != null && metas != null && reloj != null) {
            Fecha lunes = reloj.hoy().lunesDeSemana();
            MetaSemanal existente = metas.buscar(lunes, guardada.id()).orElse(null);
            MetaSemanal snap = existente == null
                    ? MetaSemanal.crear(lunes, guardada.id(), metaSemanalHoras).valor()
                    : existente.conObjetivo(metaSemanalHoras).valor();
            metas.guardar(snap);
        }
        return Resultado.ok(guardada);
    }
}
