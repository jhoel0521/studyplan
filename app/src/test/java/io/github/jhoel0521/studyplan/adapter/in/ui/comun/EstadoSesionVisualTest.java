package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;

/**
 * Mapeo de cada {@link CodigoEstadoSesion} a su rol de color de la línea gráfica (design.md, decisión 5a): ningún
 * estado usa el color terciario y todos tienen rol.
 *
 * <p>Verifica: HU-03 (escenario «Estado sin depender del color»), RNF-06.
 */
public class EstadoSesionVisualTest {

    @Test
    public void cadaEstadoTieneElRolDeLaLineaGrafica() {
        assertEquals(RolColorEstado.PRIMARIO, EstadoSesionVisual.rolDe(CodigoEstadoSesion.PENDIENTE));
        assertEquals(RolColorEstado.PRIMARIO, EstadoSesionVisual.rolDe(CodigoEstadoSesion.EN_CURSO));
        assertEquals(RolColorEstado.ERROR, EstadoSesionVisual.rolDe(CodigoEstadoSesion.NO_REALIZADA));
        assertEquals(RolColorEstado.SECUNDARIO, EstadoSesionVisual.rolDe(CodigoEstadoSesion.COMPLETADA));
        assertEquals(RolColorEstado.SECUNDARIO, EstadoSesionVisual.rolDe(CodigoEstadoSesion.COMPLETADA_TARDIA));
        assertEquals(RolColorEstado.VARIANTE, EstadoSesionVisual.rolDe(CodigoEstadoSesion.REPROGRAMADA));
    }

    @Test
    public void ningunEstadoQuedaSinRol() {
        for (CodigoEstadoSesion estado : CodigoEstadoSesion.values()) {
            assertNotNull("Sin rol: " + estado, EstadoSesionVisual.rolDe(estado));
        }
    }
}
