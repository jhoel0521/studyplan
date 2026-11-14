package io.github.jhoel0521.studyplan.application.service;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
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
}
