package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pruebas unitarias de {@link ReporteEstudio}, {@link CalculadorEstadisticas#calcularReporte} y
 * {@link SesionEstudio#minutosPlanificadosEnRango} ({@code HU-32}, {@code RN-05}, {@code RN-31}).
 */
public final class CalculadorReporteTest {

    private static final int DESFASE_UTC = 0;

    @Test
    public void repartoNocturnoDividePlanEntreSemanasYMesesYNoDuplicaNoRealizada() {
        // Domingo 2026-11-29 23:00 -> Lunes 2026-11-30 01:00 (cruce de semana L-D)
        Fecha domingoNov29 = Fecha.de(2026, 11, 29).valor();
        Fecha lunesNov30 = Fecha.de(2026, 11, 30).valor();
        SesionEstudio cruceSemana = sesion(
                1L, 1L, domingoNov29, 23, 0, 120, CodigoEstadoSesion.NO_REALIZADA);

        PeriodoEstadisticas semanaAnt = PeriodoEstadisticas.semanal(domingoNov29);
        PeriodoEstadisticas semanaSig = PeriodoEstadisticas.semanal(lunesNov30);

        assertEquals(60, cruceSemana.minutosPlanificadosEnRango(
                semanaAnt.inicio(), semanaAnt.fin()));
        assertEquals(60, cruceSemana.minutosPlanificadosEnRango(
                semanaSig.inicio(), semanaSig.fin()));

        // Fin exacto 00:00 (22:00 -> 00:00 del día siguiente, límite entre meses nov y dic)
        SesionEstudio bordeCero = sesion(
                2L, 1L, lunesNov30, 22, 0, 120, CodigoEstadoSesion.PENDIENTE);
        PeriodoEstadisticas nov = PeriodoEstadisticas.mensual(lunesNov30);
        PeriodoEstadisticas dic = PeriodoEstadisticas.mensual(Fecha.de(2026, 12, 1).valor());

        assertEquals(120, bordeCero.minutosPlanificadosEnRango(nov.inicio(), nov.fin()));
        assertEquals(0, bordeCero.minutosPlanificadosEnRango(dic.inicio(), dic.fin()));

        Map<Long, Materia> catalogo = Collections.singletonMap(
                1L, Materia.restaurar(1L, "Cálculo", 0xFF1565C0, null, false));
        List<SesionEstudio> sesiones = Collections.singletonList(cruceSemana);

        ReporteEstudio repAnt = CalculadorEstadisticas.calcularReporte(
                semanaAnt, 100L, Collections.emptyMap(), catalogo, sesiones);
        ReporteEstudio repSig = CalculadorEstadisticas.calcularReporte(
                semanaSig, 100L, Collections.emptyMap(), catalogo, sesiones);

        assertEquals(60, repAnt.minutosPlanificadosTotales());
        assertEquals(1, repAnt.sesionesNoRealizadasTotales());
        assertEquals(60, repSig.minutosPlanificadosTotales());
        assertEquals(0, repSig.sesionesNoRealizadasTotales());
    }

    @Test
    public void incluyeCeroRealesGeneralArchivadasSinPlanSobreCienYExcluyeReprogramadas() {
        Fecha miercoles = Fecha.de(2026, 11, 18).valor();
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(miercoles);

        Materia calc = Materia.restaurar(1L, "Cálculo", 0xFF1565C0, null, false);
        Materia hist = Materia.restaurar(2L, "Historia", 0xFFE53935, null, false);
        Materia archivada = Materia.restaurar(3L, "Latín", 0xFF43A047, null, true);
        Materia general = Materia.crearGeneral(0xFF607D8B).conId(4L);
        Materia libreSinPlan = Materia.restaurar(5L, "Ágebra", 0xFF8E24AA, null, false);

        Map<Long, Materia> catalogo = new HashMap<>();
        catalogo.put(1L, calc);
        catalogo.put(2L, hist);
        catalogo.put(3L, archivada);
        catalogo.put(4L, general);
        catalogo.put(5L, libreSinPlan);

        Map<Long, Integer> reales = new HashMap<>();
        reales.put(1L, 120); // sobre 60 plan -> 200 %
        reales.put(3L, 30);
        reales.put(4L, 45);
        reales.put(5L, 90);  // sin plan -> cumplimiento == null
        // hist (2L) tiene 0 reales, pero planificado 60 y 1 no realizada

        SesionEstudio sCalc = sesion(10L, 1L, miercoles, 9, 0, 60, CodigoEstadoSesion.COMPLETADA);
        SesionEstudio sHist = sesion(11L, 2L, miercoles, 11, 0, 60, CodigoEstadoSesion.NO_REALIZADA);
        SesionEstudio sReprog = sesion(
                12L, 2L, miercoles, 13, 0, 180, CodigoEstadoSesion.REPROGRAMADA);
        SesionEstudio sPendArchivada = sesion(
                13L, 3L, miercoles, 15, 0, 30, CodigoEstadoSesion.PENDIENTE);
        SesionEstudio sAhora = sesionSinPlan(14L, 5L, miercoles, 17, 0, 90);

        ReporteEstudio reporte = CalculadorEstadisticas.calcularReporte(
                semana,
                999L,
                reales,
                catalogo,
                Arrays.asList(sCalc, sHist, sReprog, sPendArchivada, sAhora));

        assertFalse(reporte.esSinDatos());
        assertEquals(285, reporte.minutosRealesTotales());
        assertEquals(150, reporte.minutosPlanificadosTotales()); // 60 + 60 + 30 (reprogramada no)
        assertEquals(1, reporte.sesionesNoRealizadasTotales());
        assertEquals(190.0, reporte.cumplimientoTotal(), 1e-4);

        List<DesgloseMateriaPeriodo> filas = reporte.materias();
        assertEquals(5, filas.size());
        // Orden esperado: reales DESC -> Calc(120), Álgebra(90), General(45), Latín(30), Historia(0)
        assertEquals(1L, filas.get(0).materiaId());
        assertEquals(200.0, filas.get(0).cumplimientoPlanificado(), 1e-4);

        assertEquals(5L, filas.get(1).materiaId());
        assertNull(filas.get(1).cumplimientoPlanificado());

        assertEquals(4L, filas.get(2).materiaId());
        assertTrue(filas.get(2).esGeneral());

        assertEquals(3L, filas.get(3).materiaId());
        assertTrue(filas.get(3).archivada());

        assertEquals(2L, filas.get(4).materiaId());
        assertEquals(0, filas.get(4).minutosReales());
        assertEquals(60, filas.get(4).minutosPlanificadosDeSesiones());
        assertEquals(1, filas.get(4).sesionesNoRealizadas());
        assertEquals(0.0, filas.get(4).cumplimientoPlanificado(), 1e-4);
    }

    @Test
    public void ordenDeterministaEnEmpatesYFallbackCatalogoNuloYSinDatosConCeroMinutos() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(hoy);

        // Sesión "Estudiar ahora" detenida en 0 min + Reprogramada sin reemplazo visible -> sin datos
        SesionEstudio ceroMin = sesionSinPlan(1L, 10L, hoy, 10, 0, 0);
        SesionEstudio reprog = sesion(2L, 10L, hoy, 12, 0, 60, CodigoEstadoSesion.REPROGRAMADA);
        ReporteEstudio vacio = CalculadorEstadisticas.calcularReporte(
                semana,
                500L,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Arrays.asList(ceroMin, reprog));
        assertTrue(vacio.esSinDatos());
        assertTrue(vacio.materias().isEmpty());
        assertNull(vacio.cumplimientoTotal());

        // Empates en reales (0) -> desempate por planificados DESC -> nombre CASE_INSENSITIVE -> id
        Map<Long, Materia> catalogo = new HashMap<>();
        catalogo.put(20L, Materia.restaurar(20L, "beta", 0xFF111111, null, false));
        catalogo.put(10L, Materia.restaurar(10L, "Alfa", 0xFF222222, null, false));
        catalogo.put(5L, Materia.restaurar(5L, "alfa", 0xFF333333, null, false));

        List<SesionEstudio> empate = Arrays.asList(
                sesion(3L, 20L, hoy, 8, 0, 30, CodigoEstadoSesion.PENDIENTE),
                sesion(4L, 10L, hoy, 9, 0, 30, CodigoEstadoSesion.PENDIENTE),
                sesion(5L, 5L, hoy, 10, 0, 30, CodigoEstadoSesion.PENDIENTE),
                sesion(6L, 99L, hoy, 11, 0, 60, CodigoEstadoSesion.PENDIENTE)); // fuera catálogo

        ReporteEstudio ordenado = CalculadorEstadisticas.calcularReporte(
                semana, 500L, null, null, empate);
        assertEquals(4, ordenado.materias().size());
        // Con catálogo null, nombres son "" y plan: 99(60), 5(30), 10(30), 20(30)
        assertEquals(99L, ordenado.materias().get(0).materiaId());
        assertEquals("", ordenado.materias().get(0).nombreMateria());
        assertEquals(0xFF607D8B, ordenado.materias().get(0).colorMateria());
        assertEquals(5L, ordenado.materias().get(1).materiaId());

        ReporteEstudio conCatalogo = CalculadorEstadisticas.calcularReporte(
                semana, 500L, Collections.emptyMap(), catalogo, empate);
        assertEquals(99L, conCatalogo.materias().get(0).materiaId()); // plan 60 > 30
        assertEquals(5L, conCatalogo.materias().get(1).materiaId());  // alfa id 5 < id 10
        assertEquals(10L, conCatalogo.materias().get(2).materiaId()); // Alfa id 10
        assertEquals(20L, conCatalogo.materias().get(3).materiaId()); // beta
    }

    private static SesionEstudio sesion(
            long id,
            long materiaId,
            Fecha fecha,
            int hora,
            int minuto,
            int duracionMin,
            CodigoEstadoSesion estado) {
        long inicio = Instante.desdeFechaYHoraLocal(
                fecha, Hora.de(hora, minuto).valor(), DESFASE_UTC).utcMilis();
        long fin = inicio + duracionMin * 60_000L;
        return SesionEstudio.restaurar(
                id, materiaId, inicio, fin, DESFASE_UTC, duracionMin, null,
                estado, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
    }

    private static SesionEstudio sesionSinPlan(
            long id, long materiaId, Fecha fecha, int hora, int minuto, int duracionRealMin) {
        long inicio = Instante.desdeFechaYHoraLocal(
                fecha, Hora.de(hora, minuto).valor(), DESFASE_UTC).utcMilis();
        return SesionEstudio.restaurar(
                id, materiaId, inicio, null, DESFASE_UTC, 0, duracionRealMin,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null);
    }
}
