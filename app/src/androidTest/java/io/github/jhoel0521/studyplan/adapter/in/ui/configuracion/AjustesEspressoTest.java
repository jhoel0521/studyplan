package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;

/**
 * Elegir tema oscuro y luego inglés cambia la pantalla y se guarda en la base.
 *
 * <p>Verifica: HU-03 (Cambiar tema y Cambiar idioma), RNF-07 y RNF-08.
 */
@RunWith(AndroidJUnit4.class)
public class AjustesEspressoTest {

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
    public void elTemaOscuroYElInglesCambianLaPantallaYSeGuardan() throws InterruptedException {
        try (ActivityScenario<AjustesActivity> escenario = ActivityScenario.launch(AjustesActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.temaOscuro));
            onView(withId(R.id.temaOscuro)).perform(click());
            esperarAjustes(Idioma.ES);

            Pruebas.esperarVisible(withId(R.id.idiomaIngles));
            onView(withId(R.id.idiomaIngles)).perform(click());
            esperarAjustes(Idioma.EN);
            Pruebas.esperarVisible(withText("Settings"));
            Pruebas.esperarVisible(withText("Dark"));
        }
        Ajustes guardados = Pruebas.app().consultarAjustes().ejecutar();
        assertEquals(Tema.OSCURO, guardados.tema());
        assertEquals(Idioma.EN, guardados.idioma());
    }

    @Test
    @SuppressWarnings("unused")
    public void recordatorioPorDefectoSeGuardaAlElegirOMostrarBannersSinBloquear()
            throws InterruptedException {
        try (ActivityScenario<AjustesActivity> escenario =
                     ActivityScenario.launch(AjustesActivity.class)) {
            onView(withId(R.id.selectorRecordatorioDefecto)).perform(scrollTo());
            Pruebas.esperarVisible(withId(R.id.selectorRecordatorioDefecto));
            onView(withId(R.id.selectorRecordatorioDefecto))
                    .check(matches(withText(R.string.recordatorio_15_min)));

            onView(withId(R.id.selectorRecordatorioDefecto)).perform(click());
            onView(withText(R.string.recordatorio_ninguno))
                    .inRoot(isPlatformPopup())
                    .perform(click());

            esperarRecordatorioDefecto(AntelacionRecordatorio.SIN_RECORDATORIO);
            onView(withId(R.id.selectorRecordatorioDefecto))
                    .check(matches(withText(R.string.recordatorio_ninguno)));

            // Verifica que seleccionar 30 min abre diálogo si falta permiso o guarda directamente
            onView(withId(R.id.selectorRecordatorioDefecto)).perform(click());
            onView(withText(R.string.recordatorio_30_min))
                    .inRoot(isPlatformPopup())
                    .perform(click());

            esperarRecordatorioDefecto(AntelacionRecordatorio.MINUTOS_30);
        }

        Ajustes guardados = Pruebas.app().consultarAjustes().ejecutar();
        assertEquals(AntelacionRecordatorio.MINUTOS_30, guardados.recordatorioPorDefecto());
    }

    @SuppressWarnings("BusyWait") // sondeo con tope: la base se escribe en otro hilo
    private static void esperarRecordatorioDefecto(AntelacionRecordatorio esperado)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < limite) {
            if (Pruebas.app().consultarAjustes().ejecutar().recordatorioPorDefecto() == esperado) {
                Thread.sleep(200);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No se guardó el recordatorio por defecto: " + esperado);
    }

    @SuppressWarnings("BusyWait") // sondeo con tope: la base se escribe en otro hilo
    private static void esperarAjustes(Idioma idioma) throws InterruptedException {
        long limite = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < limite) {
            Ajustes a = Pruebas.app().consultarAjustes().ejecutar();
            if (a.tema() == Tema.OSCURO && a.idioma() == idioma) {
                Thread.sleep(500);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Los ajustes no llegaron a OSCURO y " + idioma);
    }
}
