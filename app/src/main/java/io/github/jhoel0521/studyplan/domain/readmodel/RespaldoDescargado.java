package io.github.jhoel0521.studyplan.domain.readmodel;

import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Resultado de pedir el respaldo a Drive: el código y, si fue {@code OK}, los bytes del archivo.
 *
 * <p>Implementa: HU-37.
 */
public final class RespaldoDescargado {

    private final ResultadoRespaldo resultado;
    private final byte[] datos;

    public RespaldoDescargado(ResultadoRespaldo resultado, byte[] datos) {
        this.resultado = resultado;
        this.datos = datos;
    }

    public static RespaldoDescargado fallo(ResultadoRespaldo resultado) {
        return new RespaldoDescargado(resultado, null);
    }

    public ResultadoRespaldo resultado() {
        return resultado;
    }

    /** Bytes del archivo; {@code null} si {@link #resultado()} no es {@code OK}. */
    public byte[] datos() {
        return datos;
    }
}
