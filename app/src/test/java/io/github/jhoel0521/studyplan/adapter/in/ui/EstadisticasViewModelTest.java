package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.github.jhoel0521.studyplan.application.service.ConsultarEstadisticasService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;

/**
 * Pruebas unitarias de {@link EstadisticasViewModel} ({@code HU-29}, {@code HU-30},
 * {@code HU-31}, {@code HU-45}).
 */
public class EstadisticasViewModelTest {

    @Rule
    public InstantTaskExecutorRule regla = new InstantTaskExecutorRule();

    @Test
    public void cambiaPeriodoYSubvistaYExponeResultados() {
        Fecha hoy = Fecha.de(2026, 11, 18).valor();
        FakeReloj reloj = new FakeReloj(hoy, Hora.de(12, 0).valor(), -240);
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository(sesiones);
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository();

        Materia mat = materias.guardar(Materia.crear("Cálculo", 0xFF1E88E5, null).valor());
        Fecha ayer = hoy.sumarDias(-1);
        SesionEstudio s = sesiones.guardar(SesionEstudio.registrarPasada(
                mat.id(), ayer, hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), -240, 60, "")
                .valor());
        tramos.guardarTramos(s.id(), Collections.singletonMap(ayer, 60));
        long t0 = reloj.instanteDe(ayer, Hora.de(9, 0).valor());
        eventos.guardar(EventoEstudio.crear(s.id(), TipoEventoEstudio.INICIO, t0, -240, "").valor());
        eventos.guardar(EventoEstudio.crear(
                s.id(), TipoEventoEstudio.PAUSA, t0 + 20 * 60_000L, -240, "").valor());
        eventos.guardar(EventoEstudio.crear(
                s.id(),
                TipoEventoEstudio.REANUDACION,
                t0 + 25 * 60_000L,
                -240,
                "",
                TopicoEventoEstudio.DESCANSO_COMIDA).valor());
        eventos.guardar(EventoEstudio.crear(
                s.id(), TipoEventoEstudio.FIN, t0 + 60 * 60_000L, -240, "").valor());

        ConsultarEstadisticasService service = new ConsultarEstadisticasService(
                new VencerSesionesPendientesService(sesiones, eventos, null, null, tramos, reloj),
                tramos,
                sesiones,
                materias,
                eventos,
                reloj);

        EstadisticasViewModel vm = new EstadisticasViewModel(service, reloj, Runnable::run);
        vm.cargar();
        assertNotNull(vm.estadisticas().getValue());
        assertEquals(60, vm.estadisticas().getValue().minutosRealesTotales());
        assertEquals(PeriodoEstadisticas.Tipo.SEMANA, vm.periodo().getValue().tipo());

        vm.cambiarTipo(PeriodoEstadisticas.Tipo.MES);
        assertEquals(PeriodoEstadisticas.Tipo.MES, vm.periodo().getValue().tipo());
        assertEquals(60, vm.estadisticas().getValue().minutosRealesTotales());

        vm.periodoAnterior();
        assertEquals(0, vm.estadisticas().getValue().minutosRealesTotales());

        vm.periodoSiguiente();
        assertEquals(60, vm.estadisticas().getValue().minutosRealesTotales());

        vm.cambiarSubvista(EstadisticasViewModel.Subvista.CONCENTRACION);
        assertEquals(EstadisticasViewModel.Subvista.CONCENTRACION, vm.subvista().getValue());
    }
}
