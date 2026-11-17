package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Implementa {@link RegistrarMateria}: valida con el dominio, rechaza el nombre repetido entre las activas y solo
 * guarda si todo es válido. Una materia archivada con el mismo nombre no lo impide.
 *
 * <p>Implementa: HU-06, HU-26, RN-02, RN-14 y RNF-14.
 */
public final class RegistrarMateriaService implements RegistrarMateria {

    private final MateriaRepository repositorio;
    private final MetaSemanalRepository metas;
    private final Reloj reloj;

    public RegistrarMateriaService(MateriaRepository repositorio) {
        this(repositorio, null, null);
    }

    public RegistrarMateriaService(
            MateriaRepository repositorio, MetaSemanalRepository metas, Reloj reloj) {
        this.repositorio = repositorio;
        this.metas = metas;
        this.reloj = reloj;
    }

    @Override
    public Resultado<Materia> ejecutar(String nombre, int color, String docente) {
        return ejecutar(nombre, color, docente, null);
    }

    @Override
    public Resultado<Materia> ejecutar(
            String nombre, int color, String docente, Double metaSemanalHoras) {
        Resultado<Materia> nueva = Materia.crear(nombre, color, docente, metaSemanalHoras);
        if (!nueva.esValido()) {
            return nueva;
        }
        if (NombresDeMateria.hayActivaConClave(repositorio, nueva.valor().clave(), null)) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        Materia guardada = repositorio.guardar(nueva.valor());
        if (metaSemanalHoras != null && metas != null && reloj != null) {
            Fecha lunes = reloj.hoy().lunesDeSemana();
            MetaSemanal snap = MetaSemanal.crear(lunes, guardada.id(), metaSemanalHoras).valor();
            metas.guardar(snap);
        }
        return Resultado.ok(guardada);
    }
}
