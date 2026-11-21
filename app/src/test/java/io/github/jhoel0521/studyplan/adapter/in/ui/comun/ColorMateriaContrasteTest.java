package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.jhoel0521.studyplan.adapter.in.ui.materias.ColorMateria;

/**
 * Pruebas de {@link ColorMateria}: cualquier color de materia, ajustado, alcanza ≥ 3:1 contra la superficie clara
 * y la oscura del tema; un color que ya cumple no se toca; y el relleno del chip se compone bien.
 *
 * <p>Verifica: HU-03 (escenario «Color de materia legible»), RNF-06.
 */
public class ColorMateriaContrasteTest {

    private static final int SUPERFICIE_CLARA = 0xFFFFFFFF;
    private static final int SUPERFICIE_OSCURA = 0xFF1A1D2D;

    @Test
    public void cadaColorDeMateriaLlegaATresAUnoEnClaroYOscuro() throws Exception {
        Map<String, Integer> materias = coloresDeMateria();
        assertTrue("No se leyeron colores materia_*", materias.size() >= 8);
        for (Map.Entry<String, Integer> e : materias.entrySet()) {
            for (int superficie : new int[] {SUPERFICIE_CLARA, SUPERFICIE_OSCURA}) {
                int ajustado = ColorMateria.paraSuperficie(e.getValue(), superficie);
                double ratio = ColorMateria.contraste(ajustado, superficie);
                assertTrue(String.format("%s sobre %08X da %.2f:1", e.getKey(), superficie, ratio),
                        ratio >= ColorMateria.CONTRASTE_MINIMO);
            }
        }
    }

    @Test
    public void naranjaQueNoCumpleSobreBlancoSeOscurece() {
        int naranja = 0xFFFB8C00;
        assertTrue(ColorMateria.contraste(naranja, SUPERFICIE_CLARA) < 3.0);
        int ajustado = ColorMateria.paraSuperficie(naranja, SUPERFICIE_CLARA);
        assertNotEquals(naranja, ajustado);
        assertTrue(ColorMateria.luminancia(ajustado) < ColorMateria.luminancia(naranja));
    }

    @Test
    public void colorQueYaCumpleNoCambia() {
        int azul = 0xFF1E88E5;
        assertTrue(ColorMateria.contraste(azul, SUPERFICIE_CLARA) >= 3.0);
        assertEquals(azul, ColorMateria.paraSuperficie(azul, SUPERFICIE_CLARA));
    }

    @Test
    public void colorOscuroSeAclaraSobreSuperficieOscura() {
        int morado = 0xFF8E24AA;
        int ajustado = ColorMateria.paraSuperficie(morado, SUPERFICIE_OSCURA);
        assertTrue(ColorMateria.luminancia(ajustado) > ColorMateria.luminancia(morado));
    }

    @Test
    public void conAlfaComponeElRellenoDelChip() {
        assertEquals(0xFFFFFFFF, ColorMateria.conAlfa(0xFF000000, 0f, SUPERFICIE_CLARA));
        assertEquals(0xFF000000, ColorMateria.conAlfa(0xFF000000, 1f, SUPERFICIE_CLARA));
        // 15 % de #FB8C00 sobre blanco: 255 - 0,15 × (255 - canal)
        assertEquals(0xFFFEEED9, ColorMateria.conAlfa(0xFFFB8C00, 0.15f, SUPERFICIE_CLARA));
    }

    @Test
    public void elResultadoSiempreEsOpaco() {
        int translucido = 0x40FB8C00;
        assertEquals(0xFF, ColorMateria.paraSuperficie(translucido, SUPERFICIE_CLARA) >>> 24);
    }

    @Test
    public void colorMateriaNoDependeDeAndroid() throws Exception {
        Path fuente = null;
        for (Path c : new Path[] {
                Paths.get("src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/materias/ColorMateria.java"),
                Paths.get("app/src/main/java/io/github/jhoel0521/studyplan/adapter/in/ui/materias/ColorMateria.java")}) {
            if (Files.isRegularFile(c)) {
                fuente = c;
            }
        }
        assertTrue("No se encontró ColorMateria.java", fuente != null);
        String codigo = new String(Files.readAllBytes(fuente), StandardCharsets.UTF_8);
        assertTrue("ColorMateria debe ser Java puro (se prueba en la JVM)",
                !Pattern.compile("^\\s*import\\s+(static\\s+)?(android|androidx)\\.", Pattern.MULTILINE)
                        .matcher(codigo).find());
    }

    private static Map<String, Integer> coloresDeMateria() throws Exception {
        Path archivo = null;
        for (Path c : new Path[] {Paths.get("src/main/res/values/colors.xml"),
                Paths.get("app/src/main/res/values/colors.xml")}) {
            if (Files.isRegularFile(c)) {
                archivo = c;
            }
        }
        assertTrue("No se encontró colors.xml", archivo != null);
        String xml = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<color name=\"(materia_\\w+)\">#([0-9A-Fa-f]{6})</color>").matcher(xml);
        Map<String, Integer> colores = new LinkedHashMap<>();
        while (m.find()) {
            colores.put(m.group(1), 0xFF000000 | Integer.parseInt(m.group(2), 16));
        }
        return colores;
    }
}
