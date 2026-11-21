package io.github.jhoel0521.studyplan.domain.valueobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pruebas unitarias de {@link Instante} (RNF-17, RN-31). */
public class InstanteTest {

    @Test
    public void desfaseNegativoCruzaMedianocheUtcCorrectamente() {
        // 2026-11-16 22:00 en UTC-04:00 = 2026-11-17 02:00 UTC
        Fecha fecha = Fecha.de(2026, 11, 16).valor();
        Hora hora = Hora.de(22, 0).valor();
        Instante instante = Instante.desdeFechaYHoraLocal(fecha, hora, -240);

        assertEquals(fecha, instante.fechaLocal());
        assertEquals(hora, instante.horaLocal());
        assertEquals(Fecha.de(2026, 11, 17).valor(), instante.conDesfase(0).fechaLocal());
        assertEquals(Hora.de(2, 0).valor(), instante.conDesfase(0).horaLocal());
    }

    @Test
    public void desfasePositivoYMedianocheCalculanDiaLocalYInicioDeDia() {
        // 2026-11-16 00:30 en UTC+05:30 = 2026-11-15 19:00 UTC
        Fecha fecha = Fecha.de(2026, 11, 16).valor();
        Hora hora = Hora.de(0, 30).valor();
        Instante instante = Instante.desdeFechaYHoraLocal(fecha, hora, 330);

        assertEquals(fecha, instante.fechaLocal());
        assertEquals(hora, instante.horaLocal());

        Instante inicioDia = instante.inicioDelDiaLocal();
        assertEquals(fecha, inicioDia.fechaLocal());
        assertEquals(Hora.de(0, 0).valor(), inicioDia.horaLocal());
        assertEquals(instante.utcMilis() - 30 * 60_000L, inicioDia.utcMilis());
    }

    @Test
    public void fechasAnterioresA1970FuncionanConFloorDivYFloorMod() {
        Fecha fecha = Fecha.de(1969, 12, 31).valor();
        Hora hora = Hora.de(23, 15).valor();
        Instante instante = Instante.desdeFechaYHoraLocal(fecha, hora, -240);

        assertEquals(fecha, instante.fechaLocal());
        assertEquals(hora, instante.horaLocal());
        assertEquals(Fecha.de(1969, 12, 31).valor(), instante.inicioDelDiaLocal().fechaLocal());
        assertEquals(Hora.de(0, 0).valor(), instante.inicioDelDiaLocal().horaLocal());
        assertTrue(instante.utcMilis() > 0); // 1970-01-01 03:15 UTC
    }

    @Test
    public void comparacionUsaUtcMilisAunqueDesfaseDifiera() {
        Instante enLaPaz = Instante.desdeFechaYHoraLocal(
                Fecha.de(2026, 11, 16).valor(), Hora.de(10, 0).valor(), -240); // 14:00 UTC
        Instante enMadrid = Instante.desdeFechaYHoraLocal(
                Fecha.de(2026, 11, 16).valor(), Hora.de(15, 0).valor(), 60);   // 14:00 UTC
        Instante posterior = enMadrid.sumarMinutos(1);

        assertEquals(0, enLaPaz.compareTo(enMadrid));
        assertTrue(enLaPaz.esIgualEnTiempoA(enMadrid));
        assertTrue(enLaPaz.esAnteriorA(posterior));
        assertTrue(posterior.esPosteriorA(enLaPaz));
        assertEquals("UTC\u221204:00", enLaPaz.desfaseTexto());
        assertEquals("UTC+01:00", enMadrid.desfaseTexto());
    }
}
