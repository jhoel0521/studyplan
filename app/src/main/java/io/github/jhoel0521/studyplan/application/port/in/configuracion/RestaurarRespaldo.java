package io.github.jhoel0521.studyplan.application.port.in.configuracion;

import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Puerto de entrada (caso de uso): bajar el respaldo de Google Drive y reemplazar los datos locales. Si algo falla no
 * cambia nada.
 *
 * <p>Implementa: HU-37 y HU-36.
 */
public interface RestaurarRespaldo {

    /**
     * @param contrasena contraseña del respaldo, o {@code null} si el estudiante no escribió ninguna
     */
    ResultadoRespaldo ejecutar(char[] contrasena);
}
