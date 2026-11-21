package io.github.jhoel0521.studyplan.domain.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

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

    /** Sin horario no hay instante de fin: la fecha y la hora de fin caen en el inicio y no se desreferencia nulo. */
    @Test
    public void sinHorarioPlanificadoNoHayInstanteDeFinYLaFechaYHoraDeFinSonLasDelInicio() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        Instante inicio = Instante.desdeFechaYHoraLocal(hoy, Hora.de(10, 40).valor(), -240);
        SesionEstudio s = SesionEstudio.estudiarAhora(
                1L, inicio, TecnicaEstudio.LIBRE, null, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor().conId(51L);

        assertNull(s.finInstante());
        assertEquals(s.fecha(), s.fechaFinLocal());
        assertEquals(s.horaInicio(), s.horaFin());
        assertFalse(s.seSolapaCon(s));
    }

    /** Con horario planificado el instante de fin existe y la fecha y la hora de fin salen de él. */
    @Test
    public void conHorarioPlanificadoElInstanteDeFinExisteYDefineLaFechaYHoraDeFin() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        SesionEstudio s = SesionEstudio.planificar(
                1L, hoy, hoy, Hora.de(9, 0).valor(), Hora.de(10, 30).valor(), "", null).valor();

        assertEquals(hoy, s.fechaFinLocal());
        assertEquals(Hora.de(10, 30).valor(), s.horaFin());
        assertTrue(s.finInstante() != null);
    }
}
