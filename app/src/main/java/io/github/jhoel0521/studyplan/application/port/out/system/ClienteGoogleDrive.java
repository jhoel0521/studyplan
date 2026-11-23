package io.github.jhoel0521.studyplan.application.port.out.system;

import io.github.jhoel0521.studyplan.domain.readmodel.RespaldoDescargado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Puerto de salida: el único archivo de respaldo de StudyPlan en la carpeta oculta {@code appDataFolder} del Drive de
 * la cuenta vinculada. Ninguna operación lanza excepciones: devuelven un {@link ResultadoRespaldo}.
 *
 * <p>Implementa: HU-35, HU-37 y HU-38.
 */
public interface ClienteGoogleDrive {

    /** Sube el archivo y reemplaza el respaldo anterior. */
    ResultadoRespaldo subir(byte[] archivo);

    /** Baja el respaldo; {@code SIN_RESPALDO} si la cuenta no tiene ninguno. */
    RespaldoDescargado descargar();

    /** Borra el respaldo de Drive; es correcto si ya no existía. */
    ResultadoRespaldo eliminar();
}
