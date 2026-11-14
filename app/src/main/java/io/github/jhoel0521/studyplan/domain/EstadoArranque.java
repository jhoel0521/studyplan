package io.github.jhoel0521.studyplan.domain;

/**
 * Qué debe mostrar la aplicación al abrirse.
 *
 * <p>Implementa: HU-01 (primer uso) y HU-04 (bloqueo). El primer uso tiene prioridad: sin perfil no hay datos
 * que proteger, así que no se pide autenticación.
 */
public enum EstadoArranque {
    /** Todavía no hay perfil: se muestra el formulario de primer uso. */
    SIN_PERFIL,
    /** Hay perfil y la app está bloqueada: se pide huella o PIN antes de mostrar datos. */
    BLOQUEADA,
    /** Hay perfil y la app está abierta: se muestra Inicio. */
    LISTA
}
