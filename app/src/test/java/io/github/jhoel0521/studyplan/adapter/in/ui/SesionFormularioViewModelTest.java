package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.recordatorios.ConfigurarRecordatorioPorDefectoService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConsultarAjustesService;
import io.github.jhoel0521.studyplan.application.service.sesiones.ConsultarSesionService;
import io.github.jhoel0521.studyplan.application.service.materias.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.sesiones.PlanificarSesionService;
import io.github.jhoel0521.studyplan.application.service.sesiones.RegistrarSesionPasadaService;
import io.github.jhoel0521.studyplan.application.service.sesiones.ReprogramarSesionService;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertFalse;

/**
 * Pruebas del ViewModel del formulario de sesiones en modos Planificar, Pasada y Reprogramar.
 *
 * <p>Verifica: HU-10, HU-11 y HU-14.
 */
public class SesionFormularioViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeMateriaRepository materias;
    private FakeSesionRepository sesiones;
    private FakeAjustesRepository ajustes;
    private FakeProgramadorRecordatorios recordatorios;
    private FakeReloj reloj;
    private SesionFormularioViewModel modelo;
    private long materiaId;

    @Before
    public void preparar() {
        materias = new FakeMateriaRepository();
        sesiones = new FakeSesionRepository();
        ajustes = new FakeAjustesRepository();
        recordatorios = new FakeProgramadorRecordatorios();
        reloj = new FakeReloj(Fecha.de(2026, 11, 14).valor(), Hora.de(10, 0).valor(), -240);
        modelo = new SesionFormularioViewModel(
                new PlanificarSesionService(sesiones, materias, reloj, recordatorios),
                new RegistrarSesionPasadaService(sesiones, materias, reloj),
                new ReprogramarSesionService(sesiones, materias, reloj, recordatorios),
                new ListarMateriasService(materias, sesiones),
                new ConsultarSesionService(
                        sesiones,
                        materias,
                        reloj,
                        new VencerSesionesPendientesService(sesiones, reloj)),
                new ConsultarAjustesService(ajustes),
                reloj,
                Runnable::run);
        materiaId = materias.guardar(Materia.crear("Cálculo", 0xFF1E88E5, null).valor()).id();
    }

    @Test
    public void cargarPublicaMateriasActivasYFechaHoy() {
        modelo.cargar(-1L);
        assertEquals(Fecha.de(2026, 11, 14).valor(), modelo.fechaHoy().getValue());
        assertEquals(
                AntelacionRecordatorio.MINUTOS_15,
                modelo.recordatorioPorDefecto().getValue());
        assertEquals(1, Objects.requireNonNull(modelo.materiasActivas().getValue()).size());
    }

    @Test
    public void guardarPlanificadaSinMateriaPublicaError() {
        modelo.guardarPlanificada(null, "2026-11-15", "09:00", "10:00", "");
        EventoSesion evento = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.ERROR, evento.tipo());
        assertEquals(ErrorValidacion.MATERIA_SESION_OBLIGATORIA, evento.error());
    }

    @Test
    public void guardarPlanificadaValidaPublicaPlanificadaYSolapamientoDevuelveConflicto() {
        modelo.guardarPlanificada(materiaId, "2026-11-15", "09:00", "10:30", "Repaso");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.PLANIFICADA, ok.tipo());
        assertNotNull(ok.sesionId());

        modelo.guardarPlanificada(materiaId, "2026-11-15", "10:00", "11:00", "");
        EventoSesion choque = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.ERROR, choque.tipo());
        assertEquals(ErrorValidacion.SESION_SOLAPADA, choque.error());
        assertNotNull(choque.conflicto());
        assertEquals("Cálculo", choque.conflicto().nombreMateria());
    }

    @Test
    public void guardarPasadaValidaPublicaPasadaRegistrada() {
        modelo.guardarPasada(materiaId, "2026-11-12", "14:00", "15:30", "80", "Ejercicios");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.PASADA_REGISTRADA, ok.tipo());
        SesionEstudio guardada =
                sesiones.buscarPorId(Objects.requireNonNull(ok.sesionId())).orElseThrow();
        assertEquals(Integer.valueOf(80), guardada.duracionRealMinutos());
    }

    @Test
    public void reprogramarCargaBaseYCreaDestinoEnlazada() {
        SesionEstudio original = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                reloj.hoy(),
                reloj.hoy(),
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                null,
                null).valor());

        modelo.cargar(original.id());
        assertTrue(modelo.sesionBase().getValue() != null);

        modelo.reprogramar(original.id(), "2026-11-16", "09:00", "10:00", "Cambio de horario");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.REPROGRAMADA, ok.tipo());
        assertNotNull(ok.sesionId());
    }

    @Test
    public void aceptaNocturnaYRechazaFinIgualAInicio() {
        modelo.guardarPlanificada(materiaId, "2026-11-15", "22:00", "01:00", "Noche");
        EventoSesion ok = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.PLANIFICADA, ok.tipo());
        SesionEstudio guardada =
                sesiones.buscarPorId(Objects.requireNonNull(ok.sesionId())).orElseThrow();
        assertEquals(180, guardada.duracionPlanificadaMinutos());

        modelo.guardarPlanificada(materiaId, "2026-11-15", "10:00", "10:00", "");
        EventoSesion err = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.ERROR, err.tipo());
        assertEquals(ErrorValidacion.HORARIO_FIN_NO_POSTERIOR, err.error());
    }

    @Test
    public void preseleccionaAntelacionDesdeAjustesGuardaReprogramaYDetectaVentanaVencida() {
        new ConfigurarRecordatorioPorDefectoService(ajustes).ejecutar(AntelacionRecordatorio.MINUTOS_30);
        modelo.cargar(-1L);
        assertEquals(
                AntelacionRecordatorio.MINUTOS_30,
                modelo.recordatorioPorDefecto().getValue());

        // reloj es 2026-11-14 10:00: una sesión hoy 10:10 con 15 min antes cae en ventana vencida
        assertTrue(modelo.esVentanaRecordatorioVencida(
                "2026-11-14", "10:10", AntelacionRecordatorio.MINUTOS_15));
        assertFalse(modelo.esVentanaRecordatorioVencida(
                "2026-11-14", "10:10", AntelacionRecordatorio.SIN_RECORDATORIO));
        assertFalse(modelo.esVentanaRecordatorioVencida(
                "2026-11-14", "10:30", AntelacionRecordatorio.MINUTOS_15));

        modelo.guardarPlanificada(
                materiaId,
                "2026-11-15",
                "09:00",
                "10:00",
                "Con aviso",
                AntelacionRecordatorio.MINUTOS_30);
        Long idOriginal = Objects.requireNonNull(modelo.evento().getValue()).sesionId();
        SesionEstudio guardada = sesiones.buscarPorId(Objects.requireNonNull(idOriginal)).orElseThrow();
        assertEquals(AntelacionRecordatorio.MINUTOS_30, guardada.antelacionRecordatorio());
        assertTrue(recordatorios.estaProgramada(idOriginal));

        modelo.reprogramar(
                idOriginal,
                "2026-11-16",
                "11:00",
                "12:00",
                null,
                AntelacionRecordatorio.MINUTOS_5);
        Long idHija = Objects.requireNonNull(modelo.evento().getValue()).sesionId();
        assertEquals(
                AntelacionRecordatorio.MINUTOS_5,
                sesiones.buscarPorId(Objects.requireNonNull(idHija))
                        .orElseThrow()
                        .antelacionRecordatorio());
        assertFalse(recordatorios.estaProgramada(idOriginal));
        assertTrue(recordatorios.estaProgramada(idHija));
    }
}
