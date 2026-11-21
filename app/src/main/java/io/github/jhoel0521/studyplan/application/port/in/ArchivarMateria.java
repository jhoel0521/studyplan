package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Puerto de entrada (caso de uso): archivar una materia para ocultarla de las listas de selección sin perder su historial.
 *
 * <p>Implementa: HU-08 (Archivar o eliminar materia).
 */
public interface ArchivarMateria {

    /**
     * @return la materia archivada, o el código de error
     */
    Resultado<Materia> ejecutar(long id);
}
