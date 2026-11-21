package io.github.jhoel0521.studyplan.adapter.in.ui.sesiones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.sesiones.CompletarSesionService;
import io.github.jhoel0521.studyplan.application.service.sesiones.ConsultarSesionService;
import io.github.jhoel0521.studyplan.application.service.sesiones.EliminarSesionService;
import io.github.jhoel0521.studyplan.application.service.sesiones.RegistrarSesionTardiaService;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas del ViewModel de detalle de sesión: carga, completar, registro tardío y eliminación con confirmación.
 *
 * <p>Verifica: HU-12, HU-13, HU-14 y HU-15.
 */
public class SesionDetalleViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeMateriaRepository materias;
    private FakeSesionRepository sesiones;
    private FakeReloj reloj;
    private SesionDetalleViewModel modelo;
    private long materiaId;

    @Before
    public void preparar() {
        materias = new FakeMateriaRepository();
        sesiones = new FakeSesionRepository();
        reloj = new FakeReloj(Fecha.de(2026, 11, 14).valor(), Hora.de(12, 0).valor());
        modelo = new SesionDetalleViewModel(
                new ConsultarSesionService(
                        sesiones,
                        materias,
                        reloj,
                        new VencerSesionesPendientesService(sesiones, reloj)),
                new CompletarSesionService(sesiones, reloj),
                new RegistrarSesionTardiaService(sesiones, reloj),
                new EliminarSesionService(sesiones),
                Runnable::run);
        materiaId = materias.guardar(Materia.crear("Física", 0xFF43A047, null).valor()).id();
    }

    @Test
    public void completarMismoDiaActualizaDetalleAEstadoCompletada() {
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                reloj.hoy(),
                reloj.hoy(),
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                null).valor());

        modelo.cargar(s.id());
        DetalleSesion inicial = Objects.requireNonNull(modelo.detalle().getValue());
        assertFalse(inicial.puedeCompletar());
        assertTrue(inicial.puedeIniciar());

        modelo.completar(s.id(), "55");
        assertEquals(
                EventoSesion.Tipo.COMPLETADA,
                Objects.requireNonNull(modelo.evento().getValue()).tipo());
        DetalleSesion actualizado = Objects.requireNonNull(modelo.detalle().getValue());
        assertEquals(CodigoEstadoSesion.COMPLETADA, actualizado.sesion().codigoEstado());
        assertEquals(Integer.valueOf(55), actualizado.sesion().duracionRealMinutos());
    }

    @Test
    public void completarConDuracionInvalidaPublicaError() {
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                reloj.hoy(),
                reloj.hoy(),
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                null).valor());

        modelo.completar(s.id(), "0");
        EventoSesion ev = Objects.requireNonNull(modelo.evento().getValue());
        assertEquals(EventoSesion.Tipo.ERROR, ev.tipo());
        assertEquals(ErrorValidacion.DURACION_REAL_INVALIDA, ev.error());
    }

    @Test
    public void registrarTardioEnNoRealizadaDentroDePlazoActualizaDetalle() {
        Fecha ayer = Fecha.de(2026, 11, 13).valor();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                ayer,
                ayer,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                null).valor());
        sesiones.actualizar(s.vencer().valor());

        modelo.cargar(s.id());
        assertTrue(Objects.requireNonNull(modelo.detalle().getValue()).puedeRegistrarTardio());

        modelo.registrarTardio(s.id(), "45");
        assertEquals(
                EventoSesion.Tipo.TARDIA_REGISTRADA,
                Objects.requireNonNull(modelo.evento().getValue()).tipo());
        assertEquals(
                CodigoEstadoSesion.COMPLETADA_TARDIA,
                Objects.requireNonNull(modelo.detalle().getValue()).sesion().codigoEstado());
    }

    @Test
    public void eliminarConConfirmacionBorraLaSesionYPublicaEliminada() {
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                Fecha.de(2026, 11, 16).valor(),
                reloj.hoy(),
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                null).valor());

        modelo.pedirEliminar();
        assertTrue(Boolean.TRUE.equals(modelo.confirmandoEliminar().getValue()));
        modelo.confirmarEliminar(s.id());

        assertFalse(Boolean.TRUE.equals(modelo.confirmandoEliminar().getValue()));
        assertEquals(
                EventoSesion.Tipo.ELIMINADA,
                Objects.requireNonNull(modelo.evento().getValue()).tipo());
        assertFalse(sesiones.buscarPorId(s.id()).isPresent());
    }
}
