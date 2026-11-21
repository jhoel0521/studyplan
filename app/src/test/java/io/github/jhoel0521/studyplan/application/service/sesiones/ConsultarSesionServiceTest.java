package io.github.jhoel0521.studyplan.application.service.sesiones;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de {@link ConsultarSesionService} (HU-12 a HU-15, RN-07 a RN-10, RN-12).
 */
public class ConsultarSesionServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();
    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private ConsultarSesionService servicio;
    private long materiaId;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(10, 0).valor());
        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(sesiones, reloj);
        servicio = new ConsultarSesionService(sesiones, materias, reloj, vencer);
        materiaId = materias.guardar(Materia.crear("Redes", 0xFF00897B, null).valor()).id();
    }

    @Test
    public void vencePendienteAntiguaYExponePermisosCorrectos() {
        SesionEstudio pendienteHace5Dias = sesiones.guardar(SesionEstudio.restaurar(
                null,
                materiaId,
                HOY.sumarDias(-5),
                Hora.de(8, 0).valor(),
                Hora.de(9, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));

        DetalleSesion detalle = servicio.ejecutar(pendienteHace5Dias.id())
                .orElseThrow(AssertionError::new);
        assertEquals(CodigoEstadoSesion.NO_REALIZADA, detalle.sesion().codigoEstado());
        assertEquals("Redes", detalle.nombreMateria());
        assertFalse(detalle.puedeCompletar());
        assertFalse(detalle.puedeRegistrarTardio());
        assertTrue(detalle.noRealizadaFueraDePlazoTardio());
        assertTrue(detalle.puedeReprogramar());
        assertFalse(detalle.puedeEliminar());
    }

    @Test
    public void adjuntaEnlaceDeSesionDestinoCuandoEstaReprogramada() {
        SesionEstudio original = sesiones.guardar(SesionEstudio.restaurar(
                null,
                materiaId,
                HOY.sumarDias(-1),
                Hora.de(8, 0).valor(),
                Hora.de(9, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.REPROGRAMADA,
                null,
                null));
        SesionEstudio hija = sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                HOY.sumarDias(1),
                HOY,
                Hora.de(16, 0).valor(),
                Hora.de(17, 0).valor(),
                null,
                original.id()).valor());

        DetalleSesion detalle = servicio.ejecutar(original.id())
                .orElseThrow(AssertionError::new);
        assertNotNull(detalle.sesionDestino());
        assertEquals(hija.id(), detalle.sesionDestino().id());
        assertFalse(detalle.puedeCompletar());
        assertFalse(detalle.puedeReprogramar());
        assertFalse(detalle.puedeEliminar());
    }

    @Test
    public void exponePuedeIniciarEnCruceDeMedianocheSoloDentroDeVentanaRecordatorio() {
        FakeReloj relojLocal = new FakeReloj(HOY, Hora.de(23, 39).valor(), -240);
        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(sesiones, relojLocal);
        ConsultarSesionService consultar = new ConsultarSesionService(sesiones, materias, relojLocal, vencer);

        SesionEstudio madruga = sesiones.guardar(
                SesionEstudio.planificar(
                                materiaId,
                                HOY.sumarDias(1),
                                HOY,
                                Hora.de(0, 10).valor(),
                                Hora.de(1, 10).valor(),
                                -240,
                                null,
                                null,
                                AntelacionRecordatorio.MINUTOS_30)
                        .valor());

        DetalleSesion antesDeVentana = consultar.ejecutar(madruga.id())
                .orElseThrow(AssertionError::new);
        assertFalse(antesDeVentana.puedeIniciar());

        relojLocal.fijar(HOY, Hora.de(23, 40).valor());
        DetalleSesion enVentana = consultar.ejecutar(madruga.id())
                .orElseThrow(AssertionError::new);
        assertTrue(enVentana.puedeIniciar());
        assertEquals(AntelacionRecordatorio.MINUTOS_30, enVentana.sesion().antelacionRecordatorio());
    }
}
