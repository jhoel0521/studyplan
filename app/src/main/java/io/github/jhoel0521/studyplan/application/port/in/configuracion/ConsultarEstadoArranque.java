package io.github.jhoel0521.studyplan.application.port.in.configuracion;

import io.github.jhoel0521.studyplan.domain.enumeration.EstadoArranque;

/**
 * Puerto de entrada (caso de uso): decidir qué pantalla corresponde al abrir la app.
 *
 * <p>Implementa: HU-01 (primer uso) y HU-04 (bloqueo).
 */
public interface ConsultarEstadoArranque {

    EstadoArranque ejecutar();
}
