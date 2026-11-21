package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.in.ResolverRegresoApp;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DiagnosticoRegresoApp;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
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
    public void laSalidaRetroactivaDeLaGraciaAbreLaPreguntaDesdeLaHoraRealDeSalida() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(10, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.FORZAR_ACTIVAR);
        long inicio = reloj.ahoraMilis();

        // Sale a los 15 minutos, pasa más de 1 minuto fuera y vuelve a los 25 (HU-40, RN-23).
        reloj.avanzarMilis(25 * MIN);
        assertTrue(registrarSalida.ejecutar(inicio + 15 * MIN).isPresent());

        DiagnosticoRegresoApp diag = evaluar.ejecutar().get();
        assertEquals(inicio + 15 * MIN, diag.instanteSalidaMilis());
        assertFalse("No es un cierre abrupto", diag.esRecuperacionCierreAbrupto());
        assertEquals("Un solo evento de salida", 1, eventos.listarPorSesion(s.id()).stream()
                .filter(e -> e.tipo() == io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio.SALIDA_APP)
                .count());
    }

    @Test
    public void registraSalidaYDeclaraEstudioAppCerradaHastaHora() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(10, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.FORZAR_ACTIVAR);

        reloj.avanzarMilis(45 * MIN); // 08:45
        assertTrue(registrarSalida.ejecutar(reloj.ahoraMilis()).isPresent());

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

    @Test
    public void salidaDuranteDescansoPomodoroNoAbreDialogoYExcesoAbreDesdeFinDeDescanso() {
        Materia mate = materias.guardar(Materia.crear("Mate", 0xFF1565C0, null).valor());
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                mate.id(), reloj.hoy(), reloj.hoy(),
                Hora.de(9, 0).valor(), Hora.de(11, 0).valor(), "", null).valor());
        assertTrue(iniciar.ejecutar(s.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.HEREDAR_GLOBAL).esValido());

        // Avanzamos 27 min (descanso corto de 5 min: faltan 3 min)
        reloj.avanzarMilis(27 * MIN);
        assertFalse(registrarSalida.ejecutar(reloj.ahoraMilis()).isPresent());

        // Volvemos 2 min después (aún en descanso) -> no abre W13
        reloj.avanzarMilis(2 * MIN);
        assertFalse(evaluar.ejecutar().isPresent());

        // Salimos de nuevo en el último minuto del descanso y volvemos 6 min después (5 min de exceso)
        assertFalse(registrarSalida.ejecutar(reloj.ahoraMilis()).isPresent());
        long finDescansoEsperado = reloj.ahoraMilis() + 1 * MIN;
        reloj.avanzarMilis(6 * MIN);
        Optional<DiagnosticoRegresoApp> diag = evaluar.ejecutar();
        assertTrue(diag.isPresent());
        assertEquals(finDescansoEsperado, diag.get().instanteSalidaMilis());
    }

    @Test
    public void retomarSinSumarNoSumaTiempoFueraYGrabaTopicoEnRegreso() {
        Materia mate = materias.guardar(Materia.crear("Mate", 0xFF1565C0, null).valor());
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                mate.id(), reloj.hoy(), reloj.hoy(),
                Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "", null).valor());
        assertTrue(iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).esValido());

        reloj.avanzarMilis(10 * MIN);
        assertTrue(registrarSalida.ejecutar(reloj.ahoraMilis()).isPresent());
        reloj.avanzarMilis(4 * MIN);

        assertTrue(resolver.ejecutarConMetas(
                s.id(),
                ResolverRegresoApp.OpcionRegreso.RETOMAR_SIN_SUMAR,
                null,
                "",
                io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio.LLAMADA_MENSAJE).esValido());

        // Solo los 10 min previos deben contar como tiempo efectivo
        long milis = io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal.calcularMilisEfectivos(
                TecnicaEstudio.LIBRE, null, 60, eventos.listarPorSesion(s.id()), reloj.ahoraMilis());
        assertEquals(10 * MIN, milis);
    }

    /** HU-50: un bloqueo sin desbloqueo no crea salida ni pide recuperación; la sesión sigue contando. */
    @Test
    public void unBloqueoSinDesbloqueoNoCreaSalidaNiPideRecuperacion() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(10 * MIN);
        assertTrue(new RegistrarBloqueoPantallaService(sesiones, eventos, reloj).ejecutar().isPresent());
        int eventosAntes = eventos.listarPorSesion(s.id()).size();

        reloj.avanzarMilis(5 * 60 * MIN); // 5 horas después, con el proceso terminado

        assertFalse(evaluar.ejecutar().isPresent());
        assertEquals(eventosAntes, eventos.listarPorSesion(s.id()).size());
        assertTrue(sesiones.buscarEnCurso().isPresent());
    }

    /** HU-50: el bloqueo no evita el vencimiento de 3 días (RN-29). */
    @Test
    public void unBloqueoSinDesbloqueoVenceTrasTresDias() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(10 * MIN);
        new RegistrarBloqueoPantallaService(sesiones, eventos, reloj).ejecutar();

        reloj.avanzarMilis(4 * 24 * 60 * MIN);

        DiagnosticoRegresoApp diag = evaluar.ejecutar().get();
        assertTrue(diag.vencidaPorTresDias());
        assertFalse(sesiones.buscarEnCurso().isPresent());
    }

    /** HU-50: recién desbloqueado y de vuelta en la app no se fabrica una salida ni se pregunta nada. */
    @Test
    public void recienDesbloqueadoNoCreaSalidaNiPideRecuperacion() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(), "", null)
                .valor());
        iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(10 * MIN);
        new RegistrarBloqueoPantallaService(sesiones, eventos, reloj).ejecutar();
        reloj.avanzarMilis(25 * MIN);
        new RegistrarDesbloqueoService(sesiones, eventos, reloj).ejecutar();
        int eventosAntes = eventos.listarPorSesion(s.id()).size();

        assertFalse(evaluar.ejecutar().isPresent());
        assertEquals(eventosAntes, eventos.listarPorSesion(s.id()).size());
    }
}
