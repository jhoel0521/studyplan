package io.github.jhoel0521.studyplan.adapter.out.persistence.repository;

import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.persistence.PerfilRepository;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.adapter.out.persistence.dao.PerfilDao;
import io.github.jhoel0521.studyplan.adapter.out.persistence.entity.PerfilEntity;

/**
 * Adaptador de salida: guarda el perfil en Room.
 *
 * <p>Implementa: HU-01 y RN-01.
 */
public class PerfilRepositoryRoom implements PerfilRepository {

    private final PerfilDao dao;

    public PerfilRepositoryRoom(PerfilDao dao) {
        this.dao = dao;
    }

    @Override
    public Optional<Perfil> obtener() {
        PerfilEntity fila = dao.obtener();
        return fila == null ? Optional.empty() : Optional.ofNullable(Mapeadores.aDominio(fila));
    }

    @Override
    public void guardar(Perfil perfil) {
        dao.guardar(Mapeadores.aEntidad(perfil));
    }
}
