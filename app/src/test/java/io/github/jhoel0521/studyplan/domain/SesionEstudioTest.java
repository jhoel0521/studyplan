package io.github.jhoel0521.studyplan.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias para la entidad de dominio {@link SesionEstudio} (HU-10 a HU-15,
 * RN-04, RN-05, RN-06, RN-07, RN-11, RN-12, RN-26).
 */
public class SesionEstudioTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 12).valor(); // Jueves
    private static final Hora DIEZ = Hora.de(10, 0).valor();
    private static final Hora ONCE = Hora.de(11, 0).valor();
    private static final Hora ONCE_MEDIA = Hora.de(11, 30).valor();
    private static final Hora DOCE = Hora.de(12, 0).valor();

    @Test
    public void planificaSesionValidaCalculandoDuracionYSaneandoNotas() {
        Resultado<SesionEstudio> res = SesionEstudio.planificar(
                1L, HOY, HOY, DIEZ, ONCE_MEDIA, "  Repasar listas enlazadas  ", null);

        assertTrue(res.esValido());
        SesionEstudio sesion = res.valor();
        assertNull(sesion.id());
        assertEquals(1L, sesion.materiaId());
        assertEquals(90, sesion.duracionPlanificadaMinutos());
        assertNull(sesion.duracionRealMinutos());
        assertEquals(CodigoEstadoSesion.PENDIENTE, sesion.codigoEstado());
        assertEquals("Repasar listas enlazadas", sesion.notas());
        assertEquals(0, sesion.minutosRealesContabilizables());
    }

    @Test
    public void planificarValidaMateriaFechaPasadaRn04YLongitudDeNotas() {
        assertEquals(
                ErrorValidacion.MATERIA_SESION_OBLIGATORIA,
                SesionEstudio.planificar(0L, HOY, HOY, DIEZ, ONCE, null, null).error());
        assertEquals(
                ErrorValidacion.FECHA_SESION_OBLIGATORIA,
                SesionEstudio.planificar(1L, null, HOY, DIEZ, ONCE, null, null).error());
        assertEquals(
                ErrorValidacion.FECHA_PLANIFICACION_PASADA,
                SesionEstudio.planificar(1L, HOY.sumarDias(-1), HOY, DIEZ, ONCE, null, null)
                        .error());
        assertEquals(
                ErrorValidacion.HORARIO_FIN_NO_POSTERIOR,
                SesionEstudio.planificar(1L, HOY, HOY, ONCE, DIEZ, null, null).error());
        assertEquals(
                ErrorValidacion.HORARIO_FIN_NO_POSTERIOR,
                SesionEstudio.planificar(1L, HOY, HOY, DIEZ, DIEZ, null, null).error());

        StringBuilder notasLargas = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            notasLargas.append('a');
        }
        assertEquals(
                ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS,
                SesionEstudio.planificar(
                                1L, HOY, HOY, DIEZ, ONCE, notasLargas.toString(), null)
                        .error());
    }

    @Test
    public void registrarPasadaAplicaVentanaUnoATresDiasRn06YDuracionRn26() {
        // Jueves - Lunes = 3 días exactos -> permitido (Escenario 9 de RN-07)
        Fecha lunes = HOY.sumarDias(-3);
        Resultado<SesionEstudio> res = SesionEstudio.registrarPasada(
                2L, lunes, HOY, DIEZ, ONCE, 55, "Estudio autónomo");

        assertTrue(res.esValido());
        SesionEstudio pasada = res.valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA_TARDIA, pasada.codigoEstado());
        assertEquals(60, pasada.duracionPlanificadaMinutos());
        assertEquals(Integer.valueOf(55), pasada.duracionRealMinutos());
        assertEquals(55, pasada.minutosRealesContabilizables());

        // Domingo (4 días atrás), hoy (0 días) o mañana (-1 días) son rechazados en pasada
        assertEquals(
                ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA,
                SesionEstudio.registrarPasada(2L, HOY.sumarDias(-4), HOY, DIEZ, ONCE, 55, null)
                        .error());
        assertEquals(
                ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA,
                SesionEstudio.registrarPasada(2L, HOY, HOY, DIEZ, ONCE, 55, null).error());
        assertEquals(
                ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA,
                SesionEstudio.registrarPasada(2L, HOY.sumarDias(1), HOY, DIEZ, ONCE, 55, null)
                        .error());
        assertEquals(
                ErrorValidacion.DURACION_REAL_INVALIDA,
                SesionEstudio.registrarPasada(2L, HOY.sumarDias(-1), HOY, DIEZ, ONCE, 0, null)
                        .error());
    }

    @Test
    public void detectaSolapamientosExcluyendoAdyacentesYReprogramadasSegunRn05() {
        SesionEstudio existente = SesionEstudio.planificar(
                1L, HOY, HOY, DIEZ, ONCE_MEDIA, null, null).valor().conId(10L);

        // 11:00 - 12:00 se cruza con 10:00 - 11:30
        assertTrue(existente.seSolapaCon(HOY, ONCE, DOCE));
        // 11:30 - 12:00 es adyacente: NO se cruza
        assertFalse(existente.seSolapaCon(HOY, ONCE_MEDIA, DOCE));
        // Mismo horario en otro día: NO se cruza
        assertFalse(existente.seSolapaCon(HOY.sumarDias(1), DIEZ, ONCE_MEDIA));

        // Si se reprograma, deja de ocupar horario
        SesionEstudio reprogramada = existente.marcarReprogramada().valor();
        assertEquals(CodigoEstadoSesion.REPROGRAMADA, reprogramada.codigoEstado());
        assertFalse(reprogramada.seSolapaCon(HOY, DIEZ, ONCE_MEDIA));
    }

    @Test
    public void transicionesDeEntidadConservanInmutabilidadYSoloPendienteSeElimina() {
        SesionEstudio pendiente = SesionEstudio.planificar(
                1L, HOY, HOY, DIEZ, ONCE, "Notas", null).valor().conId(5L);

        assertTrue(pendiente.validarEliminacion().esValido());

        SesionEstudio completada = pendiente.completar(HOY, 70).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, completada.codigoEstado());
        assertEquals(Integer.valueOf(70), completada.duracionRealMinutos());
        assertEquals(70, completada.minutosRealesContabilizables());
        assertEquals(CodigoEstadoSesion.PENDIENTE, pendiente.codigoEstado());
        assertEquals(
                ErrorValidacion.SESION_NO_ELIMINABLE,
                completada.validarEliminacion().error());

        SesionEstudio vencida = pendiente.vencer().valor();
        assertEquals(CodigoEstadoSesion.NO_REALIZADA, vencida.codigoEstado());
        assertEquals(
                ErrorValidacion.SESION_NO_ELIMINABLE,
                vencida.validarEliminacion().error());

        SesionEstudio tardia = vencida.registrarTardio(HOY.sumarDias(2), 45).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA_TARDIA, tardia.codigoEstado());
        assertEquals(45, tardia.minutosRealesContabilizables());
    }
}
