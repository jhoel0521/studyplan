package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.Resultado;
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
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;

/**
 * Pruebas unitarias de {@link CambiarDeMateriaService} ({@code HU-48}).
 */
public final class CambiarDeMateriaServiceTest {

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMateriaRepository materias;
    private FakeAjustesRepository ajustes;
    private FakeControladorNoMolestar noMolestar;
    private FakeProgramadorAvisosSesion programador;
    private FakeMarcadorSalidaApp marcadorSalida;
    private FakeTramoDiarioRepository tramos;
    private FakeReloj reloj;

    @Before
    public void setUp() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        programador = new FakeProgramadorAvisosSesion();
        marcadorSalida = new FakeMarcadorSalidaApp();
        tramos = new FakeTramoDiarioRepository(sesiones);
        reloj = new FakeReloj(Fecha.parsear("2026-11-18").valor(), Hora.de(9, 0).valor());
    }

    @Test
    public void cierraTramoPrevioYAbreNuevaSesionSinPlanConservandoTiempo() {
        Materia mate = materias.guardar(Materia.crear("Matemática", 0xFF1E88E5, null).valor());
        Materia historia = materias.guardar(Materia.crear("Historia", 0xFF43A047, null).valor());

        EstudiarAhoraService estudiarAhora = new EstudiarAhoraService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, reloj);
        SesionEstudio primera = estudiarAhora.ejecutar(
                mate.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();

        reloj.avanzarMilis(25 * 60_000L);
        CambiarDeMateriaService cambiar = new CambiarDeMateriaService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, tramos, reloj);

        Resultado<SesionEstudio> res = cambiar.ejecutar(primera.id(), historia.id(), null);
        assertTrue(res.esValido());

        SesionEstudio primeraCerrada = sesiones.buscarPorId(primera.id()).get();
        assertEquals(CodigoEstadoSesion.COMPLETADA, primeraCerrada.codigoEstado());
        assertEquals(25, (int) primeraCerrada.duracionRealMinutos());

        SesionEstudio segunda = res.valor();
        assertEquals(CodigoEstadoSesion.EN_CURSO, segunda.codigoEstado());
        assertEquals((long) historia.id(), segunda.materiaId());
        assertEquals(TecnicaEstudio.LIBRE, segunda.tecnica());
        assertEquals(25, tramos.sumarMinutosEnRango(reloj.hoy(), reloj.hoy()));
    }
}
