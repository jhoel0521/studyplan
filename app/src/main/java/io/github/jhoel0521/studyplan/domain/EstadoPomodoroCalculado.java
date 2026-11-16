package io.github.jhoel0521.studyplan.domain;

/**
 * Estado Pomodoro derivado determinísticamente desde el tiempo activo dentro de la app (HU-23, RN-11).
 */
public final class EstadoPomodoroCalculado {

    private final FasePomodoro faseActual;
    private final int cicloActual;
    private final int pomodorosCompletados;
    private final long milisRestantesEnFase;
    private final long milisTrabajoAcumulados;
    private final boolean ciclosPlanificadosCompletados;

    public EstadoPomodoroCalculado(
            FasePomodoro faseActual,
            int cicloActual,
            int pomodorosCompletados,
            long milisRestantesEnFase,
            long milisTrabajoAcumulados,
            boolean ciclosPlanificadosCompletados) {
        this.faseActual = faseActual;
        this.cicloActual = cicloActual;
        this.pomodorosCompletados = pomodorosCompletados;
        this.milisRestantesEnFase = Math.max(0L, milisRestantesEnFase);
        this.milisTrabajoAcumulados = Math.max(0L, milisTrabajoAcumulados);
        this.ciclosPlanificadosCompletados = ciclosPlanificadosCompletados;
    }

    public FasePomodoro faseActual() {
        return faseActual;
    }

    public int cicloActual() {
        return cicloActual;
    }

    public int pomodorosCompletados() {
        return pomodorosCompletados;
    }

    public long milisRestantesEnFase() {
        return milisRestantesEnFase;
    }

    public long milisTrabajoAcumulados() {
        return milisTrabajoAcumulados;
    }

    public boolean ciclosPlanificadosCompletados() {
        return ciclosPlanificadosCompletados;
    }
}
