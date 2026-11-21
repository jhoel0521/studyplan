package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;

/**
 * Puerto de entrada (caso de uso): leer los ajustes actuales.
 *
 * <p>Implementa: HU-03 y HU-04 (la pantalla de Ajustes y el arranque necesitan conocerlos).
 */
public interface ConsultarAjustes {

    Ajustes ejecutar();
}
