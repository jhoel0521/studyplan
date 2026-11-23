package io.github.jhoel0521.studyplan.application.port.in.configuracion;

import io.github.jhoel0521.studyplan.domain.enumeration.AlcanceBorrado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Puerto de entrada (caso de uso): borrar los datos del estudiante del teléfono, de Drive o de ambos. La doble
 * confirmación es responsabilidad de la interfaz; este caso de uso ejecuta el borrado ya confirmado.
 *
 * <p>Implementa: HU-38 y RN-21.
 */
public interface BorrarDatos {

    /**
     * @return {@code OK} si se borró lo pedido; con alcance {@code DRIVE} o {@code AMBOS}, si Drive falla se devuelve
     *     su resultado y no se borra nada del teléfono
     */
    ResultadoRespaldo ejecutar(AlcanceBorrado alcance);
}
