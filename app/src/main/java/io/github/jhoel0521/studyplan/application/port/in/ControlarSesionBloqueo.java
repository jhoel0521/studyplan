package io.github.jhoel0521.studyplan.application.port.in;

/**
 * Puerto de entrada: el estado «bloqueada» de la sesión actual, en memoria. La interfaz lo consulta en cada pantalla
 * protegida y lo actualiza cuando la app vuelve a primer plano o el estudiante se autentica.
 *
 * <p>Implementa: HU-04 (pedir autenticación al volver a primer plano).
 */
public interface ControlarSesionBloqueo {

    /** @return {@code true} si hay que autenticarse antes de mostrar datos */
    boolean estaBloqueada();

    /** La app volvió a primer plano (no cuenta rotar la pantalla): se bloquea si el bloqueo está activo. */
    void alVolverAPrimerPlano();

    /** El estudiante se autenticó correctamente. */
    void desbloquear();

    /** @return {@code true} si el bloqueo está activo o aún no se leyeron los ajustes (se asume activo por seguridad) */
    boolean bloqueoActivo();
}
