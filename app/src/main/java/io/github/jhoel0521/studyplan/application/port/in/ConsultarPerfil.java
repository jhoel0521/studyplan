package io.github.jhoel0521.studyplan.application.port.in;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.Perfil;

/**
 * Puerto de entrada (caso de uso): consultar el perfil guardado.
 *
 * <p>Implementa: HU-01 (saber si falta configurar el perfil) y HU-02 (saludo en Inicio).
 */
public interface ConsultarPerfil {

    /** @return el perfil, o vacío si todavía no existe */
    Optional<Perfil> ejecutar();
}
