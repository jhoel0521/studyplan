package io.github.jhoel0521.studyplan.application.port.out.system;

import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;

/**
 * Puerto de salida: programa o cancela el respaldo periódico en segundo plano. Solo corre con red disponible y se
 * reintenta cuando vuelve la conexión.
 *
 * <p>Implementa: HU-35 y RN-19.
 */
public interface ProgramadorRespaldo {

    /** Programa (o reemplaza) la tarea periódica; {@code frecuencia} debe ser automática. */
    void programar(FrecuenciaRespaldo frecuencia);

    /** Cancela la tarea periódica, si existe. */
    void cancelar();
}
