package io.github.jhoel0521.studyplan.fakes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Repositorio falso en memoria de {@link EventoEstudioRepository} para pruebas unitarias (HU-39).
 */
public final class FakeEventoEstudioRepository implements EventoEstudioRepository {

    private final List<EventoEstudio> eventos = new ArrayList<>();
    private final SesionRepository sesiones;
    private long siguienteId = 1L;

    public FakeEventoEstudioRepository() {
        this(null);
    }

    public FakeEventoEstudioRepository(SesionRepository sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    public EventoEstudio guardar(EventoEstudio evento) {
        EventoEstudio guardado = evento.id() == null ? evento.conId(siguienteId++) : evento;
        eventos.add(guardado);
        return guardado;
    }

    @Override
    public List<EventoEstudio> listarPorSesion(long sesionId) {
        List<EventoEstudio> res = new ArrayList<>();
        for (EventoEstudio e : eventos) {
            if (e.sesionId() == sesionId) {
                res.add(e);
            }
        }
        ordenar(res);
        return res;
    }

    @Override
    public List<EventoEstudio> listarPorRangoSesionesUtc(long desdeUtcMilis, long hastaUtcMilis) {
        List<EventoEstudio> res = new ArrayList<>();
        if (sesiones != null) {
            Set<Long> ids = new HashSet<>();
            for (SesionEstudio s : sesiones.listarPorRangoUtc(desdeUtcMilis, hastaUtcMilis)) {
                if (s.id() != null) {
                    ids.add(s.id());
                }
            }
            for (EventoEstudio e : eventos) {
                if (ids.contains(e.sesionId())) {
                    res.add(e);
                }
            }
        } else {
            for (EventoEstudio e : eventos) {
                if (e.instanteMilis() >= desdeUtcMilis && e.instanteMilis() <= hastaUtcMilis) {
                    res.add(e);
                }
            }
        }
        ordenar(res);
        return res;
    }

    @Override
    public void eliminarPorSesion(long sesionId) {
        eventos.removeIf(e -> e.sesionId() == sesionId);
    }

    @Override
    public void eliminarTodos() {
        eventos.clear();
    }

    private static void ordenar(List<EventoEstudio> res) {
        res.sort(Comparator.comparingLong(EventoEstudio::instanteMilis)
                .thenComparingLong(e -> e.id() == null ? 0L : e.id()));
    }
}
