package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.ReporteEstudio;

/**
 * Puerto de entrada: generar el reporte tabular de un periodo semanal o mensual ({@code HU-32},
 * {@code RN-31}).
 */
public interface GenerarReporteEstudio {

    ReporteEstudio ejecutar(PeriodoEstadisticas periodo);
}
