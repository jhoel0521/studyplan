package io.github.jhoel0521.studyplan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prueba de arquitectura: comprueba la regla de dependencia de la arquitectura hexagonal leyendo los fuentes.
 * {@code domain} y {@code application} (el núcleo) no importan Android, ni los adaptadores, y el dominio tampoco
 * importa la capa de aplicación. Falla si no encuentra ningún fuente, para que una ruta mal resuelta no la deje pasar
 * en vacío.
 *
 * <p>Verifica: HU-01 a HU-04 y HU-38 (que su lógica vive en el núcleo), RNF-15 (reglas de dominio aisladas y
 * probables con JUnit) y la decisión D1 de {@code add-configuracion-inicial}.
 */
public class ArquitecturaTest {

    private static final String BASE = "io/github/jhoel0521/studyplan";
    private static final Pattern IMPORT_ANDROID =
            Pattern.compile("^\\s*import\\s+(static\\s+)?(android|androidx)\\.", Pattern.MULTILINE);
    private static final Pattern IMPORT_ADAPTADOR =
            Pattern.compile("^\\s*import\\s+(static\\s+)?io\\.github\\.jhoel0521\\.studyplan\\.adapter\\.",
                    Pattern.MULTILINE);
    private static final Pattern IMPORT_APLICACION =
            Pattern.compile("^\\s*import\\s+(static\\s+)?io\\.github\\.jhoel0521\\.studyplan\\.application\\.",
                    Pattern.MULTILINE);

    private static final Pattern ANDROID_COMPLETO = Pattern.compile("\\bandroidx?\\.[a-z]+\\.[A-Za-z]");
    private static final Pattern VISTA_O_CONTEXTO = Pattern.compile(
            "^\\s*import\\s+android\\.(content\\.Context|app\\.|view\\.|widget\\.)", Pattern.MULTILINE);

    private static Path raizDeFuentes() {
        Path[] candidatas = {
                Paths.get("src/main/java", BASE),
                Paths.get("app/src/main/java", BASE)
        };
        for (Path candidata : candidatas) {
            if (Files.isDirectory(candidata)) {
                return candidata;
            }
        }
        throw new AssertionError("No se encontró la carpeta de fuentes; la prueba no puede comprobar nada");
    }

    private static List<Path> fuentesDe(String paquete) throws IOException {
        Path carpeta = raizDeFuentes().resolve(paquete);
        assertTrue("No existe el paquete " + paquete, Files.isDirectory(carpeta));
        try (Stream<Path> recorrido = Files.walk(carpeta)) {
            List<Path> fuentes = recorrido.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
            assertFalse("El paquete " + paquete + " no tiene fuentes: la prueba pasaría en vacío", fuentes.isEmpty());
            return fuentes;
        }
    }

    private static List<String> infractores(List<Path> fuentes, Pattern prohibido) throws IOException {
        List<String> infractores = new ArrayList<>();
        for (Path fuente : fuentes) {
            String texto = Files.readString(fuente);
            if (prohibido.matcher(texto).find()) {
                infractores.add(fuente.getFileName().toString());
            }
        }
        return infractores;
    }

    @Test
    public void dominioNoImportaAndroid() throws IOException {
        assertTrue("domain importa Android: " + infractores(fuentesDe("domain"), IMPORT_ANDROID),
                infractores(fuentesDe("domain"), IMPORT_ANDROID).isEmpty());
    }

    @Test
    public void aplicacionNoImportaAndroid() throws IOException {
        assertTrue("application importa Android: " + infractores(fuentesDe("application"), IMPORT_ANDROID),
                infractores(fuentesDe("application"), IMPORT_ANDROID).isEmpty());
    }

    @Test
    public void elNucleoNoUsaNombresCompletosDeAndroid() throws IOException {
        List<Path> nucleo = new ArrayList<>(fuentesDe("domain"));
        nucleo.addAll(fuentesDe("application"));
        assertTrue("El núcleo usa android.* con nombre completo: " + infractores(nucleo, ANDROID_COMPLETO),
                infractores(nucleo, ANDROID_COMPLETO).isEmpty());
    }

    @Test
    public void losViewModelNoImportanContextNiVistas() throws IOException {
        List<Path> modelos = fuentesDe("adapter/in/ui").stream()
                .filter(p -> p.getFileName().toString().endsWith("ViewModel.java")).collect(Collectors.toList());
        assertFalse("No hay ViewModel que revisar", modelos.isEmpty());
        assertTrue("Un ViewModel importa Context o vistas: " + infractores(modelos, VISTA_O_CONTEXTO),
                infractores(modelos, VISTA_O_CONTEXTO).isEmpty());
    }

    @Test
    public void elNucleoNoDependeDeLosAdaptadores() throws IOException {
        List<Path> nucleo = new ArrayList<>(fuentesDe("domain"));
        nucleo.addAll(fuentesDe("application"));
        assertTrue("El núcleo importa un adaptador: " + infractores(nucleo, IMPORT_ADAPTADOR),
                infractores(nucleo, IMPORT_ADAPTADOR).isEmpty());
    }

    @Test
    public void elDominioNoDependeDeLaAplicacion() throws IOException {
        assertTrue("domain importa application: " + infractores(fuentesDe("domain"), IMPORT_APLICACION),
                infractores(fuentesDe("domain"), IMPORT_APLICACION).isEmpty());
    }
}
