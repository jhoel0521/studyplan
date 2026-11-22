package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/** Pruebas de {@link ModeloWidgetProgreso}: estados y porcentaje del widget ({@code HU-05}). */
public class ModeloWidgetProgresoTest {

    private static DetalleSesionInicio sesion() {
        return new DetalleSesionInicio(
                1L, 2L, "Historia", 0xFF1E88E5,
                Fecha.de(2026, 11, 22).valor(),
                Hora.de(10, 0).valor(), Hora.de(11, 30).valor(),
                90, null, CodigoEstadoSesion.PENDIENTE);
    }

    private static ResumenInicio resumen(double horas, double meta, DetalleSesionInicio... proximas) {
        List<DetalleSesionInicio> lista = Arrays.asList(proximas);
        return new ResumenInicio(
                "Ana", horas, meta, Fecha.de(2026, 11, 22).valor(),
                lista.isEmpty() ? null : lista.get(0), Collections.emptyList(), null, lista);
    }

    @Test
    public void sinPerfilInvitaAAbrirLaApp() {
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.empty());

        assertEquals(ModeloWidgetProgreso.Estado.SIN_PERFIL, m.estado());
        assertFalse(m.hayProgreso());
        assertEquals(0, m.proximas().size());
    }

    @Test
    public void metaCeroPideDefinirLaMetaYNoMuestraProgreso() {
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.of(resumen(3, 0, sesion())));

        assertEquals(ModeloWidgetProgreso.Estado.SIN_META, m.estado());
        assertEquals(0, m.porcentaje());
        assertFalse(m.hayProgreso());
    }

    @Test
    public void ceroHorasDaCeroPorCientoConAnillo() {
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.of(resumen(0, 20, sesion())));

        assertEquals(ModeloWidgetProgreso.Estado.NORMAL, m.estado());
        assertEquals(0, m.porcentaje());
        assertTrue(m.hayProgreso());
    }

    @Test
    public void doceDeVeinteEsSesentaPorCientoConLaProximaSesion() {
        DetalleSesionInicio proxima = sesion();
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.of(resumen(12, 20, proxima)));

        assertEquals(ModeloWidgetProgreso.Estado.NORMAL, m.estado());
        assertEquals(60, m.porcentaje());
        assertEquals(12.0, m.horasRealizadas(), 0.0);
        assertEquals(20.0, m.metaHoras(), 0.0);
        assertSame(proxima, m.proximas().get(0));
        assertEquals("2026-11-22", m.hoy().iso());
    }

    @Test
    public void masDeLaMetaSeAcotaACien() {
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.of(resumen(30, 20, sesion())));

        assertEquals(100, m.porcentaje());
    }

    @Test
    public void sinProximaSesionConservaElAnillo() {
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.of(resumen(5, 20)));

        assertEquals(ModeloWidgetProgreso.Estado.SIN_SESION, m.estado());
        assertEquals(25, m.porcentaje());
        assertTrue(m.hayProgreso());
        assertEquals(0, m.proximas().size());
    }

    @Test
    public void conDosProximasLasConservaEnOrden() {
        DetalleSesionInicio primera = sesion();
        DetalleSesionInicio segunda = sesion();
        ModeloWidgetProgreso m = ModeloWidgetProgreso.de(Optional.of(resumen(12, 20, primera, segunda)));

        assertEquals(2, m.proximas().size());
        assertSame(primera, m.proximas().get(0));
        assertSame(segunda, m.proximas().get(1));
    }
}
