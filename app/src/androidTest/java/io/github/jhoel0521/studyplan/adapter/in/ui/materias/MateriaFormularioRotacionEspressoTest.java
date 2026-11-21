package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.endsWith;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;

/**
 * Girar la pantalla con el formulario de materia abierto conserva lo escrito y el color elegido.
 *
 * <p>Verifica: HU-06 (registrar materia) y RNF-14: no se pierden los datos del estudiante al rotar.
 */
@RunWith(AndroidJUnit4.class)
public class MateriaFormularioRotacionEspressoTest {

    @Before
    public void conPerfil() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void girarElFormularioConservaLoEscritoYElColor() throws InterruptedException {
        try (ActivityScenario<MateriaFormularioActivity> escenario =
                     ActivityScenario.launch(MateriaFormularioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.campoNombreMateria));
            onView(withId(R.id.campoNombreMateria)).perform(replaceText("Química"), closeSoftKeyboard());
            onView(withId(R.id.campoDocenteMateria)).perform(replaceText("Dr. Paz"), closeSoftKeyboard());
            onView(withId(R.id.filaColor)).perform(click());
            Pruebas.esperarVisible(withId(R.id.atajosColores));
            onView(withContentDescription(endsWith("3"))).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            escenario.recreate();
            Pruebas.esperarVisible(withText("Química"));
            onView(withId(R.id.campoDocenteMateria)).check(matches(withText("Dr. Paz")));
            Pruebas.esperarVisible(withText("#FB8C00"));
        }
    }
}
