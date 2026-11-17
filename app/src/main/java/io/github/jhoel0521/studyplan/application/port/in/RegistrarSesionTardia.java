package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.CierreSesion;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Puerto de entrada (caso de uso): registrar tardíamente una sesión {@code NO_REALIZADA} de hace
 * 3 días o menos pasándola a {@code COMPLETADA_TARDIA} (HU-13, HU-28, RN-10, RN-16, RN-26).
 */
public interface RegistrarSesionTardia {

    Resultado<SesionEstudio> ejecutar(long sesionId, int duracionRealMinutos);

    default Resultado<CierreSesion> ejecutarConMetas(long sesionId, int duracionRealMinutos) {
        Resultado<SesionEstudio> res = ejecutar(sesionId, duracionRealMinutos);
        return res.esValido()
                ? Resultado.ok(new CierreSesion(res.valor(), java.util.Collections.emptyList()))
                : Resultado.error(res.error());
    }
}
