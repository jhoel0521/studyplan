package io.github.jhoel0521.studyplan.application.service.reportes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;

/**
 * Pruebas unitarias de {@link GenerarReporteEstudioService} verificando vencimiento previo de
 * pendientes ({@code RN-08}), suma diaria estricta ({@code RN-31}), archivadas y cálculo tabular
 * ({@code HU-32}).
 */
public final class GenerarReporteEstudioServiceTest {

    @Test
    public void vencePendientesYIncluyeMateriaArchivadaConCeroRealesYNoRealizada() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        Fecha ayer = hoy.sumarDias(-1);
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(10, 0).valor(), -240);
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository(sesiones);
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository(sesiones);

        Materia mate = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, null).valor());
        Materia hist = materias.guardar(
                Materia.restaurar(2L, "Historia", 0xFFE53935, null, true));

        SesionEstudio comp = sesiones.guardar(SesionEstudio.registrarPasada(
                mate.id(), ayer, hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), -240, 60, "")
                .valor());
        tramos.guardarTramos(comp.id(), Collections.singletonMap(ayer, 60));

        // Sesión pendiente vencida de ayer en materia archivada (0 min reales)
        sesiones.guardar(SesionEstudio.planificar(
                hist.id(), ayer, ayer, Hora.de(14, 0).valor(), Hora.de(15, 0).valor(), null, null)
                .valor());

        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(
                sesiones, eventos, null, null, tramos, reloj);
        GenerarReporteEstudioService service = new GenerarReporteEstudioService(
                vencer, tramos, sesiones, materias, reloj);

        ReporteEstudio reporte = service.ejecutar(PeriodoEstadisticas.semanal(hoy));

        assertFalse(reporte.esSinDatos());
        assertEquals(reloj.ahoraMilis(), reporte.generadoEnMilis());
        assertEquals(120, reporte.minutosPlanificadosTotales());
        assertEquals(60, reporte.minutosRealesTotales());
        assertEquals(1, reporte.sesionesNoRealizadasTotales());
        org.junit.Assert.assertNotNull(reporte.cumplimientoTotal());
        assertEquals(50.0, reporte.cumplimientoTotal(), 1e-4);

        assertEquals(2, reporte.materias().size());
        DesgloseMateriaPeriodo primera = reporte.materias().get(0);
        assertEquals("Cálculo", primera.nombreMateria());
        assertEquals(60, primera.minutosReales());

        DesgloseMateriaPeriodo segunda = reporte.materias().get(1);
        assertEquals("Historia", segunda.nombreMateria());
        assertTrue(segunda.archivada());
        assertEquals(0, segunda.minutosReales());
        assertEquals(60, segunda.minutosPlanificadosDeSesiones());
        assertEquals(1, segunda.sesionesNoRealizadas());
        assertEquals(0.0, segunda.cumplimientoPlanificado(), 1e-4);
    }

    @Test
    public void retornaSinDatosCuandoElPeriodoNoTieneActividadNiPlan() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(10, 0).valor(), -240);
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository(sesiones);
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository(sesiones);

        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(
                sesiones, eventos, null, null, tramos, reloj);
        GenerarReporteEstudioService service = new GenerarReporteEstudioService(
                vencer, tramos, sesiones, materias, reloj);

        ReporteEstudio reporte = service.ejecutar(PeriodoEstadisticas.semanal(hoy));

        assertTrue(reporte.esSinDatos());
        assertTrue(reporte.materias().isEmpty());
    }
}
