package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * Pruebas unitarias de {@link CalculadorTiempoReal} (HU-22, HU-41, RN-11, RN-22, RN-24, RN-29).
 */
public class CalculadorTiempoRealTest {

    private static final long MIN = 60_000L;
    private static final Fecha HOY = Fecha.de(2026, 11, 16).valor();
    private static final Hora HORA = Hora.de(10, 0).valor();

    @Test
    public void excluyePausasYSumaTramoAppCerradaIntegroEnLibreYPomodoro() {
        long t0 = 1_000_000L;
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, HOY, HORA, "");
        EventoEstudio pausa = EventoEstudio.restaurar(2L, 1L, TipoEventoEstudio.PAUSA, t0 + 25 * MIN, HOY, HORA, "");
        EventoEstudio reanuda = EventoEstudio.restaurar(3L, 1L, TipoEventoEstudio.REANUDACION, t0 + 35 * MIN, HOY, HORA, "");
        EventoEstudio salida = EventoEstudio.restaurar(4L, 1L, TipoEventoEstudio.SALIDA_APP, t0 + 45 * MIN, HOY, HORA, "");
        // Estudió 30 min con app cerrada
        EventoEstudio cerrada = EventoEstudio.restaurar(
                5L, 1L, TipoEventoEstudio.ESTUDIO_APP_CERRADA, t0 + 75 * MIN, HOY, HORA, "en grupo");
        EventoEstudio fin = EventoEstudio.restaurar(6L, 1L, TipoEventoEstudio.FIN, t0 + 75 * MIN, HOY, HORA, "");

        int minLibre = CalculadorTiempoReal.calcularMinutosReales(
                TecnicaEstudio.LIBRE, null, 60,
                Arrays.asList(inicio, pausa, reanuda, salida, cerrada, fin), t0 + 75 * MIN);
        // 25 min + 10 min dentro app + 30 min cerrada = 65 min
        assertEquals(65, minLibre);

        // En Pomodoro 25/5: 35 min dentro app = 25 trabajo + 5 descanso + 5 trabajo = 30 trabajo + 30 cerrada = 60 min
        int minPomodoro = CalculadorTiempoReal.calcularMinutosReales(
                TecnicaEstudio.POMODORO, ConfiguracionPomodoro.porDefecto(), 60,
                Arrays.asList(inicio, pausa, reanuda, salida, cerrada, fin), t0 + 75 * MIN);
        assertEquals(60, minPomodoro);
    }

    @Test
    public void calculaCeroMinutosEnPrimerMinutoYSoportaMultiDia() {
        long t0 = 1_000_000L;
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, HOY, HORA, "");
        assertEquals(0, CalculadorTiempoReal.calcularMinutosReales(
                TecnicaEstudio.LIBRE, null, 60, Collections.singletonList(inicio), t0 + 30_000L));

        // 25 horas después (multi-día RN-29)
        long finMultiDia = t0 + 25 * 60 * MIN;
        assertEquals(25 * 60, CalculadorTiempoReal.calcularMinutosReales(
                TecnicaEstudio.LIBRE, null, 60, Collections.singletonList(inicio), finMultiDia));
    }

    @Test
    public void detectaPausaSalidaSinResolverYUltimaMarcaSigkill() {
        long t0 = 1_000_000L;
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, HOY, HORA, "");
        EventoEstudio pausa = EventoEstudio.restaurar(2L, 1L, TipoEventoEstudio.PAUSA, t0 + 10 * MIN, HOY, HORA, "");

        assertTrue(CalculadorTiempoReal.estaPausadaActualmente(Arrays.asList(inicio, pausa)));
        assertFalse(CalculadorTiempoReal.tieneSalidaSinResolver(Arrays.asList(inicio, pausa)));
        assertEquals(Long.valueOf(t0 + 10 * MIN),
                CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp(Arrays.asList(inicio, pausa)));

        EventoEstudio salida = EventoEstudio.restaurar(3L, 1L, TipoEventoEstudio.SALIDA_APP, t0 + 15 * MIN, HOY, HORA, "");
        assertTrue(CalculadorTiempoReal.tieneSalidaSinResolver(Arrays.asList(inicio, salida)));
        assertEquals(Long.valueOf(t0 + 15 * MIN),
                CalculadorTiempoReal.obtenerInstanteSalidaSinResolver(Arrays.asList(inicio, salida)));
        // SALIDA_APP no es marca dentro de app
        assertEquals(Long.valueOf(t0),
                CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp(Arrays.asList(inicio, salida)));

        assertNull(CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp(Collections.emptyList()));
    }
}
