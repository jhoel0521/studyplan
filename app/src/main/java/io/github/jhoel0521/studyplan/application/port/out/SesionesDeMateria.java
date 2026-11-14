package io.github.jhoel0521.studyplan.application.port.out;

/**
 * Puerto de salida: ¿cuántas sesiones tiene registradas una materia? Lo usa la regla de que una materia con
 * sesiones no se elimina, solo se archiva.
 *
 * <p>Implementa: HU-08 y RN-03. Mientras no existan sesiones, el adaptador responde siempre cero; el change de
 * sesiones lo reemplaza por una consulta real.
 */
public interface SesionesDeMateria {

    int contar(long materiaId);
}
