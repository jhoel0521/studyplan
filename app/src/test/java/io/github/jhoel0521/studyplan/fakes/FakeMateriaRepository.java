package io.github.jhoel0521.studyplan.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Puerto de salida falso en memoria para las pruebas del núcleo: asigna ids como lo haría la base.
 *
 * <p>Apoya las pruebas de: HU-06 a HU-09 y HU-46.
 */
public final class FakeMateriaRepository implements MateriaRepository {

    private final List<Materia> materias = new ArrayList<>();
    private long siguienteId = 1;
    public int guardados;

    @Override
    public Materia guardar(Materia materia) {
        guardados++;
        Materia guardada = materia.id() == null ? materia.conId(siguienteId++) : materia;
        for (int i = 0; i < materias.size(); i++) {
            if (materias.get(i).id().equals(guardada.id())) {
                materias.set(i, guardada);
                return guardada;
            }
        }
        materias.add(guardada);
        return guardada;
    }

    @Override
    public Optional<Materia> obtener(long id) {
        for (Materia m : materias) {
            if (m.id() == id) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Materia> activas() {
        List<Materia> activas = new ArrayList<>();
        for (Materia m : materias) {
            if (!m.archivada()) {
                activas.add(m);
            }
        }
        return activas;
    }

    @Override
    public List<Materia> archivadas() {
        List<Materia> archivadas = new ArrayList<>();
        for (Materia m : materias) {
            if (m.archivada()) {
                archivadas.add(m);
            }
        }
        return archivadas;
    }

    @Override
    public void eliminar(long id) {
        for (int i = 0; i < materias.size(); i++) {
            if (materias.get(i).id() == id) {
                materias.remove(i);
                return;
            }
        }
    }

    public int cantidad() {
        return materias.size();
    }
}
