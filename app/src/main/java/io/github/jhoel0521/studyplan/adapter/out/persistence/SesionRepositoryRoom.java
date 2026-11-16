package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Adaptador de salida Room que implementa {@link SesionRepository} sobre {@link SesionDao}
 * (DIP, HU-02, HU-09 a HU-15, RNF-12).
 */
public final class SesionRepositoryRoom implements SesionRepository {

    private final SesionDao dao;

    public SesionRepositoryRoom(SesionDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public SesionEstudio guardar(SesionEstudio sesion) {
        long id = dao.insertar(Mapeadores.aEntidad(sesion));
        return sesion.conId(id);
    }

    @Override
    public void actualizar(SesionEstudio sesion) {
        dao.actualizar(Mapeadores.aEntidad(sesion));
    }

    @Override
    public SesionEstudio reprogramar(
            SesionEstudio originalReprogramada, SesionEstudio nuevaPendiente) {
        long nuevoId = dao.reprogramar(
                Mapeadores.aEntidad(originalReprogramada),
                Mapeadores.aEntidad(nuevaPendiente));
        return nuevaPendiente.conId(nuevoId);
    }

    @Override
    public Optional<SesionEstudio> buscarPorId(long id) {
        SesionEntity entidad = dao.buscarPorId(id);
        return entidad == null ? Optional.empty() : Optional.ofNullable(Mapeadores.aDominio(entidad));
    }

    @Override
    public Optional<SesionEstudio> buscarReprogramadaHijaDe(long sesionOrigenId) {
        SesionEntity entidad = dao.buscarReprogramadaHijaDe(sesionOrigenId);
        return entidad == null ? Optional.empty() : Optional.ofNullable(Mapeadores.aDominio(entidad));
    }

    @Override
    public List<SesionEstudio> listarPorFecha(Fecha fecha) {
        return mapearLista(dao.listarPorFecha(fecha.iso()));
    }

    @Override
    public List<SesionEstudio> listarPorRango(Fecha desde, Fecha hasta) {
        return java.util.Collections.emptyList();
    }

    @Override
    public List<SesionEstudio> listarPendientesDesde(Fecha desde) {
        return mapearLista(dao.listarPendientesDesde(desde.iso()));
    }

    @Override
    public List<SesionEstudio> listarPendientesAnterioresA(Fecha hoy) {
        return mapearLista(dao.listarPendientesAnterioresA(hoy.iso()));
    }

    @Override
    public int sumarMinutosRealesPorMateria(long materiaId) {
        return dao.sumarMinutosRealesPorMateria(materiaId);
    }

    @Override
    public int sumarMinutosRealesEnRango(Fecha desde, Fecha hasta) {
        return dao.sumarMinutosRealesEnRango(desde.iso(), hasta.iso());
    }

    @Override
    public void eliminarPorId(long id) {
        dao.eliminarPorId(id);
    }

    private static List<SesionEstudio> mapearLista(List<SesionEntity> entidades) {
        List<SesionEstudio> resultado = new ArrayList<>(entidades.size());
        for (SesionEntity entidad : entidades) {
            SesionEstudio sesion = Mapeadores.aDominio(entidad);
            if (sesion != null) {
                resultado.add(sesion);
            }
        }
        return resultado;
    }
}
