package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas del estado «bloqueada» de la sesión.
 *
 * <p>Verifica: HU-04 (pedir autenticación al volver a primer plano) y RNF-12 (la app nunca muestra datos sin
 * autenticar: la sesión nace bloqueada).
 */
public class SesionBloqueoTest {

    private SesionBloqueo sesion;

    @Before
    public void preparar() {
        sesion = new SesionBloqueo();
    }

    @Test
    public void naceBloqueadaHastaLeerLosAjustes() {
        assertTrue(sesion.estaBloqueada());
    }

    @Test
    public void conElBloqueoInactivoLaSesionQuedaAbiertaTrasLeerLosAjustes() {
        sesion.alCambiarBloqueo(false);
        assertFalse(sesion.estaBloqueada());
    }

    @Test
    public void conElBloqueoActivoLaSesionSigueBloqueadaTrasLeerLosAjustes() {
        sesion.alCambiarBloqueo(true);
        assertTrue(sesion.estaBloqueada());
    }

    @Test
    public void sinLeerLosAjustesSeAsumeElBloqueoActivo() {
        assertTrue(sesion.bloqueoActivo());
    }

    @Test
    public void elBloqueoActivoReflejaLoQueSeLeyo() {
        sesion.alCambiarBloqueo(false);
        assertFalse(sesion.bloqueoActivo());
        sesion.alCambiarBloqueo(true);
        assertTrue(sesion.bloqueoActivo());
    }

    @Test
    public void desbloquearLaAbre() {
        sesion.alCambiarBloqueo(true);
        sesion.desbloquear();
        assertFalse(sesion.estaBloqueada());
    }

    @Test
    public void volverAPrimerPlanoConBloqueoActivoLaBloquea() {
        sesion.alCambiarBloqueo(true);
        sesion.desbloquear();
        sesion.alVolverAPrimerPlano();
        assertTrue(sesion.estaBloqueada());
    }

    @Test
    public void volverAPrimerPlanoConBloqueoInactivoNoLaBloquea() {
        sesion.alCambiarBloqueo(false);
        sesion.alVolverAPrimerPlano();
        assertFalse(sesion.estaBloqueada());
    }

    @Test
    public void activarElBloqueoDentroDeLaAppNoBloqueaDeInmediato() {
        sesion.alCambiarBloqueo(false);
        sesion.alCambiarBloqueo(true);
        assertFalse(sesion.estaBloqueada());
    }

    @Test
    public void desactivarElBloqueoLaDesbloquea() {
        sesion.alCambiarBloqueo(true);
        sesion.alCambiarBloqueo(false);
        assertFalse(sesion.estaBloqueada());
    }
}
