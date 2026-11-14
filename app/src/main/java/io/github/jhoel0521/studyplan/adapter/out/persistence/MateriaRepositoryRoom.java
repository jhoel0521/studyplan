package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Adaptador de salida: guarda las materias en Room.
 *
 * <p>Implementa: HU-06 a HU-09 y HU-46.
 */
public class MateriaRepositoryRoom implements MateriaRepository {

    private final MateriaDao dao;

    public MateriaRepositoryRoom(MateriaDao dao) {
        this.dao = dao;
    }

    @Override
    public Materia guardar(Materia materia) {
        MateriaEntity fila = Mapeadores.aEntidad(materia);
        if (materia.id() == null) {
            return materia.conId(dao.insertar(fila));
        }
        dao.actualizar(fila);
        return materia;
    }

    @Override
    public Optional<Materia> obtener(long id) {
        MateriaEntity fila = dao.obtener(id);
        return fila == null ? Optional.empty() : Optional.of(Mapeadores.aDominio(fila));
    }

    @Override
    public List<Materia> activas() {
        return aDominio(dao.activas());
    }

    @Override
    public List<Materia> archivadas() {
        return aDominio(dao.archivadas());
    }

    @Override
    public void eliminar(long id) {
        dao.eliminar(id);
    }

    private static List<Materia> aDominio(List<MateriaEntity> filas) {
        List<Materia> materias = new ArrayList<>();
        for (MateriaEntity fila : filas) {
            materias.add(Mapeadores.aDominio(fila));
        }
        return materias;
    }
}
