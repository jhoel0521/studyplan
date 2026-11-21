package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
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
 * Pruebas unitarias de {@link EstudiarAhoraService} ({@code HU-47}, {@code RN-07}, {@code RN-32}).
 */
public final class EstudiarAhoraServiceTest {

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMateriaRepository materias;
    private FakeAjustesRepository ajustes;
    private FakeControladorNoMolestar noMolestar;
    private FakeProgramadorAvisosSesion programador;
    private FakeMarcadorSalidaApp marcadorSalida;
    private FakeReloj reloj;
    private EstudiarAhoraService service;

    @Before
    public void setUp() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        programador = new FakeProgramadorAvisosSesion();
        marcadorSalida = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.parsear("2026-11-18").valor(), Hora.de(14, 0).valor());
        service = new EstudiarAhoraService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, reloj);
    }

    @Test
    public void iniciaDirectamenteEnCursoSobreEstudiosVariosYBloqueaSegundaSesion() {
        Resultado<SesionEstudio> res = service.ejecutar(
                null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);

        assertTrue(res.esValido());
        SesionEstudio s = res.valor();
        assertEquals(CodigoEstadoSesion.EN_CURSO, s.codigoEstado());
        assertFalse(s.tienePlan());
        assertEquals(1, eventos.listarPorSesion(s.id()).size());
        assertTrue(materias.obtener(s.materiaId()).get().esGeneral());

        Resultado<SesionEstudio> segunda = service.ejecutar(
                null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        assertFalse(segunda.esValido());
        assertEquals(ErrorValidacion.SESION_YA_EN_CURSO, segunda.error());
    }

    @Test
    public void creaMateriaAlVueloYArrancaEnCurso() {
        Resultado<SesionEstudio> res = service.ejecutarConNuevaMateria(
                "Química Orgánica",
                0xFFE53935,
                TecnicaEstudio.POMODORO,
                PreferenciaNoMolestar.FORZAR_ACTIVAR);

        assertTrue(res.esValido());
        Materia creada = materias.obtener(res.valor().materiaId()).get();
        assertEquals("Química Orgánica", creada.nombre());
        assertEquals(CodigoEstadoSesion.EN_CURSO, res.valor().codigoEstado());
        assertEquals(TecnicaEstudio.POMODORO, res.valor().tecnica());
    }
}
