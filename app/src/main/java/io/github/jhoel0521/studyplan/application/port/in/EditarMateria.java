package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Puerto de entrada (caso de uso): modificar el nombre, el color y el docente de una materia.
 *
 * <p>Implementa: HU-07 (Editar materia) y RN-02.
 */
public interface EditarMateria {

    /**
     * @return la materia editada, o el código de error (no se guarda nada)
     */
    Resultado<Materia> ejecutar(long id, String nombre, int color, String docente);

    /**
     * Edita una materia incluyendo su meta semanal opcional ({@code HU-26}, {@code RN-14}, {@code RN-32}).
     */
    default Resultado<Materia> ejecutar(
            long id, String nombre, int color, String docente, Double metaSemanalHoras) {
        return ejecutar(id, nombre, color, docente);
    }
}
