package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Puerto de entrada (caso de uso): registrar una materia nueva.
 *
 * <p>Implementa: HU-06 (Registrar materia) y RN-02.
 */
public interface RegistrarMateria {

    /**
     * @param nombre nombre escrito por el estudiante (se sanea)
     * @param color color como entero ARGB
     * @param docente docente opcional
     * @return la materia guardada, o el código de error (no se guarda nada)
     */
    Resultado<Materia> ejecutar(String nombre, int color, String docente);
}
