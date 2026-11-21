package io.github.jhoel0521.studyplan.domain.enumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.readmodel.AvisoRecordatorioPendiente;

/**
 * Pruebas unitarias de {@link AntelacionRecordatorio} y {@link AvisoRecordatorioPendiente}
 * ({@code HU-34}, {@code RNF-15}).
 */
public class AntelacionRecordatorioTest {

    @Test
    public void propiedadesYConversionNullablePorOpcion() {
        assertFalse(AntelacionRecordatorio.SIN_RECORDATORIO.activo());
        assertEquals(0, AntelacionRecordatorio.SIN_RECORDATORIO.minutos());
        assertNull(AntelacionRecordatorio.SIN_RECORDATORIO.aMinutosNullable());
        assertEquals(0L, AntelacionRecordatorio.SIN_RECORDATORIO.milisAntes());

        assertTrue(AntelacionRecordatorio.MINUTOS_5.activo());
        assertEquals(Integer.valueOf(5), AntelacionRecordatorio.MINUTOS_5.aMinutosNullable());
        assertEquals(5 * 60_000L, AntelacionRecordatorio.MINUTOS_5.milisAntes());

        assertTrue(AntelacionRecordatorio.MINUTOS_15.activo());
        assertEquals(Integer.valueOf(15), AntelacionRecordatorio.MINUTOS_15.aMinutosNullable());
        assertEquals(15 * 60_000L, AntelacionRecordatorio.MINUTOS_15.milisAntes());

        assertTrue(AntelacionRecordatorio.MINUTOS_30.activo());
        assertEquals(Integer.valueOf(30), AntelacionRecordatorio.MINUTOS_30.aMinutosNullable());
        assertEquals(30 * 60_000L, AntelacionRecordatorio.MINUTOS_30.milisAntes());
    }

    @Test
    public void deMinutosMapeaNulosCerosYRespaldoAnteDesconocidos() {
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                AntelacionRecordatorio.deMinutos(null, AntelacionRecordatorio.MINUTOS_15));
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                AntelacionRecordatorio.deMinutos(0, AntelacionRecordatorio.MINUTOS_15));
        assertEquals(
                AntelacionRecordatorio.MINUTOS_5,
                AntelacionRecordatorio.deMinutos(5, AntelacionRecordatorio.SIN_RECORDATORIO));
        assertEquals(
                AntelacionRecordatorio.MINUTOS_15,
                AntelacionRecordatorio.deMinutos(15, AntelacionRecordatorio.SIN_RECORDATORIO));
        assertEquals(
                AntelacionRecordatorio.MINUTOS_30,
                AntelacionRecordatorio.deMinutos(30, AntelacionRecordatorio.SIN_RECORDATORIO));
        assertEquals(
                AntelacionRecordatorio.MINUTOS_15,
                AntelacionRecordatorio.deMinutos(99, AntelacionRecordatorio.MINUTOS_15));
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                AntelacionRecordatorio.deMinutos(-5, AntelacionRecordatorio.SIN_RECORDATORIO));
    }

    @Test
    public void dtoAvisoRecordatorioPendienteNormalizaValoresYEquals() {
        AvisoRecordatorioPendiente a =
                new AvisoRecordatorioPendiente(10L, "Física", "10:00 – 11:00");
        AvisoRecordatorioPendiente b =
                new AvisoRecordatorioPendiente(10L, "Física", "10:00 – 11:00");
        assertEquals(10L, a.sesionId());
        assertEquals("Física", a.nombreMateria());
        assertEquals("10:00 – 11:00", a.horarioLocal());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
