package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Reglas de uso de la línea gráfica sobre los recursos, sin emulador:
 * <ul>
 *   <li>ningún layout de {@code res/layout/} ni {@code res/layout-w600dp/} escribe un color hex fuera de los
 *       atributos {@code tools:} de vista previa;</li>
 *   <li>ningún botón fija su alto en dp (la altura la da el estilo, para que crezca con la fuente);</li>
 *   <li>ningún layout usa un estilo {@code Widget.Material3.Button*} o {@code Widget.Material3.CardView*}
 *       explícito, que saltaría el tema;</li>
 *   <li>todos los estilos {@code Widget.StudyPlan.Button*} heredan su alto de {@code @dimen/button_height};</li>
 *   <li>la única excepción a la altura común es el botón redondo Pausar/Reanudar de la sesión en curso (64 dp):
 *       ningún otro botón flotante declara un tamaño especial.</li>
 * </ul>
 *
 * <p>Verifica: HU-03 (escenarios «Botones uniformes» y «Texto ampliado»), RNF-07.
 */
public class ReglasEstiloLayoutTest {

    private static final Pattern HEX = Pattern.compile("^#([0-9A-Fa-f]{3,4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$");
    private static final Pattern DP_FIJO = Pattern.compile("^\\d+(\\.\\d+)?dp$");
    private static final String TOOLS = "http://schemas.android.com/tools";
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String[] BOTONES = {
            "com.google.android.material.button.MaterialButton",
            "com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton",
            "Button"
    };

    @Test
    public void ningunLayoutEscribeColoresHexFueraDeTools() throws Exception {
        List<String> fallos = new ArrayList<>();
        for (Path layout : layouts()) {
            recorrer(documento(layout).getDocumentElement(), e -> {
                NamedNodeMap atributos = e.getAttributes();
                for (int i = 0; i < atributos.getLength(); i++) {
                    Node a = atributos.item(i);
                    if (!TOOLS.equals(a.getNamespaceURI()) && HEX.matcher(a.getNodeValue().trim()).matches()) {
                        fallos.add(nombre(layout) + ": " + e.getTagName() + " " + a.getNodeName() + "="
                                + a.getNodeValue());
                    }
                }
            });
        }
        comprobar("Colores hex en layouts (usa atributos del tema)", fallos);
    }

    /**
     * HU-03 «Controles de la sesión en curso»: el único botón de tamaño especial (64 dp) es Pausar/Reanudar,
     * en la sesión en curso (teléfono y ancho). Cualquier otro botón flotante usa el tamaño estándar.
     */
    @Test
    public void soloPausarReanudarDeLaSesionUsaUnBotonRedondoDe64dp() throws Exception {
        List<String> fallos = new ArrayList<>();
        int deLaSesion = 0;
        for (Path layout : layouts()) {
            List<Element> botones = new ArrayList<>();
            recorrer(documento(layout).getDocumentElement(), e -> {
                if ("com.google.android.material.floatingactionbutton.FloatingActionButton"
                        .equals(e.getTagName())) {
                    botones.add(e);
                }
            });
            for (Element e : botones) {
                String id = e.getAttributeNS(ANDROID, "id");
                String tamano = e.getAttributeNS("http://schemas.android.com/apk/res-auto", "fabCustomSize");
                boolean esElDeLaSesion = nombre(layout).endsWith("activity_sesion_en_curso.xml")
                        && "@+id/botonPausarReanudar".equals(id);
                if (esElDeLaSesion) {
                    deLaSesion++;
                    if (!"64dp".equals(tamano)) {
                        fallos.add(nombre(layout) + ": " + id + " debe medir 64dp y tiene " + tamano);
                    }
                } else if (!tamano.isEmpty()) {
                    fallos.add(nombre(layout) + ": " + id + " usa fabCustomSize=" + tamano);
                }
            }
        }
        assertEquals("Debe haber un botón redondo en cada layout de la sesión (teléfono y ancho)", 2, deLaSesion);
        comprobar("Botones flotantes de tamaño especial fuera de la excepción de la sesión en curso", fallos);
    }

    @Test
    public void ningunBotonFijaSuAltoEnDp() throws Exception {
        List<String> fallos = new ArrayList<>();
        for (Path layout : layouts()) {
            recorrer(documento(layout).getDocumentElement(), e -> {
                if (esBoton(e.getTagName())) {
                    String alto = e.getAttributeNS(ANDROID, "layout_height");
                    if (DP_FIJO.matcher(alto).matches() && !"0dp".equals(alto)) {
                        fallos.add(nombre(layout) + ": " + idDe(e) + " layout_height=" + alto);
                    }
                }
            });
        }
        comprobar("Botones con alto fijo (usa el estilo, que define minHeight)", fallos);
    }

