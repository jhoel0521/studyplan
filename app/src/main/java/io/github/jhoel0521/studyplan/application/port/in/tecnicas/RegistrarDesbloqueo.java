package io.github.jhoel0521.studyplan.application.port.in.tecnicas;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;

/**
 * Puerto de entrada: registrar que el estudiante volvió a StudyPlan después de un bloqueo de
 * pantalla durante una sesión {@code EN_CURSO} (HU-50).
 */
public interface RegistrarDesbloqueo {

    /** @return el evento {@code DESBLOQUEO} guardado, o vacío si no había un bloqueo abierto */
    Optional<EventoEstudio> ejecutar();
}
