package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ConsultarSesionService;
import io.github.jhoel0521.studyplan.application.service.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.PlanificarSesionService;
import io.github.jhoel0521.studyplan.application.service.RegistrarSesionPasadaService;
import io.github.jhoel0521.studyplan.application.service.ReprogramarSesionService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas del ViewModel del formulario de sesiones en modos Planificar, Pasada y Reprogramar.
 *
 * <p>Verifica: HU-10, HU-11 y HU-14.
 */
public class SesionFormularioViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeMateriaRepository materias;
    private FakeSesionRepository sesiones;
    private FakeReloj reloj;
    private SesionFormularioViewModel modelo;
    private long materiaId;

    @Before
    public void preparar() {
        materias = new FakeMateriaRepository();
        sesiones = new FakeSesionRepository();
        reloj = new FakeReloj(Fecha.de(2026, 11, 14).valor(), Hora.de(10, 0).valor());
        modelo = new SesionFormularioViewModel(
                new PlanificarSesionService(sesiones, materias, reloj),
                new RegistrarSesionPasadaService(sesiones, materias, reloj),
                new ReprogramarSesionService(sesiones, materias, reloj),
                new ListarMateriasService(materias, sesiones),
                new ConsultarSesionService(
                        sesiones,
                        materias,
                        reloj,
                        new VencerSesionesPendientesService(sesiones, reloj)),
                reloj,
                Runnable::run);
        materiaId = materias.guardar(Materia.crear("Cálculo", 0xFF1E88E5, null).valor()).id();
    }

    @Test
    public void cargarPublicaMateriasActivasYFechaHoy() {
        modelo.cargar(-1L);
        assertEquals(Fecha.de(2026, 11, 14).valor(), modelo.fechaHoy().getValue());
        assertEquals(1, Objects.requireNonNull(modelo.materiasActivas().getValue()).size());
    }

    @Test
    public void guardarPlanificadaSinMateriaPublicaError() {
        modelo.guardarPlanificada(null, "2026-11-15", "09:00", "10:00", "");
        EventoSesion evento = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.ERROR, evento.tipo());
        assertEquals(ErrorValidacion.MATERIA_SESION_OBLIGATORIA, evento.error());
    }

    @Test
    public void guardarPlanificadaValidaPublicaPlanificadaYSolapamientoDevuelveConflicto() {
        modelo.guardarPlanificada(materiaId, "2026-11-15", "09:00", "10:30", "Repaso");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.PLANIFICADA, ok.tipo());
        assertNotNull(ok.sesionId());

        modelo.guardarPlanificada(materiaId, "2026-11-15", "10:00", "11:00", "");
        EventoSesion choque = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.ERROR, choque.tipo());
        assertEquals(ErrorValidacion.SESION_SOLAPADA, choque.error());
        assertNotNull(choque.conflicto());
        assertEquals("Cálculo", choque.conflicto().nombreMateria());
    }

    @Test
    public void guardarPasadaValidaPublicaPasadaRegistrada() {
        modelo.guardarPasada(materiaId, "2026-11-12", "14:00", "15:30", "80", "Ejercicios");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.PASADA_REGISTRADA, ok.tipo());
        SesionEstudio guardada =
                sesiones.buscarPorId(Objects.requireNonNull(ok.sesionId())).orElseThrow();
        assertEquals(Integer.valueOf(80), guardada.duracionRealMinutos());
    }

    @Test
    public void reprogramarCargaBaseYCreaDestinoEnlazada() {
        SesionEstudio original = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                reloj.hoy(),
                reloj.hoy(),
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                null,
                null).valor());

        modelo.cargar(original.id());
        assertTrue(modelo.sesionBase().getValue() != null);

        modelo.reprogramar(original.id(), "2026-11-16", "09:00", "10:00", "Cambio de horario");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.REPROGRAMADA, ok.tipo());
        assertNotNull(ok.sesionId());
    }
}
