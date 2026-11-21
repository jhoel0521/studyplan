package io.github.jhoel0521.studyplan.application.port.out.system;

import java.util.Objects;

/**
 * Resultado de una operación de exportación de reporte sin acoplar errores de entrada/salida a
 * códigos de validación de formularios ({@code HU-33}).
 */
public final class ResultadoExportacion {

    /** Causas tipificadas de fallo al exportar un reporte. */
    public enum Error {
        SIN_DATOS,
        ERROR_ESCRITURA
    }

    private final String uriContenido;
    private final Error error;

    private ResultadoExportacion(String uriContenido, Error error) {
        this.uriContenido = uriContenido;
        this.error = error;
    }

    public static ResultadoExportacion ok(String uriContenido) {
        Objects.requireNonNull(uriContenido, "uriContenido");
        if (uriContenido.trim().isEmpty()) {
            throw new IllegalArgumentException("uriContenido vacío");
        }
        return new ResultadoExportacion(uriContenido, null);
    }

    public static ResultadoExportacion error(Error error) {
        return new ResultadoExportacion(null, Objects.requireNonNull(error, "error"));
    }

    public boolean esExitoso() {
        return error == null && uriContenido != null;
    }

    public String uriContenido() {
        return uriContenido;
    }

    public Error error() {
        return error;
    }
}
