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
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
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

    @Test
    public void cambiaDeMateriaYPublicaMetasAlcanzadasAlDetener() {
        FakeMateriaRepository mats = new FakeMateriaRepository();
        FakeEventoEstudioRepository evs = new FakeEventoEstudioRepository();
        FakeAjustesRepository aj = new FakeAjustesRepository();
        FakeControladorNoMolestar dnd = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion prog = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marc = new FakeMarcadorSalidaApp();
        io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository tramos =
                new io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository(sesiones);
        io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository metas =
                new io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository();
        io.github.jhoel0521.studyplan.fakes.FakePerfilRepository perfiles =
                new io.github.jhoel0521.studyplan.fakes.FakePerfilRepository();
        perfiles.guardar(io.github.jhoel0521.studyplan.domain.Perfil.crear("Ana", 1.0).valor());

        Materia m1 = mats.guardar(Materia.crear("Matemáticas", 0xFF1E88E5, null).valor());
        Materia m2 = mats.guardar(Materia.crear("Informática", 0xFF43A047, null, 1.0).valor());

        io.github.jhoel0521.studyplan.application.service.EstudiarAhoraService estudiarAhora =
                new io.github.jhoel0521.studyplan.application.service.EstudiarAhoraService(
                        sesiones, evs, mats, aj, dnd, prog, marc, reloj);
        SesionEstudio s1 = estudiarAhora.ejecutar(
                m1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();

        SesionEnCursoViewModel vm = new SesionEnCursoViewModel(
                new ConsultarEjecucionSesionService(sesiones, evs, mats, aj, dnd, reloj),
                new PausarReanudarSesionService(sesiones, evs, mats, aj, dnd, prog, reloj),
                new DetenerSesionService(sesiones, evs, dnd, prog, marc, tramos, metas, perfiles, mats, reloj),
                new DecidirFinPlanificadoOInvasionService(
                        sesiones, evs, mats, aj, dnd, prog, marc,
                        new DetenerSesionService(sesiones, evs, dnd, prog, marc, tramos, metas, perfiles, mats, reloj),
                        tramos, reloj),
                new io.github.jhoel0521.studyplan.application.service.CambiarDeMateriaService(
                        sesiones, evs, mats, aj, dnd, prog, marc, tramos, reloj),
                new io.github.jhoel0521.studyplan.application.service.ListarMateriasService(mats, sesiones),
                Runnable::run);

        reloj.avanzarMilis(15 * 60_000L);
        vm.cambiarDeMateria(s1.id(), m2.id(), TecnicaEstudio.LIBRE);
        Long nuevaId = vm.nuevaSesionActiva().getValue();
        assertNotNull(nuevaId);

        reloj.avanzarMilis(60 * 60_000L);
        vm.detener(nuevaId);
        assertNotNull(vm.metasAlcanzadas().getValue());
        assertEquals(2, vm.metasAlcanzadas().getValue().size());
    }

    @Test
    public void exponeSalidaPendienteYReanudaConTopico() {
        FakeEventoEstudioRepository evs = new FakeEventoEstudioRepository();
        FakeMateriaRepository mats = new FakeMateriaRepository();
        FakeAjustesRepository aj = new FakeAjustesRepository();
        FakeControladorNoMolestar dnd = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion prog = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marc = new FakeMarcadorSalidaApp();
        Materia m1 = mats.guardar(Materia.crear("Física", 0xFF1E88E5, null).valor());
        SesionEstudio s1 = new io.github.jhoel0521.studyplan.application.service.EstudiarAhoraService(
                sesiones, evs, mats, aj, dnd, prog, marc, reloj)
                .ejecutar(m1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .valor();

        SesionEnCursoViewModel vm = new SesionEnCursoViewModel(
                new ConsultarEjecucionSesionService(sesiones, evs, mats, aj, dnd, reloj),
                new PausarReanudarSesionService(sesiones, evs, mats, aj, dnd, prog, reloj),
                new DetenerSesionService(sesiones, evs, dnd, prog, marc, null, null, null, mats, reloj),
                new DecidirFinPlanificadoOInvasionService(
                        sesiones, evs, mats, aj, dnd, prog, marc,
                        new DetenerSesionService(sesiones, evs, dnd, prog, marc, null, null, null, mats, reloj),
                        null, reloj),
                null,
                null,
                Runnable::run);

        vm.pausarOReanudar(s1.id());
        assertTrue(vm.resumen().getValue().pausada());

        reloj.avanzarMilis(5 * 60_000L);
        vm.reanudarConTopico(
                s1.id(),
                io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio.DESCANSO_COMIDA,
                "Almuerzo");
        assertFalse(vm.resumen().getValue().pausada());
        assertEquals(
                io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio.DESCANSO_COMIDA,
                evs.listarPorSesion(s1.id()).get(evs.listarPorSesion(s1.id()).size() - 1)
                        .topico()
                        .orElse(null));

        reloj.avanzarMilis(3 * 60_000L);
        new io.github.jhoel0521.studyplan.application.service.RegistrarSalidaAppService(
                sesiones, evs, mats, marc, reloj).ejecutar();
        vm.cargar(s1.id());
        assertTrue(vm.resumen().getValue().salidaPendienteResolver());
    }

    @Test
    public void cargarOIniciarDesdeRecordatorioIniciaLibreReabreActivaYPublicaConcurrencia() {
        FakeEventoEstudioRepository evs = new FakeEventoEstudioRepository();
        FakeMateriaRepository mats = new FakeMateriaRepository();
        FakeAjustesRepository aj = new FakeAjustesRepository();
        FakeControladorNoMolestar dnd = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion prog = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marc = new FakeMarcadorSalidaApp();
        DetenerSesionService det = new DetenerSesionService(
                sesiones, evs, dnd, prog, marc, null, null, null, mats, reloj);
        IniciarSesionService initSrv = new IniciarSesionService(
                sesiones, evs, mats, aj, dnd, prog, marc, reloj);

        SesionEnCursoViewModel vm = new SesionEnCursoViewModel(
                new ConsultarEjecucionSesionService(sesiones, evs, mats, aj, dnd, reloj),
                new PausarReanudarSesionService(sesiones, evs, mats, aj, dnd, prog, reloj),
                det,
                new DecidirFinPlanificadoOInvasionService(
                        sesiones, evs, mats, aj, dnd, prog, marc, det, null, reloj),
                null,
                null,
                initSrv,
                Runnable::run);

        Materia m = mats.guardar(Materia.crear("Biología", 0xFF2E7D32, null).valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                m.id(),
                reloj.hoy(),
                reloj.hoy(),
                Hora.de(11, 0).valor(),
                Hora.de(12, 0).valor(),
                "",
                null).valor());

        // Inicia directamente PENDIENTE -> EN_CURSO en Libre + heredar global
        vm.cargarOIniciarDesdeRecordatorio(s1.id());
        assertNotNull(vm.resumen().getValue());
        assertEquals(CodigoEstadoSesion.EN_CURSO, vm.resumen().getValue().sesion().codigoEstado());
        assertEquals(TecnicaEstudio.LIBRE, vm.resumen().getValue().sesion().tecnica());

        // Llamada repetida cuando ya está EN_CURSO recarga sin error
        vm.cargarOIniciarDesdeRecordatorio(s1.id());
        assertEquals(null, vm.errorInicioDirecto().getValue());

        // Intentar otra sesión mientras s1 sigue activa publica SESION_YA_EN_CURSO
        vm.cargarOIniciarDesdeRecordatorio(s2.id());
        assertEquals(ErrorValidacion.SESION_YA_EN_CURSO, vm.errorInicioDirecto().getValue());
    }
}
