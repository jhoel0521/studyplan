package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Puerto de entrada (caso de uso): obtener una materia por su id, por ejemplo para abrir el formulario de edición.
 *
 * <p>Implementa: HU-07 (Editar materia).
 */
public interface ConsultarMateria {

    Optional<Materia> ejecutar(long id);
}
