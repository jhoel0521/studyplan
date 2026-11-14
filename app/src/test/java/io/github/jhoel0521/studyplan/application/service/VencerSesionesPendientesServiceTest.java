package io.github.jhoel0521.studyplan.application.service;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;

/**
 * Pruebas unitarias de {@link VencerSesionesPendientesService} verificando expiración automática e
 * idempotencia (HU-13, RN-08, RN-29).
 */
public class VencerSesionesPendientesServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();
    private FakeSesionRepository sesiones;
    private VencerSesionesPendientesService servicio;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(8, 0).valor());
        servicio = new VencerSesionesPendientesService(sesiones, reloj);
    }

    @Test
    public void venceSoloPendientesAnterioresAHoyYEsIdempotente() {
        Fecha ayer = HOY.sumarDias(-1);
        SesionEstudio pendienteAyer = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                ayer,
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));
        SesionEstudio enCursoAyer = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                ayer,
                Hora.de(11, 0).valor(),
                Hora.de(12, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.EN_CURSO,
                null,
                null));
        SesionEstudio pendienteHoy = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                HOY,
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));

        assertEquals(1, servicio.ejecutar());
        assertEquals(0, servicio.ejecutar());

        assertEquals(
                CodigoEstadoSesion.NO_REALIZADA,
                sesiones.buscarPorId(pendienteAyer.id()).orElseThrow(AssertionError::new)
                        .codigoEstado());
        assertEquals(
                CodigoEstadoSesion.EN_CURSO,
                sesiones.buscarPorId(enCursoAyer.id()).orElseThrow(AssertionError::new)
                        .codigoEstado());
        assertEquals(
                CodigoEstadoSesion.PENDIENTE,
                sesiones.buscarPorId(pendienteHoy.id()).orElseThrow(AssertionError::new)
                        .codigoEstado());
    }
}
