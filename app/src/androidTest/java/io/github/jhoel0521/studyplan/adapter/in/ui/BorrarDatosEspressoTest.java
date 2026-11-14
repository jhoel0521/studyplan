package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Borrar los datos del teléfono con la doble confirmación: la base queda vacía, tema e idioma vuelven a sus valores
 * por defecto, el bloqueo queda desactivado y se muestra el primer uso.
 *
 * <p>Verifica: HU-38 (alcance «teléfono»), RN-21 y HU-03 (valores por defecto).
 */
@RunWith(AndroidJUnit4.class)
public class BorrarDatosEspressoTest {

    @Before
    public void conDatos() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
        Pruebas.app().configurarApariencia().cambiarTema(Tema.OSCURO);
        Pruebas.app().configurarApariencia().cambiarIdioma(Idioma.EN);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AplicadorApariencia.aplicarTema(Tema.OSCURO));
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void trasLaDobleConfirmacionTodoVuelveAlPrimerUso() throws InterruptedException {
        try (ActivityScenario<BorrarDatosActivity> escenario = ActivityScenario.launch(BorrarDatosActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonBorrarTelefono));
            onView(withId(R.id.botonBorrarTelefono)).perform(click());
            Pruebas.esperarVisible(withId(android.R.id.button1));
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withId(android.R.id.button1));
            onView(withId(android.R.id.button1)).perform(click());

            Pruebas.esperarVisible(withId(R.id.botonGuardar));
        }
        assertFalse(Pruebas.app().consultarPerfil().ejecutar().isPresent());
        assertEquals(Ajustes.porDefecto(), Pruebas.app().consultarAjustes().ejecutar());
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode());
        assertEquals("es", AppCompatDelegate.getApplicationLocales().toLanguageTags());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void cancelarEnLaSegundaConfirmacionNoBorraNada() throws InterruptedException {
        try (ActivityScenario<BorrarDatosActivity> escenario = ActivityScenario.launch(BorrarDatosActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonBorrarTelefono));
            onView(withId(R.id.botonBorrarTelefono)).perform(click());
            Pruebas.esperarVisible(withId(android.R.id.button1));
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withId(android.R.id.button2));
            onView(withId(android.R.id.button2)).perform(click());
            Pruebas.esperarVisible(withId(R.id.botonBorrarTelefono));
        }
        assertEquals("Ana", Pruebas.app().consultarPerfil().ejecutar().orElseThrow(AssertionError::new).nombre());
    }
}
