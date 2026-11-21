package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Puerto de entrada (caso de uso): eliminar definitivamente una materia sin sesiones.
 *
 * <p>Implementa: HU-08 (Archivar o eliminar materia) y RN-03.
 */
public interface EliminarMateria {

    /**
     * @return la materia eliminada, o el código de error (con sesiones no se elimina: se sugiere archivarla)
     */
    Resultado<Materia> ejecutar(long id);
}
