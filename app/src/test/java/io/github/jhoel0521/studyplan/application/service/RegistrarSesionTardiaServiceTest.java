package io.github.jhoel0521.studyplan.application.service;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de {@link RegistrarSesionTardiaService} (HU-13, RN-07, RN-10, RN-26).
 */
public class RegistrarSesionTardiaServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();
    private FakeSesionRepository sesiones;
    private RegistrarSesionTardiaService servicio;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(10, 0).valor());
        servicio = new RegistrarSesionTardiaService(sesiones, reloj);
    }

    @Test
    public void convierteNoRealizadaDentroDeTresDiasEnCompletadaTardiaYRechazaFueraDePlazo() {
        SesionEstudio hace2Dias = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                HOY.sumarDias(-2),
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.NO_REALIZADA,
                null,
                null));

        Resultado<SesionEstudio> res = servicio.ejecutar(hace2Dias.id(), 50);
        assertTrue(res.esValido());
        assertEquals(CodigoEstadoSesion.COMPLETADA_TARDIA, res.valor().codigoEstado());
        assertEquals(Integer.valueOf(50), res.valor().duracionRealMinutos());

        SesionEstudio hace4Dias = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                HOY.sumarDias(-4),
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.NO_REALIZADA,
                null,
                null));
        assertEquals(
                ErrorValidacion.REGISTRO_TARDIO_FUERA_DE_VENTANA,
                servicio.ejecutar(hace4Dias.id(), 40).error());

        SesionEstudio hace3DiasDuracionCero = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                HOY.sumarDias(-3),
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.NO_REALIZADA,
                null,
                null));
        assertEquals(
                ErrorValidacion.DURACION_REAL_INVALIDA,
                servicio.ejecutar(hace3DiasDuracionCero.id(), 0).error());
    }
}
