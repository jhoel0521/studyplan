package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoReparto;

/**
 * Puerto de entrada: repartir un tramo continuo entre la sesión en curso y la siguiente sesión
 * invadida en una sola transacción atómica (HU-44, RN-28).
 */
public interface RepartirTramoSesion {

    Resultado<ResultadoReparto> ejecutar(
            long sesionActualId, long sesionSiguienteId, long corteMilis);
}
