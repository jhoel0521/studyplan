package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.AlcanceBorrado;

/**
 * Puerto de entrada (caso de uso): borrar los datos del estudiante. La doble confirmación es responsabilidad de la
 * interfaz; este caso de uso ejecuta el borrado ya confirmado.
 *
 * <p>Implementa: HU-38 y RN-21 (parcial: solo el alcance «teléfono»).
 */
public interface BorrarDatos {

    void ejecutar(AlcanceBorrado alcance);
}
