package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.MainActivity;

/**
 * Navegación entre pestañas: pestaña seleccionada, pestañas habilitadas y pila de retroceso {@code [Inicio, pestaña]}.
 *
 * <p>Verifica: HU-09 (la pestaña Materias) y la decisión D3 de {@code add-gestion-materias}.
 */
@RunWith(AndroidJUnit4.class)
public class NavegacionEspressoTest {

    @Before
    public void conPerfil() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    private static void comprobarQueAtrasSaleDeLaApp() {
        try {
            pressBack();
            fail("Atrás desde Inicio debía salir de la app");
        } catch (NoActivityResumedException esperado) {
            // la app terminó: es lo que se espera
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void soloLasPestanasImplementadasEstanHabilitadas() throws InterruptedException {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(allOf(withId(R.id.nav_inicio), isSelected()));
            onView(withId(R.id.nav_materias)).check(matches(isEnabled()));
            onView(withId(R.id.nav_calendario)).check(matches(isEnabled()));
            onView(withId(R.id.nav_estadisticas)).check(matches(isEnabled()));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void irAMateriasSeleccionaLaPestanaYAtrasVuelveAInicio() throws InterruptedException {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(allOf(withId(R.id.nav_inicio), isSelected()));
            onView(withId(R.id.nav_materias)).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.nav_materias), isSelected()));
            Pruebas.esperarVisible(withId(R.id.botonAgregarMateria));

            pressBack();
            Pruebas.esperarVisible(allOf(withId(R.id.nav_inicio), isSelected()));
            comprobarQueAtrasSaleDeLaApp();
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void volverAInicioDesdeMateriasDejaInicioComoRaiz() throws InterruptedException {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(allOf(withId(R.id.nav_inicio), isSelected()));
            onView(withId(R.id.nav_materias)).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.nav_materias), isSelected()));
            onView(withId(R.id.nav_inicio)).perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.nav_inicio), isSelected()));
            comprobarQueAtrasSaleDeLaApp();
        }
    }
}
