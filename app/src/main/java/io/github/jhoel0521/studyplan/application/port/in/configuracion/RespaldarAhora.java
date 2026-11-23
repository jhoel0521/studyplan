package io.github.jhoel0521.studyplan.application.port.in.configuracion;

import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Puerto de entrada (caso de uso): subir ahora una copia de todos los datos a Google Drive, la pida el estudiante o la
 * tarea periódica.
 *
 * <p>Implementa: HU-35 y HU-36.
 */
public interface RespaldarAhora {

    ResultadoRespaldo ejecutar();
}
