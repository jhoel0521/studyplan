package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.ControlarSesionBloqueo;

/**
 * Estado «bloqueada» de la sesión, en memoria. <b>Nace bloqueado</b> hasta que se leen los ajustes: si Android
 * termina el proceso y restaura otra pantalla, la app nunca muestra datos sin autenticar.
 *
 * <p>Implementa: HU-04 (pedir autenticación al volver a primer plano) y RNF-12. Se accede desde el hilo
 * principal y desde el hilo de segundo plano, por eso los métodos son {@code synchronized}.
 */
public final class SesionBloqueo implements ControlarSesionBloqueo {

    private boolean bloqueada = true;
    private boolean bloqueoActivo = true;

    /**
     * Informa si el bloqueo está activo en los ajustes. Con el bloqueo inactivo la sesión queda abierta; con el
     * bloqueo activo no cambia el estado actual (cuando el estudiante lo activa en Ajustes ya está dentro de la app).
     */
    public synchronized void alCambiarBloqueo(boolean activo) {
        bloqueoActivo = activo;
        if (!activo) {
            bloqueada = false;
        }
    }

    @Override
    public synchronized boolean bloqueoActivo() {
        return bloqueoActivo;
    }

    @Override
    public synchronized boolean estaBloqueada() {
        return bloqueada;
    }

    @Override
    public synchronized void alVolverAPrimerPlano() {
        if (bloqueoActivo) {
            bloqueada = true;
        }
    }

    @Override
    public synchronized void desbloquear() {
        bloqueada = false;
    }
}
