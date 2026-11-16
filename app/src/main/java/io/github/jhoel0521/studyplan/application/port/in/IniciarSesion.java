package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * Puerto de entrada: iniciar una sesión de estudio en vivo (HU-22, HU-23, HU-24, RN-07, RN-17, RN-18).
 */
public interface IniciarSesion {

    Resultado<SesionEstudio> ejecutar(
            long sesionId, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar);
}
