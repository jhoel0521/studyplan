package io.github.jhoel0521.studyplan.application.port.out.persistence;

import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.entity.Materia;

/**
 * Puerto de salida: almacén de las materias. Lo implementa un adaptador (Room); el núcleo solo conoce esta interfaz.
 *
 * <p>Implementa: HU-06 a HU-09 y HU-46.
 */
public interface MateriaRepository {

    /**
     * Guarda una materia: si todavía no tiene id la crea y le asigna uno; si lo tiene, la reemplaza.
     *
     * @return la materia guardada, con su id
     */
    Materia guardar(Materia materia);

    Optional<Materia> obtener(long id);

    /** @return las materias activas, en cualquier orden */
    List<Materia> activas();

    /** @return las materias archivadas, en cualquier orden */
    List<Materia> archivadas();

    void eliminar(long id);
}
