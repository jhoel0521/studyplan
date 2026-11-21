package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarBloqueo.ResultadoActivacion;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeDisponibilidadBloqueo;

/**
 * Pruebas de activar y desactivar el bloqueo.
 *
 * <p>Verifica: HU-04 (escenarios «Activar bloqueo» y «Sin método de bloqueo») y RNF-12.
 */
public class ConfigurarBloqueoServiceTest {

    private FakeAjustesRepository ajustes;
    private FakeDisponibilidadBloqueo disponibilidad;
    private SesionBloqueo sesion;
    private ConfigurarBloqueoService servicio;

    @Before
    public void preparar() {
        ajustes = new FakeAjustesRepository();
        disponibilidad = new FakeDisponibilidadBloqueo();
        sesion = new SesionBloqueo();
        sesion.alCambiarBloqueo(false); // la app ya arrancó y está abierta
        servicio = new ConfigurarBloqueoService(ajustes, disponibilidad, sesion);
    }

    @Test
    public void sinMetodoDeBloqueoNoSeActivaYSeIndicaElMotivo() {
        disponibilidad.disponible = false;
        assertEquals(ResultadoActivacion.SIN_METODO_DE_BLOQUEO, servicio.activar());
        assertFalse(ajustes.obtener().bloqueoActivo());
        assertEquals(0, ajustes.guardados);
    }

    @Test
    public void conMetodoDeBloqueoSeActivaYSeGuarda() {
        assertEquals(ResultadoActivacion.ACTIVADO, servicio.activar());
        assertTrue(ajustes.obtener().bloqueoActivo());
    }

    @Test
    public void activarDentroDeLaAppNoLaBloqueaPeroSiAlVolverAPrimerPlano() {
        servicio.activar();
        assertFalse(sesion.estaBloqueada());
        sesion.alVolverAPrimerPlano();
        assertTrue(sesion.estaBloqueada());
    }

    @Test
    public void desactivarLoGuardaYAbreLaSesion() {
        servicio.activar();
        sesion.alVolverAPrimerPlano();
        servicio.desactivar();
        assertFalse(ajustes.obtener().bloqueoActivo());
        assertFalse(sesion.estaBloqueada());
    }
}
