package io.github.jhoel0521.studyplan.domain.result;

/**
 * Resultado de una operación de respaldo, restauración o borrado en Drive. El dominio no devuelve textos: la
 * presentación traduce cada código (RNF-08).
 *
 * <p>Implementa: HU-35, HU-36, HU-37 y HU-38.
 */
public enum ResultadoRespaldo {
    /** La operación terminó bien. */
    OK,
    /** No hay una cuenta Google vinculada. */
    SIN_CUENTA,
    /** No hay conexión a internet; se reintenta cuando vuelva (HU-35). */
    SIN_CONEXION,
    /** La autorización de Google caducó o fue revocada y exige que el estudiante la repita (HU-35). */
    AUTORIZACION_REQUERIDA,
    /** Drive devolvió un error que no se arregla reintentando. */
    ERROR_DRIVE,
    /** La cuenta no tiene ningún respaldo en {@code appDataFolder} (HU-37). */
    SIN_RESPALDO,
    /** El respaldo está cifrado y no se dio contraseña (HU-36, HU-37). */
    CONTRASENA_REQUERIDA,
    /** La contraseña no abre el respaldo; los datos locales no se tocan (HU-37). */
    CONTRASENA_INCORRECTA,
    /** El respaldo es de una versión de formato más nueva que la que entiende la app (HU-37). */
    VERSION_NO_SOPORTADA,
    /** El archivo descargado no es un respaldo válido de StudyPlan (HU-37). */
    RESPALDO_CORRUPTO
}
