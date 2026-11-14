package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.ListadoMaterias;

/**
 * Flujo completo de materias en pantalla: registrar, rechazar un nombre repetido, editar, archivar y desarchivar,
 * eliminar y lista vacía.
 *
 * <p>Verifica: HU-06 (Registro válido y Nombre duplicado), HU-07 (Edición), HU-08 (Archivar y Eliminar sin
 * historial), HU-09 (Lista y Lista vacía) y HU-46 (Desarchivar).
 */
@RunWith(AndroidJUnit4.class)
public class MateriasEspressoTest {

    private static final int COLOR = 0xFF1E88E5;

    @Before
    public void conPerfil() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    private static void abrirFormularioDeNueva() throws InterruptedException {
        Pruebas.esperarVisible(withId(R.id.botonAgregarMateria));
        onView(withId(R.id.botonAgregarMateria)).perform(click());
        Pruebas.esperarVisible(withId(R.id.campoNombreMateria));
    }

    private static void escribirNombreYGuardar(String nombre) {
        onView(withId(R.id.campoNombreMateria)).perform(replaceText(nombre), closeSoftKeyboard());
        onView(withId(R.id.botonGuardarMateria)).perform(click());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void sinMateriasSeInvitaARegistrarLaPrimera() throws InterruptedException {
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.textoMateriasVacio));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void registrarUnaMateriaLaMuestraEnLaLista() throws InterruptedException {
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            abrirFormularioDeNueva();
            escribirNombreYGuardar("  Matemáticas  ");
            Pruebas.esperarVisible(withText("Matemáticas"));
            onView(withId(R.id.textoMateriasVacio)).check(
                    (vista, error) -> assertNotEquals(android.view.View.VISIBLE, vista.getVisibility()));
        }
        ListadoMaterias listado = Pruebas.app().listarMaterias().ejecutar();
        assertEquals("Matemáticas", listado.activas().get(0).materia().nombre());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unColorEscritoEnHexadecimalSeGuardaTalCual() throws InterruptedException {
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            abrirFormularioDeNueva();
            onView(withId(R.id.campoNombreMateria)).perform(replaceText("Arte"), closeSoftKeyboard());
            onView(withId(R.id.filaColor)).perform(click());
            Pruebas.esperarVisible(withId(R.id.campoColorHex));
            onView(withId(R.id.campoColorHex)).perform(replaceText("E66465"), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            onView(withId(R.id.botonGuardarMateria)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText("Arte"));
        }
        assertEquals(0xFFE66465, Pruebas.app().listarMaterias().ejecutar().activas().get(0).materia().color());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unColorHexadecimalIncompletoSeRechazaAlElegir() throws InterruptedException {
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            abrirFormularioDeNueva();
            onView(withId(R.id.campoNombreMateria)).perform(replaceText("Arte"), closeSoftKeyboard());
            onView(withId(R.id.filaColor)).perform(click());
            Pruebas.esperarVisible(withId(R.id.campoColorHex));
            onView(withId(R.id.campoColorHex)).perform(replaceText("E6646"), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withText(R.string.error_color_hex));
        }
        assertTrue(Pruebas.app().listarMaterias().ejecutar().estaVacio());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unNombreRepetidoSeRechazaSinDistinguirMayusculasNiAcentos() throws InterruptedException {
        Pruebas.app().registrarMateria().ejecutar("Matemáticas", COLOR, null);
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            abrirFormularioDeNueva();
            escribirNombreYGuardar("  MATEMATICAS ");
            Pruebas.esperarVisible(withText(R.string.error_materia_duplicada));
        }
        assertEquals(1, Pruebas.app().listarMaterias().ejecutar().activas().size());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void editarCambiaElNombreConservandoLaMateria() throws InterruptedException {
        Pruebas.app().registrarMateria().ejecutar("Física", COLOR, null);
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            Pruebas.esperarVisible(withText("Física"));
            onView(withText("Física")).perform(click());
            Pruebas.esperarVisible(withId(R.id.campoNombreMateria));
            escribirNombreYGuardar("Física II");
            Pruebas.esperarVisible(withText("Física II"));
        }
        assertEquals(1, Pruebas.app().listarMaterias().ejecutar().activas().size());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void archivarLaMueveAArchivadasYDesarchivarLaDevuelve() throws InterruptedException {
        Pruebas.app().registrarMateria().ejecutar("Química", COLOR, null);
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            Pruebas.esperarVisible(withText("Química"));
            onView(withText("Química")).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.botonArchivarMateria), isEnabled()));
            onView(withId(R.id.botonArchivarMateria)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withId(R.id.tituloArchivadas));
            Pruebas.esperarAusente(withText("Química"));
            onView(withId(R.id.tituloArchivadas)).perform(click());
            Pruebas.esperarVisible(withText("Química"));

            onView(withText("Química")).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.botonArchivarMateria), isEnabled()));
            onView(withId(R.id.botonArchivarMateria)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText("Química"));
            Pruebas.esperarAusente(withId(R.id.tituloArchivadas));
        }
        ListadoMaterias listado = Pruebas.app().listarMaterias().ejecutar();
        assertEquals(1, listado.activas().size());
        assertTrue(listado.archivadas().isEmpty());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void eliminarUnaMateriaSinHistorialLaBorraTrasConfirmar() throws InterruptedException {
        Pruebas.app().registrarMateria().ejecutar("Historia", COLOR, null);
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            Pruebas.esperarVisible(withText("Historia"));
            onView(withText("Historia")).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.botonEliminarMateria), isEnabled()));
            onView(withId(R.id.botonEliminarMateria)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withId(android.R.id.button1));
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withId(R.id.textoMateriasVacio));
        }
        assertTrue(Pruebas.app().listarMaterias().ejecutar().estaVacio());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void cancelarLaConfirmacionDeEliminarNoBorraLaMateria() throws InterruptedException {
        Pruebas.app().registrarMateria().ejecutar("Historia", COLOR, null);
        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            Pruebas.esperarVisible(withText("Historia"));
            onView(withText("Historia")).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.botonEliminarMateria), isEnabled()));
            onView(withId(R.id.botonEliminarMateria)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withId(android.R.id.button2));
            onView(withId(android.R.id.button2)).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.botonEliminarMateria), isEnabled()));
        }
        assertFalse(Pruebas.app().listarMaterias().ejecutar().estaVacio());
    }
}
