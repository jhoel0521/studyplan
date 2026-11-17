package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias para sesiones sin horario planificado ({@code HU-47}, {@code HU-48},
 * {@code RN-05}, {@code RN-07}, {@code RN-08}).
 */
public class SesionEstudioSinPlanTest {

    @Test
    public void estudiarAhoraCreaSesionEnCursoSinPlanYSeExcluyeDeCrucesYVencimiento() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        Instante inicio = Instante.desdeFechaYHoraLocal(hoy, Hora.de(10, 40).valor(), -240);

        Resultado<SesionEstudio> res = SesionEstudio.estudiarAhora(
                1L,
                inicio,
                TecnicaEstudio.POMODORO,
                ConfiguracionPomodoro.porDefecto(),
                PreferenciaNoMolestar.HEREDAR_GLOBAL);

        assertTrue(res.esValido());
        SesionEstudio s = res.valor().conId(50L);
        assertEquals(CodigoEstadoSesion.EN_CURSO, s.codigoEstado());
        assertFalse(s.tienePlan());
        assertNull(s.planFinUtcMilis());
        assertEquals(0, s.duracionPlanificadaMinutos());
        assertEquals(TecnicaEstudio.POMODORO, s.tecnica());

        // No genera cruce horario (RN-05)
        Instante cruceIni = Instante.desdeFechaYHoraLocal(hoy, Hora.de(10, 0).valor(), -240);
        Instante cruceFin = Instante.desdeFechaYHoraLocal(hoy, Hora.de(11, 30).valor(), -240);
        assertFalse(s.seSolapaCon(cruceIni, cruceFin));

        // No vence por RN-08
        assertFalse(s.vencer().esValido());

        // Al detenerse, suma minutos reales pero 0 minutos planificados
        SesionEstudio completada = s.detener(50).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, completada.codigoEstado());
        assertEquals(50, completada.minutosRealesContabilizables());
        assertEquals(0, completada.duracionPlanificadaMinutos());
    }
}
