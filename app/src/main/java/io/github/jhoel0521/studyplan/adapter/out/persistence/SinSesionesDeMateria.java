package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.application.port.out.SesionesDeMateria;

/**
 * Adaptador provisional: como todavía no existen sesiones, ninguna materia tiene. <b>Obligatorio:</b> el change de
 * sesiones debe reemplazarlo por una consulta real; si se olvidara, una materia con sesiones se podría eliminar.
 *
 * <p>Implementa: HU-08 y RN-03 (parcial, ver {@code trazabilidad.md} de {@code add-gestion-materias}).
 */
public class SinSesionesDeMateria implements SesionesDeMateria {

    @Override
    public int contar(long materiaId) {
        return 0;
    }
}
