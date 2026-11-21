package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Collections;
import java.util.List;
import io.github.jhoel0521.studyplan.domain.entity.Materia;

/**
 * Las materias activas (con sus horas) y las archivadas, ambas en orden alfabético.
 *
 * <p>Implementa: HU-09 (lista y lista vacía) y HU-08 (las archivadas se muestran aparte).
 */
public final class ListadoMaterias {

    private final List<ResumenMateria> activas;
    private final List<Materia> archivadas;

    public ListadoMaterias(List<ResumenMateria> activas, List<Materia> archivadas) {
        this.activas = Collections.unmodifiableList(activas);
        this.archivadas = Collections.unmodifiableList(archivadas);
    }

    public List<ResumenMateria> activas() {
        return activas;
    }

    public List<Materia> archivadas() {
        return archivadas;
    }

    /** @return {@code true} si no existe ninguna materia, ni activa ni archivada */
    public boolean estaVacio() {
        return activas.isEmpty() && archivadas.isEmpty();
    }
}
