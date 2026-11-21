package io.github.jhoel0521.studyplan.application.port.in.recordatorios;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;

/**
 * Puerto de entrada para definir la antelación predeterminada de los recordatorios de sesiones
 * planificadas ({@code HU-34}).
 */
public interface ConfigurarRecordatorioPorDefecto {

    Ajustes ejecutar(AntelacionRecordatorio antelacion);
}
