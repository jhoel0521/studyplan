package io.github.jhoel0521.studyplan.domain.enumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias de {@link TipoEventoEstudio} para los eventos de bloqueo de pantalla (HU-50, HU-40).
 */
public class TipoEventoEstudioTest {

    @Test
    public void bloqueoYDesbloqueoCuentanComoActividadDentroDeLaApp() {
        assertTrue(TipoEventoEstudio.BLOQUEO_PANTALLA.esMarcaDentroDeApp());
        assertTrue(TipoEventoEstudio.DESBLOQUEO.esMarcaDentroDeApp());
    }

    @Test
    public void laSalidaSigueSinSerMarcaDentroDeLaApp() {
        assertFalse(TipoEventoEstudio.SALIDA_APP.esMarcaDentroDeApp());
    }

    @Test
    public void seGuardanPorNombreSinNecesidadDeMigrarRoom() {
        assertEquals(TipoEventoEstudio.BLOQUEO_PANTALLA,
                TipoEventoEstudio.valueOf("BLOQUEO_PANTALLA"));
        assertEquals(TipoEventoEstudio.DESBLOQUEO, TipoEventoEstudio.valueOf("DESBLOQUEO"));
    }
}
