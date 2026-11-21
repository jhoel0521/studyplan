package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;
import io.github.jhoel0521.studyplan.domain.readmodel.BloqueHorarioCalendario;
import io.github.jhoel0521.studyplan.domain.readmodel.ItemSesionCalendario;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenPeriodoCalendario;
import io.github.jhoel0521.studyplan.domain.state.EstadoCompletada;
import io.github.jhoel0521.studyplan.domain.state.EstadoCompletadaTardia;
import io.github.jhoel0521.studyplan.domain.state.EstadoNoRealizada;
import io.github.jhoel0521.studyplan.domain.state.EstadoPendiente;
import io.github.jhoel0521.studyplan.domain.state.EstadoReprogramada;
import io.github.jhoel0521.studyplan.domain.state.EstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/**
 * Pruebas unitarias de {@link CalculadorRangoHorario} (HU-17, HU-18, HU-21, RN-05, RN-09, RN-11).
 */
public class CalculadorRangoHorarioTest {

    @Test
    public void scrollInicialUsa0800SinSesionesYDosHorasAntesConSesiones() {
        assertEquals(
                8 * 60,
                CalculadorRangoHorario.calcularMinutoScrollInicial(
                        Collections.emptyList(), false, Hora.de(15, 0).valor()));

        assertEquals(
                13 * 60,
                CalculadorRangoHorario.calcularMinutoScrollInicial(
                        Collections.emptyList(), true, Hora.de(15, 0).valor()));

        ItemSesionCalendario temprana = crearItem(
                1L, Fecha.de(2026, 11, 16).valor(), 1, 0, 2, 0, EstadoPendiente.INSTANCIA, null);
        assertEquals(
                0,
                CalculadorRangoHorario.calcularMinutoScrollInicial(
                        Collections.singletonList(temprana), false, Hora.de(8, 0).valor()));

        ItemSesionCalendario tarde = crearItem(
                2L, Fecha.de(2026, 11, 16).valor(), 14, 30, 16, 0, EstadoPendiente.INSTANCIA, null);
        assertEquals(
                12 * 60 + 30,
                CalculadorRangoHorario.calcularMinutoScrollInicial(
                        Collections.singletonList(tarde), false, Hora.de(8, 0).valor()));
    }

    @Test
    public void reparteColumnasParalelasCuandoReprogramadaYActivaCoincidenEnHorario() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        ItemSesionCalendario reprogramada = crearItem(
                1L, lunes, 10, 0, 11, 30, EstadoReprogramada.INSTANCIA, null);
        ItemSesionCalendario activa = crearItem(
                2L, lunes, 10, 30, 12, 0, EstadoPendiente.INSTANCIA, null);
        ItemSesionCalendario aislada = crearItem(
                3L, lunes, 15, 0, 16, 0, EstadoCompletada.INSTANCIA, 60);

        List<BloqueHorarioCalendario> bloques = CalculadorRangoHorario.construirBloques(
                Arrays.asList(reprogramada, activa, aislada), VistaCalendario.SEMANA);

        assertEquals(3, bloques.size());
        assertEquals(0, bloques.get(0).diaColumna());
        assertEquals(0, bloques.get(0).indiceSolapamiento());
        assertEquals(2, bloques.get(0).totalSolapamientos());

        assertEquals(1, bloques.get(1).indiceSolapamiento());
        assertEquals(2, bloques.get(1).totalSolapamientos());

        assertEquals(0, bloques.get(2).indiceSolapamiento());
        assertEquals(1, bloques.get(2).totalSolapamientos());
    }

    @Test
    public void resumenExcluyeReprogramadasDePlanYSumaRealesSegunRn11() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        ItemSesionCalendario completada = crearItem(
                1L, lunes, 8, 0, 9, 0, EstadoCompletada.INSTANCIA, 55);
        ItemSesionCalendario tardia = crearItem(
                2L, lunes, 10, 0, 11, 0, EstadoCompletadaTardia.INSTANCIA, 50);
        ItemSesionCalendario reprogramada = crearItem(
                3L, lunes, 12, 0, 13, 0, EstadoReprogramada.INSTANCIA, null);
        ItemSesionCalendario pendiente = crearItem(
                4L, lunes, 14, 0, 15, 30, EstadoPendiente.INSTANCIA, null);
        ItemSesionCalendario noRealizada = crearItem(
                5L, lunes, 16, 0, 17, 0, EstadoNoRealizada.INSTANCIA, null);

        ResumenPeriodoCalendario resumen = CalculadorRangoHorario.calcularResumen(
                Arrays.asList(completada, tardia, reprogramada, pendiente, noRealizada));

        // Plan: 60 + 60 + 90 + 60 = 270 min (excluye reprogramada de 60 min)
        assertEquals(270, resumen.minutosPlanificados());
        // Real: 55 + 50 = 105 min (solo COMPLETADA y COMPLETADA_TARDIA)
        assertEquals(105, resumen.minutosRealizados());
    }

    private static ItemSesionCalendario crearItem(
            long id,
            Fecha fecha,
            int hIni,
            int mIni,
            int hFin,
            int mFin,
            EstadoSesion estado,
            Integer duracionReal) {
        Hora inicio = Hora.de(hIni, mIni).valor();
        Hora fin = Hora.de(hFin, mFin).valor();
        return new ItemSesionCalendario(
                id,
                1L,
                "Cálculo",
                0xFF2E7D32,
                false,
                fecha,
                inicio,
                fin,
                inicio.minutosHasta(fin),
                duracionReal,
                estado);
    }
}
