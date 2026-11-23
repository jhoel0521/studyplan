package io.github.jhoel0521.studyplan.application.port.out.persistence;

import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;

/**
 * Puerto de salida: el almacén completo de datos locales. Su implementación vacía todas las tablas, incluidas las de
 * épicas futuras, y exporta o restaura todo el contenido para el respaldo.
 *
 * <p>Implementa: HU-38 y RN-21 (borrado irreversible de los datos del teléfono), HU-35 y HU-37 (respaldo y
 * restauración).
 */
public interface AlmacenDatos {

    /** Borra todos los datos locales. Después, leer los ajustes devuelve los valores por defecto. */
    void borrarTodo();

    /** Copia de todos los datos locales en este instante (HU-35). */
    InstantaneaRespaldo exportarTodo(long ahoraUtcMilis);

    /**
     * Reemplaza todos los datos locales por los de la instantánea, en una sola transacción: si algo falla no cambia
     * nada (HU-37).
     */
    void restaurarTodo(InstantaneaRespaldo instantanea);
}
