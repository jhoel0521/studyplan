package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * El primer uso ofrece «Restaurar desde Google Drive» para un teléfono nuevo y no vincula ninguna cuenta mientras el
 * estudiante no lo pida.
 *
 * <p>Verifica: HU-37 y HU-35 (apagado por defecto).
 */
@RunWith(AndroidJUnit4.class)
public class RestaurarDesdeDriveEspressoTest {

    @Before
    public void sinDatos() {
        Pruebas.reiniciarEstado();
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void elPrimerUsoOfreceRestaurarYNoVinculaNingunaCuenta() throws InterruptedException {
        try (ActivityScenario<PerfilInicialActivity> escenario = ActivityScenario.launch(PerfilInicialActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonRestaurarDrive));
            onView(withId(R.id.botonRestaurarDrive)).perform(scrollTo());
            onView(withId(R.id.botonRestaurarDrive)).check(matches(isDisplayed()));
            onView(withId(R.id.botonRestaurarDrive)).check(matches(isEnabled()));
            onView(withId(R.id.botonGuardar)).check(matches(isDisplayed()));
        }
        assertFalse(Pruebas.app().configurarRespaldo().consultar().hayCuenta());
    }
}
