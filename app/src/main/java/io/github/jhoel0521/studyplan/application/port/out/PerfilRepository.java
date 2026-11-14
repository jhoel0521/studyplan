package io.github.jhoel0521.studyplan.application.port.out;

import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.Perfil;

/**
 * Puerto de salida: almacén del perfil del estudiante. Lo implementa un adaptador (Room); el núcleo solo
 * conoce esta interfaz.
 *
 * <p>Implementa: HU-01 y RN-01 (un único perfil por teléfono).
 */
public interface PerfilRepository {

    /** @return el perfil guardado, o vacío si el estudiante aún no lo configuró (primer uso) */
    Optional<Perfil> obtener();

    /** Guarda el perfil; si ya existía uno, lo reemplaza (RN-01). */
    void guardar(Perfil perfil);
}
