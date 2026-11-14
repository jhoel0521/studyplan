package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.skydoves.colorpickerview.ColorPickerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;

/**
 * El modal de selección de color: atajos, campo hexadecimal, selector para arrastrar y cancelar.
 *
 * <p>Verifica: HU-06 y HU-07 (el estudiante elige cualquier color de la materia).
 */
@RunWith(AndroidJUnit4.class)
public class SelectorColorDialogoEspressoTest {

    private static final String COLOR_POR_DEFECTO = "#1E88E5";

    @Before
    public void conPerfil() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    private static void abrirModal() throws InterruptedException {
        Pruebas.esperarVisible(withId(R.id.filaColor));
        onView(withId(R.id.filaColor)).perform(click());
        Pruebas.esperarVisible(withId(R.id.campoColorHex));
    }

    /** Toca el selector a la derecha del centro, donde el color tiene saturación alta. */
    private static GeneralClickAction tocarEnElSelector() {
        CoordinatesProvider coordenadas = vista -> {
            int[] posicion = new int[2];
            vista.getLocationOnScreen(posicion);
            return new float[]{posicion[0] + vista.getWidth() * 0.8f, posicion[1] + vista.getHeight() * 0.5f};
        };
        return new GeneralClickAction(Tap.SINGLE, coordenadas, Press.FINGER, 0, 0);
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unAtajoMuestraSuCodigoYSeAplicaAlElegir() throws InterruptedException {
        try (ActivityScenario<MateriaFormularioActivity> escenario =
                     ActivityScenario.launch(MateriaFormularioActivity.class)) {
            abrirModal();
            onView(withContentDescription(endsWith("2"))).perform(click());
            onView(withId(R.id.campoColorHex)).check(matches(withText("43A047")));
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withText("#43A047"));
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void unHexadecimalValidoMueveElSelectorAlMismoColor() throws InterruptedException {
        try (ActivityScenario<MateriaFormularioActivity> escenario =
                     ActivityScenario.launch(MateriaFormularioActivity.class)) {
            abrirModal();
            onView(withId(R.id.campoColorHex)).perform(replaceText("1E88E5"), closeSoftKeyboard());
            Thread.sleep(600);
            onView(withId(R.id.selectorColor)).check((vista, error) -> {
                int color = ((ColorPickerView) vista).getColor();
                int esperado = 0xFF1E88E5;
                for (int desplazamiento = 0; desplazamiento <= 16; desplazamiento += 8) {
                    int diferencia = Math.abs(((color >> desplazamiento) & 0xFF) - ((esperado >> desplazamiento) & 0xFF));
                    assertTrue("El selector quedó en " + Integer.toHexString(color), diferencia <= 6);
                }
            });
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void tocarElSelectorDaUnColorOpacoYElCampoHexadecimalLoMuestra() throws InterruptedException {
        try (ActivityScenario<MateriaFormularioActivity> escenario =
                     ActivityScenario.launch(MateriaFormularioActivity.class)) {
            abrirModal();
            onView(withId(R.id.selectorColor)).perform(tocarEnElSelector());
            Thread.sleep(600);
            onView(withId(R.id.campoColorHex)).check((vista, error) -> {
                String texto = ((EditText) vista).getText().toString();
                assertEquals("Debe haber seis dígitos hexadecimales: " + texto, 6, texto.length());
                assertTrue(texto.matches("[0-9A-F]{6}"));
                assertFalse("El color no cambió", texto.equals("1E88E5"));
            });
            onView(withId(android.R.id.button1)).perform(click());
        }
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void cancelarElModalNoCambiaElColor() throws InterruptedException {
        try (ActivityScenario<MateriaFormularioActivity> escenario =
                     ActivityScenario.launch(MateriaFormularioActivity.class)) {
            abrirModal();
            onView(withContentDescription(endsWith("2"))).perform(click());
            onView(withId(android.R.id.button2)).perform(click());
            Pruebas.esperarVisible(withText(COLOR_POR_DEFECTO));
        }
    }
}
