package io.github.jhoel0521.studyplan.application.port.in.configuracion;

/**
 * Puerto de entrada (caso de uso): activar o desactivar el bloqueo con huella o PIN.
 *
 * <p>Implementa: HU-04 (escenarios «Activar bloqueo» y «Sin método de bloqueo») y RNF-12.
 */
public interface ConfigurarBloqueo {

    /** Resultado de intentar activar el bloqueo. */
    enum ResultadoActivacion {
        ACTIVADO,
        /** El teléfono no tiene huella, PIN, patrón ni contraseña: el bloqueo no se activa. */
        SIN_METODO_DE_BLOQUEO
    }

    ResultadoActivacion activar();

    void desactivar();
}
