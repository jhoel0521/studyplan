package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.ResumenInicio;

/**
 * Puerto de entrada (caso de uso): los datos que muestra la pantalla de Inicio.
 *
 * <p>Implementa, de forma parcial: HU-02 y RN-15.
 */
public interface ObtenerResumenInicio {

    /** @return el resumen, o vacío si todavía no hay perfil */
    Optional<ResumenInicio> ejecutar();
}
