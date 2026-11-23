package io.github.jhoel0521.studyplan.application.port.in.configuracion;

import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Puerto de entrada (caso de uso): configurar la copia de seguridad en Google Drive: cuenta, frecuencia y contraseña
 * opcional. La autorización de Google la hace la interfaz; este caso de uso guarda el resultado.
 *
 * <p>Implementa: HU-35, HU-36, RN-19 y RN-20.
 */
public interface ConfigurarRespaldo {

    ConfiguracionRespaldo consultar();

    /** Guarda el correo de la cuenta ya autorizada. */
    void vincularCuenta(String correo);

    /** Olvida la cuenta y la contraseña, cancela la tarea periódica y deja la frecuencia en NUNCA. */
    void desvincular();

    /** @return {@code SIN_CUENTA} si se pide una frecuencia distinta de NUNCA sin cuenta vinculada */
    ResultadoRespaldo cambiarFrecuencia(FrecuenciaRespaldo frecuencia);

    /** @return {@code CONTRASENA_REQUERIDA} si la contraseña está vacía */
    ResultadoRespaldo definirContrasena(char[] contrasena);

    void quitarContrasena();
}
