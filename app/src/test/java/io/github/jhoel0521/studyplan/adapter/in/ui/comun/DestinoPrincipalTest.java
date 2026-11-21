package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.calendario.CalendarioActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.MainActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.estadisticas.EstadisticasActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.materias.MateriasActivity;

/**
 * Pruebas de las pestañas de la barra de navegación.
 *
 * <p>Verifica: HU-09 (la pestaña Materias queda habilitada) y la decisión D3 de {@code add-gestion-materias}: solo se
 * habilitan las pestañas implementadas.
 */
public class DestinoPrincipalTest {

    @Test
    public void inicioCalendarioEstadisticasYMateriasEstanImplementados() {
        assertTrue(DestinoPrincipal.INICIO.implementado());
        assertTrue(DestinoPrincipal.CALENDARIO.implementado());
        assertTrue(DestinoPrincipal.ESTADISTICAS.implementado());
        assertTrue(DestinoPrincipal.MATERIAS.implementado());
    }

    @Test
    public void cadaDestinoSeEncuentraPorElIdDeSuElementoDeMenu() {
        for (DestinoPrincipal destino : DestinoPrincipal.values()) {
            assertEquals(destino, DestinoPrincipal.deIdMenu(destino.idMenu()));
        }
    }

    @Test
    public void unIdDesconocidoNoCorrespondeANingunDestino() {
        assertNull(DestinoPrincipal.deIdMenu(R.id.main));
    }

    @Test
    public void lasActividadesDeLosDestinosImplementadosSonLasEsperadas() {
        assertEquals(MainActivity.class, DestinoPrincipal.INICIO.actividad());
        assertEquals(CalendarioActivity.class, DestinoPrincipal.CALENDARIO.actividad());
        assertEquals(EstadisticasActivity.class, DestinoPrincipal.ESTADISTICAS.actividad());
        assertEquals(MateriasActivity.class, DestinoPrincipal.MATERIAS.actividad());
    }
}
