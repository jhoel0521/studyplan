package io.github.jhoel0521.studyplan.domain.state;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Pruebas unitarias exhaustivas de la máquina de estados GoF {@link EstadoSesion} verificando los
 * 13 escenarios de RN-07 y las consultas polimórficas de capacidades (HU-10 a HU-15).
 */
public class EstadoSesionTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();
    private static final Fecha AYER = HOY.sumarDias(-1);
    private static final Fecha MANIANA = HOY.sumarDias(1);

    @Test
    public void hidrataTodosLosEstadosDesdeCodigo() {
        for (CodigoEstadoSesion codigo : CodigoEstadoSesion.values()) {
            assertEquals(codigo, EstadoSesion.de(codigo).codigo());
        }
    }

    @Test
    public void pendienteCubreTransicionesYCapacidadesDeRn07() {
        EstadoSesion pendiente = EstadoPendiente.INSTANCIA;

        assertFalse(pendiente.esFinal());
        assertFalse(pendiente.sumaHorasReales());
        assertTrue(pendiente.ocupaHorario());
        assertFalse(pendiente.puedeCompletar(HOY, HOY));
        assertFalse(pendiente.puedeCompletar(AYER, HOY));
        assertFalse(pendiente.puedeCompletar(MANIANA, HOY));
        assertFalse(pendiente.puedeRegistrarTardio(1));
        assertTrue(pendiente.puedeReprogramar());
        assertTrue(pendiente.puedeEliminar());
        assertTrue(pendiente.validarEliminacion().esValido());

        // Escenario 1: Pendiente -> En curso (iniciar)
        assertEquals(CodigoEstadoSesion.EN_CURSO, pendiente.iniciar().valor().codigo());

        // Escenario 4: Pendiente -> Completada el mismo día (marcar completada)
        assertEquals(
                CodigoEstadoSesion.COMPLETADA,
                pendiente.marcarCompletada(HOY, HOY, 60).valor().codigo());
        assertEquals(
                ErrorValidacion.COMPLETAR_SOLO_MISMO_DIA,
                pendiente.marcarCompletada(MANIANA, HOY, 60).error());
        assertEquals(
                ErrorValidacion.DURACION_REAL_INVALIDA,
                pendiente.marcarCompletada(HOY, HOY, 0).error());

        // Escenario 5: Pendiente -> No realizada (fin del día)
        assertEquals(CodigoEstadoSesion.NO_REALIZADA, pendiente.vencer().valor().codigo());

        // Escenario 6: Pendiente -> Reprogramada
        assertEquals(CodigoEstadoSesion.REPROGRAMADA, pendiente.reprogramar().valor().codigo());

        // Transiciones prohibidas en Pendiente
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                pendiente.pausarReanudar().error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                pendiente.detener(30).error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                pendiente.registrarTardio(1, 30).error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                pendiente.cambiarASiguiente(30).error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                pendiente.declararFinAlVolver(30).error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                pendiente.cerrarPorPlazoVencido(30).error());
    }

    @Test
    public void enCursoCubreTransicionesDeEjecucionYProteccionRn29() {
        EstadoSesion enCurso = EstadoEnCurso.INSTANCIA;

        assertFalse(enCurso.esFinal());
        assertFalse(enCurso.sumaHorasReales());
        assertTrue(enCurso.ocupaHorario());
        assertFalse(enCurso.puedeEliminar());
        assertEquals(ErrorValidacion.SESION_NO_ELIMINABLE, enCurso.validarEliminacion().error());

        // Escenario 2: En curso -> En curso (pausar / reanudar)
        assertEquals(CodigoEstadoSesion.EN_CURSO, enCurso.pausarReanudar().valor().codigo());

        // Escenario 3: En curso -> Completada (detener, admite 0 min en el primer minuto HU-22)
        assertEquals(CodigoEstadoSesion.COMPLETADA, enCurso.detener(45).valor().codigo());
        assertEquals(CodigoEstadoSesion.COMPLETADA, enCurso.detener(0).valor().codigo());
        assertEquals(ErrorValidacion.DURACION_REAL_INVALIDA, enCurso.detener(-1).error());

        // Escenario 10: En curso -> Completada (cambiar a siguiente sesión, RN-27)
        assertEquals(
                CodigoEstadoSesion.COMPLETADA, enCurso.cambiarASiguiente(25).valor().codigo());

        // Escenario 11: En curso -> Completada (declarar fin al volver, RN-24)
        assertEquals(
                CodigoEstadoSesion.COMPLETADA, enCurso.declararFinAlVolver(40).valor().codigo());

        // Escenario 12: En curso -> Completada (plazo vencido, RN-29)
        assertEquals(
                CodigoEstadoSesion.COMPLETADA, enCurso.cerrarPorPlazoVencido(35).valor().codigo());

        // RN-29: Una sesión En curso no vence automáticamente a No realizada ni se reprograma
        assertEquals(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA, enCurso.vencer().error());
        assertEquals(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA, enCurso.reprogramar().error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                enCurso.marcarCompletada(HOY, HOY, 30).error());
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                enCurso.registrarTardio(1, 30).error());
    }

    @Test
    public void noRealizadaCubreVentanaDeTresDiasYReprogramacion() {
        EstadoSesion noRealizada = EstadoNoRealizada.INSTANCIA;

        assertFalse(noRealizada.esFinal());
        assertFalse(noRealizada.sumaHorasReales());
        assertTrue(noRealizada.ocupaHorario());
        assertFalse(noRealizada.puedeCompletar(HOY, HOY));
        assertTrue(noRealizada.puedeRegistrarTardio(0));
        assertTrue(noRealizada.puedeRegistrarTardio(3));
        assertFalse(noRealizada.puedeRegistrarTardio(4));
        assertFalse(noRealizada.puedeRegistrarTardio(-1));
        assertTrue(noRealizada.puedeReprogramar());
        assertFalse(noRealizada.puedeEliminar());
        assertEquals(
                ErrorValidacion.SESION_NO_ELIMINABLE, noRealizada.validarEliminacion().error());

        // Escenario 8: No realizada -> Completada tardía (<= 3 días, RN-10)
        assertEquals(
                CodigoEstadoSesion.COMPLETADA_TARDIA,
                noRealizada.registrarTardio(3, 50).valor().codigo());
        assertEquals(
                ErrorValidacion.REGISTRO_TARDIO_FUERA_DE_VENTANA,
                noRealizada.registrarTardio(4, 50).error());
        assertEquals(
                ErrorValidacion.REGISTRO_TARDIO_FUERA_DE_VENTANA,
                noRealizada.registrarTardio(-1, 50).error());
        assertEquals(
                ErrorValidacion.DURACION_REAL_INVALIDA,
                noRealizada.registrarTardio(2, 0).error());

        // Escenario 7: No realizada -> Reprogramada (RN-09)
        assertEquals(CodigoEstadoSesion.REPROGRAMADA, noRealizada.reprogramar().valor().codigo());

        // Otras transiciones prohibidas
        assertEquals(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA, noRealizada.iniciar().error());
        assertEquals(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA, noRealizada.vencer().error());
    }

    @Test
    public void estadosFinalesRechazanTodaTransicionYExponenSusCapacidades() {
        List<EstadoSesion> finales = Arrays.asList(
                EstadoCompletada.INSTANCIA,
                EstadoCompletadaTardia.INSTANCIA,
                EstadoReprogramada.INSTANCIA);

        for (EstadoSesion estadoFinal : finales) {
            assertTrue(estadoFinal.esFinal());
            assertFalse(estadoFinal.puedeCompletar(HOY, HOY));
            assertFalse(estadoFinal.puedeRegistrarTardio(1));
            assertFalse(estadoFinal.puedeReprogramar());
            assertFalse(estadoFinal.puedeEliminar());
            assertEquals(
                    ErrorValidacion.SESION_NO_ELIMINABLE,
                    estadoFinal.validarEliminacion().error());

            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA, estadoFinal.iniciar().error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.pausarReanudar().error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.detener(30).error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.marcarCompletada(HOY, HOY, 30).error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA, estadoFinal.vencer().error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.reprogramar().error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.registrarTardio(1, 30).error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.cambiarASiguiente(30).error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.declararFinAlVolver(30).error());
            assertEquals(
                    ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                    estadoFinal.cerrarPorPlazoVencido(30).error());
        }

        assertTrue(EstadoCompletada.INSTANCIA.sumaHorasReales());
        assertTrue(EstadoCompletada.INSTANCIA.ocupaHorario());

        assertTrue(EstadoCompletadaTardia.INSTANCIA.sumaHorasReales());
        assertTrue(EstadoCompletadaTardia.INSTANCIA.ocupaHorario());

        assertFalse(EstadoReprogramada.INSTANCIA.sumaHorasReales());
        assertFalse(EstadoReprogramada.INSTANCIA.ocupaHorario());
    }
}