    @Test
    public void ningunLayoutUsaEstilosMaterialExplicitosDeBotonOTarjeta() throws Exception {
        List<String> fallos = new ArrayList<>();
        for (Path layout : layouts()) {
            recorrer(documento(layout).getDocumentElement(), e -> {
                String estilo = e.getAttribute("style");
                if (estilo.startsWith("@style/Widget.Material3.Button")
                        || estilo.startsWith("@style/Widget.Material3.CardView")) {
                    fallos.add(nombre(layout) + ": " + idDe(e) + " style=" + estilo);
                }
            });
        }
        comprobar("Estilos Material explícitos (usa Widget.StudyPlan.*)", fallos);
    }

    @Test
    public void todosLosEstilosDeBotonUsanLaAlturaUnica() throws Exception {
        Map<String, Element> estilos = new HashMap<>();
        NodeList nodos = documento(res().resolve("values/styles.xml")).getElementsByTagName("style");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            estilos.put(e.getAttribute("name"), e);
        }
        List<String> botones = estilos.keySet().stream()
                .filter(n -> n.startsWith("Widget.StudyPlan.Button") || n.equals("Widget.StudyPlan.BotonGrupo"))
                .sorted()
                .collect(Collectors.toList());
        assertTrue("styles.xml no declara Widget.StudyPlan.Button", botones.contains("Widget.StudyPlan.Button"));
        for (String nombre : botones) {
            assertEquals("minHeight de " + nombre, "@dimen/button_height",
                    itemHeredado(estilos, nombre, "android:minHeight"));
            if (nombre.contains(".Icon")) {
                assertEquals("minWidth de " + nombre, "@dimen/button_height",
                        itemHeredado(estilos, nombre, "android:minWidth"));
            }
        }
    }

    /** Busca un item en el estilo o en sus padres declarados en styles.xml (por {@code parent} o por punto). */
    private static String itemHeredado(Map<String, Element> estilos, String nombre, String item) {
        String actual = nombre;
        while (actual != null && estilos.containsKey(actual)) {
            Element estilo = estilos.get(actual);
            NodeList items = estilo.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element e = (Element) items.item(i);
                if (item.equals(e.getAttribute("name"))) {
                    return e.getTextContent().trim();
                }
            }
            String padre = estilo.getAttribute("parent");
            if (padre.isEmpty() && actual.contains(".")) {
                padre = actual.substring(0, actual.lastIndexOf('.'));
            }
            actual = padre.isEmpty() ? null : padre;
        }
        return null;
    }

    // --- Utilidades -----------------------------------------------------------------------------------

    private interface Visita {
        void en(Element e);
    }

    private static void recorrer(Element e, Visita visita) {
        visita.en(e);
        NodeList hijos = e.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            if (hijos.item(i) instanceof Element) {
                recorrer((Element) hijos.item(i), visita);
            }
        }
    }

    private static boolean esBoton(String etiqueta) {
        for (String b : BOTONES) {
            if (b.equals(etiqueta)) {
                return true;
            }
        }
        return false;
    }

    private static String idDe(Element e) {
        String id = e.getAttributeNS(ANDROID, "id");
        return id.isEmpty() ? e.getTagName() : id;
    }

    private static void comprobar(String titulo, List<String> fallos) {
        if (!fallos.isEmpty()) {
            fail(titulo + ":\n" + String.join("\n", fallos));
        }
    }

    private static Path res() {
        for (Path candidata : new Path[] {Paths.get("src/main/res"), Paths.get("app/src/main/res")}) {
            if (Files.isDirectory(candidata)) {
                return candidata;
            }
        }
        throw new AssertionError("No se encontró src/main/res desde " + Paths.get("").toAbsolutePath());
    }

    private static List<Path> layouts() throws IOException {
        List<Path> archivos = new ArrayList<>();
        for (String carpeta : new String[] {"layout", "layout-w600dp"}) {
            try (Stream<Path> s = Files.list(res().resolve(carpeta))) {
                archivos.addAll(s.filter(p -> p.toString().endsWith(".xml")).sorted().collect(Collectors.toList()));
            }
        }
        assertTrue("No hay layouts", !archivos.isEmpty());
        return archivos;
    }

    private static String nombre(Path layout) {
        return layout.getParent().getFileName() + "/" + layout.getFileName();
    }

    private static Document documento(Path archivo) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        return fabrica.newDocumentBuilder().parse(archivo.toFile());
    }
}
