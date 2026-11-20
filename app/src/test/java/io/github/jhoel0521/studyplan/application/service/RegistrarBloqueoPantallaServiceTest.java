package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeControladorNoMolestar;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMarcadorSalidaApp;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas unitarias de {@link RegistrarBloqueoPantallaService} (HU-50, HU-40, RN-23).
 */
public class RegistrarBloqueoPantallaServiceTest {

    private static final long MIN = 60_000L;

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMarcadorSalidaApp marcador;
    private FakeReloj reloj;
    private IniciarSesionService iniciar;
    private PausarReanudarSesionService pausarReanudar;
    private RegistrarSalidaAppService registrarSalida;
    private ConfigurarAjustesEstudioService configurarAjustes;
    private long sesionId;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeAjustesRepository ajustes = new FakeAjustesRepository();
        FakeControladorNoMolestar noMolestar = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion programador = new FakeProgramadorAvisosSesion();
        marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(9, 0).valor());
        configurarAjustes = new ConfigurarAjustesEstudioService(ajustes);
        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        pausarReanudar = new PausarReanudarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, reloj);
        registrarSalida = new RegistrarSalidaAppService(sesiones, eventos, marcador, reloj);
        Materia calculo = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, null).valor());
        Fecha hoy = reloj.hoy();
        sesionId = sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), hoy, hoy, Hora.de(9, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor()).id();
    }

    private void iniciarSesion(TecnicaEstudio tecnica) {
        assertTrue(iniciar.ejecutar(sesionId, tecnica, PreferenciaNoMolestar.HEREDAR_GLOBAL).esValido());
    }

    private TipoEventoEstudio ultimoTipo() {
        List<EventoEstudio> lista = eventos.listarPorSesion(sesionId);
        return lista.get(lista.size() - 1).tipo();
    }

    @Test
    public void conUnaSesionEnCursoRegistraElBloqueo() {
        iniciarSesion(TecnicaEstudio.LIBRE);
        RegistrarBloqueoPantallaService servicio =
                new RegistrarBloqueoPantallaService(sesiones, eventos, reloj);

        assertTrue(servicio.ejecutar().isPresent());
        assertEquals(TipoEventoEstudio.BLOQUEO_PANTALLA, ultimoTipo());
    }

    @Test
    public void sinSesionEnCursoNoRegistraNada() {
        RegistrarBloqueoPantallaService servicio =
                new RegistrarBloqueoPantallaService(sesiones, eventos, reloj);

        assertFalse(servicio.ejecutar().isPresent());
        assertTrue(eventos.listarPorSesion(sesionId).isEmpty());
    }

    @Test
    public void conLaSesionPausadaTambienRegistraElBloqueo() {
        iniciarSesion(TecnicaEstudio.LIBRE);
        reloj.avanzarMilis(10 * MIN);
        assertTrue(pausarReanudar.ejecutar(sesionId).esValido());
        RegistrarBloqueoPantallaService servicio =
                new RegistrarBloqueoPantallaService(sesiones, eventos, reloj);

        assertTrue(servicio.ejecutar().isPresent());
        assertEquals(TipoEventoEstudio.BLOQUEO_PANTALLA, ultimoTipo());
    }

    @Test
    public void unaSegundaLlamadaSeguidaNoDuplicaElEvento() {
        iniciarSesion(TecnicaEstudio.LIBRE);
        RegistrarBloqueoPantallaService servicio =
                new RegistrarBloqueoPantallaService(sesiones, eventos, reloj);
        servicio.ejecutar();
        int antes = eventos.listarPorSesion(sesionId).size();

        assertFalse(servicio.ejecutar().isPresent());
        assertEquals(antes, eventos.listarPorSesion(sesionId).size());
    }

    @Test
    public void conUnaSalidaPendienteNoRegistraElBloqueo() {
        iniciarSesion(TecnicaEstudio.LIBRE);
        reloj.avanzarMilis(10 * MIN);
        assertTrue(registrarSalida.ejecutar().isPresent());
        RegistrarBloqueoPantallaService servicio =
                new RegistrarBloqueoPantallaService(sesiones, eventos, reloj);

        assertFalse(servicio.ejecutar().isPresent());
        assertEquals(TipoEventoEstudio.SALIDA_APP, ultimoTipo());
    }

    @Test
    public void enDescansoPomodoroRegistraElBloqueoSinMarcarFinDeDescanso() {
        configurarAjustes.ejecutar(true, 25, 5, 15, 4);
        iniciarSesion(TecnicaEstudio.POMODORO);
        reloj.avanzarMilis(26 * MIN);
        RegistrarBloqueoPantallaService servicio =
                new RegistrarBloqueoPantallaService(sesiones, eventos, reloj);

        assertTrue(servicio.ejecutar().isPresent());
        assertEquals(TipoEventoEstudio.BLOQUEO_PANTALLA, ultimoTipo());
        assertNull(marcador.obtenerFinDescansoPomodoro());
    }
}
