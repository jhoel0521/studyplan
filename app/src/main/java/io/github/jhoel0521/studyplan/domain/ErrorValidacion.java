package io.github.jhoel0521.studyplan.domain;

/**
 * Códigos de error de validación del dominio. El dominio no devuelve textos: la capa de presentación
 * traduce cada código a su mensaje en {@code strings.xml} (RNF-08).
 *
 * <p>Implementa: HU-01 (nombre obligatorio), RN-14 (rango de la meta) y RNF-14 (validación de entradas).
 */
public enum ErrorValidacion {
    /** El nombre quedó vacío tras recortar los espacios (HU-01, escenario «Nombre vacío»). */
    NOMBRE_VACIO,
    /** La meta semanal no es mayor que 0 y menor o igual a 168 horas (RN-14). */
    META_FUERA_DE_RANGO
}
