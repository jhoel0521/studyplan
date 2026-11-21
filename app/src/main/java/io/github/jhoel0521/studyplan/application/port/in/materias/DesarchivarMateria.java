package io.github.jhoel0521.studyplan.application.port.in.materias;

import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Puerto de entrada (caso de uso): reactivar una materia archivada.
 *
 * <p>Implementa: HU-46 (Desarchivar materia) y RN-02.
 */
public interface DesarchivarMateria {

    /**
     * @return la materia activa, o el código de error (por ejemplo, el nombre ya lo usa otra materia activa)
     */
    Resultado<Materia> ejecutar(long id);
}
