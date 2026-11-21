package io.github.jhoel0521.studyplan.domain.enumeration;

/**
 * Qué datos borra el estudiante en «Borrar mis datos».
 *
 * <p>Implementa: HU-38 y RN-21. De momento solo existe el alcance «teléfono»; «Drive» y «ambos» se agregan con el
 * change de copia de seguridad (HU-35 a HU-37).
 */
public enum AlcanceBorrado {
    /** Todos los datos locales: perfil, ajustes y los datos de épicas futuras. */
    TELEFONO
}
