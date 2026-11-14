package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Objects;

import org.junit.Test;

/**
 * Pruebas de la conversión entre texto hexadecimal y color.
 *
 * <p>Verifica: HU-06 y HU-07 (el estudiante elige cualquier color, escrito en hexadecimal).
 */
public class ColorHexTest {

    private static int color(String texto) {
        return Objects.requireNonNull(ColorHex.parsear(texto));
    }

    @Test
    public void seisDigitosSeConviertenEnUnColorOpaco() {
        assertEquals(0xFFE66465, color("E66465"));
    }

    @Test
    public void elNumeralInicialYLasMayusculasOMinusculasSeAceptan() {
        assertEquals(0xFF1E88E5, color("#1e88e5"));
        assertEquals(0xFF1E88E5, color("  #1E88E5 "));
    }

    @Test
    public void tresDigitosSeAmplianDuplicandoCadaUno() {
        assertEquals(0xFFEE6655, color("E65"));
        assertEquals(0xFFFFFFFF, color("#fff"));
    }

    @Test
    public void untextoInvalidoDevuelveNulo() {
        assertNull(ColorHex.parsear(""));
        assertNull(ColorHex.parsear(null));
        assertNull(ColorHex.parsear("E6646"));
        assertNull(ColorHex.parsear("GGGGGG"));
        assertNull(ColorHex.parsear("1E88E5FF"));
    }

    @Test
    public void formatearDaSeisDigitosEnMayusculasSinElCanalAlfa() {
        assertEquals("E66465", ColorHex.formatear(0xFFE66465));
        assertEquals("000000", ColorHex.formatear(0xFF000000));
        assertEquals("00001F", ColorHex.formatear(0x1F));
    }

    @Test
    public void formatearYParsearSonInversos() {
        assertEquals(0xFF43A047, color(ColorHex.formatear(0xFF43A047)));
    }
}
