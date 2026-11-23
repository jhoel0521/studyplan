package io.github.jhoel0521.studyplan.domain.readmodel;

import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Lo que Ajustes muestra de la copia de seguridad: cuenta, frecuencia, contraseña, último respaldo y si el último
 * intento falló.
 *
 * <p>Implementa: HU-35 y HU-36.
 */
public final class ConfiguracionRespaldo {

    private final String correo;
    private final FrecuenciaRespaldo frecuencia;
    private final boolean conContrasena;
    private final MetadatosRespaldo ultimoRespaldo;
    private final ResultadoRespaldo ultimoResultado;

    /**
     * @param correo correo vinculado, o {@code null} si no hay cuenta
     * @param ultimoRespaldo datos del último respaldo bueno, o {@code null} si no hay
     * @param ultimoResultado resultado del último intento, o {@code null} si nunca se intentó
     */
    public ConfiguracionRespaldo(
            String correo,
            FrecuenciaRespaldo frecuencia,
            boolean conContrasena,
            MetadatosRespaldo ultimoRespaldo,
            ResultadoRespaldo ultimoResultado) {
        this.correo = correo;
        this.frecuencia = frecuencia;
        this.conContrasena = conContrasena;
        this.ultimoRespaldo = ultimoRespaldo;
        this.ultimoResultado = ultimoResultado;
    }

    public String correo() {
        return correo;
    }

    public boolean hayCuenta() {
        return correo != null;
    }

    public FrecuenciaRespaldo frecuencia() {
        return frecuencia;
    }

    public boolean conContrasena() {
        return conContrasena;
    }

    public MetadatosRespaldo ultimoRespaldo() {
        return ultimoRespaldo;
    }

    public ResultadoRespaldo ultimoResultado() {
        return ultimoResultado;
    }

    /** {@code true} si el último intento falló y Ajustes debe avisarlo (HU-35). */
    public boolean ultimoIntentoFallo() {
        return ultimoResultado != null && ultimoResultado != ResultadoRespaldo.OK;
    }
}
