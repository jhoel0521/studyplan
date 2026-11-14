package io.github.jhoel0521.studyplan.application.service;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de {@link EliminarSesionService} (HU-15, RN-12).
 */
public class EliminarSesionServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();
    private FakeSesionRepository sesiones;
    private EliminarSesionService servicio;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        servicio = new EliminarSesionService(sesiones);
    }

    @Test
    public void eliminaSoloPendienteYProtegeHistorialDeOtrosEstados() {
        SesionEstudio pendiente = sesiones.guardar(SesionEstudio.planificar(
                1L, HOY, HOY, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), null, null).valor());

        Resultado<SesionEstudio> ok = servicio.ejecutar(pendiente.id());
        assertTrue(ok.esValido());
        assertFalse(sesiones.buscarPorId(pendiente.id()).isPresent());

        for (CodigoEstadoSesion estadoProtegido : new CodigoEstadoSesion[] {
                CodigoEstadoSesion.COMPLETADA,
                CodigoEstadoSesion.COMPLETADA_TARDIA,
                CodigoEstadoSesion.NO_REALIZADA,
                CodigoEstadoSesion.REPROGRAMADA,
                CodigoEstadoSesion.EN_CURSO
        }) {
            SesionEstudio s = sesiones.guardar(SesionEstudio.restaurar(
                    null,
                    1L,
                    HOY,
                    Hora.de(12, 0).valor(),
                    Hora.de(13, 0).valor(),
                    60,
                    estadoProtegido == CodigoEstadoSesion.COMPLETADA
                                    || estadoProtegido == CodigoEstadoSesion.COMPLETADA_TARDIA
                            ? 60
                            : null,
                    estadoProtegido,
                    null,
                    null));
            assertEquals(
                    ErrorValidacion.SESION_NO_ELIMINABLE,
                    servicio.ejecutar(s.id()).error());
            assertTrue(sesiones.buscarPorId(s.id()).isPresent());
        }

        assertEquals(ErrorValidacion.SESION_NO_ENCONTRADA, servicio.ejecutar(888L).error());
    }
}
