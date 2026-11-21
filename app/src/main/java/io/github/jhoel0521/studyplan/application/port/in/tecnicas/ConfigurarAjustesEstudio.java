package io.github.jhoel0521.studyplan.application.port.in.tecnicas;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Puerto de entrada: configurar el modo No molestar global y los parámetros de Pomodoro
 * (HU-23, HU-24, RN-17, RN-18).
 */
public interface ConfigurarAjustesEstudio {

    Resultado<Ajustes> ejecutar(
            boolean noMolestarGlobal,
            int trabajoMinutos,
            int descansoCortoMinutos,
            int descansoLargoMinutos,
            int ciclosHastaLargo);
}
