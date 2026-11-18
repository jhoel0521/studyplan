package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio;

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
