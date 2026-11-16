package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias de {@link ConfiguracionPomodoro}, {@link PreferenciaNoMolestar},
 * {@link TipoEventoEstudio} y {@link Ajustes} (HU-23, HU-24, RN-17, RN-18, RN-29).
 */
public class AjustesPomodoroTest {

    @Test
    public void configuracionPomodoroValidaRangosYValoresPorDefecto() {
        ConfiguracionPomodoro defecto = ConfiguracionPomodoro.porDefecto();
        assertEquals(25, defecto.trabajoMinutos());
        assertEquals(5, defecto.descansoCortoMinutos());
        assertEquals(15, defecto.descansoLargoMinutos());
        assertEquals(4, defecto.ciclosHastaLargo());

        Resultado<ConfiguracionPomodoro> valida = ConfiguracionPomodoro.crear(50, 10, 20, 3);
        assertTrue(valida.esValido());
        assertEquals(50, valida.valor().trabajoMinutos());

        assertEquals(ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA,
                ConfiguracionPomodoro.crear(0, 5, 15, 4).error());
        assertEquals(ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA,
                ConfiguracionPomodoro.crear(181, 5, 15, 4).error());
        assertEquals(ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA,
                ConfiguracionPomodoro.crear(25, 61, 15, 4).error());
        assertEquals(ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA,
                ConfiguracionPomodoro.crear(25, 5, 91, 4).error());
        assertEquals(ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA,
                ConfiguracionPomodoro.crear(25, 5, 15, 13).error());
    }

    @Test
    public void preferenciaNoMolestarConvierteTriEstadoYTipoEventoClasificaDentroDeApp() {
        assertNull(PreferenciaNoMolestar.HEREDAR_GLOBAL.aBoolean());
        assertEquals(Boolean.TRUE, PreferenciaNoMolestar.FORZAR_ACTIVAR.aBoolean());
        assertEquals(Boolean.FALSE, PreferenciaNoMolestar.FORZAR_DESACTIVAR.aBoolean());
        assertEquals(PreferenciaNoMolestar.HEREDAR_GLOBAL, PreferenciaNoMolestar.desdeBoolean(null));
        assertEquals(PreferenciaNoMolestar.FORZAR_ACTIVAR, PreferenciaNoMolestar.desdeBoolean(true));
        assertEquals(PreferenciaNoMolestar.FORZAR_DESACTIVAR, PreferenciaNoMolestar.desdeBoolean(false));

        assertTrue(TipoEventoEstudio.INICIO.esMarcaDentroDeApp());
        assertTrue(TipoEventoEstudio.PAUSA.esMarcaDentroDeApp());
        assertFalse(TipoEventoEstudio.SALIDA_APP.esMarcaDentroDeApp());
        assertFalse(TipoEventoEstudio.ESTUDIO_APP_CERRADA.esMarcaDentroDeApp());
    }

    @Test
    public void ajustesConservaValoresPorDefectoYActualizaInmutablemente() {
        Ajustes defecto = Ajustes.porDefecto();
        assertFalse(defecto.noMolestarGlobal());
        assertEquals(ConfiguracionPomodoro.porDefecto(), defecto.pomodoro());

        ConfiguracionPomodoro custom = ConfiguracionPomodoro.crear(40, 10, 20, 2).valor();
        Ajustes actualizado = defecto.conNoMolestarGlobal(true).conPomodoro(custom);
        assertTrue(actualizado.noMolestarGlobal());
        assertEquals(custom, actualizado.pomodoro());
        assertFalse(defecto.noMolestarGlobal());
    }
}
