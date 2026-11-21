package io.github.jhoel0521.studyplan.application.service.sesiones;

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
 * Pruebas unitarias de {@link CompletarSesionService} (HU-12, RN-07, RN-26).
 */
public class CompletarSesionServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();
    private FakeSesionRepository sesiones;
    private CompletarSesionService servicio;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(12, 0).valor());
        servicio = new CompletarSesionService(sesiones, reloj);
    }

    @Test
    public void completaPendienteDeHoyConDuracionEditableYRechazaOtrosCasos() {
        SesionEstudio pendienteHoy = sesiones.guardar(SesionEstudio.planificar(
                1L, HOY, HOY, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), null, null).valor());

        Resultado<SesionEstudio> ok = servicio.ejecutar(pendienteHoy.id(), 55);
        assertTrue(ok.esValido());
        assertEquals(CodigoEstadoSesion.COMPLETADA, ok.valor().codigoEstado());
        assertEquals(Integer.valueOf(55), ok.valor().duracionRealMinutos());

        SesionEstudio pendienteManiana = sesiones.guardar(SesionEstudio.planificar(
                1L,
                HOY.sumarDias(1),
                HOY,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                null).valor());
        assertEquals(
                ErrorValidacion.COMPLETAR_SOLO_MISMO_DIA,
                servicio.ejecutar(pendienteManiana.id(), 60).error());

        SesionEstudio reprogramada = sesiones.guardar(SesionEstudio.restaurar(
                null,
                1L,
                HOY,
                Hora.de(15, 0).valor(),
                Hora.de(16, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.REPROGRAMADA,
                null,
                null));
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                servicio.ejecutar(reprogramada.id(), 60).error());

        assertEquals(
                ErrorValidacion.SESION_NO_ENCONTRADA,
                servicio.ejecutar(999L, 60).error());
    }
}
