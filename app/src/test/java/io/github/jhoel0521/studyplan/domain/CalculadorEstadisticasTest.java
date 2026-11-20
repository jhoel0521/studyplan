package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pruebas unitarias de {@link CalculadorEstadisticas} y {@link PeriodoEstadisticas} ({@code HU-29},
 * {@code HU-30}, {@code HU-31}, {@code HU-45}, {@code RN-30}, {@code RN-31}).
 */
public final class CalculadorEstadisticasTest {

    private static final long MIN = 60_000L;

    @Test
    public void calculaVariacionDistribucionIncluyendoGeneralYArchivadasYTasaSinVigentes() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(hoy);
        assertEquals(Fecha.de(2026, 11, 16).valor(), semana.inicio());
        assertEquals(Fecha.de(2026, 11, 22).valor(), semana.fin());

        Materia mate = Materia.restaurar(1L, "Matemáticas", 0xFF1565C0, null, false);
        Materia archivada = Materia.restaurar(2L, "Latín", 0xFF43A047, null, true);
        Materia general = Materia.crearGeneral(0xFF607D8B).conId(3L);

        Map<Long, Materia> catalogo = new HashMap<>();
        catalogo.put(1L, mate);
        catalogo.put(2L, archivada);
        catalogo.put(3L, general);

        Map<Long, Integer> minutosPorMateria = new HashMap<>();
        minutosPorMateria.put(1L, 600);
        minutosPorMateria.put(2L, 180);
        minutosPorMateria.put(3L, 120);

