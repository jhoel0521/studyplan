package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Instante;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/** Pruebas unitarias de {@link FormatoSesion} para estabilidad ante cambios de zona ({@code HU-10}, {@code HU-39}). */
public class FormatoSesionTest {

    @Test
    public void omiteZonaCuandoCoincideYMuestraIndicacionCuandoDifiere() {
        Fecha fecha = Fecha.de(2026, 11, 16).valor();
        SesionEstudio sesion = SesionEstudio.planificar(
                1L, fecha, fecha,
                Hora.de(10, 0).valor(), Hora.de(11, 30).valor(),
                -240, "", null).valor();

        assertEquals(
                "2026-11-16 · 10:00–11:30",
                FormatoSesion.horarioSesionConZona(sesion, -240));
        assertEquals(
                "2026-11-16 · 10:00–11:30 (UTC\u221204:00)",
                FormatoSesion.horarioSesionConZona(sesion, 60));

        Instante marca = Instante.desdeFechaYHoraLocal(fecha, Hora.de(21, 15).valor(), -240);
        EventoEstudio evento = EventoEstudio.crear(
                1L, TipoEventoEstudio.INICIO, marca, "").valor();

        assertEquals("21:15", FormatoSesion.horaEventoConZona(evento, -240));
        assertEquals("21:15 (UTC\u221204:00)", FormatoSesion.horaEventoConZona(evento, 60));
    }
}
