package io.github.jhoel0521.studyplan.application.port.in.tecnicas;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;

/**
 * Puerto de entrada: registrar que la pantalla se apagó con StudyPlan en primer plano durante una
 * sesión {@code EN_CURSO}. No es una salida: el reloj sigue y no hay recuperación (HU-50, HU-40,
 * RN-23).
 */
public interface RegistrarBloqueoPantalla {

    /** @return el evento {@code BLOQUEO_PANTALLA} guardado, o vacío si no aplica */
    Optional<EventoEstudio> ejecutar();
}
