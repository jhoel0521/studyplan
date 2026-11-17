package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.CierreSesion;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Puerto de entrada: resolver la recuperación al volver a la app con una sesión {@code EN_CURSO}
 * (HU-41, RN-24, RN-16).
 */
public interface ResolverRegresoApp {

    enum OpcionRegreso {
        NO_SEGUI,
        HASTA_HORA,
        SIGO_ESTUDIANDO
    }

    Resultado<SesionEstudio> ejecutar(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota);

    default Resultado<CierreSesion> ejecutarConMetas(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota) {
        Resultado<SesionEstudio> res = ejecutar(sesionId, opcion, hastaInstanteMilis, nota);
        return res.esValido()
                ? Resultado.ok(new CierreSesion(res.valor(), java.util.Collections.emptyList()))
                : Resultado.error(res.error());
    }
}
