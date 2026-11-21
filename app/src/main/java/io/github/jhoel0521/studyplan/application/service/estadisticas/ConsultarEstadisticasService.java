package io.github.jhoel0521.studyplan.application.service.estadisticas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.estadisticas.ConsultarEstadisticas;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorEstadisticas;
import io.github.jhoel0521.studyplan.domain.readmodel.EstadisticasPeriodo;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link ConsultarEstadisticas}: vence pendientes vencidas ({@code RN-08}), suma tramos
 * diarios ({@code RN-31}) y calcula distribución, cumplimiento y métricas de concentración
 * ({@code HU-29} a {@code HU-31}, {@code HU-45}, {@code RN-30}).
 */
public final class ConsultarEstadisticasService implements ConsultarEstadisticas {

    private final VencerSesionesPendientes vencerPendientes;
    private final TramoDiarioRepository tramos;
    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final EventoEstudioRepository eventos;
    private final Reloj reloj;

    public ConsultarEstadisticasService(
            VencerSesionesPendientes vencerPendientes,
            TramoDiarioRepository tramos,
            SesionRepository sesiones,
            MateriaRepository materias,
            EventoEstudioRepository eventos,
            Reloj reloj) {
        this.vencerPendientes = Objects.requireNonNull(vencerPendientes, "vencerPendientes");
        this.tramos = Objects.requireNonNull(tramos, "tramos");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public EstadisticasPeriodo ejecutar(PeriodoEstadisticas periodo) {
        Objects.requireNonNull(periodo, "periodo");
        vencerPendientes.ejecutar();

        PeriodoEstadisticas anterior = periodo.anterior();
        int minutosActual = tramos.sumarMinutosEnRango(periodo.inicio(), periodo.fin());
        int minutosAnterior = tramos.sumarMinutosEnRango(anterior.inicio(), anterior.fin());
        Map<Long, Integer> porMateria =
                tramos.sumarPorMateriaEnRango(periodo.inicio(), periodo.fin());

        Map<Long, Materia> catalogo = new HashMap<>();
        for (Materia m : materias.activas()) {
            if (m.id() != null) {
                catalogo.put(m.id(), m);
            }
        }
        for (Materia m : materias.archivadas()) {
            if (m.id() != null) {
                catalogo.put(m.id(), m);
            }
        }

        List<SesionEstudio> sesionesPeriodo =
                sesiones.listarPorRango(periodo.inicio(), periodo.fin());

        Hora cero = Hora.de(0, 0).valor();
        long desdeUtc = reloj.instanteDe(periodo.inicio(), cero);
        long hastaUtc = reloj.instanteDe(periodo.fin().sumarDias(1), cero) - 1L;
        List<EventoEstudio> eventosRango =
                eventos.listarPorRangoSesionesUtc(desdeUtc, hastaUtc);
        Map<Long, List<EventoEstudio>> eventosPorSesion = new HashMap<>();
        for (EventoEstudio ev : eventosRango) {
            eventosPorSesion.computeIfAbsent(ev.sesionId(), k -> new ArrayList<>()).add(ev);
        }

        return CalculadorEstadisticas.calcular(
                periodo,
                minutosActual,
                minutosAnterior,
                porMateria,
                catalogo,
                sesionesPeriodo,
                eventosPorSesion,
                reloj.ahoraMilis());
    }
}
