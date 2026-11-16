package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.DiagnosticoRegresoApp;

/**
 * Puerto de entrada: evaluar si existe una sesión {@code EN_CURSO} pendiente de recuperación o
 * vencida por más de 3 días al regresar a la app (HU-22, HU-41, RN-24, RN-29).
 */
public interface EvaluarRecuperacionSesion {

    Optional<DiagnosticoRegresoApp> ejecutar();
}
