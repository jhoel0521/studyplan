package io.github.jhoel0521.studyplan.fakes;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.Fecha;

/**
 * Repositorio falso en memoria para {@link TramoDiarioRepository} ({@code RN-31}).
 */
public final class FakeTramoDiarioRepository implements TramoDiarioRepository {

    private final Map<Long, Map<Fecha, Integer>> porSesion = new HashMap<>();

    @Override
    public void guardarTramos(long sesionId, Map<Fecha, Integer> minutosPorDia) {
        if (minutosPorDia == null || minutosPorDia.isEmpty()) {
            porSesion.remove(sesionId);
            return;
        }
        Map<Fecha, Integer> copia = new TreeMap<>();
        for (Map.Entry<Fecha, Integer> e : minutosPorDia.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                copia.put(e.getKey(), e.getValue());
            }
        }
        porSesion.put(sesionId, copia);
    }

    @Override
    public int sumarMinutosEnRango(Fecha desde, Fecha hasta) {
        int total = 0;
        for (Map<Fecha, Integer> tramos : porSesion.values()) {
            for (Map.Entry<Fecha, Integer> e : tramos.entrySet()) {
                if (!e.getKey().esAnteriorA(desde) && !e.getKey().esPosteriorA(hasta)) {
                    total += e.getValue();
                }
            }
        }
        return total;
    }

    @Override
    public Map<Fecha, Integer> sumarPorDiaEnRango(Fecha desde, Fecha hasta) {
        Map<Fecha, Integer> resultado = new TreeMap<>();
        for (Map<Fecha, Integer> tramos : porSesion.values()) {
            for (Map.Entry<Fecha, Integer> e : tramos.entrySet()) {
                if (!e.getKey().esAnteriorA(desde) && !e.getKey().esPosteriorA(hasta)) {
                    Integer prev = resultado.get(e.getKey());
                    resultado.put(e.getKey(), (prev == null ? 0 : prev) + e.getValue());
                }
            }
        }
        return resultado;
    }

    @Override
    public void eliminarPorSesion(long sesionId) {
        porSesion.remove(sesionId);
    }
}
