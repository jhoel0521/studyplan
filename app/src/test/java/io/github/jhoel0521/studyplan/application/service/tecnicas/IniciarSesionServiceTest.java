package io.github.jhoel0521.studyplan.application.service.tecnicas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
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
 * Pruebas unitarias enfocadas de {@link IniciarSesionService} respecto a la cancelación de
 * recordatorios al pasar una sesión {@code PENDIENTE} a {@code EN_CURSO} ({@code HU-22}, {@code HU-34}).
 */
public class IniciarSesionServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 16).valor();

    private FakeSesionRepository sesiones;
    private FakeProgramadorRecordatorios recordatorios;
    private IniciarSesionService iniciar;
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
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(9, 45).valor(), -240);
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

        materiaId = materias.guardar(Materia.crear("Física", 0xFF1565C0, null).valor()).id();
    }

    @Test
    public void iniciarSesionPendienteCancelaSuRecordatorioYNoCancelaSiHayOtraEnCurso() {
        SesionEstudio primera = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                HOY,
                HOY,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                -240,
                null,
                null,
                AntelacionRecordatorio.MINUTOS_15)
                .valor());
        SesionEstudio segunda = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                HOY,
                HOY,
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                -240,
                null,
                null,
                AntelacionRecordatorio.MINUTOS_30)
                .valor());

        recordatorios.programar(primera.id(), primera.instanteRecordatorioUtcMilis());
        recordatorios.programar(segunda.id(), segunda.instanteRecordatorioUtcMilis());

        assertTrue(iniciar.ejecutar(
                        primera.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .esValido());
        assertFalse(recordatorios.estaProgramada(primera.id()));
        assertTrue(recordatorios.cancelaciones().contains(primera.id()));
        assertTrue(recordatorios.estaProgramada(segunda.id()));

        assertEquals(
                ErrorValidacion.SESION_YA_EN_CURSO,
                iniciar.ejecutar(
                                segunda.id(),
                                TecnicaEstudio.LIBRE,
                                PreferenciaNoMolestar.HEREDAR_GLOBAL)
                        .error());
        assertTrue(recordatorios.estaProgramada(segunda.id()));
    }
}
