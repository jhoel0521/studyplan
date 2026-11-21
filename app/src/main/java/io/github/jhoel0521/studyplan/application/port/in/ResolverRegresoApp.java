package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Puerto de entrada: resolver la recuperación al volver a la app con una sesión {@code EN_CURSO}
 * (HU-41, RN-24, RN-16, RN-30).
 */
public interface ResolverRegresoApp {

    enum OpcionRegreso {
        NO_SEGUI,
        HASTA_HORA,
        SIGO_ESTUDIANDO,
        RETOMAR_SIN_SUMAR
    }

    Resultado<SesionEstudio> ejecutar(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota);

    default Resultado<CierreSesion> ejecutarConMetas(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota) {
        return ejecutarConMetas(sesionId, opcion, hastaInstanteMilis, nota, null);
    }

    default Resultado<CierreSesion> ejecutarConMetas(
            long sesionId,
            OpcionRegreso opcion,
            Long hastaInstanteMilis,
            String nota,
            TopicoEventoEstudio topico) {
        Resultado<SesionEstudio> res = ejecutar(sesionId, opcion, hastaInstanteMilis, nota);
        return res.esValido()
                ? Resultado.ok(new CierreSesion(res.valor(), java.util.Collections.emptyList()))
                : Resultado.error(res.error());
    }
}
