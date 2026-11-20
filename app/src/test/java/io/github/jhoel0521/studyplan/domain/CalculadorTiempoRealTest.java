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

    @Test
    public void analizaPausasSinReanudarSalidasNoAcademicasYEstudioExternoPorTopico() {
        long t0 = 1_794_834_000_000L;
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, -240, "");
        EventoEstudio pausa = EventoEstudio.restaurar(2L, 1L, TipoEventoEstudio.PAUSA, t0 + 10 * MIN, -240, "");
        EventoEstudio reanudar = EventoEstudio.restaurar(3L, 1L, TipoEventoEstudio.REANUDACION, t0 + 15 * MIN, -240, "", "DESCANSO_COMIDA");
        EventoEstudio salidaExt = EventoEstudio.restaurar(4L, 1L, TipoEventoEstudio.SALIDA_APP, t0 + 20 * MIN, -240, "");
        EventoEstudio appCerrada = EventoEstudio.restaurar(5L, 1L, TipoEventoEstudio.ESTUDIO_APP_CERRADA, t0 + 30 * MIN, -240, "", "PDF_MOODLE");
        EventoEstudio regresoExt = EventoEstudio.restaurar(6L, 1L, TipoEventoEstudio.REGRESO_APP, t0 + 30 * MIN, -240, "", "PDF_MOODLE");
        EventoEstudio salidaNoAcad = EventoEstudio.restaurar(7L, 1L, TipoEventoEstudio.SALIDA_APP, t0 + 35 * MIN, -240, "");
        EventoEstudio regresoNoAcad = EventoEstudio.restaurar(8L, 1L, TipoEventoEstudio.REGRESO_APP, t0 + 39 * MIN, -240, "", "LLAMADA_MENSAJE");
        EventoEstudio pausaSinReanudar = EventoEstudio.restaurar(9L, 1L, TipoEventoEstudio.PAUSA, t0 + 40 * MIN, -240, "");
        EventoEstudio fin = EventoEstudio.restaurar(10L, 1L, TipoEventoEstudio.FIN, t0 + 45 * MIN, -240, "");

        CalculadorTiempoReal.AnalisisEventos analisis = CalculadorTiempoReal.analizarEventos(
                Arrays.asList(inicio, pausa, reanudar, salidaExt, appCerrada, regresoExt, salidaNoAcad, regresoNoAcad, pausaSinReanudar, fin),
                t0 + 45 * MIN);

        assertEquals(Long.valueOf(t0), analisis.primerInicioMilis());
        assertEquals(10 * MIN, analisis.pausasManualesMs());
        assertEquals(2, analisis.cantidadPausasManuales());
        assertEquals(4 * MIN, analisis.salidasNoAcademicasMs());
        assertEquals(1, analisis.cantidadSalidasNoAcademicas());
        assertEquals(Long.valueOf(10 * MIN), analisis.estudioExternoPorTopicoMs().get(TopicoEventoEstudio.PDF_MOODLE));
        assertEquals(Long.valueOf(5 * MIN), analisis.interrupcionesPorTopicoMs().get(TopicoEventoEstudio.DESCANSO_COMIDA));
        assertEquals(Long.valueOf(5 * MIN), analisis.interrupcionesPorTopicoMs().get(TopicoEventoEstudio.SIN_CLASIFICAR));
        assertEquals(Long.valueOf(4 * MIN), analisis.interrupcionesPorTopicoMs().get(TopicoEventoEstudio.LLAMADA_MENSAJE));
    }

    /** HU-50: 25 minutos con el teléfono bloqueado con el candado cuentan como estudio y no son salida. */
    @Test
    public void elBloqueoDePantallaCuentaComoEstudio() {
        long t0 = 1_000_000L;
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, HOY, HORA, "");
        EventoEstudio bloqueo = EventoEstudio.restaurar(
                2L, 1L, TipoEventoEstudio.BLOQUEO_PANTALLA, t0 + 10 * MIN, HOY, HORA, "");
        EventoEstudio desbloqueo = EventoEstudio.restaurar(
                3L, 1L, TipoEventoEstudio.DESBLOQUEO, t0 + 35 * MIN, HOY, HORA, "");
        EventoEstudio fin = EventoEstudio.restaurar(4L, 1L, TipoEventoEstudio.FIN, t0 + 60 * MIN, HOY, HORA, "");

        assertEquals(60, CalculadorTiempoReal.calcularMinutosReales(
                TecnicaEstudio.LIBRE, null, 60,
                Arrays.asList(inicio, bloqueo, desbloqueo, fin), t0 + 60 * MIN));
    }

    /** HU-50: un bloqueo sin desbloqueo sigue contando hasta el final de la sesión. */
    @Test
    public void unBloqueoSinDesbloqueoSigueContando() {
        long t0 = 1_000_000L;
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, HOY, HORA, "");
        EventoEstudio bloqueo = EventoEstudio.restaurar(
                2L, 1L, TipoEventoEstudio.BLOQUEO_PANTALLA, t0 + 10 * MIN, HOY, HORA, "");

        assertEquals(300, CalculadorTiempoReal.calcularMinutosReales(
                TecnicaEstudio.LIBRE, null, 60,
                Arrays.asList(inicio, bloqueo), t0 + 300 * MIN));
    }
}
