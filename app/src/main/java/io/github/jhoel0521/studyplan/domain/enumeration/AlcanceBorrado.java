package io.github.jhoel0521.studyplan.domain.enumeration;

/**
 * Qué datos borra el estudiante en «Borrar mis datos».
 *
 * <p>Implementa: HU-38 y RN-21.
 */
public enum AlcanceBorrado {
    /** Todos los datos locales: perfil, ajustes y los datos de épicas futuras. */
    TELEFONO,
    /** Solo el respaldo de Google Drive; los datos del teléfono quedan intactos. */
    DRIVE,
    /** Los datos del teléfono y el respaldo de Google Drive. */
    AMBOS
}
