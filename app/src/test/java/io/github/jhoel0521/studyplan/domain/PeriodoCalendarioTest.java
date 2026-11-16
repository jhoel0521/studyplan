package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias de {@link PeriodoCalendario} y {@link FiltroCalendario} (HU-16 a HU-18, RN-13).
 */
public class PeriodoCalendarioTest {

    @Test
    public void periodoInicialArrancaEnSemanaConFocoEnHoy() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor(); // miércoles
        PeriodoCalendario periodo = PeriodoCalendario.inicial(hoy);

        assertEquals(VistaCalendario.SEMANA, periodo.vista());
        assertEquals(hoy, periodo.fechaFoco());
        assertEquals(hoy, periodo.diaSeleccionado());
        assertEquals(Fecha.de(2026, 11, 16).valor(), periodo.rangoVisibleInicio());
        assertEquals(Fecha.de(2026, 11, 22).valor(), periodo.rangoVisibleFin());
    }

    @Test
    public void retrocederDosMesesYCambiarASemanaSinDiaSeleccionadoTomaPrimeraSemana() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        PeriodoCalendario enMes = PeriodoCalendario.inicial(hoy)
                .cambiarVista(VistaCalendario.MES, hoy)
                .anterior() // octubre 2026
                .anterior(); // septiembre 2026

        assertEquals(VistaCalendario.MES, enMes.vista());
        assertEquals(Fecha.de(2026, 9, 1).valor(), enMes.rangoVisibleInicio());
        assertEquals(Fecha.de(2026, 9, 30).valor(), enMes.rangoVisibleFin());
        assertNull(enMes.diaSeleccionado());

        PeriodoCalendario enSemana = enMes.cambiarVista(VistaCalendario.SEMANA, hoy);
        assertEquals(VistaCalendario.SEMANA, enSemana.vista());
        assertEquals(Fecha.de(2026, 8, 31).valor(), enSemana.rangoVisibleInicio());
        assertEquals(Fecha.de(2026, 9, 6).valor(), enSemana.rangoVisibleFin());
    }

    @Test
    public void conservaDiaSeleccionadoAlCambiarEntreMesSemanaYDia() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        Fecha elegido = Fecha.de(2026, 11, 25).valor(); // miércoles

        PeriodoCalendario mes = PeriodoCalendario.inicial(hoy)
                .cambiarVista(VistaCalendario.MES, hoy)
                .seleccionarDia(elegido);

        PeriodoCalendario semana = mes.cambiarVista(VistaCalendario.SEMANA, hoy);
        assertEquals(Fecha.de(2026, 11, 23).valor(), semana.rangoVisibleInicio());
        assertEquals(Fecha.de(2026, 11, 29).valor(), semana.rangoVisibleFin());
        assertEquals(elegido, semana.diaSeleccionado());

        PeriodoCalendario dia = semana.cambiarVista(VistaCalendario.DIA, hoy);
        assertEquals(elegido, dia.rangoVisibleInicio());
        assertEquals(elegido, dia.rangoVisibleFin());

        PeriodoCalendario deVueltaAMes = dia.cambiarVista(VistaCalendario.MES, hoy);
        assertEquals(Fecha.de(2026, 11, 1).valor(), deVueltaAMes.rangoVisibleInicio());
        assertEquals(elegido, deVueltaAMes.diaSeleccionado());
    }

    @Test
    public void cuadriculaMensualRespetaLunesADomingoYFebreroBisiesto() {
        // Febrero 2024: 1 feb fue jueves, 29 feb fue jueves -> 29 ene (lun) .. 3 mar (dom) = 35 celdas
        Fecha febBisiesto = Fecha.de(2024, 2, 15).valor();
        PeriodoCalendario periodo = PeriodoCalendario.de(VistaCalendario.MES, febBisiesto, null);

        assertEquals(Fecha.de(2024, 2, 1).valor(), periodo.rangoVisibleInicio());
        assertEquals(Fecha.de(2024, 2, 29).valor(), periodo.rangoVisibleFin());
        assertEquals(Fecha.de(2024, 1, 29).valor(), periodo.rangoConsultaInicio());
        assertEquals(Fecha.de(2024, 3, 3).valor(), periodo.rangoConsultaFin());
        int totalCeldas = periodo.rangoConsultaInicio().diasHasta(periodo.rangoConsultaFin()) + 1;
        assertEquals(35, totalCeldas);
        assertEquals(1, periodo.rangoConsultaInicio().diaDeSemanaIso());
        assertEquals(7, periodo.rangoConsultaFin().diaDeSemanaIso());
    }

    @Test
    public void navegacionEntreAniosFuncionaEnMesSemanaYDia() {
        Fecha finDeAnio = Fecha.de(2026, 12, 31).valor();
        PeriodoCalendario dia = PeriodoCalendario.de(VistaCalendario.DIA, finDeAnio, finDeAnio).siguiente();
        assertEquals(Fecha.de(2027, 1, 1).valor(), dia.fechaFoco());

        PeriodoCalendario mesEnero = PeriodoCalendario.de(VistaCalendario.MES, Fecha.de(2027, 1, 1).valor(), null)
                .anterior();
        assertEquals(Fecha.de(2026, 12, 1).valor(), mesEnero.rangoVisibleInicio());
        assertEquals(Fecha.de(2026, 12, 31).valor(), mesEnero.rangoVisibleFin());
    }

    @Test
    public void filtroCalendarioEvaluaMateriaArchivadasYEstados() {
        FiltroCalendario defecto = FiltroCalendario.porDefecto();
        assertFalse(defecto.tieneRestricciones());
        assertTrue(defecto.acepta(10L, true, CodigoEstadoSesion.NO_REALIZADA));

        FiltroCalendario sinArchivadas = defecto.conIncluirArchivadas(false);
        assertTrue(sinArchivadas.tieneRestricciones());
        assertFalse(sinArchivadas.acepta(10L, true, CodigoEstadoSesion.COMPLETADA));
        assertTrue(sinArchivadas.acepta(10L, false, CodigoEstadoSesion.COMPLETADA));

        FiltroCalendario soloMateria5 = sinArchivadas.conMateria(5L);
        assertTrue(soloMateria5.acepta(5L, false, CodigoEstadoSesion.PENDIENTE));
        assertFalse(soloMateria5.acepta(6L, false, CodigoEstadoSesion.PENDIENTE));
    }
}
