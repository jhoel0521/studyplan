package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
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
 * Pruebas unitarias de {@link AlternarNoMolestarSesionService} (HU-24, RN-18, RNF-18).
 */
public class AlternarNoMolestarSesionServiceTest {

    private static final long MIN = 60_000L;

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeAjustesRepository ajustes;
    private FakeControladorNoMolestar noMolestar;
    private FakeReloj reloj;
    private IniciarSesionService iniciar;
    private PausarReanudarSesionService pausarReanudar;
    private DetenerSesionService detener;
    private AlternarNoMolestarSesionService servicio;
    private long sesionId;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion programador = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(9, 0).valor());
        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        pausarReanudar = new PausarReanudarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, reloj);
        detener = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcador, new FakeTramoDiarioRepository(), reloj);
        servicio = new AlternarNoMolestarSesionService(sesiones, eventos, ajustes, noMolestar);
        Materia calculo = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, null).valor());
        Fecha hoy = reloj.hoy();
        sesionId = sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), hoy, hoy, Hora.de(9, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor()).id();
        assertTrue(iniciar.ejecutar(sesionId, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .esValido());
    }

    @Test
    public void conNoMolestarApagadoLoActivaSoloParaLaSesion() {
        assertFalse(noMolestar.activo);

        Resultado<Boolean> r = servicio.ejecutar(sesionId);

        assertTrue(r.esValido());
        assertTrue(r.valor());
        assertTrue(noMolestar.activo);
        assertEquals(PreferenciaNoMolestar.FORZAR_ACTIVAR,
                sesiones.buscarPorId(sesionId).get().preferenciaNoMolestar());
        assertFalse(ajustes.obtener().noMolestarGlobal());
    }

    @Test
    public void conNoMolestarActivoLoDesactivaYRestauraElFiltro() {
        servicio.ejecutar(sesionId);
        int restauracionesAntes = noMolestar.restauraciones;

        Resultado<Boolean> r = servicio.ejecutar(sesionId);

        assertTrue(r.esValido());
        assertFalse(r.valor());
        assertFalse(noMolestar.activo);
        assertEquals(restauracionesAntes + 1, noMolestar.restauraciones);
        assertEquals(PreferenciaNoMolestar.FORZAR_DESACTIVAR,
                sesiones.buscarPorId(sesionId).get().preferenciaNoMolestar());
    }

    @Test
    public void sinPermisoNoCambiaNadaYDevuelveError() {
        noMolestar.permiso = false;

        Resultado<Boolean> r = servicio.ejecutar(sesionId);

        assertFalse(r.esValido());
        assertEquals(ErrorValidacion.SIN_PERMISO_NO_MOLESTAR, r.error());
        assertEquals(PreferenciaNoMolestar.HEREDAR_GLOBAL,
                sesiones.buscarPorId(sesionId).get().preferenciaNoMolestar());
    }

    @Test
    public void conUnaSesionQueNoEstaEnCursoDevuelveError() {
        Resultado<Boolean> r = servicio.ejecutar(sesionId + 99);

        assertFalse(r.esValido());
        assertEquals(ErrorValidacion.SESION_NO_ENCONTRADA, r.error());
    }

    @Test
    public void pausarYDetenerRestauranElFiltroTrasAlternar() {
        servicio.ejecutar(sesionId);
        assertTrue(noMolestar.activo);
        reloj.avanzarMilis(10 * MIN);

        assertTrue(pausarReanudar.ejecutar(sesionId).esValido());
        assertFalse(noMolestar.activo);

        assertTrue(pausarReanudar.ejecutar(sesionId).esValido());
        assertTrue(noMolestar.activo);

        reloj.avanzarMilis(5 * MIN);
        assertTrue(detener.ejecutar(sesionId).esValido());
        assertFalse(noMolestar.activo);
    }
}
