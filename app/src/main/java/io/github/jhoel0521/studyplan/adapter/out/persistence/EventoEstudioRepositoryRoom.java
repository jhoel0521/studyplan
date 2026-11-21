package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;

/**
 * Adaptador Room de {@link EventoEstudioRepository} (HU-39, RN-22, RNF-12).
 */
public final class EventoEstudioRepositoryRoom implements EventoEstudioRepository {

    private final EventoEstudioDao dao;

    public EventoEstudioRepositoryRoom(EventoEstudioDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public EventoEstudio guardar(EventoEstudio evento) {
        long id = dao.insertar(Mapeadores.aEntidad(evento));
        return evento.conId(id);
    }

    @Override
    public List<EventoEstudio> listarPorSesion(long sesionId) {
        return mapear(dao.listarPorSesion(sesionId));
    }

    @Override
    public List<EventoEstudio> listarPorRangoSesionesUtc(long desdeUtcMilis, long hastaUtcMilis) {
        return mapear(dao.listarPorRangoSesionesUtc(desdeUtcMilis, hastaUtcMilis));
    }

    private static List<EventoEstudio> mapear(List<EventoEstudioEntity> entidades) {
        List<EventoEstudio> res = new ArrayList<>(entidades.size());
        for (EventoEstudioEntity e : entidades) {
            EventoEstudio dominio = Mapeadores.aDominio(e);
            if (dominio != null) {
                res.add(dominio);
            }
        }
        return res;
    }

    @Override
    public void eliminarPorSesion(long sesionId) {
        dao.eliminarPorSesion(sesionId);
    }

    @Override
    public void eliminarTodos() {
        dao.eliminarTodos();
    }
}
