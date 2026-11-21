package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Comprueba que los colores del tema ({@code sp_*} en {@code values/colors.xml} y
 * {@code values-night/colors.xml}) coinciden con el estándar de diseño {@code .ux/standard/tokens.json} y que
 * todos sus pares de contraste ({@code ux-kit.contrast-pairs}) cumplen el mínimo en claro y en oscuro.
 *
 * <p>JVM pura: los XML se leen con {@link DocumentBuilderFactory} y el JSON con un lector mínimo propio, sin
 * Robolectric ni {@code org.json}.
 *
 * <p>Verifica: HU-03 (escenario «Línea gráfica en ambos temas»), RNF-06 (contraste ≥ 4,5:1), RNF-07.
 */
public class ContrasteTemaTest {

    private static Map<String, Object> tokens;
    private static Map<String, String> colorsClaro;
    private static Map<String, String> colorsOscuro;

    @BeforeClass
    public static void cargar() throws Exception {
        Path raiz = raizDelRepositorio();
        String json = new String(
                Files.readAllBytes(raiz.resolve(".ux/standard/tokens.json")), StandardCharsets.UTF_8);
        tokens = comoMapa(new LectorJson(json).leer());
        colorsClaro = colores(raiz.resolve("app/src/main/res/values/colors.xml"));
        colorsOscuro = colores(raiz.resolve("app/src/main/res/values-night/colors.xml"));
    }

    @Test
    public void cadaRolDelTemaExisteEnClaroYEnOscuro() {
        TreeSet<String> claro = rolesSp(colorsClaro);
        TreeSet<String> oscuro = rolesSp(colorsOscuro);
        assertTrue("No hay roles sp_* en values/colors.xml", !claro.isEmpty());
        assertEquals("values/colors.xml y values-night/colors.xml deben declarar los mismos sp_*", claro, oscuro);
        for (String nombre : colorsOscuro.keySet()) {
            assertTrue("values-night/colors.xml no debe duplicar materia_*: " + nombre,
                    !nombre.startsWith("materia_"));
        }
    }

    @Test
    public void cadaRolCoincideConSuToken() {
        comprobarRoles("light", colorsClaro);
        comprobarRoles("dark", colorsOscuro);
    }

    @Test
    public void todosLosParesDeContrasteCumplenSuMinimo() {
        Map<String, Object> extensiones = comoMapa(tokens.get("$extensions"));
        List<Object> pares = comoLista(extensiones.get("ux-kit.contrast-pairs"));
        assertTrue("tokens.json no declara pares de contraste", !pares.isEmpty());
        List<String> fallos = new ArrayList<>();
        for (Object o : pares) {
            Map<String, Object> par = comoMapa(o);
            String fg = (String) par.get("fg");
            String bg = (String) par.get("bg");
            double minimo = ((Number) par.get("min")).doubleValue();
            double ratio = contraste(colorDelTema(fg), colorDelTema(bg));
            if (ratio + 1e-9 < minimo) {
                fallos.add(String.format(Locale.ROOT, "%s / %s = %.2f:1 (mínimo %.1f)", fg, bg, ratio, minimo));
            }
        }
        if (!fallos.isEmpty()) {
            fail("Pares de contraste que no cumplen:\n" + String.join("\n", fallos));
        }
    }

    // --- Comprobaciones -------------------------------------------------------------------------------

    private static void comprobarRoles(String modo, Map<String, String> colores) {
        Map<String, Object> roles = comoMapa(ruta(tokens, "semantic", modo, "color"));
        for (String nombre : rolesSp(colores)) {
            String rol = nombre.substring("sp_".length()).replace('_', '-');
            Object token = roles.get(rol);
            assertNotNull("El recurso " + nombre + " no tiene token semantic." + modo + ".color." + rol, token);
            String esperado = resolver((String) comoMapa(token).get("$value"));
            assertEquals("Color de " + nombre + " (" + modo + ")",
                    esperado.toUpperCase(Locale.ROOT), colores.get(nombre).toUpperCase(Locale.ROOT));
        }
    }

    /** Toma el color que la app usa de verdad (el recurso sp_*), no el del JSON. */
    private static int colorDelTema(String rutaToken) {
        String[] partes = rutaToken.split("\\.");
        assertEquals("Ruta de token inesperada: " + rutaToken, 4, partes.length);
        Map<String, String> colores = "dark".equals(partes[1]) ? colorsOscuro : colorsClaro;
        String recurso = "sp_" + partes[3].replace('-', '_');
        String hex = colores.get(recurso);
        assertNotNull("Falta el recurso " + recurso + " para el par " + rutaToken, hex);
        return argb(hex);
    }

