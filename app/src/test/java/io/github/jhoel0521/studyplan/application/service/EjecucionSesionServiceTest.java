package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
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
 * Pruebas unitarias de {@link ConfigurarAjustesEstudioService}, {@link IniciarSesionService},
 * {@link PausarReanudarSesionService}, {@link DetenerSesionService} y
 * {@link ConsultarEjecucionSesionService} (HU-22 a HU-24, RN-07, RN-17, RN-18).
 */
public class EjecucionSesionServiceTest {

    private static final long MIN = 60_000L;

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMateriaRepository materias;
    private FakeAjustesRepository ajustes;
    private FakeControladorNoMolestar noMolestar;
    private FakeProgramadorAvisosSesion programador;
    private FakeMarcadorSalidaApp marcador;
    private io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository tramos;
    private FakeReloj reloj;

    private ConfigurarAjustesEstudioService configurarAjustes;
    private IniciarSesionService iniciar;
    private PausarReanudarSesionService pausarReanudar;
    private DetenerSesionService detener;
    private ConsultarEjecucionSesionService consultar;

    private Materia calculo;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        programador = new FakeProgramadorAvisosSesion();
        marcador = new FakeMarcadorSalidaApp();
        tramos = new io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(9, 0).valor());

        configurarAjustes = new ConfigurarAjustesEstudioService(ajustes);
        iniciar = new IniciarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcador, reloj);
        pausarReanudar = new PausarReanudarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, reloj);
        detener = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcador, tramos, reloj);
        consultar = new ConsultarEjecucionSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, reloj);

        calculo = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, null).valor());
    }

    @Test
    public void flujoCompletoPausaReanudacionNoMolestarYDetencion() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s1 = sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), hoy, hoy, Hora.de(9, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), hoy, hoy, Hora.de(14, 0).valor(), Hora.de(15, 0).valor(), "", null)
                .valor());

        configurarAjustes.ejecutar(true, 25, 5, 15, 4);
        assertTrue(iniciar.ejecutar(s1.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .esValido());
        assertTrue(noMolestar.activo);
        assertNotNull(programador.ultimoInstante);

        // Invariante única EN_CURSO: s2 no puede iniciar mientras s1 esté activa
        assertEquals(ErrorValidacion.SESION_YA_EN_CURSO,
                iniciar.ejecutar(s2.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                        .error());

        // Cambiar ajustes Pomodoro en caliente no altera el snapshot de s1 (RN-17)
        configurarAjustes.ejecutar(true, 50, 10, 20, 2);
        assertEquals(25, sesiones.buscarPorId(s1.id()).get().configuracionPomodoroSnapshot().trabajoMinutos());

        reloj.avanzarMilis(20 * MIN);
        pausarReanudar.ejecutar(s1.id());
        assertFalse(noMolestar.activo);

        reloj.avanzarMilis(10 * MIN); // 10 min en pausa (no suman)
        pausarReanudar.ejecutar(s1.id());
        assertTrue(noMolestar.activo);

        reloj.avanzarMilis(5 * MIN);
        ResumenEjecucionSesion resumen = consultar.ejecutar(s1.id()).get();
        assertEquals(25 * MIN, resumen.milisEfectivos());

        SesionEstudio finalizada = detener.ejecutar(s1.id()).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, finalizada.codigoEstado());
        assertEquals(Integer.valueOf(25), finalizada.duracionRealMinutos());
        assertFalse(noMolestar.activo);
    }

    @Test
    public void detenerSesionNocturnaMaterializaTramosPorDiaRn31() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = lunes.sumarDias(1);
        reloj.fijar(lunes, Hora.de(22, 0).valor());

        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), lunes, lunes,
                Hora.de(22, 0).valor(), Hora.de(1, 0).valor(), 0, "", null).valor());

        assertTrue(iniciar.ejecutar(s.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).esValido());
        reloj.avanzarMilis(180 * MIN);
        assertTrue(detener.ejecutar(s.id()).esValido());

        assertEquals(120, tramos.sumarMinutosEnRango(lunes, lunes));
        assertEquals(60, tramos.sumarMinutosEnRango(martes, martes));
    }
}
