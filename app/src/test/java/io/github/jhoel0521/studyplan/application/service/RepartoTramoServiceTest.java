package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoReparto;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeControladorNoMolestar;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMarcadorSalidaApp;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertFalse;

/**
 * Pruebas unitarias de {@link DecidirFinPlanificadoOInvasionService} y
 * {@link RepartirTramoSesionService} (HU-42, HU-43, HU-44, RN-25, RN-27, RN-28).
 */
public class RepartoTramoServiceTest {

    private static final long MIN = 60_000L;

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMateriaRepository materias;
    private FakeAjustesRepository ajustes;
    private FakeControladorNoMolestar noMolestar;
    private FakeProgramadorAvisosSesion programador;
    private FakeMarcadorSalidaApp marcador;
    private FakeReloj reloj;

    private IniciarSesionService iniciar;
    private DecidirFinPlanificadoOInvasionService decidir;
    private RepartirTramoSesionService repartir;

    private Materia mate;
    private Materia ingles;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        programador = new FakeProgramadorAvisosSesion();
        marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(10, 0).valor());

        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        DetenerSesionService detener = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcador, reloj);
        decidir = new DecidirFinPlanificadoOInvasionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, detener, reloj);
        repartir = new RepartirTramoSesionService(
                sesiones, eventos, noMolestar, programador, marcador, reloj);

        mate = materias.guardar(Materia.crear("Matemáticas", 0xFF1565C0, null).valor());
        ingles = materias.guardar(Materia.crear("Inglés", 0xFF2E7D32, null).valor());
    }

    @Test
    public void cambiaASiguienteSinDetenerElTiempo() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s1 = sesiones.guardar(SesionEstudio.planificar(
                mate.id(), hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                ingles.id(), hoy, hoy, Hora.de(11, 0).valor(), Hora.de(12, 0).valor(), "", null)
                .valor());

        iniciar.ejecutar(s1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(60 * MIN);

        SesionEstudio nuevaActiva = decidir.ejecutar(
                s1.id(),
                DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.CAMBIAR_A_SIGUIENTE,
                s2.id()).valor();

        assertEquals(s2.id(), nuevaActiva.id());
        assertEquals(CodigoEstadoSesion.EN_CURSO, nuevaActiva.codigoEstado());
        assertEquals(CodigoEstadoSesion.COMPLETADA, sesiones.buscarPorId(s1.id()).get().codigoEstado());
        assertEquals(Integer.valueOf(60), sesiones.buscarPorId(s1.id()).get().duracionRealMinutos());
    }

    @Test
    public void reparteTramoContinuoEntreDosSesionesEnUnaTransaccion() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s1 = sesiones.guardar(SesionEstudio.planificar(
                mate.id(), hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                ingles.id(), hoy, hoy, Hora.de(11, 0).valor(), Hora.de(12, 0).valor(), "", null)
                .valor());

        long tInicio = reloj.ahoraMilis();
        iniciar.ejecutar(s1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(130 * MIN); // 12:10

        long corte1105 = tInicio + 65 * MIN;
        ResultadoReparto res = repartir.ejecutar(s1.id(), s2.id(), corte1105).valor();
        assertEquals(65, res.minutosPrimeraSesion());
        assertEquals(65, res.minutosSegundaSesion());

        assertEquals(CodigoEstadoSesion.COMPLETADA, sesiones.buscarPorId(s1.id()).get().codigoEstado());
        assertEquals(CodigoEstadoSesion.COMPLETADA, sesiones.buscarPorId(s2.id()).get().codigoEstado());
        assertEquals(Integer.valueOf(65), sesiones.buscarPorId(s1.id()).get().duracionRealMinutos());
        assertEquals(Integer.valueOf(65), sesiones.buscarPorId(s2.id()).get().duracionRealMinutos());
        assertTrue(sesiones.listarEnCurso().isEmpty());
    }

    @Test
    public void repartirTramoCancelaRecordatorioDeSegundaSesionInvadida() {
        FakeProgramadorRecordatorios recordatorios = new FakeProgramadorRecordatorios();
        RepartirTramoSesionService repartirConRecordatorios = new RepartirTramoSesionService(
                sesiones, eventos, noMolestar, programador, marcador, null, reloj, recordatorios);

        Fecha hoy = reloj.hoy();
        SesionEstudio s1 = sesiones.guardar(SesionEstudio.planificar(
                mate.id(), hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                ingles.id(),
                hoy,
                hoy,
                Hora.de(11, 0).valor(),
                Hora.de(12, 0).valor(),
                0,
                "",
                null,
                AntelacionRecordatorio.MINUTOS_15)
                .valor());
        recordatorios.programar(s2.id(), s2.instanteRecordatorioUtcMilis());

        long tInicio = reloj.ahoraMilis();
        iniciar.ejecutar(s1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(130 * MIN);

        assertTrue(repartirConRecordatorios.ejecutar(s1.id(), s2.id(), tInicio + 65 * MIN).esValido());
        assertFalse(recordatorios.estaProgramada(s2.id()));
        assertTrue(recordatorios.cancelaciones().contains(s2.id()));
    }
}
