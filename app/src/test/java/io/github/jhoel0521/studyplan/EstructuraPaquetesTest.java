package io.github.jhoel0521.studyplan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
 * <p>Las reglas se escriben como métodos estáticos que devuelven los incumplimientos, para poder probar que fallan
 * ante una clase mal puesta (ver las pruebas con {@link TemporaryFolder}).
 */
public class EstructuraPaquetesTest {

    private static final String BASE = "io/github/jhoel0521/studyplan";
    static final Set<String> CARPETAS_DOMINIO = new HashSet<>(Arrays.asList(
            "entity", "valueobject", "enumeration", "state", "service", "result", "readmodel"));
    private static final Set<String> CARPETAS_CON_ENUMS = new HashSet<>(Arrays.asList(
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

    // ------------------------------------------- las reglas fallan cuando la estructura se rompe

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
