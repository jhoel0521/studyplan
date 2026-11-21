package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Métricas de concentración, interrupciones, puntualidad, respeto del tiempo y tópicos de un
 * periodo ({@code HU-45}, {@code RN-30}).
 */
public final class ConcentracionPeriodo {

    private final Double indiceConcentracionPorcentaje;
    private final double interrupcionesPorHora;
    private final int totalInterrupciones;
    private final Double puntualidadPromedioMinutos;
    private final Double respetoDelTiempoPorcentaje;
    private final int tiempoFueraAppMinutos;
    private final int estudioFueraAppMinutos;
    private final int estudioFueraSinClasificarMinutos;
    private final List<ResumenTopico> estudioFueraPorTopico;
    private final List<ResumenTopico> resumenTopicosInterrupciones;

    public ConcentracionPeriodo(
            Double indiceConcentracionPorcentaje,
            double interrupcionesPorHora,
            int totalInterrupciones,
            Double puntualidadPromedioMinutos,
            Double respetoDelTiempoPorcentaje,
            int tiempoFueraAppMinutos,
            int estudioFueraAppMinutos,
            int estudioFueraSinClasificarMinutos,
            List<ResumenTopico> estudioFueraPorTopico,
            List<ResumenTopico> resumenTopicosInterrupciones) {
        this.indiceConcentracionPorcentaje = indiceConcentracionPorcentaje;
        this.interrupcionesPorHora = Math.max(0.0, interrupcionesPorHora);
        this.totalInterrupciones = Math.max(0, totalInterrupciones);
        this.puntualidadPromedioMinutos = puntualidadPromedioMinutos;
        this.respetoDelTiempoPorcentaje = respetoDelTiempoPorcentaje;
        this.tiempoFueraAppMinutos = Math.max(0, tiempoFueraAppMinutos);
        this.estudioFueraAppMinutos = Math.max(0, estudioFueraAppMinutos);
        this.estudioFueraSinClasificarMinutos = Math.max(0, estudioFueraSinClasificarMinutos);
        this.estudioFueraPorTopico = estudioFueraPorTopico == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(estudioFueraPorTopico));
        this.resumenTopicosInterrupciones = resumenTopicosInterrupciones == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(resumenTopicosInterrupciones));
    }

    public Double indiceConcentracionPorcentaje() {
        return indiceConcentracionPorcentaje;
    }

    public double interrupcionesPorHora() {
        return interrupcionesPorHora;
    }

    public int totalInterrupciones() {
        return totalInterrupciones;
    }

    public Double puntualidadPromedioMinutos() {
        return puntualidadPromedioMinutos;
    }

    public Double respetoDelTiempoPorcentaje() {
        return respetoDelTiempoPorcentaje;
    }

    public int tiempoFueraAppMinutos() {
        return tiempoFueraAppMinutos;
    }

    public int estudioFueraAppMinutos() {
        return estudioFueraAppMinutos;
    }

    public int estudioFueraSinClasificarMinutos() {
        return estudioFueraSinClasificarMinutos;
    }

    public List<ResumenTopico> estudioFueraPorTopico() {
        return estudioFueraPorTopico;
    }

    public List<ResumenTopico> resumenTopicosInterrupciones() {
        return resumenTopicosInterrupciones;
    }
}
