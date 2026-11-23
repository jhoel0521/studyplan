package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Borrado selectivo con doble confirmación: si se elige borrar también Drive y Drive no responde, no se borra nada del
 * teléfono; cancelar la segunda confirmación no borra nada en ningún alcance.
 *
 * <p>Verifica: HU-38 y RN-21. El emulador no tiene una cuenta de Google autorizada, así que Drive falla.
 */
@RunWith(AndroidJUnit4.class)
public class BorrarDatosAlcanceEspressoTest {

    @Before
    public void conDatosYCuenta() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
        Pruebas.app().configurarRespaldo().vincularCuenta("ana@example.com");
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    private static void confirmarDosVeces() throws InterruptedException {
        Pruebas.esperarVisible(withId(android.R.id.button1));
        onView(withId(android.R.id.button1)).perform(click());
        Pruebas.esperarVisible(withId(android.R.id.button1));
        onView(withId(android.R.id.button1)).perform(click());
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void borrarTelefonoYDriveSinAccesoADriveNoBorraNadaDelTelefono() throws InterruptedException {
        try (ActivityScenario<BorrarDatosActivity> escenario = ActivityScenario.launch(BorrarDatosActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.alcanceAmbos));
            onView(withId(R.id.alcanceAmbos)).perform(click());
            onView(withId(R.id.botonBorrarTelefono)).perform(click());
            confirmarDosVeces();

            // Vuelve a la pantalla con el botón habilitado: no se pasó al primer uso.
            Pruebas.esperarVisible(withId(R.id.botonBorrarTelefono));
        }
        assertEquals("Ana", Pruebas.app().consultarPerfil().ejecutar().orElseThrow(AssertionError::new).nombre());
        assertTrue(Pruebas.app().configurarRespaldo().consultar().hayCuenta());
    }

    @Test
    @SuppressWarnings("unused")
    public void cancelarLaSegundaConfirmacionConAlcanceDriveNoBorraNada() throws InterruptedException {
        try (ActivityScenario<BorrarDatosActivity> escenario = ActivityScenario.launch(BorrarDatosActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.alcanceDrive));
            onView(withId(R.id.alcanceDrive)).perform(click());
            onView(withId(R.id.botonBorrarTelefono)).perform(click());
            Pruebas.esperarVisible(withId(android.R.id.button1));
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withId(android.R.id.button2));
            onView(withId(android.R.id.button2)).perform(click());
            Pruebas.esperarVisible(withId(R.id.botonBorrarTelefono));
        }
        assertEquals("Ana", Pruebas.app().consultarPerfil().ejecutar().orElseThrow(AssertionError::new).nombre());
        assertTrue(Pruebas.app().configurarRespaldo().consultar().hayCuenta());
    }
}
