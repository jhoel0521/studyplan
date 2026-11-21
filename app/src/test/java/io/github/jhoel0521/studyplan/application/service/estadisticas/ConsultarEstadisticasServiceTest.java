package io.github.jhoel0521.studyplan.application.service.estadisticas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.Collections;

import io.github.jhoel0521.studyplan.domain.readmodel.EstadisticasPeriodo;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;

/**
 * Pruebas unitarias de {@link ConsultarEstadisticasService} ({@code HU-29} a {@code HU-31},
 * {@code HU-45}).
 */
public final class ConsultarEstadisticasServiceTest {

    @Test
    public void vencePendientesYAgregaHorasCumplimientoYConcentracionDelPeriodo() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        Fecha ayer = hoy.sumarDias(-1);
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(10, 0).valor(), -240);
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository(sesiones);
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository(sesiones);

        Materia mate = materias.guardar(Materia.crear("Matemática", 0xFF1565C0, null).valor());
        SesionEstudio comp = sesiones.guardar(SesionEstudio.registrarPasada(
                mate.id(), ayer, hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), -240, 60, "")
                .valor());
        tramos.guardarTramos(comp.id(), Collections.singletonMap(ayer, 60));

        long t0 = reloj.instanteDe(ayer, Hora.de(9, 0).valor());
        eventos.guardar(EventoEstudio.crear(comp.id(), TipoEventoEstudio.INICIO, t0, -240, "").valor());
        eventos.guardar(EventoEstudio.crear(comp.id(), TipoEventoEstudio.PAUSA, t0 + 20 * 60_000L, -240, "").valor());
        eventos.guardar(EventoEstudio.crear(comp.id(), TipoEventoEstudio.REANUDACION, t0 + 25 * 60_000L, -240, "", TopicoEventoEstudio.DESCANSO_COMIDA).valor());
        eventos.guardar(EventoEstudio.crear(comp.id(), TipoEventoEstudio.FIN, t0 + 60 * 60_000L, -240, "").valor());

        // Sesión vencida de ayer que debe marcarse NO_REALIZADA al consultar estadísticas
        sesiones.guardar(SesionEstudio.planificar(
                mate.id(), ayer, ayer, Hora.de(14, 0).valor(), Hora.de(15, 0).valor(), null, null)
                .valor());

        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(
                sesiones, eventos, null, null, tramos, reloj);
        ConsultarEstadisticasService service = new ConsultarEstadisticasService(
                vencer, tramos, sesiones, materias, eventos, reloj);

        EstadisticasPeriodo res = service.ejecutar(PeriodoEstadisticas.semanal(hoy));
        assertNotNull(res);
        assertEquals(60, res.minutosRealesTotales());
        assertEquals(1, res.distribucionMaterias().size());
        assertEquals(1, res.completadasTardias());
        assertEquals(1, res.noRealizadas());
        assertEquals(50.0, res.tasaCumplimientoPorcentaje(), 1e-4);
    }
}
