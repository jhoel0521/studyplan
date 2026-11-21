package io.github.jhoel0521.studyplan.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.reportes.GenerarReporteEstudio;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorEstadisticas;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link GenerarReporteEstudio}: vence sesiones pendientes vencidas ({@code RN-08}),
 * suma tramos reales diarios ({@code RN-31}) y delega el cálculo tabular en
 * {@link CalculadorEstadisticas#calcularReporte} ({@code HU-32}).
 */
public final class GenerarReporteEstudioService implements GenerarReporteEstudio {

    private final VencerSesionesPendientes vencerPendientes;
    private final TramoDiarioRepository tramos;
    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;

    public GenerarReporteEstudioService(
            VencerSesionesPendientes vencerPendientes,
            TramoDiarioRepository tramos,
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj) {
        this.vencerPendientes = Objects.requireNonNull(vencerPendientes, "vencerPendientes");
        this.tramos = Objects.requireNonNull(tramos, "tramos");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ReporteEstudio ejecutar(PeriodoEstadisticas periodo) {
        Objects.requireNonNull(periodo, "periodo");
        vencerPendientes.ejecutar();

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

        return CalculadorEstadisticas.calcularReporte(
                periodo,
                reloj.ahoraMilis(),
                porMateria,
                catalogo,
                sesionesPeriodo);
    }
}
