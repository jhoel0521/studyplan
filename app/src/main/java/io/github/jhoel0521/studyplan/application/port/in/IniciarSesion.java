package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Puerto de entrada: iniciar una sesión de estudio en vivo (HU-22, HU-23, HU-24, RN-07, RN-17, RN-18).
 */
public interface IniciarSesion {

    Resultado<SesionEstudio> ejecutar(
            long sesionId, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar);
}
