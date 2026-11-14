package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.ListadoMaterias;

/**
 * Puerto de entrada (caso de uso): listar las materias activas con sus horas y las archivadas, en orden alfabético.
 *
 * <p>Implementa: HU-09 (Listar materias con horas acumuladas) y HU-08 (archivadas).
 */
public interface ListarMaterias {

    ListadoMaterias ejecutar();
}
