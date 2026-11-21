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
 * Pruebas unitarias del registro de la salida con la hora real en que ocurrió, tras la ventana de gracia de
 * 1 minuto (HU-40, RN-23).
 */
public class RegistrarSalidaAppServiceTest {

    private static final long MIN = 60_000L;

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMarcadorSalidaApp marcador;
    private FakeReloj reloj;
    private IniciarSesionService iniciar;
    private RegistrarSalidaAppService registrarSalida;
    private Materia algebra;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(8, 0).valor());
        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, new FakeAjustesRepository(), new FakeControladorNoMolestar(),
                new FakeProgramadorAvisosSesion(), marcador, reloj);
        registrarSalida = new RegistrarSalidaAppService(sesiones, eventos, materias, marcador, reloj);
        algebra = materias.guardar(Materia.crear("Álgebra", 0xFF2E7D32, null).valor());
    }

    private SesionEstudio iniciarSesion(TecnicaEstudio tecnica) {
        Fecha hoy = reloj.hoy();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                algebra.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(10, 0).valor(), "", null).valor());
        iniciar.ejecutar(s.id(), tecnica, PreferenciaNoMolestar.FORZAR_ACTIVAR);
        return s;
    }

    @Test
    public void registraLaSalidaConLaHoraRealYNoConLaDeAhora() {
        SesionEstudio s = iniciarSesion(TecnicaEstudio.LIBRE);
        long inicio = reloj.ahoraMilis();
        long salida = inicio + 15 * MIN;
        reloj.avanzarMilis(25 * MIN); // vuelve a los 25 minutos, la salida fue a los 15

        assertTrue(registrarSalida.ejecutar(salida).isPresent());

        List<EventoEstudio> lista = eventos.listarPorSesion(s.id());
        EventoEstudio ultimo = lista.get(lista.size() - 1);
        assertEquals(TipoEventoEstudio.SALIDA_APP, ultimo.tipo());
        assertEquals(salida, ultimo.instanteMilis());
        assertEquals(Long.valueOf(salida), marcador.obtenerInstanteSalida());
    }

    @Test
    public void noRegistraSiHayUnEventoPosteriorALaSalida() {
        SesionEstudio s = iniciarSesion(TecnicaEstudio.LIBRE);
        long inicio = reloj.ahoraMilis();

        // La salida habría ocurrido antes del evento de inicio: registrarla rompería el orden cronológico.
        assertFalse(registrarSalida.ejecutar(inicio - 1_000L).isPresent());
        assertEquals(1, eventos.listarPorSesion(s.id()).size());
    }

    @Test
    public void noRegistraSiLaSalidaYaEstaSinResolver() {
        iniciarSesion(TecnicaEstudio.LIBRE);
        long inicio = reloj.ahoraMilis();
        reloj.avanzarMilis(30 * MIN);

        assertTrue(registrarSalida.ejecutar(inicio + 10 * MIN).isPresent());
        assertFalse(registrarSalida.ejecutar(inicio + 20 * MIN).isPresent());
    }

    @Test
    public void saliendoEnUnDescansoPomodoroSoloMarcaSuFinProyectadoDesdeLaSalida() {
        SesionEstudio s = iniciarSesion(TecnicaEstudio.POMODORO);
        long inicio = reloj.ahoraMilis();
        long salida = inicio + 27 * MIN; // descanso corto de 25 a 30 minutos
        reloj.avanzarMilis(50 * MIN); // vuelve mucho después del fin del descanso

        assertFalse(registrarSalida.ejecutar(salida).isPresent());

        assertEquals(1, eventos.listarPorSesion(s.id()).size());
        assertEquals(Long.valueOf(inicio + 30 * MIN), marcador.obtenerFinDescansoPomodoro());
        assertNull(marcador.obtenerInstanteSalida());
    }
}
