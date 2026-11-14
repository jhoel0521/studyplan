package io.github.jhoel0521.studyplan.application.port.out;

/**
 * Puerto de salida: el almacén completo de datos locales. Su implementación vacía todas las tablas, incluidas las de
 * épicas futuras.
 *
 * <p>Implementa: HU-38 y RN-21 (borrado irreversible de los datos del teléfono).
 */
public interface AlmacenDatos {

    /** Borra todos los datos locales. Después, leer los ajustes devuelve los valores por defecto. */
    void borrarTodo();
}
