package io.github.jhoel0521.studyplan.application.service.sesiones;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de {@link RegistrarSesionPasadaService} (HU-11, RN-04, RN-05, RN-06, RN-26).
 */
public class RegistrarSesionPasadaServiceTest {

    // Jueves 2026-11-12 -> lunes es -3 días (permitido), domingo es -4 días (rechazado)
    private static final Fecha JUEVES = Fecha.de(2026, 11, 12).valor();
    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private RegistrarSesionPasadaService servicio;
    private long materiaId;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        FakeReloj reloj = new FakeReloj(JUEVES, Hora.de(15, 0).valor());
        servicio = new RegistrarSesionPasadaService(sesiones, materias, reloj);
        materiaId = materias.guardar(Materia.crear("Cálculo", 0xFFE53935, null).valor()).id();
    }

    @Test
    public void permiteLunesDesdeJuevesGuardandoCompletadaTardiaYDuracionReal() {
        Fecha lunes = Fecha.de(2026, 11, 9).valor();
        ResultadoSesion res = servicio.ejecutar(
                materiaId,
                lunes,
                Hora.de(18, 0).valor(),
                Hora.de(19, 30).valor(),
                85,
                "Derivadas");

        assertTrue(res.esValido());
        assertEquals(CodigoEstadoSesion.COMPLETADA_TARDIA, res.sesion().codigoEstado());
        assertEquals(Integer.valueOf(85), res.sesion().duracionRealMinutos());
        assertEquals(85, sesiones.sumarMinutosRealesPorMateria(materiaId));
    }

    @Test
    public void rechazaDomingoDesdeJuevesHoyYDuracionCeroOCruce() {
        Fecha domingo = Fecha.de(2026, 11, 8).valor();
        assertEquals(
                ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA,
                servicio.ejecutar(
                                materiaId,
                                domingo,
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                60,
                                null)
                        .error());
        assertEquals(
                ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA,
                servicio.ejecutar(
                                materiaId,
                                JUEVES,
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                60,
                                null)
                        .error());
        assertEquals(
                ErrorValidacion.DURACION_REAL_INVALIDA,
                servicio.ejecutar(
                                materiaId,
                                JUEVES.sumarDias(-1),
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                0,
                                null)
                        .error());

        Fecha ayer = JUEVES.sumarDias(-1);
        assertTrue(servicio.ejecutar(
                        materiaId,
                        ayer,
                        Hora.de(10, 0).valor(),
                        Hora.de(11, 0).valor(),
                        60,
                        null)
                .esValido());

        ResultadoSesion cruce = servicio.ejecutar(
                materiaId, ayer, Hora.de(10, 30).valor(), Hora.de(11, 30).valor(), 60, null);
        assertFalse(cruce.esValido());
        assertEquals(ErrorValidacion.SESION_SOLAPADA, cruce.error());
    }
}