    private static String resolver(String valor) {
        if (!valor.startsWith("{")) {
            return valor;
        }
        String[] partes = valor.substring(1, valor.length() - 1).split("\\.");
        Object nodo = tokens;
        for (String parte : partes) {
            nodo = comoMapa(nodo).get(parte);
            assertNotNull("Referencia rota en tokens.json: " + valor, nodo);
        }
        return resolver((String) comoMapa(nodo).get("$value"));
    }

    // --- Contraste WCAG 2.x ---------------------------------------------------------------------------

    static double contraste(int a, int b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(int argb) {
        return 0.2126 * canal((argb >> 16) & 0xFF) + 0.7152 * canal((argb >> 8) & 0xFF) + 0.0722 * canal(argb & 0xFF);
    }

    private static double canal(int valor) {
        double c = valor / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static int argb(String hex) {
        String h = hex.substring(1);
        if (h.length() == 6) {
            h = "FF" + h;
        }
        assertEquals("Color no soportado: " + hex, 8, h.length());
        return (int) Long.parseLong(h, 16);
    }

    // --- Lectura de archivos --------------------------------------------------------------------------

    private static Path raizDelRepositorio() {
        for (Path candidata : new Path[] {Paths.get(".."), Paths.get(".")}) {
            if (Files.isRegularFile(candidata.resolve(".ux/standard/tokens.json"))) {
                return candidata;
            }
        }
        throw new AssertionError("No se encontró .ux/standard/tokens.json desde " + Paths.get("").toAbsolutePath());
    }

    private static Map<String, String> colores(Path archivo) throws Exception {
        assertTrue("No existe " + archivo, Files.isRegularFile(archivo));
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(archivo.toFile());
        NodeList nodos = doc.getElementsByTagName("color");
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            mapa.put(e.getAttribute("name"), e.getTextContent().trim());
        }
        return mapa;
    }

    private static TreeSet<String> rolesSp(Map<String, String> colores) {
        TreeSet<String> nombres = new TreeSet<>();
        for (String nombre : colores.keySet()) {
            if (nombre.startsWith("sp_")) {
                nombres.add(nombre);
            }
        }
        return nombres;
    }

    private static Object ruta(Map<String, Object> raiz, String... claves) {
        Object nodo = raiz;
        for (String clave : claves) {
            nodo = comoMapa(nodo).get(clave);
            assertNotNull("Falta " + String.join(".", claves) + " en tokens.json", nodo);
        }
        return nodo;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoMapa(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> comoLista(Object o) {
        return (List<Object>) o;
    }

    /** Lector JSON mínimo (objetos, listas, cadenas, números, booleanos y null). */
    static final class LectorJson {
        private final String texto;
        private int pos;

        LectorJson(String texto) {
            this.texto = texto;
        }

        Object leer() throws IOException {
            Object valor = valor();
            espacios();
            if (pos != texto.length()) {
                throw new IOException("Texto sobrante en la posición " + pos);
            }
            return valor;
        }

        private Object valor() throws IOException {
            espacios();
            char c = texto.charAt(pos);
            if (c == '{') {
                return objeto();
            }
            if (c == '[') {
                return lista();
            }
            if (c == '"') {
                return cadena();
            }
            if (texto.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (texto.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            if (texto.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return numero();
        }

        private Map<String, Object> objeto() throws IOException {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++;
            espacios();
            if (texto.charAt(pos) == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                espacios();
                String clave = cadena();
                espacios();
                esperar(':');
                mapa.put(clave, valor());
                espacios();
                if (texto.charAt(pos) == ',') {
                    pos++;
                } else {
                    esperar('}');
                    return mapa;
                }
            }
        }

        private List<Object> lista() throws IOException {
            List<Object> lista = new ArrayList<>();
            pos++;
            espacios();
            if (texto.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                lista.add(valor());
                espacios();
                if (texto.charAt(pos) == ',') {
                    pos++;
                } else {
                    esperar(']');
                    return lista;
                }
            }
        }

        private String cadena() throws IOException {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = texto.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = texto.charAt(pos++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Number numero() throws IOException {
            int inicio = pos;
            while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) {
                pos++;
            }
            if (inicio == pos) {
                throw new IOException("Valor JSON inesperado en la posición " + pos);
            }
            return Double.parseDouble(texto.substring(inicio, pos));
        }

        private void esperar(char c) throws IOException {
            if (texto.charAt(pos) != c) {
                throw new IOException("Se esperaba '" + c + "' en la posición " + pos);
            }
            pos++;
        }

        private void espacios() {
            while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
                pos++;
            }
        }
    }
}
