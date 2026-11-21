package io.github.jhoel0521.studyplan.domain.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/**
 * Pruebas unitarias de las transiciones en vivo de {@link SesionEstudio} y {@link EventoEstudio}
 * (HU-12, HU-22 a HU-24, HU-39, HU-41, HU-43, RN-07, RN-17, RN-18, RN-26).
 */
public class SesionEstudioEnCursoTest {

    @Test
    public void iniciarEnPomodoroGuardaSnapshotYResuelveNoMolestar() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        SesionEstudio pendiente = SesionEstudio.planificar(
                1L, hoy, hoy, Hora.de(9, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor()
                .conPreferenciaNoMolestar(PreferenciaNoMolestar.FORZAR_ACTIVAR);

        assertFalse(pendiente.estado().puedeCompletar(hoy, hoy));
        assertTrue(pendiente.debeActivarNoMolestar(false));

        ConfiguracionPomodoro snap = ConfiguracionPomodoro.crear(50, 10, 20, 3).valor();
        SesionEstudio enCurso = pendiente.iniciar(TecnicaEstudio.POMODORO, snap).valor();

        assertEquals(CodigoEstadoSesion.EN_CURSO, enCurso.codigoEstado());
        assertEquals(TecnicaEstudio.POMODORO, enCurso.tecnica());
        assertEquals(snap, enCurso.configuracionPomodoroSnapshot());

        SesionEstudio pausada = enCurso.pausarOReanudar().valor();
        assertEquals(CodigoEstadoSesion.EN_CURSO, pausada.codigoEstado());

        // Cierre con 0 minutos en el primer minuto (HU-22)
        SesionEstudio detenidaCero = pausada.detener(0).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, detenidaCero.codigoEstado());
        assertEquals(Integer.valueOf(0), detenidaCero.duracionRealMinutos());
    }

    @Test
    public void transicionesEnVivoCambiarVolverYVencerFuncionanYRechazanFinales() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        SesionEstudio base = SesionEstudio.planificar(
                1L, hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null).valor();

        SesionEstudio enCurso = base.iniciar(TecnicaEstudio.LIBRE, null).valor();
        assertNull(enCurso.configuracionPomodoroSnapshot());
        assertFalse(enCurso.debeActivarNoMolestar(false));
        assertTrue(enCurso.debeActivarNoMolestar(true));

        SesionEstudio cambiada = enCurso.cambiarASiguiente(45).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, cambiada.codigoEstado());
        assertEquals(Integer.valueOf(45), cambiada.duracionRealMinutos());

        // Estado final rechaza más transiciones
        assertEquals(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                cambiada.detener(50).error());

        SesionEstudio devuelta = enCurso.declararFinAlVolver(15).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, devuelta.codigoEstado());

        SesionEstudio vencida = enCurso.cerrarPorPlazoVencido(20).valor();
        assertEquals(CodigoEstadoSesion.COMPLETADA, vencida.codigoEstado());
    }

    @Test
    public void eventoEstudioValidaLongitudDeNota() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        Hora hora = Hora.de(10, 0).valor();
        assertTrue(EventoEstudio.crear(1L, TipoEventoEstudio.INICIO, 1000L, hoy, hora, "OK").esValido());

        String muyLarga = new String(new char[501]).replace('\0', 'a');
        assertEquals(ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS,
                EventoEstudio.crear(1L, TipoEventoEstudio.ESTUDIO_APP_CERRADA, 1000L, hoy, hora, muyLarga)
                        .error());
    }
}
