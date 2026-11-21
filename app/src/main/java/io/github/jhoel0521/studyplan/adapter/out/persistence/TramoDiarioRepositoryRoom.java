package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Adaptador de salida Room para {@link TramoDiarioRepository} ({@code RN-31}, {@code RNF-02}).
 */
public final class TramoDiarioRepositoryRoom implements TramoDiarioRepository {

    private final TramoDiarioDao dao;

    public TramoDiarioRepositoryRoom(TramoDiarioDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public void guardarTramos(long sesionId, Map<Fecha, Integer> minutosPorDia) {
        List<TramoDiarioEntity> entidades = new ArrayList<>();
        if (minutosPorDia != null) {
            for (Map.Entry<Fecha, Integer> entry : minutosPorDia.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    TramoDiarioEntity e = new TramoDiarioEntity();
                    e.sesionId = sesionId;
                    e.fechaLocal = entry.getKey().iso();
                    e.minutos = entry.getValue();
                    entidades.add(e);
                }
            }
        }
        dao.reemplazarPorSesion(sesionId, entidades);
    }

    @Override
    public int sumarMinutosEnRango(Fecha desde, Fecha hasta) {
        return dao.sumarMinutosEnRango(desde.iso(), hasta.iso());
    }

    @Override
    public Map<Fecha, Integer> sumarPorDiaEnRango(Fecha desde, Fecha hasta) {
        Map<Fecha, Integer> resultado = new TreeMap<>();
        for (TramoDiarioEntity e : dao.listarEnRango(desde.iso(), hasta.iso())) {
            Resultado<Fecha> fecha = Fecha.parsear(e.fechaLocal);
            if (!fecha.esValido() || e.minutos <= 0) {
                continue;
            }
            Integer prev = resultado.get(fecha.valor());
            resultado.put(fecha.valor(), (prev == null ? 0 : prev) + e.minutos);
        }
        return resultado;
    }

    @Override
    public Map<Long, Integer> sumarPorMateriaEnRango(Fecha desde, Fecha hasta) {
        Map<Long, Integer> resultado = new TreeMap<>();
        for (TramoDiarioDao.MinutosPorMateria fila : dao.sumarPorMateriaEnRango(desde.iso(), hasta.iso())) {
            if (fila.totalMinutos > 0) {
                resultado.put(fila.materiaId, fila.totalMinutos);
            }
        }
        return resultado;
    }

    @Override
    public void eliminarPorSesion(long sesionId) {
        dao.eliminarPorSesion(sesionId);
    }
}
