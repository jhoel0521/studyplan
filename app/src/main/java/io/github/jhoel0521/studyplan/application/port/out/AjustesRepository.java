package io.github.jhoel0521.studyplan.application.port.out;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;

/**
 * Puerto de salida: almacén de los ajustes. Si todavía no hay ajustes guardados, la implementación devuelve
 * {@link Ajustes#porDefecto()}.
 *
 * <p>Implementa: HU-03 y HU-04.
 */
public interface AjustesRepository {

    /** @return los ajustes guardados o, si no hay ninguno, los valores por defecto (nunca nulo) */
    Ajustes obtener();

    void guardar(Ajustes ajustes);
}
