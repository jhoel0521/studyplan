package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ConsultarSesionService;
import io.github.jhoel0521.studyplan.application.service.IniciarSesionService;
import io.github.jhoel0521.studyplan.application.service.RepartirTramoSesionService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeControladorNoMolestar;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMarcadorSalidaApp;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas unitarias de {@link RepartoSesionViewModel} (HU-44, RN-28).
 */
public class RepartoSesionViewModelTest {

    @Rule
    public final InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeReloj reloj;
    private RepartoSesionViewModel modelo;
    private SesionEstudio s1;
    private SesionEstudio s2;

    @Before
    public void configurar() {
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeAjustesRepository ajustes = new FakeAjustesRepository();
        FakeControladorNoMolestar noMolestar = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion programador = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(10, 0).valor());

        IniciarSesionService iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        ConsultarSesionService consultar = new ConsultarSesionService(
                sesiones, materias, eventos, reloj,
                new VencerSesionesPendientesService(sesiones, eventos, noMolestar, programador, reloj));
        RepartirTramoSesionService repartir = new RepartirTramoSesionService(
                sesiones, eventos, noMolestar, programador, marcador, reloj);
        modelo = new RepartoSesionViewModel(consultar, eventos, repartir, reloj, Runnable::run);

        Materia mate = materias.guardar(Materia.crear("Mate", 0xFF1565C0, null).valor());
        Materia ing = materias.guardar(Materia.crear("Inglés", 0xFF2E7D32, null).valor());
        Fecha hoy = reloj.hoy();
        s1 = sesiones.guardar(SesionEstudio.planificar(
                mate.id(), hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null).valor());
        s2 = sesiones.guardar(SesionEstudio.planificar(
                ing.id(), hoy, hoy, Hora.de(11, 0).valor(), Hora.de(12, 0).valor(), "", null).valor());

        iniciar.ejecutar(s1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(120 * 60_000L);
    }

    @Test
    public void desplazaCorteYConfirmaReparto() {
        modelo.cargar(s1.id(), s2.id());
        assertNotNull(modelo.estado().getValue());
        assertEquals(60, modelo.estado().getValue().reparto.minutosPrimeraSesion());

        modelo.desplazarCorteMinutos(5);
        assertEquals(65, modelo.estado().getValue().reparto.minutosPrimeraSesion());
        assertEquals(55, modelo.estado().getValue().reparto.minutosSegundaSesion());

        modelo.confirmar();
        assertEquals(Boolean.TRUE, modelo.guardado().getValue());
    }
}
