package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;

/**
 * Flujo de primer uso, de la pantalla de perfil a Inicio, y conservación del perfil al reabrir la app.
 *
 * <p>Verifica: HU-01 (Primer uso y Nombre vacío), HU-02 (saludo y progreso {@code 0 / meta}) y RN-01.
 */
@RunWith(AndroidJUnit4.class)
public class PerfilInicialEspressoTest {

    @Before
    public void estadoLimpio() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void enElPrimerUsoSeMuestraElFormulario() throws InterruptedException {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonGuardar));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unNombreVacioMuestraElError() throws InterruptedException {
        try (ActivityScenario<PerfilInicialActivity> escenario = ActivityScenario.launch(PerfilInicialActivity.class)) {
            onView(withId(R.id.botonGuardar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.error_nombre_vacio));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unPerfilValidoAbreInicioConElSaludoYElProgresoEnCero() throws InterruptedException {
        try (ActivityScenario<PerfilInicialActivity> escenario = ActivityScenario.launch(PerfilInicialActivity.class)) {
            onView(withId(R.id.campoNombre)).perform(typeText("Ana"), closeSoftKeyboard());
            onView(withId(R.id.campoMeta)).perform(typeText("20"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardar)).perform(click());
            Pruebas.esperarVisible(withText(containsString("Ana")));
            Pruebas.esperarVisible(withText("0 / 20 h"));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void inicioQuedaSeleccionadoEnLaBarraInferior() throws InterruptedException {
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(allOf(withId(R.id.nav_inicio), isSelected()));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void alCerrarYReabrirLaAppElPerfilSeConserva() throws InterruptedException {
        Pruebas.app().configurarPerfil().ejecutar("Luis", 15);
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withText(containsString("Luis")));
            Pruebas.esperarVisible(withText("0 / 15 h"));
        }
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withText(containsString("Luis")));
        }
    }
}
