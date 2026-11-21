package io.github.jhoel0521.studyplan.domain.valueobject;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Pruebas unitarias para los Value Objects temporales {@link Fecha} y {@link Hora}
 * (HU-10, HU-11, RN-04, RN-06, RN-10, RN-13).
 */
public class FechaHoraTest {

    @Test
    public void aceptaAnioBisiestoYRechazaNoBisiestoEnFebrero() {
        Resultado<Fecha> bisiesto2024 = Fecha.de(2024, 2, 29);
        Resultado<Fecha> secularBisiesto2000 = Fecha.de(2000, 2, 29);
        Resultado<Fecha> secularNoBisiesto1900 = Fecha.de(1900, 2, 29);
        Resultado<Fecha> comun2025 = Fecha.de(2025, 2, 29);

        assertTrue(bisiesto2024.esValido());
        assertEquals("2024-02-29", bisiesto2024.valor().iso());
        assertTrue(secularBisiesto2000.esValido());
        assertFalse(secularNoBisiesto1900.esValido());
        assertEquals(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO, secularNoBisiesto1900.error());
        assertFalse(comun2025.esValido());
        assertEquals(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO, comun2025.error());
    }

    @Test
    public void sumarDiasCruzaFinDeMesYFinDeAnioConPrecision() {
        Fecha finDeAnio = Fecha.de(2025, 12, 31).valor();
        assertEquals("2026-01-01", finDeAnio.sumarDias(1).iso());

        Fecha febNoBisiesto = Fecha.de(2026, 2, 28).valor();
        assertEquals("2026-03-01", febNoBisiesto.sumarDias(1).iso());
        assertEquals("2026-02-27", febNoBisiesto.sumarDias(-1).iso());

        Fecha febBisiesto = Fecha.de(2024, 2, 28).valor();
        assertEquals("2024-02-29", febBisiesto.sumarDias(1).iso());
        assertEquals("2024-03-01", febBisiesto.sumarDias(2).iso());
    }

    @Test
    public void calculaLunesYDomingoExactosDeLaSemanaSegunRn13() {
        // 2026-11-12 es jueves: semana va del lunes 2026-11-09 al domingo 2026-11-15
        Fecha jueves = Fecha.de(2026, 11, 12).valor();
        assertEquals(4, jueves.diaDeSemanaIso());
        assertEquals("2026-11-09", jueves.lunesDeSemana().iso());
        assertEquals("2026-11-15", jueves.domingoDeSemana().iso());

        // Misma semana si cae en lunes o domingo
        Fecha lunes = Fecha.de(2026, 11, 9).valor();
        assertEquals("2026-11-09", lunes.lunesDeSemana().iso());
        assertEquals("2026-11-15", lunes.domingoDeSemana().iso());

        Fecha domingo = Fecha.de(2026, 11, 15).valor();
        assertEquals("2026-11-09", domingo.lunesDeSemana().iso());
        assertEquals("2026-11-15", domingo.domingoDeSemana().iso());

        // Cruce de cambio de año (jueves 2026-01-01 -> lunes 2025-12-29, domingo 2026-01-04)
        Fecha primeroEnero = Fecha.de(2026, 1, 1).valor();
        assertEquals("2025-12-29", primeroEnero.lunesDeSemana().iso());
        assertEquals("2026-01-04", primeroEnero.domingoDeSemana().iso());
    }

    @Test
    public void coincideConCalendarioGregorianoEnTodoUnAnioDeReferencia() {
        Fecha inicio = Fecha.de(2024, 1, 1).valor();
        LocalDate referencia = LocalDate.of(2024, 1, 1);
        for (int i = 0; i <= 731; i++) {
            Fecha f = inicio.sumarDias(i);
            LocalDate ld = referencia.plusDays(i);
            assertEquals(ld.toString(), f.iso());
            assertEquals(ld.getDayOfWeek().getValue(), f.diaDeSemanaIso());
            assertEquals(ld.with(DayOfWeek.MONDAY).toString(), f.lunesDeSemana().iso());
            assertEquals(ld.with(DayOfWeek.SUNDAY).toString(), f.domingoDeSemana().iso());
            assertEquals(i, inicio.diasHasta(f));
        }
    }

    @Test
    public void parsearFechaValidaVacioYFormatoInvalido() {
        assertEquals(ErrorValidacion.FECHA_SESION_OBLIGATORIA, Fecha.parsear(null).error());
        assertEquals(ErrorValidacion.FECHA_SESION_OBLIGATORIA, Fecha.parsear("   ").error());
        assertEquals(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO, Fecha.parsear("14/11/2026").error());
        assertEquals(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO, Fecha.parsear("2026-13-01").error());
        assertEquals(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO, Fecha.parsear("2026-04-31").error());

        Fecha parsed = Fecha.parsear(" 2026-11-14 ").valor();
        assertEquals(2026, parsed.anio());
        assertEquals(11, parsed.mes());
        assertEquals(14, parsed.dia());
    }

    @Test
    public void horaValidaLimitesYCalculoDeDuracionRn04() {
        Hora medianoche = Hora.de(0, 0).valor();
        Hora ultimoMinuto = Hora.de(23, 59).valor();

        assertEquals("00:00", medianoche.texto());
        assertEquals("23:59", ultimoMinuto.texto());
        assertEquals(0, medianoche.minutosDesdeMedianoche());
        assertEquals(1439, ultimoMinuto.minutosDesdeMedianoche());
        assertTrue(ultimoMinuto.esPosteriorA(medianoche));
        assertFalse(medianoche.esPosteriorA(medianoche));
        assertFalse(medianoche.esPosteriorA(ultimoMinuto));
        assertEquals(1439, medianoche.minutosHasta(ultimoMinuto));

        Hora diez = Hora.parsear("10:00").valor();
        Hora onceYMedia = Hora.parsear("11:30").valor();
        assertEquals(90, diez.minutosHasta(onceYMedia));
        assertFalse(diez.esPosteriorA(onceYMedia));
    }

    @Test
    public void horaRechazaLimitesVacioYFormatosInvalidos() {
        assertFalse(Hora.de(-1, 0).esValido());
        assertFalse(Hora.de(24, 0).esValido());
        assertFalse(Hora.de(12, 60).esValido());
        assertFalse(Hora.desdeMinutos(-1).esValido());
        assertFalse(Hora.desdeMinutos(1440).esValido());

        assertEquals(ErrorValidacion.HORA_INICIO_OBLIGATORIA,
                Hora.parsear("", ErrorValidacion.HORA_INICIO_OBLIGATORIA).error());
        assertEquals(ErrorValidacion.HORA_FIN_OBLIGATORIA,
                Hora.parsear(null, ErrorValidacion.HORA_FIN_OBLIGATORIA).error());
        assertEquals(ErrorValidacion.HORA_FORMATO_INVALIDO, Hora.parsear("9:30").error());
        assertEquals(ErrorValidacion.HORA_FORMATO_INVALIDO, Hora.parsear("25:00").error());
    }
}
