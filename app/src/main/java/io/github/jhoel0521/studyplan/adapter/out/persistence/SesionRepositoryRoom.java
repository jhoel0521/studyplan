package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Instante;
import io.github.jhoel0521.studyplan.domain.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Adaptador de salida Room que implementa {@link SesionRepository} sobre {@link SesionDao}
 * (DIP, HU-02, HU-09 a HU-15, RNF-12).
 */
public final class SesionRepositoryRoom implements SesionRepository {

    private static final long MARGEN_DESFASE_MILIS =
            Instante.DESFASE_MAXIMO_MINUTOS * Instante.MILIS_POR_MINUTO;

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
        return listarPorRango(fecha, fecha);
    }

    @Override
    public List<SesionEstudio> listarPorRango(Fecha desde, Fecha hasta) {
        long desdeUtc = Instante.desdeFechaYHoraLocal(
                desde, Hora.de(0, 0).valor(), 0).utcMilis() - MARGEN_DESFASE_MILIS;
        long hastaUtc = Instante.desdeFechaYHoraLocal(
                hasta.sumarDias(1), Hora.de(0, 0).valor(), 0).utcMilis() + MARGEN_DESFASE_MILIS;
        List<SesionEstudio> candidatas = mapearLista(dao.listarPorRangoUtc(desdeUtc, hastaUtc));
        List<SesionEstudio> filtradas = new ArrayList<>();
        for (SesionEstudio s : candidatas) {
            if (!s.fecha().esPosteriorA(hasta) && !s.fechaFinLocal().esAnteriorA(desde)) {
                filtradas.add(s);
            }
        }
        return filtradas;
    }

    @Override
    public List<SesionEstudio> listarPorRangoUtc(long desdeUtcMilis, long hastaUtcMilis) {
        return mapearLista(dao.listarPorRangoUtc(desdeUtcMilis, hastaUtcMilis));
    }

    @Override
    public List<SesionEstudio> listarPendientesDesde(Fecha desde) {
        long desdeUtc = Instante.desdeFechaYHoraLocal(
                desde, Hora.de(0, 0).valor(), 0).utcMilis() - MARGEN_DESFASE_MILIS;
        List<SesionEstudio> candidatas = mapearLista(dao.listarPendientesDesdeUtc(desdeUtc));
        List<SesionEstudio> filtradas = new ArrayList<>();
        for (SesionEstudio s : candidatas) {
            if (!s.fechaFinLocal().esAnteriorA(desde)) {
                filtradas.add(s);
            }
        }
        return filtradas;
    }

    @Override
    public List<SesionEstudio> listarPendientesAnterioresA(Fecha hoy) {
        long hastaUtc = Instante.desdeFechaYHoraLocal(
                hoy, Hora.de(0, 0).valor(), 0).utcMilis() + MARGEN_DESFASE_MILIS;
        List<SesionEstudio> candidatas = mapearLista(dao.listarPendientesHastaUtc(hastaUtc));
        List<SesionEstudio> filtradas = new ArrayList<>();
        for (SesionEstudio s : candidatas) {
            if (s.fechaFinLocal().esAnteriorA(hoy)) {
                filtradas.add(s);
            }
        }
        return filtradas;
    }

    @Override
    public int sumarMinutosRealesPorMateria(long materiaId) {
        return dao.sumarMinutosRealesPorMateria(materiaId);
    }

    @Override
    public int sumarMinutosRealesEnRango(Fecha desde, Fecha hasta) {
        int total = 0;
        for (SesionEstudio s : listarPorRango(desde, hasta)) {
            int min = s.minutosRealesContabilizables();
            if (min > 0) {
                for (java.util.Map.Entry<Fecha, Integer> e :
                        RepartidorPorDia.repartir(s, null, 0L, min).entrySet()) {
                    if (!e.getKey().esAnteriorA(desde) && !e.getKey().esPosteriorA(hasta)) {
                        total += e.getValue();
                    }
                }
            }
        }
        return total;
    }

    @Override
    public void eliminarPorId(long id) {
        dao.eliminarPorId(id);
    }

    @Override
    public Optional<SesionEstudio> buscarEnCurso() {
        SesionEntity e = dao.buscarEnCurso();
        return e == null ? Optional.empty() : Optional.ofNullable(Mapeadores.aDominio(e));
    }

    @Override
    public List<SesionEstudio> listarEnCurso() {
        return mapearLista(dao.listarEnCurso());
    }

    @Override
    public void completarReparto(
            SesionEstudio primeraCompletada, SesionEstudio segundaCompletada) {
        dao.completarReparto(
                Mapeadores.aEntidad(primeraCompletada),
                Mapeadores.aEntidad(segundaCompletada));
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
