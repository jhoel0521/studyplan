package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias de {@link CalculadorPomodoro} (HU-23, RN-11, RN-17, RN-25).
 */
public class CalculadorPomodoroTest {

    private static final long MIN = 60_000L;

    @Test
    public void calculaPomodorosQueCabenEnPlanYExcluyeDescansosDeTrabajo() {
        ConfiguracionPomodoro cfg = ConfiguracionPomodoro.porDefecto(); // 25/5/15 cada 4
        assertEquals(4, CalculadorPomodoro.pomodorosQueCabenEnPlan(120, cfg));
        assertEquals(1, CalculadorPomodoro.pomodorosQueCabenEnPlan(15, cfg));

        // 4 ciclos completos: 4 * 25 trabajo + 3 * 5 corto = 115 min -> entrando al 4.º descanso largo
        EstadoPomodoroCalculado enDescansoLargo = CalculadorPomodoro.calcularEstado(
                116 * MIN, cfg, 4, false);
        assertEquals(FasePomodoro.DESCANSO_LARGO, enDescansoLargo.faseActual());
        assertEquals(4, enDescansoLargo.pomodorosCompletados());
        assertEquals(100 * MIN, enDescansoLargo.milisTrabajoAcumulados());
        assertTrue(enDescansoLargo.ciclosPlanificadosCompletados());

        // Durante el 1.er descanso corto (min 27): solo 25 min de trabajo acumulado (RN-11)
        EstadoPomodoroCalculado enDescansoCorto = CalculadorPomodoro.calcularEstado(
                27 * MIN, cfg, 4, false);
        assertEquals(FasePomodoro.DESCANSO_CORTO, enDescansoCorto.faseActual());
        assertEquals(25 * MIN, enDescansoCorto.milisTrabajoAcumulados());
        assertEquals(3 * MIN, enDescansoCorto.milisRestantesEnFase());
        assertFalse(enDescansoCorto.ciclosPlanificadosCompletados());
    }

    @Test
    public void soportaTiempoExtraLibreTrasCompletarObjetivo() {
        ConfiguracionPomodoro cfg = ConfiguracionPomodoro.porDefecto();
        // Objetivo 1 pomodoro (25 min) + 10 min en tiempo extra libre = 35 min trabajo
        EstadoPomodoroCalculado extra = CalculadorPomodoro.calcularEstado(
                35 * MIN, cfg, 1, true);
        assertEquals(FasePomodoro.TIEMPO_EXTRA_LIBRE, extra.faseActual());
        assertEquals(35 * MIN, extra.milisTrabajoAcumulados());
        assertTrue(extra.ciclosPlanificadosCompletados());
    }

    @Test
    public void detectaDescansoYCalculaFinProyectado() {
        ConfiguracionPomodoro cfg = ConfiguracionPomodoro.porDefecto();
        EstadoPomodoroCalculado descanso = CalculadorPomodoro.calcularEstado(27 * MIN, cfg, 2, false);
        assertTrue(CalculadorPomodoro.estaEnDescanso(descanso.faseActual()));
        long t = 1_000_000L;
        assertEquals(t + 3 * MIN, CalculadorPomodoro.calcularFinDescansoProyectado(t, descanso));

        EstadoPomodoroCalculado trabajo = CalculadorPomodoro.calcularEstado(10 * MIN, cfg, 2, false);
        assertFalse(CalculadorPomodoro.estaEnDescanso(trabajo.faseActual()));
        assertEquals(0L, CalculadorPomodoro.calcularFinDescansoProyectado(t, trabajo));
    }
}
