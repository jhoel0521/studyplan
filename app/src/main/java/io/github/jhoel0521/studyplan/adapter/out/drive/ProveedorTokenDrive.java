package io.github.jhoel0521.studyplan.adapter.out.drive;

import java.io.IOException;

/**
 * Entrega el token de acceso de Google para {@code drive.appdata}. Separarlo permite probar el cliente REST sin Google.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public interface ProveedorTokenDrive {

    /** La autorización caducó, fue revocada o aún no se dio, y exige que el estudiante la repita en pantalla. */
    final class AutorizacionRequeridaException extends Exception {
        public AutorizacionRequeridaException(String mensaje) {
            super(mensaje);
        }
    }

    /**
     * Debe llamarse fuera del hilo principal.
     *
     * @throws AutorizacionRequeridaException si hace falta que el estudiante autorice de nuevo
     * @throws IOException si no hay conexión
     */
    String obtenerToken() throws AutorizacionRequeridaException, IOException;
}
