package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarMateria;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Implementa {@link RegistrarMateria}: valida con el dominio, rechaza el nombre repetido entre las activas y solo
 * guarda si todo es válido. Una materia archivada con el mismo nombre no lo impide.
 *
 * <p>Implementa: HU-06, RN-02 y RNF-14.
 */
public final class RegistrarMateriaService implements RegistrarMateria {

    private final MateriaRepository repositorio;

    public RegistrarMateriaService(MateriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Resultado<Materia> ejecutar(String nombre, int color, String docente) {
        Resultado<Materia> nueva = Materia.crear(nombre, color, docente);
        if (!nueva.esValido()) {
            return nueva;
        }
        if (NombresDeMateria.hayActivaConClave(repositorio, nueva.valor().clave(), null)) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        return Resultado.ok(repositorio.guardar(nueva.valor()));
    }
}
