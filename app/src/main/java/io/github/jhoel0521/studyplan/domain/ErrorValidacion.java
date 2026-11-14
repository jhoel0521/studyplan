package io.github.jhoel0521.studyplan.domain;

/**
 * Códigos de error de validación del dominio. El dominio no devuelve textos: la capa de presentación
 * traduce cada código a su mensaje en {@code strings.xml} (RNF-08).
 *
 * <p>Implementa: HU-01 (nombre obligatorio), RN-14 (rango de la meta), HU-06 a HU-08 y HU-46 (materias, RN-02 y
 * RN-03) y RNF-14 (validación de entradas).
 */
public enum ErrorValidacion {
    /** El nombre quedó vacío tras recortar los espacios (HU-01 y HU-06). */
    NOMBRE_VACIO,
    /** La meta semanal no es mayor que 0 y menor o igual a 168 horas (RN-14). */
    META_FUERA_DE_RANGO,
    /** El nombre de la materia supera los 50 caracteres una vez saneado (RN-02). */
    NOMBRE_MUY_LARGO,
    /** Ya existe una materia activa con ese nombre, sin distinguir mayúsculas ni acentos (RN-02). */
    NOMBRE_DUPLICADO,
    /** No existe la materia pedida. */
    MATERIA_NO_ENCONTRADA,
    /** La materia ya está activa y no hace falta desarchivarla (HU-46). */
    MATERIA_YA_ACTIVA,
    /** La materia tiene sesiones registradas y no se elimina, solo se archiva (RN-03). */
    MATERIA_CON_SESIONES
}
