package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.in.ResolverRegresoApp;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.DiagnosticoRegresoApp;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
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
 * Pruebas unitarias de salida de la app, recuperación {@code SIGKILL}, declaración al volver y
 * vencimiento tras 3 días (HU-22, HU-40, HU-41, RN-23, RN-24, RN-29).
 */
public class RecuperacionSalidaServiceTest {

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
    private RegistrarSalidaAppService registrarSalida;
    private EvaluarRecuperacionSesionService evaluar;
    private ResolverRegresoAppService resolver;
    private VencerSesionesPendientesService vencer;

    private Materia fisica;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        programador = new FakeProgramadorAvisosSesion();
        marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(8, 0).valor());

        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        registrarSalida = new RegistrarSalidaAppService(sesiones, eventos, marcador, reloj);
        evaluar = new EvaluarRecuperacionSesionService(
                sesiones, eventos, materias, noMolestar, programador, marcador, reloj);
        resolver = new ResolverRegresoAppService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        vencer = new VencerSesionesPendientesService(
                sesiones, eventos, noMolestar, programador, reloj);

        fisica = materias.guardar(Materia.crear("Física", 0xFF2E7D32, null).valor());
    }

    @Test
    public void registraSalidaYDeclaraEstudioAppCerradaHastaHora() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(10, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.FORZAR_ACTIVAR);

        reloj.avanzarMilis(45 * MIN); // 08:45
        assertTrue(registrarSalida.ejecutar().isPresent());

        reloj.avanzarMilis(120 * MIN); // 10:45
        DiagnosticoRegresoApp diag = evaluar.ejecutar().get();
        assertFalse(diag.esRecuperacionCierreAbrupto());
        assertFalse(diag.vencidaPorTresDias());

        long hasta10 = diag.instanteSalidaMilis() + 75 * MIN; // 10:00
        assertEquals(ErrorValidacion.HORA_DECLARADA_FUERA_DE_RANGO,
                resolver.ejecutar(s.id(), ResolverRegresoApp.OpcionRegreso.HASTA_HORA,
                        diag.instanteSalidaMilis(), "").error());

        SesionEstudio cerrada = resolver.ejecutar(
                s.id(), ResolverRegresoApp.OpcionRegreso.HASTA_HORA, hasta10, "en grupo").valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, cerrada.codigoEstado());
        // 25 trabajo + 5 desc + 15 trabajo = 40 trabajo en app + 75 min app cerrada = 115 min
        assertEquals(Integer.valueOf(115), cerrada.duracionRealMinutos());
        assertFalse(noMolestar.activo);
    }

    @Test
    public void recuperaTrasSigkillYCierraAutomaticamenteTrasTresDias() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.FORZAR_ACTIVAR);

        // Simula SIGKILL tras 20 min sin SALIDA_APP
        reloj.avanzarMilis(20 * MIN);
        DiagnosticoRegresoApp sigkill = evaluar.ejecutar().get();
        assertTrue(sigkill.esRecuperacionCierreAbrupto());

        // Simula que pasan > 3 días
        reloj.avanzarMilis(4L * 24L * 60L * MIN);
        assertEquals(1, vencer.ejecutar());
        SesionEstudio vencida = sesiones.buscarPorId(s.id()).get();
        assertEquals(CodigoEstadoSesion.COMPLETADA, vencida.codigoEstado());
        assertFalse(noMolestar.activo);

        Optional<DiagnosticoRegresoApp> vacio = evaluar.ejecutar();
        assertFalse(vacio.isPresent());
    }
}
