package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Puerto de entrada (caso de uso): guardar el perfil del estudiante en el primer uso.
 *
 * <p>Implementa: HU-01 (Configurar perfil inicial), RN-01, RN-14 y RNF-14.
 */
public interface ConfigurarPerfil {

    /**
     * @param nombre nombre del estudiante
     * @param metaSemanalHoras meta semanal predeterminada
     * @return el perfil guardado, o el código de error si los datos no son válidos (no se guarda nada)
     */
    Resultado<Perfil> ejecutar(String nombre, double metaSemanalHoras);
}
