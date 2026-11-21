package io.github.jhoel0521.studyplan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Vigila la estructura de carpetas del código: cada clase vive en la carpeta que dice qué es (RNF-15, mantenibilidad).
 *
 * <p>Regla de {@code domain}: ninguna clase suelta en la raíz y solo las siete carpetas por tipo: {@code entity},
 * {@code valueobject}, {@code enumeration}, {@code state}, {@code service}, {@code result} y {@code readmodel}. Los
 * enums solo están en {@code enumeration}, {@code state} o {@code result}, y {@code EstadoSesion} con sus
 * implementaciones en {@code state}.
 *
 * <p>Regla de {@code application}: nada suelto en {@code port.in}, {@code port.out} ni {@code service}; las carpetas de
 * {@code port.in} y {@code service} son una épica (o {@code comun}); las de {@code port.out} son {@code persistence} o
 * {@code system}; y cada {@code *Service} está en la misma épica que su puerto de entrada.
 *
 * <p>Las reglas se escriben como métodos estáticos que devuelven los incumplimientos, para poder probar que fallan
 * ante una clase mal puesta (ver las pruebas con {@link TemporaryFolder}).
 */
public class EstructuraPaquetesTest {

    private static final String BASE = "io/github/jhoel0521/studyplan";
    static final Set<String> CARPETAS_DOMINIO = new HashSet<>(Arrays.asList(
            "entity", "valueobject", "enumeration", "state", "service", "result", "readmodel"));
    static final Set<String> EPICAS = new HashSet<>(Arrays.asList(
            "configuracion", "materias", "sesiones", "calendario", "tecnicas", "metas", "estadisticas", "reportes",
            "recordatorios", "comun"));
    /** Lo que el manifiesto nombra y el sistema recuerda por su nombre de clase no se mueve de la raíz. */
    static final Set<String> RAIZ_DE_LA_INTERFAZ = new HashSet<>(Arrays.asList(
            "StudyPlanApplication", "FabricaViewModels", "ReceptorAvisosSesion", "ServicioSesionEnCurso"));
    static final Set<String> RAIZ_DE_LA_PERSISTENCIA = new HashSet<>(Arrays.asList("StudyPlanDatabase", "Migraciones"));
    static final Set<String> CARPETAS_DE_LA_PERSISTENCIA = new HashSet<>(Arrays.asList("entity", "dao", "repository"));
    static final Set<String> CARPETAS_PUERTOS_DE_SALIDA =new HashSet<>(Arrays.asList("persistence", "system"));
    private static final Set<String> CARPETAS_CON_ENUMS =new HashSet<>(Arrays.asList(
            "enumeration", "state", "result"));
    private static final Pattern DECLARA_ENUM = Pattern.compile("^public\\s+enum\\s+\\w+", Pattern.MULTILINE);
    private static final Pattern IMPLEMENTA_ESTADO = Pattern.compile("\\bimplements\\s+EstadoSesion\\b");

    @Rule
    public TemporaryFolder temporal = new TemporaryFolder();

    private static Path raizDeFuentes() {
        Path[] candidatas = {Paths.get("src/main/java", BASE), Paths.get("app/src/main/java", BASE)};
        for (Path candidata : candidatas) {
            if (Files.isDirectory(candidata)) {
                return candidata;
            }
        }
        throw new AssertionError("No se encontró la carpeta de fuentes; la prueba no puede comprobar nada");
    }

    // ------------------------------------------------------------------------------------------ reglas

