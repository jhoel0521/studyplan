package io.github.jhoel0521.studyplan.application.service.tecnicas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
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
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas unitarias enfocadas de {@link DecidirFinPlanificadoOInvasionService} para verificar la
 * cancelación del recordatorio de la sesión siguiente al cambiar a ella ({@code HU-42}, {@code HU-34}).
 */
public class DecidirFinPlanificadoOInvasionServiceTest {

    private static final long MIN = 60_000L;

    private FakeSesionRepository sesiones;
    private FakeProgramadorRecordatorios recordatorios;
    private FakeReloj reloj;
    private IniciarSesionService iniciar;
    private DecidirFinPlanificadoOInvasionService decidir;
    private long materiaId;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        FakeEventoEstudioRepository eventos = new FakeEventoEstudioRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeAjustesRepository ajustes = new FakeAjustesRepository();
        FakeControladorNoMolestar noMolestar = new FakeControladorNoMolestar();
        FakeProgramadorAvisosSesion programador = new FakeProgramadorAvisosSesion();
        FakeMarcadorSalidaApp marcador = new FakeMarcadorSalidaApp();
        reloj = new FakeReloj(Fecha.de(2026, 11, 16).valor(), Hora.de(10, 0).valor(), -240);
        recordatorios = new FakeProgramadorRecordatorios();

        iniciar = new IniciarSesionService(
                sesiones,
                eventos,
                materias,
                ajustes,
                noMolestar,
                programador,
                marcador,
                reloj,
                recordatorios);
        DetenerSesionService detener = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcador, reloj);
        decidir = new DecidirFinPlanificadoOInvasionService(
                sesiones,
                eventos,
                materias,
                ajustes,
                noMolestar,
                programador,
                marcador,
                detener,
                null,
                reloj,
                recordatorios);

        materiaId = materias.guardar(Materia.crear("Química", 0xFF2E7D32, null).valor()).id();
    }

    @Test
    public void cambiarASiguienteCancelaRecordatorioDeSiguienteSesion() {
        Fecha hoy = reloj.hoy();
        SesionEstudio s1 = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                hoy,
                hoy,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                -240,
                null,
                null,
                AntelacionRecordatorio.MINUTOS_15)
                .valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                hoy,
                hoy,
                Hora.de(11, 0).valor(),
                Hora.de(12, 0).valor(),
                -240,
                null,
                null,
                AntelacionRecordatorio.MINUTOS_30)
                .valor());
        recordatorios.programar(s2.id(), s2.instanteRecordatorioUtcMilis());

        iniciar.ejecutar(s1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        reloj.avanzarMilis(60 * MIN);

        assertEquals(
                ErrorValidacion.SESION_NO_ENCONTRADA,
                decidir.ejecutar(
                                s1.id(),
                                DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.CAMBIAR_A_SIGUIENTE,
                                999L)
                        .error());
        assertTrue(recordatorios.estaProgramada(s2.id()));

        SesionEstudio nuevaActiva = decidir.ejecutar(
                        s1.id(),
                        DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.CAMBIAR_A_SIGUIENTE,
                        s2.id())
                .valor();

        assertEquals(s2.id(), nuevaActiva.id());
        assertEquals(CodigoEstadoSesion.EN_CURSO, nuevaActiva.codigoEstado());
        assertFalse(recordatorios.estaProgramada(s2.id()));
        assertTrue(recordatorios.cancelaciones().contains(s2.id()));
    }
}
