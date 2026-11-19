package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de dominio inmutable con el resumen tabular por materia y totales de un periodo para
 * reportes de estudio ({@code HU-32}, {@code HU-33}, {@code RN-31}).
 */
public final class ReporteEstudio {

    private final PeriodoEstadisticas periodo;
    private final long generadoEnMilis;
    private final List<DesgloseMateriaPeriodo> materias;
    private final int minutosPlanificadosTotales;
    private final int minutosRealesTotales;
    private final int sesionesNoRealizadasTotales;

    public ReporteEstudio(
            PeriodoEstadisticas periodo,
            long generadoEnMilis,
            List<DesgloseMateriaPeriodo> materias) {
        this(
                periodo,
                generadoEnMilis,
                materias,
                sumarPlanificados(materias),
                sumarReales(materias),
                sumarNoRealizadas(materias));
    }

    public ReporteEstudio(
            PeriodoEstadisticas periodo,
            long generadoEnMilis,
            List<DesgloseMateriaPeriodo> materias,
            int minutosPlanificadosTotales,
            int minutosRealesTotales,
            int sesionesNoRealizadasTotales) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.generadoEnMilis = generadoEnMilis;
        this.materias = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(materias, "materias")));
        this.minutosPlanificadosTotales = Math.max(0, minutosPlanificadosTotales);
        this.minutosRealesTotales = Math.max(0, minutosRealesTotales);
        this.sesionesNoRealizadasTotales = Math.max(0, sesionesNoRealizadasTotales);
    }

    public PeriodoEstadisticas periodo() {
        return periodo;
    }

    public long generadoEnMilis() {
        return generadoEnMilis;
    }

    public List<DesgloseMateriaPeriodo> materias() {
        return materias;
    }

    public int minutosPlanificadosTotales() {
        return minutosPlanificadosTotales;
    }

    public int minutosRealesTotales() {
        return minutosRealesTotales;
    }

    public double horasPlanificadasTotales() {
        return minutosPlanificadosTotales / 60.0;
    }

    public double horasRealesTotales() {
        return minutosRealesTotales / 60.0;
    }

    public int sesionesNoRealizadasTotales() {
        return sesionesNoRealizadasTotales;
    }

    /** Porcentaje global real frente a planificado ({@code null} si no hubo horas planificadas). */
    public Double cumplimientoTotal() {
        if (minutosPlanificadosTotales <= 0) {
            return null;
        }
        return minutosRealesTotales * 100.0 / minutosPlanificadosTotales;
    }

    /** Indica si no existe ninguna actividad ni planificación en el periodo. */
    public boolean esSinDatos() {
        return materias.isEmpty();
    }

    private static int sumarPlanificados(List<DesgloseMateriaPeriodo> materias) {
        Objects.requireNonNull(materias, "materias");
        int total = 0;
        for (DesgloseMateriaPeriodo item : materias) {
            total += Math.max(0, item.minutosPlanificadosDeSesiones());
        }
        return total;
    }

    private static int sumarReales(List<DesgloseMateriaPeriodo> materias) {
        Objects.requireNonNull(materias, "materias");
        int total = 0;
        for (DesgloseMateriaPeriodo item : materias) {
            total += Math.max(0, item.minutosReales());
        }
        return total;
    }

    private static int sumarNoRealizadas(List<DesgloseMateriaPeriodo> materias) {
        Objects.requireNonNull(materias, "materias");
        int total = 0;
        for (DesgloseMateriaPeriodo item : materias) {
            total += Math.max(0, item.sesionesNoRealizadas());
        }
        return total;
    }
}
