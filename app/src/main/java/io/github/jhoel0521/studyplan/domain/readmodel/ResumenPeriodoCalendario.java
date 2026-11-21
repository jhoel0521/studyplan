package io.github.jhoel0521.studyplan.domain.readmodel;

/**
 * Totales de tiempo planificado y realizado del periodo visible del calendario (HU-21, RN-11).
 */
public final class ResumenPeriodoCalendario {

    private final int minutosPlanificados;
    private final int minutosRealizados;

    public ResumenPeriodoCalendario(int minutosPlanificados, int minutosRealizados) {
        this.minutosPlanificados = Math.max(0, minutosPlanificados);
        this.minutosRealizados = Math.max(0, minutosRealizados);
    }

    public int minutosPlanificados() {
        return minutosPlanificados;
    }

    public int minutosRealizados() {
        return minutosRealizados;
    }

    public double horasPlanificadas() {
        return minutosPlanificados / 60.0;
    }

    public double horasRealizadas() {
        return minutosRealizados / 60.0;
    }
}
