package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.Resultado;

/**
 * Caso de uso: definir o modificar la meta semanal total de la semana actual ({@code HU-25},
 * {@code RN-14}, {@code RN-15}, {@code RN-33}).
 */
public interface DefinirMetaSemanal {

    Resultado<MetaSemanal> ejecutar(double horasObjetivo);
}
