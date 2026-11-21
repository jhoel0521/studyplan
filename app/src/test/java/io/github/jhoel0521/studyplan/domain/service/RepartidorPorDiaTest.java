package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

/**
 * Pruebas unitarias de {@link RepartidorPorDia} para corte estricto en medianoche ({@code RN-31}).
 */
public class RepartidorPorDiaTest {

    private static final int DESFASE_LA_PAZ = -240;

    @Test
    public void sesionNocturna22a01SeDivideEn120Y60Minutos() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = lunes.sumarDias(1);
        SesionEstudio sesion = SesionEstudio.planificar(
                1L, lunes, lunes,
                Hora.de(22, 0).valor(), Hora.de(1, 0).valor(),
                DESFASE_LA_PAZ, "", null).valor().conId(1L);

        Instante inicio = Instante.desdeFechaYHoraLocal(lunes, Hora.de(22, 0).valor(), DESFASE_LA_PAZ);
        Instante fin = Instante.desdeFechaYHoraLocal(martes, Hora.de(1, 0).valor(), DESFASE_LA_PAZ);
        EventoEstudio evInicio = EventoEstudio.crear(1L, TipoEventoEstudio.INICIO, inicio, "").valor();
        EventoEstudio evFin = EventoEstudio.crear(1L, TipoEventoEstudio.FIN, fin, "").valor();

        Map<Fecha, Integer> tramos = RepartidorPorDia.repartir(
                sesion, Arrays.asList(evInicio, evFin), fin.utcMilis(), 180);

        assertEquals(2, tramos.size());
        assertEquals(Integer.valueOf(120), tramos.get(lunes));
        assertEquals(Integer.valueOf(60), tramos.get(martes));
    }

    @Test
    public void domingoALunesReparteEstrictoEntreSemanas() {
        Fecha domingo = Fecha.de(2026, 11, 15).valor();
        Fecha lunes = domingo.sumarDias(1);
        SesionEstudio sesion = SesionEstudio.planificar(
                1L, domingo, domingo,
                Hora.de(23, 30).valor(), Hora.de(0, 30).valor(),
                DESFASE_LA_PAZ, "", null).valor().conId(2L);

        Instante inicio = Instante.desdeFechaYHoraLocal(domingo, Hora.de(23, 30).valor(), DESFASE_LA_PAZ);
        Instante fin = Instante.desdeFechaYHoraLocal(lunes, Hora.de(0, 30).valor(), DESFASE_LA_PAZ);
        EventoEstudio evInicio = EventoEstudio.crear(2L, TipoEventoEstudio.INICIO, inicio, "").valor();
        EventoEstudio evFin = EventoEstudio.crear(2L, TipoEventoEstudio.FIN, fin, "").valor();

        Map<Fecha, Integer> tramos = RepartidorPorDia.repartir(
                sesion, Arrays.asList(evInicio, evFin), fin.utcMilis(), 60);

        assertEquals(Integer.valueOf(30), tramos.get(domingo));
        assertEquals(Integer.valueOf(30), tramos.get(lunes));
    }

    @Test
    public void pausaQueCruzaMedianocheNoSumaTiempoPausado() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = lunes.sumarDias(1);
        SesionEstudio sesion = SesionEstudio.planificar(
                1L, lunes, lunes,
                Hora.de(23, 0).valor(), Hora.de(1, 0).valor(),
                DESFASE_LA_PAZ, "", null).valor().conId(3L);

        // Estudia 23:00-23:45 (45m), pausa 23:45-00:15 (30m), reanuda 00:15-01:00 (45m)
        Instante t1 = Instante.desdeFechaYHoraLocal(lunes, Hora.de(23, 0).valor(), DESFASE_LA_PAZ);
        Instante t2 = Instante.desdeFechaYHoraLocal(lunes, Hora.de(23, 45).valor(), DESFASE_LA_PAZ);
        Instante t3 = Instante.desdeFechaYHoraLocal(martes, Hora.de(0, 15).valor(), DESFASE_LA_PAZ);
        Instante t4 = Instante.desdeFechaYHoraLocal(martes, Hora.de(1, 0).valor(), DESFASE_LA_PAZ);

        Map<Fecha, Integer> tramos = RepartidorPorDia.repartir(
                sesion,
                Arrays.asList(
                        EventoEstudio.crear(3L, TipoEventoEstudio.INICIO, t1, "").valor(),
                        EventoEstudio.crear(3L, TipoEventoEstudio.PAUSA, t2, "").valor(),
                        EventoEstudio.crear(3L, TipoEventoEstudio.REANUDACION, t3, "").valor(),
                        EventoEstudio.crear(3L, TipoEventoEstudio.FIN, t4, "").valor()),
                t4.utcMilis(),
                90);

        assertEquals(Integer.valueOf(45), tramos.get(lunes));
        assertEquals(Integer.valueOf(45), tramos.get(martes));
    }

    @Test
    public void registroTardioSinEventosReparteProporcionalAlPlanificado() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = lunes.sumarDias(1);
        Fecha hoy = martes.sumarDias(1);
        // Planificada 22:00-01:00 (180m: 2/3 lunes, 1/3 martes), estudiada 90m
        SesionEstudio pasada = SesionEstudio.registrarPasada(
                1L, lunes, hoy,
                Hora.de(22, 0).valor(), Hora.de(1, 0).valor(),
                DESFASE_LA_PAZ, 90, "Tardía").valor().conId(4L);

        Map<Fecha, Integer> tramos = RepartidorPorDia.repartir(
                pasada, Collections.emptyList(), 0L, 90);

        assertEquals(Integer.valueOf(60), tramos.get(lunes));
        assertEquals(Integer.valueOf(30), tramos.get(martes));
    }

    @Test
    public void cambioDeHorarioDuranteSesionUsaTiempoTranscurridoSinDuplicarNiPerderMinutos() {
        // Inicia 23:00 en UTC-04:00 (03:00 UTC del día siguiente) y termina 2 horas reales después
        // aunque el teléfono cambie a UTC-05:00 en el evento FIN -> suma exactamente 120 minutos.
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = lunes.sumarDias(1);
        SesionEstudio sesion = SesionEstudio.planificar(
                1L, lunes, lunes,
                Hora.de(23, 0).valor(), Hora.de(1, 0).valor(),
                -240, "", null).valor().conId(5L);

        Instante inicio = Instante.desdeFechaYHoraLocal(lunes, Hora.de(23, 0).valor(), -240);
        Instante fin = Instante.de(inicio.utcMilis() + 120 * 60_000L, -300);

        Map<Fecha, Integer> tramos = RepartidorPorDia.repartir(
                sesion,
                Arrays.asList(
                        EventoEstudio.crear(5L, TipoEventoEstudio.INICIO, inicio, "").valor(),
                        EventoEstudio.crear(5L, TipoEventoEstudio.FIN, fin, "").valor()),
                fin.utcMilis(),
                120);

        assertEquals(Integer.valueOf(60), tramos.get(lunes));
        assertEquals(Integer.valueOf(60), tramos.get(martes));
    }
}
