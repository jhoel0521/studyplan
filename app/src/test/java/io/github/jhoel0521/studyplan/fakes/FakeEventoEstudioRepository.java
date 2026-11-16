package io.github.jhoel0521.studyplan.fakes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;

/**
 * Repositorio falso en memoria de {@link EventoEstudioRepository} para pruebas unitarias (HU-39).
 */
public final class FakeEventoEstudioRepository implements EventoEstudioRepository {

    private final List<EventoEstudio> eventos = new ArrayList<>();
    private long siguienteId = 1L;

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
        res.sort(Comparator.comparingLong(EventoEstudio::instanteMilis)
                .thenComparingLong(e -> e.id() == null ? 0L : e.id()));
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
}
