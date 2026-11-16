package io.github.jhoel0521.studyplan.domain;

/**
 * Derivación determinista de fases Pomodoro sin escrituras por tick (HU-23, RN-11, RN-17, RN-25).
 */
public final class CalculadorPomodoro {

    private static final long MINUTO_MILIS = 60_000L;

    private CalculadorPomodoro() {
    }

    /** Cantidad de bloques de trabajo que caben en la duración planificada (mínimo 1). */
    public static int pomodorosQueCabenEnPlan(
            int duracionPlanificadaMinutos, ConfiguracionPomodoro config) {
        ConfiguracionPomodoro cfg = config == null ? ConfiguracionPomodoro.porDefecto() : config;
        return Math.max(1, duracionPlanificadaMinutos / cfg.trabajoMinutos());
    }

    /**
     * Deriva la fase actual, el ciclo y el tiempo de trabajo efectivo acumulado (excluyendo
     * descansos según RN-11) a partir de {@code milisActivosDentroApp}.
     */
    public static EstadoPomodoroCalculado calcularEstado(
            long milisActivosDentroApp,
            ConfiguracionPomodoro config,
            int pomodorosObjetivo,
            boolean enTiempoExtraLibre) {
        ConfiguracionPomodoro cfg = config == null ? ConfiguracionPomodoro.porDefecto() : config;
        int objetivo = Math.max(1, pomodorosObjetivo);
        long trabajoMs = cfg.trabajoMinutos() * MINUTO_MILIS;
        long cortoMs = cfg.descansoCortoMinutos() * MINUTO_MILIS;
        long largoMs = cfg.descansoLargoMinutos() * MINUTO_MILIS;

        long restante = Math.max(0L, milisActivosDentroApp);
        long trabajoAcumulado = 0L;
        int completados = 0;
        int ciclo = 1;

        while (true) {
            if (restante < trabajoMs) {
                return new EstadoPomodoroCalculado(
                        FasePomodoro.TRABAJO,
                        ciclo,
                        completados,
                        trabajoMs - restante,
                        trabajoAcumulado + restante,
                        completados >= objetivo);
            }
            restante -= trabajoMs;
            trabajoAcumulado += trabajoMs;
            completados++;

            if (completados >= objetivo && enTiempoExtraLibre) {
                return new EstadoPomodoroCalculado(
                        FasePomodoro.TIEMPO_EXTRA_LIBRE,
                        ciclo,
                        completados,
                        0L,
                        trabajoAcumulado + restante,
                        true);
            }

            long descansoMs = (ciclo % cfg.ciclosHastaLargo() == 0) ? largoMs : cortoMs;
            FasePomodoro faseDescanso = (ciclo % cfg.ciclosHastaLargo() == 0)
                    ? FasePomodoro.DESCANSO_LARGO
                    : FasePomodoro.DESCANSO_CORTO;

            if (restante < descansoMs) {
                return new EstadoPomodoroCalculado(
                        faseDescanso,
                        ciclo,
                        completados,
                        descansoMs - restante,
                        trabajoAcumulado,
                        completados >= objetivo);
            }
            restante -= descansoMs;
            ciclo++;
        }
    }
}
