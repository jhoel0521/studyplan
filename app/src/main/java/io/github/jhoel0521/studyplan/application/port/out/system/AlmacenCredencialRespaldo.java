package io.github.jhoel0521.studyplan.application.port.out.system;

/**
 * Puerto de salida: guarda la contraseña del respaldo, protegida por el sistema, para poder cifrar los respaldos
 * automáticos sin pedirla. Nunca se guarda en claro ni en la base de datos.
 *
 * <p>Implementa: HU-36 y RN-20.
 */
public interface AlmacenCredencialRespaldo {

    void guardar(char[] contrasena);

    /** @return la contraseña guardada, o {@code null} si no hay o no se puede recuperar */
    char[] obtener();

    void borrar();
}
