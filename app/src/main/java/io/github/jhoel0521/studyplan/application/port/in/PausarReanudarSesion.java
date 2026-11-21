package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Puerto de entrada: pausar o reanudar una sesión en curso (HU-22, RN-07, RN-18, RN-30).
 */
public interface PausarReanudarSesion {

    Resultado<SesionEstudio> ejecutar(long sesionId);

    default Resultado<SesionEstudio> ejecutar(
            long sesionId, TopicoEventoEstudio topico, String nota) {
        return ejecutar(sesionId);
    }
}
