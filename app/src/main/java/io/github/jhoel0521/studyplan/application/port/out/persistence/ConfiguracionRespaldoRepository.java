package io.github.jhoel0521.studyplan.application.port.out.persistence;

import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.MetadatosRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Puerto de salida: la configuración local del respaldo (cuenta, frecuencia, si hay contraseña y el último
 * resultado). No guarda la contraseña. Nace con frecuencia {@code NUNCA} y sin cuenta.
 *
 * <p>Implementa: HU-35, HU-36 y RN-19.
 */
public interface ConfiguracionRespaldoRepository {

    ConfiguracionRespaldo leer();

    void guardarCorreo(String correo);

    void guardarFrecuencia(FrecuenciaRespaldo frecuencia);

    void guardarConContrasena(boolean conContrasena);

    void guardarUltimoRespaldo(MetadatosRespaldo metadatos);

    void guardarUltimoResultado(ResultadoRespaldo resultado);

    /** Deja todo como al instalar: sin cuenta, frecuencia {@code NUNCA}, sin contraseña ni último respaldo. */
    void limpiar();
}
