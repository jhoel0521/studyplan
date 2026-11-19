package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;

import android.view.View;

import java.util.concurrent.TimeUnit;

import io.github.jhoel0521.studyplan.domain.AlcanceBorrado;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Ayudas comunes de las pruebas de pantalla: estado limpio y espera de vistas que dependen del hilo en segundo plano.
 *
 * <p>Verifica: HU-01, HU-02, HU-03, HU-04 y HU-38 (apoyo de las pruebas Espresso).
 */
final class Pruebas {

    private static final long ESPERA_MAXIMA_MS = 8000;

    private Pruebas() {
    }

    static StudyPlanApplication app() {
        return StudyPlanApplication.de(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    /** Borra todos los datos y devuelve el tema y el idioma a sus valores por defecto. */
    static void reiniciarEstado() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        try {
            app().ejecutor().submit(() ->
                    app().borrarDatos().ejecutar(AlcanceBorrado.TELEFONO))
                    .get(ESPERA_MAXIMA_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo reiniciar el estado de prueba", e);
        }
        android.content.Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.getSharedPreferences("marcador_salida_app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().clear().commit();
        ctx.getSharedPreferences("preferencias_no_molestar_sistema", android.content.Context.MODE_PRIVATE)
                .edit().clear().commit();
        NotificadorSesion.cancelarAvisoRetorno(ctx);
        NotificadorSesion.cancelarTodosRecordatorios(ctx);
        if (app().contadorActividades() != null) {
            app().contadorActividades().reiniciarParaPruebas();
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AplicadorApariencia.aplicarTema(Tema.SISTEMA);
            AplicadorApariencia.aplicarIdioma(ctx, Idioma.ES);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    /** Espera hasta 8 s a que ninguna vista cumpla el criterio (las listas animan las filas que desaparecen). */
    @SuppressWarnings("BusyWait") // sondeo con tope
    static void esperarAusente(Matcher<View> criterio) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (true) {
            try {
                onView(criterio).check(doesNotExist());
                return;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() > limite) {
                    throw e;
                }
                Thread.sleep(150);
            }
        }
    }

    /** Espera hasta 8 s a que la vista cumpla el criterio (las lecturas de datos ocurren en segundo plano). */
    @SuppressWarnings("BusyWait") // sondeo con tope: las lecturas ocurren en otro hilo
    static void esperarVisible(Matcher<View> criterio) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (true) {
            try {
                ViewInteraction vista = onView(criterio);
                vista.check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException | AssertionError e) {
                if (System.currentTimeMillis() > limite) {
                    throw e;
                }
                Thread.sleep(150);
            }
        }
    }
}
