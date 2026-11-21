package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

import io.github.jhoel0521.studyplan.application.port.in.reportes.GenerarReporteEstudio;
import io.github.jhoel0521.studyplan.application.port.out.ResultadoExportacion;
import io.github.jhoel0521.studyplan.application.service.GenerarReporteEstudioService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.enumeration.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeExportadorReporte;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;

/**
 * Pruebas unitarias de {@link ReportesViewModel} ({@code HU-32}, {@code HU-33}, {@code RNF-10}).
 */
public final class ReportesViewModelTest {

    @Rule
    public final InstantTaskExecutorRule regla = new InstantTaskExecutorRule();

    @Test
    public void inicializacionIdempotenteYCambioSemanaMesYNavegacion() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        Fecha focoInicial = Fecha.de(2026, 10, 15).valor();
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(10, 0).valor(), 0);
        FakeExportadorReporte exportador = new FakeExportadorReporte();
        GenerarReporteEstudio caso = crearCasoConDatos(reloj, focoInicial);

        ReportesViewModel modelo = new ReportesViewModel(caso, exportador, reloj, Runnable::run);
        modelo.inicializarSiFalta(PeriodoEstadisticas.Tipo.MES, focoInicial);
        assertEquals(PeriodoEstadisticas.Tipo.MES, modelo.periodo().getValue().tipo());
        assertEquals(Fecha.de(2026, 10, 1).valor(), modelo.periodo().getValue().inicio());

        // Simula rotación: nuevo intento con otra configuración no debe sobreescribir
        modelo.inicializarSiFalta(PeriodoEstadisticas.Tipo.SEMANA, hoy);
        assertEquals(PeriodoEstadisticas.Tipo.MES, modelo.periodo().getValue().tipo());
        assertEquals(Fecha.de(2026, 10, 1).valor(), modelo.periodo().getValue().inicio());

        modelo.cargar();
        assertNotNull(modelo.reporte().getValue());
        assertFalse(modelo.reporte().getValue().esSinDatos());

        modelo.periodoAnterior();
        assertEquals(Fecha.de(2026, 9, 1).valor(), modelo.periodo().getValue().inicio());
        assertTrue(modelo.reporte().getValue().esSinDatos());

        modelo.periodoSiguiente();
        assertEquals(Fecha.de(2026, 10, 1).valor(), modelo.periodo().getValue().inicio());
        assertFalse(modelo.reporte().getValue().esSinDatos());

        modelo.cambiarTipo(PeriodoEstadisticas.Tipo.SEMANA);
        assertEquals(PeriodoEstadisticas.Tipo.SEMANA, modelo.periodo().getValue().tipo());
    }

    @Test
    public void descartaRespuestaObsoletaAnteNavegacionRapidaYBloqueaDobleExportacion() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(10, 0).valor(), 0);
        FakeExportadorReporte exportador = new FakeExportadorReporte();
        Queue<Runnable> cola = new ArrayDeque<>();

        DesgloseMateriaPeriodo filaNov = new DesgloseMateriaPeriodo(
                1L, "Cálculo", false, 0xFF1565C0, false, 60, 100.0, 60, 0);
        GenerarReporteEstudio casoManual = periodo -> new ReporteEstudio(
                periodo,
                1L,
                periodo.inicio().equals(Fecha.de(2026, 11, 16).valor())
                        ? Collections.singletonList(filaNov)
                        : Collections.emptyList());

        ReportesViewModel modelo = new ReportesViewModel(
                casoManual, exportador, reloj, cola::add);
        modelo.cargar(); // versión 1 (semana actual: con datos)
        modelo.periodoAnterior(); // versión 2 (semana anterior: vacía)

        assertEquals(2, cola.size());
        Runnable tareaObsoleta = cola.poll();
        Runnable tareaVigente = cola.poll();

        // Ejecuta primero la respuesta vieja (si llega desordenada o tras avanzar de versión)
        tareaObsoleta.run();
        assertNull(modelo.reporte().getValue());
        assertTrue(Boolean.TRUE.equals(modelo.cargando().getValue()));

        tareaVigente.run();
        assertNotNull(modelo.reporte().getValue());
        assertTrue(modelo.reporte().getValue().esSinDatos());
        assertFalse(Boolean.TRUE.equals(modelo.cargando().getValue()));

        // Navega a la semana actual con datos para probar bloqueo de doble exportación
        modelo.periodoSiguiente();
        cola.poll().run();
        assertFalse(modelo.reporte().getValue().esSinDatos());

        modelo.exportarPdf();
        assertTrue(Boolean.TRUE.equals(modelo.exportando().getValue()));
        modelo.exportarCsv(); // Debe ignorarse mientras exporta
        assertEquals(1, cola.size());

        cola.poll().run();
        assertEquals(1, exportador.exportaciones);
        assertEquals(FormatoExportacionReporte.PDF, exportador.ultimoFormato);
        assertFalse(Boolean.TRUE.equals(modelo.exportando().getValue()));
    }

    @Test
    public void emiteYConsumeEventoExportacionYErroresTipificados() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(10, 0).valor(), 0);
        FakeExportadorReporte exportador = new FakeExportadorReporte();
        GenerarReporteEstudio caso = crearCasoConDatos(reloj, hoy);

        ReportesViewModel modelo = new ReportesViewModel(caso, exportador, reloj, Runnable::run);
        // Sin cargar reporte todavía -> error SIN_DATOS
        modelo.exportarPdf();
        assertEquals(ResultadoExportacion.Error.SIN_DATOS, modelo.errorExportacion().getValue());
        modelo.consumirError();
        assertNull(modelo.errorExportacion().getValue());

        modelo.cargar();
        modelo.exportarCsv();
        ReportesViewModel.EventoExportacion evento = modelo.exportacion().getValue();
        assertNotNull(evento);
        assertTrue(evento.uriContenido().endsWith(".csv"));
        assertEquals(FormatoExportacionReporte.CSV, evento.formato());
        modelo.consumirExportacion();
        assertNull(modelo.exportacion().getValue());

        exportador.errorForzado = ResultadoExportacion.Error.ERROR_ESCRITURA;
        modelo.exportarPdf();
        assertNull(modelo.exportacion().getValue());
        assertEquals(
                ResultadoExportacion.Error.ERROR_ESCRITURA,
                modelo.errorExportacion().getValue());
    }

    private static GenerarReporteEstudio crearCasoConDatos(FakeReloj reloj, Fecha fechaActividad) {
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository(sesiones);
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository(sesiones);

        Materia m = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, null).valor());
        long inicio = Instante.desdeFechaYHoraLocal(
                fechaActividad, Hora.de(9, 0).valor(), 0).utcMilis();
        SesionEstudio s = sesiones.guardar(SesionEstudio.restaurar(
                null, m.id(), inicio, inicio + 60 * 60_000L, 0, 60, 60,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null));
        tramos.guardarTramos(s.id(), Collections.singletonMap(fechaActividad, 60));

        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(
                sesiones, eventos, null, null, tramos, reloj);
        return new GenerarReporteEstudioService(vencer, tramos, sesiones, materias, reloj);
    }
}
