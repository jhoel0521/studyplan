package io.github.jhoel0521.studyplan.application.port.in.tecnicas;

import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Puerto de entrada: detener una sesión en curso, materializar su duración real y detectar metas
 * alcanzadas (HU-22, HU-28, RN-07, RN-16, RN-26).
 */
public interface DetenerSesion {

    Resultado<SesionEstudio> ejecutar(long sesionId);

    default Resultado<CierreSesion> ejecutarConMetas(long sesionId) {
        Resultado<SesionEstudio> res = ejecutar(sesionId);
        return res.esValido()
                ? Resultado.ok(new CierreSesion(res.valor(), java.util.Collections.emptyList()))
                : Resultado.error(res.error());
    }
}
