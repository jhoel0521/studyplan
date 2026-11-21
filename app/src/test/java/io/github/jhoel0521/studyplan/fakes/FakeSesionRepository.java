package io.github.jhoel0521.studyplan.fakes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Repositorio falso en memoria para {@link SesionRepository} en pruebas unitarias del núcleo y de
 * los ViewModels (HU-02, HU-09, HU-10 a HU-15).
 */
public final class FakeSesionRepository implements SesionRepository {

    private final List<SesionEstudio> sesiones = new ArrayList<>();
    private long siguienteId = 1L;

    @Override
    public SesionEstudio guardar(SesionEstudio sesion) {
        SesionEstudio guardada = sesion.id() == null ? sesion.conId(siguienteId++) : sesion;
        for (int i = 0; i < sesiones.size(); i++) {
            if (Objects.equals(sesiones.get(i).id(), guardada.id())) {
                sesiones.set(i, guardada);
                return guardada;
            }
        }
        sesiones.add(guardada);
        return guardada;
    }

    @Override
    public void actualizar(SesionEstudio sesion) {
        for (int i = 0; i < sesiones.size(); i++) {
            if (Objects.equals(sesiones.get(i).id(), sesion.id())) {
                sesiones.set(i, sesion);
                return;
            }
        }
        throw new IllegalStateException("No existe la sesión a actualizar: " + sesion.id());
    }

    @Override
    public SesionEstudio reprogramar(
            SesionEstudio originalReprogramada, SesionEstudio nuevaPendiente) {
        actualizar(originalReprogramada);
        return guardar(nuevaPendiente);
    }

    @Override
    public Optional<SesionEstudio> buscarPorId(long id) {
        for (SesionEstudio s : sesiones) {
            if (Objects.equals(s.id(), id)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SesionEstudio> buscarReprogramadaHijaDe(long sesionOrigenId) {
        for (SesionEstudio s : sesiones) {
            if (Objects.equals(s.sesionOrigenId(), sesionOrigenId)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<SesionEstudio> listarPorFecha(Fecha fecha) {
        List<SesionEstudio> resultado = new ArrayList<>();
        for (SesionEstudio s : sesiones) {
            if (!s.fecha().esPosteriorA(fecha) && !s.fechaFinLocal().esAnteriorA(fecha)) {
                resultado.add(s);
            }
        }
        resultado.sort(Comparator.comparingLong(SesionEstudio::planInicioUtcMilis));
        return resultado;
    }

    @Override
    public List<SesionEstudio> listarPorRango(Fecha desde, Fecha hasta) {
        List<SesionEstudio> resultado = new ArrayList<>();
        for (SesionEstudio s : sesiones) {
            if (!s.fecha().esPosteriorA(hasta) && !s.fechaFinLocal().esAnteriorA(desde)) {
                resultado.add(s);
            }
        }
        resultado.sort(Comparator.comparingLong(SesionEstudio::planInicioUtcMilis));
        return resultado;
    }

    @Override
    public List<SesionEstudio> listarPorRangoUtc(long desdeUtcMilis, long hastaUtcMilis) {
        List<SesionEstudio> resultado = new ArrayList<>();
        for (SesionEstudio s : sesiones) {
            long fin = s.planFinUtcMilis() == null ? s.planInicioUtcMilis() : s.planFinUtcMilis();
            if (fin >= desdeUtcMilis && s.planInicioUtcMilis() <= hastaUtcMilis) {
                resultado.add(s);
            }
        }
        resultado.sort(Comparator.comparingLong(SesionEstudio::planInicioUtcMilis));
        return resultado;
    }

    @Override
    public List<SesionEstudio> listarPendientesDesde(Fecha desde) {
        List<SesionEstudio> resultado = new ArrayList<>();
        for (SesionEstudio s : sesiones) {
            if (s.codigoEstado() == CodigoEstadoSesion.PENDIENTE
                    && !s.fechaFinLocal().esAnteriorA(desde)) {
                resultado.add(s);
            }
        }
        resultado.sort(Comparator.comparingLong(SesionEstudio::planInicioUtcMilis));
        return resultado;
    }

    @Override
    public List<SesionEstudio> listarPendientesAnterioresA(Fecha hoy) {
        List<SesionEstudio> resultado = new ArrayList<>();
        for (SesionEstudio s : sesiones) {
            if (s.codigoEstado() == CodigoEstadoSesion.PENDIENTE
                    && s.fechaFinLocal().esAnteriorA(hoy)) {
                resultado.add(s);
            }
        }
        return resultado;
    }

    @Override
    public int sumarMinutosRealesPorMateria(long materiaId) {
        int total = 0;
        for (SesionEstudio s : sesiones) {
            if (s.materiaId() == materiaId) {
                total += s.minutosRealesContabilizables();
            }
        }
        return total;
    }

    @Override
    public int sumarMinutosRealesEnRango(Fecha desde, Fecha hasta) {
        int total = 0;
        for (SesionEstudio s : sesiones) {
            if (!s.fecha().esAnteriorA(desde) && !s.fecha().esPosteriorA(hasta)) {
                total += s.minutosRealesContabilizables();
            }
        }
        return total;
    }

    @Override
    public void eliminarPorId(long id) {
        Iterator<SesionEstudio> it = sesiones.iterator();
        while (it.hasNext()) {
            if (Objects.equals(it.next().id(), id)) {
                it.remove();
                return;
            }
        }
    }

    @Override
    public Optional<SesionEstudio> buscarEnCurso() {
        for (SesionEstudio s : sesiones) {
            if (s.codigoEstado() == CodigoEstadoSesion.EN_CURSO) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<SesionEstudio> listarEnCurso() {
        List<SesionEstudio> resultado = new ArrayList<>();
        for (SesionEstudio s : sesiones) {
            if (s.codigoEstado() == CodigoEstadoSesion.EN_CURSO) {
                resultado.add(s);
            }
        }
        return resultado;
    }

    @Override
    public void completarReparto(
            SesionEstudio primeraCompletada, SesionEstudio segundaCompletada) {
        actualizar(primeraCompletada);
        actualizar(segundaCompletada);
    }
}
