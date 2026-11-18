package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.EstadisticasPeriodo;
import io.github.jhoel0521.studyplan.domain.PeriodoEstadisticas;

/**
 * Puerto de entrada: consultar las estadísticas agregadas de estudio de un periodo semanal o
 * mensual ({@code HU-29}, {@code HU-30}, {@code HU-31}, {@code HU-45}, {@code RN-30},
 * {@code RN-31}).
 */
public interface ConsultarEstadisticas {

    EstadisticasPeriodo ejecutar(PeriodoEstadisticas periodo);
}
