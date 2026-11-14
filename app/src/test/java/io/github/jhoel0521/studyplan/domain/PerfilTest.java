package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas de la validación del perfil.
 *
 * <p>Verifica: HU-01 (escenarios «Primer uso» y «Nombre vacío»), RN-14 (meta mayor que 0 y hasta 168 h)
 * y RNF-14 (validación de entradas).
 */
public class PerfilTest {

    @Test
    public void nombreVacioEsRechazado() {
        Resultado<Perfil> r = Perfil.crear("", 20);
        assertFalse(r.esValido());
        assertEquals(ErrorValidacion.NOMBRE_VACIO, r.error());
    }

    @Test
    public void nombreSoloConEspaciosEsRechazado() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Perfil.crear("   \t ", 20).error());
    }

    @Test
    public void nombreNuloEsRechazado() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Perfil.crear(null, 20).error());
    }

    @Test
    public void metaCeroEsRechazada() {
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO, Perfil.crear("Ana", 0).error());
    }

    @Test
    public void metaNegativaEsRechazada() {
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO, Perfil.crear("Ana", -1).error());
    }

    @Test
    public void meta168EsValida() {
        Resultado<Perfil> r = Perfil.crear("Ana", 168);
        assertTrue(r.esValido());
        assertEquals(168, r.valor().metaSemanalHoras(), 0);
    }

    @Test
    public void meta168ConUnDecimalMasEsRechazada() {
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO, Perfil.crear("Ana", 168.1).error());
    }

    @Test
    public void metaNoNumericaEsRechazada() {
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO, Perfil.crear("Ana", Double.NaN).error());
    }

    @Test
    public void metaConDecimalesEsValida() {
        assertTrue(Perfil.crear("Ana", 7.5).esValido());
    }

    @Test
    public void datosValidosCreanElPerfilConElNombreRecortado() {
        Perfil perfil = Perfil.crear("  Ana  ", 20).valor();
        assertEquals("Ana", perfil.nombre());
        assertEquals(20, perfil.metaSemanalHoras(), 0);
    }

    @Test
    public void conDosErroresSeInformaPrimeroElDelNombre() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Perfil.crear("", 0).error());
    }

    @Test(expected = IllegalStateException.class)
    public void pedirElValorDeUnErrorLanzaExcepcion() {
        Perfil.crear("", 20).valor();
    }
}
