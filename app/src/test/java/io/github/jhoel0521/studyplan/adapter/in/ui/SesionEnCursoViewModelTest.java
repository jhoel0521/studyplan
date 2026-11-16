package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.application.service.ConsultarEjecucionSesionService;
import io.github.jhoel0521.studyplan.application.service.DecidirFinPlanificadoOInvasionService;
import io.github.jhoel0521.studyplan.application.service.DetenerSesionService;
import io.github.jhoel0521.studyplan.application.service.IniciarSesionService;
import io.github.jhoel0521.studyplan.application.service.PausarReanudarSesionService;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeControladorNoMolestar;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMarcadorSalidaApp;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas unitarias de {@link SesionEnCursoViewModel} (HU-22, HU-23, HU-42, HU-43).
 */
public class SesionEnCursoViewModelTest {

    @Rule
    public final InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeSesionRepository sesiones;
    private FakeReloj reloj;
    private IniciarSesionService iniciar;
    private SesionEnCursoViewModel modelo;
    private SesionEstudio s1;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeAjustesRepository ajustes = new FakeAjustesRepository();
        FakeControladorNoMolestar noMolestar = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion programador = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(9, 0).valor());

        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        DetenerSesionService detener = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcador, reloj);
        modelo = new SesionEnCursoViewModel(
                new ConsultarEjecucionSesionService(
                        sesiones, eventos, materias, ajustes, noMolestar, reloj),
                new PausarReanudarSesionService(
                        sesiones, eventos, materias, ajustes, noMolestar, programador, reloj),
                detener,
                new DecidirFinPlanificadoOInvasionService(
                        sesiones, eventos, materias, ajustes, noMolestar, programador, marcador,
                        detener, reloj),
                Runnable::run);

        Materia m = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, null).valor());
        Fecha hoy = reloj.hoy();
        s1 = sesiones.guardar(SesionEstudio.planificar(
                m.id(), hoy, hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "", null).valor());
    }

    @Test
    public void pausaReanudaYDetieneSesionEnCurso() {
        iniciar.ejecutar(s1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        modelo.cargar(s1.id());
        assertNotNull(modelo.resumen().getValue());
        assertFalse(modelo.resumen().getValue().pausada());

        modelo.pausarOReanudar(s1.id());
        assertTrue(modelo.resumen().getValue().pausada());

        modelo.pausarOReanudar(s1.id());
        assertFalse(modelo.resumen().getValue().pausada());

        reloj.avanzarMilis(20 * 60_000L);
        modelo.decidir(
                s1.id(), DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.TERMINE, null);
        assertEquals(Boolean.TRUE, modelo.finalizada().getValue());
    }
}
