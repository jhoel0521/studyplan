package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.ClienteGoogleDrive;
import io.github.jhoel0521.studyplan.domain.readmodel.RespaldoDescargado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Drive falso en memoria con un único archivo; permite simular fallos (sin conexión, autorización caducada).
 *
 * <p>Apoya las pruebas de: HU-35, HU-37 y HU-38.
 */
public final class FakeClienteGoogleDrive implements ClienteGoogleDrive {

    public byte[] archivo;
    /** Si no es {@code OK}, toda operación devuelve este fallo sin tocar el archivo. */
    public ResultadoRespaldo fallo = ResultadoRespaldo.OK;
    public int subidas;
    public int eliminaciones;

    @Override
    public ResultadoRespaldo subir(byte[] datos) {
        if (fallo != ResultadoRespaldo.OK) {
            return fallo;
        }
        subidas++;
        archivo = datos.clone();
        return ResultadoRespaldo.OK;
    }

    @Override
    public RespaldoDescargado descargar() {
        if (fallo != ResultadoRespaldo.OK) {
            return RespaldoDescargado.fallo(fallo);
        }
        if (archivo == null) {
            return RespaldoDescargado.fallo(ResultadoRespaldo.SIN_RESPALDO);
        }
        return new RespaldoDescargado(ResultadoRespaldo.OK, archivo.clone());
    }

    @Override
    public ResultadoRespaldo eliminar() {
        if (fallo != ResultadoRespaldo.OK) {
            return fallo;
        }
        eliminaciones++;
        archivo = null;
        return ResultadoRespaldo.OK;
    }
}
