package io.github.jhoel0521.studyplan.application.service;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de {@link PlanificarSesionService} (HU-10, RN-04, RN-05).
 */
public class PlanificarSesionServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 12).valor();
    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private PlanificarSesionService servicio;
    private long algebraId;
    private long fisicaArchivadaId;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(9, 0).valor());
        servicio = new PlanificarSesionService(sesiones, materias, reloj);

        algebraId = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor()).id();
        Materia fisica = materias.guardar(Materia.crear("Física", 0xFF43A047, null).valor());
        fisicaArchivadaId = materias.guardar(fisica.conArchivada(true)).id();
    }

    @Test
    public void planificaSesionValidaDe90MinutosEnPendiente() {
        ResultadoSesion res = servicio.ejecutar(
                algebraId,
                HOY,
                Hora.de(10, 0).valor(),
                Hora.de(11, 30).valor(),
                "  Ejercicios de matrices  ");

        assertTrue(res.esValido());
        assertNotNull(res.sesion().id());
        assertEquals(90, res.sesion().duracionPlanificadaMinutos());
        assertEquals(CodigoEstadoSesion.PENDIENTE, res.sesion().codigoEstado());
        assertEquals("Ejercicios de matrices", res.sesion().notas());
    }

    @Test
    public void rechazaMateriaInexistenteArchivadaFechaPasadaYHorarioInvalido() {
        assertEquals(
                ErrorValidacion.MATERIA_SESION_OBLIGATORIA,
                servicio.ejecutar(
                                0L,
                                HOY,
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                null)
                        .error());
        assertEquals(
                ErrorValidacion.MATERIA_NO_ENCONTRADA,
                servicio.ejecutar(
                                999L,
                                HOY,
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                null)
                        .error());
        assertEquals(
                ErrorValidacion.MATERIA_INACTIVA,
                servicio.ejecutar(
                                fisicaArchivadaId,
                                HOY,
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                null)
                        .error());
        assertEquals(
                ErrorValidacion.FECHA_PLANIFICACION_PASADA,
                servicio.ejecutar(
                                algebraId,
                                HOY.sumarDias(-1),
                                Hora.de(10, 0).valor(),
                                Hora.de(11, 0).valor(),
                                null)
                        .error());
        assertEquals(
                ErrorValidacion.HORARIO_FIN_NO_POSTERIOR,
                servicio.ejecutar(
                                algebraId,
                                HOY,
                                Hora.de(10, 0).valor(),
                                Hora.de(10, 0).valor(),
                                null)
                        .error());
    }

    @Test
    public void planificaNocturnaYDetectaCruceEntreDiasRn04Rn05() {
        ResultadoSesion nocturna = servicio.ejecutar(
                algebraId,
                HOY,
                Hora.de(22, 0).valor(),
                Hora.de(1, 0).valor(),
                "Noche");
        assertTrue(nocturna.esValido());
        assertEquals(180, nocturna.sesion().duracionPlanificadaMinutos());

        ResultadoSesion cruceDiaSiguiente = servicio.ejecutar(
                algebraId,
                HOY.sumarDias(1),
                Hora.de(0, 30).valor(),
                Hora.de(2, 0).valor(),
                null);
        assertFalse(cruceDiaSiguiente.esValido());
        assertEquals(nocturna.sesion().id(), Long.valueOf(cruceDiaSiguiente.conflicto().sesionId()));
    }

    @Test
    public void detectaCruceDetalladoYPermiteSesionesAdyacentesEReprogramadas() {
        ResultadoSesion primera = servicio.ejecutar(
                algebraId, HOY, Hora.de(10, 0).valor(), Hora.de(11, 30).valor(), null);
        assertTrue(primera.esValido());

        // 11:00 - 12:00 cruza con 10:00 - 11:30
        ResultadoSesion cruce = servicio.ejecutar(
                algebraId, HOY, Hora.de(11, 0).valor(), Hora.de(12, 0).valor(), null);
        assertFalse(cruce.esValido());
        assertEquals(ErrorValidacion.SESION_SOLAPADA, cruce.error());
        assertNotNull(cruce.conflicto());
        assertEquals("Álgebra", cruce.conflicto().nombreMateria());
        assertEquals(Hora.de(10, 0).valor(), cruce.conflicto().horaInicio());
        assertEquals(Hora.de(11, 30).valor(), cruce.conflicto().horaFin());

        // 11:30 - 12:30 adyacente NO cruza
        ResultadoSesion adyacente = servicio.ejecutar(
                algebraId, HOY, Hora.de(11, 30).valor(), Hora.de(12, 30).valor(), null);
        assertTrue(adyacente.esValido());

        // Si la primera queda REPROGRAMADA, ya no ocupa ese bloque horario
        SesionEstudio reprogramada = primera.sesion().marcarReprogramada().valor();
        sesiones.actualizar(reprogramada);
        ResultadoSesion reemplazo = servicio.ejecutar(
                algebraId, HOY, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), null);
        assertTrue(reemplazo.esValido());
    }
}
