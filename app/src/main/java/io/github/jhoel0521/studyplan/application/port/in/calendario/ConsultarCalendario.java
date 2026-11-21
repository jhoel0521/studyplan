package io.github.jhoel0521.studyplan.application.port.in.calendario;

import io.github.jhoel0521.studyplan.domain.readmodel.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoCalendario;

/**
 * Puerto de entrada (caso de uso): consultar las sesiones, celdas o bloques horarios y resumen
 * del calendario para un periodo y filtro determinados (HU-16 a HU-21, RN-08, RN-11, RN-13).
 */
public interface ConsultarCalendario {

    /**
     * Consulta los datos del calendario para {@code periodo} aplicando {@code filtro}.
     */
    DatosCalendario ejecutar(PeriodoCalendario periodo, FiltroCalendario filtro);
}