    /** Archivos .java que están directamente en {@code carpeta}. */
    static List<String> clasesSueltas(Path carpeta) throws IOException {
        try (Stream<Path> hijos = Files.list(carpeta)) {
            return hijos.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    /** Subcarpetas de {@code carpeta} que no están entre las permitidas. */
    static List<String> carpetasNoPermitidas(Path carpeta, Set<String> permitidas) throws IOException {
        try (Stream<Path> hijos = Files.list(carpeta)) {
            return hijos.filter(Files::isDirectory).map(p -> p.getFileName().toString())
                    .filter(n -> !permitidas.contains(n)).sorted().collect(Collectors.toList());
        }
    }

    /** Enums declarados fuera de las carpetas que los admiten (ruta relativa a {@code dominio}). */
    static List<String> enumsMalUbicados(Path dominio) throws IOException {
        try (Stream<Path> recorrido = Files.walk(dominio)) {
            return recorrido.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !CARPETAS_CON_ENUMS.contains(carpetaDe(dominio, p)) && declaraEnum(p))
                    .map(p -> dominio.relativize(p).toString().replace('\\', '/')).sorted()
                    .collect(Collectors.toList());
        }
    }

    /** Clases de {@code state} que no son enums ni estados (por ejemplo, una clase de otro tipo mal puesta). */
    static List<String> implementacionesFueraDeState(Path dominio) throws IOException {
        try (Stream<Path> recorrido = Files.walk(dominio)) {
            return recorrido.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !"state".equals(carpetaDe(dominio, p)) && implementaEstado(p))
                    .map(p -> dominio.relativize(p).toString().replace('\\', '/')).sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Servicios cuyo puerto de entrada (mismo nombre sin el sufijo {@code Service}) está en otra épica. Los servicios
     * sin un puerto con su nombre (ayudantes) no se comprueban.
     */
    static List<String> serviciosFueraDeLaEpicaDeSuPuerto(Path puertosDeEntrada, Path servicios) throws IOException {
        Map<String, String> epicaDelPuerto = new HashMap<>();
        try (Stream<Path> puertos = Files.walk(puertosDeEntrada)) {
            puertos.filter(p -> p.toString().endsWith(".java") && puertosDeEntrada.relativize(p).getNameCount() > 1)
                    .forEach(p -> epicaDelPuerto.put(nombreSinExtension(p), carpetaDe(puertosDeEntrada, p)));
        }
        List<String> fuera = new ArrayList<>();
        try (Stream<Path> recorrido = Files.walk(servicios)) {
            recorrido.filter(p -> p.toString().endsWith("Service.java")).forEach(p -> {
                String nombre = nombreSinExtension(p);
                String puerto = nombre.substring(0, nombre.length() - "Service".length());
                String epicaPuerto = epicaDelPuerto.get(puerto);
                if (epicaPuerto != null && !epicaPuerto.equals(carpetaDe(servicios, p))) {
                    fuera.add(nombre + " (" + carpetaDe(servicios, p) + ") con su puerto en " + epicaPuerto);
                }
            });
        }
        Collections.sort(fuera);
        return fuera;
    }

    /** Clases en la raíz de {@code carpeta} que no están entre las permitidas (nombres sin extensión). */
    static List<String> clasesSueltasNoPermitidas(Path carpeta, Set<String> permitidas) throws IOException {
        List<String> sobran = new ArrayList<>();
        for (String archivo : clasesSueltas(carpeta)) {
            String nombre = archivo.substring(0, archivo.length() - ".java".length());
            if (!permitidas.contains(nombre)) {
                sobran.add(nombre);
            }
        }
        return sobran;
    }

    /**
     * Layouts cuyo {@code tools:context} apunta a una clase que no existe. El valor es el nombre relativo con punto
     * ({@code .adapter.in.ui.materias.MateriasActivity}) y se resuelve contra las carpetas de fuentes.
     */
    static List<String> contextosRotos(Path recursos, Path fuentes) throws IOException {
        Pattern contexto = Pattern.compile("tools:context=\"\\.([\\w.]+)\"");
        List<String> rotos = new ArrayList<>();
        try (Stream<Path> recorrido = Files.walk(recursos)) {
            for (Path layout : recorrido.filter(p -> p.toString().endsWith(".xml")
                    && p.getParent().getFileName().toString().startsWith("layout")).collect(Collectors.toList())) {
                java.util.regex.Matcher m = contexto.matcher(leer(layout));
                while (m.find()) {
                    Path clase = fuentes.resolve(m.group(1).replace('.', '/') + ".java");
                    if (!Files.isRegularFile(clase)) {
                        rotos.add(layout.getParent().getFileName() + "/" + layout.getFileName() + " -> ." + m.group(1));
                    }
                }
            }
        }
        Collections.sort(rotos);
        return rotos;
    }

    private static Path raizDeRecursos() {
        Path[] candidatas = {Paths.get("src/main/res"), Paths.get("app/src/main/res")};
        for (Path candidata : candidatas) {
            if (Files.isDirectory(candidata)) {
                return candidata;
            }
        }
        throw new AssertionError("No se encontró la carpeta de recursos; la prueba no puede comprobar nada");
    }

    private static String nombreSinExtension(Path archivo) {
        String nombre = archivo.getFileName().toString();
        return nombre.substring(0, nombre.length() - ".java".length());
    }

    private static String carpetaDe(Path dominio, Path archivo) {
        Path relativa = dominio.relativize(archivo);
        return relativa.getNameCount() > 1 ? relativa.getName(0).toString() : "";
    }

    private static boolean declaraEnum(Path archivo) {
        return DECLARA_ENUM.matcher(leer(archivo)).find();
    }

    private static boolean implementaEstado(Path archivo) {
        return IMPLEMENTA_ESTADO.matcher(leer(archivo)).find();
    }

    private static String leer(Path archivo) {
        try {
            return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("No se pudo leer " + archivo, e);
        }
    }

    // ------------------------------------------------------------------------------- reglas sobre el código

    @Test
    public void elDominioNoTieneClasesSueltasEnLaRaiz() throws IOException {
        assertEquals("Clases sueltas en domain", Arrays.asList(), clasesSueltas(raizDeFuentes().resolve("domain")));
    }

    @Test
    public void elDominioSoloTieneLasSieteCarpetasPorTipo() throws IOException {
        Path dominio = raizDeFuentes().resolve("domain");
        assertEquals("Carpetas no permitidas en domain", Arrays.asList(), carpetasNoPermitidas(dominio, CARPETAS_DOMINIO));
        for (String carpeta : CARPETAS_DOMINIO) {
            assertTrue("Falta la carpeta domain/" + carpeta, Files.isDirectory(dominio.resolve(carpeta)));
        }
    }

    @Test
    public void losEnumsDelDominioSoloEstanEnEnumerationStateOResult() throws IOException {
        assertEquals("Enums fuera de enumeration, state o result", Arrays.asList(),
                enumsMalUbicados(raizDeFuentes().resolve("domain")));
    }

    @Test
    public void laMaquinaDeEstadosDeLaSesionEstaEnState() throws IOException {
        Path dominio = raizDeFuentes().resolve("domain");
        assertTrue("EstadoSesion debe estar en domain/state", Files.isRegularFile(dominio.resolve("state/EstadoSesion.java")));
        assertEquals("Implementaciones de EstadoSesion fuera de state", Arrays.asList(),
                implementacionesFueraDeState(dominio));
    }

    @Test
    public void laAplicacionNoTieneClasesSueltasNiCarpetasQueNoSeanUnaEpica() throws IOException {
        Path aplicacion = raizDeFuentes().resolve("application");
        Path puertosDeEntrada = aplicacion.resolve("port/in");
        Path servicios = aplicacion.resolve("service");
        Path puertosDeSalida = aplicacion.resolve("port/out");
        assertEquals("Clases sueltas en port.in", Arrays.asList(), clasesSueltas(puertosDeEntrada));
        assertEquals("Clases sueltas en port.out", Arrays.asList(), clasesSueltas(puertosDeSalida));
        assertEquals("Clases sueltas en service", Arrays.asList(), clasesSueltas(servicios));
        assertEquals("Carpetas no permitidas en port.in", Arrays.asList(),
                carpetasNoPermitidas(puertosDeEntrada, EPICAS));
        assertEquals("Carpetas no permitidas en service", Arrays.asList(), carpetasNoPermitidas(servicios, EPICAS));
        assertEquals("Carpetas no permitidas en port.out", Arrays.asList(),
                carpetasNoPermitidas(puertosDeSalida, CARPETAS_PUERTOS_DE_SALIDA));
    }

    @Test
    public void cadaServicioEstaEnLaMismaEpicaQueSuPuertoDeEntrada() throws IOException {
        Path aplicacion = raizDeFuentes().resolve("application");
        assertEquals("Servicios fuera de la épica de su puerto", Arrays.asList(),
                serviciosFueraDeLaEpicaDeSuPuerto(aplicacion.resolve("port/in"), aplicacion.resolve("service")));
    }

    @Test
    public void laInterfazDeUsuarioEstaPorEpicaYSoloLaRaizTieneLoQueNombraElManifiesto() throws IOException {
        Path ui = raizDeFuentes().resolve("adapter/in/ui");
        assertEquals("Clases en la raíz de adapter.in.ui que no son las permitidas", Arrays.asList(),
                clasesSueltasNoPermitidas(ui, RAIZ_DE_LA_INTERFAZ));
        assertEquals("Carpetas no permitidas en adapter.in.ui", Arrays.asList(), carpetasNoPermitidas(ui, EPICAS));
    }

    @Test
    public void laPersistenciaEstaPorTipoYSoloLaRaizTieneLaBaseDeDatos() throws IOException {
        Path persistencia = raizDeFuentes().resolve("adapter/out/persistence");
        assertEquals("Clases en la raíz de adapter.out.persistence que no son las permitidas", Arrays.asList(),
                clasesSueltasNoPermitidas(persistencia, RAIZ_DE_LA_PERSISTENCIA));
        assertEquals("Carpetas no permitidas en adapter.out.persistence", Arrays.asList(),
                carpetasNoPermitidas(persistencia, CARPETAS_DE_LA_PERSISTENCIA));
    }

    @Test
    public void cadaToolsContextDeLosLayoutsApuntaAUnaClaseQueExiste() throws IOException {
        assertEquals("Layouts con tools:context a una clase inexistente", Arrays.asList(),
                contextosRotos(raizDeRecursos(), raizDeFuentes()));
    }

    // ------------------------------------------- las reglas fallan cuando la estructura se rompe

    @Test
    public void laReglaDetectaUnToolsContextQueApuntaAUnaClaseInexistente() throws IOException {
        Path recursos = temporal.newFolder("res").toPath();
        Path layout = Files.createDirectories(recursos.resolve("layout"));
        Path fuentes = temporal.newFolder("java").toPath();
        Files.createDirectories(fuentes.resolve("adapter/in/ui/materias"));
        Files.write(fuentes.resolve("adapter/in/ui/materias/Existe.java"), "class Existe {}"
                .getBytes(StandardCharsets.UTF_8));
        Files.write(layout.resolve("bien.xml"), "<x tools:context=\".adapter.in.ui.materias.Existe\"/>"
                .getBytes(StandardCharsets.UTF_8));
        Files.write(layout.resolve("mal.xml"), "<x tools:context=\".adapter.in.ui.NoExiste\"/>"
                .getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("layout/mal.xml -> .adapter.in.ui.NoExiste"), contextosRotos(recursos, fuentes));
    }

    @Test
    public void laReglaDetectaUnaClaseSueltaQueNoEstaPermitidaEnLaRaiz() throws IOException {
        Path carpeta = temporal.newFolder("ui").toPath();
        Files.write(carpeta.resolve("StudyPlanApplication.java"), "class A {}".getBytes(StandardCharsets.UTF_8));
        Files.write(carpeta.resolve("Rara.java"), "class Rara {}".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("Rara"), clasesSueltasNoPermitidas(carpeta, RAIZ_DE_LA_INTERFAZ));
    }

    @Test
    public void laReglaDetectaUnServicioEnOtraEpicaQueSuPuerto() throws IOException {
        Path puertos = temporal.newFolder("port").toPath();
        Path servicios = temporal.newFolder("service").toPath();
        Files.createDirectories(puertos.resolve("materias"));
        Files.createDirectories(servicios.resolve("sesiones"));
        Files.createDirectories(servicios.resolve("materias"));
        Files.write(puertos.resolve("materias/RegistrarMateria.java"), "public interface RegistrarMateria {}"
                .getBytes(StandardCharsets.UTF_8));
        Files.write(servicios.resolve("sesiones/RegistrarMateriaService.java"),
                "public class RegistrarMateriaService {}".getBytes(StandardCharsets.UTF_8));
        Files.write(servicios.resolve("materias/AyudanteService.java"), "public class AyudanteService {}"
                .getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("RegistrarMateriaService (sesiones) con su puerto en materias"),
                serviciosFueraDeLaEpicaDeSuPuerto(puertos, servicios));
    }

    @Test
    public void laReglaDetectaUnaClaseSueltaEnLaRaiz() throws IOException {
        Path dominio = temporal.newFolder("domain").toPath();
        Files.createDirectory(dominio.resolve("entity"));
        Files.write(dominio.resolve("Suelta.java"), "public class Suelta {}".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("Suelta.java"), clasesSueltas(dominio));
    }

    @Test
    public void laReglaDetectaUnaCarpetaQueNoEsUnTipo() throws IOException {
        Path dominio = temporal.newFolder("domain").toPath();
        Files.createDirectory(dominio.resolve("entity"));
        Files.createDirectory(dominio.resolve("varios"));

        assertEquals(Arrays.asList("varios"), carpetasNoPermitidas(dominio, CARPETAS_DOMINIO));
    }

    @Test
    public void laReglaDetectaUnEnumFueraDeLasCarpetasDeEnums() throws IOException {
        Path dominio = temporal.newFolder("domain").toPath();
        Path entidad = Files.createDirectory(dominio.resolve("entity"));
        Path enumeracion = Files.createDirectory(dominio.resolve("enumeration"));
        Files.write(entidad.resolve("Mal.java"), "public enum Mal { A }".getBytes(StandardCharsets.UTF_8));
        Files.write(enumeracion.resolve("Bien.java"), "public enum Bien { A }".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("entity/Mal.java"), enumsMalUbicados(dominio));
    }

    @Test
    public void laReglaDetectaUnEstadoFueraDeState() throws IOException {
        Path dominio = temporal.newFolder("domain").toPath();
        Path entidad = Files.createDirectory(dominio.resolve("entity"));
        Files.write(entidad.resolve("EstadoRaro.java"),
                "public class EstadoRaro implements EstadoSesion {}".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("entity/EstadoRaro.java"), implementacionesFueraDeState(dominio));
        assertFalse(implementacionesFueraDeState(dominio).isEmpty());
    }
}