        SesionEstudio comp1 = SesionEstudio.restaurar(
                10L, 1L, 1000L, 1000L + 60 * MIN, -240, 60, 60,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
        SesionEstudio comp2 = SesionEstudio.restaurar(
                11L, 1L, 2000L, 2000L + 60 * MIN, -240, 60, 60,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
        SesionEstudio noReal = SesionEstudio.restaurar(
                12L, 2L, 3000L, 3000L + 60 * MIN, -240, 60, null,
                CodigoEstadoSesion.NO_REALIZADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
        SesionEstudio pendienteManana = SesionEstudio.restaurar(
                13L, 1L, 4000L, 4000L + 60 * MIN, -240, 60, null,
                CodigoEstadoSesion.PENDIENTE, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);

        EstadisticasPeriodo res = CalculadorEstadisticas.calcular(
                semana,
                900,
                720,
                minutosPorMateria,
                catalogo,
                Arrays.asList(comp1, comp2, noReal, pendienteManana),
                Collections.emptyMap(),
                5000L);

        assertEquals(15.0, res.horasRealesTotales(), 1e-6);
        assertEquals(25.0, res.variacionPorcentaje(), 1e-4);
        assertEquals(3, res.distribucionMaterias().size());
        assertEquals(2, res.completadas());
        assertEquals(1, res.noRealizadas());
        assertEquals(1, res.pendientesOVigentes());
        assertEquals(66.666, res.tasaCumplimientoPorcentaje(), 0.01);

        EstadisticasPeriodo sinPrevio = CalculadorEstadisticas.calcular(
                semana, 900, 0, minutosPorMateria, catalogo,
                Collections.emptyList(), Collections.emptyMap(), 5000L);
        assertNull(sinPrevio.variacionPorcentaje());
        assertNull(sinPrevio.tasaCumplimientoPorcentaje());
    }

    @Test
    public void calculaConcentracionExcluyendoSinPlanYTardiasYSeparandoEstudioExterno() {
        long t0 = 1_794_834_000_000L;
        SesionEstudio planificada = SesionEstudio.restaurar(
                1L, 1L, t0, t0 + 60 * MIN, -240, 60, 45,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
        SesionEstudio sinPlan = SesionEstudio.restaurar(
                2L, 1L, t0 + 70 * MIN, null, -240, 0, 20,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
        SesionEstudio tardia = SesionEstudio.restaurar(
                3L, 1L, t0 + 100 * MIN, t0 + 130 * MIN, -240, 30, 30,
                CodigoEstadoSesion.COMPLETADA_TARDIA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);

        Map<Long, List<EventoEstudio>> eventos = new HashMap<>();
        // Planificada: inicia 5 min tarde, 30 min dentro app, 10 min pausa manual, salida 15 min estudiando PDF, salida 5 min llamada
        eventos.put(1L, Arrays.asList(
                EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0 + 5 * MIN, -240, ""),
                EventoEstudio.restaurar(2L, 1L, TipoEventoEstudio.PAUSA, t0 + 20 * MIN, -240, ""),
                EventoEstudio.restaurar(3L, 1L, TipoEventoEstudio.REANUDACION, t0 + 30 * MIN, -240, "", "DESCANSO_COMIDA"),
                EventoEstudio.restaurar(4L, 1L, TipoEventoEstudio.SALIDA_APP, t0 + 35 * MIN, -240, ""),
                EventoEstudio.restaurar(5L, 1L, TipoEventoEstudio.ESTUDIO_APP_CERRADA, t0 + 50 * MIN, -240, "", "PDF_MOODLE"),
                EventoEstudio.restaurar(6L, 1L, TipoEventoEstudio.REGRESO_APP, t0 + 50 * MIN, -240, "", "PDF_MOODLE"),
                EventoEstudio.restaurar(7L, 1L, TipoEventoEstudio.SALIDA_APP, t0 + 55 * MIN, -240, ""),
                EventoEstudio.restaurar(8L, 1L, TipoEventoEstudio.REGRESO_APP, t0 + 60 * MIN, -240, "", "LLAMADA_MENSAJE"),
                EventoEstudio.restaurar(9L, 1L, TipoEventoEstudio.FIN, t0 + 65 * MIN, -240, "")
        ));
        // Sin plan: 20 min dentro app
        eventos.put(2L, Arrays.asList(
                EventoEstudio.restaurar(10L, 2L, TipoEventoEstudio.INICIO, t0 + 70 * MIN, -240, ""),
                EventoEstudio.restaurar(11L, 2L, TipoEventoEstudio.FIN, t0 + 90 * MIN, -240, "")
        ));

        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(Fecha.de(2026, 11, 18).valor());
        EstadisticasPeriodo res = CalculadorEstadisticas.calcular(
                semana,
                95,
                0,
                Collections.singletonMap(1L, 95),
                Collections.singletonMap(1L, Materia.restaurar(1L, "Mate", 0xFF1565C0, null, false)),
                Arrays.asList(planificada, sinPlan, tardia),
                eventos,
                t0 + 130 * MIN);

        ConcentracionPeriodo c = res.concentracion();
        // Dentro app: 30 + 20 = 50 min; pausas manuales: 10 min -> 50 / 60 = 83.33 %
        assertEquals(83.333, c.indiceConcentracionPorcentaje(), 0.05);
        // 2 interrupciones (1 pausa + 1 salida no académica), excluyendo salida PDF_MOODLE
        assertEquals(2, c.totalInterrupciones());
        // Puntualidad: solo planificada (+5 min), sinPlan excluida
        assertEquals(5.0, c.puntualidadPromedioMinutos(), 1e-4);
        // Respeto del tiempo: solo planificada (45 / 60 = 75 %)
        assertEquals(75.0, c.respetoDelTiempoPorcentaje(), 1e-4);
        assertEquals(5, c.tiempoFueraAppMinutos());
        assertEquals(15, c.estudioFueraAppMinutos());
        assertEquals(0, c.estudioFueraSinClasificarMinutos());
        assertEquals(2, c.resumenTopicosInterrupciones().size());
        assertTrue(c.estudioFueraPorTopico().get(0).topico() == TopicoEventoEstudio.PDF_MOODLE);
    }

    /** HU-50: el bloqueo de pantalla no es interrupción ni tiempo fuera de la app (RN-30). */
    @Test
    public void elBloqueoDePantallaNoCuentaComoInterrupcionNiComoTiempoFuera() {
        long t0 = 1_794_834_000_000L;
        SesionEstudio sesion = SesionEstudio.restaurar(
                1L, 1L, t0, t0 + 60 * MIN, -240, 60, 60,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
        Map<Long, List<EventoEstudio>> eventos = new HashMap<>();
        eventos.put(1L, Arrays.asList(
                EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, -240, ""),
                EventoEstudio.restaurar(2L, 1L, TipoEventoEstudio.BLOQUEO_PANTALLA, t0 + 10 * MIN, -240, ""),
                EventoEstudio.restaurar(3L, 1L, TipoEventoEstudio.DESBLOQUEO, t0 + 35 * MIN, -240, ""),
                EventoEstudio.restaurar(4L, 1L, TipoEventoEstudio.FIN, t0 + 60 * MIN, -240, "")));

        EstadisticasPeriodo res = CalculadorEstadisticas.calcular(
                PeriodoEstadisticas.semanal(Fecha.de(2026, 11, 18).valor()),
                60,
                0,
                Collections.singletonMap(1L, 60),
                Collections.singletonMap(1L, Materia.restaurar(1L, "Mate", 0xFF1565C0, null, false)),
                Collections.singletonList(sesion),
                eventos,
                t0 + 120 * MIN);

        ConcentracionPeriodo c = res.concentracion();
        assertEquals(0, c.totalInterrupciones());
        assertEquals(0, c.tiempoFueraAppMinutos());
        assertEquals(100.0, c.indiceConcentracionPorcentaje(), 0.05);
    }
}
