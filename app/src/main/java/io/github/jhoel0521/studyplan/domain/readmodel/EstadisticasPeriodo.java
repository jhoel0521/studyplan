package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;

/**
 * Agregado completo de estadísticas de un periodo ({@code HU-29}, {@code HU-30}, {@code HU-31},
 * {@code HU-45}, {@code RN-30}, {@code RN-31}) y base reutilizable para reportes ({@code HU-32}).
 */
public final class EstadisticasPeriodo {

    private final PeriodoEstadisticas periodo;
    private final int minutosRealesTotales;
    private final int minutosRealesPeriodoAnterior;
    private final Double variacionPorcentaje;
    private final List<DesgloseMateriaPeriodo> distribucionMaterias;
    private final int completadas;
    private final int completadasTardias;
    private final int noRealizadas;
    private final int reprogramadas;
    private final int pendientesOVigentes;
    private final Double tasaCumplimientoPorcentaje;
    private final ConcentracionPeriodo concentracion;

    public EstadisticasPeriodo(
            PeriodoEstadisticas periodo,
            int minutosRealesTotales,
            int minutosRealesPeriodoAnterior,
            Double variacionPorcentaje,
            List<DesgloseMateriaPeriodo> distribucionMaterias,
            int completadas,
            int completadasTardias,
            int noRealizadas,
            int reprogramadas,
            int pendientesOVigentes,
            Double tasaCumplimientoPorcentaje,
            ConcentracionPeriodo concentracion) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.minutosRealesTotales = Math.max(0, minutosRealesTotales);
        this.minutosRealesPeriodoAnterior = Math.max(0, minutosRealesPeriodoAnterior);
        this.variacionPorcentaje = variacionPorcentaje;
        this.distribucionMaterias = distribucionMaterias == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(distribucionMaterias));
        this.completadas = Math.max(0, completadas);
        this.completadasTardias = Math.max(0, completadasTardias);
        this.noRealizadas = Math.max(0, noRealizadas);
        this.reprogramadas = Math.max(0, reprogramadas);
        this.pendientesOVigentes = Math.max(0, pendientesOVigentes);
        this.tasaCumplimientoPorcentaje = tasaCumplimientoPorcentaje;
        this.concentracion = Objects.requireNonNull(concentracion, "concentracion");
    }

    public PeriodoEstadisticas periodo() {
        return periodo;
    }

    public int minutosRealesTotales() {
        return minutosRealesTotales;
    }

    public double horasRealesTotales() {
        return minutosRealesTotales / 60.0;
    }

    public int minutosRealesPeriodoAnterior() {
        return minutosRealesPeriodoAnterior;
    }

    public Double variacionPorcentaje() {
        return variacionPorcentaje;
    }

    public List<DesgloseMateriaPeriodo> distribucionMaterias() {
        return distribucionMaterias;
    }

    public int completadas() {
        return completadas;
    }

    public int completadasTardias() {
        return completadasTardias;
    }

    public int noRealizadas() {
        return noRealizadas;
    }

    public int reprogramadas() {
        return reprogramadas;
    }

    public int pendientesOVigentes() {
        return pendientesOVigentes;
    }

    public int totalCerradas() {
        return completadas + completadasTardias + noRealizadas + reprogramadas;
    }

    public int totalCumplidas() {
        return completadas + completadasTardias;
    }

    public Double tasaCumplimientoPorcentaje() {
        return tasaCumplimientoPorcentaje;
    }

    public ConcentracionPeriodo concentracion() {
        return concentracion;
    }
}
