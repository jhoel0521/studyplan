package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Regla de unicidad del nombre entre las materias activas, compartida por los servicios.
 *
 * <p>Implementa: RN-02 (HU-06, HU-07 y HU-46).
 */
final class NombresDeMateria {

    private NombresDeMateria() {
    }

    /**
     * @param clave clave de unicidad que se quiere usar
     * @param idExcluido id de la materia que se está guardando (para no chocar consigo misma), o {@code null}
     * @return {@code true} si otra materia activa ya tiene esa clave
     */
    static boolean hayActivaConClave(MateriaRepository repositorio, String clave, Long idExcluido) {
        for (Materia activa : repositorio.activas()) {
            boolean esLaMisma = idExcluido != null && idExcluido.equals(activa.id());
            if (!esLaMisma && activa.clave().equals(clave)) {
                return true;
            }
        }
        return false;
    }
}
